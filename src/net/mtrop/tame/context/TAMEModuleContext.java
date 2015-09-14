package net.mtrop.tame.context;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.hash.CaseInsensitiveHash;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;

import net.mtrop.tame.TAMEModule;
import net.mtrop.tame.world.TAction;
import net.mtrop.tame.world.TObject;
import net.mtrop.tame.world.TPlayer;
import net.mtrop.tame.world.TRoom;
import net.mtrop.tame.world.TWorld;

/**
 * A mutable context for a module.
 * @author Matthew Tropiano
 */
public class TAMEModuleContext
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
		TWorld world = worldContext.getElement();
		Iterator<String> it = world.getActionList().keyIterator();
		while (it.hasNext())
		{
			String actionId = it.next();
			TAction action = world.getActionByIdentity(actionId);
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
	
}
