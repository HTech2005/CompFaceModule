package tech.HTECH.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import tech.HTECH.service.BenchmarkService;
import tech.HTECH.service.CSVExporter;
import tech.HTECH.service.HistoryService;
import tech.HTECH.service.FaceService;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class BenchmarkController {

    @FXML private Label lblTotal, lblFAR, lblFRR, lblRecall, lblTNR, lblPrecision, lblF1, lblEER;
    @FXML private TableView<BenchmarkService.BenchmarkResult> tableResults;
    @FXML private TableColumn<BenchmarkService.BenchmarkResult, String> colImgA, colImgB, colDecision, colStatus;
    @FXML private TableColumn<BenchmarkService.BenchmarkResult, Double> colChi2, colEucl, colCos, colGlobal;
    @FXML private BarChart<String, Number> barChart;
    @FXML private TextField txtFilter;
    @FXML private LineChart<Number, Number> chartDistribution;
    @FXML private LineChart<Number, Number> chartROC;
    @FXML private LineChart<Number, Number> chartErrorRates;
    @FXML private Button btnRunBenchmark, btnRunFullBenchmark;
    @FXML private ComboBox<tech.HTECH.Decision.DecisionMode> comboMetric;

    private final BenchmarkService benchmarkService = new BenchmarkService();
    private final ObservableList<BenchmarkService.BenchmarkResult> resultList = FXCollections.observableArrayList();
    private FilteredList<BenchmarkService.BenchmarkResult> filteredResults;
    private Timeline indexingCheck;

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

        // Gestion du filtrage
        filteredResults = new FilteredList<>(resultList, p -> true);
        txtFilter.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredResults.setPredicate(res -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();
                return res.getImageA().toLowerCase().contains(lowerCaseFilter) ||
                       res.getImageB().toLowerCase().contains(lowerCaseFilter);
            });
            updateStats(filteredResults);
            updateChart(filteredResults);
            updateHistogram(filteredResults);
            updateROC(filteredResults);
            updateErrorRatesChart(filteredResults);
        });

        SortedList<BenchmarkService.BenchmarkResult> sortedData = new SortedList<>(filteredResults);
        sortedData.comparatorProperty().bind(tableResults.comparatorProperty());
        tableResults.setItems(sortedData);

        // Initialisation de la combo des métriques
        comboMetric.setItems(FXCollections.observableArrayList(tech.HTECH.Decision.DecisionMode.values()));
        comboMetric.setValue(tech.HTECH.Decision.DecisionMode.TRIPLE_FUSION);
        comboMetric.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(tech.HTECH.Decision.DecisionMode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.getLabel());
            }
        });
        comboMetric.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(tech.HTECH.Decision.DecisionMode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item.getLabel());
            }
        });

        // Re-analyser les résultats existants si le mode change
        comboMetric.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (!resultList.isEmpty()) {
                // Pour une exploration dynamique, on recalcule les statuts sur la liste actuelle
                // Mais runFullAnalysis est plus précis car il repasse par FaceService.
                // Ici on va juste rafraîchir les vues car les objets BenchmarkResult ont déjà les scores.
                // Note: La décision et le statut doivent être recalculés.
                refreshBenchmarkResults(newVal);
            }
        });
        
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

        // Check indexing status every second
        indexingCheck = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), e -> {
            boolean isIndexing = tech.HTECH.service.FaceService.isIndexing();
            indexingNotice.setVisible(isIndexing);
            indexingNotice.setManaged(isIndexing);
            btnRunBenchmark.setDisable(isIndexing);
            btnRunFullBenchmark.setDisable(isIndexing);
        }));
        indexingCheck.setCycleCount(javafx.animation.Animation.INDEFINITE);
        indexingCheck.play();
    }
    @FXML
    public void handleRunBenchmark(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir l'image cible pour l'analyse");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png", "*.jpeg"));
        File target = fileChooser.showOpenDialog(lblTotal.getScene().getWindow());

        if (target != null) {
            tech.HTECH.Decision.DecisionMode mode = comboMetric.getValue();
            List<BenchmarkService.BenchmarkResult> results = benchmarkService.runAnalysis(target, mode);
            if (results.isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "Aucun visage détecté.").show();
                return;
            }

            resultList.setAll(results);
            updateStats(results);
            updateChart(results);
            updateHistogram(results);
            updateROC(results);
            updateErrorRatesChart(results);
        }
    }

    private void refreshBenchmarkResults(tech.HTECH.Decision.DecisionMode mode) {
        double threshold = 61.5;
        for (BenchmarkService.BenchmarkResult r : resultList) {
            double currentScore;
            switch (mode) {
                case CHI_SQUARE: currentScore = r.getChi2(); break;
                case EUCLIDEAN: currentScore = r.getEucl(); break;
                case COSINE: currentScore = r.getCos(); break;
                case TRIPLE_FUSION:
                default: currentScore = r.getGlobal(); break;
            }
            
            r.setActiveScore(currentScore);
            boolean decision = currentScore >= threshold;
            r.setDecision(decision);

            if (r.isTheoreticallySame() && decision) r.setStatus("VP (Vrai Positif)");
            else if (r.isTheoreticallySame() && !decision) r.setStatus("FN (Faux Négatif)");
            else if (!r.isTheoreticallySame() && decision) r.setStatus("FP (Faux Positif)");
            else r.setStatus("VN (Vrai Négatif)");
        }
        
        updateStats(resultList);
        updateChart(resultList);
        updateHistogram(resultList);
        updateROC(resultList);
        updateErrorRatesChart(resultList);
        tableResults.refresh();
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

    @FXML private HBox progressBox;
    @FXML private ProgressBar progressBar;
    @FXML private Label lblProgress;
    @FXML private HBox indexingNotice;

    @FXML
    public void handleRunFullBenchmark(ActionEvent event) {
        progressBox.setVisible(true);
        progressBox.setManaged(true);
        progressBar.setProgress(0);
        lblProgress.setText("0%");

        javafx.concurrent.Task<List<BenchmarkService.BenchmarkResult>> task = new javafx.concurrent.Task<>() {
            @Override
            protected List<BenchmarkService.BenchmarkResult> call() {
                tech.HTECH.Decision.DecisionMode mode = comboMetric.getValue();
                return benchmarkService.runFullAnalysis(mode, (completed, total) -> {
                    updateProgress(completed, total);
                    javafx.application.Platform.runLater(() -> {
                        double p = (double) completed / total;
                        progressBar.setProgress(p);
                        lblProgress.setText(String.format("%.0f%%", p * 100));
                    });
                });
            }
        };

        task.setOnSucceeded(e -> {
            List<BenchmarkService.BenchmarkResult> results = task.getValue();
            progressBox.setVisible(false);
            progressBox.setManaged(false);
            
            if (results.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "Aucune image trouvée dans la BDD.").show();
                return;
            }

            resultList.setAll(results);
            updateStats(results);
            updateChart(results);
            updateHistogram(results);
            updateROC(results);
            updateErrorRatesChart(results);
        });

        task.setOnFailed(e -> {
            progressBox.setVisible(false);
            progressBox.setManaged(false);
            new Alert(Alert.AlertType.ERROR, "Erreur lors de l'analyse : " + task.getException().getMessage()).show();
        });

        new Thread(task).start();
    }

    private void updateStats(List<BenchmarkService.BenchmarkResult> results) {
        int total = results.size();
        
        long fp = results.stream().filter(r -> r.getStatus().startsWith("FP")).count();
        long fn = results.stream().filter(r -> r.getStatus().startsWith("FN")).count();
        
        long totalImpostors = results.stream().filter(r -> r.getStatus().startsWith("VN") || r.getStatus().startsWith("FP")).count();
        long totalGenuines = results.stream().filter(r -> r.getStatus().startsWith("VP") || r.getStatus().startsWith("FN")).count();

        long vp = totalGenuines - fn;
        long vn = totalImpostors - fp;

        double far = totalImpostors > 0 ? (double) fp / totalImpostors * 100 : 0;
        double frr = totalGenuines > 0 ? (double) fn / totalGenuines * 100 : 0;
        
        double recall = totalGenuines > 0 ? (double) vp / totalGenuines * 100 : 0;
        double tnr = totalImpostors > 0 ? (double) vn / totalImpostors * 100 : 0;
        double precision = (vp + fp) > 0 ? (double) vp / (vp + fp) * 100 : 0;
        
        double f1 = (precision + recall) > 0 ? 2 * (precision * recall) / (precision * 10 / 10 + recall) / 100 : 0; // Simplified
        
        // Calculate EER
        double eer = calculateEER(results);

        lblTotal.setText(String.valueOf(total));
        lblFAR.setText(String.format("%.2f%%", far));
        lblFRR.setText(String.format("%.2f%%", frr));
        lblRecall.setText(String.format("%.2f%%", recall));
        lblTNR.setText(String.format("%.2f%%", tnr));
        lblPrecision.setText(String.format("%.2f%%", precision));
        lblF1.setText(String.format("%.3f", f1));
        lblEER.setText(String.format("%.2f%%", eer));
    }

    private double calculateEER(List<BenchmarkService.BenchmarkResult> results) {
        long totalGenuines = results.stream().filter(this::isTheoreticallySame).count();
        long totalImpostors = results.stream().filter(r -> !isTheoreticallySame(r)).count();
        if (totalGenuines == 0 || totalImpostors == 0) return 0;

        double minDiff = Double.MAX_VALUE;
        double eerValue = 0;

        for (int t = 0; t <= 100; t++) {
            final int threshold = t;
            long fp = results.stream().filter(r -> !isTheoreticallySame(r) && r.getActiveScore() >= threshold).count();
            long fn = results.stream().filter(r -> isTheoreticallySame(r) && r.getActiveScore() < threshold).count();

            double far = (double) fp / totalImpostors * 100;
            double frr = (double) fn / totalGenuines * 100;

            double diff = Math.abs(far - frr);
            if (diff < minDiff) {
                minDiff = diff;
                eerValue = (far + frr) / 2.0;
            }
        }
        return eerValue;
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
            int bucket = (int) (r.getActiveScore() / 5) * 5;
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
        series.setName("ROC (TPR vs FPR)");

        long totalGenuines = results.stream().filter(this::isTheoreticallySame).count();
        long totalImpostors = results.stream().filter(r -> !isTheoreticallySame(r)).count();

        if (totalGenuines == 0 || totalImpostors == 0) return;

        // Ajouter (0,0)
        series.getData().add(new XYChart.Data<>(0, 0));

        for (int t = 100; t >= 0; t -= 2) {
            final int threshold = t;
            long tp = results.stream().filter(r -> isTheoreticallySame(r) && r.getActiveScore() >= threshold).count();
            long fp = results.stream().filter(r -> !isTheoreticallySame(r) && r.getActiveScore() >= threshold).count();

            double tpr = (double) tp / totalGenuines * 100;
            double fpr = (double) fp / totalImpostors * 100;

            series.getData().add(new XYChart.Data<>(fpr, tpr));
        }

        // Ajouter (100,100)
        series.getData().add(new XYChart.Data<>(100, 100));

        chartROC.getData().add(series);
    }

    private void updateErrorRatesChart(List<BenchmarkService.BenchmarkResult> results) {
        chartErrorRates.getData().clear();
        
        XYChart.Series<Number, Number> farSeries = new XYChart.Series<>();
        farSeries.setName("FAR (Fausse Acceptation)");
        
        XYChart.Series<Number, Number> frrSeries = new XYChart.Series<>();
        frrSeries.setName("FRR (Faux Rejet)");

        long totalGenuines = results.stream().filter(this::isTheoreticallySame).count();
        long totalImpostors = results.stream().filter(r -> !isTheoreticallySame(r)).count();

        if (totalGenuines == 0 || totalImpostors == 0) return;

        for (int t = 0; t <= 100; t += 2) {
            final int threshold = t;
            long fp = results.stream().filter(r -> !isTheoreticallySame(r) && r.getActiveScore() >= threshold).count();
            long fn = results.stream().filter(r -> isTheoreticallySame(r) && r.getActiveScore() < threshold).count();

            double far = (double) fp / totalImpostors * 100;
            double frr = (double) fn / totalGenuines * 100;

            farSeries.getData().add(new XYChart.Data<>(threshold, far));
            frrSeries.getData().add(new XYChart.Data<>(threshold, frr));
        }

        chartErrorRates.getData().addAll(farSeries, frrSeries);
    }

    private boolean isTheoreticallySame(BenchmarkService.BenchmarkResult r) {
        return r.getStatus().startsWith("VP") || r.getStatus().startsWith("FN");
    }
}
