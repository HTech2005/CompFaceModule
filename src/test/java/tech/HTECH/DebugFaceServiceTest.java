package tech.HTECH;

import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.Test;
import tech.HTECH.service.FaceService;
import java.io.File;

public class DebugFaceServiceTest {

    @Test
    public void testFaceServiceDirect() {
        System.out.println(">>> DÉBUT TEST DEBUG FACESERVICE <<<");
        
        FaceService fs = new FaceService();
        
        // Simuler le rechargement de la BDD pour entraîner la PCA
        // On a besoin de quelques images pour entraîner la PCA
        fs.reloadDatabase(); 
        
        File dir = new File("src/main/bdd");
        File[] files = dir.listFiles((d, n) -> n.toLowerCase().startsWith("personne07"));
        
        if (files == null || files.length < 2) {
            System.out.println("Pas assez d'images Personne07 pour le test");
            return;
        }
        
        File f1 = new File("src/main/bdd/Personne01_01.jpg");
        File f2 = new File("src/main/bdd/Personne01_02.jpg");
        
        System.out.println("Comparaison: " + f1.getName() + " vs " + f2.getName());
        
        Mat m1 = FaceDetection.detectFace(f1.getAbsolutePath());
        Mat m2 = FaceDetection.detectFace(f2.getAbsolutePath());
        
        if (m1 == null || m2 == null) {
            System.out.println("Visage non détecté");
            return;
        }
        
        FaceService.ComparisonResult res = fs.compareFaces(m1, m2);
        
        System.out.println("=== RÉSULTATS ===");
        System.out.println("Match: " + res.isMatch());
        System.out.println("Global Score: " + res.getScoreGlobal());
        System.out.println("Chi2 Score: " + res.getScoreChi2());
        System.out.println("Eucl Score: " + res.getScoreEuclidien());
        System.out.println("Cos Score: " + res.getScoreCosinus());
        System.out.println("Actif Score: " + res.getActiveScore());
        
        m1.release();
        m2.release();
    }
}
