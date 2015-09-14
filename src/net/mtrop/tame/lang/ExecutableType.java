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
package net.mtrop.tame.lang;

import net.mtrop.tame.TAMERequest;
import net.mtrop.tame.TAMEResponse;

/**
 * An executable object called by the TAME virtual machine.
 * @author Matthew Tropiano
 */
public interface ExecutableType
{
	/**
	 * Executes something that can change the request and response.
	 * @param request the request object.
	 * @param response the response object.
	 */
	public void execute(TAMERequest request, TAMEResponse response);
	
}
