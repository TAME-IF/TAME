package net.mtrop.tame;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.blackrook.commons.math.RMath;

import net.mtrop.tame.element.TAction;
import net.mtrop.tame.element.TObject;
import net.mtrop.tame.element.TPlayer;
import net.mtrop.tame.element.TRoom;
import net.mtrop.tame.element.TWorld;
import net.mtrop.tame.element.context.TObjectContext;
import net.mtrop.tame.element.context.TOwnershipMap;
import net.mtrop.tame.element.context.TPlayerContext;
import net.mtrop.tame.element.context.TRoomContext;
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
import net.mtrop.tame.lang.Block;
import net.mtrop.tame.lang.Command;
import net.mtrop.tame.lang.CommandType;
import net.mtrop.tame.lang.Value;
import net.mtrop.tame.lang.ValueType;

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

			request.getModuleContext().resolveObjectContext(objectName).setValue(variableName, value);
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

			request.getModuleContext().resolveRoomContext(roomName).setValue(variableName, value);
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

			request.getModuleContext().resolveRoomContext(playerName).setValue(variableName, value);
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
			request.pushValue(request.getModuleContext().resolveObjectVariableValue(objectName, variableName));
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
			request.pushValue(request.getModuleContext().resolveRoomVariableValue(roomName, variableName));
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
			request.pushValue(request.getModuleContext().resolvePlayerVariableValue(playerName, variableName));
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
	 * Second POP is the replacement sequence. 
	 * Third POP is the string to do replacing in. 
	 * Returns string. 
	 */
	STRINGREPLACE (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
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
	
	/**
	 * Returns the index of where a pattern starts in a string. -1 is not found.
	 * First POP is what to search for. 
	 * Second POP is the string. 
	 * Returns integer.
	 */
	STRINGINDEX (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRINGINDEX call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRINGINDEX call.");

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
	STRINGLASTINDEX (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRINGLASTINDEX call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRINGLASTINDEX call.");

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
	STRINGCONTAINS (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRINGCONTAINS call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRINGCONTAINS call.");

			String sequence = value2.asString();
			String str = value1.asString();
			
			request.pushValue(Value.create(str.contains(sequence)));
		}
		
	},
	
	/**
	 * Returns if a character sequence matching a regular expression exists in a given string. True if so.
	 * First POP is what to search for (RegEx). 
	 * Second POP is the string. 
	 * Returns boolean.
	 */
	STRINGCONTAINSPATTERN (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRINGCONTAINSPATTERN call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRINGCONTAINSPATTERN call.");

			String pattern = value2.asString();
			String str = value1.asString();
			
			Pattern p = null;
			try {
				p = Pattern.compile(pattern);
			} catch (PatternSyntaxException e) {
				throw new ErrorInterrupt("Expected valid expression in STRINGCONTAINSPATTERN call.");
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
	STRINGCONTAINSTOKEN (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRINGCONTAINSTOKEN call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRINGCONTAINSTOKEN call.");

			String pattern = value2.asString();
			String str = value1.asString();
			
			Pattern p = null;
			try {
				p = Pattern.compile(pattern);
			} catch (PatternSyntaxException e) {
				throw new ErrorInterrupt("Expected valid expression in STRINGCONTAINSPATTERN call.");
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
	SUBSTRING (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value3 = request.popValue();
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value3.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in SUBSTRING call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in SUBSTRING call.");
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in SUBSTRING call.");

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
	LOWERCASE (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in LOWERCASE call.");

			request.pushValue(Value.create(value.asString().toLowerCase()));
		}
		
	},
	
	/**
	 * Gets a string converted to uppercase.
	 * POP is the string. 
	 * Returns string. 
	 */
	UPPERCASE (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in UPPERCASE call.");

			request.pushValue(Value.create(value.asString().toUpperCase()));
		}
		
	},
	
	/**
	 * Gets a single character from a string.
	 * First POP is the index. 
	 * Second POP is the string. 
	 * Returns string or empty string if index is out of range.
	 */
	CHARACTER (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in CHARACTER call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in CHARACTER call.");

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
	FLOOR (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	CEILING (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	ROUND (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	FIX (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	 * Gets a number to a certain power.
	 * First POP is the power.
	 * Second POP is the number. 
	 * Returns float.
	 */
	POW (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value power = request.popValue();
			Value number = request.popValue();
			
			if (!number.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in POW call.");
			if (!power.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in POW call.");

			double d = number.asDouble();
			double f = power.asDouble();

			request.pushValue(Value.create(Math.pow(d, f)));
		}
		
	},
	
	/**
	 * Gets the square root of a number.
	 * POP is the number.
	 * Returns float.
	 */
	SQRT (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	PI (false, /*Return: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			request.pushValue(Value.create(Math.PI));
		}
		
	},
	
	/**
	 * Returns Euler's number in this implementation.
	 * POPs nothing.
	 * Returns float.
	 */
	E (false, /*Return: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			request.pushValue(Value.create(Math.E));
		}
		
	},
	
	/**
	 * Returns sine of a number. Degrees are in radians.
	 * POP is the number.
	 * Returns float.
	 */
	SIN (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	COS (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value number = request.popValue();
			
			if (!number.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in COS call.");

			request.pushValue(Value.create(Math.cos(number.asDouble())));
		}
		
	},
	
	/**
	 * Returns the minimum of two numbers.
	 * First POP is the second number.
	 * Second POP is the first number.
	 * Returns type of "winner."
	 */
	MIN (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	MAX (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	CLAMP (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	RANDOM (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
					request.pushValue(Value.create(random.nextLong() % value));
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
	FRANDOM (false, /*Return: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	GRANDOM (false, /*Return: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	TIME (false, /*Return: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	SECONDS (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value valueSecond = request.popValue();
			Value valueFirst = request.popValue();
			
			if (!valueSecond.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in SECONDS call.");
			if (!valueFirst.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in SECONDS call.");

			long first = valueFirst.asLong();
			long second = valueSecond.asLong();
			
			request.pushValue(Value.create((first - second) / 1000L));
		}
		
	},
	
	/**
	 * Returns the difference in time in minutes. 
	 * Assumes inputs are integers and millisecond time.
	 * First POP is second value.
	 * Second POP is first value.
	 * Returns integer.
	 */
	MINUTES (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value valueSecond = request.popValue();
			Value valueFirst = request.popValue();
			
			if (!valueSecond.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in MINUTES call.");
			if (!valueFirst.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in MINUTES call.");

			long first = valueFirst.asLong();
			long second = valueSecond.asLong();
			
			request.pushValue(Value.create((first - second) / (1000L * 60L)));
		}
		
	},
	
	/**
	 * Returns the difference in time in hours. 
	 * Assumes inputs are integers and millisecond time.
	 * First POP is second value.
	 * Second POP is first value.
	 * Returns integer.
	 */
	HOURS (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value valueSecond = request.popValue();
			Value valueFirst = request.popValue();
			
			if (!valueSecond.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in HOURS call.");
			if (!valueFirst.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in HOURS call.");

			long first = valueFirst.asLong();
			long second = valueSecond.asLong();
			
			request.pushValue(Value.create((first - second) / (1000L * 60L * 60L)));
		}
		
	},
	
	/**
	 * Returns the difference in time in days. 
	 * Assumes inputs are integers and millisecond time.
	 * First POP is second value.
	 * Second POP is first value.
	 * Returns integer.
	 */
	DAYS (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value valueSecond = request.popValue();
			Value valueFirst = request.popValue();
			
			if (!valueSecond.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in HOURS call.");
			if (!valueFirst.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in HOURS call.");

			long first = valueFirst.asLong();
			long second = valueSecond.asLong();
			
			request.pushValue(Value.create((first - second) / (1000L * 60L * 60L * 24L)));
		}
		
	},
	
	/**
	 * Formats a date as a string. 
	 * Assumes inputs are integers and millisecond time.
	 * First POP is formatting string.
	 * Second POP is date value.
	 * Returns string.
	 */
	FORMATDATE (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	 * Calls the onPlayerBrowse() blocks on objects on a player.
	 * POP is player. 
	 * Returns nothing.
	 */
	BROWSEPLAYER (false, /*Return: */ null, /*Args: */ ArgumentType.PLAYER)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value playerValue = request.popValue();
			
			if (playerValue.getType() != ValueType.PLAYER)
				throw new UnexpectedValueTypeException("Expected player type in BROWSEPLAYER call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TPlayerContext playerContext = moduleContext.resolvePlayerContext(playerValue.asString());
			TPlayer player = playerContext.getElement();
			TOwnershipMap ownership = request.getModuleContext().getOwnershipMap();
			
			response.trace(request, "Start browse %s.", player);
			
			for (TObject object : ownership.getObjectsOwnedByPlayer(player))
			{
				response.trace(request, "Check %s for browse block.", object);

				TObjectContext objectContext = moduleContext.getObjectContext(object);

				Block block = object.getPlayerBrowseBlock();
				if (block != null)
				{
					response.trace(request, "Calling player browse block on %s.", object);
					TAMELogic.callBlock(request, response, objectContext, block);
				}
				
			}
			
		}
		
	},
	
	/**
	 * Calls the onRoomBrowse() blocks on objects on a room.
	 * POP is room. 
	 * Returns nothing.
	 */
	BROWSEROOM (false, /*Return: */ null, /*Args: */ ArgumentType.ROOM)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value roomValue = request.popValue();
			
			if (roomValue.getType() != ValueType.ROOM)
				throw new UnexpectedValueTypeException("Expected room type in BROWSEROOM call.");

			TAMEModuleContext moduleContext = request.getModuleContext();
			TRoomContext roomContext = moduleContext.resolveRoomContext(roomValue.asString());
			TRoom room = roomContext.getElement();
			TOwnershipMap ownership = request.getModuleContext().getOwnershipMap();
			
			response.trace(request, "Start browse %s.", room);
			
			for (TObject object : ownership.getObjectsOwnedByRoom(room))
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
		
	},
	
	/**
	 * Adds an object name to the object.
	 * First POP is name.
	 * Second POP is object. 
	 * Returns nothing.
	 */
	ADDOBJECTNAME (false, /*Return: */ null, /*Args: */ ArgumentType.OBJECT, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	GIVEWORLDOBJECT (false, /*Return: */ null, /*Args: */ ArgumentType.OBJECT)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	GIVEPLAYEROBJECT (false, /*Return: */ null, /*Args: */ ArgumentType.PLAYER, ArgumentType.OBJECT)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	 * Adds a room to a player.
	 * First POP is object.
	 * Second POP is room. 
	 * Returns nothing.
	 */
	GIVEROOMOBJECT (false, /*Return: */ null, /*Args: */ ArgumentType.ROOM, ArgumentType.OBJECT)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	 * Removes an object from its owner, whatever it is.
	 * POP is object.
	 * Returns nothing.
	 */
	REMOVEOBJECT (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.OBJECT)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	OBJECTHASNOOWNER (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.OBJECT)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	OBJECTSINWORLDCOUNT (false, /*Return: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			request.pushValue(Value.create(request.getModuleContext().getOwnershipMap().getObjectsOwnedByWorldCount(request.getModuleContext().resolveWorld())));
		}
		
	},
	
	/**
	 * Counts the objects in a player.
	 * POP is the player.
	 * Returns integer.
	 */
	OBJECTSINPLAYERCOUNT (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.PLAYER)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	 * POP is the player.
	 * Returns integer.
	 */
	OBJECTSINROOMCOUNT (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.ROOM)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varRoom = request.popValue();
			
			if (varRoom.getType() != ValueType.ROOM)
				throw new UnexpectedValueTypeException("Expected room type in OBJECTSINROOMCOUNT call.");
			
			TRoom room = request.getModuleContext().resolveRoom(varRoom.asString());
			request.pushValue(Value.create(request.getModuleContext().getOwnershipMap().getObjectsOwnedByRoomCount(room)));
		}
		
	},
	
	/**
	 * Checks if an object is owned by the world.
	 * POP is the object.
	 * Returns boolean.
	 */
	WORLDHASOBJECT (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.OBJECT)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	 * Checks if an object is owned by the player.
	 * First POP is the object.
	 * Second POP is the player.
	 * Returns boolean.
	 */
	PLAYERHASOBJECT (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.PLAYER, ArgumentType.OBJECT)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	 * Checks if an object is owned by the room.
	 * POP is the player.
	 * Returns boolean.
	 */
	ROOMHASOBJECT (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.ROOM, ArgumentType.OBJECT)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	 * Checks if a player in in a room. Returns true if current room or in the room stack.
	 * First POP is the room.
	 * Second POP is the player.
	 * Returns boolean.
	 */
	PLAYERISINROOM (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.PLAYER, ArgumentType.ROOM)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	PLAYERCANACCESSOBJECT (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.PLAYER, ArgumentType.OBJECT)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	 * Sets the current player. Calls unfocus on previous player, then focus on next player.
	 * POP is the player.
	 * Returns nothing.
	 */
	FOCUSONPLAYER (false, /*Return: */ null, /*Args: */ ArgumentType.PLAYER)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varPlayer = request.popValue();
			
			if (varPlayer.getType() != ValueType.PLAYER)
				throw new UnexpectedValueTypeException("Expected player type in FOCUSONPLAYER call.");

			TPlayer nextPlayer = request.getModuleContext().resolvePlayer(varPlayer.asString());
			TAMELogic.doPlayerSwitch(request, response, nextPlayer);
		}
		
	},

	/**
	 * Sets the current room and clears the stack (for the current player).
	 * POP is the new room.
	 * Returns nothing.
	 */
	FOCUSONROOM (false, /*Return: */ null, /*Args: */ ArgumentType.ROOM)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varRoom = request.popValue();
			
			if (varRoom.getType() != ValueType.ROOM)
				throw new UnexpectedValueTypeException("Expected room type in FOCUSONROOM call.");

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
	PUSHROOM (false, /*Return: */ null, /*Args: */ ArgumentType.ROOM)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	POPROOM (false, /*Return: */ null)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	SWAPROOM (false, /*Return: */ null, /*Args: */ ArgumentType.ROOM)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	CURRENTPLAYERIS (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.PLAYER)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	NOCURRENTPLAYER (false, /*Return: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	CURRENTROOMIS (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.ROOM)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	NOCURRENTROOM (false, /*Return: */ ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
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
	ENQUEUEACTION (false, /*Return: */ null, /*Args: */ ArgumentType.ACTION)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varAction = request.popValue();
			
			if (varAction.getType() != ValueType.ACTION)
				throw new UnexpectedValueTypeException("Expected action type in ENQUEUEACTION call.");

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
	ENQUEUEACTIONSTRING (false, /*Return: */ null, /*Args: */ ArgumentType.ACTION, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varTarget = request.popValue();
			Value varAction = request.popValue();
			
			if (!varTarget.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in ENQUEUEACTIONSTRING call.");
			if (varAction.getType() != ValueType.ACTION)
				throw new UnexpectedValueTypeException("Expected action type in ENQUEUEACTIONSTRING call.");

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
	ENQUEUEACTIONOBJECT (false, /*Return: */ null, /*Args: */ ArgumentType.ACTION, ArgumentType.OBJECT)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varObject = request.popValue();
			Value varAction = request.popValue();
			
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in ENQUEUEACTIONOBJECT call.");
			if (varAction.getType() != ValueType.ACTION)
				throw new UnexpectedValueTypeException("Expected action type in ENQUEUEACTIONOBJECT call.");

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
	ENQUEUEACTIONOBJECT2 (false, /*Return: */ null, /*Args: */ ArgumentType.ACTION, ArgumentType.OBJECT, ArgumentType.OBJECT)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value varObject2 = request.popValue();
			Value varObject = request.popValue();
			Value varAction = request.popValue();
			
			if (varObject2.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in ENQUEUEACTIONOBJECT2 call.");
			if (varObject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in ENQUEUEACTIONOBJECT2 call.");
			if (varAction.getType() != ValueType.ACTION)
				throw new UnexpectedValueTypeException("Expected action type in ENQUEUEACTIONOBJECT2 call.");

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
	IDENTITY (false, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.ELEMENT)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Command command) throws TAMEInterrupt
		{
			Value element = request.popValue();

			TAMEModuleContext moduleContext = request.getModuleContext();
			
			// must resolve: the passed-in value could be the "current" room/player.
			
			if (element.getType() == ValueType.ROOM)
				request.pushValue(Value.create(moduleContext.resolveRoom(element.asString()).getIdentity()));
			else if (element.getType() == ValueType.PLAYER)
				request.pushValue(Value.create(moduleContext.resolvePlayer(element.asString()).getIdentity()));
			else if (element.getType() == ValueType.OBJECT)
				request.pushValue(Value.create(moduleContext.resolveObject(element.asString()).getIdentity()));
			else
				throw new UnexpectedValueTypeException("Expected element type in IDENTITY call.");
		}
		
	}

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
