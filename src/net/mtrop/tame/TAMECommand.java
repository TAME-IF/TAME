/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package net.mtrop.tame;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.blackrook.commons.math.RMath;

import net.mtrop.tame.element.ObjectContainer;
import net.mtrop.tame.element.TAction;
import net.mtrop.tame.element.TAction.Type;
import net.mtrop.tame.element.TContainer;
import net.mtrop.tame.element.TElement;
import net.mtrop.tame.element.TObject;
import net.mtrop.tame.element.TPlayer;
import net.mtrop.tame.element.TRoom;
import net.mtrop.tame.element.TWorld;
import net.mtrop.tame.element.context.TElementContext;
import net.mtrop.tame.element.context.TOwnershipMap;
import net.mtrop.tame.exception.ModuleExecutionException;
import net.mtrop.tame.exception.UnexpectedValueTypeException;
import net.mtrop.tame.interrupt.BreakInterrupt;
import net.mtrop.tame.interrupt.FinishInterrupt;
import net.mtrop.tame.interrupt.ContinueInterrupt;
import net.mtrop.tame.interrupt.EndInterrupt;
import net.mtrop.tame.interrupt.ErrorInterrupt;
import net.mtrop.tame.interrupt.QuitInterrupt;
import net.mtrop.tame.lang.ArgumentType;
import net.mtrop.tame.lang.Block;
import net.mtrop.tame.lang.Command;
import net.mtrop.tame.lang.CommandType;
import net.mtrop.tame.lang.Value;
import net.mtrop.tame.lang.ValueHash;
import net.mtrop.tame.lang.ValueType;

/**
 * The set of commands.
 * Values in arguments are popped in reverse order on call, if arguments are taken. 
 * NOTE: THESE MUST BE DECLARED IN FULL CAPS TO ENSURE PARSING INTEGRITY!
 * @author Matthew Tropiano
 */
public enum TAMECommand implements CommandType, TAMEConstants
{
	
	/**
	 * Does nothing.
	 * Returns nothing.
	 */
	NOOP (/*Return: */ null)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) 
		{
			// Do nothing.
		}
		
		@Override
		public String getGrouping() 
		{
			return "Miscellaneous";
		}
		
	},
	
	/**
	 * [INTERNAL] Pops a value off the stack.
	 * Returns nothing.
	 */
	POP (true)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command)
		{
			request.popValue();
		}
		
		@Override
		public String getGrouping()
		{
			return null;
		}
		
	},

	/**
	 * [INTERNAL] Pops a value into a variable in the topmost context in the execution, or the locals if defined.
	 * Operand0 is the variable. 
	 * POP is the value.
	 */
	POPVALUE (true)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) 
		{
			Value varvalue = command.getOperand0();
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in POPVALUE call.");
			if (!varvalue.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in POPVALUE call.");
			
			String variableName = varvalue.asString();
			
			if (blockLocal.containsKey(variableName))
				blockLocal.put(variableName, value);
			else
				request.peekContext().setValue(variableName, value);
		}
		
		@Override
		public String getGrouping()
		{
			return null;
		}
		
	},
	
	/**
	 * [INTERNAL] Pops a value into a local variable.
	 * Operand0 is the variable. 
	 * POP is the value.
	 */
	POPLOCALVALUE (true)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) 
		{
			Value varvalue = command.getOperand0();
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in POPLOCALVALUE call.");
			if (!varvalue.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in POPLOCALVALUE call.");
			
			String variableName = varvalue.asString();
			blockLocal.put(variableName, value);
		}
		
		@Override
		public String getGrouping()
		{
			return null;
		}
		
	},
	
	/**
	 * [INTERNAL] Sets a variable on a object.
	 * Operand0 is the object. 
	 * Operand1 is the variable. 
	 * POP is the value.
	 */
	POPELEMENTVALUE (true)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws ErrorInterrupt
		{
			Value varElement = command.getOperand0();
			Value variable = command.getOperand1();
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in POPELEMENTVALUE call.");
			if (!variable.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in POPELEMENTVALUE call.");
			if (!varElement.isElement())
				throw new UnexpectedValueTypeException("Expected element type in POPELEMENTVALUE call.");
			
			String variableName = variable.asString();
			TElementContext<?> context = request.getModuleContext().resolveElementContext(varElement); 
			context.setValue(variableName, value);
		}
		
		@Override
		public String getGrouping()
		{
			return null;
		}
		
	},

	/**
	 * [INTERNAL] Pushes a value or variable from the topmost context.
	 * Operand0 is the variable or value. 
	 * Pushes the value. If variable, it is resolved before the push.
	 */
	PUSHVALUE (true)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) 
		{
			Value value = command.getOperand0();
			
			if (value.isVariable())
			{
				String variableName = value.asString();
				
				if (blockLocal.containsKey(variableName))
					request.pushValue(blockLocal.get(variableName));
				else
					request.pushValue(request.peekContext().getValue(variableName));
			}
			else
			{
				request.pushValue(value);
			}
			
		}
		
		@Override
		public String getGrouping()
		{
			return null;
		}
		
	},
	
	/**
	 * [INTERNAL] Resolves value of a variable on an object and pushes the value.
	 * Operand0 is the element. 
	 * Operand1 is the variable. 
	 * Pushes the resolved value. 
	 */
	PUSHELEMENTVALUE (true)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws ErrorInterrupt
		{
			Value varElement = command.getOperand0();
			Value variable = command.getOperand1();

			if (!variable.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in PUSHELEMENTVALUE call.");
			if (!varElement.isElement())
				throw new UnexpectedValueTypeException("Expected element type in PUSHELEMENTVALUE call.");

			String variableName = variable.asString();
			TElementContext<?> context = request.getModuleContext().resolveElementContext(varElement); 

			request.pushValue(context.getValue(variableName));
		}
		
		@Override
		public String getGrouping()
		{
			return null;
		}
		
	},
	
	/**
	 * [INTERNAL] Clears a variable from blocklocal or object member.
	 * Operand0 is the variable. 
	 * Returns nothing.
	 */
	CLEARVALUE (true)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws ErrorInterrupt
		{
			Value variable = command.getOperand0();

			if (!variable.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in CLEARVALUE call.");

			String variableName = variable.asString();
			if (blockLocal.containsKey(variableName))
				blockLocal.removeUsingKey(variableName);
			else
				request.peekContext().clearValue(variableName);
		}
		
		@Override
		public String getGrouping()
		{
			return null;
		}
		
	},
	
	/**
	 * [INTERNAL] Clears a variable from blocklocal or object member.
	 * Operand0 is the element. 
	 * Operand1 is the variable. 
	 * Returns nothing.
	 */
	CLEARELEMENTVALUE (true)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws ErrorInterrupt
		{
			Value varElement = command.getOperand0();
			Value variable = command.getOperand1();

			if (!variable.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in CLEARELEMENTVALUE call.");
			if (!varElement.isElement())
				throw new UnexpectedValueTypeException("Expected element type in CLEARELEMENTVALUE call.");

			String variableName = variable.asString();
			if (blockLocal.containsKey(variableName))
				blockLocal.removeUsingKey(variableName);
			else
			{
				TElementContext<?> context = request.getModuleContext().resolveElementContext(varElement);
				context.clearValue(variableName);
			}
				
		}
		
		@Override
		public String getGrouping()
		{
			return null;
		}
		
	},
	
	/**
	 * [INTERNAL] Pushes the element connected to the current context.
	 * Returns nothing.
	 */
	PUSHTHIS (true)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws ErrorInterrupt
		{
			TElement element = request.peekContext().getElement();
			Class<?> elementClass = element.getClass();
			if (TObject.class.isAssignableFrom(elementClass))
				request.pushValue(Value.createObject(element.getIdentity()));
			else if (TRoom.class.isAssignableFrom(elementClass))
				request.pushValue(Value.createRoom(element.getIdentity()));
			else if (TPlayer.class.isAssignableFrom(elementClass))
				request.pushValue(Value.createPlayer(element.getIdentity()));
			else if (TContainer.class.isAssignableFrom(elementClass))
				request.pushValue(Value.createContainer(element.getIdentity()));
			else if (TWorld.class.isAssignableFrom(elementClass))
				request.pushValue(Value.createWorld());
			else
				throw new ModuleExecutionException("Internal error - invalid object type for PUSHTHIS.");
		}
		
		@Override
		public String getGrouping()
		{
			return null;
		}
		
	},
	
	/**
	 * [INTERNAL] Arithmetic function.
	 * Operand0 is an integer that describes the operation. 
	 * Pops a varying amount of values off the stack depending on the function.
	 * Pushes result.
	 */
	ARITHMETICFUNC (true)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command)
		{
			Value functionValue = command.getOperand0();
			
			if (!functionValue.isInteger())
				throw new UnexpectedValueTypeException("Expected integer type in ARITHMETICFUNC call.");

			TAMELogic.doArithmeticStackFunction(request, response, (int)functionValue.asLong());
		}

		@Override
		public String getGrouping()
		{
			return null;
		}
		
	},
	
	/**
	 * [INTERNAL] If block.
	 * Has a conditional block that is called and then the success 
	 * block if POP is true, or if false and the fail block exists, call the fail block. 
	 * Returns nothing. 
	 */
	IF (true, true)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			// block should contain arithmetic commands and a last push.
			Block conditional = command.getConditionalBlock();
			if (conditional == null)
				throw new ModuleExecutionException("Conditional block for IF does NOT EXIST!");
			
			response.trace(request, "Calling IF conditional...");
			conditional.execute(request, response, blockLocal);
			
			// get remaining expression value.
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type after IF conditional block execution.");
	
			if (value.asBoolean())
			{
				response.trace(request, "Result %s evaluates true.", value);
				response.trace(request, "Calling IF success block...");
				Block success = command.getSuccessBlock();
				if (success == null)
					throw new ModuleExecutionException("Success block for IF does NOT EXIST!");
				success.execute(request, response, blockLocal);
			}
			else
			{
				response.trace(request, "Result %s evaluates false.", value);
				Block failure = command.getFailureBlock();
				if (failure != null)
				{
					response.trace(request, "Calling IF failure block...");
					failure.execute(request, response, blockLocal);
				}
				else
				{
					response.trace(request, "No failure block...");
				}
			}
			
		}
		
		@Override
		public String getGrouping()
		{
			return null;
		}
		
	},
	
	/**
	 * [INTERNAL] WHILE block.
	 * Has a conditional block that is called and then the success block if POP is true. 
	 * Returns nothing. 
	 */
	WHILE (true, true)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			while (callConditional(request, response, blockLocal, command))
			{
				try {
					response.trace(request, "Calling WHILE success block...");
					Block success = command.getSuccessBlock();
					if (success == null)
						throw new ModuleExecutionException("Success block for WHILE does NOT EXIST!");
					success.execute(request, response, blockLocal);
				} catch (BreakInterrupt interrupt) {
					break;
				} catch (ContinueInterrupt interrupt) {
					continue;
				}
			}
		}
		
		private boolean callConditional(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			response.trace(request, "Calling WHILE conditional...");

			// block should contain arithmetic commands and a last push.
			Block conditional = command.getConditionalBlock();
			if (conditional == null)
				throw new ModuleExecutionException("Conditional block for WHILE does NOT EXIST!");
			conditional.execute(request, response, blockLocal);
			
			// get remaining expression value.
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type after WHILE conditional block execution.");
	
			boolean out = value.asBoolean();
			response.trace(request, "Result %s evaluates %b.", value, out);
			return out; 
		}
		
		@Override
		public String getGrouping()
		{
			return null;
		}
		
	}, 
	
	/**
	 * [INTERNAL] FOR block.
	 * Has an init block called once, a conditional block that is called and then the success block if POP is true,
	 * and another block for the next step. 
	 * Returns nothing. 
	 */
	FOR (true, true)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Block init = command.getInitBlock();
			if (init == null)
				throw new ModuleExecutionException("Init block for FOR does NOT EXIST!");
			Block success = command.getSuccessBlock();
			if (success == null)
				throw new ModuleExecutionException("Success block for FOR does NOT EXIST!");
			Block step = command.getStepBlock();
			if (step == null)
				throw new ModuleExecutionException("Step block for FOR does NOT EXIST!");

			response.trace(request, "Calling FOR init block...");
			for (
				init.execute(request, response, blockLocal); 
				callConditional(request, response, blockLocal, command); 
				response.trace(request, "Calling FOR stepping block..."), 
				step.execute(request, response, blockLocal)
			)
			{
				try {
					response.trace(request, "Calling FOR success block...");
					success.execute(request, response, blockLocal);
				} catch (BreakInterrupt interrupt) {
					break;
				} catch (ContinueInterrupt interrupt) {
					continue;
				}
			}
		}

		private boolean callConditional(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			response.trace(request, "Calling FOR conditional...");
			
			// block should contain arithmetic commands and a last push.
			Block conditional = command.getConditionalBlock();
			if (conditional == null)
				throw new ModuleExecutionException("Conditional block for FOR does NOT EXIST!");
			conditional.execute(request, response, blockLocal);
			
			// get remaining expression value.
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type after FOR conditional block execution.");
	
			boolean out = value.asBoolean();
			response.trace(request, "Result %s evaluates %b.", value, out);
			return out; 
		}
		
		@Override
		public String getGrouping()
		{
			return null;
		}
		
	}, 
	
	/**
	 * Throws a BREAK interrupt.
	 * Is keyword. Returns nothing. 
	 */
	BREAK ()
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			response.trace(request, "Throwing break interrupt...");
			throw new BreakInterrupt();
		}
		
		@Override
		public String getGrouping()
		{
			return null;
		}
		
	},
	
	/**
	 * Throws a CONTINUE interrupt.
	 * Is keyword. Returns nothing. 
	 */
	CONTINUE ()
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			response.trace(request, "Throwing continue interrupt...");
			throw new ContinueInterrupt();
		}
		
		@Override
		public String getGrouping()
		{
			return null;
		}
		
	},
	
	/**
	 * Adds a QUIT cue to the response and throws a QUIT interrupt.
	 * Is keyword. Returns nothing. 
	 */
	QUIT ()
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			response.trace(request, "Throwing quit interrupt...");
			response.addCue(CUE_QUIT);
			throw new QuitInterrupt();
		}
		
		@Override
		public String getGrouping()
		{
			return null;
		}
		
	},
	
	/**
	 * Throws a FINISH interrupt.
	 * Is keyword. Returns nothing. 
	 */
	FINISH ()
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			response.trace(request, "Throwing finish interrupt...");
			throw new FinishInterrupt();
		}
		
		@Override
		public String getGrouping()
		{
			return null;
		}
		
	},
	
	/**
	 * Throws an END interrupt.
	 * Is keyword. Returns nothing. 
	 */
	END ()
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			response.trace(request, "Throwing end interrupt...");
			throw new EndInterrupt();
		}
		
		@Override
		public String getGrouping()
		{
			return null;
		}
		
	},
	
	/**
	 * Return from function.
	 * Sets RETURN value on blocklocal from POP and then throws an END interrupt.
	 */
	FUNCTIONRETURN ()
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value retVal = request.popValue();
			response.trace(request, "Returning "+retVal.toString());
			blockLocal.put(RETURN_VARIABLE, retVal);
			response.trace(request, "Throwing end interrupt...");
			throw new EndInterrupt();
		}
		
		@Override
		public String getGrouping()
		{
			return null;
		}
		
	},
	
	/**
	 * [INTERNAL] Calls a function.
	 * Operand0 is the function name.
	 * Pops a varying amount of values off the stack depending on the function.
	 * Pushes result.
	 * Returns nothing.
	 */
	CALLFUNCTION ()
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varFunctionName = command.getOperand0();

			if (!varFunctionName.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in CALLFUNCTION call.");

			request.pushValue(TAMELogic.callElementFunction(request, response, varFunctionName.asString(), request.peekContext()));
		}
		
		@Override
		public String getGrouping()
		{
			return null;
		}
		
	},
	
	/**
	 * [INTERNAL] Calls an element function.
	 * Operand0 is the element.
	 * Operand1 is the function name.
	 * Pops a varying amount of values off the stack depending on the function.
	 * Pushes result.
	 * Returns nothing.
	 */
	CALLELEMENTFUNCTION ()
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varElement = command.getOperand0();
			Value varFunctionName = command.getOperand1();

			if (!varElement.isElement())
				throw new UnexpectedValueTypeException("Expected element type in CALLELEMENTFUNCTION call.");
			if (!varFunctionName.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in CALLELEMENTFUNCTION call.");
			
			request.pushValue(TAMELogic.callElementFunction(request, response, varFunctionName.asString(), request.getModuleContext().resolveElementContext(varElement)));
		}
		
		@Override
		public String getGrouping()
		{
			return null;
		}
		
	},
	
	/**
	 * Enqueues a general action to perform after the current one finishes.
	 * POP is the action.
	 * Returns nothing.
	 */
	QUEUEACTION (/*Return: */ null, /*Args: */ ArgumentType.ACTION_GENERAL)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varAction = request.popValue();
			
			if (varAction.getType() != ValueType.ACTION)
				throw new UnexpectedValueTypeException("Expected action type in QUEUEACTION call.");

			TAction action = request.getModuleContext().resolveAction(varAction.asString());
			
			if (action.getType() != Type.GENERAL)
				throw new UnexpectedValueTypeException("BAD TYPE: "+action.getIdentity() + " is not a general action.");
			else
			{
				TAMEAction tameAction = TAMEAction.create(action);
				request.addActionItem(tameAction);
				response.trace(request, "Enqueued "+tameAction);
			}
		}

		@Override
		public String getGrouping()
		{
			return "Control";
		}
		
	},

	/**
	 * Enqueues an open/modal action to perform after the current one finishes.
	 * First POP is the modal or open target.
	 * Second POP is the action.
	 * Returns nothing.
	 */
	QUEUEACTIONSTRING (/*Return: */ null, /*Args: */ ArgumentType.ACTION_MODAL_OPEN, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varTarget = request.popValue();
			Value varAction = request.popValue();
			
			if (!varTarget.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in QUEUEACTIONSTRING call.");
			if (varAction.getType() != ValueType.ACTION)
				throw new UnexpectedValueTypeException("Expected action type in QUEUEACTIONSTRING call.");

			TAction action = request.getModuleContext().resolveAction(varAction.asString());
			String target = varTarget.asString();
			
			if (action.getType() != Type.MODAL && action.getType() != Type.OPEN)
				throw new UnexpectedValueTypeException("BAD TYPE: " + action.getIdentity() + " is not a modal nor open action.");
			else
			{
				TAMEAction tameAction = TAMEAction.create(action, target);
				request.addActionItem(tameAction);
				response.trace(request, "Enqueued "+tameAction);
			}
		}

		@Override
		public String getGrouping()
		{
			return "Control";
		}
		
	},
	
	/**
	 * Enqueues a transitive action to perform after the current one finishes.
	 * First POP is the object.
	 * Second POP is the action.
	 * Returns nothing.
	 */
	QUEUEACTIONOBJECT (/*Return: */ null, /*Args: */ ArgumentType.ACTION_TRANSITIVE_DITRANSITIVE, ArgumentType.OBJECT)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varObject = request.popValue();
			Value varAction = request.popValue();
			
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in QUEUEACTIONOBJECT call.");
			if (varAction.getType() != ValueType.ACTION)
				throw new UnexpectedValueTypeException("Expected action type in QUEUEACTIONOBJECT call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TAction action = moduleContext.resolveAction(varAction.asString());
			TObject object = moduleContext.resolveObject(varObject.asString());
			
			if (action.getType() != Type.TRANSITIVE && action.getType() != Type.DITRANSITIVE)
				throw new UnexpectedValueTypeException("BAD TYPE: " + action.getIdentity() + " is not a transitive nor ditransitive action.");
			else
			{
				TAMEAction tameAction = TAMEAction.create(action, object);
				request.addActionItem(tameAction);
				response.trace(request, "Enqueued "+tameAction);
			}
		}

		@Override
		public String getGrouping()
		{
			return "Control";
		}
		
	},
	
	/**
	 * Enqueues a transitive action to perform after the current one finishes for each object.
	 * First POP is the object-container.
	 * Second POP is the action.
	 * Returns nothing.
	 */
	QUEUEACTIONFOROBJECTSIN (/*Return: */ null, /*Args: */ ArgumentType.ACTION_TRANSITIVE_DITRANSITIVE, ArgumentType.OBJECT_CONTAINER)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varObjectContainer = request.popValue();
			Value varAction = request.popValue();
			
			if (!varObjectContainer.isObjectContainer())
				throw new UnexpectedValueTypeException("Expected object-container type in QUEUEACTIONFOROBJECTSIN call.");
			if (varAction.getType() != ValueType.ACTION)
				throw new UnexpectedValueTypeException("Expected action type in QUEUEACTIONFOROBJECTSIN call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			
			TAction action = moduleContext.resolveAction(varAction.asString());
			if (action.getType() != Type.TRANSITIVE && action.getType() != Type.DITRANSITIVE)
				throw new UnexpectedValueTypeException("BAD TYPE: " + action.getIdentity() + " is not a transitive nor ditransitive action.");

			Iterable<TObject> objectList;
			
			if ((objectList = moduleContext.resolveObjectList(varObjectContainer)) == null)
				throw new UnexpectedValueTypeException("INTERNAL ERROR IN QUEUEACTIONFOROBJECTSIN.");
			
			for (TObject object : objectList)
			{
				TAMEAction tameAction = TAMEAction.create(action, object);
				request.addActionItem(tameAction);
				response.trace(request, "Enqueued "+tameAction);
			}
		}

		@Override
		public String getGrouping()
		{
			return "Control";
		}
		
	},
	
	/**
	 * Enqueues a transitive action to perform after the current one finishes for each object.
	 * First POP is the tag name.
	 * Second POP is the object-container.
	 * Third POP is the action.
	 * Returns nothing.
	 */
	QUEUEACTIONFORTAGGEDOBJECTSIN (/*Return: */ null, /*Args: */ ArgumentType.ACTION_TRANSITIVE_DITRANSITIVE, ArgumentType.OBJECT_CONTAINER, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varTag = request.popValue();
			Value varObjectContainer = request.popValue();
			Value varAction = request.popValue();
			
			if (!varTag.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in QUEUEACTIONFORTAGGEDOBJECTSIN call.");
			if (!varObjectContainer.isObjectContainer())
				throw new UnexpectedValueTypeException("Expected object-container type in QUEUEACTIONFORTAGGEDOBJECTSIN call.");
			if (varAction.getType() != ValueType.ACTION)
				throw new UnexpectedValueTypeException("Expected action type in QUEUEACTIONFORTAGGEDOBJECTSIN call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			
			TAction action = moduleContext.resolveAction(varAction.asString());
			if (action.getType() != Type.TRANSITIVE && action.getType() != Type.DITRANSITIVE)
				throw new UnexpectedValueTypeException("BAD TYPE: " + action.getIdentity() + " is not a transitive nor ditransitive action.");

			Iterable<TObject> objectList;
			
			if ((objectList = moduleContext.resolveObjectList(varObjectContainer)) == null)
				throw new UnexpectedValueTypeException("INTERNAL ERROR IN QUEUEACTIONFORTAGGEDOBJECTSIN.");
	
			String tagName = varTag.asString();
			TOwnershipMap ownershipMap = moduleContext.getOwnershipMap();
			for (TObject object : objectList) if (ownershipMap.checkObjectHasTag(object, tagName))
			{
				TAMEAction tameAction = TAMEAction.create(action, object);
				request.addActionItem(tameAction);
				response.trace(request, "Enqueued "+tameAction);
			}
			
		}

		@Override
		public String getGrouping()
		{
			return "Control";
		}
		
	},
	
	/**
	 * Enqueues a ditransitive action to perform after the current one finishes.
	 * First POP is the second object.
	 * Second POP is the object.
	 * Third POP is the action.
	 * Returns nothing.
	 */
	QUEUEACTIONOBJECT2 (/*Return: */ null, /*Args: */ ArgumentType.ACTION_DITRANSITIVE, ArgumentType.OBJECT, ArgumentType.OBJECT)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varObject2 = request.popValue();
			Value varObject = request.popValue();
			Value varAction = request.popValue();
			
			if (varObject2.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in QUEUEACTIONOBJECT2 call.");
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in QUEUEACTIONOBJECT2 call.");
			if (varAction.getType() != ValueType.ACTION)
				throw new UnexpectedValueTypeException("Expected action type in QUEUEACTIONOBJECT2 call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TAction action = moduleContext.resolveAction(varAction.asString());
			TObject object = moduleContext.resolveObject(varObject.asString());
			TObject object2 = moduleContext.resolveObject(varObject2.asString());
			
			if (action.getType() != Type.DITRANSITIVE)
				throw new UnexpectedValueTypeException("BAD TYPE: " + action.getIdentity() + " is not a ditransitive action.");
			else
			{
				TAMEAction tameAction = TAMEAction.create(action, object, object2);
				request.addActionItem(tameAction);
				response.trace(request, "Enqueued "+tameAction);
			}
			
		}

		@Override
		public String getGrouping()
		{
			return "Control";
		}
		
	},

	/**
	 * Adds a cue to the response.
	 * First POP is the value to print. 
	 * Second POP is the cue name. 
	 * Returns nothing. 
	 */
	ADDCUE (/*Return: */ null, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			Value cue = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in ADDCUE call.");
			if (!cue.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in ADDCUE call.");
	
			response.addCue(cue.asString(), value.asString());
		}
		
		@Override
		public String getGrouping()
		{
			return "Cues";
		}
		
	}, 
	
	/**
	 * Adds a TEXT cue to the response.
	 * POP is the value to print. 
	 * Returns nothing. 
	 */
	TEXT (/*Return: */ null, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in TEXT call.");

			response.addCue(CUE_TEXT, value.asString());
		}
		
		@Override
		public String getGrouping()
		{
			return "Cues";
		}
		
	},
	
	/**
	 * Adds a TEXT cue to the response with a newline appended to it.
	 * POP is the value to print. 
	 * Returns nothing. 
	 */
	TEXTLN (/*Return: */ null, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in TEXTLN call.");

			response.addCue(CUE_TEXT, value.asString() + '\n');
		}
		
		@Override
		public String getGrouping()
		{
			return "Cues";
		}
		
	},
	
	/**
	 * Adds a TEXTF cue to the response.
	 * POP is the value to print. 
	 * Returns nothing. 
	 */
	TEXTF (/*Return: */ null, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in TEXTF call.");

			response.addCue(CUE_TEXTF, value.asString());
		}
		
		@Override
		public String getGrouping()
		{
			return "Cues";
		}
		
	},
	
	/**
	 * Adds a TEXTF cue to the response with a newline appended to it.
	 * POP is the value to print. 
	 * Returns nothing. 
	 */
	TEXTFLN (/*Return: */ null, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in TEXTFLN call.");

			response.addCue(CUE_TEXTF, value.asString() + '\n');
		}
		
		@Override
		public String getGrouping()
		{
			return "Cues";
		}
		
	},
	
	/**
	 * Adds a PAUSE cue to the response.
	 * POP is the value to print. 
	 * Returns nothing. 
	 */
	PAUSE (/*Return: */ null)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			response.addCue(CUE_PAUSE);
		}
		
		@Override
		public String getGrouping()
		{
			return "Cues";
		}
		
	},
	
	/**
	 * Adds a WAIT cue to the response.
	 * POP is the amount to wait for (read as integer). 
	 * Returns nothing. 
	 */
	WAIT (/*Return: */ null, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in WAIT call.");

			response.addCue(CUE_WAIT, value.asLong());
		}
		
		@Override
		public String getGrouping()
		{
			return "Cues";
		}
		
	},
	
	/**
	 * Casts a value to boolean.
	 * POP is the value to convert. 
	 * Returns value as boolean. 
	 */
	ASBOOLEAN (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in ASBOOLEAN call.");

			request.pushValue(Value.create(value.asBoolean()));
		}
		
		@Override
		public String getGrouping()
		{
			return "Values";
		}
		
	},
	
	/**
	 * Casts a value to integer.
	 * POP is the value to convert. 
	 * Returns value as integer. 
	 */
	ASINT (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in ASINT call.");

			request.pushValue(Value.create(value.asLong()));
		}
		
		@Override
		public String getGrouping()
		{
			return "Values";
		}
		
	},
	
	/**
	 * Casts a value to float.
	 * POP is the value to convert. 
	 * Returns value as float. 
	 */
	ASFLOAT (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in ASFLOAT call.");

			request.pushValue(Value.create(value.asDouble()));
		}
		
		@Override
		public String getGrouping()
		{
			return "Values";
		}
		
	},
	
	/**
	 * Casts a value to string.
	 * POP is the value to convert. 
	 * Returns value as string. 
	 */
	ASSTRING (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in ASSTRING call.");

			request.pushValue(Value.create(value.asString()));
		}
		
		@Override
		public String getGrouping()
		{
			return "Values";
		}
		
	},
	
	/**
	 * Gets the length of a string.
	 * POP is the value, cast to a string. 
	 * Returns integer. 
	 */
	STRLENGTH (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRLENGTH call.");

			request.pushValue(Value.create(value.asString().length()));
		}
		
		@Override
		public String getGrouping()
		{
			return "String Operations";
		}
		
	},
	
	/**
	 * Concatenates two strings together.
	 * POP is the second value, cast to a string. 
	 * POP is the first value, cast to a string. 
	 * Returns integer. 
	 */
	STRCONCAT (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRCONCAT call.");
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRCONCAT call.");

			request.pushValue(Value.create(value1.asString() + value2.asString()));
		}
		
		@Override
		public String getGrouping()
		{
			return "String Operations";
		}
		
	},
	
	/**
	 * Replaces a part of a string with another.
	 * First POP is the string to replace with. 
	 * Second POP is the replacement sequence. 
	 * Third POP is the string to do replacing in. 
	 * Returns string. 
	 */
	STRREPLACE (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value3 = request.popValue();
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value3.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRREPLACE call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRREPLACE call.");
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRREPLACE call.");

			String replacement = value3.asString();
			String pattern = value2.asString();
			String source = value1.asString();
			
			request.pushValue(Value.create(source.replace(pattern, replacement)));
		}
		
		@Override
		public String getGrouping()
		{
			return "String Operations";
		}
		
	},
	
	/**
	 * Replaces the first found RegEx pattern of characters in a string with another string.
	 * First POP is the string to replace with. 
	 * Second POP is the search regex pattern. 
	 * Third POP is the string to do replacing in. 
	 * Returns string. 
	 */
	STRREPLACEPATTERN (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value3 = request.popValue();
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value3.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRREPLACEPATTERN call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRREPLACEPATTERN call.");
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRREPLACEPATTERN call.");

			String replacement = value3.asString();
			String pattern = value2.asString();
			String source = value1.asString();

			request.pushValue(Value.create(source.replaceFirst(pattern, replacement)));
		}
		
		@Override
		public String getGrouping()
		{
			return "String Operations";
		}
		
	},

	/**
	 * Replaces every found RegEx pattern of characters in a string with another string.
	 * First POP is the string to replace with. 
	 * Second POP is the search regex pattern. 
	 * Third POP is the string to do replacing in. 
	 * Returns string. 
	 */
	STRREPLACEPATTERNALL (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value3 = request.popValue();
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value3.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRREPLACEPATTERNALL call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRREPLACEPATTERNALL call.");
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRREPLACEPATTERNALL call.");

			String replacement = value3.asString();
			String pattern = value2.asString();
			String source = value1.asString();

			request.pushValue(Value.create(source.replaceAll(pattern, replacement)));
		}
		
		@Override
		public String getGrouping()
		{
			return "String Operations";
		}
		
	},

	/**
	 * Returns the index of where a character sequence starts in a string. -1 is not found.
	 * First POP is what to search for. 
	 * Second POP is the string. 
	 * Returns integer.
	 */
	STRINDEX (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRINDEX call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRINDEX call.");

			String sequence = value2.asString();
			String str = value1.asString();
			
			request.pushValue(Value.create(str.indexOf(sequence)));
		}
		
		@Override
		public String getGrouping()
		{
			return "String Operations";
		}
		
	},
	
	/**
	 * Returns the last possible index of where a character sequence starts in a string. -1 is not found.
	 * First POP is what to search for. 
	 * Second POP is the string. 
	 * Returns integer.
	 */
	STRLASTINDEX (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRLASTINDEX call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRLASTINDEX call.");

			String sequence = value2.asString();
			String str = value1.asString();
			
			request.pushValue(Value.create(str.lastIndexOf(sequence)));
		}
		
		@Override
		public String getGrouping()
		{
			return "String Operations";
		}
		
	},
	
	/**
	 * Returns if a character sequence exists in a given string. True if so.
	 * First POP is what to search for. 
	 * Second POP is the string. 
	 * Returns boolean.
	 */
	STRCONTAINS (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRCONTAINS call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRCONTAINS call.");

			String sequence = value2.asString();
			String str = value1.asString();
			
			request.pushValue(Value.create(str.contains(sequence)));
		}
		
		@Override
		public String getGrouping()
		{
			return "String Operations";
		}
		
	},
	
	/**
	 * Returns if a character sequence matching a regular expression exists in a given string. True if so.
	 * First POP is what to search for (RegEx). 
	 * Second POP is the string. 
	 * Returns boolean.
	 */
	STRCONTAINSPATTERN (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRCONTAINSPATTERN call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRCONTAINSPATTERN call.");

			String pattern = value2.asString();
			String str = value1.asString();
			
			Pattern p = null;
			try {
				p = Pattern.compile(pattern);
			} catch (PatternSyntaxException e) {
				throw new UnexpectedValueTypeException("Expected valid RegEx in STRCONTAINSPATTERN call.");
			}
			
			request.pushValue(Value.create(p.matcher(str).find()));
		}
		
		@Override
		public String getGrouping()
		{
			return "String Operations";
		}
		
	},
	
	/**
	 * Returns if a string token exists in the given string (case-insensitive).
	 * A token is a whitespace-broken piece of a string.
	 * First POP is what to search for. 
	 * Second POP is the string. 
	 * Returns boolean.
	 */
	STRCONTAINSTOKEN (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRCONTAINSTOKEN call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRCONTAINSTOKEN call.");

			String token = value2.asString();
			String str = value1.asString();

			for (String t : str.split("\\s+"))
				if (t.equalsIgnoreCase(token))
				{
					request.pushValue(Value.create(true));
					return;
				}
			
			request.pushValue(Value.create(false));
		}
		
		@Override
		public String getGrouping()
		{
			return "String Operations";
		}
		
	},
	
	/**
	 * Gets if a string starts with a particular sequence of characters. True if so.
	 * First POP is the sequence to match. 
	 * Second POP is the string. 
	 * Returns boolean.
	 */
	STRSTARTSWITH (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRSTARTSWITH call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRSTARTSWITH call.");

			String sequence = value2.asString();
			String str = value1.asString();
			
			request.pushValue(Value.create(str.startsWith(sequence)));
		}
		
		@Override
		public String getGrouping()
		{
			return "String Operations";
		}
		
	},
	
	/**
	 * Gets if a string ends with a particular sequence of characters. True if so.
	 * First POP is the sequence to match. 
	 * Second POP is the string. 
	 * Returns boolean.
	 */
	STRENDSWITH (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRENDSWITH call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRENDSWITH call.");

			String sequence = value2.asString();
			String str = value1.asString();
			
			request.pushValue(Value.create(str.endsWith(sequence)));
		}
		
		@Override
		public String getGrouping()
		{
			return "String Operations";
		}
		
	},
	
	/**
	 * Gets a substring from a larger one.
	 * First POP is the ending index, exclusive. 
	 * Second POP is the starting index, inclusive. 
	 * Third POP is the string to divide. 
	 * Returns string. 
	 */
	SUBSTRING (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value3 = request.popValue();
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value3.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in SUBSTR call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in SUBSTR call.");
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in SUBSTR call.");

			int endIndex = (int)value3.asLong();
			int startIndex = (int)value2.asLong();
			String source = value1.asString();
			
			request.pushValue(Value.create(source.substring(startIndex, endIndex)));
		}
		
		@Override
		public String getGrouping()
		{
			return "String Operations";
		}
		
	},
	
	/**
	 * Gets a string converted to lowercase.
	 * POP is the string. 
	 * Returns string. 
	 */
	STRLOWER (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRLOWER call.");

			request.pushValue(Value.create(value.asString().toLowerCase()));
		}
		
		@Override
		public String getGrouping()
		{
			return "String Operations";
		}
		
	},
	
	/**
	 * Gets a string converted to uppercase.
	 * POP is the string. 
	 * Returns string. 
	 */
	STRUPPER (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRUPPER call.");

			request.pushValue(Value.create(value.asString().toUpperCase()));
		}
		
		@Override
		public String getGrouping()
		{
			return "String Operations";
		}
		
	},
	
	/**
	 * Gets a single character from a string.
	 * First POP is the index. 
	 * Second POP is the string. 
	 * Returns string or empty string if index is out of range.
	 */
	STRCHAR (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRCHAR call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRCHAR call.");

			int index = (int)value2.asLong();
			String str = value1.asString();
			
			if (index >= str.length() || index < 0)
				request.pushValue(Value.create(""));
			else
				request.pushValue(Value.create(String.valueOf(str.charAt(index))));
		}
		
		@Override
		public String getGrouping()
		{
			return "String Operations";
		}
		
	},
	
	/**
	 * Gets a string trimmed of whitespace at both ends.
	 * POP is the string. 
	 * Returns string. 
	 */
	STRTRIM (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRTRIM call.");

			request.pushValue(Value.create(value.asString().trim()));
		}
		
		@Override
		public String getGrouping()
		{
			return "String Operations";
		}
		
	},
	
	/**
	 * Gets the arithmetic floor of a number.
	 * POP is the number. 
	 * Returns float.
	 */
	FLOOR (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in FLOOR call.");

			request.pushValue(Value.create(Math.floor(value.asDouble())));
		}
		
		@Override
		public String getGrouping()
		{
			return "Mathematics";
		}
		
	},
	
	/**
	 * Gets the arithmetic ceiling of a number.
	 * POP is the number. 
	 * Returns float.
	 */
	CEILING (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in CEILING call.");

			request.pushValue(Value.create(Math.ceil(value.asDouble())));
		}
		
		@Override
		public String getGrouping()
		{
			return "Mathematics";
		}
		
	},
	
	/**
	 * Rounds a number to the nearest number.
	 * POP is the number. 
	 * Returns float.
	 */
	ROUND (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in ROUND call.");

			request.pushValue(Value.create((double)Math.round(value.asDouble())));
		}
		
		@Override
		public String getGrouping()
		{
			return "Mathematics";
		}
		
	},
	
	/**
	 * Fixed rounding: rounds a number to the nearest place after/before the decimal point.
	 * POP is the number. 
	 * Returns float.
	 */
	FIX (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value place = request.popValue();
			Value number = request.popValue();
			
			if (!number.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in FIX call.");
			if (!place.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in FIX call.");

			double d = number.asDouble();
			double f = place.asDouble();
			double t = Math.pow(10, f);

			request.pushValue(Value.create(Math.round(d * t) / t));
		}
		
		@Override
		public String getGrouping()
		{
			return "Mathematics";
		}
		
	},
	
	/**
	 * Gets the square root of a number.
	 * POP is the number.
	 * Returns float.
	 */
	SQRT (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value number = request.popValue();
			
			if (!number.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in SQRT call.");

			request.pushValue(Value.create(Math.sqrt(number.asDouble())));
		}
		
		@Override
		public String getGrouping()
		{
			return "Mathematics";
		}
		
	},
	
	/**
	 * Returns PI in this implementation.
	 * POPs nothing.
	 * Returns float.
	 */
	PI (/*Return: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			request.pushValue(Value.create(Math.PI));
		}
		
		@Override
		public String getGrouping()
		{
			return "Mathematics";
		}
		
	},
	
	/**
	 * Returns Euler's number in this implementation.
	 * POPs nothing.
	 * Returns float.
	 */
	E (/*Return: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			request.pushValue(Value.create(Math.E));
		}
		
		@Override
		public String getGrouping()
		{
			return "Mathematics";
		}
		
	},
	
	/**
	 * Returns sine of a number. Degrees are in radians.
	 * POP is the number.
	 * Returns float.
	 */
	SIN (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value number = request.popValue();
			
			if (!number.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in SIN call.");

			request.pushValue(Value.create(Math.sin(number.asDouble())));
		}
		
		@Override
		public String getGrouping()
		{
			return "Mathematics";
		}
		
	},
	
	/**
	 * Returns cosine of a number. Degrees are in radians.
	 * POP is the number.
	 * Returns float.
	 */
	COS (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value number = request.popValue();
			
			if (!number.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in COS call.");

			request.pushValue(Value.create(Math.cos(number.asDouble())));
		}
		
		@Override
		public String getGrouping()
		{
			return "Mathematics";
		}
		
	},
	
	/**
	 * Returns tangent of a number. Degrees are in radians.
	 * POP is the number.
	 * Returns float.
	 */
	TAN (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value number = request.popValue();
			
			if (!number.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in TAN call.");

			request.pushValue(Value.create(Math.tan(number.asDouble())));
		}
		
		@Override
		public String getGrouping()
		{
			return "Mathematics";
		}
		
	},
	
	/**
	 * Returns the minimum of two numbers.
	 * First POP is the second number.
	 * Second POP is the first number.
	 * Returns type of "winner."
	 */
	MIN (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value second = request.popValue();
			Value first = request.popValue();
			
			if (!second.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in MIN call.");
			if (!first.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in MIN call.");

			request.pushValue(Value.create(first.compareTo(second) <= 0 ? first : second));
		}
		
		@Override
		public String getGrouping()
		{
			return "Mathematics";
		}
		
	},
	
	/**
	 * Returns the maximum of two numbers.
	 * First POP is the second number.
	 * Second POP is the first number.
	 * Returns type of "winner."
	 */
	MAX (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value second = request.popValue();
			Value first = request.popValue();
			
			if (!second.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in MAX call.");
			if (!first.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in MAX call.");

			request.pushValue(Value.create(first.compareTo(second) > 0 ? first : second));
		}
		
		@Override
		public String getGrouping()
		{
			return "Mathematics";
		}
		
	},
	
	/**
	 * Returns the clamping of a number in an inclusive interval.
	 * First POP is the interval end.
	 * Second POP is the interval start.
	 * Third POP is the value.
	 * Returns float.
	 */
	CLAMP (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value valueEnd = request.popValue();
			Value valueStart = request.popValue();
			Value valueInput = request.popValue();
			
			if (!valueEnd.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in CLAMP call.");
			if (!valueStart.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in CLAMP call.");
			if (!valueInput.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in CLAMP call.");

			double value = valueInput.asDouble();
			double start = valueStart.asDouble();
			double end = valueEnd.asDouble();
			
			request.pushValue(Value.create(RMath.clampValue(value, start, end)));
		}
		
		@Override
		public String getGrouping()
		{
			return "Mathematics";
		}
		
	},
	
	/**
	 * Returns a random number from 0 to input-1.
	 * POP is the interval end.
	 * Returns integer or float, depending on input.
	 */
	IRANDOM (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value valueInput = request.popValue();
			
			if (!valueInput.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in RANDOM call.");

			long value = valueInput.asLong();

			Random random = request.getModuleContext().getRandom();
			
			if (value == 0)
				request.pushValue(Value.create(0));
			else
				request.pushValue(Value.create(Math.abs(random.nextLong()) % value));
		}
		
		@Override
		public String getGrouping()
		{
			return "Mathematics";
		}
		
	},
	
	/**
	 * Returns a random number from 0.0 (inclusive) to 1.0 (exclusive).
	 * POPs nothing.
	 * Returns float.
	 */
	FRANDOM (/*Return: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Random random = request.getModuleContext().getRandom();
			request.pushValue(Value.create(random.nextDouble()));
		}
		
		@Override
		public String getGrouping()
		{
			return "Mathematics";
		}
		
	},
	
	/**
	 * Returns a Gaussian-distribution random number using a provided mean and standard deviation.
	 * First POP is standard deviation.
	 * Second POP is the mean.
	 * Returns float.
	 */
	GRANDOM (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value valueStdDev = request.popValue();
			Value valueMean = request.popValue();
			
			if (!valueStdDev.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in GRANDOM call.");
			if (!valueMean.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in GRANDOM call.");

			// Box-Muller Approximate algorithm c/o Maxwell Collard on StackOverflow

			Random random = request.getModuleContext().getRandom();

			double stdDev = valueStdDev.asDouble();
			double mean = valueMean.asDouble();
			
			double u = 1.0 - random.nextDouble();
			double v = 1.0 - random.nextDouble();
			double stdNormal = Math.sqrt(-2.0 * Math.log(u)) * Math.sin(2.0 * Math.PI * v);
			double out = mean + stdDev * stdNormal;
			
			request.pushValue(Value.create(out));
		}
		
		@Override
		public String getGrouping()
		{
			return "Mathematics";
		}
		
	},
	
	/**
	 * Returns the current UTC time in milliseconds since the epoch.
	 * POPs nothing.
	 * Returns integer.
	 */
	TIME (/*Return: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			request.pushValue(Value.create(System.currentTimeMillis()));
		}
		
		@Override
		public String getGrouping()
		{
			return "Time";
		}
		
	},
	
	/**
	 * Returns the difference in time in seconds. 
	 * Assumes inputs are integers and millisecond time.
	 * First POP is second value.
	 * Second POP is first value.
	 * Returns integer.
	 */
	SECONDS (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value valueSecond = request.popValue();
			Value valueFirst = request.popValue();
			
			if (!valueSecond.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in SECONDS call.");
			if (!valueFirst.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in SECONDS call.");

			long first = valueFirst.asLong();
			long second = valueSecond.asLong();
			
			request.pushValue(Value.create((second - first) / 1000L));
		}
		
		@Override
		public String getGrouping()
		{
			return "Time";
		}
		
	},
	
	/**
	 * Returns the difference in time in minutes. 
	 * Assumes inputs are integers and millisecond time.
	 * First POP is second value.
	 * Second POP is first value.
	 * Returns integer.
	 */
	MINUTES (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value valueSecond = request.popValue();
			Value valueFirst = request.popValue();
			
			if (!valueSecond.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in MINUTES call.");
			if (!valueFirst.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in MINUTES call.");

			long first = valueFirst.asLong();
			long second = valueSecond.asLong();
			
			request.pushValue(Value.create((second - first) / (1000L * 60L)));
		}
		
		@Override
		public String getGrouping()
		{
			return "Time";
		}
		
	},
	
	/**
	 * Returns the difference in time in hours. 
	 * Assumes inputs are integers and millisecond time.
	 * First POP is second value.
	 * Second POP is first value.
	 * Returns integer.
	 */
	HOURS (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value valueSecond = request.popValue();
			Value valueFirst = request.popValue();
			
			if (!valueSecond.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in HOURS call.");
			if (!valueFirst.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in HOURS call.");

			long first = valueFirst.asLong();
			long second = valueSecond.asLong();
			
			request.pushValue(Value.create((second - first) / (1000L * 60L * 60L)));
		}
		
		@Override
		public String getGrouping()
		{
			return "Time";
		}
		
	},
	
	/**
	 * Returns the difference in time in days. 
	 * Assumes inputs are integers and millisecond time.
	 * First POP is second value.
	 * Second POP is first value.
	 * Returns integer.
	 */
	DAYS (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value valueSecond = request.popValue();
			Value valueFirst = request.popValue();
			
			if (!valueSecond.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in DAYS call.");
			if (!valueFirst.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in DAYS call.");

			long first = valueFirst.asLong();
			long second = valueSecond.asLong();
			
			request.pushValue(Value.create((second - first) / (1000L * 60L * 60L * 24L)));
		}
		
		@Override
		public String getGrouping()
		{
			return "Time";
		}
		
	},
	
	/**
	 * Formats a date/time as a string. 
	 * Assumes inputs are integers and millisecond time.
	 * First POP is formatting string.
	 * Second POP is date value.
	 * Returns string.
	 */
	FORMATTIME (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value valueSecond = request.popValue();
			Value valueFirst = request.popValue();
			
			if (!valueSecond.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in FORMATDATE call.");
			if (!valueFirst.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in FORMATDATE call.");

			long date = valueFirst.asLong();
			String format = valueSecond.asString();
			
			request.pushValue(Value.create((new SimpleDateFormat(format)).format(new Date(date))));
		}
		
		@Override
		public String getGrouping()
		{
			return "Time";
		}
		
	},
	
	/**
	 * Checks if an object has a specific name.
	 * First POP is name.
	 * Second POP is object. 
	 * Returns boolean.
	 */
	OBJECTHASNAME (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.OBJECT, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value nameValue = request.popValue();
			Value varObject = request.popValue();
			
			if (!nameValue.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in OBJECTHASNAME call.");
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in OBJECTHASNAME call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TObject object = moduleContext.resolveObject(varObject.asString());
			request.pushValue(Value.create(moduleContext.getOwnershipMap().checkObjectHasName(object, nameValue.asString())));
		}
		
		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},
	
	/**
	 * Checks if an object has a specific tag.
	 * First POP is tag.
	 * Second POP is object. 
	 * Returns boolean.
	 */
	OBJECTHASTAG (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.OBJECT, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value tagValue = request.popValue();
			Value varObject = request.popValue();
			
			if (!tagValue.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in OBJECTHASTAG call.");
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in OBJECTHASTAG call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TObject object = moduleContext.resolveObject(varObject.asString());
			request.pushValue(Value.create(moduleContext.getOwnershipMap().checkObjectHasTag(object, tagValue.asString())));
		}
		
		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},
	
	/**
	 * Adds an object name to an object. Changes nothing if it has the name already.
	 * First POP is name.
	 * Second POP is object. 
	 * Returns nothing.
	 */
	ADDOBJECTNAME (/*Return: */ null, /*Args: */ ArgumentType.OBJECT, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value nameValue = request.popValue();
			Value varObject = request.popValue();
			
			if (!nameValue.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in ADDOBJECTNAME call.");
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in ADDOBJECTNAME call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TObject object = moduleContext.resolveObject(varObject.asString());
			moduleContext.getOwnershipMap().addObjectName(object, nameValue.asString());
		}
		
		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},
	
	/**
	 * Adds an object tag to an object. Changes nothing if it has the tag already.
	 * First POP is tag.
	 * Second POP is object. 
	 * Returns nothing.
	 */
	ADDOBJECTTAG (/*Return: */ null, /*Args: */ ArgumentType.OBJECT, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value tagValue = request.popValue();
			Value varObject = request.popValue();
			
			if (!tagValue.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in ADDOBJECTTAG call.");
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in ADDOBJECTTAG call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TObject object = moduleContext.resolveObject(varObject.asString());
			moduleContext.getOwnershipMap().addObjectTag(object, tagValue.asString());
		}
		
		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},
	
	/**
	 * Adds an object tag to every object in a container-type.
	 * First POP is tag name.
	 * Second POP is container-type.
	 * Returns nothing.
	 */
	ADDOBJECTTAGTOALLIN (/*Return: */ null, /*Args: */ ArgumentType.OBJECT_CONTAINER, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value tagValue = request.popValue();
			Value varObjectContainer = request.popValue();
			
			if (!tagValue.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in ADDOBJECTTAGTOALLIN call.");
			if (!varObjectContainer.isObjectContainer())
				throw new UnexpectedValueTypeException("Expected object-container type in ADDOBJECTTAGTOALLIN call.");
			
			TAMEModuleContext moduleContext = request.getModuleContext();
			
			Iterable<TObject> objectList;
			
			if ((objectList = moduleContext.resolveObjectList(varObjectContainer)) == null)
				throw new UnexpectedValueTypeException("INTERNAL ERROR IN ADDOBJECTTOALLIN.");
			
			for (TObject object : objectList)
				moduleContext.getOwnershipMap().addObjectTag(object, tagValue.asString());
			
		}
		
		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},
	
	/**
	 * Removes an object name from an object. Does nothing if it doesn't have the name.
	 * First POP is name.
	 * Second POP is object. 
	 * Returns nothing.
	 */
	REMOVEOBJECTNAME (/*Return: */ null, /*Args: */ ArgumentType.OBJECT, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value nameValue = request.popValue();
			Value varObject = request.popValue();
			
			if (!nameValue.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in REMOVEOBJECTNAME call.");
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in REMOVEOBJECTNAME call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TObject object = moduleContext.resolveObject(varObject.asString());
			moduleContext.getOwnershipMap().removeObjectName(object, nameValue.asString());
		}
		
		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},
	
	/**
	 * Removes an object tag from an object. Does nothing if it doesn't have the tag.
	 * First POP is tag.
	 * Second POP is object. 
	 * Returns nothing.
	 */
	REMOVEOBJECTTAG (/*Return: */ null, /*Args: */ ArgumentType.OBJECT, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value tagValue = request.popValue();
			Value varObject = request.popValue();
			
			if (!tagValue.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in REMOVEOBJECTTAG call.");
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in REMOVEOBJECTTAG call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TObject object = moduleContext.resolveObject(varObject.asString());
			moduleContext.getOwnershipMap().removeObjectTag(object, tagValue.asString());
		}
		
		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},
	
	/**
	 * Removes an object tag from every object in a container-type.
	 * First POP is tag name.
	 * Second POP is container-type.
	 * Returns nothing.
	 */
	REMOVEOBJECTTAGFROMALLIN (/*Return: */ null, /*Args: */ ArgumentType.OBJECT_CONTAINER, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value tagValue = request.popValue();
			Value varObjectContainer = request.popValue();
			
			if (!tagValue.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in REMOVEOBJECTTAGFROMALLIN call.");
			if (!varObjectContainer.isObjectContainer())
				throw new UnexpectedValueTypeException("Expected object-container type in REMOVEOBJECTTAGFROMALLIN call.");
			
			TAMEModuleContext moduleContext = request.getModuleContext();
			
			Iterable<TObject> objectList;
			
			if ((objectList = moduleContext.resolveObjectList(varObjectContainer)) == null)
				throw new UnexpectedValueTypeException("INTERNAL ERROR IN REMOVEOBJECTTAGFROMALLIN.");
			
			for (TObject object : objectList)
				moduleContext.getOwnershipMap().removeObjectTag(object, tagValue.asString());
			
		}
		
		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},
	
	/**
	 * Adds an object to an object container.
	 * First POP is object.
	 * Second POP is object container element.
	 * Returns nothing.
	 */
	GIVEOBJECT (/*Return: */ null, /*Args: */ ArgumentType.OBJECT_CONTAINER, ArgumentType.OBJECT)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varObject = request.popValue();
			Value varObjectContainer = request.popValue();
			
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in GIVEOBJECT call.");
			if (!varObjectContainer.isObjectContainer())
				throw new UnexpectedValueTypeException("Expected object-container type in GIVEOBJECT call.");
			
			TAMEModuleContext moduleContext = request.getModuleContext();
			TObject object = moduleContext.resolveObject(varObject.asString());
			
			ObjectContainer element = (ObjectContainer)moduleContext.resolveElement(varObjectContainer);
			moduleContext.getOwnershipMap().addObjectToElement(object, element);
		}
		
		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},
	
	/**
	 * Removes an object from its owner, whatever it is.
	 * POP is object.
	 * Returns nothing.
	 */
	REMOVEOBJECT (/*Return: */ null, /*Args: */ ArgumentType.OBJECT)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varObject = request.popValue();
			
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in REMOVEOBJECT call.");
			
			TAMEModuleContext moduleContext = request.getModuleContext();
			TObject object = moduleContext.resolveObject(varObject.asString());
			request.getModuleContext().getOwnershipMap().removeObject(object);
		}
		
		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},
	
	/**
	 * Moves objects with a specific tag from a container and puts them in another.
	 * First POP is tag name.
	 * Second POP is destination object container.
	 * Third POP is source object container.
	 * Returns nothing.
	 */
	MOVEOBJECTSWITHTAG (/*Return: */ null, /*Args: */ ArgumentType.OBJECT_CONTAINER, ArgumentType.OBJECT_CONTAINER, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value tagValue = request.popValue();
			Value varObjectContainerDest = request.popValue();
			Value varObjectContainerSource = request.popValue();
			
			if (!tagValue.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in MOVEOBJECTSWITHTAG call.");
			if (!varObjectContainerDest.isObjectContainer())
				throw new UnexpectedValueTypeException("Expected object-container type in MOVEOBJECTSWITHTAG call.");
			if (!varObjectContainerSource.isObjectContainer())
				throw new UnexpectedValueTypeException("Expected object-container type in MOVEOBJECTSWITHTAG call.");
			
			TAMEModuleContext moduleContext = request.getModuleContext();
			
			Iterable<TObject> objectList;
			
			if ((objectList = moduleContext.resolveObjectList(varObjectContainerSource)) == null)
				throw new UnexpectedValueTypeException("INTERNAL ERROR IN MOVEOBJECTSWITHTAG.");
			
			String tag = tagValue.asString();
			TOwnershipMap ownershipMap = moduleContext.getOwnershipMap();
			ObjectContainer element = (ObjectContainer)moduleContext.resolveElement(varObjectContainerDest);
			for (TObject object : objectList) if (ownershipMap.checkObjectHasTag(object, tag)) 
				ownershipMap.addObjectToElement(object, element);
		}
		
		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},
	
	/**
	 * Counts the objects in a container.
	 * POP is object-container type.
	 * Returns integer.
	 */
	OBJECTCOUNT (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.OBJECT_CONTAINER)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varObjectContainer = request.popValue();

			if (!varObjectContainer.isObjectContainer())
				throw new UnexpectedValueTypeException("Expected object-container type in OBJECTCOUNT call.");
			
			TAMEModuleContext moduleContext = request.getModuleContext();
			ObjectContainer element = (ObjectContainer)moduleContext.resolveElement(varObjectContainer);
			request.pushValue(Value.create(moduleContext.getOwnershipMap().getObjectsOwnedByElementCount(element)));
		}
		
		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},
	
	/**
	 * Checks if an object is owned by an object container.
	 * First POP is object.
	 * Second POP is object container type.
	 * Returns boolean.
	 */
	HASOBJECT (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.OBJECT_CONTAINER, ArgumentType.OBJECT)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varObject = request.popValue();
			Value varObjectContainer = request.popValue();
			
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in HASOBJECT call.");
			if (!varObjectContainer.isObjectContainer())
				throw new UnexpectedValueTypeException("Expected object-container type in HASOBJECT call.");
			
			TAMEModuleContext moduleContext = request.getModuleContext();
			TObject object = moduleContext.resolveObject(varObject.asString());
			ObjectContainer element = (ObjectContainer)moduleContext.resolveElement(varObjectContainer);
			request.pushValue(Value.create(moduleContext.getOwnershipMap().checkElementHasObject(element, object)));
		}
		
		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},
	
	/**
	 * Checks if an object has no owner. True if so.
	 * POP is object.
	 * Returns boolean.
	 */
	OBJECTHASNOOWNER (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.OBJECT)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varObject = request.popValue();
			
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in OBJECTHASNOOWNER call.");
			
			TAMEModuleContext moduleContext = request.getModuleContext();
			TObject object = moduleContext.resolveObject(varObject.asString());
			request.pushValue(Value.create(request.getModuleContext().getOwnershipMap().checkObjectHasNoOwner(object)));
		}
		
		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	}, 
	
	/**
	 * Checks if a player in in a room. Returns true if current room or in the room stack.
	 * First POP is the room.
	 * Second POP is the player.
	 * Returns boolean.
	 */
	PLAYERISINROOM (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.PLAYER, ArgumentType.ROOM)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varRoom = request.popValue();
			Value varPlayer = request.popValue();
			
			if (varRoom.getType() != ValueType.ROOM)
				throw new UnexpectedValueTypeException("Expected room type in PLAYERISINROOM call.");
			if (varPlayer.getType() != ValueType.PLAYER)
				throw new UnexpectedValueTypeException("Expected player type in PLAYERISINROOM call.");
			
			TAMEModuleContext moduleContext = request.getModuleContext();
			TPlayer player = moduleContext.resolvePlayer(varPlayer.asString());
			TRoom room = moduleContext.resolveRoom(varRoom.asString());
			request.pushValue(Value.create(request.getModuleContext().getOwnershipMap().checkPlayerIsInRoom(player, room)));
		}
		
		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},
	
	/**
	 * Checks if a player can access a particular item.
	 * First POP is the object.
	 * Second POP is the player.
	 * Returns boolean.
	 */
	PLAYERCANACCESSOBJECT (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.PLAYER, ArgumentType.OBJECT)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varObject = request.popValue();
			Value varPlayer = request.popValue();
			
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in PLAYERCANACCESSOBJECT call.");
			if (varPlayer.getType() != ValueType.PLAYER)
				throw new UnexpectedValueTypeException("Expected player type in PLAYERCANACCESSOBJECT call.");
			
			TAMEModuleContext moduleContext = request.getModuleContext();

			TPlayer player = moduleContext.resolvePlayer(varPlayer.asString());
			TObject object = moduleContext.resolveObject(varObject.asString());
			
			request.pushValue(Value.create(TAMELogic.checkObjectAccessibility(request, response, player, object)));
		}
		
		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},
	
	/**
	 * Calls the appropriate "onBrowse" blocks on objects in an object container.
	 * POP is object container. 
	 * Returns nothing.
	 */
	BROWSE (/*Return: */ null, /*Args: */ ArgumentType.OBJECT_CONTAINER)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varObjectContainer = request.popValue();
			
			if (!varObjectContainer.isObjectContainer())
				throw new UnexpectedValueTypeException("Expected object-container type in BROWSE call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			ObjectContainer element = (ObjectContainer)moduleContext.resolveElement(varObjectContainer);
			TAMELogic.doBrowse(request, response, element);
		}
		
		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},
	
	/**
	 * Calls the appropriate "onBrowse" blocks on objects in an object container that have the provided tag.
	 * First POP is tag name. 
	 * Second POP is object container. 
	 * Returns nothing.
	 */
	BROWSETAGGED (/*Return: */ null, /*Args: */ ArgumentType.OBJECT_CONTAINER, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varTag = request.popValue();
			Value varObjectContainer = request.popValue();
			
			if (!varTag.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in BROWSETAGGED call.");
			if (!varObjectContainer.isObjectContainer())
				throw new UnexpectedValueTypeException("Expected object-container type in BROWSETAGGED call.");

			String tagName = varTag.asString(); 
			TAMEModuleContext moduleContext = request.getModuleContext();
			ObjectContainer element = (ObjectContainer)moduleContext.resolveElement(varObjectContainer);
			TAMELogic.doBrowse(request, response, element, tagName);
		}
		
		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},
	
	/**
	 * Returns true if the first element has the second element in its lineage.
	 * First POP is parent to test. 
	 * Second POP is the source element. 
	 * Returns nothing.
	 */
	ELEMENTHASANCESTOR (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.ELEMENT_ANY, ArgumentType.ELEMENT_ANY)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varParent = request.popValue();
			Value varElement = request.popValue();
			
			if (!varElement.isElement())
				throw new UnexpectedValueTypeException("Expected element type in ELEMENTHASANCESTOR call.");
			if (!varParent.isElement())
				throw new UnexpectedValueTypeException("Expected element type in ELEMENTHASANCESTOR call.");

			String parentIdentity = request.getModuleContext().resolveElement(varParent).getIdentity();
			TElement element = request.getModuleContext().resolveElement(varElement);
			
			// search up though lineage.
			boolean found = false;
			while (element != null)
			{
				if (element.getIdentity().equalsIgnoreCase(parentIdentity))
				{
					found = true;
					break;
				}
				element = element.getParent();
			}
			
			request.pushValue(Value.create(found));
		}
		
		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},
	
	/**
	 * Sets the current player. Calls unfocus on previous player, then focus on next player.
	 * POP is the player.
	 * Returns nothing.
	 */
	SETPLAYER (/*Return: */ null, /*Args: */ ArgumentType.PLAYER)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varPlayer = request.popValue();
			
			if (varPlayer.getType() != ValueType.PLAYER)
				throw new UnexpectedValueTypeException("Expected player type in SETPLAYER call.");

			TPlayer nextPlayer = request.getModuleContext().resolvePlayer(varPlayer.asString());
			TAMELogic.doPlayerSwitch(request, response, nextPlayer);
		}
		
		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},

	/**
	 * Sets the current room and clears the stack (for the current player).
	 * POP is the new room.
	 * Returns nothing.
	 */
	SETROOM (/*Return: */ null, /*Args: */ ArgumentType.PLAYER, ArgumentType.ROOM)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varRoom = request.popValue();
			Value varPlayer = request.popValue();

			if (varRoom.getType() != ValueType.ROOM)
				throw new UnexpectedValueTypeException("Expected room type in SETROOM call.");
			if (varPlayer.getType() != ValueType.PLAYER)
				throw new UnexpectedValueTypeException("Expected player type in SETROOM call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TRoom nextRoom = moduleContext.resolveRoom(varRoom.asString());
			TPlayer player = moduleContext.resolvePlayer(varPlayer.asString());

			TAMELogic.doRoomSwitch(request, response, player, nextRoom);
		}

		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},

	/**
	 * Pushes a room onto the room stack (for the current player).
	 * POP is the new room.
	 * Returns nothing.
	 */
	PUSHROOM (/*Return: */ null, /*Args: */ ArgumentType.PLAYER, ArgumentType.ROOM)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varRoom = request.popValue();
			Value varPlayer = request.popValue();
			
			if (varRoom.getType() != ValueType.ROOM)
				throw new UnexpectedValueTypeException("Expected room type in PUSHROOM call.");
			if (varPlayer.getType() != ValueType.PLAYER)
				throw new UnexpectedValueTypeException("Expected player type in PUSHROOM call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TRoom nextRoom = moduleContext.resolveRoom(varRoom.asString());
			TPlayer player = moduleContext.resolvePlayer(varPlayer.asString());
			
			// push new room on the player's stack and call focus.
			TAMELogic.doRoomPush(request, response, player, nextRoom);
		}

		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},

	/**
	 * Pops a room off of the room stack (for the current player).
	 * POPs nothing.
	 * Returns nothing.
	 */
	POPROOM (/*Return: */ null, /*Args: */ ArgumentType.PLAYER)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varPlayer = request.popValue();

			if (varPlayer.getType() != ValueType.PLAYER)
				throw new UnexpectedValueTypeException("Expected player type in POPROOM call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TPlayer player = moduleContext.resolvePlayer(varPlayer.asString());
			
			TRoom currentRoom = moduleContext.getOwnershipMap().getCurrentRoom(player);
			
			if (currentRoom == null)
				throw new ErrorInterrupt("No rooms for player "+player);
			
			TAMELogic.doRoomPop(request, response, player);
		}

		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},

	/**
	 * Pops a room off of the room stack and pushes a new one (for the current player).
	 * POP is the new room.
	 * Returns nothing.
	 */
	SWAPROOM (/*Return: */ null, /*Args: */ ArgumentType.PLAYER, ArgumentType.ROOM)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varRoom = request.popValue();
			Value varPlayer = request.popValue();

			if (varRoom.getType() != ValueType.ROOM)
				throw new UnexpectedValueTypeException("Expected room type in SWAPROOM call.");
			if (varPlayer.getType() != ValueType.PLAYER)
				throw new UnexpectedValueTypeException("Expected player type in SWAPROOM call.");


			TAMEModuleContext moduleContext = request.getModuleContext();
			TPlayer player = moduleContext.getOwnershipMap().getCurrentPlayer();
			
			if (player == null)
				throw new ErrorInterrupt("No current player!");

			TRoom nextRoom = moduleContext.resolveRoom(varRoom.asString()); 
			TRoom currentRoom = moduleContext.getOwnershipMap().getCurrentRoom(player);
			
			if (currentRoom == null)
				throw new ErrorInterrupt("No rooms for current player!");
			
			TAMELogic.doRoomPop(request, response, player);
			TAMELogic.doRoomPush(request, response, player, nextRoom);
		}

		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},

	/**
	 * Checks if the current player is the one provided.
	 * POP is the player.
	 * Returns boolean.
	 */
	CURRENTPLAYERIS (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.PLAYER)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varPlayer = request.popValue();
			
			if (varPlayer.getType() != ValueType.PLAYER)
				throw new UnexpectedValueTypeException("Expected player type in CURRENTPLAYERIS call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TPlayer player = moduleContext.resolvePlayer(varPlayer.asString());
			TPlayer currentPlayer = moduleContext.getOwnershipMap().getCurrentPlayer();
			
			request.pushValue(Value.create(currentPlayer != null && player.equals(currentPlayer)));
		}

		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},

	/**
	 * Checks if there is no current player.
	 * POPs nothing.
	 * Returns boolean.
	 */
	NOCURRENTPLAYER (/*Return: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			TAMEModuleContext moduleContext = request.getModuleContext();
			TPlayer player = moduleContext.getOwnershipMap().getCurrentPlayer();
			request.pushValue(Value.create(player == null));
		}

		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},

	/**
	 * Checks if the current room is the one provided.
	 * POP is the room.
	 * Returns boolean.
	 */
	CURRENTROOMIS (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.PLAYER, ArgumentType.ROOM)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varRoom = request.popValue();
			Value varPlayer = request.popValue();
			
			if (varPlayer.getType() != ValueType.PLAYER)
				throw new UnexpectedValueTypeException("Expected player type in CURRENTROOMIS call.");
			if (varRoom.getType() != ValueType.ROOM)
				throw new UnexpectedValueTypeException("Expected room type in CURRENTROOMIS call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TPlayer player = moduleContext.resolvePlayer(varPlayer.asString());
			TRoom room = moduleContext.resolveRoom(varRoom.asString());
			TRoom currentRoom = moduleContext.getOwnershipMap().getCurrentRoom(player);
			
			request.pushValue(Value.create(currentRoom != null && room.equals(currentRoom)));
		}

		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},

	/**
	 * Checks if there is no current room.
	 * POPs nothing.
	 * Returns boolean.
	 */
	NOCURRENTROOM (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.PLAYER)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varPlayer = request.popValue();
			
			if (varPlayer.getType() != ValueType.PLAYER)
				throw new UnexpectedValueTypeException("Expected player type in NOCURRENTROOM call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TPlayer player = moduleContext.resolvePlayer(varPlayer.asString());
			TRoom room = moduleContext.getOwnershipMap().getCurrentRoom(player);
			request.pushValue(Value.create(room == null));
		}

		@Override
		public String getGrouping()
		{
			return "Elements";
		}
		
	},
	
	/**
	 * Pushes the identity of an element onto the stack.
	 * POP is the element.
	 * Returns string.
	 */
	IDENTITY (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.ELEMENT)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varElement = request.popValue();

			if (!varElement.isElement())
				throw new UnexpectedValueTypeException("Expected element type in IDENTITY call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TElement element = moduleContext.resolveElement(varElement);
			request.pushValue(Value.create(element.getIdentity()));
		}
		
		@Override
		public String getGrouping()
		{
			return "Miscellaneous";
		}
		
	},

	/**
	 * Pushes the value of a header value of the TAME module.
	 * Returns string, always, regardless of how it is defined, and as it as defined without alteration.
	 */
	HEADER (/*Return: */ ArgumentType.VALUE, /* Args */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value headerName = request.popValue();

			if (!headerName.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in HEADER call.");

			request.pushValue(Value.create(request.getModuleContext().getModule().getHeader().getAttribute(headerName.asString())));
		}
		
		@Override
		public String getGrouping()
		{
			return "Miscellaneous";
		}
		
	}

	;
	
	/** Array to get around multiple allocations. */
	public static final TAMECommand[] VALUES = values();
	
	private boolean language;
	private boolean internal;
	private ArgumentType returnType;
	private ArgumentType[] argumentTypes;
	
	private TAMECommand()
	{
		this(true, true, null, null);
	}

	private TAMECommand(boolean internal)
	{
		this(internal, false, null, null);
	}
	
	private TAMECommand(boolean internal, boolean block)
	{
		this(internal, block, null, null);
	}
	
	private TAMECommand(ArgumentType returnType, ArgumentType ... argumentTypes)
	{
		this(false, false, returnType, argumentTypes);
	}

	private TAMECommand(boolean internal, boolean language, ArgumentType returnType, ArgumentType[] argumentTypes)
	{
		this.language = language;
		this.internal = internal;
		this.returnType = returnType;
		this.argumentTypes = argumentTypes;
	}

	@Override
	public boolean isLanguage() 
	{
		return language;
	}
	
	@Override
	public boolean isInternal() 
	{
		return internal;
	}
	
	@Override
	public ArgumentType getReturnType()
	{
		return returnType;
	}

	@Override
	public ArgumentType[] getArgumentTypes() 
	{
		return argumentTypes;
	}

	/**
	 * Performs the command.
	 * @param request the TAMERequest context.
	 * @param response the TAMEResponse object.
	 * @param blockLocal the local variables on the block call.
	 * @param command the command origin.
	 * @throws TAMEInterrupt if an interrupt occurs. 
	 */
	protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
	{
		throw new RuntimeException("UNIMPLEMENTED COMMAND");
	}
	
	/**
	 * Gets the grouping name for this command (for documentation sorting).
	 * @return the grouping name.
	 */
	public abstract String getGrouping();
	
	/**
	 * Increments the runaway command counter and calls the command.  
	 * @param request the request object.
	 * @param response the response object.
	 * @param blockLocal the local variables on the block call.
	 * @param command the command object.
	 * @throws TAMEInterrupt if an interrupt occurs. 
	 */
	public final void execute(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
	{
		doCommand(request, response, blockLocal, command);
		response.incrementAndCheckCommandsExecuted(request.getModuleContext().getCommandRunawayMax());
	}
	
}
