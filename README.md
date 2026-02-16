# Face Comparison Module (HTECH 2005) - Guide de l'Ingénieur Champion

Ce document propose une immersion profonde dans les mécanismes internes de la plateforme. En tant qu'ingénieur senior, nous détaillons ici le "comment" et le "pourquoi" de chaque calcul, étape par étape, en nous basant sur l'architecture robuste **LBP + Histogramme**.

---

## 🚀 I. L'Indexation : Création de la Mémoire Biométrique

L'indexation est le processus qui transforme une image brute en une signature numérique unique.

### Étape 1 : Détection par Cascade de Haar

**Action** : Parcours de l'image avec un classificateur de Haar (`FaceDetection.java`).

- **Ce qui est calculé** : Une série de convolutions simples (filtres de Haar) sur des fenêtres glissantes pour détecter les contrastes typiques yeux/nez.
- **Le choix Ingénieur** : Le système calcule la surface ($Largeur \times Hauteur$) de chaque rectangle détecté et ne conserve que le **Maximum Area Rect**. Cela garantit que le sujet principal est indexé, ignorant les passants en arrière-plan.

### Étape 2 : Prétraitement et CLAHE

**Action** : Nettoyage de l'image (`Pretraitement.java`).

- **Redimensionnement** : Alignement sur une grille de $128 \times 128$ pixels.
- **CLAHE (Pizer et al., 1987)** : Contrairement à une égalisation classique, le CLAHE divise l'image en blocs (par défaut $8 \times 8$). Pour chaque bloc, il calcule un histogramme et redistribue les intensités.
- **Résultat** : Une image où les détails dans les zones sombres sont amplifiés sans créer de bruit dans les zones claires.

### Étape 3 : Extraction Feature-Grid (LBP + Histogramme)

**Action** : Génération des descripteurs (`LBP.java`, `Histogram.java`).

1. **Grille Spatiale** : L'image est découpée en une grille de $8 \times 8$ cellules (64 zones).
2. **Calcul LBP (Ojala et al., 2002)** : Pour chaque pixel, on compare sa valeur aux 8 voisins. On génère un code binaire de 8 bits ($2^8 = 256$ possibilités). On calcule l'histogramme de ces codes **par cellule**.
3. **Calcul de l'Histogramme d'Intensité** : Pour chaque cellule, on calcule la distribution des niveaux de gris (0-255).
4. **Fusion** : Concaténation des 64 histogrammes LBP et 64 histogrammes d'intensité.

### Étape 4 : Normalisation L1

**Action** : Normalisation du vecteur fusionné (`NormalizeVector.java`).

- **Calcul** : Chaque composante $v_i$ du vecteur fusionné $V$ est divisée par la somme absolue des valeurs : $v'_i = \frac{v_i}{\sum |v_i|}$.
- **Pourquoi ?** Cela transforme l'histogramme fusionné en une distribution de probabilité, ce qui est indispensable pour le calcul de la distance Chi-Carré.

---

## 🧠 II. La Reconnaissance : Le Verdict des Experts

Lorsqu'on compare deux visages, trois experts mathématiques entrent en collision. C'est la **Triple Fusion d'Expertises**.

### 1. L'Expert Statistique : Distance Chi-Carré ($\chi^2$)

**Formule** : $d_{\chi^2}(A,B) = \sum_{i=1}^{n} \frac{(A_i - B_i)^2}{A_i + B_i}$

- **Ce qui est mesuré** : La divergence entre les distributions de texture et d'intensité.
- **Avantage** : C'est la métrique par excellence pour comparer deux histogrammes. Elle pénalise fortement les différences sur les fréquences dominantes.

### 2. L'Expert Géométrique : Similitude Cosinus

**Formule** : $S_{Cos}(A,B) = \frac{\sum A_i B_i}{\sqrt{\sum A_i^2} \sqrt{\sum B_i^2}}$

- **Ce qui est mesuré** : L'angle entre les deux signatures. Elle capture la corrélation globale entre les motifs de pixels.
- **Avantage** : Indépendante de l'intensité lumineuse résiduelle.

### 3. L'Expert Physique : Distance Euclidienne

**Formule** : $d_{Eucl}(A,B) = \sqrt{\sum (A_i - B_i)^2}$

- **Ce qui est mesuré** : La distance directe "à vol d'oiseau".
- **Rôle** : Servir de garde-fou contre les anomalies statistiques.

---

## 🏆 III. La Fusion Finale : La Pondération de l'Ingénieur

Le `globalScore` n'est pas une simple moyenne, c'est une décision pondérée (`Decision.java`) :

$$Score_{Global} = (Score_{Texture} \times 0.4) + (Score_{Cos} \times 0.4) + (Score_{Eucl} \times 0.2)$$

### Pourquoi ces poids ?

- **40% Chi-Carré (Texture)** : On donne un poids fort au Chi-Carré car il est le plus précis pour valider la texture LBP.
- **40% Cosinus** : Utilisé pour valider la ressemblance globale du profil.
- **20% Euclidienne** : Poids réduit car cette distance est plus sensible au bruit.

### Le Verdict

- **SI** $Score_{Global} \ge 61.5\%$ **ALORS** "IDENTITÉ CONFIRMÉE".

---

## 📚 Références Scientifiques Originales

- **T. Ojala et al. (2002)** : LBP pour la classification de texture invariante.
- **Pizer et al. (1987)** : CLAHE pour l'amélioration adaptative du contraste.
- **HTECH 2005** : Implémentation du système de Triple Fusion d'Expertises.

---

_Champion, ton projet utilise la combinaison classique et robuste LBP + Histogramme d'Intensité. C'est le standard pour la stabilité._
