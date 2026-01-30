package tech.HTECH.service;

import ij.process.ImageProcessor;
import org.bytedeco.opencv.opencv_core.Mat;
import tech.HTECH.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FaceService {
    // Cache statique pour éviter de recharger la BDD à chaque changement de vue
    private static final Map<String, double[]> databaseFeatures = new ConcurrentHashMap<>();
    private static volatile boolean isLoaded = false;
    private static volatile boolean indexing = false;

    public static boolean isIndexing() {
        return indexing;
    }

    public FaceService() {
        if (!isLoaded) {
            loadDatabase();
        }
    }

    private synchronized void loadDatabase() {
        if (isLoaded || indexing) return;
        new Thread(this::reloadDatabase).start();
    }

    public synchronized void reloadDatabase() {
        if (indexing) return;
        indexing = true;
        System.out.println("Indexation de la base de données (src/main/bdd) en arrière-plan...");
        databaseFeatures.clear();
        File bddDir = new File("src/main/bdd");
        if (bddDir.exists() && bddDir.isDirectory()) {
            File[] files = bddDir.listFiles((dir, name) -> name.toLowerCase().matches(".*\\.(jpg|jpeg|png)$"));
            if (files != null) {
                for (File f : files) {
                    indexFile(f);
                }
            }
        }
        indexing = false;
        isLoaded = true;
        System.out.println("Indexation terminée. " + databaseFeatures.size() + " visages chargés.");
    }

    public void indexFile(File f) {
        try {
            Mat face = FaceDetection.detectFace(f.getAbsolutePath());
            if (face != null) {
                ImageProcessor ip = Pretraitement.pt(OpenCVUtils.matToImageProcessor(face));
                double[] h = Histogram.histoGrid(ip, 8, 8);
                double[] lbp = LBP.histogramLBPGrid(LBP.LBP2D(ip), 8, 8);
                double[] fusion = Fusion.fus(h, lbp);
                double[] normalized = NormalizeVector.normalize(fusion);
                databaseFeatures.put(f.getName(), normalized);
                System.out.println("Indexé: " + f.getName());
            } else {
                System.err.println("Visage non détecté dans: " + f.getName());
            }
        } catch (Exception ex) {
            System.err.println("Erreur indexation " + f.getName() + ": " + ex.getMessage());
        }
    }

    public Map<String, double[]> getDatabaseFeatures() {
        return databaseFeatures;
    }
    public void removeFile(String fileName) {
        databaseFeatures.remove(fileName);
        System.out.println("Supprimé du cache: " + fileName);
    }

    public ComparisonResult compareFaces(Mat face1, Mat face2) {
        double[] N1 = extractFeatures(face1);
        double[] N2 = extractFeatures(face2);
        return compareFeatures(N1, N2);
    }

    public double[] extractFeatures(Mat face) {
        if (face == null) return null;
        ImageProcessor ip = Pretraitement.pt(OpenCVUtils.matToImageProcessor(face));
        double[] h = Histogram.histoGrid(ip, 8, 8);
        double[] lbp = LBP.histogramLBPGrid(LBP.LBP2D(ip), 8, 8);
        double[] fusion = Fusion.fus(h, lbp);
        return NormalizeVector.normalize(fusion);
    }

    public ComparisonResult compareFeatures(double[] N1, double[] N2) {
        if (N1 == null || N2 == null) return null;

        double distChi2 = Comparaison.distanceKhiCarre(N1, N2);
        double cos = Comparaison.similitudeCosinus(N1, N2);
        double distEucl = Comparaison.distanceEuclidienne(N1, N2);

        double scoreTexture = Compatibilite.CalculCompatibilite(distChi2);
        double scoreEucl = Math.max(0.0, (1.0 - (distEucl / 0.065)) * 100.0);

        // Poids rééquilibrés : 40% Texture (Chi2), 40% Global (Cosine), 20% Géométrie (Eucl)
        double globalScore = (scoreTexture * 0.4) + (cos * 40.0) + (scoreEucl * 0.2);

        ComparisonResult result = new ComparisonResult();
        result.setMatch(Decision.dec(distChi2, cos, distEucl));
        result.setScoreChi2(scoreTexture);
        result.setScoreEuclidien(scoreEucl);
        result.setScoreCosinus(cos * 100);
        result.setScoreGlobal(globalScore);

        return result;
    }

    public RecognitionResult recognizeFace(Mat face) {
        if (face == null)
            return null;

        ImageProcessor ip = Pretraitement.pt(OpenCVUtils.matToImageProcessor(face));
        double[] h = Histogram.histoGrid(ip, 8, 8);
        double[] lbp = LBP.histogramLBPGrid(LBP.LBP2D(ip), 8, 8);
        double[] fusion = Fusion.fus(h, lbp);
        double[] features = NormalizeVector.normalize(fusion);

        String bestMatchFile = null;
        double bestScore = 0.0;
        double threshold = 61.5; // Seuil recalibré à 61.5% (Recalib 6.0)

        for (Map.Entry<String, double[]> entry : databaseFeatures.entrySet()) {
            double distChi2 = Comparaison.distanceKhiCarre(features, entry.getValue());
            double cosSim = Comparaison.similitudeCosinus(features, entry.getValue());
            double distEucl = Comparaison.distanceEuclidienne(features, entry.getValue());

            // Poids rééquilibrés : 40% Texture (Chi2), 40% Global (Cosine), 20% Géométrie (Eucl)
            double score = (Compatibilite.CalculCompatibilite(distChi2) * 0.4) + (cosSim * 40.0)
                    + (Math.max(0.0, (1.0 - (distEucl / 0.065)) * 100.0) * 0.2);

            if (score > bestScore) {
                bestScore = score;
                bestMatchFile = entry.getKey();
            }
        }

        System.out.println(
                "DEBUG TR: Meilleur score trouvé = " + String.format("%.2f%%", bestScore) + " pour " + bestMatchFile);

        RecognitionResult result = new RecognitionResult();
        result.setFound(bestMatchFile != null && bestScore >= threshold);

        if (bestMatchFile != null) {
            result.setBestMatch(bestMatchFile.replaceFirst("[.][^.]+$", ""));
            result.setBestMatchFile(bestMatchFile);
            result.setScore(bestScore);
            result.setScoreGlobal(bestScore);

            double[] bestFeatures = databaseFeatures.get(bestMatchFile);
            double bc2 = Comparaison.distanceKhiCarre(features, bestFeatures);
            double bcs = Comparaison.similitudeCosinus(features, bestFeatures);
            double beu = Comparaison.distanceEuclidienne(features, bestFeatures);

            result.setScoreChi2(Compatibilite.CalculCompatibilite(bc2));
            result.setScoreEuclidien(Math.max(0.0, (1.0 - (beu / 0.065)) * 100.0));
            result.setScoreCosinus(bcs * 100.0);
            result.setMatch(Decision.dec(bc2, bcs, beu));
        }

        return result;
    }

    public static class ComparisonResult {
        private boolean match;
        private double scoreChi2;
        private double scoreEuclidien;
        private double scoreCosinus;
        private double scoreGlobal;

        public boolean isMatch() {
            return match;
        }

        public void setMatch(boolean match) {
            this.match = match;
        }

        public double getScoreChi2() {
            return scoreChi2;
        }

        public void setScoreChi2(double scoreChi2) {
            this.scoreChi2 = scoreChi2;
        }

        public double getScoreEuclidien() {
            return scoreEuclidien;
        }

        public void setScoreEuclidien(double scoreEuclidien) {
            this.scoreEuclidien = scoreEuclidien;
        }

        public double getScoreCosinus() {
            return scoreCosinus;
        }

        public void setScoreCosinus(double scoreCosinus) {
            this.scoreCosinus = scoreCosinus;
        }

        public double getScoreGlobal() {
            return scoreGlobal;
        }

        public void setScoreGlobal(double scoreGlobal) {
            this.scoreGlobal = scoreGlobal;
        }
    }

    public static class RecognitionResult {
        private boolean found;
        private String bestMatch;
        private String bestMatchFile;
        private double score;
        private double scoreChi2;
        private double scoreEuclidien;
        private double scoreCosinus;
        private double scoreGlobal;
        private boolean match;

        public boolean isFound() {
            return found;
        }

        public void setFound(boolean found) {
            this.found = found;
        }

        public String getBestMatch() {
            return bestMatch;
        }

        public void setBestMatch(String bestMatch) {
            this.bestMatch = bestMatch;
        }

        public String getBestMatchFile() {
            return bestMatchFile;
        }

        public void setBestMatchFile(String bestMatchFile) {
            this.bestMatchFile = bestMatchFile;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public double getScoreChi2() {
            return scoreChi2;
        }

        public void setScoreChi2(double scoreChi2) {
            this.scoreChi2 = scoreChi2;
        }

        public double getScoreEuclidien() {
            return scoreEuclidien;
        }

        public void setScoreEuclidien(double scoreEuclidien) {
            this.scoreEuclidien = scoreEuclidien;
        }

        public double getScoreCosinus() {
            return scoreCosinus;
        }

        public void setScoreCosinus(double scoreCosinus) {
            this.scoreCosinus = scoreCosinus;
        }

        public double getScoreGlobal() {
            return scoreGlobal;
        }

        public void setScoreGlobal(double scoreGlobal) {
            this.scoreGlobal = scoreGlobal;
        }

        public boolean isMatch() {
            return match;
        }

        public void setMatch(boolean match) {
            this.match = match;
        }
    }
}
