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

        // 3. Score Géométrique (Euclidien)
        // La distance euclidienne max sur histogrammes normalisés est sqrt(2) ~ 1.414
        double scoreEucl = Math.max(0.0, (1.0 - (distEucl / 1.414)) * 100.0);

        // Score Global Fusionné
        // Texture (60%) + Cosinus (20%) + Euclidien (20%)
        double globalScore = (scoreChi2 * 0.6) + (scoreCos * 0.2) + (scoreEucl * 0.2);

        // Seuil de 70% comme demandé
        double seuilGlobal = 70.0;

        return globalScore >= seuilGlobal;
    }
}
