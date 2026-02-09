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

    /**
     * Normalise un vecteur en norme L2 (norme Euclidienne = 1).
     */
    public static double[] normalizeL2(double[] vector) {
        double sumSq = 0.0;
        for (double v : vector) sumSq += v * v;
        double norm = Math.sqrt(sumSq);
        double[] res = new double[vector.length];
        if (norm > 1e-12) {
            for (int i = 0; i < vector.length; i++) res[i] = vector[i] / norm;
        } else {
            System.arraycopy(vector, 0, res, 0, vector.length);
        }
        return res;
    }

    /**
     * Normalise un vecteur pour que sa norme L2 soit 1 (Euclidienne).
     * Si la norme est nulle, retourne une copie du vecteur original.
     */
    // duplicate removed — single normalizeL2 implementation kept above
}
