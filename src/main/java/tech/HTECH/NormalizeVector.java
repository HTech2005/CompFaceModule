package tech.HTECH;

public class NormalizeVector {

    // Normalise un vecteur (double[]) pour que sa norme soit 1
    // Normalise un vecteur (double[]) pour que sa somme soit 1 (L1)
    // Essentiel pour la distance Chi-Carré
    public static double[] normalize(double[] vector) {
        double sum = 0;

        // Calcul de la somme
        for (double v : vector) {
            sum += Math.abs(v);
        }

        // Création du vecteur normalisé
        double[] res = new double[vector.length];
        if (sum > 0) {
            for (int i = 0; i < vector.length; i++) {
                res[i] = vector[i] / sum;
            }
        } else {
            // Éviter division par zéro (vecteur nul reste nul)
            return vector.clone();
        }

        return res;
    }
}
