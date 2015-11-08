package net.mtrop.tame.factory;

import java.io.File;

import com.blackrook.commons.Common;

import net.mtrop.tame.TAMEModule;

public final class TAMEScriptReaderTest 
{
	public static void main(String[] args) throws Exception
	{
		String res = "./scripts/test.tsc";
		TAMEModule module = TAMEScriptReader.read(new File(res));
		Common.noop();
	}
}
