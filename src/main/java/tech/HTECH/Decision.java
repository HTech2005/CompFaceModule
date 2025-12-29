package tech.HTECH;

public class Decision {
    public static boolean dec(double distance, double cosineSim) {
        // Conversion de la distance en score (%) : 1.0 distance -> 0%, 0.0 distance ->
        // 100%
        double scoreEucl = (1.0 - distance) * 100.0;
        // La similitude cosinus est déjà entre 0 et 1
        double scoreCos = cosineSim * 100.0;

        // Score fusionné (moyenne pondérée)
        // On accorde plus de poids au Score Euclidien comme demandé par l'utilisateur
        double globalScore = (scoreEucl * 0.6) + (scoreCos * 0.4);

        double seuilGlobal = 75.0; // Seuil recommandé après fusion

        return globalScore >= seuilGlobal;
    }
}
