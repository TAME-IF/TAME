package net.mtrop.tame.factory;

/**
 * The default set of options for exporting a module to JS. 
 * @author Matthew Tropiano
 */
public class DefaultJSExporterOptions implements TAMEJSExporterOptions
{

	@Override
	public String getWrapperName()
	{
		return null;
	}

}
