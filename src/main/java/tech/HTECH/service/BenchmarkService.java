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

    public List<BenchmarkResult> runFullAnalysis() {
        List<BenchmarkResult> results = new ArrayList<>();
        File bddDir = new File("src/main/bdd");
        File[] files = bddDir.listFiles((dir, name) -> name.toLowerCase().matches(".*\\.(jpg|jpeg|png)$"));

        if (files == null || files.length == 0) return results;

        // Pré-chargement des visages pour éviter de re-détecter N*N fois
        Map<String, Mat> faceMap = new java.util.HashMap<>();
        for (File file : files) {
            Mat face = FaceDetection.detectFace(file.getAbsolutePath());
            if (face != null) {
                faceMap.put(file.getName(), face);
            }
        }

        List<String> filenames = new ArrayList<>(faceMap.keySet());
        for (int i = 0; i < filenames.size(); i++) {
            for (int j = 0; j < filenames.size(); j++) {
                // On peut décider de ne pas comparer une image avec elle-même, 
                // mais pour un benchmark scientifique complet (vérifier le score de 100%), on le laisse souvent.
                String nameA = filenames.get(i);
                String nameB = filenames.get(j);
                
                Mat faceA = faceMap.get(nameA);
                Mat faceB = faceMap.get(nameB);

                FaceService.ComparisonResult res = faceService.compareFaces(faceA, faceB);

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
            }
        }
        return results;
    }

    private boolean isTheoreticallySame(String name1, String name2) {
        if (name1.equalsIgnoreCase(name2)) return true;

        // 1. Vérifier les groupes de similitude définis par l'utilisateur
        if (HistoryService.getInstance().inSameGroup(name1, name2)) return true;
        
        // 2. Nettoyage des noms (enlever extensions, chiffres et suffixes courants)
        String clean1 = cleanName(name1);
        String clean2 = cleanName(name2);
        
        // Si l'un est contenu dans l'autre (ex: "Ashley_face" et "Ashley")
        return clean1.contains(clean2) || clean2.contains(clean1);
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
