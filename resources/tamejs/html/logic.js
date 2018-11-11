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
var InputDiv = $Q1("#input-div");
var OutputBox = $Q1("#output-box");
var BodyElement = $Q1("body");

var mustContinue = false;

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

var handler = TAME.newBrowserHandler({
	
	"print": print,
	
	"onStart": function()
	{
		InputBox.disabled = true;	
	},
	
	"onSuspend": function() 
	{
		InputBox.disabled = true;
	},
	
	"onResume": function()
	{
		InputBox.disabled = true;
	},
	
	"onEnd": function()
	{
		InputBox.disabled = false;
		InputBox.focus();
	},
	
	"onPause": function()
	{
		mustContinue = true;
		InputBox.disabled = false;
		InputBox.value = "(CONTINUE)";
		InputBox.focus();
	},
	
	"onQuit": function(content)
	{
		InputDiv.remove();
	},
	
	"onError": function(content)
	{
		println("\n !ERROR!: "+content);
	},
	
	"onFatal": function(content)
	{
		println("\n!!FATAL!!: "+content);
		InputDiv.remove();
	}

});

BodyElement.onload = function() 
{	
	var modulectx = TAME.newContext();
	InputBox.addEventListener("keydown", function(event) 
	{
		// enter
		if (event.keyCode == 13) 
		{
			event.preventDefault();
			
			if (mustContinue) 
			{
				mustContinue = false;
				handler.resume();
			} 
			else 
			{
				var val = InputBox.value;
				InputBox.value = '';
				println();
				println("] "+val);
				handler.process(TAME.interpret(modulectx, val));
			}
		}
	});
	
	handler.process(TAME.initialize(modulectx));
};
