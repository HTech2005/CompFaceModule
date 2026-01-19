package tech.HTECH.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;

import java.io.File;

public class DashboardController {

    @FXML
    private Label lblCountEnregistre;
    @FXML
    private ListView<String> listBDD;

    @FXML
    public void initialize() {
        loadBDD();
    }

    private void loadBDD() {
        File bddDir = new File("src/main/bdd");
        if (bddDir.exists() && bddDir.isDirectory()) {
            File[] files = bddDir.listFiles((dir, name) -> name.toLowerCase().matches(".*\\.(jpg|jpeg|png)$"));
            if (files != null) {
                lblCountEnregistre.setText(String.valueOf(files.length));
                ObservableList<String> items = FXCollections.observableArrayList();
                for (File f : files) {
                    items.add(f.getName());
                }
                listBDD.setItems(items);
            }
        }
    }
}
