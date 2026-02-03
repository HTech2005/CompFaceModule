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
        return compareFaces(face1, face2, Decision.DecisionMode.TRIPLE_FUSION);
    }

    public ComparisonResult compareFaces(Mat face1, Mat face2, Decision.DecisionMode mode) {
        double[] N1 = extractFeatures(face1);
        double[] N2 = extractFeatures(face2);
        return compareFeatures(N1, N2, mode);
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
        return compareFeatures(N1, N2, Decision.DecisionMode.TRIPLE_FUSION);
    }

    public ComparisonResult compareFeatures(double[] N1, double[] N2, Decision.DecisionMode mode) {
        if (N1 == null || N2 == null) return null;

        double distChi2 = Comparaison.distanceKhiCarre(N1, N2);
        double cos = Comparaison.similitudeCosinus(N1, N2);
        double distEucl = Comparaison.distanceEuclidienne(N1, N2);

        double scoreTexture = Compatibilite.CalculCompatibilite(distChi2);
        double scoreEucl = Math.max(0.0, (1.0 - (distEucl / 0.065)) * 100.0);
        double scoreCos = cos * 100.0;

        // Poids optimisés (Recalib 8.0) : Cosinus (60%) + Texture (30%) + Géo (10%)
        // Le Cosinus est beaucoup plus robuste aux variations de pose et d'éclairage
        double globalScore = (scoreCos * 0.6) + (scoreTexture * 0.3) + (scoreEucl * 0.1);

        double activeScore;
        switch (mode) {
            case CHI_SQUARE: activeScore = scoreTexture; break;
            case EUCLIDEAN: activeScore = scoreEucl; break;
            case COSINE: activeScore = scoreCos; break;
            case TRIPLE_FUSION:
            default: activeScore = globalScore; break;
        }

        ComparisonResult result = new ComparisonResult();
        result.setMatch(Decision.dec(distChi2, cos, distEucl, mode));
        result.setScoreChi2(scoreTexture);
        result.setScoreEuclidien(scoreEucl);
        result.setScoreCosinus(scoreCos);
        result.setScoreGlobal(globalScore);
        result.setActiveScore(activeScore);

        return result;
    }

    public RecognitionResult recognizeFace(Mat face) {
        return recognizeFace(face, Decision.DecisionMode.TRIPLE_FUSION);
    }

    public RecognitionResult recognizeFace(Mat face, Decision.DecisionMode mode) {
        if (face == null)
            return null;

        ImageProcessor ip = Pretraitement.pt(OpenCVUtils.matToImageProcessor(face));
        double[] h = Histogram.histoGrid(ip, 8, 8);
        double[] lbp = LBP.histogramLBPGrid(LBP.LBP2D(ip), 8, 8);
        double[] fusion = Fusion.fus(h, lbp);
        double[] features = NormalizeVector.normalize(fusion);

        String bestMatchFile = null;
        double bestScore = -1.0;
        double threshold = 61.5; // Seuil recalibré à 61.5% (Recalib 6.0)

        for (Map.Entry<String, double[]> entry : databaseFeatures.entrySet()) {
            double distChi2 = Comparaison.distanceKhiCarre(features, entry.getValue());
            double cosSim = Comparaison.similitudeCosinus(features, entry.getValue());
            double distEucl = Comparaison.distanceEuclidienne(features, entry.getValue());

            double scoreChi2 = Compatibilite.CalculCompatibilite(distChi2);
            double scoreEucl = Math.max(0.0, (1.0 - (distEucl / 0.065)) * 100.0);
            double scoreCos = cosSim * 100.0;
            double globalScore = (scoreCos * 0.6) + (scoreChi2 * 0.3) + (scoreEucl * 0.1);

            double currentScore;
            switch (mode) {
                case CHI_SQUARE: currentScore = scoreChi2; break;
                case EUCLIDEAN: currentScore = scoreEucl; break;
                case COSINE: currentScore = scoreCos; break;
                case TRIPLE_FUSION:
                default: currentScore = globalScore; break;
            }

            if (currentScore > bestScore) {
                bestScore = currentScore;
                bestMatchFile = entry.getKey();
            }
        }

        System.out.println(
                "DEBUG TR: Meilleur score (" + mode + ") trouvé = " + String.format("%.2f%%", bestScore) + " pour " + bestMatchFile);

        RecognitionResult result = new RecognitionResult();
        result.setFound(bestMatchFile != null && bestScore >= threshold);

        if (bestMatchFile != null) {
            result.setBestMatch(bestMatchFile.replaceFirst("[.][^.]+$", ""));
            result.setBestMatchFile(bestMatchFile);
            result.setScore(bestScore);
            result.setScoreGlobal(bestScore); // Note: might need better clarification for "global" vs "score"

            double[] bestFeatures = databaseFeatures.get(bestMatchFile);
            double bc2 = Comparaison.distanceKhiCarre(features, bestFeatures);
            double bcs = Comparaison.similitudeCosinus(features, bestFeatures);
            double beu = Comparaison.distanceEuclidienne(features, bestFeatures);

            result.setScoreChi2(Compatibilite.CalculCompatibilite(bc2));
            result.setScoreEuclidien(Math.max(0.0, (1.0 - (beu / 0.065)) * 100.0));
            result.setScoreCosinus(bcs * 100.0);
            result.setMatch(Decision.dec(bc2, bcs, beu, mode));
        }

        return result;
    }

    public static class ComparisonResult {
        private boolean match;
        private double scoreChi2;
        private double scoreEuclidien;
        private double scoreCosinus;
        private double scoreGlobal;
        private double activeScore;

        public double getActiveScore() { return activeScore; }
        public void setActiveScore(double activeScore) { this.activeScore = activeScore; }

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
