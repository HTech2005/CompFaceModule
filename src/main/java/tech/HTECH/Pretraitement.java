package tech.HTECH;

import ij.process.ImageProcessor;
import ij.process.ByteProcessor;

public class Pretraitement {
    public static ImageProcessor pt(ImageProcessor ip) {
        // Convertir en ByteProcessor (niveaux de gris)
        ip = ip.convertToByte(true);

        // Redimensionner à 128x128
        ip = ip.resize(128, 128);

        // Égalisation d'histogramme
        ip = ip.duplicate();

        ip.resetMinAndMax(); // met à jour les valeurs min et max

        ip = ip.convertToByte(true); // convertit encore pour être sûr

        ip = ip.duplicate(); // copie finale
        // ImageJ 1.54e n'a pas de equalizeHistogram() directement
        // Pour égaliser, on utilise le plugin d'ImageJ :
        // ip.equalize() peut être utilisé si tu importes ij.plugin.filter.ContrastEnhancer
        try {
            ij.plugin.ContrastEnhancer ce = new ij.plugin.ContrastEnhancer();
            ce.equalize(ip);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ip;
    }
}
