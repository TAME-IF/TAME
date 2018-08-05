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
import com.tameif.tame.exception.UnexpectedValueTypeException;
import com.tameif.tame.interrupt.EndInterrupt;
import com.tameif.tame.interrupt.FinishInterrupt;
import com.tameif.tame.interrupt.QuitInterrupt;
import com.tameif.tame.lang.ArithmeticOperator;
import com.tameif.tame.lang.Block;
import com.tameif.tame.lang.BlockEntry;
import com.tameif.tame.lang.BlockEntryType;
import com.tameif.tame.lang.FunctionEntry;
import com.tameif.tame.lang.TraceType;
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
			in = Common.openResource("com/tameif/tame/TAMEVersion.txt");
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
	 * @param tracing if true, this does full tracing.
	 * @return a TAMERequest a new request.
	 */
	public static TAMEResponse handleInit(TAMEModuleContext moduleContext, boolean tracing)
	{
		return handleInit(moduleContext, TraceType.VALUES);
	}
	
	/**
	 * Handles context initialization, returning the response from it.
	 * This method must be called for newly-created contexts NOT LOADED FROM A PERSISTED CONTEXT STATE.
	 * @param moduleContext the module context.
	 * @param traceTypes output trace cues for each request.
	 * @return a TAMERequest a new request.
	 */
	public static TAMEResponse handleInit(TAMEModuleContext moduleContext, TraceType ... traceTypes)
	{
		TAMERequest request = TAMERequest.create(moduleContext, traceTypes);
		TAMEResponse response = new TAMEResponse();

		response.setInterpretNanos(0L);

		// time this stuff.
		long nanos = System.nanoTime();

		try {
			initializeContext(request, response);
			processCommandLoop(request, response, false, false, false);
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
	 * @param tracing if true, this does full tracing.
	 * @return a TAMERequest a new request.
	 */
	public static TAMEResponse handleRequest(TAMEModuleContext moduleContext, String input, boolean tracing)
	{
		return handleRequest(moduleContext, input, TraceType.VALUES);
	}
	
	/**
	 * Handles a full request.
	 * @param moduleContext the module context.
	 * @param input the client input query.
	 * @param traceTypes output trace cues for each request.
	 * @return a TAMERequest a new request.
	 */
	public static TAMEResponse handleRequest(TAMEModuleContext moduleContext, String input, TraceType ... traceTypes)
	{
		TAMERequest request = TAMERequest.create(moduleContext, input, traceTypes);
		TAMEResponse response = new TAMEResponse();
		
		// time this stuff.
		long nanos;
		
		nanos = System.nanoTime();
		InterpreterContext interpreterContext = interpret(request, response);
		response.setInterpretNanos(System.nanoTime() - nanos);
	
		nanos = System.nanoTime();
		
		try {
			boolean good = enqueueInterpretedAction(request, response, interpreterContext);
			processCommandLoop(request, response, good, !good, true);
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
	 * @param command the command to enqueue.
	 * @param tracing if true, this does tracing.
	 * @return a TAMERequest a new request.
	 */
	public static TAMEResponse handleAction(TAMEModuleContext moduleContext, TAMECommand command, boolean tracing)
	{
		TAMERequest request = TAMERequest.create(moduleContext, tracing);
		TAMEResponse response = new TAMEResponse();
		long nanos = System.nanoTime();
		
		try {
			request.addCommand(command);
			processCommandLoop(request, response, true, false, true);
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
	 * @param command the action to process.
	 * @throws TAMEInterrupt if an uncaught interrupt occurs.
	 * @throws TAMEFatalException if something goes wrong during execution.
	 */
	public static void processCommand(TAMERequest request, TAMEResponse response, TAMECommand command) throws TAMEInterrupt 
	{
		try {
		
			TAction action = command.getAction();
			switch (action.getType())
			{
				default:
				case GENERAL:
					doActionGeneral(request, response, action);
					break;
				case OPEN:
					doActionOpen(request, response, action, command.getTarget());
					break;
				case MODAL:
					doActionModal(request, response, action, command.getTarget());
					break;
				case TRANSITIVE:
					doActionTransitive(request, response, action, command.getObject1());
					break;
				case DITRANSITIVE:
					if (command.getObject2() == null)
						doActionTransitive(request, response, action, command.getObject1());
					else
						doActionDitransitive(request, response, action, command.getObject1(), command.getObject2());
					break;
			}
			
		} catch (FinishInterrupt finish) {
			// Catches finish.
		}
		
		request.checkStackClear();
	}

	/**
	 * Tokenizes the input string into tokens based on module settings.
	 * @param moduleContext the module context to use (for object availability).
	 * @param inputMessage the input message to tokenize.
	 * @return the tokens to parse.
	 */
	public static String[] tokenizeInput(TAMEModuleContext moduleContext, String inputMessage)
	{
		return inputMessage.trim().split("\\s+");
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
			response.trace(request, TraceType.ENTRY, "UNKNOWN ACTION");
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
						response.trace(request, TraceType.INTERPRETER, "STRICT GENERAL ACTION %s: Extra Tokens (MALFORMED)", action.getIdentity());
						if (!callMalformedCommandBlock(request, response, action))
							response.addCue(CUE_ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
						return false;
					}
					else
					{
						request.addCommand(TAMECommand.create(action));
						return true;
					}
				}
	
				case OPEN:
				{
					if (!interpreterContext.isTargetLookedUp())
					{
						response.trace(request, TraceType.INTERPRETER, "OPEN ACTION %s: No Target (INCOMPLETE)", action.getIdentity());
						if (!callIncompleteCommand(request, response, action))
							response.addCue(CUE_ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
						return false;
					}
					else
					{
						request.addCommand(TAMECommand.create(action, interpreterContext.getTarget()));
						return true;
					}
				}
	
				case MODAL:
				{
					if (!interpreterContext.isModeLookedUp())
					{
						response.trace(request, TraceType.INTERPRETER, "MODAL ACTION %s: No Mode (INCOMPLETE)", action.getIdentity());
						if (!callIncompleteCommand(request, response, action))
							response.addCue(CUE_ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (interpreterContext.getMode() == null)
					{
						response.trace(request, TraceType.INTERPRETER, "MODAL ACTION %s: Unknown Mode (MALFORMED)", action.getIdentity());
						if (!callMalformedCommandBlock(request, response, action))
							response.addCue(CUE_ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (action.isStrict() && interpreterContext.tokenOffset < interpreterContext.tokens.length)
					{
						response.trace(request, TraceType.INTERPRETER, "STRICT MODAL ACTION %s: Extra Tokens (MALFORMED)", action.getIdentity());
						if (!callMalformedCommandBlock(request, response, action))
							response.addCue(CUE_ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
						return false;
					}
					else 
					{
						request.addCommand(TAMECommand.create(action, interpreterContext.getMode()));
						return true;
					}
				}
	
				case TRANSITIVE:
				{
					if (interpreterContext.isObjectAmbiguous())
					{
						response.trace(request, TraceType.INTERPRETER, "TRANSITIVE ACTION %s (AMBIGUOUS)", action.getIdentity());
						if (!callAmbiguousCommand(request, response, action))
							response.addCue(CUE_ERROR, "AMBIGUOUS COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (!interpreterContext.isObject1LookedUp())
					{
						response.trace(request, TraceType.INTERPRETER, "TRANSITIVE ACTION %s: No Object (INCOMPLETE)", action.getIdentity());
						if (!callIncompleteCommand(request, response, action))
							response.addCue(CUE_ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (interpreterContext.getObject1() == null)
					{
						response.trace(request, TraceType.INTERPRETER, "TRANSITIVE ACTION %s: Unknown Object (MALFORMED)", action.getIdentity());
						if (!callMalformedCommandBlock(request, response, action))
							response.addCue(CUE_ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (action.isStrict() && interpreterContext.tokenOffset < interpreterContext.tokens.length)
					{
						response.trace(request, TraceType.INTERPRETER, "STRICT TRANSITIVE ACTION %s: Extra Tokens (MALFORMED)", action.getIdentity());
						if (!callMalformedCommandBlock(request, response, action))
							response.addCue(CUE_ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
						return false;
					}
					else 
					{
						request.addCommand(TAMECommand.create(action, interpreterContext.getObject1()));
						return true;
					}
				}
	
				case DITRANSITIVE:
				{
					if (interpreterContext.isObjectAmbiguous())
					{
						response.trace(request, TraceType.INTERPRETER, "DITRANSITIVE ACTION %s (AMBIGUOUS)", action.getIdentity());
						if (!callAmbiguousCommand(request, response, action))
							response.addCue(CUE_ERROR, "AMBIGUOUS COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (!interpreterContext.isObject1LookedUp())
					{
						response.trace(request, TraceType.INTERPRETER, "DITRANSITIVE ACTION %s: No First Object (INCOMPLETE)", action.getIdentity());
						if (!callIncompleteCommand(request, response, action))
							response.addCue(CUE_ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (interpreterContext.getObject1() == null)
					{
						response.trace(request, TraceType.INTERPRETER, "DITRANSITIVE ACTION %s: Unknown First Object (MALFORMED)", action.getIdentity());
						if (!callMalformedCommandBlock(request, response, action))
							response.addCue(CUE_ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (!interpreterContext.isConjugateLookedUp())
					{
						if (action.isStrict())
						{
							response.trace(request, TraceType.INTERPRETER, "STRICT DITRANSITIVE ACTION %s: No Conjunction (INCOMPLETE)", action.getIdentity());
							if (!callIncompleteCommand(request, response, action))
								response.addCue(CUE_ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
							return false;
						}
						else
						{
							response.trace(request, TraceType.INTERPRETER, "DITRANSITIVE ACTION %s: No Conjunction, Process TRANSITIVE", action.getIdentity());
							request.addCommand(TAMECommand.create(action, interpreterContext.getObject1()));
							return true;
						}
					}
					else if (!interpreterContext.isConjugateFound())
					{
						response.trace(request, TraceType.INTERPRETER, "DITRANSITIVE ACTION %s: Unknown Conjunction (MALFORMED)", action.getIdentity());
						if (!callMalformedCommandBlock(request, response, action))
							response.addCue(CUE_ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (!interpreterContext.isObject2LookedUp())
					{
						response.trace(request, TraceType.INTERPRETER, "DITRANSITIVE ACTION %s: No Second Object (INCOMPLETE)", action.getIdentity());
						if (!callIncompleteCommand(request, response, action))
							response.addCue(CUE_ERROR, "INCOMPLETE COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (interpreterContext.getObject2() == null)
					{
						response.trace(request, TraceType.INTERPRETER, "DITRANSITIVE ACTION %s: Unknown Second Object (MALFORMED)", action.getIdentity());
						if (!callMalformedCommandBlock(request, response, action))
							response.addCue(CUE_ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
						return false;
					}
					else if (action.isStrict() && interpreterContext.tokenOffset < interpreterContext.tokens.length)
					{
						response.trace(request, TraceType.INTERPRETER, "STRICT DITRANSITIVE ACTION %s: Extra Tokens (MALFORMED)", action.getIdentity());
						if (!callMalformedCommandBlock(request, response, action))
							response.addCue(CUE_ERROR, "MALFORMED COMMAND (make a better in-universe handler!).");
						return false;
					}
					else 
					{
						request.addCommand(TAMECommand.create(action, interpreterContext.getObject1(), interpreterContext.getObject2()));
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
		response.trace(request, TraceType.CONTEXT, "PUSH %s", context);
		request.pushContext(context);

		try {
			block.execute(request, response, blockLocal);
		} catch (EndInterrupt t) {
			/* Do nothing. */
		} catch (Throwable t) {
			throw t;
		} finally {
			response.trace(request, TraceType.CONTEXT, "POP %s", context);
			request.popContext();
		}
		
		if (!functionBlock)
		{
			// Stack should be clear after a main block call. If not, BIG PROBLEMS!
			request.checkStackClear();
		}
	}
	
	/**
	 * Attempts to resolve and call an element block.
	 * @param request the request.
	 * @param response the response.
	 * @param context the element context.
	 * @param blockEntry the block entry to resolve.
	 * @return true if called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static boolean callElementBlock(TAMERequest request, TAMEResponse response, TElementContext<?> context, BlockEntry blockEntry) throws TAMEInterrupt 
	{
		Block blockToCall;
		TElement element = context.getElement();
		response.trace(request, TraceType.ENTRY, "RESOLVE %s.%s", element.getIdentity(), blockEntry.toFriendlyString());
		if ((blockToCall = element.resolveBlock(blockEntry)) != null)
		{
			response.trace(request, TraceType.ENTRY, "CALL %s.%s", element.getIdentity(), blockEntry.toFriendlyString());
			callBlock(request, response, context, blockToCall);
			return true;
		}
		
		return false;
	}

	/**
	 * Attempts to resolve and call an element block.
	 * @param request the request.
	 * @param response the response.
	 * @param context the element context.
	 * @param blockEntry the block entry to resolve.
	 * @param openTarget the value of the open action.
	 * @param locals the extra strings for the open action (only the first one is used).
	 * @return true if called, false if not.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public static boolean callElementBlock(TAMERequest request, TAMEResponse response, TElementContext<?> context, BlockEntry blockEntry, String openTarget, Iterable<String> locals) throws TAMEInterrupt 
	{
		Block blockToCall;
		TElement element = context.getElement();
		response.trace(request, TraceType.ENTRY, "RESOLVE %s.%s", element.getIdentity(), blockEntry.toFriendlyString());
		if ((blockToCall = element.resolveBlock(blockEntry)) != null)
		{
			response.trace(request, TraceType.ENTRY, "CALL %s.%s", element.getIdentity(), blockEntry.toFriendlyString());
			
			// just get the first local.
			for (String variableName : locals)
			{
				Value target = Value.create(openTarget);
				// set locals
				ValueHash blockLocal = new ValueHash();
				response.trace(request, TraceType.VALUE, "SET LOCAL %s %s", variableName, target);
				blockLocal.put(variableName, target);
				callBlock(request, response, context, blockToCall, false, blockLocal);
				break;
			}
			
			callBlock(request, response, context, blockToCall);
			return true;
		}
		
		return false;
	}

	/**
	 * Calls a function from an arbitrary context, using the bound element as a lineage search point.
	 * @param request the request object.
	 * @param response the response object.
	 * @param functionName the function to execute.
	 * @param originContext the origin context (and then element).
	 * @throws ModuleException if the function does not exist in the lineage of the target element.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 * @return the return value from the function call. if no return, returns false.
	 */
	public static Value callElementFunction(TAMERequest request, TAMEResponse response, String functionName, TElementContext<?> originContext) throws TAMEInterrupt
	{
		TElement element = originContext.getElement();
		FunctionEntry entry = element.resolveFunction(functionName);
		if (entry == null)
			throw new ModuleException("No such function ("+functionName+") in lineage of element " + element);

		response.trace(request, TraceType.FUNCTION, "CALL %s", functionName);
		ValueHash blockLocal = new ValueHash();
		String[] args = entry.getArguments();
		for (int i = args.length - 1; i >= 0; i--)
		{
			Value localValue = request.popValue();
			response.trace(request, TraceType.VALUE, "SET LOCAL %s %s", args[i], localValue.toString());
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
		
		if (ownershipMap.checkElementHasObject(world, object))
			return true;
	
		if (ownershipMap.checkElementHasObject(player, object))
			return true;
		
		TRoom currentRoom = ownershipMap.getCurrentRoom(player);
		if (currentRoom != null && ownershipMap.checkElementHasObject(currentRoom, object))
			return true;
		
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
		response.trace(request, TraceType.INTERNAL, "Operator is %s", operator.name());
		
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
		response.trace(request, TraceType.CONTEXT, "CURRENT PLAYER: %s", nextPlayer);
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
		TRoom popped = moduleContext.getOwnershipMap().popRoomFromPlayer(player);
		if (popped != null)
			response.trace(request, TraceType.CONTEXT, "POP ROOM %s FROM PLAYER %s", popped.getIdentity(), player.getIdentity());
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
		response.trace(request, TraceType.CONTEXT, "PUSH ROOM %s ON PLAYER %s", nextRoom.getIdentity(), player.getIdentity());
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

		for (TObject object : ownership.getObjectsOwnedByElement(element))
		{
			TObjectContext objectContext = moduleContext.getObjectContext(object);
			
			if (tag != null && !moduleContext.getOwnershipMap().checkObjectHasTag(object, tag))
				continue;
	
			doBrowseBlockSearch(request, response, (TElement)element, objectContext);
		}
	
	}
	
	/**
	 * Searches for a valid OnBrowse block on an object and executes it.
	 * @param request the request object.
	 * @param response the response object.
	 * @param element the element being browsed.
	 * @param objectContext the object context to call the block on (and search for a block on).
	 * @return true if a block was found and called on this object, false if not.
	 */
	private static boolean doBrowseBlockSearch(TAMERequest request, TAMEResponse response, TElement element, TObjectContext objectContext) throws TAMEInterrupt
	{
		// special case for world - no hierarchy.
		if (element instanceof TWorld)
			return callElementBlock(request, response, objectContext, BlockEntry.create(BlockEntryType.ONWORLDBROWSE));
		
		TElement next = element;
		while (next != null)
		{
			BlockEntry blockEntry;
			if (next instanceof TContainer)
				blockEntry = BlockEntry.create(BlockEntryType.ONELEMENTBROWSE, Value.createContainer(next.getIdentity()));
			else if (next instanceof TRoom)
				blockEntry = BlockEntry.create(BlockEntryType.ONELEMENTBROWSE, Value.createRoom(next.getIdentity()));
			else if (next instanceof TPlayer)
				blockEntry = BlockEntry.create(BlockEntryType.ONELEMENTBROWSE, Value.createPlayer(next.getIdentity()));
			else
				throw new UnexpectedValueTypeException("Bad object container type in hierarchy.");

			if (callElementBlock(request, response, objectContext, blockEntry))
				return true;
			
			next = next.getParent();
		}
		
		if (element instanceof TContainer)
			return callElementBlock(request, response, objectContext, BlockEntry.create(BlockEntryType.ONCONTAINERBROWSE));
		else if (element instanceof TRoom)
			return callElementBlock(request, response, objectContext, BlockEntry.create(BlockEntryType.ONROOMBROWSE));
		else if (element instanceof TPlayer)
			return callElementBlock(request, response, objectContext, BlockEntry.create(BlockEntryType.ONPLAYERBROWSE));
		else
			throw new UnexpectedValueTypeException("Bad object container type in hierarchy.");
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
		try {
			callInitOnContexts(request, response, moduleContext.getContainerContextIterator());
			callInitOnContexts(request, response, moduleContext.getObjectContextIterator());
			callInitOnContexts(request, response, moduleContext.getRoomContextIterator());
			callInitOnContexts(request, response, moduleContext.getPlayerContextIterator());
			callElementBlock(request, response, moduleContext.getWorldContext(), BlockEntry.create(BlockEntryType.INIT));
			callElementBlock(request, response, moduleContext.getWorldContext(), BlockEntry.create(BlockEntryType.START));
		} catch (FinishInterrupt interrupt) {
			// Eat finish.
		} catch (Throwable t) {
			throw t;
		}
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
	private static void processCommandLoop(TAMERequest request, TAMEResponse response, boolean afterSuccessfulCommand, boolean afterFailedCommand, boolean afterEveryCommand) throws TAMEInterrupt 
	{
		doAllCommands(request, response);
		if (afterSuccessfulCommand)
		{
			callElementBlock(request, response, request.getModuleContext().getWorldContext(), BlockEntry.create(BlockEntryType.AFTERSUCCESSFULCOMMAND));
			doAllCommands(request, response);
		}
		if (afterFailedCommand)
		{
			callElementBlock(request, response, request.getModuleContext().getWorldContext(), BlockEntry.create(BlockEntryType.AFTERFAILEDCOMMAND));
			doAllCommands(request, response);
		}
		if (afterEveryCommand)
		{
			callElementBlock(request, response, request.getModuleContext().getWorldContext(), BlockEntry.create(BlockEntryType.AFTEREVERYCOMMAND));
			doAllCommands(request, response);
		}		
	}

	/**
	 * Does an action loop: this keeps processing queued commands until there is nothing left to process.
	 * @param request the request object.
	 * @param response the response object.
	 * @throws TAMEInterrupt if an uncaught interrupt occurs.
	 * @throws TAMEFatalException if something goes wrong during execution.
	 */
	private static void doAllCommands(TAMERequest request, TAMEResponse response) throws TAMEInterrupt
	{
		while (request.hasCommands())
			processCommand(request, response, request.nextCommand());
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
		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
		BlockEntry blockEntry = BlockEntry.create(BlockEntryType.ONACTION, Value.createAction(action.getIdentity()));
		
		if (currentPlayerContext != null)
		{
			// try current room.
			TRoomContext currentRoomContext = moduleContext.getCurrentRoomContext();
			if (currentRoomContext != null)
			{
				if (openTarget != null)
				{
					if (callElementBlock(request, response, currentRoomContext, blockEntry, openTarget, action.getExtraStrings()))
						return;
				}
				else if (callElementBlock(request, response, currentRoomContext, blockEntry))
					return;
			}
			
			if (openTarget != null)
			{
				if (callElementBlock(request, response, currentPlayerContext, blockEntry, openTarget, action.getExtraStrings()))
					return;
			}
			else if (callElementBlock(request, response, currentPlayerContext, blockEntry))
				return;
		}
		
		TWorldContext worldContext = moduleContext.getWorldContext();
		if (openTarget != null)
		{
			if (callElementBlock(request, response, worldContext, blockEntry, openTarget, action.getExtraStrings()))
				return;
		}
		else if (callElementBlock(request, response, worldContext, blockEntry))
			return;
		
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
		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();

		BlockEntry blockEntry = BlockEntry.create(BlockEntryType.ONMODALACTION, Value.createAction(action.getIdentity()), Value.create(mode));
		
		if (currentPlayerContext != null)
		{
			// try current room.
			TRoomContext currentRoomContext = moduleContext.getCurrentRoomContext();
			if (currentRoomContext != null)
			{
				// get modal action on room.
				if (callElementBlock(request, response, currentRoomContext, blockEntry))
					return;
			}
			
			// get modal action on player.
			if (callElementBlock(request, response, currentPlayerContext, blockEntry))
				return;
		}
		
		TWorldContext worldContext = moduleContext.getWorldContext();
		// get modal action on world.
		if (callElementBlock(request, response, worldContext, blockEntry))
			return;

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
		TObjectContext currentObjectContext = moduleContext.getObjectContext(object);
		
		Value actionValue = Value.createAction(action.getIdentity());
		if (callElementBlock(request, response, currentObjectContext, BlockEntry.create(BlockEntryType.ONACTION, actionValue)))
			return;
		
		BlockEntry actionWithEntry = BlockEntry.create(BlockEntryType.ONACTIONWITH, actionValue, Value.createObject(object.getIdentity()));
		BlockEntry actionWithOtherEntry = BlockEntry.create(BlockEntryType.ONACTIONWITHOTHER, actionValue);

		TPlayerContext currentPlayerContext = request.getModuleContext().getCurrentPlayerContext();

		// Call onActionWith(action, object) on current room, then player.
		if (currentPlayerContext != null)
		{
			TPlayer currentPlayer = currentPlayerContext.getElement();

			// try current room.
			TRoomContext currentRoomContext = moduleContext.getCurrentRoomContext();
			if (currentRoomContext != null)
			{
				// get on action with block on room.
				if (callElementBlock(request, response, currentRoomContext, actionWithEntry))
					return;
				// get on action with ancestor on room
				else if (doActionAncestorSearch(request, response, actionValue, currentRoomContext.getElement(), object))
					return;
				// get on action with other block on room.
				else if (callElementBlock(request, response, currentRoomContext, actionWithOtherEntry))
					return;
			}
			
			// get on action with block on player.
			if (callElementBlock(request, response, currentPlayerContext, actionWithEntry))
				return;
			// get on action with ancestor on player
			else if (doActionAncestorSearch(request, response, actionValue, currentPlayer, object))
				return;
			// get on action with other block on player.
			else if (callElementBlock(request, response, currentPlayerContext, actionWithOtherEntry))
				return;
		}
		
		TWorldContext worldContext = request.getModuleContext().getWorldContext();
		TWorld world = worldContext.getElement();
		
		// get on action with block on world.
		if (callElementBlock(request, response, worldContext, actionWithEntry))
			return;
		// get on action with ancestor on world
		else if (doActionAncestorSearch(request, response, actionValue, world, object))
			return;
		// get on action with other block on world.
		else if (callElementBlock(request, response, worldContext, actionWithOtherEntry))
			return;

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
		TObjectContext currentObject1Context = moduleContext.getObjectContext(object1);
		TObjectContext currentObject2Context = moduleContext.getObjectContext(object2);
		
		BlockEntry blockEntry1, blockEntry2;
		Value actionValue = Value.createAction(action.getIdentity());

		// call action on each object. one or both need to succeed for no failure.
		blockEntry1 = BlockEntry.create(BlockEntryType.ONACTIONWITH, actionValue, Value.createObject(object1.getIdentity()));
		blockEntry2 = BlockEntry.create(BlockEntryType.ONACTIONWITH, actionValue, Value.createObject(object2.getIdentity()));
		
		boolean call12 = !action.isStrict() || !action.isReversed();
		boolean call21 = !action.isStrict() || action.isReversed();
		
		if (call12 && callElementBlock(request, response, currentObject1Context, blockEntry2))
			return;
		if (call21 && callElementBlock(request, response, currentObject2Context, blockEntry1))
			return;

		// call action with ancestor on each object. one or both need to succeed for no failure.
		if (call12 && doActionAncestorSearch(request, response, actionValue, object1, object2))
			return;
		if (call21 && doActionAncestorSearch(request, response, actionValue, object2, object1))
			return;
		
		// attempt action with other on both objects.
		BlockEntry actionOtherEntry = BlockEntry.create(BlockEntryType.ONACTIONWITHOTHER, actionValue);
		if (call12 && callElementBlock(request, response, currentObject1Context, actionOtherEntry))
			return;
		if (call21 && callElementBlock(request, response, currentObject2Context, actionOtherEntry))
			return;

		// if we STILL can't do it...
		if (!callActionUnhandled(request, response, action))
			response.addCue(CUE_ERROR, "ACTION UNHANDLED (make a better in-universe handler!).");
	}
	
	/**
	 * Attempts to perform an action for the ancestor search.
	 * @param request the request object.
	 * @param response the response object.
	 * @param actionValue the action that is being called (value).
	 * @param element the element to call the block on.
	 * @param start the object to start the search from.
	 * @return true if a block was found and called.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static boolean doActionAncestorSearch(TAMERequest request, TAMEResponse response, Value actionValue, TElement element, TObject start) throws TAMEInterrupt
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TObject ancestor = (TObject)start.getParent();
		TElementContext<?> elementContext = moduleContext.getContextByIdentity(element.getIdentity());

		while (ancestor != null)
		{
			if (callElementBlock(request, response, elementContext, BlockEntry.create(BlockEntryType.ONACTIONWITHANCESTOR, actionValue, Value.createObject(ancestor.getIdentity()))))
				return true;
			ancestor = (TObject)ancestor.getParent();
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
		TAMEModuleContext moduleContext = request.getModuleContext();
		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
		BlockEntry blockEntry = BlockEntry.create(BlockEntryType.ONUNKNOWNCOMMAND);
		
		if (currentPlayerContext != null)
		{
			if (callElementBlock(request, response, currentPlayerContext, blockEntry))
				return true;
		}
	
		TWorldContext worldContext = moduleContext.getWorldContext();
		if (callElementBlock(request, response, worldContext, blockEntry))
			return true;
	
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

		BlockEntry entry = BlockEntry.create(BlockEntryType.ONAMBIGUOUSCOMMAND, Value.createAction(action.getIdentity()));
		BlockEntry genericEntry = BlockEntry.create(BlockEntryType.ONAMBIGUOUSCOMMAND);

		TPlayerContext currentPlayerContext = moduleContext.getCurrentPlayerContext();
		if (currentPlayerContext != null)
		{
			if (callElementBlock(request, response, currentPlayerContext, entry))
				return true;
			if (callElementBlock(request, response, currentPlayerContext, genericEntry))
				return true;
		}
	
		TWorldContext worldContext = moduleContext.getWorldContext();
		if (callElementBlock(request, response, worldContext, entry))
			return true;
		if (callElementBlock(request, response, worldContext, genericEntry))
			return true;
		
		return false;
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
		BlockEntry entry = BlockEntry.create(BlockEntryType.ONMALFORMEDCOMMAND, Value.createAction(action.getIdentity()));
		BlockEntry genericEntry = BlockEntry.create(BlockEntryType.ONMALFORMEDCOMMAND);
		
		// try bad action on player.
		if (currentPlayerContext != null)
		{
			if (callElementBlock(request, response, currentPlayerContext, entry))
				return true;
			if (callElementBlock(request, response, currentPlayerContext, genericEntry))
				return true;
		}
	
		// try bad action on world.
		if (callElementBlock(request, response, worldContext, entry))
			return true;
		if (callElementBlock(request, response, worldContext, genericEntry))
			return true;

		return false;
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
		BlockEntry entry = BlockEntry.create(BlockEntryType.ONINCOMPLETECOMMAND, Value.createAction(action.getIdentity()));
		BlockEntry genericEntry = BlockEntry.create(BlockEntryType.ONINCOMPLETECOMMAND);
		
		// try incomplete on player.
		if (currentPlayerContext != null)
		{
			if (callElementBlock(request, response, currentPlayerContext, entry))
				return true;
			if (callElementBlock(request, response, currentPlayerContext, genericEntry))
				return true;
		}
	
		TWorldContext worldContext = moduleContext.getWorldContext();

		// try incomplete on world.
		if (callElementBlock(request, response, worldContext, entry))
			return true;
		if (callElementBlock(request, response, worldContext, genericEntry))
			return true;

		return false;
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
		BlockEntry entry = BlockEntry.create(BlockEntryType.ONUNHANDLEDACTION, Value.createAction(action.getIdentity()));
		BlockEntry genericEntry = BlockEntry.create(BlockEntryType.ONUNHANDLEDACTION);
		if (currentPlayerContext != null)
		{
			if (callElementBlock(request, response, currentPlayerContext, entry))
				return true;
			if (callElementBlock(request, response, currentPlayerContext, genericEntry))
				return true;
		}
	
		TWorldContext worldContext = moduleContext.getWorldContext();

		// try fail on world.
		if (callElementBlock(request, response, worldContext, entry))
			return true;
		if (callElementBlock(request, response, worldContext, genericEntry))
			return true;

		return false;
	}

	// Call init on iterable contexts.
	private static void callInitOnContexts(TAMERequest request, TAMEResponse response, Iterator<? extends TElementContext<?>> contexts) throws TAMEInterrupt 
	{
		while (contexts.hasNext())
		{
			TElementContext<?> context = contexts.next();
			callElementBlock(request, response, context, BlockEntry.create(BlockEntryType.INIT));
		}
	}

	/**
	 * Interprets the input on the request.
	 * Requires a context, as objects may need to be parsed.
	 * @param request the request object.
	 * @param response the response object.
	 * @return a new interpreter context using the input.
	 */
	private static InterpreterContext interpret(TAMERequest request, TAMEResponse response)
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		String inputMessage = request.getInputMessage();
		InterpreterContext interpreterContext = new InterpreterContext(TAMELogic.tokenizeInput(moduleContext, inputMessage));
		
		interpretAction(request, response, interpreterContext);
		
		TAction action = interpreterContext.getAction();
		if (action == null)
			return interpreterContext;
		
		switch (action.getType())
		{
			default:
			case GENERAL:
				return interpreterContext;
			case OPEN:
				interpretOpen(request, response, interpreterContext);
				return interpreterContext;
			case MODAL:
				interpretMode(request, response, action, interpreterContext);
				return interpreterContext;
			case TRANSITIVE:
				interpretObject1(request, response, moduleContext, interpreterContext);
				return interpreterContext;
			case DITRANSITIVE:
				if (interpretObject1(request, response, moduleContext, interpreterContext))
					if (interpretConjugate(request, response, action, interpreterContext))
						interpretObject2(request, response, moduleContext, interpreterContext);
				return interpreterContext;
		}
	}

	/**
	 * Interprets an action from the input line.
	 * @param request the request object.
	 * @param response the response object.
	 * @param interpreterContext the TAMEInterpreterContext.
	 */
	private static void interpretAction(TAMERequest request, TAMEResponse response, InterpreterContext interpreterContext)
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
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
			response.trace(request, TraceType.INTERPRETER, "TEST ACTION %s", sb.toString());
			TAction next = module.getActionByName(sb.toString());
			if (next != null)
			{
				response.trace(request, TraceType.INTERPRETER, "MATCHED ACTION %s", next.getIdentity());
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
	private static void interpretMode(TAMERequest request, TAMEResponse response, TAction action, InterpreterContext interpreterContext)
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
			response.trace(request, TraceType.INTERPRETER, "TEST MODE %s", next);
			if (action.containsExtraString(next))
			{
				response.trace(request, TraceType.INTERPRETER, "MATCHED MODE %s", next);
				interpreterContext.setMode(next);
				interpreterContext.setTokenOffset(index);
			}
		}
	}

	/**
	 * Interprets open target.
	 * @param request the request object.
	 * @param response the response object.
	 * @param interpreterContext the TAMEInterpreterContext.
	 */
	private static void interpretOpen(TAMERequest request, TAMEResponse response, InterpreterContext interpreterContext)
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
		
		response.trace(request, TraceType.INTERPRETER, "READ OPEN TARGET %s", sb.toString());
		interpreterContext.setTarget(sb.length() > 0 ? sb.toString() : null);
		interpreterContext.setTokenOffset(index);
	}

	/**
	 * Interprets an action conjugate from the input line (like "with" or "on" or whatever).
	 * @param request the request object.
	 * @param response the response object.
	 * @param action the action to use.
	 * @param interpreterContext the TAMEInterpreterContext.
	 */
	private static boolean interpretConjugate(TAMERequest request, TAMEResponse response, TAction action, InterpreterContext interpreterContext)
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
			String name = sb.toString();
			response.trace(request, TraceType.INTERPRETER, "TEST CONJUNCTION %s", name);
			if (action.containsExtraString(name))
			{
				response.trace(request, TraceType.INTERPRETER, "MATCHED CONJUNCTION %s", name);
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
	 * @param request the request object.
	 * @param response the response object.
	 * @param moduleContext the module context.
	 * @param interpreterContext the TAMEInterpreterContext.
	 */
	private static boolean interpretObject1(TAMERequest request, TAMEResponse response, TAMEModuleContext moduleContext, InterpreterContext interpreterContext)
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
			response.trace(request, TraceType.INTERPRETER, "TEST OBJECT 1 %s", sb.toString());
			int out = moduleContext.getAccessibleObjectsByName(sb.toString(), interpreterContext.getObjects(), 0);
			if (out > 1)
			{
				response.trace(request, TraceType.INTERPRETER, "MATCHED MULTIPLE OBJECTS");
				interpreterContext.setObjectAmbiguous(true);
				interpreterContext.setObject1(null);
				interpreterContext.setTokenOffset(index);
			}
			else if (out > 0)
			{
				response.trace(request, TraceType.INTERPRETER, "MATCHED OBJECT 1 %s", interpreterContext.getObjects()[0].getIdentity());
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
	 * @param request the request object.
	 * @param response the response object.
	 * @param moduleContext the module context.
	 * @param interpreterContext the TAMEInterpreterContext.
	 */
	private static boolean interpretObject2(TAMERequest request, TAMEResponse response, TAMEModuleContext moduleContext, InterpreterContext interpreterContext)
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
			response.trace(request, TraceType.INTERPRETER, "TEST OBJECT 2 %s", sb.toString());
			int out = moduleContext.getAccessibleObjectsByName(sb.toString(), interpreterContext.getObjects(), 0);
			if (out > 1)
			{
				response.trace(request, TraceType.INTERPRETER, "MATCHED MULTIPLE OBJECTS");
				interpreterContext.setObjectAmbiguous(true);
				interpreterContext.setObject2(null);
				interpreterContext.setTokenOffset(index);
			}
			else if (out > 0)
			{
				response.trace(request, TraceType.INTERPRETER, "MATCHED OBJECT 2 %s", interpreterContext.getObjects()[0].getIdentity());
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
		
		private InterpreterContext(String[] tokens)
		{
			this.tokens = tokens;
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
