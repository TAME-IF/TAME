/*
 * 
 */
package net.mtrop.tame;

import java.io.File;
import java.io.PrintWriter;

import com.blackrook.commons.Common;

import net.mtrop.tame.factory.TAMEJSExporter;
import net.mtrop.tame.factory.TAMEScriptReader;

public final class JSTest 
{

	public static void main(String[] args) throws Exception
	{
		String out = TAMEJSExporter.exportToString(TAMEScriptReader.read(new File("./scripts/test.tame")));
		PrintWriter pw = new PrintWriter(new File("D:/butt/junk.js"));
		pw.append(out);
		Common.close(pw);
	}

}
