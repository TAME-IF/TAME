package net.mtrop.tame;

import net.mtrop.tame.exception.TAMEFatalException;
import net.mtrop.tame.exception.UnexpectedValueException;
import net.mtrop.tame.exception.UnexpectedValueTypeException;
import net.mtrop.tame.interrupt.ErrorInterrupt;
import net.mtrop.tame.interrupt.QuitInterrupt;
import net.mtrop.tame.interrupt.TAMEInterrupt;
import net.mtrop.tame.lang.ArgumentType;
import net.mtrop.tame.lang.CommandType;
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
			// TODO: Finish this.
			return null;
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
			response.trace(request, "Throwing quit...");
			response.addCue(CUE_QUIT);
			throw new QuitInterrupt();
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
		boolean initializationBlockRequired, boolean conditionalBlockRequired, boolean stepBlockRequired, boolean successBlockRequired, boolean failureBlockRequired,
		ArgumentType returnType, ArgumentType[] argumentTypes
	)
	{
		this(false, initializationBlockRequired, conditionalBlockRequired, stepBlockRequired, successBlockRequired, failureBlockRequired, returnType, argumentTypes);
	}

	private TAMECommand
	(
		boolean internal,
		boolean initializationBlockRequired, boolean conditionalBlockRequired, boolean stepBlockRequired, boolean successBlockRequired, boolean failureBlockRequired,
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
		this.failureBlockRequired = failureBlockRequired;
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
