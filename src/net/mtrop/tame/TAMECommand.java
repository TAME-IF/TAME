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

import net.mtrop.tame.element.TElement;
import net.mtrop.tame.element.type.TAction;
import net.mtrop.tame.element.type.TContainer;
import net.mtrop.tame.element.type.TObject;
import net.mtrop.tame.element.type.TPlayer;
import net.mtrop.tame.element.type.TRoom;
import net.mtrop.tame.element.type.TWorld;
import net.mtrop.tame.element.type.context.TElementContext;
import net.mtrop.tame.element.type.context.TObjectContext;
import net.mtrop.tame.exception.ModuleExecutionException;
import net.mtrop.tame.exception.UnexpectedValueTypeException;
import net.mtrop.tame.interrupt.BreakInterrupt;
import net.mtrop.tame.interrupt.EndInterrupt;
import net.mtrop.tame.interrupt.ContinueInterrupt;
import net.mtrop.tame.interrupt.ErrorInterrupt;
import net.mtrop.tame.interrupt.QuitInterrupt;
import net.mtrop.tame.interrupt.TAMEInterrupt;
import net.mtrop.tame.lang.ArgumentType;
import net.mtrop.tame.lang.Block;
import net.mtrop.tame.lang.Command;
import net.mtrop.tame.lang.CommandType;
import net.mtrop.tame.lang.Value;
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) 
		{
			// Do nothing.
		}
		
	},
	
	/**
	 * [INTERNAL] Calls a procedure local to the current context.
	 * Returns nothing.
	 */
	CALLPROCEDURE (true)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value procedureName = command.getOperand0();
			if (!procedureName.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in CALLPROCEDURE call.");
			
			if (request.peekContext() == null)
				throw new ModuleExecutionException("Attempted CALLPROCEDURE call without a context!");
			
			TElementContext<?> elementContext = request.peekContext();
			TElement element = elementContext.getElement();
			
			String name = procedureName.asString();

			Block block = element.getProcedureBlock(name);
			if (block == null)
				throw new ModuleExecutionException("Attempted CALLPROCEDURE call on a procedure that does not exist on %s: %s", element, name);
			
			TAMELogic.callProcedure(request, response, block);
		}
		
	},

	/**
	 * [INTERNAL] Pops a value off the stack.
	 * Returns nothing.
	 */
	POP (true)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command)
		{
			request.popValue();
		}
		
	},

	/**
	 * [INTERNAL] Pops a value into a variable in the topmost context in the execution.
	 * Operand0 is the variable. 
	 * POP is the value.
	 */
	POPVALUE (true)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) 
		{
			Value varvalue = command.getOperand0();
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in POPVALUE call.");
			if (!varvalue.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in POPVALUE call.");
			
			String variableName = varvalue.asString();
			request.peekContext().setValue(variableName, value);
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws ErrorInterrupt
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
					request.getModuleContext().resolveObjectContext(objectName).setValue(variableName, value);
					break;
				case ROOM:
					request.getModuleContext().resolveRoomContext(objectName).setValue(variableName, value);
					break;
				case PLAYER:
					request.getModuleContext().resolvePlayerContext(objectName).setValue(variableName, value);
					break;
				case CONTAINER:
					request.getModuleContext().resolveContainerContext(objectName).setValue(variableName, value);
					break;
				case WORLD:
					request.getModuleContext().resolveWorldContext().setValue(variableName, value);
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) 
		{
			Value value = command.getOperand0();
			
			if (value.isVariable())
			{
				String variableName = value.asString();
				request.pushValue(request.resolveVariableValue(variableName));
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws ErrorInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command)
		{
			Value functionValue = command.getOperand0();
			
			if (!functionValue.isInteger())
				throw new UnexpectedValueTypeException("Expected integer type in ARITHMETICFUNC call.");

			TAMELogic.doArithmeticStackFunction(request, (int)functionValue.asLong());
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			// block should contain arithmetic commands and a last push.
			Block conditional = command.getConditionalBlock();
			if (conditional == null)
				throw new ModuleExecutionException("Conditional block for IF does NOT EXIST!");
			
			response.trace(request, "Calling IF conditional...");
			conditional.call(request, response);
			
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
				success.call(request, response);
			}
			else
			{
				response.trace(request, "Result %s evaluates false.", value);
				Block failure = command.getFailureBlock();
				if (failure != null)
				{
					response.trace(request, "Calling IF failure block...");
					failure.call(request, response);
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			while (callConditional(request, response, command))
			{
				try {
					response.trace(request, "Calling WHILE success block...");
					Block success = command.getSuccessBlock();
					if (success == null)
						throw new ModuleExecutionException("Success block for WHILE does NOT EXIST!");
					success.call(request, response);
				} catch (BreakInterrupt interrupt) {
					break;
				} catch (ContinueInterrupt interrupt) {
					continue;
				}
			}
		}
		
		private boolean callConditional(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			response.trace(request, "Calling WHILE conditional...");

			// block should contain arithmetic commands and a last push.
			Block conditional = command.getConditionalBlock();
			if (conditional == null)
				throw new ModuleExecutionException("Conditional block for WHILE does NOT EXIST!");
			conditional.call(request, response);
			
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
				init.call(request, response); 
				callConditional(request, response, command); 
				response.trace(request, "Calling FOR stepping block..."), 
				step.call(request, response)
			)
			{
				try {
					response.trace(request, "Calling FOR success block...");
					success.call(request, response);
				} catch (BreakInterrupt interrupt) {
					break;
				} catch (ContinueInterrupt interrupt) {
					continue;
				}
			}
		}

		private boolean callConditional(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			response.trace(request, "Calling FOR conditional...");
			
			// block should contain arithmetic commands and a last push.
			Block conditional = command.getConditionalBlock();
			if (conditional == null)
				throw new ModuleExecutionException("Conditional block for WHILE does NOT EXIST!");
			conditional.call(request, response);
			
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
	 * Throws a BREAK interrupt.
	 * Is keyword. Returns nothing. 
	 */
	BREAK ()
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in TEXTLN call.");

			response.addCue(CUE_TEXT, value.asString() + '\n');
		}
		
	},
	
	/**
	 * Adds a TEXTFORMATTED cue to the response.
	 * POP is the value to print. 
	 * Returns nothing. 
	 */
	TEXTF (/*Return: */ null, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in TEXTF call.");

			response.addCue(CUE_TEXTFORMATTED, value.asString());
		}
		
	},
	
	/**
	 * Adds a TEXTFORMATTED cue to the response with a newline appended to it.
	 * POP is the value to print. 
	 * Returns nothing. 
	 */
	TEXTFLN (/*Return: */ null, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in TEXTFLN call.");

			response.addCue(CUE_TEXTFORMATTED, value.asString() + '\n');
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in INFO call.");

			response.addCue(CUE_INFO, value.asString());
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	 * Casts a value to boolean.
	 * POP is the value to convert. 
	 * Returns value as boolean. 
	 */
	ASBOOLEAN (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	 * Returns if a string token exists in the given string.
	 * A token is a whitespace-broken piece.
	 * First POP is what to search for. 
	 * Second POP is the string. 
	 * Returns boolean.
	 */
	STRCONTAINSTOKEN (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRCONTAINSTOKEN call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRCONTAINSTOKEN call.");

			String pattern = value2.asString();
			String str = value1.asString();
			
			Pattern p = null;
			try {
				p = Pattern.compile(pattern);
			} catch (PatternSyntaxException e) {
				throw new ErrorInterrupt("Expected valid expression in STRCONTAINSTOKEN call.");
			}
			
			request.pushValue(Value.create(p.matcher(str).find()));
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	 * Adds an object name to the object.
	 * First POP is name.
	 * Second POP is object. 
	 * Returns nothing.
	 */
	ADDOBJECTNAME (/*Return: */ null, /*Args: */ ArgumentType.OBJECT, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varName = request.popValue();
			Value varObject = request.popValue();
			
			if (!varName.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in ADDOBJECTNAME call.");
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in ADDOBJECTNAME call.");

			TObjectContext objectContext = request.getModuleContext().resolveObjectContext(varObject.asString());
			objectContext.addName(varName.asString());
		}
		
	},
	
	/**
	 * Adds an object to the world.
	 * POP is object.
	 * Returns nothing.
	 */
	GIVEWORLDOBJECT (/*Return: */ null, /*Args: */ ArgumentType.OBJECT)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varObject = request.popValue();
			
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in GIVEWORLDOBJECT call.");
			
			TAMEModuleContext moduleContext = request.getModuleContext();
			TWorld world = moduleContext.resolveWorld();
			TObject object = moduleContext.resolveObject(varObject.asString());
			request.getModuleContext().getOwnershipMap().addObjectToWorld(object, world);
		}
		
	},
	
	/**
	 * Adds an object to a player.
	 * First POP is object.
	 * Second POP is player. 
	 * Returns nothing.
	 */
	GIVEPLAYEROBJECT (/*Return: */ null, /*Args: */ ArgumentType.PLAYER, ArgumentType.OBJECT)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varObject = request.popValue();
			Value varPlayer = request.popValue();
			
			if (varPlayer.getType() != ValueType.PLAYER)
				throw new UnexpectedValueTypeException("Expected player type in GIVEPLAYEROBJECT call.");
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in GIVEPLAYEROBJECT call.");
			
			TAMEModuleContext moduleContext = request.getModuleContext();
			TPlayer player = moduleContext.resolvePlayer(varPlayer.asString());
			TObject object = moduleContext.resolveObject(varObject.asString());
			request.getModuleContext().getOwnershipMap().addObjectToPlayer(object, player);
		}
		
	},
	
	/**
	 * Adds an object to a room.
	 * First POP is object.
	 * Second POP is room. 
	 * Returns nothing.
	 */
	GIVEROOMOBJECT (/*Return: */ null, /*Args: */ ArgumentType.ROOM, ArgumentType.OBJECT)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varObject = request.popValue();
			Value varRoom = request.popValue();
			
			if (varRoom.getType() != ValueType.ROOM)
				throw new UnexpectedValueTypeException("Expected room type in GIVEROOMOBJECT call.");
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in GIVEROOMOBJECT call.");
			
			TAMEModuleContext moduleContext = request.getModuleContext();
			TRoom room = moduleContext.resolveRoom(varRoom.asString());
			TObject object = moduleContext.resolveObject(varObject.asString());
			request.getModuleContext().getOwnershipMap().addObjectToRoom(object, room);
		}
		
	},
	
	/**
	 * Adds an object to a container.
	 * First POP is object.
	 * Second POP is room. 
	 * Returns nothing.
	 */
	GIVECONTAINEROBJECT (/*Return: */ null, /*Args: */ ArgumentType.CONTAINER, ArgumentType.OBJECT)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varObject = request.popValue();
			Value varContainer = request.popValue();
			
			if (varContainer.getType() != ValueType.CONTAINER)
				throw new UnexpectedValueTypeException("Expected container type in GIVECONTAINEROBJECT call.");
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in GIVECONTAINEROBJECT call.");
			
			TAMEModuleContext moduleContext = request.getModuleContext();
			TContainer container = moduleContext.resolveContainer(varContainer.asString());
			TObject object = moduleContext.resolveObject(varObject.asString());
			request.getModuleContext().getOwnershipMap().addObjectToContainer(object, container);
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	 * Checks if an object has no owner. True if so.
	 * POP is object.
	 * Returns boolean.
	 */
	OBJECTHASNOOWNER (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.OBJECT)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	 * Counts the objects in the world.
	 * POPs nothing.
	 * Returns integer.
	 */
	OBJECTSINWORLDCOUNT (/*Return: */ ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			request.pushValue(Value.create(request.getModuleContext().getOwnershipMap().getObjectsOwnedByWorldCount(request.getModuleContext().resolveWorld())));
		}
		
	},
	
	/**
	 * Counts the objects in a player.
	 * POP is the player.
	 * Returns integer.
	 */
	OBJECTSINPLAYERCOUNT (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.PLAYER)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varPlayer = request.popValue();
			
			if (varPlayer.getType() != ValueType.PLAYER)
				throw new UnexpectedValueTypeException("Expected player type in OBJECTSINPLAYERCOUNT call.");
			
			TPlayer player = request.getModuleContext().resolvePlayer(varPlayer.asString());
			request.pushValue(Value.create(request.getModuleContext().getOwnershipMap().getObjectsOwnedByPlayerCount(player)));
		}
		
	},
	
	/**
	 * Counts the objects in a room.
	 * POP is the room.
	 * Returns integer.
	 */
	OBJECTSINROOMCOUNT (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.ROOM)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varRoom = request.popValue();
			
			if (varRoom.getType() != ValueType.ROOM)
				throw new UnexpectedValueTypeException("Expected room type in OBJECTSINROOMCOUNT call.");
			
			TRoom room = request.getModuleContext().resolveRoom(varRoom.asString());
			request.pushValue(Value.create(request.getModuleContext().getOwnershipMap().getObjectsOwnedByRoomCount(room)));
		}
		
	},
	
	/**
	 * Counts the objects in a container.
	 * POP is the container.
	 * Returns integer.
	 */
	OBJECTSINCONTAINERCOUNT (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.CONTAINER)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varRoom = request.popValue();
			
			if (varRoom.getType() != ValueType.CONTAINER)
				throw new UnexpectedValueTypeException("Expected container type in OBJECTSINCONTAINERCOUNT call.");
			
			TContainer container = request.getModuleContext().resolveContainer(varRoom.asString());
			request.pushValue(Value.create(request.getModuleContext().getOwnershipMap().getObjectsOwnedByContainerCount(container)));
		}
		
	},
	
	/**
	 * Checks if an object is owned by the world.
	 * POP is the object.
	 * Returns boolean.
	 */
	WORLDHASOBJECT (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.OBJECT)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varObject = request.popValue();
			
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in WORLDHASOBJECT call.");
			
			TAMEModuleContext moduleContext = request.getModuleContext();
			TObject object = moduleContext.resolveObject(varObject.asString());
			request.pushValue(Value.create(request.getModuleContext().getOwnershipMap().checkWorldHasObject(moduleContext.resolveWorld(), object)));
		}
		
	},
	
	/**
	 * Checks if an object is owned by a player.
	 * First POP is the object.
	 * Second POP is the player.
	 * Returns boolean.
	 */
	PLAYERHASOBJECT (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.PLAYER, ArgumentType.OBJECT)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varObject = request.popValue();
			Value varPlayer = request.popValue();
			
			if (varPlayer.getType() != ValueType.PLAYER)
				throw new UnexpectedValueTypeException("Expected player type in PLAYERHASOBJECT call.");
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in PLAYERHASOBJECT call.");
			
			TAMEModuleContext moduleContext = request.getModuleContext();
			TPlayer player = moduleContext.resolvePlayer(varPlayer.asString());
			TObject object = moduleContext.resolveObject(varObject.asString());
			request.pushValue(Value.create(request.getModuleContext().getOwnershipMap().checkPlayerHasObject(player, object)));
		}
		
	},
	
	/**
	 * Checks if an object is owned by a room.
	 * POP is the player.
	 * Returns boolean.
	 */
	ROOMHASOBJECT (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.ROOM, ArgumentType.OBJECT)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varObject = request.popValue();
			Value varRoom = request.popValue();
			
			if (varRoom.getType() != ValueType.ROOM)
				throw new UnexpectedValueTypeException("Expected room type in ROOMHASOBJECT call.");
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in ROOMHASOBJECT call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TRoom room = moduleContext.resolveRoom(varRoom.asString());
			TObject object = moduleContext.resolveObject(varObject.asString());
			request.pushValue(Value.create(request.getModuleContext().getOwnershipMap().checkRoomHasObject(room, object)));
		}
		
	},
	
	/**
	 * Checks if an object is owned by a container.
	 * First POP is the object.
	 * Second POP is the player.
	 * Returns boolean.
	 */
	CONTAINERHASOBJECT (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.CONTAINER, ArgumentType.OBJECT)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varObject = request.popValue();
			Value varContainer = request.popValue();
			
			if (varContainer.getType() != ValueType.CONTAINER)
				throw new UnexpectedValueTypeException("Expected container type in CONTAINERHASOBJECT call.");
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in CONTAINERHASOBJECT call.");
			
			TAMEModuleContext moduleContext = request.getModuleContext();
			TContainer container = moduleContext.resolveContainer(varContainer.asString());
			TObject object = moduleContext.resolveObject(varObject.asString());
			request.pushValue(Value.create(request.getModuleContext().getOwnershipMap().checkContainerHasObject(container, object)));
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		
	},
	
	/**
	 * Calls the onPlayerBrowse() blocks on objects in a player.
	 * POP is player. 
	 * Returns nothing.
	 */
	BROWSEPLAYER (/*Return: */ null, /*Args: */ ArgumentType.PLAYER)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value playerValue = request.popValue();
			
			if (playerValue.getType() != ValueType.PLAYER)
				throw new UnexpectedValueTypeException("Expected player type in BROWSEPLAYER call.");

			TAMELogic.doPlayerBrowse(request, response, request.getModuleContext().resolvePlayer(playerValue.asString()));
		}
		
	},
	
	/**
	 * Calls the onRoomBrowse() blocks on objects in a room.
	 * POP is room. 
	 * Returns nothing.
	 */
	BROWSEROOM (/*Return: */ null, /*Args: */ ArgumentType.ROOM)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value roomValue = request.popValue();
			
			if (roomValue.getType() != ValueType.ROOM)
				throw new UnexpectedValueTypeException("Expected room type in BROWSEROOM call.");

			TAMELogic.doRoomBrowse(request, response, request.getModuleContext().resolveRoom(roomValue.asString()));
		}
		
	},
	
	/**
	 * Calls the onContainerBrowse() blocks on objects in a container.
	 * POP is container. 
	 * Returns nothing.
	 */
	BROWSECONTAINER (/*Return: */ null, /*Args: */ ArgumentType.CONTAINER)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value containerValue = request.popValue();
			
			if (containerValue.getType() != ValueType.CONTAINER)
				throw new UnexpectedValueTypeException("Expected container type in BROWSECONTAINER call.");

			TAMELogic.doContainerBrowse(request, response, request.getModuleContext().resolveContainer(containerValue.asString()));
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varPlayer = request.popValue();
			
			if (varPlayer.getType() != ValueType.PLAYER)
				throw new UnexpectedValueTypeException("Expected player type in SETPLAYER call.");

			TPlayer nextPlayer = request.getModuleContext().resolvePlayer(varPlayer.asString());
			TAMELogic.doPlayerSwitch(request, response, nextPlayer);
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varRoom = request.popValue();
			
			if (varRoom.getType() != ValueType.ROOM)
				throw new UnexpectedValueTypeException("Expected room type in SETROOM call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TRoom nextRoom = moduleContext.resolveRoom(varRoom.asString());
			TPlayer player = moduleContext.getCurrentPlayer();

			if (player == null)
				throw new ErrorInterrupt("No current player!");

			TAMELogic.doRoomSwitch(request, response, player, nextRoom);
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
			TAMELogic.doRoomPush(request, response, player, nextRoom);
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			TAMEModuleContext moduleContext = request.getModuleContext();
			TPlayer player = moduleContext.getCurrentPlayer();
			
			if (player == null)
				throw new ErrorInterrupt("No current player!");

			TRoom currentRoom = moduleContext.getOwnershipMap().getCurrentRoom(player);
			
			if (currentRoom == null)
				throw new ErrorInterrupt("No rooms for current player!");
			
			TAMELogic.doRoomPop(request, response, player);
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
			
			TAMELogic.doRoomPop(request, response, player);
			TAMELogic.doRoomPush(request, response, player, nextRoom);
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			TAMEModuleContext moduleContext = request.getModuleContext();
			TRoom room = moduleContext.getCurrentRoom();
			request.pushValue(Value.create(room == null));
		}

	},
	
	/**
	 * Enqueues an general action to perform after the current one.
	 * POP is the action.
	 * Returns nothing.
	 */
	QUEUEACTION (/*Return: */ null, /*Args: */ ArgumentType.ACTION)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varAction = request.popValue();
			
			if (varAction.getType() != ValueType.ACTION)
				throw new UnexpectedValueTypeException("Expected action type in QUEUEACTION call.");

			TAction action = request.getModuleContext().resolveAction(varAction.asString());
			request.addActionItem(TAMEAction.create(action));
		}

	},

	/**
	 * Enqueues a open/modal action to perform after the current one.
	 * First POP is the modal or open target.
	 * Second POP is the action.
	 * Returns nothing.
	 */
	QUEUEACTIONSTRING (/*Return: */ null, /*Args: */ ArgumentType.ACTION, ArgumentType.VALUE)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varTarget = request.popValue();
			Value varAction = request.popValue();
			
			if (!varTarget.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in QUEUEACTIONSTRING call.");
			if (varAction.getType() != ValueType.ACTION)
				throw new UnexpectedValueTypeException("Expected action type in QUEUEACTIONSTRING call.");

			TAction action = request.getModuleContext().resolveAction(varAction.asString());
			String target = varAction.asString();
			
			request.addActionItem(TAMEAction.create(action, target));
		}

	},
	
	/**
	 * Enqueues a transitive action to perform after the current one.
	 * First POP is the object.
	 * Second POP is the action.
	 * Returns nothing.
	 */
	QUEUEACTIONOBJECT (/*Return: */ null, /*Args: */ ArgumentType.ACTION, ArgumentType.OBJECT)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
			
			request.addActionItem(TAMEAction.create(action, object));
		}

	},
	
	/**
	 * Enqueues a ditransitive action to perform after the current one.
	 * First POP is the second object.
	 * Second POP is the object.
	 * Third POP is the action.
	 * Returns nothing.
	 */
	QUEUEACTIONOBJECT2 (/*Return: */ null, /*Args: */ ArgumentType.ACTION, ArgumentType.OBJECT, ArgumentType.OBJECT)
	{
		@Override
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
		protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value element = request.popValue();

			TAMEModuleContext moduleContext = request.getModuleContext();
			
			// must resolve: the passed-in value could be the "current" room/player.
			
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

	private TAMECommand(boolean internal, boolean block, ArgumentType returnType, ArgumentType[] argumentTypes)
	{
		this.language = block;
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
	 * @param command the command origin.
	 * @throws TAMEInterrupt if an interrupt occurs. 
	 */
	protected void doCommand(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
	{
		throw new RuntimeException("UNIMPLEMENTED COMMAND");
	}
	
	/**
	 * Increments the runaway command counter and calls the command.  
	 * @param request the request object.
	 * @param response the response object.
	 * @param command the command object.
	 * @throws TAMEInterrupt if an interrupt occurs. 
	 */
	public final void call(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
	{
		doCommand(request, response, command);
		response.incrementAndCheckCommandsExecuted();
	}
	
}
