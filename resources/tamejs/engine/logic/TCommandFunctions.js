/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var TAction = TAction || ((typeof require) !== 'undefined' ? require('../objects/TAction.js') : null);
var TValue = TValue || ((typeof require) !== 'undefined' ? require('../objects/TValue.js') : null);
var TLogic = TLogic || ((typeof require) !== 'undefined' ? require('../TAMELogic.js') : null);
var TAMEConstants = TAMEConstants || ((typeof require) !== 'undefined' ? require('../TAMEConstants.js') : null);
var TAMEError = TAMEError || ((typeof require) !== 'undefined' ? require('../TAMEError.js') : null);
var TAMEInterrupt = TAMEInterrupt || ((typeof require) !== 'undefined' ? require('../TAMEInterrupt.js') : null);
var Util = Util || ((typeof require) !== 'undefined' ? require('../Util.js') : null);
// ======================================================================================================

//##[[EXPORTJS-START

/*****************************************************************************
Command entry points.
*****************************************************************************/
var TCommandFunctions =
[
	/* NOOP */
	{
		"name": 'NOOP', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// Do nothing.
		}
	},

	/* POP */
	{
		"name": 'POP', 
		"doCommand": function(request, response, blockLocal, command)
		{
			request.popValue();
		}
	},

	/* POPVALUE */
	{
		"name": 'POPVALUE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varvalue = command.operand0;
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in POPVALUE call.");
			if (!TValue.isVariable(varvalue))
				throw TAMEError.UnexpectedValueType("Expected variable type in POPVALUE call.");

			var variableName = TValue.asString(varvalue);
			
			if (blockLocal[variableName.toLowerCase()])
				TLogic.setValue(blockLocal, variableName, value);
			else
				TLogic.setValue(request.peekContext().variables, variableName, value);
		}
	},

	/* POPLOCALVALUE */
	{
		"name": 'POPLOCALVALUE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varvalue = command.operand0;
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in POPLOCALVALUE call.");
			if (!TValue.isVariable(varvalue))
				throw TAMEError.UnexpectedValueType("Expected variable type in POPLOCALVALUE call.");

			TLogic.setValue(blockLocal, TValue.asString(varvalue), value);
		}
	},

	/* POPELEMENTVALUE */
	{
		"name": 'POPELEMENTVALUE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varObject = command.operand0;
			var variable = command.operand1;
			var value = request.popValue();

			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in POPELEMENTVALUE call.");
			if (!TValue.isVariable(variable))
				throw TAMEError.UnexpectedValueType("Expected variable type in POPELEMENTVALUE call.");
			if (!TValue.isElement(varObject))
				throw TAMEError.UnexpectedValueType("Expected element type in POPELEMENTVALUE call.");

			var objectName = TValue.asString(varObject);

			TLogic.setValue(request.moduleContext.resolveElementContext(objectName).variables, TValue.asString(variable), value);
		}
	},

	/* PUSHVALUE */
	{
		"name": 'PUSHVALUE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = command.operand0;
			
			if (TValue.isVariable(value))
			{
				var variableName = TValue.asString(value).toLowerCase();
				if (blockLocal[variableName])
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
		"doCommand": function(request, response, blockLocal, command)
		{
			var varElement = command.operand0;
			var variable = command.operand1;

			if (!TValue.isVariable(variable))
				throw TAMEError.UnexpectedValueType("Expected variable type in PUSHELEMENTVALUE call.");
			if (!TValue.isElement(varElement))
				throw TAMEError.UnexpectedValueType("Expected element type in PUSHELEMENTVALUE call.");

			var elementName = TValue.asString(varElement);

			request.pushValue(TLogic.getValue(request.moduleContext.resolveElementContext(elementName).variables, TValue.asString(variable)));
		}
	},

	/* CLEARVALUE */
	{
		"name": 'CLEARVALUE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = command.operand0;

			if (!TValue.isVariable(value))
				throw TAMEError.UnexpectedValueType("Expected variable type in CLEARVALUE call.");
			
			var variableName = TValue.asString(value).toLowerCase();
			if (blockLocal[variableName])
				TLogic.clearValue(blockLocal, variableName);
			else
				TLogic.clearValue(request.peekContext().variables, variableName);
		}
	},

	/* CLEARELEMENTVALUE */
	{
		"name": 'CLEARELEMENTVALUE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varElement = command.operand0;
			var variable = command.operand1;

			if (!TValue.isVariable(variable))
				throw TAMEError.UnexpectedValueType("Expected variable type in CLEARELEMENTVALUE call.");
			if (!TValue.isElement(varElement))
				throw TAMEError.UnexpectedValueType("Expected element type in CLEARELEMENTVALUE call.");

			var variableName = TValue.asString(variable).toLowerCase();
			if (blockLocal[variableName])
				TLogic.clearValue(blockLocal, TValue.asString(variable));
			else
			{
				var element = request.moduleContext.resolveElementContext(TValue.asString(varElement));
				TLogic.clearValue(element.variables, TValue.asString(variable))
			}
		}
	},

	/* ARITHMETICFUNC */
	{
		"name": 'ARITHMETICFUNC', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var functionValue = command.operand0;

			if (!TValue.isInteger(functionValue))
				throw TAMEError.UnexpectedValueType("Expected integer type in ARITHMETICFUNC call.");

			TLogic.doArithmeticStackFunction(request, response, TValue.asLong(functionValue));
		}
	},

	/* IF */
	{
		"name": 'IF', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var result = TLogic.callConditional('IF', request, response, blockLocal, command);
			
			if (result)
			{
				response.trace(request, "Calling IF success block...");
				var success = command.successBlock;
				if (!success)
					throw TAMEError.ModuleExecution("Success block for IF does NOT EXIST!");
				TLogic.executeBlock(success, request, response, blockLocal);
			}
			else
			{
				var failure = command.failureBlock;
				if (failure)
				{
					response.trace(request, "Calling IF failure block...");
					TLogic.executeBlock(failure, request, response, blockLocal);
				}
				else
				{
					response.trace(request, "No failure block.");
				}
			}
		}
	},

	/* WHILE */
	{
		"name": 'WHILE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			while (TLogic.callConditional('WHILE', request, response, blockLocal, command))
			{
				try {
					response.trace(request, "Calling WHILE success block...");
					var success = command.successBlock;
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
		"doCommand": function(request, response, blockLocal, command)
		{
			var init = command.initBlock;
			if (!init)
				throw TAMEError.ModuleExecution("Init block for FOR does NOT EXIST!");
			var success = command.successBlock;
			if (!success)
				throw TAMEError.ModuleExecution("Success block for FOR does NOT EXIST!");
			var step = command.stepBlock;
			if (!step)
				throw TAMEError.ModuleExecution("Step block for FOR does NOT EXIST!");

			response.trace(request, "Calling FOR init block...");

			for (
				TLogic.executeBlock(init, request, response, blockLocal);
				TLogic.callConditional('FOR', request, response, blockLocal, command);
				response.trace(request, "Calling FOR stepping block..."),
				TLogic.executeBlock(step, request, response, blockLocal)
			)
			{
				try {
					response.trace(request, "Calling FOR success block...");
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
		"doCommand": function(request, response, blockLocal, command)
		{
			response.trace(request, "Throwing break interrupt...");
			throw TAMEInterrupt.Break();
		}
	},

	/* CONTINUE */
	{
		"name": 'CONTINUE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			response.trace(request, "Throwing continue interrupt...");
			throw TAMEInterrupt.Continue();
		}
	},

	/* QUIT */
	{
		"name": 'QUIT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			response.trace(request, "Throwing quit interrupt...");
			response.addCue(TAMEConstants.Cue.QUIT);
			throw TAMEInterrupt.Quit();
		}
	},

	/* FINISH */
	{
		"name": 'FINISH', 
		"doCommand": function(request, response, blockLocal, command)
		{
			response.trace(request, "Throwing finish interrupt...");
			throw TAMEInterrupt.Finish();
		}
	},

	/* END */
	{
		"name": 'END', 
		"doCommand": function(request, response, blockLocal, command)
		{
			response.trace(request, "Throwing end interrupt...");
			throw TAMEInterrupt.End();
		}
	},

	/* FUNCTIONRETURN */
	{
		"name": 'FUNCTIONRETURN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var retVal = request.popValue();
			response.trace(request, "Returning "+TValue.toString(retVal));
			TLogic.setValue(blockLocal, TAMEConstants.RETURN_VARIABLE, retVal);
			response.trace(request, "Throwing end interrupt...");
			throw TAMEInterrupt.End();
		}
	},

	/* CALLFUNCTION */
	{
		"name": 'CALLFUNCTION', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varFunctionName = command.operand0;

			if (!!TValue.isLiteral(varFunctionName))
				throw TAMEError.UnexpectedValueType("Expected literal type in CALLFUNCTION call.");

			request.pushValue(TLogic.callElementFunction(request, response, TValue.asString(varFunctionName), request.peekContext()));
		}
	},

	/* CALLELEMENTFUNCTION */
	{
		"name": 'CALLELEMENTFUNCTION', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varElement = command.operand0;
			var varFunctionName = command.operand1;

			if (!TValue.isElement(varElement))
				throw TAMEError.UnexpectedValueType("Expected element type in CALLELEMENTFUNCTION call.");
			if (!TValue.isLiteral(varFunctionName))
				throw TAMEError.UnexpectedValueType("Expected literal type in CALLELEMENTFUNCTION call.");

			var elementContext = request.moduleContext.resolveElementContext(TValue.asString(varElement));
			request.pushValue(TLogic.callElementFunction(request, response, TValue.asString(varFunctionName), elementContext));
		}
	},

	/* CALL */
	{
		"name": 'CALL', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var procedureName = request.popValue();
			if (!TValue.isLiteral(procedureName))
				throw TAMEError.UnexpectedValueType("Expected literal type in CALL call.");
			
			var elementContext = request.peekContext();
			if (!elementContext)
				throw TAMEError.ModuleExecution("Attempted CALL call without a context!");

			TLogic.callProcedureFrom(request, response, procedureName, elementContext, false);
		}
	},

	/* CALLFROM */
	{
		"name": 'CALLFROM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var procedureName = request.popValue();
			var elementValue = request.popValue();
			
			if (!TValue.isLiteral(procedureName))
				throw TAMEError.UnexpectedValueType("Expected literal type in CALLFROM call.");
			if (!TValue.isElement(elementValue))
				throw TAMEError.UnexpectedValueType("Expected element type in CALLFROM call.");

			var id = TValue.asString(elementValue);

			// IMPORTANT: Must resolve: the passed-in value could be the "current" room/player.
			var elementContext = request.moduleContext.resolveElementContext(id);
			TLogic.callProcedureFrom(request, response, procedureName, elementContext, false);
		}
	},

	/* CALLMAYBE */
	{
		"name": 'CALLMAYBE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var procedureName = request.popValue();
			if (!TValue.isLiteral(procedureName))
				throw TAMEError.UnexpectedValueType("Expected literal type in CALLMAYBE call.");
			
			var elementContext = request.peekContext();
			if (!elementContext)
				throw TAMEError.ModuleExecution("Attempted CALLMAYBE call without a context!");

			TLogic.callProcedureFrom(request, response, procedureName, elementContext, true);
		}
	},

	/* CALLFROMMAYBE */
	{
		"name": 'CALLFROMMAYBE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var procedureName = request.popValue();
			var elementValue = request.popValue();
			
			if (!TValue.isLiteral(procedureName))
				throw TAMEError.UnexpectedValueType("Expected literal type in CALLFROMMAYBE call.");
			if (!TValue.isElement(elementValue))
				throw TAMEError.UnexpectedValueType("Expected element type in CALLFROMMAYBE call.");

			var id = TValue.asString(elementValue);

			// IMPORTANT: Must resolve: the passed-in value could be the "current" room/player.
			var elementContext = request.moduleContext.resolveElementContext(id);
			TLogic.callProcedureFrom(request, response, procedureName, elementContext, true);
		}
	},

	/* QUEUEACTION */
	{
		"name": 'QUEUEACTION', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varAction = request.popValue();

			if (!TValue.isAction(varAction))
				throw TAMEError.UnexpectedValueType("Expected action type in QUEUEACTION call.");

			var action = request.moduleContext.resolveAction(TValue.asString(varAction));

			if (action.type != TAMEConstants.ActionType.GENERAL)
				throw TAMEInterrupt.Error(action.identity + " is not a general action.");

			var tameAction = TAction.create(action);
			request.addActionItem(tameAction);
			response.trace(request, "Enqueued "+tameAction.toString());
		}
	},

	/* QUEUEACTIONSTRING */
	{
		"name": 'QUEUEACTIONSTRING', 
		"doCommand": function(request, response, blockLocal, command)
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

			var tameAction = TAction.createModal(action, TValue.asString(varTarget));
			request.addActionItem(tameAction);
			response.trace(request, "Enqueued "+tameAction.toString());
		}
	},

	/* QUEUEACTIONOBJECT */
	{
		"name": 'QUEUEACTIONOBJECT', 
		"doCommand": function(request, response, blockLocal, command)
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
				throw TAMEInterrupt.Error(action.identity + " is not a transitive nor ditransitive action.");

			var tameAction = TAction.createObject(action, object);
			request.addActionItem(tameAction);
			response.trace(request, "Enqueued "+tameAction.toString());
		}
	},

	/* QUEUEACTIONFOROBJECTSIN */
	{
		"name": 'QUEUEACTIONFOROBJECTSIN', 
		"doCommand": function(request, response, blockLocal, command)
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
				throw TAMEInterrupt.Error(action.identity + " is not a transitive nor ditransitive action.");

			var element = context.resolveElement(TValue.asString(varObjectContainer));
			Util.each(context.getObjectsOwnedByElement(element.identity), function(objectIdentity){
				var object = context.resolveElement(objectIdentity);
				var tameAction = TAction.createObject(action, object);
				request.addActionItem(tameAction);
				response.trace(request, "Enqueued "+tameAction.toString());
			});
		}

	},
	
	/* QUEUEACTIONFORTAGGEDOBJECTSIN */
	{
		"name": 'QUEUEACTIONFORTAGGEDOBJECTSIN', 
		"doCommand": function(request, response, blockLocal, command)
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
				throw TAMEInterrupt.Error(action.identity + " is not a transitive nor ditransitive action.");

			var tagName = TValue.asString(varTag);
			var element = context.resolveElement(TValue.asString(varObjectContainer));
			Util.each(context.getObjectsOwnedByElement(element.identity), function(objectIdentity){
				if (!context.checkObjectHasTag(objectIdentity, tagName))
					return;
				
				var object = context.resolveElement(objectIdentity);
				var tameAction = TAction.createObject(action, object);
				request.addActionItem(tameAction);
				response.trace(request, "Enqueued "+tameAction.toString());
			});
		}

	},
	
	/* QUEUEACTIONOBJECT2 */
	{
		"name": 'QUEUEACTIONOBJECT2', 
		"doCommand": function(request, response, blockLocal, command)
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
				throw TAMEInterrupt.Error(action.identity + " is not a ditransitive action.");
			
			var tameAction = TAction.createObject2(action, object, object2);
			request.addActionItem(tameAction);
			response.trace(request, "Enqueued "+tameAction.toString());
		}
	},

	/* ADDCUE */
	{
		"name": 'ADDCUE', 
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
		{
			response.addCue(TAMEConstants.Cue.PAUSE);
		}
	},

	/* WAIT */
	{
		"name": 'WAIT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in WAIT call.");

			response.addCue(TAMEConstants.Cue.WAIT, TValue.asLong(value));
		}
	},

	/* TIP */
	{
		"name": 'TIP', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in TIP call.");

			response.addCue(TAMEConstants.Cue.TIP, TValue.asString(value));
		}
	},

	/* INFO */
	{
		"name": 'INFO', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in INFO call.");

			response.addCue(TAMEConstants.Cue.INFO, TValue.asString(value));
		}
	},

	/* ASBOOLEAN */
	{
		"name": 'ASBOOLEAN', 
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in ASSTRING call.");

			request.pushValue(TValue.createString(TValue.asString(value)));
		}
	},

	/* STRLENGTH */
	{
		"name": 'STRLENGTH', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRLENGTH call.");

			request.pushValue(TValue.createInteger(TValue.asString(value).length));
		}
	},

	/* STRCONCAT */
	{
		"name": 'STRCONCAT', 
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
			
			request.pushValue(TValue.createString(source.replace(new RegExp(pattern, 'm'), replacement)));
		}
	},

	/* STRREPLACEPATTERNALL */
	{
		"name": 'STRREPLACEPATTERNALL', 
		"doCommand": function(request, response, blockLocal, command)
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
			
			request.pushValue(TValue.createString(source.replace(new RegExp(pattern, 'gm'), replacement)));
		}
	},

	/* STRINDEX */
	{
		"name": 'STRINDEX', 
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCONTAINSPATTERN call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCONTAINSPATTERN call.");
			
			var pattern = TValue.asString(value2);
			var str = TValue.asString(value1);

			request.pushValue(TValue.createBoolean((new RegExp(pattern, 'gm')).test(str)));
		}
	},

	/* STRCONTAINSTOKEN */
	{
		"name": 'STRCONTAINSTOKEN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCONTAINSTOKEN call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRCONTAINSTOKEN call.");
			
			var token = TValue.asString(value2).toLowerCase();
			var str = TValue.asString(value1).toLowerCase();

			request.pushValue(TValue.createBoolean(str.split(/\s+/).indexOf(token) >= 0));
		}
	},

	/* STRSTARTSWITH */
	{
		"name": 'STRSTARTSWITH', 
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
		{
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRTRIM call.");

			request.pushValue(TValue.createString(TValue.asString(value1).trim()));
		}
	},

	/* FLOOR */
	{
		"name": 'FLOOR', 
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
		{
			request.pushValue(TValue.createFloat(Math.PI));
		}
	},

	/* E */
	{
		"name": 'E', 
		"doCommand": function(request, response, blockLocal, command)
		{
			request.pushValue(TValue.createFloat(Math.E));
		}
	},

	/* SIN */
	{
		"name": 'SIN', 
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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

	/* RANDOM */
	{
		"name": 'RANDOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in RANDOM call.");

			if (TValue.isInteger(value1) || TValue.isBoolean(value1))
			{
				var value = TValue.asLong(value1);
				if (value == 0)
					request.pushValue(TValue.createInteger(0));
				else
				{
					var v = Math.floor(Math.random() * Math.abs(value));
					if (value < 0)
						request.pushValue(TValue.createInteger(-v));
					else
						request.pushValue(TValue.createInteger(v));
				}
			}
			else
			{
				var value = TValue.asDouble(value1);
				if (value == 0.0)
					request.pushValue(TValue.createFloat(0.0));
				else
					request.pushValue(TValue.createFloat(Math.random() * value));
			}
		}
	},

	/* FRANDOM */
	{
		"name": 'FRANDOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			request.pushValue(TValue.createFloat(Math.random()));
		}
	},

	/* GRANDOM */
	{
		"name": 'GRANDOM', 
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
		{
			request.pushValue(TValue.createInteger(Date.now()));
		}
	},

	/* SECONDS */
	{
		"name": 'SECONDS', 
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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

	/* SETPLAYER */
	{
		"name": 'SETPLAYER', 
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
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
		"doCommand": function(request, response, blockLocal, command)
		{
			var currentPlayer = request.moduleContext.getCurrentPlayer();
			request.pushValue(TValue.createBoolean(currentPlayer == null));
		}
	},

	/* CURRENTROOMIS */
	{
		"name": 'CURRENTROOMIS', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varRoom = request.popValue();

			if (!TValue.isRoom(varRoom))
				throw TAMEError.UnexpectedValueType("Expected room type in CURRENTPLAYERIS call.");

			var context = request.moduleContext;
			var room = context.resolveElement(TValue.asString(varRoom));
			var currentRoom = context.getCurrentRoom();
			
			request.pushValue(TValue.createBoolean(currentRoom != null && room.identity == currentRoom.identity));
		}
	},

	/* NOCURRENTROOM */
	{
		"name": 'NOCURRENTROOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var currentRoom = request.moduleContext.getCurrentRoom();
			request.pushValue(TValue.createBoolean(currentRoom == null));
		}
	},

	/* IDENTITY */
	{
		"name": 'IDENTITY', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var elementValue = request.popValue();
			
			if (!TValue.isElement(elementValue))
				throw TAMEError.UnexpectedValueType("Expected element type in IDENTITY call.");
			
			var element = request.moduleContext.resolveElement(TValue.asString(elementValue));
			request.pushValue(TValue.createString(element.identity));
		}
	},

	/* CONTEXTIDENTITY */
	{
		"name": 'CONTEXTIDENTITY', 
		"doCommand": function(request, response, blockLocal, command)
		{
			request.pushValue(TValue.createString(request.peekContext().identity));
		}
	},

];

//##[[EXPORTJS-END

// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TCommandFunctions;
// =========================================================================
