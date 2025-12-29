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
            normA += A[i] * A[i];
            normB += B[i] * B[i];
        }

        if (normA <= 0 || normB <= 0)
            return 0.0;

        double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));

        // Bornage pour éviter les imprécisions flottantes (ex: 1.0000000000000002)
        return Math.min(1.0, Math.max(-1.0, similarity));
    }
}
