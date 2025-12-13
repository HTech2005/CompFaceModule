package tech.HTECH;

public class NormalizeVector {

    // Normalise un vecteur (double[]) pour que sa norme soit 1
    public static double[] normalize(double[] vector) {
        double norm = 0;

        // Calcul de la norme
        for (double v : vector) {
            norm += v * v;
        }
        norm = Math.sqrt(norm);

        // Création du vecteur normalisé
        double[] res = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            res[i] = vector[i] / norm;
        }

        return res;
    }
}

