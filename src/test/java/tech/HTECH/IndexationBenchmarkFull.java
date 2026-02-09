package tech.HTECH;

import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.Test;
import tech.HTECH.service.FaceService;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Arrays;

public class IndexationBenchmarkFull {

    @Test
    public void fullLeaveOneOutBenchmark() throws Exception {
        File bdd = new File("src/main/bdd");
        if (!bdd.exists() || !bdd.isDirectory()) {
            System.out.println("No src/main/bdd folder found — skipping benchmark.");
            return;
        }

        File[] files = bdd.listFiles((dir, name) -> name.toLowerCase().matches(".*\\.(jpg|jpeg|png)$"));
        if (files == null || files.length == 0) {
            System.out.println("No images found in src/main/bdd — skipping.");
            return;
        }

        FaceService fs = new FaceService();

        File out = new File("target/benchmark_results_full.csv");
        out.getParentFile().mkdirs();
        try (PrintWriter pw = new PrintWriter(new FileWriter(out))) {
            pw.println("file,found,bestMatch,score,scoreChi2,scoreCos,scoreEucl");

            double threshold = 61.5;

            // for each image A, compute features and compare on-the-fly to all B
            for (File fa : files) {
                String nameA = fa.getName();
                try {
                    Mat faceA = FaceDetection.detectFace(fa.getAbsolutePath());
                    if (faceA == null) {
                        System.err.println("Face not detected for " + nameA);
                        pw.printf("%s,%b,%s,%.4f,%.4f,%.4f,%.4f\n", nameA, false, "", 0.0, 0.0, 0.0, 0.0);
                        continue;
                    }

                    double[] featsA = fs.extractFeatures(faceA);
                    try { if (faceA != null && !faceA.empty()) faceA.release(); } catch (Exception ignored) {}

                    String bestMatch = null;
                    double bestScore = -1.0;
                    double bestChi = 0.0, bestCos = 0.0, bestEuc = 0.0;

                    for (File fb : files) {
                        if (fb.getName().equals(nameA)) continue;
                        Mat faceB = FaceDetection.detectFace(fb.getAbsolutePath());
                        if (faceB == null) continue;
                        try {
                            double[] featsB = fs.extractFeatures(faceB);
                            try { if (faceB != null && !faceB.empty()) faceB.release(); } catch (Exception ignored) {}

                            double distChi2 = Comparaison.distanceKhiCarre(featsA, featsB);
                            double cos = Comparaison.similitudeCosinus(featsA, featsB);
                            double distEucl = Comparaison.distanceEuclidienne(featsA, featsB);

                            double scoreChi2 = Compatibilite.CalculCompatibilite(distChi2);
                            double scoreEucl = Math.max(0.0, (1.0 - (distEucl / 0.065)) * 100.0);
                            double scoreCos = cos * 100.0;
                            double globalScore = (scoreChi2 * 0.4) + (scoreCos * 0.4) + (scoreEucl * 0.2);

                            if (globalScore > bestScore) {
                                bestScore = globalScore;
                                bestMatch = fb.getName();
                                bestChi = scoreChi2; bestCos = scoreCos; bestEuc = scoreEucl;
                            }
                        } catch (Exception ex) {
                            System.err.println("Error comparing " + nameA + " with " + fb.getName() + ": " + ex.getMessage());
                        }
                    }

                    boolean found = bestMatch != null && bestScore >= threshold;
                    String bestMatchShort = bestMatch != null ? bestMatch.replaceFirst("[.][^.]+$", "") : "";
                    pw.printf("%s,%b,%s,%.4f,%.4f,%.4f,%.4f\n",
                            nameA, found, bestMatchShort, bestScore, bestChi, bestCos, bestEuc);

                } catch (Exception e) {
                    System.err.println("Error processing " + nameA + ": " + e.getMessage());
                }
            }
        }

        System.out.println("Full benchmark completed. Results in target/benchmark_results_full.csv");
    }
}
