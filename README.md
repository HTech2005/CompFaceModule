# FaceModule - Logiciel de Reconnaissance et Comparaison Faciale

Ce projet est une application desktop JavaFX utilisant OpenCV et des techniques avancées de Computer Vision pour la reconnaissance faciale.

## 🚀 Comment lancer le projet

1.  **Prérequis** : Java 17+ et Maven installés.
2.  **Configuration** : Placez les images de référence (.jpg/.png) dans le dossier `src/main/bdd`. Le nom du fichier sera utilisé comme nom de la personne.
3.  **Lancement** :
    ```bash
    mvn javafx:run
    ```
    _Note : Le mode Temps Réel nécessite une caméra active._

---

## 🔬 Fonctionnement Technique Approfondi

Le système s'appuie sur une extraction locale de caractéristiques et une triple expertise mathématique.

### 1. CDV : Comparaison de Visages (Mode 1:1)

Permet de vérifier si deux visages sont identiques.

**Étapes du processus :**

- **Détection** : Utilise `FaceDetection.java`. L'image est passée en gris, filtrée par CLAHE (Contrast Limited Adaptive Histogram Equalization) pour l'éclairage, puis détectée via Haar Cascade. Un recadrage (crop) de 15% est appliqué.
- **Prétraitement** : Dans `Pretraitement.java`.
  - Redimensionnement : Matrice **128x128**.
  - Filtre Médian + Flou Gaussien (sigma=0.8) pour le bruit.
  - CLAHE final pour accentuer les traits.
- **Extraction** : Utilise `Histogram.java` et `LBP.java`. Le visage est divisé en une **Grille 8x8** (64 cellules de 16x16 pixels).
- **Vecteur de Caractéristiques** : Chaque cellule génère un histogramme de 256 valeurs. Concaténation de 64 cellules = Vecteur de **16 384** valeurs.
- **Score Global** : Calculé dans `FaceService.java` ou `Decision.java`.

### 2. TR : Reconnaissance Temps Réel (Mode 1:N)

Identifie une personne en direct via webcam.

**Étapes du processus :**

- **Cadrage** : Un cadre guide (vert) force l'utilisateur à se centrer.
- **Boucle d'Analyse** : Le flux vidéo est traité en continu via `RecognitionController.java`.
- **Recherche** : Chaque visage détecté est comparé à TOUT le cache de la base de données (`FaceService.java`).
- **Logique de Verdict** :
  - **Validation Immédiate** : Score > 60%.
  - **Validation par Stabilité** : Si le score est entre 50% et 60%, le système attend **5 secondes** de stabilité sur la même identité avant de valider l'accès.
- **Pondération des Scores (TR)** :
  - 40% Texture (Chi-Carré)
  - 40% Structure (Cosinus)
  - 20% Géométrie (Euclidiènne)

### 3. CV : Analyse Visuelle (Module Biométrique)

Analyse les traits spécifiques du visage dans `AnalysisController.java` et `FaceAnalyzer.java`.

**Étapes du processus :**

- Détection des composants (Haar Cascades spécifiques).
- Calcul des dimensions et distances en pixels (px) :
  - Distance Inter-oculaire (Yeux).
  - Largeur du nez.
  - Largeur de la bouche.
- Visualisation : Dessin de boîtes englobantes colorées sur l'interface.

---

## 📐 Formules Mathématiques & Matrices

Le système utilise trois "experts" pour une décision robuste. Les calculs sont effectués dans `Comparaison.java`.

### A. Distance Chi-Carré ($\chi^2$) - Expert Texture

Utilisée pour comparer les histogrammes LBP (Local Binary Patterns).

- **Formule** : $\chi^2(A,B) = \sum \frac{(A_i - B_i)^2}{A_i + B_i}$
- **Signification** : Mesure la divergence entre les répartitions de texture fine.
- **Fichier** : `Comparaison.distanceKhiCarre`

### B. Similitude Cosinus ($Cos$) - Expert Structure

Mesure l'angle entre deux vecteurs.

- **Formule** : $Cos(\theta) = \frac{A \cdot B}{\|A\| \|B\|}$
- **Signification** : Indépendant de la luminosité brute. Mesure la corrélation structurelle des traits.
- **Fichier** : `Comparaison.similitudeCosinus`

### C. Distance Euclidienne ($d$) - Expert Géométrie

Distance géométrique directe par la méthode des moindres carrés.

- **Formule** : $d(A,B) = \sqrt{\sum (A_i - B_i)^2}$
- **Signification** : Écart global entre les signatures.
- **Fichier** : `Comparaison.distanceEuclidienne`

---

## 🛠️ Spécifications Techniques Résumées

| Paramètre        | Valeur           | Fichier Source               |
| :--------------- | :--------------- | :--------------------------- |
| Taille Image     | 128 x 128        | `Pretraitement.java`         |
| Division Grille  | 8 x 8 (64 blocs) | `Histogram.java`             |
| Taille Vecteur   | 16 384 valeurs   | `Fusion.java`                |
| Seuil Validation | 60%              | `Decision.java`              |
| Stabilité TR     | 5 secondes       | `RecognitionController.java` |

---

_Développement par HTECH 2005_
