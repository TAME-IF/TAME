var $Q = function(x){return document.querySelectorAll(x);};
var $Q1 = function(x){return document.querySelector(x);};

var InputBox = $Q1("#input-box");
var OutputBox = $Q1("#output-box");
var BodyElement = $Q1("body");

function print(text) {
	if (!text)
		return;
	OutputBox.innerHTML = OutputBox.innerHTML + text;
	BodyElement.scrollTop = BodyElement.scrollHeight
}

function println(text) {
	if (!text)
		print('\n');
	else
		print(text + '\n');
}

var stop = false;
var pause = false;
var waitTime = 0;
var waiting = false;
var textBuffer = '';

function handleCue(type, content) {
	
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
			waitTime = parseInt(content, 10);
			return false;

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

var CueHandler = null;

function readResponse() {
	
	if (waiting) {
		waiting = false;
		endWait();
	}
	if (CueHandler.read()) {
		if (stop)
			InputBox.disabled = true;
		else if (pause)
			startPause();
		else if (waitTime > 0) {
			startWait();
			waiting = true;
			setTimeout(readResponse, waitTime);
			waitTime = 0;
		}
	} else if (stop) {
		endModule();
	} 
	
	if (textBuffer.length > 0) {
		print(textBuffer);
		textBuffer = '';
	}

	if (!CueHandler.hasMoreCues())
		println();

}

function startPause() {
	InputBox.value = '(CONTINUE)';
	InputBox.focus();
}

function endPause() {
	InputBox.value = '';
	InputBox.focus();
}

function startWait() {
	InputBox.disabled = true;
}

function endWait() {
	InputBox.disabled = false;
	InputBox.focus();
}

function startModule() {
	InputBox.disabled = false;
	InputBox.focus();
}

function endModule() {
	InputBox.disabled = true;
	InputBox.display = 'none';
}

function onSendInput(input) {
	InputBox.value = '';
	InputBox.focus();
}

function startFormatTag(tag) {
	// Nothing
}

function endFormatTag(tag) {
	// Nothing
}

function formatText(text) {
	textBuffer += text;
}

var CueEvents = 
{
	"start": function() 
	{
		InputBox.disabled = true;
	},
	"pause": function() 
	{
		InputBox.disabled = true;
	},
	"resume": function()
	{
		
	},
	"end": function()
	{
		InputBox.disabled = false;
	}
};

BodyElement.onload = function() {
	
	var modulectx = TAME.newContext();
	InputBox.addEventListener("keydown", function(event) {
		// enter
		if (event.keyCode == 13) {
			
			event.preventDefault();
			
			if (pause) {
				pause = false;
				endPause();
				readResponse();
			} else {
				var val = InputBox.value;
				onSendInput(val);
				println("> "+val);
				CueHandler = TAME.createResponseReader(TAME.interpret(modulectx, val), CueEvents, handleCue);
				readResponse();
			}
		}
	});
	
	CueHandler = TAME.createResponseReader(TAME.initialize(modulectx), CueEvents, handleCue);
	readResponse();
	startModule();
};
