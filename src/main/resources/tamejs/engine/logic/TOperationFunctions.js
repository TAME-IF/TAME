/*******************************************************************************
 * Copyright (c) 2016-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var TCommand = TCommand || ((typeof require) !== 'undefined' ? require('../objects/TCommand.js') : null);
var TValue = TValue || ((typeof require) !== 'undefined' ? require('../objects/TValue.js') : null);
var TLogic = TLogic || ((typeof require) !== 'undefined' ? require('../TAMELogic.js') : null);
var TAMEConstants = TAMEConstants || ((typeof require) !== 'undefined' ? require('../TAMEConstants.js') : null);
var TAMEError = TAMEError || ((typeof require) !== 'undefined' ? require('../TAMEError.js') : null);
var TAMEInterrupt = TAMEInterrupt || ((typeof require) !== 'undefined' ? require('../TAMEInterrupt.js') : null);
var Util = Util || ((typeof require) !== 'undefined' ? require('../Util.js') : null);
// ======================================================================================================

//[[EXPORTJS-START

/*****************************************************************************
Operation entry points.
*****************************************************************************/
var TOperationFunctions =
[
	/* NOOP */
	{
		"name": 'NOOP', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			// Do nothing.
		}
	},

	/* POP */
	{
		"name": 'POP',
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			request.popValue();
		}
	},

	/* POPVALUE */
	{
		"name": 'POPVALUE', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varvalue = operation.operand0;
			let value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in POPVALUE call.");
			if (!TValue.isVariable(varvalue))
				throw TAMEError.UnexpectedValueType("Expected variable type in POPVALUE call.");

			let variableName = TValue.asString(varvalue);
			
			if (TLogic.containsValue(blockLocal, variableName))
			{
				response.trace(request, TAMEConstants.TraceType.VALUE, Util.format("SET LOCAL {0} {1}", variableName, TValue.toString(value)));
				TLogic.setValue(blockLocal, variableName, value);
			}
			else
			{
				response.trace(request, TAMEConstants.TraceType.VALUE, Util.format("SET {0}.{1} {2}", request.peekContext().identity, variableName, TValue.toString(value)));
				TLogic.setValue(request.peekContext().variables, variableName, value);
			}
		}
	},

	/* POPLOCALVALUE */
	{
		"name": 'POPLOCALVALUE', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varvalue = operation.operand0;
			let value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in POPLOCALVALUE call.");
			if (!TValue.isVariable(varvalue))
				throw TAMEError.UnexpectedValueType("Expected variable type in POPLOCALVALUE call.");

			let variableName = TValue.asString(varvalue);
			response.trace(request, TAMEConstants.TraceType.VALUE, Util.format("SET LOCAL {0} {1}", variableName, TValue.toString(value)));
			TLogic.setValue(blockLocal, variableName, value);
		}
	},

	/* POPELEMENTVALUE */
	{
		"name": 'POPELEMENTVALUE', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varElement = operation.operand0;
			let variable = operation.operand1;
			let value = request.popValue();

			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in POPELEMENTVALUE call.");
			if (!TValue.isVariable(variable))
				throw TAMEError.UnexpectedValueType("Expected variable type in POPELEMENTVALUE call.");
			if (!TValue.isElement(varElement))
				throw TAMEError.UnexpectedValueType("Expected element type in POPELEMENTVALUE call.");

			let elementName = TValue.asString(varElement);
			let variableName = TValue.asString(variable);
			let context = request.moduleContext.resolveElementContext(elementName);
			response.trace(request, TAMEConstants.TraceType.VALUE, Util.format("SET {0}.{1} {2}", context.identity, variableName, TValue.toString(value)));
			TLogic.setValue(context.variables, variableName, value);
		}
	},

	/* POPLISTVALUE */
	{
		"name": 'POPLISTVALUE', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value = request.popValue();
			let index = request.popValue();
			let listValue = request.popValue();
			
			if (!TValue.isLiteral(index))
				throw TAMEError.UnexpectedValueType("Expected literal type in POPLISTVALUE call.");
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in POPLISTVALUE call.");
			if (!TValue.isLiteral(listValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in POPLISTVALUE call.");
			
			if (!TValue.isList(listValue))
				return;
			
			response.trace(request, TAMEConstants.TraceType.VALUE, Util.format("SET LIST [{0}] {1}", TValue.asLong(index), TValue.toString(value)));
			TValue.listSet(listValue, TValue.asLong(index), value);
		}
	},
	
	/* PUSHVALUE */
	{
		"name": 'PUSHVALUE', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value = operation.operand0;
			
			if (TValue.isVariable(value))
			{
				let variableName = TValue.asString(value);
				if (TLogic.containsValue(blockLocal, variableName))
					request.pushValue(TLogic.getValue(blockLocal, variableName));
				else
					request.pushValue(TLogic.getValue(request.peekContext().variables, variableName));
			}
			else
			{
				request.pushValue(value);
			}
		}
	},

	/* PUSHELEMENTVALUE */
	{
		"name": 'PUSHELEMENTVALUE', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varElement = operation.operand0;
			let variable = operation.operand1;

			if (!TValue.isVariable(variable))
				throw TAMEError.UnexpectedValueType("Expected variable type in PUSHELEMENTVALUE call.");
			if (!TValue.isElement(varElement))
				throw TAMEError.UnexpectedValueType("Expected element type in PUSHELEMENTVALUE call.");

			let elementName = TValue.asString(varElement);
			let variableName = TValue.asString(variable);
			
			request.pushValue(TLogic.getValue(request.moduleContext.resolveElementContext(elementName).variables, variableName));
		}
	},

	/* PUSHLISTVALUE */
	{
		"name": 'PUSHLISTVALUE', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let index = request.popValue();
			let listValue = request.popValue();

			if (!TValue.isLiteral(listValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in PUSHLISTVALUE call.");
			if (!TValue.isLiteral(index))
				throw TAMEError.UnexpectedValueType("Expected literal type in PUSHLISTVALUE call.");
			
			if (!TValue.isList(listValue))
				request.pushValue(TValue.createBoolean(false));
			else
				request.pushValue(TValue.listGet(listValue, TValue.asLong(index)));
		}
	},
	
	/* PUSHNEWLIST */
	{
		"name": 'PUSHNEWLIST', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			request.pushValue(TValue.createList([]));
		}
		
	},
	
	/* PUSHINITLIST */
	{
		"name": 'PUSHINITLIST', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let length = request.popValue();

			if (!TValue.isLiteral(length))
				throw TAMEError.UnexpectedValueType("Expected literal type in PUSHINITLIST call.");

			let size = TValue.asLong(length);
			let list = TValue.createList([]);
			while (size-- > 0)
			{
				let popped = request.popValue();
				if (!(TValue.isLiteral(popped) || TValue.isList(popped)))
					throw TAMEError.UnexpectedValueType("Expected literal or list type in PUSHINITLIST call.");
				TValue.listAddAt(list, 0, popped);
			}
			
			request.pushValue(list);
		}
	},
	
	/* CLEARVALUE */
	{
		"name": 'CLEARVALUE', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value = operation.operand0;

			if (!TValue.isVariable(value))
				throw TAMEError.UnexpectedValueType("Expected variable type in CLEARVALUE call.");
			
			let variableName = TValue.asString(value).toLowerCase();
			if (blockLocal[variableName])
			{
				response.trace(request, TAMEConstants.TraceType.VALUE, Util.format("CLEAR LOCAL {0}", variableName));
				TLogic.clearValue(blockLocal, variableName);
			}
			else
			{
				response.trace(request, TAMEConstants.TraceType.VALUE, Util.format("CLEAR {0}.{1}", request.peekContext().identity, variableName));
				TLogic.clearValue(request.peekContext().variables, variableName);
			}
		}
	},

	/* CLEARELEMENTVALUE */
	{
		"name": 'CLEARELEMENTVALUE', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varElement = operation.operand0;
			let variable = operation.operand1;

			if (!TValue.isVariable(variable))
				throw TAMEError.UnexpectedValueType("Expected variable type in CLEARELEMENTVALUE call.");
			if (!TValue.isElement(varElement))
				throw TAMEError.UnexpectedValueType("Expected element type in CLEARELEMENTVALUE call.");

			let variableName = TValue.asString(variable).toLowerCase();
			let context = request.moduleContext.resolveElementContext(TValue.asString(varElement));
			response.trace(request, TAMEConstants.TraceType.VALUE, Util.format("CLEAR {0}.{1}", context.identity, variableName));
			TLogic.clearValue(context.variables, TValue.asString(variable));
		}
	},

	/* PUSHTHIS */
	{
		"name": 'PUSHTHIS', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let element = request.moduleContext.resolveElement(request.peekContext().identity);
			if (element.tameType === 'TObject')
				request.pushValue(TValue.createObject(element.identity));
			else if (element.tameType === 'TRoom')
				request.pushValue(TValue.createRoom(element.identity));
			else if (element.tameType === 'TPlayer')
				request.pushValue(TValue.createPlayer(element.identity));
			else if (element.tameType === 'TContainer')
				request.pushValue(TValue.createContainer(element.identity));
			else if (element.tameType === 'TWorld')
				request.pushValue(TValue.createWorld());
			else
				throw TAMEError.ModuleExecution("Internal error - invalid object type for PUSHTHIS.");
		}
	},
	
	/* ARITHMETICFUNC */
	{
		"name": 'ARITHMETICFUNC', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let functionValue = operation.operand0;

			if (!TValue.isInteger(functionValue))
				throw TAMEError.UnexpectedValueType("Expected integer type in ARITHMETICFUNC call.");

			TLogic.doArithmeticStackFunction(request, response, TValue.asLong(functionValue));
		}
	},

	/* IF */
	{
		"name": 'IF', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let result = TLogic.callConditional('IF', request, response, blockLocal, operation);
			
			if (result)
			{
				let success = operation.successBlock;
				if (!success)
					throw TAMEError.ModuleExecution("Success block for IF does NOT EXIST!");
				TLogic.executeBlock(success, request, response, blockLocal);
			}
			else
			{
				let failure = operation.failureBlock;
				if (failure)
					TLogic.executeBlock(failure, request, response, blockLocal);
			}
		}
	},

	/* WHILE */
	{
		"name": 'WHILE', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			while (TLogic.callConditional('WHILE', request, response, blockLocal, operation))
			{
				try {
					let success = operation.successBlock;
					if (!success)
						throw TAMEError.ModuleExecution("Success block for WHILE does NOT EXIST!");
					TLogic.executeBlock(success, request, response, blockLocal);
				} catch (err) {
					if (err instanceof TAMEInterrupt)
					{
						if (err.type === TAMEInterrupt.Type.Break)
							break;
						else if (err.type === TAMEInterrupt.Type.Continue)
							continue;
						else
							throw err;
					}
					else
						throw err;
				}
			}
		}
	},

	/* FOR */
	{
		"name": 'FOR', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let init = operation.initBlock;
			if (!init)
				throw TAMEError.ModuleExecution("Init block for FOR does NOT EXIST!");
			let success = operation.successBlock;
			if (!success)
				throw TAMEError.ModuleExecution("Success block for FOR does NOT EXIST!");
			let step = operation.stepBlock;
			if (!step)
				throw TAMEError.ModuleExecution("Step block for FOR does NOT EXIST!");

			response.trace(request, TAMEConstants.TraceType.CONTROL, "FOR Init");

			for (
				TLogic.executeBlock(init, request, response, blockLocal);
				TLogic.callConditional('FOR', request, response, blockLocal, operation);
				response.trace(request, TAMEConstants.TraceType.CONTROL, "FOR Step"),
				TLogic.executeBlock(step, request, response, blockLocal)
			)
			{
				try {
					response.trace(request, TAMEConstants.TraceType.CONTROL, "FOR Success");
					TLogic.executeBlock(success, request, response, blockLocal);
				} catch (err) {
					if (err instanceof TAMEInterrupt)
					{
						if (err.type === TAMEInterrupt.Type.Break)
							break;
						else if (err.type === TAMEInterrupt.Type.Continue)
							continue;
						else
							throw err;
					}
					else
						throw err;
				}
			}
		}
	},

	/* BREAK */
	{
		"name": 'BREAK', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			response.trace(request, TAMEConstants.TraceType.CONTROL, "THROW BREAK");
			throw TAMEInterrupt.Break();
		}
	},

	/* CONTINUE */
	{
		"name": 'CONTINUE', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			response.trace(request, TAMEConstants.TraceType.CONTROL, "THROW CONTINUE");
			throw TAMEInterrupt.Continue();
		}
	},

	/* QUIT */
	{
		"name": 'QUIT', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			response.trace(request, TAMEConstants.TraceType.CONTROL, "THROW QUIT");
			response.addCue(TAMEConstants.Cue.QUIT);
			throw TAMEInterrupt.Quit();
		}
	},

	/* FINISH */
	{
		"name": 'FINISH', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			response.trace(request, TAMEConstants.TraceType.CONTROL, "THROW FINISH");
			throw TAMEInterrupt.Finish();
		}
	},

	/* END */
	{
		"name": 'END', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			response.trace(request, TAMEConstants.TraceType.CONTROL, "THROW END");
			throw TAMEInterrupt.End();
		}
	},

	/* FUNCTIONRETURN */
	{
		"name": 'FUNCTIONRETURN', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let retVal = request.popValue();
			response.trace(request, TAMEConstants.TraceType.FUNCTION, "RETURN "+TValue.toString(retVal));
			TLogic.setValue(blockLocal, TAMEConstants.RETURN_VARIABLE, retVal);
			response.trace(request, TAMEConstants.TraceType.CONTROL, "THROW END");
			throw TAMEInterrupt.End();
		}
	},

	/* CALLFUNCTION */
	{
		"name": 'CALLFUNCTION', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varFunctionName = operation.operand0;

			if (!TValue.isLiteral(varFunctionName))
				throw TAMEError.UnexpectedValueType("Expected literal type in CALLFUNCTION call.");

			request.pushValue(TLogic.callElementFunction(request, response, TValue.asString(varFunctionName), request.peekContext()));
		}
	},

	/* CALLELEMENTFUNCTION */
	{
		"name": 'CALLELEMENTFUNCTION', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varElement = operation.operand0;
			let varFunctionName = operation.operand1;

			if (!TValue.isElement(varElement))
				throw TAMEError.UnexpectedValueType("Expected element type in CALLELEMENTFUNCTION call.");
			if (!TValue.isLiteral(varFunctionName))
				throw TAMEError.UnexpectedValueType("Expected literal type in CALLELEMENTFUNCTION call.");

			let elementContext = request.moduleContext.resolveElementContext(TValue.asString(varElement));
			request.pushValue(TLogic.callElementFunction(request, response, TValue.asString(varFunctionName), elementContext));
		}
	},

	/* QUEUEACTION */
	{
		"name": 'QUEUEACTION', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varAction = request.popValue();

			if (!TValue.isAction(varAction))
				throw TAMEError.UnexpectedValueType("Expected action type in QUEUEACTION call.");

			let action = request.moduleContext.resolveAction(TValue.asString(varAction));

			if (action.type != TAMEConstants.ActionType.GENERAL)
				throw TAMEError.UnexpectedValueType("BAD TYPE: " + action.identity + " is not a general action.");

			let command = TCommand.create(action);
			request.addCommand(command);
			response.trace(request, TAMEConstants.TraceType.CONTROL, "Enqueue command "+command.toString());
		}
	},

	/* QUEUEACTIONSTRING */
	{
		"name": 'QUEUEACTIONSTRING', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varTarget = request.popValue();
			let varAction = request.popValue();

			if (!TValue.isLiteral(varTarget))
				throw TAMEError.UnexpectedValueType("Expected literal type in QUEUEACTIONSTRING call.");
			if (!TValue.isAction(varAction))
				throw TAMEError.UnexpectedValueType("Expected action type in QUEUEACTIONSTRING call.");

			let action = request.moduleContext.resolveAction(TValue.asString(varAction));

			if (action.type != TAMEConstants.ActionType.MODAL && action.type != TAMEConstants.ActionType.OPEN)
				throw TAMEInterrupt.Error(action.identity + " is not a modal nor open action.");

			let command = TCommand.createModal(action, TValue.asString(varTarget));
			request.addCommand(command);
			response.trace(request, TAMEConstants.TraceType.CONTROL, "Enqueue command "+command.toString());
		}
	},

	/* QUEUEACTIONOBJECT */
	{
		"name": 'QUEUEACTIONOBJECT', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varObject = request.popValue();
			let varAction = request.popValue();

			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected literal type in QUEUEACTIONOBJECT call.");
			if (!TValue.isAction(varAction))
				throw TAMEError.UnexpectedValueType("Expected action type in QUEUEACTIONOBJECT call.");

			let action = request.moduleContext.resolveAction(TValue.asString(varAction));
			let object = request.moduleContext.resolveElement(TValue.asString(varObject));

			if (action.type != TAMEConstants.ActionType.TRANSITIVE && action.type != TAMEConstants.ActionType.DITRANSITIVE)
				throw TAMEError.UnexpectedValueType("BAD TYPE: " + action.identity + " is not a transitive nor ditransitive action.");

			let command = TCommand.createObject(action, object);
			request.addCommand(command);
			response.trace(request, TAMEConstants.TraceType.CONTROL, "Enqueue command "+command.toString());
		}
	},

	/* QUEUEACTIONFOROBJECTSIN */
	{
		"name": 'QUEUEACTIONFOROBJECTSIN', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varObjectContainer = request.popValue();
			let varAction = request.popValue();

			if (!TValue.isObjectContainer(varObjectContainer))
				throw TAMEError.UnexpectedValueType("Expected object-container type in QUEUEACTIONFOROBJECTSIN call.");
			if (!TValue.isAction(varAction))
				throw TAMEError.UnexpectedValueType("Expected action type in QUEUEACTIONFOROBJECTSIN call.");

			let context = request.moduleContext;
			let action = context.resolveAction(TValue.asString(varAction));

			if (action.type != TAMEConstants.ActionType.TRANSITIVE && action.type != TAMEConstants.ActionType.DITRANSITIVE)
				throw TAMEError.UnexpectedValueType("BAD TYPE: " + action.identity + " is not a transitive nor ditransitive action.");

			let element = context.resolveElement(TValue.asString(varObjectContainer));
			Util.each(context.getObjectsOwnedByElement(element.identity), function(objectIdentity){
				let object = context.resolveElement(objectIdentity);
				let command = TCommand.createObject(action, object);
				request.addCommand(command);
				response.trace(request, TAMEConstants.TraceType.CONTROL, "Enqueue command "+command.toString());
			});
		}

	},
	
	/* QUEUEACTIONFORTAGGEDOBJECTSIN */
	{
		"name": 'QUEUEACTIONFORTAGGEDOBJECTSIN', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varTag = request.popValue();
			let varObjectContainer = request.popValue();
			let varAction = request.popValue();

			if (!TValue.isLiteral(varTag))
				throw TAMEError.UnexpectedValueType("Expected literal type in QUEUEACTIONFORTAGGEDOBJECTSIN call.");
			if (!TValue.isObjectContainer(varObjectContainer))
				throw TAMEError.UnexpectedValueType("Expected object-container type in QUEUEACTIONFORTAGGEDOBJECTSIN call.");
			if (!TValue.isAction(varAction))
				throw TAMEError.UnexpectedValueType("Expected action type in QUEUEACTIONFORTAGGEDOBJECTSIN call.");

			let context = request.moduleContext;
			let action = context.resolveAction(TValue.asString(varAction));

			if (action.type != TAMEConstants.ActionType.TRANSITIVE && action.type != TAMEConstants.ActionType.DITRANSITIVE)
				throw TAMEError.UnexpectedValueType("BAD TYPE: " + action.identity + " is not a transitive nor ditransitive action.");

			let tagName = TValue.asString(varTag);
			let element = context.resolveElement(TValue.asString(varObjectContainer));
			Util.each(context.getObjectsOwnedByElement(element.identity), function(objectIdentity){
				if (!context.checkObjectHasTag(objectIdentity, tagName))
					return;
				
				let object = context.resolveElement(objectIdentity);
				let command = TCommand.createObject(action, object);
				request.addCommand(command);
				response.trace(request, TAMEConstants.TraceType.CONTROL, "Enqueue command "+command.toString());
			});
		}

	},
	
	/* QUEUEACTIONOBJECT2 */
	{
		"name": 'QUEUEACTIONOBJECT2', 
		"internal": true,
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varObject2 = request.popValue();
			let varObject = request.popValue();
			let varAction = request.popValue();

			if (!TValue.isObject(varObject2))
				throw TAMEError.UnexpectedValueType("Expected literal type in QUEUEACTIONOBJECT2 call.");
			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected literal type in QUEUEACTIONOBJECT2 call.");
			if (!TValue.isAction(varAction))
				throw TAMEError.UnexpectedValueType("Expected action type in QUEUEACTIONOBJECT2 call.");

			let context = request.moduleContext;
			let action = context.resolveAction(TValue.asString(varAction));
			let object = context.resolveElement(TValue.asString(varObject));
			let object2 = context.resolveElement(TValue.asString(varObject2));

			if (action.type != TAMEConstants.ActionType.DITRANSITIVE)
				throw TAMEError.UnexpectedValueType("BAD TYPE: " + action.identity + " is not a ditransitive action.");
			
			let command = TCommand.createObject2(action, object, object2);
			request.addCommand(command);
			response.trace(request, TAMEConstants.TraceType.CONTROL, "Enqueue command "+command.toString());
		}
	},

	/* ADDCUE */
	{
		"name": 'ADDCUE', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value = request.popValue();
			let cue = request.popValue();

			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in ADDCUE call.");
			if (!TValue.isLiteral(cue))
				throw TAMEError.UnexpectedValueType("Expected literal type in ADDCUE call.");

			response.addCue(TValue.asString(cue), TValue.asString(value));
		}
	},

	/* TEXT */
	{
		"name": 'TEXT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in TEXT call.");

			response.addCue(TAMEConstants.Cue.TEXT, TValue.asString(value));
		}
	},

	/* TEXTLN */
	{
		"name": 'TEXTLN', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in TEXTLN call.");

			response.addCue(TAMEConstants.Cue.TEXT, TValue.asString(value) + '\n');
		}
	},

	/* TEXTF */
	{
		"name": 'TEXTF', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in TEXTF call.");

			response.addCue(TAMEConstants.Cue.TEXTF, TValue.asString(value));
		}
	},

	/* TEXTFLN */
	{
		"name": 'TEXTFLN', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in TEXTFLN call.");

			response.addCue(TAMEConstants.Cue.TEXTF, TValue.asString(value) + '\n');
		}
	},

	/* PAUSE */
	{
		"name": 'PAUSE', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			response.addCue(TAMEConstants.Cue.PAUSE);
		}
	},

	/* WAIT */
	{
		"name": 'WAIT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in WAIT call.");

			response.addCue(TAMEConstants.Cue.WAIT, TValue.asLong(value));
		}
	},

	/* ASBOOLEAN */
	{
		"name": 'ASBOOLEAN', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in ASBOOLEAN call.");

			request.pushValue(TValue.createBoolean(TValue.asBoolean(value)));
		}
	},

	/* ASINT */
	{
		"name": 'ASINT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in ASINT call.");

			request.pushValue(TValue.createInteger(TValue.asLong(value)));
		}
	},

	/* ASFLOAT */
	{
		"name": 'ASFLOAT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in ASFLOAT call.");

			request.pushValue(TValue.createFloat(TValue.asDouble(value)));
		}
	},

	/* ASSTRING */
	{
		"name": 'ASSTRING', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in ASSTRING call.");

			request.pushValue(TValue.createString(TValue.asString(value)));
		}
	},

	/* ASLIST */
	{
		"name": 'ASLIST', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in ASLIST call.");

			if (TValue.isList(value))
				request.pushValue(value);
			else
			{
				let out = TValue.createList([]);
				TValue.listAdd(out, value);
				request.pushValue(out);
			}
		}
	},

	/* LENGTH */
	{
		"name": 'LENGTH', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in LENGTH call.");

			request.pushValue(TValue.createInteger(TValue.length(value)));
		}
	},

	/* EMPTY */
	{
		"name": 'EMPTY', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in EMPTY call.");

			request.pushValue(TValue.createBoolean(TValue.isEmpty(value)));
		}
	},

	/* STRCONCAT */
	{
		"name": 'STRCONCAT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value2 = request.popValue();
			let value1 = request.popValue();
			
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCONCAT call.");
			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCONCAT call.");

			request.pushValue(TValue.createString(TValue.asString(value1) + TValue.asString(value2)));
		}
	},

	/* STRREPLACE */
	{
		"name": 'STRREPLACE', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value3 = request.popValue();
			let value2 = request.popValue();
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACE call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACE call.");
			if (!TValue.isLiteral(value3))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACE call.");

			let replacement = TValue.asString(value3);
			let sequence = TValue.asString(value2);
			let source = TValue.asString(value1);
			let index = source.indexOf(sequence);
			if (index >= 0)
			{
				request.pushValue(TValue.createString(
					source.substring(0, index) + 
					replacement + 
					source.substring(index + sequence.length, source.length)
				));
			}
			else
			{
				request.pushValue(TValue.createString(source));
			}
		}
	},

	/* STRREPLACELAST */
	{
		"name": 'STRREPLACELAST', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value3 = request.popValue();
			let value2 = request.popValue();
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACELAST call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACELAST call.");
			if (!TValue.isLiteral(value3))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACELAST call.");

			let replacement = TValue.asString(value3);
			let sequence = TValue.asString(value2);
			let source = TValue.asString(value1);
			let index = source.lastIndexOf(sequence);
			if (index >= 0)
			{
				request.pushValue(TValue.createString(
					source.substring(0, index) + 
					replacement + 
					source.substring(index + sequence.length, source.length)
				));
			}
			else
			{
				request.pushValue(TValue.createString(source));
			}
		}
	},

	/* STRREPLACEALL */
	{
		"name": 'STRREPLACEALL', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value3 = request.popValue();
			let value2 = request.popValue();
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACEALL call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACEALL call.");
			if (!TValue.isLiteral(value3))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACEALL call.");

			let replacement = TValue.asString(value3);
			let sequence = TValue.asString(value2);
			let source = TValue.asString(value1);

			let out = source;
			while (out.indexOf(sequence) >= 0)
				out = out.replace(sequence, replacement);
			
			request.pushValue(TValue.createString(out));
		}
	},

	/* STRINDEX */
	{
		"name": 'STRINDEX', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value2 = request.popValue();
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRINDEX call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRINDEX call.");
			
			let sequence = TValue.asString(value2);
			let str = TValue.asString(value1);

			request.pushValue(TValue.createInteger(str.indexOf(sequence)));
		}
	},

	/* STRLASTINDEX */
	{
		"name": 'STRLASTINDEX', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value2 = request.popValue();
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRLASTINDEX call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRLASTINDEX call.");
			
			let sequence = TValue.asString(value2);
			let str = TValue.asString(value1);

			request.pushValue(TValue.createInteger(str.lastIndexOf(sequence)));
		}
	},

	/* STRCONTAINS */
	{
		"name": 'STRCONTAINS', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value2 = request.popValue();
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCONTAINS call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCONTAINS call.");
			
			let sequence = TValue.asString(value2);
			let str = TValue.asString(value1);

			request.pushValue(TValue.createBoolean(str.indexOf(sequence) >= 0));
		}
	},

	/* STRSPLIT */
	{
		"name": 'STRSPLIT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value2 = request.popValue();
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRSPLIT call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRSPLIT call.");
			
			let sequence = TValue.asString(value2);
			let str = TValue.asString(value1);

			let out = TValue.createList([]);
			let index = str.indexOf(sequence);
			while (index >= 0)
			{
				TValue.listAdd(out, TValue.createString(str.substring(0, index)));
				str = str.substring(index + sequence.length, str.length);
				index = str.indexOf(sequence);
			}
			TValue.listAdd(out, TValue.createString(str));
			request.pushValue(out);
		}
	},

	/* STRJOIN */
	{
		"name": 'STRJOIN', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let joiner = request.popValue();
			let list = request.popValue();

			if (!TValue.isLiteral(joiner))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRJOIN call.");
			if (!TValue.isLiteral(list))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRJOIN call.");
			
			let str = TValue.asString(joiner);

			if (!TValue.isList(list) || TValue.length(list) == 1)
				request.pushValue(TValue.createString(TValue.asString(list)));
			else
			{
				let sb = new TStringBuilder();
				let len = TValue.length(list);
				for (let i = 0; i < len; i++)
				{
					sb.append(TValue.asString(TValue.listGet(list, i)));
					if (i < len - 1)
						sb.append(str);
				}
				request.pushValue(TValue.createString(sb.toString()));
			}
		}
	},
	
	/* STRSTARTSWITH */
	{
		"name": 'STRSTARTSWITH', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value2 = request.popValue();
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRSTARTSWITH call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRSTARTSWITH call.");
			
			let sequence = TValue.asString(value2);
			let str = TValue.asString(value1);

			request.pushValue(TValue.createBoolean(str.substring(0, sequence.length) === sequence));
		}
	},

	/* STRENDSWITH */
	{
		"name": 'STRENDSWITH', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value2 = request.popValue();
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRENDSWITH call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRENDSWITH call.");
			
			let sequence = TValue.asString(value2);
			let str = TValue.asString(value1);

			request.pushValue(TValue.createBoolean(str.substring(str.length - sequence.length) === sequence));
		}
	},

	/* SUBSTRING */
	{
		"name": 'SUBSTRING', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value3 = request.popValue();
			let value2 = request.popValue();
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in SUBSTRING call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in SUBSTRING call.");
			if (!TValue.isLiteral(value3))
				throw TAMEError.UnexpectedValueType("Expected literal type in SUBSTRING call.");

			let end = TValue.asLong(value3);
			let start = TValue.asLong(value2);
			let source = TValue.asString(value1);
			
			start = Math.min(Math.max(start, 0), source.length);
			end = Math.min(Math.max(end, 0), source.length);
			if (end <= start)
				request.pushValue(TValue.createString(""));
			else
				request.pushValue(TValue.createString(source.substring(start, end)));
		}
	},

	/* STRLOWER */
	{
		"name": 'STRLOWER', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRLOWER call.");

			request.pushValue(TValue.createString(TValue.asString(value1).toLowerCase()));
		}
	},

	/* STRUPPER */
	{
		"name": 'STRUPPER', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRUPPER call.");

			request.pushValue(TValue.createString(TValue.asString(value1).toUpperCase()));
		}
	},

	/* STRCHAR */
	{
		"name": 'STRCHAR', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value2 = request.popValue();
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCHAR call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCHAR call.");
			
			let index = TValue.asLong(value2);
			let str = TValue.asString(value1);

			if (index < 0 || index >= str.length)
				request.pushValue(TValue.createString(''));
			else
				request.pushValue(TValue.createString(str.charAt(index)));
		}
	},

	/* STRTRIM */
	{
		"name": 'STRTRIM', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRTRIM call.");

			request.pushValue(TValue.createString(TValue.asString(value1).trim()));
		}
	},

	/* STRFORMAT */
	{
		"name": 'STRFORMAT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let list = request.popValue();
			let str = request.popValue();

			if (!TValue.isLiteral(list))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRFORMAT call.");
			if (!TValue.isLiteral(str))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRFORMAT call.");

			if (!TValue.isList(list))
			{
				let nl = TValue.createList([]);
				TValue.listAdd(nl, list);
				list = nl;
			}

			let sb = '';
			let err = '';
			let chars = TValue.asString(str);
			
			const STATE_START = 0;
			const STATE_INDEX = 1;
			let DIGIT_ZERO = '0'.codePointAt(0);
			let DIGIT_NINE = '9'.codePointAt(0);
			let state = STATE_START;
			let index = 0;

			for (let i = 0; i < chars.length; i++)
			{
				let c = chars.charAt(i);
				switch (state)
				{
					case STATE_START:
					{
						if (c === '{')
						{
							state = STATE_INDEX;
							index = 0;
							err = '{';
						}
						else
							sb += c;
					}
					break;
					
					case STATE_INDEX:
					{
						if (c === '}')
						{
							state = STATE_START;
							sb += TValue.asString(TValue.listGet(list, index));
						}
						else if (c.codePointAt(0) >= DIGIT_ZERO && c.codePointAt(0) <= DIGIT_NINE)
						{
							index = (index * 10) + (c.codePointAt(0) - DIGIT_ZERO);
							err += c;
						}
						else
						{
							err += c;
							sb += err;
							state = STATE_START;
						}
					}
					break;
				}
			}
			
			request.pushValue(TValue.createString(sb));
		}
	},

	/* ISREGEX */
	{
		"name": 'ISREGEX', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let valPattern = request.popValue();
			
			if (!TValue.isLiteral(valPattern))
				throw TAMEError.UnexpectedValueType("Expected literal type in ISREGEX call.");

			try {
				new RegExp(TValue.asString(valPattern));
				request.pushValue(TValue.createBoolean(true));
			} catch (err) {
				request.pushValue(TValue.createBoolean(false));
			}
		}
	},

	/* REGEXCONTAINS */
	{
		"name": 'REGEXCONTAINS', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let valInput = request.popValue();
			let valPattern = request.popValue();
			
			if (!TValue.isLiteral(valPattern))
				throw TAMEError.UnexpectedValueType("Expected literal type in REGEXCONTAINS call.");
			if (!TValue.isLiteral(valInput))
				throw TAMEError.UnexpectedValueType("Expected literal type in REGEXCONTAINS call.");

			let pattern = null;
			try {
				pattern = new RegExp(TValue.asString(valPattern), "g");
			} catch (err) {
				throw TAMEError.BadParameter("RegEx could not be compiled:\n" + err.message);
			}
			
			let input = TValue.asString(valInput);
			let result = pattern.test(input);
			request.pushValue(TValue.createBoolean(pattern.test(input)));
		}
	},
	
	/* REGEXFIND */
	{
		"name": 'REGEXFIND', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let valInput = request.popValue();
			let valPattern = request.popValue();
			
			if (!TValue.isLiteral(valPattern))
				throw TAMEError.UnexpectedValueType("Expected literal type in REGEXFIND call.");
			if (!TValue.isLiteral(valInput))
				throw TAMEError.UnexpectedValueType("Expected literal type in REGEXFIND call.");

			let pattern = null;
			try {
				pattern = new RegExp(TValue.asString(valPattern), "g");
			} catch (err) {
				throw TAMEError.BadParameter("RegEx could not be compiled:\n" + err.message);
			}
			
			let input = TValue.asString(valInput);
			let result = pattern.exec(input);
			if (result)
				request.pushValue(TValue.createInteger(result.index));
			else
				request.pushValue(TValue.createInteger(-1));
		}
	},
	
	/* REGEXFINDLAST */
	{
		"name": 'REGEXFINDLAST', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let valInput = request.popValue();
			let valPattern = request.popValue();
			
			if (!TValue.isLiteral(valPattern))
				throw TAMEError.UnexpectedValueType("Expected literal type in REGEXFINDLAST call.");
			if (!TValue.isLiteral(valInput))
				throw TAMEError.UnexpectedValueType("Expected literal type in REGEXFINDLAST call.");

			let pattern = null;
			try {
				pattern = new RegExp(TValue.asString(valPattern), "g");
			} catch (err) {
				throw TAMEError.BadParameter("RegEx could not be compiled:\n" + err.message);
			}
			
			let input = TValue.asString(valInput);
			let result = null;
			let index = -1;
			while (result = pattern.exec(input)) // Intentional assignment.
				index = result.index;
			request.pushValue(TValue.createInteger(index));
		}
	},
	
	/* REGEXGET */
	{
		"name": 'REGEXGET', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let valInput = request.popValue();
			let valPattern = request.popValue();
			
			if (!TValue.isLiteral(valPattern))
				throw TAMEError.UnexpectedValueType("Expected literal type in REGEXGET call.");
			if (!TValue.isLiteral(valInput))
				throw TAMEError.UnexpectedValueType("Expected literal type in REGEXGET call.");

			let pattern = null;
			try {
				pattern = new RegExp(TValue.asString(valPattern), "g");
			} catch (err) {
				throw TAMEError.BadParameter("RegEx could not be compiled:\n" + err.message);
			}
			
			let input = TValue.asString(valInput);
			let result = pattern.exec(input);
			if (result)
				request.pushValue(TValue.createString(result[0]));
			else
				request.pushValue(TValue.createBoolean(false));
		}
	},
	
	/* REGEXGETLAST */
	{
		"name": 'REGEXGETLAST', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let valInput = request.popValue();
			let valPattern = request.popValue();
			
			if (!TValue.isLiteral(valPattern))
				throw TAMEError.UnexpectedValueType("Expected literal type in REGEXGETLAST call.");
			if (!TValue.isLiteral(valInput))
				throw TAMEError.UnexpectedValueType("Expected literal type in REGEXGETLAST call.");

			let pattern = null;
			try {
				pattern = new RegExp(TValue.asString(valPattern), "g");
			} catch (err) {
				throw TAMEError.BadParameter("RegEx could not be compiled:\n" + err.message);
			}
			
			let input = TValue.asString(valInput);
			let result = null;
			let found = null;
			while (result = pattern.exec(input)) // Intentional assignment.
				found = result[0];
			if (found !== null)
				request.pushValue(TValue.createString(found));
			else
				request.pushValue(TValue.createBoolean(false));
		}
	},
	
	/* REGEXGETALL */
	{
		"name": 'REGEXGETALL', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let valInput = request.popValue();
			let valPattern = request.popValue();
			
			if (!TValue.isLiteral(valPattern))
				throw TAMEError.UnexpectedValueType("Expected literal type in REGEXGETALL call.");
			if (!TValue.isLiteral(valInput))
				throw TAMEError.UnexpectedValueType("Expected literal type in REGEXGETALL call.");

			let pattern = null;
			try {
				pattern = new RegExp(TValue.asString(valPattern), "g");
			} catch (err) {
				throw TAMEError.BadParameter("RegEx could not be compiled:\n" + err.message);
			}
			
			let input = TValue.asString(valInput);
			let out = TValue.createList([]);
			while (result = pattern.exec(input)) // Intentional assignment.
				TValue.listAdd(out, TValue.createString(result[0]));
			request.pushValue(out);
		}
	},
	
	/* REGEXMATCHES */
	{
		"name": 'REGEXMATCHES', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let valInput = request.popValue();
			let valPattern = request.popValue();
			
			if (!TValue.isLiteral(valPattern))
				throw TAMEError.UnexpectedValueType("Expected literal type in REGEXMATCHES call.");
			if (!TValue.isLiteral(valInput))
				throw TAMEError.UnexpectedValueType("Expected literal type in REGEXMATCHES call.");

			let pattern = null;
			try {
				pattern = new RegExp(TValue.asString(valPattern), "g");
			} catch (err) {
				throw TAMEError.BadParameter("RegEx could not be compiled:\n" + err.message);
			}
			
			let input = TValue.asString(valInput);
			let result = pattern.exec(input);
			if (result)
				request.pushValue(TValue.createBoolean(input === result[0]));
			else
				request.pushValue(TValue.createBoolean(false));
		}
	},
		
	/* REGEXSPLIT */
	{
		"name": 'REGEXSPLIT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let valInput = request.popValue();
			let valPattern = request.popValue();
			
			if (!TValue.isLiteral(valPattern))
				throw TAMEError.UnexpectedValueType("Expected literal type in REGEXSPLIT call.");
			if (!TValue.isLiteral(valInput))
				throw TAMEError.UnexpectedValueType("Expected literal type in REGEXSPLIT call.");

			let pattern = null;
			try {
				pattern = new RegExp(TValue.asString(valPattern), "g");
			} catch (err) {
				throw TAMEError.BadParameter("RegEx could not be compiled:\n" + err.message);
			}
			
			let out = TValue.createList([]);
			let tokens = TValue.asString(valInput).split(pattern);
			for (let i = 0; i < tokens.length; i++)
				TValue.listAdd(out, TValue.createString(tokens[i]));
			request.pushValue(out);
		}
	},
		
	/* REGEXREPLACE */
	{
		"name": 'REGEXREPLACE', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let valInput = request.popValue();
			let valReplacer = request.popValue();
			let valPattern = request.popValue();
			
			if (!TValue.isLiteral(valPattern))
				throw TAMEError.UnexpectedValueType("Expected literal type in REGEXSPLIT call.");
			if (!TValue.isLiteral(valReplacer))
				throw TAMEError.UnexpectedValueType("Expected literal type in REGEXSPLIT call.");
			if (!TValue.isLiteral(valInput))
				throw TAMEError.UnexpectedValueType("Expected literal type in REGEXSPLIT call.");

			let pattern = null;
			try {
				pattern = new RegExp(TValue.asString(valPattern), "g");
			} catch (err) {
				throw TAMEError.BadParameter("RegEx could not be compiled:\n" + err.message);
			}
			
			let sb = new TStringBuilder();
			let tokens = TValue.asString(valInput).split(pattern);
			let replacer = TValue.asString(valReplacer);
			for (let i = 0; i < tokens.length; i++)
			{
				sb.append(tokens[i]);
				if (i < tokens.length - 1)
					sb.append(replacer);
			}
			request.pushValue(TValue.createString(sb.toString()));
		}
	},
		
	/* LISTNEW */
	{
		"name": 'LISTNEW', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let size = request.popValue();
			
			if (!TValue.isLiteral(size))
				throw TAMEError.UnexpectedValueType("Expected literal type in LISTNEW call.");

			let len = TValue.asLong(size);
			if (len < 0)
				len = 0;
			
			let list = TValue.createList([]);
			while (len-- > 0)
				TValue.listAdd(list, TValue.createBoolean(false));
			request.pushValue(list);
		}
	},
		
	/* LISTADD */
	{
		"name": 'LISTADD', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value = request.popValue();
			let list = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in LISTADD call.");
			if (!TValue.isLiteral(list))
				throw TAMEError.UnexpectedValueType("Expected literal type in LISTADD call.");

			request.pushValue(TValue.createBoolean(TValue.listAdd(list, value)));
		}
	},
		
	/* LISTADDAT */
	{
		"name": 'LISTADDAT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let index = request.popValue();
			let value = request.popValue();
			let list = request.popValue();
			
			if (!TValue.isLiteral(index))
				throw TAMEError.UnexpectedValueType("Expected literal type in LISTADDAT call.");
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in LISTADDAT call.");
			if (!TValue.isLiteral(list))
				throw TAMEError.UnexpectedValueType("Expected literal type in LISTADDAT call.");

			request.pushValue(TValue.createBoolean(TValue.listAddAt(list, TValue.asLong(index), value)));
		}
	},
		
	/* LISTREMOVE */
	{
		"name": 'LISTREMOVE', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value = request.popValue();
			let list = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in LISTREMOVE call.");
			if (!TValue.isLiteral(list))
				throw TAMEError.UnexpectedValueType("Expected literal type in LISTREMOVE call.");

			request.pushValue(TValue.createBoolean(TValue.listRemove(list, value)));
		}
	
	},
		
	/* LISTREMOVEAT */
	{
		"name": 'LISTREMOVEAT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let index = request.popValue();
			let list = request.popValue();
			
			if (!TValue.isLiteral(index))
				throw TAMEError.UnexpectedValueType("Expected literal type in LISTREMOVEAT call.");
			if (!TValue.isLiteral(list))
				throw TAMEError.UnexpectedValueType("Expected literal type in LISTREMOVEAT call.");

			request.pushValue(TValue.listRemoveAt(list, TValue.asLong(index)));
		}
	},
		
	/* LISTCONCAT */
	{
		"name": 'LISTCONCAT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let appendix = request.popValue();
			let list = request.popValue();
			
			if (!TValue.isLiteral(appendix))
				throw TAMEError.UnexpectedValueType("Expected literal type in LISTCONCAT call.");
			if (!TValue.isLiteral(list))
				throw TAMEError.UnexpectedValueType("Expected literal type in LISTCONCAT call.");

			if (!TValue.isList(list))
			{
				let v = list;
				list = TValue.createList([]);
				TValue.listAdd(list, v);
			}

			if (!TValue.isList(appendix))
			{
				let v = appendix;
				appendix = TValue.createList([]);
				TValue.listAdd(appendix, v);
			}
			
			let out = TValue.createList([]);
			for (let i = 0; i < TValue.length(list); i++)
				TValue.listAdd(out, TValue.listGet(list, i));
			for (let i = 0; i < TValue.length(appendix); i++)
				TValue.listAdd(out, TValue.listGet(appendix, i));
			
			request.pushValue(out);
		}
	},
		
	/* LISTINDEX */
	{
		"name": 'LISTINDEX', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value = request.popValue();
			let list = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in LISTINDEX call.");
			if (!TValue.isLiteral(list))
				throw TAMEError.UnexpectedValueType("Expected literal type in LISTINDEX call.");

			request.pushValue(TValue.createInteger(TValue.listIndexOf(list, value)));
		}
	},
		
	/* LISTCONTAINS */
	{
		"name": 'LISTCONTAINS', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value = request.popValue();
			let list = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in LISTCONTAINS call.");
			if (!TValue.isLiteral(list))
				throw TAMEError.UnexpectedValueType("Expected literal type in LISTCONTAINS call.");

			request.pushValue(TValue.createBoolean(TValue.listContains(list, value)));
		}
	},
		
	/* FLOOR */
	{
		"name": 'FLOOR', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in FLOOR call.");

			request.pushValue(TValue.createFloat(Math.floor(TValue.asDouble(value1))));
		}
	},

	/* CEILING */
	{
		"name": 'CEILING', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in CEILING call.");

			request.pushValue(TValue.createFloat(Math.ceil(TValue.asDouble(value1))));
		}
	},

	/* ROUND */
	{
		"name": 'ROUND', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in ROUND call.");

			request.pushValue(TValue.createFloat(Math.round(TValue.asDouble(value1))));
		}
	},

	/* FIX */
	{
		"name": 'FIX', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value2 = request.popValue();
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in FIX call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in FIX call.");
			
			let d = TValue.asDouble(value1);
			let f = TValue.asDouble(value2);
			let t = Math.pow(10, f);

			request.pushValue(TValue.createFloat(Math.round(d * t) / t));
		}
	},

	/* SQRT */
	{
		"name": 'SQRT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in SQRT call.");

			request.pushValue(TValue.createFloat(Math.sqrt(TValue.asDouble(value1))));
		}
	},

	/* PI */
	{
		"name": 'PI', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			request.pushValue(TValue.createFloat(Math.PI));
		}
	},

	/* E */
	{
		"name": 'E', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			request.pushValue(TValue.createFloat(Math.E));
		}
	},

	/* SIN */
	{
		"name": 'SIN', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in SIN call.");

			request.pushValue(TValue.createFloat(Math.sin(TValue.asDouble(value1))));
		}
	},

	/* COS */
	{
		"name": 'COS', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in COS call.");

			request.pushValue(TValue.createFloat(Math.cos(TValue.asDouble(value1))));
		}
	},

	/* TAN */
	{
		"name": 'TAN', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in TAN call.");

			request.pushValue(TValue.createFloat(Math.tan(TValue.asDouble(value1))));
		}
	},

	/* MIN */
	{
		"name": 'MIN', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value2 = request.popValue();
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in MIN call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in MIN call.");
			
			request.pushValue(TValue.compare(value1, value2) <= 0 ? value1 : value2);
		}
	},

	/* MAX */
	{
		"name": 'MAX', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value2 = request.popValue();
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in MAX call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in MAX call.");
			
			request.pushValue(TValue.compare(value1, value2) > 0 ? value1 : value2);
		}
	},

	/* CLAMP */
	{
		"name": 'CLAMP', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value3 = request.popValue();
			let value2 = request.popValue();
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in CLAMP call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in CLAMP call.");
			if (!TValue.isLiteral(value3))
				throw TAMEError.UnexpectedValueType("Expected literal type in CLAMP call.");

			let hi = TValue.asDouble(value3);
			let lo = TValue.asDouble(value2);
			let number = TValue.asDouble(value1);
			
			request.pushValue(TValue.createFloat(Math.min(Math.max(number, lo), hi)));
		}
	},

	/* IRANDOM */
	{
		"name": 'IRANDOM', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in IRANDOM call.");

			let value = TValue.asLong(value1);

			if (value === 0)
				request.pushValue(TValue.createInteger(0));
			else if (value < 0)
				request.pushValue(TValue.createInteger(-(Math.floor(Math.random() * Math.abs(value)))));
			else
				request.pushValue(TValue.createInteger(Math.floor(Math.random() * Math.abs(value))));
		}
	},

	/* FRANDOM */
	{
		"name": 'FRANDOM', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			request.pushValue(TValue.createFloat(Math.random()));
		}
	},

	/* GRANDOM */
	{
		"name": 'GRANDOM', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value2 = request.popValue();
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in GRANDOM call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in GRANDOM call.");
			
			// Box-Muller Approximate algorithm c/o Maxwell Collard on StackOverflow

			let stdDev = TValue.asDouble(value2);
			let mean = TValue.asDouble(value1);
			
		    let u = 1.0 - Math.random();
		    let v = 1.0 - Math.random();
		    let stdNormal = Math.sqrt(-2.0 * Math.log(u)) * Math.cos(2.0 * Math.PI * v);
		    let out = mean + stdDev * stdNormal;

		    request.pushValue(TValue.createFloat(out));
		}
	},

	/* TIME */
	{
		"name": 'TIME', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			request.pushValue(TValue.createInteger(Date.now()));
		}
	},

	/* TIMEFORMAT */
	{
		"name": 'TIMEFORMAT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let value2 = request.popValue();
			let value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in TIMEFORMAT call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in TIMEFORMAT call.");

			let date = TValue.asLong(value1);
			let format = TValue.asString(value2);

			request.pushValue(TValue.createString(Util.formatDate(date, format)));
		}
	},

	/* OBJECTHASNAME */
	{
		"name": 'OBJECTHASNAME', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let nameValue = request.popValue();
			let varObject = request.popValue();

			if (!TValue.isLiteral(nameValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in OBJECTHASNAME call.");
			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in OBJECTHASNAME call.");

			request.pushValue(TValue.createBoolean(request.moduleContext.checkObjectHasName(TValue.asString(varObject), TValue.asString(nameValue))));
		}
	},

	/* OBJECTHASTAG */
	{
		"name": 'OBJECTHASTAG', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let tagValue = request.popValue();
			let varObject = request.popValue();

			if (!TValue.isLiteral(tagValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in OBJECTHASTAG call.");
			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in OBJECTHASTAG call.");

			request.pushValue(TValue.createBoolean(request.moduleContext.checkObjectHasTag(TValue.asString(varObject), TValue.asString(tagValue))));
		}
	},

	/* ADDOBJECTNAME */
	{
		"name": 'ADDOBJECTNAME', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let nameValue = request.popValue();
			let varObject = request.popValue();

			if (!TValue.isLiteral(nameValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in ADDOBJECTNAME call.");
			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in ADDOBJECTNAME call.");

			request.moduleContext.addObjectName(TValue.asString(varObject), TValue.asString(nameValue));
		}
	},

	/* ADDOBJECTTAG */
	{
		"name": 'ADDOBJECTTAG', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let tagValue = request.popValue();
			let varObject = request.popValue();

			if (!TValue.isLiteral(tagValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in ADDOBJECTTAG call.");
			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in ADDOBJECTTAG call.");

			request.moduleContext.addObjectTag(TValue.asString(varObject), TValue.asString(tagValue));
		}
	},

	/* ADDOBJECTTAGTOALLIN */
	{
		"name": 'ADDOBJECTTAGTOALLIN', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let tagValue = request.popValue();
			let elementValue = request.popValue();

			if (!TValue.isLiteral(tagValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in ADDOBJECTTAGTOALLIN call.");
			if (!TValue.isObjectContainer(elementValue))
				throw TAMEError.UnexpectedValueType("Expected object-container type in ADDOBJECTTAGTOALLIN call.");

			let context = request.moduleContext;
			let element = context.resolveElement(TValue.asString(elementValue));
			
			let tag = TValue.asString(tagValue);
			Util.each(context.getObjectsOwnedByElement(element.identity), function(objectIdentity){
				context.addObjectTag(objectIdentity, tag);
			});
		}
	},

	/* REMOVEOBJECTNAME */
	{
		"name": 'REMOVEOBJECTNAME', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let nameValue = request.popValue();
			let varObject = request.popValue();

			if (!TValue.isLiteral(nameValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in REMOVEOBJECTNAME call.");
			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in REMOVEOBJECTNAME call.");

			request.moduleContext.removeObjectName(TValue.asString(varObject), TValue.asString(nameValue));
		}
	},

	/* REMOVEOBJECTTAG */
	{
		"name": 'REMOVEOBJECTTAG', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let tagValue = request.popValue();
			let varObject = request.popValue();

			if (!TValue.isLiteral(tagValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in REMOVEOBJECTTAG call.");
			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in REMOVEOBJECTTAG call.");

			request.moduleContext.removeObjectTag(TValue.asString(varObject), TValue.asString(tagValue));
		}
	},

	/* REMOVEOBJECTTAGFROMALLIN */
	{
		"name": 'REMOVEOBJECTTAGFROMALLIN', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let tagValue = request.popValue();
			let elementValue = request.popValue();

			if (!TValue.isLiteral(tagValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in REMOVEOBJECTTAGFROMALLIN call.");
			if (!TValue.isObjectContainer(elementValue))
				throw TAMEError.UnexpectedValueType("Expected object-container type in REMOVEOBJECTTAGFROMALLIN call.");

			let context = request.moduleContext;
			let element = context.resolveElement(TValue.asString(elementValue));
			
			let tag = TValue.asString(tagValue);
			Util.each(context.getObjectsOwnedByElement(element.identity), function(objectIdentity){
				context.removeObjectTag(objectIdentity, tag);
			});
		}
	},

	/* GIVEOBJECT */
	{
		"name": 'GIVEOBJECT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varObject = request.popValue();
			let varObjectContainer = request.popValue();

			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in GIVEOBJECT call.");
			if (!TValue.isObjectContainer(varObjectContainer))
				throw TAMEError.UnexpectedValueType("Expected object-container type in GIVEOBJECT call.");

			let element = request.moduleContext.resolveElement(TValue.asString(varObjectContainer));

			request.moduleContext.addObjectToElement(element.identity, TValue.asString(varObject));
		}
	},

	/* REMOVEOBJECT */
	{
		"name": 'REMOVEOBJECT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varObject = request.popValue();

			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in REMOVEOBJECT call.");

			request.moduleContext.removeObject(TValue.asString(varObject));
		}
	},

	/* MOVEOBJECTSWITHTAG */
	{
		"name": 'MOVEOBJECTSWITHTAG', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let tagValue = request.popValue();
			let varObjectContainerDest = request.popValue();
			let varObjectContainerSource = request.popValue();

			if (!TValue.isLiteral(tagValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in MOVEOBJECTSWITHTAG call.");
			if (!TValue.isObjectContainer(varObjectContainerDest))
				throw TAMEError.UnexpectedValueType("Expected object-container type in MOVEOBJECTSWITHTAG call.");
			if (!TValue.isObjectContainer(varObjectContainerSource))
				throw TAMEError.UnexpectedValueType("Expected object-container type in MOVEOBJECTSWITHTAG call.");

			let context = request.moduleContext;
			let destination = context.resolveElement(TValue.asString(varObjectContainerDest));
			let source = context.resolveElement(TValue.asString(varObjectContainerSource));
			let tag = TValue.asString(tagValue);
			
			Util.each(context.getObjectsOwnedByElement(source.identity), function(objectIdentity){
				if (context.checkObjectHasTag(objectIdentity, tag))
					context.addObjectToElement(destination.identity, objectIdentity);
			});
		}
	},

	/* OBJECTCOUNT */
	{
		"name": 'OBJECTCOUNT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let elementValue = request.popValue();

			if (!TValue.isObjectContainer(elementValue))
				throw TAMEError.UnexpectedValueType("Expected object-container type in OBJECTCOUNT call.");

			let element = request.moduleContext.resolveElement(TValue.asString(elementValue));

			request.pushValue(TValue.createInteger(request.moduleContext.getObjectsOwnedByElementCount(element.identity)));
		}
	},

	/* HASOBJECT */
	{
		"name": 'HASOBJECT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varObject = request.popValue();
			let varObjectContainer = request.popValue();

			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in HASOBJECT call.");
			if (!TValue.isObjectContainer(varObjectContainer))
				throw TAMEError.UnexpectedValueType("Expected object-container type in HASOBJECT call.");

			let element = request.moduleContext.resolveElement(TValue.asString(varObjectContainer));

			request.pushValue(TValue.createBoolean(request.moduleContext.checkElementHasObject(element.identity, TValue.asString(varObject))));
		}
	},

	/* OBJECTHASNOOWNER */
	{
		"name": 'OBJECTHASNOOWNER', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varObject = request.popValue();

			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in OBJECTHASNOOWNER call.");

			request.pushValue(TValue.createBoolean(request.moduleContext.checkObjectHasNoOwner(TValue.asString(varObject))));
		}
	},

	/* PLAYERISINROOM */
	{
		"name": 'PLAYERISINROOM', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varRoom = request.popValue();
			let varPlayer = request.popValue();
			
			if (!TValue.isRoom(varRoom))
				throw TAMEError.UnexpectedValueType("Expected room type in PLAYERISINROOM call.");
			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in PLAYERISINROOM call.");

			let context = request.moduleContext;
			let room = context.resolveElement(TValue.asString(varRoom));
			let player = context.resolveElement(TValue.asString(varPlayer));

			request.pushValue(TValue.createBoolean(context.checkPlayerIsInRoom(player.identity, room.identity)));
		}
	},

	/* PLAYERCANACCESSOBJECT */
	{
		"name": 'PLAYERCANACCESSOBJECT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varObject = request.popValue();
			let varPlayer = request.popValue();

			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in PLAYERCANACCESSOBJECT call.");
			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in PLAYERCANACCESSOBJECT call.");

			let player = request.moduleContext.resolveElement(TValue.asString(varPlayer));

			request.pushValue(TValue.createBoolean(TLogic.checkObjectAccessibility(request, response, player.identity, TValue.asString(varObject))));
		}
	},

	/* BROWSE */
	{
		"name": 'BROWSE', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varObjectContainer = request.popValue();

			if (!TValue.isObjectContainer(varObjectContainer))
				throw TAMEError.UnexpectedValueType("Expected object-container type in BROWSE call.");

			let element = request.moduleContext.resolveElement(TValue.asString(varObjectContainer));

			TLogic.doBrowse(request, response, element.identity);
		}
	},

	/* BROWSETAGGED */
	{
		"name": 'BROWSETAGGED', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varTag = request.popValue();
			let varObjectContainer = request.popValue();

			if (!TValue.isLiteral(varTag))
				throw TAMEError.UnexpectedValueType("Expected literal type in BROWSETAGGED call.");
			if (!TValue.isObjectContainer(varObjectContainer))
				throw TAMEError.UnexpectedValueType("Expected object-container type in BROWSETAGGED call.");

			let tagName = TValue.asString(varTag);
			let element = request.moduleContext.resolveElement(TValue.asString(varObjectContainer));

			TLogic.doBrowse(request, response, element.identity, tagName);
		}
	},

	/* ELEMENTHASANCESTOR */
	{
		"name": 'ELEMENTHASANCESTOR', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varParent = request.popValue();
			let varElement = request.popValue();

			if (!TValue.isElement(varElement))
				throw TAMEError.UnexpectedValueType("Expected element type in ELEMENTHASANCESTOR call.");
			if (!TValue.isElement(varParent))
				throw TAMEError.UnexpectedValueType("Expected element type in ELEMENTHASANCESTOR call.");

			let context = request.moduleContext;
			let parentIdentity = context.resolveElement(TValue.asString(varParent)).identity;
			let element = context.resolveElement(TValue.asString(varElement));

			let found = false;
			while (element)
			{
				if (element.identity == parentIdentity)
				{
					found = true;
					break;
				}
				
				element = element.parent ? context.resolveElement(element.parent) : null;
			}

			request.pushValue(TValue.createBoolean(found));
		}
	},

	/* SETPLAYER */
	{
		"name": 'SETPLAYER', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varPlayer = request.popValue();

			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in SETPLAYER call.");

			let player = request.moduleContext.resolveElement(TValue.asString(varPlayer));

			TLogic.doPlayerSwitch(request, response, player.identity);
		}
	},

	/* SETROOM */
	{
		"name": 'SETROOM', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varRoom = request.popValue();
			let varPlayer = request.popValue();

			if (!TValue.isRoom(varRoom))
				throw TAMEError.UnexpectedValueType("Expected room type in SETROOM call.");
			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in SETROOM call.");

			let context = request.moduleContext;
			let room = context.resolveElement(TValue.asString(varRoom));
			let player = context.resolveElement(TValue.asString(varPlayer));
			
			TLogic.doRoomSwitch(request, response, player.identity, room.identity);
		}
	},

	/* PUSHROOM */
	{
		"name": 'PUSHROOM', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varRoom = request.popValue();
			let varPlayer = request.popValue();

			if (!TValue.isRoom(varRoom))
				throw TAMEError.UnexpectedValueType("Expected room type in PUSHROOM call.");
			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in PUSHROOM call.");

			let context = request.moduleContext;
			let room = context.resolveElement(TValue.asString(varRoom));
			let player = context.resolveElement(TValue.asString(varPlayer));

			TLogic.doRoomPush(request, response, player.identity, room.identity);
		}
	},

	/* POPROOM */
	{
		"name": 'POPROOM', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varPlayer = request.popValue();

			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in POPROOM call.");

			let context = request.moduleContext;
			let player = context.resolveElement(TValue.asString(varPlayer));

			let currentRoom = context.getCurrentRoom(player.identity);
			
			if (currentRoom === null)
				throw TAMEInterrupt.Error("No rooms for player" + TLogic.elementToString(player));
			
			TLogic.doRoomPop(request, response, player.identity);
		}
	},

	/* SWAPROOM */
	{
		"name": 'SWAPROOM', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varRoom = request.popValue();
			let varPlayer = request.popValue();

			if (!TValue.isRoom(varRoom))
				throw TAMEError.UnexpectedValueType("Expected room type in SWAPROOM call.");
			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in SWAPROOM call.");

			let context = request.moduleContext;
			let player = context.resolveElement(TValue.asString(varPlayer));

			if (player === null)
				throw TAMEInterrupt.Error("No current player!");

			let nextRoom = context.resolveElement(TValue.asString(varRoom)); 
			let currentRoom = context.getCurrentRoom(player.identity);

			if (currentRoom === null)
				throw new ErrorInterrupt("No rooms for current player!");
			
			TLogic.doRoomPop(request, response, player.identity);
			TLogic.doRoomPush(request, response, player.identity, nextRoom.identity);
		}
	},

	/* CURRENTPLAYERIS */
	{
		"name": 'CURRENTPLAYERIS', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varPlayer = request.popValue();

			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in CURRENTPLAYERIS call.");

			let context = request.moduleContext;
			let player = context.resolveElement(TValue.asString(varPlayer));
			let currentPlayer = context.getCurrentPlayer();
			
			request.pushValue(TValue.createBoolean(currentPlayer !== null && player.identity === currentPlayer.identity));
		}
	},

	/* NOCURRENTPLAYER */
	{
		"name": 'NOCURRENTPLAYER', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let currentPlayer = request.moduleContext.getCurrentPlayer();
			request.pushValue(TValue.createBoolean(currentPlayer === null));
		}
	},

	/* CURRENTROOMIS */
	{
		"name": 'CURRENTROOMIS', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varRoom = request.popValue();
			let varPlayer = request.popValue();

			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in CURRENTROOMIS call.");
			if (!TValue.isRoom(varRoom))
				throw TAMEError.UnexpectedValueType("Expected room type in CURRENTROOMIS call.");

			let context = request.moduleContext;
			let playerIdentity = TValue.asString(varPlayer);
			let player = context.resolveElement(playerIdentity);
			let room = context.resolveElement(TValue.asString(varRoom));
			
			let currentRoom = context.getCurrentRoom(player.identity);
			request.pushValue(TValue.createBoolean(currentRoom !== null && room.identity === currentRoom.identity));
		}
	},

	/* NOCURRENTROOM */
	{
		"name": 'NOCURRENTROOM', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let varPlayer = request.popValue();

			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in NOCURRENTROOM call.");
			
			let context = request.moduleContext;
			let playerIdentity = TValue.asString(varPlayer);
			let player = context.resolveElement(playerIdentity);
			let currentRoom = context.getCurrentRoom(player.identity);
			request.pushValue(TValue.createBoolean(currentRoom === null));
		}
	},

	/* IDENTITY */
	{
		"name": 'IDENTITY', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let elementValue = request.popValue();
			
			if (!TValue.isElement(elementValue))
				throw TAMEError.UnexpectedValueType("Expected element type in IDENTITY call.");
			
			let element = request.moduleContext.resolveElement(TValue.asString(elementValue));
			request.pushValue(TValue.createString(element.identity));
		}
	},

	/* HEADER */
	{
		"name": 'HEADER', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			let headerName = request.popValue();
			
			if (!TValue.isLiteral(headerName))
				throw TAMEError.UnexpectedValueType("Expected literal type in HEADER call.");
			
			let val = request.moduleContext.module.header[TValue.asString(headerName)];
			if ((typeof val) === 'undefined' || val === null)
				request.pushValue(TValue.createBoolean(false));
			else
				request.pushValue(TValue.createString(val));
		}
	},

];

//[[EXPORTJS-END

// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TOperationFunctions;
// =========================================================================
