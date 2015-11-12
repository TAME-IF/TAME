package net.mtrop.tame.console;

import java.io.PrintStream;

import net.mtrop.tame.TAMEModule;
import net.mtrop.tame.TAMEModuleContext;
import net.mtrop.tame.TAMEResponse;
import net.mtrop.tame.TAMEResponseReader;
import net.mtrop.tame.struct.Cue;

/**
 * A console client implementation.
 * @author Matthew Tropiano
 */
public class TAMEConsoleClientMain 
{
	// Entry point.
	public static void main(String ... args)
	{
		// TODO: Finish.
	}
	
	/**
	 * Game context.
	 */
	static class Context
	{
		TAMEModule module;
		TAMEModuleContext context;
		CueHandler currentHander;
		boolean paused;
	}
	
	/**
	 * Cue handler.
	 */
	static class CueHandler extends TAMEResponseReader
	{
		StringBuilder textBuffer;
		PrintStream out;
		PrintStream err;
		
		CueHandler(TAMEResponse response, Context context, PrintStream out, PrintStream err) 
		{
			super(response);
			this.textBuffer = new StringBuilder();
			this.out = out;
			this.err = err;
		}

		@Override
		public boolean handleCue(Cue cue) 
		{
			switch (cue.getType())
			{
				default:
					return true;
				// TODO: Finish.
			}
		}
		
		public void handleTrace(String content)
		{
			// TODO: Finish.
		}
		
		public void handleText(String content)
		{
			// TODO: Finish.
		}
		
		public void handlePause(String content)
		{
			// TODO: Finish.
		}

		public void handleWait(String content)
		{
			// TODO: Finish.
		}
		
	}
	
}
