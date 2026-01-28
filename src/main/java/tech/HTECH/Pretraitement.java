package tech.HTECH;

import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import tech.HTECH.OpenCVUtils;

public class Pretraitement {
    public static ImageProcessor pt(ImageProcessor ip) {
        // 1. Convertir en niveaux de gris et redimensionner
        ip = ip.convertToByte(true);
        ip = ip.resize(128, 128);
        
        // 1.5. Appliquer un léger flou gaussien pour réduire le bruit (Améliore la stabilité LBP)
        ip.blurGaussian(0.8);

        // 2. Appliquer CLAHE via OpenCV pour une robustesse maximale à l'éclairage
        try {
            Mat mat = OpenCVUtils.imageProcessorToMat(ip);
            Mat claheMat = new Mat();
            org.bytedeco.opencv.opencv_imgproc.CLAHE clahe = opencv_imgproc.createCLAHE(2.0, new Size(8, 8));
            clahe.apply(mat, claheMat);

            // Re-convertir en ImageProcessor
            ip = OpenCVUtils.matToImageProcessor(claheMat);

            mat.release();
            claheMat.release();
        } catch (Exception e) {
            // Fallback sur normalisation linéaire si OpenCV échoue
            linearNormalize(ip);
        }

        return ip;
    }

    private static void linearNormalize(ImageProcessor ip) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        int min = 255, max = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int val = ip.getPixel(x, y);
                if (val < min)
                    min = val;
                if (val > max)
                    max = val;
            }
        }

        if (max > min) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int val = ip.getPixel(x, y);
                    int normalized = (int) (((val - min) * 255.0) / (max - min));
                    ip.putPixel(x, y, normalized);
                }
            }
        }
    }
}
