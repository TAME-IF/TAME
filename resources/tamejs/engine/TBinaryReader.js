/*******************************************************************************
 * Copyright (c) 2016-2019 Matt Tropiano
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
var TModule = TModule || ((typeof require) !== 'undefined' ? require('./objects/TModule.js') : null);
// ======================================================================================================

//[[EXPORTJS-START

//[[EXPORTJS-INCLUDE TDataReader.js

/****************************************************
 Factory for reading modules from binary data.
 ****************************************************/

var TBinaryReader = {};

// Block entry types.
TBinaryReader.BlockEntryType = [
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

/**
 * Reads a set of ASCII characters from a data reader.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @param characters (Number:int) the amount of characters to read. 
 * @return (string) a JS string.
 * @throws RangeError on a read error (incomplete data).
 */
TBinaryReader.readASCII = function(dataReader, characters)
{
	let out = '';
	while (characters--)
		out += dataReader.readASCIIChar();
	return out;
};


/**
 * Reads a UTF-8 encoded string from a set of bytes assumed to be a UTF-8 string.
 * @param byteArray (Array) an array of 8-bit unsigned bytes.
 * @return (string) a JS string.
 * @throws Error on a read error (incomplete/bad data).
 */
TBinaryReader.utf8BytesToString = function(byteArray)
{
	let out = '';
	let index = 0;
	while (index < byteArray.length)
	{
		let c1 = byteArray[index++];
		switch (c1 >> 4)
		{
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
				out += String.fromCodePoint(c1);
				continue;
			
			case 12:
			case 13:
			{
				let c2 = byteArray[index++];
				out += String.fromCodePoint(((c1 & 0x1f) << 6) | (c2 & 0x3f));
				continue;
			}
			
			case 14:
			{
				let c2 = byteArray[index++];
				let c3 = byteArray[index++];
				out += String.fromCodePoint(((c1 & 0x0f) << 12) | ((c2 & 0x3f) << 6) | ((c3 & 0x3f) << 0));
				continue;
			}
			
			case 15:
			{
				let c2 = byteArray[index++];
				let c3 = byteArray[index++];
				let c4 = byteArray[index++];
				out += String.fromCodePoint(((c1 & 0x07) << 18) | ((c2 & 0x3f) << 12) | ((c3 & 0x3f) << 6) | (c4 & 0x3f));
				continue;
			}
			
			default:
				throw new Error("Bad UTF-8 character encoding in ["+byteArray.toString()+"]");
		}
	}
	
	return out;
};

/**
 * Reads a UTF-8 encoded string from a data reader.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (string) a JS string.
 * @throws RangeError on a read error (incomplete data).
 */
TBinaryReader.readUTF8String = function(dataReader)
{
	// length is in bytes, not chars!
	let len = dataReader.readInt32();
	let bytes = dataReader.readBytes(len);
	return TBinaryReader.utf8BytesToString(bytes);
};

/**
 * Reads a variable-length encoded 32-bit integer.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (Number:int) an integer.
 * @throws RangeError on a read error (incomplete data).
 */
TBinaryReader.readVariableLengthInt = function(dataReader)
{
	let out = 0;
	let b = 0;
	do {
		b = dataReader.readUInt8();
		out = out | (b & 0x7f);
		if ((b & 0x80) !== 0)
			out = out << 7;
	} while ((b & 0x80) !== 0);
	return out;
};

/**
 * Reads a string array from a data reader (strings are UTF-8 encoded).
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (object) an object that maps strings to strings.
 * @throws RangeError on a read error (incomplete data).
 */
TBinaryReader.readUTF8StringArray = function(dataReader)
{
	let out = [];
	let size = dataReader.readInt32();
	while (size--)
		out.push(TBinaryReader.readUTF8String(dataReader));
	return out;
};

/**
 * Reads a string map from a data reader (strings are UTF-8 encoded).
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (object) an object that maps strings to strings.
 * @throws RangeError on a read error (incomplete data).
 */
TBinaryReader.readUTF8StringMap = function(dataReader)
{
	let out = {};
	let size = dataReader.readInt32();
	while (size--)
		out[TBinaryReader.readUTF8String(dataReader)] = TBinaryReader.readUTF8String(dataReader);
	return out;
};

/**
 * Reads a TAME value from a data reader.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (object) an object that represents a TValue.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TBinaryReader.readValue = function(dataReader)
{
	let type = dataReader.readUInt8();
	switch (type)
	{
		case 0:
			return TValue.createBoolean(dataReader.readBoolean());
		case 1:
			return TValue.createInteger(dataReader.readInt64());
		case 2:
			return TValue.createFloat(dataReader.readFloat64());
		case 3:
			return TValue.createString(TBinaryReader.readUTF8String(dataReader));
		case 4:
			let list = TValue.createList([]);
			let size = dataReader.readInt32();
			while (size--)
				TValue.listAdd(list, TBinaryReader.readValue(dataReader));
			return list;
		case 5:
			return TValue.createObject(TBinaryReader.readUTF8String(dataReader));
		case 6:
			return TValue.createContainer(TBinaryReader.readUTF8String(dataReader));
		case 7:
			return TValue.createPlayer(TBinaryReader.readUTF8String(dataReader));
		case 8:
			return TValue.createRoom(TBinaryReader.readUTF8String(dataReader));
		case 9:
			return TValue.createWorld(TBinaryReader.readUTF8String(dataReader));
		case 10:
			return TValue.createAction(TBinaryReader.readUTF8String(dataReader));
		case 11:
			return TValue.createVariable(TBinaryReader.readUTF8String(dataReader));
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
TBinaryReader.readOperation = function(dataReader)
{
	let out = {};
	out.opcode = TBinaryReader.readVariableLengthInt(dataReader);

	let bits = dataReader.readUInt8();
	if ((bits & 0x01) !== 0)
		out.operand0 = TBinaryReader.readValue(dataReader);
	if ((bits & 0x02) !== 0)
		out.operand1 = TBinaryReader.readValue(dataReader);

	if ((bits & 0x04) !== 0)
		out.initBlock = TBinaryReader.readBlock(dataReader);
	if ((bits & 0x08) !== 0)
		out.conditionalBlock = TBinaryReader.readBlock(dataReader);
	if ((bits & 0x10) !== 0)
		out.stepBlock = TBinaryReader.readBlock(dataReader);
	if ((bits & 0x20) !== 0)
		out.successBlock = TBinaryReader.readBlock(dataReader);
	if ((bits & 0x40) !== 0)
		out.failureBlock = TBinaryReader.readBlock(dataReader);
	
	return out;
};

/**
 * Reads a block.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (Array) a deserialized block of operations.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TBinaryReader.readBlock = function(dataReader)
{
	let out = [];
	let size = dataReader.readInt32();
	while (size--)
		out.push(TBinaryReader.readOperation(dataReader));
	return out;
};

/**
 * Reads a block table entry key.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (string) a deserialized block entry key for a block table.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TBinaryReader.readBlockEntryKey = function(dataReader)
{
	let out = '';
	out += TBinaryReader.BlockEntryType[dataReader.readUInt8()];
	let size = dataReader.readInt32();
	out += '(';
	while (size--)
	{
		out += TValue.toString(TBinaryReader.readValue(dataReader));
		if (size > 0)
			out += ',';
	}
	out += ')';
	return out;
};

/**
 * Reads a block table.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (object) a deserialized block table.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TBinaryReader.readBlockTable = function(dataReader)
{
	let out = {};
	let size = dataReader.readInt32();
	while (size--)
	{
		let key = TBinaryReader.readBlockEntryKey(dataReader);
		let block = TBinaryReader.readBlock(dataReader);
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
TBinaryReader.readFunctionEntry = function(dataReader)
{
	let out = {};
	let argumentCount = dataReader.readInt32();
	out.arguments = [];
	while (argumentCount--)
		out.arguments.push(TBinaryReader.readUTF8String(dataReader));
	out.block = TBinaryReader.readBlock(dataReader);
	return out;
};

/**
 * Reads a function table.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (object) a deserialized function table.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TBinaryReader.readFunctionTable = function(dataReader)
{
	let out = {};
	let size = dataReader.readInt32();
	while (size--)
	{
		let name = TBinaryReader.readUTF8String(dataReader).toLowerCase();
		let functionEntry = TBinaryReader.readFunctionEntry(dataReader);
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
TBinaryReader.readElement = function(dataReader, element)
{
	element.identity = TBinaryReader.readUTF8String(dataReader);
	element.archetype = dataReader.readBoolean();
	element.blockTable = TBinaryReader.readBlockTable(dataReader);
	element.functionTable = TBinaryReader.readFunctionTable(dataReader);
};

/**
 * Reads a container element.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (object) a deserialized world element.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TBinaryReader.readContainer = function(dataReader)
{
	let out = {};
	out.tameType = "TContainer";
	TBinaryReader.readElement(dataReader, out);
	return out;
};

/**
 * Reads a object element.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (object) a deserialized world element.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TBinaryReader.readObject = function(dataReader)
{
	let out = {};
	out.tameType = "TObject";
	TBinaryReader.readElement(dataReader, out);
	out.names = TBinaryReader.readUTF8StringArray(dataReader);
	out.determiners = TBinaryReader.readUTF8StringArray(dataReader);
	out.tags = TBinaryReader.readUTF8StringArray(dataReader);
	return out;
};

/**
 * Reads a room element.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (object) a deserialized world element.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TBinaryReader.readRoom = function(dataReader)
{
	let out = {};
	out.tameType = "TRoom";
	TBinaryReader.readElement(dataReader, out);
	return out;
};

/**
 * Reads a player element.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (object) a deserialized world element.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TBinaryReader.readPlayer = function(dataReader)
{
	let out = {};
	out.tameType = "TPlayer";
	TBinaryReader.readElement(dataReader, out);
	return out;
};

/**
 * Reads a world element.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (object) a deserialized world element.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TBinaryReader.readWorld = function(dataReader)
{
	let out = {};
	out.tameType = "TWorld";
	TBinaryReader.readElement(dataReader, out);
	return out;
};

/**
 * Reads a world element.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (object) a deserialized world element.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TBinaryReader.readAction = function(dataReader)
{
	let out = {};
	out.tameType = "TAction";
	out.identity = TBinaryReader.readUTF8String(dataReader);
	out.type = dataReader.readUInt8();

	let bits = dataReader.readUInt8();
	out.strict = ((bits & 0x01) !== 0);
	out.reversed = ((bits & 0x02) !== 0);

	out.names = TBinaryReader.readUTF8StringArray(dataReader);
	out.extraStrings = TBinaryReader.readUTF8StringArray(dataReader);
	return out;
};

/**
 * Reads a version 1 module (digest, actions, elements).
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @param moduleOut (object) the output object to add parsed elements from {actions, elements}.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TBinaryReader.readModuleV1 = function(dataReader, moduleOut)
{
	moduleOut.digest = dataReader.readBytes(20);
	let bytes = dataReader.readInt32();
	let reader = dataReader.split(bytes);
	
	let actions = [];
	let elements = [];
	let elementMap = {};
	
	let world = TBinaryReader.readWorld(dataReader);
	elements[world.identity] = world;
	
	let READELEMENTS = function(f)
	{
		let count = dataReader.readInt32();
		while (count--) 
		{
			let e = f(dataReader);
			elementMap[e.identity] = e;
			elements.push(e);
		}
	};

	let READPARENTS = function()
	{
		let map = TBinaryReader.readUTF8StringMap(dataReader);
		Util.each(map, (value, key) => {elementMap[key].parent = value;});
	};

	count = dataReader.readInt32();
	while (count--)
		actions.push(TBinaryReader.readAction(dataReader));
	
	READELEMENTS(TBinaryReader.readPlayer);
	READELEMENTS(TBinaryReader.readRoom);
	READELEMENTS(TBinaryReader.readObject);
	READELEMENTS(TBinaryReader.readContainer);
	
	// Hierarchy arranged as players, rooms, objects, containers in binary - unnecessary to separate in JS.  
	READPARENTS();
	READPARENTS();
	READPARENTS();
	READPARENTS();
	
	moduleOut.actions = actions;
	moduleOut.elements = elements;
};

/**
 * Reads a module header from a binary data reader.
 * @param dataReader (TDataReader) the data reader already positioned for reading.
 * @return (object) an object containing the header keys mapped to values.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TBinaryReader.readModuleHeader = function(dataReader)
{
	let out = {};
	let size = dataReader.readInt32();
	while (size--)
	{
		let key = TBinaryReader.readUTF8String(dataReader);
		let value = TBinaryReader.readUTF8String(dataReader);
		out[key] = value;
	}
	return out;
};

/**
 * Reads a module from its serialized representation.
 * @param base64Data (DataView) the input data view to use for reading.
 * @return (TModule) a deserialized TAME Module.
 * @throws TAMEError on a read error, RangeError on a read error (incomplete data).
 */
TBinaryReader.readModule = function(dataView)
{
	let reader = new TDataReader(dataView, true);
	
	if (TBinaryReader.readASCII(reader, 4) != 'TAME')
		throw TAMEError.Module("Not a TAME Module.");
	
	let out = {};
	out.header = TBinaryReader.readModuleHeader(reader);
	
	let version = reader.readUInt8();
	if (version === 1)
		TBinaryReader.readModuleV1(reader, out);
	else
		throw TAMEError.Module("Module does not have a recognized version.");
	
	return new TModule(out.header, out.digest, out.actions, out.elements);
};


//[[EXPORTJS-END

// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TBinaryReader;
// =========================================================================
