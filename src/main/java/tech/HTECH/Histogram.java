package tech.HTECH;

import ij.process.ImageProcessor;

public class Histogram {

    public static double[] histo(ImageProcessor ip) {
        int[] hist = new int[256];
        int width = ip.getWidth();
        int height = ip.getHeight();
        int cpt = 0;

        for (int k = 0; k <= 255; k++) {

            for (int j = 0; j < height; j++) {
                for (int i = 0; i < width; i++) {

                    int intensity = ip.getPixel(i, j);

                    if (intensity == k)
                        cpt++;
                }
            }

            hist[k] = cpt;
            cpt = 0;

        }

        double[] H = new double[256];
        for (int k = 0; k <= 255; k++) {
            H[k] = hist[k] / (width * height);
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
