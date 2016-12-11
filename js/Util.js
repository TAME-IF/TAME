/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

//##[[CONTENT-START

/*****************************************************************************
 Exception handling.
 *****************************************************************************/
var Util = {};

//Smarter foreach.
Util.each = function(obj, func)
{
	for (x in obj) 
		if (obj.hasOwnProperty(x)) 
			func(obj[x], x, obj.length);
}

// Mapify - [object, ...] to {object.memberKey -> object, ...}
Util.mapify = function(objlist, memberKey, multi) 
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
}

// Pairify - [object, ...] to {object.memberKey -> object.memberValue, ...}
Util.pairify = function(objlist, memberKey, memberValue, multi) 
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
}

//##[[CONTENT-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = Util;
// =========================================================================
