package tech.HTECH;

import ij.process.ImageProcessor;

public class Histogram {

    public static double[] histo (ImageProcessor ip) {
        int[] hist = new int[256] ;
        int width = ip.getWidth();
        int height = ip.getHeight();
        int cpt = 0;

        for (int k = 0; k <= 255; k++) {

            for (int j = 0; j < height; j++) {
                for (int i = 0; i< width; i++) {

                    int intensity=ip.getPixel(i,j);

                    if(intensity==k)
                        cpt++;
                }
            }

            hist[k]=cpt;
            cpt=0;

        }

        double[] H = new double[256] ;
        for(int k = 0; k <= 255; k++)
        {
            H[k]=hist[k]/(width*height);
        }


        return H;
    }
}
