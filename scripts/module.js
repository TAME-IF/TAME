
/*************************************************************************
 * TAME v2017.08.09.020045731-SNAPSHOT with Embedded Module
 * (C) 2016-2017 Matthew Tropiano
 * https://tame-if.com
 *
 * Module Information:
 * Title: Lifecycle Test
 *************************************************************************/

var TAME = new (function(theader, tactions, telements){

this.version = "2017.08.09.020045731-SNAPSHOT";


/*****************************************************************************
 Utilities
 *****************************************************************************/
var Util = {};

// Nanosecond time (for timing stuff). Resolution varies by environment.
// Stub here.
Util.nanoTime = null;

// toBase64 - converts a string to base64.
//Stub here.
Util.toBase64 = null;

// fromBase64 - converts a string from base64.
// Stub here.
Util.fromBase64 = null; 

// Smarter foreach.
Util.each = function(obj, func)
{
	for (var x in obj) 
		if (obj.hasOwnProperty(x)) 
			func(obj[x], x, obj.length);
};

// Array remove
Util.arrayRemove = function(arr, obj)
{
	for (var i = 0; i < arr.length; i++) 
		if (arr[i] == obj)
		{
			arr.splice(i, 1);
			return true;
		}
	
	return false;
};

// Adds a string to a lookup hash.
Util.objectStringAdd = function(hash, identity, str)
{
	var arr = hash[identity];
	if (!arr)
		arr = hash[identity] = {};
	if (!arr[str])
		arr[str] = true;
};

// Remove a string from a lookup hash.
Util.objectStringRemove = function(hash, identity, str)
{
	var arr = hash[identity];
	if (!arr)
		return;
	if (arr[str])
		delete arr[str];
};

// Checks if a string is in a lookup hash.
// True if contained, false if not.
Util.objectStringContains = function(hash, identity, str)
{
	var arr = hash[identity];
	return (arr && arr[str]);
};

// Mapify - [object, ...] to {object.memberKey -> object, ...}
Util.mapify = function(objlist, memberKey, multi) 
{
	var out = {}; 
	for (var x in objlist) 
		if (objlist.hasOwnProperty(x))
		{				
			var chain = out[objlist[x][memberKey]];
			if (multi && chain)
			{
				if (Object.prototype.toString.call(chain) === '[object Array]')
					chain.push(objlist[x]); 
				else
					out[objlist[x][memberKey]] = [out[objlist[x][memberKey]], objlist[x]]; 
			}
			else
				out[objlist[x][memberKey]] = objlist[x]; 
		}
	return out;
};

// Pairify - [object, ...] to {object.memberKey -> object.memberValue, ...}
Util.pairify = function(objlist, memberKey, memberValue, multi) 
{
	var out = {}; 
	for (var x in objlist) 
		if (objlist.hasOwnProperty(x))
		{				
			var chain = out[objlist[x][memberKey]];
			if (multi && chain)
			{
				if (Object.prototype.toString.call(chain) === '[object Array]')
					chain.push(objlist[x][memberValue]); 
				else
					out[objlist[x][memberKey]] = [out[objlist[x][memberKey]], objlist[x][memberValue]]; 
			}
			else
				out[objlist[x][memberKey]] = objlist[x][memberValue]; 
		}
	return out;
};

// replaceall - Return a string that replaces all matching patterns in inputstr with replacement
Util.replaceAll = function(inputstr, expression, replacement) 
{
	return inputstr.replace(new RegExp(expression, 'g'), replacement);
};

// withEscChars - Return a string that includes its escape characters.
Util.withEscChars = function(text) 
{
	var t = JSON.stringify(text);
	return t.substring(1, t.length - 1);
};


// formatDate - Return a string that is a formatted date. Similar to SimpleDateFormat in Java.
Util.formatDate = function(date, formatstring, utc) 
{
	// Enumerations and stuff.
	var _DAYINWEEK = [
		['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'],
		['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
		['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']
	];
	var _MONTHINYEAR = [
		['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'], // MMM
		['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'] // MMMM
	];

	var _PAD = function(value, len)
	{
		value = parseInt(value, 10);
		var out = '';
		do {
			out = value % 10 + out;
			value = Math.floor(value / 10);
			len--;
		} while (value > 0);

		while (len-- > 0)
			out = '0' + out;

		return out;
	}

	// The regular expression for finding all pertinent tokens.
	var _DATEFORMATREGEX = /G+|Y+|y+|M+|w+|W+|D+|d+|F+|E+|a+|H+|k+|K+|h+|m+|s+|S+|z+|Z+|'.*'/g;

	/* Mapping of token types to value function. All return strings. */
	var _TOKENFUNCS =
	{
		"G": function(token, date, utc)
		{
			if ((utc ? date.getUTCFullYear() : date.getFullYear()) < 0)
				return token.length === 1 ? 'B' : 'BC';
			else
				return token.length === 1 ? 'A' : 'AD';
		},
		"Y": function(token, date, utc)
		{
			var year = (utc ? date.getUTCFullYear() : date.getFullYear());
			if (token.length === 2)
				return Math.floor(year % 100)+'';
			else
				return _PAD(year, token.length);
		},
		"y": function(token, date, utc)
		{
			var year = (utc ? date.getUTCFullYear() : date.getFullYear());
			if (token.length === 2)
				return Math.floor(year % 100)+'';
			else
				return _PAD(year, token.length);
		},
		"M": function(token, date, utc)
		{
			var month = (utc ? date.getUTCMonth() : date.getMonth());
			if (token.length === 1)
				return (month + 1)+'';
			else if (token.length === 2)
				return _PAD(month + 1, 2);
			else if (token.length === 3)
				return _MONTHINYEAR[0][month];
			else
				return _MONTHINYEAR[1][month];
		},
		"d": function(token, date, utc)
		{
			var d = (utc ? date.getUTCDate() : date.getDate());
			if (token.length === 1)
				return d+'';
			else
				return _PAD(d, token.length);
		},
		"E": function(token, date, utc)
		{
			var day = (utc ? date.getUTCDay() : date.getDay());
			if (token.length === 1)
				return day+'';
			else if (token.length === 2)
				return _DAYINWEEK[0][day];
			else if (token.length === 3)
				return _DAYINWEEK[1][day];
			else
				return _DAYINWEEK[2][day];
		},
		"a": function(token, date, utc)
		{
			var pm = (utc ? date.getUTCHours() >= 12 : date.getHours() >= 12);
			if (token.length === 1)
				return pm ? 'P' : 'A';
			else
				return pm ? 'PM' : 'AM';
		},
		"H": function(token, date, utc)
		{
			var hours = (utc ? date.getUTCHours() : date.getHours());
			if (token.length === 1)
				return hours+'';
			else
				return _PAD(hours, token.length);
		},
		"k": function(token, date, utc)
		{
			var hours = (utc ? date.getUTCHours() : date.getHours()) + 1;
			if (token.length === 1)
				return hours+'';
			else
				return _PAD(hours, token.length);
		},
		"K": function(token, date, utc)
		{
			var hours = Math.floor((utc ? date.getUTCHours() : date.getHours()) % 12);
			if (token.length === 1)
				return hours+'';
			else
				return _PAD(hours, token.length);
		},
		"h": function(token, date, utc)
		{
			var hours = Math.floor((utc ? date.getUTCHours() : date.getHours()) % 12);
			if (hours === 0)
				hours = 12;
			if (token.length === 1)
				return hours+'';
			else
				return _PAD(hours, token.length);
		},
		"m": function(token, date, utc)
		{
			var minutes = (utc ? date.getUTCMinutes() : date.getMinutes());
			if (token.length === 1)
				return minutes+'';
			else
				return _PAD(minutes, token.length);
		},
		"s": function(token, date, utc)
		{
			var seconds = (utc ? date.getUTCSeconds() : date.getSeconds());
			if (token.length === 1)
				return seconds+'';
			else
				return _PAD(seconds, token.length);
		},
		"S": function(token, date, utc)
		{
			var millis = (utc ? date.getUTCMilliseconds() : date.getMilliseconds());
			if (token.length === 1)
				return Math.round(millis / 100) + '';
			else if (token.length === 2)
				return _PAD(Math.round(millis / 10), 2);
			else
				return _PAD(millis, 3);
		},
		"z": function(token, date, utc)
		{
			var offset = (date.getTimezoneOffset() / 60) * 100;
			return (offset > 0 ? '-' : '') +_PAD(offset, 4)+'';
		},
		"Z": function(token, date, utc)
		{
			var offset = (date.getTimezoneOffset() / 60) * 100;
			return (offset > 0 ? '-' : '') +_PAD(offset, 4)+'';
		},
		"'": function(token, date, utc)
		{
			if (token.length === 2)
				return "'";
			else
				return token.substring(1, token.length - 1);
		}
	};

	date = new Date(date);
	var out = formatstring;
	var tokens = formatstring.match(_DATEFORMATREGEX);

	for (var i = tokens.length - 1; i >= 0; i--)
	{
		var element = tokens[i];
		var func = _TOKENFUNCS[element[0]];
		if (func)
			out = out.replace(element, func(element, date, utc));
	}

	return out;
};

/**
 * Assists in parsing a cue with formatted text (TEXTF cue), or one known to have formatted text.
 * @param sequence the character sequence to parse.
 * @param tagStartFunc the function called on tag start. Should take one argument: the tag name.  
 * @param tagEndFunc the function called on tag end. Should take one argument: the tag name.  
 * @param textFunc the function called on tag contents (does not include tags - it is recommended to maintain a stack). Should take one argument: the text read inside tags.  
 */
Util.parseFormatted = function(sequence, tagStartFunc, tagEndFunc, textFunc)
{
	var builder = '';
	var tagStack = [];
	
	var emitText = function()
	{
		if (builder.length == 0)
			return;
		
		textFunc(builder);
		builder = '';
	}

	var emitTag = function()
	{
		if (builder.length == 0)
			return;

		var tag = builder;
		builder = '';
		
		if (tag == '/')
		{
			if (tagStack.length == 0)
				return;
			tagEndFunc(tagStack.pop());
		}
		else
		{
			tagStack.push(tag);
			tagStartFunc(tag);
		}
	}
	
	const STATE_TEXT = 0;
	const STATE_TAG_MAYBE = 1;
	const STATE_TAG = 2;
	const STATE_TAG_END_MAYBE = 3;
	
	var state = STATE_TEXT;
	var len = sequence.length, i = 0;

	while (i < len)
	{
		var c = sequence.charAt(i);

		switch (state)
		{
			case STATE_TEXT:
			{
				if (c == '[')
					state = STATE_TAG_MAYBE;
				else
					builder += (c);
			}
			break;

			case STATE_TAG_MAYBE:
			{
				if (c == '[')
				{
					state = STATE_TEXT;
					builder += (c);
				}
				else
				{
					state = STATE_TAG;
					emitText();
					i--;
				}
			}
			break;
			
			case STATE_TAG:
			{
				if (c == ']')
					state = STATE_TAG_END_MAYBE;
				else
					builder += (c);
			}
			break;
			
			case STATE_TAG_END_MAYBE:
			{
				if (c == ']')
				{
					state = STATE_TAG;
					builder += (c);
				}
				else
				{
					state = STATE_TEXT;
					emitTag();
					i--;
				}
			}
			break;
		}
		
		i++;
	}
	
	if (state == STATE_TAG_END_MAYBE)
		emitTag();
	
	emitText();
	while (tagStack.length > 0)
		tagEndFunc(tagStack.pop());
};



Util.nanoTime = function()
{
	// s,ns to ns (ns res)
	var t = process.hrtime();
	return t[0] * 1e9 + t[1];
};

Util.toBase64 = (function()
{
	if (Buffer.from)
	{
		return function(text) {
			return Buffer.from(text).toString('base64');
		};
	}
	else
	{
		return function(text) {
			return (new Buffer(text)).toString('base64');
		};
	}
})();

Util.fromBase64 = (function()
{
	if (Buffer.from)
	{
		return function(data) {
			return Buffer.from(data, 'base64').toString('utf8');
		};
	}
	else
	{
		return function(data) {
			return (new Buffer(data, 'base64')).toString('utf8');
		};
	}
})();


var TAMEConstants = {};
TAMEConstants.ActionType = {};
TAMEConstants.RestrictionType = {};
TAMEConstants.Cue = {};
TAMEConstants.Identity = {};

TAMEConstants.ActionType.GENERAL = 0;
TAMEConstants.ActionType.TRANSITIVE = 1;
TAMEConstants.ActionType.DITRANSITIVE = 2;
TAMEConstants.ActionType.MODAL = 3;
TAMEConstants.ActionType.OPEN = 4;

TAMEConstants.RestrictionType.FORBID = 0;
TAMEConstants.RestrictionType.ALLOW = 1;

TAMEConstants.Cue.QUIT = "QUIT";
TAMEConstants.Cue.TEXT = "TEXT";
TAMEConstants.Cue.TEXTF = "TEXTF";
TAMEConstants.Cue.WAIT = "WAIT";
TAMEConstants.Cue.PAUSE = "PAUSE";
TAMEConstants.Cue.TRACE = "TRACE";
TAMEConstants.Cue.TIP = "TIP";
TAMEConstants.Cue.INFO = "INFO";
TAMEConstants.Cue.ERROR = "ERROR";
TAMEConstants.Cue.FATAL = "FATAL";

TAMEConstants.Identity.ROOM = "room";
TAMEConstants.Identity.PLAYER = "player";
TAMEConstants.Identity.WORLD = "world";

TAMEConstants.DEFAULT_RUNAWAY_THRESHOLD = 100000;
TAMEConstants.DEFAULT_FUNCTION_DEPTH = 256;
TAMEConstants.RETURN_VARIABLE = "-. 0Return0 .-";


/*****************************************************************************
 Exception handling.
 *****************************************************************************/
var TAMEError = function(type, message)
{
	this.type = type;
	this.message = message;
};

TAMEError.Type = 
{
	"Module": "Module",
	"ModuleExecution": "ModuleExecution",
	"ModuleState": "ModuleState",
	"Arithmetic": "Arithmetic",
	"ArithmeticStackState": "ArithmeticStackState",
	"RunawayRequest": "RunawayRequest",
	"UnexpectedValue": "UnexpectedValue",
	"UnexpectedValueType": "UnexpectedValueType"
};

TAMEError.prototype.toString = function()
{
	return "TAMEError: "+ this.type + ": " + this.message;
};

// Convenience Constructors

TAMEError.Module = function(message) {return new TAMEError(TAMEError.Type.Module, message);};
TAMEError.ModuleExecution = function(message) {return new TAMEError(TAMEError.Type.ModuleExecution, message);};
TAMEError.ModuleState = function(message) {return new TAMEError(TAMEError.Type.ModuleState, message);};
TAMEError.Arithmetic = function(message) {return new TAMEError(TAMEError.Type.Arithmetic, message);};
TAMEError.ArithmeticStackState = function(message) {return new TAMEError(TAMEError.Type.ArithmeticStackState, message);};
TAMEError.RunawayRequest = function(message) {return new TAMEError(TAMEError.Type.RunawayRequest, message);};
TAMEError.UnexpectedValue = function(message) {return new TAMEError(TAMEError.Type.UnexpectedValue, message);};
TAMEError.UnexpectedValueType = function(message) {return new TAMEError(TAMEError.Type.UnexpectedValueType, message);};


/*****************************************************************************
 Interrupt handling.
 *****************************************************************************/
var TAMEInterrupt = function(type, message)
{
	this.type = type;
	this.message = message;
};

TAMEInterrupt.Type = 
{
	"Break": "Break",
	"Continue": "Continue",
	"Error": "Error",
	"End": "End",
	"Quit": "Quit",
	"Finish": "Finish"
};

// Convenience Constructors
TAMEInterrupt.Break = function() { return new TAMEInterrupt(TAMEInterrupt.Type.Break, "A break interrupt was thrown!"); };
TAMEInterrupt.Continue = function() { return new TAMEInterrupt(TAMEInterrupt.Type.Continue, "A continue interrupt was thrown!"); };
TAMEInterrupt.Error = function(message) { return new TAMEInterrupt(TAMEInterrupt.Type.Error, message); };
TAMEInterrupt.End = function() { return new TAMEInterrupt(TAMEInterrupt.Type.End, "An end interrupt was thrown!"); };
TAMEInterrupt.Quit = function() { return new TAMEInterrupt(TAMEInterrupt.Type.Quit, "A quit interrupt was thrown!"); };
TAMEInterrupt.Finish = function() { return new TAMEInterrupt(TAMEInterrupt.Type.Finish, "A finish interrupt was thrown!"); };


TAMEInterrupt.prototype.toString = function()
{
	return "TAMEInterrupt: "+ this.type + ": " + this.message;
};


/*****************************************************************************
 See net.mtrop.tame.lang.Value
 *****************************************************************************/
var TValue = {};

/* Type Constants */
TValue.Type = 
{
	"BOOLEAN": "BOOLEAN",
	"INTEGER": "INTEGER",
	"FLOAT": "FLOAT",
	"STRING": "STRING",
	"OBJECT": "OBJECT",
	"CONTAINER": "CONTAINER",
	"PLAYER": "PLAYER",
	"ROOM": "ROOM",
	"WORLD": "WORLD",
	"ACTION": "ACTION",
	"VARIABLE": "VARIABLE"
};

// Factory.
TValue.create = function(type, value)
{
	if (!type)
		throw TAMEError.UnexpectedValueType("Invalid value type in TValue()");
	if (typeof value === 'undefined' || value == null)
		throw TAMEError.UnexpectedValueType("Value cannot be undefined or null in TValue()");

	var out = {};
	out.type = type;
	out.value = value;
	return out;
};

// Convenience constructors.
TValue.createBoolean = function(value) {return TValue.create(TValue.Type.BOOLEAN, Boolean(value));}
TValue.createInteger = function(value) {return TValue.create(TValue.Type.INTEGER, parseInt(value, 10));}
TValue.createFloat = function(value) {return TValue.create(TValue.Type.FLOAT, parseFloat(value));}
TValue.createString = function(value) {return TValue.create(TValue.Type.STRING, Util.toBase64(String(value)));}
TValue.createWorld = function() {return TValue.create(TValue.Type.WORLD, Util.toBase64("world"));}
TValue.createObject = function(value) {return TValue.create(TValue.Type.OBJECT, Util.toBase64(String(value)));}
TValue.createContainer = function(value) {return TValue.create(TValue.Type.CONTAINER, Util.toBase64(String(value)));}
TValue.createPlayer = function(value) {return TValue.create(TValue.Type.PLAYER, Util.toBase64(String(value)));}
TValue.createRoom = function(value) {return TValue.create(TValue.Type.ROOM, Util.toBase64(String(value)));}
TValue.createAction = function(value) {return TValue.create(TValue.Type.ACTION, Util.toBase64(String(value)));}
TValue.createVariable = function(value) {return TValue.create(TValue.Type.VARIABLE, Util.toBase64(String(value)));}
TValue.createNaN = function() {return TValue.create(TValue.Type.FLOAT, NaN);}
TValue.createInfinity = function() {return TValue.create(TValue.Type.FLOAT, Infinity);}
TValue.createNegativeInfinity = function() {return TValue.create(TValue.Type.FLOAT, -Infinity);}

/**
 * Returns if this value is equal to another, value-wise.
 * If they are literals, they are compared by their string values.
 * @param v1 the first value.
 * @param v2 the second value.
 * @return true if so, false if not.
 */
TValue.areEqualIgnoreType = function(v1, v2)
{
	if (TValue.isLiteral(v1) && TValue.isLiteral(v2))
	{
		if (TValue.isString(v1) && TValue.isString(v2))
			return v1.value == v2.value;
		else
			return TValue.asDouble(v1) == TValue.asDouble(v2);
	}
	else
		return TValue.areEqual(v1, v2); 
};

/**
 * Returns if this value is equal to another: PERFECTLY EQUAL, type strict.
 * @param v1 the first value.
 * @param v2 the second value.
 * @return true if so, false if not.
 */
TValue.areEqual = function(v1, v2)
{
	return v1.type === v2.type && v1.value === v2.value;
};

/**
 * Compares two values.
 * @param v1 the first value.
 * @param v2 the second value.
 * @return -1 if v1 < v2, 0 if equal (ignore type), 1 if v1 > v2.
 */
TValue.compare = function(v1, v2)
{
	// complete equality.
	if (TValue.areEqual(v1, v2))
		return 0;
	
	var d1 = null;
	var d2 = null;

	// one is not a literal
	if (!TValue.isLiteral(v1) || !TValue.isLiteral(v2))
	{
		d1 = TValue.asString(v1);
		d2 = TValue.asString(v2);
	}
	else if (TValue.isString(v1) || TValue.isString(v2))
	{
		d1 = TValue.asString(v1);
		d2 = TValue.asString(v2);
	}
	else if (TValue.isFloatingPoint(v1) || TValue.isFloatingPoint(v2))
	{
		d1 = TValue.asDouble(v1);
		d2 = TValue.asDouble(v2);
	}
	else if (TValue.isInteger(v1) || TValue.isInteger(v2))
	{
		d1 = TValue.asLong(v1);
		d2 = TValue.asLong(v2);
	}
	else if (TValue.isBoolean(v1) || TValue.isBoolean(v2))
	{
		d1 = TValue.asBoolean(v1);
		d2 = TValue.asBoolean(v2);
		// special case
		return d1 === d2 ? 0 : (!d1 ? -1 : 1);
	}
	
	return d1 === d2 ? 0 : (d1 < d2 ? -1 : 1);
	
};

/**
 * Returns the absolute value of a literal value.
 * @param value1 the first operand.
 * @return the resultant value.
 */
TValue.absolute = function(value1)
{
	if (TValue.isInteger(value1))
		return TValue.createInteger(Math.abs(TValue.asLong(value1)));
	else if (TValue.isNumeric(value1))
		return TValue.createFloat(Math.abs(TValue.asDouble(value1)));
	else
		return TValue.createNaN();
};

/**
 * Returns the negative value of a literal value.
 * @param value1 the first operand.
 * @return the resultant value.
 */
TValue.negate = function(value1)
{
	if (TValue.isInteger(value1))
		return TValue.createInteger(-TValue.asLong(value1));
	else if (TValue.isNumeric(value1))
		return TValue.createFloat(-TValue.asDouble(value1));
	else
		return TValue.createNaN();
};

/**
 * Returns the "logical not" value of a literal value.
 * @param value1 the first operand.
 * @return the resultant value as a boolean value.
 */
TValue.logicalNot = function(value1)
{
	if (TValue.isLiteral(value1))
		return TValue.createBoolean(!TValue.asBoolean(value1));
	else
		return TValue.createNaN();
};

/**
 * Returns the bitwise compliment value of a literal value.
 * @param value1 the first operand.
 * @return the resultant value.
 */
TValue.not = function(value1)
{
	if (TValue.isInfinite(value1))
		return TValue.createInteger(-1);
	else if (TValue.isNaN(value1))
		return TValue.createInteger(-1);
	else if (TValue.isBoolean(value1))
		return TValue.createBoolean(!TValue.asBoolean(value1));
	else if (TValue.isNumeric(value1))
		return TValue.createInteger(~TValue.asLong(value1));
	else if (TValue.isString(value1))
		return TValue.createInteger(-1);
	else
		return TValue.createNaN();
};

/**
 * Returns the addition of two literal values.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.add = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be added: " + value1 + ", " + value2);

	if (TValue.isBoolean(value1) && TValue.isBoolean(value2))
	{
		var v1 = TValue.asBoolean(value1);
		var v2 = TValue.asBoolean(value2);
		return TValue.createBoolean(v1 || v2);
	}
	else if (TValue.isString(value1) || TValue.isString(value2))
	{
		var v1 = TValue.asString(value1);
		var v2 = TValue.asString(value2);
		return TValue.createString(v1 + v2);
	}
	else if (TValue.isInteger(value1) && TValue.isInteger(value2))
	{
		var v1 = TValue.asLong(value1);
		var v2 = TValue.asLong(value2);
		return TValue.createInteger(v1 + v2);
	}
	else
	{
		var v1 = TValue.asDouble(value1);
		var v2 = TValue.asDouble(value2);
		return TValue.createFloat(v1 + v2);
	}
};

/**
 * Returns the subtraction of the second literal value from the first.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.subtract = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be subtracted: " + value1 + ", " + value2);

	if (TValue.isBoolean(value1) && TValue.isBoolean(value2))
	{
		var v1 = TValue.asBoolean(value1);
		var v2 = TValue.asBoolean(value2);
		return TValue.createBoolean(v1 && !v2);
	}
	else if (TValue.isInteger(value1) && TValue.isInteger(value2))
	{
		var v1 = TValue.asLong(value1);
		var v2 = TValue.asLong(value2);
		return TValue.createInteger(v1 - v2);
	}
	else
	{
		var v1 = TValue.asDouble(value1);
		var v2 = TValue.asDouble(value2);
		return TValue.createFloat(v1 - v2);
	}
};

/**
 * Returns the multiplication of two literal values.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.multiply = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be multiplied: " + value1 + ", " + value2);

	if (TValue.isBoolean(value1) && TValue.isBoolean(value2))
	{
		var v1 = TValue.asBoolean(value1);
		var v2 = TValue.asBoolean(value2);
		return TValue.createBoolean(v1 && v2);
	}
	else if (TValue.isInteger(value1) && TValue.isInteger(value2))
	{
		var v1 = TValue.asLong(value1);
		var v2 = TValue.asLong(value2);
		return TValue.createInteger(v1 * v2);
	}
	else
	{
		var v1 = TValue.asDouble(value1);
		var v2 = TValue.asDouble(value2);
		return TValue.createFloat(v1 * v2);
	}
};

/**
 * Returns the division of two literal values.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value.
 * @throws Arithmetic an arithmetic exception, if any (or divide by zero).
 */
TValue.divide = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be divided: " + value1 + ", " + value2);

	if (TValue.isInteger(value1) && TValue.isInteger(value2))
	{
		var v1 = TValue.asLong(value1);
		var v2 = TValue.asLong(value2);
		if (v2 == 0)
		{
			if (v1 != 0)
				return v1 < 0 ? TValue.createNegativeInfinity() : TValue.createInfinity();
			else
				return TValue.createNaN();
		}
		else
			return TValue.createInteger(v1 / v2);
	}
	else
	{
		var v1 = TValue.asDouble(value1);
		var v2 = TValue.asDouble(value2);
		if (v2 == 0.0)
		{
			if (!Number.isNaN(v1) && v1 != 0.0)
				return v1 < 0.0 ? TValue.createNegativeInfinity() : TValue.createInfinity();
			else
				return TValue.createNaN();
		}
		else
			return TValue.createFloat(v1 / v2);
	}
};

/**
 * Returns the modulo of one literal value using another.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value.
 * @throws Arithmetic an arithmetic exception, if any (or divide by zero).
 */
TValue.modulo = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be modulo divided: " + value1 + ", " + value2);

	if (TValue.isInteger(value1) && TValue.isInteger(value2))
	{
		var v1 = TValue.asLong(value1);
		var v2 = TValue.asLong(value2);
		if (v2 == 0)
			return TValue.createNaN();
		else
			return TValue.createInteger(v1 % v2);
	}
	else
	{
		var v1 = TValue.asDouble(value1);
		var v2 = TValue.asDouble(value2);
		if (v2 == 0.0)
			return TValue.createNaN();
		else
			return TValue.createFloat(v1 % v2);
	}
};

/**
 * Returns the result of one value raised to a certain power. 
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value.
 * @throws Arithmetic an arithmetic exception, if any (or divide by zero).
 */
TValue.power = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be raised to a power: " + value1 + ", " + value2);

	var v1 = TValue.asDouble(value1);
	var v2 = TValue.asDouble(value2);
	var p = Math.pow(v1, v2);
	return TValue.createFloat(p);
};

/**
 * Returns the "logical and" of two literal values.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.logicalAnd = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be and'ed: " + value1 + ", " + value2);
	
	var v1 = TValue.asBoolean(value1);
	var v2 = TValue.asBoolean(value2);
	return TValue.createBoolean(v1 && v2);
};

/**
 * Returns the "logical or" of two literal values.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.logicalOr = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be and'ed: " + value1 + ", " + value2);
	
	var v1 = TValue.asBoolean(value1);
	var v2 = TValue.asBoolean(value2);
	return TValue.createBoolean(v1 || v2);
};

/**
 * Returns the "logical xor" of two literal values.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.logicalXOr = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be and'ed: " + value1 + ", " + value2);
	
	var v1 = TValue.asBoolean(value1);
	var v2 = TValue.asBoolean(value2);
	return TValue.createBoolean(v1 ^ v2);
};

/**
 * Returns if two values are equal, no type safety if they are literals.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.equals = function(value1, value2)
{
	return TValue.createBoolean(TValue.areEqualIgnoreType(value1, value2));
};

/**
 * Returns if two values are not equal, no type safety if they are literals.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.notEquals = function(value1, value2)
{
	return TValue.createBoolean(!TValue.areEqualIgnoreType(value1, value2));
};

/**
 * Returns if two values are equal, with type strictness.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.strictEquals = function(value1, value2)
{
	return TValue.createBoolean(TValue.areEqual(value1, value2));
};

/**
 * Returns if two values are not equal, with type strictness.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.strictNotEquals = function(value1, value2)
{
	return TValue.createBoolean(!TValue.areEqual(value1, value2));
};

/**
 * Returns if the first literal value is less than the second.
 * If either are strings, they are compared lexicographically.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.less = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be compared: " + value1 + ", " + value2);
	else if (TValue.isStrictlyNaN(value1) || TValue.isStrictlyNaN(value2))
		return TValue.createBoolean(false);
	else 
		return TValue.createBoolean(TValue.compare(value1, value2) < 0);
};

/**
 * Returns if the first literal value is less than or equal to the second.
 * If either are strings, they are compared lexicographically.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.lessOrEqual = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be compared: " + value1 + ", " + value2);
	else if (TValue.isStrictlyNaN(value1) || TValue.isStrictlyNaN(value2))
		return TValue.createBoolean(false);
	else 
		return TValue.createBoolean(TValue.compare(value1, value2) <= 0);
};

/**
 * Returns if the first literal value is greater than the second.
 * If either are strings, they are compared lexicographically.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.greater = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be compared: " + value1 + ", " + value2);
	else if (TValue.isStrictlyNaN(value1) || TValue.isStrictlyNaN(value2))
		return TValue.createBoolean(false);
	else 
		return TValue.createBoolean(TValue.compare(value1, value2) > 0);
};

/**
 * Returns if the first literal value is greater than or equal to the second.
 * If either are strings, they are compared lexicographically.
 * @param value1 the first operand.
 * @param value2 the second operand.
 * @return the resultant value, as a boolean.
 * @throws Arithmetic If an arithmetic exception occurs.
 */
TValue.greaterOrEqual = function(value1, value2)
{
	if (!(TValue.isLiteral(value1) || TValue.isLiteral(value2)))
		throw TAMEError.Arithmetic("These values can't be compared: " + value1 + ", " + value2);
	else if (TValue.isStrictlyNaN(value1) || TValue.isStrictlyNaN(value2))
		return TValue.createBoolean(false);
	else 
		return TValue.createBoolean(TValue.compare(value1, value2) >= 0);
};

/**
 * Returns if this value is a boolean value.
 * @return true if so, false if not.
 */
TValue.isBoolean = function(value)
{
	return value.type === TValue.Type.BOOLEAN;
};

/**
 * Returns if this value is an integer.
 * @return true if so, false if not.
 */
TValue.isInteger = function(value)
{
	return value.type === TValue.Type.INTEGER;
};

/**
 * Returns if this value is a floating-point number.
 * @return true if so, false if not.
 */
TValue.isFloatingPoint = function(value)
{
	return value.type === TValue.Type.FLOAT;
};

/**
 * Returns if this value is a number.
 * @return true if so, false if not.
 */
TValue.isNumeric = function(value)
{
	return value.type === TValue.Type.INTEGER
		|| value.type === TValue.Type.FLOAT;
};

/**
 * Returns if this value is a string value.
 * @return true if so, false if not.
 */
TValue.isString = function(value)
{
	return value.type === TValue.Type.STRING;
};

/**
 * Returns if this value is a literal value.
 * @return true if so, false if not.
 */
TValue.isLiteral = function(value)
{
	return value.type === TValue.Type.BOOLEAN
		|| value.type === TValue.Type.INTEGER
		|| value.type === TValue.Type.FLOAT
		|| value.type === TValue.Type.STRING;
};

/**
 * Returns if this value represents an element.
 * @return true if so, false if not.
 */
TValue.isElement = function(value)
{
	return value.type === TValue.Type.OBJECT
		|| value.type === TValue.Type.PLAYER
		|| value.type === TValue.Type.ROOM
		|| value.type === TValue.Type.CONTAINER
		|| value.type === TValue.Type.WORLD;
};

/**
 * Returns if this value represents an object container.
 * @return true if so, false if not.
 */
TValue.isObjectContainer = function(value)
{
	return value.type === TValue.Type.PLAYER
		|| value.type === TValue.Type.ROOM
		|| value.type === TValue.Type.CONTAINER
		|| value.type === TValue.Type.WORLD;
};

/**
 * Returns if this value represents an object.
 * @return true if so, false if not.
 */
TValue.isObject = function(value)
{
	return value.type === TValue.Type.OBJECT;
};

/**
 * Returns if this value represents a room.
 * @return true if so, false if not.
 */
TValue.isRoom = function(value)
{
	return value.type === TValue.Type.ROOM;
};

/**
 * Returns if this value represents a player.
 * @return true if so, false if not.
 */
TValue.isPlayer = function(value)
{
	return value.type === TValue.Type.PLAYER;
};

/**
 * Returns if this value represents a container.
 * @return true if so, false if not.
 */
TValue.isContainer = function(value)
{
	return value.type === TValue.Type.CONTAINER;
};

/**
 * Returns if this value represents an action.
 * @return true if so, false if not.
 */
TValue.isAction = function(value)
{
	return value.type === TValue.Type.ACTION;
};

/**
 * Returns if this value represents a variable.
 * @return true if so, false if not.
 */
TValue.isVariable = function(value)
{
	return value.type === TValue.Type.VARIABLE;
};

/**
 * Returns if this value represents a boolean.
 * @return true if so, false if not.
 */
TValue.isBoolean = function(value)
{
	return value.type === TValue.Type.BOOLEAN;
};

/**
 * Returns if this value evaluates to <code>NaN</code>.
 * @return true if so, false if not.
 */
TValue.isNaN = function(value)
{
	return Number.isNaN(TValue.asDouble(value));
};

/**
 * Returns if this value is floating point and literally <code>NaN</code>.
 * @return true if so, false if not.
 */
TValue.isStrictlyNaN = function(value)
{
	return TValue.isFloatingPoint(value) && TValue.isNaN(value);
};

/**
 * Returns if this value evaluates to positive or negative infinity.
 * @return true if so, false if not.
 */
TValue.isInfinite = function(value)
{
	var v = TValue.asDouble(value);
	return v === Infinity || v === -Infinity;
};

/**
 * Returns this value as a long value.
 * @return the long value of this value.
 */
TValue.asLong = function(value)
{
	if (TValue.isInfinite(value) || TValue.isNaN(value))
		return 0;
	else if (TValue.isBoolean(value))
		return TValue.asBoolean(value) ? 1 : 0;
	else if (TValue.isInteger(value))
		return value.value;
	else if (TValue.isFloatingPoint(value))
		return parseInt(value.value, 10);
	else if (TValue.isString(value))
		return parseInt(TValue.asString(value).toLowerCase(), 10);
	else
		return 0;
};

/**
 * Returns the double value of this value.
 * @return the double value of this value, or {@link Double#NaN} if not parsable as a number.
 */
TValue.asDouble = function(value)
{
	if (TValue.isBoolean(value))
		return TValue.asBoolean(value) ? 1.0 : 0.0;
	else if (TValue.isInteger(value))
		return parseFloat(value.value);
	else if (TValue.isFloatingPoint(value))
		return value.value;
	else if (TValue.isString(value))
	{
		var vlower = Util.fromBase64(value.value).toLowerCase();
		if (vlower === "nan")
			return NaN;
		else if (vlower === "infinity")
			return Infinity;
		else if (vlower === "-infinity")
			return -Infinity;
		else
			return parseFloat(value.value);
	}
	else
		return NaN;
};

/**
 * Returns the String value of this value (not the same as toString()!!).
 * @return the String value of this value.
 */
TValue.asString = function(value)
{
	if (TValue.isString(value) || TValue.isElement(value) || TValue.isVariable(value) || TValue.isAction(value))
		return Util.fromBase64(value.value);
	else if (TValue.isInfinite(value) || TValue.isNaN(value))
		return ""+value.value;
	else if (TValue.isFloatingPoint(value))
	{
		// make it equal to Java/C#
		var d = TValue.asDouble(value);
		if (Math.abs(d) == 0.0)
			return "0.0";
		else if (Math.abs(d) < 0.001 || Math.abs(d) >= 10000000)
		{
			var out = d.toExponential().toUpperCase().replace('+','');
			if (out.indexOf('.') < 0)
			{
				var ie = out.indexOf('E');
				return out.substring(0, ie) + ".0" + out.substring(ie);
			}
			else
				return out;
		}
		else if (d % 1 == 0)		
			return value.value+".0";
		else
			return ""+value.value;
	}
	else
		return ""+value.value;
};

/**
 * Returns this value as a boolean value.
 * @return true if this evaluates true, false if not.
 */
TValue.asBoolean = function(value)
{
	if (TValue.isBoolean(value))
		return value.value;
	else if (TValue.isFloatingPoint(value))
	{
		if (TValue.isInfinite(value))
			return true;
		else if (Number.isNaN(value.value))
			return false;
		else
			return TValue.asDouble(value) != 0;
	}
	else if (TValue.isInteger(value))
		return TValue.asLong(value) != 0;
	else if (TValue.isString(value))
		return Util.fromBase64(value.value).length !== 0;
	else
		return true; // all objects are true
};

/**
 * Returns if this value evaluates to "true".
 * @return true if so, false if not.
 */
TValue.isTrue = function(value)
{
	return TValue.asBoolean(value);
};
    
/**
 * @return a string representation of this value (for debug, usually).
 */
TValue.toString = function(value)
{
	return value.type + "[" + Util.withEscChars(TValue.asString(value)) + "]";
};


/*****************************************************************************
 See net.mtrop.tame.TAMERequest
 *****************************************************************************/
var TRequest = function(context, inputMessage, tracing)
{
	this.moduleContext = context;
    this.inputMessage = inputMessage;
    this.tracing = tracing;
 
	// Stacks and Queues
    this.actionQueue = [];
    this.valueStack = [];
    this.contextStack = [];
};

/**
 * Gets the request's input message.
 * This gets interpreted by the TAME virtual machine.
 * @return the message used in the request.
 */
TRequest.prototype.getInputMessage = function()
{
	return this.inputMessage;
};

/**
 * Is this a tracing request?
 * @return true if so, false if not.
 */
TRequest.prototype.isTracing = function()
{
	return this.tracing;
};

/**
 * Adds an action item to the queue to be processed later.
 * @param item the action item to add.
 */
TRequest.prototype.addActionItem = function(item)
{
	this.actionQueue.push(item);
};

/**
 * Checks if this still has action items to process.
 * @return true if so, false if not.
 */
TRequest.prototype.hasActionItems = function()
{
	return this.actionQueue.length != 0;
};

/**
 * Dequeues an action item from the queue to be processed later.
 * @return the next action item to process.
 */
TRequest.prototype.nextActionItem = function()
{
	return this.actionQueue.shift();
};

/**
 * Pushes an element context value onto the context stack.
 * @param context the context to push.
 */
TRequest.prototype.pushContext = function(context)
{
	this.contextStack.push(context);
};

/**
 * Removes an element context value off of the context stack and returns it.
 * @return the element context on the stack or null if none in the stack.
 */
TRequest.prototype.popContext = function()
{
	return this.contextStack.pop();
};

/**
 * Looks at the top of the element context stack.
 * @return the top of the context stack, or null if the stack is empty.
 */
TRequest.prototype.peekContext = function()
{
	return this.contextStack[this.contextStack.length - 1];
};

/**
 * Pushes a value onto the arithmetic stack.
 * @param value the value to push.
 */
TRequest.prototype.pushValue = function(value)
{
	this.valueStack.push(value);
};

/**
 * Removes the topmost value off the arithmetic stack.
 * @return the value popped off the stack or null if the stack is empty.
 * @throws ArithmeticStackStateException if the stack is empty.
 */
TRequest.prototype.popValue = function()
{
	if (this.valueStack.length == 0)
		throw TAMEError.ArithmeticStackState("Attempt to pop an empty arithmetic stack.");
	return this.valueStack.pop();
};

/**
 * Checks if the arithmetic stack is empty.
 * Should be called after a full request is made.
 * @throws ArithmeticStackStateException if the stack is NOT empty.
 */
TRequest.prototype.checkStackClear = function()
{
	if (this.valueStack.length != 0)
		throw TAMEError.ArithmeticStackState("Arithmetic stack is not empty.");
};



/*****************************************************************************
 See net.mtrop.tame.TAMEResponse
 *****************************************************************************/
var TResponse = function()
{
    this.responseCues = [];
    this.commandsExecuted = 0;
    this.functionDepth = 0;
    this.requestNanos = 0;
    this.interpretNanos = 0;
};

/**
 * Adds a cue to the response.
 * @param type the cue type name.
 * @param content the cue content.
 */
TResponse.prototype.addCue = function(type, content)
{
	if ((typeof content) === 'undefined' || content == null)
		content = "";
	else
		content = String(content);
	this.responseCues.push({"type": type, "content": content});
};

/**
 * Adds a TRACE cue to the response, if tracing is enabled.
 * @param request (TRequest) the request object.
 * @param content the content to add.
 */
TResponse.prototype.trace = function(request, content)
{
	if (request.tracing)
		this.addCue("TRACE", content);
};

/**
 * Increments and checks if command amount breaches the threshold.
 * @throw TAMEError if a breach is detected.
 */
TResponse.prototype.incrementAndCheckCommandsExecuted = function(maxCommands)
{
	this.commandsExecuted++;
	if (this.commandsExecuted >= maxCommands)
		throw TAMEError.RunawayRequest("Too many commands executed - possible infinite loop.");
};

/**
 * Increments and checks if function depth breaches the threshold.
 * @throw TAMEError if a breach is detected.
 */
TResponse.prototype.incrementAndCheckFunctionDepth = function(maxDepth)
{
	this.functionDepth++;
	if (this.functionDepth >= maxDepth)
		throw TAMEError.RunawayRequest("Too many function calls deep - possible stack overflow.");
};

/**
 * Decrements the function depth.
 */
TResponse.prototype.decrementFunctionDepth = function()
{
	this.functionDepth--;
};


/****************************************************
 See net.mtrop.tame.TAMEAction
 ****************************************************/
var TAction = function(action, target, object1, object2)
{
	this.action = action; 
	this.target = target; 
	this.object1 = object1; 
	this.object2 = object2;
};

// Convenience constructors.

TAction.create = function(action) { return new TAction(action); };
TAction.createModal = function(action, target) { return new TAction(action, target); };
TAction.createObject = function(action, object1) { return new TAction(action, null, object1); };
TAction.createObject2 = function(action, object1, object2) { return new TAction(action, null, object1, object2); };

TAction.prototype.toString = function()
{
	var out = "ActionItem ";
	
	out += "[";
	if (this.action)
		out += this.action.identity;

	if (this.target)
		out += ", " + this.target;

	if (this.object1)
		out += ", " + this.object1.identity;

	if (this.object2)
	{
		if (this.object1.identity)
			out += ", ";
		out += this.object2.identity;
	}
	
	out += "]";
	
	return out;
};



/****************************************************
 Constructor for the TAME Module.
 ****************************************************/

function TModule(theader, tactions, telements)
{	
	// Fields --------------------
	this.header = theader;
	this.actions = Util.mapify(tactions, "identity");
	this.elements = {};
	this.actionNameTable = {};
	
	var elem = this.elements;
	var act = this.actions;
	var antbl = this.actionNameTable;

	var typeHash = {
		"TAction": true, 
		"TObject": true, 
		"TRoom": true, 
		"TPlayer": true, 
		"TContainer": true, 
		"TWorld": true
	};
	
	Util.each(Util.mapify(telements, "identity"), function(element, identity) {
		identity = identity.toLowerCase(); 
		if (!typeHash[element.tameType])
			throw TAMEError.Module("Unknown element type: "+element.tameType);
		if (elem[identity] || act[identity])
			throw TAMEError.Module("Another element already has the identity "+identity);
		elem[identity] = element;
	});

	Util.each(this.actions, function(action) {
		if (!typeHash[action.tameType])
			throw TAMEError.Module("Unknown element type: "+action.tameType);
		Util.each(action.names, function(name) {
			antbl[Util.replaceAll(name.toLowerCase(), "\\s+", " ")] = action.identity;
		});
	});

	if (!this.elements['world'])
		throw TAMEError.Module('No world element!');
	
};

TModule.prototype.getActionByName = function(name)
{
	var identity = this.actionNameTable[name.toLowerCase()];
	if (!identity)
		return null;
	return this.actions[identity];
}


/****************************************************
 See net.mtrop.tame.TAMEModuleContext
 ****************************************************/
var TModuleContext = function(module)
{
	this.module = module,			// module reference
	this.state = {};				// savable state.
	
	this.state.player = null;		// current player
	this.state.elements = {}; 		// element-to-contexts
	this.state.owners = {}; 		// element-to-objects
	this.state.objectOwners = {};	// object-to-element
	this.state.roomStacks = {};		// player-to-rooms
	this.state.names = {};			// object-to-names
	this.state.tags = {};			// object-to-tags
	
	var s = this.state;
	var m = this.module;
	
	var mc = this;
	
	// create element contexts.
	Util.each(m.elements, function(element, identity)
	{
		identity = identity.toLowerCase();
		
		if (element.archetype)
			return;
		if (s.elements[identity])
			throw TAMEError.Module("Another element already has the identity "+identity);
		s.elements[identity] = {
			"identity": identity,
			"variables": {}
		};
		
		// just for objects
		if (element.tameType === 'TObject')
		{
			s.names[identity] = {};
			s.tags[identity] = {};
			Util.each(element.names, function(name)
			{
				mc.addObjectName(element.identity, name);
			});
			Util.each(element.tags, function(tag)
			{
				mc.addObjectTag(element.identity, tag);
			});		
		}
		
	});
	
	var cr = parseInt(module.header['tame_runaway_max'], 10);
	var fd = parseInt(module.header['tame_funcdepth_max'], 10);
	
	this.commandRunawayMax = cr <= 0 || isNaN(cr) ? TAMEConstants.DEFAULT_RUNAWAY_THRESHOLD : cr;
	this.functionDepthMax = fd <= 0 || isNaN(fd) ? TAMEConstants.DEFAULT_FUNCTION_DEPTH : fd;
	
};

/**
 * Sets the current player.
 * @param playerIdentity the player identity, or null.
 * @throws TAMEError if no such player.
 */
TModuleContext.prototype.setCurrentPlayer = function(playerIdentity) 
{
	var contextState = this.state;

	if (!contextState)
		throw TAMEError.ModuleState("Context is invalid or null");
	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	
	playerIdentity = playerIdentity.toLowerCase();
	
	if (!contextState.elements[playerIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+playerIdentity);
	
	contextState.player = playerIdentity;
};

/**
 * Removes a player from all rooms.
 * @param playerIdentity the player identity.
 * @throws TAMEError if no such player.
 */
TModuleContext.prototype.removePlayer = function(playerIdentity) 
{
	var contextState = this.state;

	if (!contextState)
		throw TAMEError.ModuleState("Context is invalid or null");
	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	
	playerIdentity = playerIdentity.toLowerCase();
	
	if (!contextState.elements[playerIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+playerIdentity);
	delete contextState.roomStacks[playerIdentity];
};

/**
 * Pushes a room onto a player room stack.
 * @param playerIdentity the player identity.
 * @param roomIdentity the room identity.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.pushRoomOntoPlayer = function(playerIdentity, roomIdentity) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.roomStacks)
		throw TAMEError.ModuleState("Context state is missing required member: roomStacks");

	playerIdentity = playerIdentity.toLowerCase();
	roomIdentity = roomIdentity.toLowerCase();

	if (!contextState.elements[playerIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+playerIdentity);
	if (!contextState.elements[roomIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+roomIdentity);
	
	if (!contextState.roomStacks[playerIdentity])
		contextState.roomStacks[playerIdentity] = [];
	contextState.roomStacks[playerIdentity].push(roomIdentity);
};
	
/**
 * Pops a room off of a player room stack.
 * @param playerIdentity the player identity.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.popRoomFromPlayer = function(playerIdentity)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.roomStacks)
		throw TAMEError.ModuleState("Context state is missing required member: roomStacks");

	playerIdentity = playerIdentity.toLowerCase();

	if (!contextState.elements[playerIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+playerIdentity);
	if (!contextState.roomStacks[playerIdentity])
		return;

	contextState.roomStacks[playerIdentity].pop();
	if (!contextState.roomStacks[playerIdentity].length)
		delete contextState.roomStacks[playerIdentity];
};

/**
 * Checks if a player is in a room (or if the room is in the player's room stack).
 * @param playerIdentity the player identity.
 * @param roomIdentity the room identity.
 * @return true if the room is in the player's stack, false if not, or the player is in no room.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.checkPlayerIsInRoom = function(playerIdentity, roomIdentity) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.roomStacks)
		throw TAMEError.ModuleState("Context state is missing required member: roomStacks");

	playerIdentity = playerIdentity.toLowerCase();
	roomIdentity = roomIdentity.toLowerCase();

	if (!contextState.elements[playerIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+playerIdentity);
	if (!contextState.elements[roomIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+roomIdentity);
	
	var roomstack = contextState.roomStacks[playerIdentity];
	if (!roomstack)
		return false;
	else
		roomstack.indexOf(roomIdentity) >= 0;
};

/**
 * Removes an object from its owner.
 * @param objectIdentity the object identity.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.removeObject = function(objectIdentity) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.objectOwners)
		throw TAMEError.ModuleState("Context state is missing required member: objectOwners");
	if(!contextState.owners)
		throw TAMEError.ModuleState("Context state is missing required member: owners");

	objectIdentity = objectIdentity.toLowerCase();
	
	if (!contextState.elements[objectIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+objectIdentity);
	
	var elementIdentity = contextState.objectOwners[objectIdentity];
	if (!elementIdentity)
		return;
	
	delete contextState.objectOwners[objectIdentity];
	Util.arrayRemove(contextState.owners[elementIdentity], objectIdentity);
};

/**
 * Adds an object to an element.
 * @param elementIdentity the element identity.
 * @param objectIdentity the object identity.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.addObjectToElement = function(elementIdentity, objectIdentity) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.objectOwners)
		throw TAMEError.ModuleState("Context state is missing required member: objectOwners");
	if(!contextState.owners)
		throw TAMEError.ModuleState("Context state is missing required member: owners");

	elementIdentity = elementIdentity.toLowerCase();
	objectIdentity = objectIdentity.toLowerCase();
	
	if (!contextState.elements[elementIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+elementIdentity);
	if (!contextState.elements[objectIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+objectIdentity);
	
	this.removeObject(objectIdentity);
	contextState.objectOwners[objectIdentity] = elementIdentity;
	
	if (!contextState.owners[elementIdentity])
		contextState.owners[elementIdentity] = [];
	contextState.owners[elementIdentity].push(objectIdentity);
};

/**
 * Checks if an object is owned by an element.
 * @param elementIdentity the element identity.
 * @param objectIdentity the object identity.
 * @return true if the element is the owner of the object, false if not.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.checkElementHasObject = function(elementIdentity, objectIdentity) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.objectOwners)
		throw TAMEError.ModuleState("Context state is missing required member: objectOwners");

	elementIdentity = elementIdentity.toLowerCase();
	objectIdentity = objectIdentity.toLowerCase();

	if (!contextState.elements[elementIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+elementIdentity);
	if (!contextState.elements[objectIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+objectIdentity);
	
	return contextState.objectOwners[objectIdentity] == elementIdentity;
};


/**
 * Checks if an object has no owner.
 * @param objectIdentity the object identity.
 * @return true if the object is owned by nobody, false if it has an owner.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.checkObjectHasNoOwner = function(objectIdentity) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.objectOwners)
		throw TAMEError.ModuleState("Context state is missing required member: objectOwners");

	objectIdentity = objectIdentity.toLowerCase();
	
	if (!contextState.elements[objectIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+objectIdentity);
	
	return !contextState.objectOwners[objectIdentity];
};

/**
 * Gets a list of objects owned by this element.
 * The list is a copy, and can be modified without ruining the original.
 * @param elementIdentity the element identity.
 * @return an array of object identities contained by this element.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.getObjectsOwnedByElement = function(elementIdentity)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.owners)
		throw TAMEError.ModuleState("Context state is missing required member: owners");

	elementIdentity = elementIdentity.toLowerCase();
	
	if (!contextState.elements[elementIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+elementIdentity);
	
	var arr = contextState.owners[elementIdentity];
	if (!arr)
		return [];
	else
		return arr.slice(); // return copy of full array.
};

/**
 * Gets a count of objects owned by this element.
 * The list is a copy, and can be modified without ruining the original.
 * @param elementIdentity the element identity.
 * @return an array of object identities contained by this element.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.getObjectsOwnedByElementCount = function(elementIdentity)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.owners)
		throw TAMEError.ModuleState("Context state is missing required member: owners");

	elementIdentity = elementIdentity.toLowerCase();

	if (!contextState.elements[elementIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+elementIdentity);
	
	var arr = contextState.owners[elementIdentity];
	if (!arr)
		return 0;
	else
		return arr.length;
};

/**
 * Adds a interpretable name to an object.
 * The name is converted to lowercase and all contiguous whitespace is replaced with single spaces.
 * Does nothing if the object already has the name.
 * More than one object with this name can result in "ambiguous" actions!
 * @param objectIdentity the object identity.
 * @param name the name to add.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.addObjectName = function(objectIdentity, name)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.names)
		throw TAMEError.ModuleState("Context state is missing required member: names");

	objectIdentity = objectIdentity.toLowerCase();
	
	if (!contextState.elements[objectIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+objectIdentity);
	
	var object = this.getElement(objectIdentity);
	
	name = Util.replaceAll(name.trim().toLowerCase(), "\\s+", " ");
	Util.objectStringAdd(contextState.names, objectIdentity, name);
	Util.each(object.determiners, function(determiner)
	{
		determiner = Util.replaceAll(determiner.trim().toLowerCase(), "\\s+", " ");
		Util.objectStringAdd(contextState.names, objectIdentity, determiner + ' ' + name);
	});
};

/**
 * Removes an interpretable name from an object.
 * The name is converted to lowercase and all contiguous whitespace is replaced with single spaces.
 * Does nothing if the object does not have the name.
 * @param objectIdentity the object identity.
 * @param name the name to remove.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.removeObjectName = function(objectIdentity, name)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.names)
		throw TAMEError.ModuleState("Context state is missing required member: names");

	objectIdentity = objectIdentity.toLowerCase();

	if (!contextState.elements[objectIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+objectIdentity);

	var object = this.getElement(objectIdentity);
	
	name = Util.replaceAll(name.trim().toLowerCase(), "\\s+", " ");
	Util.objectStringRemove(contextState.names, objectIdentity, name);
	Util.each(object.determiners, function(determiner)
	{
		determiner = Util.replaceAll(determiner.trim().toLowerCase(), "\\s+", " ");
		Util.objectStringRemove(contextState.names, objectIdentity, determiner + ' ' + name);
	});
};

/**
 * Checks for an interpretable name on an object.
 * @param objectIdentity the object identity.
 * @param name the name to remove.
 * @return true if it exists, false if not.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.checkObjectHasName = function(objectIdentity, name) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.names)
		throw TAMEError.ModuleState("Context state is missing required member: names");
	
	objectIdentity = objectIdentity.toLowerCase();

	if (!contextState.elements[objectIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+objectIdentity);

	name = name.toLowerCase();
	return Util.objectStringContains(contextState.names, objectIdentity, name);
};

/**
 * Adds a tag to an object. Tags are case-insensitive.
 * Unlike names, tags undergo no whitespace conversion.
 * Does nothing if the object already has the tag.
 * @param objectIdentity the object identity.
 * @param name the name to add.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.addObjectTag = function(objectIdentity, tag)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.tags)
		throw TAMEError.ModuleState("Context state is missing required member: tags");
	
	objectIdentity = objectIdentity.toLowerCase();

	if (!contextState.elements[objectIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+objectIdentity);
	
	tag = tag.toLowerCase();
	Util.objectStringAdd(contextState.tags, objectIdentity, tag);
};

/**
 * Removes a tag from an object. Tags are case-insensitive.
 * Unlike names, tags undergo no whitespace conversion.
 * Does nothing if the object does not have the tag.
 * @param objectIdentity the object identity.
 * @param name the name to remove.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.removeObjectTag = function(objectIdentity, tag)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.tags)
		throw TAMEError.ModuleState("Context state is missing required member: tags");
	
	objectIdentity = objectIdentity.toLowerCase();

	if (!contextState.elements[objectIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+objectIdentity);

	tag = tag.toLowerCase();
	Util.objectStringRemove(contextState.tags, objectIdentity, tag);
};

/**
 * Checks for a tag on an object. Tags are case-insensitive.
 * Unlike names, tags undergo no whitespace conversion.
 * @param objectIdentity the object identity.
 * @param name the name to remove.
 * @return true if it exists, false if not.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.checkObjectHasTag = function(objectIdentity, tag) 
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.tags)
		throw TAMEError.ModuleState("Context state is missing required member: tags");
	
	objectIdentity = objectIdentity.toLowerCase();

	if (!contextState.elements[objectIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+objectIdentity);

	tag = tag.toLowerCase();
	return Util.objectStringContains(contextState.tags, objectIdentity, tag);
};

/**
 * Gets an element by its identity.
 * @return the element or null.
 */
TModuleContext.prototype.getElement = function(elementIdentity)
{
	return this.module.elements[elementIdentity.toLowerCase()];
};

/**
 * Gets an element context by its identity.
 * @return the element context or null.
 * @throws TAMEError if no such stored element context.
 */
TModuleContext.prototype.getElementContext = function(elementIdentity)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	
	return contextState.elements[elementIdentity.toLowerCase()];
};

/**
 * Gets the current player.
 * @return player element, or null/undefined if no current player.
 * @throws TAMEError if no such stored element context.
 */
TModuleContext.prototype.getCurrentPlayer = function()
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");

	if (!contextState.player)
		return null;
	
	return this.getElement(contextState.player);
};

/**
 * Gets the current player context.
 * @return player context, or null/undefined if no current player.
 * @throws TAMEError if no such stored element context.
 */
TModuleContext.prototype.getCurrentPlayerContext = function()
{
	var player = this.getCurrentPlayer();
	if (!player)
		return null;
	
	return this.getElementContext(player.identity);
};

/**
 * Gets the current room. Influenced by current player.
 * @return room element, or null if no current room (or no current player).
 * @throws TAMEError if no such stored element context.
 */
TModuleContext.prototype.getCurrentRoom = function(playerIdentity)
{
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");
	if(!contextState.roomStacks)
		throw TAMEError.ModuleState("Context state is missing required member: roomStacks");

	if (!playerIdentity)
		playerIdentity = contextState.player;
	else
		playerIdentity = playerIdentity.toLowerCase(); 
	
	if (!playerIdentity)
		return null;

	if (!contextState.elements[playerIdentity])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+playerIdentity);

	var stack = contextState.roomStacks[playerIdentity];

	if (!stack)
		return null;

	return this.getElement(stack[stack.length - 1]);
};

/**
 * Gets the current room context.
 * @return room context, or null/undefined if no current player.
 * @throws TAMEError if no such stored element context.
 */
TModuleContext.prototype.getCurrentRoomContext = function(playerIdentity)
{
	var room = this.getCurrentRoom(playerIdentity);
	if (!room)
		return null;
	
	return this.getElementContext(room.identity);
};

/**
 * Resolves an action by its action identity.
 * @param actionIdentity the action identity.
 * @return the corresponding action or null if no current room or player.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.resolveAction = function(actionIdentity)
{	
	var out = this.module.actions[actionIdentity];
	if (!out)
		throw TAMEError.ModuleExecution("Action is missing from module: "+actionIdentity);		
	return this.module.actions[actionIdentity];
};

/**
 * Resolves an element by its identity.
 * The identities "player", "room", and "world" are special.
 * @param elementIdentity the element identity.
 * @return the corresponding action or null if no current room or player.
 * @throws TAMEError if no such element context.
 * @throws TAMEInterrupt if identity refers to a current object that is not set.
 */
TModuleContext.prototype.resolveElement = function(elementIdentity)
{	
	var element = null;
	elementIdentity = elementIdentity.toLowerCase();
	
	// current player
	if (elementIdentity === 'player')
	{
		element = this.getCurrentPlayer();
		if (!element)
			throw TAMEInterrupt.Error("Current player context called with no current player!");
		return element;
	}
	// current room
	else if (elementIdentity === 'room')
	{
		var player = this.getCurrentPlayer();
		if (!player)
			throw TAMEInterrupt.Error("Current room context called with no current player!");
		
		element = this.getCurrentRoom();
		if (!element)
			throw TAMEInterrupt.Error("Current room context called with no current room!");
		return element;
	}
	else
	{
		element = this.getElement(elementIdentity);
		if (!element)
			throw TAMEError.ModuleExecution("Expected element '"+elementIdentity+"' in module!");
		return element;
	}
};

/**
 * Resolves an element context by its identity.
 * The identities "player", "room", and "world" are special.
 * @param elementIdentity the element identity.
 * @return the corresponding action or null if no current room or player.
 * @throws TAMEError if no such element context.
 * @throws TAMEInterrupt if identity refers to a current object that is not set.
 */
TModuleContext.prototype.resolveElementContext = function(elementIdentity)
{	
	var contextState = this.state;

	if(!contextState.elements)
		throw TAMEError.ModuleState("Context state is missing required member: elements");

	var element = this.resolveElement(elementIdentity);

	var ident = element.identity.toLowerCase();
	
	if (!contextState.elements[ident])
		throw TAMEError.ModuleExecution("Element is missing from context state: "+element.identity);
	
	return contextState.elements[ident];
};

/**
 * Resolves a qualifying code block starting from an element.
 * The identities "player", "room", and "world" are special.
 * @param elementIdentity the starting element identity.
 * @param blockType the block entry type.
 * @param blockValues the values for matching the block.
 * @return the first qualifying block in the lineage, or null if no matching block.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.resolveBlock = function(elementIdentity, blockType, blockValues)
{
	var blockname =	blockType + "(";
	if (blockValues) for (var i = 0; i < blockValues.length; i++)
	{
		blockname += TValue.toString(blockValues[i]);
		if (i < blockValues.length - 1)
			blockname += ",";
	}
	blockname += ")";

	var element = this.resolveElement(elementIdentity); 
	
	while (element)
	{
		var out = element.blockTable[blockname];
		if (out)
			return out;
		if (element.parent)
			element = this.resolveElement(element.parent);
		else
			element = null;
	}

	return null;
};

/**
 * Resolves a qualifying function by name starting from an element.
 * The identities "player", "room", and "world" are special.
 * @param elementIdentity the starting element identity.
 * @param functionName the name of the function.
 * @return the first qualifying function in the lineage, or null if no matching entry.
 * @throws TAMEError if no such element context.
 */
TModuleContext.prototype.resolveFunction = function(elementIdentity, functionName)
{
	var element = this.resolveElement(elementIdentity); 
	functionName = functionName.toLowerCase();
	
	while (element)
	{
		var out = element.functionTable[functionName];
		if (out)
			return out;
		if (element.parent)
			element = this.resolveElement(element.parent);
		else
			element = null;
	}

	return null;	
};

/**
 * Returns all objects in the accessible area by an object name read from the interpreter.
 * The output stops if the size of the output array is reached.
 * @param name the name from the interpreter.
 * @param outputArray the output vector of found objects.
 * @param arrayOffset the starting offset into the array to put them.
 * @return the amount of objects found.
 */
TModuleContext.prototype.getAccessibleObjectsByName = function(name, outputArray, arrayOffset)
{
	var playerContext = this.getCurrentPlayerContext();
	var roomContext = this.getCurrentRoomContext();
	var worldContext = this.getElementContext("world");

	var start = arrayOffset;
	var arr = null;
	
	if (playerContext != null)
	{
		arr = this.getObjectsOwnedByElement(playerContext.identity);
		for (var x in arr) if (arr.hasOwnProperty(x))
		{
			var objectIdentity = arr[x];
			if (this.checkObjectHasName(objectIdentity, name))
			{
				outputArray[arrayOffset++] = this.getElement(objectIdentity);
				if (arrayOffset == outputArray.length)
					return arrayOffset - start;
			}
		}
	}
	if (roomContext != null) 
	{
		arr = this.getObjectsOwnedByElement(roomContext.identity);
		for (var x in arr) if (arr.hasOwnProperty(x))
		{
			var objectIdentity = arr[x];
			if (this.checkObjectHasName(objectIdentity, name))
			{
				outputArray[arrayOffset++] = this.getElement(objectIdentity);
				if (arrayOffset == outputArray.length)
					return arrayOffset - start;
			}
		}
	}
	
	arr = this.getObjectsOwnedByElement(worldContext.identity);
	for (var x in arr) if (arr.hasOwnProperty(x))
	{
		var objectIdentity = arr[x];
		if (this.checkObjectHasName(objectIdentity, name))
		{
			outputArray[arrayOffset++] = this.getElement(objectIdentity);
			if (arrayOffset == outputArray.length)
				return arrayOffset - start;
		}
	}
	
	return arrayOffset - start;
};



var TLogic = {};


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
	
 	/* LOGICAL AND */
	{
		"name": 'LOGICAL_AND',
		"symbol": '&',
		"binary": true,
		"doOperation": TValue.logicalAnd
	},
	
 	/* LOGICAL OR */
	{
		"name": 'LOGICAL_OR',
		"symbol": '|',
		"binary": true,
		"doOperation": TValue.logicalOr
	},
	
 	/* LOGICAL XOR */
	{
		"name": 'LOGICAL_XOR',
		"symbol": '^',
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
	"ADD": 3,
	"SUBTRACT": 4,
	"MULTIPLY": 5,
	"DIVIDE": 6,
	"MODULO": 7,
	"POWER": 8,
	"LOGICAL_AND": 9,
	"LOGICAL_OR": 10,
	"LOGICAL_XOR": 11,
	"EQUALS": 12,
	"NOT_EQUALS": 13,
	"STRICT_EQUALS": 14,
	"STRICT_NOT_EQUALS": 15,
	"LESS": 16,
	"LESS_OR_EQUAL": 17,
	"GREATER": 18,
	"GREATER_OR_EQUAL": 19
};

TArithmeticFunctions.COUNT = TArithmeticFunctions.Type.length; 


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
			// Do nothing.
		}
	},

	/* POP */
	{
		"name": 'POP', 
		"doCommand": function(request, response, blockLocal, command)
		{
			request.popValue();
		}
	},

	/* POPVALUE */
	{
		"name": 'POPVALUE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varvalue = command.operand0;
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in POPVALUE call.");
			if (!TValue.isVariable(varvalue))
				throw TAMEError.UnexpectedValueType("Expected variable type in POPVALUE call.");

			var variableName = TValue.asString(varvalue);
			
			if (blockLocal[variableName.toLowerCase()])
				TLogic.setValue(blockLocal, variableName, value);
			else
				TLogic.setValue(request.peekContext().variables, variableName, value);
		}
	},

	/* POPLOCALVALUE */
	{
		"name": 'POPLOCALVALUE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varvalue = command.operand0;
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in POPLOCALVALUE call.");
			if (!TValue.isVariable(varvalue))
				throw TAMEError.UnexpectedValueType("Expected variable type in POPLOCALVALUE call.");

			TLogic.setValue(blockLocal, TValue.asString(varvalue), value);
		}
	},

	/* POPELEMENTVALUE */
	{
		"name": 'POPELEMENTVALUE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varObject = command.operand0;
			var variable = command.operand1;
			var value = request.popValue();

			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in POPELEMENTVALUE call.");
			if (!TValue.isVariable(variable))
				throw TAMEError.UnexpectedValueType("Expected variable type in POPELEMENTVALUE call.");
			if (!TValue.isElement(varObject))
				throw TAMEError.UnexpectedValueType("Expected element type in POPELEMENTVALUE call.");

			var objectName = TValue.asString(varObject);

			TLogic.setValue(request.moduleContext.resolveElementContext(objectName).variables, TValue.asString(variable), value);
		}
	},

	/* PUSHVALUE */
	{
		"name": 'PUSHVALUE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = command.operand0;
			
			if (TValue.isVariable(value))
			{
				var variableName = TValue.asString(value).toLowerCase();
				if (blockLocal[variableName])
					request.pushValue(TLogic.getValue(blockLocal, variableName));
				else
					request.pushValue(TLogic.getValue(request.peekContext().variables, variableName));
			}
			else
			{
				request.pushValue(value);
			}
		}
	},

	/* PUSHELEMENTVALUE */
	{
		"name": 'PUSHELEMENTVALUE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varElement = command.operand0;
			var variable = command.operand1;

			if (!TValue.isVariable(variable))
				throw TAMEError.UnexpectedValueType("Expected variable type in PUSHELEMENTVALUE call.");
			if (!TValue.isElement(varElement))
				throw TAMEError.UnexpectedValueType("Expected element type in PUSHELEMENTVALUE call.");

			var elementName = TValue.asString(varElement);

			request.pushValue(TLogic.getValue(request.moduleContext.resolveElementContext(elementName).variables, TValue.asString(variable)));
		}
	},

	/* CLEARVALUE */
	{
		"name": 'CLEARVALUE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = command.operand0;

			if (!TValue.isVariable(value))
				throw TAMEError.UnexpectedValueType("Expected variable type in CLEARVALUE call.");
			
			var variableName = TValue.asString(value).toLowerCase();
			if (blockLocal[variableName])
				TLogic.clearValue(blockLocal, variableName);
			else
				TLogic.clearValue(request.peekContext().variables, variableName);
		}
	},

	/* CLEARELEMENTVALUE */
	{
		"name": 'CLEARELEMENTVALUE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varElement = command.operand0;
			var variable = command.operand1;

			if (!TValue.isVariable(variable))
				throw TAMEError.UnexpectedValueType("Expected variable type in CLEARELEMENTVALUE call.");
			if (!TValue.isElement(varElement))
				throw TAMEError.UnexpectedValueType("Expected element type in CLEARELEMENTVALUE call.");

			var variableName = TValue.asString(variable).toLowerCase();
			if (blockLocal[variableName])
				TLogic.clearValue(blockLocal, TValue.asString(variable));
			else
			{
				var element = request.moduleContext.resolveElementContext(TValue.asString(varElement));
				TLogic.clearValue(element.variables, TValue.asString(variable))
			}
		}
	},

	/* PUSHTHIS */
	{
		"name": 'PUSHTHIS', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var element = request.moduleContext.resolveElement(request.peekContext().identity);
			if (element.tameType === 'TObject')
				request.pushValue(TValue.createObject(element.identity));
			else if (element.tameType === 'TRoom')
				request.pushValue(TValue.createRoom(element.identity));
			else if (element.tameType === 'TPlayer')
				request.pushValue(TValue.createPlayer(element.identity));
			else if (element.tameType === 'TContainer')
				request.pushValue(TValue.createContainer(element.identity));
			else if (element.tameType === 'TWorld')
				request.pushValue(TValue.createWorld());
			else
				throw TAMEError.ModuleExecution("Internal error - invalid object type for PUSHTHIS.");
		}
	},
	
	/* ARITHMETICFUNC */
	{
		"name": 'ARITHMETICFUNC', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var functionValue = command.operand0;

			if (!TValue.isInteger(functionValue))
				throw TAMEError.UnexpectedValueType("Expected integer type in ARITHMETICFUNC call.");

			TLogic.doArithmeticStackFunction(request, response, TValue.asLong(functionValue));
		}
	},

	/* IF */
	{
		"name": 'IF', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var result = TLogic.callConditional('IF', request, response, blockLocal, command);
			
			if (result)
			{
				response.trace(request, "Calling IF success block...");
				var success = command.successBlock;
				if (!success)
					throw TAMEError.ModuleExecution("Success block for IF does NOT EXIST!");
				TLogic.executeBlock(success, request, response, blockLocal);
			}
			else
			{
				var failure = command.failureBlock;
				if (failure)
				{
					response.trace(request, "Calling IF failure block...");
					TLogic.executeBlock(failure, request, response, blockLocal);
				}
				else
				{
					response.trace(request, "No failure block.");
				}
			}
		}
	},

	/* WHILE */
	{
		"name": 'WHILE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			while (TLogic.callConditional('WHILE', request, response, blockLocal, command))
			{
				try {
					response.trace(request, "Calling WHILE success block...");
					var success = command.successBlock;
					if (!success)
						throw TAMEError.ModuleExecution("Success block for WHILE does NOT EXIST!");
					TLogic.executeBlock(success, request, response, blockLocal);
				} catch (err) {
					if (err instanceof TAMEInterrupt)
					{
						if (err.type === TAMEInterrupt.Type.Break)
							break;
						else if (err.type === TAMEInterrupt.Type.Continue)
							continue;
						else
							throw err;
					}
					else
						throw err;
				}
			}
		}
	},

	/* FOR */
	{
		"name": 'FOR', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var init = command.initBlock;
			if (!init)
				throw TAMEError.ModuleExecution("Init block for FOR does NOT EXIST!");
			var success = command.successBlock;
			if (!success)
				throw TAMEError.ModuleExecution("Success block for FOR does NOT EXIST!");
			var step = command.stepBlock;
			if (!step)
				throw TAMEError.ModuleExecution("Step block for FOR does NOT EXIST!");

			response.trace(request, "Calling FOR init block...");

			for (
				TLogic.executeBlock(init, request, response, blockLocal);
				TLogic.callConditional('FOR', request, response, blockLocal, command);
				response.trace(request, "Calling FOR stepping block..."),
				TLogic.executeBlock(step, request, response, blockLocal)
			)
			{
				try {
					response.trace(request, "Calling FOR success block...");
					TLogic.executeBlock(success, request, response, blockLocal);
				} catch (err) {
					if (err instanceof TAMEInterrupt)
					{
						if (err.type === TAMEInterrupt.Type.Break)
							break;
						else if (err.type === TAMEInterrupt.Type.Continue)
							continue;
						else
							throw err;
					}
					else
						throw err;
				}
			}
		}
	},

	/* BREAK */
	{
		"name": 'BREAK', 
		"doCommand": function(request, response, blockLocal, command)
		{
			response.trace(request, "Throwing break interrupt...");
			throw TAMEInterrupt.Break();
		}
	},

	/* CONTINUE */
	{
		"name": 'CONTINUE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			response.trace(request, "Throwing continue interrupt...");
			throw TAMEInterrupt.Continue();
		}
	},

	/* QUIT */
	{
		"name": 'QUIT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			response.trace(request, "Throwing quit interrupt...");
			response.addCue(TAMEConstants.Cue.QUIT);
			throw TAMEInterrupt.Quit();
		}
	},

	/* FINISH */
	{
		"name": 'FINISH', 
		"doCommand": function(request, response, blockLocal, command)
		{
			response.trace(request, "Throwing finish interrupt...");
			throw TAMEInterrupt.Finish();
		}
	},

	/* END */
	{
		"name": 'END', 
		"doCommand": function(request, response, blockLocal, command)
		{
			response.trace(request, "Throwing end interrupt...");
			throw TAMEInterrupt.End();
		}
	},

	/* FUNCTIONRETURN */
	{
		"name": 'FUNCTIONRETURN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var retVal = request.popValue();
			response.trace(request, "Returning "+TValue.toString(retVal));
			TLogic.setValue(blockLocal, TAMEConstants.RETURN_VARIABLE, retVal);
			response.trace(request, "Throwing end interrupt...");
			throw TAMEInterrupt.End();
		}
	},

	/* CALLFUNCTION */
	{
		"name": 'CALLFUNCTION', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varFunctionName = command.operand0;

			if (!TValue.isLiteral(varFunctionName))
				throw TAMEError.UnexpectedValueType("Expected literal type in CALLFUNCTION call.");

			request.pushValue(TLogic.callElementFunction(request, response, TValue.asString(varFunctionName), request.peekContext()));
		}
	},

	/* CALLELEMENTFUNCTION */
	{
		"name": 'CALLELEMENTFUNCTION', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varElement = command.operand0;
			var varFunctionName = command.operand1;

			if (!TValue.isElement(varElement))
				throw TAMEError.UnexpectedValueType("Expected element type in CALLELEMENTFUNCTION call.");
			if (!TValue.isLiteral(varFunctionName))
				throw TAMEError.UnexpectedValueType("Expected literal type in CALLELEMENTFUNCTION call.");

			var elementContext = request.moduleContext.resolveElementContext(TValue.asString(varElement));
			request.pushValue(TLogic.callElementFunction(request, response, TValue.asString(varFunctionName), elementContext));
		}
	},

	/* QUEUEACTION */
	{
		"name": 'QUEUEACTION', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varAction = request.popValue();

			if (!TValue.isAction(varAction))
				throw TAMEError.UnexpectedValueType("Expected action type in QUEUEACTION call.");

			var action = request.moduleContext.resolveAction(TValue.asString(varAction));

			if (action.type != TAMEConstants.ActionType.GENERAL)
				throw TAMEInterrupt.Error(action.identity + " is not a general action.");

			var tameAction = TAction.create(action);
			request.addActionItem(tameAction);
			response.trace(request, "Enqueued "+tameAction.toString());
		}
	},

	/* QUEUEACTIONSTRING */
	{
		"name": 'QUEUEACTIONSTRING', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varTarget = request.popValue();
			var varAction = request.popValue();

			if (!TValue.isLiteral(varTarget))
				throw TAMEError.UnexpectedValueType("Expected literal type in QUEUEACTIONSTRING call.");
			if (!TValue.isAction(varAction))
				throw TAMEError.UnexpectedValueType("Expected action type in QUEUEACTIONSTRING call.");

			var action = request.moduleContext.resolveAction(TValue.asString(varAction));

			if (action.type != TAMEConstants.ActionType.MODAL && action.type != TAMEConstants.ActionType.OPEN)
				throw TAMEInterrupt.Error(action.identity + " is not a modal nor open action.");

			var tameAction = TAction.createModal(action, TValue.asString(varTarget));
			request.addActionItem(tameAction);
			response.trace(request, "Enqueued "+tameAction.toString());
		}
	},

	/* QUEUEACTIONOBJECT */
	{
		"name": 'QUEUEACTIONOBJECT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varObject = request.popValue();
			var varAction = request.popValue();

			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected literal type in QUEUEACTIONOBJECT call.");
			if (!TValue.isAction(varAction))
				throw TAMEError.UnexpectedValueType("Expected action type in QUEUEACTIONOBJECT call.");

			var action = request.moduleContext.resolveAction(TValue.asString(varAction));
			var object = request.moduleContext.resolveElement(TValue.asString(varObject));

			if (action.type != TAMEConstants.ActionType.TRANSITIVE && action.type != TAMEConstants.ActionType.DITRANSITIVE)
				throw TAMEInterrupt.Error(action.identity + " is not a transitive nor ditransitive action.");

			var tameAction = TAction.createObject(action, object);
			request.addActionItem(tameAction);
			response.trace(request, "Enqueued "+tameAction.toString());
		}
	},

	/* QUEUEACTIONFOROBJECTSIN */
	{
		"name": 'QUEUEACTIONFOROBJECTSIN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varObjectContainer = request.popValue();
			var varAction = request.popValue();

			if (!TValue.isObjectContainer(varObjectContainer))
				throw TAMEError.UnexpectedValueType("Expected object-container type in QUEUEACTIONFOROBJECTSIN call.");
			if (!TValue.isAction(varAction))
				throw TAMEError.UnexpectedValueType("Expected action type in QUEUEACTIONFOROBJECTSIN call.");

			var context = request.moduleContext;
			var action = context.resolveAction(TValue.asString(varAction));

			if (action.type != TAMEConstants.ActionType.TRANSITIVE && action.type != TAMEConstants.ActionType.DITRANSITIVE)
				throw TAMEInterrupt.Error(action.identity + " is not a transitive nor ditransitive action.");

			var element = context.resolveElement(TValue.asString(varObjectContainer));
			Util.each(context.getObjectsOwnedByElement(element.identity), function(objectIdentity){
				var object = context.resolveElement(objectIdentity);
				var tameAction = TAction.createObject(action, object);
				request.addActionItem(tameAction);
				response.trace(request, "Enqueued "+tameAction.toString());
			});
		}

	},
	
	/* QUEUEACTIONFORTAGGEDOBJECTSIN */
	{
		"name": 'QUEUEACTIONFORTAGGEDOBJECTSIN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varTag = request.popValue();
			var varObjectContainer = request.popValue();
			var varAction = request.popValue();

			if (!TValue.isLiteral(varTag))
				throw TAMEError.UnexpectedValueType("Expected literal type in QUEUEACTIONFORTAGGEDOBJECTSIN call.");
			if (!TValue.isObjectContainer(varObjectContainer))
				throw TAMEError.UnexpectedValueType("Expected object-container type in QUEUEACTIONFORTAGGEDOBJECTSIN call.");
			if (!TValue.isAction(varAction))
				throw TAMEError.UnexpectedValueType("Expected action type in QUEUEACTIONFORTAGGEDOBJECTSIN call.");

			var context = request.moduleContext;
			var action = context.resolveAction(TValue.asString(varAction));

			if (action.type != TAMEConstants.ActionType.TRANSITIVE && action.type != TAMEConstants.ActionType.DITRANSITIVE)
				throw TAMEInterrupt.Error(action.identity + " is not a transitive nor ditransitive action.");

			var tagName = TValue.asString(varTag);
			var element = context.resolveElement(TValue.asString(varObjectContainer));
			Util.each(context.getObjectsOwnedByElement(element.identity), function(objectIdentity){
				if (!context.checkObjectHasTag(objectIdentity, tagName))
					return;
				
				var object = context.resolveElement(objectIdentity);
				var tameAction = TAction.createObject(action, object);
				request.addActionItem(tameAction);
				response.trace(request, "Enqueued "+tameAction.toString());
			});
		}

	},
	
	/* QUEUEACTIONOBJECT2 */
	{
		"name": 'QUEUEACTIONOBJECT2', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varObject2 = request.popValue();
			var varObject = request.popValue();
			var varAction = request.popValue();

			if (!TValue.isObject(varObject2))
				throw TAMEError.UnexpectedValueType("Expected literal type in QUEUEACTIONOBJECT2 call.");
			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected literal type in QUEUEACTIONOBJECT2 call.");
			if (!TValue.isAction(varAction))
				throw TAMEError.UnexpectedValueType("Expected action type in QUEUEACTIONOBJECT2 call.");

			var context = request.moduleContext;
			var action = context.resolveAction(TValue.asString(varAction));
			var object = context.resolveElement(TValue.asString(varObject));
			var object2 = context.resolveElement(TValue.asString(varObject2));

			if (action.type != TAMEConstants.ActionType.DITRANSITIVE)
				throw TAMEInterrupt.Error(action.identity + " is not a ditransitive action.");
			
			var tameAction = TAction.createObject2(action, object, object2);
			request.addActionItem(tameAction);
			response.trace(request, "Enqueued "+tameAction.toString());
		}
	},

	/* ADDCUE */
	{
		"name": 'ADDCUE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = request.popValue();
			var cue = request.popValue();

			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in ADDCUE call.");
			if (!TValue.isLiteral(cue))
				throw TAMEError.UnexpectedValueType("Expected literal type in ADDCUE call.");

			response.addCue(TValue.asString(cue), TValue.asString(value));
		}
	},

	/* TEXT */
	{
		"name": 'TEXT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in TEXT call.");

			response.addCue(TAMEConstants.Cue.TEXT, TValue.asString(value));
		}
	},

	/* TEXTLN */
	{
		"name": 'TEXTLN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in TEXTLN call.");

			response.addCue(TAMEConstants.Cue.TEXT, TValue.asString(value) + '\n');
		}
	},

	/* TEXTF */
	{
		"name": 'TEXTF', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in TEXTF call.");

			response.addCue(TAMEConstants.Cue.TEXTF, TValue.asString(value));
		}
	},

	/* TEXTFLN */
	{
		"name": 'TEXTFLN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in TEXTFLN call.");

			response.addCue(TAMEConstants.Cue.TEXTF, TValue.asString(value) + '\n');
		}
	},

	/* PAUSE */
	{
		"name": 'PAUSE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			response.addCue(TAMEConstants.Cue.PAUSE);
		}
	},

	/* WAIT */
	{
		"name": 'WAIT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in WAIT call.");

			response.addCue(TAMEConstants.Cue.WAIT, TValue.asLong(value));
		}
	},

	/* TIP */
	{
		"name": 'TIP', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in TIP call.");

			response.addCue(TAMEConstants.Cue.TIP, TValue.asString(value));
		}
	},

	/* INFO */
	{
		"name": 'INFO', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in INFO call.");

			response.addCue(TAMEConstants.Cue.INFO, TValue.asString(value));
		}
	},

	/* ASBOOLEAN */
	{
		"name": 'ASBOOLEAN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in ASBOOLEAN call.");

			request.pushValue(TValue.createBoolean(TValue.asBoolean(value)));
		}
	},

	/* ASINT */
	{
		"name": 'ASINT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in ASINT call.");

			request.pushValue(TValue.createInteger(TValue.asLong(value)));
		}
	},

	/* ASFLOAT */
	{
		"name": 'ASFLOAT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in ASFLOAT call.");

			request.pushValue(TValue.createFloat(TValue.asDouble(value)));
		}
	},

	/* ASSTRING */
	{
		"name": 'ASSTRING', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in ASSTRING call.");

			request.pushValue(TValue.createString(TValue.asString(value)));
		}
	},

	/* STRLENGTH */
	{
		"name": 'STRLENGTH', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRLENGTH call.");

			request.pushValue(TValue.createInteger(TValue.asString(value).length));
		}
	},

	/* STRCONCAT */
	{
		"name": 'STRCONCAT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();
			
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCONCAT call.");
			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCONCAT call.");

			request.pushValue(TValue.createString(TValue.asString(value1) + TValue.asString(value2)));
		}
	},

	/* STRREPLACE */
	{
		"name": 'STRREPLACE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value3 = request.popValue();
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACE call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACE call.");
			if (!TValue.isLiteral(value3))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACE call.");

			var replacement = TValue.asString(value3);
			var pattern = TValue.asString(value2);
			var source = TValue.asString(value1);

			request.pushValue(TValue.createString(source.replace(pattern, replacement)));
		}
	},

	/* STRREPLACEPATTERN */
	{
		"name": 'STRREPLACEPATTERN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value3 = request.popValue();
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACEPATTERN call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACEPATTERN call.");
			if (!TValue.isLiteral(value3))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACEPATTERN call.");

			var replacement = TValue.asString(value3);
			var pattern = TValue.asString(value2);
			var source = TValue.asString(value1);
			
			request.pushValue(TValue.createString(source.replace(new RegExp(pattern, 'm'), replacement)));
		}
	},

	/* STRREPLACEPATTERNALL */
	{
		"name": 'STRREPLACEPATTERNALL', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value3 = request.popValue();
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACEPATTERNALL call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACEPATTERNALL call.");
			if (!TValue.isLiteral(value3))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACEPATTERNALL call.");

			var replacement = TValue.asString(value3);
			var pattern = TValue.asString(value2);
			var source = TValue.asString(value1);
			
			request.pushValue(TValue.createString(source.replace(new RegExp(pattern, 'gm'), replacement)));
		}
	},

	/* STRINDEX */
	{
		"name": 'STRINDEX', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRINDEX call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRINDEX call.");
			
			var sequence = TValue.asString(value2);
			var str = TValue.asString(value1);

			request.pushValue(TValue.createInteger(str.indexOf(sequence)));
		}
	},

	/* STRLASTINDEX */
	{
		"name": 'STRLASTINDEX', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRLASTINDEX call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRLASTINDEX call.");
			
			var sequence = TValue.asString(value2);
			var str = TValue.asString(value1);

			request.pushValue(TValue.createInteger(str.lastIndexOf(sequence)));
		}
	},

	/* STRCONTAINS */
	{
		"name": 'STRCONTAINS', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCONTAINS call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCONTAINS call.");
			
			var sequence = TValue.asString(value2);
			var str = TValue.asString(value1);

			request.pushValue(TValue.createBoolean(str.indexOf(sequence) >= 0));
		}
	},

	/* STRCONTAINSPATTERN */
	{
		"name": 'STRCONTAINSPATTERN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCONTAINSPATTERN call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCONTAINSPATTERN call.");
			
			var pattern = TValue.asString(value2);
			var str = TValue.asString(value1);

			request.pushValue(TValue.createBoolean((new RegExp(pattern, 'gm')).test(str)));
		}
	},

	/* STRCONTAINSTOKEN */
	{
		"name": 'STRCONTAINSTOKEN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCONTAINSTOKEN call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCONTAINSTOKEN call.");
			
			var token = TValue.asString(value2).toLowerCase();
			var str = TValue.asString(value1).toLowerCase();

			request.pushValue(TValue.createBoolean(str.split(/\s+/).indexOf(token) >= 0));
		}
	},

	/* STRSTARTSWITH */
	{
		"name": 'STRSTARTSWITH', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRSTARTSWITH call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRSTARTSWITH call.");
			
			var sequence = TValue.asString(value2);
			var str = TValue.asString(value1);

			request.pushValue(TValue.createBoolean(str.substring(0, sequence.length) === sequence));
		}
	},

	/* STRENDSWITH */
	{
		"name": 'STRENDSWITH', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRENDSWITH call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRENDSWITH call.");
			
			var sequence = TValue.asString(value2);
			var str = TValue.asString(value1);

			request.pushValue(TValue.createBoolean(str.substring(str.length - sequence.length) === sequence));
		}
	},

	/* SUBSTRING */
	{
		"name": 'SUBSTRING', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value3 = request.popValue();
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in SUBSTRING call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in SUBSTRING call.");
			if (!TValue.isLiteral(value3))
				throw TAMEError.UnexpectedValueType("Expected literal type in SUBSTRING call.");

			var end = TValue.asLong(value3);
			var start = TValue.asLong(value2);
			var source = TValue.asString(value1);
			
			request.pushValue(TValue.createString(source.substring(start, end)));
		}
	},

	/* STRLOWER */
	{
		"name": 'STRLOWER', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRLOWER call.");

			request.pushValue(TValue.createString(TValue.asString(value1).toLowerCase()));
		}
	},

	/* STRUPPER */
	{
		"name": 'STRUPPER', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRUPPER call.");

			request.pushValue(TValue.createString(TValue.asString(value1).toUpperCase()));
		}
	},

	/* STRCHAR */
	{
		"name": 'STRCHAR', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCHAR call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCHAR call.");
			
			var index = TValue.asLong(value2);
			var str = TValue.asString(value1);

			if (index < 0 || index >= str.length)
				request.pushValue(TValue.createString(''));
			else
				request.pushValue(TValue.createString(str.charAt(index)));
		}
	},

	/* STRTRIM */
	{
		"name": 'STRTRIM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRTRIM call.");

			request.pushValue(TValue.createString(TValue.asString(value1).trim()));
		}
	},

	/* FLOOR */
	{
		"name": 'FLOOR', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in FLOOR call.");

			request.pushValue(TValue.createFloat(Math.floor(TValue.asDouble(value1))));
		}
	},

	/* CEILING */
	{
		"name": 'CEILING', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in CEILING call.");

			request.pushValue(TValue.createFloat(Math.ceil(TValue.asDouble(value1))));
		}
	},

	/* ROUND */
	{
		"name": 'ROUND', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in ROUND call.");

			request.pushValue(TValue.createFloat(Math.round(TValue.asDouble(value1))));
		}
	},

	/* FIX */
	{
		"name": 'FIX', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in FIX call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in FIX call.");
			
			var d = TValue.asDouble(value1);
			var f = TValue.asDouble(value2);
			var t = Math.pow(10, f);

			request.pushValue(TValue.createFloat(Math.round(d * t) / t));
		}
	},

	/* SQRT */
	{
		"name": 'SQRT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in SQRT call.");

			request.pushValue(TValue.createFloat(Math.sqrt(TValue.asDouble(value1))));
		}
	},

	/* PI */
	{
		"name": 'PI', 
		"doCommand": function(request, response, blockLocal, command)
		{
			request.pushValue(TValue.createFloat(Math.PI));
		}
	},

	/* E */
	{
		"name": 'E', 
		"doCommand": function(request, response, blockLocal, command)
		{
			request.pushValue(TValue.createFloat(Math.E));
		}
	},

	/* SIN */
	{
		"name": 'SIN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in SIN call.");

			request.pushValue(TValue.createFloat(Math.sin(TValue.asDouble(value1))));
		}
	},

	/* COS */
	{
		"name": 'COS', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in COS call.");

			request.pushValue(TValue.createFloat(Math.cos(TValue.asDouble(value1))));
		}
	},

	/* TAN */
	{
		"name": 'TAN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in TAN call.");

			request.pushValue(TValue.createFloat(Math.tan(TValue.asDouble(value1))));
		}
	},

	/* MIN */
	{
		"name": 'MIN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in MIN call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in MIN call.");
			
			request.pushValue(TValue.compare(value1, value2) <= 0 ? value1 : value2);
		}
	},

	/* MAX */
	{
		"name": 'MAX', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in MAX call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in MAX call.");
			
			request.pushValue(TValue.compare(value1, value2) > 0 ? value1 : value2);
		}
	},

	/* CLAMP */
	{
		"name": 'CLAMP', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value3 = request.popValue();
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in CLAMP call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in CLAMP call.");
			if (!TValue.isLiteral(value3))
				throw TAMEError.UnexpectedValueType("Expected literal type in CLAMP call.");

			var hi = TValue.asDouble(value3);
			var lo = TValue.asDouble(value2);
			var number = TValue.asDouble(value1);
			
			request.pushValue(TValue.createFloat(Math.min(Math.max(number, lo), hi)));
		}
	},

	/* IRANDOM */
	{
		"name": 'IRANDOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in IRANDOM call.");

			var value = TValue.asLong(value1);

			if (value == 0)
				request.pushValue(TValue.createInteger(0));
			else if (value < 0)
				request.pushValue(TValue.createInteger(-(Math.floor(Math.random() * Math.abs(value)))));
			else
				request.pushValue(TValue.createInteger(Math.floor(Math.random() * Math.abs(value))));
		}
	},

	/* FRANDOM */
	{
		"name": 'FRANDOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			request.pushValue(TValue.createFloat(Math.random()));
		}
	},

	/* GRANDOM */
	{
		"name": 'GRANDOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in GRANDOM call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in GRANDOM call.");
			
			// Box-Muller Approximate algorithm c/o Maxwell Collard on StackOverflow

			var stdDev = TValue.asDouble(value2);
			var mean = TValue.asDouble(value1);
			
		    var u = 1.0 - Math.random();
		    var v = 1.0 - Math.random();
		    var stdNormal = Math.sqrt(-2.0 * Math.log(u)) * Math.cos(2.0 * Math.PI * v);
		    var out = mean + stdDev * stdNormal;

		    request.pushValue(TValue.createFloat(out));
		}
	},

	/* TIME */
	{
		"name": 'TIME', 
		"doCommand": function(request, response, blockLocal, command)
		{
			request.pushValue(TValue.createInteger(Date.now()));
		}
	},

	/* SECONDS */
	{
		"name": 'SECONDS', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in SECONDS call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in SECONDS call.");

			var first = TValue.asLong(value1);
			var second = TValue.asLong(value2);

			request.pushValue(TValue.createInteger((second - first) / 1000));
		}
	},

	/* MINUTES */
	{
		"name": 'MINUTES', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in MINUTES call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in MINUTES call.");

			var first = TValue.asLong(value1);
			var second = TValue.asLong(value2);

			request.pushValue(TValue.createInteger((second - first) / (1000 * 60)));
		}
	},

	/* HOURS */
	{
		"name": 'HOURS', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in HOURS call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in HOURS call.");

			var first = TValue.asLong(value1);
			var second = TValue.asLong(value2);

			request.pushValue(TValue.createInteger((second - first) / (1000 * 60 * 60)));
		}
	},

	/* DAYS */
	{
		"name": 'DAYS', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in DAYS call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in DAYS call.");

			var first = TValue.asLong(value1);
			var second = TValue.asLong(value2);

			request.pushValue(TValue.createInteger((second - first) / (1000 * 60 * 60 * 24)));
		}
	},

	/* FORMATTIME */
	{
		"name": 'FORMATTIME', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in FORMATTIME call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in FORMATTIME call.");

			var date = TValue.asLong(value1);
			var format = TValue.asString(value2);

			request.pushValue(TValue.createString(Util.formatDate(date, format, false)));
		}
	},

	/* OBJECTHASNAME */
	{
		"name": 'OBJECTHASNAME', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var nameValue = request.popValue();
			var varObject = request.popValue();

			if (!TValue.isLiteral(nameValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in OBJECTHASNAME call.");
			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in OBJECTHASNAME call.");

			request.pushValue(TValue.createBoolean(request.moduleContext.checkObjectHasName(TValue.asString(varObject), TValue.asString(nameValue))));
		}
	},

	/* OBJECTHASTAG */
	{
		"name": 'OBJECTHASTAG', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var tagValue = request.popValue();
			var varObject = request.popValue();

			if (!TValue.isLiteral(tagValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in OBJECTHASTAG call.");
			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in OBJECTHASTAG call.");

			request.pushValue(TValue.createBoolean(request.moduleContext.checkObjectHasTag(TValue.asString(varObject), TValue.asString(tagValue))));
		}
	},

	/* ADDOBJECTNAME */
	{
		"name": 'ADDOBJECTNAME', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var nameValue = request.popValue();
			var varObject = request.popValue();

			if (!TValue.isLiteral(nameValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in ADDOBJECTNAME call.");
			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in ADDOBJECTNAME call.");

			request.moduleContext.addObjectName(TValue.asString(varObject), TValue.asString(nameValue));
		}
	},

	/* ADDOBJECTTAG */
	{
		"name": 'ADDOBJECTTAG', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var tagValue = request.popValue();
			var varObject = request.popValue();

			if (!TValue.isLiteral(tagValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in ADDOBJECTTAG call.");
			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in ADDOBJECTTAG call.");

			request.moduleContext.addObjectTag(TValue.asString(varObject), TValue.asString(tagValue));
		}
	},

	/* ADDOBJECTTAGTOALLIN */
	{
		"name": 'ADDOBJECTTAGTOALLIN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var tagValue = request.popValue();
			var elementValue = request.popValue();

			if (!TValue.isLiteral(tagValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in ADDOBJECTTAGTOALLIN call.");
			if (!TValue.isObjectContainer(elementValue))
				throw TAMEError.UnexpectedValueType("Expected object-container type in ADDOBJECTTAGTOALLIN call.");

			var context = request.moduleContext;
			var element = context.resolveElement(TValue.asString(elementValue));
			
			var tag = TValue.asString(tagValue);
			Util.each(context.getObjectsOwnedByElement(element.identity), function(objectIdentity){
				context.addObjectTag(objectIdentity, tag);
			});
		}
	},

	/* REMOVEOBJECTNAME */
	{
		"name": 'REMOVEOBJECTNAME', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var nameValue = request.popValue();
			var varObject = request.popValue();

			if (!TValue.isLiteral(nameValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in REMOVEOBJECTNAME call.");
			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in REMOVEOBJECTNAME call.");

			request.moduleContext.removeObjectName(TValue.asString(varObject), TValue.asString(nameValue));
		}
	},

	/* REMOVEOBJECTTAG */
	{
		"name": 'REMOVEOBJECTTAG', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var tagValue = request.popValue();
			var varObject = request.popValue();

			if (!TValue.isLiteral(tagValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in REMOVEOBJECTTAG call.");
			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in REMOVEOBJECTTAG call.");

			request.moduleContext.removeObjectTag(TValue.asString(varObject), TValue.asString(tagValue));
		}
	},

	/* REMOVEOBJECTTAGFROMALLIN */
	{
		"name": 'REMOVEOBJECTTAGFROMALLIN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var tagValue = request.popValue();
			var elementValue = request.popValue();

			if (!TValue.isLiteral(tagValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in REMOVEOBJECTTAGFROMALLIN call.");
			if (!TValue.isObjectContainer(elementValue))
				throw TAMEError.UnexpectedValueType("Expected object-container type in REMOVEOBJECTTAGFROMALLIN call.");

			var context = request.moduleContext;
			var element = context.resolveElement(TValue.asString(elementValue));
			
			var tag = TValue.asString(tagValue);
			Util.each(context.getObjectsOwnedByElement(element.identity), function(objectIdentity){
				context.removeObjectTag(objectIdentity, tag);
			});
		}
	},

	/* GIVEOBJECT */
	{
		"name": 'GIVEOBJECT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varObject = request.popValue();
			var varObjectContainer = request.popValue();

			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in GIVEOBJECT call.");
			if (!TValue.isObjectContainer(varObjectContainer))
				throw TAMEError.UnexpectedValueType("Expected object-container type in GIVEOBJECT call.");

			var element = request.moduleContext.resolveElement(TValue.asString(varObjectContainer));

			request.moduleContext.addObjectToElement(element.identity, TValue.asString(varObject));
		}
	},

	/* REMOVEOBJECT */
	{
		"name": 'REMOVEOBJECT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varObject = request.popValue();

			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in REMOVEOBJECT call.");

			request.moduleContext.removeObject(TValue.asString(varObject));
		}
	},

	/* MOVEOBJECTSWITHTAG */
	{
		"name": 'MOVEOBJECTSWITHTAG', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var tagValue = request.popValue();
			var varObjectContainerDest = request.popValue();
			var varObjectContainerSource = request.popValue();

			if (!TValue.isLiteral(tagValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in MOVEOBJECTSWITHTAG call.");
			if (!TValue.isObjectContainer(varObjectContainerDest))
				throw TAMEError.UnexpectedValueType("Expected object-container type in MOVEOBJECTSWITHTAG call.");
			if (!TValue.isObjectContainer(varObjectContainerSource))
				throw TAMEError.UnexpectedValueType("Expected object-container type in MOVEOBJECTSWITHTAG call.");

			var context = request.moduleContext;
			var destination = context.resolveElement(TValue.asString(varObjectContainerDest));
			var source = context.resolveElement(TValue.asString(varObjectContainerSource));
			var tag = TValue.asString(tagValue);
			
			Util.each(context.getObjectsOwnedByElement(source.identity), function(objectIdentity){
				if (context.checkObjectHasTag(objectIdentity, tag))
					context.addObjectToElement(destination.identity, objectIdentity);
			});
		}
	},

	/* OBJECTCOUNT */
	{
		"name": 'OBJECTCOUNT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var elementValue = request.popValue();

			if (!TValue.isObjectContainer(elementValue))
				throw TAMEError.UnexpectedValueType("Expected object-container type in OBJECTCOUNT call.");

			var element = request.moduleContext.resolveElement(TValue.asString(elementValue));

			request.pushValue(TValue.createInteger(request.moduleContext.getObjectsOwnedByElementCount(element.identity)));
		}
	},

	/* HASOBJECT */
	{
		"name": 'HASOBJECT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varObject = request.popValue();
			var varObjectContainer = request.popValue();

			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in HASOBJECT call.");
			if (!TValue.isObjectContainer(varObjectContainer))
				throw TAMEError.UnexpectedValueType("Expected object-container type in HASOBJECT call.");

			var element = request.moduleContext.resolveElement(TValue.asString(varObjectContainer));

			request.pushValue(TValue.createBoolean(request.moduleContext.checkElementHasObject(element.identity, TValue.asString(varObject))));
		}
	},

	/* OBJECTHASNOOWNER */
	{
		"name": 'OBJECTHASNOOWNER', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varObject = request.popValue();

			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in OBJECTHASNOOWNER call.");

			request.pushValue(TValue.createBoolean(request.moduleContext.checkObjectHasNoOwner(TValue.asString(varObject))));
		}
	},

	/* PLAYERISINROOM */
	{
		"name": 'PLAYERISINROOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varRoom = request.popValue();
			var varPlayer = request.popValue();
			
			if (!TValue.isRoom(varRoom))
				throw TAMEError.UnexpectedValueType("Expected room type in PLAYERISINROOM call.");
			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in PLAYERISINROOM call.");

			var context = request.moduleContext;
			var room = context.resolveElement(TValue.asString(varRoom));
			var player = context.resolveElement(TValue.asString(varPlayer));

			request.pushValue(TValue.createBoolean(context.checkPlayerIsInRoom(player.identity, room.identity)))
		}
	},

	/* PLAYERCANACCESSOBJECT */
	{
		"name": 'PLAYERCANACCESSOBJECT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varObject = request.popValue();
			var varPlayer = request.popValue();

			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in PLAYERCANACCESSOBJECT call.");
			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in PLAYERCANACCESSOBJECT call.");

			var player = request.moduleContext.resolveElement(TValue.asString(varPlayer));

			request.pushValue(TValue.createBoolean(TLogic.checkObjectAccessibility(request, response, player.identity, TValue.asString(varObject))));
		}
	},

	/* BROWSE */
	{
		"name": 'BROWSE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varObjectContainer = request.popValue();

			if (!TValue.isObjectContainer(varObjectContainer))
				throw TAMEError.UnexpectedValueType("Expected object-container type in BROWSE call.");

			var element = request.moduleContext.resolveElement(TValue.asString(varObjectContainer));

			TLogic.doBrowse(request, response, element.identity);
		}
	},

	/* BROWSETAGGED */
	{
		"name": 'BROWSETAGGED', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varTag = request.popValue();
			var varObjectContainer = request.popValue();

			if (!TValue.isLiteral(varTag))
				throw TAMEError.UnexpectedValueType("Expected literal type in BROWSETAGGED call.");
			if (!TValue.isObjectContainer(varObjectContainer))
				throw TAMEError.UnexpectedValueType("Expected object-container type in BROWSETAGGED call.");

			var tagName = TValue.asString(varTag);
			var element = request.moduleContext.resolveElement(TValue.asString(varObjectContainer));

			TLogic.doBrowse(request, response, element.identity, tagName);
		}
	},

	/* ELEMENTHASANCESTOR */
	{
		"name": 'ELEMENTHASANCESTOR', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varParent = request.popValue();
			var varElement = request.popValue();

			if (!TValue.isElement(varElement))
				throw TAMEError.UnexpectedValueType("Expected element type in ELEMENTHASANCESTOR call.");
			if (!TValue.isElement(varParent))
				throw TAMEError.UnexpectedValueType("Expected element type in ELEMENTHASANCESTOR call.");

			var context = request.moduleContext;
			var parentIdentity = context.resolveElement(TValue.asString(varParent)).identity;
			var element = context.resolveElement(TValue.asString(varElement));

			var found = false;
			while (element)
			{
				if (element.identity == parentIdentity)
				{
					found = true;
					break;
				}
				
				element = element.parent ? context.resolveElement(element.parent) : null;
			}

			request.pushValue(TValue.createBoolean(found));
		}
	},

	/* SETPLAYER */
	{
		"name": 'SETPLAYER', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varPlayer = request.popValue();

			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in SETPLAYER call.");

			var player = request.moduleContext.resolveElement(TValue.asString(varPlayer));

			TLogic.doPlayerSwitch(request, response, player.identity);
		}
	},

	/* SETROOM */
	{
		"name": 'SETROOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varRoom = request.popValue();
			var varPlayer = request.popValue();

			if (!TValue.isRoom(varRoom))
				throw TAMEError.UnexpectedValueType("Expected room type in SETROOM call.");
			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in SETROOM call.");

			var context = request.moduleContext;
			var room = context.resolveElement(TValue.asString(varRoom));
			var player = context.resolveElement(TValue.asString(varPlayer));
			
			TLogic.doRoomSwitch(request, response, player.identity, room.identity);
		}
	},

	/* PUSHROOM */
	{
		"name": 'PUSHROOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varRoom = request.popValue();
			var varPlayer = request.popValue();

			if (!TValue.isRoom(varRoom))
				throw TAMEError.UnexpectedValueType("Expected room type in PUSHROOM call.");
			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in PUSHROOM call.");

			var context = request.moduleContext;
			var room = context.resolveElement(TValue.asString(varRoom));
			var player = context.resolveElement(TValue.asString(varPlayer));

			TLogic.doRoomPush(request, response, player.identity, room.identity);
		}
	},

	/* POPROOM */
	{
		"name": 'POPROOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varPlayer = request.popValue();

			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in POPROOM call.");

			var context = request.moduleContext;
			var player = context.resolveElement(TValue.asString(varPlayer));

			var currentRoom = context.getCurrentRoom(player.identity);
			
			if (currentRoom == null)
				throw TAMEInterrupt.Error("No rooms for player" + TLogic.elementToString(player));
			
			TLogic.doRoomPop(request, response, player.identity);
		}
	},

	/* SWAPROOM */
	{
		"name": 'SWAPROOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varRoom = request.popValue();
			var varPlayer = request.popValue();

			if (!TValue.isRoom(varRoom))
				throw TAMEError.UnexpectedValueType("Expected room type in SWAPROOM call.");
			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in SWAPROOM call.");

			var context = request.moduleContext;
			var player = context.resolveElement(TValue.asString(varPlayer));

			if (player == null)
				throw TAMEInterrupt.Error("No current player!");

			var nextRoom = context.resolveElement(TValue.asString(varRoom)); 
			var currentRoom = context.getCurrentRoom(player.identity);

			if (currentRoom == null)
				throw new ErrorInterrupt("No rooms for current player!");
			
			TLogic.doRoomPop(request, response, player.identity);
			TLogic.doRoomPush(request, response, player.identity, nextRoom.identity);
		}
	},

	/* CURRENTPLAYERIS */
	{
		"name": 'CURRENTPLAYERIS', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varPlayer = request.popValue();

			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in CURRENTPLAYERIS call.");

			var context = request.moduleContext;
			var player = context.resolveElement(TValue.asString(varPlayer));
			var currentPlayer = context.getCurrentPlayer();
			
			request.pushValue(TValue.createBoolean(currentPlayer != null && player.identity == currentPlayer.identity));
		}
	},

	/* NOCURRENTPLAYER */
	{
		"name": 'NOCURRENTPLAYER', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var currentPlayer = request.moduleContext.getCurrentPlayer();
			request.pushValue(TValue.createBoolean(currentPlayer == null));
		}
	},

	/* CURRENTROOMIS */
	{
		"name": 'CURRENTROOMIS', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varRoom = request.popValue();
			var varPlayer = request.popValue();

			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in CURRENTROOMIS call.");
			if (!TValue.isRoom(varRoom))
				throw TAMEError.UnexpectedValueType("Expected room type in CURRENTROOMIS call.");

			var context = request.moduleContext;
			var playerIdentity = TValue.asString(varPlayer);
			var player = context.resolveElement(playerIdentity);
			var room = context.resolveElement(TValue.asString(varRoom));
			
			var currentRoom = context.getCurrentRoom(player.identity);
			request.pushValue(TValue.createBoolean(currentRoom != null && room.identity == currentRoom.identity));
		}
	},

	/* NOCURRENTROOM */
	{
		"name": 'NOCURRENTROOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varPlayer = request.popValue();

			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in NOCURRENTROOM call.");
			
			var context = request.moduleContext;
			var playerIdentity = TValue.asString(varPlayer);
			var player = context.resolveElement(playerIdentity);
			var currentRoom = context.getCurrentRoom(player.identity);
			request.pushValue(TValue.createBoolean(currentRoom == null));
		}
	},

	/* IDENTITY */
	{
		"name": 'IDENTITY', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var elementValue = request.popValue();
			
			if (!TValue.isElement(elementValue))
				throw TAMEError.UnexpectedValueType("Expected element type in IDENTITY call.");
			
			var element = request.moduleContext.resolveElement(TValue.asString(elementValue));
			request.pushValue(TValue.createString(element.identity));
		}
	},

	/* HEADER */
	{
		"name": 'HEADER', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var headerName = request.popValue();
			
			if (!TValue.isLiteral(headerName))
				throw TAMEError.UnexpectedValueType("Expected literal type in HEADER call.");
			
			request.pushValue(TValue.createString(request.moduleContext.module.header[TValue.asString(headerName)]));
		}
	},

];


/****************************************************************************
 * Main logic junk.
 ****************************************************************************/

/**
 * Sets a value on a variable hash.
 * @param valueHash the hash that contains the variables.
 * @param variableName the variable name.
 * @param value the value.
 */
TLogic.setValue = function(valueHash, variableName, value)
{
	variableName = variableName.toLowerCase();
	valueHash[variableName] = value;
};

/**
 * Sets a value on a variable hash.
 * @param valueHash the hash that contains the variables.
 * @param variableName the variable name.
 * @return the corresponding value or TValue.createBoolean(false) if no value.
 */
TLogic.getValue = function(valueHash, variableName)
{
	variableName = variableName.toLowerCase();
	if (!valueHash[variableName])
		return TValue.createBoolean(false);
	else
		return valueHash[variableName];
};

/**
 * Clears a value on a variable hash.
 * @param valueHash the hash that contains the variables.
 * @param variableName the variable name.
 */
TLogic.clearValue = function(valueHash, variableName)
{
	variableName = variableName.toLowerCase();
	delete valueHash[variableName];
};

/**
 * Turns a command into a readable string.
 * @param cmdObject (Object) the command object.
 * @return a string.
 */
TLogic.commandToString = function(cmdObject)
{
	var out = TCommandFunctions[cmdObject.opcode].name;
	if (cmdObject.operand0 != null)
		out += ' ' + TValue.toString(cmdObject.operand0);
	if (cmdObject.operand1 != null)
		out += ' ' + TValue.toString(cmdObject.operand1);
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
 * Turns an element into a readable string.
 * @param elemObject (Object) the command object.
 * @return a string.
 */
TLogic.elementToString = function(elemObject)
{
	return elemObject.tameType + "[" + elemObject.identity + "]";
};

/**
 * Checks if an action is allowed on an element (player or room).
 * @param element the element to check.
 * @param action the action that is being called.
 * @return true if allowed, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.allowsAction = function(element, action)
{
	if (!element.permittedActionList)
		return true;
	else if (element.permissionType == TAMEConstants.RestrictionType.ALLOW)
		return element.permittedActionList.indexOf(action.identity) >= 0;
	else if (action.restricted)
		return false;
	else if (element.permissionType == TAMEConstants.RestrictionType.FORBID)
		return permittedActionList.indexOf(action.identity) < 0;
	else
		throw TAMEError.Module("Bad or unknown permission type found: "+permissionType);
};

/**
 * Executes a block of commands.
 * @param block (Array) the block of commands.
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param blockLocal (Object) the local variables on the block call.
 * @throws TAMEInterrupt if an interrupt occurs. 
 */
TLogic.executeBlock = function(block, request, response, blockLocal)
{
	response.trace(request, "Start block.");
	Util.each(block, function(command){
		response.trace(request, "CALL "+TLogic.commandToString(command));
		TLogic.executeCommand(request, response, blockLocal, command);
	});
	response.trace(request, "End block.");
};

/**
 * Increments the runaway command counter and calls the command.  
 * Command index.
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param blockLocal (Object) the local variables on the block call.
 * @param command (Object) the command object.
 * @throws TAMEInterrupt if an interrupt occurs. 
 */
TLogic.executeCommand = function(request, response, blockLocal, command)
{
	TCommandFunctions[command.opcode].doCommand(request, response, blockLocal, command);
	response.incrementAndCheckCommandsExecuted(request.moduleContext.commandRunawayMax);
}

/**
 * Calls the conditional block on a command, returning the result as a .
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param blockLocal (Object) the local variables on the block call.
 * @param command (Object) the command object.
 * @return true if result is equivalent to true, false if not.
 * @throws TAMEInterrupt if an interrupt occurs. 
 */
TLogic.callConditional = function(commandName, request, response, blockLocal, command)
{
	// block should contain arithmetic commands and a last push.
	var conditional = command.conditionalBlock;
	if (!conditional)
		throw TAMEError.ModuleExecution("Conditional block for "+commandName+" does NOT EXIST!");
	
	response.trace(request, "Calling "+commandName+" conditional...");
	TLogic.executeBlock(conditional, request, response, blockLocal);

	// get remaining expression value.
	var value = request.popValue();
	
	if (!TValue.isLiteral(value))
		throw TAMEError.UnexpectedValueType("Expected literal type after "+commandName+" conditional block execution.");

	var result = TValue.asBoolean(value);
	response.trace(request, "Result "+TValue.toString(value)+" evaluates "+result+".");
	return result;
}


/**
 * Enqueues an action based on how it is interpreted.
 * @param request the request object.
 * @param response the response object.
 * @param interpreterContext the interpreter context (left after interpretation).
 * @return true if interpret was good and an action was enqueued, false if error.
 * @throws TAMEInterrupt if an uncaught interrupt occurs.
 * @throws TAMEError if something goes wrong during execution.
 */
TLogic.enqueueInterpretedAction = function(request, response, interpreterContext) 
{
	var action = interpreterContext.action;
	if (action == null)
	{
		response.trace(request, "Performing unknown action.");
		if (!TLogic.callUnknownAction(request, response))
			response.addCue(TAMEConstants.Cue.ERROR, "ACTION IS UNKNOWN! (make a better in-universe handler!).");
		return false;
	}
	else
	{
		switch (action.type)
		{
			default:
			case TAMEConstants.ActionType.GENERAL:
			{
				request.addActionItem(TAction.create(action));
				return true;
			}

			case TAMEConstants.ActionType.OPEN:
			{
				if (!interpreterContext.targetLookedUp)
				{
					response.trace(request, "Performing open action "+action.identity+" with no target (incomplete)!");
					if (!TLogic.callActionIncomplete(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "ACTION INCOMPLETE (make a better in-universe handler!).");
					return false;
				}
				else
				{
					request.addActionItem(TAction.createModal(action, interpreterContext.target));
					return true;
				}
			}

			case TAMEConstants.ActionType.MODAL:
			{
				if (!interpreterContext.modeLookedUp)
				{
					response.trace(request, "Performing modal action "+action.identity+" with no mode (incomplete)!");
					if (!TLogic.callActionIncomplete(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "ACTION INCOMPLETE (make a better in-universe handler!).");
					return false;
				}
				else if (interpreterContext.mode == null)
				{
					response.trace(request, "Performing modal action "+action.identity+" with an unknown mode!");
					if (!TLogic.callBadAction(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "BAD ACTION (make a better in-universe handler!).");
					return false;
				}
				else
				{
					request.addActionItem(TAction.createModal(action, interpreterContext.mode));
					return true;
				}
			}

			case TAMEConstants.ActionType.TRANSITIVE:
			{
				if (interpreterContext.objectAmbiguous)
				{
					response.trace(request, "Object is ambiguous for action "+action.identity+".");
					if (!TLogic.callAmbiguousAction(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "OBJECT IS AMBIGUOUS (make a better in-universe handler!).");
					return false;
				}
				else if (!interpreterContext.object1LookedUp)
				{
					response.trace(request, "Performing transitive action "+action.identity+" with no object (incomplete)!");
					if (!TLogic.callActionIncomplete(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "ACTION INCOMPLETE (make a better in-universe handler!).");
					return false;
				}
				else if (interpreterContext.object1 == null)
				{
					response.trace(request, "Performing transitive action "+action.identity+" with an unknown object!");
					if (!TLogic.callBadAction(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "BAD ACTION (make a better in-universe handler!).");
					return false;
				}
				else
				{
					request.addActionItem(TAction.createObject(action, interpreterContext.object1));
					return true;
				}
			}
	
			case TAMEConstants.ActionType.DITRANSITIVE:
			{
				if (interpreterContext.objectAmbiguous)
				{
					response.trace(request, "Object is ambiguous for action "+action.identity+".");
					if (!TLogic.callAmbiguousAction(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "ONE OR MORE OBJECTS ARE AMBIGUOUS (make a better in-universe handler!).");
					return false;
				}
				else if (!interpreterContext.object1LookedUp)
				{
					response.trace(request, "Performing ditransitive action "+action.identity+" with no first object (incomplete)!");
					if (!TLogic.callActionIncomplete(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "ACTION INCOMPLETE (make a better in-universe handler!).");
					return false;
				}
				else if (interpreterContext.object1 == null)
				{
					response.trace(request, "Performing ditransitive action "+action.identity+" with an unknown first object!");
					if (!TLogic.callBadAction(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "BAD ACTION (make a better in-universe handler!).");
					return false;
				}
				else if (!interpreterContext.conjugateLookedUp)
				{
					response.trace(request, "Performing ditransitive action "+action.identity+" as a transitive one...");
					request.addActionItem(TAction.createObject(action, interpreterContext.object1));
					return true;
				}
				else if (!interpreterContext.conjugateFound)
				{
					response.trace(request, "Performing ditransitive action "+action.identity+" with an unknown conjugate!");
					if (!TLogic.callBadAction(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "BAD ACTION (make a better in-universe handler!).");
					return false;
				}
				else if (!interpreterContext.object2LookedUp)
				{
					response.trace(request, "Performing ditransitive action "+action.identity+" with no second object (incomplete)!");
					if (!TLogic.callActionIncomplete(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "ACTION INCOMPLETE (make a better in-universe handler!).");
					return false;
				}
				else if (interpreterContext.object2 == null)
				{
					response.trace(request, "Performing ditransitive action "+action.identity+" with an unknown second object!");
					if (!TLogic.callBadAction(request, response, action))
						response.addCue(TAMEConstants.Cue.ERROR, "BAD ACTION (make a better in-universe handler!).");
					return false;
				}
				else
				{
					request.addActionItem(TAction.createObject2(action, interpreterContext.object1, interpreterContext.object2));
					return true;
				}
			}
		}
	}
};

/**
 * Does an action loop: this keeps processing queued actions 
 * until there is nothing left to process.
 * @param request the request context.
 * @param response the response object.
 * @param tameAction (TAction) the action to process.
 * @throws TAMEInterrupt if an uncaught interrupt occurs.
 * @throws TAMEError if something goes wrong during execution.
 */
TLogic.processAction = function(request, response, tameAction) 
{
	try {
		
		switch (tameAction.action.type)
		{
			default:
			case TAMEConstants.ActionType.GENERAL:
				TLogic.doActionGeneral(request, response, tameAction.action);
				break;
			case TAMEConstants.ActionType.OPEN:
				TLogic.doActionOpen(request, response, tameAction.action, tameAction.target);
				break;
			case TAMEConstants.ActionType.MODAL:
				TLogic.doActionModal(request, response, tameAction.action, tameAction.target);
				break;
			case TAMEConstants.ActionType.TRANSITIVE:
				TLogic.doActionTransitive(request, response, tameAction.action, tameAction.object1);
				break;
			case TAMEConstants.ActionType.DITRANSITIVE:
				if (tameAction.object2 == null)
					TLogic.doActionTransitive(request, response, tameAction.action, tameAction.object1);
				else
					TLogic.doActionDitransitive(request, response, tameAction.action, tameAction.object1, tameAction.object2);
				break;
		}
		
	} catch (err) {
		// catch finish interrupt, throw everything else.
		if (!(err instanceof TAMEInterrupt) || err.type != TAMEInterrupt.Type.Finish)
			throw err;
	} 
	
	request.checkStackClear();
	
};

/**
 * Does an action loop: this keeps processing queued actions until there is nothing left to process.
 * @param request the request object.
 * @param response the response object.
 * @throws TAMEInterrupt if an uncaught interrupt occurs.
 * @throws TAMEError if something goes wrong during execution.
 */
TLogic.doAllActionItems = function(request, response) 
{
	while (request.hasActionItems())
		TLogic.processAction(request, response, request.nextActionItem());
};

/**
 * Does an action loop: this keeps processing queued actions 
 * until there is nothing left to process.
 * @param request the request object.
 * @param response the response object.
 * @param afterSuccessfulCommand if true, executes the "after successful command" block.
 * @param afterFailedCommand if true, executes the "after failed command" block.
 * @param afterEveryCommand if true, executes the "after every command" block.
 * @throws TAMEInterrupt if an uncaught interrupt occurs.
 * @throws TAMEError if something goes wrong during execution.
 */
TLogic.processActionLoop = function(request, response, afterSuccessfulCommand, afterFailedCommand, afterEveryCommand) 
{
	TLogic.doAllActionItems(request, response);
	if (afterSuccessfulCommand)
	{
		TLogic.doAfterSuccessfulCommand(request, response);
		TLogic.doAllActionItems(request, response);
	}
	if (afterFailedCommand)
	{
		TLogic.doAfterFailedCommand(request, response);
		TLogic.doAllActionItems(request, response);
	}
	if (afterEveryCommand)
	{
		TLogic.doAfterEveryCommand(request, response);
		TLogic.doAllActionItems(request, response);
	}		
	
};

/**
 * Handles initializing a context. Must be called after a new context and game is started.
 * @param context the module context.
 * @param tracing if true, add trace cues.
 * @return (TResponse) the response from the initialize.
 */
TLogic.handleInit = function(context, tracing) 
{
	var request = new TRequest(context, "[INITIALIZE]", tracing);
	var response = new TResponse();
	
	response.interpretNanos = 0;
	var time = Util.nanoTime();

	try 
	{
		TLogic.initializeContext(request, response);
		TLogic.processActionLoop(request, response, false, false, false);
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

	response.requestNanos = Util.nanoTime() - time;
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
	var request = new TRequest(context, inputMessage, tracing);
	var response = new TResponse();

	var time = Util.nanoTime();
	var interpreterContext = TLogic.interpret(context, inputMessage);
	response.interpretNanos = Util.nanoTime() - time; 

	time = Util.nanoTime();
	
	try 
	{
		var good = TLogic.enqueueInterpretedAction(request, response, interpreterContext);
		TLogic.processActionLoop(request, response, good, !good, true);
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
	
	response.requestNanos = Util.nanoTime() - time;
	return response;
};

/**
 * Creates a viable blocklocal object for use in callBlock.
 * @param localValues (object) map of name to value. 
 */
TLogic.createBlockLocal = function(localValues)
{
	var out = {};
	// set locals
	Util.each(localValues, function(value, key){
		response.trace(request, "Setting local variable \""+key+"\" to \""+value+"\"");
		TLogic.setValue(out, key, value);
	});

	return out;
};

/**
 * Performs the necessary tasks for calling an object block.
 * Ensures that the block is called cleanly.
 * @param request (TRequest) the request object.
 * @param response (TResponse) the response object.
 * @param elementContext (object) the context that the block is executed through.
 * @param block [Object, ...] the block to execute.
 * @param isFunctionBlock (boolean) if true, this is a function call (changes some logic).
 * @param blockLocal (object) the initial block-local values to set on invoke.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callBlock = function(request, response, elementContext, block, isFunctionBlock, blockLocal)
{
	response.trace(request, "Pushing Context:"+elementContext.identity+"...");
	request.pushContext(elementContext);
	
	if (!blockLocal)
		blockLocal = {};
	
	try {
		TLogic.executeBlock(block, request, response, blockLocal);
	} catch (err) {
		// catch end interrupt, throw everything else.
		if (!(err instanceof TAMEInterrupt) || err.type != TAMEInterrupt.Type.End)
			throw err;
	} finally {
		response.trace(request, "Popping Context:"+elementContext.identity+"...");
		request.popContext();
	}
	
	if (!isFunctionBlock)
		request.checkStackClear();
	
};

/**
 * Calls a function from an arbitrary context, using the bound element as a lineage search point.
 * @param request the request object.
 * @param response the response object.
 * @param functionName the function to execute.
 * @param originContext the origin context (and then element).
 * @throws TAMEInterrupt if an interrupt occurs.
 * @return the return value from the function call. if no return, returns false.
 */
TLogic.callElementFunction = function(request, response, functionName, originContext)
{
	var context = request.moduleContext;
	var element = context.resolveElement(originContext.identity);

	var entry = context.resolveFunction(originContext.identity, functionName);
	if (entry == null)
		throw TAMEError.UnexpectedValue("No such function ("+functionName+") in lineage of element " + TLogic.elementToString(element));

	response.trace(request, "Calling function \""+functionName+"\"...");
	var blockLocal = {};
	var args = entry.arguments;
	for (var i = args.length - 1; i >= 0; i--)
	{
		var localValue = request.popValue();
		response.trace(request, "Setting local variable \""+args[i]+"\" to \""+localValue+"\"");
		blockLocal[args[i]] = localValue;
	}
	
	response.incrementAndCheckFunctionDepth(request.moduleContext.functionDepthMax);
	TLogic.callBlock(request, response, originContext, entry.block, true, blockLocal);
	response.decrementFunctionDepth();

	return TLogic.getValue(blockLocal, TAMEConstants.RETURN_VARIABLE);
}


/**
 * Interprets the input on the request.
 * @param context (TModuleContext) the module context.
 * @param input (string) the input text.
 * @return a new interpreter context using the input.
 */
TLogic.interpret = function(context, input)
{
	var tokens = input.toLowerCase().split(/\s+/);
	var interpreterContext = 
	{
		"tokens": tokens,
		"tokenOffset": 0,
		"objects": [null, null],
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

	TLogic.interpretAction(context, interpreterContext);

	var action = interpreterContext.action;
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
			TLogic.interpretObject1(context, interpreterContext);
			return interpreterContext;
		case TAMEConstants.ActionType.DITRANSITIVE:
			if (TLogic.interpretObject1(context, interpreterContext))
				if (TLogic.interpretConjugate(action, interpreterContext))
					TLogic.interpretObject2(context, interpreterContext);
			return interpreterContext;
	}
	
};

/**
 * Interprets an action from the input line.
 * @param moduleContext (TModuleContext) the module context.
 * @param interpreterContext (Object) the interpreter context.
 */
TLogic.interpretAction = function(moduleContext, interpreterContext)
{
	var module = moduleContext.module;
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;

	while (index < tokens.length)
	{
		if (sb.length > 0)
			sb += ' ';
		sb += tokens[index];
		index++;

		var next = module.getActionByName(sb);
		if (next != null)
		{
			interpreterContext.action = next;
			interpreterContext.tokenOffset = index;
		}
	
	}
	
};

/**
 * Interprets an action mode from the input line.
 * @param action (object:action) the action to use.
 * @param interpreterContext (Object) the interpreter context.
 */
TLogic.interpretMode = function(action, interpreterContext)
{
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;

	while (index < tokens.length)
	{
		if (sb.length > 0)
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
 * @param interpreterContext (Object) the interpreter context.
 */
TLogic.interpretOpen = function(interpreterContext)
{
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;
	
	while (index < tokens.length)
	{
		interpreterContext.targetLookedUp = true;
		if (sb.length > 0)
			sb += ' ';
		sb += tokens[index];
		index++;
	}
	
	interpreterContext.target = sb.length > 0 ? sb : null;
	interpreterContext.tokenOffset = index;
};

/**
 * Interprets an action conjugate from the input line (like "with" or "on" or whatever).
 * @param action the action to use.
 * @param interpreterContext (Object) the interpreter context.
 */
TLogic.interpretConjugate = function(action, interpreterContext)
{
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;
	var out = false;

	while (index < tokens.length)
	{
		if (sb.length > 0)
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
 * @param moduleContext (TModuleContext) the module context.
 * @param interpreterContext (Object) the interpreter context.
 */
TLogic.interpretObject1 = function(moduleContext, interpreterContext)
{
	var sb = '';
	var index = interpreterContext.tokenOffset;
	var tokens = interpreterContext.tokens;

	while (index < tokens.length)
	{
		if (sb.length > 0)
			sb += ' ';
		sb += tokens[index];
		index++;
		
		interpreterContext.object1LookedUp = true;
		var out = moduleContext.getAccessibleObjectsByName(sb, interpreterContext.objects, 0);
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
		if (sb.length > 0)
			sb += ' ';
		sb += tokens[index];
		index++;
		
		interpreterContext.object2LookedUp = true;
		var out = moduleContext.getAccessibleObjectsByName(sb, interpreterContext.objects, 0);
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
 * Checks if an object is accessible to a player.
 * @param request the request object.
 * @param response the response object.
 * @param playerIdentity the player viewpoint identity.
 * @param objectIdentity the object to check's identity.
 * @return true if the object is considered "accessible," false if not.
 */
TLogic.checkObjectAccessibility = function(request, response, playerIdentity, objectIdentity) 
{
	var context = request.moduleContext;
	var world = context.getElement('world');

	response.trace(request, "Check world for "+objectIdentity+"...");
	if (context.checkElementHasObject(world.identity, objectIdentity))
	{
		response.trace(request, "Found.");
		return true;
	}

	response.trace(request, "Check "+playerIdentity+" for "+objectIdentity+"...");
	if (context.checkElementHasObject(playerIdentity, objectIdentity))
	{
		response.trace(request, "Found.");
		return true;
	}

	var currentRoom = context.getCurrentRoom(playerIdentity);
	
	if (currentRoom != null)
	{
		response.trace(request, "Check "+currentRoom.identity+" for "+objectIdentity+"...");
		if (context.checkElementHasObject(currentRoom.identity, objectIdentity))
		{
			response.trace(request, "Found.");
			return true;
		}
	}
	
	response.trace(request, "Not found.");
	return false;
};


/**
 * Performs an arithmetic function on the stack.
 * @param request the request context.
 * @param response the response object.
 * @param functionType the function type (index).
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doArithmeticStackFunction = function(request, response, functionType)
{
	if (functionType < 0 || functionType >= TArithmeticFunctions.COUNT)
		throw TAMEError.UnexpectedValue("Expected arithmetic function type, got illegal value "+functionType+".");
	
	var operator = TArithmeticFunctions[functionType];
	response.trace(request, "Function is " + operator.name);
	
	if (operator.binary)
	{
		var v2 = request.popValue();
		var v1 = request.popValue();
		request.pushValue(operator.doOperation(v1, v2));
	}
	else
	{
		var v1 = request.popValue();
		request.pushValue(operator.doOperation(v1));
	}
};

/**
 * Attempts to perform a player switch.
 * @param request the request object.
 * @param response the response object.
 * @param playerIdentity the next player identity.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doPlayerSwitch = function(request, response, playerIdentity)
{
	var context = request.moduleContext;
	response.trace(request, "Setting current player to " + playerIdentity);
	context.setCurrentPlayer(playerIdentity);
};

/**
 * Attempts to perform a room stack pop for a player.
 * @param request the request object.
 * @param response the response object.
 * @param playerIdentity the player identity to pop a room context from.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doRoomPop = function(request, response, playerIdentity)
{
	var context = request.moduleContext;
	response.trace(request, "Popping top room from "+playerIdentity+".");
	context.popRoomFromPlayer(playerIdentity);
};

/**
 * Attempts to perform a room stack push for a player.
 * @param request the request object.
 * @param response the response object.
 * @param playerIdentity the player identity to push a room context onto.
 * @param roomIdentity the room identity to push.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doRoomPush = function(request, response, playerIdentity, roomIdentity)
{
	var context = request.moduleContext;
	response.trace(request, "Pushing "+roomIdentity+" on "+playerIdentity+".");
	context.pushRoomOntoPlayer(playerIdentity, roomIdentity);
};

/**
 * Attempts to perform a room switch.
 * @param request the request object.
 * @param response the response object.
 * @param playerIdentity the player identity that is switching rooms.
 * @param roomIdentity the target room identity.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doRoomSwitch = function(request, response, playerIdentity, roomIdentity)
{
	var context = request.moduleContext;
	response.trace(request, "Leaving rooms for "+playerIdentity+".");

	// pop all rooms on the stack.
	while (context.getCurrentRoom(playerIdentity) != null)
		TLogic.doRoomPop(request, response, playerIdentity);

	// push new room on the stack and call focus.
	TLogic.doRoomPush(request, response, playerIdentity, roomIdentity);
};

/**
 * Attempts to perform a player browse.
 * @param request the request object.
 * @param response the response object.
 * @param blockEntryTypeName the block entry type name.
 * @param elementIdentity the element identity to browse through.
 * @param tag the tag to filter by.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doBrowse = function(request, response, elementIdentity, tag)
{
	var context = request.moduleContext;
	
	var element = context.resolveElement(elementIdentity);
	var blockEntryTypeName = null;
	
	if (element.tameType === 'TContainer')
		blockEntryTypeName = 'ONCONTAINERBROWSE';
	else if (element.tameType === 'TPlayer')
		blockEntryTypeName = 'ONPLAYERBROWSE';
	else if (element.tameType === 'TRoom')
		blockEntryTypeName = 'ONROOMBROWSE';
	else if (element.tameType === 'TWorld')
		blockEntryTypeName = 'ONWORLDBROWSE';
	else
		throw TAMEError.UnexpectedValueType("INTERNAL ERROR IN BROWSE.");

	response.trace(request, "Start browse "+TLogic.elementToString(element)+".");

	Util.each(context.getObjectsOwnedByElement(element.identity), function(objectIdentity)
	{
		var object = context.getElement(objectIdentity);
		var objectContext = context.getElementContext(objectIdentity);
		
		if (tag != null && !context.checkObjectHasTag(objectIdentity, tag))
			return;
		
		var objtostr = TLogic.elementToString(object);
		response.trace(request, "Check "+objtostr+" for browse block.");
		var block = context.resolveBlock(objectIdentity, blockEntryTypeName);
		if (block != null)
		{
			response.trace(request, "Found! Calling "+blockEntryTypeName+" block.");
			TLogic.callBlock(request, response, objectContext, block);
		}
	});
};

/**
 * Attempts to call the after successful command block on the world.
 * @param request the request object.
 * @param response the response object.
 * @throws TAMEInterrupt if an uncaught interrupt occurs.
 * @throws TAMEError if something goes wrong during execution.
 */
TLogic.doAfterSuccessfulCommand = function(request, response)
{
	response.trace(request, "Finding \"after successful command\" request block...");

	var context = request.moduleContext;
	var worldContext = context.getElementContext('world');
	var blockToCall = null;

	// get block on world.
	if ((blockToCall = context.resolveBlock(worldContext.identity, "AFTERSUCCESSFULCOMMAND")) != null)
	{
		response.trace(request, "Found \"after successful command\" block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
	}
	else
		response.trace(request, "No \"after successful command\" block to call.");
};

/**
 * Attempts to call the after failed command block on the world.
 * @param request the request object.
 * @param response the response object.
 * @throws TAMEInterrupt if an uncaught interrupt occurs.
 * @throws TAMEError if something goes wrong during execution.
 */
TLogic.doAfterFailedCommand = function(request, response)
{
	response.trace(request, "Finding \"after failed command\" request block...");

	var context = request.moduleContext;
	var worldContext = context.getElementContext('world');
	var blockToCall = null;

	// get block on world.
	if ((blockToCall = context.resolveBlock(worldContext.identity, "AFTERFAILEDCOMMAND")) != null)
	{
		response.trace(request, "Found \"after failed command\" block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
	}
	else
		response.trace(request, "No \"after failed command\" block to call.");
};

/**
 * Attempts to call the after every command block on the world.
 * @param request the request object.
 * @param response the response object.
 * @throws TAMEInterrupt if an uncaught interrupt occurs.
 * @throws TAMEError if something goes wrong during execution.
 */
TLogic.doAfterEveryCommand = function(request, response)
{
	response.trace(request, "Finding \"after every command\" request block...");

	var context = request.moduleContext;
	var worldContext = context.getElementContext('world');
	var blockToCall = null;

	// get block on world.
	if ((blockToCall = context.resolveBlock(worldContext.identity, "AFTEREVERYCOMMAND")) != null)
	{
		response.trace(request, "Found \"after every command\" block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
	}
	else
		response.trace(request, "No \"after every command\" block to call.");
};

/**
 * Call after module init block on the world.
 */
TLogic.callStartBlock = function(request, response)
{
	var context = request.moduleContext;
	response.trace(request, "Attempt to call start block on world.");
	var worldContext = context.getElementContext('world');

	if ((initBlock = context.resolveBlock('world', "START")) != null)
	{
		response.trace(request, "Calling start block from Context:"+worldContext.identity+".");
		TLogic.callBlock(request, response, worldContext, initBlock);
	}
	else
	{
		response.trace(request, "No start block on world.");
	}
};

/**
 * Call init on a single context.
 */
TLogic.callInitBlock = function(request, response, context)
{
	var elementIdentity = context.identity;
	response.trace(request, "Attempt init from Context:"+elementIdentity+".");
	var element = request.moduleContext.resolveElement(elementIdentity);
	
	var initBlock = request.moduleContext.resolveBlock(elementIdentity, "INIT");
	if (initBlock != null)
	{
		response.trace(request, "Calling init block from Context:"+elementIdentity+".");
		TLogic.callBlock(request, response, context, initBlock);
	}
	else
	{
		response.trace(request, "No init block.");
	}
};

/**
 * Call init on iterable contexts.
 */
TLogic.callInitOnContexts = function(request, response, contextList)
{
	Util.each(contextList, function(context)
	{
		TLogic.callInitBlock(request, response, context);
	});
};

/**
 * Initializes a newly-created context by executing each initialization block on each object.
 * Order is Containers, Objects, Rooms, Players, and the World.
 * @param request the request object containing the module context.
 * @param response the response object.
 * @throws TAMEInterrupt if an interrupt is thrown.
 * @throws TAMEFatalException if something goes wrong during execution.
 */
TLogic.initializeContext = function(request, response) 
{
	var context = request.moduleContext;
	
	response.trace(request, "Starting init...");

	var containerContexts = [];
	var objectContexts = [];
	var roomContexts = [];
	var playerContexts = [];
	
	Util.each(context.state.elements, function(elementContext)
	{
		var element = context.resolveElement(elementContext.identity);
		if (element.tameType === 'TContainer')
			containerContexts.push(elementContext);
		else if (element.tameType === 'TObject')
			objectContexts.push(elementContext);
		else if (element.tameType === 'TPlayer')
			playerContexts.push(elementContext);
		else if (element.tameType === 'TRoom')
			roomContexts.push(elementContext);
	});
	
	TLogic.callInitOnContexts(request, response, containerContexts);
	TLogic.callInitOnContexts(request, response, objectContexts);
	TLogic.callInitOnContexts(request, response, roomContexts);
	TLogic.callInitOnContexts(request, response, playerContexts);
	TLogic.callInitBlock(request, response, context.resolveElementContext("world"));
	TLogic.callStartBlock(request, response);
};

/**
 * Calls the appropriate action fail blocks if they exist on the world.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param worldContext the world context.
 * @return true if a fail block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callWorldAmbiguousActionBlock = function(request, response, action, worldContext)
{
	var context = request.moduleContext;
	var blockToCall = null;

	// get specific block on world.
	if ((blockToCall = context.resolveBlock(worldContext.identity, "ONAMBIGUOUSACTION", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found specific ambiguous action block on world for action "+action.identity+".");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	// get block on world.
	if ((blockToCall = context.resolveBlock(worldContext.identity, "ONAMBIGUOUSACTION")) != null)
	{
		response.trace(request, "Found default ambiguous action block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	return false;
};

/**
 * Calls the appropriate action fail blocks if they exist on a player.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param playerContext the player context.
 * @return true if a fail block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callPlayerAmbiguousActionBlock = function(request, response, action, playerContext)
{
	var context = request.moduleContext;
	var blockToCall = null;
	
	// get specific block on player.
	if ((blockToCall = context.resolveBlock(playerContext.identity, "ONAMBIGUOUSACTION", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found specific ambiguous action block in player "+playerContext.identity+" lineage for action "+action.identity+".");
		TLogic.callBlock(request, response, playerContext, blockToCall);
		return true;
	}

	// get block on player.
	if ((blockToCall = context.resolveBlock(playerContext.identity, "ONAMBIGUOUSACTION")) != null)
	{
		response.trace(request, "Found default ambiguous action block in player "+playerContext.identity+" lineage.");
		TLogic.callBlock(request, response, playerContext, blockToCall);
		return true;
	}
	
	return false;
};

/**
 * Attempts to call the ambiguous action blocks.
 * @param request the request object.
 * @param response the response object.
 * @param action the action used.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callAmbiguousAction = function(request, response, action)
{
	var context = request.moduleContext;
	var currentPlayerContext = context.getCurrentPlayerContext();

	if (currentPlayerContext != null && TLogic.callPlayerAmbiguousActionBlock(request, response, action, currentPlayerContext))
		return true;

	var worldContext = context.getElementContext('world');

	return TLogic.callWorldAmbiguousActionBlock(request, response, action, worldContext);
};

/**
 * Calls the appropriate bad action block on the world if it exists.
 * Bad actions are actions with mismatched conjugates, unknown modal parts, or unknown object references. 
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param worldContext the world context.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callWorldBadActionBlock = function(request, response, action, worldContext)
{
	var context = request.moduleContext;
	var world = context.getElement('world');
	
	var blockToCall = null;

	if ((blockToCall = context.resolveBlock('world', "ONBADACTION", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found specific bad action block on world with action %s.", action.getIdentity());
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	if ((blockToCall = context.resolveBlock('world', "ONBADACTION")) != null)
	{
		response.trace(request, "Found default bad action block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	response.trace(request, "No bad action block on world.");
	return false;
};

/**
 * Calls the appropriate bad action block on a player if it exists.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param playerContext the player context.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callPlayerBadActionBlock = function(request, response, action, playerContext)
{
	var context = request.moduleContext;

	var blockToCall = null;
	
	if ((blockToCall = context.resolveBlock(playerContext.identity, "ONBADACTION", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found specific bad action block in player "+playerContext.identity+" lineage, action "+action.identity+".");
		TLogic.callBlock(request, response, playerContext, blockToCall);
		return true;
	}

	if ((blockToCall = context.resolveBlock(playerContext.identity, "ONBADACTION")) != null)
	{
		response.trace(request, "Found default bad action block on player "+playerContext.identity+".");
		TLogic.callBlock(request, response, playerContext, blockToCall);
		return true;
	}

	response.trace(request, "No bad action block on player.");
	return false;
};

/**
 * Calls the appropriate bad action blocks if they exist.
 * Bad actions are actions with mismatched conjugates, unknown modal parts, or unknown object references. 
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callBadAction = function(request, response, action)
{
	var context = request.moduleContext;
	var currentPlayerContext = context.getCurrentPlayerContext();

	// try bad action on player.
	if (currentPlayerContext != null && TLogic.callPlayerBadActionBlock(request, response, action, currentPlayerContext))
		return true;

	var worldContext = context.getElementContext('world');

	// try bad action on world.
	return TLogic.callWorldBadActionBlock(request, response, action, worldContext);
};

/**
 * Calls the appropriate action forbidden block on a player.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param context the player context.
 * @return true if handled by this block, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callPlayerActionForbiddenBlock = function(request, response, action, playerContext)
{
	var context = request.moduleContext;

	// get forbid block.
	var forbidBlock = null;

	if ((forbidBlock = context.resolveBlock(playerContext.identity, "ONFORBIDDENACTION", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Got specific forbid block in player "+playerContext.identity+" lineage, action "+action.identity);
		TLogic.callBlock(request, response, playerContext, forbidBlock);
		return true;
	}
	
	if ((forbidBlock = context.resolveBlock(playerContext.identity, "ONFORBIDDENACTION")) != null)
	{
		response.trace(request, "Got default forbid block in player "+playerContext.identity+" lineage.");
		TLogic.callBlock(request, response, playerContext, forbidBlock);
		return true;
	}
	
	response.trace(request, "No forbid block on player.");
	return false;
};

/**
 * Calls the appropriate room action forbidden block on a player.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param context the room context.
 * @return true if handled by this block, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callRoomActionForbiddenBlock = function(request, response, action, playerContext)
{
	var context = request.moduleContext;

	// get forbid block.
	var forbidBlock = null;

	if ((forbidBlock = context.resolveBlock(playerContext.identity, "ONROOMFORBIDDENACTION", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Calling specific room forbid block in player "+playerContext.identity+" lineage, action "+action.identity);
		TLogic.callBlock(request, response, playerContext, forbidBlock);
		return true;
	}
	
	if ((forbidBlock = context.resolveBlock(playerContext.identity, "ONROOMFORBIDDENACTION")) != null)
	{
		response.trace(request, "Calling default room forbid block in player "+playerContext.identity+" lineage.");
		TLogic.callBlock(request, response, playerContext, forbidBlock);
		return true;
	}
	
	response.trace(request, "No room forbid block on player to call.");
	return false;
};

/**
 * Checks and calls the action forbidden blocks.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @return true if an action is forbidden and steps were taken to call a forbidden block, or false otherwise.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callCheckActionForbidden = function(request, response, action)
{
	var context = request.moduleContext;
	var currentPlayerContext = context.getCurrentPlayerContext();
	var currentPlayer = context.getCurrentPlayer();

	if (currentPlayerContext != null)
	{
		response.trace(request, "Checking current player "+TLogic.elementToString(currentPlayer)+" for action permission.");

		// check if the action is disallowed by the player.
		if (!TLogic.allowsAction(currentPlayer, action))
		{
			response.trace(request, "Action is forbidden.");
			if (!TLogic.callPlayerActionForbiddenBlock(request, response, action, currentPlayerContext))
			{
				response.addCue(TAMEConstants.Cue.ERROR, "ACTION IS FORBIDDEN (make a better in-universe handler!).");
				return true;
			}
		}

		// try current room.
		var currentRoomContext = context.getCurrentRoomContext();
		var currentRoom = context.getCurrentRoom();

		if (currentRoomContext != null)
		{
			response.trace(request, "Checking current room "+TLogic.elementToString(currentRoom)+" for action permission.");

			// check if the action is disallowed by the room.
			if (!TLogic.allowsAction(currentRoom, action))
			{
				response.trace(request, "Action is forbidden.");
				if (!TLogic.callRoomActionForbiddenBlock(request, response, action, currentPlayerContext))
				{
					response.addCue(TAMEConstants.Cue.ERROR, "ACTION IS FORBIDDEN IN THIS ROOM (make a better in-universe handler!).");
					return true;
				}
			}
		}
	}
	
	return false;
};

/**
 * Calls the appropriate action incomplete block on the world if it exists.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param worldContext the world context.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callWorldActionIncompleteBlock = function(request, response, action, worldContext)
{
	var context = request.moduleContext;
	
	var blockToCall = null;
	
	if ((blockToCall = context.resolveBlock('world', "ONINCOMPLETEACTION", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found specific action incomplete block on world, action "+action.identity+".");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	if ((blockToCall = context.resolveBlock('world', "ONINCOMPLETEACTION")) != null)
	{
		response.trace(request, "Found default action incomplete block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	response.trace(request, "No action incomplete block on world.");
	return false;
};

/**
 * Calls the appropriate action incomplete block on a player if it exists.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param context the player context.
 * @return true if a block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callPlayerActionIncompleteBlock = function(request, response, action, playerContext)
{
	var context = request.moduleContext;
	
	var blockToCall = null;
	
	if ((blockToCall = context.resolveBlock(playerContext.identity, "ONINCOMPLETEACTION", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found specific action incomplete block in player "+playerContext.identity+" lineage, action "+action.identity+".");
		TLogic.callBlock(request, response, playerContext, blockToCall);
		return true;
	}

	if ((blockToCall = context.resolveBlock(playerContext.identity, "ONINCOMPLETEACTION")) != null)
	{
		response.trace(request, "Found default action incomplete block in player "+playerContext.identity+" lineage.");
		TLogic.callBlock(request, response, playerContext, blockToCall);
		return true;
	}

	response.trace(request, "No action incomplete block on player.");
	return false;
};

/**
 * Calls the appropriate action incomplete blocks if they exist.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @return true if a fail block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callActionIncomplete = function(request, response, action)
{
	var context = request.moduleContext;
	var currentPlayerContext = context.getCurrentPlayerContext();

	// try incomplete on player.
	if (currentPlayerContext != null && TLogic.callPlayerActionIncompleteBlock(request, response, action, currentPlayerContext))
		return true;

	var worldContext = context.getElementContext('world');

	// try incomplete on world.
	return TLogic.callWorldActionIncompleteBlock(request, response, action, worldContext);
};

/**
 * Calls the appropriate action fail block on the world if it exists.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param worldContext the world context.
 * @return true if a fail block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callWorldActionFailBlock = function(request, response, action, worldContext)
{
	var context = request.moduleContext;
	
	var blockToCall = null;
	
	if ((blockToCall = context.resolveBlock('world', "ONFAILEDACTION", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found specific action failure block on world, action "+action.identity+".");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	if ((blockToCall = context.resolveBlock('world', "ONFAILEDACTION")) != null)
	{
		response.trace(request, "Found default action failure block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	response.trace(request, "No action failure block on world.");
	return false;
};

/**
 * Calls the appropriate action fail block on a player if it exists.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @param context the player context.
 * @return true if a fail block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callPlayerActionFailBlock = function(request, response, action, playerContext)
{
	var context = request.moduleContext;

	var blockToCall = null;
	
	if ((blockToCall = context.resolveBlock(playerContext.identity, "ONFAILEDACTION", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found specific action failure block in player "+playerContext.identity+" lineage, action "+action.identity+".");
		TLogic.callBlock(request, response, playerContext, blockToCall);
		return true;
	}

	if ((blockToCall = context.resolveBlock(playerContext.identity, "ONFAILEDACTION")) != null)
	{
		response.trace(request, "Found default action failure block in player "+playerContext.identity+" lineage.");
		TLogic.callBlock(request, response, playerContext, blockToCall);
		return true;
	}

	response.trace(request, "No action failure block on player.");
	return false;
};

/**
 * Calls the appropriate action fail blocks if they exist.
 * @param request the request object.
 * @param response the response object.
 * @param action the action attempted.
 * @return true if a fail block was called, false if not.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callActionFailed = function(request, response, action)
{
	var context = request.moduleContext;

	var currentPlayerContext = context.getCurrentPlayerContext();

	// try fail on player.
	if (currentPlayerContext != null && TLogic.callPlayerActionFailBlock(request, response, action, currentPlayerContext))
		return true;

	var worldContext = context.getElementContext('world');

	// try fail on world.
	return TLogic.callWorldActionFailBlock(request, response, action, worldContext);
};


/**
 * Attempts to call the after request block on the world.
 * @param request the request object.
 * @param response the response object.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doAfterRequest = function(request, response)
{
	response.trace(request, "Finding after request block...");
	var context = request.moduleContext;
	var world = context.getElement('world');
	
	// get block on world.
	var blockToCall;

	if ((blockToCall = context.resolveBlock('world', 'AFTERREQUEST')) != null)
	{
		var worldContext = context.getElementContext('world');
		response.trace(request, "Found after request block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
	}
	else
		response.trace(request, "No after request block to call.");
};

/**
 * Attempts to call the unknown action blocks.
 * @param request the request object.
 * @param response the response object.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.callUnknownAction = function(request, response)
{
	response.trace(request, "Finding unknown action blocks...");
	var context = request.moduleContext;
	var currentPlayerContext = context.getCurrentPlayerContext();

	var blockToCall = null;

	if (currentPlayerContext != null)
	{
		var currentPlayer = context.getCurrentPlayer();
		response.trace(request, "For current player "+TLogic.elementToString(currentPlayer)+"...");

		// get block on player.
		// find via inheritance.
		if ((blockToCall = context.resolveBlock(currentPlayer.identity, "ONUNKNOWNACTION"))  != null)
		{
			response.trace(request, "Found unknown action block on player.");
			TLogic.callBlock(request, response, currentPlayerContext, blockToCall);
			return true;
		}
	}
	
	var worldContext = context.getElementContext('world');

	// get block on world.
	if ((blockToCall = context.resolveBlock(worldContext.identity, "ONUNKNOWNACTION"))  != null)
	{
		response.trace(request, "Found unknown action block on player.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return true;
	}

	return false;
};

/**
 * Attempts to perform a general action.
 * @param request the request object.
 * @param response the response object.
 * @param action the action that is being called.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doActionGeneral = function(request, response, action)
{
	TLogic.doActionOpen(request, response, action, null);
};

/**
 * Attempts to perform a general action.
 * @param request the request object.
 * @param response the response object.
 * @param action the action that is being called.
 * @param openTarget if not null, added as a target variable.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doActionOpen = function(request, response, action, openTarget)
{
	var context = request.moduleContext;
	response.trace(request, "Performing general/open action "+TLogic.elementToString(action));

	if (TLogic.callCheckActionForbidden(request, response, action))
		return;

	var currentPlayerContext = context.getCurrentPlayerContext();
	var blockToCall = null;

	if (currentPlayerContext != null)
	{
		var currentPlayer = context.getCurrentPlayer();

		// try current room.
		var currentRoomContext = context.getCurrentRoomContext();
		if (currentRoomContext != null)
		{
			var currentRoom = context.getCurrentRoom();

			// get general action on room.
			if ((blockToCall = context.resolveBlock(currentRoom.identity, "ONACTION", [TValue.createAction(action.identity)])) != null)
			{
				response.trace(request, "Found general action block on room.");
				if (openTarget != null)
				{
					// just get the first one.
					var localmap = {};
					localmap[action.extraStrings[0]] = TValue.createString(openTarget);
					TLogic.callBlock(request, response, currentRoomContext, blockToCall, false, TLogic.createBlockLocal(localmap));
				}
				else
					TLogic.callBlock(request, response, currentRoomContext, blockToCall);
				return;
			}
			
			response.trace(request, "No general action block on room.");
		}
		
		// get general action on player.
		if ((blockToCall = context.resolveBlock(currentPlayer.identity, "ONACTION", [TValue.createAction(action.identity)])) != null)
		{
			response.trace(request, "Found general action block on player.");
			if (openTarget != null)
			{
				// just get the first one.
				var localmap = {};
				localmap[action.extraStrings[0]] = TValue.createString(openTarget);
				TLogic.callBlock(request, response, currentPlayerContext, blockToCall, false, TLogic.createBlockLocal(localmap));
			}
			else
				TLogic.callBlock(request, response, currentPlayerContext, blockToCall);
			return;
		}
		
		response.trace(request, "No general action block on player.");
	}
	
	var worldContext = context.getElementContext('world');
	var world = context.getElement('world');

	// get general action on world.
	if ((blockToCall = context.resolveBlock(world.identity, "ONACTION", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found general action block on world.");
		if (openTarget != null)
		{
			// just get the first one.
			var localmap = {};
			localmap[action.extraStrings[0]] = TValue.createString(openTarget);
			TLogic.callBlock(request, response, worldContext, blockToCall, false, TLogic.createBlockLocal(localmap));
		}
		else
			TLogic.callBlock(request, response, worldContext, blockToCall);
		return;
	}

	// try fail on player.
	if (currentPlayerContext != null && TLogic.callPlayerActionFailBlock(request, response, action, currentPlayerContext))
		return;

	// try fail on world.
	TLogic.callWorldActionFailBlock(request, response, action, worldContext);
};

/**
 * Attempts to perform a modal action.
 * @param request the request object.
 * @param response the response object.
 * @param action the action that is being called.
 * @param mode the mode to process.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doActionModal = function(request, response, action, mode)
{
	var context = request.moduleContext;
	response.trace(request, "Performing modal action "+TLogic.elementToString(action)+", \""+mode+"\"");

	if (TLogic.callCheckActionForbidden(request, response, action))
		return;

	var currentPlayerContext = context.getCurrentPlayerContext();
	var blockToCall = null;

	if (currentPlayerContext != null)
	{
		var currentPlayer = context.getCurrentPlayer();

		// try current room.
		var currentRoomContext = context.getCurrentRoomContext();
		if (currentRoomContext != null)
		{
			var currentRoom = context.getCurrentRoom();

			// get modal action on room.
			if ((blockToCall = context.resolveBlock(currentRoom.identity, "ONMODALACTION", [TValue.createAction(action.identity), TValue.createString(mode)])) != null)
			{
				response.trace(request, "Found modal action block on room.");
				TLogic.callBlock(request, response, currentRoomContext, blockToCall);
				return;
			}
			
			response.trace(request, "No modal action block on room.");
		}
		
		// get modal action on player.
		if ((blockToCall = context.resolveBlock(currentPlayer.identity, "ONMODALACTION", [TValue.createAction(action.identity), TValue.createString(mode)])) != null)
		{
			response.trace(request, "Found modal action block on player.");
			TLogic.callBlock(request, response, currentPlayerContext, blockToCall);
			return;
		}
		
		response.trace(request, "No modal action block on player.");
	}
	
	var worldContext = context.getElementContext('world');
	var world = context.getElement('world');

	// get modal action on world.
	if ((blockToCall = context.resolveBlock(world.identity, "ONMODALACTION", [TValue.createAction(action.identity), TValue.createString(mode)])) != null)
	{
		response.trace(request, "Found modal action block on world.");
		TLogic.callBlock(request, response, worldContext, blockToCall);
		return;
	}

	if (!TLogic.callActionFailed(request, response, action))
		response.addCue(TAMEConstants.Cue.ERROR, "ACTION FAILED (make a better in-universe handler!).");
};

/**
 * Attempts to perform a transitive action.
 * @param request the request object.
 * @param response the response object.
 * @param action the action that is being called.
 * @param object the target object for the action.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doActionTransitive = function(request, response, action, object)
{
	var context = request.moduleContext;
	response.trace(request, "Performing transitive action "+TLogic.elementToString(action)+" on "+TLogic.elementToString(object));
	
	if (TLogic.callCheckActionForbidden(request, response, action))
		return;

	var currentObjectContext = context.getElementContext(object.identity);
	var blockToCall = null;

	// call action on object.
	if ((blockToCall = context.resolveBlock(object.identity, "ONACTION", [TValue.createAction(action.identity)])) != null)
	{
		response.trace(request, "Found action block on object.");
		TLogic.callBlock(request, response, currentObjectContext, blockToCall);
		return;
	}
	
	if (!TLogic.callActionFailed(request, response, action))
		response.addCue(TAMEConstants.Cue.ERROR, "ACTION FAILED (make a better in-universe handler!).");
};


/**
 * Attempts to perform a ditransitive action for the ancestor search.
 * @param request the request object.
 * @param response the response object.
 * @param actionValue the action that is being called (value).
 * @param object the object to call the block on.
 * @param start the object to start the search from.
 * @return true if a block was found an called.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doActionDitransitiveAncestorSearch = function(request, response, actionValue, object, start)
{
	var blockToCall = null;
	var context = request.moduleContext;
	var ancestor = start.parent != null ? context.getElement(start.parent) : null;
	var objectContext = context.getElementContext(object.identity);

	while (ancestor != null)
	{
		if ((blockToCall = context.resolveBlock(object.identity, "ONACTIONWITHANCESTOR", [actionValue, TValue.createObject(ancestor.identity)])) != null)
		{
			response.trace(request, "Found action with ancestor block in object "+TLogic.elementToString(object)+" lineage - ancestor is "+TLogic.elementToString(ancestor)+".");
			TLogic.callBlock(request, response, objectContext, blockToCall);
			return true;
		}
		ancestor = ancestor.parent != null ? context.getElement(ancestor.parent) : null;
	}
	
	return false;
};

/**
 * Attempts to perform a ditransitive action.
 * @param request the request object.
 * @param response the response object.
 * @param action the action that is being called.
 * @param object1 the first object for the action.
 * @param object2 the second object for the action.
 * @throws TAMEInterrupt if an interrupt occurs.
 */
TLogic.doActionDitransitive = function(request, response, action, object1, object2)
{
	var context = request.moduleContext;
	response.trace(request, "Performing ditransitive action "+TLogic.elementToString(action)+" on "+TLogic.elementToString(object1)+" with "+TLogic.elementToString(object2));

	if (TLogic.callCheckActionForbidden(request, response, action))
		return;

	var currentObject1Context = context.getElementContext(object1.identity);
	var currentObject2Context = context.getElementContext(object2.identity);
	var blockToCall = null;

	var success = false;
	var actionValue = TValue.createAction(action.identity);
	
	// call action on each object. one or both need to succeed for no failure.
	if ((blockToCall = context.resolveBlock(object1.identity, "ONACTIONWITH", [actionValue, TValue.createObject(object2.identity)])) != null)
	{
		response.trace(request, "Found action block in object "+TLogic.elementToString(object1)+" lineage with "+TLogic.elementToString(object2));
		TLogic.callBlock(request, response, currentObject1Context, blockToCall);
		success = true;
	}
	if ((blockToCall = context.resolveBlock(object2.identity, "ONACTIONWITH", [actionValue, TValue.createObject(object1.identity)])) != null)
	{
		response.trace(request, "Found action block in object "+TLogic.elementToString(object2)+" lineage with "+TLogic.elementToString(object1));
		TLogic.callBlock(request, response, currentObject2Context, blockToCall);
		success = true;
	}
	if (success)
		return;
	
	// call action with ancestor on each object. one or both need to succeed for no failure.
	success |= TLogic.doActionDitransitiveAncestorSearch(request, response, actionValue, object1, object2);
	success |= TLogic.doActionDitransitiveAncestorSearch(request, response, actionValue, object2, object1);
	if (success)
		return;
	
	// attempt action with other on both objects.
	if ((blockToCall = context.resolveBlock(object1.identity, "ONACTIONWITHOTHER", [actionValue])) != null)
	{
		response.trace(request, "Found action with other block in object "+TLogic.elementToString(object1)+" lineage.");
		TLogic.callBlock(request, response, currentObject1Context, blockToCall);
		success = true;
	}
	if ((blockToCall = context.resolveBlock(object2.identity, "ONACTIONWITHOTHER", [actionValue])) != null)
	{
		response.trace(request, "Found action with other block in object "+TLogic.elementToString(object2)+" lineage.");
		TLogic.callBlock(request, response, currentObject2Context, blockToCall);
		success = true;
	}
	if (success)
		return;

	// if we STILL can't do it...
	if (!success)
	{
		response.trace(request, "No blocks called in ditransitive action call.");
		if (!TLogic.callActionFailed(request, response, action))
			response.addCue(TAMEConstants.Cue.ERROR, "ACTION FAILED (make a better in-universe handler!).");
	}
};



	var tameModule = new TModule(theader, tactions, telements);

	/**
	 * Creates a new context for the embedded module.
	 */
	this.newContext = function() 
	{
		return new TModuleContext(tameModule);
	};

	/**
	 * Initializes a context. Must be called after a new context and game is started.
	 * @param context the module context.
	 * @param tracing if true, add trace cues.
	 * @return (TResponse) the response from the initialize.
	 */
	this.initialize = function(context, tracing) 
	{
		return TLogic.handleInit(context, tracing);
	};
	
	/**
	 * Interprets and performs actions.
	 * @param context the module context.
	 * @param inputMessage the input message to interpret.
	 * @param tracing if true, add trace cues.
	 * @return (TResponse) the response.
	 */
	this.interpret = function(context, inputMessage, tracing) 
	{
		return TLogic.handleRequest(context, inputMessage, tracing);
	};

	/**
	 * Assists in parsing a cue with formatted text (TEXTF cue), or one known to have formatted text.
	 * @param sequence the character sequence to parse.
	 * @param tagStartFunc the function called on tag start. Should take one argument: the tag name.  
	 * @param tagEndFunc the function called on tag end. Should take one argument: the tag name.  
	 * @param textFunc the function called on tag contents (does not include tags - it is recommended to maintain a stack). Should take one argument: the text read inside tags.  
	 */
	this.parseFormatted = function(sequence, tagStartFunc, tagEndFunc, textFunc)
	{
		return Util.parseFormatted(sequence, tagStartFunc, tagEndFunc, textFunc);
	};

	return this;
	
})(
{"title":"Lifecycle Test"},[{"names":["quit"],"tameType":"TAction","identity":"a_quit","type":0},{"names":["test"],"tameType":"TAction","identity":"a_test","type":0}],[{"tameType":"TWorld","blockTable":{"ONUNKNOWNACTION()":[{"opcode":5,"operand0":{"type":"STRING","value":"dW5rbm93biE="}},{"opcode":30}],"AFTERFAILEDCOMMAND()":[{"opcode":5,"operand0":{"type":"STRING","value":"ZmFpbGVk"}},{"opcode":30}],"START()":[{"opcode":5,"operand0":{"type":"STRING","value":"U3RhcnQ="}},{"opcode":30}],"AFTEREVERYCOMMAND()":[{"opcode":5,"operand0":{"type":"STRING","value":"ZXZlcnk="}},{"opcode":30}],"AFTERSUCCESSFULCOMMAND()":[{"opcode":5,"operand0":{"type":"STRING","value":"c3VjY2Vzcw=="}},{"opcode":30}]},"identity":"world","functionTable":{}}]
);

/****************************************
 * NodeJS Shell
 ****************************************/



function print(text) {
	if (text)
		process.stdout.write(text);
}

function println(text) {
	if (!text)
		process.stdout.write('\n');
	else
		process.stdout.write(text + '\n');
}

function withEscChars(text) {
	var t = JSON.stringify(text);
	return t.substring(1, t.length - 1);
}


var RegexWhitespace = /\s/g;

/**
 * Prints a message out to a PrintStream, word-wrapped
 * to a set column width (in characters). The width cannot be
 * 1 or less or this does nothing. This will also turn any whitespace
 * character it encounters into a single space, regardless of speciality.
 * @param message the output message.
 * @param startColumn the starting column.
 * @param width the width in characters.
 * @return the ending column for subsequent calls.
 */
function printWrapped(message, startColumn, width) 
{
	
	if (width <= 1) return startColumn;
	
	var token = '';
	var line = '';
	var ln = startColumn;
	var tok = 0;
	
	for (var i = 0; i < message.length; i++)
	{
		var c = message.charAt(i);
		if (c == '\n') {
			line += token;
			ln += token.length;
			token = '';
			tok = 0;
			println(line);
			line = '';
			ln = 0;
		} 
		else if (RegexWhitespace.test(c))
		{
			line += token;
			ln += token.length;
			if (ln < width-1)
			{
				line += ' ';
				ln++;
			}
			token = '';
			tok = 0;
		} 
		else if (c == '-') 
		{
			line += token;
			ln += token.length;
			line += '-';
			ln++;
			token = '';
			tok = 0;
		} 
		else if (ln + token.length + 1 > width-1)
		{
			println(line);
			line = '';
			ln = 0;
			token += c;
			tok++;
		} 
		else 
		{
			token += c;
			tok++;
		}
	}
	
	if (line.length > 0)
		print(line);
	if (token.length > 0)
		print(token);
	
	return ln + tok;
}

// Need a better don't-eat-CPU-solution.
function sleep(sleepDuration) {
    var now = Date.now();
    while(Date.now() < now + sleepDuration){ /* do nothing */ } 
}


const readline = require('readline');
const fs = require('fs');

var debug = false;
var trace = false;

// Read commandline.
var args = process.argv;

for (var x in args) if (args.hasOwnProperty(x))
{
	var arg = args[x];
	
	if (arg == '--debug')
		debug = true;
	else if (arg == '--trace')
		trace = true;
}

// Create context.
var tamectx = TAME.newContext();

const rl = readline.createInterface ({
	input: process.stdin,
	output: process.stdout
});

var stop = false;
var pause = false;
var textBuffer = '';
var lastColumn = 0;

function startFormatTag(tag)
{
	// Nothing
}

function endFormatTag(tag)
{
	// Nothing
}

function formatText(text) 
{
	textBuffer += text;
}

/**
 * Handles a TAME cue (for debugging).
 * @return true to continue handling, false to halt.
 */
function debugCue(cue)
{
	var type = cue.type.toLowerCase();
	println('['+type+'] '+withEscChars(cue.content));
	if (type === 'quit' || type === 'fatal')
		stop = true;
		
	return true;
}

/**
 * Handles a TAME cue.
 * @return true to continue handling, false to halt.
 */
function doCue(cue)
{
	var type = cue.type.toLowerCase();
	var content = cue.content;
	
	if (type !== 'text' && type !== 'textf')
	{
		lastColumn = printWrapped(textBuffer, lastColumn, process.stdout.columns);
		textBuffer = '';
	}
	
	switch (type)
	{
	
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
			sleep(parseInt(content, 10));
			return true;

		case 'pause':
			pause = true;
			lastColumn = 0;
			return false;

		case 'trace':
			// Ignore trace.
			return true;

		case 'tip':	
			println('(TIP: '+content+')');
			lastColumn = 0;
			return true;

		case 'info':	
			println('INFO: '+content);
			lastColumn = 0;
			return true;

		case 'error':	
			println('\n!ERROR! '+content);
			lastColumn = 0;
			return true;

		case 'fatal':
			println('\n!!FATAL!! '+content);
			lastColumn = 0;
			stop = true;
			return false;
	}
	
}

var currentResponseCue = 0;
var currentResponse = null;

function responseStop(notDone)
{
	if (textBuffer.length > 0)
	{
		printWrapped(textBuffer, lastColumn, process.stdout.columns);
		lastColumn = 0;
		textBuffer = '';
	}

	if (stop)
		rl.close();
	else if (pause) 
	{
		rl.setPrompt('(CONTINUE) ');
		rl.prompt();
	} 
	else if (!notDone) 
	{
		if (!stop) 
		{
			rl.setPrompt('] ');
			println();
			rl.prompt();
		} 
		else
			rl.close();
	}
}

function responseRead()
{
    var keepGoing = true;
    while (currentResponseCue < currentResponse.responseCues.length && keepGoing) 
    {
        var cue = currentResponse.responseCues[currentResponseCue++];
        if (debug)
        	keepGoing = debugCue(cue);
        else
        	keepGoing = doCue(cue);
    }

    return currentResponseCue < currentResponse.responseCues.length;
}

/**
 * Handles a new TAME response.
 * @param response the TAME response object.
 */
function startResponse(response) 
{
	currentResponseCue = 0;
	currentResponse = response;
	
	var handler = debug ? debugCue : doCue;
	if (debug)
	{
		println('Interpret time: '+(response.interpretNanos/1000000.0)+' ms');
		println('Request time: '+(response.requestNanos/1000000.0)+' ms');
		println('Commands: '+response.commandsExecuted);
		println('Cues: '+response.responseCues.length);
	}
	println();
		
	responseStop(responseRead());
}

const COMMAND_SAVE = '!save';
const COMMAND_LOAD = '!load';

// Loop.
rl.on('line', function(line){
	line = line.trim();
	if (pause) {
		pause = false;
		responseStop(responseRead());
	} else {
		if (COMMAND_SAVE == line.substring(0, COMMAND_SAVE.length))
		{
			var name = line.substring(COMMAND_SAVE.length).trim();
			try {
				fs.writeFileSync(name+'.json', JSON.stringify(tamectx.state), {"encoding": 'utf8'});
				println("State saved: "+name+'.json');
			} catch (err) {
				println(err);
			}
			rl.prompt();
		}
		else if (COMMAND_LOAD == line.substring(0, COMMAND_LOAD.length))
		{
			var name = line.substring(COMMAND_LOAD.length).trim();
			try {
				var stateData = fs.readFileSync(name+'.json', {"encoding": 'utf8'});
				tamectx.state = JSON.parse(stateData);
				println("State loaded: "+name+'.json');
			} catch (err) {
				println(err);
			}
			rl.prompt();
		}
		else
			startResponse(TAME.interpret(tamectx, line.trim(), trace));
	}
}).on('close', function(){
	process.exit(0);
});

//Initialize.
startResponse(TAME.initialize(tamectx, trace));

// start loop.
if (!stop)
	rl.prompt();


