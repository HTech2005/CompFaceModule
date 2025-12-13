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

        // Endpoint: Analyze Face (Option 4 equivalent)
        post("/api/analyze", (req, res) -> {
            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));

            try (InputStream is = req.raw().getPart("image").getInputStream()) {
                Path tempFile = Files.createTempFile("upload_", ".jpg");
                Files.copy(is, tempFile, StandardCopyOption.REPLACE_EXISTING);

                Mat image = opencv_imgcodecs.imread(tempFile.toAbsolutePath().toString());
                if (image.empty()) {
                    res.status(400);
                    return "Impossible de lire l'image";
                }

                FaceAnalyzer analyzer = new FaceAnalyzer();
                List<FaceFeature> features = analyzer.analyzeFace(image);

                Map<String, Object> result = new HashMap<>();
                if (!features.isEmpty()) {
                    FaceFeature largest = features.get(0);
                    for (FaceFeature f : features) {
                        if (f.faceWidth > largest.faceWidth)
                            largest = f;
                    }
                    result.put("found", true);
                    result.put("features", largest);
                } else {
                    result.put("found", false);
                }

                Files.deleteIfExists(tempFile);

                res.type("application/json");
                return new Gson().toJson(result);

            } catch (Exception e) {
                e.printStackTrace();
                res.status(500);
                return "Erreur serveur: " + e.getMessage();
            }
        });

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

                double dist = Comparaison.distanceEuclidienne(N1, N2);
                double cos = Comparaison.similitudeCosinus(N1, N2);
                double score = Compatibilite.CalculCompatibilite(dist);

                Map<String, Object> result = new HashMap<>();
                result.put("match", Decision.dec(dist));
                result.put("scoreEuclidien", score);
                result.put("scoreCosinus", cos * 100);

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

                    // Seuil de décision (75% pour inclure le cas à 79%)
                    double threshold = 75.0;

                    for (Map.Entry<String, double[]> entry : databaseFeatures.entrySet()) {
                        double dist = Comparaison.distanceEuclidienne(features, entry.getValue());
                        double score = Compatibilite.CalculCompatibilite(dist);

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
                    result.put("isMatch", bestScore >= threshold);
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
