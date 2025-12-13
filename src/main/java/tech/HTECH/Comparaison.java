package tech.HTECH;

public class Comparaison {
    public static double distanceEuclidienne(double[] A, double[] B) {
        double som = 0;

        for (int i = 0; i < A.length; i++) {
            som = som + (A[i] - B[i]) * (A[i] - B[i]);
        }

        return Math.sqrt(som);
    }

    public static double similitudeCosinus(double[] A, double[] B) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < A.length; i++) {
            dotProduct += A[i] * B[i];
            normA += Math.pow(A[i], 2);
            normB += Math.pow(B[i], 2);
        }

        if (normA == 0 || normB == 0)
            return 0.0;

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
