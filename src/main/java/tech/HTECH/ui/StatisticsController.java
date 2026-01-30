package tech.HTECH.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tech.HTECH.service.HistoryService;
import tech.HTECH.service.BenchmarkService;
import tech.HTECH.service.CSVExporter;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;

public class StatisticsController {

    @FXML
    private Label lblTotalCDV, lblFP, lblFN;
    @FXML
    private ListView<String> listHistory;
    @FXML
    private ListView<List<String>> listGroups;
    @FXML
    private TextField txtImgNames;

    private HistoryService historyService = HistoryService.getInstance();

    @FXML
    public void initialize() {
        refreshUI();
        listHistory.setItems(historyService.getLogs());
        listGroups.setItems(historyService.getSimilarityGroups());

        setupGroupListCellFactory();
    }

    private void setupGroupListCellFactory() {
        listGroups.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(List<String> item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    HBox mainBox = new HBox(10);
                    mainBox.setAlignment(Pos.CENTER_LEFT);
                    mainBox.setPadding(new Insets(5));

                    HBox photoContainer = new HBox(10);
                    for (String fileName : item) {
                        VBox photoBox = new VBox(5);
                        photoBox.setAlignment(Pos.CENTER);
                        File file = new File("src/main/bdd", fileName.trim());
                        if (file.exists()) {
                            ImageView iv = new ImageView(new Image(file.toURI().toString()));
                            iv.setFitHeight(50);
                            iv.setFitWidth(50);
                            iv.setPreserveRatio(true);
                            Label lbl = new Label(fileName.trim());
                            lbl.setStyle("-fx-text-fill: #aaaaaa; -fx-font-size: 8;");
                            photoBox.getChildren().addAll(iv, lbl);
                        } else {
                            Label lblErr = new Label("?");
                            lblErr.setStyle("-fx-text-fill: red;");
                            photoBox.getChildren().add(lblErr);
                        }
                        photoContainer.getChildren().add(photoBox);
                    }

                    ScrollPane sp = new ScrollPane(photoContainer);
                    sp.setPrefHeight(80);
                    sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
                    sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
                    sp.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
                    HBox.setHgrow(sp, Priority.ALWAYS);

                    Button btnDelete = new Button("✕");
                    btnDelete.setStyle(
                            "-fx-background-color: #ff4e4e; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10;");
                    btnDelete.setOnAction(e -> {
                        historyService.getSimilarityGroups().remove(item);
                        historyService.addLog("Test: Groupe de similitude supprimé");
                    });

                    mainBox.getChildren().addAll(sp, btnDelete);
                    setGraphic(mainBox);
                }
            }
        });
    }

    private void refreshUI() {
        lblTotalCDV.setText(String.valueOf(historyService.getTotalCDV()));
        lblFP.setText(String.valueOf(historyService.getFalsePositives()));
        lblFN.setText(String.valueOf(historyService.getFalseNegatives()));
    }

    @FXML
    public void handleCreateGroup(ActionEvent event) {
        String input = txtImgNames.getText().trim();
        if (!input.isEmpty()) {
            List<String> names = Arrays.stream(input.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            if (!names.isEmpty()) {
                historyService.createSimilarityGroup(names);
                txtImgNames.clear();
            }
        }
    }

    @FXML
    public void handleAddFP(ActionEvent event) {
        historyService.addFalsePositive();
        refreshUI();
    }

    @FXML
    public void handleAddFN(ActionEvent event) {
        historyService.addFalseNegative();
        refreshUI();
    }

    @FXML
    public void handleReset(ActionEvent event) {
        historyService.resetStats();
        refreshUI();
        historyService.getSimilarityGroups().clear();
    }

    @FXML
    public void handleRunBenchmark(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir l'image cible pour l'analyse scientifique");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png", "*.jpeg"));
        File target = fileChooser.showOpenDialog(listHistory.getScene().getWindow());

        if (target != null) {
            BenchmarkService benchmarkService = new BenchmarkService();
            List<BenchmarkService.BenchmarkResult> results = benchmarkService.runAnalysis(target);

            if (results.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Aucun visage détecté ou base de données vide.");
                alert.show();
                return;
            }

            // Proposer de sauvegarder le CSV
            FileChooser saveChooser = new FileChooser();
            saveChooser.setTitle("Sauvegarder les résultats Excel (CSV)");
            saveChooser.setInitialFileName("benchmark_result_" + target.getName().split("\\.")[0] + ".csv");
            saveChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File saveFile = saveChooser.showSaveDialog(listHistory.getScene().getWindow());

            if (saveFile != null) {
                try {
                    CSVExporter.exportBenchmark(results, saveFile);
                    historyService.addLog("Test: Benchmark scientifique exporté (" + results.size() + " lignes)");
                    
                    Alert success = new Alert(Alert.AlertType.INFORMATION, "Exportation terminée avec succès !");
                    success.show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Alert err = new Alert(Alert.AlertType.ERROR, "Erreur lors de l'exportation: " + e.getMessage());
                    err.show();
                }
            }
        }
    }
}
