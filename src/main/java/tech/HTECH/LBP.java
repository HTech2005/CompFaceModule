package tech.HTECH;

import ij.process.ImageProcessor;

public class LBP {
    public static double[][] LBP2D(ImageProcessor ip) {
        int width = ip.getWidth();
        int height = ip.getHeight();

        double[][] TLPB = new double[height][width];

        for (int j = 1; j < height - 1; j++) {
            for (int i = 1; i <= width - 1; i++) {
                int som = 0;
                int center = ip.getPixel(i, j);

                int[][] voisin = {
                        { -1, -1 }, { 0, -1 }, { 1, -1 },
                        { 1, 0 }, { 1, 1 }, { 0, 1 }, { -1, 1 },
                        { -1, 0 }
                };

                for (int k = 0; k < 8; k++) {
                    int nx = i + voisin[k][0];
                    int ny = j + voisin[k][1];
                    int s = 0;
                    int intensity = ip.getPixel(nx, ny);

                    if ((intensity - center) >= 0)
                        s = 1;
                    else
                        s = 0;

                    som = (int) (som + s * Math.pow(2, (double) k));
                }
                TLPB[j][i] = som;
            }
        }
        return TLPB;
    }

    public static double[] histogramLBP(double[][] tlbp) {
        double[] hist = new double[256];
        int height = tlbp.length;
        int width = tlbp[0].length;

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int val = (int) tlbp[j][i];
                hist[val]++;
            }
        }

        // Normalisation
        double sum = 0;
        for (double v : hist)
            sum += v;
        if (sum > 0) {
            for (int i = 0; i < 256; i++)
                hist[i] /= sum;
        }
        return hist;
    }

    /**
     * Calcule un histogramme LBP spatial (Blocking)
     * Divise l'image en gridX * gridY blocs et concatène les histogrammes.
     */
    public static double[] spatialHistogramLBP(double[][] tlbp, int gridX, int gridY) {
        int height = tlbp.length;
        int width = tlbp[0].length;
        int blockH = height / gridY;
        int blockW = width / gridX;

        double[] spatialHist = new double[gridX * gridY * 256];
        int offset = 0;

        for (int gy = 0; gy < gridY; gy++) {
            for (int gx = 0; gx < gridX; gx++) {
                double[] cellHist = new double[256];
                int yStart = gy * blockH;
                int xStart = gx * blockW;

                for (int j = yStart; j < yStart + blockH && j < height; j++) {
                    for (int i = xStart; i < xStart + blockW && i < width; i++) {
                        int val = (int) tlbp[j][i];
                        cellHist[val]++;
                    }
                }

                // Normalisation locale du bloc
                double sum = 0;
                for (double v : cellHist)
                    sum += v;
                if (sum > 0) {
                    for (int k = 0; k < 256; k++) {
                        spatialHist[offset + k] = cellHist[k] / sum;
                    }
                }
                offset += 256;
            }
        }

        return spatialHist;
    }
}
