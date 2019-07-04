/*****************************************************************************
 * TAME.newResponseHandler(options)
 *****************************************************************************
 * Factory function - creates a new response handler (for aiding in browser functions).
 * Handles all standard cues and provides a way via a function to handle other cues.
 * This accumulates the results of contiguous "text" and "textf" cues before "print()" is called.
 * 
 * @param options options object, consisting of the following optional fields that reference functions:
 * 		print: fn(text)
 * 			Called when a string needs printing (may contain HTML or other things).
 * 		onStart: fn()
 * 			Called before cues start processing.
 * 			Should disable input.
 * 		onEnd: fn()
 * 			Called when cues stop processing, and the end is reached.
 * 			Should enable input.
 * 		onSuspend: fn()
 * 			Called when a call to process a cue initiates a suspension (processing stops, but not due to a pause).
 * 			Should disable input.
 * 		onResume: fn()
 * 			Called when cues process again.
 * 			Should disable input.
 * 		onPause: fn()
 * 			Called after a "pause" cue is processed. 
 * 			Should prompt for continuation somehow, then call resume() after the user "continues."
 * 		onQuit: fn()
 * 			Called after a "quit" cue is processed.
 * 			Should stop input and prevent further input. 
 * 		onError: fn(message)
 * 			Called after an "error" cue is processed.
 * 			Should make an error message appear on screen. Dismissable.
 * 				message: (string)
 * 					The message to display. 
 * 		onFatal: fn(message)
 * 			Called after a "fatal" cue is processed.
 * 			Should make a message appear on screen, and stop input as though a "quit" occurred. 
 * 				message: (string)
 * 					The message to display. 
 * 		onTrace: fn(type, content)
 * 			Called after a "trace" cue is processed.
 * 				type: (string)
 * 					The tracing type (internal, function, ... etc.). 
 * 				content: (string)
 * 					The trace message content. 
 * 		onOtherCue: fn(cueType, cueContent) 
 * 			Called when a cue that is not handled by this handler needs processing. 
 * 			Should return boolean. true = keep going, false = suspend (until resume() is called).
 * 				cueType: (string)
 * 					The cue type. 
 * 				cueContent: (string)
 * 					The cue content. 
 * 		onStartFormatTag: fn(tagname, accum)
 * 			Called when a formatted string (TEXTF) starts a tag.
 * 				tagname: (string)
 * 					the tag that is being started.
 * 				accum: (Array)
 * 					the accumulator to add the output to (combined into one print() call after formatting).
 * 		onEndFormatTag: fn(tagname, accum)
 * 			Called when a formatted string (TEXTF) ends a tag.
 * 				tagname: (string)
 * 					the tag that is being started.
 * 				accum: (Array)
 * 					the accumulator to add the output to (combined into one print() call after formatting).
 * 		onFormatText: fn(text, accum)
 * 			Called when a formatted string (TEXTF) needs to process text.
 * 				text: (string)
 * 					the text that is being formatted.
 * 				accum: (Array)
 * 					the accumulator to add the output to (combined into one print() call after formatting).
 * 
 * @return (TResponseHandler) 
 * 		a new response handler that calls the provided functions during response read.
 *
 * 
 *****************************************************************************
 * TResponseHandler.reset()
 *****************************************************************************
 * Resets the cue read state.
 * 
 * 
 *****************************************************************************
 * TResponseHandler.prepare(response)
 *****************************************************************************
 * Prepares the response for read.
 * 
 * @param response (TResponse)
 * 		the response from a TAME.initialize(...) or TAME.interpret(...) call.
 * 
 * 
 *****************************************************************************
 * TResponseHandler.resume()
 *****************************************************************************
 * Resumes reading the response.
 * Will return once reading is suspended or ends.
 * See prepare(response) for starting.
 * 
 * @return true if more unprocessed cues remain, or false if not. 
 * 
 * 
 *****************************************************************************
 * TResponseHandler.process(response)
 *****************************************************************************
 * Prepares the response for read and calls resume.
 * Convenience for: 
 * this.prepare(response);
 * this.resume();
 *
 * @param response (TResponse)
 * 		the response from a TAME.initialize(...) or TAME.interpret(...) call.
 * 
 * @return true if more unprocessed cues remain, or false if not. 
 * 
 * 
 *****************************************************************************
 * TAME.readModule(dataView)
 *****************************************************************************
 * Creates a new module from a DataView.
 * 
 * @param dataView (DataView) 
 * 		a DataView wrapping the serialized module.
 * 
 * @return (TModule) 
 * 		a deserialized module.
 *
 * 
 *****************************************************************************
 * TAME.newContext(module)
 *****************************************************************************
 * Creates a new context for a module.
 * 
 * @param module (TModule) 
 * 		the TAME module to create a context for.
 * 
 * @return (TModuleContext) 
 * 		a new module context, or null if no usable module.
 * 
 * 
 *****************************************************************************
 * TAME.initialize(context, traceTypes)
 *****************************************************************************
 * Initializes a context. Must be called after a new context and game is started.
 * 
 * @param context (TModuleContext) 
 * 		the module context.
 * @param traceTypes 
 * 		(boolean) if true, add all trace types, false for none.
 * 		(Array) list of tracing types (case-insensitive).
 * 		(Object) map of tracing types (case-insensitive).
 * 
 * @return (TResponse) 
 * 		the response from the initialize.
 * 
 * 
 *****************************************************************************
 * TAME.interpret(context, inputMessage, traceTypes)
 *****************************************************************************
 * Interprets and performs actions.
 * 
 * @param context (TModuleContext) 
 * 		the module context.
 * @param inputMessage (string) 
 * 		the input message to interpret (usually typed by the user).
 * @param traceTypes 
 * 		(boolean) if true, add all trace types, false for none.
 * 		(Array) list of tracing types (case-insensitive).
 * 		(Object) map of tracing types (case-insensitive).
 * 
 * @return (TResponse) 
 * 		the response.
 * 
 * 
 *****************************************************************************
 * TAME.inspect(context, elementIdentity, variable)
 *****************************************************************************
 * Inspects an element or element's value.
 * 
 * @param context (TModuleContext) 
 * 		the module context.
 * @param elementIdentity (string) 
 * 		the identity of a non-archetype element.
 * @param variable (string) [OPTIONAL] 
 * 		the name of the variable to inspect.
 *  
 * @return (Object) 
 * 		the queried identifiers as keys with debug strings as values.
 * 
 *****************************************************************************
 * TAME.parseFormatted(content, startFormatTag, endFormatTag, formatText)
 *****************************************************************************
 * Assists in parsing a cue with formatted text (TEXTF cue), or one known to have formatted text.
 * The target functions passed in are provided an accumulator array to push generated text into. 
 * On return, this function returns the accumulator's contents joined into a string.
 * 
 * @param sequence (string) 
 * 		the character sequence to parse.
 * @param tagStartFunc fn(tagName, accum) 
 * 		the function called on tag start. arguments: tagName (string), accumulator (Array)  
 * @param tagEndFunc fn(tagName, accum) 
 * 		the function called on tag end. arguments: tagName (string), accumulator (Array)
 * @param textFunc fn(text, accum)
 * 		the function called on tag contents. arguments: text (string), accumulator (Array)
 * 
 * @return (string) 
 * 		the full accumulated result.  
 *
 *****************************************************************************/
