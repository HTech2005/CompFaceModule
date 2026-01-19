# FaceModule - Logiciel de Reconnaissance et Comparaison Faciale

Ce projet est une application desktop JavaFX utilisant OpenCV et des techniques classiques de Computer Vision (LBP, Histogrammes, CLAHE) pour la reconnaissance faciale.

## 📂 Architecture des Fichiers et Rôles

### 🖥️ Interface Utilisateur (UI)
- **[AppJavaFX.java](file:///c:/Users/HP/Desktop/TNI/CompFaceModule/src/main/java/tech/HTECH/ui/AppJavaFX.java)** : Point d'entrée de l'application. Fenêtre principale (800x400).
- **[MainController.java](file:///c:/Users/HP/Desktop/TNI/CompFaceModule/src/main/java/tech/HTECH/ui/MainController.java)** : Navigation latérale et gestion dynamique.
- **[ComparisonController.java](file:///c:/Users/HP/Desktop/TNI/CompFaceModule/src/main/java/tech/HTECH/ui/ComparisonController.java)** (CDV) : Comparaison locale et détection d'erreurs.
- **[DashboardController.java](file:///c:/Users/HP/Desktop/TNI/CompFaceModule/src/main/java/tech/HTECH/ui/DashboardController.java)** : Gestion BDD (Ajout, Suppr, Rename).
- **[StatisticsController.java](file:///c:/Users/HP/Desktop/TNI/CompFaceModule/src/main/java/tech/HTECH/ui/StatisticsController.java)** : Reporting, stats et galerie.
- **[RecognitionController.java](file:///c:/Users/HP/Desktop/TNI/CompFaceModule/src/main/java/tech/HTECH/ui/RecognitionController.java)** (TR) : Reconnaissance temps réel.
- **[AnalysisController.java](file:///c:/Users/HP/Desktop/TNI/CompFaceModule/src/main/java/tech/HTECH/ui/AnalysisController.java)** (CV) : Analyse traits biométriques.

### 🧠 Logique Métier (Service)
- **[FaceService.java](file:///c:/Users/HP/Desktop/TNI/CompFaceModule/src/main/java/tech/HTECH/service/FaceService.java)** : Indexation, cache statique et extraction algorithmique.
- **[HistoryService.java](file:///c:/Users/HP/Desktop/TNI/CompFaceModule/src/main/java/tech/HTECH/service/HistoryService.java)** : Système de logs, stats et détection auto de FP/FN.

### 🔬 Algorithmes de Vision
- **[FaceDetection.java](file:///c:/Users/HP/Desktop/TNI/CompFaceModule/src/main/java/tech/HTECH/FaceDetection.java)** : Utilise Haar Cascades pour trouver les visages. Applique un recadrage intelligent de 15% pour se concentrer sur les traits internes.
- **[Pretraitement.java](file:///c:/Users/HP/Desktop/TNI/CompFaceModule/src/main/java/tech/HTECH/Pretraitement.java)** : Prépare l'image. Redimensionnement en 128x128 et application de **CLAHE** pour neutraliser les variations de lumière.
- **[LBP.java](file:///c:/Users/HP/Desktop/TNI/CompFaceModule/src/main/java/tech/HTECH/LBP.java)** : Extrait la texture (le "grain" de la peau). Compare chaque pixel à ses 8 voisins pour créer une signature unique.
- **[Histogram.java](file:///c:/Users/HP/Desktop/TNI/CompFaceModule/src/main/java/tech/HTECH/Histogram.java)** : Analyse la distribution des intensités. Utilisé en mode "Grid" (8x8) pour capturer la structure locale du visage.
- **[Decision.java](file:///c:/Users/HP/Desktop/TNI/CompFaceModule/src/main/java/tech/HTECH/Decision.java)** : Fusionne les distances (Chi-Carré, Cosinus, Euclidienne) avec des poids spécifiques (60%, 20%, 20%) pour donner le verdict final.

---

## 🔍 Explication Ligne par Ligne (Cœur de l'Algorithme)

### 1. La Détection (FaceDetection.java)
```java
// On applique CLAHE pour "aplanir" la lumière
CLAHE clahe = opencv_imgproc.createCLAHE(2.0, new Size(8, 8));
clahe.apply(gray, claheApplied);

// minNeighbors = 4 : assure que l'objet détecté ressemble vraiment à un visage
classifier.detectMultiScale(gray, faces, 1.1, 4, 0, new Size(100, 100), new Size(0, 0));
```

### 2. L'Extraction de Texture (LBP.java)
LBP (Local Binary Patterns) est la clé de la précision :
- Pour chaque pixel central, on regarde ses 8 voisins.
- Si le voisin est plus clair, on met `1`, sinon `0`.
- On obtient un code binaire de 8 bits (0-255) qui représente la "forme" locale de la peau à cet endroit précis.

### 3. La Fusion et Normalisation (FaceService.java)
```java
// On divise le visage en 64 zones (8x8)
double[] h = Histogram.histoGrid(ip, 8, 8); 
double[] lbp = LBP.histogramLBPGrid(LBP.LBP2D(ip), 8, 8);

// On fusionne les deux types d'informations (forme + texture)
double[] fusion = Fusion.fus(h, lbp);

// On normalise (Somme = 1) pour pouvoir comparer des images de luminosités différentes
double[] normalized = NormalizeVector.normalize(fusion);
```

### 4. Le Verdict (Decision.java)
Le système ne se base pas sur un seul chiffre, mais sur une **triple expertise** :
- **Chi-Carré** : Très sensible aux changements de texture (peau). Poids dominant (60%).
- **Cosinus** : Regarde si les vecteurs de caractéristiques sont "orientés" de la même façon (alignement).
- **Euclidien** : Regarde la distance brute entre les valeurs.

---

## 🚀 Installation et Utilisation
1.  Assurez-vous d'avoir Java 17+ et Maven installés.
2.  Placez les images de référence dans `src/main/bdd`.
3.  Lancez avec : `mvn javafx:run`
4.  Le mode **Temps Réel** nécessite une webcam. Positionnez votre visage dans le **cadre vert en pointillés** pour une analyse optimale.
