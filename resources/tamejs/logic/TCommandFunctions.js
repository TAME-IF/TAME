/*******************************************************************************
 * Copyright 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var TArithmeticFunctions = TArithmeticFunctions || ((typeof require) !== 'undefined' ? require('./TArithmeticFunctions.js') : null);
var TLogic = TLogic || ((typeof require) !== 'undefined' ? require('./TLogic.js') : null);

var TAMEError = TAMEError || ((typeof require) !== 'undefined' ? require('../TAMEError.js') : null);
var TValue = TValue || ((typeof require) !== 'undefined' ? require('../objects/TValue.js') : null);
// ======================================================================================================

//##[[CONTENT-START

/*****************************************************************************
 Arithmetic function entry points.
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

/**
 * Increments the runaway command counter and calls the command.  
 * Command index.
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param blockLocal (TValueHash) the local variables on the block call.
 * @param command (TCommand) the command object.
 * @throws TAMEInterrupt if an interrupt occurs. 
 */
TCommandFunctions.execute = function(index, request, response, blockLocal, command)
{
	TCommandFunctions[index].doCommand(request, response, blockLocal, command);
	response.incrementAndCheckCommandsExecuted();
}

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

//##[[CONTENT-END


// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TCommandFunctions;
// =========================================================================
