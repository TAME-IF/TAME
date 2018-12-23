const TBinaryReader = require("../resources/tamejs/engine/TBinaryReader.js");
const TDataReader = require("../resources/tamejs/engine/TDataReader.js");
let Util = require("../resources/tamejs/engine/TDataReader.js");

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

// TODO: Finish this.