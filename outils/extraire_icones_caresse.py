"""Transforme la planche 6 x 5 des defis de caresse en atlas Minecraft.

La planche produite par le generateur contient un damier gris peint dans le
PNG, et non une vraie transparence. Ce script l'enleve, recadre chaque cellule,
ramene le dessin sur une grille 24 x 24 et reconstruit un atlas RGBA propre.
"""

from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image


COLONNES = 6
LIGNES = 5
TAILLE = 24
MARGE = 1


def est_damier(pixel: tuple[int, int, int]) -> bool:
    """Le fond est un gris clair presque neutre ; les contours sont bruns."""
    rouge, vert, bleu = pixel
    return max(pixel) >= 160 and max(pixel) - min(pixel) <= 18


def rendre_transparent(cellule: Image.Image) -> Image.Image:
    source = cellule.convert("RGB")
    sortie = Image.new("RGBA", source.size, (0, 0, 0, 0))
    pixels_source = source.load()
    pixels_sortie = sortie.load()
    for y in range(source.height):
        for x in range(source.width):
            couleur = pixels_source[x, y]
            if not est_damier(couleur):
                pixels_sortie[x, y] = (*couleur, 255)
    return sortie


def pixeliser(cellule: Image.Image) -> Image.Image:
    boite = cellule.getbbox()
    if boite is None:
        raise ValueError("Cellule vide apres suppression du damier")
    dessin = cellule.crop(boite)
    place = TAILLE - MARGE * 2
    echelle = min(place / dessin.width, place / dessin.height)
    largeur = max(1, round(dessin.width * echelle))
    hauteur = max(1, round(dessin.height * echelle))

    # Le premier passage garde les petits details, puis la quantification rend
    # les aplats nets. L'alpha final est strictement binaire.
    dessin = dessin.resize((largeur, hauteur), Image.Resampling.LANCZOS)
    alpha = dessin.getchannel("A").point(lambda a: 255 if a >= 96 else 0)
    couleurs = dessin.convert("RGB").quantize(
        colors=24, method=Image.Quantize.FASTOCTREE
    ).convert("RGB")
    couleurs.putalpha(alpha)

    icone = Image.new("RGBA", (TAILLE, TAILLE), (0, 0, 0, 0))
    icone.alpha_composite(
        couleurs, ((TAILLE - largeur) // 2, (TAILLE - hauteur) // 2)
    )
    return icone


def extraire(source: Path, destination: Path, individuels: Path | None) -> None:
    planche = Image.open(source).convert("RGB")
    if planche.width % COLONNES or planche.height % LIGNES:
        raise ValueError(
            f"Planche {planche.size}: dimensions non divisibles par "
            f"{COLONNES} x {LIGNES}"
        )
    cellule_largeur = planche.width // COLONNES
    cellule_hauteur = planche.height // LIGNES
    atlas = Image.new(
        "RGBA", (COLONNES * TAILLE, LIGNES * TAILLE), (0, 0, 0, 0)
    )
    if individuels is not None:
        individuels.mkdir(parents=True, exist_ok=True)

    numero = 0
    for ligne in range(LIGNES):
        for colonne in range(COLONNES):
            cellule = planche.crop(
                (
                    colonne * cellule_largeur,
                    ligne * cellule_hauteur,
                    (colonne + 1) * cellule_largeur,
                    (ligne + 1) * cellule_hauteur,
                )
            )
            icone = pixeliser(rendre_transparent(cellule))
            atlas.alpha_composite(icone, (colonne * TAILLE, ligne * TAILLE))
            numero += 1
            if individuels is not None:
                icone.save(individuels / f"caresse_{numero:02d}.png")

    destination.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(destination)
    assert atlas.size == (144, 120)
    assert set(atlas.getchannel("A").get_flattened_data()) == {0, 255}
    print(f"30 icones -> {destination} ({atlas.width} x {atlas.height}, RGBA)")


def main() -> None:
    analyseur = argparse.ArgumentParser()
    analyseur.add_argument("source", type=Path)
    analyseur.add_argument("destination", type=Path)
    analyseur.add_argument("--individuels", type=Path)
    arguments = analyseur.parse_args()
    extraire(arguments.source, arguments.destination, arguments.individuels)


if __name__ == "__main__":
    main()
