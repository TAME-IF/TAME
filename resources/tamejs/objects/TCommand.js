/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

// REQUIREMENTS =========================================================================================
var TCommandFunctions = TCommandFunctions || ((typeof require) !== 'undefined' ? require('../logic/TCommandFunctions.js') : null);
// ======================================================================================================

//##[[CONTENT-START

/****************************************************
 See net.mtrop.tame.lang.Command
 ****************************************************/
var TCommand = function(commandIndex, operand0, operand1, initBlock, conditionalBlock, stepBlock, successBlock, failureBlock)
{
	this.commandIndex = commandIndex;
	this.operand0 = operand0;
	this.operand1 = operand1;
	this.initBlock = initBlock;
	this.conditionalBlock = conditionalBlock;
	this.stepBlock = stepBlock;
	this.successBlock = successBlock;
	this.failureBlock = failureBlock;
}

// Convenience constructors.
TCommand.create = function (commandIndex) { return new TCommand(commandIndex); };
TCommand.createOp1 = function (commandIndex, operand0) { return new TCommand(commandIndex, operand0); };
TCommand.createOp2 = function (commandIndex, operand0, operand1) { return new TCommand(commandIndex, operand0, operand1); };
TCommand.createIf = function (conditionalBlock, successBlock) { return new TCommand(TCommandFunctions.Type.IF, null, null, null, conditionalBlock, null, successBlock); };
TCommand.createIfElse = function (conditionalBlock, successBlock, failureBlock) { return new TCommand(TCommandFunctions.Type.IF, null, null, null, conditionalBlock, null, successBlock, failureBlock); };
TCommand.createWhile = function (conditionalBlock, successBlock) { return new TCommand(TCommandFunctions.Type.WHILE, null, null, null, conditionalBlock, null, successBlock); };
TCommand.createFor = function (initBlock, conditionalBlock, stepBlock, successBlock) { return new TCommand(TCommandFunctions.Type.FOR, null, null, initBlock, conditionalBlock, stepBlock, successBlock); };

/**
 * Executes the command.
 */
TCommand.prototype.execute = function(request, response, blockLocal)
{
	TCommandFunctions.execute(this.commandIndex, request, response, blockLocal, this);
};

//##[[CONTENT-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TCommand;
// =========================================================================
