var Util = require("../resources/tamejs/Util.js");
var TAction = require("../resources/tamejs/objects/TAction.js");

console.log(TAction.create("a_examine").toString());
console.log(TAction.createModal("a_examine", "butt").toString());
console.log(TAction.createObject("a_examine", "o_apple").toString());
console.log(TAction.createObject2("a_examine", "o_apple", "o_pear").toString());
console.log(TAction.createInitial("a_examine").toString());
console.log(TAction.createInitialModal("a_examine", "butt").toString());
console.log(TAction.createInitialObject("a_examine", "o_apple").toString());
console.log(TAction.createInitialObject2("a_examine", "o_apple", "o_pear").toString());
