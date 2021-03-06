/*******************************************************************************
 * Copyright (c) 2016-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

//[[EXPORTJS-START

function print(text) 
{
	if (text)
		process.stdout.write(text);
}

function println(text) 
{
	if (!text)
		process.stdout.write('\n');
	else
		process.stdout.write(text + '\n');
}

function withEscChars(text) 
{
	let t = JSON.stringify(text);
	return t.substring(1, t.length - 1);
}


const RegexWhitespace = /\s/g;

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
	
	let token = '';
	let line = '';
	let ln = startColumn;
	let tok = 0;
	
	for (let i = 0; i < message.length; i++)
	{
		let c = message.charAt(i);
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
function sleep(sleepDuration) 
{
	let now = Date.now();
	while (Date.now() < now + sleepDuration) {}
}

function base64ToDataView(base64)
{
	let buffer = Buffer.from(base64, 'base64');
	let out = new DataView(new ArrayBuffer(buffer.length));
	let i = 0;
	for (i = 0; i < buffer.length; i++)
		out.setUint8(i, buffer.readUInt8(i));
	return out;
}

//[[EXPORTJS-END
