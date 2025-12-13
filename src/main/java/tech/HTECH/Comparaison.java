package tech.HTECH;

public class Comparaison {
    public static double distanceEuclidienne(double[] A, double[] B)
    {
        double som=0;

        for(int i=0;i<A.length;i++)
        {
            som=som+(A[i]-B[i])*(A[i]-B[i]);
        }

        return Math.sqrt(som);
    }
}
