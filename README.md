# Face Comparison Module (HTECH 2005) - Documentation Technique Profonde

Ce document détaille l'architecture algorithmique et les choix mathématiques derrière la plateforme de reconnaissance faciale.

---

## 🛠️ Pipeline de Traitement : Du Pixel au Verdict

Le système suit un pipeline rigoureux divisé en quatre phases majeures. Chaque choix a été optimisé pour la robustesse (gestion des lunettes, éclairage variable).

### 1. Détection et Normalisation Géométrique

_Composant : `FaceDetection.java`_

- **Algorithme** : Cascade de Classificateurs de Haar (`haarcascade_frontalface_default.xml`).
- **Logique de Sélection** : En cas de multiples visages, le système verrouille le **plus grand** (supposé être l'utilisateur principal).
- **Internal Crop (Padding 15%)** :
  - **Pourquoi ?** Nous appliquons un recadrage interne de 15% sur les bords du rectangle détecté.
  - **But** : Éliminer le bruit de fond, les cheveux et les oreilles qui sont des variables non fiables pour la reconnaissance pure, afin de se concentrer sur le "T-Zone" du visage (yeux, nez, bouche).

### 2. Prétraitement (Image Enhancement)

_Composant : `Pretraitement.java`_

Pour que l'IA "voie" la même chose peu importe l'environnement, l'image subit une transformation lourde :

1. **Conversion en Gris** : Élimine les biais liés à la balance des blancs des caméras.
2. **Redimensionnement (128x128)** : Fixe une résolution standard indispensable pour la grille de caractéristiques.
3. **Filtre Médian** : Supprime le bruit poivre et sel. **Choix critique** : aide à atténuer les reflets sur les montures de lunettes fines.
4. **Flou Gaussien ($\sigma=0.8$)** : Lisse les micro-défauts de capteur.
5. **CLAHE (Contrast Limited Adaptive Histogram Equalization)** :
   - **Formule** : Améliore le contraste localement sur des blocs de 8x8 pixels.
   - **Pourquoi ?** Contrairement à une égalisation globale, le CLAHE empêche la surexposition. Il permet de voir les détails dans les zones d'ombre (ex: sous une casquette).

### 3. Extraction de Caractéristiques (Signature Biométrique)

_Composants : `LBP.java`, `Histogram.java`, `Fusion.java`_

Nous utilisons une approche par **Grille de 8x8 blocs** (64 sous-régions de 16x16 pixels).

#### A. Expert Texture : LBP (Local Binary Patterns)

Pour chaque pixel $P_c$ d'un bloc, on compare son intensité à ses 8 voisins $P_i$ :
$$LBP(P_c) = \sum_{i=0}^{7} s(P_i - P_c) 2^i$$ où $s(x) = 1$ si $x \geq 0$ et $0$ sinon.

- **Vecteur** : Un histogramme de 256 valeurs par bloc.
- **Pourquoi ?** Invariant aux changements globaux de lumière, capture la signature unique de la peau.

#### B. Expert Structure : Grille d'Histogrammes

- Calcule la distribution des niveaux de gris dans chaque bloc.
- **Pourquoi ?** Capture la morphologie (formes sombres des yeux, clarté du front).

#### C. Fusion et Taille du Vecteur Final

Les 64 blocs LBP (256 bins $\times$ 64 = 16 384) sont concaténés aux 64 blocs d'histogrammes (256 bins $\times$ 64 = 16 384).

- **Taille du Vecteur** : **32 768** valeurs flottantes.
- **Normalisation L1** : Les vecteurs sont normalisés pour que $\sum |v_i| = 1$.

### 4. Triple Expertise Mathématique (Décision)

_Composants : `Comparaison.java`, `Decision.java`_

Le verdict final est une fusion pondérée de trois mesures de distance :

| Expert                   | Formule                                | Poids   | Rôle                                                 |
| :----------------------- | :------------------------------------- | :------ | :--------------------------------------------------- |
| **Chi-Carré ($\chi^2$)** | $\sum \frac{(A_i - B_i)^2}{A_i + B_i}$ | **40%** | Analyse la texture fine.                             |
| **Cosinus ($Cos$)**      | $\frac{A \cdot B}{\|A\| \|B\|}$        | **40%** | Analyse la structure globale (robuste aux lunettes). |
| **Euclidienne ($d$)**    | $\sqrt{\sum (A_i - B_i)^2}$            | **20%** | Mesure l'écart géométrique pur.                      |

**Fusion Finale (Recalibration 6.0)** :
$$Score = (Score_{\chi^2} \times 0.4) + (Score_{Cos} \times 0.4) + (Score_{Eucl} \times 0.2)$$

- **Diviseur Euclidien** : 0.065 (Choisi empiriquement pour équilibrer la sévérité).
- **Seuil de Verdict** : **61.5%**.

---

## 📊 Laboratoire de Tests Scientifiques

Le module de tests permet de déduire la performance réelle de l'algorithme sur une base de données.

### Déduction des Tableaux & Résultats

- **VP (Vrai Positif)** : L'IA a dit "MATCH" et c'était la bonne personne.
- **VN (Vrai Négatif)** : L'IA a dit "NON" et c'était bien un inconnu (Rejet correct).
- **FP (Faux Positif)** : **Danger !** L'IA a accepté un imposteur. _Remède : Augmenter le seuil._
- **FN (Faux Négatif)** : **Frustration !** L'IA a rejeté un accès légitime. _Remède : Baisser le seuil ou détendre le diviseur Euclidien._

### Métriques d'Évaluation

1. **Recall/Rappel ($\frac{VP}{VP+FN}$)** : Capacité à "rappeler" les personnes connues.
2. **TNR/Spécificité ($\frac{VN}{VN+FP}$)** : Capacité à rejeter les inconnus.
3. **F1-Score** : Moyenne harmonique entre Précision et Rappel. Si ce score est bas, c'est que soit le système est trop laxiste, soit il est trop sévère.

### Interpréation des Graphiques

- **Separability** : Si les deux courbes (Authentiques vs Imposteurs) sont séparées par un vide, le système est stable.
- **ROC Curve** : La performance optimale se situe là où la courbe est la plus proche du coin idéal.

---

_Ce document technique est maintenu par HTECH 2005._
