package net.mtrop.tame.factory;

/**
 * An interface for reader options. 
 * These influence reader/compiler behavior.
 * @author Matthew Tropiano
 */
public interface TAMEScriptReaderOptions 
{
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
