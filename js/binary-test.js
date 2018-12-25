const fs = require('fs');
const TBinaryReader = require("../resources/tamejs/engine/TBinaryReader.js");
const TDataReader = require("../resources/tamejs/engine/TDataReader.js");

let Util = require("../resources/tamejs/engine/Util.js");

// fill stub function
Util.fromBase64 = (function()
{
	if (Buffer.from)
	{
		return function(data) {
			return Buffer.from(data, 'base64').toString('utf8');
		};
	}
	else
	{
		return function(data) {
			return (new Buffer(data, 'base64')).toString('utf8');
		};
	}
})();

let buffer = fs.readFileSync(process.argv[2]);

let view = new DataView(new ArrayBuffer(buffer.length));
for (let i = 0; i < buffer.length; i++) 
	view.setUint8(i, buffer.readUInt8(i));

let reader = new TDataReader(view, true);
console.log(reader.readASCII(4));
console.log(TBinaryReader.readModuleHeader(reader));

// TODO: Finish this.
