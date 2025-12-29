package tech.HTECH;

import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import org.bytedeco.opencv.opencv_core.*;
import java.util.Base64;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

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

    public static String matToBase64(Mat mat) {
        if (mat == null || mat.empty())
            return "";
        try {
            int width = mat.cols();
            int height = mat.rows();
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            byte[] buffer = new byte[3];
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    mat.ptr(y, x).get(buffer);
                    int b = buffer[0] & 0xFF;
                    int g = buffer[1] & 0xFF;
                    int r = buffer[2] & 0xFF;
                    image.setRGB(x, y, (r << 16) | (g << 8) | b);
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
