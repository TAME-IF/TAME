/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

//##[[EXPORTJS-START

/* Class */ function FormatParser(tagStartFunc, tagEndFunc, textFunc)
{
	this.builder = '';
	this.tagStack = '';
	this.tagStartFunc = tagStartFunc; 
	this.tagEndFunc = tagEndFunc; 
	this.textFunc = textFunc;
}

FormatParser.prototype.parse = function(sequence)
{
	var self = this;
	
	function emitText()
	{
		if (self.builder.length == 0)
			return;
		
		self.textFunc(self.builder);
		self.builder = '';
	}

	function emitTag()
	{
		if (self.builder.length == 0)
			return;

		var tag = self.builder;
		self.builder = '';
		
		if (tag == '/')
		{
			if (self.tagStack.length == 0)
				return;
			self.tagEndFunc(self.tagStack.pop());
		}
		else
		{
			self.tagStack.push(tag);
			self.tagStartFunc(tag);
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
					self.builder += (c);
			}
			break;

			case STATE_TAG_MAYBE:
			{
				if (c == '[')
				{
					state = STATE_TEXT;
					self.builder += (c);
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
					self.builder += (c);
			}
			break;
			
			case STATE_TAG_END_MAYBE:
			{
				if (c == ']')
				{
					state = STATE_TAG;
					self.builder += (c);
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
	while (self.tagStack.length > 0)
		self.tagEndFunc(self.tagStack.pop());
};

// Need a better don't-eat-CPU-solution.
function sleep(sleepDuration)
{
    var now = Date.now();
    while(Date.now() < now + sleepDuration){ /* do nothing */ } 
}

//##[[EXPORTJS-END
