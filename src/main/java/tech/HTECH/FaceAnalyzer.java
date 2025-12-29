package tech.HTECH;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import java.util.ArrayList;
import java.util.List;

public class FaceAnalyzer {
    private CascadeClassifier faceDetector = new CascadeClassifier(
            "src/main/resources/haarcascade_frontalface_default.xml");
    private CascadeClassifier eyesDetector = new CascadeClassifier("src/main/resources/haarcascade_eye.xml");
    private CascadeClassifier mouthDetector = new CascadeClassifier("src/main/resources/haarcascade_mcs_mouth.xml");

    public List<FaceFeature> analyzeFace(Mat image) {
        List<FaceFeature> results = new ArrayList<>();
        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(image, faces);

        for (long i = 0; i < faces.size(); i++) {
            Rect face = faces.get(i);
            Mat faceROI = new Mat(image, face);

            FaceFeature f = new FaceFeature(face.width(), face.height(), 0, 0, 0);
            f.faceRect = new int[] { face.x(), face.y(), face.width(), face.height() };

            // Detect Eyes
            RectVector eyes = new RectVector();
            eyesDetector.detectMultiScale(faceROI, eyes, 1.1, 3, 0, new Size(20, 20), new Size(0, 0));

            if (eyes.size() >= 2) {
                Rect e1 = eyes.get(0);
                Rect e2 = eyes.get(1);

                // Sort by X to identify left/right
                Rect leftEye = (e1.x() < e2.x()) ? e1 : e2;
                Rect rightEye = (e1.x() < e2.x()) ? e2 : e1;

                f.eyeLeftRect = new int[] { face.x() + leftEye.x(), face.y() + leftEye.y(), leftEye.width(),
                        leftEye.height() };
                f.eyeRightRect = new int[] { face.x() + rightEye.x(), face.y() + rightEye.y(), rightEye.width(),
                        rightEye.height() };

                // Distance calculation
                double dx = (rightEye.x() + rightEye.width() / 2.0) - (leftEye.x() + leftEye.width() / 2.0);
                double dy = (rightEye.y() + rightEye.height() / 2.0) - (leftEye.y() + leftEye.y() / 2.0);
                f.eyeDistance = (int) Math.sqrt(dx * dx + dy * dy);
            }

            // Detect Mouth (Bottom half)
            int bh = face.height() / 2;
            Mat mouthROI = new Mat(faceROI, new Rect(0, bh, face.width(), bh));
            RectVector mouths = new RectVector();
            mouthDetector.detectMultiScale(mouthROI, mouths, 1.1, 5, 0, new Size(30, 20), new Size(0, 0));

            if (mouths.size() > 0) {
                Rect m = mouths.get(0);
                f.mouthWidth = m.width();
                f.mouthHeight = m.height();
                f.mouthRect = new int[] { face.x() + m.x(), face.y() + bh + m.y(), m.width(), m.height() };
            }

            // Heuristic for Nose (since we lack cascade)
            // Typically between eyes and mouth
            int noseX = face.width() / 2 - face.width() / 10;
            int noseY = (int) (face.height() * 0.55);
            f.noseWidth = face.width() / 5;
            f.noseRect = new int[] { face.x() + noseX, face.y() + noseY, f.noseWidth, face.height() / 8 };

            results.add(f);
        }
        return results;
    }
}
