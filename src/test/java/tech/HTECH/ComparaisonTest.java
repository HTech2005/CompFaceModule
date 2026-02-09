package tech.HTECH;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ComparaisonTest {

    @Test
    public void testCosineSimilarity() {
        double[] a = {1.0, 0.0, 0.0};
        double[] b = {1.0, 0.0, 0.0};
        double s = Comparaison.similitudeCosinus(a, b);
        assertEquals(1.0, s, 1e-9);
    }

    @Test
    public void testChiSquareZero() {
        double[] a = {0.5, 0.5};
        double[] b = {0.5, 0.5};
        double d = Comparaison.distanceKhiCarre(a, b);
        assertEquals(0.0, d, 1e-9);
    }
}
