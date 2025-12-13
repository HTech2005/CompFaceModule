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
        List<Double> scoresList = new ArrayList<>();
        int totalFramesProcessed = 0;
        double currentAverage = 0.0;

        long startTime = System.currentTimeMillis();
        long endTime = startTime + 20_000; // 20 secondes

        System.out.println("Scan démarré... Placez votre visage dans le cadre pendant 20 secondes.");

        while (System.currentTimeMillis() < endTime && canvas.isVisible()) {
            if (!cap.read(frame) || frame.empty()) continue;

            int width = frame.cols();
            int height = frame.rows();

            // === CADRE CENTRÉ + DESIGN PRO ===
            int boxSize = Math.min(width, height) * 2 / 3;
            int x = (width - boxSize) / 2;
            int y = (height - boxSize) / 2;

            Scalar guideColor = new Scalar(100, 255, 150, 0);
            Point boxTL = new Point(x, y);
            Point boxBR = new Point(x + boxSize, y + boxSize);

            opencv_imgproc.rectangle(frame, boxTL, boxBR, guideColor, 2, opencv_imgproc.LINE_AA, 0);

            int c = boxSize / 10;
            opencv_imgproc.line(frame, new Point(x, y), new Point(x + c, y), guideColor, 3, opencv_imgproc.LINE_AA, 0);
            opencv_imgproc.line(frame, new Point(x, y), new Point(x, y + c), guideColor, 3, opencv_imgproc.LINE_AA, 0);
            opencv_imgproc.line(frame, new Point(x + boxSize, y), new Point(x + boxSize - c, y), guideColor, 3, opencv_imgproc.LINE_AA, 0);
            opencv_imgproc.line(frame, new Point(x + boxSize, y), new Point(x + boxSize, y + c), guideColor, 3, opencv_imgproc.LINE_AA, 0);
            opencv_imgproc.line(frame, new Point(x, y + boxSize), new Point(x + c, y + boxSize), guideColor, 3, opencv_imgproc.LINE_AA, 0);
            opencv_imgproc.line(frame, new Point(x, y + boxSize), new Point(x, y + boxSize - c), guideColor, 3, opencv_imgproc.LINE_AA, 0);
            opencv_imgproc.line(frame, new Point(x + boxSize, y + boxSize), new Point(x + boxSize - c, y + boxSize), guideColor, 3, opencv_imgproc.LINE_AA, 0);
            opencv_imgproc.line(frame, new Point(x + boxSize, y + boxSize), new Point(x + boxSize, y + boxSize - c), guideColor, 3, opencv_imgproc.LINE_AA, 0);

            opencv_imgproc.putText(frame, "PLACEZ VOTRE VISAGE DANS LE CADRE",
                    new Point(Math.max(20, x - 100), y - 40),
                    opencv_imgproc.FONT_HERSHEY_DUPLEX, 1.3, guideColor, 3, opencv_imgproc.LINE_AA, false);

            long remaining = (endTime - System.currentTimeMillis()) / 1000;
            opencv_imgproc.putText(frame, "Temps : " + remaining + " s",
                    new Point(25, 60), opencv_imgproc.FONT_HERSHEY_SIMPLEX, 1.3,
                    new Scalar(255, 255, 100, 0), 3, opencv_imgproc.LINE_AA, false);

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

                if (cx >= x && cx <= x + boxSize && cy >= y && cy <= y + boxSize) {
                    faceInZone = true;

                    Mat faceROI = new Mat(frame, r).clone();
                    ImageProcessor ip = OpenCVUtils.matToImageProcessor(faceROI);
                    ip = Pretraitement.pt(ip);

                    double[] H = Histogram.histo(ip);
                    double[] LBPH = LBP.histogramLBP(LBP.LBP2D(ip));
                    double[] fused = Fusion.fus(H, LBPH);
                    double[] Nfused = NormalizeVector.normalize(fused);

                    double distance = Comparaison.distanceEuclidienne(Nfused, referenceVector);
                    double score = Compatibilite.CalculCompatibilite(distance);

                    // On stocke le score pour la moyenne
                    scoresList.add(score);
                    totalFramesProcessed++;

                    // Moyenne en temps réel
                    currentAverage = scoresList.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

                    boolean isMatch = score >= 75.0;

                    Scalar color = isMatch ? new Scalar(0, 255, 0, 0) : new Scalar(0, 0, 255, 0);
                    opencv_imgproc.rectangle(frame,
                            new Point(r.x(), r.y()),
                            new Point(r.x() + r.width(), r.y() + r.height()),
                            color, 4, opencv_imgproc.LINE_AA, 0);

                    String txt = String.format("%.1f%% (moy: %.1f%%)", score, currentAverage);
                    opencv_imgproc.putText(frame, txt,
                            new Point(r.x(), r.y() - 10),
                            opencv_imgproc.FONT_HERSHEY_SIMPLEX, 0.8, color, 2, opencv_imgproc.LINE_AA, false);
                }
            }

            // Affichage moyenne globale
            opencv_imgproc.putText(frame, String.format("MOYENNE : %.2f%%", currentAverage),
                    new Point(width / 2 - 180, 100),
                    opencv_imgproc.FONT_HERSHEY_DUPLEX, 1.1,
                    new Scalar(255, 200, 0, 0), 3, opencv_imgproc.LINE_AA, false);

            if (!faceInZone && faces.size() > 0) {
                opencv_imgproc.putText(frame, "Placez-vous dans le cadre vert !",
                        new Point(width - 500, height - 40),
                        opencv_imgproc.FONT_HERSHEY_DUPLEX, 1.0,
                        new Scalar(0, 150, 255, 0), 3, opencv_imgproc.LINE_AA, false);
            }

            if (faces.size() == 0) {
                opencv_imgproc.putText(frame, "Aucun visage détecté",
                        new Point(25, height - 40),
                        opencv_imgproc.FONT_HERSHEY_SIMPLEX, 1.0,
                        new Scalar(0, 0, 255, 0), 2, opencv_imgproc.LINE_AA, false);
            }

            canvas.showImage(converter.convert(frame));
        }

        cap.release();
        canvas.dispose();

        // === RÉSULTAT FINAL PAR MOYENNE ===
        double finalAverage = scoresList.isEmpty() ? 0.0 :
                scoresList.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);

        System.out.println("\n" + "═".repeat(70));
        System.out.println("           SCAN TERMINÉ - RÉSULTAT PAR MOYENNE");
        System.out.println("═".repeat(70));

        if (totalFramesProcessed == 0) {
            System.out.println("Aucun visage détecté pendant le scan.");
            System.out.println("RÉSULTAT : ÉCHEC");
        } else {
            System.out.printf("Mesures effectuées : %d\n", totalFramesProcessed);
            System.out.printf("SCORE MOYEN FINAL  : %.2f%%\n", finalAverage);

            if (finalAverage >= 78.0) {
                System.out.println("RÉSULTAT : MATCH → ACCÈS AUTORISÉ");
            } else if (finalAverage >= 65.0) {
                System.out.println("RÉSULTAT : DOUTEUX (score moyen trop bas)");
            } else {
                System.out.println("RÉSULTAT : REFUSÉ");
            }
        }
        System.out.println("═".repeat(70));
    }

}