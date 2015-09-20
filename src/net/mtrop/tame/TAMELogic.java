package net.mtrop.tame;

import net.mtrop.tame.element.TObject;
import net.mtrop.tame.element.TPlayer;
import net.mtrop.tame.element.TRoom;
import net.mtrop.tame.element.TWorld;
import net.mtrop.tame.element.context.TElementContext;
import net.mtrop.tame.element.context.TOwnershipMap;
import net.mtrop.tame.element.context.TPlayerContext;
import net.mtrop.tame.element.context.TRoomContext;
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
	 * @param response the response object.
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
	
	/**
	 * Checks if an object is accessible to a player.
	 * @param request the request object.
	 * @param response the response object.
	 * @param player the player viewpoint.
	 * @param object the object to check.
	 * @return true if the object is considered "accessible," false if not.
	 */
	public static boolean checkObjectAccessibility(TAMERequest request, TAMEResponse response, TPlayer player, TObject object)
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TWorld world = moduleContext.resolveWorld();
		TOwnershipMap ownershipMap = moduleContext.getOwnershipMap();
		
		response.trace(request, "Check world for %s...", object);
		if (ownershipMap.checkWorldHasObject(world, object))
		{
			response.trace(request, "Found.");
			return true;
		}

		response.trace(request, "Check %s for %s...", player, object);
		if (ownershipMap.checkPlayerHasObject(player, object))
		{
			response.trace(request, "Found.");
			return true;
		}
		
		TRoom currentRoom = ownershipMap.getCurrentRoom(player);
		
		response.trace(request, "Check %s for %s...", currentRoom, object);
		if (currentRoom != null && ownershipMap.checkRoomHasObject(currentRoom, object))
		{
			response.trace(request, "Found.");
			return true;
		}
		
		response.trace(request, "Not found.");
		return false;
	}
	
	/**
	 * Attempts to perform a player switch.
	 * @param request the request object.
	 * @param response the response object.
	 * @param nextPlayer the next player.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void doPlayerSwitch(TAMERequest request, TAMEResponse response, TPlayer nextPlayer) throws TAMEInterrupt 
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TPlayerContext currentPlayerContext; 
		
		// unfocus.
		currentPlayerContext = moduleContext.getCurrentPlayerContext();
		if (currentPlayerContext != null)
		{
			TPlayer currentPlayer = currentPlayerContext.getElement();
			response.trace(request, "Check %s for unfocus block.", currentPlayer);
			Block block = currentPlayer.getUnfocusBlock();
			if (block != null)
			{
				response.trace(request, "Calling unfocus block on %s.", currentPlayer);
				callBlock(request, response, currentPlayerContext, block);
			}
		}
		else
		{
			response.trace(request, "No current player. Skipping unfocus.");
		}
		
		// set next player.
		response.trace(request, "Setting current player to %s.", nextPlayer);
		moduleContext.setCurrentPlayer(nextPlayer);
		
		// focus.
		currentPlayerContext = moduleContext.getCurrentPlayerContext();
		TPlayer currentPlayer = currentPlayerContext.getElement();
		response.trace(request, "Check %s for focus block.", currentPlayer);
		Block block = currentPlayer.getFocusBlock();
		if (block != null)
		{
			response.trace(request, "Calling focus block on %s.", currentPlayer);
			callBlock(request, response, currentPlayerContext, block);
		}
	}

	/**
	 * Attempts to perform a room stack pop for a player.
	 * @param request the request object.
	 * @param response the response object.
	 * @param player the player to pop a room context from.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void doRoomPop(TAMERequest request, TAMEResponse response, TPlayer player) throws TAMEInterrupt 
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TOwnershipMap ownership = moduleContext.getOwnershipMap();
		TRoom currentRoom = ownership.getCurrentRoom(player);
		
		response.trace(request, "Check %s for unfocus block.", currentRoom);
		Block block = currentRoom.getUnfocusBlock();
		if (block != null)
		{
			TRoomContext roomContext = moduleContext.getRoomContext(currentRoom);
			response.trace(request, "Calling unfocus block on %s.", currentRoom);
			TAMELogic.callBlock(request, response, roomContext, block);
		}

		response.trace(request, "Popping top room from %s.", player);
		ownership.popRoomFromPlayer(player);
	}
	
	/**
	 * Attempts to perform a room stack push for a player.
	 * @param request the request object.
	 * @param response the response object.
	 * @param player the player that entered the room.
	 * @param room the player to push a room context onto.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void doRoomPush(TAMERequest request, TAMEResponse response, TPlayer player, TRoom nextRoom) throws TAMEInterrupt
	{
		TAMEModuleContext moduleContext = request.getModuleContext();

		response.trace(request, "Pushing %s on %s.", nextRoom, player);
		moduleContext.getOwnershipMap().pushRoomOntoPlayer(player, nextRoom);
		
		response.trace(request, "Check %s for focus block.", nextRoom);
		Block block = nextRoom.getFocusBlock();
		if (block != null)
		{
			TRoomContext roomContext = moduleContext.getRoomContext(nextRoom);
			response.trace(request, "Calling focus block on %s.", nextRoom);
			TAMELogic.callBlock(request, response, roomContext, block);
		}
		
	}
	
	/**
	 * Attempts to perform a room switch.
	 * @param request the request object.
	 * @param response the response object.
	 * @param player the player that is switching rooms.
	 * @param nextRoom the target room.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void doRoomSwitch(TAMERequest request, TAMEResponse response, TPlayer player, TRoom nextRoom) throws TAMEInterrupt
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TOwnershipMap ownership = moduleContext.getOwnershipMap();
		response.trace(request, "Leaving rooms for %s.", player);

		// pop all rooms on the stack.
		while (ownership.getCurrentRoom(player) != null)
			TAMELogic.doRoomPop(request, response, player);

		// push new room on the stack and call focus.
		TAMELogic.doRoomPush(request, response, player, nextRoom);
	}

	/**
	 * Attempts to perform a room swap (pop then push).
	 * @param request the request object.
	 * @param response the response object.
	 * @param player the player that is swapping rooms.
	 * @param nextRoom the target room.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void doRoomSwap(TAMERequest request, TAMEResponse response, TPlayer player, TRoom nextRoom) throws TAMEInterrupt
	{
		response.trace(request, "Leaving rooms for %s.", player);

		// pop room from the stack.
		TAMELogic.doRoomPop(request, response, player);

		// push new room on the stack and call focus.
		TAMELogic.doRoomPush(request, response, player, nextRoom);
	}

}
