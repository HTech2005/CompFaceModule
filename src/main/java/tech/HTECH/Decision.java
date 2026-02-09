package tech.HTECH;

public class Decision {

    // Public configuration constants so UI and other modules use the same values
    // Candidate configuration from grid search
    public static final double THRESHOLD = 61.5; // Aligné sur la documentation technique
    
    // Diviseur restauré pour LBP+Histogramme
    // Valeur optimale: 0.065
    public static final double EUCLIDEAN_DIVISOR = 0.065; 
    
    public static double W_CHI = 0.4;
    public static double W_COS = 0.4;
    public static double W_EUCL = 0.2;

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
        double scoreChi2 = Math.max(0.0, (1.0 - (distChi2 / 2.0)) * 100.0);
        double scoreCos = Math.max(0.0, Math.min(100.0, cosineSim * 100.0));
        double scoreEucl = Math.max(0.0, (1.0 - (distEucl / EUCLIDEAN_DIVISOR)) * 100.0);
        switch (mode) {
            case CHI_SQUARE:
                return scoreChi2 >= THRESHOLD;
            case COSINE:
                return scoreCos >= THRESHOLD;
            case EUCLIDEAN:
                return scoreEucl >= THRESHOLD;
            case TRIPLE_FUSION:
            default:
                double globalScore = (scoreChi2 * W_CHI) + (scoreCos * W_COS) + (scoreEucl * W_EUCL);
                return globalScore >= THRESHOLD;
        }
    }
}
