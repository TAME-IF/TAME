package net.mtrop.tame.exception;

/**
 * Thrown when the arithmetic stack ends up in a way that should not be.
 * @author Matthew Tropiano
 */
public class ArithmeticStackStateException extends TAMEFatalException
{
	private static final long serialVersionUID = 2838202334130697209L;

	public ArithmeticStackStateException()
	{
		super();
	}

	public ArithmeticStackStateException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public ArithmeticStackStateException(String message) 
	{
		super(message);
	}

	public ArithmeticStackStateException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public ArithmeticStackStateException(Throwable cause)
	{
		super(cause);
	}

}
