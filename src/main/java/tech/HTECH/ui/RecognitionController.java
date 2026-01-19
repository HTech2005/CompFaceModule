package tech.HTECH.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.javacv.VideoInputFrameGrabber;
import org.bytedeco.opencv.opencv_core.Mat;
import tech.HTECH.FaceDetection;
import tech.HTECH.OpenCVUtils;
import tech.HTECH.service.FaceService;
import tech.HTECH.service.HistoryService;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RecognitionController {

    @FXML
    private ImageView videoFeed, imgResult;
    @FXML
    private VBox overlay, resultCard;
    @FXML
    private Label lblScanning, lblTimer, lblName, lblScore, lblStatus, lblMatchIcon, lblWaiting;
    @FXML
    private ProgressBar progressGlobal;
    @FXML
    private Button btnStart, btnStop;
    @FXML
    private Pane faceGuide;

    private FrameGrabber grabber;
    private ScheduledExecutorService timer;
    private OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
    private FaceService faceService = new FaceService();

    private int secondsLeft = 10;
    private int lastRecognitionSecond = -1;
    private boolean isScanning = false;

    @FXML
    public void startCamera() {
        try {
            grabber = new VideoInputFrameGrabber(0);
            grabber.start();

            btnStart.setDisable(true);
            btnStop.setDisable(false);
            lblWaiting.setVisible(false);

            secondsLeft = 10;
            isScanning = true;
            overlay.setVisible(true);

            timer = Executors.newSingleThreadScheduledExecutor();
            timer.scheduleAtFixedRate(this::grabFrame, 0, 33, TimeUnit.MILLISECONDS);

            // Background thread for countdown
            new Thread(() -> {
                while (secondsLeft > 0 && isScanning) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    secondsLeft--;
                    Platform.runLater(() -> lblTimer.setText(secondsLeft + "s"));
                }
                if (isScanning) {
                    Platform.runLater(this::stopCamera);
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void grabFrame() {
        try {
            Frame frame = grabber.grab();
            if (frame != null) {
                Mat mat = converter.convert(frame);

                // Mirror effect
                org.bytedeco.opencv.global.opencv_core.flip(mat, mat, 1);

                javafx.scene.image.Image image = OpenCVUtils.matToImage(mat);
                Platform.runLater(() -> videoFeed.setImage(image));

                // Perform recognition every ~1 second
                if (isScanning && secondsLeft != lastRecognitionSecond) {
                    lastRecognitionSecond = secondsLeft;
                    processRecognition(mat);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processRecognition(Mat mat) {
        int matW = mat.cols();
        int matH = mat.rows();

        // Calculer la zone de crop proportionnelle au cadre UI (200x240 sur 480x360)
        // On prend un peu plus large (marge de 20%) pour être plus souple
        double ratioW = 200.0 / 480.0 * 1.2;
        double ratioH = 240.0 / 360.0 * 1.2;

        int cropW = (int) (matW * ratioW);
        int cropH = (int) (matH * ratioH);
        int x = (matW - cropW) / 2;
        int y = (matH - cropH) / 2;

        try {
            // S'assurer que les coordonnées sont valides
            x = Math.max(0, x);
            y = Math.max(0, y);
            cropW = Math.min(matW - x, cropW);
            cropH = Math.min(matH - y, cropH);

            org.bytedeco.opencv.opencv_core.Rect guideRect = new org.bytedeco.opencv.opencv_core.Rect(x, y, cropW,
                    cropH);
            Mat croppedMat = new Mat(mat, guideRect);

            Mat face = FaceDetection.detectFaceMat(croppedMat);
            if (face != null) {
                Platform.runLater(() -> faceGuide.setStyle(
                        "-fx-border-color: #00ff88; -fx-border-width: 4; -fx-border-style: solid; -fx-border-radius: 15;"));
                FaceService.RecognitionResult result = faceService.recognizeFace(face);
                if (result.isFound()) {
                    HistoryService.getInstance().addLog("Reconnaissance TR: " + result.getBestMatch() + " ("
                            + String.format("%.1f", result.getScoreGlobal()) + "%)");
                }
                Platform.runLater(() -> updateUI(result));
            } else {
                // Notifier que rien n'est détecté
                Platform.runLater(() -> {
                    faceGuide.setStyle(
                            "-fx-border-color: #ffffff; -fx-border-width: 2; -fx-border-style: dashed; -fx-border-radius: 15; -fx-opacity: 0.5;");
                    lblStatus.setText("VISAGE NON DÉTECTÉ");
                    lblStatus.setStyle("-fx-text-fill: #aaaaaa;");
                    lblMatchIcon.setText("🔍");
                });
            }
        } catch (Exception e) {
            System.err.println("Erreur de cropping: " + e.getMessage());
        }
    }

    private void updateUI(FaceService.RecognitionResult result) {
        if (result == null) {
            lblMatchIcon.setText("🚫");
            lblName.setText("Inconnu");
            imgResult.setImage(null);
            progressGlobal.setProgress(0);
            lblScore.setText("Score: 0%");
            lblStatus.setText("AUCUN VISAGE");
            lblStatus.setStyle("-fx-text-fill: #ff4e4e;");
        } else {
            // On affiche toujours le meilleur candidat trouvé
            lblMatchIcon.setText("👤");
            lblName.setText(result.getBestMatch());

            // Charger la photo de l'individu
            if (result.getBestMatchFile() != null) {
                File imgFile = new File("src/main/bdd", result.getBestMatchFile());
                if (imgFile.exists()) {
                    imgResult.setImage(new javafx.scene.image.Image(imgFile.toURI().toString()));
                }
            }

            progressGlobal.setProgress(result.getScoreGlobal() / 100.0);
            lblScore.setText(String.format("Ressemblance: %.1f%%", result.getScoreGlobal()));

            if (result.isFound()) {
                lblStatus.setText("✅ ACCÈS AUTORISÉ");
                lblStatus.setStyle("-fx-text-fill: #00ff88;");
            } else {
                lblStatus.setText("⛔ COMPATIBILITÉ INSUFFISANTE");
                lblStatus.setStyle("-fx-text-fill: #ff9900;"); // Orange pour dire "presque"
            }
        }
        resultCard.setVisible(true);
    }

    @FXML
    public void stopCamera() {
        isScanning = false;
        overlay.setVisible(false);
        if (timer != null && !timer.isShutdown()) {
            timer.shutdown();
        }
        try {
            if (grabber != null) {
                grabber.stop();
                grabber.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        btnStart.setDisable(false);
        btnStop.setDisable(true);
    }
}
