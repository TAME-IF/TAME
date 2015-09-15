package net.mtrop.tame;

import net.mtrop.tame.context.TObjectContext;
import net.mtrop.tame.context.TPlayerContext;
import net.mtrop.tame.context.TRoomContext;
import net.mtrop.tame.context.TWorldContext;
import net.mtrop.tame.exception.ModuleStateException;
import net.mtrop.tame.exception.UnexpectedValueTypeException;
import net.mtrop.tame.interrupt.ErrorInterrupt;
import net.mtrop.tame.interrupt.QuitInterrupt;
import net.mtrop.tame.interrupt.TAMEInterrupt;
import net.mtrop.tame.lang.ArgumentType;
import net.mtrop.tame.lang.CommandType;
import net.mtrop.tame.lang.command.Statement;
import net.mtrop.tame.struct.Value;
import net.mtrop.tame.struct.ValueType;

/**
 * The set of commands.
 * Values in arguments are popped in reverse order on call, if arguments are taken. 
 * @author Matthew Tropiano
 */
public enum TAMECommand implements CommandType, TAMEConstants
{
	
	/**
	 * [INTERNAL] Sets a variable.
	 * Assigns a value to a variable in the topmost context in the execution.
	 * Returns the assigned value.
	 */
	SETVARIABLE (true, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.VARIABLE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Statement statement) 
		{
			Value value = request.popValue();
			Value varvalue = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in SETVARIABLE call.");
			if (!varvalue.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in SETVARIABLE call.");
			
			String variableName = varvalue.asString();
			request.peekContext().setValue(variableName, value);
			
			// return
			request.pushValue(value);
		}
		
	},
	
	/**
	 * [INTERNAL] Sets a variable.
	 * Assigns a value to a variable in an object.
	 * Returns the assigned value.
	 */
	SETOBJECTVARIABLE (true, /*Return: */ ArgumentType.VALUE, /*Args: */ ArgumentType.OBJECT, ArgumentType.VARIABLE, ArgumentType.VALUE)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Statement statement) throws ErrorInterrupt
		{
			Value value = request.popValue();
			Value varvalue = request.popValue();
			Value varobject = request.popValue();
			
			if (!value.isLiteral())
				throw new UnexpectedValueTypeException("Expected literal type in SETOBJECTVARIABLE call.");
			if (!varvalue.isVariable())
				throw new UnexpectedValueTypeException("Expected variable type in SETOBJECTVARIABLE call.");
			if (varobject.getType() != ValueType.OBJECT)
				throw new UnexpectedValueTypeException("Expected object type in SETOBJECTVARIABLE call.");
			
			String variableName = varvalue.asString();
			String objectName = varobject.asString();

			resolveObject(request, objectName).setValue(variableName, value);
			
			// return
			request.pushValue(value);
		}
		
	},

	// TODO: Finish SET commands, ARITHPUSH/POP/FUNC.

	/**
	 * Adds a QUIT cue to the response and throws a quit interrupt.
	 * Is keyword. Returns nothing. 
	 */
	QUIT (/*Return: */ null)
	{
		@Override
		public void execute(TAMERequest request, TAMEResponse response, Statement statement) throws TAMEInterrupt
		{
			response.trace(request, "Throwing quit...");
			response.addCue(CUE_QUIT);
			throw new QuitInterrupt();
		}
		
	},
	
	// TODO: Finish this.
	
	;
	
	private boolean internal;
	private boolean keyword;
	private ArgumentType returnType;
	private ArgumentType[] argumentTypes;
	private boolean initializationBlockRequired;
	private boolean conditionalBlockRequired;
	private boolean stepBlockRequired;
	private boolean successBlockRequired;
	private boolean failureBlockRequired;
	
	private TAMECommand()
	{
		this(false, false, false, false, false, false, null, null);
	}

	private TAMECommand(ArgumentType returnType)
	{
		this(false, false, false, false, false, false, returnType, null);
	}

	private TAMECommand(ArgumentType returnType, ArgumentType ... argumentTypes)
	{
		this(false, false, false, false, false, false, returnType, argumentTypes);
	}

	private TAMECommand(boolean internal, ArgumentType returnType, ArgumentType ... argumentTypes)
	{
		this(internal, false, false, false, false, false, returnType, argumentTypes);
	}

	private TAMECommand
	(
		boolean initializationBlockRequired, boolean conditionalBlockRequired, boolean stepBlockRequired, boolean successBlockRequired, boolean failureBlockRequired,
		ArgumentType returnType, ArgumentType[] argumentTypes
	)
	{
		this(false, initializationBlockRequired, conditionalBlockRequired, stepBlockRequired, successBlockRequired, failureBlockRequired, returnType, argumentTypes);
	}

	private TAMECommand
	(
		boolean internal, 
		boolean initializationBlockRequired, boolean conditionalBlockRequired, boolean stepBlockRequired, boolean successBlockRequired, boolean failureBlockRequired,
		ArgumentType returnType, ArgumentType[] argumentTypes
	)
	{
		this.internal = internal;
		this.returnType = returnType;
		this.argumentTypes = argumentTypes;
		this.initializationBlockRequired = initializationBlockRequired;
		this.conditionalBlockRequired = conditionalBlockRequired;
		this.stepBlockRequired = stepBlockRequired;
		this.successBlockRequired = successBlockRequired;
		this.failureBlockRequired = failureBlockRequired;
	}

	@Override
	public boolean isInternal() 
	{
		return internal;
	}
	
	@Override
	public ArgumentType getReturnType()
	{
		return returnType;
	}

	@Override
	public ArgumentType[] getArgumentTypes() 
	{
		return argumentTypes;
	}
	
	@Override
	public boolean isKeyword()
	{
		return keyword;
	}

	/**
	 * Returns if this requires more than one block with the statement.
	 * @return true if so, false if not.
	 */
	public boolean isEvaluating() 
	{
		return 
			isInitializationBlockRequired()
			|| isConditionalBlockRequired()
			|| isStepBlockRequired()
			|| isSuccessBlockRequired()
			|| isFailureBlockRequired();
	}
	
	@Override
	public boolean isInitializationBlockRequired() 
	{
		return initializationBlockRequired;
	}
	
	@Override
	public boolean isConditionalBlockRequired()
	{
		return conditionalBlockRequired;
	}
	
	@Override
	public boolean isStepBlockRequired() 
	{
		return stepBlockRequired;
	}
	
	@Override
	public boolean isSuccessBlockRequired() 
	{
		return successBlockRequired;
	}
	
	@Override
	public boolean isFailureBlockRequired() 
	{
		return failureBlockRequired;
	}

	@Override
	public void execute(TAMERequest request, TAMEResponse response, Statement statement) throws TAMEInterrupt
	{
		throw new RuntimeException("UNIMPLEMENTED COMMAND");
	}
	
	/**
	 * Resolves a world.
	 * @param request the request object.
	 * @return the context resolved.
	 */
	protected TWorldContext resolveWorld(TAMERequest request)
	{
		return request.getModuleContext().getWorldContext();
	}

	/**
	 * Resolves a player.
	 * @param request the request object.
	 * @param playerIdentity the player identity.
	 * @return the context resolved.
	 * @throws ErrorInterrupt if no current player when requested.
	 * @throws ModuleStateException if the non-current player identity cannot be found.
	 */
	protected TPlayerContext resolvePlayer(TAMERequest request, String playerIdentity) throws ErrorInterrupt
	{
		TPlayerContext context = null;
		if (playerIdentity.equals(IDENTITY_CURRENT_PLAYER))
		{
			context = request.getModuleContext().getCurrentPlayerContext();
			if (context == null)
				throw new ErrorInterrupt("Current player context called with no current player!");
		}
		else
		{
			context = request.getModuleContext().getPlayerContextByIdentity(playerIdentity);
			if (context == null)
				throw new ModuleStateException("Expected player '%s' in module context!", playerIdentity);
		}
		
		return context;
	}

	/**
	 * Resolves a room.
	 * @param request the request object.
	 * @param roomIdentity the roomIdentity.
	 * @return the context resolved.
	 * @throws ErrorInterrupt if no current room when requested.
	 * @throws ModuleStateException if the non-current room identity cannot be found.
	 */
	protected TRoomContext resolveRoom(TAMERequest request, String roomIdentity) throws ErrorInterrupt
	{
		TRoomContext context = null;
		if (roomIdentity.equals(IDENTITY_CURRENT_ROOM))
		{
			context = request.getModuleContext().getCurrentRoomContext();
			if (context == null)
				throw new ErrorInterrupt("Current room context called with no current room!");
		}
		else
		{
			context = request.getModuleContext().getRoomContextByIdentity(roomIdentity);
			if (context == null)
				throw new ModuleStateException("Expected room '%s' in module context!", roomIdentity);
		}
		
		return context;
	}

	/**
	 * Resolves a object.
	 * @param request the request object.
	 * @param objectIdentity the object identity.
	 * @return the context resolved.
	 * @throws ModuleStateException if object not found.
	 */
	protected TObjectContext resolveObject(TAMERequest request, String objectIdentity) throws ErrorInterrupt
	{
		TObjectContext context = request.getModuleContext().getObjectContextByIdentity(objectIdentity);
		if (context == null)
			throw new ModuleStateException("Expected object '%s' in module context!", objectIdentity);
		return context;
	}

	/**
	 * Resolves a variable from the topmost element context.
	 * @param variableName the variable name.
	 * @return the value resolved.
	 */
	protected Value resolveVariableValue(TAMERequest request, String variableName)
	{
		return request.peekContext().getValue(variableName);
	}

	/**
	 * Resolves a variable from the world context element.
	 * @param request the request object.
	 * @param variableName the variable name.
	 * @return the value resolved.
	 */
	protected Value resolveWorldVariableValue(TAMERequest request, String variableName)
	{
		return resolveWorld(request).getValue(variableName);
	}

	/**
	 * Resolves a variable from a player context element.
	 * @param request the request object.
	 * @param playerIdentity a player identity.
	 * @param variableName the variable name.
	 * @return the value resolved.
	 * @throws ErrorInterrupt 
	 */
	protected Value resolvePlayerVariableValue(TAMERequest request, String playerIdentity, String variableName) throws ErrorInterrupt
	{
		return resolvePlayer(request, playerIdentity).getValue(variableName);
	}

	/**
	 * Resolves a variable from a room context element.
	 * @param request the request object.
	 * @param roomIdentity a room identity.
	 * @param variableName the variable name.
	 * @return the value resolved.
	 */
	protected Value resolveRoomVariableValue(TAMERequest request, String roomIdentity, String variableName) throws ErrorInterrupt
	{
		return resolveRoom(request, roomIdentity).getValue(variableName);
	}

	/**
	 * Resolves a variable from an object context element.
	 * @param request the request object.
	 * @param objectIdentity an object identity.
	 * @param variableName the variable name.
	 * @return the value resolved.
	 */
	protected Value resolveObjectVariableValue(TAMERequest request, String objectIdentity, String variableName) throws ErrorInterrupt
	{
		return resolveObject(request, objectIdentity).getValue(variableName);
	}

}
