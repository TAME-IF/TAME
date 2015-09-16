package net.mtrop.tame.exception;

/**
 * Thrown when a command reads a value that has an unexpected type.
 * @author Matthew Tropiano
 */
public class UnexpectedValueException extends TAMEFatalException
{
	private static final long serialVersionUID = 9084390457262102600L;

	public UnexpectedValueException()
	{
		super();
	}

	public UnexpectedValueException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public UnexpectedValueException(String message) 
	{
		super(message);
	}

	public UnexpectedValueException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public UnexpectedValueException(Throwable cause)
	{
		super(cause);
	}

}
