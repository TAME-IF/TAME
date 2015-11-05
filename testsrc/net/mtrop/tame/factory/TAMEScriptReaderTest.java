package net.mtrop.tame.factory;

import java.io.File;

import com.blackrook.commons.Common;

public final class TAMEScriptReaderTest 
{
	public static void main(String[] args) throws Exception
	{
		String res = "./scripts/test.tsc";
		TAMEScriptReader.read(new File(res));
	}
}
