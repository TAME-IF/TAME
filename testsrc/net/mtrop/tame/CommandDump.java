package net.mtrop.tame;

import java.io.PrintStream;
import java.util.Arrays;

import net.mtrop.tame.lang.BlockEntryType;

public final class CommandDump 
{

	public static void main(String[] args)
	{
		PrintStream out = System.out;
		
		for (TAMECommand command : TAMECommand.values())
		{
			if (command.isInternal() || command.isLanguage())
				continue;
			
			out.println(command.name() + " Returns: " + command.getReturnType() + " Arguments: " + Arrays.toString(command.getArgumentTypes()));
		}

		out.println("---------------------------");
		
		for (TAMECommand command : TAMECommand.values())
		{
			if (command.isInternal() || command.isLanguage())
				continue;
			
			out.print(command.name() + " ");
		}

		out.println("\n---------------------------");
		
		for (BlockEntryType command : BlockEntryType.values())
		{
			out.print(command.name() + " ");
		}
	}

}
