package net.mtrop.tame.exception;

/**
 * Thrown when a module state read/write exception occurs.
 * @author Matthew Tropiano
 */
public class ModuleException extends TAMEFatalException
{
	private static final long serialVersionUID = 7860423798911413272L;

	public ModuleException()
	{
		super();
	}

	public ModuleException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public ModuleException(String message) 
	{
		super(message);
	}

	public ModuleException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public ModuleException(Throwable cause)
	{
		super(cause);
	}

}
