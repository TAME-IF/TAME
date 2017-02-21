//##[[EXPORTJS-START
var $Q = function(x){return document.querySelectorAll(x);};
var $Q1 = function(x){return document.querySelector(x);};

var InputBox = $Q1("#input-box");
var OutputDiv = $Q1("#output-div");
var OutputBox = $Q1("#output-box");

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
	OutputDiv.scrollTop = OutputDiv.scrollHeight
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

/**
 * Handles a TAME cue (for debugging).
 * @return true to continue handling, false to halt.
 */
function debugCue(type, content) {

	type = type.toLowerCase();
	println('['+type+'] '+withEscChars(content));
	if (type === 'quit' || type === 'fatal')
		stop = true;
		
	return true;
}

var stop = false;
var pause = false;
var textBuffer = '';

function startFormatTag(tag) {
	// Nothing
}

function endFormatTag(tag) {
	// Nothing
}

function formatText(text) {
	textBuffer += text;
}

/**
 * Handles a TAME cue.
 * @return true to continue handling, false to halt.
 */
function doCue(type, content) {
	
	type = type.toLowerCase();
	
	if (type !== 'text' && type !== 'textf') {
		print(textBuffer);
		textBuffer = '';
	}
	
	switch (type) {
	
		case 'quit':
			stop = true;
			return false;
		
		case 'text':
			textBuffer += content;
			return true;
		
		case 'textf':
			TAME.parseFormatted(content, startFormatTag, endFormatTag, formatText);
			return true;
			
		case 'wait':
			sleep(parseInt(content, 10));
			return true;

		case 'pause':
			pause = true;
			return false;

		case 'trace':
			// Ignore trace.
			return true;

		case 'tip':	
			println('(TIP: '+content+')');
			return true;

		case 'info':	
			println('INFO: '+content);
			return true;

		case 'error':	
			println('\n!ERROR! '+content);
			return true;

		case 'fatal':
			println('\n!!FATAL!! '+content);
			stop = true;
			return false;
	}
	
}

var CueHandler = TAME.createResponseHandler({}, doCue);
var DebugCueHandler = TAME.createResponseHandler({}, debugCue);

function processResponse(response, debug) {
	
	var h = debug ? DebugCueHandler : CueHandler;
	var remainder = h.handleResponse(response);
	
	while (!stop && remainder) {
		if (stop) break;
		// TODO: Fix.
		remainder = h.resume();
	}
	
	if (stop)
		InputBox.disabled = true;
	
	if (textBuffer.length > 0) {
		print(textBuffer);
		textBuffer = '';
	}
	println();
}

function _TAMESetup() {
	var modulectx = TAME.newContext();
	InputBox.addEventListener("keydown", function(event)
	{
		var trace = event.shiftKey;
		var debug = event.ctrlKey;
		if (event.keyCode == 13) // enter
		{
			event.preventDefault();
			var val = InputBox.value;
			InputBox.value = '';
			processResponse(TAME.interpret(modulectx, val, trace), debug);
		}
	});
	
	processResponse(TAME.initialize(modulectx));
	InputBox.focus();
}

$Q1("body").onload = _TAMESetup;
//##[[EXPORTJS-END
