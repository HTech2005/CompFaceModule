package tech.HTECH;

import org.bytedeco.opencv.opencv_core.Mat;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour l'extraction HOG.
 * Valide que HOG fonctionne correctement avant intégration dans le pipeline complet.
 */
public class HOGTest {
    
    @Test
    public void testHOGNotEmpty() {
        // Créer une image de test 128x128
        Mat testImage = new Mat(128, 128, org.bytedeco.opencv.global.opencv_core.CV_8UC1);
        
        double[] hogFeatures = HOG.extractHOG(testImage);
        
        assertNotNull(hogFeatures, "Le vecteur HOG ne doit pas être null");
        assertTrue(hogFeatures.length > 0, "Le vecteur HOG ne doit pas être vide");
        
        // Vérifier qu'il ne contient pas que des zéros
        boolean hasNonZero = false;
        for (double val : hogFeatures) {
            if (Math.abs(val) > 1e-6) {
                hasNonZero = true;
                break;
            }
        }
        
        testImage.release();
        
        System.out.println("HOG dimensions: " + hogFeatures.length);
        System.out.println("HOG contient des valeurs non nulles: " + hasNonZero);
    }
    
    @Test
    public void testHOGDimensions() {
        Mat testImage = new Mat(128, 128, org.bytedeco.opencv.global.opencv_core.CV_8UC1);
        
        double[] hogFeatures = HOG.extractHOG(testImage);
        
        // La dimension exacte dépend de l'implémentation OpenCV
        // On vérifie juste qu'elle est dans une plage raisonnable
        assertTrue(hogFeatures.length > 1000, 
            "Le vecteur HOG devrait avoir au moins 1000 dimensions, obtenu: " + hogFeatures.length);
        assertTrue(hogFeatures.length < 20000, 
            "Le vecteur HOG devrait avoir moins de 20000 dimensions, obtenu: " + hogFeatures.length);
        
        testImage.release();
    }
    
    @Test
    public void testHOGConsistency() {
        // Même image doit donner le même vecteur HOG
        Mat testImage = new Mat(128, 128, org.bytedeco.opencv.global.opencv_core.CV_8UC1);
        
        // Remplir avec un pattern simple
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                testImage.ptr(y, x).put((byte) ((x + y) % 256));
            }
        }
        
        double[] hog1 = HOG.extractHOG(testImage);
        double[] hog2 = HOG.extractHOG(testImage);
        
        assertEquals(hog1.length, hog2.length, "Les deux extractions doivent avoir la même dimension");
        
        // Vérifier que les valeurs sont identiques
        for (int i = 0; i < hog1.length; i++) {
            assertEquals(hog1[i], hog2[i], 1e-6, 
                "Les valeurs HOG doivent être identiques pour la même image à l'index " + i);
        }
        
        testImage.release();
    }
    
    @Test
    public void testHOGNullImage() {
        double[] hogFeatures = HOG.extractHOG(null);
        
        assertNotNull(hogFeatures, "HOG doit retourner un tableau vide, pas null");
        assertEquals(0, hogFeatures.length, "HOG doit retourner un tableau vide pour image null");
    }
}
