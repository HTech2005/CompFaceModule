package tech.HTECH;

import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;

public class FaceDetection {

    private static CascadeClassifier faceDetector;

    private static CascadeClassifier getDetector() {
        if (faceDetector == null) {
            String xmlPath = "src/main/resources/haarcascade_frontalface_default.xml";
            if (!new java.io.File(xmlPath).exists()) {
                System.err.println("ERREUR: Le fichier Haar Cascade est introuvable à : " + xmlPath);
                return null;
            }
            faceDetector = new CascadeClassifier(xmlPath);
        }
        return faceDetector;
    }

    public static Mat detectFace(String imagePath) {
        Mat image = opencv_imgcodecs.imread(imagePath);
        if (image == null || image.empty())
            return null;
        return detectFaceMat(image);
    }

    public static Mat detectFaceMat(Mat image) {
        CascadeClassifier classifier = getDetector();
        if (classifier == null)
            return null;

        // Convertir en gris
        Mat gray = new Mat();
        try {
            opencv_imgproc.cvtColor(image, gray, opencv_imgproc.COLOR_BGR2GRAY);

            // --- DEBUT MODIFICATION CLAHE (Conformité README) ---
            Mat claheApplied = new Mat();
            org.bytedeco.opencv.opencv_imgproc.CLAHE clahe = opencv_imgproc.createCLAHE(2.0, new Size(8, 8));
            clahe.apply(gray, claheApplied);
            gray = claheApplied;
            // --- FIN MODIFICATION CLAHE ---

            // Détecter les visages (50x50 suffit pour le temps réel)
            RectVector faces = new RectVector();
            classifier.detectMultiScale(gray, faces, 1.1, 4, 0, new Size(50, 50), new Size(0, 0));

            if (faces.size() == 0)
                return null;

            // Trouver le visage avec la plus grande dimension
            Rect largestFace = faces.get(0);
            long maxArea = largestFace.width() * largestFace.height();

            for (long i = 1; i < faces.size(); i++) {
                Rect r = faces.get(i);
                long area = r.width() * r.height();
                if (area > maxArea) {
                    largestFace = r;
                    maxArea = area;
                }
            }

            // Recadrage interne : Réduire de 15%
            int paddingW = (int) (largestFace.width() * 0.15);
            int paddingH = (int) (largestFace.height() * 0.15);

            Rect coreFace = new Rect(
                    largestFace.x() + paddingW,
                    largestFace.y() + paddingH,
                    Math.max(1, largestFace.width() - (2 * paddingW)),
                    Math.max(1, largestFace.height() - (2 * paddingH)));

            // Extraire le "cœur" du visage
            return new Mat(image, coreFace).clone();

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            gray.release();
        }
    }
}
