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

function print(text) 
{
	if (!text)
		return;
	OutputBox.appendChild($DOMNew('span',{},[$DOMText(text)]));
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
	println('['+cue.type+'] '+withEscChars(cue.content));
	let type = cue.type.toLowerCase();
	if (type === 'quit' || type === 'fatal')
		stop = true;
	return true;
}

function readResponse(response)
{
	println();
	println('Interpret time: '+(response.interpretNanos/1000000.0)+' ms');
	println('Request time: '+(response.requestNanos/1000000.0)+' ms');
	println('Operations: '+response.operationsExecuted);
	println('Cues: '+response.responseCues.length);

	for (let i in response.responseCues) if (response.responseCues.hasOwnProperty(i))
		handleCue(response.responseCues[i]);
	
	if (stop)
		InputBox.disabled = true;
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
			println("] "+val);
			readResponse(TAME.interpret(modulectx, val, trace));
		}
	});
	
	readResponse(TAME.initialize(modulectx));
	InputBox.focus();
};
