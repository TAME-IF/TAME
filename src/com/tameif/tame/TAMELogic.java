/*******************************************************************************
 * Copyright (c) 2015-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import com.blackrook.commons.Common;
import com.blackrook.commons.CommonTokenizer;
import com.blackrook.commons.linkedlist.Queue;
import com.tameif.tame.element.ObjectContainer;
import com.tameif.tame.element.TAction;
import com.tameif.tame.element.TContainer;
import com.tameif.tame.element.TElement;
import com.tameif.tame.element.TObject;
import com.tameif.tame.element.TPlayer;
import com.tameif.tame.element.TRoom;
import com.tameif.tame.element.TWorld;
import com.tameif.tame.element.context.TElementContext;
import com.tameif.tame.element.context.TObjectContext;
import com.tameif.tame.element.context.TOwnershipMap;
import com.tameif.tame.element.context.TPlayerContext;
import com.tameif.tame.element.context.TRoomContext;
import com.tameif.tame.element.context.TWorldContext;
import com.tameif.tame.exception.ModuleException;
import com.tameif.tame.exception.ModuleExecutionException;
import com.tameif.tame.interrupt.EndInterrupt;
import com.tameif.tame.interrupt.FinishInterrupt;
import com.tameif.tame.interrupt.QuitInterrupt;
import com.tameif.tame.lang.ArithmeticOperator;
import com.tameif.tame.lang.Block;
import com.tameif.tame.lang.BlockEntry;
import com.tameif.tame.lang.BlockEntryType;
import com.tameif.tame.lang.FunctionEntry;
import com.tameif.tame.lang.Value;
import com.tameif.tame.lang.ValueHash;

/**
 * Rules class.
 * Governs most if not all logic in TAME.
 * @author Matthew Tropiano
 */
public final class TAMELogic implements TAMEConstants
{
	/** Version number. */
	private static String VERSION = null;
	
	/**
	 * Gets the embedded version string.
	 * @return the version string or "SNAPSHOT"
	 */
	public static String getVersion()
	{
		if (VERSION != null)
			return VERSION;
		
		InputStream in = null;
		try {
			in = Common.openResource("net/mtrop/tame/TAMEVersion.txt");
			if (in != null)
				VERSION = Common.getTextualContents(in, "UTF-8").trim();
		} catch (IOException e) {
			/* Do nothing. */
		} finally {
			Common.close(in);
		}
		
		return VERSION != null ? VERSION : "SNAPSHOT";
	}
	
	/**
	 * Handles context initialization, returning the response from it.
	 * This method must be called for newly-created contexts NOT LOADED FROM A PERSISTED CONTEXT STATE.
	 * @param moduleContext the module context.
	 * @param tracing if true, this does tracing.
	 * @return a TAMERequest a new request.
	 */
	public static TAMEResponse handleInit(TAMEModuleContext moduleContext, boolean tracing)
	{
		TAMERequest request = TAMERequest.create(moduleContext, tracing);
		TAMEResponse response = new TAMEResponse();

		response.setInterpretNanos(0L);

		// time this stuff.
		long nanos = System.nanoTime();

		try {
			initializeContext(request, response);
			processActionLoop(request, response, false, false, false);
		} catch (QuitInterrupt interrupt) {
			/* Do nothing. */
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
		TAMERequest request = TAMERequest.create(moduleContext, input, tracing);
		TAMEResponse response = new TAMEResponse();
		
		// time this stuff.
		long nanos;
		
		nanos = System.nanoTime();
		InterpreterContext interpreterContext = interpret(request.getModuleContext(), request.getInputMessage());
		response.setInterpretNanos(System.nanoTime() - nanos);
	
		nanos = System.nanoTime();
		
		try {
			boolean good = enqueueInterpretedAction(request, response, interpreterContext);
			processActionLoop(request, response, good, !good, true);
		} catch (TAMEFatalException exception) {
			response.addCue(CUE_FATAL, exception.getMessage());
		} catch (QuitInterrupt end) {
			// Catches quit.
		} catch (TAMEInterrupt interrupt) {
			response.addCue(CUE_ERROR, interrupt.getMessage());
		}
	
		response.setRequestNanos(System.nanoTime() - nanos);
		return response;
	}

	/**
	 * Handles a single action.
	 * Counts as a "successful command."
	 * @param moduleContext the module context.
	 * @param action the action to enqueue.
	 * @param tracing if true, this does tracing.
	 * @return a TAMERequest a new request.
	 */
	public static TAMEResponse handleAction(TAMEModuleContext moduleContext, TAMEAction action, boolean tracing)
	{
		TAMERequest request = TAMERequest.create(moduleContext, tracing);
		TAMEResponse response = new TAMEResponse();
		long nanos = System.nanoTime();
		
		try {
			request.addActionItem(action);
			processActionLoop(request, response, true, false, true);
		} catch (TAMEFatalException exception) {
			response.addCue(CUE_FATAL, exception.getMessage());
		} catch (QuitInterrupt end) {
			// Catches quit.
		} catch (TAMEInterrupt interrupt) {
			response.addCue(CUE_ERROR, interrupt.getMessage());
		}
	
		response.setRequestNanos(System.nanoTime() - nanos);
		return response;		
	}
	
	/**
	 * Processes a single action. 
	 * @param request the request object.
	 * @param response the response object.
	 * @param tameAction the action to process.
	 * @throws TAMEInterrupt if an uncaught interrupt occurs.
	 * @throws TAMEFatalException if something goes wrong during execution.
	 */
	public static void processAction(TAMERequest request, TAMEResponse response, TAMEAction tameAction) throws TAMEInterrupt 
	{
		try {
		
			TAction action = tameAction.getAction();
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
			
		} catch (FinishInterrupt finish) {
			// Catches finish.
		}
		
		request.checkStackClear();
	}

	/**
	 * Interprets the input on the request.
	 * Requires a context, as objects may need to be parsed.
	 * @param moduleContext the module context to use (for object availability).
	 * @param inputMessage the input message to interpret.
	 * @return a new interpreter context using the input.
	 */
	public static InterpreterContext interpret(TAMEModuleContext moduleContext, String inputMessage)
	{
		InterpreterContext interpreterContext = new InterpreterContext(inputMessage);
		
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
	 * Enqueues an action based on how it is interpreted.
	 * @param request the request object.
	 * @param response the response object.
	 * @param interpreterContext the interpreter context (left after interpretation).
	 * @return true if interpret was good and an action was enqueued, false if error.
	 * @throws TAMEInterrupt if an uncaught interrupt occurs.
	 * @throws TAMEFatalException if something goes wrong during execution.
	 */
	public static boolean enqueueInterpretedAction(TAMERequest request, TAMEResponse response, InterpreterContext interpreterContext) throws TAMEInterrupt 
	{
		TAction action = interpreterContext.getAction();
		if (action == null)
		{
			response.trace(request, "Performing unknown command.");
			if (!callUnknownCommand(request, response))
				response.addCue(CUE_ERROR, "UNKNOWN COMMAND (make a better in-universe handler!).");
			return false;
		}
		else
		{
			switch (action.getType())
			{
				default:
				case GENERAL:
				{
					if (action.isStrict() && interpreterContext.tokenOffset < interpreterContext.tokens.length)
					{
						response.trace(request, "Performing general action %s, but has extra tokens!", action);
						if (!callMalformedCommandBlock(request, response, action))
							response.addCue(CUE_ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
						return false;
					}
					else
					{
						request.addActionItem(TAMEAction.create(action));
						return true;
					}
				}
	
				case OPEN:
				{
					if (!interpreterContext.isTargetLookedUp())
					{
						response.trace(request, "Performing open action %s with no target (incomplete)!", action);
						if (!callIncompleteCommand(request, response, action))
							response.addCue(CUE_ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
						return false;
					}
					else
					{
						request.addActionItem(TAMEAction.create(action, interpreterContext.getTarget()));
						return true;
					}
				}
	
				case MODAL:
				{
					if (!interpreterContext.isModeLookedUp())
					{
						response.trace(request, "Performing modal action %s with no mode (incomplete)!", action);
						if (!callIncompleteCommand(request, response, action))
							response.addCue(CUE_ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (interpreterContext.getMode() == null)
					{
						response.trace(request, "Performing modal action %s with an unknown mode!", action);
						if (!callMalformedCommandBlock(request, response, action))
							response.addCue(CUE_ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (action.isStrict() && interpreterContext.tokenOffset < interpreterContext.tokens.length)
					{
						response.trace(request, "Performing modal action %s, but has extra tokens!", action);
						if (!callMalformedCommandBlock(request, response, action))
							response.addCue(CUE_ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
						return false;
					}
					else 
					{
						request.addActionItem(TAMEAction.create(action, interpreterContext.getMode()));
						return true;
					}
				}
	
				case TRANSITIVE:
				{
					if (interpreterContext.isObjectAmbiguous())
					{
						response.trace(request, "Ambiguous command for action %s.", action);
						if (!callAmbiguousCommand(request, response, action))
							response.addCue(CUE_ERROR, "AMBIGUOUS COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (!interpreterContext.isObject1LookedUp())
					{
						response.trace(request, "Performing transitive action %s with no object (incomplete)!", action);
						if (!callIncompleteCommand(request, response, action))
							response.addCue(CUE_ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (interpreterContext.getObject1() == null)
					{
						response.trace(request, "Performing transitive action %s with an unknown object!", action);
						if (!callMalformedCommandBlock(request, response, action))
							response.addCue(CUE_ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (action.isStrict() && interpreterContext.tokenOffset < interpreterContext.tokens.length)
					{
						response.trace(request, "Performing transitive action %s, but has extra tokens!", action);
						if (!callMalformedCommandBlock(request, response, action))
							response.addCue(CUE_ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
						return false;
					}
					else 
					{
						request.addActionItem(TAMEAction.create(action, interpreterContext.getObject1()));
						return true;
					}
				}
	
				case DITRANSITIVE:
				{
					if (interpreterContext.isObjectAmbiguous())
					{
						response.trace(request, "Ambiguous command for action %s.", action);
						if (!callAmbiguousCommand(request, response, action))
							response.addCue(CUE_ERROR, "AMBIGUOUS COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (!interpreterContext.isObject1LookedUp())
					{
						response.trace(request, "Performing ditransitive action %s with no first object (incomplete)!", action);
						if (!callIncompleteCommand(request, response, action))
							response.addCue(CUE_ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (interpreterContext.getObject1() == null)
					{
						response.trace(request, "Performing ditransitive action %s with an unknown first object!", action);
						if (!callMalformedCommandBlock(request, response, action))
							response.addCue(CUE_ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (!interpreterContext.isConjugateLookedUp())
					{
						response.trace(request, "Performing ditransitive action %s as a transitive one...", action);
						request.addActionItem(TAMEAction.create(action, interpreterContext.getObject1()));
						return true;
					}
					else if (!interpreterContext.isConjugateFound())
					{
						response.trace(request, "Performing ditransitive action %s with an unknown conjugate!", action);
						if (!callMalformedCommandBlock(request, response, action))
							response.addCue(CUE_ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (!interpreterContext.isObject2LookedUp())
					{
						response.trace(request, "Performing ditransitive action %s with no second object (incomplete)!", action);
						if (!callIncompleteCommand(request, response, action))
							response.addCue(CUE_ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (interpreterContext.getObject2() == null)
					{
						response.trace(request, "Performing ditransitive action %s with an unknown second object!", action);
						if (!callMalformedCommandBlock(request, response, action))
							response.addCue(CUE_ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (action.isStrict() && interpreterContext.tokenOffset < interpreterContext.tokens.length)
					{
						response.trace(request, "Performing ditransitive action %s, but has extra tokens!", action);
						if (!callMalformedCommandBlock(request, response, action))
							response.addCue(CUE_ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
						return false;
					}
					else 
					{
						request.addActionItem(TAMEAction.create(action, interpreterContext.getObject1(), interpreterContext.getObject2()));
						return true;
					}
				}
			}
		}
	}

	/**
	 * Performs the necessary tasks for calling an object block.
	 * Ensures that the block is called cleanly.
	 * @param request the request object.
	 * @param response the response object.
	 * @param context the context that the block is executed through.
	 * @param block the block to execute.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void callBlock(TAMERequest request, TAMEResponse response, TElementContext<?> context, Block block) throws TAMEInterrupt
	{
		ValueHash blockLocal = new ValueHash();
		callBlock(request, response, context, block, false, blockLocal);
	}
	
	/**
	 * Performs the necessary tasks for calling an object block.
	 * Ensures that the block is called cleanly.
	 * @param request the request object.
	 * @param response the response object.
	 * @param context the context that the block is executed through.
	 * @param block the block to execute.
	 * @param functionBlock if true, this is a function call (which is slightly different).
	 * @param blockLocal the local value hash to use on invoke.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void callBlock(TAMERequest request, TAMEResponse response, TElementContext<?> context, Block block, boolean functionBlock, ValueHash blockLocal) throws TAMEInterrupt
	{
		response.trace(request, "Pushing %s...", context);
		request.pushContext(context);

		try {
			block.execute(request, response, blockLocal);
		} catch (EndInterrupt t) {
			/* Do nothing. */
		} catch (Throwable t) {
			throw t;
		} finally {
			response.trace(request, "Popping %s...", context);
			request.popContext();
		}
		
		if (!functionBlock)
		{
			// Stack should be clear after a main block call. If not, BIG PROBLEMS!
			request.checkStackClear();
		}
	}
	
	/**
	 * Calls a function from an arbitrary context, using the bound element as a lineage search point.
	 * @param request the request object.
	 * @param response the response object.
	 * @param functionName the function to execute.
	 * @param originContext the origin context (and then element).
	 * @throws TAMEInterrupt if an interrupt occurs.
	 * @return the return value from the function call. if no return, returns false.
	 */
	public static Value callElementFunction(TAMERequest request, TAMEResponse response, String functionName, TElementContext<?> originContext) throws TAMEInterrupt
	{
		TElement element = originContext.getElement();
		FunctionEntry entry = element.resolveFunction(functionName);
		if (entry == null)
			throw new ModuleException("No such function ("+functionName+") in lineage of element " + element);

		response.trace(request, "Calling function \"%s\"...", functionName);
		ValueHash blockLocal = new ValueHash();
		String[] args = entry.getArguments();
		for (int i = args.length - 1; i >= 0; i--)
		{
			Value localValue = request.popValue();
			response.trace(request, "Setting local variable \"%s\" to \"%s\"", args[i], localValue);
			blockLocal.put(args[i], localValue);
		}

		response.incrementAndCheckFunctionDepth(request.getModuleContext().getFunctionDepthMax());
		callBlock(request, response, originContext, entry.getBlock(), true, blockLocal);
		response.decrementFunctionDepth();
		if (blockLocal.containsKey(RETURN_VARIABLE))
			return blockLocal.get(RETURN_VARIABLE);
		else
			return Value.create(false);
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
		if (ownershipMap.checkElementHasObject(world, object))
		{
			response.trace(request, "Found.");
			return true;
		}
	
		response.trace(request, "Check %s for %s...", player, object);
		if (ownershipMap.checkElementHasObject(player, object))
		{
			response.trace(request, "Found.");
			return true;
		}
		
		TRoom currentRoom = ownershipMap.getCurrentRoom(player);
		
		if (currentRoom != null)
		{
			response.trace(request, "Check %s for %s...", currentRoom, object);
			if (currentRoom != null && ownershipMap.checkElementHasObject(currentRoom, object))
			{
				response.trace(request, "Found.");
				return true;
			}	
		}
		
		response.trace(request, "Not found.");
		return false;
	}

	/**
	 * Performs an arithmetic function on the stack.
	 * @param request the request context.
	 * @param response the response object.
	 * @param functionType the function type.
	 * @throws ModuleExecutionException if functionType is less than 0 or greater than or equal to <code>ArithmeticOperator.VALUES.length</code>. 
	 */
	public static void doArithmeticStackFunction(TAMERequest request, TAMEResponse response, int functionType)
	{
		if (functionType < 0 || functionType >= ArithmeticOperator.VALUES.length)
			throw new ModuleExecutionException("Expected arithmetic function type, got illegal value "+functionType+".");
	
		ArithmeticOperator operator =  ArithmeticOperator.VALUES[functionType];
		response.trace(request, "Function is %s", operator.name());
		
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
		response.trace(request, "Setting current player to %s.", nextPlayer);
		moduleContext.getOwnershipMap().setCurrentPlayer(nextPlayer);
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
		response.trace(request, "Popping top room from %s.", player);
		moduleContext.getOwnershipMap().popRoomFromPlayer(player);
	}

	/**
	 * Attempts to perform a room stack push for a player.
	 * @param request the request object.
	 * @param response the response object.
	 * @param player the player to push a room context onto.
	 * @param nextRoom the room to push.
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
			doRoomPop(request, response, player);
	
		// push new room on the stack and call focus.
		doRoomPush(request, response, player, nextRoom);
	}

	/**
	 * Attempts to perform an element browse.
	 * @param request the request object.
	 * @param response the response object.
	 * @param element the element to browse.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void doBrowse(TAMERequest request, TAMEResponse response, ObjectContainer element) throws TAMEInterrupt 
	{
		doBrowse(request, response, element, null);
	}
	
	/**
	 * Attempts to perform an element browse.
	 * @param request the request object.
	 * @param response the response object.
	 * @param element the element to browse.
	 * @param tag the tag name to search for, if any (null for no tag filter).
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static void doBrowse(TAMERequest request, TAMEResponse response, ObjectContainer element, String tag) throws TAMEInterrupt 
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TOwnershipMap ownership = moduleContext.getOwnershipMap();
		BlockEntry blockEntry = null; 
				
		if (element instanceof TContainer)
			blockEntry = BlockEntry.create(BlockEntryType.ONCONTAINERBROWSE);
		else if (element instanceof TRoom)
			blockEntry = BlockEntry.create(BlockEntryType.ONROOMBROWSE);
		else if (element instanceof TPlayer)
			blockEntry = BlockEntry.create(BlockEntryType.ONPLAYERBROWSE);
		else if (element instanceof TWorld)
			blockEntry = BlockEntry.create(BlockEntryType.ONWORLDBROWSE);
		else
			return;
	
		response.trace(request, "Start browse %s.", element);
		
		for (TObject object : ownership.getObjectsOwnedByElement(element))
		{
			TObjectContext objectContext = moduleContext.getObjectContext(object);
			
			if (tag != null && !moduleContext.getOwnershipMap().checkObjectHasTag(object, tag))
				continue;
	
			// find via inheritance.
			response.trace(request, "Check %s for browse block.", object);
			Block block = object.resolveBlock(blockEntry);
			if (block != null)
			{
				response.trace(request, "Found! Calling %s block.", blockEntry.getEntryType().name());
				callBlock(request, response, objectContext, block);
			}
			
		}
	
	}

	/**
	 * Initializes a newly-created context by executing each initialization block on each object.
	 * Order is Containers, Objects, Rooms, Players, and the World.
	 * @param request the request object containing the module context.
	 * @param response the response object.
	 * @throws TAMEInterrupt if an interrupt is thrown.
	 * @throws TAMEFatalException if something goes wrong during execution.
	 */
	private static void initializeContext(TAMERequest request, TAMEResponse response) throws TAMEInterrupt
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		
		response.trace(request, "Starting init...");
	
		callInitOnContexts(request, response, moduleContext.getContainerContextIterator());
		callInitOnContexts(request, response, moduleContext.getObjectContextIterator());
		callInitOnContexts(request, response, moduleContext.getRoomContextIterator());
		callInitOnContexts(request, response, moduleContext.getPlayerContextIterator());
		callInitBlock(request, response, moduleContext.getWorldContext());
		callStartBlock(request, response);
	}

	/**
	 * Does an action loop: this keeps processing queued actions until there is nothing left to process.
	 * @param request the request object.
	 * @param response the response object.
	 * @param afterSuccessfulCommand if true, executes the "after successful command" block.
	 * @param afterFailedCommand if true, executes the "after failed command" block.
	 * @param afterEveryCommand if true, executes the "after every command" block.
	 * @throws TAMEInterrupt if an uncaught interrupt occurs.
	 * @throws TAMEFatalException if something goes wrong during execution.
	 */
	private static void processActionLoop(TAMERequest request, TAMEResponse response, boolean afterSuccessfulCommand, boolean afterFailedCommand, boolean afterEveryCommand) throws TAMEInterrupt 
	{
		doAllActionItems(request, response);
		if (afterSuccessfulCommand)
		{
			doAfterSuccessfulCommand(request, response);
			doAllActionItems(request, response);
		}
		if (afterFailedCommand)
		{
			doAfterFailedCommand(request, response);
			doAllActionItems(request, response);
		}
		if (afterEveryCommand)
		{
			doAfterEveryCommand(request, response);
			doAllActionItems(request, response);
		}		
	}

	/**
	 * Does an action loop: this keeps processing queued actions until there is nothing left to process.
	 * @param request the request object.
	 * @param response the response object.
	 * @throws TAMEInterrupt if an uncaught interrupt occurs.
	 * @throws TAMEFatalException if something goes wrong during execution.
	 */
	private static void doAllActionItems(TAMERequest request, TAMEResponse response) throws TAMEInterrupt
	{
		while (request.hasActionItems())
			processAction(request, response, request.nextActionItem());
	}
	
	/**
	 * Attempts to call the after successful command block on the world.
	 * @param request the request object.
	 * @param response the response object.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 * @throws TAMEFatalException if something goes wrong during execution.
	 */
	private static void doAfterSuccessfulCommand(TAMERequest request, TAMEResponse response) throws TAMEInterrupt
	{
		response.trace(request, "Finding \"after successful command\" request block...");

		TAMEModuleContext moduleContext = request.getModuleContext();
		TWorldContext worldContext = moduleContext.getWorldContext();
		Block blockToCall = null;
		
		// get block on world.
		if ((blockToCall = worldContext.getElement().resolveBlock(BlockEntry.create(BlockEntryType.AFTERSUCCESSFULCOMMAND))) != null)
		{
			response.trace(request, "Found \"after successful command\" block on world.");
			callBlock(request, response, worldContext, blockToCall);
		}
		else
			response.trace(request, "No \"after successful command\" block to call.");
	}
	
	/**
	 * Attempts to call the after failed command block on the world.
	 * @param request the request object.
	 * @param response the response object.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 * @throws TAMEFatalException if something goes wrong during execution.
	 */
	private static void doAfterFailedCommand(TAMERequest request, TAMEResponse response) throws TAMEInterrupt
	{
		response.trace(request, "Finding \"after failed command\" request block...");

		TAMEModuleContext moduleContext = request.getModuleContext();
		TWorldContext worldContext = moduleContext.getWorldContext();
		Block blockToCall = null;
		
		// get block on world.
		if ((blockToCall = worldContext.getElement().resolveBlock(BlockEntry.create(BlockEntryType.AFTERFAILEDCOMMAND))) != null)
		{
			response.trace(request, "Found \"after failed command\" block on world.");
			callBlock(request, response, worldContext, blockToCall);
		}
		else
			response.trace(request, "No \"after failed command\" block to call.");
	}
	
	/**
	 * Attempts to call the after every command block on the world.
	 * @param request the request object.
	 * @param response the response object.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 * @throws TAMEFatalException if something goes wrong during execution.
	 */
	private static void doAfterEveryCommand(TAMERequest request, TAMEResponse response) throws TAMEInterrupt
	{
		response.trace(request, "Finding \"after every command\" request block...");

		TAMEModuleContext moduleContext = request.getModuleContext();
		TWorldContext worldContext = moduleContext.getWorldContext();
		Block blockToCall = null;
		
		// get block on world.
		if ((blockToCall = worldContext.getElement().resolveBlock(BlockEntry.create(BlockEntryType.AFTEREVERYCOMMAND))) != null)
		{
			response.trace(request, "Found \"after every command\" block on world.");
			callBlock(request, response, worldContext, blockToCall);
		}
		else
			response.trace(request, "No \"after every command\" block to call.");
	}
	
	/**
	 * Attempts to perform a general action.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action that is being called.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static void doActionGeneral(TAMERequest request, TAMEResponse response, TAction action) throws TAMEInterrupt
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
	private static void doActionOpen(TAMERequest request, TAMEResponse response, TAction action, String openTarget) throws TAMEInterrupt
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		response.trace(request, "Performing general/open action %s", action);

		if (callCheckActionForbidden(request, response, action))
			return;

		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
		Block blockToCall = null;
		
		BlockEntry blockEntry = BlockEntry.create(BlockEntryType.ONACTION, Value.createAction(action.getIdentity()));
		
		if (currentPlayerContext != null)
		{
			TPlayer currentPlayer = currentPlayerContext.getElement();

			// try current room.
			TRoomContext currentRoomContext = moduleContext.getCurrentRoomContext();
			if (currentRoomContext != null)
			{
				TRoom currentRoom = currentRoomContext.getElement();
				
				// get general action on room.
				if ((blockToCall = currentRoom.resolveBlock(blockEntry)) != null)
				{
					response.trace(request, "Found general action block on room.");
					if (openTarget != null)
					{
						// just get the first one.
						for (String variableName : action.getExtraStrings())
						{
							Value target = Value.create(openTarget);
							// set locals
							ValueHash blockLocal = new ValueHash();
							response.trace(request, "Setting local variable \"%s\" to \"%s\"", variableName, target);
							blockLocal.put(variableName, target);
							callBlock(request, response, currentRoomContext, blockToCall, false, blockLocal);
							break;
						}
					}
					else
						callBlock(request, response, currentRoomContext, blockToCall);
					return;
				}
				
				response.trace(request, "No general action block on room.");
			}
			
			// get general action on player.
			if ((blockToCall = currentPlayer.resolveBlock(blockEntry)) != null)
			{
				response.trace(request, "Found general action block on player.");
				if (openTarget != null)
				{
					// just get the first one.
					for (String variableName : action.getExtraStrings())
					{
						Value target = Value.create(openTarget);
						// set locals
						ValueHash blockLocal = new ValueHash();
						response.trace(request, "Setting local variable \"%s\" to \"%s\"", variableName, target);
						blockLocal.put(variableName, target);
						callBlock(request, response, currentPlayerContext, blockToCall, false, blockLocal);
						break;
					}
				}
				else
					callBlock(request, response, currentPlayerContext, blockToCall);
				return;
			}
			
			response.trace(request, "No general action block on player.");
		}
		
		TWorldContext worldContext = moduleContext.getWorldContext();
		TWorld world = worldContext.getElement();
		
		// get general action on world.
		if ((blockToCall = world.resolveBlock(blockEntry)) != null)
		{
			response.trace(request, "Found general action block on world.");
			if (openTarget != null)
			{
				// just get the first one.
				for (String variableName : action.getExtraStrings())
				{
					Value target = Value.create(openTarget);
					// set locals
					ValueHash blockLocal = new ValueHash();
					response.trace(request, "Setting local variable \"%s\" to \"%s\"", variableName, target);
					blockLocal.put(variableName, target);
					callBlock(request, response, worldContext, blockToCall, false, blockLocal);
					break;
				}
			}
			else
				callBlock(request, response, worldContext, blockToCall);
			return;
		}
		
		if (!callActionUnhandled(request, response, action))
			response.addCue(CUE_ERROR, "ACTION UNHANDLED (make a better in-universe handler!).");
	}

	/**
	 * Attempts to perform a modal action.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action that is being called.
	 * @param mode the mode to process.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static void doActionModal(TAMERequest request, TAMEResponse response, TAction action, String mode) throws TAMEInterrupt
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		response.trace(request, "Performing modal action %s, \"%s\"", action, mode);

		if (callCheckActionForbidden(request, response, action))
			return;

		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
		Block blockToCall = null;

		BlockEntry blockEntry = BlockEntry.create(BlockEntryType.ONMODALACTION, Value.createAction(action.getIdentity()), Value.create(mode));
		
		if (currentPlayerContext != null)
		{
			TPlayer currentPlayer = currentPlayerContext.getElement();

			// try current room.
			TRoomContext currentRoomContext = moduleContext.getCurrentRoomContext();
			if (currentRoomContext != null)
			{
				TRoom currentRoom = currentRoomContext.getElement();

				// get modal action on room.
				if ((blockToCall = currentRoom.resolveBlock(blockEntry)) != null)
				{
					response.trace(request, "Found modal action block on room.");
					callBlock(request, response, currentRoomContext, blockToCall);
					return;
				}
				
				response.trace(request, "No modal action block on room.");
			}
			
			// get modal action on player.
			if ((blockToCall = currentPlayer.resolveBlock(blockEntry)) != null)
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
		if ((blockToCall = world.resolveBlock(blockEntry)) != null)
		{
			response.trace(request, "Found modal action block on world.");
			callBlock(request, response, worldContext, blockToCall);
			return;
		}

		if (!callActionUnhandled(request, response, action))
			response.addCue(CUE_ERROR, "ACTION UNHANDLED (make a better in-universe handler!).");
	}
	
	/**
	 * Attempts to perform a transitive action.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action that is being called.
	 * @param object the target object for the action.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static void doActionTransitive(TAMERequest request, TAMEResponse response, TAction action, TObject object) throws TAMEInterrupt
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		response.trace(request, "Performing transitive action %s on %s", action, object);

		if (callCheckActionForbidden(request, response, action))
			return;

		TObjectContext currentObjectContext = moduleContext.getObjectContext(object);
		Block blockToCall = null;
		
		BlockEntry blockEntry = BlockEntry.create(BlockEntryType.ONACTION, Value.createAction(action.getIdentity()));
		
		// call action on object.
		if ((blockToCall = object.resolveBlock(blockEntry)) != null)
		{
			response.trace(request, "Found action block on object.");
			callBlock(request, response, currentObjectContext, blockToCall);
			return;
		}
		
		if (!callActionUnhandled(request, response, action))
			response.addCue(CUE_ERROR, "ACTION UNHANDLED (make a better in-universe handler!).");
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
	private static void doActionDitransitive(TAMERequest request, TAMEResponse response, TAction action, TObject object1, TObject object2) throws TAMEInterrupt
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		response.trace(request, "Performing ditransitive action %s on %s with %s", action, object1, object2);

		if (callCheckActionForbidden(request, response, action))
			return;

		TObjectContext currentObject1Context = moduleContext.getObjectContext(object1);
		TObjectContext currentObject2Context = moduleContext.getObjectContext(object2);
		Block blockToCall = null;
		
		BlockEntry blockEntry1, blockEntry2;
		Value actionValue = Value.createAction(action.getIdentity());

		// call action on each object. one or both need to succeed for no failure.
		blockEntry1 = BlockEntry.create(BlockEntryType.ONACTIONWITH, actionValue, Value.createObject(object1.getIdentity()));
		blockEntry2 = BlockEntry.create(BlockEntryType.ONACTIONWITH, actionValue, Value.createObject(object2.getIdentity()));
		
		boolean call12 = !action.isStrict() || !action.isReversed();
		boolean call21 = !action.isStrict() || action.isReversed();
		
		if (call12 && (blockToCall = object1.resolveBlock(blockEntry2)) != null)
		{
			response.trace(request, "Found \"action with\" block in object %s lineage with %s.", object1, object2);
			callBlock(request, response, currentObject1Context, blockToCall);
			return;
		}
		else
			response.trace(request, "No matching \"action with\" block in object %s lineage with %s.", object1, object2);

		if (call21 && (blockToCall = object2.resolveBlock(blockEntry1)) != null)
		{
			response.trace(request, "Found \"action with\" block in object %s lineage with %s.", object2, object1);
			callBlock(request, response, currentObject2Context, blockToCall);
			return;
		}
		else
			response.trace(request, "No matching \"action with\" block in object %s lineage with %s.", object2, object1);

		// call action with ancestor on each object. one or both need to succeed for no failure.
		if (call12 && doActionDitransitiveAncestorSearch(request, response, actionValue, object1, object2))
			return;
		if (call21 && doActionDitransitiveAncestorSearch(request, response, actionValue, object2, object1))
			return;
		
		// attempt action with other on both objects.
		BlockEntry actionOtherEntry = BlockEntry.create(BlockEntryType.ONACTIONWITHOTHER, actionValue);
		if (call12 && (blockToCall = object1.resolveBlock(actionOtherEntry)) != null)
		{
			response.trace(request, "Found \"action with other\" block in object %s lineage.", object1);
			callBlock(request, response, currentObject1Context, blockToCall);
			return;
		}
		else
			response.trace(request, "No matching \"action with other\" block in object %s lineage.", object1);

		if (call21 && (blockToCall = object2.resolveBlock(actionOtherEntry)) != null)
		{
			response.trace(request, "Found \"action with other\" block in object %s lineage.", object2);
			callBlock(request, response, currentObject2Context, blockToCall);
			return;
		}
		else
			response.trace(request, "No matching \"action with other\" block in object %s lineage.", object2);
		
		// if we STILL can't do it...
		response.trace(request, "No blocks called in ditransitive action call.");
		if (!callActionUnhandled(request, response, action))
			response.addCue(CUE_ERROR, "ACTION UNHANDLED (make a better in-universe handler!).");
	}
	
	/**
	 * Attempts to perform a ditransitive action for the ancestor search.
	 * @param request the request object.
	 * @param response the response object.
	 * @param actionValue the action that is being called (value).
	 * @param object the object to call the block on.
	 * @param start the object to start the search from.
	 * @return true if a block was found an called.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean doActionDitransitiveAncestorSearch(TAMERequest request, TAMEResponse response, Value actionValue, TObject object, TObject start) throws TAMEInterrupt
	{
		Block blockToCall;
		BlockEntry blockEntry;
		TAMEModuleContext moduleContext = request.getModuleContext();
		TObject ancestor = (TObject)start.getParent();
		TObjectContext objectContext = moduleContext.getObjectContext(object);

		while (ancestor != null)
		{
			blockEntry = BlockEntry.create(BlockEntryType.ONACTIONWITHANCESTOR, actionValue, Value.createObject(ancestor.getIdentity()));
			if ((blockToCall = object.resolveBlock(blockEntry)) != null)
			{
				response.trace(request, "Found \"action with ancestor\" block in object %s lineage - ancestor is %s.", object, ancestor);
				callBlock(request, response, objectContext, blockToCall);
				return true;
			}
			ancestor = (TObject)ancestor.getParent();
		}
		
		response.trace(request, "No matching \"action with ancestor\" block in object %s lineage.", object);
		return false;
	}
	
	/**
	 * Checks and calls the action forbidden blocks.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @return true if an action is forbidden and steps were taken to call a forbidden block, or false otherwise.
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
				if (!callPlayerActionForbiddenBlock(request, response, action, currentPlayerContext))
				{
					response.addCue(CUE_ERROR, "ACTION IS FORBIDDEN (make a better in-universe handler!).");
					return true;
				}
	
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
					if (!callRoomActionForbiddenBlock(request, response, action, currentPlayerContext))
					{
						response.addCue(CUE_ERROR, "ACTION IS FORBIDDEN IN THIS ROOM (make a better in-universe handler!).");
						return true;
					}
				}
			}
		}
		
		return false;
	}

	/**
	 * Attempts to call the unknown command blocks.
	 * @param request the request object.
	 * @param response the response object.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callUnknownCommand(TAMERequest request, TAMEResponse response) throws TAMEInterrupt
	{
		response.trace(request, "Finding unknown command blocks...");
	
		TAMEModuleContext moduleContext = request.getModuleContext();
		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
		Block blockToCall = null;
		
		BlockEntry blockEntry = BlockEntry.create(BlockEntryType.ONUNKNOWNCOMMAND);
		
		if (currentPlayerContext != null)
		{
			TPlayer currentPlayer = currentPlayerContext.getElement();
			response.trace(request, "For current player %s...", currentPlayer);
	
			// get block on player.
			// find via inheritance.
			blockToCall = currentPlayer.resolveBlock(blockEntry);
			if (blockToCall != null)
			{
				response.trace(request, "Found unknown command block on player.");
				callBlock(request, response, currentPlayerContext, blockToCall);
				return true;
			}
	
		}
	
		TWorldContext worldContext = moduleContext.getWorldContext();
		
		// get block on world.
		if ((blockToCall = worldContext.getElement().resolveBlock(blockEntry)) != null)
		{
			response.trace(request, "Found unknown command block on world.");
			callBlock(request, response, worldContext, blockToCall);
			return true;
		}
	
		return false;
	}

	/**
	 * Attempts to call the ambiguous command blocks.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action used.
	 * @return true if a block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callAmbiguousCommand(TAMERequest request, TAMEResponse response, TAction action) throws TAMEInterrupt
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
	
		if (currentPlayerContext != null && callPlayerAmbiguousCommandBlock(request, response, action, currentPlayerContext))
			return true;
	
		TWorldContext worldContext = moduleContext.getWorldContext();
		
		return callWorldAmbiguousCommandBlock(request, response, action, worldContext);
	}

	/**
	 * Calls the appropriate malformed command blocks if they exist.
	 * Malformed commands are commands with mismatched conjugates, unknown modal parts, or unknown object references. 
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @return true if a block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callMalformedCommandBlock(TAMERequest request, TAMEResponse response, TAction action) throws TAMEInterrupt
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TWorldContext worldContext = moduleContext.getWorldContext();
		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
		
		// try bad action on player.
		if (currentPlayerContext != null && callPlayerMalformedCommandBlock(request, response, action, currentPlayerContext))
			return true;
	
		// try bad action on world.
		return callWorldMalformedCommandBlock(request, response, action, worldContext);
	}

	/**
	 * Calls the appropriate incomplete command blocks if they exist.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @return true if a block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callIncompleteCommand(TAMERequest request, TAMEResponse response, TAction action) throws TAMEInterrupt
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
		
		// try incomplete on player.
		if (currentPlayerContext != null && callPlayerIncompleteCommandBlock(request, response, action, currentPlayerContext))
			return true;
	
		TWorldContext worldContext = moduleContext.getWorldContext();

		// try incomplete on world.
		return callWorldIncompleteCommandBlock(request, response, action, worldContext);
	}

	/**
	 * Calls the appropriate action unhandled blocks if they exist.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @return true if an unhandled block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callActionUnhandled(TAMERequest request, TAMEResponse response, TAction action) throws TAMEInterrupt
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
		
		// try fail on player.
		if (currentPlayerContext != null && callPlayerActionUnhandledBlock(request, response, action, currentPlayerContext))
			return true;
	
		TWorldContext worldContext = moduleContext.getWorldContext();

		// try fail on world.
		return callWorldActionUnhandledBlock(request, response, action, worldContext);
	}

	/**
	 * Calls the appropriate ambiguous command blocks if they exist on the world.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @param worldContext the world context.
	 * @return true if a block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callWorldAmbiguousCommandBlock(TAMERequest request, TAMEResponse response, TAction action, TWorldContext worldContext) throws TAMEInterrupt
	{
		Block blockToCall = null;
		
		// get specific block on world.
		if ((blockToCall = worldContext.getElement().resolveBlock(BlockEntry.create(BlockEntryType.ONAMBIGUOUSCOMMAND, Value.createAction(action.getIdentity())))) != null)
		{
			response.trace(request, "Found specific ambiguous command block on world for action %s.", action.getIdentity());
			callBlock(request, response, worldContext, blockToCall);
			return true;
		}
	
		// get block on world.
		if ((blockToCall = worldContext.getElement().resolveBlock(BlockEntry.create(BlockEntryType.ONAMBIGUOUSCOMMAND))) != null)
		{
			response.trace(request, "Found default ambiguous command block on world.");
			callBlock(request, response, worldContext, blockToCall);
			return true;
		}
	
		return false;
	}

	/**
	 * Calls the appropriate ambiguous command blocks if they exist on a player.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @param playerContext the player context.
	 * @return true if a block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callPlayerAmbiguousCommandBlock(TAMERequest request, TAMEResponse response, TAction action, TPlayerContext playerContext) throws TAMEInterrupt 
	{
		TPlayer currentPlayer = playerContext.getElement();
	
		Block blockToCall = null;
		
		// get specific block on player.
		if ((blockToCall = currentPlayer.resolveBlock(BlockEntry.create(BlockEntryType.ONAMBIGUOUSCOMMAND, Value.createAction(action.getIdentity())))) != null)
		{
			response.trace(request, "Found specific ambiguous command block in player %s lineage for action %s.", currentPlayer.getIdentity(), action.getIdentity());
			callBlock(request, response, playerContext, blockToCall);
			return true;
		}
	
		// get block on player.
		if ((blockToCall = currentPlayer.resolveBlock(BlockEntry.create(BlockEntryType.ONAMBIGUOUSCOMMAND))) != null)
		{
			response.trace(request, "Found default ambiguous command block in player %s lineage.", currentPlayer.getIdentity());
			callBlock(request, response, playerContext, blockToCall);
			return true;
		}
		
		response.trace(request, "No ambiguous command block on player.");
		return false;
	}

	/**
	 * Calls the appropriate malformed command block on the world if it exists.
	 * Malformed commands are commands with mismatched conjugates, unknown modal parts, or unknown object references. 
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @param worldContext the world context.
	 * @return true if a block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callWorldMalformedCommandBlock(TAMERequest request, TAMEResponse response, TAction action, TWorldContext worldContext) throws TAMEInterrupt 
	{
		TWorld world = worldContext.getElement();
		
		Block blockToCall;
		
		if ((blockToCall = world.resolveBlock(BlockEntry.create(BlockEntryType.ONMALFORMEDCOMMAND, Value.createAction(action.getIdentity())))) != null)
		{
			response.trace(request, "Found specific malformed command block on world with action %s.", action.getIdentity());
			callBlock(request, response, worldContext, blockToCall);
			return true;
		}
	
		if ((blockToCall = world.resolveBlock(BlockEntry.create(BlockEntryType.ONMALFORMEDCOMMAND))) != null)
		{
			response.trace(request, "Found default malformed command block on world.");
			callBlock(request, response, worldContext, blockToCall);
			return true;
		}
	
		response.trace(request, "No malformed command block on world.");
		return false;
	}

	/**
	 * Calls the appropriate malformed command block on a player if it exists.
	 * Malformed commands are commands with mismatched conjugates, unknown modal parts, or unknown object references. 
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @param context the player context.
	 * @return true if a block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callPlayerMalformedCommandBlock(TAMERequest request, TAMEResponse response, TAction action, TPlayerContext context) throws TAMEInterrupt 
	{
		TPlayer player = context.getElement();
		
		Block blockToCall;
		
		if ((blockToCall = player.resolveBlock(BlockEntry.create(BlockEntryType.ONMALFORMEDCOMMAND, Value.createAction(action.getIdentity())))) != null)
		{
			response.trace(request, "Found specific malformed command block in player %s lineage, action %s.", player.getIdentity(), action.getIdentity());
			callBlock(request, response, context, blockToCall);
			return true;
		}
	
		if ((blockToCall = player.resolveBlock(BlockEntry.create(BlockEntryType.ONMALFORMEDCOMMAND))) != null)
		{
			response.trace(request, "Found default malformed command block on player %s.", player.getIdentity());
			callBlock(request, response, context, blockToCall);
			return true;
		}
	
		response.trace(request, "No malformed command block on player.");
		return false;
	}

	/**
	 * Calls the appropriate action forbidden block on a player.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @param context the player context.
	 * @return true if handled by this block, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callPlayerActionForbiddenBlock(TAMERequest request, TAMEResponse response, TAction action, TPlayerContext context) throws TAMEInterrupt 
	{
		TPlayer player = context.getElement();
	
		// get forbid block.
		Block forbidBlock = null;
	
		if ((forbidBlock = player.resolveBlock(BlockEntry.create(BlockEntryType.ONFORBIDDENACTION, Value.createAction(action.getIdentity())))) != null)
		{
			response.trace(request, "Got specific forbid block in player %s lineage, action %s", player.getIdentity(), action.getIdentity());
			callBlock(request, response, context, forbidBlock);
			return true;
		}
		
		if ((forbidBlock = player.resolveBlock(BlockEntry.create(BlockEntryType.ONFORBIDDENACTION))) != null)
		{
			response.trace(request, "Got default forbid block in player %s lineage.", player.getIdentity());
			callBlock(request, response, context, forbidBlock);
			return true;
		}
		
		response.trace(request, "No forbid block on player to call.");
		return false;
	}

	/**
	 * Calls the appropriate room action forbidden block on a player.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @param context the room context.
	 * @return true if handled by this block, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callRoomActionForbiddenBlock(TAMERequest request, TAMEResponse response, TAction action, TPlayerContext context) throws TAMEInterrupt 
	{
		TPlayer player = context.getElement();
		
		// get forbid block.
		Block forbidBlock = null;
		
		if ((forbidBlock = player.resolveBlock(BlockEntry.create(BlockEntryType.ONROOMFORBIDDENACTION, Value.createAction(action.getIdentity())))) != null)
		{
			response.trace(request, "Calling specific room forbid block in player %s lineage, action %s.", player.getIdentity(), action.getIdentity());
			callBlock(request, response, context, forbidBlock);
			return true;
		}

		if ((forbidBlock = player.resolveBlock(BlockEntry.create(BlockEntryType.ONROOMFORBIDDENACTION))) != null)
		{
			response.trace(request, "Calling default room forbid block in player %s lineage.", player.getIdentity());
			callBlock(request, response, context, forbidBlock);
			return true;
		}

		response.trace(request, "No room forbid block on player.");
		return false;
	}

	/**
	 * Calls the appropriate incomplete command block on the world if it exists.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @param worldContext the world context.
	 * @return true if a block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callWorldIncompleteCommandBlock(TAMERequest request, TAMEResponse response, TAction action, TWorldContext worldContext) throws TAMEInterrupt 
	{
		TWorld world = worldContext.getElement();
		
		Block blockToCall;
		
		if ((blockToCall = world.resolveBlock(BlockEntry.create(BlockEntryType.ONINCOMPLETECOMMAND, Value.createAction(action.getIdentity())))) != null)
		{
			response.trace(request, "Found specific incomplete command block on world, action %s.", action.getIdentity());
			callBlock(request, response, worldContext, blockToCall);
			return true;
		}
	
		if ((blockToCall = world.resolveBlock(BlockEntry.create(BlockEntryType.ONINCOMPLETECOMMAND))) != null)
		{
			response.trace(request, "Found default incomplete command block on world.");
			callBlock(request, response, worldContext, blockToCall);
			return true;
		}
	
		response.trace(request, "No incomplete command block on world.");
		return false;
	}

	/**
	 * Calls the appropriate incomplete command block on a player if it exists.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @param context the player context.
	 * @return true if a block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callPlayerIncompleteCommandBlock(TAMERequest request, TAMEResponse response, TAction action, TPlayerContext context) throws TAMEInterrupt 
	{
		TPlayer player = context.getElement();
		
		Block blockToCall;
		
		if ((blockToCall = player.resolveBlock(BlockEntry.create(BlockEntryType.ONINCOMPLETECOMMAND, Value.createAction(action.getIdentity())))) != null)
		{
			response.trace(request, "Found specific incomplete command block in player %s lineage, action %s.", player.getIdentity(), action.getIdentity());
			callBlock(request, response, context, blockToCall);
			return true;
		}
	
		if ((blockToCall = player.resolveBlock(BlockEntry.create(BlockEntryType.ONINCOMPLETECOMMAND))) != null)
		{
			response.trace(request, "Found default incomplete command block in player %s lineage.", player.getIdentity());
			callBlock(request, response, context, blockToCall);
			return true;
		}
	
		response.trace(request, "No incomplete command block on player.");
		return false;
	}

	/**
	 * Calls the appropriate action unhandled block on the world if it exists.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @param worldContext the world context.
	 * @return true if a fail block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callWorldActionUnhandledBlock(TAMERequest request, TAMEResponse response, TAction action, TWorldContext worldContext) throws TAMEInterrupt 
	{
		TWorld world = worldContext.getElement();
		
		Block blockToCall;
		
		if ((blockToCall = world.resolveBlock(BlockEntry.create(BlockEntryType.ONUNHANDLEDACTION, Value.createAction(action.getIdentity())))) != null)
		{
			response.trace(request, "Found specific action unhandled block on world, action %s.", action.getIdentity());
			callBlock(request, response, worldContext, blockToCall);
			return true;
		}

		if ((blockToCall = world.resolveBlock(BlockEntry.create(BlockEntryType.ONUNHANDLEDACTION))) != null)
		{
			response.trace(request, "Found default action unhandled block on world.");
			callBlock(request, response, worldContext, blockToCall);
			return true;
		}

		response.trace(request, "No action unhandled block on world.");
		return false;
	}

	/**
	 * Calls the appropriate action unhandled block on a player if it exists.
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action attempted.
	 * @param context the player context.
	 * @return true if a fail block was called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean callPlayerActionUnhandledBlock(TAMERequest request, TAMEResponse response, TAction action, TPlayerContext context) throws TAMEInterrupt 
	{
		TPlayer player = context.getElement();
		
		Block blockToCall;
		
		if ((blockToCall = player.resolveBlock(BlockEntry.create(BlockEntryType.ONUNHANDLEDACTION, Value.createAction(action.getIdentity())))) != null)
		{
			response.trace(request, "Found specific action unhandled block in player %s lineage, action %s.", player.getIdentity(), action.getIdentity());
			callBlock(request, response, context, blockToCall);
			return true;
		}

		if ((blockToCall = player.resolveBlock(BlockEntry.create(BlockEntryType.ONUNHANDLEDACTION))) != null)
		{
			response.trace(request, "Found default action unhandled block in player %s lineage.", player.getIdentity());
			callBlock(request, response, context, blockToCall);
			return true;
		}

		response.trace(request, "No action unhandled block on player.");
		return false;
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
		response.trace(request, "Attempt init from %s.", context);
		TElement element = context.getElement();

		Block initBlock = element.resolveBlock(BlockEntry.create(BlockEntryType.INIT));
		if (initBlock != null)
		{
			response.trace(request, "Calling init block from %s.", context);
			callBlock(request, response, context, initBlock);
		}
		else
		{
			response.trace(request, "No init block.");
		}
	}

	// Call after module init block on the world.
	private static void callStartBlock(TAMERequest request, TAMEResponse response) throws TAMEInterrupt 
	{
		TWorldContext worldContext = request.getModuleContext().getWorldContext();
		response.trace(request, "Attempt to call start block on world.");
		TWorld world = worldContext.getElement();

		Block initBlock = world.resolveBlock(BlockEntry.create(BlockEntryType.START));
		if (initBlock != null)
		{
			response.trace(request, "Calling start block from %s.", worldContext);
			callBlock(request, response, worldContext, initBlock);
		}
		else
		{
			response.trace(request, "No start block on world.");
		}
	}

	/**
	 * Interprets an action from the input line.
	 * @param moduleContext the module context.
	 * @param interpreterContext the TAMEInterpreterContext.
	 */
	private static void interpretAction(TAMEModuleContext moduleContext, InterpreterContext interpreterContext)
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
	private static void interpretMode(TAction action, InterpreterContext interpreterContext)
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
			if (action.containsExtraString(next))
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
	private static void interpretOpen(InterpreterContext interpreterContext)
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
	private static boolean interpretConjugate(TAction action, InterpreterContext interpreterContext)
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
			if (action.containsExtraString(sb.toString()))
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
	private static boolean interpretObject1(TAMEModuleContext moduleContext, InterpreterContext interpreterContext)
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
			int out = moduleContext.getAccessibleObjectsByName(sb.toString(), interpreterContext.getObjects(), 0);
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
	private static boolean interpretObject2(TAMEModuleContext moduleContext, InterpreterContext interpreterContext)
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
			int out = moduleContext.getAccessibleObjectsByName(sb.toString(), interpreterContext.getObjects(), 0);
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
	
	/**
	 * Context used when some input is getting parsed/interpreted.
	 */
	private static class InterpreterContext 
	{
		private String[] tokens;
		private int tokenOffset;
		private TObject[] objects;
		private TAction action;
		private boolean modeLookedUp;
		private String mode;
		private boolean targetLookedUp;
		private String target;
		private boolean conjugateLookedUp;
		private boolean conjugateFound;
		private boolean object1LookedUp;
		private TObject object1;
		private boolean object2LookedUp;
		private TObject object2;
		private boolean objectAmbiguous;
		
		private InterpreterContext(String input)
		{
			input = input.replaceAll("\\s+", " ").trim();
			
			Queue<String> tokenQueue = new Queue<>();
			CommonTokenizer ct = new CommonTokenizer(input);
			while (ct.hasMoreTokens())
				tokenQueue.add(ct.nextToken());
			this.tokens = new String[tokenQueue.size()];
			tokenQueue.toArray(tokens);
			
			this.tokenOffset = 0;
			this.objects = new TObject[2];
			this.action = null;
			this.modeLookedUp = false;
			this.mode = null;
			this.targetLookedUp = false;
			this.target = null;
			this.object1LookedUp = false;
			this.object1 = null;
			this.object2LookedUp = false;
			this.object2 = null;
			this.objectAmbiguous = false;
		}

		private void setTokenOffset(int tokenOffset) 
		{
			this.tokenOffset = tokenOffset;
		}

		private void setAction(TAction action) 
		{
			this.action = action;
		}

		private void setMode(String mode) 
		{
			this.mode = mode;
		}

		private void setConjugateFound(boolean conjugateFound) 
		{
			this.conjugateFound = conjugateFound;
		}
		
		private void setTarget(String target) 
		{
			this.target = target;
		}

		private void setObject1(TObject object1) 
		{
			this.object1 = object1;
		}

		private void setObject2(TObject object2) 
		{
			this.object2 = object2;
		}

		private void setModeLookedUp(boolean modeLookedUp) 
		{
			this.modeLookedUp = modeLookedUp;
		}

		private void setConjugateLookedUp(boolean conjugateLookedUp) 
		{
			this.conjugateLookedUp = conjugateLookedUp;
		}
		
		private void setTargetLookedUp(boolean targetLookedUp) 
		{
			this.targetLookedUp = targetLookedUp;
		}

		private void setObject1LookedUp(boolean object1LookedUp) 
		{
			this.object1LookedUp = object1LookedUp;
		}

		private void setObject2LookedUp(boolean object2LookedUp) 
		{
			this.object2LookedUp = object2LookedUp;
		}

		private void setObjectAmbiguous(boolean objectAmbiguous) 
		{
			this.objectAmbiguous = objectAmbiguous;
		}

		private String[] getTokens() 
		{
			return tokens;
		}

		private int getTokenOffset() 
		{
			return tokenOffset;
		}

		private TObject[] getObjects() 
		{
			return objects;
		}

		private TAction getAction() 
		{
			return action;
		}

		private String getMode() 
		{
			return mode;
		}

		private boolean isConjugateFound() 
		{
			return conjugateFound;
		}
		
		private String getTarget() 
		{
			return target;
		}

		private TObject getObject1() 
		{
			return object1;
		}

		private TObject getObject2() 
		{
			return object2;
		}

		private boolean isModeLookedUp() 
		{
			return modeLookedUp;
		}

		private boolean isConjugateLookedUp() 
		{
			return conjugateLookedUp;
		}
		
		private boolean isTargetLookedUp() 
		{
			return targetLookedUp;
		}

		private boolean isObject1LookedUp() 
		{
			return object1LookedUp;
		}

		private boolean isObject2LookedUp() 
		{
			return object2LookedUp;
		}

		private boolean isObjectAmbiguous() 
		{
			return objectAmbiguous;
		}
		
	}

}
