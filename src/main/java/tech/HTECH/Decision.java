package tech.HTECH;

public class Decision {
    public static boolean dec(double distance, double cosineSim) {
        // Conversion de la distance Chi-Carré en score (%) : 2.0 -> 0%, 0.0 -> 100%
        double scoreTexture = (1.0 - (distance / 2.0)) * 100.0;
        // La similitude cosinus est déjà entre 0 et 1
        double scoreCos = cosineSim * 100.0;

        // Score fusionné (moyenne pondérée)
        // On accorde plus de poids au Score de Texture (Chi-Carré) comme demandé par
        // l'utilisateur
        double globalScore = (scoreTexture * 0.6) + (scoreCos * 0.4);

        double seuilGlobal = 75.0; // Seuil recommandé après fusion

        return globalScore >= seuilGlobal;
    }
}
