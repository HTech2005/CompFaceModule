package tech.HTECH;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import ij.process.ImageProcessor;

public class RealtimeComparator {

    private final double[] referenceVector;
    private final String cascadePath;

    public RealtimeComparator(double[] referenceVector, String cascadePath) {
        this.referenceVector = referenceVector;
        this.cascadePath = cascadePath;
    }

    public void scanFaceFor20Seconds() {
        Loader.load(org.bytedeco.opencv.global.opencv_core.class);

        org.bytedeco.opencv.opencv_videoio.VideoCapture cap = new org.bytedeco.opencv.opencv_videoio.VideoCapture(0);
        if (!cap.isOpened()) {
            System.err.println("Impossible d'ouvrir la camera !");
            return;
        }

        try {
            cap.set(org.bytedeco.opencv.global.opencv_videoio.CAP_PROP_BRIGHTNESS, 255);
            cap.set(org.bytedeco.opencv.global.opencv_videoio.CAP_PROP_GAIN, 255);
        } catch (Exception e) {
            System.out.println("Avertissement: Impossible de regler la luminosite.");
        }

        Mat frame = new Mat();
        Mat gray = new Mat();
        OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
        
        List<Double> scoresGlobaux = new ArrayList<>();
        List<Double> scoresEuclidiensBruts = new ArrayList<>();
        List<Double> scoresCosinus = new ArrayList<>();
        int totalFramesProcessed = 0;

        try (CascadeClassifier faceDetector = new CascadeClassifier(cascadePath)) {
            if (faceDetector.isNull() || faceDetector.empty()) {
                System.err.println("Cascade non chargee : " + cascadePath);
                cap.release();
                return;
            }

            CanvasFrame canvas = new CanvasFrame("Scan Facial - 20 secondes", 1.0);
            canvas.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            try {
                long stableStartTime = 0;
                System.out.println("Scan demarre...");

                while (canvas.isVisible()) {
                    if (!cap.read(frame) || frame.empty())
                        continue;

                    org.bytedeco.opencv.global.opencv_core.flip(frame, frame, 1);

                    int width = frame.cols();
                    int height = frame.rows();

                    int boxSize = Math.min(width, height) * 2 / 3;
                    int x = (width - boxSize) / 2;
                    int y = (height - boxSize) / 2;

                    Scalar guideColor = new Scalar(0, 255, 0, 0);
                    opencv_imgproc.rectangle(frame, new Point(x, y), new Point(x + boxSize, y + boxSize), guideColor, 2, opencv_imgproc.LINE_AA, 0);

                    opencv_imgproc.cvtColor(frame, gray, opencv_imgproc.COLOR_BGR2GRAY);
                    opencv_imgproc.equalizeHist(gray, gray);

                    try (RectVector faces = new RectVector()) {
                        faceDetector.detectMultiScale(gray, faces);
                        boolean faceInZone = false;

                        for (long i = 0; i < faces.size(); i++) {
                            try (Rect r = faces.get(i)) {
                                int cx = r.x() + r.width() / 2;
                                int cy = r.y() + r.height() / 2;

                                if (cx >= x && cx <= x + boxSize && cy >= y && cy <= y + boxSize) {
                                    faceInZone = true;
                                    try (Mat faceROI = new Mat(frame, r).clone()) {
                                        ImageProcessor ip = OpenCVUtils.matToImageProcessor(faceROI);
                                        ip = Pretraitement.pt(ip);

                                        double[] H = Histogram.histoGrid(ip, 8, 8);
                                        double[] LBPH = LBP.histogramLBPGrid(LBP.LBP2D(ip), 8, 8);
                                        double[] Nfused = NormalizeVector.normalize(Fusion.fus(H, LBPH));

                                        double distChi2 = Comparaison.distanceKhiCarre(Nfused, referenceVector);
                                        double cosSim = Comparaison.similitudeCosinus(Nfused, referenceVector);
                                        double distEucl = Comparaison.distanceEuclidienne(Nfused, referenceVector);

                                        double scoreTexture = Compatibilite.CalculCompatibilite(distChi2);
                                        double scoreCosinus = cosSim * 100.0;
                                        double scoreEucl = Math.max(0.0, (1.0 - (distEucl / Decision.EUCLIDEAN_DIVISOR)) * 100.0);
                                        double scoreGlobal = (scoreTexture * 0.4) + (scoreCosinus * 0.4) + (scoreEucl * 0.2);

                                        if (scoreGlobal >= 50.0) {
                                            if (stableStartTime == 0) stableStartTime = System.currentTimeMillis();
                                            if (System.currentTimeMillis() - stableStartTime >= 5000) {
                                                System.out.println(">>> IDENTITE CONFIRMEE");
                                                canvas.setVisible(false);
                                            }
                                        } else {
                                            stableStartTime = 0;
                                        }

                                        scoresGlobaux.add(scoreGlobal);
                                        scoresEuclidiensBruts.add(scoreEucl);
                                        scoresCosinus.add(scoreCosinus);
                                        totalFramesProcessed++;
                                    }
                                }
                            }
                        }
                    }
                    canvas.showImage(converter.convert(frame));
                }
            } finally {
                canvas.dispose();
            }
        }

        cap.release();
        frame.release();
        gray.release();

        System.out.println("Scan termine. " + totalFramesProcessed + " images traitees.");
    }
}
