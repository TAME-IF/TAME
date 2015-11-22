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
import net.mtrop.tame.interrupt.TAMEInterrupt;

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
	 * @param statement the calling statement (get blocks from this).
	 * @throws TAMEInterrupt if a TAMEInterrupt occurs.
	 */
	public void call(TAMERequest request, TAMEResponse response, Command statement) throws TAMEInterrupt;
	
	/**
	 * Does this represent a linguistic construct?
	 * These are parsed in the language.
	 * @return true if so, false if not.
	 */
	public boolean isLanguage();
	
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
