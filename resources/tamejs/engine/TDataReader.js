/*******************************************************************************
 * Copyright (c) 2016-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

//[[EXPORTJS-START

/**
 * Constructor for TDataReader.
 * This is a sequential reader for array buffers.
 * @param dataView (DataView) the Data View to read from.
 */
var TDataReader = function(dataView, littleEndian)
{
	this.dataView = dataView;
	this.littleEndian = littleEndian;
	this.pos = 0;
};

/**
 * Reads a signed 8-bit int.
 * Advances 1 byte.
 */
TDataReader.prototype.readInt8 = function()
{
	let out = this.dataView.getInt8(this.pos);
	this.pos = this.pos + 1;
	return out;
};

/**
 * Reads an unsigned 8-bit int.
 * Advances 1 byte.
 */
TDataReader.prototype.readUInt8 = function()
{
	let out = this.dataView.getUint8(this.pos);
	this.pos = this.pos + 1;
	return out;
};

/**
 * Reads a signed 16-bit int.
 * Advances 2 bytes.
 */
TDataReader.prototype.readInt16 = function()
{
	let out = this.dataView.getInt16(this.pos, this.littleEndian);
	this.pos = this.pos + 2;
	return out;
};

/**
 * Reads an unsigned 16-bit int.
 * Advances 2 bytes.
 */
TDataReader.prototype.readUInt16 = function()
{
	let out = this.dataView.getUint16(this.pos, this.littleEndian);
	this.pos = this.pos + 2;
	return out;
};

/**
 * Reads a signed 32-bit int.
 * Advances 4 bytes.
 */
TDataReader.prototype.readInt32 = function()
{
	let out = this.dataView.getInt32(this.pos, this.littleEndian);
	this.pos = this.pos + 4;
	return out;
};

/**
 * Reads an unsigned 32-bit int.
 * Advances 4 bytes.
 */
TDataReader.prototype.readUInt32 = function()
{
	let out = this.dataView.getUint32(this.pos, this.littleEndian);
	this.pos = this.pos + 4;
	return out;
};

/**
 * Reads a signed 64-bit int.
 * Advances 8 bytes.
 */
TDataReader.prototype.readInt64 = function()
{
	let left =  this.readUint32();
	let right = this.readUint32();
	return this.littleEndian ? left + (2**32*right) : (2**32*left) + right;
};

/**
 * Reads a 32-bit float.
 * Advances 4 bytes.
 */
TDataReader.prototype.readFloat32 = function()
{
	let out = this.dataView.getFloat32(this.pos);
	this.pos = this.pos + 4;
	return out;
};

/**
 * Reads a 64-bit float.
 * Advances 8 bytes.
 */
TDataReader.prototype.readFloat64 = function()
{
	let out = this.dataView.getFloat64(this.pos);
	this.pos = this.pos + 8;
	return out;
};

/**
 * Reads an ASCII character.
 * Advances 1 byte.
 * @return (string)
 */
TDataReader.prototype.readASCIIChar = function()
{
	let c = this.readUInt8();
	this.pos = this.pos + 1;
	return String.fromCharCode(c);
};

/**
 * Reads a UTF-8 character.
 * Advances the amount of bytes that it takes to read one character, since UTF-8 characters very in width.
 * @return (string)
 */
TDataReader.prototype.readUTF8Char = function()
{
	let c1 = this.readUInt8();
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
			return String.fromCharCode(c1);
		
		case 12:
		case 13:
		{
			let c2 = this.readUInt8();
			return String.fromCharCode(((c1 & 0x1f) << 6) | (c2 & 0x3f));
		}
		
		case 14:
		{
			let c2 = this.readUInt8();
			let c3 = this.readUInt8();
			return String.fromCharCode(((c1 & 0x0f) << 12) | ((c2 & 0x3f) << 6) | ((c3 & 0x3f) << 0));
		}
		
		case 15:
		{
			let c2 = this.readUInt8();
			let c3 = this.readUInt8();
			let c4 = this.readUInt8();
			return String.fromCodePoint(((c1 & 0x07) << 18) | ((c2 & 0x3f) << 12) | ((c3 & 0x3f) << 6) | (c4 & 0x3f));
		}
		
		default:
			throw new RangeError("Bad UTF-8 character encoding.");
	}
};

/**
 * Reads a set of bytes as an ASCII string.
 * Advances 'byteCount' bytes.
 * @return (string)
 */
TDataReader.prototype.readASCII = function(byteCount)
{
	let out = ""; 
	while (byteCount--)
		out += this.readASCIIChar();
	return out;
};

/**
 * Reads "charCount" UTF-8 characters.
 * Advances as many bytes as it takes to read "charCount" characters.
 * @return (string)
 */
TDataReader.prototype.readUTF8 = function(charCount)
{
	let out = ""; 
	while (charCount--)
		out += this.readUTF8Char();
	return out;
};

//[[EXPORTJS-END

//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TDataReader;
// =========================================================================
