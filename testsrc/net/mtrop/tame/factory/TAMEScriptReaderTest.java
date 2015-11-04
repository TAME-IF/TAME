package net.mtrop.tame.factory;

public final class TAMEScriptReaderTest 
{
	public static void main(String[] args) throws Exception
	{
		TAMEScriptReader.read("stringReplace(\"banana\" + 56 / 9 % 7, \"na\", \"\")");
	}
}
