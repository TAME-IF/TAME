package net.mtrop.tame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.hash.Hash;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

import net.mtrop.tame.element.TAction;
import net.mtrop.tame.element.TContainer;
import net.mtrop.tame.element.TObject;
import net.mtrop.tame.element.TPlayer;
import net.mtrop.tame.element.TRoom;
import net.mtrop.tame.element.TWorld;
import net.mtrop.tame.element.context.TContainerContext;
import net.mtrop.tame.element.context.TObjectContext;
import net.mtrop.tame.element.context.TOwnershipMap;
import net.mtrop.tame.element.context.TPlayerContext;
import net.mtrop.tame.element.context.TRoomContext;
import net.mtrop.tame.element.context.TWorldContext;
import net.mtrop.tame.exception.ModuleExecutionException;
import net.mtrop.tame.exception.ModuleStateException;
import net.mtrop.tame.interrupt.ErrorInterrupt;
import net.mtrop.tame.lang.StateSaveable;
import net.mtrop.tame.lang.Value;

/**
 * A mutable context for a module.
 * @author Matthew Tropiano
 */
public class TAMEModuleContext implements TAMEConstants, StateSaveable
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
	protected HashMap<String, TPlayerContext> playerContextHash;
	/** List of rooms. */
	protected HashMap<String, TRoomContext> roomContextHash;
	/** List of objects. */
	protected HashMap<String, TObjectContext> objectContextHash;
	/** List of containers. */
	protected HashMap<String, TContainerContext> containerContextHash;
	
	/** Ownership map for players. */
	protected TOwnershipMap ownershipMap;

	public TAMEModuleContext(TAMEModule module)
	{
		this.module = module;
		this.random = new Random();
		
		currentPlayer = null;
		playerContextHash = new HashMap<String, TPlayerContext>(3);
		roomContextHash = new HashMap<String, TRoomContext>(20);
		objectContextHash = new HashMap<String, TObjectContext>(40);
		containerContextHash = new HashMap<String, TContainerContext>(10);

		// Build contexts.
		
		worldContext = new TWorldContext(module.getWorld());
		for (ObjectPair<String, TPlayer> element : module.getPlayerList())
			playerContextHash.put(element.getKey(), new TPlayerContext(element.getValue()));
		for (ObjectPair<String, TRoom> element : module.getRoomList())
			roomContextHash.put(element.getKey(), new TRoomContext(element.getValue()));
		for (ObjectPair<String, TObject> element : module.getObjectList())
			objectContextHash.put(element.getKey(), new TObjectContext(element.getValue()));
		for (ObjectPair<String, TContainer> element : module.getContainerList())
			containerContextHash.put(element.getKey(), new TContainerContext(element.getValue()));
		
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
		Hash<String> hash = new Hash<String>();
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
	 * Gets a player context by player.
	 */
	public TPlayerContext getPlayerContext(TPlayer player)
	{
		return playerContextHash.get(player.getIdentity());
	}

	/**
	 * Gets a room context by room.
	 */
	public TRoomContext getRoomContext(TRoom room)
	{
		return roomContextHash.get(room.getIdentity());
	}

	/**
	 * Gets an object context by object.
	 */
	public TObjectContext getObjectContext(TObject object)
	{
		return objectContextHash.get(object.getIdentity());
	}

	/**
	 * Gets an container context by container.
	 */
	public TContainerContext getContainerContext(TContainer container)
	{
		return containerContextHash.get(container.getIdentity());
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
	 * Gets an object context by object name.
	 */
	public TObjectContext getObjectContextByIdentity(String name)
	{
		return objectContextHash.get(name);
	}

	/**
	 * Gets a container context by object name.
	 */
	public TContainerContext getContainerContextByIdentity(String name)
	{
		return containerContextHash.get(name);
	}

	/**
	 * Get the player context list.
	 */
	public HashMap<String, TPlayerContext> getPlayerContextList()
	{
		return playerContextHash;
	}

	/**
	 * Get the room context list.
	 */
	public HashMap<String, TRoomContext> getRoomContextList()
	{
		return roomContextHash;
	}

	/**
	 * Get the object context list.
	 */
	public HashMap<String, TObjectContext> getObjectContextList()
	{
		return objectContextHash;
	}

	/**
	 * Get the container context list.
	 */
	public HashMap<String, TContainerContext> getContainerContextList()
	{
		return containerContextHash;
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
	 * Resolves an object context.
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
	 * Resolves an object.
	 * @param objectIdentity the object identity.
	 * @return the context resolved.
	 * @throws ModuleExecutionException if object not found.
	 */
	public TObject resolveObject(String objectIdentity) throws ErrorInterrupt
	{
		return resolveObjectContext(objectIdentity).getElement();
	}

	/**
	 * Resolves a container context.
	 * @param containerIdentity the container identity.
	 * @return the context resolved.
	 * @throws ModuleExecutionException if container not found.
	 */
	public TContainerContext resolveContainerContext(String containerIdentity) throws ErrorInterrupt
	{
		TContainerContext context = getContainerContextByIdentity(containerIdentity);
		if (context == null)
			throw new ModuleExecutionException("Expected container '%s' in module context!", containerIdentity);
		return context;
	}

	/**
	 * Resolves a container.
	 * @param containerIdentity the object identity.
	 * @return the context resolved.
	 * @throws ModuleExecutionException if object not found.
	 */
	public TContainer resolveContainer(String containerIdentity) throws ErrorInterrupt
	{
		return resolveContainerContext(containerIdentity).getElement();
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

	/**
	 * Resolves a variable from a container context element.
	 * @param containerIdentity a container identity.
	 * @param variableName the variable name.
	 * @return the value resolved.
	 */
	public Value resolveContainerVariableValue(String containerIdentity, String variableName) throws ErrorInterrupt
	{
		return resolveContainerContext(containerIdentity).getValue(variableName);
	}

	@Override
	public void writeStateBytes(TAMEModule module, OutputStream out) throws IOException 
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);

		sw.writeBoolean(currentPlayer != null);
		if (currentPlayer != null)
			sw.writeString(currentPlayer.getIdentity(), "UTF-8");
		
		sw.writeString(worldContext.getElement().getIdentity(), "UTF-8");
		worldContext.writeStateBytes(module, out);
		
		sw.writeInt(playerContextHash.size());
		for (ObjectPair<String, TPlayerContext> entry : playerContextHash)
		{
			sw.writeString(entry.getKey(), "UTF-8");
			entry.getValue().writeStateBytes(module, out);
		}

		sw.writeInt(roomContextHash.size());
		for (ObjectPair<String, TRoomContext> entry : roomContextHash)
		{
			sw.writeString(entry.getKey(), "UTF-8");
			entry.getValue().writeStateBytes(module, out);
		}
		
		sw.writeInt(objectContextHash.size());
		for (ObjectPair<String, TObjectContext> entry : objectContextHash)
		{
			sw.writeString(entry.getKey(), "UTF-8");
			entry.getValue().writeStateBytes(module, out);
		}

		sw.writeInt(containerContextHash.size());
		for (ObjectPair<String, TContainerContext> entry : containerContextHash)
		{
			sw.writeString(entry.getKey(), "UTF-8");
			entry.getValue().writeStateBytes(module, out);
		}

		ownershipMap.writeStateBytes(module, out);
	}

	@Override
	public void readStateBytes(TAMEModule module, InputStream in) throws IOException 
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		String identity;
		int size;
		
		// has current player.
		if (sr.readBoolean())
		{
			identity = sr.readString("UTF-8");
			currentPlayer = module.getPlayerByIdentity(identity);
			if (currentPlayer == null)
				throw new ModuleStateException("Expected player '%s' in module context!", identity);
		}
		else
			currentPlayer = null;

		identity = sr.readString("UTF-8");
		if (!identity.equals(IDENTITY_CURRENT_WORLD))
			throw new ModuleStateException("Expected world '%s' in module context!", identity);
		worldContext.readStateBytes(module, in);	
		
		size = sr.readInt();
		while (size-- > 0)
		{
			identity = sr.readString("UTF-8");
			if (!playerContextHash.containsKey(identity))
				throw new ModuleStateException("Expected player '%s' in module context!", identity);
			else
				playerContextHash.get(identity).readStateBytes(module, in);
		}
		
		size = sr.readInt();
		while (size-- > 0)
		{
			identity = sr.readString("UTF-8");
			if (!roomContextHash.containsKey(identity))
				throw new ModuleStateException("Expected room '%s' in module context!", identity);
			else
				roomContextHash.get(identity).readStateBytes(module, in);
		}

		size = sr.readInt();
		while (size-- > 0)
		{
			identity = sr.readString("UTF-8");
			if (!objectContextHash.containsKey(identity))
				throw new ModuleStateException("Expected object '%s' in module context!", identity);
			else
				objectContextHash.get(identity).readStateBytes(module, in);
		}
		
		ownershipMap.readStateBytes(module, in);
	}

	@Override
	public byte[] toStateBytes(TAMEModule module) throws IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		writeStateBytes(module, bos);
		return bos.toByteArray();
	}

	@Override
	public void fromStateBytes(TAMEModule module, byte[] data) throws IOException 
	{
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		readStateBytes(module, bis);
		bis.close();
	}
	
}