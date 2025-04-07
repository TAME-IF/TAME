/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.tameif.tame.element.TAction;
import com.tameif.tame.element.TContainer;
import com.tameif.tame.element.TElement;
import com.tameif.tame.element.TObject;
import com.tameif.tame.element.TPlayer;
import com.tameif.tame.element.TRoom;
import com.tameif.tame.element.TWorld;
import com.tameif.tame.exception.ModuleException;
import com.tameif.tame.lang.Saveable;
import com.tameif.tame.struct.CaseInsensitiveStringSet;
import com.tameif.tame.struct.CaseInsensitiveStringMap;
import com.tameif.tame.struct.EncodingUtils;
import com.tameif.tame.struct.IOUtils;
import com.tameif.tame.struct.SerialReader;
import com.tameif.tame.struct.SerialWriter;

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
	private CaseInsensitiveStringMap<TAction> actions;
	/** List of players. */
	private CaseInsensitiveStringMap<TPlayer> players;
	/** List of rooms. */
	private CaseInsensitiveStringMap<TRoom> rooms;
	/** List of objects. */
	private CaseInsensitiveStringMap<TObject> objects;
	/** List of containers. */
	private CaseInsensitiveStringMap<TContainer> containers;

	/** Maps action common names to action objects (not saved). */
	private CaseInsensitiveStringMap<TAction> actionNameTable;

	/** Data digest (generated if read by script). */
	private byte[] digest;
	
	
	/** Not saved, used for checking - known identities. */
	private CaseInsensitiveStringSet knownIdentities; 
	
	/**
	 * Creates a new module.
	 */
	public TAMEModule()
	{
		this.header = new Header();
		
		this.world = null;
		this.actions = new CaseInsensitiveStringMap<TAction>(20);
		this.players = new CaseInsensitiveStringMap<TPlayer>(2);
		this.rooms = new CaseInsensitiveStringMap<TRoom>(10);
		this.objects = new CaseInsensitiveStringMap<TObject>(20);
		this.containers = new CaseInsensitiveStringMap<TContainer>(5);
		this.actionNameTable = new CaseInsensitiveStringMap<TAction>(15);
		this.digest = null;
		
		this.knownIdentities = new CaseInsensitiveStringSet(200);
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
		SerialReader sr = new SerialReader(SerialReader.LITTLE_ENDIAN);
		if (!(new String(sr.readBytes(in, 4), "ASCII")).equals("TAME"))
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
	 * Sets the module's world.
	 * @param world the world to set.
	 */
	public void setWorld(TWorld world) 
	{
		this.world = world;
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

	/**
	 * Retrieves an element by identity.
	 * @param identity the element's identity.
	 * @return the corresponding element, or null if not found.
	 */
	public TElement getElementByIdentity(String identity)
	{
		TElement out;
		if (TAMEConstants.IDENTITY_CURRENT_WORLD.equalsIgnoreCase(identity))
			return world;
		else if ((out = objects.get(identity)) != null)
			return out;
		else if ((out = containers.get(identity)) != null)
			return out;
		else if ((out = rooms.get(identity)) != null)
			return out;
		else if ((out = players.get(identity)) != null)
			return out;
		else
			return null;
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
	public Iterable<Map.Entry<String, TPlayer>> getPlayerList()
	{
		return players;
	}

	/**
	 * @return an iterable list of action pairs. 
	 */
	public Iterable<Map.Entry<String, TAction>> getActionList()
	{
		return actions;
	}

	/**
	 * @return an iterable list of room pairs. 
	 */
	public Iterable<Map.Entry<String, TRoom>> getRoomList()
	{
		return rooms;
	}

	/**
	 * @return an iterable list of object pairs. 
	 */
	public Iterable<Map.Entry<String, TObject>> getObjectList()
	{
		return objects;
	}

	/**
	 * @return an iterable list of container pairs. 
	 */
	public Iterable<Map.Entry<String, TContainer>> getContainerList()
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
			IOUtils.close(bos);
		}

		byte[] data = bos.toByteArray();
		return (this.digest = EncodingUtils.sha1(data));
	}
	
	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SerialWriter sw = new SerialWriter(SerialWriter.LITTLE_ENDIAN);

		ByteArrayOutputStream bos = new ByteArrayOutputStream(32768);
		writeImmutableData(bos);
		bos.close();

		byte[] data = bos.toByteArray();
		byte[] digest = EncodingUtils.sha1(data);
		
		sw.writeBytes(out, "TAME".getBytes("ASCII"));

		header.writeBytes(out);
		
		// write version
		sw.writeByte(out, (byte)0x01);
		
		sw.writeBytes(out, digest);
		sw.writeByteArray(out, data);
	}
	
	private void writeImmutableData(OutputStream out) throws IOException
	{
		SerialWriter sw = new SerialWriter(SerialWriter.LITTLE_ENDIAN);
		
		HashMap<String, String> playerMap = new HashMap<>();
		HashMap<String, String> roomMap = new HashMap<>();
		HashMap<String, String> objectMap = new HashMap<>();
		HashMap<String, String> containerMap = new HashMap<>();

		world.writeBytes(out);
		sw.writeInt(out, actions.size());
		for (Map.Entry<String, TAction> pair : actions)
			pair.getValue().writeBytes(out);
		
		sw.writeInt(out, players.size());
		for (Map.Entry<String, TPlayer> pair : players)
		{
			TPlayer player = pair.getValue();
			player.writeBytes(out);
			if (player.getParent() != null)
				playerMap.put(player.getIdentity(), player.getParent().getIdentity());
		}
		sw.writeInt(out, rooms.size());
		for (Map.Entry<String, TRoom> pair : rooms)
		{
			TRoom room = pair.getValue();
			room.writeBytes(out);
			if (room.getParent() != null)
				roomMap.put(room.getIdentity(), room.getParent().getIdentity());
		}
		sw.writeInt(out, objects.size());
		for (Map.Entry<String, TObject> pair : objects)
		{
			TObject object = pair.getValue();
			object.writeBytes(out);
			if (object.getParent() != null)
				objectMap.put(object.getIdentity(), object.getParent().getIdentity());
		}
		sw.writeInt(out, containers.size());
		for (Map.Entry<String, TContainer> pair : containers)
		{
			TContainer container = pair.getValue();
			container.writeBytes(out);
			if (container.getParent() != null)
				containerMap.put(container.getIdentity(), container.getParent().getIdentity());
		}
		
		writeStringMap(out, playerMap);
		writeStringMap(out, roomMap);
		writeStringMap(out, objectMap);
		writeStringMap(out, containerMap);
	}

	// writes a string map.
	private void writeStringMap(OutputStream out, HashMap<String, String> map) throws IOException
	{
		SerialWriter sw = new SerialWriter(SerialWriter.LITTLE_ENDIAN);
		sw.writeInt(out, map.size());
		for (Map.Entry<String, String> pair : map.entrySet())
		{
			sw.writeString(out, pair.getKey(), "UTF-8");
			sw.writeString(out, pair.getValue(), "UTF-8");
		}
	}

	@Override
	public void readBytes(InputStream in) throws IOException
	{
		header = readModuleHeader(in);
		
		SerialReader sr = new SerialReader(SerialReader.LITTLE_ENDIAN);

		if (sr.readByte(in) != 0x01)
			throw new ModuleException("Module does not have a recognized version.");

		byte[] readDigest = sr.readBytes(in, 20);
		byte[] data = sr.readByteArray(in);
				
		byte[] digest = EncodingUtils.sha1(data);
		if (!Arrays.equals(readDigest, digest))
			throw new ModuleException("Module digest does not match data! Possible data corruption!");

		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		readImmutableData(bis);
		this.digest = readDigest;
		bis.close();
	}

	private void readImmutableData(InputStream in) throws IOException
	{
		SerialReader sr = new SerialReader(SerialReader.LITTLE_ENDIAN);
		
		actions.clear();
		actionNameTable.clear();
		players.clear();
		rooms.clear();
		objects.clear();
		containers.clear();
		
		knownIdentities.clear();

		int size;
		
		world = TWorld.create(in);
		size = sr.readInt(in);
		while(size-- > 0)
			addAction(TAction.create(in));
		size = sr.readInt(in);
		while(size-- > 0)
			addPlayer(TPlayer.create(in));
		size = sr.readInt(in);
		while(size-- > 0)
			addRoom(TRoom.create(in));
		size = sr.readInt(in);
		while(size-- > 0)
			addObject(TObject.create(in));
		size = sr.readInt(in);
		while(size-- > 0)
			addContainer(TContainer.create(in));
		
		HashMap<String, String> map; 
		
		map = readStringMap(in);
		for (Map.Entry<String, String> pair : map.entrySet())
			getPlayerByIdentity(pair.getKey()).setParent(getPlayerByIdentity(pair.getValue()));
		map = readStringMap(in);
		for (Map.Entry<String, String> pair : map.entrySet())
			getRoomByIdentity(pair.getKey()).setParent(getRoomByIdentity(pair.getValue()));
		map = readStringMap(in);
		for (Map.Entry<String, String> pair : map.entrySet())
			getObjectByIdentity(pair.getKey()).setParent(getObjectByIdentity(pair.getValue()));
		map = readStringMap(in);
		for (Map.Entry<String, String> pair : map.entrySet())
			getContainerByIdentity(pair.getKey()).setParent(getContainerByIdentity(pair.getValue()));
	}
	
	// reads a string map.
	private HashMap<String, String> readStringMap(InputStream in) throws IOException
	{
		SerialReader sr = new SerialReader(SerialReader.LITTLE_ENDIAN);
		HashMap<String, String> out = new HashMap<>();
		
		int size = sr.readInt(in);
		while(size-- > 0)
			out.put(sr.readString(in, "UTF-8"), sr.readString(in, "UTF-8"));
		
		return out;
	}

	/**
	 * TAME Module Header.
	 * @author Matthew Tropiano
	 */
	public static class Header implements Saveable
	{
		/** Module attributes. */
		private CaseInsensitiveStringMap<String> attributes;
		
		/**
		 * Creates a new module header.
		 */
		public Header()
		{
			this.attributes = new CaseInsensitiveStringMap<String>(4);
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
		 * Gets all of this module header's attributes.
		 * @return an array of all of the attributes. Never returns null.
		 */
		public String[] getAllAttributes()
		{
			List<String> outList = new LinkedList<>();
			for (Map.Entry<String, String> pair : attributes)
				outList.add(pair.getKey());
			String[] out = new String[outList.size()];
			outList.toArray(out);
			return out;
		}
		
		/**
		 * Gets the attribute map for the header.
		 * @return a reference to the map.
		 */
		public CaseInsensitiveStringMap<String> getAttributeMap()
		{
			return attributes;
		}
		
		@Override
		public void writeBytes(OutputStream out) throws IOException
		{
			SerialWriter sw = new SerialWriter(SerialWriter.LITTLE_ENDIAN);
			sw.writeInt(out, attributes.size());
			for (Map.Entry<String, String> pair : attributes)
			{
				sw.writeString(out, pair.getKey(), "UTF-8");
				sw.writeString(out, pair.getValue(), "UTF-8");
			}
		}
		
		@Override
		public void readBytes(InputStream in) throws IOException
		{
			SerialReader sr = new SerialReader(SerialReader.LITTLE_ENDIAN);
			attributes.clear();
			int attribCount = sr.readInt(in);
			while(attribCount-- > 0)
				attributes.put(sr.readString(in, "UTF-8"), sr.readString(in, "UTF-8"));		
		}
	
	}

}
