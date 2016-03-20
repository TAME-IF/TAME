/*******************************************************************************
 * Copyright (c) 2015 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *
 * Contributors:
 *     Matt Tropiano - initial API and implementation
 *******************************************************************************/
package net.mtrop.tame;

import java.util.Iterator;

import net.mtrop.tame.element.TElement;
import net.mtrop.tame.element.type.TAction;
import net.mtrop.tame.element.type.TContainer;
import net.mtrop.tame.element.type.TObject;
import net.mtrop.tame.element.type.TPlayer;
import net.mtrop.tame.element.type.TRoom;
import net.mtrop.tame.element.type.TWorld;
import net.mtrop.tame.element.type.context.TElementContext;
import net.mtrop.tame.element.type.context.TObjectContext;
import net.mtrop.tame.element.type.context.TOwnershipMap;
import net.mtrop.tame.element.type.context.TPlayerContext;
import net.mtrop.tame.element.type.context.TRoomContext;
import net.mtrop.tame.element.type.context.TWorldContext;
import net.mtrop.tame.exception.TAMEFatalException;
import net.mtrop.tame.exception.UnexpectedValueException;
import net.mtrop.tame.interrupt.EndInterrupt;
import net.mtrop.tame.interrupt.TAMEInterrupt;
import net.mtrop.tame.lang.ArithmeticOperator;
import net.mtrop.tame.lang.Block;
import net.mtrop.tame.lang.Value;

/**
 * Rules class.
 * Governs most if not all logic in TAME.
 * @author Matthew Tropiano
 */
public final class TAMELogic implements TAMEConstants
{
	/**
	 * Handles context initialization, returning the response from it.
	 * @param moduleContext the module context.
	 * @param tracing if true, this does tracing.
	 * @return a TAMERequest a new request.
	 */
	public static TAMEResponse handleInit(TAMEModuleContext moduleContext, boolean tracing)
	{
		TAMERequest request = createRequest(moduleContext, "[INITIALIZE]", tracing);
		TAMEResponse response = new TAMEResponse();

		response.setInterpretNanos(0L);

		// time this stuff.
		long nanos = System.nanoTime();

		try {
			initializeContext(request, response);
			processActionLoop(request, response);
		} catch (TAMEFatalException exception) {
			response.addCue(CUE_FATAL, exception.getMessage());
		} catch (TAMEInterrupt interrupt) {
			response.addCue(CUE_ERROR, interrupt.getMessage());
		}
		
		response.setRequestNanos(System.nanoTime() - nanos);

		return response;
	}
	
	/**
	 * Handles a full request.
	 * @param moduleContext the module context.
	 * @param input the client input query.
	 * @param tracing if true, this does tracing.
	 * @return a TAMERequest a new request.
	 */
	public static TAMEResponse handleRequest(TAMEModuleContext moduleContext, String input, boolean tracing)
	{
		TAMERequest request = createRequest(moduleContext, input, tracing);
		TAMEResponse response = new TAMEResponse();
		
		// time this stuff.
		long nanos;
		
		nanos = System.nanoTime();
		TAMEInterpreterContext interpreterContext = interpret(request);
		response.setInterpretNanos(System.nanoTime() - nanos);
	
		nanos = System.nanoTime();
		
		try {
			enqueueInterpretedAction(request, response, interpreterContext);			
			processActionLoop(request, response);
		} catch (TAMEFatalException exception) {
			response.addCue(CUE_FATAL, exception.getMessage());
		} catch (TAMEInterrupt interrupt) {
			response.addCue(CUE_ERROR, interrupt.getMessage());
		}
	
		response.setRequestNanos(System.nanoTime() - nanos);
		return response;
	}

	/**
	 * Creates the request object.
	 * @param moduleContext the module context.
	 * @param input the client input query.
	 * @param tracing if true, this does tracing.
	 * @return a TAMERequest a new request.
	 */
	public static TAMERequest createRequest(TAMEModuleContext moduleContext, String input, boolean tracing)
	{
		TAMERequest out = new TAMERequest();
		out.setInputMessage(input);
		out.setModuleContext(moduleContext);
		out.setTracing(tracing);
		return out;
	}
	
	/**
	 * Interprets the input on the request.
	 * @param request the request.
	 */
	public static TAMEInterpreterContext interpret(TAMERequest request)
	{
		TAMEInterpreterContext interpreterContext = new TAMEInterpreterContext(request.getInputMessage());
		TAMEModuleContext moduleContext = request.getModuleContext();
		
		interpretAction(moduleContext, interpreterContext);
		
		TAction action = interpreterContext.getAction();
		if (action == null)
			return interpreterContext;
		
		switch (action.getType())
		{
			default:
			case GENERAL:
				return interpreterContext;
			case OPEN:
				interpretOpen(interpreterContext);
				return interpreterContext;
			case MODAL:
				interpretMode(action, interpreterContext);
				return interpreterContext;
			case TRANSITIVE:
				interpretObject1(moduleContext, interpreterContext);
				return interpreterContext;
			case DITRANSITIVE:
				if (interpretObject1(moduleContext, interpreterContext))
					if (interpretConjugate(action, interpreterContext))
						interpretObject2(moduleContext, interpreterContext);
				return interpreterContext;
		}
	}

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
			block.call(request, response);
		} catch (Throwable t) {
			throw t;
		} finally {
			response.trace(request, "Popping %s...", context);
			request.popContext();
		}
		request.checkStackClear();
	}
	
	/**
	 * Performs the necessary tasks for calling a procedure on the same current context.
	 * Ensures that the block is called cleanly.
	 * @param request the request object.
	 * @param response the response object.
	 * @param block the block to execute.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void callProcedure(TAMERequest request, TAMEResponse response, Block block) throws TAMEInterrupt
	{
		try {
			block.call(request, response);
		} catch (Throwable t) {
			throw t;
		}
		request.checkStackClear();
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
	 * Performs an arithmetic function on the stack.
	 * @param request
	 * @param functionType
	 */
	public static void doArithmeticStackFunction(TAMERequest request, int functionType)
	{
		if (functionType < 0 || functionType >= ArithmeticOperator.values().length)
			throw new UnexpectedValueException("Expected arithmetic function type, got illegal value %d.", functionType);

		ArithmeticOperator operator =  ArithmeticOperator.values()[functionType];
		
		if (operator.isBinary())
		{
			Value v2 = request.popValue();
			Value v1 = request.popValue();
			request.pushValue(operator.doOperation(v1, v2));
		}
		else
		{
			Value v1 = request.popValue();
			request.pushValue(operator.doOperation(v1));
		}
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
		
		// set next player.
		response.trace(request, "Setting current player to %s.", nextPlayer);
		moduleContext.setCurrentPlayer(nextPlayer);
	}

	/**
	 * Attempts to perform a player browse.
	 * @param request the request object.
	 * @param response the response object.
	 * @param player the player to browse.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void doPlayerBrowse(TAMERequest request, TAMEResponse response, TPlayer player) throws TAMEInterrupt 
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TOwnershipMap ownership = moduleContext.getOwnershipMap();
		
		response.trace(request, "Start browse %s.", player);
		
		for (TObject object : ownership.getObjectsOwnedByPlayer(player))
		{
			response.trace(request, "Check %s for browse block.", object);
	
			TObjectContext objectContext = moduleContext.getObjectContext(object);
	
			Block block = object.getPlayerBrowseBlock();
			if (block != null)
			{
				response.trace(request, "Calling room browse block on %s.", object);
				TAMELogic.callBlock(request, response, objectContext, block);
			}
			
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
		
		response.trace(request, "Popping top room from %s.", player);
		ownership.popRoomFromPlayer(player);
	}
	
	/**
	 * Attempts to perform a room stack push for a player.
	 * @param request the request object.
	 * @param response the response object.
	 * @param player the player that entered the room.
	 * @param nextRoom the player to push a room context onto.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void doRoomPush(TAMERequest request, TAMEResponse response, TPlayer player, TRoom nextRoom) throws TAMEInterrupt
	{
		TAMEModuleContext moduleContext = request.getModuleContext();

		response.trace(request, "Pushing %s on %s.", nextRoom, player);
		moduleContext.getOwnershipMap().pushRoomOntoPlayer(player, nextRoom);
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
	 * Attempts to perform a room browse.
	 * @param request the request object.
	 * @param response the response object.
	 * @param room the room to browse.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void doRoomBrowse(TAMERequest request, TAMEResponse response, TRoom room) throws TAMEInterrupt 
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TOwnershipMap ownership = moduleContext.getOwnershipMap();
		
		response.trace(request, "Start browse %s.", room);
		
		for (TObject object : ownership.getObjectsOwnedByRoom(room))
		{
			response.trace(request, "Check %s for browse block.", object);
	
			TObjectContext objectContext = moduleContext.getObjectContext(object);
	
			Block block = object.getRoomBrowseBlock();
			if (block != null)
			{
				response.trace(request, "Calling room browse block on %s.", object);
				TAMELogic.callBlock(request, response, objectContext, block);
			}
			
		}
	
	}

	/**
	 * Attempts to perform a container browse.
	 * @param request the request object.
	 * @param response the response object.
	 * @param container the container to browse.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void doContainerBrowse(TAMERequest request, TAMEResponse response, TContainer container) throws TAMEInterrupt 
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TOwnershipMap ownership = moduleContext.getOwnershipMap();
		
		response.trace(request, "Start browse %s.", container);
		
		for (TObject object : ownership.getObjectsOwnedByContainer(container))
		{
			response.trace(request, "Check %s for browse block.", object);
	
			TObjectContext objectContext = moduleContext.getObjectContext(object);
	
			Block block = object.getContainerBrowseBlock();
			if (block != null)
			{
				response.trace(request, "Calling container browse block on %s.", object);
				TAMELogic.callBlock(request, response, objectContext, block);
			}
			
		}
	
	}

	/**
	 * Attempts to call the after request block on the world.
	 * @param request the request object.
	 * @param response the response object.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void doAfterRequest(TAMERequest request, TAMEResponse response) throws TAMEInterrupt
	{
		response.trace(request, "Finding after request block...");

		TAMEModuleContext moduleContext = request.getModuleContext();
		TWorldContext worldContext = moduleContext.getWorldContext();
		Block blockToCall = null;
		
		// get block on world.
		if ((blockToCall = worldContext.getElement().getAfterRequestBlock()) != null)
		{
			response.trace(request, "Found after request block on world.");
			callBlock(request, response, worldContext, blockToCall);
			return;
		}
		else
			response.trace(request, "No after request block to call.");
	}
	
	/**
	 * Attempts to call the bad action blocks.
	 * @param request the request object.
	 * @param response the response object.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void doUnknownAction(TAMERequest request, TAMEResponse response) throws TAMEInterrupt
	{
		response.trace(request, "Finding unknown action blocks...");

		TAMEModuleContext moduleContext = request.getModuleContext();
		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
		Block blockToCall = null;
		
		if (currentPlayerContext != null)
		{
			TPlayer currentPlayer = currentPlayerContext.getElement();
			response.trace(request, "For current player %s...", currentPlayer);
			
			// get block on player.
			if ((blockToCall = currentPlayer.getUnknownActionBlock()) != null)
			{
				response.trace(request, "Found unknown action block on player.");
				callBlock(request, response, currentPlayerContext, blockToCall);
				return;
			}

		}

		TWorldContext worldContext = moduleContext.getWorldContext();
		
		// get block on world.
		if ((blockToCall = worldContext.getElement().getUnknownActionBlock()) != null)
		{
			response.trace(request, "Found unknown action block on world.");
			callBlock(request, response, worldContext, blockToCall);
			return;
		}

		response.trace(request, "No unknown action block to call. Sending error.");
		response.addCue(CUE_ERROR, "ACTION IS BAD or NOT FOUND! (make a better in-universe handler!).");
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
		TAMEModuleContext moduleContext = request.getModuleContext();
		response.trace(request, "Performing general/open action %s", action);

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
					{
						response.trace(request, "Setting variable \"%s\" to \"%s\"", OPEN_TARGET_VARIABLE, openTarget);
						currentRoomContext.setValue(OPEN_TARGET_VARIABLE, Value.create(openTarget));
					}
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
				{
					response.trace(request, "Setting variable \"%s\" to \"%s\"", OPEN_TARGET_VARIABLE, openTarget);
					currentPlayerContext.setValue(OPEN_TARGET_VARIABLE, Value.create(openTarget));
				}
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
			{
				response.trace(request, "Setting variable \"%s\" to \"%s\"", OPEN_TARGET_VARIABLE, openTarget);
				worldContext.setValue(OPEN_TARGET_VARIABLE, Value.create(openTarget));
			}
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
		TAMEModuleContext moduleContext = request.getModuleContext();
		response.trace(request, "Performing modal action %s, \"%s\"", action, mode);

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

		if (!callActionFailed(request, response, action))
			response.addCue(CUE_ERROR, "ACTION FAILED (make a better in-universe handler!).");
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
		TAMEModuleContext moduleContext = request.getModuleContext();
		response.trace(request, "Performing transitive action %s on %s", action, object);

		TObjectContext currentObjectContext = moduleContext.getObjectContext(object);
		Block blockToCall = null;
		
		if (callCheckActionForbidden(request, response, action))
			return;

		// call action on object.
		if ((blockToCall = object.getActionTable().get(action.getIdentity())) != null)
		{
			response.trace(request, "Found action block on object.");
			callBlock(request, response, currentObjectContext, blockToCall);
			return;
		}
		
		if (!callActionFailed(request, response, action))
			response.addCue(CUE_ERROR, "ACTION FAILED (make a better in-universe handler!).");
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
		TAMEModuleContext moduleContext = request.getModuleContext();
		
		response.trace(request, "Performing ditransitive action %s on %s with %s", action, object1, object2);

		TObjectContext currentObject1Context = moduleContext.getObjectContext(object1);
		TObjectContext currentObject2Context = moduleContext.getObjectContext(object2);
		Block blockToCall = null;
		
		if (callCheckActionForbidden(request, response, action))
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
		
		// attempt action with other on both objects.
		if (!success)
		{
			if ((blockToCall = object1.getActionWithOtherTable().get(action.getIdentity())) != null)
			{
				response.trace(request, "Found action with other block on object %s.", object1);
				callBlock(request, response, currentObject1Context, blockToCall);
				success = true;
			}
			if ((blockToCall = object2.getActionWithOtherTable().get(action.getIdentity())) != null)
			{
				response.trace(request, "Found action with other block on object %s.", object2);
				callBlock(request, response, currentObject2Context, blockToCall);
				success = true;
			}
		}
		
		// if we STILL can't do it...
		if (!success)
		{
			response.trace(request, "No blocks called in ditransitive action call.");
			if (!callActionFailed(request, response, action))
				response.addCue(CUE_ERROR, "ACTION FAILED (make a better in-universe handler!).");
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

		moduleContext.setInitialObjectNames();
		callInitOnContexts(request, response, moduleContext.getContainerContextList().valueIterator());
		callInitOnContexts(request, response, moduleContext.getObjectContextList().valueIterator());
		callInitOnContexts(request, response, moduleContext.getRoomContextList().valueIterator());
		callInitOnContexts(request, response, moduleContext.getPlayerContextList().valueIterator());
		callInitBlock(request, response, moduleContext.getWorldContext());
		
	}

	/**
	 * Does an action loop: this keeps processing queued actions 
	 * until there is nothing left to process.
	 * @param request the request context.
	 * @param response the response object.
	 * @throws TAMEInterrupt if an uncaught interrupt occurs.
	 */
	private static void processActionLoop(TAMERequest request, TAMEResponse response) throws TAMEInterrupt 
	{
		boolean initial = true;
		while (request.hasActionItems())
		{
			TAMEAction tameAction = request.getActionItem();
			TAction action = tameAction.getAction();
			
			try {
			
				switch (action.getType())
				{
					default:
					case GENERAL:
						doActionGeneral(request, response, action);
						break;
					case OPEN:
						doActionOpen(request, response, action, tameAction.getTarget());
						break;
					case MODAL:
						doActionModal(request, response, action, tameAction.getTarget());
						break;
					case TRANSITIVE:
						doActionTransitive(request, response, action, tameAction.getObject1());
						break;
					case DITRANSITIVE:
						if (tameAction.getObject2() == null)
							doActionTransitive(request, response, action, tameAction.getObject1());
						else
							doActionDitransitive(request, response, action, tameAction.getObject1(), tameAction.getObject2());
						break;
				}
				
				request.checkStackClear();
				
			} catch (EndInterrupt end) {
				// Catches end.
			}
			
			// do the "after" stuff.
			if (!request.hasActionItems() && initial)
			{
				initial = false;
				doAfterRequest(request, response);
			}
			
		}
		
	}

	private static void enqueueInterpretedAction(TAMERequest request, TAMEResponse response, TAMEInterpreterContext interpreterContext) throws TAMEInterrupt 
	{
		TAction action = interpreterContext.getAction();
		if (action == null)
			doUnknownAction(request, response);
		else
		{
			switch (action.getType())
			{
				default:
				case GENERAL:
					request.addActionItem(TAMEAction.createInitial(action));
					break;
				case OPEN:
				{
					if (!interpreterContext.isTargetLookedUp())
					{
						response.trace(request, "Performing open action %s with no target (incomplete)!", action);
						if (!callActionIncomplete(request, response, action))
							response.addCue(CUE_ERROR, "ACTION INCOMPLETE (make a better in-universe handler!).");
					}
					else
						request.addActionItem(TAMEAction.createInitial(action, interpreterContext.getTarget()));
					break;
				}
				case MODAL:
				{
					if (!interpreterContext.isModeLookedUp())
					{
						response.trace(request, "Performing modal action %s with no mode (incomplete)!", action);
						if (!callActionIncomplete(request, response, action))
							response.addCue(CUE_ERROR, "ACTION INCOMPLETE (make a better in-universe handler!).");
					}
					else if (interpreterContext.getMode() == null)
					{
						response.trace(request, "Performing modal action %s with an unknown mode!", action);
						if (!callBadAction(request, response, action))
							response.addCue(CUE_ERROR, "BAD ACTION (make a better in-universe handler!).");
					}
					else
						request.addActionItem(TAMEAction.createInitial(action, interpreterContext.getMode()));
					break;
				}
				case TRANSITIVE:
				{
					if (interpreterContext.isObjectAmbiguous())
					{
						response.trace(request, "Object is ambiguous for action %s.", action);
						if (!callAmbiguousAction(request, response, action))
							response.addCue(CUE_ERROR, "OBJECT IS AMBIGUOUS (make a better in-universe handler!).");
					}
					else if (!interpreterContext.isObject1LookedUp())
					{
						response.trace(request, "Performing transitive action %s with no object (incomplete)!", action);
						if (!callActionIncomplete(request, response, action))
							response.addCue(CUE_ERROR, "ACTION INCOMPLETE (make a better in-universe handler!).");
					}
					else if (interpreterContext.getObject1() == null)
					{
						response.trace(request, "Performing transitive action %s with an unknown object!", action);
						if (!callBadAction(request, response, action))
							response.addCue(CUE_ERROR, "BAD ACTION (make a better in-universe handler!).");
					}
					else
						request.addActionItem(TAMEAction.createInitial(action, interpreterContext.getObject1()));
					break;
				}
				case DITRANSITIVE:
				{
					if (interpreterContext.isObjectAmbiguous())
					{
						response.trace(request, "Object is ambiguous for action %s.", action);
						if (!callAmbiguousAction(request, response, action))
							response.addCue(CUE_ERROR, "OBJECT IS AMBIGUOUS (make a better in-universe handler!).");
					}
					else if (!interpreterContext.isObject1LookedUp())
					{
						response.trace(request, "Performing ditransitive action %s with no first object (incomplete)!", action);
						if (!callActionIncomplete(request, response, action))
							response.addCue(CUE_ERROR, "ACTION INCOMPLETE (make a better in-universe handler!).");
					}
					else if (interpreterContext.getObject1() == null)
					{
						response.trace(request, "Performing ditransitive action %s with an unknown first object!", action);
						if (!callBadAction(request, response, action))
							response.addCue(CUE_ERROR, "BAD ACTION (make a better in-universe handler!).");
					}
					else if (!interpreterContext.isConjugateLookedUp())
					{
						response.trace(request, "Performing ditransitive action %s as a transitive one...", action);
						request.addActionItem(TAMEAction.createInitial(action, interpreterContext.getObject1()));
					}
					else if (!interpreterContext.isConjugateFound())
					{
						response.trace(request, "Performing ditransitive action %s with an unknown conjugate!", action);
						if (!callBadAction(request, response, action))
							response.addCue(CUE_ERROR, "BAD ACTION (make a better in-universe handler!).");
					}
					else if (!interpreterContext.isObject2LookedUp())
					{
						response.trace(request, "Performing ditransitive action %s with no second object (incomplete)!", action);
						if (!callActionIncomplete(request, response, action))
							response.addCue(CUE_ERROR, "ACTION INCOMPLETE (make a better in-universe handler!).");
					}
					else if (interpreterContext.getObject2() == null)
					{
						response.trace(request, "Performing ditransitive action %s with an unknown second object!", action);
						if (!callBadAction(request, response, action))
							response.addCue(CUE_ERROR, "BAD ACTION (make a better in-universe handler!).");
					}
					else
						request.addActionItem(TAMEAction.createInitial(action, interpreterContext.getObject1(), interpreterContext.getObject2()));
					break;
				}
			}
		}
	}

	/**
	 * Attempts to call the ambiguous action blocks.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action used.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callAmbiguousAction(TAMERequest request, TAMEResponse response, TAction action) throws TAMEInterrupt
	{
		response.trace(request, "Finding ambiguous action blocks...");
	
		TAMEModuleContext moduleContext = request.getModuleContext();
		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
		Block blockToCall = null;
		
		if (currentPlayerContext != null)
		{
			TPlayer currentPlayer = currentPlayerContext.getElement();
			response.trace(request, "For current player %s...", currentPlayer);
			
			// get specific block on player.
			if ((blockToCall = currentPlayer.getAmbiguousActionTable().get(action.getIdentity())) != null)
			{
				response.trace(request, "Found specific ambiguous action block on player.");
				callBlock(request, response, currentPlayerContext, blockToCall);
				return true;
			}
	
			// get block on player.
			if ((blockToCall = currentPlayer.getAmbiguousActionBlock()) != null)
			{
				response.trace(request, "Found default ambiguous action block on player.");
				callBlock(request, response, currentPlayerContext, blockToCall);
				return true;
			}
	
		}
	
		TWorldContext worldContext = moduleContext.getWorldContext();
		
		// get specific block on world.
		if ((blockToCall = worldContext.getElement().getAmbiguousActionTable().get(action.getIdentity())) != null)
		{
			response.trace(request, "Found specific ambiguous action block on world.");
			callBlock(request, response, worldContext, blockToCall);
			return true;
		}
	
		// get block on world.
		if ((blockToCall = worldContext.getElement().getAmbiguousActionBlock()) != null)
		{
			response.trace(request, "Found default ambiguous action block on world.");
			callBlock(request, response, worldContext, blockToCall);
			return true;
		}

		return false;
	}

	/**
	 * Checks and calls the action forbidden blocks.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @return true if a forbidden block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callCheckActionForbidden(TAMERequest request, TAMEResponse response, TAction action) throws TAMEInterrupt 
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
		
		if (currentPlayerContext != null)
		{
			TPlayer currentPlayer = currentPlayerContext.getElement();
			response.trace(request, "Checking current player %s for action permission.", currentPlayer);
	
			// check if the action is disallowed by the player.
			if (!currentPlayer.allowsAction(action))
			{
				response.trace(request, "Action is forbidden.");
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
					response.trace(request, "Action is forbidden.");
					callRoomActionForbiddenBlock(request, response, action, currentRoomContext);
					return true;
				}
			}
		}
		
		return false;
	}

	/**
	 * Calls the appropriate bad action blocks if they exist.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @return true if a block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callBadAction(TAMERequest request, TAMEResponse response, TAction action) throws TAMEInterrupt
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TWorldContext worldContext = moduleContext.getWorldContext();
		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
		
		// try incomplete on player.
		if (currentPlayerContext != null && callPlayerBadActionBlock(request, response, action, currentPlayerContext))
			return true;
	
		// try incomplete on world.
		return callWorldBadActionBlock(request, response, action, worldContext);
	}

	/**
	 * Calls the appropriate bad action block on the world if it exists.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @param worldContext the world context.
	 * @return true if a block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callWorldBadActionBlock(TAMERequest request, TAMEResponse response, TAction action, TWorldContext worldContext) throws TAMEInterrupt 
	{
		TWorld world = worldContext.getElement();
		
		Block blockToCall;
		
		if ((blockToCall = world.getBadActionTable().get(action.getIdentity())) != null)
		{
			response.trace(request, "Found specific bad action block on world.");
			callBlock(request, response, worldContext, blockToCall);
			return true;
		}
	
		if ((blockToCall = world.getBadActionBlock()) != null)
		{
			response.trace(request, "Found default bad action block on world.");
			callBlock(request, response, worldContext, blockToCall);
			return true;
		}
	
		response.trace(request, "No bad action block on world.");
		return false;
	}

	/**
	 * Calls the appropriate bad action block on a player if it exists.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @param context the player context.
	 * @return true if a block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callPlayerBadActionBlock(TAMERequest request, TAMEResponse response, TAction action, TPlayerContext context) throws TAMEInterrupt 
	{
		TPlayer player = context.getElement();
		
		Block blockToCall;
		
		if ((blockToCall = player.getBadActionTable().get(action.getIdentity())) != null)
		{
			response.trace(request, "Found specific bad action block on player.");
			callBlock(request, response, context, blockToCall);
			return true;
		}
	
		if ((blockToCall = player.getActionIncompleteBlock()) != null)
		{
			response.trace(request, "Found default bad action block on player.");
			callBlock(request, response, context, blockToCall);
			return true;
		}
	
		response.trace(request, "No bad action block on player.");
		return false;
	}

	/**
	 * Calls the appropriate action incomplete blocks if they exist.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @return true if a fail block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callActionIncomplete(TAMERequest request, TAMEResponse response, TAction action) throws TAMEInterrupt
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TWorldContext worldContext = moduleContext.getWorldContext();
		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
		
		// try incomplete on player.
		if (currentPlayerContext != null && callPlayerActionIncompleteBlock(request, response, action, currentPlayerContext))
			return true;
	
		// try incomplete on world.
		return callWorldActionIncompleteBlock(request, response, action, worldContext);
	}

	/**
	 * Calls the appropriate action incomplete block on the world if it exists.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @param worldContext the world context.
	 * @return true if a block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callWorldActionIncompleteBlock(TAMERequest request, TAMEResponse response, TAction action, TWorldContext worldContext) throws TAMEInterrupt 
	{
		TWorld world = worldContext.getElement();
		
		Block blockToCall;
		
		if ((blockToCall = world.getActionIncompleteTable().get(action.getIdentity())) != null)
		{
			response.trace(request, "Found specific action incomplete block on world.");
			callBlock(request, response, worldContext, blockToCall);
			return true;
		}
	
		if ((blockToCall = world.getActionIncompleteBlock()) != null)
		{
			response.trace(request, "Found default action incomplete block on world.");
			callBlock(request, response, worldContext, blockToCall);
			return true;
		}
	
		response.trace(request, "No action incomplete block on world.");
		return false;
	}

	/**
	 * Calls the appropriate action incomplete block on a player if it exists.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @param context the player context.
	 * @return true if a block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callPlayerActionIncompleteBlock(TAMERequest request, TAMEResponse response, TAction action, TPlayerContext context) throws TAMEInterrupt 
	{
		TPlayer player = context.getElement();
		
		Block blockToCall;
		
		if ((blockToCall = player.getActionIncompleteTable().get(action.getIdentity())) != null)
		{
			response.trace(request, "Found specific action incomplete block on player.");
			callBlock(request, response, context, blockToCall);
			return true;
		}
	
		if ((blockToCall = player.getActionIncompleteBlock()) != null)
		{
			response.trace(request, "Found default action incomplete block on player.");
			callBlock(request, response, context, blockToCall);
			return true;
		}
	
		response.trace(request, "No action incomplete block on player.");
		return false;
	}

	/**
	 * Calls the appropriate action fail blocks if they exist.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @return true if a fail block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callActionFailed(TAMERequest request, TAMEResponse response, TAction action) throws TAMEInterrupt
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TWorldContext worldContext = moduleContext.getWorldContext();
		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
		
		// try fail on player.
		if (currentPlayerContext != null && callPlayerActionFailBlock(request, response, action, currentPlayerContext))
			return true;
	
		// try fail on world.
		return callWorldActionFailBlock(request, response, action, worldContext);
	}

	/**
	 * Calls the appropriate action fail block on the world if it exists.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @param worldContext the world context.
	 * @return true if a fail block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callWorldActionFailBlock(TAMERequest request, TAMEResponse response, TAction action, TWorldContext worldContext) throws TAMEInterrupt 
	{
		TWorld world = worldContext.getElement();
		
		Block blockToCall;
		
		if ((blockToCall = world.getActionFailedTable().get(action.getIdentity())) != null)
		{
			response.trace(request, "Found specific action failure block on world.");
			callBlock(request, response, worldContext, blockToCall);
			return true;
		}

		if ((blockToCall = world.getActionFailedBlock()) != null)
		{
			response.trace(request, "Found default action failure block on world.");
			callBlock(request, response, worldContext, blockToCall);
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
		
		if ((blockToCall = player.getActionFailedTable().get(action.getIdentity())) != null)
		{
			response.trace(request, "Found specific action failure block on player.");
			callBlock(request, response, context, blockToCall);
			return true;
		}

		if ((blockToCall = player.getActionFailedBlock()) != null)
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

		if ((forbidBlock = player.getActionForbiddenTable().get(action.getIdentity())) != null)
		{
			response.trace(request, "Got specific forbid block on player.");
			callBlock(request, response, context, forbidBlock);
		}
		else if ((forbidBlock = player.getActionForbiddenBlock()) != null)
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
		
		if ((forbidBlock = room.getActionForbiddenTable().get(action.getIdentity())) != null)
		{
			response.trace(request, "Calling specific forbid block on room.");
			callBlock(request, response, context, forbidBlock);
		}
		else if ((forbidBlock = room.getActionForbiddenBlock()) != null)
		{
			response.trace(request, "Calling default forbid block on room.");
			callBlock(request, response, context, forbidBlock);
		}
		else
		{
			response.trace(request, "No forbid block on room to call. Sending error.");
			response.addCue(CUE_ERROR, "ACTION IS FORBIDDEN (make a better in-universe handler!).");
		}
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

	/**
	 * Interprets an action from the input line.
	 * @param moduleContext the module context.
	 * @param interpreterContext the TAMEInterpreterContext.
	 */
	private static void interpretAction(TAMEModuleContext moduleContext, TAMEInterpreterContext interpreterContext)
	{
		TAMEModule module = moduleContext.getModule();
		StringBuilder sb = new StringBuilder();
		int index = interpreterContext.getTokenOffset();
		String[] tokens = interpreterContext.getTokens();
		
		while (index < tokens.length)
		{
			if (sb.length() > 0)
				sb.append(' ');
			sb.append(tokens[index]);
			index++;
			TAction next = module.getActionByName(sb.toString());
			if (next != null)
			{
				interpreterContext.setAction(next);
				interpreterContext.setTokenOffset(index);
			}
		}
	}

	/**
	 * Interprets an action mode from the input line.
	 * @param action the action to use.
	 * @param interpreterContext the TAMEInterpreterContext.
	 */
	private static void interpretMode(TAction action, TAMEInterpreterContext interpreterContext)
	{
		StringBuilder sb = new StringBuilder();
		int index = interpreterContext.getTokenOffset();
		String[] tokens = interpreterContext.getTokens();
		
		while (index < tokens.length)
		{
			if (sb.length() > 0)
				sb.append(' ');
			sb.append(tokens[index]);
			index++;
			
			interpreterContext.setModeLookedUp(true);

			String next = sb.toString();
			if (action.getExtraStrings().contains(next))
			{
				interpreterContext.setMode(next);
				interpreterContext.setTokenOffset(index);
			}
		}
	}

	/**
	 * Interprets open target.
	 * @param interpreterContext the TAMEInterpreterContext.
	 */
	private static void interpretOpen(TAMEInterpreterContext interpreterContext)
	{
		StringBuilder sb = new StringBuilder();
		int index = interpreterContext.getTokenOffset();
		String[] tokens = interpreterContext.getTokens();
		
		while (index < tokens.length)
		{
			interpreterContext.setTargetLookedUp(true);
			if (sb.length() > 0)
				sb.append(' ');
			sb.append(tokens[index]);
			index++;
		}
		
		interpreterContext.setTarget(sb.length() > 0 ? sb.toString() : null);
		interpreterContext.setTokenOffset(index);
	}

	/**
	 * Interprets an action conjugate from the input line (like "with" or "on" or whatever).
	 * @param action the action to use.
	 * @param interpreterContext the TAMEInterpreterContext.
	 */
	private static boolean interpretConjugate(TAction action, TAMEInterpreterContext interpreterContext)
	{
		StringBuilder sb = new StringBuilder();
		int index = interpreterContext.getTokenOffset();
		String[] tokens = interpreterContext.getTokens();
		boolean out = false;
		
		while (index < tokens.length)
		{
			if (sb.length() > 0)
				sb.append(' ');
			sb.append(tokens[index]);
			index++;
			
			interpreterContext.setConjugateLookedUp(true);
			if (action.getExtraStrings().contains(sb.toString()))
			{
				interpreterContext.setTokenOffset(index);
				out = true;
			}
		}
	
		interpreterContext.setConjugateFound(out);
		return out;
	}

	/**
	 * Interprets the first object from the input line.
	 * This is context-sensitive, as its priority is to match objects on the current
	 * player's person, as well as in the current room. These checks are skipped if
	 * the player is null, or the current room is null.
	 * <p>
	 * The priority order is player inventory, then room contents.
	 * @param moduleContext the module context.
	 * @param interpreterContext the TAMEInterpreterContext.
	 */
	private static boolean interpretObject1(TAMEModuleContext moduleContext, TAMEInterpreterContext interpreterContext)
	{
		StringBuilder sb = new StringBuilder();
		int index = interpreterContext.getTokenOffset();
		String[] tokens = interpreterContext.getTokens();
		
		while (index < tokens.length)
		{
			if (sb.length() > 0)
				sb.append(' ');
			sb.append(tokens[index]);
			index++;

			interpreterContext.setObject1LookedUp(true);
			int out = findAccessibleObjectsByName(moduleContext, sb.toString(), interpreterContext.getObjects(), 0);
			if (out > 1)
			{
				interpreterContext.setObjectAmbiguous(true);
				interpreterContext.setObject1(null);
				interpreterContext.setTokenOffset(index);
			}
			else if (out > 0)
			{
				interpreterContext.setObjectAmbiguous(false);
				interpreterContext.setObject1(interpreterContext.getObjects()[0]);
				interpreterContext.setTokenOffset(index);
			}
		}
		
		return interpreterContext.getObject1() != null;
	}

	/**
	 * Interprets the second object from the input line.
	 * This is context-sensitive, as its priority is to match objects on the current
	 * player's person, as well as in the current room. These checks are skipped if
	 * the player is null, or the current room is null.
	 * <p>
	 * The priority order is player inventory, then room contents.
	 * @param moduleContext the module context.
	 * @param interpreterContext the TAMEInterpreterContext.
	 */
	private static boolean interpretObject2(TAMEModuleContext moduleContext, TAMEInterpreterContext interpreterContext)
	{
		StringBuilder sb = new StringBuilder();
		int index = interpreterContext.getTokenOffset();
		String[] tokens = interpreterContext.getTokens();
		
		while (index < tokens.length)
		{
			if (sb.length() > 0)
				sb.append(' ');
			sb.append(tokens[index]);
			index++;

			interpreterContext.setObject2LookedUp(true);
			int out = findAccessibleObjectsByName(moduleContext, sb.toString(), interpreterContext.getObjects(), 0);
			if (out > 1)
			{
				interpreterContext.setObjectAmbiguous(true);
				interpreterContext.setObject2(null);
				interpreterContext.setTokenOffset(index);
			}
			else if (out > 0)
			{
				interpreterContext.setObjectAmbiguous(false);
				interpreterContext.setObject2(interpreterContext.getObjects()[0]);
				interpreterContext.setTokenOffset(index);
			}
		}
		
		return interpreterContext.getObject2() != null;
	}
	
}
