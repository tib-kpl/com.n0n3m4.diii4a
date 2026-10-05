# RealRTCW : langues

Traductions de RealRTCW 5.44c sous forme de fichiers, choisies dans les options du jeu
(Options > Système > Son > Langue). Le choix est la cvar `cl_language` de RTCW (0 anglais, 1 français),
il recharge aussitôt menus et textes.

Avec une langue choisie, le moteur cherche d'abord chaque texte ou image de menu sous `lang/<code>/`
(`lang/fr/text/text.txt`…) et prend le fichier anglais quand la traduction n'existe pas.

- `pack/` : ce qui sert à toutes les langues, à la racine du pack (les menus avec l'option Langue)
- `fr/` : les fichiers français, rangés sous `lang/fr/` dans le pack
  - `text/text.txt` : les menus (clé → texte)
  - `text/strings.txt` : les messages en jeu (dans l'ordre de l'original anglais, à conserver)
  - `text/pickupnames.txt` : les noms des objets ramassés (dans l'ordre de l'original, `---` = nom d'origine)
  - `text/bonus_strings.txt` : les récompenses des secrets
  - `text/EnglishUSA/` : sous-titres, carnet, briefings, documents, générique (le dossier garde le nom
    que lit le jeu)
  - `ui/assets/` : les boutons des menus (images), faits par `images/make_images.py` à partir des
    images anglaises du jeu, avec les textes de `images/images_fr.py`

## Images des menus

Les boutons du menu sont des images avec le texte incrusté. Pour les refaire (après une mise à jour de
RealRTCW, ou pour changer un texte dans `images/images_fr.py`) :

    pip install pillow numpy opencv-python-headless
    python images/make_images.py <dossier main de RealRTCW>

Le texte anglais est effacé (reconstruit à partir du bandeau autour) et le français dessiné à la même
place : les titres avec la police du jeu, les phrases d'aide avec Bahnschrift (Windows).

## Construction

    python build_pk3.py

donne `zzz_realrtcw_lang.pk3`. La CI le met dans l'APK, qui le copie dans le dossier `Main` du jeu au
lancement.

À chaque changement des traductions, augmenter `REALRTCW_LANGUAGE_PACK_VERSION`
(`Q3EGameConstants.java`) pour que l'APK réinstalle le pack.

Les fichiers texte sont en UTF-8 ici et convertis en Latin-1 (le seul jeu de caractères des polices du
jeu) dans le pack : pas de `œ`, de guillemets typographiques ni d'espaces insécables.
