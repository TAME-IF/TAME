/*******************************************************************************
 * Copyright (c) 2016-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

// If testing with NODEJS ==================================================
var window = null;
// =========================================================================

//[[EXPORTJS-START

/*****************************************************************************
 Utilities
 *****************************************************************************/
var Util = {};

// Nanosecond time (for timing stuff). Resolution varies by environment.
// Stub here.
Util.nanoTime = null;

// Smarter foreach.
Util.each = function(obj, func)
{
	for (let x in obj) 
		if (obj.hasOwnProperty(x)) 
			func(obj[x], x, obj.length);
};

// String format ({Number}) 
Util.format = function( /* str, args ... */ )
{
	if (!arguments.length)
		return null;
	
	let str = arguments[0];
	let args = Array.prototype.slice.call(arguments, 1);
	
    for (let key in args) 
        str = str.replace(new RegExp("\\{" + key + "\\}", "gi"), args[key]);
    
    return str;
};

// String compare.
// Adapted from Java - java.lang.String.compareTo(String).
Util.strcmp = function(s1, s2)
{
    let len1 = s1.length;
    let len2 = s2.length;
    let lim = Math.min(len1, len2);
    let k = 0;
    while (k < lim)
    {
        let c1 = s1.charCodeAt(k);
        let c2 = s2.charCodeAt(k);
        if (c1 != c2) 
            return c1 - c2;
        k++;
    }
    return len1 - len2;
};

// Array remove
Util.arrayRemove = function(arr, obj)
{
	for (let i = 0; i < arr.length; i++) 
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
	let arr = hash[identity];
	if (!arr)
		arr = hash[identity] = {};
	if (!arr[str])
		arr[str] = true;
};

// Remove a string from a lookup hash.
Util.objectStringRemove = function(hash, identity, str)
{
	let arr = hash[identity];
	if (!arr)
		return;
	if (arr[str])
		delete arr[str];
};

// Checks if a string is in a lookup hash.
// True if contained, false if not.
Util.objectStringContains = function(hash, identity, str)
{
	let arr = hash[identity];
	return (arr && arr[str]);
};

// isArray - tests if an object is an array.
Util.isArray = function(obj)
{
	return Object.prototype.toString.call(obj) === '[object Array]';
};

//isObject - tests if an object is an object.
Util.isObject = function(obj)
{
	return Object.prototype.toString.call(obj) === '[object Object]';
};

//isObject - tests if an object is an object.
Util.isFunction = function(obj)
{
	return Object.prototype.toString.call(obj) === '[object Function]';
};

// Mapify - [object, ...] to {object.memberKey -> object, ...}
Util.mapify = function(objlist, memberKey, multi) 
{
	let out = {}; 
	for (let x in objlist) 
		if (objlist.hasOwnProperty(x))
		{				
			let chain = out[objlist[x][memberKey]];
			if (multi && chain)
			{
				if (Util.isArray(chain))
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
	let out = {}; 
	for (let x in objlist) 
		if (objlist.hasOwnProperty(x))
		{				
			let chain = out[objlist[x][memberKey]];
			if (multi && chain)
			{
				if (Util.isArray(chain))
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
	let t = JSON.stringify(text);
	return t.substring(1, t.length - 1);
};

// formatDate - Return a string that is a formatted date. Similar to SimpleDateFormat in Java.
Util.formatDate = function(date, formatstring) 
{
	// Enumerations and stuff.
	let DEFAULT_LOCALE = 
	{
		"dayInWeek": [
			['S', 'M', 'T', 'W', 'T', 'F', 'S'],
			['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'],
			['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'],
			['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday']
		],
		"monthInYear": [
			['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'], // MMM
			['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'] // MMMM
		]
	};

	let _PAD = function(value, len)
	{
		value = parseInt(value, 10);
		let out = '';
		do {
			out = value % 10 + out;
			value = Math.floor(value / 10);
			len--;
		} while (value > 0);

		while (len-- > 0)
			out = '0' + out;

		return out;
	};

	// The regular expression for finding all pertinent tokens.
	let _DATEFORMATREGEX = /e+|E+|y+|M+|d+|w+|W+|a+|A+|h+|H+|k+|K+|m+|s+|S+|z+|Z+|X+|'.*'/g;

	/* Mapping of token types to value function. All return strings. */
	// TODO: Finish this.
	let _TOKENFUNCS =
	{
		"e": function(locale, token, date)
		{
			let val = date.getFullYear();
			if (val < 0)
				return token.length < 2 ? "b" : "bc";
			else
				return token.length < 2 ? "a" : "ad";
		},
		"E": function(locale, token, date)
		{
			let val = date.getFullYear();
			if (val < 0)
				return token.length < 2 ? "B" : "BC";
			else
				return token.length < 2 ? "A" : "AD";
		},
		"y": function(locale, token, date)
		{
			return _PAD(date.getFullYear(), token.length);
		},
		"M": function(locale, token, date)
		{
			if (token.length <= 2)
				return _PAD(date.getMonth() + 1, token.length);
			else
			{
				let month = date.getMonth();
				let index = Math.min(Math.max(token.length - 3, 0), 1);
				return locale.monthInYear[index][month];
			}
		},
		"d": function(locale, token, date)
		{
			return _PAD(date.getDate(), token.length);
		},
		"W": function(locale, token, date)
		{
			let index = Math.min(Math.max(token.length - 1, 0), 3);
			let day = date.getDay();
			return locale.dayInWeek[index][day];
		},
		"w": function(locale, token, date)
		{
			return _PAD(date.getDay(), token.length);
		},
		"a": function(locale, token, date)
		{
			let val = date.getHours();
			if (val < 12)
				return token.length < 2 ? "a" : "am";
			else
				return token.length < 2 ? "p" : "pm";
		},
		"A": function(locale, token, date)
		{
			let val = date.getHours();
			if (val < 12)
				return token.length < 2 ? "A" : "AM";
			else
				return token.length < 2 ? "P" : "PM";
		},
		"h": function(locale, token, date)
		{
			return _PAD(date.getHours(), token.length);
		},
		"H": function(locale, token, date)
		{
			return _PAD(date.getHours() + 1, token.length);
		},
		"k": function(locale, token, date)
		{
			return _PAD(date.getHours() % 12, token.length);
		},
		"K": function(locale, token, date)
		{
			return _PAD((date.getHours() % 12) + 1, token.length);
		},
		"m": function(locale, token, date)
		{
			return _PAD(date.getMinutes(), token.length);
		},
		"s": function(locale, token, date)
		{
			return _PAD(date.getSeconds(), token.length);
		},
		"S": function(locale, token, date)
		{
			return _PAD(date.getMilliseconds(), token.length);
		},
		"z": function(locale, token, date)
		{
			let minuteOffset = date.getTimezoneOffset();
			let absMinuteOffset = Math.abs(date.getTimezoneOffset());
			return "GMT" + (minuteOffset > 0 ? "-" : "+") + _PAD(absMinuteOffset / 60, 2) + ":" + _PAD(absMinuteOffset % 60, 2);
		},
		"Z": function(locale, token, date)
		{
			let minuteOffset = date.getTimezoneOffset();
			let absMinuteOffset = Math.abs(date.getTimezoneOffset());
			return (minuteOffset > 0 ? "-" : "+") + _PAD(absMinuteOffset / 60, 2) + _PAD(absMinuteOffset % 60, 2);
		},
		"X": function(locale, token, date)
		{
			let minuteOffset = date.getTimezoneOffset();
			let absMinuteOffset = Math.abs(date.getTimezoneOffset());
			switch (token.length)
			{
				case 1:
					return (minuteOffset > 0 ? "-" : "+") + (absMinuteOffset / 60);
				case 2:
					return (minuteOffset > 0 ? "-" : "+") + _PAD(absMinuteOffset / 60, 2);
				case 3:
					return (minuteOffset > 0 ? "-" : "+") + _PAD(absMinuteOffset / 60, 2) + _PAD(absMinuteOffset % 60, 2);
				case 4:
					return (minuteOffset > 0 ? "-" : "+") + _PAD(absMinuteOffset / 60, 2) + ":" + _PAD(absMinuteOffset % 60, 2);
				default:
					return "GMT" + (minuteOffset > 0 ? "-" : "+") + _PAD(absMinuteOffset / 60, 2) + ":" + _PAD(absMinuteOffset % 60, 2);
			}
		},
		"'": function(locale, token, date)
		{
			if (token.length == 2)
				return "'";
			else
				return token.substring(1, token.length - 1);
		},
	};

	date = new Date(date);
	
	let lastEnd = 0;
	let sb = '';
	let match;
	while (match = _DATEFORMATREGEX.exec(formatstring)) // Intentional assignment.
	{
		let token = match[0];
		let start = match.index;
		if (start > lastEnd)
			sb += formatstring.substring(lastEnd, start);
		
		sb += _TOKENFUNCS[token[0]](DEFAULT_LOCALE, token, date);
		
		lastEnd = _DATEFORMATREGEX.lastIndex;
	}
	
	if (lastEnd < formatstring.length)
		sb += formatstring.substring(lastEnd);

	return sb;
};

/**
 * Assists in parsing a cue with formatted text (TEXTF cue), or one known to have formatted text.
 * The target functions passed in are provided an accumulator array to push generated text into. 
 * On return, this function returns the accumulator's contents joined into a string. 
 * @param sequence the character sequence to parse.
 * @param tagStartFunc the function called on tag start. arguments: tagName (string), accumulator (Array)  
 * @param tagEndFunc the function called on tag end. arguments: tagName (string), accumulator (Array)
 * @param textFunc the function called on tag contents. arguments: text (string), accumulator (Array)
 * @return the full accumulated result.  
 */
Util.parseFormatted = function(sequence, tagStartFunc, tagEndFunc, textFunc)
{
	let builder = '';
	let tagStack = [];
	let accumulator = [];
	
	let emitText = function()
	{
		if (builder.length === 0)
			return;
		
		let accum = [];
		textFunc(builder, accum);
		accumulator.push.apply(accumulator, accum);
		builder = '';
	};

	let emitTag = function()
	{
		if (builder.length === 0)
			return;

		let tag = builder;
		builder = '';
		
		if (tag == '/')
		{
			if (tagStack.length === 0)
				return;
			let accum = [];
			tagEndFunc(tagStack.pop(), accum);
			accumulator.push.apply(accumulator, accum);
		}
		else
		{
			tagStack.push(tag);
			let accum = [];
			tagStartFunc(tag, accum);
			accumulator.push.apply(accumulator, accum);
		}
	};
	
	let STATE_TEXT = 0;
	let STATE_TAG_MAYBE = 1;
	let STATE_TAG = 2;
	let STATE_TAG_END_MAYBE = 3;
	
	let state = STATE_TEXT;
	let len = sequence.length, i = 0;

	while (i < len)
	{
		let c = sequence.charAt(i);

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
	{
		let accum = [];
		tagEndFunc(tagStack.pop(), accum);
		accumulator.push.apply(accumulator, accum);
	}
	
	return accumulator.join('');
};


//[[EXPORTJS-END


// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = Util;
// =========================================================================
