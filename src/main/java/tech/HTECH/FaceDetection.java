package tech.HTECH;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_imgcodecs;

public class FaceDetection {
    private static int cpt=1;
    public static Mat detectFace(String imagePath) {
        // Charger le classifieur Haar cascade frontal face
        String xmlPath = "src/main/resources/haarcascade_frontalface_default.xml";
        if (!new java.io.File(xmlPath).exists()) {
             System.err.println("ERREUR: Le fichier Haar Cascade est introuvable à : " + xmlPath);
             return null;
        }
        CascadeClassifier faceDetector = new CascadeClassifier(xmlPath);

        // Lire l'image
        Mat image = opencv_imgcodecs.imread(imagePath);
        if (image.empty()) return null;

        // Convertir en gris
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(image, gray, opencv_imgproc.COLOR_BGR2GRAY);

        // Détecter les visages
        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(gray, faces);

        if (faces.size() == 0) return null;

        // Trouver le visage avec la plus grande dimension (largeur * hauteur)
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

        // Dessiner un rectangle autour du visage le plus grand
        opencv_imgproc.rectangle(image, largestFace, new Scalar(0, 255, 0, 0), 2, 8, 0);

        // Extraire le visage
        Mat face = new Mat(image, largestFace);

        // Sauvegarder l'image annotée (facultatif)
        opencv_imgcodecs.imwrite("C:/Users/HP/Desktop/TNI/CompFaceModule/resultat_face_detected"+cpt+".jpg", image);
        cpt++;
        System.out.println("Visage détecté et entouré d'un rectangle. Image sauvegardée sous 'resultat_face_detected.jpg'");

        return face;
    }
}
