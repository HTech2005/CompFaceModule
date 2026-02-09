package tech.HTECH;

import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.Test;
import tech.HTECH.service.FaceService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class FullIndexationBenchmark {

    // Phase 1: extract features and write per-file features to target/features/<name>.feat
    // Phase 2: for each file, load its feature and compare against all other .feat files streaming

    @Test
    public void runFullBenchmark() throws Exception {
        File bdd = new File("src/main/bdd");
        if (!bdd.exists() || !bdd.isDirectory()) {
            System.out.println("No src/main/bdd folder found — skipping benchmark.");
            return;
        }

        File featuresDir = new File("target/features");
        if (!featuresDir.exists()) featuresDir.mkdirs();

        FaceService fs = new FaceService();

        // Phase 1: extract features
        File[] imgs = bdd.listFiles((d, name) -> name.toLowerCase().matches(".*\\.(jpg|jpeg|png)$"));
        if (imgs == null || imgs.length == 0) {
            System.out.println("No image files found in src/main/bdd — skipping.");
            return;
        }

        System.out.println("Phase 1: extraction des features vers target/features (stream)");
        for (File f : imgs) {
            try {
                Mat face = FaceDetection.detectFace(f.getAbsolutePath());
                if (face == null) {
                    System.err.println("Visage non détecté dans: " + f.getName());
                    continue;
                }
                double[] features = fs.extractFeatures(face);
                // save features as CSV
                File out = new File(featuresDir, f.getName() + ".feat");
                try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8))) {
                    for (int i = 0; i < features.length; i++) {
                        if (i > 0) bw.write(',');
                        bw.write(Double.toString(features[i]));
                    }
                }
                try { if (face != null && !face.empty()) face.release(); } catch (Exception ignored) {}
                System.out.println("Extracted: " + f.getName());
            } catch (Throwable t) {
                System.err.println("Erreur extraction pour " + f.getName() + " : " + t.getMessage());
            }
        }

        // Phase 2: streaming comparison
        System.out.println("Phase 2: comparaison streaming et génération de target/benchmark_results_full.csv");
        File[] feats = featuresDir.listFiles((d, name) -> name.toLowerCase().endsWith(".feat"));
        if (feats == null || feats.length == 0) {
            System.out.println("Aucun fichier .feat trouvé — rien à comparer.");
            return;
        }

        File results = new File("target/benchmark_results_full.csv");
        results.getParentFile().mkdirs();
        try (PrintWriter pw = new PrintWriter(new FileWriter(results))) {
            pw.println("file,found,bestMatch,score,scoreChi2,scoreCos,scoreEucl");

            // For each feature file A
            for (File fa : feats) {
                double[] featA = readFeatureFile(fa);
                if (featA == null) continue;

                String bestMatch = null;
                double bestScore = -1.0;
                double bestChi = 0, bestCos = 0, bestEu = 0;

                // compare against others by streaming each file B
                for (File fb : feats) {
                    if (fb.getName().equals(fa.getName())) continue;
                    double[] featB = readFeatureFile(fb);
                    if (featB == null) continue;

                    double chi = Comparaison.distanceKhiCarre(featA, featB);
                    double cos = Comparaison.similitudeCosinus(featA, featB);
                    double eu = Comparaison.distanceEuclidienne(featA, featB);

                    double scoreChi = Compatibilite.CalculCompatibilite(chi);
                    double scoreEu = Math.max(0.0, (1.0 - (eu / 0.065)) * 100.0);
                    double scoreCos = cos * 100.0;
                    double globalScore = (scoreChi * 0.4) + (scoreCos * 0.4) + (scoreEu * 0.2);

                    if (globalScore > bestScore) {
                        bestScore = globalScore;
                        bestMatch = fb.getName();
                        bestChi = scoreChi; bestCos = scoreCos; bestEu = scoreEu;
                    }
                }

                boolean found = bestMatch != null && bestScore >= 61.5;
                pw.printf("%s,%b,%s,%.4f,%.4f,%.4f,%.4f\n",
                        fa.getName(), found, bestMatch == null ? "" : bestMatch, bestScore, bestChi, bestCos, bestEu);

                System.out.println("Compared: " + fa.getName() + " -> " + (bestMatch == null ? "(none)" : bestMatch) + " (" + String.format("%.2f", bestScore) + "%)");
            }
        }

        System.out.println("Full benchmark finished. Results: target/benchmark_results_full.csv");
    }

    private double[] readFeatureFile(File f) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8))) {
            String line = br.readLine();
            if (line == null || line.isEmpty()) return null;
            String[] toks = line.split(",");
            double[] res = new double[toks.length];
            for (int i = 0; i < toks.length; i++) res[i] = Double.parseDouble(toks[i]);
            return res;
        } catch (Exception e) {
            System.err.println("Erreur lecture feature " + f.getName() + " : " + e.getMessage());
            return null;
        }
    }
}
