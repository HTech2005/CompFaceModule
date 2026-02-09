package tech.HTECH;

import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HistogramLBPTest {

    @Test
    public void testHistogramSum() {
        ImageProcessor ip = new ByteProcessor(4, 4);
        // fill with two values
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                ip.putPixel(x, y, (x + y) % 2 == 0 ? 10 : 200);
            }
        }

        double[] hist = Histogram.histo(ip);
        double sum = 0.0;
        for (double v : hist) sum += v;
        assertEquals(1.0, sum, 1e-9, "Histogram should be normalized to sum=1");
    }

    @Test
    public void testLBPAndGridSizes() {
        ImageProcessor ip = new ByteProcessor(8, 8);
        for (int y = 0; y < 8; y++) for (int x = 0; x < 8; x++) ip.putPixel(x, y, (x + y) % 256);

        double[][] tlbp = LBP.LBP2D(ip);
        assertEquals(8, tlbp.length);
        assertEquals(8, tlbp[0].length);

        double[] hist = LBP.histogramLBP(tlbp);
        assertEquals(256, hist.length);

        double[] grid = LBP.histogramLBPGrid(tlbp, 2, 2);
        assertEquals(256 * 2 * 2, grid.length);
    }
}
