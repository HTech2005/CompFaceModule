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
        // 1. Score de Texture (Chi-Carré) : 2.0 -> 0%, 0.0 -> 100%
        double scoreChi2 = (1.0 - (distChi2 / 2.0)) * 100.0;

        // 2. Score d'Alignement (Cosinus) : 0.0 -> 0%, 1.0 -> 100%
        double scoreCos = cosineSim * 100.0;

        // 3. Score Géométrique (Euclidien) : 1.0 -> 0%, 0.0 -> 100%
        double scoreEucl = (1.0 - distEucl) * 100.0;

        // Score Global Fusionné
        // Chi-Carré (50%) + Cosinus (30%) + Euclidien (20%)
        double globalScore = (scoreChi2 * 0.5) + (scoreCos * 0.3) + (scoreEucl * 0.2);

        double seuilGlobal = 75.0;

        return globalScore >= seuilGlobal;
    }
}
