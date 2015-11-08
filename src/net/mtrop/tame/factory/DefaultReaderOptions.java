package net.mtrop.tame.factory;

/**
 * Default script reader options implementation.
 * @author Matthew Tropiano
 */
public class DefaultReaderOptions implements TAMEScriptReaderOptions
{
	private boolean optimizing;
	private boolean verbose;

	public DefaultReaderOptions()
	{
		this.optimizing = true;
		this.verbose = false;
	}
	
	/**
	 * Gets if this reader optimizes finished blocks.
	 * Default is true.
	 */
	public void setOptimizing(boolean optimizing)
	{
		this.optimizing = optimizing;
	}
	
	@Override
	public boolean isOptimizing()
	{
		return optimizing;
	}

	/**
	 * Gets if this prints what it is emitting or constructing.
	 * Only good for debugging.
	 * Default is false.
	 */
	public void setVerbose(boolean verbose)
	{
		this.verbose = verbose;
	}
	
	@Override
	public boolean isVerbose()
	{
		return verbose;
	}

}
