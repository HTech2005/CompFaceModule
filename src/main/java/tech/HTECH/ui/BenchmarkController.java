package tech.HTECH.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import tech.HTECH.service.BenchmarkService;
import tech.HTECH.service.CSVExporter;
import tech.HTECH.service.HistoryService;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BenchmarkController {

    @FXML private Label lblTotal, lblFAR, lblFRR;
    @FXML private TableView<BenchmarkService.BenchmarkResult> tableResults;
    @FXML private TableColumn<BenchmarkService.BenchmarkResult, String> colImgA, colImgB, colDecision, colStatus;
    @FXML private TableColumn<BenchmarkService.BenchmarkResult, Double> colChi2, colEucl, colCos, colGlobal;
    @FXML private BarChart<String, Number> barChart;

    private final BenchmarkService benchmarkService = new BenchmarkService();
    private final ObservableList<BenchmarkService.BenchmarkResult> resultList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        colImgA.setCellValueFactory(new PropertyValueFactory<>("imageA"));
        colImgB.setCellValueFactory(new PropertyValueFactory<>("imageB"));
        colChi2.setCellValueFactory(new PropertyValueFactory<>("chi2"));
        colEucl.setCellValueFactory(new PropertyValueFactory<>("eucl"));
        colCos.setCellValueFactory(new PropertyValueFactory<>("cos"));
        colGlobal.setCellValueFactory(new PropertyValueFactory<>("global"));
        colDecision.setCellValueFactory(new PropertyValueFactory<>("decision"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        tableResults.setItems(resultList);
        
        // Custom styling for decision column
        colDecision.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("MATCH")) {
                        setStyle("-fx-text-fill: #00ff88; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ff4e4e;");
                    }
                }
            }
        });
    }

    @FXML
    public void handleRunBenchmark(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir l'image cible pour l'analyse");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png", "*.jpeg"));
        File target = fileChooser.showOpenDialog(lblTotal.getScene().getWindow());

        if (target != null) {
            List<BenchmarkService.BenchmarkResult> results = benchmarkService.runAnalysis(target);
            if (results.isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "Aucun visage détecté.").show();
                return;
            }

            resultList.setAll(results);
            updateStats(results);
            updateChart(results);
        }
    }

    @FXML
    public void handleExportCSV(ActionEvent event) {
        if (resultList.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Aucun résultat à exporter.").show();
            return;
        }

        FileChooser saveChooser = new FileChooser();
        saveChooser.setTitle("Exporter en CSV");
        saveChooser.setInitialFileName("benchmark_results.csv");
        saveChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = saveChooser.showSaveDialog(lblTotal.getScene().getWindow());

        if (file != null) {
            try {
                CSVExporter.exportBenchmark(resultList, file);
                new Alert(Alert.AlertType.INFORMATION, "Exportation réussie !").show();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Erreur d'export : " + e.getMessage()).show();
            }
        }
    }

    private void updateStats(List<BenchmarkService.BenchmarkResult> results) {
        int total = results.size();
        
        long fp = results.stream().filter(r -> r.getStatus().startsWith("FP")).count();
        long fn = results.stream().filter(r -> r.getStatus().startsWith("FN")).count();
        
        long totalImpostors = results.stream().filter(r -> r.getStatus().startsWith("VN") || r.getStatus().startsWith("FP")).count();
        long totalGenuines = results.stream().filter(r -> r.getStatus().startsWith("VP") || r.getStatus().startsWith("FN")).count();

        double far = totalImpostors > 0 ? (double) fp / totalImpostors * 100 : 0;
        double frr = totalGenuines > 0 ? (double) fn / totalGenuines * 100 : 0;

        lblTotal.setText(String.valueOf(total));
        lblFAR.setText(String.format("%.2f%%", far));
        lblFRR.setText(String.format("%.2f%%", frr));
    }

    private void updateChart(List<BenchmarkService.BenchmarkResult> results) {
        barChart.getData().clear();
        
        Map<String, Long> counts = results.stream()
                .collect(Collectors.groupingBy(r -> r.getStatus().substring(0, 2), Collectors.counting()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Statuts");

        counts.forEach((status, count) -> {
            series.getData().add(new XYChart.Data<>(status, count));
        });

        barChart.getData().add(series);
    }
}
