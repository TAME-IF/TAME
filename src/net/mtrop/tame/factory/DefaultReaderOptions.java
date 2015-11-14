package net.mtrop.tame.factory;

/**
 * Default script reader options implementation.
 * @author Matthew Tropiano
 */
public class DefaultReaderOptions implements TAMEScriptReaderOptions
{
	private String[] defines;
	private boolean optimizing;
	private boolean verbose;

	public DefaultReaderOptions()
	{
		this.optimizing = true;
		this.verbose = false;
	}
	
	/**
	 * Sets the defines used for compiling. 
	 * @param defines the list of defined tokens.
	 */
	public void setDefines(String ... defines)
	{
		this.defines = defines;
	}
	
	@Override
	public String[] getDefines() 
	{
		return defines;
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
