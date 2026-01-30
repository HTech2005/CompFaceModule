package tech.HTECH.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class MainController {

    @FXML
    private StackPane contentArea;
    @FXML
    private Button btnHome, btnCDV, btnTR, btnCV, btnDashboard, btnBenchmark;

    @FXML
    public void initialize() {
        showHome();
    }

    @FXML
    private void showHome() {
        loadView("/fxml/home.fxml");
        setActiveButton(btnHome);
    }

    @FXML
    private void showCDV() {
        loadView("/fxml/comparison.fxml");
        setActiveButton(btnCDV);
    }

    @FXML
    private void showTR() {
        loadView("/fxml/recognition.fxml");
        setActiveButton(btnTR);
    }

    @FXML
    private void showCV() {
        loadView("/fxml/analysis.fxml");
        setActiveButton(btnCV);
    }

    @FXML
    private void showDashboard() {
        loadView("/fxml/dashboard.fxml");
        setActiveButton(btnDashboard);
    }

    @FXML
    private void showBenchmark() {
        loadView("/fxml/benchmark.fxml");
        setActiveButton(btnBenchmark);
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setActiveButton(Button activeBtn) {
        btnHome.setStyle("-fx-background-color: transparent; -fx-text-fill: #cccccc; -fx-alignment: CENTER_LEFT;");
        btnCDV.setStyle("-fx-background-color: transparent; -fx-text-fill: #cccccc; -fx-alignment: CENTER_LEFT;");
        btnTR.setStyle("-fx-background-color: transparent; -fx-text-fill: #cccccc; -fx-alignment: CENTER_LEFT;");
        btnCV.setStyle("-fx-background-color: transparent; -fx-text-fill: #cccccc; -fx-alignment: CENTER_LEFT;");
        btnDashboard.setStyle("-fx-background-color: transparent; -fx-text-fill: #cccccc; -fx-alignment: CENTER_LEFT;");
        if (btnBenchmark != null) {
            btnBenchmark.setStyle("-fx-background-color: transparent; -fx-text-fill: #cccccc; -fx-alignment: CENTER_LEFT;");
        }

        activeBtn.setStyle(
                "-fx-background-color: #3b3b3b; -fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-font-weight: bold;");
    }
}
