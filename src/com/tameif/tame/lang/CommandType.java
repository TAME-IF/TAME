/*******************************************************************************
 * Copyright (c) 2015-2018 Matt Tropiano
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
 * An executable command run by the TAME virtual machine.
 * This is the abstract for all commands executed by the context. 
 * @author Matthew Tropiano
 */
public interface CommandType
{
	/**
	 * Calls the command and increments the command count.
	 * Also performs a runaway check.
	 * @param request the TAME request.
	 * @param response the TAME response.
	 * @param blockLocal the block local variable bank.
	 * @param statement the calling statement (get blocks from this).
	 * @throws TAMEInterrupt if a TAMEInterrupt occurs.
	 */
	public void execute(TAMERequest request, TAMEResponse response, ValueHash blockLocal, Command statement) throws TAMEInterrupt;
	
	/**
	 * Is this an internal command type?
	 * Internal commands are not exposed to the parser.
	 * @return true if so, false if not.
	 */
	public boolean isInternal();
	
	/**
	 * Returns the argument types that this command expects.
	 * @return the argument types.
	 */
	public ArgumentType[] getArgumentTypes();
	
	/**
	 * Returns the return types that this command return.
	 * @return the return type, or null for no return.
	 */
	public ArgumentType getReturnType();
	
}
