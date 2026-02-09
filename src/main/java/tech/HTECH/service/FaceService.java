package tech.HTECH.service;

import ij.process.ImageProcessor;
import org.bytedeco.opencv.opencv_core.Mat;
import tech.HTECH.*;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service principal de reconnaissance faciale.
 * ROLLBACK VERSION: Retour à la méthode éprouvée LBP + Histogramme pour corriger le FRR 100%.
 */
public class FaceService {
    // Cache statique
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
        System.out.println(">>> Démarrage Indexation : Mode LBP + Histogramme (Restauré) <<<");
        databaseFeatures.clear();

        File bddDir = new File("src/main/bdd");
        if (bddDir.exists() && bddDir.isDirectory()) {
            File[] files = bddDir.listFiles((dir, name) -> name.toLowerCase().matches(".*\\.(jpg|jpeg|png)$"));

            if (files != null && files.length > 0) {
                int count = 0;
                for (File f : files) {
                    indexFile(f);
                    count++;
                    if (count % 10 == 0) System.gc();
                }
            } else {
                System.out.println("Aucun fichier image trouvé dans src/main/bdd");
            }
        } else {
            System.err.println("Dossier src/main/bdd introuvable !");
        }

        indexing = false;
        isLoaded = true;
        System.out.println("Indexation terminée. " + databaseFeatures.size() + " visages chargés.");
    }

    public void indexFile(File f) {
        Mat face = null;
        try {
            face = FaceDetection.detectFace(f.getAbsolutePath());
            if (face != null) {
                double[] features = extractFeatures(face);
                if (features != null) {
                    databaseFeatures.put(f.getName(), features);
                    System.out.println("Indexé: " + f.getName());
                }
            } else {
                System.err.println("Visage non détecté dans: " + f.getName());
            }
        } catch (Exception ex) {
            System.err.println("Erreur indexation " + f.getName() + ": " + ex.getMessage());
        } finally {
            if (face != null) face.release();
        }
    }

    /**
     * Extraction Caractéristiques : LBP + Histogramme
     */
    public double[] extractFeatures(Mat face) {
        if (face == null) return null;

        // 1. Prétraitement
        ImageProcessor ipRaw = OpenCVUtils.matToImageProcessor(face);
        ImageProcessor ip = Pretraitement.pt(ipRaw);

        // 2. Histogramme (Structure) - 8x8
        double[] hist = Histogram.histoGrid(ip, 8, 8);

        // 3. LBP (Texture) - 8x8
        double[] lbp = LBP.histogramLBPGrid(LBP.LBP2D(ip), 8, 8);

        // 4. Fusion
        double[] fusion = Fusion.fus(hist, lbp);
        
        // 5. Normalisation
        return NormalizeVector.normalize(fusion);
    }

    // --- LOGIQUE DE COMPARAISON RESTAURÉE ---

    public ComparisonResult compareFaces(Mat face1, Mat face2) {
        return compareFaces(face1, face2, Decision.DecisionMode.TRIPLE_FUSION);
    }

    public ComparisonResult compareFaces(Mat face1, Mat face2, Decision.DecisionMode mode) {
        double[] N1 = extractFeatures(face1);
        double[] N2 = extractFeatures(face2);
        return compareFeatures(N1, N2, mode);
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
        double scoreEucl = Math.max(0.0, (1.0 - (distEucl / Decision.EUCLIDEAN_DIVISOR)) * 100.0);
        double scoreCos = cos * 100.0; 

        double globalScore = (scoreTexture * Decision.W_CHI) + 
                             (scoreCos * Decision.W_COS) + 
                             (scoreEucl * Decision.W_EUCL);

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
        if (face == null) return null;

        double[] features = extractFeatures(face);
        
        String bestMatchFile = null;
        double bestScore = -1.0;

        for (Map.Entry<String, double[]> entry : databaseFeatures.entrySet()) {
            ComparisonResult comp = compareFeatures(features, entry.getValue(), mode); 
            
            if (comp.getActiveScore() > bestScore) {
                bestScore = comp.getActiveScore();
                bestMatchFile = entry.getKey();
            }
        }

        System.out.println("DEBUG: Best=" + String.format("%.2f%%", bestScore) + " File=" + bestMatchFile);

        RecognitionResult result = new RecognitionResult();
        result.setFound(bestMatchFile != null && bestScore >= Decision.THRESHOLD);

        if (bestMatchFile != null) {
            result.setBestMatch(bestMatchFile.replaceFirst("[.][^.]+$", ""));
            result.setBestMatchFile(bestMatchFile);
            result.setScore(bestScore);
            result.setScoreGlobal(bestScore);
            
            double[] bestFeat = databaseFeatures.get(bestMatchFile);
            ComparisonResult detailed = compareFeatures(features, bestFeat, mode);
            
            result.setScoreChi2(detailed.getScoreChi2());
            result.setScoreEuclidien(detailed.getScoreEuclidien());
            result.setScoreCosinus(detailed.getScoreCosinus());
            result.setMatch(result.isFound());
        }

        return result;
    }

    public void removeFile(String fileName) {
        databaseFeatures.remove(fileName);
        System.out.println("Supprimé du cache: " + fileName);
    }

    public Map<String, double[]> getDatabaseFeatures() {
        return databaseFeatures;
    }

    public static class ComparisonResult {
        private boolean match;
        private double scoreChi2, scoreEuclidien, scoreCosinus, scoreGlobal, activeScore;
        public double getActiveScore() { return activeScore; }
        public void setActiveScore(double s) { this.activeScore = s; }
        public boolean isMatch() { return match; }
        public void setMatch(boolean m) { this.match = m; }
        public double getScoreChi2() { return scoreChi2; }
        public void setScoreChi2(double s) { this.scoreChi2 = s; }
        public double getScoreEuclidien() { return scoreEuclidien; }
        public void setScoreEuclidien(double s) { this.scoreEuclidien = s; }
        public double getScoreCosinus() { return scoreCosinus; }
        public void setScoreCosinus(double s) { this.scoreCosinus = s; }
        public double getScoreGlobal() { return scoreGlobal; }
        public void setScoreGlobal(double s) { this.scoreGlobal = s; }
    }

    public static class RecognitionResult {
        private boolean found, match;
        private String bestMatch, bestMatchFile;
        private double score, scoreChi2, scoreEuclidien, scoreCosinus, scoreGlobal;
        public boolean isFound() { return found; }
        public void setFound(boolean f) { this.found = f; }
        public String getBestMatch() { return bestMatch; }
        public void setBestMatch(String s) { this.bestMatch = s; }
        public String getBestMatchFile() { return bestMatchFile; }
        public void setBestMatchFile(String s) { this.bestMatchFile = s; }
        public double getScore() { return score; }
        public void setScore(double s) { this.score = s; }
        public double getScoreChi2() { return scoreChi2; }
        public void setScoreChi2(double s) { this.scoreChi2 = s; }
        public double getScoreEuclidien() { return scoreEuclidien; }
        public void setScoreEuclidien(double s) { this.scoreEuclidien = s; }
        public double getScoreCosinus() { return scoreCosinus; }
        public void setScoreCosinus(double s) { this.scoreCosinus = s; }
        public double getScoreGlobal() { return scoreGlobal; }
        public void setScoreGlobal(double s) { this.scoreGlobal = s; }
        public boolean isMatch() { return match; }
        public void setMatch(boolean m) { this.match = m; }
    }
}
