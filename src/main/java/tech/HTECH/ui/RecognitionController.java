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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RecognitionController {

    @FXML
    private ImageView videoFeed;
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
        // Définir la zone du cadre de guidage (calculé par rapport à la taille de
        // l'image)
        // L'ImageView fait 480x360. Le guide fait 200x240 au centre.
        int guideWidth = 200;
        int guideHeight = 240;
        int x = (mat.cols() - guideWidth) / 2;
        int y = (mat.rows() - guideHeight) / 2;

        try {
            // Créer une sous-matrice correspondant au cadre de guidage
            org.bytedeco.opencv.opencv_core.Rect guideRect = new org.bytedeco.opencv.opencv_core.Rect(x, y, guideWidth,
                    guideHeight);
            Mat croppedMat = new Mat(mat, guideRect);

            // Détecter dans cette zone réduite (plus rapide et précis)
            Mat face = FaceDetection.detectFaceMat(croppedMat);
            if (face != null) {
                FaceService.RecognitionResult result = faceService.recognizeFace(face);
                Platform.runLater(() -> updateUI(result));
            }
        } catch (Exception e) {
            System.err.println("Erreur de cropping: " + e.getMessage());
        }
    }

    private void updateUI(FaceService.RecognitionResult result) {
        if (result == null || !result.isFound()) {
            lblMatchIcon.setText("🚫");
            lblName.setText("Inconnu");
            progressGlobal.setProgress(0);
            lblScore.setText("Score: 0%");
            lblStatus.setText("NON RECONNU");
            lblStatus.setStyle("-fx-text-fill: #ff4e4e;");
        } else {
            lblMatchIcon.setText("👤");
            lblName.setText(result.getBestMatch());
            progressGlobal.setProgress(result.getScoreGlobal() / 100.0);
            lblScore.setText(String.format("Score: %.1f%%", result.getScoreGlobal()));
            if (result.isMatch()) {
                lblStatus.setText("✅ ACCÈS AUTORISÉ");
                lblStatus.setStyle("-fx-text-fill: #00ff88;");
            } else {
                lblStatus.setText("⛔ ACCÈS REFUSÉ");
                lblStatus.setStyle("-fx-text-fill: #ff4e4e;");
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
