package tech.HTECH;

import ij.process.ImageProcessor;

public class Histogram {

    public static double[] histo(ImageProcessor ip) {
        int[] hist = new int[256];
        int width = ip.getWidth();
        int height = ip.getHeight();

        // Single-pass counting O(n*m)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int intensity = ip.getPixel(x, y);
                if (intensity < 0) intensity = 0;
                else if (intensity > 255) intensity = 255;
                hist[intensity]++;
            }
        }

        double[] H = new double[256];
        double total = (double) (width * height);
        if (total <= 0) return H;
        for (int k = 0; k < 256; k++) {
            H[k] = hist[k] / total;
        }

        return H;
    }

    public static double[] histoGrid(ImageProcessor ip, int gridX, int gridY) {
        int width = ip.getWidth();
        int height = ip.getHeight();
        int cellH = height / gridY;
        int cellW = width / gridX;

        double[] finalHist = new double[256 * gridX * gridY];
        int vectorIndex = 0;

        for (int gy = 0; gy < gridY; gy++) {
            for (int gx = 0; gx < gridX; gx++) {

                int[] cellCounts = new int[256];
                int startY = gy * cellH;
                int startX = gx * cellW;
                int endY = startY + cellH;
                int endX = startX + cellW;

                if (gy == gridY - 1)
                    endY = height;
                if (gx == gridX - 1)
                    endX = width;

                int pixelsInCell = 0;

                for (int y = startY; y < endY; y++) {
                    for (int x = startX; x < endX; x++) {
                        int intensity = ip.getPixel(x, y);
                        if (intensity < 0) intensity = 0;
                        else if (intensity > 255) intensity = 255;
                        cellCounts[intensity]++;
                        pixelsInCell++;
                    }
                }

                // Normalisation
                for (int k = 0; k < 256; k++) {
                    if (pixelsInCell > 0) {
                        finalHist[vectorIndex++] = (double) cellCounts[k] / pixelsInCell;
                    } else {
                        finalHist[vectorIndex++] = 0.0;
                    }
                }
            }
        }
        return finalHist;
    }
}
