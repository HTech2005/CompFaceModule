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
    *   **Pourquoi** : Permet la comparaison mathématique de vecteurs de même dimension.
*   **C. Recadrage "Cœur de Visage" (Tighter Crop)**
    *   **Comment** : Réduction du rectangle de détection de **15%** sur chaque bord après la détection.
    *   **Pourquoi** : Élimine les cheveux, les oreilles et le fond pour ne garder que les traits discriminants (yeux, nez, bouche). Réduit drastiquement les faux positifs.
*   **D. Égalisation d'Histogramme**
    *   **Comment** : Étirement dynamique via ContrastEnhancer.
    *   **Pourquoi** : Normalise l'éclairage pour une robustesse accrue.

### 3. Extraction de Caractéristiques (Features)
*   **Cœur du système** : Utilisation d'un **LBP Global** (Histogramme de texture) couplé à un histogramme de forme.

---

## 📊 Formules Mathématiques

### Distance Chi-Carré ($\chi^2$)
Mesure statistique pour comparer des histogrammes de texture (LBP).
$$D(A, B) = \sum \frac{(A_i - B_i)^2}{A_i + B_i}$$
> Plus robuste aux variations de lumière et plus sensible aux détails fins que l'Euclidienne.

### Similarité Cosinus
Mesure l'angle entre les deux vecteurs (l'alignement des traits).
$$s(A, B) = \frac{\sum_{i=1}^{n} A_i \cdot B_i}{\sqrt{\sum_{i=1}^{n} A_i^2} \cdot \sqrt{\sum_{i=1}^{n} B_i^2}}$$
> Résultat entre **0** (différent) et **1** (parfaitement aligné).

### Taux de Compatibilité
Traduction humaine de la distance Chi-Carré.
$$\text{Taux} = (1 - \frac{D}{2}) \times 100$$

---

Le système utilise désormais une **Triple Fusion d'Expertises** pour une fiabilité maximale. Chaque méthode compense les faiblesses des autres :

1.  **Texture fine (Chi-Carré $\chi^2$) - 50%** : Analyse microscopique des pores et micro-reliefs. C'est le cœur de la décision.
2.  **Alignement (Cosinus) - 30%** : Analyse l'angle des traits faciaux. Très robuste aux variations d'éclairage.
3.  **Géométrie (Euclidienne) - 20%** : Mesure l'écart de forme globale entre les deux signatures.

### Formule du Score Global Fusionné :
$$Score_{Global} = (Score_{Chi2} \times 0.5) + (Score_{Cos} \times 0.3) + (Score_{Eucl} \times 0.2)$$

| Paramètre | Valeurs & Poids | Rôle |
| :--- | :--- | :--- |
| **Texture Chi2** | **50%** | Identification précise de la peau/pores. |
| **Cosinus** | **30%** | Stabilité face aux changements de lumière. |
| **Euclidien** | **20%** | Vérification de la structure globale. |
| **Seuil Global** | **75.0%** | Score minimum pour valider le Match. |

### Logique de Verdict :
- **SI** $Score_{Global} \ge 75\%$ $\rightarrow$ **MATCH (Identité Confirmée)**.
- **SINON** $\rightarrow$ **REFUSÉ**.

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

© 2025 Tech HTECH - Module de Compétition Faciale
