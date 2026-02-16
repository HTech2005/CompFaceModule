# Face Comparison Module (HTECH 2005) - Guide de l'Ingénieur Champion

Ce document propose une immersion profonde dans les mécanismes internes de la plateforme. En tant qu'ingénieur senior, nous détaillons ici le "comment" et le "pourquoi" de chaque calcul, étape par étape.

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

### Étape 3 : Extraction Feature-Grid (LBP + HOG)

**Action** : Génération des descripteurs (`LBP.java`, `HOG.java`).

1. **Grille Spatiale** : L'image est découpée en une grille de $8 \times 8$ cellules (64 zones).
2. **Calcul LBP (Ojala et al., 2002)** : Pour chaque pixel, on compare sa valeur aux 8 voisins. On génère un code binaire de 8 bits ($2^8 = 256$ possibilités). On calcule l'histogramme de ces codes **par cellule**.
3. **Calcul HOG (Dalal & Triggs, 2005)** : On calcule l'orientation du gradient pour chaque pixel. On regroupe ces orientations dans un histogramme de 9 canaux par cellule.
4. **Fusion** : Concaténation des 64 histogrammes LBP et 64 histogrammes HOG.

### Étape 4 : Normalisation L2

**Action** : Projection sur la sphère unitaire (`NormalizeVector.java`).

- **Calcul** : Chaque composante $v_i$ du vecteur fusionné $V$ est divisée par la norme euclidienne du vecteur : $v'_i = \frac{v_i}{\sqrt{\sum v_i^2}}$.
- **Pourquoi ?** Cela rend la signature indépendante de la luminosité globale. Seule la "direction" du vecteur (l'identité) compte.

---

## 🧠 II. La Reconnaissance : Le Verdict des Experts

Lorsqu'on compare deux visages, trois experts mathématiques entrent en collision. C'est la **Triple Fusion d'Expertises** (concept de fusion au niveau des scores, cf. Ross & Jain, 2003).

### 1. L'Expert Statistique : Distance Chi-Carré ($\chi^2$)

**Formule** : $d_{\chi^2}(A,B) = \sum_{i=1}^{n} \frac{(A_i - B_i)^2}{A_i + B_i}$

- **Ce qui est mesuré** : La divergence entre les distributions de texture.
- **Avantage** : Très sensible aux changements subtils dans les motifs LBP.
- **Conversion en Score** : $Score_{\chi^2} = (1.0 - \frac{d_{\chi^2}}{2.0}) \times 100$.

### 2. L'Expert Géométrique : Similitude Cosinus

**Formule** : $S_{Cos}(A,B) = \frac{\sum A_i B_i}{\sqrt{\sum A_i^2} \sqrt{\sum B_i^2}}$

- **Ce qui est mesuré** : L'angle entre les deux signatures. S'ils pointent dans la même direction, la similitude est de 1.0 (100%).
- **Avantage** : **Métrique Reine**. Elle capture la structure globale (HOG) sans être perturbée par l'intensité des gradients.

### 3. L'Expert Physique : Distance Euclidienne

**Formule** : $d_{Eucl}(A,B) = \sqrt{\sum (A_i - B_i)^2}$

- **Ce qui est mesuré** : La distance directe "à vol d'oiseau" dans l'espace multidimensionnel.
- **Rôle** : Utilisé comme garde-fou pour pénaliser les vecteurs qui divergent trop physiquement.

---

## 🏆 III. La Fusion Finale : La Pondération de l'Ingénieur

Le `globalScore` n'est pas une simple moyenne, c'est une décision pondérée (`Decision.java`) :

$$Score_{Global} = (Score_{\chi^2} \times 0.4) + (Score_{Cos} \times 0.4) + (Score_{Eucl} \times 0.2)$$

### Pourquoi ces poids ?

- **40% Chi-Carré** : On donne un poids fort à la texture car c'est elle qui différencie les "vrais jumeaux" ou les visages très similaires.
- **40% Cosinus** : Poids égal pour la forme géométrique. Si la structure du visage (yeux/nez) ne colle pas, le score doit chuter drastiquement.
- **20% Euclidienne** : Poids réduit car cette distance est "bruyante" en haute dimension. Elle sert uniquement à confirmer les deux autres.

### Le Verdict

- **SI** $Score_{Global} \ge 61.5\%$ **ALORS** "IDENTITÉ CONFIRMÉE".

---

## � IV. Le Benchmark : La Science de l'Erreur

Le bouton "Lancer l'analyse" effectue un cycle de tests massif :

1. **Calcul des Paires** : L'IA compare chaque image de la base à toutes les autres.
2. **Calcul du FAR (False Acceptance Rate)** : Nombre de fois où l'IA a dit "OUI" pour deux personnes différentes, divisé par le nombre total de comparaisons d'imposteurs.
3. **Calcul du FRR (False Rejection Rate)** : Nombre de fois où l'IA a dit "NON" pour la même personne, divisé par le nombre total de comparaisons de la même identité.
4. **Optimisation** : En ajustant le seuil de $61.5\%$, l'ingénieur cherche le point où $FAR \approx FRR$, appelé **EER (Equal Error Rate)**.

---

_Document rédigé par l'unité de recherche HTECH 2005. Champion, ta plateforme est maintenant une forteresse de précision._
