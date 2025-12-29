package tech.HTECH;

import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.ByteVector;
import org.bytedeco.opencv.global.opencv_core;

public class OpenCVUtils {

    /**
     * Convertit une image Bytedeco OpenCV (Mat) en ImageProcessor (ImageJ)
     */
    public static ImageProcessor matToImageProcessor(Mat mat) {
        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();

        // Créer un tableau pour les pixels
        int[] pixels = new int[width * height];

        if (channels == 3) {
            // Image couleur (BGR)
            byte[] buffer = new byte[3];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    mat.ptr(y, x).get(buffer);
                    int b = buffer[0] & 0xFF;
                    int g = buffer[1] & 0xFF;
                    int r = buffer[2] & 0xFF;
                    pixels[y * width + x] = (r << 16) | (g << 8) | b;
                }
            }
        } else if (channels == 1) {
            // Image niveaux de gris
            byte[] buffer = new byte[1];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    mat.ptr(y, x).get(buffer);
                    int gray = buffer[0] & 0xFF;
                    pixels[y * width + x] = (gray << 16) | (gray << 8) | gray;
                }
            }
        } else {
            throw new IllegalArgumentException("Nombre de canaux non supporté : " + channels);
        }

        return new ColorProcessor(width, height, pixels);
    }

    public static String matToBase64(org.bytedeco.opencv.opencv_core.Mat mat) {
        try {
            org.bytedeco.opencv.opencv_core.ByteVector buf = new org.bytedeco.opencv.opencv_core.ByteVector();
            org.bytedeco.opencv.global.opencv_imgcodecs.imencode(".jpg", mat, buf);
            byte[] bytes = new byte[(int) buf.size()];
            buf.get(bytes);
            return java.util.Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
