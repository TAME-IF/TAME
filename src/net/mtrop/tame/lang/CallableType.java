/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame.lang;

import net.mtrop.tame.TAMERequest;
import net.mtrop.tame.TAMEResponse;
import net.mtrop.tame.interrupt.TAMEInterrupt;

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
	public void call(TAMERequest request, TAMEResponse response, ValueHash blockLocal) throws TAMEInterrupt;
	
}
