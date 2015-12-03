/*******************************************************************************
 * Copyright (c) 2015 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *
 * Contributors:
 *     Matt Tropiano - initial API and implementation
 *******************************************************************************/
package net.mtrop.tame;

import com.blackrook.commons.CommonTokenizer;
import com.blackrook.commons.linkedlist.Queue;

import net.mtrop.tame.element.type.TAction;
import net.mtrop.tame.element.type.TObject;

/**
 * Context used when some input is getting parsed/interpreted.
 * @author Matthew Tropiano
 */
public class TAMEInterpreterContext 
{
	private String[] tokens;
	private int tokenOffset;
	private TObject[] objects;
	private TAction action;
	private boolean modeLookedUp;
	private String mode;
	private boolean targetLookedUp;
	private String target;
	private boolean conjugateLookedUp;
	private boolean conjugateFound;
	private boolean object1LookedUp;
	private TObject object1;
	private boolean object2LookedUp;
	private TObject object2;
	private boolean objectAmbiguous;
	
	TAMEInterpreterContext(String input)
	{
		input = input.replaceAll("\\s+", " ").trim();
		
		Queue<String> tokenQueue = new Queue<>();
		CommonTokenizer ct = new CommonTokenizer(input);
		while (ct.hasMoreTokens())
			tokenQueue.add(ct.nextToken());
		this.tokens = new String[tokenQueue.size()];
		tokenQueue.toArray(tokens);
		
		this.tokenOffset = 0;
		this.objects = new TObject[2];
		this.action = null;
		this.modeLookedUp = false;
		this.mode = null;
		this.targetLookedUp = false;
		this.target = null;
		this.object1LookedUp = false;
		this.object1 = null;
		this.object2LookedUp = false;
		this.object2 = null;
		this.objectAmbiguous = false;
	}

	void setTokens(String[] tokens) 
	{
		this.tokens = tokens;
	}

	void setTokenOffset(int tokenOffset) 
	{
		this.tokenOffset = tokenOffset;
	}

	void setObjects(TObject[] objects) 
	{
		this.objects = objects;
	}

	void setAction(TAction action) 
	{
		this.action = action;
	}

	void setMode(String mode) 
	{
		this.mode = mode;
	}

	void setConjugateFound(boolean conjugateFound) 
	{
		this.conjugateFound = conjugateFound;
	}
	
	void setTarget(String target) 
	{
		this.target = target;
	}

	void setObject1(TObject object1) 
	{
		this.object1 = object1;
	}

	void setObject2(TObject object2) 
	{
		this.object2 = object2;
	}

	void setModeLookedUp(boolean modeLookedUp) 
	{
		this.modeLookedUp = modeLookedUp;
	}

	void setConjugateLookedUp(boolean conjugateLookedUp) 
	{
		this.conjugateLookedUp = conjugateLookedUp;
	}
	
	void setTargetLookedUp(boolean targetLookedUp) 
	{
		this.targetLookedUp = targetLookedUp;
	}

	void setObject1LookedUp(boolean object1LookedUp) 
	{
		this.object1LookedUp = object1LookedUp;
	}

	void setObject2LookedUp(boolean object2LookedUp) 
	{
		this.object2LookedUp = object2LookedUp;
	}

	void setObjectAmbiguous(boolean objectAmbiguous) 
	{
		this.objectAmbiguous = objectAmbiguous;
	}

	public String[] getTokens() 
	{
		return tokens;
	}

	public int getTokenOffset() 
	{
		return tokenOffset;
	}

	public TObject[] getObjects() 
	{
		return objects;
	}

	public TAction getAction() 
	{
		return action;
	}

	public String getMode() 
	{
		return mode;
	}

	public boolean isConjugateFound() 
	{
		return conjugateFound;
	}
	
	public String getTarget() 
	{
		return target;
	}

	public TObject getObject1() 
	{
		return object1;
	}

	public TObject getObject2() 
	{
		return object2;
	}

	public boolean isModeLookedUp() 
	{
		return modeLookedUp;
	}

	public boolean isConjugateLookedUp() 
	{
		return conjugateLookedUp;
	}
	
	public boolean isTargetLookedUp() 
	{
		return targetLookedUp;
	}

	public boolean isObject1LookedUp() 
	{
		return object1LookedUp;
	}

	public boolean isObject2LookedUp() 
	{
		return object2LookedUp;
	}

	public boolean isObjectAmbiguous() 
	{
		return objectAmbiguous;
	}
	
}
