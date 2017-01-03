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
		String outProperty = System.getProperty("jstest.outfile.path");
		if (outProperty != null)
		{
			PrintWriter pw = new PrintWriter(new File(outProperty));
			pw.append(out);
			Common.close(pw);
		}
		else
		{
			System.out.println(out);
		}
	}

}
