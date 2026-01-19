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
        double scoreEucl = Math.max(0.0, (1.0 - distEucl) * 100.0);

        // Score Global Fusionné
        // Texture (60%) + Cosinus (20%) + Euclidien (20%)
        double globalScore = (scoreChi2 * 0.6) + (scoreCos * 0.2) + (scoreEucl * 0.2);

        // Seuil plus strict (75%) car la granularité 8x8 est plus précise
        double seuilGlobal = 75.0;

        return globalScore >= seuilGlobal;
    }
}
