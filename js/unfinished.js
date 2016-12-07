/*
	TAction:
	{
		identity: 'IDENTITY',
		type: TAME.ActionType,
		names: ['NAME',],
		restricted: false
	}
	TWorld:
	{
		identity: 'IDENTITY',
		blockTable: {},
		parent: {}
	}
	TObject:
	{
		identity: 'IDENTITY',
		blockTable: {},
		parent: {},
		names: ['NAME',],
		tags: ['NAME',]
	}
	TPlayer:
	{
		identity: 'IDENTITY',
		blockTable: {},
		parent: {},
		permissionType: TAME.RestrictionType, 
		permittedActionList: []
	}
	TRoom:
	{
		identity: 'IDENTITY',
		blockTable: {},
		parent: {},
		permissionType: TAME.RestrictionType, 
		permittedActionList: []
	}
	TContainer:
	{
		identity: 'IDENTITY',
		blockTable: {},
		parent: {}
	}
*/

/*****************************************************************************
 Constants.
 *****************************************************************************/
var TAMEConstants = TAMEConstants || {
	
	/* Enum for TAMECommand symbols. */
	"CommandType": {
	},

	/* Enum for ArithmeticFunc symbols. */
	"ArithmeticType": {
	},
	
	"ActionType": {
		"GENERAL": 1,
		"TRANSITIVE": 2,
		"DITRANSITIVE": 3,
		"MODAL": 4,
		"OPEN": 5
	},

	"RestrictionType": {
		"FORBID": 1,
		"ALLOW": 2
	},

};


/*****************************************************************************
 See net.mtrop.tame.TAMECommand
 Just doCommand() functions. No need for script metadata.
 *****************************************************************************/
var TCommandFunctions = TCommandFunctions || [

	/* NOOP */
	function(request, response, blockLocal, command)
	{
		// Nothing.
	},

];

/****************************************************
 See net.mtrop.tame.lang.Command
 ****************************************************/
var TCommand = TCommand || function(commandfunc, operand0, operand1, ifBlock, conditionalBlock, stepBlock, successBlock, failureBlock)
{
	this.commandfunc = commandfunc;
	this.operand0 = operand0;
	this.operand1 = operand1;
	this.ifBlock = ifBlock;
	this.conditionalBlock = conditionalBlock;
	this.stepBlock = stepBlock;
	this.successBlock = successBlock;
	this.failureBlock = failureBlock;
}

/**
 * Executes the command.
 */
TCommand.prototype.execute = function(request, response, blockLocal)
{
	commandfunc(request, response, blockLocal, this);
}

/****************************************************
 See net.mtrop.tame.lang.Block
 ****************************************************/
var TBlock = TBlock || function(commandList)
{
	this.commandList = commandList;
}

TBlock.prototype.execute = function(request, response, locals)
{
	response.trace(request, "Start block.");
	_each(this.commandList, function(command){
		response.trace(request, "CALL %s", command);
		command.execute(request, response, blockLocal);
	});
	response.trace(request, "End block.");
}


/****************************************************
 Constructor for the TAME Module.
 ****************************************************/
(function TAMEModule(header, tactions, tworld, tobjects, tplayers, trooms, tcontainers)
{
	// Smarter foreach.
	var _each = function(obj, func)
	{
		for (x in obj) 
			if (obj.hasOwnProperty(x)) 
				func(obj[x], x, obj.length);
	}

	// Mapify - [object, ...] to {object.memberKey -> object, ...}
	var _mapify = function(objlist, memberKey, multi) 
	{
		var out = {}; 
		for (x in objlist) 
			if (objlist.hasOwnProperty(x))
			{				
				var chain = out[objlist[x][memberKey]];
				if (multi && chain)
				{
					if (Object.prototype.toString.call(chain) === '[object Array]')
						chain.push(objlist[x]); 
					else
						out[objlist[x][memberKey]] = [out[objlist[x][memberKey]], objlist[x]]; 
				}
				else
					out[objlist[x][memberKey]] = objlist[x]; 
			}
		return out;
	}

	// Pairify - [object, ...] to {object.memberKey -> object.memberValue, ...}
	var _pairify = function(objlist, memberKey, memberValue, multi) 
	{
		var out = {}; 
		for (x in objlist) 
			if (objlist.hasOwnProperty(x))
			{				
				var chain = out[objlist[x][memberKey]];
				if (multi && chain)
				{
					if (Object.prototype.toString.call(chain) === '[object Array]')
						chain.push(objlist[x][memberValue]); 
					else
						out[objlist[x][memberKey]] = [out[objlist[x][memberKey]], objlist[x][memberValue]]; 
				}
				else
					out[objlist[x][memberKey]] = objlist[x][memberValue]; 
			}
		return out;
	}

	
	// Fields --------------------
	this.header = theader;
	this.actions = _mapify(tactions, "identity");
	this.objects = _mapify(tobjects, "identity");
	this.players = _mapify(tplayers, "identity");
	this.rooms = _mapify(trooms, "identity");
	this.containers = _mapify(tcontainers, "identity");
	this.world = tworld;
	var at = this.actionNameTable = {};
	_each(actions, function(action){
		_each(action.names, function(name){
			at[name] = action.identity;
		});
	});
	// ---------------------------
	
})(/* MODULE SHIT GOES HERE - header, actions, world, objects, players, rooms, containers */);
