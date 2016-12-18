var TValue = require("../resources/tamejs/objects/TValue.js");
var TBlockEntry = require("../resources/tamejs/objects/TBlockEntry.js");

console.log(TBlockEntry.create(TBlockEntry.Type.ONACTION, [TValue.createAction("a_examine")]).entryString);
console.log(TBlockEntry.create(TBlockEntry.Type.ONACTION, [TValue.createAction("a_examine"), TValue.createString("butt")]).entryString);
console.log(TBlockEntry.create(TBlockEntry.Type.ONACTION, [TValue.createAction("a_examine")]).entryString);