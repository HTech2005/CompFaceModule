package tech.HTECH;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import ij.process.ImageProcessor;

import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class RealtimeComparator {

    private final double[] referenceVector;
    private final String cascadePath;

    public RealtimeComparator(double[] referenceVector, String cascadePath) {
        this.referenceVector = referenceVector;
        this.cascadePath = cascadePath;
    }

    public void scanFaceFor20Seconds() {
        Loader.load(org.bytedeco.opencv.global.opencv_core.class);

        VideoCapture cap = new VideoCapture(0);
        if (!cap.isOpened()) {
            System.err.println("Impossible d'ouvrir la caméra !");
            return;
        }

        CascadeClassifier faceDetector = new CascadeClassifier(cascadePath);
        if (faceDetector.isNull() || faceDetector.empty()) {
            System.err.println("Cascade non chargée : " + cascadePath);
            cap.release();
            return;
        }

        Mat frame = new Mat();
        Mat gray = new Mat();
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        CanvasFrame canvas = new CanvasFrame("Scan Facial - 20 secondes", 1.0);
        canvas.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Pour calculer la moyenne
        List<Double> scoresEuclidien = new ArrayList<>();
        List<Double> scoresCosinus = new ArrayList<>();

        int totalFramesProcessed = 0;

        long startTime = System.currentTimeMillis();
        long endTime = startTime + 20_000; // 20 secondes

        System.out.println("Scan démarré... Placez votre visage dans le cadre pendant 20 secondes.");

        while (System.currentTimeMillis() < endTime && canvas.isVisible()) {
            if (!cap.read(frame) || frame.empty())
                continue;

            // Miroir (optionnel mais plus naturel)
            org.bytedeco.opencv.global.opencv_core.flip(frame, frame, 1);

            int width = frame.cols();
            int height = frame.rows();

            // === CADRE CENTRÉ + DESIGN PRO ===
            int boxSize = Math.min(width, height) * 2 / 3;
            int x = (width - boxSize) / 2;
            int y = (height - boxSize) / 2;

            Scalar guideColor = new Scalar(0, 255, 0, 0); // Vert
            Point boxTL = new Point(x, y);
            Point boxBR = new Point(x + boxSize, y + boxSize);

            // Dessin du viseur (Coins seulement pour style "tech")
            int c = boxSize / 5;
            int th = 3; // épaisseur
            opencv_imgproc.line(frame, new Point(x, y), new Point(x + c, y), guideColor, th, opencv_imgproc.LINE_AA, 0);
            opencv_imgproc.line(frame, new Point(x, y), new Point(x, y + c), guideColor, th, opencv_imgproc.LINE_AA, 0);

            opencv_imgproc.line(frame, new Point(x + boxSize, y), new Point(x + boxSize - c, y), guideColor, th,
                    opencv_imgproc.LINE_AA, 0);
            opencv_imgproc.line(frame, new Point(x + boxSize, y), new Point(x + boxSize, y + c), guideColor, th,
                    opencv_imgproc.LINE_AA, 0);

            opencv_imgproc.line(frame, new Point(x, y + boxSize), new Point(x + c, y + boxSize), guideColor, th,
                    opencv_imgproc.LINE_AA, 0);
            opencv_imgproc.line(frame, new Point(x, y + boxSize), new Point(x, y + boxSize - c), guideColor, th,
                    opencv_imgproc.LINE_AA, 0);

            opencv_imgproc.line(frame, new Point(x + boxSize, y + boxSize), new Point(x + boxSize - c, y + boxSize),
                    guideColor, th, opencv_imgproc.LINE_AA, 0);
            opencv_imgproc.line(frame, new Point(x + boxSize, y + boxSize), new Point(x + boxSize, y + boxSize - c),
                    guideColor, th, opencv_imgproc.LINE_AA, 0);

            long remaining = (endTime - System.currentTimeMillis()) / 1000;
            String timeText = "TEMPS : " + remaining + " s";

            // Centrer le texte du temps en haut
            int[] baseline = new int[1];
            Size textSize = opencv_imgproc.getTextSize(timeText, opencv_imgproc.FONT_HERSHEY_SIMPLEX, 1.0, 2, baseline);
            Point textOrg = new Point((width - textSize.width()) / 2, 50);

            opencv_imgproc.putText(frame, timeText, textOrg, opencv_imgproc.FONT_HERSHEY_SIMPLEX, 1.0,
                    new Scalar(255, 255, 255, 0), 2, opencv_imgproc.LINE_AA, false);

            // Détection du visage
            opencv_imgproc.cvtColor(frame, gray, opencv_imgproc.COLOR_BGR2GRAY);
            opencv_imgproc.equalizeHist(gray, gray);

            RectVector faces = new RectVector();
            faceDetector.detectMultiScale(gray, faces);

            boolean faceInZone = false;

            for (long i = 0; i < faces.size(); i++) {
                Rect r = faces.get(i);
                int cx = r.x() + r.width() / 2;
                int cy = r.y() + r.height() / 2;

                // Vérifier si le centre du visage est bien dans le cadre
                if (cx >= x && cx <= x + boxSize && cy >= y && cy <= y + boxSize) {
                    faceInZone = true;

                    Mat faceROI = new Mat(frame, r).clone();
                    ImageProcessor ip = OpenCVUtils.matToImageProcessor(faceROI);
                    ip = Pretraitement.pt(ip);

                    double[] H = Histogram.histo(ip);
                    double[] LBPH = LBP.histogramLBP(LBP.LBP2D(ip));
                    double[] fused = Fusion.fus(H, LBPH);
                    double[] Nfused = NormalizeVector.normalize(fused);

                    double distChi2 = Comparaison.distanceKhiCarre(Nfused, referenceVector);
                    double cosSim = Comparaison.similitudeCosinus(Nfused, referenceVector);
                    double distEucl = Comparaison.distanceEuclidienne(Nfused, referenceVector);

                    double scoreTexture = Compatibilite.CalculCompatibilite(distChi2);
                    double scoreCosinus = cosSim * 100.0;
                    double scoreGlobal = ((1.0 - (distChi2 / 2.0)) * 50.0 + (cosSim * 30.0) + (1.0 - distEucl) * 20.0);

                    // Stockage
                    scoresEuclidien.add(scoreGlobal); // On stocke le score fusionné pour le résultat final
                    scoresCosinus.add(scoreCosinus);
                    totalFramesProcessed++;

                    // Affichage Temps Réel
                    boolean isMatch = Decision.dec(distChi2, cosSim, distEucl);
                    Scalar resultColor = isMatch ? new Scalar(0, 255, 0, 0) : new Scalar(0, 0, 255, 0); // Vert ou Rouge

                    // Cadre autour du visage détecté
                    opencv_imgproc.rectangle(frame, new Point(r.x(), r.y()),
                            new Point(r.x() + r.width(), r.y() + r.height()), resultColor, 2, opencv_imgproc.LINE_AA,
                            0);

                    // Infos
                    String info1 = String.format("Texture Chi2: %.1f%%", scoreTexture);
                    String info2 = String.format("Cos: %.1f%%", scoreCosinus);
                    String info3 = String.format("Global: %.1f%%", scoreGlobal);

                    opencv_imgproc.putText(frame, info1, new Point(r.x(), r.y() - 45),
                            opencv_imgproc.FONT_HERSHEY_PLAIN, 1.2, resultColor, 2, opencv_imgproc.LINE_AA, false);
                    opencv_imgproc.putText(frame, info2, new Point(r.x(), r.y() - 25),
                            opencv_imgproc.FONT_HERSHEY_PLAIN,
                            1.2, resultColor, 2, opencv_imgproc.LINE_AA, false);
                    opencv_imgproc.putText(frame, info3, new Point(r.x(), r.y() - 5), opencv_imgproc.FONT_HERSHEY_PLAIN,
                            1.2, resultColor, 2, opencv_imgproc.LINE_AA, false);
                }
            }

            if (!faceInZone) {
                String msg = "PLACEZ VOTRE VISAGE AU CENTRE";
                Size s = opencv_imgproc.getTextSize(msg, opencv_imgproc.FONT_HERSHEY_SIMPLEX, 0.8, 2, baseline);
                opencv_imgproc.putText(frame, msg, new Point((width - s.width()) / 2, height - 50),
                        opencv_imgproc.FONT_HERSHEY_SIMPLEX, 0.8, new Scalar(0, 255, 255, 0), 2, opencv_imgproc.LINE_AA,
                        false);
            }

            canvas.showImage(converter.convert(frame));
        }

        cap.release();
        canvas.dispose();

        // === RÉSULTAT FINAL ===
        double avgEuc = scoresEuclidien.stream().mapToDouble(d -> d).average().orElse(0.0);
        double avgCos = scoresCosinus.stream().mapToDouble(d -> d).average().orElse(0.0);

        System.out.println("\n" + "═".repeat(60));
        System.out.println("           RÉSULTAT DU SCAN");
        System.out.println("═".repeat(60));

        if (totalFramesProcessed == 0) {
            System.out.println("ERREUR : Aucun visage analysé.");
        } else {
            System.out.printf("Images analysées : %d\n", totalFramesProcessed);
            System.out.println("------------------------------------------------------------");
            System.out.printf("SCORE MOYEN EUCLIDIEN : %.2f%%\n", avgEuc);
            System.out.printf("SCORE MOYEN COSINUS   : %.2f%%\n", avgCos);
            System.out.println("------------------------------------------------------------");

            // Décision basée sur le Cosinus (plus robuste)
            if (avgCos >= 95.0) {
                System.out.println(">>> ACCÈS AUTORISÉ (Identité confirmée)");
            } else if (avgCos >= 85.0) {
                System.out.println(">>> INCERTAIN (Ressemblance partielle)");
            } else {
                System.out.println(">>> ACCÈS REFUSÉ (Visage inconnu)");
            }
        }
        System.out.println("═".repeat(60));
    }
}
