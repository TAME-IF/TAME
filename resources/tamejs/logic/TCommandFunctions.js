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
var Util = Util || ((typeof require) !== 'undefined' ? require('../Util.js') : null);
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
			if (!TValue.isVariable(varvalue))
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

			var block = request.moduleContext.resolveBlock(element.identity, 'PROCEDURE', [procedureName]);
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
			
			var token = TValue.asString(value2);
			var str = TValue.asString(value1);

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

			request.pushValue(TValue.createBoolean(str.startsWith(sequence)));
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

			request.pushValue(TValue.createBoolean(str.endsWith(sequence)));
		}
	},

	/* SUBSTR */
	{
		"name": 'SUBSTR', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var value3 = request.popValue();
			var value2 = request.popValue();
			var value1 = request.popValue();

			if (!TValue.isLiteral(value1))
				throw TAMEError.UnexpectedValueType("Expected literal type in SUBSTR call.");
			if (!TValue.isLiteral(value2))
				throw TAMEError.UnexpectedValueType("Expected literal type in SUBSTR call.");
			if (!TValue.isLiteral(value3))
				throw TAMEError.UnexpectedValueType("Expected literal type in SUBSTR call.");

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
					request.pushValue(Value.createInteger(0));
				else
				{
					var v = Math.floor(Math.random() * Math.abs(value));
					if (value < 0)
						request.pushValue(Value.createInteger(-v));
					else
						request.pushValue(Value.createInteger(v));
				}
			}
			else
			{
				var value = TValue.asDouble(value1);
				if (value == 0.0)
					request.pushValue(Value.createFloat(0.0));
				else
					request.pushValue(Value.createFloat(Math.random() * value));
			}
		}
	},

	/* FRANDOM */
	{
		"name": 'FRANDOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			request.pushValue(Value.createFloat(Math.random()));
		}
	},

	/* GRANDOM */
	{
		"name": 'GRANDOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// Box-Muller Approximate algorithm c/o Maxwell Collard on StackOverflow
		    var u = 1 - Math.random();
		    var v = 1 - Math.random();
			request.pushValue(Value.createFloat(Math.sqrt(-2.0 * Math.log(u)) * Math.cos(2.0 * Math.PI * v)));
		}
	},

	/* TIME */
	{
		"name": 'TIME', 
		"doCommand": function(request, response, blockLocal, command)
		{
			request.pushValue(Value.createInteger(Date.now()));
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

			request.pushValue(Value.createInteger((second - first) / 1000));
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

			request.pushValue(Value.createInteger((second - first) / (1000 * 60)));
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

			request.pushValue(Value.createInteger((second - first) / (1000 * 60 * 60)));
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

			request.pushValue(Value.createInteger((second - first) / (1000 * 60 * 60 * 24)));
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

			request.pushValue(Value.createString(Util.formatDate(date, format, false)));
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

			request.pushValue(TValue.createBoolean(request.moduleContext.addObjectName(TValue.asString(varObject), TValue.asString(nameValue))));
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

			request.pushValue(TValue.createBoolean(request.moduleContext.addObjectTag(TValue.asString(varObject), TValue.asString(tagValue))));
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

			var element = request.moduleContext.resolveElement(TValue.asString(elementValue));
			
			var tag = TValue.asString(tagValue);
			Util.each(request.moduleContext.getObjectsOwnedByElement(element.identity), function(objectIdentity){
				request.moduleContext.addObjectTag(objectIdentity, tag);
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

			request.pushValue(TValue.createBoolean(request.moduleContext.removeObjectName(TValue.asString(varObject), TValue.asString(nameValue))));
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

			request.pushValue(TValue.createBoolean(request.moduleContext.removeObjectTag(TValue.asString(varObject), TValue.asString(tagValue))));
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

			var element = request.moduleContext.resolveElement(TValue.asString(elementValue));
			
			var tag = TValue.asString(tagValue);
			Util.each(request.moduleContext.getObjectsOwnedByElement(element.identity), function(objectIdentity){
				request.moduleContext.removeObjectTag(objectIdentity, tag);
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
			var elementValue = request.popValue();

			if (!TValue.isLiteral(tagValue))
				throw TAMEError.UnexpectedValueType("Expected literal type in MOVEOBJECTSWITHTAG call.");
			if (!TValue.isObjectContainer(elementValue))
				throw TAMEError.UnexpectedValueType("Expected object-container type in MOVEOBJECTSWITHTAG call.");

			var element = request.moduleContext.resolveElement(TValue.asString(elementValue));
			
			var tag = TValue.asString(tagValue);
			Util.each(request.moduleContext.getObjectsOwnedByElement(element.identity), function(objectIdentity){
				request.moduleContext.addObjectToElement(objectIdentity, element.identity);
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

			request.pushValue(TValue.createBoolean(context.checkPlayerIsInRoom(player, room)))
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

			if (element.tameType === 'TContainer')
				TLogic.doBrowse(request, response, 'ONCONTAINERBROWSE', element.identity);
			else if (element.tameType === 'TPlayer')
				TLogic.doBrowse(request, response, 'ONPLAYERBROWSE', element.identity);
			else if (element.tameType === 'TRoom')
				TLogic.doBrowse(request, response, 'ONROOMBROWSE', element.identity);
			else if (element.tameType === 'TWorld')
				TLogic.doBrowse(request, response, 'ONWORLDBROWSE', element.identity);
			else
				throw TAMEError.UnexpectedValueType("INTERNAL ERROR IN BROWSE.");
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

			request.moduleContext.setCurrentPlayer(player.identity);
		}
	},

	/* SETROOM */
	{
		"name": 'SETROOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varRoom = request.popValue();

			if (!TValue.isRoom(varRoom))
				throw TAMEError.UnexpectedValueType("Expected room type in SETROOM call.");

			var context = request.moduleContext;
			var player = context.getCurrentPlayer();
			
			if (!player)
				throw TAMEInterrupt.Error("No current player!");

			var room = context.resolveElement(TValue.asString(varRoom));
			TLogic.doRoomSwitch(request, response, player.identity, room.identity);
		}
	},

	/* PUSHROOM */
	{
		"name": 'PUSHROOM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			var varRoom = request.popValue();

			if (!TValue.isRoom(varRoom))
				throw TAMEError.UnexpectedValueType("Expected room type in PUSHROOM call.");

			var context = request.moduleContext;
			var player = context.getCurrentPlayer();
			
			if (!player)
				throw TAMEInterrupt.Error("No current player!");

			var room = context.resolveElement(TValue.asString(varRoom));
			TLogic.doRoomPush(request, response, player.identity, room.identity);
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
	"STRSTARTSWITH": 39, 
	"STRENDSWITH": 40, 
	"SUBSTR": 41, 
	"STRLOWER": 42, 
	"STRUPPER": 43, 
	"STRCHAR": 44, 
	"STRTRIM": 45, 
	"FLOOR": 46, 
	"CEILING": 47, 
	"ROUND": 48, 
	"FIX": 49, 
	"SQRT": 50, 
	"PI": 51, 
	"E": 52, 
	"SIN": 53, 
	"COS": 54, 
	"TAN": 55, 
	"MIN": 56, 
	"MAX": 57, 
	"CLAMP": 58, 
	"RANDOM": 59, 
	"FRANDOM": 60, 
	"GRANDOM": 61, 
	"TIME": 62, 
	"SECONDS": 63, 
	"MINUTES": 64, 
	"HOURS": 65, 
	"DAYS": 66, 
	"FORMATTIME": 67, 
	"OBJECTHASNAME": 68, 
	"OBJECTHASTAG": 69, 
	"ADDOBJECTNAME": 70, 
	"ADDOBJECTTAG": 71, 
	"ADDOBJECTTAGTOALLIN": 72, 
	"REMOVEOBJECTNAME": 73, 
	"REMOVEOBJECTTAG": 74, 
	"REMOVEOBJECTTAGFROMALLIN": 75, 
	"GIVEOBJECT": 76, 
	"REMOVEOBJECT": 77, 
	"MOVEOBJECTSWITHTAG": 78, 
	"OBJECTCOUNT": 79, 
	"HASOBJECT": 80, 
	"OBJECTHASNOOWNER": 81, 
	"PLAYERISINROOM": 82, 
	"PLAYERCANACCESSOBJECT": 83, 
	"BROWSE": 84, 
	"SETPLAYER": 85, 
	"SETROOM": 86, 
	"PUSHROOM": 87, 
	"POPROOM": 88, 
	"SWAPROOM": 89, 
	"CURRENTPLAYERIS": 90, 
	"NOCURRENTPLAYER": 91, 
	"CURRENTROOMIS": 92, 
	"NOCURRENTROOM": 93, 
	"QUEUEACTION": 94, 
	"QUEUEACTIONSTRING": 95, 
	"QUEUEACTIONOBJECT": 96, 
	"QUEUEACTIONOBJECT2": 97, 
	"IDENTITY": 98, 
};

//##[[CONTENT-END


// If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TCommandFunctions;
// =========================================================================
