package tech.HTECH;

import ij.process.ImageProcessor;

public class LBP {
    public static double[][] LBP2D(ImageProcessor ip)
    {
        int width=ip.getWidth();
        int height=ip.getHeight();

        double[][] TLPB= new double[height][width];

        for(int j=1;j<height-1;j++)
        {
            for(int i=1;i<=width-1;i++)
            {
                int som=0;
                int center=ip.getPixel(i,j);

                int[][] voisin ={
                        {-1,-1},{0,-1},{1,-1},
                        {1,0},{1,1},{0,1},{-1,1},
                        {-1,0}
                };

                for(int k=0;k<8;k++)
                {
                    int nx=i+voisin[k][0];
                    int ny=j+voisin[k][1];
                    int s=0;
                    int intensity=ip.getPixel(nx,ny);

                    if((intensity-center)>=0)
                        s=1;
                    else
                        s=0;

                    som=(int) (som+s*Math.pow(2,(double)k));
                }
                TLPB[i][j]=som;
            }
        }
        return TLPB;
    }

    public static double[] histogramLBP(double[][] tlbp) {
        int height = tlbp.length;
        int width = tlbp[0].length;
        double[] hist = new double[256];

        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                int val = (int) tlbp[j][i];
                hist[val]++;
            }
        }

        double total = width * height;
        for (int k = 0; k < 256; k++) {
            hist[k] /= total;
        }

        return hist;
    }

}
