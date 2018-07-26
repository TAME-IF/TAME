/*******************************************************************************
 * Copyright (c) 2016-2018 Matt Tropiano
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

//##[[EXPORTJS-START

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
		"doOperation": function(request, response, blockLocal, operation)
		{
			request.popValue();
		}
	},

	/* POPVALUE */
	{
		"name": 'POPVALUE', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varvalue = operation.operand0;
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in POPVALUE call.");
			if (!TValue.isVariable(varvalue))
				throw TAMEError.UnexpectedValueType("Expected variable type in POPVALUE call.");

			var variableName = TValue.asString(varvalue);
			
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
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varvalue = operation.operand0;
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in POPLOCALVALUE call.");
			if (!TValue.isVariable(varvalue))
				throw TAMEError.UnexpectedValueType("Expected variable type in POPLOCALVALUE call.");

			var variableName = TValue.asString(varvalue);
			response.trace(request, TAMEConstants.TraceType.VALUE, Util.format("SET LOCAL {0} {1}", variableName, TValue.toString(value)));
			TLogic.setValue(blockLocal, variableName, value);
		}
	},

	/* POPELEMENTVALUE */
	{
		"name": 'POPELEMENTVALUE', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varElement = operation.operand0;
			var variable = operation.operand1;
			var value = request.popValue();

			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in POPELEMENTVALUE call.");
			if (!TValue.isVariable(variable))
				throw TAMEError.UnexpectedValueType("Expected variable type in POPELEMENTVALUE call.");
			if (!TValue.isElement(varElement))
				throw TAMEError.UnexpectedValueType("Expected element type in POPELEMENTVALUE call.");

			var elementName = TValue.asString(varElement);
			var variableName = TValue.asString(variable);
			var context = request.moduleContext.resolveElementContext(elementName);
			response.trace(request, TAMEConstants.TraceType.VALUE, Util.format("SET {0}.{1} {2}", context.identity, variableName, TValue.toString(value)));
			TLogic.setValue(context.variables, variableName, value);
		}
	},

	/* POPLISTVALUE */
	{
		"name": 'POPLISTVALUE', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value = request.popValue();
			var index = request.popValue();
			var listValue = request.popValue();
			
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
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value = operation.operand0;
			
			if (TValue.isVariable(value))
			{
				var variableName = TValue.asString(value);
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
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varElement = operation.operand0;
			var variable = operation.operand1;

			if (!TValue.isVariable(variable))
				throw TAMEError.UnexpectedValueType("Expected variable type in PUSHELEMENTVALUE call.");
			if (!TValue.isElement(varElement))
				throw TAMEError.UnexpectedValueType("Expected element type in PUSHELEMENTVALUE call.");

			var elementName = TValue.asString(varElement);

			request.pushValue(TLogic.getValue(request.moduleContext.resolveElementContext(elementName).variables, TValue.asString(variable)));
		}
	},

	/* PUSHLISTVALUE */
	{
		"name": 'PUSHLISTVALUE', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var index = request.popValue();
			var listValue = request.popValue();

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
		"doOperation": function(request, response, blockLocal, operation)
		{
			request.pushValue(TValue.createList([]));
		}
		
	},
	
	/* PUSHINITLIST */
	{
		"name": 'PUSHINITLIST', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var length = request.popValue();

			if (!TValue.isLiteral(length))
				throw TAMEError.UnexpectedValueType("Expected literal type in PUSHINITLIST call.");

			var size = TValue.asLong(length);
			var list = TValue.createList([]);
			while (size-- > 0)
			{
				var popped = request.popValue();
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
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value = operation.operand0;

			if (!TValue.isVariable(value))
				throw TAMEError.UnexpectedValueType("Expected variable type in CLEARVALUE call.");
			
			var variableName = TValue.asString(value).toLowerCase();
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
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varElement = operation.operand0;
			var variable = operation.operand1;

			if (!TValue.isVariable(variable))
				throw TAMEError.UnexpectedValueType("Expected variable type in CLEARELEMENTVALUE call.");
			if (!TValue.isElement(varElement))
				throw TAMEError.UnexpectedValueType("Expected element type in CLEARELEMENTVALUE call.");

			var variableName = TValue.asString(variable).toLowerCase();
			var context = request.moduleContext.resolveElementContext(TValue.asString(varElement));
			response.trace(request, TAMEConstants.TraceType.VALUE, Util.format("CLEAR {0}.{1}", context.identity, variableName));
			TLogic.clearValue(context.variables, TValue.asString(variable))
		}
	},

	/* PUSHTHIS */
	{
		"name": 'PUSHTHIS', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var element = request.moduleContext.resolveElement(request.peekContext().identity);
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
		"doOperation": function(request, response, blockLocal, operation)
		{
			var functionValue = operation.operand0;

			if (!TValue.isInteger(functionValue))
				throw TAMEError.UnexpectedValueType("Expected integer type in ARITHMETICFUNC call.");

			TLogic.doArithmeticStackFunction(request, response, TValue.asLong(functionValue));
		}
	},

	/* IF */
	{
		"name": 'IF', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var result = TLogic.callConditional('IF', request, response, blockLocal, operation);
			
			if (result)
			{
				var success = operation.successBlock;
				if (!success)
					throw TAMEError.ModuleExecution("Success block for IF does NOT EXIST!");
				TLogic.executeBlock(success, request, response, blockLocal);
			}
			else
			{
				var failure = operation.failureBlock;
				if (failure)
					TLogic.executeBlock(failure, request, response, blockLocal);
			}
		}
	},

	/* WHILE */
	{
		"name": 'WHILE', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			while (TLogic.callConditional('WHILE', request, response, blockLocal, operation))
			{
				try {
					var success = operation.successBlock;
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
		"doOperation": function(request, response, blockLocal, operation)
		{
			var init = operation.initBlock;
			if (!init)
				throw TAMEError.ModuleExecution("Init block for FOR does NOT EXIST!");
			var success = operation.successBlock;
			if (!success)
				throw TAMEError.ModuleExecution("Success block for FOR does NOT EXIST!");
			var step = operation.stepBlock;
			if (!step)
				throw TAMEError.ModuleExecution("Step block for FOR does NOT EXIST!");

			response.trace(request, TAMEConstants.TraceType.CONTROL, "FOR Init");

			for (
				TLogic.executeBlock(init, request, response, blockLocal);
				TLogic.callConditional('FOR', request, response, blockLocal, operation);
				response.trace(request, TAMEConstants.TraceType.CONTROL, "FOR Step");
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
		"doOperation": function(request, response, blockLocal, operation)
		{
			response.trace(request, TAMEConstants.TraceType.CONTROL, "THROW BREAK");
			throw TAMEInterrupt.Break();
		}
	},

	/* CONTINUE */
	{
		"name": 'CONTINUE', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			response.trace(request, TAMEConstants.TraceType.CONTROL, "THROW CONTINUE");
			throw TAMEInterrupt.Continue();
		}
	},

	/* QUIT */
	{
		"name": 'QUIT', 
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
		"doOperation": function(request, response, blockLocal, operation)
		{
			response.trace(request, TAMEConstants.TraceType.CONTROL, "THROW FINISH");
			throw TAMEInterrupt.Finish();
		}
	},

	/* END */
	{
		"name": 'END', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			response.trace(request, TAMEConstants.TraceType.CONTROL, "THROW END");
			throw TAMEInterrupt.End();
		}
	},

	/* FUNCTIONRETURN */
	{
		"name": 'FUNCTIONRETURN', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var retVal = request.popValue();
			response.trace(request, TAMEConstants.TraceType.FUNCTION, "RETURN "+TValue.toString(retVal));
			TLogic.setValue(blockLocal, TAMEConstants.RETURN_VARIABLE, retVal);
			response.trace(request, TAMEConstants.TraceType.CONTROL, "THROW END");
			throw TAMEInterrupt.End();
		}
	},

	/* CALLFUNCTION */
	{
		"name": 'CALLFUNCTION', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varFunctionName = operation.operand0;

			if (!TValue.isLiteral(varFunctionName))
				throw TAMEError.UnexpectedValueType("Expected literal type in CALLFUNCTION call.");

			request.pushValue(TLogic.callElementFunction(request, response, TValue.asString(varFunctionName), request.peekContext()));
		}
	},

	/* CALLELEMENTFUNCTION */
	{
		"name": 'CALLELEMENTFUNCTION', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varElement = operation.operand0;
			var varFunctionName = operation.operand1;

			if (!TValue.isElement(varElement))
				throw TAMEError.UnexpectedValueType("Expected element type in CALLELEMENTFUNCTION call.");
			if (!TValue.isLiteral(varFunctionName))
				throw TAMEError.UnexpectedValueType("Expected literal type in CALLELEMENTFUNCTION call.");

			var elementContext = request.moduleContext.resolveElementContext(TValue.asString(varElement));
			request.pushValue(TLogic.callElementFunction(request, response, TValue.asString(varFunctionName), elementContext));
		}
	},

	/* QUEUEACTION */
	{
		"name": 'QUEUEACTION', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varAction = request.popValue();

			if (!TValue.isAction(varAction))
				throw TAMEError.UnexpectedValueType("Expected action type in QUEUEACTION call.");

			var action = request.moduleContext.resolveAction(TValue.asString(varAction));

			if (action.type != TAMEConstants.ActionType.GENERAL)
				throw TAMEError.UnexpectedValueType("BAD TYPE: " + action.identity + " is not a general action.");

			var command = TCommand.create(action);
			request.addCommand(command);
			response.trace(request, TAMEConstants.TraceType.CONTROL, "Enqueued command "+command.toString());
		}
	},

	/* QUEUEACTIONSTRING */
	{
		"name": 'QUEUEACTIONSTRING', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varTarget = request.popValue();
			var varAction = request.popValue();

			if (!TValue.isLiteral(varTarget))
				throw TAMEError.UnexpectedValueType("Expected literal type in QUEUEACTIONSTRING call.");
			if (!TValue.isAction(varAction))
				throw TAMEError.UnexpectedValueType("Expected action type in QUEUEACTIONSTRING call.");

			var action = request.moduleContext.resolveAction(TValue.asString(varAction));

			if (action.type != TAMEConstants.ActionType.MODAL && action.type != TAMEConstants.ActionType.OPEN)
				throw TAMEInterrupt.Error(action.identity + " is not a modal nor open action.");

			var command = TCommand.createModal(action, TValue.asString(varTarget));
			request.addCommand(command);
			response.trace(request, TAMEConstants.TraceType.CONTROL, "Enqueued command "+command.toString());
		}
	},

	/* QUEUEACTIONOBJECT */
	{
		"name": 'QUEUEACTIONOBJECT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varObject = request.popValue();
			var varAction = request.popValue();

			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected literal type in QUEUEACTIONOBJECT call.");
			if (!TValue.isAction(varAction))
				throw TAMEError.UnexpectedValueType("Expected action type in QUEUEACTIONOBJECT call.");

			var action = request.moduleContext.resolveAction(TValue.asString(varAction));
			var object = request.moduleContext.resolveElement(TValue.asString(varObject));

			if (action.type != TAMEConstants.ActionType.TRANSITIVE && action.type != TAMEConstants.ActionType.DITRANSITIVE)
				throw TAMEError.UnexpectedValueType("BAD TYPE: " + action.identity + " is not a transitive nor ditransitive action.");

			var command = TCommand.createObject(action, object);
			request.addCommand(command);
			response.trace(request, TAMEConstants.TraceType.CONTROL, "Enqueued command "+command.toString());
		}
	},

	/* QUEUEACTIONFOROBJECTSIN */
	{
		"name": 'QUEUEACTIONFOROBJECTSIN', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varObjectContainer = request.popValue();
			var varAction = request.popValue();

			if (!TValue.isObjectContainer(varObjectContainer))
				throw TAMEError.UnexpectedValueType("Expected object-container type in QUEUEACTIONFOROBJECTSIN call.");
			if (!TValue.isAction(varAction))
				throw TAMEError.UnexpectedValueType("Expected action type in QUEUEACTIONFOROBJECTSIN call.");

			var context = request.moduleContext;
			var action = context.resolveAction(TValue.asString(varAction));

			if (action.type != TAMEConstants.ActionType.TRANSITIVE && action.type != TAMEConstants.ActionType.DITRANSITIVE)
				throw TAMEError.UnexpectedValueType("BAD TYPE: " + action.identity + " is not a transitive nor ditransitive action.");

			var element = context.resolveElement(TValue.asString(varObjectContainer));
			Util.each(context.getObjectsOwnedByElement(element.identity), function(objectIdentity){
				var object = context.resolveElement(objectIdentity);
				var command = TCommand.createObject(action, object);
				request.addCommand(command);
				response.trace(request, TAMEConstants.TraceType.CONTROL, "Enqueued command "+command.toString());
			});
		}

	},
	
	/* QUEUEACTIONFORTAGGEDOBJECTSIN */
	{
		"name": 'QUEUEACTIONFORTAGGEDOBJECTSIN', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varTag = request.popValue();
			var varObjectContainer = request.popValue();
			var varAction = request.popValue();

			if (!TValue.isLiteral(varTag))
				throw TAMEError.UnexpectedValueType("Expected literal type in QUEUEACTIONFORTAGGEDOBJECTSIN call.");
			if (!TValue.isObjectContainer(varObjectContainer))
				throw TAMEError.UnexpectedValueType("Expected object-container type in QUEUEACTIONFORTAGGEDOBJECTSIN call.");
			if (!TValue.isAction(varAction))
				throw TAMEError.UnexpectedValueType("Expected action type in QUEUEACTIONFORTAGGEDOBJECTSIN call.");

			var context = request.moduleContext;
			var action = context.resolveAction(TValue.asString(varAction));

			if (action.type != TAMEConstants.ActionType.TRANSITIVE && action.type != TAMEConstants.ActionType.DITRANSITIVE)
				throw TAMEError.UnexpectedValueType("BAD TYPE: " + action.identity + " is not a transitive nor ditransitive action.");

			var tagName = TValue.asString(varTag);
			var element = context.resolveElement(TValue.asString(varObjectContainer));
			Util.each(context.getObjectsOwnedByElement(element.identity), function(objectIdentity){
				if (!context.checkObjectHasTag(objectIdentity, tagName))
					return;
				
				var object = context.resolveElement(objectIdentity);
				var command = TCommand.createObject(action, object);
				request.addCommand(command);
				response.trace(request, TAMEConstants.TraceType.CONTROL, "Enqueued command "+command.toString());
			});
		}

	},
	
	/* QUEUEACTIONOBJECT2 */
	{
		"name": 'QUEUEACTIONOBJECT2', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varObject2 = request.popValue();
			var varObject = request.popValue();
			var varAction = request.popValue();

			if (!TValue.isObject(varObject2))
				throw TAMEError.UnexpectedValueType("Expected literal type in QUEUEACTIONOBJECT2 call.");
			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected literal type in QUEUEACTIONOBJECT2 call.");
			if (!TValue.isAction(varAction))
				throw TAMEError.UnexpectedValueType("Expected action type in QUEUEACTIONOBJECT2 call.");

			var context = request.moduleContext;
			var action = context.resolveAction(TValue.asString(varAction));
			var object = context.resolveElement(TValue.asString(varObject));
			var object2 = context.resolveElement(TValue.asString(varObject2));

			if (action.type != TAMEConstants.ActionType.DITRANSITIVE)
				throw TAMEError.UnexpectedValueType("BAD TYPE: " + action.identity + " is not a ditransitive action.");
			
			var command = TCommand.createObject2(action, object, object2);
			request.addCommand(command);
			response.trace(request, TAMEConstants.TraceType.CONTROL, "Enqueued command "+command.toString());
		}
	},

	/* ADDCUE */
	{
		"name": 'ADDCUE', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value = request.popValue();
			var cue = request.popValue();

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
			var value = request.popValue();
			
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
			var value = request.popValue();
			
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
			var value = request.popValue();
			
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
			var value = request.popValue();
			
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
			var value = request.popValue();
			
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
			var value = request.popValue();
			
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
			var value = request.popValue();
			
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
			var value = request.popValue();
			
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
			var value = request.popValue();
			
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
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in ASLIST call.");
			
			var out = TValue.createList([]);
			TValue.listAdd(out, value);
			request.pushValue(out);
		}
	},

	/* LENGTH */
	{
		"name": 'LENGTH', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value = request.popValue();
			
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
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in EMPTY call.");

			request.pushValue(TValue.createInteger(TValue.isEmpty(value)));
		}
	},

	/* STRCONCAT */
	{
		"name": 'STRCONCAT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();
			
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
			var value3 = request.popValue();
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACE call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACE call.");
			if (!TValue.isLiteral(value3))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACE call.");

			var replacement = TValue.asString(value3);
			var pattern = TValue.asString(value2);
			var source = TValue.asString(value1);

			request.pushValue(TValue.createString(source.replace(pattern, replacement)));
		}
	},

	/* STRREPLACEPATTERN */
	{
		"name": 'STRREPLACEPATTERN', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value3 = request.popValue();
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACEPATTERN call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACEPATTERN call.");
			if (!TValue.isLiteral(value3))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACEPATTERN call.");

			var replacement = TValue.asString(value3);
			var pattern = TValue.asString(value2);
			var source = TValue.asString(value1);
			
			try {
				request.pushValue(TValue.createString(source.replace(new RegExp(pattern, 'm'), replacement)));
			} catch (err) {
				throw TAMEError.UnexpectedValueType("Expected valid RegEx in STRREPLACEPATTERN call.");
			}
		}
	},

	/* STRREPLACEPATTERNALL */
	{
		"name": 'STRREPLACEPATTERNALL', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value3 = request.popValue();
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACEPATTERNALL call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACEPATTERNALL call.");
			if (!TValue.isLiteral(value3))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRREPLACEPATTERNALL call.");

			var replacement = TValue.asString(value3);
			var pattern = TValue.asString(value2);
			var source = TValue.asString(value1);
			
			try {
				request.pushValue(TValue.createString(source.replace(new RegExp(pattern, 'gm'), replacement)));
			} catch (err) {
				throw TAMEError.UnexpectedValueType("Expected valid RegEx in STRREPLACEPATTERNALL call.");
			}
		}
	},

	/* STRINDEX */
	{
		"name": 'STRINDEX', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRINDEX call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRINDEX call.");
			
			var sequence = TValue.asString(value2);
			var str = TValue.asString(value1);

			request.pushValue(TValue.createInteger(str.indexOf(sequence)));
		}
	},

	/* STRLASTINDEX */
	{
		"name": 'STRLASTINDEX', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRLASTINDEX call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRLASTINDEX call.");
			
			var sequence = TValue.asString(value2);
			var str = TValue.asString(value1);

			request.pushValue(TValue.createInteger(str.lastIndexOf(sequence)));
		}
	},

	/* STRCONTAINS */
	{
		"name": 'STRCONTAINS', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCONTAINS call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCONTAINS call.");
			
			var sequence = TValue.asString(value2);
			var str = TValue.asString(value1);

			request.pushValue(TValue.createBoolean(str.indexOf(sequence) >= 0));
		}
	},

	/* STRCONTAINSPATTERN */
	{
		"name": 'STRCONTAINSPATTERN', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCONTAINSPATTERN call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCONTAINSPATTERN call.");
			
			var pattern = TValue.asString(value2);
			var str = TValue.asString(value1);
			
			try {
				var regex = new RegExp(pattern, 'gm');
				request.pushValue(TValue.createBoolean(regex.test(str)));
			} catch (err) {
				throw TAMEError.UnexpectedValueType("Expected valid RegEx in STRCONTAINSPATTERN call.");
			}
		}
	},

	/* STRSPLIT */
	{
		"name": 'STRSPLIT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRSPLIT call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRSPLIT call.");
			
			var pattern = TValue.asString(value2);
			var str = TValue.asString(value1);

			try {
				var regex = new RegExp(pattern, 'gm');
				var tokens = str.split(regex);
				var out = TValue.createList([]);
				for (var x in tokens)
					TValue.listAdd(out, TValue.createString(tokens[x]));
				request.pushValue(out);
			} catch (err) {
				throw TAMEError.UnexpectedValueType("Expected valid RegEx in STRSPLIT call.");
			}
		}
	},

	/* STRJOIN */
	{
		"name": 'STRJOIN', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var joiner = request.popValue();
			var list = request.popValue();

			if (!TValue.isLiteral(joiner))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRJOIN call.");
			if (!TValue.isLiteral(list))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRJOIN call.");
			
			var str = TValue.asString(joiner);

			if (!TValue.isList(list) || TValue.length(list) == 1)
				request.pushValue(TValue.createString(TValue.asString(list)));
			else
			{
				var sb = new TStringBuilder();
				var len = TValue.length(list);
				for (var i = 0; i < len; i++)
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
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRSTARTSWITH call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRSTARTSWITH call.");
			
			var sequence = TValue.asString(value2);
			var str = TValue.asString(value1);

			request.pushValue(TValue.createBoolean(str.substring(0, sequence.length) === sequence));
		}
	},

	/* STRENDSWITH */
	{
		"name": 'STRENDSWITH', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRENDSWITH call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRENDSWITH call.");
			
			var sequence = TValue.asString(value2);
			var str = TValue.asString(value1);

			request.pushValue(TValue.createBoolean(str.substring(str.length - sequence.length) === sequence));
		}
	},

	/* SUBSTRING */
	{
		"name": 'SUBSTRING', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value3 = request.popValue();
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in SUBSTRING call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in SUBSTRING call.");
			if (!TValue.isLiteral(value3))
				throw TAMEError.UnexpectedValueType("Expected literal type in SUBSTRING call.");

			var end = TValue.asLong(value3);
			var start = TValue.asLong(value2);
			var source = TValue.asString(value1);
			
			request.pushValue(TValue.createString(source.substring(start, end)));
		}
	},

	/* STRLOWER */
	{
		"name": 'STRLOWER', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value1 = request.popValue();

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
			var value1 = request.popValue();

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
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCHAR call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCHAR call.");
			
			var index = TValue.asLong(value2);
			var str = TValue.asString(value1);

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
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRTRIM call.");

			request.pushValue(TValue.createString(TValue.asString(value1).trim()));
		}
	},

	/* LISTADD */
	{
		"name": 'LISTADD', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value = request.popValue();
			var list = request.popValue();
			
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
			var index = request.popValue();
			var value = request.popValue();
			var list = request.popValue();
			
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
			var value = request.popValue();
			var list = request.popValue();
			
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
			var index = request.popValue();
			var list = request.popValue();
			
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
			var appendix = request.popValue();
			var list = request.popValue();
			
			if (!TValue.isLiteral(appendix))
				throw TAMEError.UnexpectedValueType("Expected literal type in LISTCONCAT call.");
			if (!TValue.isLiteral(list))
				throw TAMEError.UnexpectedValueType("Expected literal type in LISTCONCAT call.");

			if (!TValue.isList(list))
			{
				var v = list;
				list = TValue.createList([]);
				TValue.listAdd(list, v);
			}

			if (!TValue.isList(appendix))
			{
				var v = appendix;
				appendix = TValue.createList([]);
				TValue.listAdd(appendix, v);
			}
			
			var out = TValue.createList([]);
			for (var i = 0; i < TValue.length(list); i++)
				TValue.listAdd(out, TValue.listGet(list, i));
			for (var i = 0; i < TValue.length(appendix); i++)
				TValue.listAdd(out, TValue.listGet(appendix, i));
			
			request.pushValue(out);
		}
	},
		
	/* LISTINDEX */
	{
		"name": 'LISTINDEX', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value = request.popValue();
			var list = request.popValue();
			
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
			var value = request.popValue();
			var list = request.popValue();
			
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
			var value1 = request.popValue();

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
			var value1 = request.popValue();

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
			var value1 = request.popValue();

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
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in FIX call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in FIX call.");
			
			var d = TValue.asDouble(value1);
			var f = TValue.asDouble(value2);
			var t = Math.pow(10, f);

			request.pushValue(TValue.createFloat(Math.round(d * t) / t));
		}
	},

	/* SQRT */
	{
		"name": 'SQRT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value1 = request.popValue();

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
			var value1 = request.popValue();

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
			var value1 = request.popValue();

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
			var value1 = request.popValue();

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
			var value2 = request.popValue();
			var value1 = request.popValue();

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
			var value2 = request.popValue();
			var value1 = request.popValue();

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
			var value3 = request.popValue();
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in CLAMP call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in CLAMP call.");
			if (!TValue.isLiteral(value3))
				throw TAMEError.UnexpectedValueType("Expected literal type in CLAMP call.");

			var hi = TValue.asDouble(value3);
			var lo = TValue.asDouble(value2);
			var number = TValue.asDouble(value1);
			
			request.pushValue(TValue.createFloat(Math.min(Math.max(number, lo), hi)));
		}
	},

	/* IRANDOM */
	{
		"name": 'IRANDOM', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in IRANDOM call.");

			var value = TValue.asLong(value1);

			if (value == 0)
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
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in GRANDOM call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in GRANDOM call.");
			
			// Box-Muller Approximate algorithm c/o Maxwell Collard on StackOverflow

			var stdDev = TValue.asDouble(value2);
			var mean = TValue.asDouble(value1);
			
		    var u = 1.0 - Math.random();
		    var v = 1.0 - Math.random();
		    var stdNormal = Math.sqrt(-2.0 * Math.log(u)) * Math.cos(2.0 * Math.PI * v);
		    var out = mean + stdDev * stdNormal;

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

	/* SECONDS */
	{
		"name": 'SECONDS', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in SECONDS call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in SECONDS call.");

			var first = TValue.asLong(value1);
			var second = TValue.asLong(value2);

			request.pushValue(TValue.createInteger((second - first) / 1000));
		}
	},

	/* MINUTES */
	{
		"name": 'MINUTES', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in MINUTES call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in MINUTES call.");

			var first = TValue.asLong(value1);
			var second = TValue.asLong(value2);

			request.pushValue(TValue.createInteger((second - first) / (1000 * 60)));
		}
	},

	/* HOURS */
	{
		"name": 'HOURS', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in HOURS call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in HOURS call.");

			var first = TValue.asLong(value1);
			var second = TValue.asLong(value2);

			request.pushValue(TValue.createInteger((second - first) / (1000 * 60 * 60)));
		}
	},

	/* DAYS */
	{
		"name": 'DAYS', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in DAYS call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in DAYS call.");

			var first = TValue.asLong(value1);
			var second = TValue.asLong(value2);

			request.pushValue(TValue.createInteger((second - first) / (1000 * 60 * 60 * 24)));
		}
	},

	/* FORMATTIME */
	{
		"name": 'FORMATTIME', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in FORMATTIME call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in FORMATTIME call.");

			var date = TValue.asLong(value1);
			var format = TValue.asString(value2);

			request.pushValue(TValue.createString(Util.formatDate(date, format, false)));
		}
	},

	/* OBJECTHASNAME */
	{
		"name": 'OBJECTHASNAME', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var nameValue = request.popValue();
			var varObject = request.popValue();

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
			var tagValue = request.popValue();
			var varObject = request.popValue();

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
			var nameValue = request.popValue();
			var varObject = request.popValue();

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
			var tagValue = request.popValue();
			var varObject = request.popValue();

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
			var tagValue = request.popValue();
			var elementValue = request.popValue();

			if (!TValue.isLiteral(tagValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in ADDOBJECTTAGTOALLIN call.");
			if (!TValue.isObjectContainer(elementValue))
				throw TAMEError.UnexpectedValueType("Expected object-container type in ADDOBJECTTAGTOALLIN call.");

			var context = request.moduleContext;
			var element = context.resolveElement(TValue.asString(elementValue));
			
			var tag = TValue.asString(tagValue);
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
			var nameValue = request.popValue();
			var varObject = request.popValue();

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
			var tagValue = request.popValue();
			var varObject = request.popValue();

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
			var tagValue = request.popValue();
			var elementValue = request.popValue();

			if (!TValue.isLiteral(tagValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in REMOVEOBJECTTAGFROMALLIN call.");
			if (!TValue.isObjectContainer(elementValue))
				throw TAMEError.UnexpectedValueType("Expected object-container type in REMOVEOBJECTTAGFROMALLIN call.");

			var context = request.moduleContext;
			var element = context.resolveElement(TValue.asString(elementValue));
			
			var tag = TValue.asString(tagValue);
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
			var varObject = request.popValue();
			var varObjectContainer = request.popValue();

			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in GIVEOBJECT call.");
			if (!TValue.isObjectContainer(varObjectContainer))
				throw TAMEError.UnexpectedValueType("Expected object-container type in GIVEOBJECT call.");

			var element = request.moduleContext.resolveElement(TValue.asString(varObjectContainer));

			request.moduleContext.addObjectToElement(element.identity, TValue.asString(varObject));
		}
	},

	/* REMOVEOBJECT */
	{
		"name": 'REMOVEOBJECT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varObject = request.popValue();

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
			var tagValue = request.popValue();
			var varObjectContainerDest = request.popValue();
			var varObjectContainerSource = request.popValue();

			if (!TValue.isLiteral(tagValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in MOVEOBJECTSWITHTAG call.");
			if (!TValue.isObjectContainer(varObjectContainerDest))
				throw TAMEError.UnexpectedValueType("Expected object-container type in MOVEOBJECTSWITHTAG call.");
			if (!TValue.isObjectContainer(varObjectContainerSource))
				throw TAMEError.UnexpectedValueType("Expected object-container type in MOVEOBJECTSWITHTAG call.");

			var context = request.moduleContext;
			var destination = context.resolveElement(TValue.asString(varObjectContainerDest));
			var source = context.resolveElement(TValue.asString(varObjectContainerSource));
			var tag = TValue.asString(tagValue);
			
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
			var elementValue = request.popValue();

			if (!TValue.isObjectContainer(elementValue))
				throw TAMEError.UnexpectedValueType("Expected object-container type in OBJECTCOUNT call.");

			var element = request.moduleContext.resolveElement(TValue.asString(elementValue));

			request.pushValue(TValue.createInteger(request.moduleContext.getObjectsOwnedByElementCount(element.identity)));
		}
	},

	/* HASOBJECT */
	{
		"name": 'HASOBJECT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varObject = request.popValue();
			var varObjectContainer = request.popValue();

			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in HASOBJECT call.");
			if (!TValue.isObjectContainer(varObjectContainer))
				throw TAMEError.UnexpectedValueType("Expected object-container type in HASOBJECT call.");

			var element = request.moduleContext.resolveElement(TValue.asString(varObjectContainer));

			request.pushValue(TValue.createBoolean(request.moduleContext.checkElementHasObject(element.identity, TValue.asString(varObject))));
		}
	},

	/* OBJECTHASNOOWNER */
	{
		"name": 'OBJECTHASNOOWNER', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varObject = request.popValue();

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
			var varRoom = request.popValue();
			var varPlayer = request.popValue();
			
			if (!TValue.isRoom(varRoom))
				throw TAMEError.UnexpectedValueType("Expected room type in PLAYERISINROOM call.");
			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in PLAYERISINROOM call.");

			var context = request.moduleContext;
			var room = context.resolveElement(TValue.asString(varRoom));
			var player = context.resolveElement(TValue.asString(varPlayer));

			request.pushValue(TValue.createBoolean(context.checkPlayerIsInRoom(player.identity, room.identity)))
		}
	},

	/* PLAYERCANACCESSOBJECT */
	{
		"name": 'PLAYERCANACCESSOBJECT', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varObject = request.popValue();
			var varPlayer = request.popValue();

			if (!TValue.isObject(varObject))
				throw TAMEError.UnexpectedValueType("Expected object type in PLAYERCANACCESSOBJECT call.");
			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in PLAYERCANACCESSOBJECT call.");

			var player = request.moduleContext.resolveElement(TValue.asString(varPlayer));

			request.pushValue(TValue.createBoolean(TLogic.checkObjectAccessibility(request, response, player.identity, TValue.asString(varObject))));
		}
	},

	/* BROWSE */
	{
		"name": 'BROWSE', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varObjectContainer = request.popValue();

			if (!TValue.isObjectContainer(varObjectContainer))
				throw TAMEError.UnexpectedValueType("Expected object-container type in BROWSE call.");

			var element = request.moduleContext.resolveElement(TValue.asString(varObjectContainer));

			TLogic.doBrowse(request, response, element.identity);
		}
	},

	/* BROWSETAGGED */
	{
		"name": 'BROWSETAGGED', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varTag = request.popValue();
			var varObjectContainer = request.popValue();

			if (!TValue.isLiteral(varTag))
				throw TAMEError.UnexpectedValueType("Expected literal type in BROWSETAGGED call.");
			if (!TValue.isObjectContainer(varObjectContainer))
				throw TAMEError.UnexpectedValueType("Expected object-container type in BROWSETAGGED call.");

			var tagName = TValue.asString(varTag);
			var element = request.moduleContext.resolveElement(TValue.asString(varObjectContainer));

			TLogic.doBrowse(request, response, element.identity, tagName);
		}
	},

	/* ELEMENTHASANCESTOR */
	{
		"name": 'ELEMENTHASANCESTOR', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varParent = request.popValue();
			var varElement = request.popValue();

			if (!TValue.isElement(varElement))
				throw TAMEError.UnexpectedValueType("Expected element type in ELEMENTHASANCESTOR call.");
			if (!TValue.isElement(varParent))
				throw TAMEError.UnexpectedValueType("Expected element type in ELEMENTHASANCESTOR call.");

			var context = request.moduleContext;
			var parentIdentity = context.resolveElement(TValue.asString(varParent)).identity;
			var element = context.resolveElement(TValue.asString(varElement));

			var found = false;
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
			var varPlayer = request.popValue();

			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in SETPLAYER call.");

			var player = request.moduleContext.resolveElement(TValue.asString(varPlayer));

			TLogic.doPlayerSwitch(request, response, player.identity);
		}
	},

	/* SETROOM */
	{
		"name": 'SETROOM', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varRoom = request.popValue();
			var varPlayer = request.popValue();

			if (!TValue.isRoom(varRoom))
				throw TAMEError.UnexpectedValueType("Expected room type in SETROOM call.");
			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in SETROOM call.");

			var context = request.moduleContext;
			var room = context.resolveElement(TValue.asString(varRoom));
			var player = context.resolveElement(TValue.asString(varPlayer));
			
			TLogic.doRoomSwitch(request, response, player.identity, room.identity);
		}
	},

	/* PUSHROOM */
	{
		"name": 'PUSHROOM', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varRoom = request.popValue();
			var varPlayer = request.popValue();

			if (!TValue.isRoom(varRoom))
				throw TAMEError.UnexpectedValueType("Expected room type in PUSHROOM call.");
			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in PUSHROOM call.");

			var context = request.moduleContext;
			var room = context.resolveElement(TValue.asString(varRoom));
			var player = context.resolveElement(TValue.asString(varPlayer));

			TLogic.doRoomPush(request, response, player.identity, room.identity);
		}
	},

	/* POPROOM */
	{
		"name": 'POPROOM', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varPlayer = request.popValue();

			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in POPROOM call.");

			var context = request.moduleContext;
			var player = context.resolveElement(TValue.asString(varPlayer));

			var currentRoom = context.getCurrentRoom(player.identity);
			
			if (currentRoom == null)
				throw TAMEInterrupt.Error("No rooms for player" + TLogic.elementToString(player));
			
			TLogic.doRoomPop(request, response, player.identity);
		}
	},

	/* SWAPROOM */
	{
		"name": 'SWAPROOM', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varRoom = request.popValue();
			var varPlayer = request.popValue();

			if (!TValue.isRoom(varRoom))
				throw TAMEError.UnexpectedValueType("Expected room type in SWAPROOM call.");
			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in SWAPROOM call.");

			var context = request.moduleContext;
			var player = context.resolveElement(TValue.asString(varPlayer));

			if (player == null)
				throw TAMEInterrupt.Error("No current player!");

			var nextRoom = context.resolveElement(TValue.asString(varRoom)); 
			var currentRoom = context.getCurrentRoom(player.identity);

			if (currentRoom == null)
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
			var varPlayer = request.popValue();

			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in CURRENTPLAYERIS call.");

			var context = request.moduleContext;
			var player = context.resolveElement(TValue.asString(varPlayer));
			var currentPlayer = context.getCurrentPlayer();
			
			request.pushValue(TValue.createBoolean(currentPlayer != null && player.identity == currentPlayer.identity));
		}
	},

	/* NOCURRENTPLAYER */
	{
		"name": 'NOCURRENTPLAYER', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var currentPlayer = request.moduleContext.getCurrentPlayer();
			request.pushValue(TValue.createBoolean(currentPlayer == null));
		}
	},

	/* CURRENTROOMIS */
	{
		"name": 'CURRENTROOMIS', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varRoom = request.popValue();
			var varPlayer = request.popValue();

			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in CURRENTROOMIS call.");
			if (!TValue.isRoom(varRoom))
				throw TAMEError.UnexpectedValueType("Expected room type in CURRENTROOMIS call.");

			var context = request.moduleContext;
			var playerIdentity = TValue.asString(varPlayer);
			var player = context.resolveElement(playerIdentity);
			var room = context.resolveElement(TValue.asString(varRoom));
			
			var currentRoom = context.getCurrentRoom(player.identity);
			request.pushValue(TValue.createBoolean(currentRoom != null && room.identity == currentRoom.identity));
		}
	},

	/* NOCURRENTROOM */
	{
		"name": 'NOCURRENTROOM', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var varPlayer = request.popValue();

			if (!TValue.isPlayer(varPlayer))
				throw TAMEError.UnexpectedValueType("Expected player type in NOCURRENTROOM call.");
			
			var context = request.moduleContext;
			var playerIdentity = TValue.asString(varPlayer);
			var player = context.resolveElement(playerIdentity);
			var currentRoom = context.getCurrentRoom(player.identity);
			request.pushValue(TValue.createBoolean(currentRoom == null));
		}
	},

	/* IDENTITY */
	{
		"name": 'IDENTITY', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var elementValue = request.popValue();
			
			if (!TValue.isElement(elementValue))
				throw TAMEError.UnexpectedValueType("Expected element type in IDENTITY call.");
			
			var element = request.moduleContext.resolveElement(TValue.asString(elementValue));
			request.pushValue(TValue.createString(element.identity));
		}
	},

	/* HEADER */
	{
		"name": 'HEADER', 
		"doOperation": function(request, response, blockLocal, operation)
		{
			var headerName = request.popValue();
			
			if (!TValue.isLiteral(headerName))
				throw TAMEError.UnexpectedValueType("Expected literal type in HEADER call.");
			
			request.pushValue(TValue.createString(request.moduleContext.module.header[TValue.asString(headerName)]));
		}
	},

];

//##[[EXPORTJS-END

// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TOperationFunctions;
// =========================================================================
