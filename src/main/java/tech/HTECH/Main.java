package tech.HTECH;

import ij.process.ImageProcessor;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
//import org.opencv.imgcodecs.Imgcodecs;


import java.awt.Desktop;
import java.io.File;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {

        //Charger automatiquement les bibliothèques natives OpenCV
        //Loader.load(opencv_core.class);


        System.out.println("OPTIONS");
        while(true)
        {
            Loader.load(opencv_core.class);

            Scanner sc = new Scanner(System.in);
            System.out.println("1-Comparer deux visages\n2-Comparer un visage aux visages de la bdd\n3-Temps Réel avec un visage de votre choix\n4-Caractéristiques d'un visage\nChoix:");
            int choix  = sc.nextInt();

            switch(choix)
            {
                case 1:
                    // --- Image 1 ---
                    Loader.load(opencv_core.class);

                    Scanner s = new Scanner(System.in);
                    System.out.print("Entrez chemin de la première image:");
                    String imagePath1 = s.nextLine();


                    //String imagePath1 = "C:/Users/HP/Music/Downloads/img_align_celeba/000003.jpg";
                    Mat face1 = FaceDetection.detectFace(imagePath1);
                    if (face1 != null) {
                        System.out.println("Face de l'image 1 détectée.");
                        ImageProcessor ip1 = OpenCVUtils.matToImageProcessor(face1);
                        saveAndOpenImage(face1, "face_detected1.jpg");
                    } else {
                        System.out.println("Aucune face détectée dans l'image 1.");
                        return;
                    }

                    // --- Image 2 ---
                    System.out.print("Entrez chemin de la seconde image:");

                    String imagePath2 = s.nextLine();
                    //String imagePath2 = "C:/Users/HP/Music/Downloads/img_align_celeba/018692.jpg";
                    Mat face2 = FaceDetection.detectFace(imagePath2);
                    if (face2 != null) {
                        System.out.println("Face de l'image 2 détectée.");
                        ImageProcessor ip2 = OpenCVUtils.matToImageProcessor(face2);
                        saveAndOpenImage(face2, "face_detected2.jpg");
                    } else {
                        System.out.println("Aucune face détectée dans l'image 2.");
                        return;
                    }

                    // --- Prétraitement + Histogrammes ---
                    ImageProcessor ip1 = Pretraitement.pt(OpenCVUtils.matToImageProcessor(face1));
                    ImageProcessor ip2 = Pretraitement.pt(OpenCVUtils.matToImageProcessor(face2));

                    double[] AH = Histogram.histo(ip1);
                    double[] BH = Histogram.histo(ip2);

                    double[] ALBP = LBP.histogramLBP(LBP.LBP2D(ip1));
                    double[] BLPB = LBP.histogramLBP(LBP.LBP2D(ip2));

                    double[] fusionA = Fusion.fus(AH, ALBP);
                    double[] fusionB = Fusion.fus(BH, BLPB);

                    double[] NfusionA = NormalizeVector.normalize(fusionA);
                    double[] NfusionB = NormalizeVector.normalize(fusionB);

                    // --- Décision ---
                    double distance = Comparaison.distanceEuclidienne(NfusionA, NfusionB);
                    if (Decision.dec(distance))
                        System.out.println("Décision : Même personne");
                    else
                        System.out.println("Décision : Personnes différentes");

                    System.out.println("Score de compatibilité : "
                            + Compatibilite.CalculCompatibilite(distance) + " %");

                    break;
                case 2:
                    Scanner s2 = new Scanner(System.in);
                    System.out.print("Entrez chemin de l'image:");
                    String imaPath = s2.nextLine();

                    String dossier = "src/main/bdd";

                    File[] fichiers = new File(dossier).listFiles((dir, name) ->
                            name.toLowerCase().matches(".*\\.(jpg|jpeg|png)$")
                    );

                    if (fichiers == null || fichiers.length == 0) {
                        System.out.println("Aucune image trouvée dans le dossier.");
                        return;
                    }

                    double meilleurScore = -1; // Plus élevé = meilleure correspondance
                    File meilleureImage = null;

                    for (File f : fichiers) {
                        Mat imgTest = opencv_imgcodecs.imread(f.getAbsolutePath(), opencv_imgcodecs.IMREAD_GRAYSCALE);

                        Mat fa = FaceDetection.detectFace(imaPath);
                        if (fa != null) {
                            System.out.println("Face de l'image détectée.");
                            ImageProcessor ip = OpenCVUtils.matToImageProcessor(fa);
                            saveAndOpenImage(fa, "face_2.jpg");
                        } else {
                            System.out.println("Aucune face détectée dans l'image 2.");
                            return;
                        }

                        // --- Prétraitement + Histogrammes ---
                        ImageProcessor IP = Pretraitement.pt(OpenCVUtils.matToImageProcessor(fa));
                        ImageProcessor IP2 = Pretraitement.pt(OpenCVUtils.matToImageProcessor(imgTest));

                        double[] AH2 = Histogram.histo(IP);
                        double[] BH2 = Histogram.histo(IP2);

                        double[] ALBP2 = LBP.histogramLBP(LBP.LBP2D(IP));
                        double[] BLPB2 = LBP.histogramLBP(LBP.LBP2D(IP2));

                        double[] fusionA2 = Fusion.fus(AH2, ALBP2);
                        double[] fusionB2 = Fusion.fus(BH2, BLPB2);

                        double[] NfusionA2 = NormalizeVector.normalize(fusionA2);
                        double[] NfusionB2 = NormalizeVector.normalize(fusionB2);

                        // --- Décision ---
                        double distance2 = Comparaison.distanceEuclidienne(NfusionA2, NfusionB2);


                        double score = Compatibilite.CalculCompatibilite(distance2);

                        System.out.println(f.getName() + " -> Similarité : " + score);

                        if (score > meilleurScore) {
                            meilleurScore = score;
                            meilleureImage = f;
                        }
                    }

                    if (meilleureImage != null) {
                        System.out.println("\nMeilleure correspondance : " + meilleureImage.getAbsolutePath());
                        System.out.println("Score de similarité : " + meilleurScore);

                        double finalAverage = meilleurScore;

                        // Décision selon le score
                        if (finalAverage >= 78.0) {
                            System.out.println("RÉSULTAT : MATCH → ACCÈS AUTORISÉ");
                        } else if (finalAverage >= 50.0) {
                            System.out.println("RÉSULTAT : DOUTEUX (score moyen trop bas)");
                        } else {
                            System.out.println("RÉSULTAT : REFUSÉ");
                        }
                    }

                    break;
                case 3:
                    Scanner s3 = new Scanner(System.in);
                    System.out.print("Entrez chemin de l'image:");
                    String imagePath = s3.nextLine();

                    //String imagePath1 = "C:/Users/HP/Music/Downloads/img_align_celeba/000404.jpg";
                    Mat face = FaceDetection.detectFace(imagePath);
                    if (face != null) {
                        System.out.println("Face de l'image détectée.");
                        ImageProcessor ip = OpenCVUtils.matToImageProcessor(face);
                        saveAndOpenImage(face, "face_detected3.jpg");
                    } else {
                        System.out.println("Aucune face détectée dans l'image 1.");
                        return;
                    }

                    ImageProcessor ip = Pretraitement.pt(OpenCVUtils.matToImageProcessor(face));

                    double[] IH = Histogram.histo(ip);


                    double[] ILBP = LBP.histogramLBP(LBP.LBP2D(ip));

                    double[] fusionI = Fusion.fus(IH, ILBP);

                    double[] NfusionI = NormalizeVector.normalize(fusionI);

                    String xmlPath = "src/main/resources/haarcascade_frontalface_alt2.xml";
                    if (!new File(xmlPath).exists()) {
                        System.err.println("ERREUR: Le fichier Haar Cascade est introuvable à : " + xmlPath);
                        break;
                    }

                    RealtimeComparator rc = new RealtimeComparator(
                            NfusionI,
                            xmlPath
                    );


                    rc.scanFaceFor20Seconds();

                    break;
                case 4:
                    Scanner s4 = new Scanner(System.in);
                    System.out.print("Entrez chemin de l'image:");
                    String imagPath = s4.nextLine();

                    //String imagePath1 = "C:/Users/HP/Music/Downloads/img_align_celeba/000003.jpg";
                    Mat fac = FaceDetection.detectFace(imagPath);
                    if (fac != null) {
                        System.out.println("Face de l'image détectée.");
                        ImageProcessor img = OpenCVUtils.matToImageProcessor(fac);
                        saveAndOpenImage(fac, "face_detected4.jpg");
                    } else {
                        System.out.println("Aucune face détectée dans l'image.");
                        return;
                    }
                    Mat image = opencv_imgcodecs.imread(imagPath);
                    FaceAnalyzer analyzer = new FaceAnalyzer();
                    List<FaceFeature> results = analyzer.analyzeFace(image);

                    System.out.println(results);

                    break;
            }
        }










    }

    private static void saveAndOpenImage(Mat mat, String fileName) {
        try {
            String desktopPath = System.getProperty("user.home") + "/Desktop/TNI/CompFaceModule/" + fileName;
            File outputFile = new File(desktopPath);
            opencv_imgcodecs.imwrite(outputFile.getAbsolutePath(), mat);
            System.out.println("Visage sauvegardé : " + outputFile.getAbsolutePath());

           /* if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(outputFile);
            }*/
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
