/**
 * This is the cue handler object passed to the response handler in the logic.
 */
let cueHandler = 
{
		
	/**
	 * print()
	 * -------------------------------------------------
	 * Called when a string needs printing (may contain HTML or other things).
	 * Should disable input.
	 */
	"print": print,
	
	/**
	 * onStart()
	 * -------------------------------------------------
	 * Called before cues start processing.
	 * Should disable input.
	 */
	"onStart": function()
	{
		InputBox.disabled = true;	
	},
	
	/**
	 * onSuspend()
	 * -------------------------------------------------
	 * Called when a call to process a cue initiates a suspension (processing stops, but not due to a pause).
	 * Should disable input.
	 */
	"onSuspend": function() 
	{
		InputBox.disabled = true;
	},
	
	/**
	 * onResume()
	 * -------------------------------------------------
	 * Called when cues process again.
	 * Should disable input.
	 */
	"onResume": function()
	{
		InputBox.disabled = true;
	},
	
	/**
	 * onEnd()
	 * -------------------------------------------------
	 * Called when cues stop processing, and the end is reached.
	 * Should enable input.
	 */
	"onEnd": function()
	{
		InputBox.disabled = false;
		InputBox.value = "";
		InputBox.focus();
	},
	
	/**
	 * onPause()
	 * -------------------------------------------------
	 * Called after a "pause" cue is processed. 
	 * Should prompt for continuation somehow, then call resume() after the user "continues."
	 */
	"onPause": function()
	{
		mustContinue = true;
		InputBox.disabled = false;
		InputBox.value = "(CONTINUE)";
		InputBox.focus();
	},

	/**
	 * onQuit()
	 * -------------------------------------------------
	 * Called after a "quit" cue is processed.
	 * Should stop input and prevent further input. 
	 */
	"onQuit": function()
	{
		InputDiv.remove();
	},
	
	/**
	 * onError(message)
	 * -------------------------------------------------
	 * Called after an "error" cue is processed.
	 * Should make an error message appear on screen. Dismissable.
	 * 		message: (string)
	 * 			The message to display. 
	 */
	"onError": function(message)
	{
		println("\n !ERROR!: "+message);
	},
	
	/**
	 * onFatal(message)
	 * -------------------------------------------------
	 * Called after a "fatal" cue is processed.
	 * Should make a message appear on screen, and stop input as though a "quit" occurred. 
	 * 		message: (string)
	 * 			The message to display. 
	 */
	"onFatal": function(message)
	{
		println("\n!!FATAL!!: "+message);
		InputDiv.remove();
	},

	/**
	 * onTrace(type, content)
	 * -------------------------------------------------
	 * Called after a "trace" cue is processed.
	 * 		type: (string)
	 * 			The tracing type (internal, function, ... etc.). 
	 * 		content: (string)
	 * 			The trace message content. 
	 */
	"onTrace": function(type, content)
	{
		// Blank
	},
	
	/**
	 * onOtherCue(cueType, cueContent)
	 * -------------------------------------------------
	 * Called when a cue that is not handled by this handler needs processing. 
	 * Should return boolean. true = keep going, false = suspend (until resume() is called).
	 * 		cueType: (string)
	 * 			The cue type. 
	 * 		cueContent: (string)
	 * 			The cue content. 
	 */
	"onOtherCue": function(cueType, cueContent)
	{
		return true;
	},
	
	/**
	 * onStartFormatTag(tagName, accum)
	 * -------------------------------------------------
	 * Called when a formatted string (TEXTF) starts a tag.
	 * 		tagName: (string)
	 * 			the tag that is being started.
	 * 		accum: (Array)
	 * 			the accumulator to add the output to (combined into one print() call after formatting).
	 */
	"onStartFormatTag": function(tagName, accum)
	{
		// Blank
	},
	
	/**
	 * onEndFormatTag(tagName, accum)
	 * -------------------------------------------------
	 * Called when a formatted string (TEXTF) ends a tag.
	 * 		tagName: (string)
	 * 			the tag that is being started.
	 * 		accum: (Array)
	 * 			the accumulator to add the output to (combined into one print() call after formatting).
	 */
	"onEndFormatTag": function(tagName, accum)
	{
		// Blank
	},
	
	/**
	 * onFormatText(text, accum)
	 * -------------------------------------------------
	 * Called when a formatted string (TEXTF) needs to process text.
	 * 		text: (string)
	 * 			the text that is being formatted.
	 * 		accum: (Array)
	 * 			the accumulator to add the output to (combined into one print() call after formatting).
	 */
	"onFormatText": function(text, accum)
	{
		// Blank
	}
	
};
