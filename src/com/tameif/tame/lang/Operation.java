/*******************************************************************************
 * Copyright (c) 2015-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.lang;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.blackrook.io.SuperReader;
import com.blackrook.io.SuperWriter;
import com.tameif.tame.TAMEOperation;
import com.tameif.tame.TAMEInterrupt;
import com.tameif.tame.TAMERequest;
import com.tameif.tame.TAMEResponse;

/**
 * A single low-level machine operation.
 * @author Matthew Tropiano
 */
public class Operation implements CallableType, Saveable
{
	/** Operation opcode. */
	private TAMEOperation operation;
	/** Operation operand 0. */
	private Value operand0;
	/** Operation operand 1. */
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
	private Operation()
	{
		// Nothing.
	}
	
	// Hidden constructor.
	private Operation(TAMEOperation opcode, Value operand0, Value operand1, Block initBlock, Block conditionalBlock, Block stepBlock, Block successBlock, Block failureBlock) 
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
	 * Creates an operation.
	 * @param operation the operation.
	 * @return the new operation.
	 */
	public static Operation create(TAMEOperation operation)
	{
		return new Operation(operation, null, null, null, null, null, null, null);
	}

	/**
	 * Creates an operation with an operand.
	 * @param operation the operation.
	 * @param operand0 the first operand.
	 * @return the new operation.
	 */
	public static Operation create(TAMEOperation operation, Value operand0)
	{
		return new Operation(operation, operand0, null, null, null, null, null, null);
	}

	/**
	 * Creates an operation with operands.
	 * @param operation the operation.
	 * @param operand0 the first operand.
	 * @param operand1 the second operand.
	 * @return the new operation.
	 */
	public static Operation create(TAMEOperation operation, Value operand0, Value operand1)
	{
		return new Operation(operation, operand0, operand1, null, null, null, null, null);
	}

	/**
	 * Creates an operation with a conditional and success block.
	 * @param operation the operation.
	 * @param conditionalBlock the conditional block.
	 * @param successBlock the success block.
	 * @return the new operation.
	 */
	public static Operation create(TAMEOperation operation, Block conditionalBlock, Block successBlock)
	{
		return new Operation(operation, null, null, null, conditionalBlock, null, successBlock, null);
	}

	/**
	 * Creates an operation with a conditional, success, and failure block.
	 * @param operation the operation.
	 * @param conditionalBlock the conditional block.
	 * @param successBlock the success block.
	 * @param failureBlock the failure block.
	 * @return the new operation.
	 */
	public static Operation create(TAMEOperation operation, Block conditionalBlock, Block successBlock, Block failureBlock)
	{
		return new Operation(operation, null, null, null, conditionalBlock, null, successBlock, failureBlock);
	}

	/**
	 * Creates a operation with an initializer, conditional, step, success, and failure block.
	 * @param operation the operation.
	 * @param initializationBlock the init block.
	 * @param conditionalBlock the conditional block.
	 * @param stepBlock the step block.
	 * @param successBlock the success block.
	 * @return the new operation.
	 */
	public static Operation create(TAMEOperation operation, Block initializationBlock, Block conditionalBlock, Block stepBlock, Block successBlock)
	{
		return new Operation(operation, null, null, initializationBlock, conditionalBlock, stepBlock, successBlock, null);
	}

	@Override
	public void execute(TAMERequest request, TAMEResponse response, ValueHash blockLocal) throws TAMEInterrupt
	{
		operation.execute(request, response, blockLocal, this);
	}

	/**
	 * Gets the operation on this operation.
	 * @return the operation's encapsulated operation.
	 */
	public TAMEOperation getOperation()
	{
		return operation;
	}
	
	/**
	 * Gets the first operand, if any.
	 * @return the operand, or null if no operand.
	 */
	public Value getOperand0()
	{
		return operand0;
	}
	
	/**
	 * Gets the second operand, if any.
	 * @return the operand, or null if no operand.
	 */
	public Value getOperand1()
	{
		return operand1;
	}
	
	/**
	 * Gets the initializer block, if any.
	 * @return the block, or null if no block.
	 */
	public Block getInitBlock() 
	{
		return initBlock;
	}
	
	/**
	 * Gets the conditional assessment block, if any.
	 * @return the block, or null if no block.
	 */
	public Block getConditionalBlock()
	{
		return conditionalBlock;
	}

	/**
	 * Gets the stepping control block, if any.
	 * @return the block, or null if no block.
	 */
	public Block getStepBlock() 
	{
		return stepBlock;
	}
	
	/**
	 * Gets the success control block, if any.
	 * @return the block, or null if no block.
	 */
	public Block getSuccessBlock()
	{
		return successBlock;
	}

	/**
	 * Gets the failure control block, if any.
	 * @return the block, or null if no block.
	 */
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
			sb.append(' ').append(operand0.toString());
		if (operand1 != null)
			sb.append(' ').append(operand1.toString());
		return sb.toString();
	}

	/**
	 * Creates this object from an input stream, expecting its byte representation. 
	 * @param in the input stream to read from.
	 * @return the read object.
	 * @throws IOException if a read error occurs.
	 */
	public static Operation create(InputStream in) throws IOException
	{
		Operation out = new Operation();
		out.readBytes(in);
		return out;
	}

	@Override
	public void writeBytes(OutputStream out) throws IOException
	{
		SuperWriter sw = new SuperWriter(out, SuperWriter.LITTLE_ENDIAN);
		sw.writeVariableLengthInt(operation.ordinal());
		
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
		this.operation = TAMEOperation.VALUES[sr.readVariableLengthInt()];
		
		byte objectbits = sr.readByte();
		
		if ((objectbits & 0x01) != 0)
			this.operand0 = Value.read(in);
		if ((objectbits & 0x02) != 0)
			this.operand1 = Value.read(in);
	
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
