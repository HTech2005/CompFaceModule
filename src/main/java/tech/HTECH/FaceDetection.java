package tech.HTECH;

import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_imgcodecs;

public class FaceDetection {
    private static int cpt = 1;

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
        if (image.empty())
            return null;

        // Convertir en gris
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(image, gray, opencv_imgproc.COLOR_BGR2GRAY);

        // Détecter les visages
        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(gray, faces);

        if (faces.size() == 0)
            return null;

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

        // Recadrage interne (Option 2) : Réduire de 15% pour ne garder que le "cœur" du
        // visage
        int paddingW = (int) (largestFace.width() * 0.15);
        int paddingH = (int) (largestFace.height() * 0.15);

        Rect coreFace = new Rect(
                largestFace.x() + paddingW,
                largestFace.y() + paddingH,
                largestFace.width() - (2 * paddingW),
                largestFace.height() - (2 * paddingH));

        // Dessiner le rectangle interne (bleu) pour vérification visuelle dans les
        // logs/images si besoin
        opencv_imgproc.rectangle(image, coreFace, new Scalar(255, 0, 0, 0), 2, 8, 0);

        // Extraire le "cœur" du visage et cloner pour persistance
        Mat face = new Mat(image, coreFace).clone();

        // Sauvegarder l'image annotée (facultatif)
        // opencv_imgcodecs.imwrite("C:/Users/HP/Desktop/TNI/CompFaceModule/resultat_face_detected"+cpt+".jpg",
        // image);
        // cpt++;
        // System.out.println("Visage détecté et entouré d'un rectangle. Image
        // sauvegardée sous 'resultat_face_detected.jpg'");

        return face;
    }
}
