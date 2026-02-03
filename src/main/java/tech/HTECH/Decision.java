package tech.HTECH;

public class Decision {

    public enum DecisionMode {
        CHI_SQUARE("Chi-Carré seul"),
        EUCLIDEAN("Distance Euclidienne seule"),
        COSINE("Similitude Cosinus seule"),
        TRIPLE_FUSION("Triple Fusion d'expertises");

        private final String label;
        DecisionMode(String label) { this.label = label; }
        public String getLabel() { return label; }
        @Override
        public String toString() { return label; }
    }

    /**
     * Effectue une décision selon le mode choisi.
     */
    public static boolean dec(double distChi2, double cosineSim, double distEucl) {
        return dec(distChi2, cosineSim, distEucl, DecisionMode.TRIPLE_FUSION);
    }

    public static boolean dec(double distChi2, double cosineSim, double distEucl, DecisionMode mode) {
        double scoreChi2 = (1.0 - (distChi2 / 2.0)) * 100.0;
        double scoreCos = cosineSim * 100.0;
        double scoreEucl = Math.max(0.0, (1.0 - (distEucl / 0.065)) * 100.0);

        switch (mode) {
            case CHI_SQUARE:
                return scoreChi2 >= 61.5; // Seuil aligné sur le global par défaut
            case COSINE:
                return scoreCos >= 61.5;
            case EUCLIDEAN:
                return scoreEucl >= 61.5;
            case TRIPLE_FUSION:
            default:
                // Texture (30%) + Cosinus (60%) + Euclidien (10%)
                double globalScore = (scoreCos * 0.6) + (scoreChi2 * 0.3) + (scoreEucl * 0.1);
                return globalScore >= 61.5;
        }
    }
}
