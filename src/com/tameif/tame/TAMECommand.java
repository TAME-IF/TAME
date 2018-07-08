/*******************************************************************************
 * Copyright (c) 2015-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame;

import com.tameif.tame.element.TAction;
import com.tameif.tame.element.TObject;

/**
 * TAME processed command item.
 * Not to be confused with {@link TAction}.
 * @author Matthew Tropiano
 */
public class TAMECommand
{
	/** The action to process. */
	private TAction action; 
	/** The action target to process (modal, open). */
	private String target; 
	/** The action first object to use. */
	private TObject object1; 
	/** The action second object to use. */
	private TObject object2;
	
	private TAMECommand(TAction action, String target, TObject object1, TObject object2)
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
	 * @throws IllegalArgumentException if the action provided is not a general one.
	 */
	public static TAMECommand create(TAction action)
	{
		if (action.getType() != TAction.Type.GENERAL)
			throw new IllegalArgumentException("Action is not a general action.");
		return new TAMECommand(action, null, null, null);
	}
	
	/**
	 * Creates a modal or open action item to execute later.
	 * @param action the action to call.
	 * @param target the open target.
	 * @return a new TAME action item.
	 * @throws IllegalArgumentException if the action provided is not a modal or open one.
	 */
	public static TAMECommand create(TAction action, String target)
	{
		if (action.getType() != TAction.Type.MODAL && action.getType() != TAction.Type.OPEN)
			throw new IllegalArgumentException("Action is not a modal nor open action.");
		return new TAMECommand(action, target, null, null);
	}
	
	/**
	 * Creates a transitive action item to execute later.
	 * @param action the action to call.
	 * @param object the first object.
	 * @return a new TAME action item.
	 * @throws IllegalArgumentException if the action provided is not a transitive or ditransitive one.
	 */
	public static TAMECommand create(TAction action, TObject object)
	{
		if (action.getType() != TAction.Type.TRANSITIVE && action.getType() != TAction.Type.DITRANSITIVE)
			throw new IllegalArgumentException("Action is not a transitive nor ditransitive action.");
		return new TAMECommand(action, null, object, null);
	}
	
	/**
	 * Creates a ditransitive action item to execute later.
	 * @param action the action to call.
	 * @param object1 the first object.
	 * @param object2 the second object.
	 * @return a new TAME action item.
	 * @throws IllegalArgumentException if the action provided is not a ditransitive one.
	 */
	public static TAMECommand create(TAction action, TObject object1, TObject object2)
	{
		if (action.getType() != TAction.Type.DITRANSITIVE)
			throw new IllegalArgumentException("Action is not a ditransitive action.");
		return new TAMECommand(action, null, object1, object2);
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
