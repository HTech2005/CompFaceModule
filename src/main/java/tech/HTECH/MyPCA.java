package tech.HTECH;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.PCA;

import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.*;

public class MyPCA {

    private PCA pca;
    private int maxComponents;
    private Mat mean;
    private Mat vectors;

    // Constante pour DATA_AS_ROW (souvent 0 dans OpenCV)
    // On l'utilise en dur pour éviter les erreurs "cannot find symbol" sur certaines versions
    private static final int PCA_DATA_AS_ROW = 0;

    public MyPCA(int maxComponents) {
        this.maxComponents = maxComponents;
        this.pca = new PCA();
    }

    public void train(List<double[]> data) {
        if (data == null || data.isEmpty()) return;

        int numSamples = data.size();
        int dimension = data.get(0).length;

        // Convertir List<double[]> en Mat (Rows = Samples, Cols = Features)
        Mat dataMat = new Mat(numSamples, dimension, CV_64FC1);
        
        // Optimization: Create a unified array
        double[] flatData = new double[numSamples * dimension];
        for (int i = 0; i < numSamples; i++) {
            System.arraycopy(data.get(i), 0, flatData, i * dimension, dimension);
        }
        
        // Use DoublePointer to manipulate data
        new DoublePointer(dataMat.data()).put(flatData);

        // Compute PCA
        System.out.println("Entraînement PCA sur " + numSamples + " vecteurs de dimension " + dimension + "...");
        
        pca = new PCA(dataMat, new Mat(), PCA_DATA_AS_ROW, maxComponents);
        
        this.mean = pca.mean().clone();
        this.vectors = pca.eigenvectors().clone();
        
        System.out.println("PCA terminés. Composantes conservées : " + vectors.rows());
        
        dataMat.release();
    }

    public double[] project(double[] feature) {
        if (this.mean == null || this.vectors == null) return feature; // Pas de PCA entraînée

        Mat vec = new Mat(1, feature.length, CV_64FC1);
        new DoublePointer(vec.data()).put(feature);

        Mat result = pca.project(vec);
        
        double[] projection = new double[result.cols()];
        new DoublePointer(result.data()).get(projection);
        
        vec.release();
        result.release();
        
        return projection;
    }
    
    public boolean isTrained() {
        return this.mean != null && !this.mean.empty();
    }
}
