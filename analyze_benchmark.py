import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import os
import sys

def analyze_csv(file_path):
    if not os.path.exists(file_path):
        print(f"Erreur : Le fichier {file_path} n'existe pas.")
        return

    # Lecture du CSV (séparateur point-virgule)
    df = pd.read_csv(file_path, sep=';')
    
    # Remplacer les virgules par des points pour les colonnes numériques si nécessaire
    cols = ['Chi2_%', 'Eucl_%', 'Cos_%', 'Global_%']
    for col in cols:
        if df[col].dtype == object:
            df[col] = df[col].str.replace(',', '.').astype(float)

    print("\n--- RÉSUMÉ SCIENTIFIQUE ---")
    print(df.groupby('Statut_Scientifique')['Global_%'].agg(['count', 'mean', 'std']))

    # 1. Distribution des scores Globaux
    plt.figure(figsize=(10, 6))
    sns.histplot(data=df, x='Global_%', hue='Statut_Scientifique', kde=True, bins=20)
    plt.axvline(x=60, color='red', linestyle='--', label='Seuil Actuel (60%)')
    plt.title("Distribution des Scores Globaux (Vibrations de l'Identité)")
    plt.xlabel("Score Global (%)")
    plt.ylabel("Fréquence")
    plt.legend()
    plt.savefig("distribution_scores.png")
    print("Graphique de distribution sauvegardé : distribution_scores.png")

    # 2. Matrice de Confusion simplifiée
    confusion = pd.crosstab(df['Statut_Scientifique'].str.split(' ').str[0], df['Decision'])
    print("\n--- MATRICE DE CONFUSION ---")
    print(confusion)

    # 3. Calcul FAR / FRR
    total_impostors = len(df[df['Statut_Scientifique'].str.startswith('V N') | df['Statut_Scientifique'].str.startswith('FP')])
    total_genuines = len(df[df['Statut_Scientifique'].str.startswith('VP') | df['Statut_Scientifique'].str.startswith('FN')])
    
    fps = len(df[df['Statut_Scientifique'].str.startswith('FP')])
    fns = len(df[df['Statut_Scientifique'].str.startswith('FN')])

    far = (fps / total_impostors) * 100 if total_impostors > 0 else 0
    frr = (fns / total_genuines) * 100 if total_genuines > 0 else 0

    print(f"\nFAR (Taux de Fausse Acceptation) : {far:.2f}%")
    print(f"FRR (Taux de Faux Rejet)         : {frr:.2f}%")
    
    plt.show()

if __name__ == "__main__":
    if len(sys.argv) > 1:
        analyze_csv(sys.argv[1])
    else:
        print("Usage: python analyze_benchmark.py <nom_du_fichier.csv>")
