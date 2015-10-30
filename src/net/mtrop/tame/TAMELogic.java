package net.mtrop.tame;

import java.util.Iterator;

import net.mtrop.tame.element.TAction;
import net.mtrop.tame.element.TElement;
import net.mtrop.tame.element.TObject;
import net.mtrop.tame.element.TPlayer;
import net.mtrop.tame.element.TRoom;
import net.mtrop.tame.element.TWorld;
import net.mtrop.tame.element.context.TElementContext;
import net.mtrop.tame.element.context.TObjectContext;
import net.mtrop.tame.element.context.TOwnershipMap;
import net.mtrop.tame.element.context.TPlayerContext;
import net.mtrop.tame.element.context.TRoomContext;
import net.mtrop.tame.element.context.TWorldContext;
import net.mtrop.tame.interrupt.TAMEInterrupt;
import net.mtrop.tame.lang.Block;
import net.mtrop.tame.lang.Value;

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

	/**
	 * Attempts to call the ambiguous action blocks.
	 * @param request the request object.
	 * @param response the response object.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void doActionAmbiguity(TAMERequest request, TAMEResponse response) throws TAMEInterrupt
	{
		// TODO: Finish this.
	}

	/**
	 * Attempts to call the bad action blocks.
	 * @param request the request object.
	 * @param response the response object.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void doBadAction(TAMERequest request, TAMEResponse response) throws TAMEInterrupt
	{
		// TODO: Finish this.
	}

	/**
	 * Attempts to perform a general action.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action that is being called.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void doActionGeneral(TAMERequest request, TAMEResponse response, TAction action) throws TAMEInterrupt
	{
		doActionOpen(request, response, action, null);
	}

	/**
	 * Attempts to perform a general action.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action that is being called.
	 * @param openTarget if not null, added as a target variable.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void doActionOpen(TAMERequest request, TAMEResponse response, TAction action, String openTarget) throws TAMEInterrupt
	{
		response.trace(request, "Performing general action %s", action);

		TAMEModuleContext moduleContext = request.getModuleContext();
		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
		Block blockToCall = null;
		
		if (currentPlayerContext != null)
		{
			TPlayer currentPlayer = currentPlayerContext.getElement();
			response.trace(request, "For current player %s...", currentPlayer);

			// check if the action is disallowed by the player.
			if (!currentPlayer.allowsAction(action))
			{
				response.trace(request, "Action is not allowed.");
				callPlayerActionForbiddenBlock(request, response, action, currentPlayerContext);
				return;
			}

			// try current room.
			TRoomContext currentRoomContext = moduleContext.getCurrentRoomContext();
			if (currentRoomContext != null)
			{
				TRoom currentRoom = currentRoomContext.getElement();
				
				response.trace(request, "And current room %s...", currentRoom);

				// check if the action is disallowed by the room.
				if (!currentRoom.allowsAction(action))
				{
					response.trace(request, "Action is not allowed.");
					callRoomActionForbiddenBlock(request, response, action, currentRoomContext);
					return;
				}

				// get general action on room.
				if ((blockToCall = currentRoom.getActionTable().get(action.getIdentity())) != null)
				{
					response.trace(request, "Found general action block on room.");
					if (openTarget != null)
						currentRoomContext.setValue(openTarget, Value.create(openTarget));
					callBlock(request, response, currentRoomContext, blockToCall);
					return;
				}
				
				response.trace(request, "No general action block on room.");
			}
			
			// get general action on player.
			if ((blockToCall = currentPlayer.getActionTable().get(action.getIdentity())) != null)
			{
				response.trace(request, "Found general action block on player.");
				if (openTarget != null)
					currentPlayerContext.setValue(openTarget, Value.create(openTarget));
				callBlock(request, response, currentPlayerContext, blockToCall);
				return;
			}
			
			response.trace(request, "No general action block on player.");
			
		}
		
		TWorldContext worldContext = moduleContext.getWorldContext();
		TWorld world = worldContext.getElement();
		
		// get general action on world.
		if ((blockToCall = world.getActionTable().get(action.getIdentity())) != null)
		{
			response.trace(request, "Found general action block on world.");
			if (openTarget != null)
				worldContext.setValue(openTarget, Value.create(openTarget));
			callBlock(request, response, worldContext, blockToCall);
			return;
		}

		// try fail on player.
		if (currentPlayerContext != null && callPlayerActionFailBlock(request, response, action, currentPlayerContext))
			return;

		// try fail on world.
		callWorldActionFailBlock(request, response, action, worldContext);
	}

	/**
	 * Attempts to perform a modal action.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action that is being called.
	 * @param mode the mode to process.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void doActionModal(TAMERequest request, TAMEResponse response, TAction action, String mode) throws TAMEInterrupt
	{
		response.trace(request, "Performing modal action %s, \"%s\"", action, mode);

		TAMEModuleContext moduleContext = request.getModuleContext();
		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
		Block blockToCall = null;
		
		if (currentPlayerContext != null)
		{
			TPlayer currentPlayer = currentPlayerContext.getElement();
			response.trace(request, "For current player %s...", currentPlayer);

			// check if the action is disallowed by the player.
			if (!currentPlayer.allowsAction(action))
			{
				response.trace(request, "Action is not allowed.");
				callPlayerActionForbiddenBlock(request, response, action, currentPlayerContext);
				return;
			}

			// try current room.
			TRoomContext currentRoomContext = moduleContext.getCurrentRoomContext();
			if (currentRoomContext != null)
			{
				TRoom currentRoom = currentRoomContext.getElement();
				
				response.trace(request, "And current room %s...", currentRoom);

				// check if the action is disallowed by the room.
				if (!currentRoom.allowsAction(action))
				{
					response.trace(request, "Action is not allowed.");
					callRoomActionForbiddenBlock(request, response, action, currentRoomContext);
					return;
				}

				// get modal action on room.
				if ((blockToCall = currentRoom.getModalActionTable().get(action.getIdentity(), mode)) != null)
				{
					response.trace(request, "Found modal action block on room.");
					callBlock(request, response, currentRoomContext, blockToCall);
					return;
				}
				
				response.trace(request, "No modal action block on room.");
			}
			
			// get modal action on player.
			if ((blockToCall = currentPlayer.getModalActionTable().get(action.getIdentity(), mode)) != null)
			{
				response.trace(request, "Found modal action block on player.");
				callBlock(request, response, currentPlayerContext, blockToCall);
				return;
			}
			
			response.trace(request, "No modal action block on player.");
			
		}
		
		TWorldContext worldContext = moduleContext.getWorldContext();
		TWorld world = worldContext.getElement();
		
		// get modal action on world.
		if ((blockToCall = world.getModalActionTable().get(action.getIdentity(), mode)) != null)
		{
			response.trace(request, "Found modal action block on world.");
			callBlock(request, response, worldContext, blockToCall);
			return;
		}

		// try fail on player.
		if (currentPlayerContext != null && callPlayerActionFailBlock(request, response, action, currentPlayerContext))
			return;

		// try fail on world.
		callWorldActionFailBlock(request, response, action, worldContext);
	}
	
	/**
	 * Attempts to perform a transitive action.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action that is being called.
	 * @param object the target object for the action.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void doActionTransitive(TAMERequest request, TAMEResponse response, TAction action, TObject object) throws TAMEInterrupt
	{
		response.trace(request, "Performing transitive action %s on %s", action, object);

		TAMEModuleContext moduleContext = request.getModuleContext();
		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
		TObjectContext currentObjectContext = moduleContext.getObjectContext(object);
		Block blockToCall = null;
		
		if (callCheckActionForbidden(request, response, action, moduleContext))
			return;

		// call action on object.
		if ((blockToCall = object.getActionTable().get(action.getIdentity())) != null)
		{
			response.trace(request, "Found action block on object.");
			callBlock(request, response, currentObjectContext, blockToCall);
			return;
		}
		
		TWorldContext worldContext = moduleContext.getWorldContext();
		
		// try fail on player.
		if (currentPlayerContext != null && callPlayerActionFailBlock(request, response, action, currentPlayerContext))
			return;

		// try fail on world.
		callWorldActionFailBlock(request, response, action, worldContext);
	}

	/**
	 * Attempts to perform a ditransitive action.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action that is being called.
	 * @param object1 the first object for the action.
	 * @param object2 the second object for the action.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void doActionDitransitive(TAMERequest request, TAMEResponse response, TAction action, TObject object1, TObject object2) throws TAMEInterrupt
	{
		response.trace(request, "Performing ditransitive action %s on %s with %s", action, object1, object2);

		TAMEModuleContext moduleContext = request.getModuleContext();
		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
		TObjectContext currentObject1Context = moduleContext.getObjectContext(object1);
		TObjectContext currentObject2Context = moduleContext.getObjectContext(object2);
		Block blockToCall = null;
		
		if (callCheckActionForbidden(request, response, action, moduleContext))
			return;

		boolean success = false;
		
		// call action on each object. one or both need to succeed for no failure.
		if ((blockToCall = object1.getActionWithTable().get(action.getIdentity(), object2.getIdentity())) != null)
		{
			response.trace(request, "Found action block on object %s with %s.", object1, object2);
			callBlock(request, response, currentObject1Context, blockToCall);
			success = true;
		}
		if ((blockToCall = object2.getActionWithTable().get(action.getIdentity(), object1.getIdentity())) != null)
		{
			response.trace(request, "Found action block on object %s with %s.", object2, object1);
			callBlock(request, response, currentObject2Context, blockToCall);
			success = true;
		}
		
		if (!success)
		{
			TWorldContext worldContext = moduleContext.getWorldContext();
			
			// try fail on player.
			if (currentPlayerContext != null && callPlayerActionFailBlock(request, response, action, currentPlayerContext))
				return;

			// try fail on world.
			callWorldActionFailBlock(request, response, action, worldContext);
		}
		
	}
	
	/**
	 * Checks and calls the action forbidden blocks.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @param moduleContext the module context.
	 * @return true if a forbidden block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callCheckActionForbidden(TAMERequest request, TAMEResponse response, TAction action, TAMEModuleContext moduleContext) throws TAMEInterrupt 
	{
		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
		
		if (currentPlayerContext != null)
		{
			TPlayer currentPlayer = currentPlayerContext.getElement();
			response.trace(request, "Checking current player %s for action permission.", currentPlayer);
	
			// check if the action is disallowed by the player.
			if (!currentPlayer.allowsAction(action))
			{
				response.trace(request, "Action is not allowed.");
				callPlayerActionForbiddenBlock(request, response, action, currentPlayerContext);
				return true;
			}
			
			// try current room.
			TRoomContext currentRoomContext = moduleContext.getCurrentRoomContext();
			if (currentRoomContext != null)
			{
				TRoom currentRoom = currentRoomContext.getElement();
				
				response.trace(request, "Checking current room %s for action permission.", currentRoom);
	
				// check if the action is disallowed by the room.
				if (!currentRoom.allowsAction(action))
				{
					response.trace(request, "Action is not allowed.");
					callRoomActionForbiddenBlock(request, response, action, currentRoomContext);
					return true;
				}
			}
		}
		
		return false;
	}

	/**
	 * Calls the appropriate action fail block on the world if it exists.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @param context the player context.
	 * @return true if a fail block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callWorldActionFailBlock(TAMERequest request, TAMEResponse response, TAction action, TWorldContext context) throws TAMEInterrupt 
	{
		TWorld world = context.getElement();
		
		Block blockToCall;
		
		if ((blockToCall = world.getActionFailTable().get(action.getIdentity())) != null)
		{
			response.trace(request, "Found specific action failure block on world.");
			callBlock(request, response, context, blockToCall);
			return true;
		}

		if ((blockToCall = world.getActionFailBlock()) != null)
		{
			response.trace(request, "Found default action failure block on world.");
			callBlock(request, response, context, blockToCall);
			return true;
		}

		response.trace(request, "No action failure block on world.");
		return false;
	}

	/**
	 * Calls the appropriate action fail block on a player if it exists.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @param context the player context.
	 * @return true if a fail block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callPlayerActionFailBlock(TAMERequest request, TAMEResponse response, TAction action, TPlayerContext context) throws TAMEInterrupt 
	{
		TPlayer player = context.getElement();
		
		Block blockToCall;
		
		if ((blockToCall = player.getActionFailTable().get(action.getIdentity())) != null)
		{
			response.trace(request, "Found specific action failure block on player.");
			callBlock(request, response, context, blockToCall);
			return true;
		}

		if ((blockToCall = player.getActionFailBlock()) != null)
		{
			response.trace(request, "Found default action failure block on player.");
			callBlock(request, response, context, blockToCall);
			return true;
		}

		response.trace(request, "No action failure block on player.");
		return false;
	}

	/**
	 * Calls the appropriate action forbidden block on a player.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @param context the player context.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static void callPlayerActionForbiddenBlock(TAMERequest request, TAMEResponse response, TAction action, TPlayerContext context) throws TAMEInterrupt 
	{
		TPlayer player = context.getElement();

		// get forbid block.
		Block forbidBlock = null;

		if ((forbidBlock = player.getActionForbidTable().get(action.getIdentity())) != null)
		{
			response.trace(request, "Got specific forbid block on player.");
			callBlock(request, response, context, forbidBlock);
		}
		else if ((forbidBlock = player.getActionForbidBlock()) != null)
		{
			response.trace(request, "Got default forbid block on player.");
			callBlock(request, response, context, forbidBlock);
		}
		else
		{
			response.trace(request, "No forbid block on player to call. Cancelling.");
		}
	}

	/**
	 * Calls the appropriate action forbidden block on a room.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @param context the room context.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static void callRoomActionForbiddenBlock(TAMERequest request, TAMEResponse response, TAction action, TRoomContext context) throws TAMEInterrupt 
	{
		TRoom room = context.getElement();
		
		// get forbid block.
		Block forbidBlock = null;
		
		if ((forbidBlock = room.getActionForbidTable().get(action.getIdentity())) != null)
		{
			response.trace(request, "Calling specific forbid block on room.");
			callBlock(request, response, context, forbidBlock);
		}
		else if ((forbidBlock = room.getActionForbidBlock()) != null)
		{
			response.trace(request, "Calling default forbid block on room.");
			callBlock(request, response, context, forbidBlock);
		}
		else
		{
			response.trace(request, "No forbid block on room to call. Cancelling.");
		}
	}

	/**
	 * Returns all objects in the accessible area by an object name read from the interpreter.
	 * The output stops if the size of the output array is reached.
	 * @param moduleContext the module context.
	 * @param name the name from the interpreter.
	 * @param outputArray the output vector of found objects.
	 * @param arrayOffset the starting offset into the array to put them.
	 * @return the amount of objects found.
	 */
	public static int findAccessibleObjectsByName(TAMEModuleContext moduleContext, String name, TObject[] outputArray, int arrayOffset)
	{
		TPlayerContext playerContext = moduleContext.getCurrentPlayerContext();
		TRoomContext roomContext = moduleContext.getCurrentRoomContext();
		TWorldContext worldContext = moduleContext.getWorldContext();
		TOwnershipMap ownerMap = moduleContext.getOwnershipMap();
		int start = arrayOffset;
		
		if (playerContext != null) for (TObject obj : ownerMap.getObjectsOwnedByPlayer(playerContext.getElement()))
		{
			if (moduleContext.getObjectContext(obj).containsName(name))
			{
				outputArray[arrayOffset++] = obj;
				if (arrayOffset == outputArray.length)
					return arrayOffset - start;
			}
		}
		
		if (roomContext != null) for (TObject obj : ownerMap.getObjectsOwnedByRoom(roomContext.getElement()))
		{
			if (moduleContext.getObjectContext(obj).containsName(name))
			{
				outputArray[arrayOffset++] = obj;
				if (arrayOffset == outputArray.length)
					return arrayOffset - start;
			}
		}
	
		for (TObject obj : ownerMap.getObjectsOwnedByWorld(worldContext.getElement()))
		{
			if (moduleContext.getObjectContext(obj).containsName(name))
			{
				outputArray[arrayOffset++] = obj;
				if (arrayOffset == outputArray.length)
					return arrayOffset - start;
			}
		}
	
		return arrayOffset - start;
	}

	/**
	 * Initializes a newly-created context by executing each initialization block on each object.
	 * @param request the request object containing the module context.
	 * @param response the response object.
	 */
	public static void initializeContext(TAMERequest request, TAMEResponse response) throws TAMEInterrupt
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		
		response.trace(request, "Starting init...");
		
		callInitOnContexts(request, response, moduleContext.getObjectContextList().valueIterator());
		callInitOnContexts(request, response, moduleContext.getRoomContextList().valueIterator());
		callInitOnContexts(request, response, moduleContext.getPlayerContextList().valueIterator());
		callInitBlock(request, response, moduleContext.getWorldContext());
		
	}

	// Call init on iterable contexts.
	private static void callInitOnContexts(TAMERequest request, TAMEResponse response, Iterator<? extends TElementContext<?>> contexts) throws TAMEInterrupt 
	{
		while (contexts.hasNext())
		{
			TElementContext<?> context = contexts.next();
			callInitBlock(request, response, context);
		}
	}

	// Call init on a single context.
	private static void callInitBlock(TAMERequest request, TAMEResponse response, TElementContext<?> context) throws TAMEInterrupt 
	{
		response.trace(request, "Attempt init on %s.", context);
		TElement element = context.getElement();

		Block initBlock = element.getInitBlock();
		if (initBlock != null)
		{
			response.trace(request, "Calling onInit() on %s.", context);
			callBlock(request, response, context, initBlock);
		}
		else
		{
			response.trace(request, "No init block.");
		}
	}
	
}
