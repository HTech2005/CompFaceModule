package tech.HTECH.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class HistoryService {
    private static HistoryService instance;
    private final ObservableList<String> logs = FXCollections.observableArrayList();
    private final ObservableList<List<String>> similarityGroups = FXCollections.observableArrayList();

    private int totalCDV = 0;
    private int falsePositives = 0;
    private int falseNegatives = 0;

    private HistoryService() {
    }

    public static synchronized HistoryService getInstance() {
        if (instance == null) {
            instance = new HistoryService();
        }
        return instance;
    }

    public void addLog(String action) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logs.add(0, "[" + timestamp + "] " + action);
    }

    public void incrementCDV() {
        totalCDV++;
        // On ne loggue plus systématiquement ici, on le fait dans ComparisonController
        // pour plus de détails
    }

    public void checkAutomatedError(String name1, String name2, boolean isMatch) {
        boolean theoreticallySimilar = inSameGroup(name1, name2) || name1.equals(name2);

        if (theoreticallySimilar && !isMatch) {
            falseNegatives++;
            addLog("🔴 AUTO-DETECT: Faux Négatif (" + name1 + " vs " + name2 + ")");
        } else if (!theoreticallySimilar && isMatch) {
            falsePositives++;
            addLog("🔴 AUTO-DETECT: Faux Positif (" + name1 + " vs " + name2 + ")");
        }
    }

    public boolean inSameGroup(String n1, String n2) {
        for (List<String> group : similarityGroups) {
            boolean contains1 = false, contains2 = false;
            for (String name : group) {
                if (name.equalsIgnoreCase(n1))
                    contains1 = true;
                if (name.equalsIgnoreCase(n2))
                    contains2 = true;
            }
            if (contains1 && contains2)
                return true;
        }
        return false;
    }

    public void addFalsePositive() {
        falsePositives++;
        addLog("⚠️ Faux Positif manuel signalé");
    }

    public void addFalseNegative() {
        falseNegatives++;
        addLog("⚠️ Faux Négatif manuel signalé");
    }

    public void createSimilarityGroup(List<String> photoNames) {
        if (photoNames != null && !photoNames.isEmpty()) {
            similarityGroups.add(new ArrayList<>(photoNames));
            addLog("Test: Groupe de similitude créé: " + String.join(", ", photoNames));
        }
    }

    public void resetStats() {
        totalCDV = 0;
        falsePositives = 0;
        falseNegatives = 0;
        addLog("Réinitialisation des statistiques de test");
    }

    // Getters
    public ObservableList<String> getLogs() {
        return logs;
    }

    public ObservableList<List<String>> getSimilarityGroups() {
        return similarityGroups;
    }

    public int getTotalCDV() {
        return totalCDV;
    }

    public int getFalsePositives() {
        return falsePositives;
    }

    public int getFalseNegatives() {
        return falseNegatives;
    }
}
