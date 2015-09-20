package net.mtrop.tame;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.hash.CaseInsensitiveHash;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;

import net.mtrop.tame.element.TAction;
import net.mtrop.tame.element.TObject;
import net.mtrop.tame.element.TPlayer;
import net.mtrop.tame.element.TRoom;
import net.mtrop.tame.element.TWorld;
import net.mtrop.tame.element.context.TObjectContext;
import net.mtrop.tame.element.context.TOwnershipMap;
import net.mtrop.tame.element.context.TPlayerContext;
import net.mtrop.tame.element.context.TRoomContext;
import net.mtrop.tame.element.context.TWorldContext;
import net.mtrop.tame.exception.ModuleExecutionException;
import net.mtrop.tame.interrupt.ErrorInterrupt;
import net.mtrop.tame.struct.Value;

/**
 * A mutable context for a module.
 * @author Matthew Tropiano
 */
public class TAMEModuleContext implements TAMEConstants
{
	/** A random number generator. */
	private TAMEModule module;

	/** A random number generator. */
	private Random random;

	/** Current active player. */
	protected TPlayer currentPlayer;

	/** World context. */
	protected TWorldContext worldContext;
	/** List of players. */
	protected CaseInsensitiveHashMap<TPlayerContext> playerContextHash;
	/** List of rooms. */
	protected CaseInsensitiveHashMap<TRoomContext> roomContextHash;
	/** List of objects. */
	protected CaseInsensitiveHashMap<TObjectContext> objectContextHash;
	
	/** Ownership map for players. */
	protected TOwnershipMap ownershipMap;

	public TAMEModuleContext(TAMEModule module)
	{
		this.module = module;
		this.random = new Random();
		
		currentPlayer = null;
		playerContextHash = new CaseInsensitiveHashMap<TPlayerContext>(3);
		roomContextHash = new CaseInsensitiveHashMap<TRoomContext>(20);
		objectContextHash = new CaseInsensitiveHashMap<TObjectContext>(40);

		// Build contexts.
		
		worldContext = new TWorldContext(module.getWorld());
		for (ObjectPair<String, TPlayer> element : module.getPlayerList())
			playerContextHash.put(element.getKey(), new TPlayerContext(element.getValue()));
		for (ObjectPair<String, TRoom> element : module.getRoomList())
			roomContextHash.put(element.getKey(), new TRoomContext(element.getValue()));
		for (ObjectPair<String, TObject> element : module.getObjectList())
			objectContextHash.put(element.getKey(), new TObjectContext(element.getValue()));
		
		ownershipMap = new TOwnershipMap();

	}

	/**
	 * Returns the module.
	 */
	public TAMEModule getModule()
	{
		return module;
	}
	
	/**
	 * Gets the context random.
	 */
	public Random getRandom()
	{
		return random;
	}
	
	/**
	 * Reflection method - get available action names according to current context
	 * and filtered by a prefix.
	 */
	public String[] getAvailableActionNames()
	{
		CaseInsensitiveHash hash = new CaseInsensitiveHash();
		Iterator<String> it = module.getActionList().keyIterator();
		while (it.hasNext())
		{
			String actionId = it.next();
			TAction action = module.getActionList().get(actionId);
			TPlayer player = getCurrentPlayerContext() != null ? getCurrentPlayerContext().getElement() : null;
			TRoom room = getCurrentRoomContext() != null ? getCurrentRoomContext().getElement() : null;
			
			if (room != null && !room.allowsAction(action))
				continue;
			if (player != null && !player.allowsAction(action))
				continue;
			
			for (String s : action.getNames())
				hash.put(s);
		}
		
		it = hash.iterator();
		String[] out = new String[hash.size()];
		int i = 0;
		while (it.hasNext())
			out[i++] = it.next();
		Arrays.sort(out);
		return out;
	}

	/**
	 * Returns the world context.
	 */
	public TWorldContext getWorldContext()
	{
		return worldContext;
	}
	
	/**
	 * Gets a player context by player identity.
	 */
	public TPlayerContext getPlayerContext(TPlayer player)
	{
		return playerContextHash.get(player.getIdentity());
	}

	/**
	 * Gets a room context by room identity.
	 */
	public TRoomContext getRoomContext(TRoom room)
	{
		return roomContextHash.get(room.getIdentity());
	}

	/**
	 * Gets a object context by object identity.
	 */
	public TObjectContext getObjectContext(TObject object)
	{
		return objectContextHash.get(object.getIdentity());
	}

	/**
	 * Gets a player context by player name.
	 */
	public TPlayerContext getPlayerContextByIdentity(String name)
	{
		return playerContextHash.get(name);
	}

	/**
	 * Gets a room context by room name.
	 */
	public TRoomContext getRoomContextByIdentity(String name)
	{
		return roomContextHash.get(name);
	}

	/**
	 * Gets a object context by object name.
	 */
	public TObjectContext getObjectContextByIdentity(String name)
	{
		return objectContextHash.get(name);
	}

	/**
	 * Get the player context list.
	 */
	public CaseInsensitiveHashMap<TPlayerContext> getPlayerContextList()
	{
		return playerContextHash;
	}

	/**
	 * Get the room context list.
	 */
	public CaseInsensitiveHashMap<TRoomContext> getRoomContextList()
	{
		return roomContextHash;
	}

	/**
	 * Get the object context list.
	 */
	public CaseInsensitiveHashMap<TObjectContext> getObjectContextList()
	{
		return objectContextHash;
	}

	/**
	 * Gets the ownership map.
	 */
	public TOwnershipMap getOwnershipMap()
	{
		return ownershipMap;
	}

	/**
	 * Sets the current player.
	 */
	public void setCurrentPlayer(TPlayer player)
	{
		currentPlayer = player;
	}

	/**
	 * Gets the current player by the current player id.
	 * @return	the current player, or null if the id is bad or not set.
	 */
	public TPlayer getCurrentPlayer()
	{
		return currentPlayer;
	}

	/**
	 * Gets the current room by the current room id.
	 * @return	the current room, or null if the id is bad or not set.
	 */
	public TRoom getCurrentRoom()
	{
		return currentPlayer != null ? ownershipMap.getCurrentRoom(currentPlayer) : null;
	}

	/**
	 * Gets the current player context by the current player id.
	 * @return	the current player context, or null if the id is bad or not set.
	 */
	public TPlayerContext getCurrentPlayerContext()
	{
		return currentPlayer != null ? getPlayerContext(currentPlayer) : null;
	}

	/**
	 * Gets the current room context by the current room id.
	 * @return	the current room context, or null if the id is bad or not set.
	 */
	public TRoomContext getCurrentRoomContext()
	{
		TRoom room = getCurrentRoom();
		return room != null ? getRoomContext(room) : null;
	}

	/**
	 * Resolves a world context.
	 * @return the context resolved.
	 */
	public TWorldContext resolveWorldContext()
	{
		return getWorldContext();
	}

	/**
	 * Resolves an action by its identity.
	 * @param actionIdentity the action identity.
	 * @return the element resolved.
	 */
	public TAction resolveAction(String actionIdentity)
	{
		TAction action = module.getActionList().get(actionIdentity);

		if (action == null)
			throw new ModuleExecutionException("Expected action '%s' in module context!", actionIdentity);

		return action;
	}

	/**
	 * Resolves a world (or THE world).
	 * @return the element resolved.
	 */
	public TWorld resolveWorld()
	{
		return resolveWorldContext().getElement();
	}

	/**
	 * Resolves a player context.
	 * @param playerIdentity the player identity.
	 * @return the context resolved.
	 * @throws ErrorInterrupt if no current player when requested.
	 * @throws ModuleExecutionException if the non-current player identity cannot be found.
	 */
	public TPlayerContext resolvePlayerContext(String playerIdentity) throws ErrorInterrupt
	{
		TPlayerContext context = null;
		if (playerIdentity.equals(IDENTITY_CURRENT_PLAYER))
		{
			context = getCurrentPlayerContext();
			if (context == null)
				throw new ErrorInterrupt("Current player context called with no current player!");
		}
		else
		{
			context = getPlayerContextByIdentity(playerIdentity);
			if (context == null)
				throw new ModuleExecutionException("Expected player '%s' in module context!", playerIdentity);
		}
		
		return context;
	}

	/**
	 * Resolves a player.
	 * @param playerIdentity the player identity.
	 * @return the context resolved.
	 * @throws ErrorInterrupt if no current player when requested.
	 * @throws ModuleExecutionException if the non-current player identity cannot be found.
	 */
	public TPlayer resolvePlayer(String playerIdentity) throws ErrorInterrupt
	{
		return resolvePlayerContext(playerIdentity).getElement();
	}

	/**
	 * Resolves a room context.
	 * @param roomIdentity the roomIdentity.
	 * @return the context resolved.
	 * @throws ErrorInterrupt if no current room when requested.
	 * @throws ModuleExecutionException if the non-current room identity cannot be found.
	 */
	public TRoomContext resolveRoomContext(String roomIdentity) throws ErrorInterrupt
	{
		TRoomContext context = null;
		if (roomIdentity.equals(IDENTITY_CURRENT_ROOM))
		{
			context = getCurrentRoomContext();
			if (context == null)
				throw new ErrorInterrupt("Current room context called with no current room!");
		}
		else
		{
			context = getRoomContextByIdentity(roomIdentity);
			if (context == null)
				throw new ModuleExecutionException("Expected room '%s' in module context!", roomIdentity);
		}
		
		return context;
	}

	/**
	 * Resolves a room.
	 * @param roomIdentity the roomIdentity.
	 * @return the context resolved.
	 * @throws ErrorInterrupt if no current room when requested.
	 * @throws ModuleExecutionException if the non-current room identity cannot be found.
	 */
	public TRoom resolveRoom(String roomIdentity) throws ErrorInterrupt
	{
		return resolveRoomContext(roomIdentity).getElement();
	}

	/**
	 * Resolves a object context.
	 * @param objectIdentity the object identity.
	 * @return the context resolved.
	 * @throws ModuleExecutionException if object not found.
	 */
	public TObjectContext resolveObjectContext(String objectIdentity) throws ErrorInterrupt
	{
		TObjectContext context = getObjectContextByIdentity(objectIdentity);
		if (context == null)
			throw new ModuleExecutionException("Expected object '%s' in module context!", objectIdentity);
		return context;
	}

	/**
	 * Resolves a object.
	 * @param objectIdentity the object identity.
	 * @return the context resolved.
	 * @throws ModuleExecutionException if object not found.
	 */
	public TObject resolveObject(String objectIdentity) throws ErrorInterrupt
	{
		return resolveObjectContext(objectIdentity).getElement();
	}

	/**
	 * Resolves a variable from the world context element.
	 * @param variableName the variable name.
	 * @return the value resolved.
	 */
	public Value resolveWorldVariableValue(String variableName)
	{
		return resolveWorldContext().getValue(variableName);
	}

	/**
	 * Resolves a variable from a player context element.
	 * @param playerIdentity a player identity.
	 * @param variableName the variable name.
	 * @return the value resolved.
	 * @throws ErrorInterrupt 
	 */
	public Value resolvePlayerVariableValue(String playerIdentity, String variableName) throws ErrorInterrupt
	{
		return resolvePlayerContext(playerIdentity).getValue(variableName);
	}

	/**
	 * Resolves a variable from a room context element.
	 * @param roomIdentity a room identity.
	 * @param variableName the variable name.
	 * @return the value resolved.
	 */
	public Value resolveRoomVariableValue(String roomIdentity, String variableName) throws ErrorInterrupt
	{
		return resolveRoomContext(roomIdentity).getValue(variableName);
	}

	/**
	 * Resolves a variable from an object context element.
	 * @param objectIdentity an object identity.
	 * @param variableName the variable name.
	 * @return the value resolved.
	 */
	public Value resolveObjectVariableValue(String objectIdentity, String variableName) throws ErrorInterrupt
	{
		return resolveObjectContext(objectIdentity).getValue(variableName);
	}
	
}
