package net.mtrop.tame.exception;

/**
 * Thrown when a module has unexpected structures.
 * @author Matthew Tropiano
 */
public class ModuleExecutionException extends TAMEFatalException
{
	private static final long serialVersionUID = 2838202334130697209L;

	public ModuleExecutionException()
	{
		super();
	}

	public ModuleExecutionException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public ModuleExecutionException(String message) 
	{
		super(message);
	}

	public ModuleExecutionException(String message, Object ... args) 
	{
		super(String.format(message, args));
	}

	public ModuleExecutionException(Throwable cause)
	{
		super(cause);
	}

}
