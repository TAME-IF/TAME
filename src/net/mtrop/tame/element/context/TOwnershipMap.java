/*******************************************************************************
 * Copyright (c) 2009-2013 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  
 * Contributors:
 *     Matt Tropiano - initial API and implementation
 ******************************************************************************/
package net.mtrop.tame.element.context;

import net.mtrop.tame.element.TElement;
import net.mtrop.tame.element.TObject;
import net.mtrop.tame.element.TPlayer;
import net.mtrop.tame.element.TRoom;
import net.mtrop.tame.element.TWorld;

import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.hash.HashedQueueMap;
import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.commons.linkedlist.Stack;
import com.blackrook.commons.list.List;

/**
 * An ownership map for all objects.
 * @author Matthew Tropiano
 */
public class TOwnershipMap
{
	/** Ownership map - objects owned by world. */
	protected HashedQueueMap<TWorld, TObject> objectsOwnedByWorld;
	/** Ownership map - objects owned by room. */
	protected HashedQueueMap<TRoom, TObject> objectsOwnedByRoom;
	/** Ownership map - objects owned by player. */
	protected HashedQueueMap<TPlayer, TObject> objectsOwnedByPlayer;
	/** Reverse lookup object - not saved. */
	protected HashMap<TObject, TElement> objectToElement;
	
	/** Room stack. */
	protected HashMap<TPlayer, Stack<TRoom>> playerToRoomStack;
	
	/**
	 * Creates a new ownership.
	 */
	public TOwnershipMap()
	{
		objectsOwnedByWorld = new HashedQueueMap<TWorld, TObject>(1);
		objectsOwnedByRoom = new HashedQueueMap<TRoom, TObject>(10);
		objectsOwnedByPlayer = new HashedQueueMap<TPlayer, TObject>(4);
		objectToElement = new HashMap<TObject, TElement>(20);
		playerToRoomStack = new HashMap<TPlayer, Stack<TRoom>>(3);
	}
	
	/**
	 * Clears entire ownership table.
	 */
	public void reset()
	{
		objectsOwnedByWorld.clear();
		objectsOwnedByRoom.clear();
		objectsOwnedByPlayer.clear();
		objectToElement.clear();
		playerToRoomStack.clear();
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
	 * Adds an object to a room.
	 * The object is first removed for its owners before it is bound to another. 
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
	 * The object is first removed for its owners before it is bound to another. 
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
	 * The object is first removed for its owners before it is bound to another. 
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
	 * @param player the player to change.
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
	 * Returns if a world possesses an object.
	 * @param world the world to use.
	 * @return true if so, false if not.
	 */
	public boolean checkWorldHasObject(TWorld world, TObject object)
	{
		if (objectsOwnedByWorld.containsKey(world))
			return objectsOwnedByWorld.get(world).contains(object);
		return false;
	}

	/**
	 * Returns if a room possesses an object.
	 * @param room the room to use.
	 * @return true if so, false if not.
	 */
	public boolean checkRoomHasObject(TRoom room, TObject object)
	{
		if (objectsOwnedByRoom.containsKey(room))
			return objectsOwnedByRoom.get(room).contains(object);
		return false;
	}
	
	/**
	 * Returns if a player possesses an object.
	 * @param player the player to use.
	 * @return true if so, false if not.
	 */
	public boolean checkPlayerHasObject(TPlayer player, TObject object)
	{
		if (objectsOwnedByPlayer.containsKey(player))
			return objectsOwnedByPlayer.get(player).contains(object);
		return false;
	}

	/**
	 * Returns if an object has no owner.
	 * @param object the object to use.
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
	 */
	public List<TObject> getObjectsOwnedByWorld(TWorld world)
	{
		return getObjectsInQueue(objectsOwnedByWorld.get(world));
	}

	/**
	 * Gets the list of objects owned by a room.
	 */
	public List<TObject> getObjectsOwnedByRoom(TRoom room)
	{
		return getObjectsInQueue(objectsOwnedByRoom.get(room)); 
	}

	/**
	 * Gets the list of objects owned by a player.
	 */
	public List<TObject> getObjectsOwnedByPlayer(TPlayer player)
	{
		return getObjectsInQueue(objectsOwnedByPlayer.get(player)); 
	}

	/**
	 * Gets the count of objects owned by a world.
	 */
	public int getObjectsOwnedByWorldCount(TWorld world)
	{
		return getObjectsInQueueCount(objectsOwnedByWorld.get(world));
	}

	/**
	 * Gets the count of objects owned by a room.
	 */
	public int getObjectsOwnedByRoomCount(TRoom room)
	{
		return getObjectsInQueueCount(objectsOwnedByRoom.get(room)); 
	}

	/**
	 * Gets the count of objects owned by a player.
	 */
	public int getObjectsOwnedByPlayerCount(TPlayer player)
	{
		return getObjectsInQueueCount(objectsOwnedByPlayer.get(player)); 
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
	

}
