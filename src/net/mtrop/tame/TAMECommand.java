/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.blackrook.commons.math.RMath;

import net.mtrop.tame.element.TAction;
import net.mtrop.tame.element.TAction.Type;
import net.mtrop.tame.element.TContainer;
import net.mtrop.tame.element.TElement;
import net.mtrop.tame.element.TObject;
import net.mtrop.tame.element.TPlayer;
import net.mtrop.tame.element.TRoom;
import net.mtrop.tame.element.TWorld;
import net.mtrop.tame.element.context.TElementContext;
import net.mtrop.tame.element.context.TObjectContext;
import net.mtrop.tame.element.context.TOwnershipMap;
import net.mtrop.tame.exception.ModuleExecutionException;
import net.mtrop.tame.exception.UnexpectedValueException;
import net.mtrop.tame.exception.UnexpectedValueTypeException;
import net.mtrop.tame.interrupt.BreakInterrupt;
import net.mtrop.tame.interrupt.EndInterrupt;
import net.mtrop.tame.interrupt.ContinueInterrupt;
import net.mtrop.tame.interrupt.ErrorInterrupt;
import net.mtrop.tame.interrupt.QuitInterrupt;
import net.mtrop.tame.interrupt.TAMEInterrupt;
import net.mtrop.tame.lang.ArgumentType;
import net.mtrop.tame.lang.ArithmeticOperator;
import net.mtrop.tame.lang.Block;
import net.mtrop.tame.lang.BlockEntry;
import net.mtrop.tame.lang.BlockEntryType;
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
				request.peekContext().setPersistantValue(variableName, value);
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
			Value varObject = command.getOperand0();
			Value variable = command.getOperand1();
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in POPELEMENTVALUE call.");
			if (!variable.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in POPELEMENTVALUE call.");
			if (!varObject.isElement())
				throw new UnexpectedValueTypeException("Expected element type in POPELEMENTVALUE call.");
			
			String variableName = variable.asString();
			String objectName = varObject.asString();

			switch (varObject.getType())
			{
				default:
					throw new UnexpectedValueTypeException("INTERNAL ERROR IN POPELEMENTVALUE.");
				case OBJECT:
					request.getModuleContext().resolveObjectContext(objectName).setPersistantValue(variableName, value);
					break;
				case ROOM:
					request.getModuleContext().resolveRoomContext(objectName).setPersistantValue(variableName, value);
					break;
				case PLAYER:
					request.getModuleContext().resolvePlayerContext(objectName).setPersistantValue(variableName, value);
					break;
				case CONTAINER:
					request.getModuleContext().resolveContainerContext(objectName).setPersistantValue(variableName, value);
					break;
				case WORLD:
					request.getModuleContext().resolveWorldContext().setPersistantValue(variableName, value);
					break;
			}
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
					request.pushValue(request.peekContext().getPersistantValue(variableName));
			}
			else
			{
				request.pushValue(value);
			}
			
		}
		
	},
	
	/**
	 * [INTERNAL] Resolves value of a variable on an object and pushes the value.
	 * Operand0 is the object. 
	 * Operand1 is the variable. 
	 * Pushes the resolved value. 
	 */
	PUSHELEMENTVALUE (true)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws ErrorInterrupt
		{
			Value varObject = command.getOperand0();
			Value variable = command.getOperand1();

			if (!variable.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in PUSHELEMENTVALUE call.");
			if (!varObject.isElement())
				throw new UnexpectedValueTypeException("Expected element type in PUSHELEMENTVALUE call.");

			String objectName = varObject.asString();
			String variableName = variable.asString();

			switch (varObject.getType())
			{
				default:
					throw new UnexpectedValueTypeException("INTERNAL ERROR IN PUSHELEMENTVALUE.");
				case OBJECT:
					request.pushValue(request.getModuleContext().resolveObjectVariableValue(objectName, variableName));
					break;
				case ROOM:
					request.pushValue(request.getModuleContext().resolveRoomVariableValue(objectName, variableName));
					break;
				case PLAYER:
					request.pushValue(request.getModuleContext().resolvePlayerVariableValue(objectName, variableName));
					break;
				case CONTAINER:
					request.pushValue(request.getModuleContext().resolveContainerVariableValue(objectName, variableName));
					break;
				case WORLD:
					request.pushValue(request.getModuleContext().resolveWorldVariableValue(variableName));
					break;
			}
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

			doArithmeticStackFunction(request, response, (int)functionValue.asLong());
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
			conditional.call(request, response, blockLocal);
			
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
				success.call(request, response, blockLocal);
			}
			else
			{
				response.trace(request, "Result %s evaluates false.", value);
				Block failure = command.getFailureBlock();
				if (failure != null)
				{
					response.trace(request, "Calling IF failure block...");
					failure.call(request, response, blockLocal);
				}
			}
			
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
					success.call(request, response, blockLocal);
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
			conditional.call(request, response, blockLocal);
			
			// get remaining expression value.
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type after WHILE conditional block execution.");
	
			boolean out = value.asBoolean();
			response.trace(request, "Result %s evaluates %b.", value, out);
			return out; 
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
				init.call(request, response, blockLocal); 
				callConditional(request, response, blockLocal, command); 
				response.trace(request, "Calling FOR stepping block..."), 
				step.call(request, response, blockLocal)
			)
			{
				try {
					response.trace(request, "Calling FOR success block...");
					success.call(request, response, blockLocal);
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
				throw new ModuleExecutionException("Conditional block for WHILE does NOT EXIST!");
			conditional.call(request, response, blockLocal);
			
			// get remaining expression value.
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type after WHILE conditional block execution.");
	
			boolean out = value.asBoolean();
			response.trace(request, "Result %s evaluates %b.", value, out);
			return out; 
		}
		
	}, 
	
	/**
	 * Calls a procedure local to the current context's owner's lineage.
	 * First POP is the procedure name/value. 
	 * Returns nothing.
	 */
	CALL (/*Return: */ null, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value procedureName = request.popValue();
			if (!procedureName.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in CALL call.");
			
			if (request.peekContext() == null)
				throw new ModuleExecutionException("Attempted CALL call without a context!");
			
			TElementContext<?> elementContext = request.peekContext();
			TElement element = elementContext.getElement();
			
			Block block = element.resolveBlock(BlockEntry.create(BlockEntryType.PROCEDURE, procedureName));
			if (block != null)
				TAMELogic.callBlock(request, response, elementContext, block);
			else
				response.addCue(CUE_ERROR, "No such procedure ("+procedureName.asString()+") in lineage of element " + element);
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
		
	},
	
	/**
	 * Adds a TIP cue to the response.
	 * POP is the message to send. 
	 * Returns nothing. 
	 */
	TIP (/*Return: */ null, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in TIP call.");

			response.addCue(CUE_TIP, value.asString());
		}
		
	},
	
	/**
	 * Adds a INFO cue to the response.
	 * POP is the message to send. 
	 * Returns nothing. 
	 */
	INFO (/*Return: */ null, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in INFO call.");

			response.addCue(CUE_INFO, value.asString());
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
		
	},
	
	/**
	 * Gets the length of a string.
	 * POP is the value, cast to a string. 
	 * Returns integer. 
	 */
	STRLEN (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRLEN call.");

			request.pushValue(Value.create(value.asString().length()));
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
		
	},
	
	/**
	 * Returns the index of where a pattern starts in a string. -1 is not found.
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
		
	},
	
	/**
	 * Returns the last possible index of where a pattern starts in a string. -1 is not found.
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
		
	},
	
	/**
	 * Returns if a character sequence matching a regular expression exists in a given string. True if so.\
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
				throw new ErrorInterrupt("Expected valid expression in STRCONTAINSPATTERN call.");
			}
			
			request.pushValue(Value.create(p.matcher(str).find()));
		}
		
	},
	
	/**
	 * Returns if a string token exists in the given string (case-insensitive).
	 * A token is a whitespace-broken piece.
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
		
	},
	
	/**
	 * Gets a substring from a larger one.
	 * First POP is the ending index, exclusive. 
	 * Second POP is the starting index, inclusive. 
	 * Third POP is the string to divide. 
	 * Returns string. 
	 */
	SUBSTR (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
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
		
	},
	
	/**
	 * Returns the clamping of a number in an inclusive interval.
	 * First POP is the interval end.
	 * Second POP is the interval end.
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
		
	},
	
	/**
	 * Returns a random number from 0 to input-1.
	 * POP is the interval end.
	 * Returns integer or float.
	 */
	RANDOM (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value valueInput = request.popValue();
			
			if (!valueInput.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in RANDOM call.");

			if (valueInput.isInteger() || valueInput.isBoolean())
			{
				Random random = request.getModuleContext().getRandom();
				long value = valueInput.asLong();
				
				if (value == 0)
					request.pushValue(Value.create(0));
				else
					request.pushValue(Value.create(Math.abs(random.nextLong()) % value));
			}
			else
			{
				Random random = request.getModuleContext().getRandom();
				double value = valueInput.asDouble();
				if (value == 0.0)
					request.pushValue(Value.create(0.0));
				else
					request.pushValue(Value.create(random.nextDouble() * value));
			}
			
		}
		
	},
	
	/**
	 * Returns a random number from 0.0 to 1.0 exclusive.
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
		
	},
	
	/**
	 * Returns a Gaussian random number.
	 * POPs nothing.
	 * Returns float.
	 */
	GRANDOM (/*Return: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Random random = request.getModuleContext().getRandom();
			request.pushValue(Value.create(random.nextGaussian()));
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
			
			if ((objectList = resolveObjectList(varObjectContainer, moduleContext)) == null)
				throw new UnexpectedValueTypeException("INTERNAL ERROR IN ADDOBJECTTOALLIN.");
			
			for (TObject object : objectList)
				moduleContext.getOwnershipMap().addObjectTag(object, tagValue.asString());
			
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
			
			if ((objectList = resolveObjectList(varObjectContainer, moduleContext)) == null)
				throw new UnexpectedValueTypeException("INTERNAL ERROR IN REMOVEOBJECTTAGFROMALLIN.");
			
			for (TObject object : objectList)
				moduleContext.getOwnershipMap().removeObjectTag(object, tagValue.asString());
			
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
			
			switch (varObjectContainer.getType())
			{
				default:
					throw new UnexpectedValueTypeException("INTERNAL ERROR IN GIVEOBJECT.");
				case ROOM:
					moduleContext.getOwnershipMap().addObjectToRoom(object, moduleContext.resolveRoom(varObjectContainer.asString()));
					break;
				case PLAYER:
					moduleContext.getOwnershipMap().addObjectToPlayer(object, moduleContext.resolvePlayer(varObjectContainer.asString()));
					break;
				case CONTAINER:
					moduleContext.getOwnershipMap().addObjectToContainer(object, moduleContext.resolveContainer(varObjectContainer.asString()));
					break;
				case WORLD:
					moduleContext.getOwnershipMap().addObjectToWorld(object, moduleContext.resolveWorld());
					break;
			}
			
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
			
			if ((objectList = resolveObjectList(varObjectContainerSource, moduleContext)) == null)
				throw new UnexpectedValueTypeException("INTERNAL ERROR IN MOVEOBJECTSWITHTAG.");
			
			String tag = tagValue.asString();
			TOwnershipMap ownershipMap = moduleContext.getOwnershipMap();
			
			switch (varObjectContainerDest.getType())
			{
				default:
					throw new UnexpectedValueTypeException("INTERNAL ERROR IN MOVEOBJECTSWITHTAG.");
				
				case ROOM:
				{
					TRoom room = moduleContext.resolveRoom(varObjectContainerDest.asString());
					for (TObject object : objectList) if (ownershipMap.checkObjectHasTag(object, tag)) 
						ownershipMap.addObjectToRoom(object, room);
				}
				break;
				
				case PLAYER:
				{
					TPlayer player = moduleContext.resolvePlayer(varObjectContainerDest.asString());
					for (TObject object : objectList) if (ownershipMap.checkObjectHasTag(object, tag)) 
						ownershipMap.addObjectToPlayer(object, player);
				}
				break;
				
				case CONTAINER:
				{
					TContainer container = moduleContext.resolveContainer(varObjectContainerDest.asString());
					for (TObject object : objectList) if (ownershipMap.checkObjectHasTag(object, tag)) 
						ownershipMap.addObjectToContainer(object, container);
				}
				break;

				case WORLD:
				{
					TWorld world = moduleContext.resolveWorld();
					for (TObject object : objectList) if (ownershipMap.checkObjectHasTag(object, tag)) 
						ownershipMap.addObjectToWorld(object, world);
				}
				break;
				
			}
			
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
			
			int count = 0;
			
			switch (varObjectContainer.getType())
			{
				default:
					throw new UnexpectedValueTypeException("INTERNAL ERROR IN OBJECTCOUNT.");
				case ROOM:
					count = moduleContext.getOwnershipMap().getObjectsOwnedByRoomCount(moduleContext.resolveRoom(varObjectContainer.asString()));
					break;
				case PLAYER:
					count = moduleContext.getOwnershipMap().getObjectsOwnedByPlayerCount(moduleContext.resolvePlayer(varObjectContainer.asString()));
					break;
				case CONTAINER:
					count = moduleContext.getOwnershipMap().getObjectsOwnedByContainerCount(moduleContext.resolveContainer(varObjectContainer.asString()));
					break;
				case WORLD:
					count = moduleContext.getOwnershipMap().getObjectsOwnedByWorldCount(moduleContext.resolveWorld());
					break;
			}

			request.pushValue(Value.create(count));
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
			
			boolean contained = false; 
			
			switch (varObjectContainer.getType())
			{
				default:
					throw new UnexpectedValueTypeException("INTERNAL ERROR IN HASOBJECT.");
				case ROOM:
					contained = moduleContext.getOwnershipMap().checkRoomHasObject(moduleContext.resolveRoom(varObjectContainer.asString()), object);
					break;
				case PLAYER:
					contained = moduleContext.getOwnershipMap().checkPlayerHasObject(moduleContext.resolvePlayer(varObjectContainer.asString()), object);
					break;
				case CONTAINER:
					contained = moduleContext.getOwnershipMap().checkContainerHasObject(moduleContext.resolveContainer(varObjectContainer.asString()), object);
					break;
				case WORLD:
					contained = moduleContext.getOwnershipMap().checkWorldHasObject(moduleContext.resolveWorld(), object);
					break;
			}

			request.pushValue(Value.create(contained));
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
			
			request.pushValue(Value.create(checkObjectAccessibility(request, response, player, object)));
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
			
			switch (varObjectContainer.getType())
			{
				default:
					throw new UnexpectedValueTypeException("INTERNAL ERROR IN BROWSE.");
				case ROOM:
					doRoomBrowse(request, response, moduleContext.resolveRoom(varObjectContainer.asString()));
					break;
				case PLAYER:
					doPlayerBrowse(request, response, moduleContext.resolvePlayer(varObjectContainer.asString()));
					break;
				case CONTAINER:
					doContainerBrowse(request, response, moduleContext.resolveContainer(varObjectContainer.asString()));
					break;
				case WORLD:
					doWorldBrowse(request, response, moduleContext.resolveWorld());
					break;
			}
			
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
			doPlayerSwitch(request, response, nextPlayer);
		}
		
	},

	/**
	 * Sets the current room and clears the stack (for the current player).
	 * POP is the new room.
	 * Returns nothing.
	 */
	SETROOM (/*Return: */ null, /*Args: */ ArgumentType.ROOM)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varRoom = request.popValue();
			
			if (varRoom.getType() != ValueType.ROOM)
				throw new UnexpectedValueTypeException("Expected room type in SETROOM call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TRoom nextRoom = moduleContext.resolveRoom(varRoom.asString());
			TPlayer player = moduleContext.getCurrentPlayer();

			if (player == null)
				throw new ErrorInterrupt("No current player!");

			doRoomSwitch(request, response, player, nextRoom);
		}

	},

	/**
	 * Pushes a room onto the room stack (for the current player).
	 * ErrorInterrupt if no current player.
	 * POP is the new room.
	 * Returns nothing.
	 */
	PUSHROOM (/*Return: */ null, /*Args: */ ArgumentType.ROOM)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varRoom = request.popValue();
			
			if (varRoom.getType() != ValueType.ROOM)
				throw new UnexpectedValueTypeException("Expected room type in PUSHROOM call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TRoom nextRoom = moduleContext.resolveRoom(varRoom.asString());
			TPlayer player = moduleContext.getCurrentPlayer();
			
			if (player == null)
				throw new ErrorInterrupt("No current player!");
			
			// push new room on the player's stack and call focus.
			doRoomPush(request, response, player, nextRoom);
		}

	},

	/**
	 * Pops a room off of the room stack (for the current player).
	 * ErrorInterrupt if no current player or no rooms on the room stack for the player.
	 * POPs nothing.
	 * Returns nothing.
	 */
	POPROOM (/*Return: */ null)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			TAMEModuleContext moduleContext = request.getModuleContext();
			TPlayer player = moduleContext.getCurrentPlayer();
			
			if (player == null)
				throw new ErrorInterrupt("No current player!");

			TRoom currentRoom = moduleContext.getOwnershipMap().getCurrentRoom(player);
			
			if (currentRoom == null)
				throw new ErrorInterrupt("No rooms for current player!");
			
			doRoomPop(request, response, player);
		}

	},

	/**
	 * Pops a room off of the room stack and pushes a new one (for the current player).
	 * ErrorInterrupt if no current player or no rooms on the room stack for the player.
	 * POP is the new room.
	 * Returns nothing.
	 */
	SWAPROOM (/*Return: */ null, /*Args: */ ArgumentType.ROOM)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varRoom = request.popValue();
			
			if (varRoom.getType() != ValueType.ROOM)
				throw new UnexpectedValueTypeException("Expected room type in SWAPROOM call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TPlayer player = moduleContext.getCurrentPlayer();
			
			if (player == null)
				throw new ErrorInterrupt("No current player!");

			TRoom nextRoom = moduleContext.resolveRoom(varRoom.asString()); 
			TRoom currentRoom = moduleContext.getOwnershipMap().getCurrentRoom(player);
			
			if (currentRoom == null)
				throw new ErrorInterrupt("No rooms for current player!");
			
			doRoomPop(request, response, player);
			doRoomPush(request, response, player, nextRoom);
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
			TPlayer currentPlayer = moduleContext.getCurrentPlayer();
			
			request.pushValue(Value.create(currentPlayer != null && player.equals(currentPlayer)));
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
			TPlayer player = moduleContext.getCurrentPlayer();
			request.pushValue(Value.create(player == null));
		}

	},

	/**
	 * Checks if the current room is the one provided.
	 * POP is the room.
	 * Returns boolean.
	 */
	CURRENTROOMIS (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.ROOM)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varRoom = request.popValue();
			
			if (varRoom.getType() != ValueType.ROOM)
				throw new UnexpectedValueTypeException("Expected player type in CURRENTROOMIS call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TRoom room = moduleContext.resolveRoom(varRoom.asString());
			TRoom currentRoom = moduleContext.getCurrentRoom();
			
			request.pushValue(Value.create(currentRoom != null && room.equals(currentRoom)));
		}

	},

	/**
	 * Checks if there is no current room.
	 * POPs nothing.
	 * Returns boolean.
	 */
	NOCURRENTROOM (/*Return: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			TAMEModuleContext moduleContext = request.getModuleContext();
			TRoom room = moduleContext.getCurrentRoom();
			request.pushValue(Value.create(room == null));
		}

	},
	
	/**
	 * Enqueues an general action to perform after the current one finishes.
	 * POP is the action.
	 * Returns nothing.
	 */
	QUEUEACTION (/*Return: */ null, /*Args: */ ArgumentType.ACTION)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
		{
			Value varAction = request.popValue();
			
			if (varAction.getType() != ValueType.ACTION)
				throw new UnexpectedValueTypeException("Expected action type in QUEUEACTION call.");

			TAction action = request.getModuleContext().resolveAction(varAction.asString());
			
			if (action.getType() != Type.GENERAL)
				throw new ErrorInterrupt(action.getIdentity() + " is not a general action.");
			else
				request.addActionItem(TAMEAction.create(action));
		}

	},

	/**
	 * Enqueues a open/modal action to perform after the current one finishes.
	 * First POP is the modal or open target.
	 * Second POP is the action.
	 * Returns nothing.
	 */
	QUEUEACTIONSTRING (/*Return: */ null, /*Args: */ ArgumentType.ACTION, ArgumentType.VALUE)
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
				throw new ErrorInterrupt(action.getIdentity() + " is not a modal nor open action.");
			else
				request.addActionItem(TAMEAction.create(action, target));
		}

	},
	
	/**
	 * Enqueues a transitive action to perform after the current one finishes.
	 * First POP is the object.
	 * Second POP is the action.
	 * Returns nothing.
	 */
	QUEUEACTIONOBJECT (/*Return: */ null, /*Args: */ ArgumentType.ACTION, ArgumentType.OBJECT)
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
				throw new ErrorInterrupt(action.getIdentity() + " is not a transitive nor ditransitive action.");
			else
				request.addActionItem(TAMEAction.create(action, object));
		}

	},
	
	/**
	 * Enqueues a ditransitive action to perform after the current one finishes.
	 * First POP is the second object.
	 * Second POP is the object.
	 * Third POP is the action.
	 * Returns nothing.
	 */
	QUEUEACTIONOBJECT2 (/*Return: */ null, /*Args: */ ArgumentType.ACTION, ArgumentType.OBJECT, ArgumentType.OBJECT)
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
				throw new ErrorInterrupt(action.getIdentity() + " is not a ditransitive action.");
			else
				request.addActionItem(TAMEAction.create(action, object, object2));
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
			Value element = request.popValue();

			TAMEModuleContext moduleContext = request.getModuleContext();
			
			// IMPORTANT: Must resolve: the passed-in value could be the "current" room/player.
			
			if (element.getType() == ValueType.CONTAINER)
				request.pushValue(Value.create(moduleContext.resolveContainer(element.asString()).getIdentity()));
			else if (element.getType() == ValueType.ROOM)
				request.pushValue(Value.create(moduleContext.resolveRoom(element.asString()).getIdentity()));
			else if (element.getType() == ValueType.PLAYER)
				request.pushValue(Value.create(moduleContext.resolvePlayer(element.asString()).getIdentity()));
			else if (element.getType() == ValueType.OBJECT)
				request.pushValue(Value.create(moduleContext.resolveObject(element.asString()).getIdentity()));
			else if (element.getType() == ValueType.WORLD)
				request.pushValue(Value.create(moduleContext.resolveWorld().getIdentity()));
			else
				throw new UnexpectedValueTypeException("Expected element type in IDENTITY call.");
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
	 * Increments the runaway command counter and calls the command.  
	 * @param request the request object.
	 * @param response the response object.
	 * @param blockLocal the local variables on the block call.
	 * @param command the command object.
	 * @throws TAMEInterrupt if an interrupt occurs. 
	 */
	public final void call(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command command) throws TAMEInterrupt
	{
		doCommand(request, response, blockLocal, command);
		response.incrementAndCheckCommandsExecuted();
	}

	/**
	 * Checks if an object is accessible to a player.
	 * @param request the request object.
	 * @param response the response object.
	 * @param player the player viewpoint.
	 * @param object the object to check.
	 * @return true if the object is considered "accessible," false if not.
	 */
	private static boolean checkObjectAccessibility(TAMERequest request, TAMEResponse response, TPlayer player, TObject object)
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
	 * @param request the request context.
	 * @param response the response object.
	 * @param functionType the function type.
	 */
	private static void doArithmeticStackFunction(TAMERequest request, TAMEResponse response, int functionType)
	{
		if (functionType < 0 || functionType >= ArithmeticOperator.VALUES.length)
			throw new UnexpectedValueException("Expected arithmetic function type, got illegal value %d.", functionType);
	
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
	private static void doPlayerSwitch(TAMERequest request, TAMEResponse response, TPlayer nextPlayer) throws TAMEInterrupt 
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		
		// set next player.
		response.trace(request, "Setting current player to %s.", nextPlayer);
		moduleContext.setCurrentPlayer(nextPlayer);
	}

	/**
	 * Attempts to perform a room stack pop for a player.
	 * @param request the request object.
	 * @param response the response object.
	 * @param player the player to pop a room context from.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static void doRoomPop(TAMERequest request, TAMEResponse response, TPlayer player) throws TAMEInterrupt 
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
	private static void doRoomPush(TAMERequest request, TAMEResponse response, TPlayer player, TRoom nextRoom) throws TAMEInterrupt
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
	private static void doRoomSwitch(TAMERequest request, TAMEResponse response, TPlayer player, TRoom nextRoom) throws TAMEInterrupt
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
	 * Attempts to perform a world browse.
	 * @param request the request object.
	 * @param response the response object.
	 * @param world the world to browse.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static void doWorldBrowse(TAMERequest request, TAMEResponse response, TWorld world) throws TAMEInterrupt 
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TOwnershipMap ownership = moduleContext.getOwnershipMap();
		BlockEntry blockEntry = BlockEntry.create(BlockEntryType.ONWORLDBROWSE);
		response.trace(request, "Start browse %s.", world);
		
		for (TObject object : ownership.getObjectsOwnedByWorld(world))
		{
			TObjectContext objectContext = moduleContext.getObjectContext(object);
	
			// find via inheritance.
			response.trace(request, "Check %s for browse block.", object);
			Block block = object.resolveBlock(blockEntry);
			if (block != null)
			{
				response.trace(request, "Calling world browse block.");
				TAMELogic.callBlock(request, response, objectContext, block);
			}
			
		}
	
	}

	/**
	 * Attempts to perform a player browse.
	 * @param request the request object.
	 * @param response the response object.
	 * @param player the player to browse.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static void doPlayerBrowse(TAMERequest request, TAMEResponse response, TPlayer player) throws TAMEInterrupt 
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TOwnershipMap ownership = moduleContext.getOwnershipMap();
		BlockEntry blockEntry = BlockEntry.create(BlockEntryType.ONPLAYERBROWSE);
		response.trace(request, "Start browse %s.", player);
		
		for (TObject object : ownership.getObjectsOwnedByPlayer(player))
		{
			TObjectContext objectContext = moduleContext.getObjectContext(object);
	
			// find via inheritance.
			response.trace(request, "Check %s for browse block.", object);
			Block block = object.resolveBlock(blockEntry);
			if (block != null)
			{
				response.trace(request, "Calling player browse block.");
				TAMELogic.callBlock(request, response, objectContext, block);
			}
			
		}
	
	}

	/**
	 * Attempts to perform a room browse.
	 * @param request the request object.
	 * @param response the response object.
	 * @param room the room to browse.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	private static void doRoomBrowse(TAMERequest request, TAMEResponse response, TRoom room) throws TAMEInterrupt 
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TOwnershipMap ownership = moduleContext.getOwnershipMap();
		BlockEntry blockEntry = BlockEntry.create(BlockEntryType.ONROOMBROWSE);
		
		response.trace(request, "Start browse %s.", room);
		
		for (TObject object : ownership.getObjectsOwnedByRoom(room))
		{
			TObjectContext objectContext = moduleContext.getObjectContext(object);
	
			// find via inheritance.
			response.trace(request, "Check %s for browse block.", object);
			Block block = object.resolveBlock(blockEntry);
			if (block != null)
			{
				response.trace(request, "Calling room browse block.");
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
	private static void doContainerBrowse(TAMERequest request, TAMEResponse response, TContainer container) throws TAMEInterrupt 
	{
		TAMEModuleContext moduleContext = request.getModuleContext();
		TOwnershipMap ownership = moduleContext.getOwnershipMap();
		BlockEntry blockEntry = BlockEntry.create(BlockEntryType.ONCONTAINERBROWSE);
	
		response.trace(request, "Start browse %s.", container);
		
		for (TObject object : ownership.getObjectsOwnedByContainer(container))
		{
			TObjectContext objectContext = moduleContext.getObjectContext(object);
	
			// find via inheritance.
			response.trace(request, "Check %s for browse block.", object);
			Block block = object.resolveBlock(blockEntry);
			if (block != null)
			{
				response.trace(request, "Calling container browse block.");
				TAMELogic.callBlock(request, response, objectContext, block);
			}
			
		}
	
	}
	
	/**
	 * Resolves a list of all objects contained by an object container.
	 * @param varObjectContainer the value to resolve via module context.
	 * @param moduleContext the module context.
	 * @return an iterable list of objects, or null if the value does not refer to an object container.
	 * @throws ErrorInterrupt if a major error occurs.
	 */
	private static Iterable<TObject> resolveObjectList(Value varObjectContainer, TAMEModuleContext moduleContext) throws ErrorInterrupt 
	{
		switch (varObjectContainer.getType())
		{
			default:
				return null;
			case ROOM:
				return moduleContext.getOwnershipMap().getObjectsOwnedByRoom(moduleContext.resolveRoom(varObjectContainer.asString()));
			case PLAYER:
				return moduleContext.getOwnershipMap().getObjectsOwnedByPlayer(moduleContext.resolvePlayer(varObjectContainer.asString()));
			case CONTAINER:
				return moduleContext.getOwnershipMap().getObjectsOwnedByContainer(moduleContext.resolveContainer(varObjectContainer.asString()));
			case WORLD:
				return moduleContext.getOwnershipMap().getObjectsOwnedByWorld(moduleContext.resolveWorld());
		}
		
	}
	
}
