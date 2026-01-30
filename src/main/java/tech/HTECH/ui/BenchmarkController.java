package tech.HTECH.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import tech.HTECH.service.BenchmarkService;
import tech.HTECH.service.CSVExporter;
import tech.HTECH.service.HistoryService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class BenchmarkController {

    @FXML private Label lblTotal, lblFAR, lblFRR;
    @FXML private TableView<BenchmarkService.BenchmarkResult> tableResults;
    @FXML private TableColumn<BenchmarkService.BenchmarkResult, String> colImgA, colImgB, colDecision, colStatus;
    @FXML private TableColumn<BenchmarkService.BenchmarkResult, Double> colChi2, colEucl, colCos, colGlobal;
    @FXML private BarChart<String, Number> barChart;
    @FXML private LineChart<Number, Number> chartDistribution;
    @FXML private LineChart<Number, Number> chartROC;

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
            updateHistogram(results);
            updateROC(results);
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
                .collect(Collectors.groupingBy(BenchmarkService.BenchmarkResult::getStatus, Collectors.counting()));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Statuts");

        String[] fullStatus = {"VP (Vrai Positif)", "VN (Vrai Négatif)", "FP (Faux Positif)", "FN (Faux Négatif)"};
        for (String status : fullStatus) {
            long count = counts.getOrDefault(status, 0L);
            series.getData().add(new XYChart.Data<>(status.split(" ")[0], count));
        }

        barChart.getData().add(series);
    }

    private void updateHistogram(List<BenchmarkService.BenchmarkResult> results) {
        chartDistribution.getData().clear();

        XYChart.Series<Number, Number> authentics = new XYChart.Series<>();
        authentics.setName("Authentiques (VP/FN)");
        
        XYChart.Series<Number, Number> impostors = new XYChart.Series<>();
        impostors.setName("Imposteurs (VN/FP)");

        // Buckets de 5%
        Map<Integer, Long> authBuckets = new TreeMap<>();
        Map<Integer, Long> impBuckets = new TreeMap<>();

        for (BenchmarkService.BenchmarkResult r : results) {
            int bucket = (int) (r.getGlobal() / 5) * 5;
            boolean isGenuine = r.getStatus().startsWith("VP") || r.getStatus().startsWith("FN");
            if (isGenuine) authBuckets.put(bucket, authBuckets.getOrDefault(bucket, 0L) + 1);
            else impBuckets.put(bucket, impBuckets.getOrDefault(bucket, 0L) + 1);
        }

        for (int i = 0; i <= 100; i += 5) {
            authentics.getData().add(new XYChart.Data<>(i, authBuckets.getOrDefault(i, 0L)));
            impostors.getData().add(new XYChart.Data<>(i, impBuckets.getOrDefault(i, 0L)));
        }

        chartDistribution.getData().addAll(authentics, impostors);
    }

    private void updateROC(List<BenchmarkService.BenchmarkResult> results) {
        chartROC.getData().clear();
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Courbe FAR vs FRR");

        long totalGenuines = results.stream().filter(r -> r.getStatus().startsWith("VP") || r.getStatus().startsWith("FN")).count();
        long totalImpostors = results.stream().filter(r -> r.getStatus().startsWith("VN") || r.getStatus().startsWith("FP")).count();

        if (totalGenuines == 0 || totalImpostors == 0) return;

        // Calculer FAR/FRR pour des seuils de 0 à 100
        for (int t = 0; t <= 100; t += 2) {
            final int threshold = t;
            long fp = results.stream().filter(r -> !isTheoreticallySame(r) && r.getGlobal() >= threshold).count();
            long fn = results.stream().filter(r -> isTheoreticallySame(r) && r.getGlobal() < threshold).count();

            double far = (double) fp / totalImpostors * 100;
            double frr = (double) fn / totalGenuines * 100;

            series.getData().add(new XYChart.Data<>(far, frr));
        }

        chartROC.getData().add(series);
    }

    private boolean isTheoreticallySame(BenchmarkService.BenchmarkResult r) {
        return r.getStatus().startsWith("VP") || r.getStatus().startsWith("FN");
    }
}
