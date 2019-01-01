/*
	TOperation
	{
		opcode: 0,
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
		tameType: 'TAction',
		identity: 'IDENTITY',
		type: TAction.Type.ordinal(),
		strict: false,
		reversed: false,
		names: ['name',],
		extraStrings:['string',],
	}
	TWorld:
	{
		tameType: 'TWorld',
		identity: 'IDENTITY',
		parent: 'IDENTITY',
		archetype: false,
		blockTable: {},
		functionTable: {},
	}
	TObject:
	{
		tameType: 'TObject',
		identity: 'IDENTITY',
		parent: 'IDENTITY',
		archetype: false,
		blockTable: {},
		functionTable: {},
		names: ['NAME',],
		determiners: ['NAME',],
		tags: ['NAME',]
	}
	TPlayer:
	{
		tameType: 'TPlayer',
		identity: 'IDENTITY',
		parent: 'IDENTITY',
		archetype: false,
		blockTable: {},
		functionTable: {},
	}
	TRoom:
	{
		tameType: 'TRoom',
		identity: 'IDENTITY',
		parent: 'IDENTITY',
		archetype: false,
		blockTable: {},
		functionTable: {},
	}
	TContainer:
	{
		tameType: 'TContainer',
		identity: 'IDENTITY',
		parent: 'IDENTITY'
		archetype: false,
		blockTable: {},
		functionTable: {},
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

