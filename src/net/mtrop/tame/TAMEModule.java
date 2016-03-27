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

import net.mtrop.tame.element.type.TAction;
import net.mtrop.tame.element.type.TContainer;
import net.mtrop.tame.element.type.TObject;
import net.mtrop.tame.element.type.TPlayer;
import net.mtrop.tame.element.type.TRoom;
import net.mtrop.tame.element.type.TWorld;
import net.mtrop.tame.exception.ModuleException;
import net.mtrop.tame.lang.Saveable;

import com.blackrook.commons.Common;
import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

/**
 * An instantiated module.
 * @author Matthew Tropiano
 */
public class TAMEModule implements Saveable
{
	/** Module header. */
	private TAMEModuleHeader header;
	
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
	
	/**
	 * Creates a new module.
	 */
	public TAMEModule()
	{
		this.header = new TAMEModuleHeader();
		
		this.world = null;
		this.actions = new HashMap<String, TAction>(20);
		this.players = new HashMap<String, TPlayer>(2);
		this.rooms = new HashMap<String, TRoom>(10);
		this.objects = new HashMap<String, TObject>(20);
		this.containers = new HashMap<String, TContainer>(5);
		this.actionNameTable = new CaseInsensitiveHashMap<TAction>(15);
		this.digest = null;
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
	public static TAMEModuleHeader readModuleHeader(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		if (!(new String(sr.readBytes(4), "ASCII")).equals("TAME"))
			throw new ModuleException("Not a TAME module.");
			
		TAMEModuleHeader out = new TAMEModuleHeader();
		out.readBytes(in);
		return out;
	}
	
	/**
	 * Returns the module header.
	 * The module header contains a lot of header information about the module.
	 * @return the header.
	 */
	public TAMEModuleHeader getHeader()
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

	HashMap<String, TPlayer> getPlayerList()
	{
		return players;
	}

	HashMap<String, TAction> getActionList()
	{
		return actions;
	}

	HashMap<String, TRoom> getRoomList()
	{
		return rooms;
	}

	HashMap<String, TObject> getObjectList()
	{
		return objects;
	}

	HashMap<String, TContainer> getContainerList()
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
		
		world.writeBytes(out);
		sw.writeInt(actions.size());
		for (ObjectPair<String, TAction> pair : actions)
			pair.getValue().writeBytes(out);
		sw.writeInt(players.size());
		for (ObjectPair<String, TPlayer> pair : players)
			pair.getValue().writeBytes(out);
		sw.writeInt(rooms.size());
		for (ObjectPair<String, TRoom> pair : rooms)
			pair.getValue().writeBytes(out);
		sw.writeInt(objects.size());
		for (ObjectPair<String, TObject> pair : objects)
			pair.getValue().writeBytes(out);
		sw.writeInt(containers.size());
		for (ObjectPair<String, TContainer> pair : containers)
			pair.getValue().writeBytes(out);
		
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
