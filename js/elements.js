/*
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
	Context: 
	{
		"module": module,	// module reference
		"player": null,		// current player
		"elements": {}, 	// element-to-variables
		"owners": {}, 		// element-to-objects
		"objectOwners": {}, // object-to-element
		"roomStacks": {},	// player-to-rooms
		"names": {},		// object-to-names
		"tags": {},			// object-to-tags
	}

*/

