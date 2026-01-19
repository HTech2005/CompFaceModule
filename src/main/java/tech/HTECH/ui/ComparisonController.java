package tech.HTECH.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.bytedeco.opencv.opencv_core.Mat;
import tech.HTECH.FaceDetection;
import tech.HTECH.OpenCVUtils;
import tech.HTECH.service.FaceService;
import tech.HTECH.service.HistoryService;

import java.io.File;

public class ComparisonController {

    @FXML
    private ImageView imgView1, imgView2, imgDetected1, imgDetected2;
    @FXML
    private Label lblPlaceholder1, lblPlaceholder2;
    @FXML
    private Label lblVerdict, lblChi2, lblEucl, lblCos, lblGlobal;
    @FXML
    private VBox resultPane;
    @FXML
    private Button btnCompare;

    private File file1, file2;
    private FaceService faceService = new FaceService();

    @FXML
    private void handleUpload1() {
        file1 = chooseFile();
        if (file1 != null) {
            imgView1.setImage(new javafx.scene.image.Image(file1.toURI().toString()));
            lblPlaceholder1.setVisible(false);
        }
    }

    @FXML
    private void handleUpload2() {
        file2 = chooseFile();
        if (file2 != null) {
            imgView2.setImage(new javafx.scene.image.Image(file2.toURI().toString()));
            lblPlaceholder2.setVisible(false);
        }
    }

    private File chooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image de visage");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png", "*.jpeg"));
        return fileChooser.showOpenDialog(imgView1.getScene().getWindow());
    }

    @FXML
    private void handleCompare() {
        if (file1 == null || file2 == null) {
            return;
        }

        try {
            Mat face1 = FaceDetection.detectFace(file1.getAbsolutePath());
            Mat face2 = FaceDetection.detectFace(file2.getAbsolutePath());

            if (face1 == null || face2 == null) {
                lblVerdict.setText("ERREUR: Visage non détecté");
                lblVerdict.setStyle("-fx-text-fill: #ff4e4e; -fx-font-size: 24; -fx-font-weight: bold;");
                resultPane.setVisible(true);
                return;
            }

            FaceService.ComparisonResult result = faceService.compareFaces(face1, face2);

            HistoryService.getInstance().incrementCDV();
            HistoryService.getInstance().addLog("Comparaison CDV: " + file1.getName() + " vs " + file2.getName() + " ("
                    + (result.isMatch() ? "OK" : "ÉCHEC") + ")");
            HistoryService.getInstance().checkAutomatedError(file1.getName(), file2.getName(), result.isMatch());

            // Afficher les visages détectés
            imgDetected1.setImage(OpenCVUtils.matToImage(face1));
            imgDetected2.setImage(OpenCVUtils.matToImage(face2));

            lblVerdict.setText(result.isMatch() ? "COMPATIBLE" : "NON COMPATIBLE");
            lblVerdict.setStyle(result.isMatch() ? "-fx-text-fill: #00ff88; -fx-font-size: 24; -fx-font-weight: bold;"
                    : "-fx-text-fill: #ff4e4e; -fx-font-size: 24; -fx-font-weight: bold;");

            lblChi2.setText(String.format("%.1f %%", result.getScoreChi2()));
            lblEucl.setText(String.format("%.1f %%", result.getScoreEuclidien()));
            lblCos.setText(String.format("%.1f %%", result.getScoreCosinus()));
            lblGlobal.setText(String.format("%.1f %%", result.getScoreGlobal()));

            resultPane.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            lblVerdict.setText("ERREUR DE CALCUL");
            lblVerdict.setStyle("-fx-text-fill: #ff0000; -fx-font-size: 24; -fx-font-weight: bold;");
            resultPane.setVisible(true);
        }
    }
}
