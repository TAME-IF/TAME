/*
	TCommand
	{
		commandIndex: 0,
		operand0: TValue,
		operand1: TValue,
		initBlock: [],
		conditionalBlock: [],
		stepBlock: [],
		successBlock: [],
		failureBlock: [],
	}
	TAction:
	{
		identity: 'IDENTITY',
		type: TAME.ActionType,
		names: ['name',],
		extraStrings:['string',],
		restricted: false
	}
	TWorld:
	{
		identity: 'IDENTITY',
		parent: 'IDENTITY'
		blockTable: {},
	}
	TObject:
	{
		identity: 'IDENTITY',
		parent: 'IDENTITY',
		blockTable: {},
		names: ['NAME',],
		tags: ['NAME',]
	}
	TPlayer:
	{
		identity: 'IDENTITY',
		parent: 'IDENTITY',
		blockTable: {},
		permissionType: TAME.RestrictionType, 
		permittedActionList: []
	}
	TRoom:
	{
		identity: 'IDENTITY',
		parent: 'IDENTITY',
		blockTable: {},
		permissionType: TAME.RestrictionType, 
		permittedActionList: []
	}
	TContainer:
	{
		identity: 'IDENTITY',
		parent: 'IDENTITY'
		blockTable: {},
	}
	Context: 
	{
		"module": module,		// module reference
		"state": {
			"player": null,		// current player
			"elements": {}, 	// element-to-variables
			"owners": {}, 		// element-to-objects
			"objectOwners": {}, // object-to-element
			"roomStacks": {},	// player-to-rooms
			"names": {},		// object-to-names
			"tags": {}			// object-to-tags
		}
	}

*/

