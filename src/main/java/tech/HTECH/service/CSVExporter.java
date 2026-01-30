package tech.HTECH.service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class CSVExporter {

    public static void exportBenchmark(List<BenchmarkService.BenchmarkResult> results, File destination) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(destination))) {
            // Header
            writer.println("Image_A;Image_B;Chi2_%;Eucl_%;Cos_%;Global_%;Decision;Statut_Scientifique");
            
            // Data
            for (BenchmarkService.BenchmarkResult res : results) {
                writer.println(res.toCSVRow());
            }
        }
    }
}
