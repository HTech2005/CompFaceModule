package tech.HTECH.ui;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
import tech.HTECH.FaceAnalyzer;
import tech.HTECH.FaceFeature;

import java.io.File;
import java.util.List;

public class AnalysisController {

    @FXML
    private ImageView imgPreview;
    @FXML
    private Canvas overlayCanvas;
    @FXML
    private Label lblFileName, lblResDim, lblResEye, lblResNose, lblResMouth;
    @FXML
    private Button btnAnalyze;
    @FXML
    private GridPane resultsGrid;

    private File imageFile;
    private FaceAnalyzer analyzer = new FaceAnalyzer();

    @FXML
    private void handleUpload() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png", "*.jpeg"));
        imageFile = fileChooser.showOpenDialog(imgPreview.getScene().getWindow());

        if (imageFile != null) {
            lblFileName.setText(imageFile.getName());
            imgPreview.setImage(new javafx.scene.image.Image(imageFile.toURI().toString()));
            btnAnalyze.setDisable(false);
            resultsGrid.setVisible(false);
            clearCanvas();
        }
    }

    @FXML
    private void handleAnalyze() {
        if (imageFile == null)
            return;

        Mat image = opencv_imgcodecs.imread(imageFile.getAbsolutePath());
        if (image.empty())
            return;

        List<FaceFeature> featuresList = analyzer.analyzeFace(image);
        if (featuresList.isEmpty()) {
            return;
        }

        FaceFeature f = featuresList.get(0);

        lblResDim.setText(f.faceWidth + "x" + f.faceHeight);
        lblResEye.setText(f.eyeDistance + "px");
        lblResNose.setText(f.noseWidth + "px");
        lblResMouth.setText(f.mouthWidth + "px");

        drawResults(f, image.cols(), image.rows());
        resultsGrid.setVisible(true);
    }

    private void clearCanvas() {
        GraphicsContext gc = overlayCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, overlayCanvas.getWidth(), overlayCanvas.getHeight());
    }

    private void drawResults(FaceFeature f, int origW, int origH) {
        clearCanvas();

        double viewW = imgPreview.getBoundsInParent().getWidth();
        double viewH = imgPreview.getBoundsInParent().getHeight();

        // Match canvas size to image view display size
        overlayCanvas.setWidth(viewW);
        overlayCanvas.setHeight(viewH);

        double ratioX = viewW / origW;
        double ratioY = viewH / origH;

        GraphicsContext gc = overlayCanvas.getGraphicsContext2D();
        gc.setLineWidth(2.0);

        drawBox(gc, f.faceRect, Color.BLUE, ratioX, ratioY);
        drawBox(gc, f.eyeLeftRect, Color.RED, ratioX, ratioY);
        drawBox(gc, f.eyeRightRect, Color.RED, ratioX, ratioY);
        drawBox(gc, f.noseRect, Color.LIMEGREEN, ratioX, ratioY);
        drawBox(gc, f.mouthRect, Color.GOLD, ratioX, ratioY);
    }

    private void drawBox(GraphicsContext gc, int[] rect, Color color, double rx, double ry) {
        if (rect == null)
            return;
        gc.setStroke(color);
        gc.strokeRect(rect[0] * rx, rect[1] * ry, rect[2] * rx, rect[3] * ry);
    }
}
