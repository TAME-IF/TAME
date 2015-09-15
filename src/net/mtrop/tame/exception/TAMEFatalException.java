package net.mtrop.tame.exception;

/**
 * Thrown when an unexpected error occurs in TAME.
 * @author Matthew Tropiano
 */
public class TAMEFatalException extends RuntimeException
{
	private static final long serialVersionUID = -2158136537645075334L;

	public TAMEFatalException()
	{
		super();
	}

	public TAMEFatalException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public TAMEFatalException(String message) 
	{
		super(message);
	}

	public TAMEFatalException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public TAMEFatalException(Throwable cause)
	{
		super(cause);
	}

}
