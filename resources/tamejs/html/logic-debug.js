var $Q = function(x){return document.querySelectorAll(x);};
var $Q1 = function(x){return document.querySelector(x);};

var InputBox = $Q1("#input-box");
var OutputBox = $Q1("#output-box");
var BodyElement = $Q1("body");

function print(text) 
{
	if (!text)
		return;
	OutputBox.innerHTML = OutputBox.innerHTML + text;
	BodyElement.scrollTop = BodyElement.scrollHeight;
}

function println(text)
{
	if (!text)
		print('\n');
	else
		print(text + '\n');
}

function withEscChars(text) 
{
	var t = JSON.stringify(text);
	return t.substring(1, t.length - 1);
}

var stop = false;

function handleCue(cue) 
{
	var type = cue.type.toLowerCase();
	println('['+type+'] '+withEscChars(cue.content));
	if (type === 'quit' || type === 'fatal')
		stop = true;
	return true;
}

function readResponse(response)
{
	println('Interpret time: '+(response.interpretNanos/1000000.0)+' ms');
	println('Request time: '+(response.requestNanos/1000000.0)+' ms');
	println('Operations: '+response.operationsExecuted);
	println('Cues: '+response.responseCues.length);

	for (i in response.responseCues) if (response.responseCues.hasOwnProperty(i))
		handleCue(response.responseCues[i]);
	
	if (stop)
		InputBox.disabled = true;

	println();
}

BodyElement.onload = function() 
{
	var modulectx = TAME.newContext();
	
	InputBox.addEventListener("keydown", function(event) 
	{
		var trace = event.shiftKey;
		// enter
		if (event.keyCode == 13) 
		{
			event.preventDefault();
			var val = InputBox.value;
			InputBox.value = '';
			println("> "+val);
			readResponse(TAME.interpret(modulectx, val, trace));
		}
	});
	
	readResponse(TAME.initialize(modulectx));
	InputBox.focus();
};
