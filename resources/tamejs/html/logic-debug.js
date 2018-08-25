var $Q = function(x){return document.querySelectorAll(x);};
var $Q1 = function(x){return document.querySelector(x);};

//text: text in node
var $DOMText = function(text)
{
	return document.createTextNode(text);
};

// name: tagname
// attribs: object {attrname: 'value'}
// children: array of elements/nodes to append in order
var $DOMNew = function(name, attribs, children)
{
	let out = document.createElement(name);
	if (attribs) for (let a in attribs) if (attribs.hasOwnProperty(a))
	{
		let attrObj = document.createAttribute(a);
		attrObj.value = attribs[a];
		out.setAttributeNode(attrObj);
	}
	if (children) for (let i = 0; i < children.length; i++)
		out.appendChild(children[i]);
	
	return out;
};

var InputBox = $Q1("#input-box");
var OutputBox = $Q1("#output-box");
var BodyElement = $Q1("body");

function print(text, fade) 
{
	if (!text)
		return;
	OutputBox.appendChild($DOMNew('span',fade ? {style:"color:#999;"}:{},[$DOMText(text)]));
	BodyElement.scrollTop = BodyElement.scrollHeight;
}

function println(text, fade)
{
	if (!text)
		print('\n', fade);
	else
		print(text + '\n', fade);
}

function withEscChars(text) 
{
	var t = JSON.stringify(text);
	return t.substring(1, t.length - 1);
}

var stop = false;

function handleCue(cue) 
{
	println('['+cue.type+'] '+withEscChars(cue.content));
	let type = cue.type.toLowerCase();
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

	for (let i in response.responseCues) if (response.responseCues.hasOwnProperty(i))
		handleCue(response.responseCues[i]);
	
	if (stop)
		InputBox.disabled = true;
}

function inspect(context, input)
{
	var split = input.split(/\./);
	if (split.length < 1 || !split[0] || !split[0].trim())
	{
		println("?> Must specify a variable.");
	}
	else
	{
		var result = TAME.inspect(context, split[0], split[1]);
		if (result === null)
		{
			println("?> Context \""+split[0]+"\" not found.");
		}
		else
		{
			for (let key in result) if (result.hasOwnProperty(key))
				println("?> "+key+" = "+result[key]);			
		}
	}
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
			println();
			println("] "+val);
			if (val.trim().substr(0,1) === '?')
				inspect(modulectx, val.trim().substring(1));
			else
				readResponse(TAME.interpret(modulectx, val, trace));
		}
	});
	println("[[Press Shift+Enter to trace the input command.]]", true);
	println("[[The inspector is enabled. You may query elements and their values.]]", true);
	readResponse(TAME.initialize(modulectx));
	InputBox.focus();
};
