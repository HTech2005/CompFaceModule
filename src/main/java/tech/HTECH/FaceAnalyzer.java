package tech.HTECH;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import java.util.ArrayList;
import java.util.List;

public class FaceAnalyzer {

    private static final int MIN_FACE_WIDTH = 100; // largeur minimale en pixels
    private static final int MIN_FACE_HEIGHT = 100; // hauteur minimale en pixels

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

            // Filtrer les petits visages
            if (face.width() < MIN_FACE_WIDTH || face.height() < MIN_FACE_HEIGHT) {
                continue; // ignorer ce rectangle
            }

            // Dessiner rectangle du visage
            opencv_imgproc.rectangle(image, face, new Scalar(0, 255, 0, 0), 2, 8, 0);

            Mat faceROI = new Mat(image, face);

            // Détection des yeux
            // Détection des yeux (Sensibilité accrue)
            RectVector eyes = new RectVector();
            // scaleFactor=1.05 (plus lent mais précis), minNeighbors=3 (tolérant),
            // minSize=20x20
            eyesDetector.detectMultiScale(faceROI, eyes, 1.05, 3, 0, new Size(20, 20), new Size(0, 0));

            int eyeDistance = 0;
            if (eyes.size() >= 2) {
                Rect eye1 = eyes.get(0);
                Rect eye2 = eyes.get(1);
                eyeDistance = (int) distance(eye1, eye2);
                opencv_imgproc.rectangle(faceROI, eye1, new Scalar(255, 0, 0, 0), 2, 8, 0);
                opencv_imgproc.rectangle(faceROI, eye2, new Scalar(255, 0, 0, 0), 2, 8, 0);
            }

            // Détection de la bouche (Zone inférieure du visage uniquement)
            RectVector mouths = new RectVector();

            // On cherche la bouche seulement dans la moitié inférieure du visage pour
            // éviter les fausses détections (nez, yeux)
            int halfHeight = face.height() / 2;
            Mat mouthROI = new Mat(faceROI, new Rect(0, halfHeight, face.width(), halfHeight));

            // scaleFactor=1.1, minNeighbors=3, minSize=30x20
            mouthDetector.detectMultiScale(mouthROI, mouths, 1.1, 3, 0, new Size(30, 20), new Size(0, 0));

            int mouthWidth = 0;
            int mouthHeight = 0;
            if (mouths.size() > 0) {
                Rect mouth = mouths.get(0);

                // Ajuster coordonnées relatives à faceROI
                mouth.y(mouth.y() + halfHeight);

                mouthWidth = mouth.width();
                mouthHeight = mouth.height();
                opencv_imgproc.rectangle(faceROI, mouth, new Scalar(0, 255, 255, 0), 2, 8, 0);
            }

            // Ajouter les caractéristiques dans la liste
            results.add(new FaceFeature(face.width(), face.height(), eyeDistance, mouthWidth, mouthHeight));
        }

        // Sauvegarde de l'image annotée
        // Sauvegarde de l'image annotée
        String desktopPath = System.getProperty("user.home") + "/Desktop/TNI/CompFaceModule/resultat_analyse.jpg";
        opencv_imgcodecs.imwrite(desktopPath, image);
        System.out.println("Image analysée sauvegardée sous : " + desktopPath);

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
