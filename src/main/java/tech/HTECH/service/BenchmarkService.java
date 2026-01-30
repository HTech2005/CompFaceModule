package tech.HTECH.service;

import org.bytedeco.opencv.opencv_core.Mat;
import tech.HTECH.FaceDetection;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BenchmarkService {

    private final FaceService faceService = new FaceService();

    public static class BenchmarkResult {
        private String imageA;
        private String imageB;
        private double chi2;
        private double eucl;
        private double cos;
        private double global;
        private boolean decision;
        private String status; // VP, VN, FP, FN

        public String getImageA() { return imageA; }
        public String getImageB() { return imageB; }
        public double getChi2() { return chi2; }
        public double getEucl() { return eucl; }
        public double getCos() { return cos; }
        public double getGlobal() { return global; }
        public String getDecision() { return decision ? "MATCH" : "NO_MATCH"; }
        public boolean isDecision() { return decision; }
        public String getStatus() { return status; }

        public String toCSVRow() {
            return String.format("%s;%s;%.2f;%.2f;%.2f;%.2f;%s;%s",
                    imageA, imageB, chi2, eucl, cos, global, getDecision(), status);
        }
    }

    public List<BenchmarkResult> runAnalysis(File targetFile) {
        List<BenchmarkResult> results = new ArrayList<>();
        Mat faceA = FaceDetection.detectFace(targetFile.getAbsolutePath());
        if (faceA == null) return results;

        double[] featuresA = faceService.extractFeatures(faceA);
        Map<String, double[]> featureMap = faceService.getDatabaseFeatures();

        for (Map.Entry<String, double[]> entry : featureMap.entrySet()) {
            String nameB = entry.getKey();
            
            // Éviter de se comparer avec soi-même
            if (targetFile.getName().equalsIgnoreCase(nameB)) continue;

            double[] featuresB = entry.getValue();

            FaceService.ComparisonResult res = faceService.compareFeatures(featuresA, featuresB);
            if (res == null) continue;

            BenchmarkResult br = new BenchmarkResult();
            br.imageA = targetFile.getName();
            br.imageB = nameB;
            br.chi2 = res.getScoreChi2();
            br.eucl = res.getScoreEuclidien();
            br.cos = res.getScoreCosinus();
            br.global = res.getScoreGlobal();
            br.decision = res.isMatch();
            
            boolean theoreticallySame = isTheoreticallySame(targetFile.getName(), nameB);
            
            if (theoreticallySame && br.decision) br.status = "VP (Vrai Positif)";
            else if (theoreticallySame && !br.decision) br.status = "FN (Faux Négatif)";
            else if (!theoreticallySame && br.decision) br.status = "FP (Faux Positif)";
            else br.status = "VN (Vrai Négatif)";

            results.add(br);
        }
        return results;
    }
    public List<BenchmarkResult> runFullAnalysis() {
        return runFullAnalysis(null);
    }

    public List<BenchmarkResult> runFullAnalysis(java.util.function.BiConsumer<Integer, Integer> progressCallback) {
        List<BenchmarkResult> results = new ArrayList<>();
        Map<String, double[]> featureMap = faceService.getDatabaseFeatures();

        if (featureMap.isEmpty()) return results;

        List<String> filenames = new ArrayList<>(featureMap.keySet());
        int n = filenames.size();
        int totalComparisons = n * n;
        int completed = 0;

        for (int i = 0; i < n; i++) {
            String nameA = filenames.get(i);
            double[] featuresA = featureMap.get(nameA);

            for (int j = 0; j < n; j++) {
                String nameB = filenames.get(j);
                
                // Éviter de se comparer avec soi-même
                if (nameA.equalsIgnoreCase(nameB)) continue;

                double[] featuresB = featureMap.get(nameB);

                FaceService.ComparisonResult res = faceService.compareFeatures(featuresA, featuresB);
                if (res == null) continue;

                BenchmarkResult br = new BenchmarkResult();
                br.imageA = nameA;
                br.imageB = nameB;
                br.chi2 = res.getScoreChi2();
                br.eucl = res.getScoreEuclidien();
                br.cos = res.getScoreCosinus();
                br.global = res.getScoreGlobal();
                br.decision = res.isMatch();

                boolean theoreticallySame = isTheoreticallySame(nameA, nameB);

                if (theoreticallySame && br.decision) br.status = "VP (Vrai Positif)";
                else if (theoreticallySame && !br.decision) br.status = "FN (Faux Négatif)";
                else if (!theoreticallySame && br.decision) br.status = "FP (Faux Positif)";
                else br.status = "VN (Vrai Négatif)";

                results.add(br);
                
                completed++;
                if (progressCallback != null && (completed % 100 == 0 || completed == totalComparisons)) {
                    progressCallback.accept(completed, totalComparisons);
                }
            }
        }
        return results;
    }

    private boolean isTheoreticallySame(String name1, String name2) {
        if (name1.equalsIgnoreCase(name2)) return true;

        // Extraction du préfixe "PersonneXX" (tout ce qui précède le premier underscore ou l'extension)
        String prefix1 = extractPrefix(name1);
        String prefix2 = extractPrefix(name2);

        return prefix1.equalsIgnoreCase(prefix2) && !prefix1.isEmpty();
    }

    private String extractPrefix(String name) {
        // Enlève l'extension
        String clean = name.split("\\.")[0];
        // Prend la partie avant l'underscore (ex: Personne01_02 -> Personne01)
        if (clean.contains("_")) {
            return clean.split("_")[0];
        }
        return clean;
    }
    // Changement de visibilité de inSameGroup dans HistoryService requis

    private String cleanName(String name) {
        return name.toLowerCase()
                .replaceAll("\\.jpg|\\.jpeg|\\.png", "")
                .replaceAll("[0-9]", "")
                .replaceAll("_", " ")
                .trim()
                .split(" ")[0]; // Prend le premier mot (généralement le prénom/ID)
    }
}
