const fs=require("fs"),path=require("path");
const R=process.argv[2];
const D=path.join(R,"src/main/resources/data/compagnon");
const L=JSON.parse(fs.readFileSync(path.join(R,"src/main/resources/assets/compagnon/lang/fr_fr.json"),"utf8"));
let ko=[];

// Chaque reve aussi.
const rv=JSON.parse(fs.readFileSync(path.join(D,"reves.json"),"utf8"));
for (const id of rv.reves) if (!L["moment.compagnon.reve."+id]) ko.push("reve sans phrase : "+id);
console.log(`reves : ${rv.reves.length}`);

// Les situations que le code sait reconnaitre, contre celles du fichier.

// Les noms proposes.
const nm=JSON.parse(fs.readFileSync(path.join(D,"noms.json"),"utf8"));
console.log(`noms proposes : ${nm.noms.length}`);

console.log(ko.length? "PROBLEMES :\n  "+ko.join("\n  ") : "tout est coherent");
