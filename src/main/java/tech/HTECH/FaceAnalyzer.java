package tech.HTECH; import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import java.util.ArrayList; import java.util.List;
public class FaceAnalyzer {

    private static final int MIN_FACE_WIDTH = 100;  // largeur minimale en pixels
    private static final int MIN_FACE_HEIGHT = 100; // hauteur minimale en pixels

    private CascadeClassifier faceDetector = new CascadeClassifier("src/main/resources/haarcascade_frontalface_default.xml");
    private CascadeClassifier eyesDetector = new CascadeClassifier("src/main/resources/haarcascade_eye.xml");
    private CascadeClassifier mouthDetector = new CascadeClassifier("src/main/resources/haarcascade_mcs_mouth.xml");

    public List<FaceFeature> analyzeFace(Mat image) {

        List<FaceFeature> results = new ArrayList<>();

        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(image, faces);

        for (long i = 0; i < faces.size(); i++) {
            Rect face = faces.get(i);

            // Filtrer les petits visages
            if (face.width() < MIN_FACE_WIDTH || face.height() < MIN_FACE_HEIGHT) {
                continue; // ignorer ce rectangle
            }

            // Dessiner rectangle du visage
            opencv_imgproc.rectangle(image, face, new Scalar(0, 255, 0, 0), 2, 8, 0);

            Mat faceROI = new Mat(image, face);

            // Détection des yeux
            RectVector eyes = new RectVector();
            eyesDetector.detectMultiScale(faceROI, eyes);

            int eyeDistance = 0;
            if (eyes.size() >= 2) {
                Rect eye1 = eyes.get(0);
                Rect eye2 = eyes.get(1);
                eyeDistance = (int) distance(eye1, eye2);
                opencv_imgproc.rectangle(faceROI, eye1, new Scalar(255, 0, 0, 0), 2, 8, 0);
                opencv_imgproc.rectangle(faceROI, eye2, new Scalar(255, 0, 0, 0), 2, 8, 0);
            }

            // Détection de la bouche
            RectVector mouths = new RectVector();
            mouthDetector.detectMultiScale(faceROI, mouths);

            int mouthWidth = 0;
            int mouthHeight = 0;
            if (mouths.size() > 0) {
                Rect mouth = mouths.get(0);
                mouthWidth = mouth.width();
                mouthHeight = mouth.height();
                opencv_imgproc.rectangle(faceROI, mouth, new Scalar(0, 255, 255, 0), 2, 8, 0);
            }

            // Ajouter les caractéristiques dans la liste
            results.add(new FaceFeature(face.width(), face.height(), eyeDistance, mouthWidth, mouthHeight));
        }

        // Sauvegarde de l'image annotée
        opencv_imgcodecs.imwrite("C:/Users/HP/Desktop/TNI/CompFaceModule/resultat_analyse.jpg", image);
        System.out.println("Image analysée sauvegardée sous 'resultat_analyse.jpg'");

        return results;
    }

    private double distance(Rect r1, Rect r2) {
        double x1 = r1.x() + r1.width() / 2.0;
        double y1 = r1.y() + r1.height() / 2.0;

        double x2 = r2.x() + r2.width() / 2.0;
        double y2 = r2.y() + r2.height() / 2.0;

        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }
}
