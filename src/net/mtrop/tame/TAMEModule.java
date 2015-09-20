package net.mtrop.tame;

import net.mtrop.tame.element.TAction;
import net.mtrop.tame.element.TObject;
import net.mtrop.tame.element.TPlayer;
import net.mtrop.tame.element.TRoom;
import net.mtrop.tame.element.TWorld;

import com.blackrook.commons.hash.CaseInsensitiveHashMap;

/**
 * An instantiated module.
 * @author Matthew Tropiano
 */
public class TAMEModule
{
	public static final byte CURRENT_FORMAT_VERSION = 0;
	
	/** Module name. */
	protected String moduleName;
	/** Module magic number. */
	protected long moduleId;

	/** The world. */
	protected TWorld world;
	/** List of actions. */
	private CaseInsensitiveHashMap<TAction> actionList;
	/** Maps action common names to action objects. */
	private CaseInsensitiveHashMap<TAction> actionNameTable;
	/** List of players. */
	protected CaseInsensitiveHashMap<TPlayer> playerList;
	/** List of rooms. */
	protected CaseInsensitiveHashMap<TRoom> roomList;
	/** List of objects. */
	protected CaseInsensitiveHashMap<TObject> objectList;
	
	/**
	 * Creates a new module.
	 */
	public TAMEModule()
	{
		this.moduleId = -1L;
		this.world = null;
		this.actionList = new CaseInsensitiveHashMap<TAction>(20);
		this.actionNameTable = new CaseInsensitiveHashMap<TAction>(15);
		this.playerList = new CaseInsensitiveHashMap<TPlayer>(2);
		this.roomList = new CaseInsensitiveHashMap<TRoom>(10);
		this.objectList = new CaseInsensitiveHashMap<TObject>(20);
	}

	public String getModuleName() 
	{
		return moduleName;
	}
	
	public void setModuleName(String moduleName) 
	{
		this.moduleName = moduleName;
	}
	
	/**
	 * Gets this module's "magic" id.
	 */
	public long getModuleId()
	{
		return moduleId;
	}
	
	public void setModuleId(long moduleId)
	{
		this.moduleId = moduleId;
	}

	/**
	 * Add an action to this world. World loaders will use this.
	 * @param a	the Action to add.
	 */
	public void addAction(TAction a)
	{
		actionList.put(a.getIdentity(), a);
		for (String s : a.getNames())
			actionNameTable.put(s, a);
	}

	/**
	 * Retrieves an Action's id by name, case-insensitively.
	 * @param name the Action's name.
	 * @return the corresponding Action, or null if not found.
	 */
	public TAction getActionByName(String name)
	{
		return actionNameTable.get(name);
	}

	/**
	 * Gets the reference to the list of actions.
	 */
	public CaseInsensitiveHashMap<TAction> getActionList()
	{
		return actionList;
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
	 * Retrieves a Room by identity, case-insensitively.
	 * @param identity the Room's identity.
	 * @return the corresponding Room, or null if not found.
	 */
	public TRoom getRoomByIdentity(String identity)
	{
		return roomList.get(identity);
	}

	/**
	 * Gets the reference to the list of rooms.
	 */
	public CaseInsensitiveHashMap<TRoom> getRoomList()
	{
		return roomList;
	}

	/**
	 * Retrieves an Object by identity, case-insensitively.
	 * @param identity the Object's identity.
	 * @returnb the corresponding Object, or null if not found.
	 */
	public TObject getObjectByIdentity(String identity)
	{
		return objectList.get(identity);
	}

	/**
	 * Gets the reference to the list of objects.
	 */
	public CaseInsensitiveHashMap<TObject> getObjectList()
	{
		return objectList;
	}

	/**
	 * Retrieves a Player by identity, case-insensitively.
	 * @param identity the Player's identity.
	 * @return the corresponding Player, or null if not found.
	 */
	public TPlayer getPlayerByIdentity(String identity)
	{
		return playerList.get(identity);
	}

	/**
	 * Gets the reference to the list of players.
	 */
	public CaseInsensitiveHashMap<TPlayer> getPlayerList()
	{
		return playerList;
	}
	
}
