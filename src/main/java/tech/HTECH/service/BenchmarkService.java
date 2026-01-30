package tech.HTECH.service;

import org.bytedeco.opencv.opencv_core.Mat;
import tech.HTECH.FaceDetection;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BenchmarkService {

    private final FaceService faceService = new FaceService();

    public static class BenchmarkResult {
        public String imageA;
        public String imageB;
        public double chi2;
        public double eucl;
        public double cos;
        public double global;
        public boolean decision;
        public String status; // VP, VN, FP, FN

        public String toCSVRow() {
            return String.format("%s;%s;%.2f;%.2f;%.2f;%.2f;%s;%s",
                    imageA, imageB, chi2, eucl, cos, global, decision ? "MATCH" : "NO_MATCH", status);
        }
    }

    public List<BenchmarkResult> runAnalysis(File targetFile) {
        List<BenchmarkResult> results = new ArrayList<>();
        Mat faceA = FaceDetection.detectFace(targetFile.getAbsolutePath());
        if (faceA == null) return results;

        File bddDir = new File("src/main/bdd");
        File[] files = bddDir.listFiles((dir, name) -> name.toLowerCase().matches(".*\\.(jpg|jpeg|png)$"));

        if (files != null) {
            for (File fileB : files) {
                Mat faceB = FaceDetection.detectFace(fileB.getAbsolutePath());
                if (faceB == null) continue;

                FaceService.ComparisonResult res = faceService.compareFaces(faceA, faceB);
                
                BenchmarkResult br = new BenchmarkResult();
                br.imageA = targetFile.getName();
                br.imageB = fileB.getName();
                br.chi2 = res.getScoreChi2();
                br.eucl = res.getScoreEuclidien();
                br.cos = res.getScoreCosinus();
                br.global = res.getScoreGlobal();
                br.decision = res.isMatch();
                
                // Détermination du statut scientifique
                boolean theoreticallySame = isTheoreticallySame(targetFile.getName(), fileB.getName());
                
                if (theoreticallySame && br.decision) br.status = "VP (Vrai Positif)";
                else if (theoreticallySame && !br.decision) br.status = "FN (Faux Négatif)";
                else if (!theoreticallySame && br.decision) br.status = "FP (Faux Positif)";
                else br.status = "VN (Vrai Négatif)";

                results.add(br);
            }
        }
        return results;
    }

    private boolean isTheoreticallySame(String name1, String name2) {
        if (name1.equalsIgnoreCase(name2)) return true;
        
        // Nettoyage des noms (enlever extensions, chiffres et suffixes courants)
        String clean1 = cleanName(name1);
        String clean2 = cleanName(name2);
        
        // Si l'un est contenu dans l'autre (ex: "Ashley_face" et "Ashley")
        return clean1.contains(clean2) || clean2.contains(clean1);
    }

    private String cleanName(String name) {
        return name.toLowerCase()
                .replaceAll("\\.jpg|\\.jpeg|\\.png", "")
                .replaceAll("[0-9]", "")
                .replaceAll("_", " ")
                .trim()
                .split(" ")[0]; // Prend le premier mot (généralement le prénom/ID)
    }
}
