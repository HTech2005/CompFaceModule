# Face Comparison Module (HTECH 2005)

Logiciel de reconnaissance faciale haute fidélité utilisant la triple fusion d'expertises (Texture, Structure, Géométrie).

---

## 🚀 Guide Rapide de Lancement

1.  **Prérequis** : Java 17+ et Maven.
2.  **Base de Données** : Placez vos photos de référence dans `src/main/bdd`. Nommez les fichiers par l'identité de la personne (ex: `Elon_Musk_01.jpg`).
3.  **Lancement** :
    ```bash
    mvn javafx:run
    ```

---

## 🛠️ Manuel d'Utilisation Étape par Étape

### 1. CDV : Comparaison de Visages (Mode 1:1)

_Vérifiez si deux photos appartiennent à la même personne._

1. Sélectionnez l'**Image 1** (Cible).
2. Sélectionnez l'**Image 2** (Comparaison).
3. Le système affiche instantanément les scores détaillés des experts.
4. **Interprétation** : Si le score global est vert (> 61.5%), les visages sont considérés comme identiques.

### 2. TR : Reconnaissance Temps Réel (Mode 1:N)

_Identification automatique via Webcam._

1. Activez votre caméra.
2. Placez votre visage dans le **cadre vert** au centre.
3. Le système scanne la base de données en continu.
4. **Validation** :
   - **Accès Immédiat** : Pour les scores > 61.5%.
   - **Stabilité (5s)** : Pour les scores entre 55% et 61.5%. Restez immobile 5 secondes pour valider.

### 3. CV : Analyse Biométrique

_Analyse des traits spécifiques du visage._

1. Chargez une image.
2. Cliquez sur **Analyser**.
3. Observez le dessin des composants (yeux, nez, bouche) et les mesures précises en pixels affichées dans le panneau latéral.

### 4. LAB : Laboratoire de Tests Scientifiques (Benchmark)

_Évaluez les performances globales de l'algorithme._

1. Cliquez sur **Analyse All:N** pour comparer chaque image de la base avec toutes les autres.
2. Observez les indicateurs de performance se mettre à jour en direct.
3. Exportez les résultats en **CSV** pour un audit externe.

---

## 🔬 Expertise Scientifique & Métriques

Le système utilise la **Recalibration 6.0**, équilibrant sécurité et confort.

### Définition des Métriques du Dashboard

| Métrique                        | Utilité Scientifique  | Ce qu'elle indique                                        |
| :------------------------------ | :-------------------- | :-------------------------------------------------------- |
| **FAR (False Acceptance Rate)** | Sécurité              | Risque qu'un étranger soit accepté par erreur.            |
| **FRR (False Rejection Rate)**  | Confort               | Risque qu'une personne autorisée soit refusée.            |
| **Recall (Rappel)**             | Capacité de détection | % de visages connus que le système a réussi à trouver.    |
| **TNR (Rejet Correct)**         | Spécificité           | Capacité du système à ne pas se tromper sur les inconnus. |
| **Précision**                   | Fiabilité du Verdict  | Probabilité que si le système dit "MATCH", ce soit vrai.  |
| **F1-Score**                    | Score Global          | La moyenne harmonique qui résume la performance totale.   |

### Que déduire des Graphiques ?

- **Confusion Matrix (BarChart)** : Permet de voir visuellement le volume de VP (Vrais Positifs) par rapport aux erreurs (FP/FN).
- **Separability (Distribution)** : Un bon système montre deux "cloches" bien séparées : une pour les imposteurs (bas scores) et une pour les authentiques (hauts scores). Plus elles se chevauchent, plus il y a d'erreurs.
- **ROC Curve (FAR vs FRR)** : La courbe idéale doit "coller" en bas à gauche de l'axe. C'est le graphique de référence pour comparer deux versions de l'IA.

---

## ⚙️ Détails de la Recalibration 6.0 (Logic)

Le verdict final est une fusion pondérée de 3 mesures :

1.  **Texture (40%)** : Utilise le **LBP (Local Binary Patterns)** sur une grille 8x8. Très précis pour les détails fins.
2.  **Structure (40%)** : Utilise la **Similitude Cosinus**. Très robuste aux changements de lunettes et de lumière.
3.  **Géométrie (20%)** : Utilise la **Distance Euclidienne** (Diviseur : 0.065). Mesure l'écart global des caractéristiques.

**Seuil de Décision Final : 61.5%**

---

## 📁 Nettoyage du Projet

Pour garantir la stabilité, seuls les fichiers sources (`src/`), la configuration Maven (`pom.xml`) et cette documentation sont conservés. Tous les fichiers de logs (`.log`), scripts de tests temporaires (`.py`) et résultats intermédiaires ont été supprimés.

---

_Développement par HTECH 2005_
