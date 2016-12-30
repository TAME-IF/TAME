/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;

import net.mtrop.tame.element.TAction;
import net.mtrop.tame.element.TContainer;
import net.mtrop.tame.element.TObject;
import net.mtrop.tame.element.TPlayer;
import net.mtrop.tame.element.TRoom;
import net.mtrop.tame.element.TWorld;
import net.mtrop.tame.exception.ModuleException;
import net.mtrop.tame.lang.Saveable;

import com.blackrook.commons.Common;
import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.hash.CaseInsensitiveHash;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.commons.hash.Hash;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.list.List;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

/**
 * An instantiated module.
 * @author Matthew Tropiano
 */
public class TAMEModule implements Saveable
{
	/** Module header. */
	private Header header;
	
	/** The world. */
	private TWorld world;
	/** List of actions. */
	private HashMap<String, TAction> actions;
	/** List of players. */
	private HashMap<String, TPlayer> players;
	/** List of rooms. */
	private HashMap<String, TRoom> rooms;
	/** List of objects. */
	private HashMap<String, TObject> objects;
	/** List of containers. */
	private HashMap<String, TContainer> containers;

	/** Maps action common names to action objects (not saved). */
	private CaseInsensitiveHashMap<TAction> actionNameTable;

	/** Data digest (generated if read by script). */
	private byte[] digest;
	
	
	/** Not saved, used for checking - known identities. */
	private Hash<String> knownIdentities; 
	
	
	/**
	 * Creates a new module.
	 */
	public TAMEModule()
	{
		this.header = new Header();
		
		this.world = null;
		this.actions = new CaseInsensitiveHashMap<TAction>(20);
		this.players = new CaseInsensitiveHashMap<TPlayer>(2);
		this.rooms = new CaseInsensitiveHashMap<TRoom>(10);
		this.objects = new CaseInsensitiveHashMap<TObject>(20);
		this.containers = new CaseInsensitiveHashMap<TContainer>(5);
		this.actionNameTable = new CaseInsensitiveHashMap<TAction>(15);
		this.digest = null;
		
		this.knownIdentities = new CaseInsensitiveHash(200);
	}

	/**
	 * Reads a module from binary content.
	 * @param in the input stream to read from.
	 * @return a deserialized module.
	 * @throws IOException if the stream can't be read.
	 */
	public static TAMEModule create(InputStream in) throws IOException
	{
		TAMEModule out = new TAMEModule();
		out.readBytes(in);
		return out;
	}
	
	/**
	 * Reads a module header and only the header from a module stream.
	 * @param in the input stream to read from.
	 * @return a module header.
	 * @throws IOException if the stream can't be read.
	 */
	public static Header readModuleHeader(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		if (!(new String(sr.readBytes(4), "ASCII")).equals("TAME"))
			throw new ModuleException("Not a TAME module.");
			
		Header out = new Header();
		out.readBytes(in);
		return out;
	}
	
	/**
	 * Returns the module header.
	 * The module header contains a lot of header information about the module.
	 * @return the header.
	 */
	public Header getHeader()
	{
		return header;
	}
	
	/**
	 * Gets the module digest. 
	 * @return the digest or null if not calculated.
	 * @see #calculateDigest()
	 */
	public byte[] getDigest()
	{
		return digest;
	}
	
	/**
	 * Gets the module's world.
	 * @return the world.
	 */
	public TWorld getWorld() 
	{
		return world;
	}

	/**
	 * Sets the module's world.
	 * @param world the world to set.
	 */
	public void setWorld(TWorld world) 
	{
		this.world = world;
	}

	/**
	 * Add an action to this world. World loaders will use this.
	 * @param a	the Action to add.
	 */
	public void addAction(TAction a)
	{
		identityCheck(a.getIdentity());
		actions.put(a.getIdentity(), a);
		for (String s : a.getNames())
			actionNameTable.put(s, a);
	}

	/**
	 * Retrieves an action by name, case-insensitively.
	 * @param name the Action's name.
	 * @return the corresponding Action, or null if not found.
	 */
	public TAction getActionByName(String name)
	{
		return actionNameTable.get(name);
	}

	/**
	 * Retrieves an action by its identity.
	 * @param identity the Action's identity.
	 * @return the corresponding Action, or null if not found.
	 */
	public TAction getActionByIdentity(String identity)
	{
		return actions.get(identity);
	}

	/**
	 * Adds a room to the module.
	 * @param room the Room.
	 */
	public void addRoom(TRoom room)
	{
		identityCheck(room.getIdentity());
		rooms.put(room.getIdentity(), room);
	}

	/**
	 * Retrieves a Room by identity.
	 * @param identity the Room's identity.
	 * @return the corresponding Room, or null if not found.
	 */
	public TRoom getRoomByIdentity(String identity)
	{
		return rooms.get(identity);
	}

	/**
	 * Adds an object to the module.
	 * @param object the Object.
	 */
	public void addObject(TObject object)
	{
		identityCheck(object.getIdentity());
		objects.put(object.getIdentity(), object);
	}

	/**
	 * Retrieves an Object by identity.
	 * @param identity the Object's identity.
	 * @return the corresponding Object, or null if not found.
	 */
	public TObject getObjectByIdentity(String identity)
	{
		return objects.get(identity);
	}

	/**
	 * Adds an container to the module.
	 * @param container the container.
	 */
	public void addContainer(TContainer container)
	{
		identityCheck(container.getIdentity());
		containers.put(container.getIdentity(), container);
	}

	/**
	 * Retrieves a container by identity.
	 * @param identity the container's identity.
	 * @return the corresponding container, or null if not found.
	 */
	public TContainer getContainerByIdentity(String identity)
	{
		return containers.get(identity);
	}

	/**
	 * Adds an player to the module.
	 * @param player the Player's identity.
	 */
	public void addPlayer(TPlayer player)
	{
		identityCheck(player.getIdentity());
		players.put(player.getIdentity(), player);
	}

	/**
	 * Retrieves a Player by identity.
	 * @param identity the Player's identity.
	 * @return the corresponding Player, or null if not found.
	 */
	public TPlayer getPlayerByIdentity(String identity)
	{
		return players.get(identity);
	}

	// Check if we saw the identity before. If so, throw exception.
	private void identityCheck(String identity)
	{
		if (knownIdentities.contains(identity))
			throw new ModuleException("Identity "+identity+" has already been seen.");
		else
			knownIdentities.put(identity);
	}
	
	/**
	 * Gets how many players are in this module.
	 * @return the number of players.
	 */
	public int getPlayerCount()
	{
		return players.size();
	}

	/**
	 * Gets how many actions are in this module.
	 * @return the number of actions.
	 */
	public int getActionCount()
	{
		return actions.size();
	}

	/**
	 * Gets how many rooms are in this module.
	 * @return the number of rooms.
	 */
	public int getRoomCount()
	{
		return rooms.size();
	}

	/**
	 * Gets how many objects are in this module.
	 * @return the number of objects.
	 */
	public int getObjectCount()
	{
		return objects.size();
	}

	/**
	 * Gets how many containers are in this module.
	 * @return the number of containers.
	 */
	public int getContainerCount()
	{
		return containers.size();
	}

	/**
	 * @return an iterable list of player pairs. 
	 */
	public Iterable<ObjectPair<String, TPlayer>> getPlayerList()
	{
		return players;
	}

	/**
	 * @return an iterable list of action pairs. 
	 */
	public Iterable<ObjectPair<String, TAction>> getActionList()
	{
		return actions;
	}

	/**
	 * @return an iterable list of room pairs. 
	 */
	public Iterable<ObjectPair<String, TRoom>> getRoomList()
	{
		return rooms;
	}

	/**
	 * @return an iterable list of object pairs. 
	 */
	public Iterable<ObjectPair<String, TObject>> getObjectList()
	{
		return objects;
	}

	/**
	 * @return an iterable list of container pairs. 
	 */
	public Iterable<ObjectPair<String, TContainer>> getContainerList()
	{
		return containers;
	}

	/**
	 * Calculates this module's digest - only necessary if read 
	 * from a script but never saved. Must be calculated to save
	 * a module state.
	 * @return the calculated digest.
	 * @throws ModuleException if it cannot be calculated.
	 */
	public byte[] calculateDigest()
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream(32768);
		try {
			writeImmutableData(bos);
		} catch (IOException e) {
			throw new ModuleException("Could not calculate digest for module.");
		} finally {
			Common.close(bos);
		}

		byte[] data = bos.toByteArray();
		return (this.digest = Common.sha1(data));
	}
	
	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);

		ByteArrayOutputStream bos = new ByteArrayOutputStream(32768);
		writeImmutableData(bos);
		bos.close();

		byte[] data = bos.toByteArray();
		byte[] digest = Common.sha1(data);
		
		sw.writeBytes("TAME".getBytes("ASCII"));

		header.writeBytes(out);
		
		// write version
		sw.writeByte((byte)0x01);
		
		sw.writeBytes(digest);
		sw.writeByteArray(data);
	}
	
	private void writeImmutableData(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		
		HashMap<String, String> playerMap = new HashMap<>();
		HashMap<String, String> roomMap = new HashMap<>();
		HashMap<String, String> objectMap = new HashMap<>();
		HashMap<String, String> containerMap = new HashMap<>();

		world.writeBytes(out);
		sw.writeInt(actions.size());
		for (ObjectPair<String, TAction> pair : actions)
			pair.getValue().writeBytes(out);
		
		sw.writeInt(players.size());
		for (ObjectPair<String, TPlayer> pair : players)
		{
			TPlayer player = pair.getValue();
			player.writeBytes(out);
			if (player.getParent() != null)
				playerMap.put(player.getIdentity(), player.getParent().getIdentity());
		}
		sw.writeInt(rooms.size());
		for (ObjectPair<String, TRoom> pair : rooms)
		{
			TRoom room = pair.getValue();
			room.writeBytes(out);
			if (room.getParent() != null)
				roomMap.put(room.getIdentity(), room.getParent().getIdentity());
		}
		sw.writeInt(objects.size());
		for (ObjectPair<String, TObject> pair : objects)
		{
			TObject object = pair.getValue();
			object.writeBytes(out);
			if (object.getParent() != null)
				objectMap.put(object.getIdentity(), object.getParent().getIdentity());
		}
		sw.writeInt(containers.size());
		for (ObjectPair<String, TContainer> pair : containers)
		{
			TContainer container = pair.getValue();
			container.writeBytes(out);
			if (container.getParent() != null)
				containerMap.put(container.getIdentity(), container.getParent().getIdentity());
		}
		
		writeStringMap(sw, playerMap);
		writeStringMap(sw, roomMap);
		writeStringMap(sw, objectMap);
		writeStringMap(sw, containerMap);
	}

	// writes a string map.
	private void writeStringMap(SuperWriter sw, HashMap<String, String> map) throws IOException
	{
		sw.writeInt(map.size());
		for (ObjectPair<String, String> pair : map)
		{
			sw.writeString(pair.getKey(), "UTF-8");
			sw.writeString(pair.getValue(), "UTF-8");
		}
	}

	@Override
	public void readBytes(InputStream in) throws IOException
	{
		header = readModuleHeader(in);
		
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);

		if (sr.readByte() != 0x01)
			throw new ModuleException("Module does not have a recognized version.");

		byte[] readDigest = sr.readBytes(20);
		byte[] data = sr.readByteArray();
				
		byte[] digest = Common.sha1(data);
		if (!Arrays.equals(readDigest, digest))
			throw new ModuleException("Module digest does not match data! Possible data corruption!");

		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		readImmutableData(bis);
		this.digest = readDigest;
		bis.close();
	}

	private void readImmutableData(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		
		actions.clear();
		actionNameTable.clear();
		players.clear();
		rooms.clear();
		objects.clear();
		containers.clear();
		
		knownIdentities.clear();

		int size;
		
		world = TWorld.create(in);
		size = sr.readInt();
		while(size-- > 0)
			addAction(TAction.create(in));
		size = sr.readInt();
		while(size-- > 0)
			addPlayer(TPlayer.create(in));
		size = sr.readInt();
		while(size-- > 0)
			addRoom(TRoom.create(in));
		size = sr.readInt();
		while(size-- > 0)
			addObject(TObject.create(in));
		size = sr.readInt();
		while(size-- > 0)
			addContainer(TContainer.create(in));
		
		HashMap<String, String> map; 
		
		map = readStringMap(sr);
		for (ObjectPair<String, String> pair : map)
			getPlayerByIdentity(pair.getKey()).setParent(getPlayerByIdentity(pair.getValue()));
		map = readStringMap(sr);
		for (ObjectPair<String, String> pair : map)
			getRoomByIdentity(pair.getKey()).setParent(getRoomByIdentity(pair.getValue()));
		map = readStringMap(sr);
		for (ObjectPair<String, String> pair : map)
			getObjectByIdentity(pair.getKey()).setParent(getObjectByIdentity(pair.getValue()));
		map = readStringMap(sr);
		for (ObjectPair<String, String> pair : map)
			getContainerByIdentity(pair.getKey()).setParent(getContainerByIdentity(pair.getValue()));
	}
	
	// reads a string map.
	private HashMap<String, String> readStringMap(SuperReader sr) throws IOException
	{
		HashMap<String, String> out = new HashMap<>();
		
		int size = sr.readInt();
		while(size-- > 0)
			out.put(sr.readString("UTF-8"), sr.readString("UTF-8"));
		
		return out;
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

	/**
	 * TAME Module Header.
	 * @author Matthew Tropiano
	 */
	public static class Header implements Saveable
	{
		/** Module attributes. */
		private CaseInsensitiveHashMap<String> attributes;
		
		/**
		 * Creates a new module header.
		 */
		public Header()
		{
			this.attributes = new CaseInsensitiveHashMap<String>(4);
		}
	
		/**
		 * Adds an attribute to the module. Attributes are case-insensitive.
		 * There are a bunch of suggested ones that all clients/servers should read.
		 * @param attribute the attribute name.
		 * @param value the value.
		 */
		public void addAttribute(String attribute, String value)
		{
			attributes.put(attribute, value);
		}
		
		/**
		 * Gets an attribute value from the module. 
		 * Attributes are case-insensitive.
		 * There are a bunch of suggested ones that all clients/servers should read.
		 * @param attribute the attribute name.
		 * @return the corresponding value or null if not found.
		 */
		public String getAttribute(String attribute)
		{
			return attributes.get(attribute);
		}
		
		/**
		 * Gets all of this module's attributes.
		 * @return an array of all of the attributes. Never returns null.
		 */
		public String[] getAllAttributes()
		{
			List<String> outList = new List<>();
			Iterator<String> it = attributes.keyIterator();
			while (it.hasNext())
				outList.add(it.next());
			
			String[] out = new String[outList.size()];
			outList.toArray(out);
			return out;
		}
		
		/**
		 * Gets the attribute map for the header.
		 * @return a reference to the map.
		 */
		public CaseInsensitiveHashMap<String> getAttributeMap()
		{
			return attributes;
		}
		
		@Override
		public void writeBytes(OutputStream out) throws IOException
		{
			SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
			
			sw.writeInt(attributes.size());
			for (ObjectPair<String, String> pair : attributes)
			{
				sw.writeString(pair.getKey(), "UTF-8");
				sw.writeString(pair.getValue(), "UTF-8");
			}
		}
		
		@Override
		public void readBytes(InputStream in) throws IOException
		{
			SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
			attributes.clear();
			int attribCount = sr.readInt();
			while(attribCount-- > 0)
				attributes.put(sr.readString("UTF-8"), sr.readString("UTF-8"));		
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

}
