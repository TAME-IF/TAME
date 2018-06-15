var $Q1 = function(x){return document.querySelector(x);};

var InputBox = $Q1("#input-box");
var InputDiv = $Q1("#input-div");
var OutputBox = $Q1("#output-box");
var BodyElement = $Q1("body");

var mustContinue = false;

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

var handler = TAME.newBrowserHandler({
	
	"print": print,
	
	"onStart": function()
	{
		InputBox.disabled = true;	
	},
	
	"onSuspend": function() 
	{
		InputBox.disabled = false;
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
	
	"onPauseCue": function()
	{
		mustContinue = true;
		InputBox.disabled = false;
		InputBox.value = "(CONTINUE)";
		InputBox.focus();
	},
	
	"onQuitCue": function(content)
	{
		InputDiv.remove();
	},
	
	"onErrorCue": function(content)
	{
		println("\n !ERROR!: "+content);
	},
	
	"onFatalCue": function(content)
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
				println("\n> "+val);
				handler.prepare(TAME.interpret(modulectx, val));
				handler.resume();
			}
		}
	});
	
	handler.prepare(TAME.initialize(modulectx));
	handler.resume();
};
