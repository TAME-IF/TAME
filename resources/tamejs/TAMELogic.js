/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var Util = Util || ((typeof require) !== 'undefined' ? require('../Util.js') : null);
var TAMEError = TAMEError || ((typeof require) !== 'undefined' ? require('../TAMEError.js') : null);
var TValue = TValue || ((typeof require) !== 'undefined' ? require('../objects/TValue.js') : null);
var TRequest = TRequest || ((typeof require) !== 'undefined' ? require('../objects/TRequest.js') : null);
var TResponse = TResponse || ((typeof require) !== 'undefined' ? require('../objects/TResponse.js') : null);
// ======================================================================================================

//##[[CONTENT-START

/*****************************************************************************
 Arithmetic function entry points.
 *****************************************************************************/
var TArithmeticFunctions = 
[
 	/* ABSOLUTE */
	{
		"name": 'ABSOLUTE',
		"symbol": '+',
		"binary": false,
		"doOperation": TValue.absolute
	},
	
 	/* NEGATE */
	{
		"name": 'NEGATE',
		"symbol": '-',
		"binary": false,
		"doOperation": TValue.negate
	},
	
 	/* LOGICAL NOT */
	{
		"name": 'LOGICAL_NOT',
		"symbol": '!',
		"binary": false,
		"doOperation": TValue.logicalNot
	},
	
 	/* NOT */
	{
		"name": 'NOT',
		"symbol": '~',
		"binary": false,
		"doOperation": TValue.not
	},
	
 	/* ADD */
	{
		"name": 'ADD',
		"symbol": '+',
		"binary": true,
		"doOperation": TValue.add
	},
	
 	/* SUBTRACT */
	{
		"name": 'SUBTRACT',
		"symbol": '-',
		"binary": true,
		"doOperation": TValue.subtract
	},
	
 	/* MULTIPLY */
	{
		"name": 'MULTIPLY',
		"symbol": '*',
		"binary": true,
		"doOperation": TValue.multiply
	},
	
 	/* DIVIDE */
	{
		"name": 'DIVIDE',
		"symbol": '/',
		"binary": true,
		"doOperation": TValue.divide
	},
	
 	/* MODULO */
	{
		"name": 'MODULO',
		"symbol": '%',
		"binary": true,
		"doOperation": TValue.modulo
	},
	
 	/* POWER */
	{
		"name": 'POWER',
		"symbol": '**',
		"binary": true,
		"doOperation": TValue.power
	},
	
 	/* AND */
	{
		"name": 'AND',
		"symbol": '&',
		"binary": true,
		"doOperation": TValue.and
	},
	
 	/* OR */
	{
		"name": 'OR',
		"symbol": '|',
		"binary": true,
		"doOperation": TValue.or
	},
	
 	/* XOR */
	{
		"name": 'XOR',
		"symbol": '^',
		"binary": true,
		"doOperation": TValue.xor
	},
	
 	/* LSHIFT */
	{
		"name": 'LSHIFT',
		"symbol": '<<',
		"binary": true,
		"doOperation": TValue.leftShift
	},
	
 	/* RSHIFT */
	{
		"name": 'RSHIFT',
		"symbol": '>>',
		"binary": true,
		"doOperation": TValue.rightShift
	},
	
 	/* RSHIFTPAD */
	{
		"name": 'RSHIFTPAD',
		"symbol": '>>>',
		"binary": true,
		"doOperation": TValue.rightShiftPadded
	},
	
 	/* LOGICAL AND */
	{
		"name": 'LOGICAL_AND',
		"symbol": '&&',
		"binary": true,
		"doOperation": TValue.logicalAnd
	},
	
 	/* LOGICAL OR */
	{
		"name": 'LOGICAL_OR',
		"symbol": '||',
		"binary": true,
		"doOperation": TValue.logicalOr
	},
	
 	/* LOGICAL XOR */
	{
		"name": 'LOGICAL_XOR',
		"symbol": '^^',
		"binary": true,
		"doOperation": TValue.logicalXOr
	},
	
 	/* EQUALS */
	{
		"name": 'EQUALS',
		"symbol": '==',
		"binary": true,
		"doOperation": TValue.equals
	},
	
 	/* NOT EQUALS */
	{
		"name": 'NOT_EQUALS',
		"symbol": '!=',
		"binary": true,
		"doOperation": TValue.notEquals
	},
	
 	/* STRICT EQUALS */
	{
		"name": 'STRICT_EQUALS',
		"symbol": '===',
		"binary": true,
		"doOperation": TValue.strictEquals
	},
	
 	/* STRICT NOT EQUALS */
	{
		"name": 'STRICT_NOT_EQUALS',
		"symbol": '!==',
		"binary": true,
		"doOperation": TValue.strictNotEquals
	},
	
 	/* LESS */
	{
		"name": 'LESS',
		"symbol": '<',
		"binary": true,
		"doOperation": TValue.less
	},
	
 	/* LESS OR EQUAL */
	{
		"name": 'LESS_OR_EQUAL',
		"symbol": '<=',
		"binary": true,
		"doOperation": TValue.lessOrEqual
	},
	
 	/* GREATER */
	{
		"name": 'GREATER',
		"symbol": '>',
		"binary": true,
		"doOperation": TValue.greater
	},
	
 	/* GREATER_OR_EQUAL */
	{
		"name": 'GREATER_OR_EQUAL',
		"symbol": '>=',
		"binary": true,
		"doOperation": TValue.greaterOrEqual
	},
	
];

/* Type enumeration. */
TArithmeticFunctions.Type = 
{
	"ABSOLUTE": 0,
	"NEGATE": 1,
	"LOGICAL_NOT": 2,
	"NOT": 3,
	"ADD": 4,
	"SUBTRACT": 5,
	"MULTIPLY": 6,
	"DIVIDE": 7,
	"MODULO": 8,
	"POWER": 9,
	"AND": 10,
	"OR": 11,
	"XOR": 12,
	"LSHIFT": 13,
	"RSHIFT": 14,
	"RSHIFTPAD": 15,
	"LOGICAL_AND": 16,
	"LOGICAL_OR": 17,
	"LOGICAL_XOR": 18,
	"EQUALS": 19,
	"NOT_EQUALS": 20,
	"STRICT_EQUALS": 21,
	"STRICT_NOT_EQUALS": 22,
	"LESS": 23,
	"LESS_OR_EQUAL": 24,
	"GREATER": 25,
	"GREATER_OR_EQUAL": 26
};

/*****************************************************************************
 Command entry points.
 *****************************************************************************/
var TCommandFunctions = 
[
	/* NOOP */
	{
		"name": 'NOOP',
		"doCommand": function(request, response, blockLocal, command)
		{
			// Does nothing.
		}
	},
	
	/* POP */
	{
		"name": 'POP',
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish.
		}
	},

	/* POPVALUE */
	/* POPLOCALVALUE */
	/* POPELEMENTVALUE */
	/* PUSHVALUE */
	/* PUSHELEMENTVALUE */
	/* ARITHMETICFUNC */
	/* IF */
	/* WHILE */
	/* FOR */
	/* CALL */
	/* ADDCUE */
	/* BREAK */
	/* CONTINUE */
	/* QUIT */
	/* END */
	/* TEXT */
	/* TEXTLN */
	/* TEXTF */
	/* TEXTFLN */
	/* PAUSE */
	/* WAIT */
	/* TIP */
	/* INFO */
	/* ASBOOLEAN */
	/* ASINT */
	/* ASFLOAT */
	/* ASSTRING */
	/* STRLEN */
	/* STRREPLACE */
	/* STRINDEX */
	/* STRLASTINDEX */
	/* STRCONTAINS */
	/* STRCONTAINSPATTERN */
	/* STRCONTAINSTOKEN */
	/* SUBSTR */
	/* STRLOWER */
	/* STRUPPER */
	/* STRCHAR */
	/* FLOOR */
	/* CEILING */
	/* ROUND */
	/* FIX */
	/* SQRT */
	/* PI */
	/* E */
	/* SIN */
	/* COS */
	/* TAN */
	/* MIN */
	/* MAX */
	/* CLAMP */
	/* RANDOM */
	/* FRANDOM */
	/* GRANDOM */
	/* TIME */
	/* SECONDS */
	/* MINUTES */
	/* HOURS */
	/* DAYS */
	/* FORMATTIME */
	/* OBJECTHASNAME */
	/* OBJECTHASTAG */
	/* ADDOBJECTNAME */
	/* ADDOBJECTTAG */
	/* ADDOBJECTTAGTOALLIN */
	/* REMOVEOBJECTNAME */
	/* REMOVEOBJECTTAG */
	/* REMOVEOBJECTTAGFROMALLIN */
	/* GIVEOBJECT */
	/* REMOVEOBJECT */
	/* MOVEOBJECTSWITHTAG */
	/* OBJECTCOUNT */
	/* HASOBJECT */
	/* OBJECTHASNOOWNER */
	/* PLAYERISINROOM */
	/* PLAYERCANACCESSOBJECT */
	/* BROWSE */
	/* SETPLAYER */
	/* SETROOM */
	/* PUSHROOM */
	/* POPROOM */
	/* SWAPROOM */
	/* CURRENTPLAYERIS */
	/* NOCURRENTPLAYER */
	/* CURRENTROOMIS */
	/* NOCURRENTROOM */
	/* QUEUEACTION */
	/* QUEUEACTIONSTRING */
	/* QUEUEACTIONOBJECT */
	/* QUEUEACTIONOBJECT2 */
	/* IDENTITY */

	/*
		NOOP (null)
		POP (true)
		POPVALUE (true)
		POPLOCALVALUE (true)
		POPELEMENTVALUE (true)
		PUSHVALUE (true)
		PUSHELEMENTVALUE (true)
		ARITHMETICFUNC (true)
		IF (true, true)
		WHILE (true, true)
		FOR (true, true)
		CALL (null, ArgumentType.VALUE)
		ADDCUE (null, ArgumentType.VALUE, ArgumentType.VALUE)
		BREAK ()
		CONTINUE ()
		QUIT ()
		END ()
		TEXT (null, ArgumentType.VALUE)
		TEXTLN (null, ArgumentType.VALUE)
		TEXTF (null, ArgumentType.VALUE)
		TEXTFLN (null, ArgumentType.VALUE)
		PAUSE (null)
		WAIT (null, ArgumentType.VALUE)
		TIP (null, ArgumentType.VALUE)
		INFO (null, ArgumentType.VALUE)
		ASBOOLEAN (ArgumentType.VALUE, ArgumentType.VALUE)
		ASINT (ArgumentType.VALUE, ArgumentType.VALUE)
		ASFLOAT (ArgumentType.VALUE, ArgumentType.VALUE)
		ASSTRING (ArgumentType.VALUE, ArgumentType.VALUE)
		STRLEN (ArgumentType.VALUE, ArgumentType.VALUE)
		STRREPLACE (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		STRINDEX (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		STRLASTINDEX (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		STRCONTAINS (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		STRCONTAINSPATTERN (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		STRCONTAINSTOKEN (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		SUBSTR (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		STRLOWER (ArgumentType.VALUE, ArgumentType.VALUE)
		STRUPPER (ArgumentType.VALUE, ArgumentType.VALUE)
		STRCHAR (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		FLOOR (ArgumentType.VALUE, ArgumentType.VALUE)
		CEILING (ArgumentType.VALUE, ArgumentType.VALUE)
		ROUND (ArgumentType.VALUE, ArgumentType.VALUE)
		FIX (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		SQRT (ArgumentType.VALUE, ArgumentType.VALUE)
		PI (ArgumentType.VALUE)
		E (ArgumentType.VALUE)
		SIN (ArgumentType.VALUE, ArgumentType.VALUE)
		COS (ArgumentType.VALUE, ArgumentType.VALUE)
		TAN (ArgumentType.VALUE, ArgumentType.VALUE)
		MIN (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		MAX (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		CLAMP (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		RANDOM (ArgumentType.VALUE, ArgumentType.VALUE)
		FRANDOM (ArgumentType.VALUE)
		GRANDOM (ArgumentType.VALUE)
		TIME (ArgumentType.VALUE)
		SECONDS (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		MINUTES (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		HOURS (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		DAYS (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		FORMATTIME (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		OBJECTHASNAME (ArgumentType.VALUE, ArgumentType.OBJECT, ArgumentType.VALUE)
		OBJECTHASTAG (ArgumentType.VALUE, ArgumentType.OBJECT, ArgumentType.VALUE)
		ADDOBJECTNAME (null, ArgumentType.OBJECT, ArgumentType.VALUE)
		ADDOBJECTTAG (null, ArgumentType.OBJECT, ArgumentType.VALUE)
		ADDOBJECTTAGTOALLIN (null, ArgumentType.OBJECT_CONTAINER, ArgumentType.VALUE)
		REMOVEOBJECTNAME (null, ArgumentType.OBJECT, ArgumentType.VALUE)
		REMOVEOBJECTTAG (null, ArgumentType.OBJECT, ArgumentType.VALUE)
		REMOVEOBJECTTAGFROMALLIN (null, ArgumentType.OBJECT_CONTAINER, ArgumentType.VALUE)
		GIVEOBJECT (null, ArgumentType.OBJECT_CONTAINER, ArgumentType.OBJECT)
		REMOVEOBJECT (null, ArgumentType.OBJECT)
		MOVEOBJECTSWITHTAG (null, ArgumentType.OBJECT_CONTAINER, ArgumentType.OBJECT_CONTAINER, ArgumentType.VALUE)
		OBJECTCOUNT (ArgumentType.VALUE, ArgumentType.OBJECT_CONTAINER)
		HASOBJECT (ArgumentType.VALUE, ArgumentType.OBJECT_CONTAINER, ArgumentType.OBJECT)
		OBJECTHASNOOWNER (ArgumentType.VALUE, ArgumentType.OBJECT)
		PLAYERISINROOM (ArgumentType.VALUE, ArgumentType.PLAYER, ArgumentType.ROOM)
		PLAYERCANACCESSOBJECT (ArgumentType.VALUE, ArgumentType.PLAYER, ArgumentType.OBJECT)
		BROWSE (null, ArgumentType.OBJECT_CONTAINER)
		SETPLAYER (null, ArgumentType.PLAYER)
		SETROOM (null, ArgumentType.ROOM)
		PUSHROOM (null, ArgumentType.ROOM)
		POPROOM (null)
		SWAPROOM (null, ArgumentType.ROOM)
		CURRENTPLAYERIS (ArgumentType.VALUE, ArgumentType.PLAYER)
		NOCURRENTPLAYER (ArgumentType.VALUE)
		CURRENTROOMIS (ArgumentType.VALUE, ArgumentType.ROOM)
		NOCURRENTROOM (ArgumentType.VALUE)
		QUEUEACTION (null, ArgumentType.ACTION)
		QUEUEACTIONSTRING (null, ArgumentType.ACTION, ArgumentType.VALUE)
		QUEUEACTIONOBJECT (null, ArgumentType.ACTION, ArgumentType.OBJECT)
		QUEUEACTIONOBJECT2 (null, ArgumentType.ACTION, ArgumentType.OBJECT, ArgumentType.OBJECT)
		IDENTITY (ArgumentType.VALUE, ArgumentType.ELEMENT)
	*/
	
];

/* Type enumeration. */
TCommandFunctions.Type = 
{
	NOOP: 0,
	POP: 1,
	POPVALUE: 2,
	POPLOCALVALUE: 3,
	POPELEMENTVALUE: 4,
	PUSHVALUE: 5,
	PUSHELEMENTVALUE: 6,
	ARITHMETICFUNC: 7,
	IF: 8,
	WHILE: 9,
	FOR: 10,
	CALL: 11,
	ADDCUE: 12,
	BREAK: 13,
	CONTINUE: 14,
	QUIT: 15,
	END: 16,
	TEXT: 17,
	TEXTLN: 18,
	TEXTF: 19,
	TEXTFLN: 20,
	PAUSE: 21,
	WAIT: 22,
	TIP: 23,
	INFO: 24,
	ASBOOLEAN: 25,
	ASINT: 26,
	ASFLOAT: 27,
	ASSTRING: 28,
	STRLEN: 29,
	STRREPLACE: 30,
	STRINDEX: 31,
	STRLASTINDEX: 32,
	STRCONTAINS: 33,
	STRCONTAINSPATTERN: 34,
	STRCONTAINSTOKEN: 35,
	SUBSTR: 36,
	STRLOWER: 37,
	STRUPPER: 38,
	STRCHAR: 39,
	FLOOR: 40,
	CEILING: 41,
	ROUND: 42,
	FIX: 43,
	SQRT: 44,
	PI: 45,
	E: 46,
	SIN: 47,
	COS: 48,
	TAN: 49,
	MIN: 50,
	MAX: 51,
	CLAMP: 52,
	RANDOM: 53,
	FRANDOM: 54,
	GRANDOM: 55,
	TIME: 56,
	SECONDS: 57,
	MINUTES: 58,
	HOURS: 59,
	DAYS: 60,
	FORMATTIME: 61,
	OBJECTHASNAME: 62,
	OBJECTHASTAG: 63,
	ADDOBJECTNAME: 64,
	ADDOBJECTTAG: 65,
	ADDOBJECTTAGTOALLIN: 66,
	REMOVEOBJECTNAME: 67,
	REMOVEOBJECTTAG: 68,
	REMOVEOBJECTTAGFROMALLIN: 69,
	GIVEOBJECT: 70,
	REMOVEOBJECT: 71,
	MOVEOBJECTSWITHTAG: 72,
	OBJECTCOUNT: 73,
	HASOBJECT: 74,
	OBJECTHASNOOWNER: 75,
	PLAYERISINROOM: 76,
	PLAYERCANACCESSOBJECT: 77,
	BROWSE: 78,
	SETPLAYER: 79,
	SETROOM: 80,
	PUSHROOM: 81,
	POPROOM: 82,
	SWAPROOM: 83,
	CURRENTPLAYERIS: 84,
	NOCURRENTPLAYER: 85,
	CURRENTROOMIS: 86,
	NOCURRENTROOM: 87,
	QUEUEACTION: 88,
	QUEUEACTIONSTRING: 89,
	QUEUEACTIONOBJECT: 90,
	QUEUEACTIONOBJECT2: 91,
	IDENTITY: 92
};


/****************************************************************************
 * Main logic junk.
 ****************************************************************************/
var TLogic = {};

/**
 * Turns a command into a readable string.
 * @param cmdObject (Object) the command object.
 * @return a string.
 */
TLogic.commandToString = function(cmdObject)
{
	var out = TCommandFunctions[cmdObject.commandIndex].name;
	if (cmdObject.operand0 != null)
		out += ' ' + cmdObject.operand0.toString();
	if (cmdObject.operand1 != null)
		out += ' ' + cmdObject.operand1.toString();
	if (cmdObject.initBlock != null)
		out += " [INIT]";
	if (cmdObject.conditionalBlock != null)
		out += " [CONDITIONAL]";
	if (cmdObject.stepBlock != null)
		out += " [STEP]";
	if (cmdObject.successBlock != null)
		out += " [SUCCESS]";
	if (cmdObject.failureBlock != null)
		out += " [FAILURE]";
	
	return out;
};

/**
 * Executes a block of commands.
 * @param command (Object) the command object.
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param blockLocal (Object) the local variables on the block call.
 * @throws TAMEInterrupt if an interrupt occurs. 
 */
TLogic.executeBlock = function(block, request, response, blockLocal)
{
	response.trace(request, "Start block.");
	Util.each(this.commandList, function(command){
		response.trace(request, "CALL "+TLogic.commandToString(command));
		TLogic.executeCommand(command, request, response, blockLocal);
	});
	response.trace(request, "End block.");
};

/**
 * Increments the runaway command counter and calls the command.  
 * Command index.
 * @param command (Object) the command object.
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param blockLocal (Object) the local variables on the block call.
 * @throws TAMEInterrupt if an interrupt occurs. 
 */
TLogic.executeCommand = function(command, request, response, blockLocal)
{
	TCommandFunctions[command.commandIndex].doCommand(request, response, blockLocal, command);
	response.incrementAndCheckCommandsExecuted();
}

/**
 * Handles initializing a context. Must be called after a new context and game is started.
 * @param context the module context.
 * @param tracing if true, add trace cues.
 * @return (TResponse) the response from the initialize.
 */
TLogic.handleInit = function(context, tracing) 
{
	var request = TRequest.create(context, "[INITIALIZE]", tracing);
	var response = new TResponse();
	
	response.interpretNanos = 0;
	var time = Date.now();

	try 
	{
		TLogic.initializeContext(request, response);
		TLogic.processActionLoop(request, response);
	} 
	catch (err) 
	{
		if (err instanceof TAMEInterrupt)
		{
			if (err.type != TAMEInterrupt.Type.Quit)
				response.addCue(TAMEConstants.Cue.ERROR, err.type+" interrupt was thrown.");
		}
		else if (err instanceof TAMEError)
			response.addCue(TAMEConstants.Cue.FATAL, err.message);
		else
			response.addCue(TAMEConstants.Cue.FATAL, err);
	}

	time = (Date.now() - time) * 1000000; // ms to ns
	
	response.requestNanos = time;

	return response;
};

/**
 * Handles interpretation and performs actions.
 * @param context (object) the module context.
 * @param inputMessage (string) the input message to interpret.
 * @param tracing (boolean) if true, add trace cues.
 * @return (TResponse) the response.
 */
TLogic.handleRequest = function(context, inputMessage, tracing)
{
	var request = TRequest.create(context, inputMessage, tracing);
	var response = new TResponse();

	var time = Date.now();
	var interpreterContext = TLogic.interpret(request);
	response.interpretNanos = (Date.now() - time) * 1000000; 

	time = Date.now();
	
	try 
	{
		TLogic.enqueueInterpretedAction(request, response, interpreterContext);
		TLogic.processActionLoop(request, response);
	} 
	catch (err) 
	{
		if (err instanceof TAMEInterrupt)
		{
			if (err.type != TAMEInterrupt.Type.Quit)
				response.addCue(TAMEConstants.Cue.ERROR, err.type+" interrupt was thrown.");
		}
		else if (err instanceof TAMEError)
			response.addCue(TAMEConstants.Cue.FATAL, err.message);
		else
			response.addCue(TAMEConstants.Cue.FATAL, err);
	}
	
	response.requestNanos = (Date.now() - time) * 1000000;
	return response;
};

/**
 * Performs the necessary tasks for calling an object block.
 * Ensures that the block is called cleanly.
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param elementContext (object) the context that the block is executed through.
 * @param block (TBlock) the block to execute.
 * @param localValues (object) the local values to set on invoke.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callBlock = function(request, response, elementContext, block, localValues)
{
	response.trace(request, "Pushing Context:"+elementContext.identity+"...");
	request.pushContext(elementContext);
	
	var blockLocal = {};
	// set locals
	Util.each(localValues, function(value, key){
		response.trace(request, "Setting local variable \""+key+"\" to \""+value+"\"");
		blockLocal.put(key, value);
	});

	try {
		block.execute(request, response, blockLocal);
	} catch (t) {
		throw t;
	} finally {
		response.trace(request, "Popping Context:"+elementContext.identity+"...");
		request.popContext();
	}
	
	request.checkStackClear();
	
};


/**
 * Interprets the input on the request.
 * @param request (TRequest) the request.
 * @return a new interpreter context using the input.
 */
TLogic.interpret = function(request)
{
	var tokens = request.inputMessage.toLowerCase().split("\\s+");
	var interpreterContext = 
	{
		"tokens": tokens,
		"tokenOffset": 0,
		"objects": [],
		"action": null,
		"modeLookedUp": false,
		"mode": null,
		"targetLookedUp": false,
		"target": null,
		"conjugateLookedUp": false,
		"conjugate": null,
		"object1LookedUp": false,
		"object1": null,
		"object2LookedUp": false,
		"object2": null,
		"objectAmbiguous": false
	};

	var moduleContext = request.moduleContext;
	TLogic.interpretAction(moduleContext, interpreterContext);

	var action = moduleContext.module.actions[interpreterContext.action];
	if (action == null)
		return interpreterContext;

	switch (action.type)
	{
		default:
		case TAMEConstants.ActionType.GENERAL:
			return interpreterContext;
		case TAMEConstants.ActionType.OPEN:
			TLogic.interpretOpen(interpreterContext);
			return interpreterContext;
		case TAMEConstants.ActionType.MODAL:
			TLogic.interpretMode(action, interpreterContext);
			return interpreterContext;
		case TAMEConstants.ActionType.TRANSITIVE:
			TLogic.interpretObject1(moduleContext, interpreterContext);
			return interpreterContext;
		case TAMEConstants.ActionType.DITRANSITIVE:
			if (TLogic.interpretObject1(moduleContext, interpreterContext))
				if (TLogic.interpretConjugate(action, interpreterContext))
					TLogic.interpretObject2(moduleContext, interpreterContext);
			return interpreterContext;
	}
	
};

/**
 * Interprets an action from the input line.
 * @param moduleContext (Object) the module context.
 * @param interpreterContext (Object) the TAMEInterpreterContext.
 */
TLogic.interpretAction = function(moduleContext, interpreterContext)
{
	var module = moduleContext.module;
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;

	while (index < tokens.length)
	{
		if (sb.length() > 0)
			sb += ' ';
		sb += tokens[index];
		index++;

		var next = module.getActionByName(sb);
		if (next != null)
		{
			interpreterContext.action = next.identity;
			interpreterContext.tokenOffset = index;
		}
	
	}
	
};

/**
 * Interprets an action mode from the input line.
 * @param action (object:action) the action to use.
 * @param interpreterContext (object) the interpreterContext.
 */
TLogic.interpretMode = function(action, interpreterContext)
{
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;

	while (index < tokens.length)
	{
		if (sb.length() > 0)
			sb += ' ';
		sb += tokens[index];
		index++;

		interpreterContext.modeLookedUp = true;
		var next = sb;
		
		if (action.extraStrings.indexOf(sb) >= 0)
		{
			interpreterContext.mode = next;
			interpreterContext.tokenOffset = index;
		}
		
	}
	
};

/**
 * Interprets open target.
 * @param interpreterContext the TAMEInterpreterContext.
 */
TLogic.interpretOpen = function(interpreterContext)
{
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;
	
	while (index < tokens.length)
	{
		interpreterContext.targetLookedUp = true;
		if (sb.length() > 0)
			sb += ' ';
		sb += tokens[index];
		index++;
	}
	
	interpreterContext.target = sb.length() > 0 ? sb : null;
	interpreterContext.tokenOffset = index;
};

/**
 * Interprets an action conjugate from the input line (like "with" or "on" or whatever).
 * @param action the action to use.
 * @param interpreterContext the TAMEInterpreterContext.
 */
TLogic.interpretConjugate = function(action, interpreterContext)
{
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;
	var out = false;

	while (index < tokens.length)
	{
		if (sb.length() > 0)
			sb += ' ';
		sb += tokens[index];
		index++;
		
		interpreterContext.conjugateLookedUp = true;
		if (action.extraStrings.indexOf(sb) >= 0)
		{
			interpreterContext.tokenOffset = index;
			out = true;
		}
		
	}

	interpreterContext.conjugateFound = out;
	return out;
};

/**
 * Interprets the first object from the input line.
 * This is context-sensitive, as its priority is to match objects on the current
 * player's person, as well as in the current room. These checks are skipped if
 * the player is null, or the current room is null.
 * The priority order is player inventory, then room contents, then world.
 * @param moduleContext the module context.
 * @param interpreterContext the TAMEInterpreterContext.
 */
TLogic.interpretObject1 = function(moduleContext, interpreterContext)
{
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;

	while (index < tokens.length)
	{
		if (sb.length() > 0)
			sb += ' ';
		sb += tokens[index];
		index++;
		
		interpreterContext.object1LookedUp = true;
		var out = TLogic.findAccessibleObjectsByName(moduleContext, sb, interpreterContext.objects, 0);
		if (out > 1)
		{
			interpreterContext.objectAmbiguous = true;
			interpreterContext.object1 = null;
			interpreterContext.tokenOffset = index;
		}
		else if (out > 0)
		{
			interpreterContext.objectAmbiguous = false;
			interpreterContext.object1 = interpreterContext.objects[0];
			interpreterContext.tokenOffset = index;
		}
	}
		
	return interpreterContext.object1 != null;
};

/**
 * Interprets the second object from the input line.
 * This is context-sensitive, as its priority is to match objects on the current
 * player's person, as well as in the current room. These checks are skipped if
 * the player is null, or the current room is null.
 * The priority order is player inventory, then room contents, then world.
 * @param moduleContext the module context.
 * @param interpreterContext the TAMEInterpreterContext.
 */
TLogic.interpretObject2 = function(moduleContext, interpreterContext)
{
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;

	while (index < tokens.length)
	{
		if (sb.length() > 0)
			sb += ' ';
		sb += tokens[index];
		index++;
		
		interpreterContext.object2LookedUp = true;
		var out = TLogic.findAccessibleObjectsByName(moduleContext, sb, interpreterContext.objects, 0);
		if (out > 1)
		{
			interpreterContext.objectAmbiguous = true;
			interpreterContext.object2 = null;
			interpreterContext.tokenOffset = index;
		}
		else if (out > 0)
		{
			interpreterContext.objectAmbiguous = false;
			interpreterContext.object2 = interpreterContext.objects[0];
			interpreterContext.tokenOffset = index;
		}
	}
		
	return interpreterContext.object2 != null;
};

/**
 * Returns all objects in the accessible area by an object name read from the interpreter.
 * The output stops if the size of the output array is reached.
 * @param moduleContext the module context.
 * @param name the name from the interpreter.
 * @param outputArray the output vector of found objects.
 * @param arrayOffset the starting offset into the array to put them.
 * @return the amount of objects found.
 */
TLogic.findAccessibleObjectsByName = function(moduleContext, name, outputArray, arrayOffset)
{
	// TODO: Finish this.
};

TLogic.enqueueInterpretedAction = function(request, response, interpreterContext) 
{
	// TODO: Finish this.
};

TLogic.processActionLoop = function(request, response) 
{
	// TODO: Finish this.
};

TLogic.initializeContext = function(request, response) 
{
	// TODO: Finish this.
};

/*
doAfterRequest(TAMERequest, TAMEResponse)
doUnknownAction(TAMERequest, TAMEResponse)
doActionGeneral(TAMERequest, TAMEResponse, TAction)
doActionOpen(TAMERequest, TAMEResponse, TAction, String)
doActionModal(TAMERequest, TAMEResponse, TAction, String)
doActionTransitive(TAMERequest, TAMEResponse, TAction, TObject)
doActionDitransitive(TAMERequest, TAMEResponse, TAction, TObject, TObject)
callAmbiguousAction(TAMERequest, TAMEResponse, TAction)
callCheckActionForbidden(TAMERequest, TAMEResponse, TAction)
callBadAction(TAMERequest, TAMEResponse, TAction)
callWorldBadActionBlock(TAMERequest, TAMEResponse, TAction, TWorldContext)
callPlayerBadActionBlock(TAMERequest, TAMEResponse, TAction, TPlayerContext)
callActionIncomplete(TAMERequest, TAMEResponse, TAction)
callWorldActionIncompleteBlock(TAMERequest, TAMEResponse, TAction, TWorldContext)
callPlayerActionIncompleteBlock(TAMERequest, TAMEResponse, TAction, TPlayerContext)
callActionFailed(TAMERequest, TAMEResponse, TAction)
callWorldActionFailBlock(TAMERequest, TAMEResponse, TAction, TWorldContext)
callPlayerActionFailBlock(TAMERequest, TAMEResponse, TAction, TPlayerContext)
callPlayerActionForbiddenBlock(TAMERequest, TAMEResponse, TAction, TPlayerContext)
callRoomActionForbiddenBlock(TAMERequest, TAMEResponse, TAction, TRoomContext)
callInitOnContexts(TAMERequest, TAMEResponse, Iterator<? extends TElementContext<?>>)
callInitBlock(TAMERequest, TAMEResponse, TElementContext<?>)
 */

//TODO: Finish

//##[[CONTENT-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TLogic;
// =========================================================================

