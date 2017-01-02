/*
 * 
 */
package net.mtrop.tame;

import java.io.File;

import net.mtrop.tame.factory.TAMEJSExporter;
import net.mtrop.tame.factory.TAMEScriptReader;

public final class JSTest 
{

	public static void main(String[] args) throws Exception
	{
		String out = TAMEJSExporter.exportToString(TAMEScriptReader.read(new File("./scripts/test.tame")));
		System.out.println(out);
	}

}
