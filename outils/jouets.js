// Dessine les textures des jouets, en 32x32 comme le reste des objets du mod.
//
// La finesse vient du SURECHANTILLONNAGE : on dessine tout en 128x128, puis on
// reduit par moyenne de seize pixels. Les bords deviennent doux et les couleurs
// se nuancent toutes seules, ce qui rapproche ces textures des soixante-dix
// autres au lieu d'un aplat de pixels durs au milieu d'elles.
//
//   node outils/jouets.js
//
// Aucune dependance : le PNG est ecrit a la main, comme dans police.js.

const fs = require("fs");
const zlib = require("zlib");
const path = require("path");

const COTE = 32;
const FINESSE = 4;                    // on dessine quatre fois plus grand
const GRAND = COTE * FINESSE;
const SORTIE = path.join(__dirname, "..",
	"src/main/resources/assets/compagnon/textures/item");

// --- Une toile en grand ------------------------------------------------------

function toile() {
	return {l: GRAND, h: GRAND, px: new Float64Array(GRAND * GRAND * 4)};
}

function poser(t, x, y, [r, v, b], a = 1.0) {
	if (x < 0 || y < 0 || x >= t.l || y >= t.h || a <= 0) return;
	const k = (y * t.l + x) * 4;
	const reste = 1 - a;
	t.px[k] = t.px[k] * reste + r * a;
	t.px[k + 1] = t.px[k + 1] * reste + v * a;
	t.px[k + 2] = t.px[k + 2] * reste + b * a;
	t.px[k + 3] = Math.min(1, t.px[k + 3] * reste + a);
}

/** Un disque plein, avec une fonction de couleur qui depend de la position. */
function disque(t, cx, cy, rayon, couleurDe) {
	for (let y = Math.floor(cy - rayon); y <= Math.ceil(cy + rayon); y++) {
		for (let x = Math.floor(cx - rayon); x <= Math.ceil(cx + rayon); x++) {
			const dx = (x + 0.5 - cx) / rayon;
			const dy = (y + 0.5 - cy) / rayon;
			const d = Math.hypot(dx, dy);
			if (d > 1) continue;
			const c = couleurDe(dx, dy, d);
			if (c) poser(t, x, y, c, c[3] === undefined ? 1 : c[3]);
		}
	}
}

/** Une capsule : un rectangle aux bouts arrondis, entre deux points. */
function capsule(t, x1, y1, x2, y2, rayon, couleurDe) {
	const minX = Math.floor(Math.min(x1, x2) - rayon);
	const maxX = Math.ceil(Math.max(x1, x2) + rayon);
	const minY = Math.floor(Math.min(y1, y2) - rayon);
	const maxY = Math.ceil(Math.max(y1, y2) + rayon);
	const vx = x2 - x1, vy = y2 - y1;
	const longueur2 = vx * vx + vy * vy || 1;
	for (let y = minY; y <= maxY; y++) {
		for (let x = minX; x <= maxX; x++) {
			const px = x + 0.5 - x1, py = y + 0.5 - y1;
			let t2 = (px * vx + py * vy) / longueur2;
			t2 = Math.max(0, Math.min(1, t2));
			const d = Math.hypot(px - vx * t2, py - vy * t2) / rayon;
			if (d > 1) continue;
			const c = couleurDe(x, y, d, t2);
			if (c) poser(t, x, y, c);
		}
	}
}

// --- Reduire, cerner, encoder ------------------------------------------------

/** Moyenne de seize pixels : c'est ce qui donne les bords doux. */
function reduire(t) {
	const petit = Buffer.alloc(COTE * COTE * 4);
	for (let y = 0; y < COTE; y++) {
		for (let x = 0; x < COTE; x++) {
			let r = 0, v = 0, b = 0, a = 0;
			for (let sy = 0; sy < FINESSE; sy++) {
				for (let sx = 0; sx < FINESSE; sx++) {
					const k = ((y * FINESSE + sy) * t.l + x * FINESSE + sx) * 4;
					// On pondere la couleur par l'opacite, sinon les bords tirent
					// vers le noir des pixels vides.
					const o = t.px[k + 3];
					r += t.px[k] * o; v += t.px[k + 1] * o; b += t.px[k + 2] * o;
					a += o;
				}
			}
			const k = (y * COTE + x) * 4;
			if (a > 0) {
				petit[k] = Math.round(Math.max(0, Math.min(255, r / a)));
				petit[k + 1] = Math.round(Math.max(0, Math.min(255, v / a)));
				petit[k + 2] = Math.round(Math.max(0, Math.min(255, b / a)));
			}
			petit[k + 3] = Math.round(255 * a / (FINESSE * FINESSE));
		}
	}
	return petit;
}

/**
 * Le trait sombre autour de la forme.
 *
 * <p>Toutes les textures du mod en ont un : sans lui, un objet clair disparait
 * sur le fond clair d'un inventaire. On l'ajoute apres reduction pour qu'il
 * reste net d'un pixel.
 */
function cerner(petit, [r, v, b]) {
	const copie = Buffer.from(petit);
	for (let y = 0; y < COTE; y++) {
		for (let x = 0; x < COTE; x++) {
			const k = (y * COTE + x) * 4;
			if (copie[k + 3] > 40) continue;
			let voisin = false;
			for (const [dx, dy] of [[1, 0], [-1, 0], [0, 1], [0, -1]]) {
				const nx = x + dx, ny = y + dy;
				if (nx < 0 || ny < 0 || nx >= COTE || ny >= COTE) continue;
				if (copie[(ny * COTE + nx) * 4 + 3] > 140) voisin = true;
			}
			if (!voisin) continue;
			petit[k] = r; petit[k + 1] = v; petit[k + 2] = b; petit[k + 3] = 235;
		}
	}
	return petit;
}

function png(petit) {
	const brut = Buffer.alloc(COTE * (COTE * 4 + 1));
	for (let y = 0; y < COTE; y++) {
		brut[y * (COTE * 4 + 1)] = 0;
		petit.copy(brut, y * (COTE * 4 + 1) + 1, y * COTE * 4, (y + 1) * COTE * 4);
	}
	const morceau = (tag, corps) => {
		const m = Buffer.alloc(corps.length + 12);
		m.writeUInt32BE(corps.length, 0);
		m.write(tag, 4, "ascii");
		corps.copy(m, 8);
		m.writeInt32BE(crc(Buffer.concat([Buffer.from(tag, "ascii"), corps])), corps.length + 8);
		return m;
	};
	const ihdr = Buffer.alloc(13);
	ihdr.writeUInt32BE(COTE, 0); ihdr.writeUInt32BE(COTE, 4);
	ihdr[8] = 8; ihdr[9] = 6;
	return Buffer.concat([
		Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
		morceau("IHDR", ihdr),
		morceau("IDAT", zlib.deflateSync(brut, {level: 9})),
		morceau("IEND", Buffer.alloc(0))]);
}

let tableCrc = null;
function crc(buf) {
	if (!tableCrc) {
		tableCrc = new Int32Array(256);
		for (let n = 0; n < 256; n++) {
			let c = n;
			for (let k = 0; k < 8; k++) c = (c & 1) ? (0xEDB88320 ^ (c >>> 1)) : (c >>> 1);
			tableCrc[n] = c;
		}
	}
	let c = -1;
	for (const octet of buf) c = tableCrc[(c ^ octet) & 0xFF] ^ (c >>> 8);
	return c ^ -1;
}

// --- Les deux jouets ---------------------------------------------------------

/** Melange deux couleurs. */
const entre = (a, b, p) => [
	a[0] + (b[0] - a[0]) * p,
	a[1] + (b[1] - a[1]) * p,
	a[2] + (b[2] - a[2]) * p];

/**
 * La balle.
 *
 * <p>Rouge profond avec une bande creme, l'eclairage en haut a gauche comme
 * partout dans le jeu. La bande n'est pas decorative : elle donne a la sphere
 * une orientation, ce qui la fait paraitre tourner quand elle roule.
 */
function balle() {
	const t = toile();
	const CLAIR = [232, 104, 92];
	const CORPS = [186, 52, 48];
	const OMBRE = [104, 24, 30];
	const BANDE = [242, 232, 208];
	const BANDE_OMBRE = [188, 172, 148];

	const cx = GRAND / 2, cy = GRAND / 2 + GRAND * 0.02;
	disque(t, cx, cy, GRAND * 0.42, (dx, dy, d) => {
		// Une sphere : l'eclairage suit la normale, pas la distance au centre.
		const z = Math.sqrt(Math.max(0, 1 - dx * dx - dy * dy));
		const lumiere = Math.max(0, (-dx * 0.5 - dy * 0.72 + z * 0.48));
		// La bande suit la COURBURE : tracee sur la sphere et non sur le
		// disque, elle se pince vers les bords au lieu de s y elargir.
		const surLaBande = Math.abs(dy * 0.94 + dx * 0.34) < 0.115 * (0.35 + z * 0.65);
		const base = surLaBande ? BANDE : CORPS;
		const sombre = surLaBande ? BANDE_OMBRE : OMBRE;
		let c = entre(sombre, base, Math.min(1, 0.18 + lumiere * 1.25));
		if (!surLaBande) c = entre(c, CLAIR, Math.max(0, lumiere - 0.62) * 2.1);
		// Le bord s'assombrit : c'est ce qui fait la rondeur.
		c = entre(OMBRE, c, Math.min(1, (1 - d) * 3.0));
		return c;
	});
	// Le reflet, petit et franc.
	disque(t, cx - GRAND * 0.15, cy - GRAND * 0.19, GRAND * 0.075,
		(dx, dy, d) => entre([255, 236, 226], [255, 255, 255], 0.5).concat([Math.pow(1 - d, 1.4) * 0.85]));
	return cerner(reduire(t), [58, 14, 18]);
}

/**
 * L'os a macher.
 *
 * <p>Deux lobes a chaque bout et un manche, en diagonale pour tenir dans le
 * carre. Ivoire chaud plutot que blanc : un objet blanc pur disparait sur la
 * moitie des fonds d'inventaire.
 */
function osAMacher() {
	const t = toile();
	const CLAIR = [252, 246, 228];
	const CORPS = [226, 214, 184];
	const OMBRE = [166, 150, 118];

	const teindre = (d, appui) => {
		let c = entre(CORPS, CLAIR, Math.max(0, 1 - d * 1.5) * appui);
		c = entre(OMBRE, c, Math.min(1, (1 - d) * 3.2 + 0.15));
		return c;
	};

	const x1 = GRAND * 0.26, y1 = GRAND * 0.70;
	const x2 = GRAND * 0.74, y2 = GRAND * 0.30;

	// Le manche d'abord, les lobes par-dessus : les jointures disparaissent.
	capsule(t, x1, y1, x2, y2, GRAND * 0.085, (x, y, d) => teindre(d, 1.0));

	const lobe = GRAND * 0.115;
	const ecart = GRAND * 0.085;
	for (const [cx, cy, sx, sy] of [
		[x1, y1, -0.62, -0.78], [x1, y1, 0.62, 0.78],
		[x2, y2, -0.62, -0.78], [x2, y2, 0.62, 0.78]]) {
		disque(t, cx + sx * ecart, cy + sy * ecart, lobe, (dx, dy, d) => teindre(d, 1.0));
	}
	return cerner(reduire(t), [88, 74, 52]);
}

// --- Ecrire ------------------------------------------------------------------

const AFAIRE = [["balle", balle], ["os_a_macher", osAMacher]];

fs.mkdirSync(SORTIE, {recursive: true});
for (const [nom, dessin] of AFAIRE) {
	const fichier = path.join(SORTIE, nom + ".png");
	fs.writeFileSync(fichier, png(dessin()));
	console.log("  " + nom + ".png  " + COTE + "x" + COTE
		+ "  " + fs.statSync(fichier).size + " octets");
}
console.log(AFAIRE.length + " textures de jouet ecrites");
