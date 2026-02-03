package tech.HTECH.ui;

import ij.process.ImageProcessor;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import tech.HTECH.*;
import tech.HTECH.service.FaceService;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

public class StepAnalysisController {

    // Column 1
    @FXML private ImageView img1_Original, img1_Gray, img1_Median, img1_Clahe, img1_LBP;
    @FXML private TextArea txt1_Histo, txt1_LBP, txt1_Fusion;
    @FXML private Label lblStatus1;

    // Column 2
    @FXML private ImageView img2_Original, img2_Gray, img2_Median, img2_Clahe, img2_LBP;
    @FXML private TextArea txt2_Histo, txt2_LBP, txt2_Fusion;
    @FXML private Label lblStatus2;

    // Results Table
    @FXML private Label resChiBrut, resChiPct, resChiDesc;
    @FXML private Label resCosBrut, resCosPct, resCosDesc;
    @FXML private Label resEuclBrut, resEuclPct, resEuclDesc;
    @FXML private Label resGlobalScore, resMatch, lblSummary;

    private File file1, file2;
    private double[] features1, features2;
    private FaceService faceService = new FaceService();

    @FXML
    private void handleUpload1() {
        file1 = chooseFile();
        if (file1 != null) {
            lblStatus1.setText(file1.getName());
            features1 = processSlot(file1, 1);
            refreshComparison();
        }
    }

    @FXML
    private void handleUpload2() {
        file2 = chooseFile();
        if (file2 != null) {
            lblStatus2.setText(file2.getName());
            features2 = processSlot(file2, 2);
            refreshComparison();
        }
    }

    @FXML
    private void handleReset() {
        file1 = null; file2 = null;
        features1 = null; features2 = null;
        clearSlot(1); clearSlot(2);
        clearResults();
        lblStatus1.setText("Vide");
        lblStatus2.setText("Vide");
        lblSummary.setText("Réinitialisé. En attente d'images...");
    }

    private File chooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png", "*.jpeg"));
        return fileChooser.showOpenDialog(lblStatus1.getScene().getWindow());
    }

    private double[] processSlot(File file, int slot) {
        try {
            Mat original = opencv_imgcodecs.imread(file.getAbsolutePath());
            if (original.empty()) return null;
            
            setImg(slot, "Original", OpenCVUtils.matToImage(original));

            Mat face = FaceDetection.detectFace(file.getAbsolutePath());
            if (face == null) {
                if (slot == 1) lblStatus1.setText("! Visage non détecté !");
                else lblStatus2.setText("! Visage non détecté !");
                return null;
            }

            ImageProcessor ip = OpenCVUtils.matToImageProcessor(face);
            ip = ip.convertToByte(true);
            ip = ip.resize(130, 130);
            setImg(slot, "Gray", OpenCVUtils.matToImage(OpenCVUtils.imageProcessorToMat(ip)));

            ip.medianFilter();
            setImg(slot, "Median", OpenCVUtils.matToImage(OpenCVUtils.imageProcessorToMat(ip)));

            Mat mat = OpenCVUtils.imageProcessorToMat(ip);
            Mat claheMat = new Mat();
            org.bytedeco.opencv.opencv_imgproc.CLAHE clahe = opencv_imgproc.createCLAHE(2.0, new Size(8, 8));
            clahe.apply(mat, claheMat);
            setImg(slot, "Clahe", OpenCVUtils.matToImage(claheMat));
            
            ip = OpenCVUtils.matToImageProcessor(claheMat);
            double[][] lbpMap = LBP.LBP2D(ip);
            setImg(slot, "LBP", OpenCVUtils.matToImage(OpenCVUtils.imageProcessorToMat(LBP.drawLBP(lbpMap))));

            double[] h = Histogram.histoGrid(ip, 8, 8);
            double[] lbpFinal = LBP.histogramLBPGrid(lbpMap, 8, 8);
            double[] fusion = Fusion.fus(h, lbpFinal);
            double[] normalized = NormalizeVector.normalize(fusion);

            setTxt(slot, "Histo", formatVector(h));
            setTxt(slot, "LBP", formatVector(lbpFinal));
            setTxt(slot, "Fusion", formatVector(normalized));

            original.release(); face.release(); mat.release(); claheMat.release();
            
            return normalized;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void refreshComparison() {
        if (features1 != null && features2 != null) {
            // 1-vs-1 Comparison
            lblSummary.setText("Comparaison directe entre Image 1 et Image 2 terminée.");
            displayComparison(features1, features2, "Image 2");
        } else {
            // No comparison if only 1 or 0 images
            clearResults();
            if (features1 != null || features2 != null) {
                lblSummary.setText("En attente de la deuxième image pour lancer les calculs de distance...");
            } else {
                lblSummary.setText("Prêt. Veuillez charger des images.");
            }
        }
    }

    private void displayComparison(double[] v1, double[] v2, String targetName) {
        double dChi2 = Comparaison.distanceKhiCarre(v1, v2);
        double cos = Comparaison.similitudeCosinus(v1, v2);
        double dEucl = Comparaison.distanceEuclidienne(v1, v2);

        double sChi2 = Compatibilite.CalculCompatibilite(dChi2);
        double sCos = cos * 100.0;
        double sEucl = Math.max(0.0, (1.0 - (dEucl / 0.065)) * 100.0);
        double global = (sChi2 * 0.4) + (sCos * 0.4) + (sEucl * 0.2);

        resChiBrut.setText(String.format("%.4f", dChi2));
        resChiPct.setText(String.format("%.1f%%", sChi2));
        resChiDesc.setText(sChi2 > 70 ? "Excellente correspondance texturale" : "Structure de texture différente");

        resCosBrut.setText(String.format("%.4f", cos));
        resCosPct.setText(String.format("%.1f%%", sCos));
        resCosDesc.setText(sCos > 80 ? "Orientation globale très proche" : "Globalement différent");

        resEuclBrut.setText(String.format("%.4f", dEucl));
        resEuclPct.setText(String.format("%.1f%%", sEucl));
        resEuclDesc.setText(dEucl < 0.05 ? "Géométrie faciale quasi identique" : "Écart géométrique significatif");

        resGlobalScore.setText(String.format("%.2f%%", global));
        resMatch.setText(global >= 61.5 ? "MATCH POSITIF (" + targetName + ")" : "NON RECONNU");
        resMatch.setStyle(global >= 61.5 ? "-fx-text-fill: #00ff00;" : "-fx-text-fill: #ff0000;");
    }

    private String formatVector(double[] v) {
        if (v == null) return "Vecteur vide";
        StringBuilder sb = new StringBuilder();
        sb.append("Taille totale : ").append(v.length).append(" éléments\n\n");
        for (int i = 0; i < v.length; i++) {
            sb.append(String.format("[%04d]: %.6f", i, v[i]));
            if ((i + 1) % 4 == 0) sb.append("\n");
            else sb.append("   ");
            
            // Limit to first 400 values to avoid UI lag while still showing a large chunk
            if (i > 400) {
                sb.append("\n... (tronqué pour affichage)");
                break;
            }
        }
        return sb.toString();
    }

    private void setImg(int slot, String type, javafx.scene.image.Image img) {
        if (slot == 1) {
            if (type.equals("Original")) img1_Original.setImage(img);
            else if (type.equals("Gray")) img1_Gray.setImage(img);
            else if (type.equals("Median")) img1_Median.setImage(img);
            else if (type.equals("Clahe")) img1_Clahe.setImage(img);
            else if (type.equals("LBP")) img1_LBP.setImage(img);
        } else {
            if (type.equals("Original")) img2_Original.setImage(img);
            else if (type.equals("Gray")) img2_Gray.setImage(img);
            else if (type.equals("Median")) img2_Median.setImage(img);
            else if (type.equals("Clahe")) img2_Clahe.setImage(img);
            else if (type.equals("LBP")) img2_LBP.setImage(img);
        }
    }

    private void setTxt(int slot, String type, String val) {
        if (slot == 1) {
            if (type.equals("Histo")) txt1_Histo.setText(val);
            else if (type.equals("LBP")) txt1_LBP.setText(val);
            else if (type.equals("Fusion")) txt1_Fusion.setText(val);
        } else {
            if (type.equals("Histo")) txt2_Histo.setText(val);
            else if (type.equals("LBP")) txt2_LBP.setText(val);
            else if (type.equals("Fusion")) txt2_Fusion.setText(val);
        }
    }

    private void clearSlot(int slot) {
        if (slot == 1) {
            img1_Original.setImage(null); img1_Gray.setImage(null); img1_Median.setImage(null); img1_Clahe.setImage(null); img1_LBP.setImage(null);
            txt1_Histo.clear(); txt1_LBP.clear(); txt1_Fusion.clear();
        } else {
            img2_Original.setImage(null); img2_Gray.setImage(null); img2_Median.setImage(null); img2_Clahe.setImage(null); img2_LBP.setImage(null);
            txt2_Histo.clear(); txt2_LBP.clear(); txt2_Fusion.clear();
        }
    }

    private void clearResults() {
        resChiBrut.setText("-"); resChiPct.setText("-"); resChiDesc.setText("-");
        resCosBrut.setText("-"); resCosPct.setText("-"); resCosDesc.setText("-");
        resEuclBrut.setText("-"); resEuclPct.setText("-"); resEuclDesc.setText("-");
        resGlobalScore.setText("-%"); resMatch.setText("");
    }
}
