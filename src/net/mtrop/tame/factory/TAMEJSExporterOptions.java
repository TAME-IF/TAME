package net.mtrop.tame.factory;

/**
 * The set of options for exporting a module to JS. 
 * @author Matthew Tropiano
 */
public interface TAMEJSExporterOptions 
{
	/**
	 * Gets the JS Wrapper name to use.
	 * @return the wrapper name, or null for no wrapper.
	 */
	public String getWrapperName();
}
