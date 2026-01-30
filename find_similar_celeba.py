from collections import defaultdict
import os

identity_file = "d:/Dossier Harold/CompFaceModule/identity_CelebA.txt"
bdd_dir = "d:/Dossier Harold/CompFaceModule/src/main/bdd"

# List files in BDD
bdd_files = set(os.listdir(bdd_dir))

identities = defaultdict(list)

try:
    with open(identity_file, "r") as f:
        for line in f:
            parts = line.strip().split()
            if len(parts) == 2:
                img, identity = parts
                identities[identity].append(img)

    # Find groups where at least one image is in BDD
    print("--- Groupes trouvés pour les images dans src/main/bdd ---")
    found_any = False
    for identity, imgs in identities.items():
        # Check if any image in this group is in BDD
        overlap = [img for img in imgs if img in bdd_files]
        if overlap:
            if len(imgs) > 1:
                print(f"ID {identity} (Vraie Identité) :")
                print(f"  Images dans BDD : {', '.join(overlap)}")
                others = [img for img in imgs if img not in bdd_files]
                if others:
                    # Limit output if there are too many
                    print(f"  Autres photos de cette personne (pas dans BDD) : {', '.join(others[:5])}{'...' if len(others) > 5 else ''}")
                print("-" * 30)
                found_any = True
            
    if not found_any:
        print("Aucune image de la BDD n'a de 'sosie' (même ID) répertorié dans ce fichier.")

except Exception as e:
    print(f"Erreur : {e}")
