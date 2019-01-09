/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.lang;

import com.tameif.tame.TAMEInterrupt;
import com.tameif.tame.TAMERequest;
import com.tameif.tame.TAMEResponse;

/**
 * An executable object called by the TAME virtual machine.
 * @author Matthew Tropiano
 */
public interface CallableType
{
	/**
	 * Executes something that can change the request and response.
	 * @param request the request object.
	 * @param response the response object.
	 * @param blockLocal the block local variable bank.
	 * @throws TAMEInterrupt if an interrupt occurs.
	 */
	public void execute(TAMERequest request, TAMEResponse response, ValueHash blockLocal) throws TAMEInterrupt;
	
}
