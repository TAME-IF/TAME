package net.mtrop.tame.factory;

/**
 * Thrown when an a TAME script parse goes wrong.
 * @author Matthew Tropiano
 */
public class TAMEScriptParseException extends RuntimeException
{
	private static final long serialVersionUID = -2158136537645075334L;

	public TAMEScriptParseException()
	{
		super();
	}

	public TAMEScriptParseException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public TAMEScriptParseException(String message) 
	{
		super(message);
	}

	public TAMEScriptParseException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public TAMEScriptParseException(Throwable cause)
	{
		super(cause);
	}

}
