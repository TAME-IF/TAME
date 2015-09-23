package net.mtrop.tame.exception;

/**
 * Thrown when a command reads a value that has an unexpected type.
 * @author Matthew Tropiano
 */
public class UnexpectedValueTypeException extends TAMEFatalException
{
	private static final long serialVersionUID = 3977795940884710775L;

	public UnexpectedValueTypeException()
	{
		super();
	}

	public UnexpectedValueTypeException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public UnexpectedValueTypeException(String message) 
	{
		super(message);
	}

	public UnexpectedValueTypeException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public UnexpectedValueTypeException(Throwable cause)
	{
		super(cause);
	}

}
