/*
 * bbmodel -> geo.json + animation.json + textures
 *
 * Blockbench garde tout dans SON repere et ne bascule vers celui de Bedrock
 * qu'a l'export. On refait donc ici exactement ce que fait son exporteur :
 *
 *   os      pivot    = [-origin.x, origin.y, origin.z]
 *           rotation = [-rx, -ry, rz]
 *   cube    origin   = [-to.x, from.y, from.z]     (le coin change de cote)
 *           size     = to - from
 *   anim    rotation = [-x, -y, z]
 *           position = [-x, y, z]
 *           scale    = tel quel
 *
 * Le X s'inverse partout, et les rotations autour de X et Y avec lui — c'est la
 * meme bascule, vue de trois facons.
 *
 * Verification a la fin : les deux ailes doivent se retrouver symetriques
 * autour de zero, et la tete devant. Si ces deux la sont bonnes, la bascule
 * l'est aussi.
 */
const fs = require("fs");
const path = require("path");

const source = process.argv[2];
const espece = process.argv[3];
const sortie = process.argv[4];

const m = JSON.parse(fs.readFileSync(source, "utf8"));

const groupes = {};
for (const g of m.groups) groupes[g.uuid] = g;
const elements = {};
for (const e of m.elements) elements[e.uuid] = e;

const n = v => Number(v) || 0;
const arrondi = v => Math.round(v * 100000) / 100000;

// --- Les os ------------------------------------------------------------------

const os = [];

function cubeDe(e) {
	const cube = {
		origin: [-n(e.to[0]), n(e.from[1]), n(e.from[2])].map(arrondi),
		size: [
			arrondi(n(e.to[0]) - n(e.from[0])),
			arrondi(n(e.to[1]) - n(e.from[1])),
			arrondi(n(e.to[2]) - n(e.from[2]))
		],
		uv: (e.uv_offset || [0, 0]).map(n)
	};
	if (e.inflate) cube.inflate = arrondi(n(e.inflate));
	if (e.mirror_uv) cube.mirror = true;
	if (e.rotation && e.rotation.some(x => n(x) !== 0)) {
		cube.pivot = [-n(e.origin[0]), n(e.origin[1]), n(e.origin[2])].map(arrondi);
		cube.rotation = [-n(e.rotation[0]), -n(e.rotation[1]), n(e.rotation[2])].map(arrondi);
	}
	return cube;
}

function parcourir(noeuds, parent) {
	for (const noeud of noeuds) {
		if (typeof noeud === "string") continue;          // un cube pose a la racine
		const g = groupes[noeud.uuid];
		if (!g) continue;

		const bone = {
			name: g.name,
			pivot: [-n(g.origin[0]), n(g.origin[1]), n(g.origin[2])].map(arrondi)
		};
		if (parent) bone.parent = parent;
		if (g.rotation && g.rotation.some(x => n(x) !== 0)) {
			bone.rotation = [-n(g.rotation[0]), -n(g.rotation[1]), n(g.rotation[2])].map(arrondi);
		}

		const cubes = [];
		for (const enfant of noeud.children || []) {
			if (typeof enfant === "string" && elements[enfant]) {
				const e = elements[enfant];
				// Un cube sans volume ne se voit pas et fait clignoter les faces
				// coplanaires : on l'ecarte plutot que de l'embarquer.
				const vide = ["0", "1", "2"].every(i =>
					n(e.to[i]) - n(e.from[i]) === 0) && !e.inflate;
				if (!vide) cubes.push(cubeDe(e));
			}
		}
		if (cubes.length) bone.cubes = cubes;

		os.push(bone);
		parcourir((noeud.children || []).filter(x => typeof x === "object"), g.name);
	}
}
parcourir(m.outliner, null);

// --- Les bornes, pour la boite visible et pour la verification ----------------

let min = [1e9, 1e9, 1e9];
let max = [-1e9, -1e9, -1e9];
for (const bone of os) {
	for (const c of bone.cubes || []) {
		for (let i = 0; i < 3; i++) {
			min[i] = Math.min(min[i], c.origin[i]);
			max[i] = Math.max(max[i], c.origin[i] + c.size[i]);
		}
	}
}

const geo = {
	format_version: "1.12.0",
	"minecraft:geometry": [{
		description: {
			identifier: "geometry." + espece,
			texture_width: m.resolution.width,
			texture_height: m.resolution.height,
			visible_bounds_width: arrondi(Math.max(max[0] - min[0], max[2] - min[2]) / 16 + 1),
			visible_bounds_height: arrondi((max[1] - min[1]) / 16 + 1),
			visible_bounds_offset: [0, arrondi((max[1] + min[1]) / 32), 0]
		},
		bones: os
	}]
};

// --- Les animations -----------------------------------------------------------

const animations = {};
let posees = 0;

for (const a of m.animations) {
	// On renomme : le mod range les animations par espece grace au deuxieme
	// morceau du nom. « animation.mount.vol » serait vu comme appartenant a une
	// espece « mount » qui n'existe pas, et s'afficherait dans la roue de tout
	// le monde. Voir Especes.concerne.
	const court = a.name.replace(/^animation\.[^.]+\./, "");
	const nom = "animation." + espece + "." + court;

	const bones = {};
	for (const an of Object.values(a.animators || {})) {
		if (an.type !== "bone" || !an.name) continue;
		const canaux = {};
		for (const k of an.keyframes || []) {
			const p = (k.data_points || [])[0] || {};
			let valeur;
			if (k.channel === "rotation") {
				valeur = [-n(p.x), -n(p.y), n(p.z)];
			} else if (k.channel === "position") {
				valeur = [-n(p.x), n(p.y), n(p.z)];
			} else {
				valeur = [n(p.x), n(p.y), n(p.z)];
			}
			valeur = valeur.map(arrondi);
			(canaux[k.channel] ||= {})[String(arrondi(n(k.time)))] = valeur;
		}
		if (Object.keys(canaux).length) bones[an.name] = canaux;
	}
	if (!Object.keys(bones).length) continue;

	const anim = {};
	if (a.loop === "loop") anim.loop = true;
	else if (a.loop === "hold") anim.loop = "hold_on_last_frame";
	if (a.length) anim.animation_length = arrondi(n(a.length));
	anim.bones = bones;

	animations[nom] = anim;
	posees++;
}

fs.mkdirSync(path.dirname(sortie.geo || sortie), { recursive: true });
fs.writeFileSync(process.argv[4], JSON.stringify(geo, null, "\t") + "\n", "utf8");
fs.writeFileSync(process.argv[5],
	JSON.stringify({ format_version: "1.8.0", animations }, null, "\t") + "\n", "utf8");

// --- Les textures --------------------------------------------------------------

const dossierTextures = process.argv[6];
fs.mkdirSync(dossierTextures, { recursive: true });
const variantes = {};
m.textures.forEach((t, i) => {
	const b64 = (t.source || "").replace(/^data:image\/png;base64,/, "");
	if (!b64) return;
	const nom = espece + (i === 0 ? "" : "_" + (i + 1));
	fs.writeFileSync(path.join(dossierTextures, nom + ".png"), Buffer.from(b64, "base64"));
	variantes[nom] = "compagnon:textures/entity/" + nom + ".png";
});

// --- La verification -------------------------------------------------------------

const parNom = {};
for (const b of os) parNom[b.name] = b;

console.log(os.length + " os, " + (geo["minecraft:geometry"][0].bones
	.reduce((t, b) => t + (b.cubes || []).length, 0)) + " cubes");
console.log(posees + " animations");
console.log(Object.keys(variantes).length + " textures");
console.log();
console.log("bornes  x " + min[0] + " a " + max[0]
	+ "   y " + min[1] + " a " + max[1]
	+ "   z " + min[2] + " a " + max[2]);
console.log("en blocs : largeur " + arrondi((max[0] - min[0]) / 16)
	+ "  hauteur " + arrondi(max[1] / 16)
	+ "  longueur " + arrondi((max[2] - min[2]) / 16));
console.log();
// Les anciens modeles nommaient les ailes wingr/wingl, le nouveau dragon
// wing_r/wing_l. La verification doit suivre les deux conventions : sinon le
// convertisseur reussit mais ne dit plus si le modele est parti en miroir.
const aileDroite = parNom.wingr || parNom.wing_r;
const aileGauche = parNom.wingl || parNom.wing_l;
if (aileDroite && aileGauche) {
	const d = aileDroite.pivot[0], g = aileGauche.pivot[0];
	console.log("ailes : droite x=" + d + "  gauche x=" + g
		+ "   -> " + (Math.abs(d + g) < 0.001 ? "SYMETRIQUES" : "!! ASYMETRIQUES"));
	// wingr/wingl etait une convention anatomique connue. Les fichiers en
	// wing_r/wing_l peuvent etre nommes depuis la vue de face de l'animateur :
	// leur signe ne prouve alors rien, seule la symetrie est universelle.
	if (parNom.wingr && parNom.wingl) {
		console.log("        la droite est " + (d > 0
			? "en +X : correct pour une bete qui regarde -Z"
			: "!! en -X : la bascule est a l'envers"));
	}
}
if (parNom.head) {
	console.log("tete  : z=" + parNom.head.pivot[2]
		+ "   -> " + (parNom.head.pivot[2] < 0 ? "devant, elle regarde -Z" : "!! derriere"));
}
if (parNom.saddle) {
	console.log("selle : pivot " + JSON.stringify(parNom.saddle.pivot));
}
