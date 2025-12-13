# CompFaceModule - Système de Reconnaissance et Analyse Faciale

Ce projet est une solution complète de biométrie faciale intégrant un Backend Java (OpenCV, SparkJava) et un Frontend React moderne. Il utilise des méthodes de vision par ordinateur classiques (LBP + Histogrammes) pour une détection rapide et sans GPU.

## 🚀 Fonctionnalités Principales

### 1. Comparaison de Visages (CDV)
*   **Interface** : `/cdv-compare`
*   **Fonction** : Compare deux images uploadées.
*   **Sortie** : Scores de similarité (Euclidien & Cosinus) et verdict.
*   **Seuil** : Strict (Distance < 0.25 soit **75%** de similarité minimale).

### 2. Analyse Temps Réel (SV - Scanner Visage)
*   **Interface** : `/sv-analysis`
*   **Fonction** : Analyse un flux webcam en 1280x720.
*   **Métriques** : Largeur du visage, distance inter-oculaire, dimensions de la bouche.
*   **Afffichage** : Jauges dynamiques et mesures en pixels.

### 3. Identification Temps Réel (TR)
*   **Interface** : `/tr-recognition`
*   **Fonction** : Identifie une personne en direct via la webcam.
*   **Processus** :
    1.  Capture vidéo et détection faciale.
    2.  Envoi au backend `/api/search`.
    3.  Comparaison instantanée avec la base de données (`src/main/bdd`).
*   **Feedback** : Overlay de visée, Timer de 10s, Badge d'accès (Autorisé/Refusé).

---

## 🛠 Architecture Technique

### Backend (Java)
*   **Framework** : SparkJava (Micro-serveur HTTP).
*   **Vision** : OpenCV (via JavaCV).
*   **Algorithme** :
    1.  **Détection** : Haar Cascades (Viola-Jones).
    2.  **Extraction** : Histogrammes de niveaux de gris + Local Binary Patterns (LBP).
    3.  **Fusion** : Vecteur unique normalisé.
    4.  **Comparaison** : Distance Euclidienne & Similarité Cosinus.
*   **Base de Données** : Charge les images de `src/main/bdd` en mémoire au démarrage pour une recherche ultra-rapide.

### Frontend (React)
*   **Design** : Interface "Glassmorphism" moderne (fonds sombres, flou, néons).
*   **Navigation** : React Router v6.
*   **Composants** :
    *   `TRRecognition` : Logique de timer et overlay.
    *   `SVAnalysis` : Tableaux de bord de métriques.
    *   `CDVCompare` : Drag & drop et visualisations.

---

## ⚡ Installation et Lancement

### 1. Démarrer le Backend
Le serveur API Java doit être lancé en premier. Il écoute sur le port **4567**.

```bash
mvn exec:java "-Dexec.mainClass=tech.HTECH.APIServer"
```
*Note : Assurez-vous d'avoir des images dans `src/main/bdd` pour que l'identification fonctionne.*

### 2. Démarrer le Frontend
Dans un nouveau terminal, lancez l'application React (Port 3000).

```bash
cd frontend
npm start
```

---

## ⚙️ Configuration des Seuils

Le système est configuré pour une sécurité équilibrée :

*   **Seuil de Décision** : **75%** (Distance < 0.25).
*   **Tolérance** : Ajustée pour accepter les légères variations (éclairage, angle) tout en rejetant les imposteurs.
*   **Fichiers Clés** :
    *   `Decision.java` : Logique booléenne de validation.
    *   `APIServer.java` : Logique de l'API de recherche.

---

## 📊 Performance Estimée

*   **Robustesse** : ~80-90% en conditions contrôlées.
*   **Vitesse** : Traitement < 200ms par image (CPU standard).
*   **Limitations** : Sensible aux fortes contre-jours et rotations extrêmes (>20°).

---

© 2024 Tech HTECH - Module de Compétition Faciale
