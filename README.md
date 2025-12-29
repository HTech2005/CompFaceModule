# CompFaceModule - Système de Reconnaissance et Analyse Faciale

Ce projet est une solution complète de biométrie faciale intégrant un Backend Java (OpenCV, SparkJava) et un Frontend React moderne. Il utilise des méthodes de vision par ordinateur classiques (LBP + Histogrammes) pour une détection rapide et sans GPU.

---

## 🚀 Fonctionnalités Principales

### 1. Comparaison de Visages (CDV)
*   **Interface** : `/cdv-compare`
*   **Fonction** : Compare deux images uploadées.
*   **Sortie** : Scores de similarité (Euclidien & Cosinus) et verdict.

### 2. Analyse Temps Réel (SV - Scanner Visage)
*   **Interface** : `/sv-analysis`
*   **Fonction** : Analyse un flux webcam pour mesurer la structure morphologique.

### 3. Identification Temps Réel (TR)
*   **Interface** : `/tr-recognition`
*   **Fonction** : Identifie une personne en direct par rapport à la base de données (`src/main/bdd`).

---

## 🧠 Fonctionnement Technique de A à Z

Le système suit un pipeline de traitement rigoureux pour transformer une image brute en une signature biométrique unique.

### 1. Détection du Visage (Haar Cascade)
*   **Comment ça marche ?** : Le système utilise l'algorithme de **Viola-Jones** via les classificateurs "Haar Cascade" d'OpenCV. Il scanne l'image avec des fenêtres de différentes tailles et cherche des motifs de contraste spécifiques (ex: la zone des yeux est souvent plus sombre que les pommettes et le front).
*   **Pourquoi le fait-on ?** :
    *   **Isolation** : Pour éliminer tout ce qui n'est pas le visage (fonds, vêtements).
    *   **Optimisation** : Traiter uniquement la zone d'intérêt (ROI) réduit drastiquement les calculs.

### 2. Prétraitement de l'Image (`Pretraitement.java`)
*   **A. Conversion en Niveaux de Gris**
    *   **Comment** : Fusion des canaux RVB en une seule valeur d'intensité.
    *   **Pourquoi** : La couleur n'est pas fiable (dépend de l'éclairage). Les niveaux de gris préservent la **structure**.
*   **B. Redimensionnement Standard (128x128)**
    *   **Comment** : Interpolation des pixels pour atteindre une taille fixe.
    *   **Pourquoi** : Permet la comparaison mathématique de vecteurs de même dimension, quelle que soit la résolution d'origine.
*   **C. Égalisation d'Histogramme**
    *   **Comment** : Étirement du spectre de gris de 0 à 255.
    *   **Pourquoi** : **Normaliser l'éclairage**. Indispensable pour reconnaître une personne dans différentes conditions lumineuses.

### 3. Extraction de Caractéristiques (Features)
*   **A. Histogramme Global (`Histogram.java`)**
    *   **Comment** : Distribution statistique des niveaux de gris.
    *   **Pourquoi** : Capture la **forme générale** et la distribution lumineuse du visage.
*   **B. LBP - Local Binary Pattern (`LBP.java`)**
    *   **Comment** : Compare chaque pixel à ses 8 voisins pour générer un code binaire 8-bits.
    *   **Pourquoi** : C'est le cœur du système. Il capture la **texture fine** (pores de la peau, rides, micro-contours). Très robuste aux changements de lumière.

### 4. Fusion et Normalisation (`Fusion.java` & `NormalizeVector.java`)
*   **A. Fusion**
    *   **Comment** : Concaténation des vecteurs Histogramme et LBP.
    *   **Pourquoi** : Combine les informations de forme globale et de texture locale pour une signature complète.
*   **B. Normalisation**
    *   **Comment** : Division par la norme Euclidienne.
    *   **Pourquoi** : Transforme le vecteur en une "direction" mathématique pure. Garantit que la distance dépend de la similitude des traits et non de l'intensité brute.

---

## 📊 Formules Mathématiques

### Distance Euclidienne
Mesure de l'écart direct entre deux signatures $A$ et $B$.
$$d(A, B) = \sqrt{\sum_{i=1}^{n} (A_i - B_i)^2}$$
> Plus $d$ est proche de **0**, plus les visages sont **identiques**.

### Similarité Cosinus
Mesure l'angle entre les deux vecteurs (l'alignement des traits).
$$s(A, B) = \frac{\sum_{i=1}^{n} A_i \cdot B_i}{\sqrt{\sum_{i=1}^{n} A_i^2} \cdot \sqrt{\sum_{i=1}^{n} B_i^2}}$$
> Résultat entre **0** (différent) et **1** (parfaitement aligné).

### Taux de Compatibilité
Traduction humaine de la distance.
$$\text{Taux} = (1 - d) \times 100$$

---

## ⚙️ Seuils et Décision (`Comparaison.java` & `Decision.java`)

Le système est calibré sur un **Seuil (Threshold)** de sécurité de **0.30** :

| État | Distance | Taux | Verdict |
| :--- | :--- | :--- | :--- |
| **Match Parfait** | 0.00 | 100% | ACCÈS AUTORISÉ |
| **Limite Acceptation** | **0.30** | **70%** | ACCÈS AUTORISÉ |
| **Douteux** | 0.35 | 65% | REFUSÉ |
| **Rejeté** | > 0.40 | < 60% | REFUSÉ |

---

## 🩺 Analyse Morphologique (`FaceAnalyzer.java`)
*   **Comment** : Détection des coordonnées des yeux et de la bouche.
*   **Pourquoi** : Couche de sécurité supplémentaire pour valider la structure anatomique (écart inter-oculaire, largeur de bouche).

---

## ⚡ Installation et Lancement

### 1. Démarrer le Backend (Port 4567)
```bash
mvn exec:java "-Dexec.mainClass=tech.HTECH.APIServer"
```

### 2. Démarrer le Frontend (Port 3000)
```bash
cd frontend
npm start
```

---

© 2024 Tech HTECH - Module de Compétition Faciale
