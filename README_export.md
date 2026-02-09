Fichiers générés

- target/figures/roc_curve.png : Courbe ROC générée depuis `target/full_benchmark_results.csv`.
- target/figures/score_distribution.png : Histogramme des scores (genuine vs impostor).
- target/figures/roc_info.txt : AUC et meilleur seuil (F1) trouvé.
- target/article_scientifique_face_recognition.pdf : PDF de l'article (généré via Python/reportlab).
- article.tex : Source LaTeX (peut être compilée avec `pdflatex`).
- article_scientifique_face_recognition.txt : Article en texte brut (version complète).

Comment recompiler le PDF localement

1) Si vous préférez compiler `article.tex` en PDF avec LaTeX : installez TeX Live / MikTeX puis exécutez dans le répertoire du projet :

```powershell
cd "d:\Dossier Harold\CompFaceModule"
pdflatex -interaction=nonstopmode -halt-on-error article.tex
```

2) Le PDF généré par `pdflatex` s'appellera `article.pdf` (ou `article.tex` compilé). Si `pdflatex` n'est pas installé, utilisez le PDF déjà produit : `target/article_scientifique_face_recognition.pdf`.

Remarques

- Le PDF final généré automatiquement inclut la figure ROC et l'histogramme, puis le texte complet de l'article. Si vous souhaitez un formatage LaTeX plus professionnel (IEEE/LNCS), je peux générer un template LaTeX adapté.
- Pour régénérer les figures depuis les données CSV en local, utilisez l'environnement Python du projet (`.venv`) et installez : `matplotlib`, `numpy`, `scikit-learn`.

```powershell
cd "d:\Dossier Harold\CompFaceModule"
# activer venv si nécessaire
.\.venv\Scripts\Activate.ps1
python -m pip install matplotlib numpy scikit-learn
python tools/plot_from_csv.py  # (script équivalent utilisé)
```
