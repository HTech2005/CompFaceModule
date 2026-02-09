package tech.HTECH;

import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.Test;
import tech.HTECH.service.FaceService;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class IndexationBenchmarkOnDisk {

    @Test
    public void benchmarkToDiskThenCompare() throws Exception {
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

        File featuresDir = new File("target/features");
        featuresDir.mkdirs();

        FaceService fs = new FaceService();

        // Phase 1: extract features and write to disk
        System.out.println("Phase 1: extraction vers disque (target/features)");
        for (File f : files) {
            String name = f.getName();
            try {
                Mat face = FaceDetection.detectFace(f.getAbsolutePath());
                if (face == null) {
                    System.err.println("Face not detected for " + name);
                    continue;
                }
                double[] feats = fs.extractFeatures(face);
                try { if (face != null && !face.empty()) face.release(); } catch (Exception ignored) {}

                // write features
                File out = new File(featuresDir, name + ".bin");
                try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(out)))) {
                    dos.writeInt(feats.length);
                    for (double v : feats) dos.writeDouble(v);
                }
            } catch (Exception e) {
                System.err.println("Error extracting " + name + ": " + e.getMessage());
            }
        }

        // Phase 2: compare in streaming mode
        System.out.println("Phase 2: comparaison en streaming, génération de target/benchmark_results_full.csv");
        File outCsv = new File("target/benchmark_results_full.csv");
        try (PrintWriter pw = new PrintWriter(new FileWriter(outCsv))) {
            pw.println("file,found,bestMatch,score,scoreChi2,scoreCos,scoreEucl");
            double threshold = 61.5;

            File[] bins = featuresDir.listFiles((d, name) -> name.endsWith(".bin"));
            if (bins == null) bins = new File[0];

            for (File aBin : bins) {
                String nameA = aBin.getName().replaceFirst("\\.bin$", "");
                double[] featsA = null;
                try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(aBin)))) {
                    int len = dis.readInt();
                    featsA = new double[len];
                    for (int i = 0; i < len; i++) featsA[i] = dis.readDouble();
                }

                String bestMatch = null; double bestScore=-1, bestChi=0, bestCos=0, bestEuc=0;
                for (File bBin : bins) {
                    String nameB = bBin.getName().replaceFirst("\\.bin$", "");
                    if (nameA.equals(nameB)) continue;
                    double[] featsB = null;
                    try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(bBin)))) {
                        int len = dis.readInt();
                        featsB = new double[len];
                        for (int i = 0; i < len; i++) featsB[i] = dis.readDouble();
                    }

                    double distChi2 = Comparaison.distanceKhiCarre(featsA, featsB);
                    double cos = Comparaison.similitudeCosinus(featsA, featsB);
                    double distEucl = Comparaison.distanceEuclidienne(featsA, featsB);

                    double scoreChi2 = Compatibilite.CalculCompatibilite(distChi2);
                    double scoreEucl = Math.max(0.0, (1.0 - (distEucl / 0.065)) * 100.0);
                    double scoreCos = cos * 100.0;
                    double globalScore = (scoreChi2 * 0.4) + (scoreCos * 0.4) + (scoreEucl * 0.2);

                    if (globalScore > bestScore) {
                        bestScore = globalScore; bestMatch = nameB; bestChi = scoreChi2; bestCos = scoreCos; bestEuc = scoreEucl;
                    }
                }

                boolean found = bestMatch != null && bestScore >= threshold;
                pw.printf("%s,%b,%s,%.4f,%.4f,%.4f,%.4f\n",
                        nameA, found, bestMatch!=null?bestMatch.replaceFirst("[.][^.]+$", "") : "", bestScore, bestChi, bestCos, bestEuc);
            }
        }

        System.out.println("On-disk benchmark completed: target/benchmark_results_full.csv");
    }
}
