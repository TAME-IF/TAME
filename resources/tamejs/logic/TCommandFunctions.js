/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var TValue = TValue || ((typeof require) !== 'undefined' ? require('../objects/TValue.js') : null);
var TArithmeticFunctions = TArithmeticFunctions || ((typeof require) !== 'undefined' ? require('./TArithmeticFunctions.js') : null);
var TLogic = TLogic || ((typeof require) !== 'undefined' ? require('../TAMELogic.js') : null);
var TAMEConstants = TAMEConstants || ((typeof require) !== 'undefined' ? require('../TAMEConstants.js') : null);
// ======================================================================================================

//##[[CONTENT-START

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
			var value = command.operand1;
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in POPVALUE call.");
			if (!TValue.isVariable(varvalue))
				throw TAMEError.UnexpectedValueType("Expected variable type in POPVALUE call.");

			var variableName = TValue.asString(varvalue);
			
			if (blockLocal[variableName])
				blockLocal[variableName] = value;
			else
				request.peekContext().variables[variableName] = value;
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

			var variableName = TValue.asString(varvalue);
			blockLocal[variableName] = value;
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
			if (!TValue.isVariable(varvalue))
				throw TAMEError.UnexpectedValueType("Expected variable type in POPELEMENTVALUE call.");
			if (!TValue.isElement(varObject))
				throw TAMEError.UnexpectedValueType("Expected element type in POPELEMENTVALUE call.");

			var variableName = TValue.asString(variable);
			var objectName = TValue.asString(varObject);

			request.moduleContext.resolveElementContext(objectName).variables[variableName] = value;
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
				var variableName = TValue.asString(value);
				if (blockLocal[variableName])
					request.pushValue(blockLocal[variableName]);
				else
					request.pushValue(request.peekContext().variables[variableName]);
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
			var varObject = command.operand0;
			var variable = command.operand1;

			if (!TValue.isVariable(variable))
				throw TAMEError.UnexpectedValueType("Expected variable type in PUSHELEMENTVALUE call.");
			if (!TValue.isElement(varObject))
				throw TAMEError.UnexpectedValueType("Expected element type in PUSHELEMENTVALUE call.");

			var objectName = TValue.asString(varObject);
			var varibleName = TValue.asString(variable);

			request.pushValue(request.moduleContext.resolveElementContext(objectName).variables[variableName]);
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

			var funcval = TValue.asLong(functionValue);
			if (functionType < 0 || functionType >= TArithmeticFunctions.COUNT)
				throw TAMEError.UnexpectedValue("Expected arithmetic function type, got illegal value "+funcval+".");
			
			var operator = TArithmeticFunctions[funcval];
			response.trace(request, "Function is " + operator.name);
			
			if (operator.binary)
			{
				var v2 = request.popValue();
				var v1 = request.popValue();
				request.pushValue(operator.doOperation(v1, v2));
			}
			else
			{
				var v1 = request.popValue();
				request.pushValue(operator.doOperation(v1));
			}
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
				TLogic.callConditional(request, response, blockLocal, command);
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

	/* CALL */
	{
		"name": 'CALL', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var procedureName = request.popValue();
			if (!TValue.isLiteral(procedureName))
				throw TAMEError.UnexpectedValueType("Expected literal type in CALL call.");
			
			if (!request.peekContext())
				throw TAMEError.ModuleExecution("Attempted CALL call without a context!");

			var elementContext = request.peekContext();
			var element = request.moduleContext.resolveElement(elementContext.identity);

			var block = request.moduleContext.resolveBlock(element.identity, 'PROCEDURE', [procedureName]);
			if (block)
				TLogic.executeBlock(block, request, response, elementContext);
			else
				response.addCue(TAMEConstants.Cue.ERROR, "No such procedure ("+TValue.asString(procedureName)+") in lineage of element " + TLogic.elementToString(element));
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
			var element = request.moduleContext.resolveElement(id);

			var block = TLogic.executeBlock(block, request, response, elementContext);
			if (block)
				TLogic.executeBlock(block, request, response, elementContext);
			else
				response.addCue(TAMEConstants.Cue.ERROR, "No such procedure ("+TValue.asString(procedureName)+") in lineage of element " + TLogic.elementToString(element));
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

	/* END */
	{
		"name": 'END', 
		"doCommand": function(request, response, blockLocal, command)
		{
			response.trace(request, "Throwing end interrupt...");
			throw TAMEInterrupt.End();
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
			"name": 'TEXTF', 
			"doCommand": function(request, response, blockLocal, command)
			{
				var value = request.popValue();
				
				if (!TValue.isLiteral(value))
					throw TAMEError.UnexpectedValueType("Expected literal type in TEXTFLN call.");

				response.addCue(TAMEConstants.Cue.TEXTF, TValue.asString(value) + '\n');
			}
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

			response.addCue(TAMEConstants.Cue.TEXTF, TValue.asLong(value));
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

	/* STRLEN */
	{
		"name": 'STRLEN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value = request.popValue();
			
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type in STRLEN call.");

			request.pushValue(TValue.createInteger(TValue.asString(value).length));
		}
	},

	/* STRREPLACE */
	{
		"name": 'STRREPLACE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* STRREPLACEPATTERN */
	{
		"name": 'STRREPLACEPATTERN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* STRREPLACEPATTERNALL */
	{
		"name": 'STRREPLACEPATTERNALL', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* STRINDEX */
	{
		"name": 'STRINDEX', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* STRLASTINDEX */
	{
		"name": 'STRLASTINDEX', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* STRCONTAINS */
	{
		"name": 'STRCONTAINS', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* STRCONTAINSPATTERN */
	{
		"name": 'STRCONTAINSPATTERN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* STRCONTAINSTOKEN */
	{
		"name": 'STRCONTAINSTOKEN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* SUBSTR */
	{
		"name": 'SUBSTR', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* STRLOWER */
	{
		"name": 'STRLOWER', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* STRUPPER */
	{
		"name": 'STRUPPER', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* STRCHAR */
	{
		"name": 'STRCHAR', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* FLOOR */
	{
		"name": 'FLOOR', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* CEILING */
	{
		"name": 'CEILING', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* ROUND */
	{
		"name": 'ROUND', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* FIX */
	{
		"name": 'FIX', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* SQRT */
	{
		"name": 'SQRT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* PI */
	{
		"name": 'PI', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* E */
	{
		"name": 'E', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* SIN */
	{
		"name": 'SIN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* COS */
	{
		"name": 'COS', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* TAN */
	{
		"name": 'TAN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* MIN */
	{
		"name": 'MIN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* MAX */
	{
		"name": 'MAX', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* CLAMP */
	{
		"name": 'CLAMP', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* RANDOM */
	{
		"name": 'RANDOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* FRANDOM */
	{
		"name": 'FRANDOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* GRANDOM */
	{
		"name": 'GRANDOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* TIME */
	{
		"name": 'TIME', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* SECONDS */
	{
		"name": 'SECONDS', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* MINUTES */
	{
		"name": 'MINUTES', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* HOURS */
	{
		"name": 'HOURS', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* DAYS */
	{
		"name": 'DAYS', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* FORMATTIME */
	{
		"name": 'FORMATTIME', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* OBJECTHASNAME */
	{
		"name": 'OBJECTHASNAME', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* OBJECTHASTAG */
	{
		"name": 'OBJECTHASTAG', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* ADDOBJECTNAME */
	{
		"name": 'ADDOBJECTNAME', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* ADDOBJECTTAG */
	{
		"name": 'ADDOBJECTTAG', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* ADDOBJECTTAGTOALLIN */
	{
		"name": 'ADDOBJECTTAGTOALLIN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* REMOVEOBJECTNAME */
	{
		"name": 'REMOVEOBJECTNAME', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* REMOVEOBJECTTAG */
	{
		"name": 'REMOVEOBJECTTAG', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* REMOVEOBJECTTAGFROMALLIN */
	{
		"name": 'REMOVEOBJECTTAGFROMALLIN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* GIVEOBJECT */
	{
		"name": 'GIVEOBJECT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* REMOVEOBJECT */
	{
		"name": 'REMOVEOBJECT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* MOVEOBJECTSWITHTAG */
	{
		"name": 'MOVEOBJECTSWITHTAG', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* OBJECTCOUNT */
	{
		"name": 'OBJECTCOUNT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* HASOBJECT */
	{
		"name": 'HASOBJECT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* OBJECTHASNOOWNER */
	{
		"name": 'OBJECTHASNOOWNER', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* PLAYERISINROOM */
	{
		"name": 'PLAYERISINROOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* PLAYERCANACCESSOBJECT */
	{
		"name": 'PLAYERCANACCESSOBJECT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* BROWSE */
	{
		"name": 'BROWSE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* SETPLAYER */
	{
		"name": 'SETPLAYER', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* SETROOM */
	{
		"name": 'SETROOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* PUSHROOM */
	{
		"name": 'PUSHROOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* POPROOM */
	{
		"name": 'POPROOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* SWAPROOM */
	{
		"name": 'SWAPROOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* CURRENTPLAYERIS */
	{
		"name": 'CURRENTPLAYERIS', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* NOCURRENTPLAYER */
	{
		"name": 'NOCURRENTPLAYER', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* CURRENTROOMIS */
	{
		"name": 'CURRENTROOMIS', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* NOCURRENTROOM */
	{
		"name": 'NOCURRENTROOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* QUEUEACTION */
	{
		"name": 'QUEUEACTION', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* QUEUEACTIONSTRING */
	{
		"name": 'QUEUEACTIONSTRING', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* QUEUEACTIONOBJECT */
	{
		"name": 'QUEUEACTIONOBJECT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* QUEUEACTIONOBJECT2 */
	{
		"name": 'QUEUEACTIONOBJECT2', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* IDENTITY */
	{
		"name": 'IDENTITY', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

];

TCommandFunctions.Type =
{
	"NOOP": 0, 
	"POP": 1, 
	"POPVALUE": 2, 
	"POPLOCALVALUE": 3, 
	"POPELEMENTVALUE": 4, 
	"PUSHVALUE": 5, 
	"PUSHELEMENTVALUE": 6, 
	"ARITHMETICFUNC": 7, 
	"IF": 8, 
	"WHILE": 9, 
	"FOR": 10, 
	"CALL": 11, 
	"CALLFROM": 12, 
	"BREAK": 13, 
	"CONTINUE": 14, 
	"QUIT": 15, 
	"END": 16, 
	"ADDCUE": 17, 
	"TEXT": 18, 
	"TEXTLN": 19, 
	"TEXTF": 20, 
	"TEXTFLN": 21, 
	"PAUSE": 22, 
	"WAIT": 23, 
	"TIP": 24, 
	"INFO": 25, 
	"ASBOOLEAN": 26, 
	"ASINT": 27, 
	"ASFLOAT": 28, 
	"ASSTRING": 29, 
	"STRLEN": 30, 
	"STRREPLACE": 31, 
	"STRREPLACEPATTERN": 32, 
	"STRREPLACEPATTERNALL": 33, 
	"STRINDEX": 34, 
	"STRLASTINDEX": 35, 
	"STRCONTAINS": 36, 
	"STRCONTAINSPATTERN": 37, 
	"STRCONTAINSTOKEN": 38, 
	"SUBSTR": 39, 
	"STRLOWER": 40, 
	"STRUPPER": 41, 
	"STRCHAR": 42, 
	"FLOOR": 43, 
	"CEILING": 44, 
	"ROUND": 45, 
	"FIX": 46, 
	"SQRT": 47, 
	"PI": 48, 
	"E": 49, 
	"SIN": 50, 
	"COS": 51, 
	"TAN": 52, 
	"MIN": 53, 
	"MAX": 54, 
	"CLAMP": 55, 
	"RANDOM": 56, 
	"FRANDOM": 57, 
	"GRANDOM": 58, 
	"TIME": 59, 
	"SECONDS": 60, 
	"MINUTES": 61, 
	"HOURS": 62, 
	"DAYS": 63, 
	"FORMATTIME": 64, 
	"OBJECTHASNAME": 65, 
	"OBJECTHASTAG": 66, 
	"ADDOBJECTNAME": 67, 
	"ADDOBJECTTAG": 68, 
	"ADDOBJECTTAGTOALLIN": 69, 
	"REMOVEOBJECTNAME": 70, 
	"REMOVEOBJECTTAG": 71, 
	"REMOVEOBJECTTAGFROMALLIN": 72, 
	"GIVEOBJECT": 73, 
	"REMOVEOBJECT": 74, 
	"MOVEOBJECTSWITHTAG": 75, 
	"OBJECTCOUNT": 76, 
	"HASOBJECT": 77, 
	"OBJECTHASNOOWNER": 78, 
	"PLAYERISINROOM": 79, 
	"PLAYERCANACCESSOBJECT": 80, 
	"BROWSE": 81, 
	"SETPLAYER": 82, 
	"SETROOM": 83, 
	"PUSHROOM": 84, 
	"POPROOM": 85, 
	"SWAPROOM": 86, 
	"CURRENTPLAYERIS": 87, 
	"NOCURRENTPLAYER": 88, 
	"CURRENTROOMIS": 89, 
	"NOCURRENTROOM": 90, 
	"QUEUEACTION": 91, 
	"QUEUEACTIONSTRING": 92, 
	"QUEUEACTIONOBJECT": 93, 
	"QUEUEACTIONOBJECT2": 94, 
	"IDENTITY": 95, 
};

	/*
		NOOP (null)
		POP (true)
		POPVALUE (true)
		POPLOCALVALUE (true)
		POPELEMENTVALUE (true)
		PUSHVALUE (true)
		PUSHELEMENTVALUE (true)
		ARITHMETICFUNC (true)
		IF (true, true)
		WHILE (true, true)
		FOR (true, true)
		CALL (null, ArgumentType.VALUE)
		CALLFROM (null, ArgumentType.ELEMENT, ArgumentType.VALUE)
		BREAK ()
		CONTINUE ()
		QUIT ()
		END ()
		ADDCUE (null, ArgumentType.VALUE, ArgumentType.VALUE)
		TEXT (null, ArgumentType.VALUE)
		TEXTLN (null, ArgumentType.VALUE)
		TEXTF (null, ArgumentType.VALUE)
		TEXTFLN (null, ArgumentType.VALUE)
		PAUSE (null)
		WAIT (null, ArgumentType.VALUE)
		TIP (null, ArgumentType.VALUE)
		INFO (null, ArgumentType.VALUE)
		ASBOOLEAN (ArgumentType.VALUE, ArgumentType.VALUE)
		ASINT (ArgumentType.VALUE, ArgumentType.VALUE)
		ASFLOAT (ArgumentType.VALUE, ArgumentType.VALUE)
		ASSTRING (ArgumentType.VALUE, ArgumentType.VALUE)
		STRLEN (ArgumentType.VALUE, ArgumentType.VALUE)
		STRREPLACE (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		STRINDEX (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		STRLASTINDEX (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		STRCONTAINS (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		STRCONTAINSPATTERN (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		STRCONTAINSTOKEN (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		SUBSTR (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		STRLOWER (ArgumentType.VALUE, ArgumentType.VALUE)
		STRUPPER (ArgumentType.VALUE, ArgumentType.VALUE)
		STRCHAR (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		FLOOR (ArgumentType.VALUE, ArgumentType.VALUE)
		CEILING (ArgumentType.VALUE, ArgumentType.VALUE)
		ROUND (ArgumentType.VALUE, ArgumentType.VALUE)
		FIX (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		SQRT (ArgumentType.VALUE, ArgumentType.VALUE)
		PI (ArgumentType.VALUE)
		E (ArgumentType.VALUE)
		SIN (ArgumentType.VALUE, ArgumentType.VALUE)
		COS (ArgumentType.VALUE, ArgumentType.VALUE)
		TAN (ArgumentType.VALUE, ArgumentType.VALUE)
		MIN (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		MAX (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		CLAMP (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		RANDOM (ArgumentType.VALUE, ArgumentType.VALUE)
		FRANDOM (ArgumentType.VALUE)
		GRANDOM (ArgumentType.VALUE)
		TIME (ArgumentType.VALUE)
		SECONDS (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		MINUTES (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		HOURS (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		DAYS (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		FORMATTIME (ArgumentType.VALUE, ArgumentType.VALUE, ArgumentType.VALUE)
		OBJECTHASNAME (ArgumentType.VALUE, ArgumentType.OBJECT, ArgumentType.VALUE)
		OBJECTHASTAG (ArgumentType.VALUE, ArgumentType.OBJECT, ArgumentType.VALUE)
		ADDOBJECTNAME (null, ArgumentType.OBJECT, ArgumentType.VALUE)
		ADDOBJECTTAG (null, ArgumentType.OBJECT, ArgumentType.VALUE)
		ADDOBJECTTAGTOALLIN (null, ArgumentType.OBJECT_CONTAINER, ArgumentType.VALUE)
		REMOVEOBJECTNAME (null, ArgumentType.OBJECT, ArgumentType.VALUE)
		REMOVEOBJECTTAG (null, ArgumentType.OBJECT, ArgumentType.VALUE)
		REMOVEOBJECTTAGFROMALLIN (null, ArgumentType.OBJECT_CONTAINER, ArgumentType.VALUE)
		GIVEOBJECT (null, ArgumentType.OBJECT_CONTAINER, ArgumentType.OBJECT)
		REMOVEOBJECT (null, ArgumentType.OBJECT)
		MOVEOBJECTSWITHTAG (null, ArgumentType.OBJECT_CONTAINER, ArgumentType.OBJECT_CONTAINER, ArgumentType.VALUE)
		OBJECTCOUNT (ArgumentType.VALUE, ArgumentType.OBJECT_CONTAINER)
		HASOBJECT (ArgumentType.VALUE, ArgumentType.OBJECT_CONTAINER, ArgumentType.OBJECT)
		OBJECTHASNOOWNER (ArgumentType.VALUE, ArgumentType.OBJECT)
		PLAYERISINROOM (ArgumentType.VALUE, ArgumentType.PLAYER, ArgumentType.ROOM)
		PLAYERCANACCESSOBJECT (ArgumentType.VALUE, ArgumentType.PLAYER, ArgumentType.OBJECT)
		BROWSE (null, ArgumentType.OBJECT_CONTAINER)
		SETPLAYER (null, ArgumentType.PLAYER)
		SETROOM (null, ArgumentType.ROOM)
		PUSHROOM (null, ArgumentType.ROOM)
		POPROOM (null)
		SWAPROOM (null, ArgumentType.ROOM)
		CURRENTPLAYERIS (ArgumentType.VALUE, ArgumentType.PLAYER)
		NOCURRENTPLAYER (ArgumentType.VALUE)
		CURRENTROOMIS (ArgumentType.VALUE, ArgumentType.ROOM)
		NOCURRENTROOM (ArgumentType.VALUE)
		QUEUEACTION (null, ArgumentType.ACTION)
		QUEUEACTIONSTRING (null, ArgumentType.ACTION, ArgumentType.VALUE)
		QUEUEACTIONOBJECT (null, ArgumentType.ACTION, ArgumentType.OBJECT)
		QUEUEACTIONOBJECT2 (null, ArgumentType.ACTION, ArgumentType.OBJECT, ArgumentType.OBJECT)
		IDENTITY (ArgumentType.VALUE, ArgumentType.ELEMENT)
	*/

//##[[CONTENT-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TCommandFunctions;
// =========================================================================
