package tech.HTECH;

import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.Test;
import tech.HTECH.service.FaceService;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Map;

public class IndexationBenchmark {

    @Test
    public void leaveOneOutBenchmark() throws Exception {
        File bdd = new File("src/main/bdd");
        if (!bdd.exists() || !bdd.isDirectory()) {
            System.out.println("No src/main/bdd folder found — skipping benchmark.");
            return;
        }

        FaceService fs = new FaceService();

        File bddDir = new File("src/main/bdd");
        File[] files = bddDir.listFiles((d, name) -> name.toLowerCase().matches(".*\\.(jpg|jpeg|png)$"));
        if (files == null || files.length == 0) {
            System.out.println("No images in src/main/bdd — skipping benchmark.");
            return;
        }

        File out = new File("target/benchmark_results.csv");
        out.getParentFile().mkdirs();
        try (PrintWriter pw = new PrintWriter(new FileWriter(out))) {
            pw.println("file,found,bestMatch,score,scoreChi2,scoreCos,scoreEucl");

            // limit processed files to avoid OOM on small machines
            int maxFiles = 50;
            int total = Math.min(files.length, maxFiles);

            for (int i = 0; i < total; i++) {
                File targetFile = files[i];
                Mat targetMat = FaceDetection.detectFace(targetFile.getAbsolutePath());
                if (targetMat == null) {
                    System.err.println("Face not detected for " + targetFile.getName());
                    continue;
                }

                double[] targetFeatures = fs.extractFeatures(targetMat);
                try { if (targetMat != null && !targetMat.empty()) targetMat.release(); } catch (Exception ignored) {}

                String bestMatch = null;
                double bestScore = -1.0;
                double bestChi = 0, bestCos = 0, bestEu = 0;

                // compare against all other files on-the-fly (no DB kept in memory)
                for (File candidate : files) {
                    if (candidate.getName().equals(targetFile.getName())) continue;
                    Mat candMat = FaceDetection.detectFace(candidate.getAbsolutePath());
                    if (candMat == null) continue;
                    double[] candFeatures = fs.extractFeatures(candMat);
                    try { if (candMat != null && !candMat.empty()) candMat.release(); } catch (Exception ignored) {}

                    if (targetFeatures == null || candFeatures == null) continue;

                    double chi = Comparaison.distanceKhiCarre(targetFeatures, candFeatures);
                    double cos = Comparaison.similitudeCosinus(targetFeatures, candFeatures) * 100.0;
                    double eu = Math.max(0.0, (1.0 - (Comparaison.distanceEuclidienne(targetFeatures, candFeatures) / 0.065)) * 100.0);
                    double global = (Compatibilite.CalculCompatibilite(chi) * 0.4) + (cos * 0.4) + (eu * 0.2);

                    if (global > bestScore) {
                        bestScore = global;
                        bestMatch = candidate.getName().replaceFirst("[.][^.]+$", "");
                        bestChi = Compatibilite.CalculCompatibilite(chi);
                        bestCos = cos;
                        bestEu = eu;
                    }
                }

                boolean found = bestScore >= 61.5;
                pw.printf("%s,%b,%s,%.4f,%.4f,%.4f,%.4f\n",
                        targetFile.getName(),
                        found,
                        bestMatch != null ? bestMatch : "",
                        bestScore,
                        bestChi,
                        bestCos,
                        bestEu
                );
            }
        }

        System.out.println("Benchmark completed. Results in target/benchmark_results.csv");
    }
}
