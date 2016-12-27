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
			// block should contain arithmetic commands and a last push.
			var conditional = command.conditionalBlock;
			if (!contitional)
				throw TAMEError.ModuleExecution("Conditional block for IF does NOT EXIST!");
			
			response.trace(request, "Calling IF conditional...");
			TLogic.executeBlock(conditional, request, response, blockLocal);

			// get remaining expression value.
			var value = request.popValue();
			if (!TValue.isLiteral(value))
				throw TAMEError.UnexpectedValueType("Expected literal type after IF conditional block execution.");

			if (TValue.asBoolean(value))
			{
				response.trace(request, "Result "+TValue.toString(value)+" evaluates true.");
				response.trace(request, "Calling IF success block...");
				
				var success = command.successBlock;
				if (!success)
					throw TAMEError.ModuleExecution("Success block for IF does NOT EXIST!");
				TLogic.executeBlock(success, request, response, blockLocal);
			}
			else
			{
				response.trace(request, "Result "+TValue.toString(value)+" evaluates false.");
				var failure = command.failureBlock;
				if (failure)
				{
					response.trace(request, "Calling IF failure block...");
					TLogic.executeBlock(failure, request, response, blockLocal);
				}
			}
		}
	},

	/* WHILE */
	{
		"name": 'WHILE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			while ()
			
			// TODO: Finish this.
			/*
			while (callConditional(request, response, blockLocal, command))
			{
				try {
					response.trace(request, "Calling WHILE success block...");
					Block success = command.getSuccessBlock();
					if (success == null)
						throw new ModuleExecutionException("Success block for WHILE does NOT EXIST!");
					success.execute(request, response, blockLocal);
				} catch (BreakInterrupt interrupt) {
					break;
				} catch (ContinueInterrupt interrupt) {
					continue;
				}
			}
			 */
		}
	},

	/* FOR */
	{
		"name": 'FOR', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* CALL */
	{
		"name": 'CALL', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* CALLFROM */
	{
		"name": 'CALLFROM', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* BREAK */
	{
		"name": 'BREAK', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* CONTINUE */
	{
		"name": 'CONTINUE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* QUIT */
	{
		"name": 'QUIT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* END */
	{
		"name": 'END', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* ADDCUE */
	{
		"name": 'ADDCUE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* TEXT */
	{
		"name": 'TEXT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* TEXTLN */
	{
		"name": 'TEXTLN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* TEXTF */
	{
		"name": 'TEXTF', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* TEXTFLN */
	{
		"name": 'TEXTFLN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* PAUSE */
	{
		"name": 'PAUSE', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* WAIT */
	{
		"name": 'WAIT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* TIP */
	{
		"name": 'TIP', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* INFO */
	{
		"name": 'INFO', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* ASBOOLEAN */
	{
		"name": 'ASBOOLEAN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* ASINT */
	{
		"name": 'ASINT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* ASFLOAT */
	{
		"name": 'ASFLOAT', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* ASSTRING */
	{
		"name": 'ASSTRING', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
		}
	},

	/* STRLEN */
	{
		"name": 'STRLEN', 
		"doCommand": function(request, response, blockLocal, command)
		{
			// TODO: Finish this.
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
	"STRINDEX": 32, 
	"STRLASTINDEX": 33, 
	"STRCONTAINS": 34, 
	"STRCONTAINSPATTERN": 35, 
	"STRCONTAINSTOKEN": 36, 
	"SUBSTR": 37, 
	"STRLOWER": 38, 
	"STRUPPER": 39, 
	"STRCHAR": 40, 
	"FLOOR": 41, 
	"CEILING": 42, 
	"ROUND": 43, 
	"FIX": 44, 
	"SQRT": 45, 
	"PI": 46, 
	"E": 47, 
	"SIN": 48, 
	"COS": 49, 
	"TAN": 50, 
	"MIN": 51, 
	"MAX": 52, 
	"CLAMP": 53, 
	"RANDOM": 54, 
	"FRANDOM": 55, 
	"GRANDOM": 56, 
	"TIME": 57, 
	"SECONDS": 58, 
	"MINUTES": 59, 
	"HOURS": 60, 
	"DAYS": 61, 
	"FORMATTIME": 62, 
	"OBJECTHASNAME": 63, 
	"OBJECTHASTAG": 64, 
	"ADDOBJECTNAME": 65, 
	"ADDOBJECTTAG": 66, 
	"ADDOBJECTTAGTOALLIN": 67, 
	"REMOVEOBJECTNAME": 68, 
	"REMOVEOBJECTTAG": 69, 
	"REMOVEOBJECTTAGFROMALLIN": 70, 
	"GIVEOBJECT": 71, 
	"REMOVEOBJECT": 72, 
	"MOVEOBJECTSWITHTAG": 73, 
	"OBJECTCOUNT": 74, 
	"HASOBJECT": 75, 
	"OBJECTHASNOOWNER": 76, 
	"PLAYERISINROOM": 77, 
	"PLAYERCANACCESSOBJECT": 78, 
	"BROWSE": 79, 
	"SETPLAYER": 80, 
	"SETROOM": 81, 
	"PUSHROOM": 82, 
	"POPROOM": 83, 
	"SWAPROOM": 84, 
	"CURRENTPLAYERIS": 85, 
	"NOCURRENTPLAYER": 86, 
	"CURRENTROOMIS": 87, 
	"NOCURRENTROOM": 88, 
	"QUEUEACTION": 89, 
	"QUEUEACTIONSTRING": 90, 
	"QUEUEACTIONOBJECT": 91, 
	"QUEUEACTIONOBJECT2": 92, 
	"IDENTITY": 93 
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
