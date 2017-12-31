/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package net.mtrop.tame.struct;

import net.mtrop.tame.lang.FormatParser;

/**
 * What it says on the box.
 * @author Matthew Tropiano
 */
public final class TextFormattingParserTest
{
	public static void main(String[] args) 
	{
		FormatParser parser = new FormatParser() 
		{
			@Override
			public void startTag(String tagName)
			{
				System.out.println("[START] " + tagName);
			}
			
			@Override
			public void sendText(String text) 
			{
				System.out.println("[TEXT] " + text);
			}
			
			@Override
			public void endTag(String tagName) 
			{
				System.out.println("[END] " + tagName);
			}
		};
		
		System.out.println("--------------");
		parser.parse("hello");
		System.out.println("--------------");
		parser.parse("[matt]asdfasdf[/]");
		System.out.println("--------------");
		parser.parse("fasdfasdf [matt]asdfasdf[/] asdfasdfasdfsdfffs");
		System.out.println("--------------");
		parser.parse("[1][2][3]asdfasdfasdf[/]asdfasdf[/]asdfsdf[/][4][/]asdfasdf[5]asdf[/]wertrert");
		
	}
	
}
