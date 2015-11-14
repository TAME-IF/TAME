package net.mtrop.tame.factory;

/**
 * An interface for reader options. 
 * These influence reader/compiler behavior.
 * @author Matthew Tropiano
 */
public interface TAMEScriptReaderOptions 
{
	/**
	 * Gets what to predefine in the preprocessor.
	 * This can affect what gets compiled and what doesn't.
	 * Must not return null.
	 * @return a list of defined tokens.
	 */
	public String[] getDefines();
	
	/**
	 * Gets if this reader optimizes finished blocks.
	 * @return true if so, false if not.
	 */
	public boolean isOptimizing();
	
	/**
	 * Gets if this prints what it is emitting or constructing.
	 * Only good for debugging.
	 * @return true if so, false if not.
	 */
	public boolean isVerbose();
	
}
