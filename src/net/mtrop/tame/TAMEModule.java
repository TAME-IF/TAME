package net.mtrop.tame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
	/** Module name. */
	private String moduleName;

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
	
	/**
	 * Creates a new module.
	 */
	public TAMEModule()
	{
		this.actions = new HashMap<String, TAction>(20);
		this.world = null;
		this.players = new HashMap<String, TPlayer>(2);
		this.rooms = new HashMap<String, TRoom>(10);
		this.objects = new HashMap<String, TObject>(20);
		this.containers = new HashMap<String, TContainer>(5);
		this.actionNameTable = new CaseInsensitiveHashMap<TAction>(15);
	}

	public String getModuleName() 
	{
		return moduleName;
	}
	
	public void setModuleName(String moduleName) 
	{
		if (Common.isEmpty(moduleName))
			throw new IllegalArgumentException("Identity cannot be blank.");
		this.moduleName = moduleName;
	}
	
	/**
	 * Gets the module's world.
	 */
	public TWorld getWorld() 
	{
		return world;
	}

	/**
	 * Sets the module's world.
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
	 * @returnb the corresponding Object, or null if not found.
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

	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);

		ByteArrayOutputStream bos = new ByteArrayOutputStream(32768);
		GZIPOutputStream gzout = new GZIPOutputStream(bos);
		writeImmutableData(gzout);
		gzout.flush();

		byte[] data = bos.toByteArray();
		byte[] digest = Common.sha1(data);
		
		sw.writeBytes("TAME".getBytes("ASCII"));
		sw.writeByte((byte)0x01);
		sw.writeString(moduleName, "UTF-8");
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
		
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		if (!sr.readString("ASCII").equals("TAME"))
			throw new ModuleException("Module is not a TAME module.");
			
		if (sr.readByte() != 0x01)
			throw new ModuleException("Module does not have a recognized version.");

		moduleName = sr.readString("UTF-8");
		byte[] readDigest = sr.readBytes(20);
		byte[] data = sr.readByteArray();
				
		byte[] digest = Common.sha1(data);
		if (!Arrays.equals(readDigest, digest))
			throw new ModuleException("Module digest does not match data!");

		GZIPInputStream gzin = new GZIPInputStream(new ByteArrayInputStream(data));
		readImmutableData(gzin);
		gzin.close();
	}

	private void readImmutableData(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		
		actions.clear();
		actionNameTable.clear();
		players.clear();
		rooms.clear();
		objects.clear();

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
