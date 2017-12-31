/*******************************************************************************
 * Copyright (c) 2015-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package net.mtrop.tame.struct;

import java.io.PrintStream;

import com.tameif.tame.lang.Value;

/**
 * Test lists.
 * @author Matthew Tropiano
 */
public final class ListValueTest 
{

	static final Value[] TEST_VALUES = 
	{
		Value.create(false),
		Value.create(true),
		Value.create(Double.POSITIVE_INFINITY),
		Value.create(Double.NEGATIVE_INFINITY),
		Value.create(Double.NaN),
		Value.create(0),
		Value.create(0.0),
		Value.create(10),
		Value.create(3),
		Value.create(10.0),
		Value.create(3.0),
		Value.create(10.5),
		Value.create(3.5),
		Value.create(-10),
		Value.create(-3),
		Value.create(-10.0),
		Value.create(-3.0),
		Value.create(-10.5),
		Value.create(-3.5),
		Value.create(""),
		Value.create("0"),
		Value.create("0.0"),
		Value.create("10"),
		Value.create("3"),
		Value.create("10.0"),
		Value.create("3.0"),
		Value.create("10.5"),
		Value.create("3.5"),
		Value.create("-10"),
		Value.create("-3"),
		Value.create("-10.0"),
		Value.create("-3.0"),
		Value.create("-10.5"),
		Value.create("-3.5"),
		Value.create("apple"),
		Value.create("banana"),
	};
	
	public static void main(String[] args) 
	{
		PrintStream out = System.out;
	
		Value list = Value.createEmptyList();
		for (Value v : TEST_VALUES)
			list.listAdd(v);

		out.println(list.asString());

		for (Value v : TEST_VALUES)
			list.listRemove(v);
		
		out.println(list.asString());
		
		for (Value v : TEST_VALUES)
			list.listAddAt(0, v);
		
		out.println(list.asString());

		while (!list.isEmpty())
			list.listRemoveAt(0);
		
		out.println(list.asString());
		
		list.listAdd(Value.create(false));

		for (Value v : TEST_VALUES)
			list.listSet(0, v);
		
		out.println(list.asString());

		list = Value.createEmptyList();
		for (Value v : TEST_VALUES)
			list.listAdd(v);

		out.println(list.asString());
		
		for (Value v : TEST_VALUES)
			out.println(list.listIndexOf(v));

		out.println(list.asString());
		out.println(list.length());
	}
}
