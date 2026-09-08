// Fabrique la police d'icones du mod : un PNG et son fichier de police.
//
// Minecraft teinte les glyphes par la couleur du texte. On dessine donc en
// BLANC : la meme icone prend l'or du niveau, le rouge de l'alerte ou l'encre
// du parchemin, sans qu'on ait a en dessiner trois.
const fs = require("fs");
const zlib = require("zlib");
const path = require("path");

const TAILLE = 8;
const BS = String.fromCharCode(92);

// '.' = rien, 'X' = blanc plein, 'o' = blanc a moitie (l'ombre).
const GLYPHES = [
	["coeur", [
		".XX..XX.",
		"XXXXXXXX",
		"XXXXXXXX",
		"XXXXXXXX",
		".XXXXXX.",
		"..XXXX..",
		"...XX...",
		"........",
	]],
	["faim", [
		"........",
		"........",
		"XXXXXXXX",
		".XXXXXX.",
		".XXXXXX.",
		"..XXXX..",
		"...XX...",
		"........",
	]],
	["energie", [
		"....XX..",
		"...XX...",
		"..XX....",
		".XXXXX..",
		"...XX...",
		"..XX....",
		".XX.....",
		"........",
	]],
	["etoile", [
		"...XX...",
		"...XX...",
		"..XXXX..",
		"XXXXXXXX",
		"XXXXXXXX",
		"..XXXX..",
		"...XX...",
		"...XX...",
	]],
	["patte", [
		".XX..XX.",
		".XX..XX.",
		"XX....XX",
		"XX....XX",
		"..XXXX..",
		".XXXXXX.",
		".XXXXXX.",
		"..XXXX..",
	]],
	["plume", [
		"......XX",
		".....XXX",
		"....XXX.",
		".o.XXX..",
		".oXXX...",
		".XXX....",
		".XX.....",
		".X......",
	]],
	["soin", [
		"..XXXX..",
		"..XXXX..",
		"XXXXXXXX",
		"XXXXXXXX",
		"XXXXXXXX",
		"XXXXXXXX",
		"..XXXX..",
		"..XXXX..",
	]],
	["couronne", [
		"........",
		"........",
		"X..XX..X",
		"X.XXXX.X",
		"XXXXXXXX",
		"XXXXXXXX",
		"XXXXXXXX",
		"........",
	]],
	["cloche", [
		"...XX...",
		"..XXXX..",
		".XXXXXX.",
		".XXXXXX.",
		"XXXXXXXX",
		"XXXXXXXX",
		"........",
		"...XX...",
	]],
	["parole", [
		"XXXXXXXX",
		"X......X",
		"X......X",
		"X......X",
		"XXXXXXXX",
		".XX.....",
		".X......",
		"........",
	]],
];

// --- La grille ------------------------------------------------------------

const PAR_LIGNE = 5;
const lignes = Math.ceil(GLYPHES.length / PAR_LIGNE);
const largeur = PAR_LIGNE * TAILLE;
const hauteur = lignes * TAILLE;

const pixels = Buffer.alloc(largeur * hauteur * 4, 0);

function poser(colonne, ligne, dessin) {
	for (let y = 0; y < TAILLE; y++) {
		const rangee = dessin[y] || "........";
		for (let x = 0; x < TAILLE; x++) {
			const c = rangee[x] || ".";
			if (c === ".") continue;
			const alpha = c === "o" ? 130 : 255;
			const px = colonne * TAILLE + x;
			const py = ligne * TAILLE + y;
			const i = (py * largeur + px) * 4;
			pixels[i] = 255;
			pixels[i + 1] = 255;
			pixels[i + 2] = 255;
			pixels[i + 3] = alpha;
		}
	}
}

GLYPHES.forEach(([, dessin], i) => poser(i % PAR_LIGNE, Math.floor(i / PAR_LIGNE), dessin));

// --- L'ecriture du PNG, a la main -----------------------------------------
//
// Aucune bibliotheque : un PNG est un en-tete, des morceaux, et un CRC. On sait
// faire, et ca evite une dependance pour dessiner dix icones.

function crc32(donnees) {
	const table = [];
	for (let n = 0; n < 256; n++) {
		let c = n;
		for (let k = 0; k < 8; k++) c = c & 1 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1;
		table[n] = c >>> 0;
	}
	let crc = 0xFFFFFFFF;
	for (const octet of donnees) crc = table[(crc ^ octet) & 0xFF] ^ (crc >>> 8);
	return (crc ^ 0xFFFFFFFF) >>> 0;
}

function morceau(type, donnees) {
	const longueur = Buffer.alloc(4);
	longueur.writeUInt32BE(donnees.length);
	const corps = Buffer.concat([Buffer.from(type, "ascii"), donnees]);
	const somme = Buffer.alloc(4);
	somme.writeUInt32BE(crc32(corps));
	return Buffer.concat([longueur, corps, somme]);
}

const entete = Buffer.alloc(13);
entete.writeUInt32BE(largeur, 0);
entete.writeUInt32BE(hauteur, 4);
entete[8] = 8;
entete[9] = 6;

const brut = Buffer.alloc(hauteur * (largeur * 4 + 1));
for (let y = 0; y < hauteur; y++) {
	brut[y * (largeur * 4 + 1)] = 0;
	pixels.copy(brut, y * (largeur * 4 + 1) + 1, y * largeur * 4, (y + 1) * largeur * 4);
}

const png = Buffer.concat([
	Buffer.from([0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]),
	morceau("IHDR", entete),
	morceau("IDAT", zlib.deflateSync(brut, {level: 9})),
	morceau("IEND", Buffer.alloc(0)),
]);

const RACINE = "src/main/resources/assets/compagnon";
fs.mkdirSync(path.join(RACINE, "textures/font"), {recursive: true});
fs.mkdirSync(path.join(RACINE, "font"), {recursive: true});
fs.writeFileSync(path.join(RACINE, "textures/font/icones.png"), png);

// --- Le fichier de police -------------------------------------------------

const rangees = [];
for (let l = 0; l < lignes; l++) {
	let rangee = "";
	for (let c = 0; c < PAR_LIGNE; c++) {
		const i = l * PAR_LIGNE + c;
		// Les codes de la zone privee : ils n'appartiennent a aucune langue,
		// donc ils ne peuvent voler la place d'aucun vrai caractere.
		rangee += i < GLYPHES.length
			? BS + "u" + (0xE000 + i).toString(16)
			: BS + "u0020";
	}
	rangees.push(rangee);
}

const texte = [
	"{",
	'\t"providers": [',
	"\t\t{",
	'\t\t\t"type": "bitmap",',
	'\t\t\t"file": "compagnon:font/icones.png",',
	// L'ascension place le glyphe par rapport a la ligne d'ecriture. Sept pour
	// huit pixels de haut : l'icone s'aligne sur les majuscules.
	'\t\t\t"ascent": 7,',
	'\t\t\t"height": ' + TAILLE + ",",
	'\t\t\t"chars": [',
	rangees.map(r => '\t\t\t\t"' + r + '"').join(",\n"),
	"\t\t\t]",
	"\t\t}",
	"\t]",
	"}",
	"",
].join("\n");

fs.writeFileSync(path.join(RACINE, "font/icones.json"), texte, "utf8");

console.log("police ecrite : " + largeur + " x " + hauteur + " pixels, "
	+ GLYPHES.length + " icones");
GLYPHES.forEach(([nom], i) =>
	console.log("  " + BS + "u" + (0xE000 + i).toString(16) + "  " + nom));
