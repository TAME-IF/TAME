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

import net.mtrop.tame.TAMEModuleCommand;

/**
 * An executable command run by the TAME virtual machine.
 * This is the abstract for all commands executed by the context. 
 * @author Matthew Tropiano
 */
public interface CommandType
{
	/**
	 * Returns the command to call for evaluating.
	 * CANNOT RETURN NULL. 
	 */
	public TAMEModuleCommand getCommand();
	
	/**
	 * Is this an internal command type?
	 * Internal commands are not exposed to the parser.
	 * @return true if so, false if not.
	 */
	public boolean isInternal();
	
	/**
	 * Is this command a single-keyword command (no parenthesis or arguments).
	 * @return true if so, false if not.
	 */
	public boolean isKeyword();
	
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
	 * Returns if this command has an evaluation block set.
	 * @return true if so, false if not.
	 */
	public boolean isEvaluating();

	/**
	 * Returns if this command has an evaluation initialization block.
	 * @return true if so, false if not.
	 */
	public boolean hasEvaluatingInitializer();

	/**
	 * Returns if this command has an evaluation conditional block.
	 * @return true if so, false if not.
	 */
	public boolean hasEvaluatingConditional();

	/**
	 * Returns if this command has an evaluation step block, called after the body.
	 * @return true if so, false if not.
	 */
	public boolean hasEvaluatingStep();

	/**
	 * Returns if this command has a body block to call if the evaluation conditional succeeds.
	 * @return true if so, false if not.
	 */
	public boolean hasSuccessBody();

	/**
	 * Returns if this command has a branch block to call if the evaluation conditional fails.
	 * @return true if so, false if not.
	 */
	public boolean hasFailingBody();
	
}
