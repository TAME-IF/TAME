/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

//##[[CONTENT-START

/*****************************************************************************
 Utilities
 *****************************************************************************/
var Util = {

	// Smarter foreach.
	"each": function(obj, func)
	{
		for (x in obj) 
			if (obj.hasOwnProperty(x)) 
				func(obj[x], x, obj.length);
	},

	// Array remove
	"arrayRemove": function(arr, obj)
	{
		for (var i = 0; i < arr.length; i++) 
			if (arr[i] == obj)
			{
				arr.splice(i, 1);
				return true;
			}
		
		return false;
	},

	// Mapify - [object, ...] to {object.memberKey -> object, ...}
	"mapify": function(objlist, memberKey, multi) 
	{
		var out = {}; 
		for (x in objlist) 
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
	},
	
	// Pairify - [object, ...] to {object.memberKey -> object.memberValue, ...}
	"pairify": function(objlist, memberKey, memberValue, multi) 
	{
		var out = {}; 
		for (x in objlist) 
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
	},
	
	// replaceall - Return a string that replaces all matching patterns in inputstr with replacement
	"replaceAll": function(inputstr, expression, replacement) 
	{
		return inputstr.replace(new RegExp(expression, 'g'), replacement);
	},

};

//##[[CONTENT-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = Util;
// =========================================================================
