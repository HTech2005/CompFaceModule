package tech.HTECH;

import org.bytedeco.opencv.opencv_core.Mat;

public class HOG {
    public static double[] extractHOG(Mat image) {
        // HOG Désactivé temporairement pour cause d'incompatibilité de compilation
        // Retourne un vecteur vide pour ne pas casser le pipeline PCA
        return new double[100]; 
    }
}
