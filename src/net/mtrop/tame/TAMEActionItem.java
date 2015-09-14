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
package net.mtrop.tame;

import net.mtrop.tame.world.TAction;
import net.mtrop.tame.world.TObject;

/**
 * TAME action item.
 * @author Matthew Tropiano
 */
public class TAMEActionItem
{
	/** The action to process. */
	protected TAction action; 
	/** The action target to process (modal, open). */
	protected String target; 
	/** The action first object to use. */
	protected TObject object1; 
	/** The action second object to use. */
	protected TObject object2;
	
	private TAMEActionItem(TAction action, String target, TObject object1, TObject object2)
	{
		this.action = action;
		this.target = target;
		this.object1 = object1;
		this.object2 = object2;
	}

	/**
	 * Creates a general action item to execute later.
	 * @param action the action to call.
	 * @return a new TAME action item.
	 */
	public static TAMEActionItem create(TAction action)
	{
		return new TAMEActionItem(action, null, null, null);
	}
	
	/**
	 * Creates a model or open action item to execute later.
	 * @param action the action to call.
	 * @param target the open target.
	 * @return a new TAME action item.
	 */
	public static TAMEActionItem create(TAction action, String target)
	{
		return new TAMEActionItem(action, target, null, null);
	}
	
	/**
	 * Creates a transitive action item to execute later.
	 * @param action the action to call.
	 * @param object the first object.
	 * @return a new TAME action item.
	 */
	public static TAMEActionItem create(TAction action, TObject object)
	{
		return new TAMEActionItem(action, null, object, null);
	}
	
	/**
	 * Creates a ditransitive action item to execute later.
	 * @param action the action to call.
	 * @param object1 the first object.
	 * @param object2 the second object.
	 * @return a new TAME action item.
	 */
	public static TAMEActionItem create(TAction action, TObject object1, TObject object2)
	{
		return new TAMEActionItem(action, null, object1, object2);
	}

	/**
	 * Gets the action to call.
	 */
	public TAction getAction()
	{
		return action;
	}

	/**
	 * Gets the open target.
	 */
	public String getTarget()
	{
		return target;
	}

	/**
	 * Gets the first object.
	 */
	public TObject getObject1()
	{
		return object1;
	}

	/**
	 * Gets the second object.
	 */
	public TObject getObject2()
	{
		return object2;
	}

}
