package tech.HTECH;

public class Decision {
    public static boolean dec(double distance, double cosineSim) {
        // Conversion de la distance en score (%) : 1.0 distance -> 0%, 0.0 distance ->
        // 100%
        double scoreEucl = (1.0 - distance) * 100.0;
        // La similitude cosinus est déjà entre 0 et 1
        double scoreCos = cosineSim * 100.0;

        // Score fusionné (moyenne pondérée)
        // On accorde plus de poids au Cosinus (70%) pour réduire les faux positifs
        double globalScore = (scoreEucl * 0.3) + (scoreCos * 0.7);

        double seuilGlobal = 80.0; // Seuil augmenté pour réduire les faux positifs

        return globalScore >= seuilGlobal;
    }
}
