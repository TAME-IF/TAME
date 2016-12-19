var TValue = require("../resources/tamejs/objects/TValue.js");
var TLogic = require("../resources/tamejs/logic/TLogic.js");

var context = 	
{
	"elements": {
		"world":{identity:"world", "variables":{}}
	},
	"owners": {}, 		// element-to-objects
	"objectOwners": {}, // object-to-element
	"roomStacks": {},	// player-to-rooms
	"names": {},		// object-to-names
	"tags": {},			// object-to-tags
};

TLogic.setValue(context, "world", "x", TValue.createInteger(1));
console.log(TLogic.getValue(context, "world", "x"));