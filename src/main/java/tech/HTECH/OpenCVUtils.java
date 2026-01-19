package tech.HTECH;

import ij.process.ByteProcessor;
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
     * Retourne un ByteProcessor pour le gris, un ColorProcessor pour la couleur.
     */
    public static ImageProcessor matToImageProcessor(Mat mat) {
        if (mat == null || mat.empty())
            return null;

        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();

        if (channels == 1) {
            byte[] data = new byte[width * height];
            mat.data().get(data);
            return new ByteProcessor(width, height, data);
        } else if (channels == 3) {
            int[] pixels = new int[width * height];
            byte[] buffer = new byte[width * height * 3];
            mat.data().get(buffer);
            for (int i = 0; i < width * height; i++) {
                int b = buffer[i * 3] & 0xFF;
                int g = buffer[i * 3 + 1] & 0xFF;
                int r = buffer[i * 3 + 2] & 0xFF;
                pixels[i] = (r << 16) | (g << 8) | b;
            }
            return new ColorProcessor(width, height, pixels);
        } else {
            throw new IllegalArgumentException("Nombre de canaux non supporté : " + channels);
        }
    }

    public static String matToBase64(Mat mat) {
        if (mat == null || mat.empty())
            return "";
        try {
            int width = mat.cols();
            int height = mat.rows();
            int channels = mat.channels();
            BufferedImage image;
            if (channels == 1) {
                image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
                byte[] data = ((java.awt.image.DataBufferByte) image.getRaster().getDataBuffer()).getData();
                mat.data().get(data);
            } else {
                image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
                byte[] data = ((java.awt.image.DataBufferByte) image.getRaster().getDataBuffer()).getData();
                mat.data().get(data);
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Convertit un Mat OpenCV en Image JavaFX
     */
    public static javafx.scene.image.Image matToImage(Mat mat) {
        if (mat == null || mat.empty())
            return null;

        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();

        javafx.scene.image.WritableImage image = new javafx.scene.image.WritableImage(width, height);
        javafx.scene.image.PixelWriter pw = image.getPixelWriter();

        int[] argb = new int[width * height];
        if (channels == 3) {
            byte[] data = new byte[width * height * 3];
            mat.data().get(data);
            for (int i = 0; i < width * height; i++) {
                int b = data[i * 3] & 0xFF;
                int g = data[i * 3 + 1] & 0xFF;
                int r = data[i * 3 + 2] & 0xFF;
                argb[i] = (255 << 24) | (r << 16) | (g << 8) | b;
            }
        } else if (channels == 1) {
            byte[] data = new byte[width * height];
            mat.data().get(data);
            for (int i = 0; i < width * height; i++) {
                int gray = data[i] & 0xFF;
                argb[i] = (255 << 24) | (gray << 16) | (gray << 8) | gray;
            }
        }

        pw.setPixels(0, 0, width, height, javafx.scene.image.PixelFormat.getIntArgbInstance(), argb, 0, width);
        return image;
    }

    /**
     * Convertit un ImageProcessor (ImageJ) en Mat Bytedeco OpenCV.
     */
    public static Mat imageProcessorToMat(ImageProcessor ip) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        Mat mat;

        if (ip instanceof ColorProcessor) {
            mat = new Mat(height, width, org.bytedeco.opencv.global.opencv_core.CV_8UC3);
            int[] pixels = (int[]) ip.getPixels();
            byte[] data = new byte[width * height * 3];
            for (int i = 0; i < width * height; i++) {
                data[i * 3] = (byte) (pixels[i] & 0xFF); // B
                data[i * 3 + 1] = (byte) ((pixels[i] >> 8) & 0xFF); // G
                data[i * 3 + 2] = (byte) ((pixels[i] >> 16) & 0xFF); // R
            }
            mat.data().put(data);
        } else {
            // Pour ByteProcessor, getPixels() retourne un byte[]
            mat = new Mat(height, width, org.bytedeco.opencv.global.opencv_core.CV_8UC1);
            byte[] data = (byte[]) ip.convertToByteProcessor().getPixels();
            mat.data().put(data);
        }
        return mat;
    }
}
