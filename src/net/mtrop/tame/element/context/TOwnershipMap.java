/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame.element.context;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.mtrop.tame.TAMEConstants;
import net.mtrop.tame.TAMEModule;
import net.mtrop.tame.element.TContainer;
import net.mtrop.tame.element.TElement;
import net.mtrop.tame.element.TObject;
import net.mtrop.tame.element.TPlayer;
import net.mtrop.tame.element.TRoom;
import net.mtrop.tame.element.TWorld;
import net.mtrop.tame.exception.ModuleStateException;
import net.mtrop.tame.lang.StateSaveable;

import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.hash.CaseInsensitiveHash;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.hash.HashedQueueMap;
import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.commons.linkedlist.Stack;
import com.blackrook.commons.list.List;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

/**
 * An ownership map for all objects.
 * @author Matthew Tropiano
 */
public class TOwnershipMap implements StateSaveable, TAMEConstants
{
	// These must be queues - order added should be reflected in retrieval and presentation.
	
	/** Ownership map - objects owned by world. */
	protected HashedQueueMap<TWorld, TObject> objectsOwnedByWorld;
	/** Ownership map - objects owned by room. */
	protected HashedQueueMap<TRoom, TObject> objectsOwnedByRoom;
	/** Ownership map - objects owned by player. */
	protected HashedQueueMap<TPlayer, TObject> objectsOwnedByPlayer;
	/** Ownership map - objects owned by container. */
	protected HashedQueueMap<TContainer, TObject> objectsOwnedByContainer;
	/** Room stack. */
	protected HashMap<TPlayer, Stack<TRoom>> playerToRoomStack;
	
	/** Map of object to its current names. */
	protected HashMap<TObject, CaseInsensitiveHash> objectCurrentNames;
	/** Map of object to its current tags. */
	protected HashMap<TObject, CaseInsensitiveHash> objectCurrentTags;

	
	/** Reverse lookup object - not saved. */
	protected HashMap<TObject, TElement> objectToElement;

	/**
	 * Creates a new ownership.
	 */
	public TOwnershipMap()
	{
		objectsOwnedByWorld = new HashedQueueMap<TWorld, TObject>(1);
		objectsOwnedByRoom = new HashedQueueMap<TRoom, TObject>(10);
		objectsOwnedByPlayer = new HashedQueueMap<TPlayer, TObject>(4);
		objectsOwnedByContainer = new HashedQueueMap<TContainer, TObject>(3);
		playerToRoomStack = new HashMap<TPlayer, Stack<TRoom>>(3);
		objectCurrentNames = new HashMap<TObject, CaseInsensitiveHash>();
		objectCurrentTags = new HashMap<TObject, CaseInsensitiveHash>();
		
		objectToElement = new HashMap<TObject, TElement>(20);
	}
	
	/**
	 * Clears entire ownership table.
	 */
	public void reset()
	{
		objectsOwnedByWorld.clear();
		objectsOwnedByRoom.clear();
		objectsOwnedByPlayer.clear();
		playerToRoomStack.clear();
		objectCurrentNames.clear();
		objectCurrentTags.clear();

		objectToElement.clear();
	}
	
	/**
	 * Removes an object from all owners.
	 * @param object the object to remove.
	 */
	public void removeObject(TObject object)
	{
		TElement element = objectToElement.removeUsingKey(object);
		if (element == null)
			return;
		
		if (element instanceof TWorld)
			objectsOwnedByWorld.removeValue((TWorld)element, object);
		else if (element instanceof TPlayer)
			objectsOwnedByPlayer.removeValue((TPlayer)element, object);
		else if (element instanceof TRoom)
			objectsOwnedByRoom.removeValue((TRoom)element, object);
		else if (element instanceof TContainer)
			objectsOwnedByContainer.removeValue((TContainer)element, object);
	}
	
	/**
	 * Removes a player from all rooms.
	 * @param player the player to remove.
	 */
	public void removePlayer(TPlayer player)
	{
		playerToRoomStack.removeUsingKey(player);
	}

	/**
	 * Adds an object to a container.
	 * The object is first removed from its owners before it is bound to another. 
	 * @param object the object to add.
	 * @param container the target container.
	 */
	public void addObjectToContainer(TObject object, TContainer container)
	{
		removeObject(object);
		objectToElement.put(object, container);
		objectsOwnedByContainer.enqueue(container, object);
	}
	
	/**
	 * Adds an object to a room.
	 * The object is first removed from its owners before it is bound to another. 
	 * @param object the object to add.
	 * @param room the target room.
	 */
	public void addObjectToRoom(TObject object, TRoom room)
	{
		removeObject(object);
		objectToElement.put(object, room);
		objectsOwnedByRoom.enqueue(room, object);
	}
	
	/**
	 * Adds an object to a player.
	 * The object is first removed from its owners before it is bound to another. 
	 * @param object the object to add.
	 * @param player the target player.
	 */
	public void addObjectToPlayer(TObject object, TPlayer player)
	{
		removeObject(object);
		objectToElement.put(object, player);
		objectsOwnedByPlayer.enqueue(player, object);
	}
	
	/**
	 * Adds an object to a world.
	 * The object is first removed from its owners before it is bound to another. 
	 * @param object the object to add.
	 * @param world the target world.
	 */
	public void addObjectToWorld(TObject object, TWorld world)
	{
		removeObject(object);
		objectToElement.put(object, world);
		objectsOwnedByWorld.enqueue(world, object);
	}
	
	/**
	 * Adds a player to a room and discards the stack.
	 * @param player the player to add.
	 * @param room the target room.
	 */
	public void addPlayerToRoom(TPlayer player, TRoom room)
	{
		removePlayer(player);
		Stack<TRoom> stack = playerToRoomStack.get(player);
		if (stack == null)
		{
			stack = new Stack<TRoom>();
			playerToRoomStack.put(player, stack);
		}
		stack.clear();
		stack.push(room);
	}
	
	/**
	 * Pushes a room onto a player's room stack.
	 * @param player the player to change.
	 * @param room the room to push.
	 */
	public void pushRoomOntoPlayer(TPlayer player, TRoom room)
	{
		Stack<TRoom> stack = playerToRoomStack.get(player);
		if (stack == null)
		{
			stack = new Stack<TRoom>();
			playerToRoomStack.put(player, stack);
		}
		stack.push(room);
	}
	
	/**
	 * Pops a room off of a player's room stack.
	 * The room is removed.
	 * @param player the player to change.
	 * @return the topmost removed room. 
	 */
	public TRoom popRoomFromPlayer(TPlayer player)
	{
		Stack<TRoom> stack = playerToRoomStack.get(player);
		if (stack == null)
			return null;
		
		TRoom out = stack.pop();
		if (stack.size() == 0)
			playerToRoomStack.removeUsingKey(player);
		return out;
	}
	
	/**
	 * Returns the current room for a player.
	 * @param player the player to use.
	 * @return the current room or null if no room.
	 */
	public TRoom getCurrentRoom(TPlayer player)
	{
		Stack<TRoom> stack = playerToRoomStack.get(player);
		return stack != null ? stack.peek() : null;
	}
	
	/**
	 * Checks if a world possesses an object.
	 * @param world the world to use.
	 * @param object the object in question.
	 * @return true if so, false if not.
	 */
	public boolean checkWorldHasObject(TWorld world, TObject object)
	{
		if (objectsOwnedByWorld.containsKey(world))
			return objectsOwnedByWorld.get(world).contains(object);
		return false;
	}

	/**
	 * Checks if a room possesses an object.
	 * @param room the room to use.
	 * @param object the object in question.
	 * @return true if so, false if not.
	 */
	public boolean checkRoomHasObject(TRoom room, TObject object)
	{
		if (objectsOwnedByRoom.containsKey(room))
			return objectsOwnedByRoom.get(room).contains(object);
		return false;
	}
	
	/**
	 * Checks if a container possesses an object.
	 * @param container the container to use.
	 * @param object the object in question.
	 * @return true if so, false if not.
	 */
	public boolean checkContainerHasObject(TContainer container, TObject object)
	{
		if (objectsOwnedByContainer.containsKey(container))
			return objectsOwnedByContainer.get(container).contains(object);
		return false;
	}
	
	/**
	 * Checks if a player possesses an object.
	 * @param player the player to use.
	 * @param object the object in question.
	 * @return true if so, false if not.
	 */
	public boolean checkPlayerHasObject(TPlayer player, TObject object)
	{
		if (objectsOwnedByPlayer.containsKey(player))
			return objectsOwnedByPlayer.get(player).contains(object);
		return false;
	}

	/**
	 * Checks if an object has no owner.
	 * @param object the object in question.
	 * @return true if so, false if not.
	 */
	public boolean checkObjectHasNoOwner(TObject object)
	{
		return !objectToElement.containsKey(object);
	}
	
	/**
	 * Returns if a player is in a room (or room is in stack).
	 * @param player the player to use.
	 * @param room the room to use.
	 * @return true if so, false if not.
	 */
	public boolean checkPlayerIsInRoom(TPlayer player, TRoom room)
	{
		if (playerToRoomStack.containsKey(player))
			return playerToRoomStack.get(player).contains(room);
		return false;
	}

	/**
	 * Gets the list of objects owned by a world.
	 * @param world the world in question.
	 * @return the list of all objects owned by the world.
	 */
	public Iterable<TObject> getObjectsOwnedByWorld(TWorld world)
	{
		return getObjectsInQueue(objectsOwnedByWorld.get(world));
	}

	/**
	 * Gets the list of objects owned by a room.
	 * @param room the room in question.
	 * @return the list of all objects owned by the room.
	 */
	public Iterable<TObject> getObjectsOwnedByRoom(TRoom room)
	{
		return getObjectsInQueue(objectsOwnedByRoom.get(room)); 
	}

	/**
	 * Gets the list of objects owned by a player.
	 * @param player the player in question.
	 * @return the list of all objects owned by the player.
	 */
	public Iterable<TObject> getObjectsOwnedByPlayer(TPlayer player)
	{
		return getObjectsInQueue(objectsOwnedByPlayer.get(player)); 
	}

	/**
	 * Gets the list of objects owned by a container.
	 * @param container the container in question.
	 * @return the list of all objects owned by the container.
	 */
	public Iterable<TObject> getObjectsOwnedByContainer(TContainer container)
	{
		return getObjectsInQueue(objectsOwnedByContainer.get(container)); 
	}

	/**
	 * Gets the count of objects owned by a world.
	 * @param world the world in question.
	 * @return the amount of objects owned by the world.
	 */
	public int getObjectsOwnedByWorldCount(TWorld world)
	{
		return getObjectsInQueueCount(objectsOwnedByWorld.get(world));
	}

	/**
	 * Gets the count of objects owned by a room.
	 * @param room the room in question.
	 * @return the amount of objects owned by the room.
	 */
	public int getObjectsOwnedByRoomCount(TRoom room)
	{
		return getObjectsInQueueCount(objectsOwnedByRoom.get(room)); 
	}

	/**
	 * Gets the count of objects owned by a player.
	 * @param player the player in question.
	 * @return the amount of objects owned by the player.
	 */
	public int getObjectsOwnedByPlayerCount(TPlayer player)
	{
		return getObjectsInQueueCount(objectsOwnedByPlayer.get(player)); 
	}

	/**
	 * Gets the count of objects owned by a container.
	 * @param container the container in question.
	 * @return the amount of objects owned by the container.
	 */
	public int getObjectsOwnedByContainerCount(TContainer container)
	{
		return getObjectsInQueueCount(objectsOwnedByContainer.get(container)); 
	}

	/** 
	 * Adds a name to an object.
	 * This name is the one referred to in requests.
	 * The name is converted to lowercase and all contiguous whitespace is replaced with single spaces.
	 * @param object the object to use.
	 * @param name the name to add.
	 */
	public void addObjectName(TObject object, String name) 
	{
		addStringToObjectMap(objectCurrentNames, object, name.toLowerCase().replaceAll("\\s+", " "));
	}

	/** 
	 * Removes a name from an object. 
	 * This name is the one referred to in requests.
	 * The name is converted to lowercase and all contiguous whitespace is replaced with single spaces.
	 * @param object the object to use.
	 * @param name the name to remove.
	 */
	public void removeObjectName(TObject object, String name) 
	{
		removeStringFromObjectMap(objectCurrentNames, object, name.toLowerCase().replaceAll("\\s+", " "));
	}

	/**
	 * Checks if an object contains a particular name.
	 * This name is the one referred to in requests.
	 * @param name the name to check.
	 * @param object the object to use.
	 * @return true if so, false if not.
	 */
	public boolean checkObjectHasName(TObject object, String name)
	{
		return checkStringInObjectMap(objectCurrentNames, object, name);
	}

	/** 
	 * Adds a tag to this object.
	 * This is referred to in tag operations.
	 * Unlike names, tags do not undergo conversion.
	 * @param object the object to use.
	 * @param tag the tag to add.
	 */
	public void addObjectTag(TObject object, String tag) 
	{
		addStringToObjectMap(objectCurrentTags, object, tag);
	}

	/** 
	 * Removes a tag from an object. 
	 * This is referred to in tag operations.
	 * Unlike names, tags do not undergo conversion.
	 * @param object the object to use.
	 * @param tag the tag to remove.
	 */
	public void removeObjectTag(TObject object, String tag) 
	{
		removeStringFromObjectMap(objectCurrentTags, object, tag);
	}

	/**
	 * Checks if an object contains a particular tag.
	 * This is referred to in tag operations.
	 * @param object the object to use.
	 * @param tag the tag to check.
	 * @return true if so, false if not.
	 */
	public boolean checkObjectHasTag(TObject object, String tag)
	{
		return checkStringInObjectMap(objectCurrentTags, object, tag);
	}

	private List<TObject> getObjectsInQueue(Queue<TObject> hash)
	{
		List<TObject> out = new List<TObject>(hash != null ? hash.size() : 1);
		if (hash != null) for (TObject object : hash)
			out.add(object);
		return out; 
	}
	
	private int getObjectsInQueueCount(Queue<TObject> hash)
	{
		return hash != null ? hash.size() : 0; 
	}
	
	private void addStringToObjectMap(HashMap<TObject, CaseInsensitiveHash> table, TObject object, String str)
	{
		CaseInsensitiveHash hash = null;
		if ((hash = table.get(object)) == null)
			table.put(object, hash = new CaseInsensitiveHash());
		hash.put(str);
	}
	
	private void removeStringFromObjectMap(HashMap<TObject, CaseInsensitiveHash> table, TObject object, String str)
	{
		CaseInsensitiveHash hash = null;
		if ((hash = table.get(object)) == null)
			return;

		hash.remove(str);

		// clean up entry if no strings.
		if (hash.isEmpty())
			table.removeUsingKey(object);
	}
	
	private boolean checkStringInObjectMap(HashMap<TObject, CaseInsensitiveHash> table, TObject object, String str)
	{
		CaseInsensitiveHash hash = null;
		if ((hash = table.get(object)) == null)
			return false;

		return hash.contains(str);
	}
	
	@Override
	public void writeStateBytes(TAMEModule module, OutputStream out) throws IOException 
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);

		writeQueueMap(sw, objectsOwnedByWorld);
		writeQueueMap(sw, objectsOwnedByRoom);
		writeQueueMap(sw, objectsOwnedByPlayer);
		writeQueueMap(sw, objectsOwnedByContainer);
		
		sw.writeInt(playerToRoomStack.size());
		for (ObjectPair<TPlayer, Stack<TRoom>> playerPair : playerToRoomStack)
		{
			TPlayer player = playerPair.getKey();
			Stack<TRoom> roomList = playerPair.getValue();
			
			sw.writeString(player.getIdentity(), "UTF-8");
			sw.writeInt(roomList.size());
			for (TRoom room : roomList)
				sw.writeString(room.getIdentity(), "UTF-8");
		}
		
		writeStringMap(sw, objectCurrentNames);
		writeStringMap(sw, objectCurrentTags);
	}

	// Writes a map.
	private void writeQueueMap(SuperWriter sw, HashedQueueMap<? extends TElement, TObject> map) throws IOException 
	{
		sw.writeInt(map.size());
		for (ObjectPair<? extends TElement, Queue<TObject>> elementPair : map)
		{
			TElement element = elementPair.getKey();
			Queue<TObject> objectList = elementPair.getValue();
			
			sw.writeString(element.getIdentity(), "UTF-8");
			sw.writeInt(objectList.size());
			for (TObject object : objectList)
				sw.writeString(object.getIdentity(), "UTF-8");
		}
	}
	
	// Writes a string map.
	private void writeStringMap(SuperWriter sw, HashMap<TObject, CaseInsensitiveHash> map) throws IOException 
	{
		sw.writeInt(map.size());
		for (ObjectPair<TObject, CaseInsensitiveHash> elementPair : map)
		{
			TObject object = elementPair.getKey();
			CaseInsensitiveHash stringList = elementPair.getValue();
			
			sw.writeString(object.getIdentity(), "UTF-8");
			sw.writeInt(stringList.size());
			for (String str : stringList)
				sw.writeString(str, "UTF-8");
		}
	}
	
	@Override
	public void readStateBytes(TAMEModule module, InputStream in) throws IOException 
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		reset();
		
		int oobwsize = sr.readInt();
		while (oobwsize-- > 0)
		{
			String worldIdentity = sr.readString("UTF-8");
			if (!worldIdentity.equals(IDENTITY_CURRENT_WORLD))
				throw new ModuleStateException("World is not named 'world'.");
			
			TWorld world = module.getWorld();
			
			int size = sr.readInt();
			while (size-- > 0)
			{
				String id = sr.readString("UTF-8");
				TObject object = module.getObjectByIdentity(id);
				if (object == null)
					throw new ModuleStateException("Object %s cannot be found!", id);
				addObjectToWorld(object, world);
			}
		}
		
		int oobrsize = sr.readInt();
		while (oobrsize-- > 0)
		{
			String roomIdentity = sr.readString("UTF-8");
			TRoom room = module.getRoomByIdentity(roomIdentity);
			if (room == null)
				throw new ModuleStateException("Room %s cannot be found!", roomIdentity);
			
			int size = sr.readInt();
			while (size-- > 0)
			{
				String id = sr.readString("UTF-8");
				TObject object = module.getObjectByIdentity(id);
				if (object == null)
					throw new ModuleStateException("Object %s cannot be found!", id);
				addObjectToRoom(object, room);
			}
		}
		
		int oobpsize = sr.readInt();
		while (oobpsize-- > 0)
		{
			String playerIdentity = sr.readString("UTF-8");
			TPlayer player = module.getPlayerByIdentity(playerIdentity);
			if (player == null)
				throw new ModuleStateException("Player %s cannot be found!", playerIdentity);
			
			int size = sr.readInt();
			while (size-- > 0)
			{
				String id = sr.readString("UTF-8");
				TObject object = module.getObjectByIdentity(id);
				if (object == null)
					throw new ModuleStateException("Object %s cannot be found!", id);
				addObjectToPlayer(object, player);
			}
		}

		int oobcsize = sr.readInt();
		while (oobcsize-- > 0)
		{
			String containerIdentity = sr.readString("UTF-8");
			TContainer container = module.getContainerByIdentity(containerIdentity);
			if (container == null)
				throw new ModuleStateException("Container %s cannot be found!", containerIdentity);
			
			int size = sr.readInt();
			while (size-- > 0)
			{
				String id = sr.readString("UTF-8");
				TObject object = module.getObjectByIdentity(id);
				if (object == null)
					throw new ModuleStateException("Object %s cannot be found!", id);
				addObjectToContainer(object, container);
			}
		}

		int ptrssize = sr.readInt();
		while (ptrssize-- > 0)
		{
			String playerIdentity = sr.readString("UTF-8");
			TPlayer player = module.getPlayerByIdentity(playerIdentity);
			if (player == null)
				throw new ModuleStateException("Player %s cannot be found!", playerIdentity);
			
			int size = sr.readInt();
			Stack<String> stack = new Stack<String>();
			while (size-- > 0)
			{
				stack.push(sr.readString("UTF-8"));
			}
			
			while (!stack.isEmpty())
			{
				String id = stack.pop();
				TRoom room = module.getRoomByIdentity(id);
				if (room == null)
					throw new ModuleStateException("Object %s cannot be found!", id);
				pushRoomOntoPlayer(player, room);
			}
			
		}

		objectCurrentNames = readStringMap(module, sr);
		objectCurrentTags = readStringMap(module, sr);
	}

	// Reads a string map.
	private HashMap<TObject, CaseInsensitiveHash> readStringMap(TAMEModule module, SuperReader sr) throws IOException 
	{
		int objsize = sr.readInt();
		HashMap<TObject, CaseInsensitiveHash> out = new HashMap<TObject, CaseInsensitiveHash>(objsize);
		while (objsize-- > 0)
		{
			String id = sr.readString("UTF-8");
			TObject object = module.getObjectByIdentity(id);
			if (object == null)
				throw new ModuleStateException("Object %s cannot be found!", id);
			
			int size = sr.readInt();
			while (size-- > 0)
				addStringToObjectMap(out, object, sr.readString("UTF-8"));
		}
		
		return out;
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
