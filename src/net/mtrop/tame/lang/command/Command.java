package net.mtrop.tame.lang.command;

import net.mtrop.tame.TAMECommand;
import net.mtrop.tame.TAMERequest;
import net.mtrop.tame.TAMEResponse;
import net.mtrop.tame.interrupt.TAMEInterrupt;
import net.mtrop.tame.lang.ExecutableType;
import net.mtrop.tame.struct.Value;

/**
 * A single low-level machine operation.
 * @author Matthew Tropiano
 */
public class Command implements ExecutableType
{
	/** Command opcode. */
	private TAMECommand operation;
	/** Command operand 0. */
	private Value operand0;
	/** Command operand 1. */
	private Value operand1;
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
	private Command(TAMECommand opcode, Value operand0, Value operand1, Block initializationBlock, Block conditionalBlock, Block stepBlock, Block successBlock, Block failureBlock) 
	{
		super();
		this.operation = opcode;
		this.operand0 = operand0;
		this.operand1 = operand1;
		this.initializationBlock = initializationBlock;
		this.conditionalBlock = conditionalBlock;
		this.stepBlock = stepBlock;
		this.successBlock = successBlock;
		this.failureBlock = failureBlock;
	}

	/**
	 * Creates a command.
	 * @param command the command.
	 */
	public static Command create(TAMECommand command)
	{
		return new Command(command, null, null, null, null, null, null, null);
	}

	/**
	 * Creates a command with an operand.
	 * @param command the command.
	 * @param operand0 the first operand.
	 */
	public static Command create(TAMECommand command, Value operand0)
	{
		return new Command(command, operand0, null, null, null, null, null, null);
	}

	/**
	 * Creates a command with operands.
	 * @param command the command.
	 * @param operand0 the first operand.
	 * @param operand1 the second operand.
	 */
	public static Command create(TAMECommand command, Value operand0, Value operand1)
	{
		return new Command(command, operand0, operand1, null, null, null, null, null);
	}

	/**
	 * Creates a command with a conditional and success block.
	 * @param command the command.
	 * @param conditionalBlock the conditional block.
	 * @param successBlock the success block.
	 */
	public static Command create(TAMECommand command, Block conditionalBlock, Block successBlock)
	{
		return new Command(command, null, null, null, conditionalBlock, null, successBlock, null);
	}

	/**
	 * Creates a command with a conditional, success, and failure block.
	 * @param command the command.
	 * @param conditionalBlock the conditional block.
	 * @param successBlock the success block.
	 * @param failureBlock the failure block.
	 */
	public static Command create(TAMECommand command, Block conditionalBlock, Block successBlock, Block failureBlock)
	{
		return new Command(command, null, null, null, conditionalBlock, null, successBlock, failureBlock);
	}

	/**
	 * Creates a command with an initializer, conditional, step, success, and failure block.
	 * @param command the command.
	 * @param initializationBlock the init block.
	 * @param conditionalBlock the conditional block.
	 * @param stepBlock the step block.
	 * @param successBlock the success block.
	 * @param failureBlock the failure block.
	 */
	public static Command create(TAMECommand command, Block initializationBlock, Block conditionalBlock, Block stepBlock, Block successBlock, Block failureBlock)
	{
		return new Command(command, null, null, initializationBlock, conditionalBlock, stepBlock, successBlock, failureBlock);
	}

	@Override
	public void execute(TAMERequest request, TAMEResponse response) throws TAMEInterrupt
	{
		operation.execute(request, response, this);
	}

	public Value getOperand0()
	{
		return operand0;
	}
	
	public Value getOperand1()
	{
		return operand1;
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
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append(operation.name());
		if (operand0 != null)
			sb.append(' ').append(operand0);
		if (operand1 != null)
			sb.append(' ').append(operand1);
		if (initializationBlock != null)
			sb.append(" INIT{").append(initializationBlock).append('}');
		if (conditionalBlock != null)
			sb.append(" CONDITIONAL{").append(conditionalBlock).append('}');
		if (stepBlock != null)
			sb.append(" STEP{").append(stepBlock).append('}');
		if (successBlock != null)
			sb.append(" SUCCESS{").append(successBlock).append('}');
		if (failureBlock != null)
			sb.append(" FAILURE{").append(failureBlock).append('}');
		return sb.toString();
	}
	
}
