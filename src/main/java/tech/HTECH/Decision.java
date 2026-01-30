package tech.HTECH;

public class Decision {
    /**
     * Effectue une décision robuste par Triple Fusion d'expertises.
     * 
     * @param distChi2  Distance Chi-Carré (Texture fine, poids 50%)
     * @param cosineSim Similitude Cosinus (Alignement, poids 30%)
     * @param distEucl  Distance Euclidienne (Géométrie globale, poids 20%)
     * @return boolean Vrai si le score fusionné dépasse le seuil
     */
    public static boolean dec(double distChi2, double cosineSim, double distEucl) {
        // 1. Score de Texture (Chi-Carré) - Très fiable avec 8x8
        double scoreChi2 = (1.0 - (distChi2 / 2.0)) * 100.0;

        // 2. Score d'Alignement (Cosinus)
        double scoreCos = cosineSim * 100.0;

        // Avec 128 blocs (8x8 Hist + 8x8 LBP), la distance max est ajustée
        double scoreEucl = Math.max(0.0, (1.0 - (distEucl / 0.065)) * 100.0);

        // Score Global Fusionné (Rééquilibré pour les lunettes : moins de texture, plus de global)
        // Texture (40%) + Cosinus (40%) + Euclidien (20%)
        double globalScore = (scoreChi2 * 0.4) + (scoreCos * 0.4) + (scoreEucl * 0.2);

        // Seuil ajusté pour Recalibration 6.0
        double seuilGlobal = 61.5;

        return globalScore >= seuilGlobal;
    }
}
