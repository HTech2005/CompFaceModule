# Face Comparison Module (HTECH 2005) - Documentation Technique Profonde

Ce document détaille l'architecture algorithmique et les choix mathématiques derrière la plateforme de reconnaissance faciale.

---

## 🛠️ Pipeline de Traitement : Du Pixel au Verdict (Mise à jour PCA)

Le système a évolué vers une architecture hybride **LBP + HOG + PCA** pour maximiser la robustesse et la rapidité.

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
2. **Redimensionnement (128x128)** : Fixe une résolution standard.
3. **Flou Gaussien ($\sigma=0.8$)** : Lisse les micro-défauts de capteur.
4. **CLAHE (Contrast Limited Adaptive Histogram Equalization)** :
   - **Formule** : Améliore le contraste localement sur des blocs de 8x8 pixels.
   - **Pourquoi ?** Contrairement à une égalisation globale, le CLAHE empêche la surexposition. Il permet de voir les détails dans les zones d'ombre (ex: sous une casquette).

### 3. Extraction de Caractéristiques (Signature Biométrique)

_Composants : `LBP.java`, `HOG.java`, `FaceService.java`_

Nous utilisons une **double extraction** (Texture + Forme) suivie d'une réduction de dimensionnelle.

#### A. Expert Texture : LBP (Local Binary Patterns)

- **Rôle** : Capture la micro-texture de la peau (rides, grain).
- **Vecteur** : Histogramme concaténé (16 384 dimensions).

#### B. Expert Forme : HOG (Histogram of Oriented Gradients)

- **Rôle** : Capture les contours principaux et la géométrie du visage (Yeux, Nez, Bouche).
- **Vecteur** : Histogramme de gradients (~10 000 dimensions).

#### C. Fusion et Compression (PCA) `[NOUVEAU]`

Les vecteurs LBP et HOG sont concaténés (>26 000 dimensions) puis projetés par **Analyse en Composantes Principales (PCA)**.

- **Vecteur Final** : **128** valeurs flottantes (Composantes principales).
- **Normalisation L2** : Les vecteurs sont normalisés pour que $\|\vec{v}\| = 1$ (Sphère unitaire).
- **Avantage** : Comparaison 100x plus rapide et élimination du bruit non corrélé.

### 4. Triple Expertise Mathématique (Décision)

_Composants : `Comparaison.java`, `Decision.java`_

Le verdict final est une fusion pondérée de trois mesures de distance sur les vecteurs PCA :

| Expert                   | Formule                                | Poids   | Rôle (Espace PCA)                                    |
| :----------------------- | :------------------------------------- | :------ | :--------------------------------------------------- |
| **Chi-Carré ($\chi^2$)** | $\sum \frac{(A_i - B_i)^2}{A_i + B_i}$ | **40%** | Mesure la divergence statistique (sur valeurs abs).  |
| **Cosinus ($Cos$)**      | $\frac{A \cdot B}{\|A\| \|B\|}$        | **40%** | **Métrique Reine** pour les vecteurs PCA normalisés. |
| **Euclidienne ($d$)**    | $\sqrt{\sum (A_i - B_i)^2}$            | **20%** | Écart géométrique pur dans l'espace latent.          |

**Fusion Finale (Configuration Personnalisée)** :
$$Score = (Score_{\chi^2} \times 0.4) + (Score_{Cos} \times 0.4) + (Score_{Eucl} \times 0.2)$$

- **Seuil de Verdict** : **61.5%**.

---

## 📊 Laboratoire de Tests Scientifiques

Le module de tests permet de déduire la performance réelle de l'algorithme sur une base de données.

### Déduction des Tableaux & Résultats

- **VP (Vrai Positif)** : L'IA a dit "MATCH" et c'était la bonne personne.
- **VN (Vrai Négatif)** : L'IA a dit "NON" et c'était bien un inconnu (Rejet correct).
- **FP (Faux Positif)** : **Danger !** L'IA a accepté un imposteur. _Remède : Augmenter le seuil._
- **FN (Faux Négatif)** : **Frustration !** L'IA a rejeté un accès légitime. _Remède : Baisser le seuil._

### 🔬 Mesures de Performance Scientifiques

| Métrique                     | Formule Mathématique                | Interprétation                                                              |
| :--------------------------- | :---------------------------------- | :-------------------------------------------------------------------------- |
| **FAR** (Fausse Acceptation) | $FAR = \frac{FP}{FP + VN}$          | **Sécurité** : Risque qu'un intrus soit accepté.                            |
| **FRR** (Faux Rejet)         | $FRR = \frac{FN}{FN + VP}$          | **Confort** : Risque qu'un utilisateur légitime soit rejeté.                |
| **EER** (Equal Error Rate)   | $T$ tel que $FAR(T) \approx FRR(T)$ | **Point d'Équilibre** : Plus l'EER est bas, plus le système est performant. |

> [!IMPORTANT]
>
> - Dans un contexte de **haute sécurité** (banque, coffre), on privilégie un FAR très bas, quitte à avoir un FRR un peu plus haut.
> - Dans un contexte de **confort** (déverrouillage téléphone), on privilégie un FRR bas pour ne pas frustrer l'utilisateur.

_Ce document technique est maintenu par HTECH 2005._
