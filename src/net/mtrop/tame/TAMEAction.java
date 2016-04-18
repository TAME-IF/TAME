/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame;

import net.mtrop.tame.element.TAction;
import net.mtrop.tame.element.TObject;

/**
 * TAME action item.
 * Not to be confused with {@link TAction}.
 * @author Matthew Tropiano
 */
public class TAMEAction
{
	/** Is this from the initial request? */
	private boolean initial;
	/** The action to process. */
	private TAction action; 
	/** The action target to process (modal, open). */
	private String target; 
	/** The action first object to use. */
	private TObject object1; 
	/** The action second object to use. */
	private TObject object2;
	
	private TAMEAction(boolean initial, TAction action, String target, TObject object1, TObject object2)
	{
		this.initial = initial;
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
	public static TAMEAction create(TAction action)
	{
		return new TAMEAction(false, action, null, null, null);
	}
	
	/**
	 * Creates a model or open action item to execute later.
	 * @param action the action to call.
	 * @param target the open target.
	 * @return a new TAME action item.
	 */
	public static TAMEAction create(TAction action, String target)
	{
		return new TAMEAction(false, action, target, null, null);
	}
	
	/**
	 * Creates a transitive action item to execute later.
	 * @param action the action to call.
	 * @param object the first object.
	 * @return a new TAME action item.
	 */
	public static TAMEAction create(TAction action, TObject object)
	{
		return new TAMEAction(false, action, null, object, null);
	}
	
	/**
	 * Creates a ditransitive action item to execute later.
	 * @param action the action to call.
	 * @param object1 the first object.
	 * @param object2 the second object.
	 * @return a new TAME action item.
	 */
	public static TAMEAction create(TAction action, TObject object1, TObject object2)
	{
		return new TAMEAction(false, action, null, object1, object2);
	}

	/**
	 * Creates a general action item to execute later.
	 * @param action the action to call.
	 * @return a new TAME action item.
	 */
	public static TAMEAction createInitial(TAction action)
	{
		return new TAMEAction(true, action, null, null, null);
	}
	
	/**
	 * Creates a model or open action item to execute later.
	 * @param action the action to call.
	 * @param target the open target.
	 * @return a new TAME action item.
	 */
	public static TAMEAction createInitial(TAction action, String target)
	{
		return new TAMEAction(true, action, target, null, null);
	}
	
	/**
	 * Creates a transitive action item to execute later.
	 * @param action the action to call.
	 * @param object the first object.
	 * @return a new TAME action item.
	 */
	public static TAMEAction createInitial(TAction action, TObject object)
	{
		return new TAMEAction(true, action, null, object, null);
	}
	
	/**
	 * Creates a ditransitive action item to execute later.
	 * @param action the action to call.
	 * @param object1 the first object.
	 * @param object2 the second object.
	 * @return a new TAME action item.
	 */
	public static TAMEAction createInitial(TAction action, TObject object1, TObject object2)
	{
		return new TAMEAction(true, action, null, object1, object2);
	}

	/**
	 * Checks if this was the action interpreted from the initial request.
	 * @return true if so, false if not. 
	 */
	public boolean isInitial()
	{
		return initial;
	}
	
	/**
	 * Gets the action to call.
	 * @return the returned action.
	 */
	public TAction getAction()
	{
		return action;
	}

	/**
	 * Gets the open target.
	 * @return the open target, if any.
	 */
	public String getTarget()
	{
		return target;
	}

	/**
	 * Gets the first object.
	 * @return the first object, if any.
	 */
	public TObject getObject1()
	{
		return object1;
	}

	/**
	 * Gets the second object.
	 * @return the second object, if any.
	 */
	public TObject getObject2()
	{
		return object2;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("ActionItem ");
		if (initial)
			sb.append("INITIAL ");
			
		switch (action.getType())
		{
			case GENERAL:
				sb.append('[').append(action.getIdentity()).append(']');
				break;
			case MODAL:
			case OPEN:
				sb.append('[').append(action.getIdentity()).append(", ").append(target).append(']');
				break;
			case TRANSITIVE:
				sb.append('[').append(action.getIdentity()).append(", ").append(object1.getIdentity()).append(']');
				break;
			case DITRANSITIVE:
				sb.append('[').append(action.getIdentity()).append(", ").append(object1.getIdentity()).append(", ").append(object2.getIdentity()).append(']');
				break;
		}
		
		return sb.toString();
	}
	
}
