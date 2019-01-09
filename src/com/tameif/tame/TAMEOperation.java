/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.blackrook.commons.math.RMath;
import com.tameif.tame.element.ObjectContainer;
import com.tameif.tame.element.TAction;
import com.tameif.tame.element.TContainer;
import com.tameif.tame.element.TElement;
import com.tameif.tame.element.TObject;
import com.tameif.tame.element.TPlayer;
import com.tameif.tame.element.TRoom;
import com.tameif.tame.element.TWorld;
import com.tameif.tame.element.TAction.Type;
import com.tameif.tame.element.context.TElementContext;
import com.tameif.tame.element.context.TOwnershipMap;
import com.tameif.tame.exception.ModuleExecutionException;
import com.tameif.tame.exception.UnexpectedValueTypeException;
import com.tameif.tame.interrupt.BreakInterrupt;
import com.tameif.tame.interrupt.ContinueInterrupt;
import com.tameif.tame.interrupt.EndInterrupt;
import com.tameif.tame.interrupt.ErrorInterrupt;
import com.tameif.tame.interrupt.FinishInterrupt;
import com.tameif.tame.interrupt.QuitInterrupt;
import com.tameif.tame.lang.ArgumentType;
import com.tameif.tame.lang.Block;
import com.tameif.tame.lang.Operation;
import com.tameif.tame.lang.OperationType;
import com.tameif.tame.lang.TraceType;
import com.tameif.tame.lang.Value;
import com.tameif.tame.lang.ValueHash;
import com.tameif.tame.lang.ValueType;

/**
 * The set of operations.
 * Values in arguments are popped in reverse order on call, if arguments are taken. 
 * NOTE: THESE MUST BE DECLARED IN FULL CAPS TO ENSURE PARSING INTEGRITY!
 * @author Matthew Tropiano
 */
public enum TAMEOperation implements OperationType, TAMEConstants
{
	
	/**
	 * Does nothing.
	 * Returns nothing.
	 */
	NOOP (/*Return: */ null)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) 
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation)
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) 
		{
			Value varvalue = operation.getOperand0();
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in POPVALUE call.");
			if (!varvalue.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in POPVALUE call.");
			
			String variableName = varvalue.asString();
			if (blockLocal.containsKey(variableName))
			{
				response.trace(request, TraceType.VALUE, "SET LOCAL %s %s", variableName, value.toString());
				blockLocal.put(variableName, value);
			}
			else
			{
				response.trace(request, TraceType.VALUE, "SET %s.%s %s", request.peekContext().getElement().getIdentity(), variableName, value.toString());
				request.peekContext().setValue(variableName, value);
			}
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) 
		{
			Value varvalue = operation.getOperand0();
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in POPLOCALVALUE call.");
			if (!varvalue.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in POPLOCALVALUE call.");
			
			String variableName = varvalue.asString();
			response.trace(request, TraceType.VALUE, "SET LOCAL %s %s", variableName, value.toString());
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws ErrorInterrupt
		{
			Value varElement = operation.getOperand0();
			Value variable = operation.getOperand1();
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in POPELEMENTVALUE call.");
			if (!variable.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in POPELEMENTVALUE call.");
			if (!varElement.isElement())
				throw new UnexpectedValueTypeException("Expected element type in POPELEMENTVALUE call.");
			
			String variableName = variable.asString();
			TElementContext<?> context = request.getModuleContext().resolveElementContext(varElement); 
			response.trace(request, TraceType.VALUE, "SET %s.%s %s", context.getElement().getIdentity(), variableName, value.toString());
			context.setValue(variableName, value);
		}
		
	},

	/**
	 * [INTERNAL] Sets an index in a list-valued variable on a object.
	 * First POP is the value.
	 * Second POP is the index.
	 * Third POP is the list.
	 */
	POPLISTVALUE (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws ErrorInterrupt
		{
			Value value = request.popValue();
			Value index = request.popValue();
			Value listValue = request.popValue();
			
			if (!index.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in POPLISTVALUE call.");
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in POPLISTVALUE call.");
			if (!listValue.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in POPLISTVALUE call.");
			
			if (!listValue.isList())
				return;
			
			response.trace(request, TraceType.VALUE, "SET LIST [%d] %s", (int)index.asLong(), value.toString());
			listValue.listSet((int)index.asLong(), value);
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) 
		{
			Value value = operation.getOperand0();
			
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws ErrorInterrupt
		{
			Value varElement = operation.getOperand0();
			Value variable = operation.getOperand1();

			if (!variable.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in PUSHELEMENTVALUE call.");
			if (!varElement.isElement())
				throw new UnexpectedValueTypeException("Expected element type in PUSHELEMENTVALUE call.");

			String variableName = variable.asString();
			TElementContext<?> context = request.getModuleContext().resolveElementContext(varElement); 

			request.pushValue(context.getValue(variableName));
		}
		
	},
	
	/**
	 * [INTERNAL] Pushes a list-valued variable from the topmost context.
	 * First POP is the index.
	 * Second POP is the list.
	 * Pushes the value. If variable, it is resolved before the push.
	 */
	PUSHLISTVALUE (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) 
		{
			Value index = request.popValue();
			Value listValue = request.popValue();

			if (!listValue.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in PUSHLISTVALUE call.");
			if (!index.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in PUSHLISTVALUE call.");
			
			if (!listValue.isList())
				request.pushValue(Value.create(false));
			else
				request.pushValue(listValue.listGet((int)index.asLong()));
		}
		
	},
	
	/**
	 * [INTERNAL] Pushes a new empty list.
	 * Pops nothing.
	 * Pushes a new list. 
	 */
	PUSHNEWLIST (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws ErrorInterrupt
		{
			request.pushValue(Value.createEmptyList());
		}
		
	},
	
	/**
	 * [INTERNAL] Pushes a new list, initialized with values.
	 * First POP is length, then POPs [length] values and fills backwards.
	 * Pushes a new list. 
	 */
	PUSHINITLIST (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws ErrorInterrupt
		{
			Value length = request.popValue();

			if (!length.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in PUSHINITLIST call.");

			int size = (int)length.asLong();
			Value list = Value.createEmptyList(size);
			while (size-- > 0)
			{
				Value popped = request.popValue();
				if (!(popped.isLiteral() || popped.isList()))
					throw new UnexpectedValueTypeException("Expected literal or list type in PUSHINITLIST call.");
				list.listAddAt(0, popped);
			}
			
			request.pushValue(list);
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws ErrorInterrupt
		{
			Value variable = operation.getOperand0();

			if (!variable.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in CLEARVALUE call.");

			String variableName = variable.asString();
			if (blockLocal.containsKey(variableName))
			{
				response.trace(request, TraceType.VALUE, "CLEAR LOCAL %s", variableName);
				blockLocal.removeUsingKey(variableName);
			}
			else
			{
				response.trace(request, TraceType.VALUE, "CLEAR %s.%s", request.peekContext().getElement().getIdentity(), variableName);
				request.peekContext().clearValue(variableName);
			}
		}
		
	},
	
	/**
	 * [INTERNAL] Clears a variable from object member.
	 * Operand0 is the element. 
	 * Operand1 is the variable. 
	 * Returns nothing.
	 */
	CLEARELEMENTVALUE (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws ErrorInterrupt
		{
			Value varElement = operation.getOperand0();
			Value variable = operation.getOperand1();

			if (!variable.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in CLEARELEMENTVALUE call.");
			if (!varElement.isElement())
				throw new UnexpectedValueTypeException("Expected element type in CLEARELEMENTVALUE call.");

			String variableName = variable.asString();
			TElementContext<?> context = request.getModuleContext().resolveElementContext(varElement);
			response.trace(request, TraceType.VALUE, "CLEAR %s.%s", context.getElement().getIdentity(), variableName);
			context.clearValue(variableName);
		}
		
	},
	
	/**
	 * [INTERNAL] Pushes the element connected to the current context.
	 * Returns nothing.
	 */
	PUSHTHIS (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws ErrorInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation)
		{
			Value functionValue = operation.getOperand0();
			
			if (!functionValue.isInteger())
				throw new UnexpectedValueTypeException("Expected integer type in ARITHMETICFUNC call.");

			TAMELogic.doArithmeticStackFunction(request, response, (int)functionValue.asLong());
		}

	},
	
	/**
	 * [INTERNAL] If block.
	 * Has a conditional block that is called and then the success 
	 * block if POP is true, or if false and the fail block exists, call the fail block. 
	 * Returns nothing. 
	 */
	IF (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			// block should contain arithmetic operations and a last push.
			Block conditional = operation.getConditionalBlock();
			if (conditional == null)
				throw new ModuleExecutionException("Conditional block for IF does NOT EXIST!");
			
			response.trace(request, TraceType.CONTROL, "IF Conditional");
			conditional.execute(request, response, blockLocal);
			
			// get remaining expression value.
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type after IF conditional block execution.");
	
			boolean result = value.asBoolean();
			response.trace(request, TraceType.CONTROL, "IF Conditional %s is %b", value, result);
			if (result)
			{
				Block success = operation.getSuccessBlock();
				if (success == null)
					throw new ModuleExecutionException("Success block for IF does NOT EXIST!");
				success.execute(request, response, blockLocal);
			}
			else
			{
				Block failure = operation.getFailureBlock();
				if (failure != null)
					failure.execute(request, response, blockLocal);
			}
			
		}
		
	},
	
	/**
	 * [INTERNAL] WHILE block.
	 * Has a conditional block that is called and then the success block if POP is true. 
	 * Returns nothing. 
	 */
	WHILE (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			while (callConditional(request, response, blockLocal, operation))
			{
				try {
					Block success = operation.getSuccessBlock();
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
		
		private boolean callConditional(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			response.trace(request, TraceType.CONTROL, "WHILE Conditional");

			// block should contain arithmetic operations and a last push.
			Block conditional = operation.getConditionalBlock();
			if (conditional == null)
				throw new ModuleExecutionException("Conditional block for WHILE does NOT EXIST!");
			conditional.execute(request, response, blockLocal);
			
			// get remaining expression value.
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type after WHILE conditional block execution.");
	
			boolean out = value.asBoolean();
			response.trace(request, TraceType.CONTROL, "WHILE Conditional %s is %b", value, out);
			return out; 
		}
		
	}, 
	
	/**
	 * [INTERNAL] FOR block.
	 * Has an init block called once, a conditional block that is called and then the success block if POP is true,
	 * and another block for the next step. 
	 * Returns nothing. 
	 */
	FOR (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			Block init = operation.getInitBlock();
			if (init == null)
				throw new ModuleExecutionException("Init block for FOR does NOT EXIST!");
			Block success = operation.getSuccessBlock();
			if (success == null)
				throw new ModuleExecutionException("Success block for FOR does NOT EXIST!");
			Block step = operation.getStepBlock();
			if (step == null)
				throw new ModuleExecutionException("Step block for FOR does NOT EXIST!");

			response.trace(request, TraceType.CONTROL, "FOR Init");
			for (
				init.execute(request, response, blockLocal); 
				callConditional(request, response, blockLocal, operation); 
				response.trace(request, TraceType.CONTROL, "FOR Step"),
				step.execute(request, response, blockLocal)
			)
			{
				try {
					response.trace(request, TraceType.CONTROL, "FOR Success");
					success.execute(request, response, blockLocal);
				} catch (BreakInterrupt interrupt) {
					break;
				} catch (ContinueInterrupt interrupt) {
					continue;
				}
			}
		}

		private boolean callConditional(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			response.trace(request, TraceType.CONTROL, "FOR Contitional");
			
			// block should contain arithmetic operations and a last push.
			Block conditional = operation.getConditionalBlock();
			if (conditional == null)
				throw new ModuleExecutionException("Conditional block for FOR does NOT EXIST!");
			conditional.execute(request, response, blockLocal);
			
			// get remaining expression value.
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type after FOR conditional block execution.");
	
			boolean out = value.asBoolean();
			response.trace(request, TraceType.CONTROL, "FOR Conditional %s is %b", value, out);
			return out; 
		}
		
	}, 
	
	/**
	 * [INTERNAL] Throws a BREAK interrupt.
	 * Is keyword. Returns nothing. 
	 */
	BREAK (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			response.trace(request, TraceType.CONTROL, "THROW BREAK");
			throw new BreakInterrupt();
		}
		
	},
	
	/**
	 * [INTERNAL] Throws a CONTINUE interrupt.
	 * Is keyword. Returns nothing. 
	 */
	CONTINUE (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			response.trace(request, TraceType.CONTROL, "THROW CONTINUE");
			throw new ContinueInterrupt();
		}
		
	},
	
	/**
	 * [INTERNAL] Adds a QUIT cue to the response and throws a QUIT interrupt.
	 * Is keyword. Returns nothing. 
	 */
	QUIT (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			response.addCue(CUE_QUIT);
			response.trace(request, TraceType.CONTROL, "THROW QUIT");
			throw new QuitInterrupt();
		}
		
	},
	
	/**
	 * [INTERNAL] Throws a FINISH interrupt.
	 * Is keyword. Returns nothing. 
	 */
	FINISH (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			response.trace(request, TraceType.CONTROL, "THROW FINISH");
			throw new FinishInterrupt();
		}
		
	},
	
	/**
	 * [INTERNAL] Throws an END interrupt.
	 * Is keyword. Returns nothing. 
	 */
	END (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			response.trace(request, TraceType.CONTROL, "THROW END");
			throw new EndInterrupt();
		}
		
	},
	
	/**
	 * [INTERNAL] Return from function.
	 * Sets RETURN value on blocklocal from POP and then throws an END interrupt.
	 */
	FUNCTIONRETURN (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			Value retVal = request.popValue();
			response.trace(request, TraceType.FUNCTION, "RETURN "+retVal.toString());
			blockLocal.put(RETURN_VARIABLE, retVal);
			response.trace(request, TraceType.CONTROL, "THROW END");
			throw new EndInterrupt();
		}
		
	},
	
	/**
	 * [INTERNAL] Calls a function.
	 * Operand0 is the function name.
	 * Pops a varying amount of values off the stack depending on the function.
	 * Pushes result.
	 * Returns nothing.
	 */
	CALLFUNCTION (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			Value varFunctionName = operation.getOperand0();

			if (!varFunctionName.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in CALLFUNCTION call.");

			request.pushValue(TAMELogic.callElementFunction(request, response, varFunctionName.asString(), request.peekContext()));
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
	CALLELEMENTFUNCTION (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			Value varElement = operation.getOperand0();
			Value varFunctionName = operation.getOperand1();

			if (!varElement.isElement())
				throw new UnexpectedValueTypeException("Expected element type in CALLELEMENTFUNCTION call.");
			if (!varFunctionName.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in CALLELEMENTFUNCTION call.");
			
			request.pushValue(TAMELogic.callElementFunction(request, response, varFunctionName.asString(), request.getModuleContext().resolveElementContext(varElement)));
		}
		
	},
	
	/**
	 * Enqueues a general action to perform after the current one finishes.
	 * POP is the action.
	 * Returns nothing.
	 */
	QUEUEACTION (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			Value varAction = request.popValue();
			
			if (varAction.getType() != ValueType.ACTION)
				throw new UnexpectedValueTypeException("Expected action type in QUEUEACTION call.");

			TAction action = request.getModuleContext().resolveAction(varAction.asString());
			
			if (action.getType() != Type.GENERAL)
				throw new UnexpectedValueTypeException("BAD TYPE: "+action.getIdentity() + " is not a general action.");
			else
			{
				TAMECommand command = TAMECommand.create(action);
				request.addCommand(command);
				response.trace(request, TraceType.CONTROL, "Enqueue command "+command);
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
	QUEUEACTIONSTRING (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
				TAMECommand command = TAMECommand.create(action, target);
				request.addCommand(command);
				response.trace(request, TraceType.CONTROL, "Enqueue command "+command);
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
	QUEUEACTIONOBJECT (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
				TAMECommand command = TAMECommand.create(action, object);
				request.addCommand(command);
				response.trace(request, TraceType.CONTROL, "Enqueue command "+command);
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
	QUEUEACTIONFOROBJECTSIN (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
				TAMECommand command = TAMECommand.create(action, object);
				request.addCommand(command);
				response.trace(request, TraceType.CONTROL, "Enqueue command "+command);
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
	QUEUEACTIONFORTAGGEDOBJECTSIN (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
				TAMECommand command = TAMECommand.create(action, object);
				request.addCommand(command);
				response.trace(request, TraceType.CONTROL, "Enqueue command "+command);
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
	QUEUEACTIONOBJECT2 (true)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
				TAMECommand command = TAMECommand.create(action, object, object2);
				request.addCommand(command);
				response.trace(request, TraceType.CONTROL, "Enqueue command "+command);
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
	 * Casts a value to a list with one .
	 * POP is the value to convert. 
	 * Returns value as string. 
	 */
	ASLIST (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in ASLIST call.");

			Value out = Value.createEmptyList(2);
			out.listAdd(value);
			request.pushValue(out);
		}
		
		@Override
		public String getGrouping()
		{
			return "Values";
		}
		
	},
	
	/**
	 * Gets the length of a value.
	 * POP is the value. 
	 * Returns integer. 
	 */
	LENGTH (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in LENGTH call.");

			request.pushValue(Value.create(value.length()));
		}
		
		@Override
		public String getGrouping()
		{
			return "Values";
		}
		
	},
	
	/**
	 * Gets if a a value is "empty."
	 * POP is the value. 
	 * Returns boolean. 
	 */
	EMPTY (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			Value value = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in EMPTY call.");

			request.pushValue(Value.create(value.isEmpty()));
		}
		
		@Override
		public String getGrouping()
		{
			return "Values";
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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

			try {
				request.pushValue(Value.create(source.replaceFirst(pattern, replacement)));
			} catch (PatternSyntaxException e) {
				throw new UnexpectedValueTypeException("Expected valid RegEx in STRREPLACEPATTERN call.");
			}
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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

			try {
				request.pushValue(Value.create(source.replaceAll(pattern, replacement)));
			} catch (PatternSyntaxException e) {
				throw new UnexpectedValueTypeException("Expected valid RegEx in STRREPLACEPATTERNALL call.");
			}
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
	 * Returns if a character sequence matching a regular expression exists in a given string. 
	 * True if so. False if not. Fatal error on bad pattern.
	 * First POP is what to search for (RegEx). 
	 * Second POP is the string. 
	 * Returns boolean.
	 */
	STRCONTAINSPATTERN (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
	 * Returns a string split into a list of strings using a regular expression for matching the delimitation.
	 * First POP is the regular expression. 
	 * Second POP is the string. 
	 * Returns a list or FALSE if the pattern is not a regular expression.
	 */
	STRSPLIT (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			Value value2 = request.popValue();
			Value value1 = request.popValue();
			
			if (!value1.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRSPLIT call.");
			if (!value2.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRSPLIT call.");

			String pattern = value2.asString();
			String str = value1.asString();
			
			try {
				String[] tokens = str.split(pattern);
				Value out = Value.createEmptyList(tokens.length);
				for (String s : tokens)
					out.listAdd(Value.create(s));
				request.pushValue(out);
			} catch (PatternSyntaxException e) {
				throw new UnexpectedValueTypeException("Expected valid RegEx in STRSPLIT call.");
			}
		}
		
		@Override
		public String getGrouping()
		{
			return "String Operations";
		}
		
	},
	
	/**
	 * Returns a string that is the result of joining a list of strings together with a string in between each element.
	 * First POP is the string to put in between tokens. 
	 * Second POP is the list of values to treat as strings. 
	 * Returns a single string.
	 */
	STRJOIN (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			Value joiner = request.popValue();
			Value list = request.popValue();
			
			if (!joiner.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRJOIN call.");
			if (!list.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in STRJOIN call.");

			String str = joiner.asString();
			
			if (!list.isList() || list.length() == 1)
				request.pushValue(Value.create(list.asString()));
			else
			{
				StringBuilder sb = new StringBuilder();
				int len = list.length();
				for (int i = 0; i < len; i++)
				{
					sb.append(list.listGet(i).asString());
					if (i < len - 1)
						sb.append(str);
				}
				request.pushValue(Value.create(sb.toString()));
			}
			
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
	 * Adds a value to the end of the list.
	 * First POP is the value to add. 
	 * Second POP is the list. 
	 * Returns boolean. 
	 */
	LISTADD (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			Value value = request.popValue();
			Value list = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in LISTADD call.");
			if (!list.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in LISTADD call.");

			request.pushValue(Value.create(list.listAdd(value)));
		}
		
		@Override
		public String getGrouping()
		{
			return "List Operations";
		}
		
	},
		
	/**
	 * Adds a value to an index in the list, shifting contents.
	 * First POP is the target index. 
	 * Second POP is the value to add. 
	 * Third POP is the list. 
	 * Returns boolean. 
	 */
	LISTADDAT (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			Value index = request.popValue();
			Value value = request.popValue();
			Value list = request.popValue();
			
			if (!index.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in LISTADDAT call.");
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in LISTADDAT call.");
			if (!list.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in LISTADDAT call.");

			request.pushValue(Value.create(list.listAddAt((int)index.asLong(), value)));
		}
		
		@Override
		public String getGrouping()
		{
			return "List Operations";
		}
		
	},
		
	/**
	 * Removes a value from the list, if the value is found in it, shifting contents.
	 * First POP is the value to add. 
	 * Second POP is the list. 
	 * Returns boolean. 
	 */
	LISTREMOVE (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			Value value = request.popValue();
			Value list = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in LISTREMOVE call.");
			if (!list.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in LISTREMOVE call.");

			request.pushValue(Value.create(list.listRemove(value)));
		}
		
		@Override
		public String getGrouping()
		{
			return "List Operations";
		}
		
	},
		
	/**
	 * Removes a value from the list by index, shifting contents.
	 * First POP is the index. 
	 * Second POP is the list. 
	 * Returns the value removed. 
	 */
	LISTREMOVEAT (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			Value index = request.popValue();
			Value list = request.popValue();
			
			if (!index.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in LISTREMOVEAT call.");
			if (!list.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in LISTREMOVEAT call.");

			request.pushValue(list.listRemoveAt((int)index.asLong()));
		}
		
		@Override
		public String getGrouping()
		{
			return "List Operations";
		}
		
	},
		
	/**
	 * Concatenates two lists and creates a new list.
	 * First POP is the value/list to append. 
	 * Second POP is the source list. 
	 * Returns the value removed. 
	 */
	LISTCONCAT (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			Value appendix = request.popValue();
			Value list = request.popValue();
			
			if (!appendix.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in LISTCONCAT call.");
			if (!list.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in LISTCONCAT call.");

			if (!list.isList())
			{
				Value v = list;
				list = Value.createEmptyList(1);
				list.listAdd(v);
			}

			if (!appendix.isList())
			{
				Value v = appendix;
				appendix = Value.createEmptyList(1);
				appendix.listAdd(v);
			}
			
			Value out = Value.createEmptyList();
			for (int i = 0; i < list.length(); i++)
				out.listAdd(list.listGet(i));
			for (int i = 0; i < appendix.length(); i++)
				out.listAdd(appendix.listGet(i));
			
			request.pushValue(out);
		}
		
		@Override
		public String getGrouping()
		{
			return "List Operations";
		}
		
	},
		
	/**
	 * Gets the index of a value in the list, starting from the beginning.
	 * First POP is the value to search for. 
	 * Second POP is the list. 
	 * Returns integer. 
	 */
	LISTINDEX (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			Value value = request.popValue();
			Value list = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in LISTINDEX call.");
			if (!list.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in LISTINDEX call.");

			request.pushValue(Value.create(list.listIndexOf(value)));
		}
		
		@Override
		public String getGrouping()
		{
			return "List Operations";
		}
		
	},
		
	/**
	 * Searches for the existence of a value in the list, starting from the beginning.
	 * First POP is the value to search for. 
	 * Second POP is the list. 
	 * Returns boolean. 
	 */
	LISTCONTAINS (/*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VALUE, ArgumentType.VALUE)
	{
		@Override
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			Value value = request.popValue();
			Value list = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in LISTCONTAINS call.");
			if (!list.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in LISTCONTAINS call.");

			request.pushValue(Value.create(list.listContains(value)));
		}
		
		@Override
		public String getGrouping()
		{
			return "List Operations";
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
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
		protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
		{
			Value headerName = request.popValue();

			if (!headerName.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in HEADER call.");

			String val = request.getModuleContext().getModule().getHeader().getAttribute(headerName.asString());
			if (val != null)
				request.pushValue(Value.create(val));
			else
				request.pushValue(Value.create(false));
		}
		
		@Override
		public String getGrouping()
		{
			return "Miscellaneous";
		}
		
	}

	;
	
	/** Array to get around multiple allocations. */
	public static final TAMEOperation[] VALUES = values();
	
	private boolean internal;
	private ArgumentType returnType;
	private ArgumentType[] argumentTypes;
	
	private TAMEOperation(boolean internal)
	{
		this(internal, null, null);
	}
	
	private TAMEOperation(ArgumentType returnType, ArgumentType ... argumentTypes)
	{
		this(false, returnType, argumentTypes);
	}

	private TAMEOperation(boolean internal, ArgumentType returnType, ArgumentType[] argumentTypes)
	{
		this.internal = internal;
		this.returnType = returnType;
		this.argumentTypes = argumentTypes;
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
	 * Performs the operation.
	 * @param request the TAMERequest context.
	 * @param response the TAMEResponse object.
	 * @param blockLocal the local variables on the block call.
	 * @param operation the operation origin.
	 * @throws TAMEInterrupt if an interrupt occurs. 
	 */
	protected void doOperation(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
	{
		throw new RuntimeException("UNIMPLEMENTED OPERATION");
	}
	
	/**
	 * Gets the grouping name for this operation (for documentation sorting).
	 * @return the grouping name.
	 */
	public String getGrouping()
	{
		return null;
	}
	
	/**
	 * Increments the runaway operation counter and calls the operation.  
	 * @param request the request object.
	 * @param response the response object.
	 * @param blockLocal the local variables on the block call.
	 * @param operation the operation object.
	 * @throws TAMEInterrupt if an interrupt occurs. 
	 */
	public final void execute(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Operation operation) throws TAMEInterrupt
	{
		doOperation(request, response, blockLocal, operation);
		response.incrementAndCheckOperationsExecuted(request.getModuleContext().getOperationRunawayMax());
	}
	
}
