package net.mtrop.tame.element;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.mtrop.tame.struct.ActionTable;

/**
 * Common engine element object that can be the active or passive target of actions.
 * @author Matthew Tropiano
 */
public abstract class TActionableElement extends TElement 
{
	/** Table of associated actions. */
	private ActionTable actionTable;

	protected TActionableElement()
	{
		super();
		this.actionTable = new ActionTable();
	}
	
	protected TActionableElement(String identity)
	{
		this();
		super.setIdentity(identity);
	}
	
	/**
	 * Gets the action table for "onAction" calls (general).
	 */
	public ActionTable getActionTable()
	{
		return actionTable;
	}
	
	@Override
	public void writeBytes(OutputStream out) throws IOException 
	{
		super.writeBytes(out);
		actionTable.writeBytes(out);
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException 
	{
		super.readBytes(in);
		actionTable = ActionTable.create(in);
	}
	
}
