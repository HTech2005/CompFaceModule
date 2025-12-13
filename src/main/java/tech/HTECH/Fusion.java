package tech.HTECH;

public class Fusion {
    public static double[] fus(double[] hist,double[] lbp)
    {
        double[] hist_lpb = new double[hist.length+lbp.length];
        int index=0;

        for (double val : hist) hist_lpb[index++] = val;
        for (double val : lbp)  hist_lpb[index++] = val;

        return hist_lpb;
    }
}
