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

import java.io.PrintStream;

import com.blackrook.commons.Common;

import net.mtrop.tame.lang.Value;

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

		Common.noop();

		for (Value v : TEST_VALUES)
			list.listRemove(v);
		
		Common.noop();
		
		for (Value v : TEST_VALUES)
			list.listAddAt(0, v);
		
		Common.noop();

		while (!list.isEmpty())
			list.listRemoveAt(0);
		
		Common.noop();
		
		list.listAdd(Value.create(false));

		for (Value v : TEST_VALUES)
			list.listSet(0, v);
		
		Common.noop();

		list = Value.createEmptyList();
		for (Value v : TEST_VALUES)
			list.listAdd(v);

		Common.noop();
		
		for (Value v : TEST_VALUES)
			out.println(list.listIndexOf(v));

		Common.noop();
		
		out.println(list.length());
		out.println(list.asString());
	}
}