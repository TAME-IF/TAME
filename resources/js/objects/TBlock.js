/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

//##[[CONTENT-START

/****************************************************
 See net.mtrop.tame.lang.Block
 ****************************************************/
var TBlock = function(commandList)
{
	this.commandList = commandList;
};

TBlock.prototype.execute = function(request, response, locals)
{
	response.trace(request, "Start block.");
	_each(this.commandList, function(command){
		response.trace(request, "CALL %s", command);
		command.execute(request, response, blockLocal);
	});
	response.trace(request, "End block.");
};

//##[[CONTENT-END


//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TBlock;
// =========================================================================
