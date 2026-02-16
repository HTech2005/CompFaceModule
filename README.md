# Face Comparison Module (HTECH 2005) - Architecture des Systèmes de Vision Experts

Ce document constitue la référence technique absolue du module. Il détaille la synergie entre le prétraitement d'image, l'extraction de signatures biométriques et la **Triple Fusion d'Expertises** pour la prise de décision.

---

## 🏗️ I. Les Flux de Travail (Workflows)

La plateforme gère deux modes opératoires distincts, chacun optimisé pour un cas d'usage spécifique.

### 1. Comparaison de Visages (CDV - Mode Statique)

Ce mode compare deux fichiers images existants.

1. **Chargement Dual** : Les fichiers A et B sont chargés en mémoire sous forme de matrices (`org.bytedeco.opencv.opencv_core.Mat`).
2. **Détection Indépendante** : `FaceDetection.detectFace()` est appelé sur chaque image.
   - _Calcul_ : L'IA identifie les régions d'intérêt (ROI). Si plusieurs visages existent, elle calcule la surface maximale pour isoler le sujet.
3. **Extraction de Signature** : Transformation des pixels en vecteurs de caractéristiques (LBP + Intensité).
4. **Calcul de Distance** : Confrontation directe des deux vecteurs via les trois experts (Chi2, Cos, Eucl).
5. **Verdict** : Pondération des scores et affichage des métriques de compatibilité.

### 2. Reconnaissance en Temps Réel (Mode Dynamique)

Ce mode traite un flux vidéo continu.

1. **Frame Grabbing** : Capture d'une image toutes les $N$ millisecondes depuis la webcam via JavaFX.
2. **Détection "On-the-fly"** : Localisation du visage en temps réel. Si aucun visage n'est détecté, le cycle s'arrête immédiatement (économie CPU).
3. **Indexation Éclair** : Extraction de la signature du visage détecté dans le flux.
4. **Top-K Search** : Le système compare cette "signature live" à l'intégralité de la base de données (`databaseFeatures`).
5. **Score de Crédibilité** : Seul le meilleur match (`bestScore`) ayant un score supérieur au seuil (`THRESHOLD`) est retenu. Si le score est trop bas, le système affiche "Inconnu".

---

## 🧠 II. Analyse Comparative des Métriques de Distance

Le système ne se contente pas d'une distance, il utilise une **Fusion au niveau des Scores (Score-Level Fusion)**.

### 1. Distance Chi-Carré ($\chi^2$) - L'Expert Statistique

- **Usage** : Idéal pour les histogrammes (LBP et Intensité).
- **Formule** : $\sum \frac{(A_i - B_i)^2}{A_i + B_i}$
- **Avantages** : Très sensible aux variations de distribution. Elle accorde plus de poids aux bins d'histogrammes ayant de faibles valeurs, capturant ainsi des traits d'identité subtils.
- **Limites** : Elle est non symétrique et nécessite que les vecteurs soient normalisés (somme = 1). Elle est sensible au "bruit de zéro" (géré par notre garde-fou $1e-10$).

### 2. Similitude Cosinus - L'Expert d'Orientation

- **Usage** : Mesure l'angle entre deux vecteurs.
- **Formule** : $\frac{A \cdot B}{\|A\| \|B\|}$
- **Avantages** : **Invariance d'Échelle**. Si l'image est plus sombre ou plus claire (multiplication constante des intensités), l'angle reste le même. Elle capture la "direction" de l'identité.
- **Limites** : Ignore totalement la magnitude (l'énergie) du signal. Deux visages avec les mêmes motifs mais des contrastes radicalement opposés pourraient être jugés proches.

### 3. Distance Euclidienne ($L_2$) - L'Expert Géométrique

- **Usage** : Distance physique directe.
- **Formule** : $\sqrt{\sum (A_i - B_i)^2}$
- **Avantages** : Intuitive et robuste. Elle représente l'écart quadratique moyen.
- **Limites** : "La malédiction de la dimensionnalité". Dans un espace à 32 000 dimensions (notre fusion), les distances ont tendance à se concentrer, perdant leur pouvoir discriminant si elles sont utilisées seules.

---

## � III. Fondements Scientifiques et Triple Fusion

### La Triple Fusion d'Expertises

Ce concept s'appuie sur la théorie de la **Fusion de Classificateurs Multiples (Kittler et al., 1998)** et la **Fusion Multimodale de Ross & Jain (2003)**.

**Pourquoi trois métriques ?**

1. Le $\chi^2$ valide la **Texture fine** (LBP).
2. Le Cosinus valide l'**Invariance** aux conditions de capture.
3. L'Euclidienne valide la **Cohérence Globale**.

**Justification des Poids (40/40/20) :**

- Les 80% accordés au duo $\chi^2 / Cos$ assurent la précision sur les données normalisées (probabilistes).
- Les 20% d'Euclidienne agissent comme un **régularisateur** (Ponce et al., 2006) pour éviter qu'un score de cosinus parfait sur un mauvais sujet ne déclenche un faux positif (FP).

### Bibliographie de Référence

1. **Ross, A., & Jain, A. (2003)**. "Information Fusion in Biometrics". _Pattern Recognition Letters_. C'est l'article de référence pour la fusion au niveau des scores utilisée dans `Decision.java`.
2. **Kittler, J., et al. (1998)**. "On Combining Classifiers". _IEEE T-PAMI_. Justifie l'utilisation de règles de somme pondérée pour augmenter la robustesse.
3. **Ojala, T., et al. (2002)**. "Multiresolution gray-scale and rotation invariant texture classification with local binary patterns". _IEEE T-PAMI_. Base scientifique de notre composant `LBP.java`.
4. **Dalal, N., & Triggs, B. (2005)**. "Histograms of Oriented Gradients for Human Detection". _CVPR_. (Cité pour la structure des gradients, bien qu'on utilise l'intensite simple pour la robustesse actuelle).

---

_HTECH 2005 - L'ingénierie au service de la sécurité._
