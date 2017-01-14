/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
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

//##[[EXPORTJS-START

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

//##[[EXPORTJS-END


// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = Util;
// =========================================================================
