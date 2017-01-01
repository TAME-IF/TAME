var TValue = require("../resources/tamejs/objects/TValue.js");
var v = TValue.createFloat(4.5);
console.log(v);
console.log(TValue.asString(v));
console.log(TValue.toString(v));