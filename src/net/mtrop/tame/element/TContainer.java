package net.mtrop.tame.element;

import java.io.IOException;
import java.io.InputStream;

/**
 * Container that just holds objects. It cannot be actioned on.
 * @author Matthew Tropiano
 */
public class TContainer extends TElement 
{
	
	private TContainer()
	{
		super();
	}
	
	/**
	 * Creates an empty container.
	 * @param identity its main identity.
	 */
	public TContainer(String identity) 
	{
		this();
		setIdentity(identity);
	}

	/**
	 * Creates this container from an input stream, expecting its byte representation. 
	 * @param in the input stream to read from.
	 * @return the read object.
	 * @throws IOException if a read error occurs.
	 */
	public static TContainer create(InputStream in) throws IOException
	{
		TContainer out = new TContainer();
		out.readBytes(in);
		return out;
	}

	
}
