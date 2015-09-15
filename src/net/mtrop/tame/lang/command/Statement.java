package net.mtrop.tame.lang.command;

import net.mtrop.tame.TAMECommand;
import net.mtrop.tame.TAMERequest;
import net.mtrop.tame.TAMEResponse;
import net.mtrop.tame.interrupt.TAMEInterrupt;
import net.mtrop.tame.lang.ExecutableType;

/**
 * A command or block that gets called in a request pass.
 * @author Matthew Tropiano
 */
public class Statement implements ExecutableType
{
	/** TAME command opcode. */
	private int opcode;
	/** Init block for loops. */
	private Block initializationBlock;
	/** Conditional block for loops. */
	private Block conditionalBlock;
	/** Step block for loops. */
	private Block stepBlock;
	/** Success block for conditionals. */
	private Block successBlock;
	/** Failure block for conditionals. */
	private Block failureBlock;

	// Hidden constructor.
	private Statement(int opcode, Block initializationBlock, Block conditionalBlock, Block stepBlock, Block successBlock, Block failureBlock) 
	{
		super();
		this.opcode = opcode;
		this.initializationBlock = initializationBlock;
		this.conditionalBlock = conditionalBlock;
		this.stepBlock = stepBlock;
		this.successBlock = successBlock;
		this.failureBlock = failureBlock;
	}

	/**
	 * Creates a statement with a conditional and success block.
	 * @param command the command.
	 * @param conditionalBlock the conditional block.
	 * @param successBlock the success block.
	 */
	public static Statement create(TAMECommand command, Block conditionalBlock, Block successBlock)
	{
		return new Statement(command.ordinal(), null, conditionalBlock, null, successBlock, null);
	}

	/**
	 * Creates a statement with a conditional, success, and failure block.
	 * @param command the command.
	 * @param conditionalBlock the conditional block.
	 * @param successBlock the success block.
	 * @param failureBlock the failure block.
	 */
	public static Statement create(TAMECommand command, Block conditionalBlock, Block successBlock, Block failureBlock)
	{
		return new Statement(command.ordinal(), null, conditionalBlock, null, successBlock, failureBlock);
	}

	/**
	 * Creates a statement with an initializer, conditional, step, success, and failure block.
	 * @param command the command.
	 * @param initializationBlock the init block.
	 * @param conditionalBlock the conditional block.
	 * @param stepBlock the step block.
	 * @param successBlock the success block.
	 * @param failureBlock the failure block.
	 */
	public static Statement create(TAMECommand command, Block initializationBlock, Block conditionalBlock, Block stepBlock, Block successBlock, Block failureBlock)
	{
		return new Statement(command.ordinal(), initializationBlock, conditionalBlock, stepBlock, successBlock, failureBlock);
	}

	@Override
	public void execute(TAMERequest request, TAMEResponse response) throws TAMEInterrupt
	{
		if (opcode < 0 || opcode >= TAMECommand.values().length)
			throw new RuntimeException("Bad opcode for command.");
		TAMECommand.values()[opcode].execute(request, response, this);
	}

	public Block getInitializationBlock()
	{
		return initializationBlock;
	}

	public Block getConditionalBlock()
	{
		return conditionalBlock;
	}

	public Block getStepBlock()
	{
		return stepBlock;
	}

	public Block getSuccessBlock()
	{
		return successBlock;
	}

	public Block getFailureBlock()
	{
		return failureBlock;
	}
	
}
