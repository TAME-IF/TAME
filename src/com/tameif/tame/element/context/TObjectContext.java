/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.element.context;

import com.tameif.tame.element.TObject;

/**
 * Object context.
 * @author Matthew Tropiano
 */
public class TObjectContext extends TElementContext<TObject>
{	
	/**
	 * Creates an object context. 
	 * @param object the object reference.
	 */
	public TObjectContext(TObject object)
	{
		super(object);
	}

}
