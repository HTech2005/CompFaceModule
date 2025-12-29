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
*   **Comment** : Utilisation de l'algorithme de **Viola-Jones**. Le système scanne l'image pour trouver des contrastes spécifiques (yeux plus sombres que le front, etc.).
*   **Pourquoi** : Pour isoler le visage et éliminer le "bruit" (fond, vêtements) afin d'optimiser les calculs.

### 2. Prétraitement (`Pretraitement.java`)
*   **Gris** : On retire la couleur car elle n'est pas fiable biométriquement (dépend de la lampe). On garde la **structure**.
*   **Resize (128x128)** : Normalisation de la taille pour permettre la comparaison mathématique de vecteurs de même dimension.
*   **Égalisation d'Histogramme** : On étire le contraste. **Pourquoi ?** Pour que le système reconnaisse la même personne qu'il fasse jour ou nuit.

### 3. Extraction de Caractéristiques
*   **Histogramme Global** : Compte la distribution des intensités. Capture la **forme générale**.
*   **LBP (Local Binary Pattern)** : Analyse la relation entre un pixel et ses voisins.
    *   **Comment** : On génère un code binaire 8-bits par pixel.
    *   **Pourquoi** : Capture la **texture fine** (pores, rides). C'est la partie la plus précise de la reconnaissance.

### 4. Fusion et Normalisation
*   **Fusion** : Combinaison des vecteur Histogramme + LBP.
*   **Normalisation** : Conversion du vecteur pour que sa norme soit égale à 1. **Pourquoi ?** Pour comparer des "directions" de traits faciaux et non des valeurs brutes de pixels.

---

## 📊 Formules Mathématiques

### Distance Euclidienne
C'est la mesure de l'écart direct entre deux signatures $A$ et $B$ dans un espace à $n$ dimensions.
$$d(A, B) = \sqrt{\sum_{i=1}^{n} (A_i - B_i)^2}$$
> Plus $d$ tend vers **0**, plus les visages sont **identiques**.

### Similarité Cosinus
Elle mesure l'angle entre les deux vecteurs de caractéristiques. Contrairement à la distance qui mesure l'écart "physique", le cosinus mesure l'alignement des traits.
$$s(A, B) = \frac{\sum_{i=1}^{n} A_i \cdot B_i}{\sqrt{\sum_{i=1}^{n} A_i^2} \cdot \sqrt{\sum_{i=1}^{n} B_i^2}}$$
> Le résultat varie de **0** (totalement différent) à **1** (parfaitement aligné).

---

## ⚙️ Seuils et Décision

Le système utilise un **Seuil (Threshold)** critique pour valider une identité :

| Paramètre | Valeur | Description |
| :--- | :--- | :--- |
| **Seuil de Distance** | **0.25** | Limite maximale pour un "Match". |
| **Taux de Compatibilité** | **75%** | Correspondance minimale exigée ($ (1 - d) \times 100 $). |

### Logique de Décision :
- **SI** Distance $< 0.25$ $\rightarrow$ **MATCH (Accès Autorisé)**.
- **SINON** $\rightarrow$ **REFUSÉ**.

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
