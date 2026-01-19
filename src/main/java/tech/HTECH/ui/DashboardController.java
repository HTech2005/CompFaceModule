package tech.HTECH.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import tech.HTECH.service.FaceService;
import tech.HTECH.service.HistoryService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class DashboardController {

    @FXML
    private Label lblCountEnregistre;
    @FXML
    private ListView<String> listBDD;
    @FXML
    private VBox previewPane;
    @FXML
    private ImageView imgPreview;
    @FXML
    private TextField txtFileName;

    private FaceService faceService = new FaceService();
    private File currentSelectedFile;

    @FXML
    public void initialize() {
        loadBDD();

        // Listener pour la sélection dans la liste
        listBDD.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showPreview(newVal);
            } else {
                previewPane.setVisible(false);
            }
        });
    }

    private void loadBDD() {
        File bddDir = new File("src/main/bdd");
        if (!bddDir.exists())
            bddDir.mkdirs();

        File[] files = bddDir.listFiles((dir, name) -> name.toLowerCase().matches(".*\\.(jpg|jpeg|png)$"));
        ObservableList<String> items = FXCollections.observableArrayList();
        if (files != null) {
            lblCountEnregistre.setText(String.valueOf(files.length));
            for (File f : files) {
                items.add(f.getName());
            }
        } else {
            lblCountEnregistre.setText("0");
        }
        listBDD.setItems(items);
    }

    private void showPreview(String fileName) {
        currentSelectedFile = new File("src/main/bdd", fileName);
        if (currentSelectedFile.exists()) {
            imgPreview.setImage(new Image(currentSelectedFile.toURI().toString()));
            txtFileName.setText(fileName);
            previewPane.setVisible(true);
        }
    }

    @FXML
    private void handleAdd() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Ajouter un visage à la BDD");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.jpg", "*.png", "*.jpeg"));
        File selected = fileChooser.showOpenDialog(listBDD.getScene().getWindow());

        if (selected != null) {
            File dest = new File("src/main/bdd", selected.getName());
            try {
                Files.copy(selected.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                faceService.indexFile(dest); // Mettre à jour le cache
                HistoryService.getInstance().addLog("BDD: Ajout du visage " + dest.getName());
                loadBDD();
                listBDD.getSelectionModel().select(dest.getName());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleRename() {
        if (currentSelectedFile == null)
            return;

        String newName = txtFileName.getText().trim();
        if (newName.isEmpty() || newName.equals(currentSelectedFile.getName()))
            return;

        // S'assurer que l'extension est conservée si l'utilisateur l'oublie
        if (!newName.toLowerCase().matches(".*\\.(jpg|jpeg|png)$")) {
            String ext = currentSelectedFile.getName().substring(currentSelectedFile.getName().lastIndexOf("."));
            newName += ext;
        }

        File dest = new File(currentSelectedFile.getParent(), newName);
        if (currentSelectedFile.renameTo(dest)) {
            String oldName = currentSelectedFile.getName();
            faceService.removeFile(oldName);
            faceService.indexFile(dest);
            HistoryService.getInstance().addLog("BDD: Renommage " + oldName + " -> " + newName);
            loadBDD();
            listBDD.getSelectionModel().select(newName);
        }
    }

    @FXML
    private void handleDelete() {
        if (currentSelectedFile != null && currentSelectedFile.exists()) {
            String name = currentSelectedFile.getName();
            if (currentSelectedFile.delete()) {
                faceService.removeFile(name);
                HistoryService.getInstance().addLog("BDD: Suppression du visage " + name);
                loadBDD();
                previewPane.setVisible(false);
            }
        }
    }
}
