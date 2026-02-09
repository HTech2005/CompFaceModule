package tech.HTECH;

import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.Test;
import tech.HTECH.service.FaceService;
import tech.HTECH.Decision;

import java.io.*;
import java.util.Arrays;
import java.util.Comparator;

public class SequentialBenchmark {

    @Test
    public void fullSequentialBenchmark() throws Exception {
        File bdd = new File("src/main/bdd");
        if (!bdd.exists() || !bdd.isDirectory()) {
            System.out.println("No src/main/bdd folder found — skipping benchmark.");
            return;
        }

        File featDir = new File("target/features");
        if (!featDir.exists()) featDir.mkdirs();

        FaceService fs = new FaceService();

        // STEP 1: extract features to disk (one file per image)
        File[] files = bdd.listFiles((d, n) -> n.toLowerCase().matches(".*\\.(jpg|jpeg|png)$"));
        if (files == null || files.length == 0) {
            System.out.println("No images found in src/main/bdd — skipping.");
            return;
        }

        // sort for deterministic order
        Arrays.sort(files, Comparator.comparing(File::getName));

        for (File f : files) {
            String name = f.getName();
            Mat face = null;
            try {
                face = FaceDetection.detectFace(f.getAbsolutePath());
                if (face == null) {
                    System.err.println("Face not detected for " + name);
                    continue;
                }

                double[] features = fs.extractFeatures(face);
                if (features == null || features.length == 0) {
                    System.err.println("No features for " + name);
                    continue;
                }

                File out = new File(featDir, name + ".feat");
                try (PrintWriter pw = new PrintWriter(new FileWriter(out))) {
                    for (int i = 0; i < features.length; i++) {
                        if (i > 0) pw.print(',');
                        pw.print(features[i]);
                    }
                }
                System.out.println("Feature saved: " + out.getName());

            } catch (Exception ex) {
                System.err.println("Error processing " + name + ": " + ex.getMessage());
            } finally {
                try {
                    if (face != null && !face.empty()) face.release();
                } catch (Exception ignored) {
                }
            }
        }

        // STEP 2: streaming comparisons (read one feature file at a time)
        File outCsv = new File("target/full_benchmark_results.csv");
        try (PrintWriter pw = new PrintWriter(new FileWriter(outCsv))) {
            pw.println("file,found,bestMatch,score,scoreChi2,scoreCos,scoreEucl");

            File[] feats = featDir.listFiles((d, n) -> n.endsWith(".feat"));
            if (feats == null || feats.length == 0) {
                System.out.println("No feature files found — aborting comparisons.");
                return;
            }
            Arrays.sort(feats, Comparator.comparing(File::getName));

            for (File qf : feats) {
                String qname = qf.getName().replaceFirst("\\.feat$", "");
                double[] qfeat = readFeat(qf);
                if (qfeat == null || qfeat.length == 0) {
                    System.err.println("Empty features for " + qname);
                    continue;
                }

                String bestMatch = null;
                double bestScore = -Double.MAX_VALUE;
                double bestChi = 0, bestCos = 0, bestEu = 0;

                for (File cf : feats) {
                    if (cf.getName().equals(qf.getName())) continue;
                    double[] cfeat = readFeat(cf);
                    if (cfeat == null || cfeat.length == 0) continue;

                    double chi = Comparaison.distanceKhiCarre(qfeat, cfeat);
                    double cos = Comparaison.similitudeCosinus(qfeat, cfeat);
                    double eu = Comparaison.distanceEuclidienne(qfeat, cfeat);

                    double scoreChi = Compatibilite.CalculCompatibilite(chi);
                    double scoreEu = Math.max(0.0, (1.0 - (eu / 0.065)) * 100.0);
                    double scoreCos = cos * 100.0;
                    double global = (scoreChi * Decision.W_CHI) + (scoreCos * Decision.W_COS) + (scoreEu * Decision.W_EUCL);

                    if (global > bestScore) {
                        bestScore = global;
                        bestMatch = cf.getName().replaceFirst("\\.feat$", "");
                        bestChi = scoreChi;
                        bestCos = scoreCos;
                        bestEu = scoreEu;
                    }
                }

                boolean found = false;
                if (bestMatch != null) {
                    String qid = qname.split("_")[0];
                    String bid = bestMatch.split("_")[0];
                    found = qid.equals(bid);
                }

                pw.printf("%s,%b,%s,%.4f,%.4f,%.4f,%.4f\n",
                        qname,
                        found,
                        bestMatch != null ? bestMatch : "",
                        bestScore,
                        bestChi,
                        bestCos,
                        bestEu
                );
                pw.flush();
                System.out.println("Compared: " + qname + " -> " + bestMatch + " (" + String.format("%.2f", bestScore) + "% )");
            }
        }

        System.out.println("Full sequential benchmark done: target/full_benchmark_results.csv");
    }

    private double[] readFeat(File f) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line = br.readLine();
            if (line == null || line.isEmpty()) return new double[0];
            String[] parts = line.split(",");
            double[] res = new double[parts.length];
            for (int i = 0; i < parts.length; i++) res[i] = Double.parseDouble(parts[i]);
            return res;
        }
    }
}
