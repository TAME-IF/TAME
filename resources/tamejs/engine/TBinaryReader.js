/*******************************************************************************
 * Copyright (c) 2016-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var Util = Util || ((typeof require) !== 'undefined' ? require('../Util.js') : null);
var TAMEError = TAMEError || ((typeof require) !== 'undefined' ? require('../TAMEError.js') : null);
var TDataReader = TDataReader || ((typeof require) !== 'undefined' ? require('../TAMEError.js') : null);
// ======================================================================================================

//[[EXPORTJS-START

/****************************************************
 Factory for reading modules from binary data.
 ****************************************************/

var TAMEBinaryReader = {};

/**
 * Reads a version 1 module (actions, elements).
 * @param moduleOut (object) the output object to add parsed elements from {actions, elements}.
 * @return (object) an object containing {header, actions, elements} for use in TAME.createModule().
 * @throws TAMEError on a read error.
 */
TAMEBinaryReader.readModuleV1 = function(moduleOut)
{
	
};

/**
 * Reads a module header from a binary data reader.
 * Assumes the FOURCC ("TAME") was read already.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (object) an object containing the header keys mapped to values.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TAMEBinaryReader.readModuleHeader = function(dataReader)
{
	let out = {};
	
	
	
	return out;
};

/**
 * Reads a module from its serialized representation in base64 data.
 * @param base64Data (string) the input base64 data to read.
 * @return (object) an object containing {header, actions, elements} for use in TAME.createModule().
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TAMEBinaryReader.readModule = function(base64Data)
{
	let reader = new TDataReader(Util.base64ToDataView(b), true);
	
	reader.
	
	let out = {};
	out.header = TAMEBinaryReader.readModuleHeader(reader);
	
	// TODO: Finish.
	
	return out;
};


//[[EXPORTJS-END

// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAMEReader;
// =========================================================================
