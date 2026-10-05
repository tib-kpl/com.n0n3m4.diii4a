# RealRTCW en français

Traduction française de RealRTCW 5.44c, sous forme de fichiers texte : aucune modification du code.

- `text/text.txt` : les menus (clé → texte)
- `text/strings.txt` : les messages en jeu (dans l'ordre de l'original anglais, à conserver)
- `text/pickupnames.txt` : les noms des objets ramassés (dans l'ordre de l'original, `---` = nom d'origine)
- `text/bonus_strings.txt` : les récompenses des secrets
- `text/EnglishUSA/` : les textes du carnet (atouts, classes de survie). Le dossier garde son nom anglais,
  c'est celui que lit le jeu.

## Installation

    python build_pk3.py

puis copier `zz_realrtcw_fr.pk3` dans le dossier `main` de RealRTCW (à côté de `z_realrtcw_localization.pk3`).
Son nom le fait charger après les packs du jeu, ses fichiers remplacent donc les anglais.
Le retirer rend le jeu en anglais.

Les fichiers sont en UTF-8 ici et convertis en Latin-1 (le seul jeu de caractères des polices du jeu)
dans le pack : pas de `œ`, de guillemets typographiques ni d'espaces insécables.

Non traduits pour l'instant : les boutons du menu principal (ce sont des images), les sous-titres
(`text/EnglishUSA/maps/`), les briefings et documents trouvés en jeu, le générique.
