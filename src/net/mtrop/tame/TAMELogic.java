package net.mtrop.tame;

import net.mtrop.tame.context.TElementContext;
import net.mtrop.tame.interrupt.TAMEInterrupt;
import net.mtrop.tame.lang.command.Block;

/**
 * Utility and rules class. 
 * @author Matthew Tropiano
 */
public final class TAMELogic
{
	
	/**
	 * Performs the necessary tasks for calling an object block.
	 * Ensures that the block is called cleanly.
	 * @param request the request object.
	 * @param response  the response object.
	 * @param context the context that the block is owned by.
	 * @param block the block to execute.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void callBlock(TAMERequest request, TAMEResponse response, TElementContext<?> context, Block block) throws TAMEInterrupt
	{
		response.trace(request, "Pushing %s...", context);
		request.pushContext(context);
		try {
			block.execute(request, response);
		} finally {
			response.trace(request, "Popping %s...", context);
			request.checkStackClear();
		}
	}
	
}
