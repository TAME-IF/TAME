const fs = require('fs');
const TBinaryReader = require("../resources/tamejs/engine/TBinaryReader.js");
const TDataReader = require("../resources/tamejs/engine/TDataReader.js");

let Util = require("../resources/tamejs/engine/Util.js");

let buffer = fs.readFileSync(process.argv[2]);
let view = new DataView(new ArrayBuffer(buffer.length));
for (let i = 0; i < buffer.length; i++) 
	view.setUint8(i, buffer.readUInt8(i));

let out = TBinaryReader.readModule(view);
console.log(JSON.stringify(out));
