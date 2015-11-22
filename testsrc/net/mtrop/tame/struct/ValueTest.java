package net.mtrop.tame.struct;

import java.io.PrintStream;

import net.mtrop.tame.lang.Value;

public final class ValueTest {

	public static void main(String[] args) 
	{
		Value vfalse = Value.create(false);
		Value vtrue = Value.create(true);
		Value vinf = Value.create(Double.POSITIVE_INFINITY);
		Value vnan = Value.create(Double.NaN);

		Value v0 = Value.create(0);
		
		Value v2 = Value.create(2);
		Value v3 = Value.create(3);
		Value v9 = Value.create(9);

		Value vn2 = Value.create(-2);
		Value vn3 = Value.create(-3);
		Value vn9 = Value.create(-9);

		Value vf0 = Value.create(0.0);
		
		Value vf2 = Value.create(2.0);
		Value vf3 = Value.create(3.0);
		Value vf9 = Value.create(9.0);

		Value vfn2 = Value.create(-2.0);
		Value vfn3 = Value.create(-3.0);
		Value vfn9 = Value.create(-9.0);

		Value ves = Value.create("");
		Value vapple = Value.create("apple");
		Value vorange = Value.create("orange");
		Value vran = Value.create("ran");
		Value vbanana = Value.create("banana");
		Value vna = Value.create("na");
		
		PrintStream out = System.out;
		
		out.println("ABS--------------------");
		out.println(v2 + " " + Value.absolute(v2));
		out.println(vn2 + " " + Value.absolute(vn2));
		out.println("NEG-------------------");
		out.println(v2 + " " + Value.negate(v2));
		out.println(vn2 + " " + Value.negate(vn2));
		out.println(vf2 + " " + Value.negate(vf2));
		out.println(vfn9 + " " + Value.negate(vfn9));
		out.println("LNO-------------------");
		out.println(v0 + " " + Value.logicalNot(v0));
		out.println(vf0 + " " + Value.logicalNot(vf0));
		out.println(v2 + " " + Value.logicalNot(v2));
		out.println(vf2 + " " + Value.logicalNot(vf2));
		out.println(ves + " " + Value.logicalNot(ves));
		out.println(vapple + " " + Value.logicalNot(vapple));
		out.println("NOT-------------------");
		out.println(vfalse + " " + Value.not(vfalse));
		out.println(vtrue + " " + Value.not(vtrue));
		out.println(v0 + " " + Value.not(v0));
		out.println(vf0 + " " + Value.not(vf0));
		out.println(v2 + " " + Value.not(v2));
		out.println(vf2 + " " + Value.not(vf2));

		out.println("ADD-------------------");
		print(vfalse, vfalse, Value.add(vfalse, vfalse), "+");
		print(vtrue, vfalse, Value.add(vtrue, vfalse), "+");
		print(vfalse, vtrue, Value.add(vfalse, vtrue), "+");
		print(vtrue, vtrue, Value.add(vtrue, vtrue), "+");
		print(v2, v3, Value.add(v2, v3), "+");
		print(vf2, vf3, Value.add(vf2, vf3), "+");
		print(vf2, v3, Value.add(vf2, v3), "+");
		print(v2, vf3, Value.add(v2, vf3), "+");
		print(vf2, v3, Value.add(vf2, v3), "+");
		print(vapple, vorange, Value.add(vapple, vorange), "+");
		print(vapple, v3, Value.add(vapple, v3), "+");
		print(vapple, vf3, Value.add(vapple, vf3), "+");

		out.println("SUB-------------------");
		print(vfalse, vfalse, Value.subtract(vfalse, vfalse), "-");
		print(vtrue, vfalse, Value.subtract(vtrue, vfalse), "-");
		print(vfalse, vtrue, Value.subtract(vfalse, vtrue), "-");
		print(vtrue, vtrue, Value.subtract(vtrue, vtrue), "-");
		print(v2, v3, Value.subtract(v2, v3), "-");
		print(vf2, vf3, Value.subtract(vf2, vf3), "-");
		print(vf2, v3, Value.subtract(vf2, v3), "-");
		print(v2, vf3, Value.subtract(v2, vf3), "-");
		print(vf2, v3, Value.subtract(vf2, v3), "-");
		
		out.println("MUL-------------------");
		print(vfalse, vfalse, Value.multiply(vfalse, vfalse), "*");
		print(vtrue, vfalse, Value.multiply(vtrue, vfalse), "*");
		print(vfalse, vtrue, Value.multiply(vfalse, vtrue), "*");
		print(vtrue, vtrue, Value.multiply(vtrue, vtrue), "*");
		print(v2, v3, Value.multiply(v2, v3), "*");
		print(vf2, vf3, Value.multiply(vf2, vf3), "*");
		print(vf2, v3, Value.multiply(vf2, v3), "*");
		print(v2, vf3, Value.multiply(v2, vf3), "*");
		print(vf2, v3, Value.multiply(vf2, v3), "*");
		
		out.println("DIV-------------------");
		print(v2, v3, Value.divide(v2, v3), "/");
		print(vf2, vf3, Value.divide(vf2, vf3), "/");
		print(vf2, v3, Value.divide(vf2, v3), "/");
		print(v2, vf3, Value.divide(v2, vf3), "/");
		print(vf2, v3, Value.divide(vf2, v3), "/");
		print(v9, v2, Value.divide(v9, v2), "/");
		print(vf9, v2, Value.divide(vf9, v2), "/");
		print(v9, vf2, Value.divide(v9, vf2), "/");
		print(vf9, vf2, Value.divide(vf9, vf2), "/");
		
		out.println("MOD-------------------");
		print(v2, v3, Value.modulo(v2, v3), "%");
		print(vf2, vf3, Value.modulo(vf2, vf3), "%");
		print(vf2, v3, Value.modulo(vf2, v3), "%");
		print(v2, vf3, Value.modulo(v2, vf3), "%");
		print(vf2, v3, Value.modulo(vf2, v3), "%");
		print(v9, v2, Value.modulo(v9, v2), "%");
		print(vf9, v2, Value.modulo(vf9, v2), "%");
		print(v9, vf2, Value.modulo(v9, vf2), "%");
		print(vf9, vf2, Value.modulo(vf9, vf2), "%");
	}
	
	private static void print(Value v1, Value v2, Value out, String s)
	{
		System.out.println(v1 + " "+s+" " + v2 + " = " + out);
	}

}
