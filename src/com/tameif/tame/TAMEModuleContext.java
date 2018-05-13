/*******************************************************************************
 * Copyright (c) 2015-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import com.blackrook.commons.Common;
import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.hash.Hash;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;
import com.tameif.tame.element.ObjectContainer;
import com.tameif.tame.element.TAction;
import com.tameif.tame.element.TContainer;
import com.tameif.tame.element.TElement;
import com.tameif.tame.element.TObject;
import com.tameif.tame.element.TPlayer;
import com.tameif.tame.element.TRoom;
import com.tameif.tame.element.TWorld;
import com.tameif.tame.element.context.TContainerContext;
import com.tameif.tame.element.context.TElementContext;
import com.tameif.tame.element.context.TObjectContext;
import com.tameif.tame.element.context.TOwnershipMap;
import com.tameif.tame.element.context.TPlayerContext;
import com.tameif.tame.element.context.TRoomContext;
import com.tameif.tame.element.context.TWorldContext;
import com.tameif.tame.exception.ModuleException;
import com.tameif.tame.exception.ModuleExecutionException;
import com.tameif.tame.exception.ModuleStateException;
import com.tameif.tame.interrupt.ErrorInterrupt;
import com.tameif.tame.lang.Saveable;
import com.tameif.tame.lang.Value;

/**
 * A mutable context for a module.
 * @author Matthew Tropiano
 */
public class TAMEModuleContext implements TAMEConstants, Saveable
{
	/** A random number generator. */
	private TAMEModule module;

	/** A random number generator. */
	private Random random;

	/** World context. */
	private TWorldContext worldContext;
	/** List of players. */
	private HashMap<String, TPlayerContext> playerContextHash;
	/** List of rooms. */
	private HashMap<String, TRoomContext> roomContextHash;
	/** List of objects. */
	private HashMap<String, TObjectContext> objectContextHash;
	/** List of containers. */
	private HashMap<String, TContainerContext> containerContextHash;
	
	/** Ownership map for players. */
	private TOwnershipMap ownershipMap;

	/** Command runaway max from the Header. */
	private long commandRunawayMax;
	/** Function call depth max from the Header. */
	private long functionDepthMax;
	
	/**
	 * Creates a new module context.
	 * @param module the module to create the context for.
	 */
	public TAMEModuleContext(TAMEModule module)
	{
		this.module = module;
		this.random = new Random();
		
		this.playerContextHash = new HashMap<String, TPlayerContext>(3);
		this.roomContextHash = new HashMap<String, TRoomContext>(20);
		this.objectContextHash = new HashMap<String, TObjectContext>(40);
		this.containerContextHash = new HashMap<String, TContainerContext>(10);

		// Build contexts.
		
		this.worldContext = new TWorldContext(module.getWorld());
		for (ObjectPair<String, TPlayer> element : module.getPlayerList()) 
			if (!element.getValue().isArchetype())
				playerContextHash.put(element.getKey(), new TPlayerContext(element.getValue()));
		for (ObjectPair<String, TRoom> element : module.getRoomList())
			if (!element.getValue().isArchetype())
				roomContextHash.put(element.getKey(), new TRoomContext(element.getValue()));
		for (ObjectPair<String, TObject> element : module.getObjectList())
			if (!element.getValue().isArchetype())
				objectContextHash.put(element.getKey(), new TObjectContext(element.getValue()));
		for (ObjectPair<String, TContainer> element : module.getContainerList())
			if (!element.getValue().isArchetype())
				containerContextHash.put(element.getKey(), new TContainerContext(element.getValue()));
		
		this.ownershipMap = new TOwnershipMap();

		for (ObjectPair<String, TObjectContext> element : objectContextHash)
		{
			TObjectContext context = element.getValue();
			TObject object = context.getElement();
			for (String name : object.getNames())
				ownershipMap.addObjectName(object, name);
			for (String tag : object.getTags())
				ownershipMap.addObjectTag(object, tag);
		}
		
		long cr = Common.parseLong(module.getHeader().getAttribute(HEADER_TAME_RUNAWAY_MAX));
		long fd = Common.parseLong(module.getHeader().getAttribute(HEADER_TAME_FUNCDEPTH_MAX));
		
		this.commandRunawayMax = cr <= 0 ? DEFAULT_RUNAWAY_THRESHOLD: cr;
		this.functionDepthMax = fd <= 0 ? DEFAULT_FUNCTION_DEPTH: fd;
	}

	/**
	 * Gets the encapsulated module.
	 * @return the module.
	 */
	public TAMEModule getModule()
	{
		return module;
	}
	
	/**
	 * Gets the context random number generator.
	 * @return the current random generator.
	 */
	public Random getRandom()
	{
		return random;
	}
	
	/**
	 * @return the command runaway detection limit.
	 */
	public long getCommandRunawayMax() 
	{
		return commandRunawayMax;
	}
	
	/**
	 * @return the function depth detection limit.
	 */
	public long getFunctionDepthMax() 
	{
		return functionDepthMax;
	}
	
	/**
	 * Get the player context list.
	 * @return the context map.
	 */
	public Iterator<TPlayerContext> getPlayerContextIterator()
	{
		return playerContextHash.valueIterator();
	}

	/**
	 * Get the room context list.
	 * @return the context map.
	 */
	public Iterator<TRoomContext> getRoomContextIterator()
	{
		return roomContextHash.valueIterator();
	}

	/**
	 * Get the object context list.
	 * @return the context map.
	 */
	public Iterator<TObjectContext> getObjectContextIterator()
	{
		return objectContextHash.valueIterator();
	}

	/**
	 * Get the container context list.
	 * @return the context map.
	 */
	public Iterator<TContainerContext> getContainerContextIterator()
	{
		return containerContextHash.valueIterator();
	}

	/**
	 * Reflection method - get available action names according to current context
	 * and filtered by a prefix.
	 * @return the valid action names that can be.
	 */
	public String[] getAvailableActionNames()
	{
		Hash<String> hash = new Hash<String>();
		Iterator<ObjectPair<String, TAction>> it = module.getActionList().iterator();
		while (it.hasNext())
		{
			ObjectPair<String, TAction> actionId = it.next();
			TAction action = module.getActionByIdentity(actionId.getKey());
			TPlayer player = getCurrentPlayerContext() != null ? getCurrentPlayerContext().getElement() : null;
			TRoom room = getCurrentRoomContext() != null ? getCurrentRoomContext().getElement() : null;
			
			if (room != null && !room.allowsAction(action))
				continue;
			if (player != null && !player.allowsAction(action))
				continue;
			
			for (String s : action.getNames())
				hash.put(s);
		}
		
		Iterator<String> hit = hash.iterator();
		String[] out = new String[hash.size()];
		int i = 0;
		while (hit.hasNext())
			out[i++] = hit.next();
		Arrays.sort(out);
		return out;
	}

	/**
	 * Gets this module's world context.
	 * @return the world context.
	 */
	public TWorldContext getWorldContext()
	{
		return worldContext;
	}
	
	/**
	 * Gets a player context by player.
	 * @param player the player to use for the lookup.
	 * @return the matching context.
	 */
	public TPlayerContext getPlayerContext(TPlayer player)
	{
		return playerContextHash.get(player.getIdentity());
	}

	/**
	 * Gets a room context by room.
	 * @param room the room to use for the lookup.
	 * @return the matching context.
	 */
	public TRoomContext getRoomContext(TRoom room)
	{
		return roomContextHash.get(room.getIdentity());
	}

	/**
	 * Gets an object context by object.
	 * @param object the object to use for the lookup.
	 * @return the matching context.
	 */
	public TObjectContext getObjectContext(TObject object)
	{
		return objectContextHash.get(object.getIdentity());
	}

	/**
	 * Gets an container context by container.
	 * @param container the container to use for the lookup.
	 * @return the matching context.
	 */
	public TContainerContext getContainerContext(TContainer container)
	{
		return containerContextHash.get(container.getIdentity());
	}

	/**
	 * Gets an element context by element name.
	 * @param identity the identity to use for the lookup.
	 * @return the matching context, or null if no match.
	 */
	public TElementContext<?> getContextByIdentity(String identity)
	{
		if (TAMEConstants.IDENTITY_CURRENT_WORLD.equalsIgnoreCase(identity))
			return worldContext;
		else if (playerContextHash.containsKey(identity))
			return playerContextHash.get(identity);
		else if (roomContextHash.containsKey(identity))
			return roomContextHash.get(identity);
		else if (objectContextHash.containsKey(identity))
			return objectContextHash.get(identity);
		else if (containerContextHash.containsKey(identity))
			return containerContextHash.get(identity);
		else
			return null;
	}

	/**
	 * Gets a player context by player name.
	 * @param identity the identity to use for the lookup.
	 * @return the matching context.
	 */
	public TPlayerContext getPlayerContextByIdentity(String identity)
	{
		return playerContextHash.get(identity);
	}

	/**
	 * Gets a room context by room name.
	 * @param identity the identity to use for the lookup.
	 * @return the matching context.
	 */
	public TRoomContext getRoomContextByIdentity(String identity)
	{
		return roomContextHash.get(identity);
	}

	/**
	 * Gets an object context by object name.
	 * @param identity the identity to use for the lookup.
	 * @return the matching context.
	 */
	public TObjectContext getObjectContextByIdentity(String identity)
	{
		return objectContextHash.get(identity);
	}

	/**
	 * Gets a container context by object name.
	 * @param identity the identity to use for the lookup.
	 * @return the matching context.
	 */
	public TContainerContext getContainerContextByIdentity(String identity)
	{
		return containerContextHash.get(identity);
	}

	/**
	 * Gets the current player.
	 * @return the current player, or null if not set.
	 */
	public TPlayer getCurrentPlayer()
	{
		return ownershipMap.getCurrentPlayer();
	}

	/**
	 * Gets the current player context.
	 * @return the current player context, or null if not set.
	 */
	public TPlayerContext getCurrentPlayerContext()
	{
		return ownershipMap.getCurrentPlayer() != null ? getPlayerContext(ownershipMap.getCurrentPlayer()) : null;
	}

	/**
	 * Gets the current room.
	 * @return the current room, or null if not set.
	 */
	public TRoom getCurrentRoom()
	{
		return ownershipMap.getCurrentRoom();
	}

	/**
	 * Gets the current room context.
	 * @return the current room context, or null if not set.
	 */
	public TRoomContext getCurrentRoomContext()
	{
		TRoom room = ownershipMap.getCurrentRoom();
		return room != null ? getRoomContext(room) : null;
	}

	/**
	 * @return the ownership map for this context.
	 */
	public TOwnershipMap getOwnershipMap() 
	{
		return ownershipMap;
	}
	
	/**
	 * Returns all objects in the accessible area by an object name read from the interpreter.
	 * The output stops if the size of the output array is reached.
	 * @param name the name from the interpreter.
	 * @param outputArray the output vector of found objects.
	 * @param arrayOffset the starting offset into the array to put them.
	 * @return the amount of objects found.
	 */
	public int getAccessibleObjectsByName(String name, TObject[] outputArray, int arrayOffset)
	{
		TPlayerContext playerContext = getCurrentPlayerContext();
		TRoomContext roomContext = getCurrentRoomContext();
		TWorldContext worldContext = getWorldContext();
		int start = arrayOffset;
		
		if (playerContext != null) for (TObject obj : ownershipMap.getObjectsOwnedByElement(playerContext.getElement()))
		{
			if (ownershipMap.checkObjectHasName(obj, name))
			{
				outputArray[arrayOffset++] = obj;
				if (arrayOffset == outputArray.length)
					return arrayOffset - start;
			}
		}
		
		if (roomContext != null) for (TObject obj : ownershipMap.getObjectsOwnedByElement(roomContext.getElement()))
		{
			if (ownershipMap.checkObjectHasName(obj, name))
			{
				outputArray[arrayOffset++] = obj;
				if (arrayOffset == outputArray.length)
					return arrayOffset - start;
			}
		}
	
		for (TObject obj : ownershipMap.getObjectsOwnedByElement(worldContext.getElement()))
		{
			if (ownershipMap.checkObjectHasName(obj, name))
			{
				outputArray[arrayOffset++] = obj;
				if (arrayOffset == outputArray.length)
					return arrayOffset - start;
			}
		}
	
		return arrayOffset - start;
	}

	/**
	 * Resolves an action by its identity.
	 * @param actionIdentity the action identity.
	 * @return the element resolved.
	 */
	public TAction resolveAction(String actionIdentity)
	{
		TAction action = module.getActionByIdentity(actionIdentity);

		if (action == null)
			throw new ModuleExecutionException("Expected action '%s' in module context!", actionIdentity);

		return action;
	}

	/**
	 * Resolves an element by a value.
	 * @param varElement the value to resolve via module context.
	 * @return the corresponding element, or null if the value does not refer to an object container.
	 * @throws ErrorInterrupt if a major error occurs.
	 */
	public TElement resolveElement(Value varElement) throws ErrorInterrupt 
	{
		switch (varElement.getType())
		{
			default:
				return null;
			case OBJECT:
				return resolveObject(varElement.asString());
			case ROOM:
				return resolveRoom(varElement.asString());
			case PLAYER:
				return resolvePlayer(varElement.asString());
			case CONTAINER:
				return resolveContainer(varElement.asString());
			case WORLD:
				return resolveWorld();
		}
		
	}

	/**
	 * Resolves an element context by a value.
	 * @param varElement the value to resolve via module context.
	 * @return the corresponding element context, or null if the value does not refer to an object container.
	 * @throws ErrorInterrupt if a major error occurs.
	 */
	public TElementContext<?> resolveElementContext(Value varElement) throws ErrorInterrupt 
	{
		switch (varElement.getType())
		{
			default:
				return null;
			case OBJECT:
				return resolveObjectContext(varElement.asString());
			case ROOM:
				return resolveRoomContext(varElement.asString());
			case PLAYER:
				return resolvePlayerContext(varElement.asString());
			case CONTAINER:
				return resolveContainerContext(varElement.asString());
			case WORLD:
				return resolveWorldContext();
		}
		
	}

	/**
	 * Resolves a list of all objects contained by an object container.
	 * @param varObjectContainer the value to resolve via module context.
	 * @return an iterable list of objects, or null if the value does not refer to an object container.
	 * @throws ErrorInterrupt if a major error occurs.
	 */
	public Iterable<TObject> resolveObjectList(Value varObjectContainer) throws ErrorInterrupt 
	{
		return getOwnershipMap().getObjectsOwnedByElement((ObjectContainer)resolveElement(varObjectContainer));
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
	 * Resolves a world (or THE world).
	 * @return the element resolved.
	 */
	public TWorld resolveWorld()
	{
		return module.getWorld();
	}

	/**
	 * Resolves a player context.
	 * @param playerIdentity the player identity.
	 * @return the context resolved.
	 * @throws ModuleExecutionException if the non-current player identity cannot be found, or if no current player when requested.
	 */
	public TPlayerContext resolvePlayerContext(String playerIdentity)
	{
		TPlayerContext context = null;
		if (playerIdentity.equals(IDENTITY_CURRENT_PLAYER))
		{
			context = getCurrentPlayerContext();
			if (context == null)
				throw new ModuleExecutionException("Current player context requested with no current player!");
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
	 * @throws ModuleExecutionException if the non-current player identity cannot be found, or if no current player if requesed.
	 */
	public TPlayer resolvePlayer(String playerIdentity)
	{
		TPlayer element;
		if (playerIdentity.equals(TAMEConstants.IDENTITY_CURRENT_PLAYER))
		{
			element = getCurrentPlayer();
			if (element == null)
				throw new ModuleExecutionException("Current player requested with no current player!");
			return element;
		}
		else
		{
			element = module.getPlayerByIdentity(playerIdentity); 
			if (element == null)
				throw new ModuleExecutionException("Expected player '%s' in module context!", playerIdentity);
			return element;
		}
	}

	/**
	 * Resolves a room context.
	 * @param roomIdentity the roomIdentity.
	 * @return the context resolved.
	 * @throws ModuleExecutionException if the non-current room identity cannot be found, or if no current room if requested.
	 */
	public TRoomContext resolveRoomContext(String roomIdentity)
	{
		TRoomContext context = null;
		if (roomIdentity.equals(IDENTITY_CURRENT_ROOM))
		{
			context = getCurrentRoomContext();
			if (context == null)
				throw new ModuleExecutionException("Current room context requested with no current room!");
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
	 * @throws ModuleExecutionException if the non-current room identity cannot be found, or if not current room if requested.
	 */
	public TRoom resolveRoom(String roomIdentity)
	{
		TRoom element;
		if (roomIdentity.equals(TAMEConstants.IDENTITY_CURRENT_ROOM))
		{
			element = getCurrentRoom();
			if (element == null)
				throw new ModuleExecutionException("Current room requested with no current room!");
			return element;
		}
		else
		{
			element = module.getRoomByIdentity(roomIdentity); 
			if (element == null)
				throw new ModuleExecutionException("Expected room '%s' in module context!", roomIdentity);
			return element;
		}
	}

	/**
	 * Resolves an object context.
	 * @param objectIdentity the object identity.
	 * @return the context resolved.
	 * @throws ModuleExecutionException if object not found.
	 */
	public TObjectContext resolveObjectContext(String objectIdentity)
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
	public TObject resolveObject(String objectIdentity)
	{
		TObject element = module.getObjectByIdentity(objectIdentity); 
		if (element == null)
			throw new ModuleExecutionException("Expected object '%s' in module context!", objectIdentity);
		return element;
	}

	/**
	 * Resolves a container context.
	 * @param containerIdentity the container identity.
	 * @return the context resolved.
	 * @throws ModuleExecutionException if container not found.
	 */
	public TContainerContext resolveContainerContext(String containerIdentity)
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
	public TContainer resolveContainer(String containerIdentity)
	{
		TContainer element = module.getContainerByIdentity(containerIdentity); 
		if (element == null)
			throw new ModuleExecutionException("Expected container '%s' in module context!", containerIdentity);
		return element;
	}

	@Override
	public void writeBytes(OutputStream out) throws IOException 
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);

		sw.writeBytes("TSAV".getBytes("ASCII"));
		// write version
		sw.writeByte((byte)0x01);
		
		byte[] digest;
		if ((digest = module.getDigest()) == null)
			digest = module.calculateDigest();
		sw.writeBytes(digest);

		AtomicLong refCounter = new AtomicLong(0L);
		HashMap<Object, Long> refSet = new HashMap<>(16);
		
		sw.writeString(worldContext.getElement().getIdentity(), "UTF-8");
		worldContext.writeStateBytes(module, refCounter, refSet, out);
		
		sw.writeInt(playerContextHash.size());
		for (ObjectPair<String, TPlayerContext> entry : playerContextHash)
		{
			sw.writeString(entry.getKey(), "UTF-8");
			entry.getValue().writeStateBytes(module, refCounter, refSet, out);
		}

		sw.writeInt(roomContextHash.size());
		for (ObjectPair<String, TRoomContext> entry : roomContextHash)
		{
			sw.writeString(entry.getKey(), "UTF-8");
			entry.getValue().writeStateBytes(module, refCounter, refSet, out);
		}
		
		sw.writeInt(objectContextHash.size());
		for (ObjectPair<String, TObjectContext> entry : objectContextHash)
		{
			sw.writeString(entry.getKey(), "UTF-8");
			entry.getValue().writeStateBytes(module, refCounter, refSet, out);
		}

		sw.writeInt(containerContextHash.size());
		for (ObjectPair<String, TContainerContext> entry : containerContextHash)
		{
			sw.writeString(entry.getKey(), "UTF-8");
			entry.getValue().writeStateBytes(module, refCounter, refSet, out);
		}

		ownershipMap.writeStateBytes(module, refCounter, refSet, out);
	}

	@Override
	public void readBytes(InputStream in) throws IOException 
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		String identity;
		int size;
		
		if (!(new String(sr.readBytes(4), "ASCII")).equals("TSAV"))
			throw new ModuleException("Not a TAME module save state.");
		
		if (sr.readByte() != 0x01)
			throw new ModuleException("Module save state does not have a recognized version.");

		byte[] digest = sr.readBytes(20);
		byte[] moduleDigest = module.getDigest();
		if ((moduleDigest = module.getDigest()) == null)
			moduleDigest = module.calculateDigest();
		if (!Arrays.equals(digest, moduleDigest))
			throw new ModuleStateException("Module and state digests do not match. Save state may not be for this module.");
		
		HashMap<Long, Value> refMap = new HashMap<>(16);

		identity = sr.readString("UTF-8");
		if (!identity.equals(IDENTITY_CURRENT_WORLD))
			throw new ModuleStateException("Expected world '%s' in module context!", identity);
		worldContext.readStateBytes(module, refMap, in);	
		
		size = sr.readInt();
		while (size-- > 0)
		{
			identity = sr.readString("UTF-8");
			if (!playerContextHash.containsKey(identity))
				throw new ModuleStateException("Expected player '%s' in module context!", identity);
			else
				playerContextHash.get(identity).readStateBytes(module, refMap, in);
		}
		
		size = sr.readInt();
		while (size-- > 0)
		{
			identity = sr.readString("UTF-8");
			if (!roomContextHash.containsKey(identity))
				throw new ModuleStateException("Expected room '%s' in module context!", identity);
			else
				roomContextHash.get(identity).readStateBytes(module, refMap, in);
		}

		size = sr.readInt();
		while (size-- > 0)
		{
			identity = sr.readString("UTF-8");
			if (!objectContextHash.containsKey(identity))
				throw new ModuleStateException("Expected object '%s' in module context!", identity);
			else
				objectContextHash.get(identity).readStateBytes(module, refMap, in);
		}
		
		size = sr.readInt();
		while (size-- > 0)
		{
			identity = sr.readString("UTF-8");
			if (!containerContextHash.containsKey(identity))
				throw new ModuleStateException("Expected container '%s' in module context!", identity);
			else
				containerContextHash.get(identity).readStateBytes(module, refMap, in);
		}
		
		ownershipMap.readStateBytes(module, refMap, in);
	}

	@Override
	public byte[] toBytes() throws IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		writeBytes(bos);
		return bos.toByteArray();
	}

	@Override
	public void fromBytes(byte[] data) throws IOException 
	{
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		readBytes(bis);
		bis.close();
	}
	
	
	
}
