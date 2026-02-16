# Face Comparison Module (HTECH 2005) - Guide de l'Ingénieur Champion

Ce document constitue la référence technique absolue du module. Il détaille la synergie entre le prétraitement d'image, l'extraction de signatures biométriques et la **Triple Fusion d'Expertises** (LBP + Histogramme d'Intensité) pour la prise de décision.

---

## 🏗️ I. Les Flux de Travail (Workflows)

La plateforme gère deux modes opératoires distincts, chacun optimisé pour un cas d'usage spécifique.

### 1. Comparaison de Visages (CDV - Mode Statique)

Ce mode compare deux fichiers images existants.

1. **Chargement Dual** : Les fichiers A et B sont chargés en mémoire sous forme de matrices (`org.bytedeco.opencv.opencv_core.Mat`).
2. **Détection Indépendante** : `FaceDetection.detectFace()` est appelé sur chaque image.
   - _Calcul_ : L'IA identifie les régions d'intérêt (ROI). Si plusieurs visages existent, elle calcule la surface maximale pour isoler le sujet.
3. **Extraction de Signature** : Transformation des pixels en vecteurs de caractéristiques (**LBP + Intensité**).
4. **Calcul de Distance** : Confrontation directe des deux vecteurs via les trois experts (Chi2, Cos, Eucl).
5. **Verdict** : Pondération des scores et affichage des métriques de compatibilité.

### 2. Reconnaissance en Temps Réel (Mode Dynamique)

Ce mode traite un flux vidéo continu.

1. **Frame Grabbing** : Capture d'une image depuis la webcam via JavaFX.
2. **Détection "On-the-fly"** : Localisation du visage. Si aucun visage n'est détecté, le cycle s'arrête immédiatement (économie CPU).
3. **Indexation Éclair** : Extraction de la signature (LBP + Intensité) du visage détecté.
4. **Top-K Search** : Le système compare cette "signature live" à l'intégralité de la base de données (`databaseFeatures`).
5. **Score de Crédibilité** : Seul le meilleur match (`bestScore`) ayant un score supérieur au seuil (`THRESHOLD`) est retenu. Si le score est trop bas, le système affiche "Inconnu".

---

## 🧠 II. Analyse Comparative des Métriques de Distance

Le système utilise une **Fusion au niveau des Scores (Score-Level Fusion)** entre trois métriques calculées sur les histogrammes fusionnés.

### 1. Distance Chi-Carré ($\chi^2$) - L'Expert Statistique

- **Usage** : Idéal pour les histogrammes.
- **Formule** : $\sum \frac{(A_i - B_i)^2}{A_i + B_i}$
- **Avantages** : Très sensible aux variations de distribution. Elle accorde plus de poids aux bins ayant de faibles valeurs, capturant ainsi des traits d'identité subtils dans la texture.
- **Limites** : Nécessite des vecteurs normalisés (somme = 1). Géré par notre garde-fou $1e-10$ contre la division par zéro.

### 2. Similitude Cosinus - L'Expert d'Orientation

- **Usage** : Mesure l'angle entre deux vecteurs de caractéristiques.
- **Formule** : $\frac{A \cdot B}{\|A\| \|B\|}$
- **Avantages** : **Invariance d'Échelle**. Capture la "direction" de la signature, ce qui aide à rester constant malgré les ombres portées.
- **Limites** : Ignore la magnitude absolue du signal.

### 3. Distance Euclidienne ($L_2$) - L'Expert Géométrique

- **Usage** : Distance physique directe.
- **Formule** : $\sqrt{\sum (A_i - B_i)^2}$
- **Avantages** : Mesure l'écart quadratique moyen. Simple et intuitive.
- **Limites** : Devient moins discriminante seule dans des espaces de très haute dimension (notre vecteur fusionné).

---

## 🎓 III. Fondements Scientifiques et Triple Fusion

### La Triple Fusion d'Expertises

Ce concept s'appuie sur la théorie de la **Fusion de Classificateurs Multiples (Kittler et al., 1998)** et la **Fusion Multimodale de Ross & Jain (2003)**.

**Pourquoi cette architecture ?**

1. Le $\chi^2$ valide la **Texture fine** (LBP).
2. Le Cosinus valide la **Corrélation globale** de l'image.
3. L'Euclidienne valide la **Cohérence Physique**.

**Justification des Poids (40/40/20) :**

- Les **40% au Chi-Carré** et **40% au Cosinus** assurent que la texture et l'orientation dominent la décision.
- Les **20% d'Euclidienne** agissent comme une régularisation pour éviter les scores aberrants.

### Bibliographie de Référence

1. **Ross, A., & Jain, A. (2003)**. "Information Fusion in Biometrics". _Pattern Recognition Letters_. Base de notre fusion de scores dans `Decision.java`.
2. **Kittler, J., et al. (1998)**. "On Combining Classifiers". _IEEE T-PAMI_. Justification de la règle de somme pondérée.
3. **Ojala, T., et al. (2002)**. "Multiresolution gray-scale and rotation invariant texture classification with local binary patterns". _IEEE T-PAMI_. Fondement scientifique de `LBP.java`.
4. **Pizer, S. M., et al. (1987)**. "Adaptive histogram equalization and its variations". Fondement du CLAHE utilisé dans `Pretraitement.java`.

---

_HTECH 2005 - L'ingénierie au service de la précision. (Baseline: LBP + Intensité)_
