var $Q = function(x){return document.querySelectorAll(x);};
var $Q1 = function(x){return document.querySelector(x);};

var InputBox = $Q1("#input-box");
var OutputBox = $Q1("#output-box");
var BodyElement = $Q1("body");

var REGEX_AMP = /&/g;
var REGEX_LT = /</g;
var REGEX_GT = />/g;
var REGEX_QUOT = /\"/g;

function safetags(text) {
	return text.replace(REGEX_QUOT, '&quot;').replace(REGEX_AMP, '&amp;').replace(REGEX_LT, '&lt;').replace(REGEX_GT, '&gt;');
}

function print(text) {
	if (!text)
		return;
	text = safetags(text);
	OutputBox.innerHTML = OutputBox.innerHTML + text;
	BodyElement.scrollTop = BodyElement.scrollHeight;
}

function println(text) {
	if (!text)
		print('\n');
	else
		print(text + '\n');
}

function withEscChars(text) {
	var t = JSON.stringify(text);
	return t.substring(1, t.length - 1);
}

var stop = false;

var CueHandler = null;

function handleCue(type, content) {
	type = type.toLowerCase();
	println('['+type+'] '+withEscChars(content));
	if (type === 'quit' || type === 'fatal')
		stop = true;
		
	return true;
}

function readResponse() {
	
	var response = CueHandler.currentResponse;
	println('Interpret time: '+(response.interpretNanos/1000000.0)+' ms');
	println('Request time: '+(response.requestNanos/1000000.0)+' ms');
	println('Commands: '+response.commandsExecuted);
	println('Cues: '+response.responseCues.length);

	CueHandler.read();
	
	if (stop)
		InputBox.disabled = true;

	println();
}

function _TAMESetup() {
	var modulectx = TAME.newContext();
	InputBox.addEventListener("keydown", function(event) {
		var trace = event.shiftKey;
		// enter
		if (event.keyCode == 13) {
			event.preventDefault();
			var val = InputBox.value;
			InputBox.value = '';
			println("> "+val);
			CueHandler = TAME.createResponseReader(TAME.interpret(modulectx, val, trace), {}, handleCue);
			readResponse();
		}
	});
	
	CueHandler = TAME.createResponseReader(TAME.initialize(modulectx), {}, handleCue);
	readResponse();
	InputBox.focus();
}

BodyElement.onload = _TAMESetup;
