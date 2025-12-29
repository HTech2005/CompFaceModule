package tech.HTECH;

public class Compatibilite {
    public static double CalculCompatibilite(double d) {
        // Pour Chi-Carré sur histogrammes normalisés, la distance max est 2.0
        double score = (1.0 - (d / 2.0)) * 100.0;

        if (score < 0)
            score = 0;

        score = Math.round(score * 100.0) / 100.0;

        return score;
    }
}
