package tech.HTECH;

public class Compatibilite {
    public static double CalculCompatibilite(double d)
    {
        //double dmax=1.05;
        double score=(1-d)*100;
        
        if (score < 0) score = 0;

        score = Math.round(score * 100.0) / 100.0;

        return score;
    }
}
