package tech.HTECH;

import static spark.Spark.*;

import ij.process.ImageProcessor;
import com.google.gson.Gson;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;

import javax.servlet.MultipartConfigElement;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class APIServer {

    public static void main(String[] args) {
        // Chargement OpenCV
        Loader.load(opencv_core.class);

        port(4567);

        // Configuration pour upload de fichiers (Static files)
        staticFiles.location("/public");

        // CORS
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }

            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }

            return "OK";
        });

        before((request, response) -> response.header("Access-Control-Allow-Origin", "*"));

        // --- Initialisation Base de Données Temps Réel ---
        Map<String, double[]> databaseFeatures = new HashMap<>();
        try {
            File bddDir = new File("src/main/bdd");
            if (bddDir.exists() && bddDir.isDirectory()) {
                System.out.println("Chargement de la BDD pour le temps réel...");
                File[] files = bddDir.listFiles((dir, name) -> name.toLowerCase().matches(".*\\.(jpg|jpeg|png)$"));
                if (files != null) {
                    for (File f : files) {
                        try {
                            Mat face = FaceDetection.detectFace(f.getAbsolutePath());
                            if (face != null) {
                                ImageProcessor ip = Pretraitement.pt(OpenCVUtils.matToImageProcessor(face));
                                double[] h = Histogram.histo(ip);
                                double[] lbp = LBP.histogramLBP(LBP.LBP2D(ip));
                                double[] fusion = Fusion.fus(h, lbp);
                                double[] normalized = NormalizeVector.normalize(fusion);
                                databaseFeatures.put(f.getName(), normalized);
                                System.out.println("Indexé: " + f.getName());
                            }
                        } catch (Exception ex) {
                            System.err.println("Erreur indexation " + f.getName());
                        }
                    }
                }
                System.out.println("BDD chargée : " + databaseFeatures.size() + " visages.");
            } else {
                System.out.println("ATTENTION: Dossier src/main/bdd introuvable.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Endpoint: Compare Two Faces (Option 1 equivalent)
        post("/api/compare", (req, res) -> {
            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

            Path tempFile1 = Files.createTempFile("upload1_", ".jpg");
            Path tempFile2 = Files.createTempFile("upload2_", ".jpg");

            try (InputStream is1 = req.raw().getPart("image1").getInputStream();
                    InputStream is2 = req.raw().getPart("image2").getInputStream()) {

                Files.copy(is1, tempFile1, StandardCopyOption.REPLACE_EXISTING);
                Files.copy(is2, tempFile2, StandardCopyOption.REPLACE_EXISTING);

                Mat face1 = FaceDetection.detectFace(tempFile1.toAbsolutePath().toString());
                Mat face2 = FaceDetection.detectFace(tempFile2.toAbsolutePath().toString());

                if (face1 == null || face2 == null) {
                    res.type("application/json");
                    return new Gson().toJson(Map.of("error", "Visage non détecté sur l'une des images"));
                }

                ImageProcessor ip1 = Pretraitement.pt(OpenCVUtils.matToImageProcessor(face1));
                ImageProcessor ip2 = Pretraitement.pt(OpenCVUtils.matToImageProcessor(face2));

                double[] features1 = Fusion.fus(Histogram.histo(ip1), LBP.histogramLBP(LBP.LBP2D(ip1)));
                double[] features2 = Fusion.fus(Histogram.histo(ip2), LBP.histogramLBP(LBP.LBP2D(ip2)));

                double[] N1 = NormalizeVector.normalize(features1);
                double[] N2 = NormalizeVector.normalize(features2);

                double distChi2 = Comparaison.distanceKhiCarre(N1, N2);
                double cos = Comparaison.similitudeCosinus(N1, N2);
                double distEucl = Comparaison.distanceEuclidienne(N1, N2);

                double scoreTexture = Compatibilite.CalculCompatibilite(distChi2);

                // Encodage des visages détectés en Base64 pour prouver que ça marche
                String face1Base64 = OpenCVUtils.matToBase64(face1);
                String face2Base64 = OpenCVUtils.matToBase64(face2);

                double scoreEucl = (1.0 - distEucl) * 100.0;
                double globalScore = (scoreTexture * 0.5) + (cos * 100.0 * 0.3) + (scoreEucl * 0.2);

                Map<String, Object> result = new HashMap<>();
                result.put("match", Decision.dec(distChi2, cos, distEucl));
                result.put("scoreChi2", scoreTexture);
                result.put("scoreEuclidien", scoreEucl);
                result.put("scoreCosinus", cos * 100);
                result.put("scoreGlobal", globalScore);
                result.put("face1", face1Base64);
                result.put("face2", face2Base64);

                Files.deleteIfExists(tempFile1);
                Files.deleteIfExists(tempFile2);

                res.type("application/json");
                return new Gson().toJson(result);

            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return "Erreur: " + e.getMessage();
            }
        });

        // Endpoint: Realtime Search (Nouveau TR)
        post("/api/search", (req, res) -> {
            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

            try (InputStream is = req.raw().getPart("image").getInputStream()) {
                Path tempFile = Files.createTempFile("search_", ".jpg");
                Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);

                Mat face = FaceDetection.detectFace(tempFile.toAbsolutePath().toString());
                Files.deleteIfExists(tempFile);

                Map<String, Object> result = new HashMap<>();

                if (face == null) {
                    result.put("found", false);
                } else {
                    result.put("found", true);

                    // Extract features
                    ImageProcessor ip = Pretraitement.pt(OpenCVUtils.matToImageProcessor(face));
                    double[] h = Histogram.histo(ip);
                    double[] lbp = LBP.histogramLBP(LBP.LBP2D(ip));
                    double[] fusion = Fusion.fus(h, lbp);
                    double[] features = NormalizeVector.normalize(fusion);

                    // Compare against DB
                    String bestMatch = "Inconnu";
                    double bestScore = 0.0;

                    // Seuil de décision (70% pour inclure le cas à 79%)
                    double threshold = 90.0;

                    for (Map.Entry<String, double[]> entry : databaseFeatures.entrySet()) {
                        double distChi2 = Comparaison.distanceKhiCarre(features, entry.getValue());
                        double cosSim = Comparaison.similitudeCosinus(features, entry.getValue());
                        double distEucl = Comparaison.distanceEuclidienne(features, entry.getValue());

                        // Calcul d'un score fusionné triple pour le classement
                        double score = ((1.0 - (distChi2 / 2.0)) * 50.0) + (cosSim * 30.0) + ((1.0 - distEucl) * 20.0);

                        if (score > bestScore) {
                            bestScore = score;
                            bestMatch = entry.getKey();
                        }
                    }

                    // Logique finale
                    if (bestScore < threshold) {
                        bestMatch = "Inconnu";
                    }

                    result.put("bestMatch", bestMatch.replaceFirst("[.][^.]+$", "")); // Remove ext
                    result.put("score", bestScore);

                    // On recalcule les scores individuels pour le meilleur match pour l'affichage
                    double[] bestFeatures = databaseFeatures.get(bestMatch + ".jpg"); // Tentative avec extension
                    if (bestFeatures == null)
                        bestFeatures = databaseFeatures.get(bestMatch + ".png");
                    if (bestFeatures == null)
                        bestFeatures = databaseFeatures.get(bestMatch);

                    if (bestFeatures != null) {
                        double bestChi2 = Comparaison.distanceKhiCarre(features, bestFeatures);
                        double bestCos = Comparaison.similitudeCosinus(features, bestFeatures);
                        double bestEucl = Comparaison.distanceEuclidienne(features, bestFeatures);

                        double scoreEucl = (1.0 - bestEucl) * 100.0;
                        double globalScore = (Compatibilite.CalculCompatibilite(bestChi2) * 0.5)
                                + (bestCos * 100.0 * 0.3) + (scoreEucl * 0.2);

                        result.put("scoreChi2", Compatibilite.CalculCompatibilite(bestChi2));
                        result.put("scoreEuclidien", scoreEucl);
                        result.put("scoreCosinus", bestCos * 100.0);
                        result.put("scoreGlobal", globalScore);
                        result.put("isMatch", Decision.dec(bestChi2, bestCos, bestEucl));
                    }
                }

                res.type("application/json");
                return new Gson().toJson(result);

            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return "Erreur: " + e.getMessage();
            }
        });

        System.out.println("API Server is running on http://localhost:4567");
    }
}
