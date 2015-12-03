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
package net.mtrop.tame.element.type.context;

import net.mtrop.tame.element.type.TWorld;

/**
 * Holds contextual information for a TElement.
 * @author Matthew Tropiano
 */
public class TWorldContext extends TElementContext<TWorld>
{
	/**
	 * Creates a blank WorldContext.
	 */
	public TWorldContext(TWorld world)
	{
		super(world);
	}
	
}
