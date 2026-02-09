package tech.HTECH;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NormalizeVectorTest {

    @Test
    public void testNormalizeL1() {
        double[] v = {1.0, 2.0, 3.0};
        double[] n = NormalizeVector.normalize(v);
        double sum = 0.0;
        for (double x : n) sum += Math.abs(x);
        assertEquals(1.0, sum, 1e-9);
    }

    @Test
    public void testNormalizeL2() {
        double[] v = {3.0, 4.0};
        double[] n = NormalizeVector.normalizeL2(v);
        double norm = 0.0;
        for (double x : n) norm += x * x;
        assertEquals(1.0, norm, 1e-9);
    }
}
