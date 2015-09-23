package net.mtrop.tame.exception;

/**
 * Thrown when a module state read/write exception occurs.
 * @author Matthew Tropiano
 */
public class ModuleStateException extends ModuleException
{
	private static final long serialVersionUID = 2838202334130697209L;

	public ModuleStateException()
	{
		super();
	}

	public ModuleStateException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public ModuleStateException(String message) 
	{
		super(message);
	}

	public ModuleStateException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public ModuleStateException(Throwable cause)
	{
		super(cause);
	}

}
