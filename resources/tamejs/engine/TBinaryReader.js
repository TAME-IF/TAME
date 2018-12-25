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
var Util = Util || ((typeof require) !== 'undefined' ? require('./Util.js') : null);
var TAMEError = TAMEError || ((typeof require) !== 'undefined' ? require('./TAMEError.js') : null);
var TDataReader = TDataReader || ((typeof require) !== 'undefined' ? require('./TDataReader.js') : null);
var TValue = TValue || ((typeof require) !== 'undefined' ? require('./objects/TValue.js') : null);
// ======================================================================================================

//[[EXPORTJS-START

//[[EXPORTJS-INCLUDE TDataReader.js

/****************************************************
 Factory for reading modules from binary data.
 ****************************************************/

var TAMEBinaryReader = {};

// Block entry types.
TAMEBinaryReader.BlockEntryType = [
	'INIT',
	'START',
	'AFTERSUCCESSFULCOMMAND',
	'AFTERFAILEDCOMMAND',
	'AFTEREVERYCOMMAND',
	'ONACTION',
	'ONMODALACTION',
	'ONACTIONWITH',
	'ONACTIONWITHANCESTOR',
	'ONACTIONWITHOTHER',
	'ONUNHANDLEDACTION',
	'ONUNKNOWNCOMMAND',
	'ONAMBIGUOUSCOMMAND',
	'ONINCOMPLETECOMMAND',
	'ONMALFORMEDCOMMAND',
	'ONELEMENTBROWSE',
	'ONWORLDBROWSE',
	'ONROOMBROWSE',
	'ONPLAYERBROWSE',
	'ONCONTAINERBROWSE'
];

// Value types.
TAMEBinaryReader.ValueType = [
	'BOOLEAN',
	'INTEGER',
	'FLOAT',
	'STRING',
	'LIST',
	'OBJECT',
	'CONTAINER',
	'PLAYER',
	'ROOM',
	'WORLD',
	'ACTION',
	'VARIABLE'
];

/**
 * Reads a UTF-8 encoded string from a data reader.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (string) a JS string.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TAMEBinaryReader.readUTF8String = function(dataReader)
{
	let out = '';
	let len = dataReader.readInt32();
	while (len--)
		out += dataReader.readUTF8Char();
	return out;
};

/**
 * Reads a variable-length encoded 32-bit integer.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (Number:int) an integer.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TAMEBinaryReader.readVariableLengthInt = function(dataReader)
{
	/*
	int out = 0;
	byte b = 0;
	do {
		b = readByte();
		out |= b & 0x7f;
		if ((b & 0x80) != 0)
			out <<= 7;
	} while ((b & 0x80) != 0);
	return out;
	 */
	
	// TODO: Finish this.
	
	return out;
};

/**
 * Reads a TAME value from a data reader.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (object) an object that represents a TValue.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TAMEBinaryReader.readValue = function(dataReader)
{
	let type = TAMEBinaryReader.ValueType[dataReader.readUInt8()];
	switch (type)
	{
		case 0:
			return TValue.createBoolean(dataReader.readBoolean());
		case 1:
			return TValue.createInteger(dataReader.readInt64());
		case 2:
			return TValue.createFloat(dataReader.readFloat64());
		case 3:
			return TValue.createString(TAMEBinaryReader.readUTF8String(dataReader));
		case 4:
			let list = TValue.createList([]);
			let size = dataReader.readInt32();
			while (size--)
				TValue.listAdd(list, TAMEBinaryReader.readValue(dataReader));
			return list;
		case 5:
			return TValue.createObject(TAMEBinaryReader.readUTF8String(dataReader));
		case 6:
			return TValue.createContainer(TAMEBinaryReader.readUTF8String(dataReader));
		case 7:
			return TValue.createPlayer(TAMEBinaryReader.readUTF8String(dataReader));
		case 8:
			return TValue.createRoom(TAMEBinaryReader.readUTF8String(dataReader));
		case 9:
			return TValue.createWorld(TAMEBinaryReader.readUTF8String(dataReader));
		case 10:
			return TValue.createAction(TAMEBinaryReader.readUTF8String(dataReader));
		case 11:
			return TValue.createVariable(TAMEBinaryReader.readUTF8String(dataReader));
		default:
			throw TAMEError.Module("Bad value type. Internal error!");
	}
};

/**
 * Reads an operation.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (object) a deserialized operation.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TAMEBinaryReader.readOperation = function(dataReader)
{
	let out = {};
	
	// TODO: Finish this.
	return out;
};

/**
 * Reads a block.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (Array) a deserialized block of operations.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TAMEBinaryReader.readBlock = function(dataReader)
{
	let out = [];
	let size = sr.readInt();
	while (size--)
		out.push(TAMEBinaryReader.readOperation(dataReader));
	return out;
};

/**
 * Reads a block table entry key.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (string) a deserialized block entry key for a block table.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TAMEBinaryReader.readBlockEntryKey = function(dataReader)
{
	let out = '';
	out += TAMEBinaryReader.BlockEntryType[dataReader.readUInt8()];
	let size = dataReader.readInt32();
	while (size--)
		out += TValue.toString(TAMEBinaryReader.readValue(dataReader));
	return out;
};

/**
 * Reads a block table.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (object) a deserialized block table.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TAMEBinaryReader.readBlockTable = function(dataReader)
{
	let out = {};
	let size = dataReader.readInt32();
	while (size--)
	{
		let key = TAMEBinaryReader.readBlockEntryKey(dataReader);
		let block = TAMEBinaryReader.readBlock(dataReader);
		out[key] = block;
	}
	return out;
};

/**
 * Reads a function entry.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (object) a deserialized function entry.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TAMEBinaryReader.readFunctionEntry = function(dataReader)
{
	let out = {};
	let argumentCount = dataReader.readInt32();
	out.arguments = [];
	while (argumentCount--)
		out.arguments.push(TAMEBinaryReader.readUTF8String(dataReader));
	out.block = TAMEBinaryReader.readBlock(dataReader);
	return out;
};

/**
 * Reads a function table.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (object) a deserialized function table.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TAMEBinaryReader.readFunctionTable = function(dataReader)
{
	let out = {};
	let size = dataReader.readInt32();
	while (size--)
	{
		let name = TAMEBinaryReader.readUTF8String(dataReader).toLowerCase();
		let functionEntry = TAMEBinaryReader.readFunctionEntry(dataReader);
		out[name] = functionEntry;
	}
	return out;
};

/**
 * Reads an element into an object.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @param element (object) the element object to read data into.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TAMEBinaryReader.readElement = function(dataReader, element)
{
	element.identity = TAMEBinaryReader.readUTF8String(dataReader);
	element.archetype = dataReader.readBoolean();
	element.blockTable = TAMEBinaryReader.readBlockTable(dataReader);
	element.functionTable = TAMEBinaryReader.readFunctionTable(dataReader);
};

/**
 * Reads a world element.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (object) a deserialized world element.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TAMEBinaryReader.readWorld = function(dataReader)
{
	let out = {};
	out.tameType = "TWorld";
	TAMEBinaryReader.readElement(dataReader, out);
	return out;
};

/**
 * Reads a version 1 module (digest, actions, elements).
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @param moduleOut (object) the output object to add parsed elements from {actions, elements}.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TAMEBinaryReader.readModuleV1 = function(dataReader, moduleOut)
{
	out.digest = dataReader.readBytes(20);
	let bytes = dataReader.readInt32();
	let reader = dataReader.split(bytes);
	
	let actions = {};
	let elements = {};
	
	let world = TAMEBinaryReader.readWorld(reader);
	elements[world.identity] = world;
	
	// TODO: Finish this.
	
	out.actions = actions;
	out.elements = elements;
};

/**
 * Reads a module header from a binary data reader.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (object) an object containing the header keys mapped to values.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TAMEBinaryReader.readModuleHeader = function(dataReader)
{
	let out = {};
	let size = dataReader.readInt32();
	while (size--)
	{
		let key = TAMEBinaryReader.readUTF8String(dataReader);
		let value = TAMEBinaryReader.readUTF8String(dataReader);
		out[key] = value;
	}
	return out;
};

/**
 * Reads a module from its serialized representation in base64 data.
 * @param base64Data (string) the input base64 data to read.
 * @return (TModule) a deserialized TAME Module.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TAMEBinaryReader.readModule = function(base64Data)
{
	let reader = new TDataReader(Util.base64ToDataView(b), true);
	
	if (reader.readASCII(4) != 'TAME')
		throw TAMEError.Module("Not a TAME Module.");
	
	let out = {};
	out.header = TAMEBinaryReader.readModuleHeader(reader);
	
	let version = reader.readUInt8();
	if (version === 1)
		TAMEBinaryReader.readModuleV1(out);
	else
		throw TAMEError.Module("Module does not have a recognized version.");
	
	return new TModule(out.header, out.digest, out.actions, out.elements);
};


//[[EXPORTJS-END

// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAMEBinaryReader;
// =========================================================================
