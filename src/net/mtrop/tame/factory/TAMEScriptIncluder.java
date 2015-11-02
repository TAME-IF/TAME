package net.mtrop.tame.factory;

import java.io.IOException;
import java.io.InputStream;

/**
 * An interface that allows the user to resolve a resource by path when the
 * {@link TAMEScriptReader} parses it.
 * @author Matthew Tropiano
 */
public interface TAMEScriptIncluder 
{
	/**
	 * Returns an open {@link InputStream} for a path when the parser needs a resource.
	 * By default, this attempts to open a file at the provided path.
	 * @param streamName the current name of the stream. This includer may use this to procure a relative path.
	 * @param path the stream path.
	 * @return an open {@link InputStream} for the requested resource, or null if not found.
	 */
	public InputStream getIncludeResource(String streamName, String path) throws IOException;
		
}
