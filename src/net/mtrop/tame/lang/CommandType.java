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
	 * Executes the command.
	 * @param request the TAME request.
	 * @param response the TAME response.
	 * @param statement the calling statement (get blocks from this).
	 * @throws TAMEInterrupt if a TAMEInterrupt occurs.
	 */
	public void execute(TAMERequest request, TAMEResponse response, Command statement) throws TAMEInterrupt;
	
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
	
	/**
	 * Returns if this command has an evaluation initialization block.
	 * @return true if so, false if not.
	 */
	public boolean isInitializationBlockRequired();

	/**
	 * Returns if this command has an evaluation conditional block.
	 * @return true if so, false if not.
	 */
	public boolean isConditionalBlockRequired();

	/**
	 * Returns if this command has an evaluation step block, called after the body.
	 * @return true if so, false if not.
	 */
	public boolean isStepBlockRequired();

	/**
	 * Returns if this command has a body block to call if the evaluation conditional succeeds.
	 * @return true if so, false if not.
	 */
	public boolean isSuccessBlockRequired();

	/**
	 * Returns if this command has a branch block to call if the evaluation conditional fails.
	 * @return true if so, false if not.
	 */
	public boolean isFailureBlockRequired();
	
}
