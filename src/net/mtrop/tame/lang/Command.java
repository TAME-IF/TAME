/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame.lang;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;

import net.mtrop.tame.TAMECommand;
import net.mtrop.tame.TAMERequest;
import net.mtrop.tame.TAMEResponse;
import net.mtrop.tame.interrupt.TAMEInterrupt;

/**
 * A single low-level machine operation.
 * @author Matthew Tropiano
 */
public class Command implements CallableType, Saveable
{
	/** Command opcode. */
	private TAMECommand operation;
	/** Command operand 0. */
	private Value operand0;
	/** Command operand 1. */
	private Value operand1;
	/** Init block for loops. */
	private Block initBlock;
	/** Conditional block for loops. */
	private Block conditionalBlock;
	/** Step block for loops. */
	private Block stepBlock;
	/** Success block for conditionals. */
	private Block successBlock;
	/** Failure block for conditionals. */
	private Block failureBlock;

	// Private blank constructor for state reader.
	private Command()
	{
		// Nothing.
	}
	
	// Hidden constructor.
	private Command(TAMECommand opcode, Value operand0, Value operand1, Block initBlock, Block conditionalBlock, Block stepBlock, Block successBlock, Block failureBlock) 
	{
		this.operation = opcode;
		this.operand0 = operand0;
		this.operand1 = operand1;
		this.initBlock = initBlock;
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
	 */
	public static Command create(TAMECommand command, Block initializationBlock, Block conditionalBlock, Block stepBlock, Block successBlock)
	{
		return new Command(command, null, null, initializationBlock, conditionalBlock, stepBlock, successBlock, null);
	}

	@Override
	public void call(TAMERequest request, TAMEResponse response) throws TAMEInterrupt
	{
		operation.call(request, response, this);
	}

	public Value getOperand0()
	{
		return operand0;
	}
	
	public Value getOperand1()
	{
		return operand1;
	}
	
	public Block getInitBlock() 
	{
		return initBlock;
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
		if (initBlock != null)
			sb.append(" INIT{").append(initBlock).append('}');
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

	/**
	 * Creates this object from an input stream, expecting its byte representation. 
	 * @param in the input stream to read from.
	 * @return the read object.
	 * @throws IOException if a read error occurs.
	 */
	public static Command create(InputStream in) throws IOException
	{
		Command out = new Command();
		out.readBytes(in);
		return out;
	}

	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		sw.writeByte((byte)operation.ordinal());
		
		sw.writeBit(operand0 != null);
		sw.writeBit(operand1 != null);
		sw.writeBit(initBlock != null);
		sw.writeBit(conditionalBlock != null);
		sw.writeBit(stepBlock != null);
		sw.writeBit(successBlock != null);
		sw.writeBit(failureBlock != null);
		sw.flushBits();
		
		if (operand0 != null)
			operand0.writeBytes(out);
		if (operand1 != null)
			operand1.writeBytes(out);
		if (initBlock != null)
			initBlock.writeBytes(out);
		if (conditionalBlock != null)
			conditionalBlock.writeBytes(out);
		if (stepBlock != null)
			stepBlock.writeBytes(out);
		if (successBlock != null)
			successBlock.writeBytes(out);
		if (failureBlock != null)
			failureBlock.writeBytes(out);
		
	}
	
	@Override
	public void readBytes(InputStream in) throws IOException
	{
		SuperReader sr = new SuperReader(in, SuperReader.LITTLE_ENDIAN);
		this.operation = TAMECommand.values()[(int)sr.readByte()];
		
		byte objectbits = sr.readByte();
		
		if ((objectbits & 0x01) != 0)
			this.operand0 = Value.create(in);
		if ((objectbits & 0x02) != 0)
			this.operand1 = Value.create(in);
	
		if ((objectbits & 0x04) != 0)
			this.initBlock = Block.create(in);
		if ((objectbits & 0x08) != 0)
			this.conditionalBlock = Block.create(in);
		if ((objectbits & 0x10) != 0)
			this.stepBlock = Block.create(in);
		if ((objectbits & 0x20) != 0)
			this.successBlock = Block.create(in);
		if ((objectbits & 0x40) != 0)
			this.failureBlock = Block.create(in);
		
	}

	@Override
	public byte[] toBytes() throws IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		writeBytes(bos);
		return bos.toByteArray();
	}

	@Override
	public void fromBytes(byte[] data) throws IOException 
	{
		ByteArrayInputStream bis = new ByteArrayInputStream(data);
		readBytes(bis);
		bis.close();
	}

}
