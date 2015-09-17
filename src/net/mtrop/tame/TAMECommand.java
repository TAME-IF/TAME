package net.mtrop.tame;

import net.mtrop.tame.exception.ModuleExecutionException;
import net.mtrop.tame.exception.UnexpectedValueException;
import net.mtrop.tame.exception.UnexpectedValueTypeException;
import net.mtrop.tame.interrupt.BreakInterrupt;
import net.mtrop.tame.interrupt.CancelInterrupt;
import net.mtrop.tame.interrupt.ContinueInterrupt;
import net.mtrop.tame.interrupt.ErrorInterrupt;
import net.mtrop.tame.interrupt.QuitInterrupt;
import net.mtrop.tame.interrupt.TAMEInterrupt;
import net.mtrop.tame.lang.ArgumentType;
import net.mtrop.tame.lang.CommandType;
import net.mtrop.tame.lang.command.Block;
import net.mtrop.tame.lang.command.Command;
import net.mtrop.tame.struct.Value;
import net.mtrop.tame.struct.ValueType;

/**
 * The set of commands.
 * Values in arguments are popped in reverse order on call, if arguments are taken. 
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
		public void execute(TAMERequest request, TAMEResponse response, Command command) 
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
		public void execute(TAMERequest request, TAMEResponse response, Command command)
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
		public void execute(TAMERequest request, TAMEResponse response, Command command) 
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
	POPOBJECTVALUE (true)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws ErrorInterrupt
		{
			Value varObject = command.getOperand0();
			Value variable = command.getOperand1();
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in POPOBJECTVALUE call.");
			if (!variable.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in POPOBJECTVALUE call.");
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in POPOBJECTVALUE call.");
			
			String variableName = variable.asString();
			String objectName = varObject.asString();

			request.resolveObject(objectName).setValue(variableName, value);
		}
		
	},

	/**
	 * [INTERNAL] Sets a variable on a room.
	 * Operand0 is the room. 
	 * Operand1 is the variable. 
	 * POP is the value.
	 */
	POPROOMVALUE (true)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws ErrorInterrupt
		{
			Value varRoom = command.getOperand0();
			Value varValue = command.getOperand1();
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in POPROOMVALUE call.");
			if (!varValue.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in POPROOMVALUE call.");
			if (varRoom.getType() != ValueType.ROOM)
				throw new UnexpectedValueTypeException("Expected room type in POPROOMVALUE call.");
			
			String variableName = varValue.asString();
			String roomName = varRoom.asString();

			request.resolveRoom(roomName).setValue(variableName, value);
		}
		
	},

	/**
	 * [INTERNAL] Sets a variable on a player.
	 * Operand0 is the player. 
	 * Operand1 is the variable. 
	 * POP is the value.
	 */
	POPPLAYERVALUE (true)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws ErrorInterrupt
		{
			Value varPlayer = command.getOperand0();
			Value varValue = command.getOperand1();
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in POPPLAYERVALUE call.");
			if (!varValue.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in POPPLAYERVALUE call.");
			if (varPlayer.getType() != ValueType.PLAYER)
				throw new UnexpectedValueTypeException("Expected player type in POPPLAYERVALUE call.");
			
			String variableName = varValue.asString();
			String playerName = varPlayer.asString();

			request.resolveRoom(playerName).setValue(variableName, value);
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
		public void execute(TAMERequest request, TAMEResponse response, Command command) 
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
	PUSHOBJECTVALUE (true)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws ErrorInterrupt
		{
			Value varObject = command.getOperand0();
			Value variable = command.getOperand1();

			if (!variable.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in PUSHOBJECTVALUE call.");
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in PUSHOBJECTVALUE call.");

			String variableName = variable.asString();
			String objectName = varObject.asString();

			// return
			request.pushValue(request.resolveObjectVariableValue(objectName, variableName));
		}
		
	},
	
	/**
	 * [INTERNAL] Resolves value of a variable on a room and pushes the value.
	 * Operand0 is the room. 
	 * Operand1 is the variable. 
	 * Pushes the resolved value. 
	 */
	PUSHROOMVARIABLE (true)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws ErrorInterrupt
		{
			Value varRoom = command.getOperand0();
			Value variable = command.getOperand1();

			if (!variable.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in PUSHROOMVARIABLE call.");
			if (varRoom.getType() != ValueType.ROOM)
				throw new UnexpectedValueTypeException("Expected room type in PUSHROOMVARIABLE call.");

			String variableName = variable.asString();
			String roomName = varRoom.asString();

			// return
			request.pushValue(request.resolveRoomVariableValue(roomName, variableName));
		}
		
	},
	
	/**
	 * [INTERNAL] Resolves value of a variable on a player and pushes the value.
	 * Operand0 is the player. 
	 * Operand1 is the variable. 
	 * Pushes the resolved value. 
	 */
	PUSHPLAYERVARIABLE (true)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws ErrorInterrupt
		{
			Value varPlayer = command.getOperand0();
			Value variable = command.getOperand1();

			if (!variable.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in PUSHPLAYERVARIABLE call.");
			if (varPlayer.getType() != ValueType.PLAYER)
				throw new UnexpectedValueTypeException("Expected player type in PUSHPLAYERVARIABLE call.");

			String variableName = variable.asString();
			String playerName = varPlayer.asString();

			// return
			request.pushValue(request.resolvePlayerVariableValue(playerName, variableName));
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
		public void execute(TAMERequest request, TAMEResponse response, Command command)
		{
			Value functionValue = command.getOperand0();
			
			if (!functionValue.isInteger())
				throw new UnexpectedValueTypeException("Expected integer type in ARITHMETICFUNC call.");

			int functionType = (int)functionValue.asLong();
			
			switch (functionType)
			{
				default:
					throw new UnexpectedValueException("Expected arithmetic function type, got illegal value %d.", functionType);
					
				case ARITHMETIC_FUNCTION_ABSOLUTE:
				case ARITHMETIC_FUNCTION_NEGATE:
				case ARITHMETIC_FUNCTION_LOGICAL_NOT:
				case ARITHMETIC_FUNCTION_NOT:
				{
					Value value = request.popValue();

					if (!value.isLiteral())
						throw new UnexpectedValueTypeException("Expected literal type in ARITHMETICFUNC call.");

					request.pushValue(unaryFunction(functionType, value));
				}
				break;
				
				case ARITHMETIC_FUNCTION_ADD:
				case ARITHMETIC_FUNCTION_SUBTRACT:
				case ARITHMETIC_FUNCTION_MULTIPLY:
				case ARITHMETIC_FUNCTION_DIVIDE:
				case ARITHMETIC_FUNCTION_MODULO:
				case ARITHMETIC_FUNCTION_AND:
				case ARITHMETIC_FUNCTION_OR:
				case ARITHMETIC_FUNCTION_XOR:
				case ARITHMETIC_FUNCTION_LSHIFT:
				case ARITHMETIC_FUNCTION_RSHIFT:
				case ARITHMETIC_FUNCTION_RSHIFTPAD:
				case ARITHMETIC_FUNCTION_LOGICAL_AND:
				case ARITHMETIC_FUNCTION_LOGICAL_OR:
				case ARITHMETIC_FUNCTION_LOGICAL_XOR:
				case ARITHMETIC_FUNCTION_EQUALS:
				case ARITHMETIC_FUNCTION_NOT_EQUALS:
				case ARITHMETIC_FUNCTION_STRICT_EQUALS:
				case ARITHMETIC_FUNCTION_STRICT_NOT_EQUALS:
				case ARITHMETIC_FUNCTION_LESS:
				case ARITHMETIC_FUNCTION_LESS_OR_EQUAL:
				case ARITHMETIC_FUNCTION_GREATER:
				case ARITHMETIC_FUNCTION_GREATER_OR_EQUAL:
				{
					Value value2 = request.popValue();
					Value value1 = request.popValue();

					if (!value1.isLiteral())
						throw new UnexpectedValueTypeException("Expected literal type in ARITHMETICFUNC call.");
					if (!value2.isLiteral())
						throw new UnexpectedValueTypeException("Expected literal type in ARITHMETICFUNC call.");

					request.pushValue(binaryFunction(functionType, value1, value2));
				}
				break;
				
			}
			
		}
		
		// Unary function.
		private Value unaryFunction(int functionType, Value value1)
		{
			switch (functionType)
			{
				default:
					throw new UnexpectedValueException("INTERNAL ERROR: EXPECTED UNARY OP. GOT %d.", functionType);
					
				case ARITHMETIC_FUNCTION_ABSOLUTE:
					return Value.absolute(value1);
				case ARITHMETIC_FUNCTION_NEGATE:
					return Value.negate(value1);
				case ARITHMETIC_FUNCTION_LOGICAL_NOT:
					return Value.logicalNot(value1);
				case ARITHMETIC_FUNCTION_NOT:
					return Value.not(value1);
			}
		}
		
		// Binary function.
		private Value binaryFunction(int functionType, Value value1, Value value2)
		{
			switch (functionType)
			{
				default:
					throw new UnexpectedValueException("INTERNAL ERROR: EXPECTED BINARY OP. GOT %d.", functionType);
					
				case ARITHMETIC_FUNCTION_ADD:
					return Value.add(value1, value2);
				case ARITHMETIC_FUNCTION_SUBTRACT:
					return Value.subtract(value1, value2);
				case ARITHMETIC_FUNCTION_MULTIPLY:
					return Value.multiply(value1, value2);
				case ARITHMETIC_FUNCTION_DIVIDE:
					return Value.divide(value1, value2);
				case ARITHMETIC_FUNCTION_MODULO:
					return Value.modulo(value1, value2);
				case ARITHMETIC_FUNCTION_AND:
					return Value.and(value1, value2);
				case ARITHMETIC_FUNCTION_OR:
					return Value.or(value1, value2);
				case ARITHMETIC_FUNCTION_XOR:
					return Value.xor(value1, value2);
				case ARITHMETIC_FUNCTION_LSHIFT:
					return Value.leftShift(value1, value2);
				case ARITHMETIC_FUNCTION_RSHIFT:
					return Value.rightShift(value1, value2);
				case ARITHMETIC_FUNCTION_RSHIFTPAD:
					return Value.rightShiftPadded(value1, value2);
				case ARITHMETIC_FUNCTION_LOGICAL_AND:
					return Value.logicalAnd(value1, value2);
				case ARITHMETIC_FUNCTION_LOGICAL_OR:
					return Value.logicalOr(value1, value2);
				case ARITHMETIC_FUNCTION_LOGICAL_XOR:
					return Value.logicalXOr(value1, value2);
				case ARITHMETIC_FUNCTION_EQUALS:
					return Value.equals(value1, value2);
				case ARITHMETIC_FUNCTION_NOT_EQUALS:
					return Value.notEquals(value1, value2);
				case ARITHMETIC_FUNCTION_STRICT_EQUALS:
					return Value.strictEquals(value1, value2);
				case ARITHMETIC_FUNCTION_STRICT_NOT_EQUALS:
					return Value.strictNotEquals(value1, value2);
				case ARITHMETIC_FUNCTION_LESS:
					return Value.less(value1, value2);
				case ARITHMETIC_FUNCTION_LESS_OR_EQUAL:
					return Value.lessOrEqual(value1, value2);
				case ARITHMETIC_FUNCTION_GREATER:
					return Value.greater(value1, value2);
				case ARITHMETIC_FUNCTION_GREATER_OR_EQUAL:
					return Value.greaterOrEqual(value1, value2);
			}
		}
		
	},
	
	/**
	 * Adds a QUIT cue to the response and throws a quit interrupt.
	 * Is keyword. Returns nothing. 
	 */
	QUIT ()
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			response.addCue(CUE_QUIT);
			response.trace(request, "Throwing quit...");
			throw new QuitInterrupt();
		}
		
	},
	
	/**
	 * Adds a throws a break interrupt.
	 * Is keyword. Returns nothing. 
	 */
	BREAK ()
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			response.trace(request, "Throwing break...");
			throw new BreakInterrupt();
		}
		
	},
	
	/**
	 * Adds a throws a cancel interrupt.
	 * Is keyword. Returns nothing. 
	 */
	CANCEL ()
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			response.trace(request, "Throwing cancel...");
			throw new CancelInterrupt();
		}
		
	},
	
	/**
	 * Adds a throws a continue interrupt.
	 * Is keyword. Returns nothing. 
	 */
	CONTINUE ()
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			response.trace(request, "Throwing continue...");
			throw new ContinueInterrupt();
		}
		
	},
	
	/**
	 * Adds a TEXT cue to the response.
	 * POP is the value to print. 
	 * Returns nothing. 
	 */
	PRINT (false, /*Return: */ null, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in PRINT call.");

			response.addCue(CUE_TEXT, value.asString());
		}
		
	},
	
	/**
	 * Adds a TEXT cue to the response with a newline appended to it.
	 * POP is the value to print. 
	 * Returns nothing. 
	 */
	PRINTLN (false, /*Return: */ null, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in PRINTLN call.");

			response.addCue(CUE_TEXT, value.asString() + '\n');
		}
		
	},
	
	/**
	 * Adds a PAUSE cue to the response.
	 * POP is the value to print. 
	 * Returns nothing. 
	 */
	PAUSE (false, /*Return: */ null)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			response.addCue(CUE_PAUSE);
		}
		
	},
	
	/**
	 * Adds a WAIT cue to the response.
	 * POP is the amount to wait for (read as integer). 
	 * Returns nothing. 
	 */
	WAIT (false, /*Return: */ null, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	TIP (false, /*Return: */ null, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	INFO (false, /*Return: */ null, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in INFO call.");

			response.addCue(CUE_INFO, value.asString());
		}
		
	},
	
	/**
	 * Adds a SAVE cue to the response.
	 * This does no state-saving. The cue is supposed to be read on response and the client needs to save. 
	 * POP is the save name. 
	 * Returns nothing. 
	 */
	SAVE (false, /*Return: */ null, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in SAVE call.");

			response.addCue(CUE_SAVE, value.asString());
		}
		
	},
	
	/**
	 * Adds a LOAD cue to the response.
	 * This does no state-loading. The cue is supposed to be read on response and the client needs to load. 
	 * POP is the load name. 
	 * Returns nothing. 
	 */
	LOAD (false, /*Return: */ null, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in LOAD call.");

			response.addCue(CUE_LOAD, value.asString());
		}
		
	},
	
	/**
	 * Adds a cue to the response.
	 * First POP is the value to print. 
	 * Second POP is the cue name. 
	 * Returns nothing. 
	 */
	ADDCUE (false, /*Return: */ null, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	 * If block.
	 * Has a conditional block that is called and then the success 
	 * block if POP is true, or if false and the fail block exists, call the fail block. 
	 * Returns nothing. 
	 */
	IF (false, true, false, true, true)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			// block should contain arithmetic commands and a last push.
			Block conditional = command.getConditionalBlock();
			if (conditional == null)
				throw new ModuleExecutionException("Conditional block for IF does NOT EXIST!");
			conditional.execute(request, response);
			
			// get remaining expression value.
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type after IF conditional block execution.");

			if (value.asBoolean())
			{
				Block success = command.getSuccessBlock();
				if (success == null)
					throw new ModuleExecutionException("Success block for IF does NOT EXIST!");
				success.execute(request, response);
			}
			else
			{
				Block failure = command.getFailureBlock();
				if (failure != null)
					failure.execute(request, response);
			}
			
		}
		
	},
	
	/**
	 * WHILE block.
	 * Has a conditional block that is called and then the success block if POP is true. 
	 * Returns nothing. 
	 */
	WHILE (false, true, false, true, false)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			while (callConditional(request, response, command))
			{
				try {
					Block success = command.getSuccessBlock();
					if (success == null)
						throw new ModuleExecutionException("Success block for WHILE does NOT EXIST!");
					success.execute(request, response);
				} catch (BreakInterrupt interrupt) {
					break;
				} catch (ContinueInterrupt interrupt) {
					continue;
				}
			}
		}
		
		private boolean callConditional(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			// block should contain arithmetic commands and a last push.
			Block conditional = command.getConditionalBlock();
			if (conditional == null)
				throw new ModuleExecutionException("Conditional block for WHILE does NOT EXIST!");
			conditional.execute(request, response);
			
			// get remaining expression value.
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type after WHILE conditional block execution.");

			return value.asBoolean(); 
		}
		
	},
	
	/**
	 * FOR block.
	 * Has an init, conditional, and step block that is called and then the success block if POP is true after conditional. 
	 * Returns nothing. 
	 */
	FOR (true, true, true, true, false)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			for (callInit(request, response, command); callConditional(request, response, command); callStep(request, response, command))
			{
				try {
					Block success = command.getSuccessBlock();
					if (success == null)
						throw new ModuleExecutionException("Success block for FOR does NOT EXIST!");
					success.execute(request, response);
				} catch (BreakInterrupt interrupt) {
					break;
				} catch (ContinueInterrupt interrupt) {
					continue;
				}
			}
		}
		
		private void callInit(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Block init = command.getInitializationBlock();
			if (init == null)
				throw new ModuleExecutionException("Init block for FOR does NOT EXIST!");
			init.execute(request, response);
		}
		
		private void callStep(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Block step = command.getStepBlock();
			if (step == null)
				throw new ModuleExecutionException("Step block for FOR does NOT EXIST!");
			step.execute(request, response);
		}
		
		private boolean callConditional(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			// block should contain arithmetic commands and a last push.
			Block conditional = command.getConditionalBlock();
			if (conditional == null)
				throw new ModuleExecutionException("Conditional block for FOR does NOT EXIST!");
			conditional.execute(request, response);
			
			// get remaining expression value.
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type after FOR conditional block execution.");

			return value.asBoolean(); 
		}
		
	},
	
	/**
	 * Casts a value to boolean.
	 * POP is the value to convert. 
	 * Returns value as boolean. 
	 */
	ASBOOLEAN (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	ASINT (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	ASFLOAT (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	ASSTRING (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	STRINGLENGTH (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRINGLENGTH call.");

			request.pushValue(Value.create(value.asString().length()));
		}
		
	},
	
	/**
	 * Gets the length of a string.
	 * First POP is the string to replace with. 
	 * Second POP is the replacement string. 
	 * Third POP is the string to do replacing in. 
	 * Returns integer. 
	 */
	STRINGREPLACE (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value3 = request.popValue();
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value3.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRINGREPLACE call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRINGREPLACE call.");
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRINGREPLACE call.");

			String replacement = value3.asString();
			String pattern = value2.asString();
			String source = value1.asString();
			
			request.pushValue(Value.create(source.replace(pattern, replacement)));
		}
		
	},
	
	// TODO: Finish this.
	
	;
	
	private boolean internal;
	private ArgumentType returnType;
	private ArgumentType[] argumentTypes;
	private boolean initializationBlockRequired;
	private boolean conditionalBlockRequired;
	private boolean stepBlockRequired;
	private boolean successBlockRequired;
	private boolean failureBlockRequired;
	
	private TAMECommand()
	{
		this(false, false, false, false, false, false, null, null);
	}

	private TAMECommand(boolean internal)
	{
		this(internal, false, false, false, false, false, null, null);
	}
	
	private TAMECommand(ArgumentType returnType, ArgumentType ... argumentTypes)
	{
		this(false, false, false, false, false, false, returnType, argumentTypes);
	}

	private TAMECommand(boolean internal, ArgumentType returnType, ArgumentType ... argumentTypes)
	{
		this(internal, false, false, false, false, false, returnType, argumentTypes);
	}

	private TAMECommand
	(
		boolean initializationBlockRequired, boolean conditionalBlockRequired, boolean stepBlockRequired, boolean successBlockRequired, boolean failureBlockPossible
	)
	{
		this(false, initializationBlockRequired, conditionalBlockRequired, stepBlockRequired, successBlockRequired, failureBlockPossible, null, null);
	}

	private TAMECommand
	(
		boolean internal,
		boolean initializationBlockRequired, boolean conditionalBlockRequired, boolean stepBlockRequired, boolean successBlockRequired, boolean failureBlockPossible,
		ArgumentType returnType, ArgumentType[] argumentTypes
	)
	{
		this.internal = internal;
		this.returnType = returnType;
		this.argumentTypes = argumentTypes;
		this.initializationBlockRequired = initializationBlockRequired;
		this.conditionalBlockRequired = conditionalBlockRequired;
		this.stepBlockRequired = stepBlockRequired;
		this.successBlockRequired = successBlockRequired;
		this.failureBlockRequired = failureBlockPossible;
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
	 * Returns if this requires more than one block with the statement.
	 * @return true if so, false if not.
	 */
	public boolean isEvaluating() 
	{
		return 
			isInitializationBlockRequired()
			|| isConditionalBlockRequired()
			|| isStepBlockRequired()
			|| isSuccessBlockRequired()
			|| isFailureBlockRequired();
	}
	
	@Override
	public boolean isInitializationBlockRequired() 
	{
		return initializationBlockRequired;
	}
	
	@Override
	public boolean isConditionalBlockRequired()
	{
		return conditionalBlockRequired;
	}
	
	@Override
	public boolean isStepBlockRequired() 
	{
		return stepBlockRequired;
	}
	
	@Override
	public boolean isSuccessBlockRequired() 
	{
		return successBlockRequired;
	}
	
	@Override
	public boolean isFailureBlockRequired() 
	{
		return failureBlockRequired;
	}

	@Override
	public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
	{
		throw new RuntimeException("UNIMPLEMENTED COMMAND");
	}

}
