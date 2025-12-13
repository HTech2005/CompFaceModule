# CompFaceModule - Documentation Technique

Ce projet implémente un système de reconnaissance faciale basé sur des méthodes de vision par ordinateur classiques (non-Deep Learning), utilisant OpenCV et ImageJ.

## 🚀 Pipeline de Reconnaissance (De A à Z)

Le processus de reconnaissance suit scrupuleusement les étapes suivantes pour chaque image :

### 1. Détection de Visage (Face Detection)
*   **Outil** : OpenCV (`CascadeClassifier`)
*   **Méthode** : Viola-Jones (Haar Cascades)
*   **Fonctionnement** : L'algorithme scanne l'image pour trouver des motifs rectangulaires contrastés ressemblant à un visage.
*   **Action** : L'image est rognée (croppée) autour du visage détecté pour éliminer le fond inutile.

### 2. Prétraitement (Preprocessing)
Avant l'analyse, le visage subit des transformations pour standardiser l'entrée :
1.  **Niveaux de gris** : Conversion de l'image couleur en noir et blanc (0-255).
2.  **Redimensionnement** : Image ramenée à **128x128 pixels**.
3.  **Égalisation d'Histogramme** : Amélioration du contraste pour compenser les variations d'éclairage.

### 3. Extraction de Caractéristiques (Feature Extraction)
Nous utilisons une approche hybride combinant deux descripteurs :

#### A. Histogramme de Niveau de Gris
Il représente la distribution des intensités lumineuses du visage.
*   On compte le nombre de pixels pour chaque niveau de gris $k$ (de 0 à 255).
*   **Formule** : $H(k) = \frac{n_k}{N}$
    *   $n_k$ : nombre de pixels d'intensité $k$.
    *   $N$ : nombre total de pixels.

#### B. Local Binary Patterns (LBP)
Le LBP analyse la texture locale (les micro-détails de la peau).
*   Pour chaque pixel central $g_c$, on compare sa valeur avec ses 8 voisins $g_p$.
*   **Formule LBP** :
    $$LBP_{P,R} = \sum_{p=0}^{P-1} s(g_p - g_c) 2^p$$
    *   Où la fonction seuil $s(x)$ vaut 1 si $x \ge 0$, sinon 0.
*   On construit ensuite un histogramme de ces valeurs LBP.

### 4. Fusion et Normalisation
Les deux vecteurs (Histogramme global et Histogramme LBP) sont concaténés en un seul vecteur unique de caractéristiques.
Ce vecteur est ensuite **normalisé** (rendu unitaire) pour que l'échelle des valeurs n'influence pas la comparaison.

*   **Formule de Normalisation Euclidienne** :
    $$V_{norm} = \frac{V}{||V||} = \frac{V}{\sqrt{\sum V_i^2}}$$

### 5. Comparaison et Décision
Pour vérifier si deux visages correspondent, on compare leurs vecteurs normalisés $A$ et $B$.

#### Méthode 1 : Distance Euclidienne
C'est la distance géométrique standard entre deux points.
*   **Formule** :
    $$d(A, B) = \sqrt{\sum_{i=1}^{n} (A_i - B_i)^2}$$
*   **Interprétation** : Plus la distance est proche de **0**, plus les visages sont similaires.

#### Méthode 2 : Similarité Cosinus (Recommandée)
Elle mesure le cosinus de l'angle entre les deux vecteurs.
*   **Formule** :
    $$\text{Cosinus}(A, B) = \frac{A \cdot B}{||A|| \times ||B||} = \sum_{i=1}^{n} A_i \times B_i$$
    *(Puisque nos vecteurs sont déjà normalisés, $||A|| = ||B|| = 1$)*
*   **Interprétation** : Le résultat est entre 0 et 1 (ou 0% et 100%). Plus il est proche de **1 (100%)**, plus les visages sont identiques.

#### Score de Compatibilité
Un score simplifié est calculé à partir de la distance Euclidienne :
$$Score = (1 - d(A, B)) \times 100$$
*(Si le score est négatif, il est ramené à 0)*.

## 🛠 Compilation et Usage

**Compiler :**
```bash
mvn clean compile
```

**Lancer :**
```bash
mvn exec:java
```
