/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.element.context;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

import com.tameif.tame.TAMEConstants;
import com.tameif.tame.TAMEModule;
import com.tameif.tame.element.ObjectContainer;
import com.tameif.tame.element.TElement;
import com.tameif.tame.element.TObject;
import com.tameif.tame.element.TPlayer;
import com.tameif.tame.element.TRoom;
import com.tameif.tame.exception.ModuleStateException;
import com.tameif.tame.lang.StateSaveable;
import com.tameif.tame.lang.Value;
import com.tameif.tame.struct.CaseInsensitiveHash;
import com.tameif.tame.struct.HashDequeMap;
import com.tameif.tame.struct.SerialReader;
import com.tameif.tame.struct.SerialWriter;

/**
 * An ownership map for all objects.
 * @author Matthew Tropiano
 */
public class TOwnershipMap implements StateSaveable, TAMEConstants
{
	/** Current player. */
	private TPlayer currentPlayer;
	/** Room stack. */
	protected Map<TPlayer, Deque<TRoom>> playerToRoomStack;
	
	// This must be a queue - order added should be reflected in retrieval and presentation.
	/** Ownership map - objects owned by elements. */
	protected HashDequeMap<ObjectContainer, TObject> objectsOwnedByElements;

	/** Map of object to its current names. */
	protected Map<TObject, CaseInsensitiveHash> objectCurrentNames;
	/** Map of object to its current tags. */
	protected Map<TObject, CaseInsensitiveHash> objectCurrentTags;

	/** Reverse lookup object - not saved. */
	protected Map<TObject, ObjectContainer> objectsToElement;

	/**
	 * Creates a new ownership.
	 */
	public TOwnershipMap()
	{
		currentPlayer = null;
		playerToRoomStack = new HashMap<TPlayer, Deque<TRoom>>(3);
		objectsOwnedByElements =  new HashDequeMap<ObjectContainer, TObject>(20);
		objectCurrentNames = new HashMap<TObject, CaseInsensitiveHash>();
		objectCurrentTags = new HashMap<TObject, CaseInsensitiveHash>();
		objectsToElement = new HashMap<TObject, ObjectContainer>(20);
	}
	
	/**
	 * Clears entire ownership table.
	 */
	public void reset()
	{
		currentPlayer = null;
		playerToRoomStack.clear();
		objectsOwnedByElements.clear();
		objectCurrentNames.clear();
		objectCurrentTags.clear();
		objectsToElement.clear();
	}
	
	/**
	 * Removes an object from all owners.
	 * @param object the object to remove.
	 */
	public void removeObject(TObject object)
	{
		ObjectContainer element = objectsToElement.remove(object);
		if (element == null)
			return;
		
		objectsOwnedByElements.removeValue(element, object);
	}
	
	/**
	 * Removes a player from all rooms.
	 * @param player the player to remove.
	 */
	public void removePlayer(TPlayer player)
	{
		playerToRoomStack.remove(player);
	}

	/**
	 * Adds an object to an element that can hold objects.
	 * The object is first removed from its owners before it is bound to another. 
	 * @param object the object to add.
	 * @param element the target element.
	 */
	public void addObjectToElement(TObject object, ObjectContainer element)
	{
		removeObject(object);
		objectsToElement.put(object, element);
		objectsOwnedByElements.add(element, object);
	}
	
	/**
	 * Adds a player to a room and discards the stack.
	 * @param player the player to add.
	 * @param room the target room.
	 */
	public void addPlayerToRoom(TPlayer player, TRoom room)
	{
		removePlayer(player);
		Deque<TRoom> stack = playerToRoomStack.get(player);
		if (stack == null)
		{
			stack = new LinkedList<TRoom>();
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
		Deque<TRoom> stack = playerToRoomStack.get(player);
		if (stack == null)
		{
			stack = new LinkedList<TRoom>();
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
		Deque<TRoom> stack = playerToRoomStack.get(player);
		if (stack == null)
			return null;
		
		TRoom out = stack.pollFirst();
		if (stack.size() == 0)
			playerToRoomStack.remove(player);
		return out;
	}
	
	/**
	 * Checks if an element possesses an object.
	 * @param element the element to use.
	 * @param object the object in question.
	 * @return true if so, false if not.
	 */
	public boolean checkElementHasObject(ObjectContainer element, TObject object)
	{
		if (objectsOwnedByElements.containsKey(element))
			return objectsOwnedByElements.get(element).contains(object);
		return false;
	}
	
	/**
	 * Checks if an object has no owner.
	 * @param object the object in question.
	 * @return true if so, false if not.
	 */
	public boolean checkObjectHasNoOwner(TObject object)
	{
		return !objectsToElement.containsKey(object);
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
	 * Sets the current player.
	 * @param player the player to set as current player.
	 */
	public void setCurrentPlayer(TPlayer player)
	{
		currentPlayer = player;
	}

	/**
	 * Gets the current player by the current player id.
	 * @return the current player, or null if not set.
	 */
	public TPlayer getCurrentPlayer()
	{
		return currentPlayer;
	}

	/**
	 * Returns the current room for a player.
	 * @param player the player to use.
	 * @return the current room or null if no room.
	 */
	public TRoom getCurrentRoom(TPlayer player)
	{
		Deque<TRoom> stack = playerToRoomStack.get(player);
		return stack != null ? stack.peekFirst() : null;
	}

	/**
	 * Gets the current room by the current room id.
	 * @return the current room, or null if not set or current player is not set.
	 */
	public TRoom getCurrentRoom()
	{
		return currentPlayer != null ? getCurrentRoom(currentPlayer) : null;
	}

	/**
	 * Gets the list of objects owned by an element.
	 * @param element the element in question.
	 * @return the list of all objects owned by the element.
	 */
	public Iterable<TObject> getObjectsOwnedByElement(ObjectContainer element)
	{
		return getObjectsInQueue(objectsOwnedByElements.get(element));
	}

	/**
	 * Gets the count of objects owned by an element.
	 * @param element the element in question.
	 * @return the amount of objects owned by the element.
	 */
	public int getObjectsOwnedByElementCount(ObjectContainer element)
	{
		return getObjectsInQueueCount(objectsOwnedByElements.get(element));
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
		name = name.trim().replaceAll("\\s+", " ");
		addStringToObjectMap(objectCurrentNames, object, name);
		for (String determiner : object.getDeterminers())
		{
			determiner = determiner.trim().replaceAll("\\s+", " ");
			addStringToObjectMap(objectCurrentNames, object, determiner + " " + name);
		}
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
		name = name.trim().replaceAll("\\s+", " ");
		removeStringFromObjectMap(objectCurrentNames, object, name);
		for (String determiner : object.getDeterminers())
		{
			determiner = determiner.trim().replaceAll("\\s+", " ");
			removeStringFromObjectMap(objectCurrentNames, object, determiner + " " + name);
		}
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
	 * Unlike names, tags do not undergo whitespace conversion.
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
	 * Unlike names, tags do not undergo whitespace conversion.
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
	 * Unlike names, tags do not undergo whitespace conversion.
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
		List<TObject> out = new ArrayList<TObject>(hash != null ? hash.size() : 1);
		if (hash != null) for (TObject object : hash)
			out.add(object);
		return out; 
	}
	
	private int getObjectsInQueueCount(Queue<TObject> hash)
	{
		return hash != null ? hash.size() : 0; 
	}
	
	private void addStringToObjectMap(Map<TObject, CaseInsensitiveHash> table, TObject object, String str)
	{
		CaseInsensitiveHash hash = null;
		if ((hash = table.get(object)) == null)
			table.put(object, hash = new CaseInsensitiveHash());
		hash.put(str);
	}
	
	private void removeStringFromObjectMap(Map<TObject, CaseInsensitiveHash> table, TObject object, String str)
	{
		CaseInsensitiveHash hash = null;
		if ((hash = table.get(object)) == null)
			return;

		hash.remove(str);

		// clean up entry if no strings.
		if (hash.isEmpty())
			table.remove(object);
	}
	
	private boolean checkStringInObjectMap(Map<TObject, CaseInsensitiveHash> table, TObject object, String str)
	{
		CaseInsensitiveHash hash = null;
		if ((hash = table.get(object)) == null)
			return false;

		return hash.contains(str);
	}
	
	@Override
	public void writeStateBytes(TAMEModule module, AtomicLong referenceCounter, Map<Object, Long> referenceSet, OutputStream out) throws IOException 
	{
		SerialWriter sw = new SerialWriter(SerialWriter.LITTLE_ENDIAN);

		writeQueueMap(out, objectsOwnedByElements);
		
		sw.writeBoolean(out, currentPlayer != null);
		if (currentPlayer != null)
			sw.writeString(out, currentPlayer.getIdentity(), "UTF-8");

		sw.writeInt(out, playerToRoomStack.size());
		for (Map.Entry<TPlayer, Deque<TRoom>> playerPair : playerToRoomStack.entrySet())
		{
			TPlayer player = playerPair.getKey();
			Deque<TRoom> roomList = playerPair.getValue();
			
			sw.writeString(out, player.getIdentity(), "UTF-8");
			sw.writeInt(out, roomList.size());
			for (TRoom room : roomList)
				sw.writeString(out, room.getIdentity(), "UTF-8");
		}
		
		writeStringMap(out, objectCurrentNames);
		writeStringMap(out, objectCurrentTags);
	}

	// Writes a map.
	private void writeQueueMap(OutputStream out, HashDequeMap<? extends ObjectContainer, TObject> map) throws IOException 
	{
		SerialWriter sw = new SerialWriter(SerialWriter.LITTLE_ENDIAN);
		sw.writeInt(out, map.size());
		for (Map.Entry<? extends ObjectContainer, Deque<TObject>> elementPair : map.entrySet())
		{
			ObjectContainer element = elementPair.getKey();
			Queue<TObject> objectList = elementPair.getValue();
			
			sw.writeString(out, ((TElement)element).getIdentity(), "UTF-8");
			sw.writeInt(out, objectList.size());
			for (TObject object : objectList)
				sw.writeString(out, object.getIdentity(), "UTF-8");
		}
	}
	
	// Writes a string map.
	private void writeStringMap(OutputStream out, Map<TObject, CaseInsensitiveHash> map) throws IOException 
	{
		SerialWriter sw = new SerialWriter(SerialWriter.LITTLE_ENDIAN);
		sw.writeInt(out, map.size());
		for (Map.Entry<TObject, CaseInsensitiveHash> elementPair : map.entrySet())
		{
			TObject object = elementPair.getKey();
			CaseInsensitiveHash stringList = elementPair.getValue();
			
			sw.writeString(out, object.getIdentity(), "UTF-8");
			sw.writeInt(out, stringList.size());
			for (String str : stringList)
				sw.writeString(out, str, "UTF-8");
		}
	}
	
	@Override
	public void readStateBytes(TAMEModule module, Map<Long, Value> referenceMap, InputStream in) throws IOException 
	{
		SerialReader sr = new SerialReader(SerialReader.LITTLE_ENDIAN);
		reset();
		
		int elementsize = sr.readInt(in);
		while (elementsize-- > 0)
		{
			String identity = sr.readString(in, "UTF-8");
			TElement element = module.getElementByIdentity(identity);
			if (element == null)
				throw new ModuleStateException("Element %s cannot be found!", identity);
			
			int size = sr.readInt(in);
			while (size-- > 0)
			{
				String id = sr.readString(in, "UTF-8");
				TObject object = module.getObjectByIdentity(id);
				if (object == null)
					throw new ModuleStateException("Object %s cannot be found!", id);
				addObjectToElement(object, (ObjectContainer)element);
			}
		}
		
		// has current player.
		if (sr.readBoolean(in))
		{
			String identity = sr.readString(in, "UTF-8");
			currentPlayer = module.getPlayerByIdentity(identity);
			if (currentPlayer == null)
				throw new ModuleStateException("Expected player '%s' in module context!", identity);
		}
		else
			currentPlayer = null;

		int ptrssize = sr.readInt(in);
		while (ptrssize-- > 0)
		{
			String playerIdentity = sr.readString(in, "UTF-8");
			TPlayer player = module.getPlayerByIdentity(playerIdentity);
			if (player == null)
				throw new ModuleStateException("Player %s cannot be found!", playerIdentity);
			
			int size = sr.readInt(in);
			Deque<String> stack = new LinkedList<>();
			while (size-- > 0)
				stack.push(sr.readString(in, "UTF-8"));
			
			while (!stack.isEmpty())
			{
				String id = stack.pollFirst();
				TRoom room = module.getRoomByIdentity(id);
				if (room == null)
					throw new ModuleStateException("Object %s cannot be found!", id);
				pushRoomOntoPlayer(player, room);
			}
			
		}

		objectCurrentNames = readStringMap(module, in);
		objectCurrentTags = readStringMap(module, in);
	}

	// Reads a string map.
	private HashMap<TObject, CaseInsensitiveHash> readStringMap(TAMEModule module, InputStream in) throws IOException 
	{
		SerialReader sr = new SerialReader(SerialReader.LITTLE_ENDIAN);
		int objsize = sr.readInt(in);
		HashMap<TObject, CaseInsensitiveHash> out = objsize <= 0 
				? new HashMap<TObject, CaseInsensitiveHash>() 
				: new HashMap<TObject, CaseInsensitiveHash>(objsize);
		while (objsize-- > 0)
		{
			String id = sr.readString(in, "UTF-8");
			TObject object = module.getObjectByIdentity(id);
			if (object == null)
				throw new ModuleStateException("Object %s cannot be found!", id);
			
			int size = sr.readInt(in);
			while (size-- > 0)
				addStringToObjectMap(out, object, sr.readString(in, "UTF-8"));
		}
		
		return out;
	}
	
}
