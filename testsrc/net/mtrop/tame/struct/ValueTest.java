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

import net.mtrop.tame.lang.ArithmeticOperator;
import net.mtrop.tame.lang.Value;

/**
 * Test all value functions and output.
 * @author Matthew Tropiano
 */
public final class ValueTest 
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
		Value.create(" "),
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
		Value.createEmptyList(),
		Value.createList(Value.create(true), Value.create(3), Value.create(5.0), Value.create("orange")),
	};
	
	public static void main(String[] args) 
	{
		PrintStream out = System.out;
		
		for (int i = 0; i < TEST_VALUES.length; i++)
			printEmpty(out, TEST_VALUES[i]);
		out.println("-------------------------------");
		for (int i = 0; i < TEST_VALUES.length; i++)
			printLength(out, TEST_VALUES[i]);
		out.println("-------------------------------");
		for (int i = 0; i < TEST_VALUES.length; i++)
			printBoolean(out, TEST_VALUES[i]);
		out.println("-------------------------------");
		for (int i = 0; i < TEST_VALUES.length; i++)
			printInteger(out, TEST_VALUES[i]);
		out.println("-------------------------------");
		for (int i = 0; i < TEST_VALUES.length; i++)
			printFloat(out, TEST_VALUES[i]);
		out.println("-------------------------------");
		for (int i = 0; i < TEST_VALUES.length; i++)
			printString(out, TEST_VALUES[i]);
		out.println("-------------------------------");
		
		for (ArithmeticOperator op : ArithmeticOperator.VALUES)
		{
			if (op.isBinary())
			{
				for (int i = 0; i < TEST_VALUES.length; i++)
					for (int j = 0; j < TEST_VALUES.length; j++)
						print(out, op, TEST_VALUES[i], TEST_VALUES[j]);
			}
			else
			{
				for (int i = 0; i < TEST_VALUES.length; i++)
					print(out, op, TEST_VALUES[i]);
			}
			out.println("-------------------------------");
		}
			
	}
	
	private static void printEmpty(PrintStream out, Value v1)
	{
		out.println(v1 + " > EMPTY? > " +v1.isEmpty());
	}

	private static void printLength(PrintStream out, Value v1)
	{
		out.println(v1 + " > LENGTH > " +v1.length());
	}

	private static void printBoolean(PrintStream out, Value v1)
	{
		out.println(v1 + " > BOOLEAN > " +v1.asBoolean());
	}

	private static void printInteger(PrintStream out, Value v1)
	{
		out.println(v1 + " > INT > " +v1.asLong());
	}
	
	private static void printFloat(PrintStream out, Value v1)
	{
		out.println(v1 + " > FLOAT > " +v1.asDouble());
	}
	
	private static void printString(PrintStream out, Value v1)
	{
		out.println(v1 + " > STRING > \"" +v1.asString()+ "\"");
	}

	private static void print(PrintStream out, ArithmeticOperator op, Value v1)
	{
		out.println(op.getSymbol() +" "+ v1 + " = " + op.doOperation(v1));
	}

	private static void print(PrintStream out, ArithmeticOperator op, Value v1, Value v2)
	{
		out.println(v1 + " " + op.getSymbol() + " " + v2 + " = " + op.doOperation(v1, v2));
	}

}
