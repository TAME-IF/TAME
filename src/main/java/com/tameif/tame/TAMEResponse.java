/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame;

import java.util.Formatter;
import java.util.LinkedList;
import java.util.Queue;

import com.tameif.tame.exception.RunawayRequestException;
import com.tameif.tame.lang.Cue;
import com.tameif.tame.lang.TraceType;

/**
 * Response object generated by the engine in which error
 * or output messages get sent back (and perhaps re-interpreted by the client
 * or other objects to facilitate user feedback).
 * @author Matthew Tropiano
 */
public class TAMEResponse implements TAMEConstants
{
	/** The output message. */
	private Queue<Cue> responseCues;
	/** Operation counter. */
	private long operationsExecuted;
	/** Function depth. */
	private long functionDepth;
	/** Time in nanos to process a full request. */
	private long requestNanos;
	/** Time in nanos to interpret input. */
	private long interpretNanos;
	
	/**
	 * Creates a new request object.
	 */
	TAMEResponse()
	{
		this.responseCues = new LinkedList<>();
		this.operationsExecuted = 0;
		this.functionDepth = 0;
		this.requestNanos = 0L;
		this.interpretNanos = 0L;
	}

	/**
	 * Returns the cues on this response. 
	 * @return the queue of cues on the response.
	 */
	public Queue<Cue> getCues()
	{
		return responseCues;
	}

	/**
	 * Adds a response cue to this response object. It can range from
	 * sound cues to changes in scenery, or additional instructions from
	 * the virtual machine to a client. The client is required to obey certain
	 * cues, and can safely ignore the rest.
	 * @param type the cue type name.
	 * @param content the cue content.
	 */
	public void addCue(String type, long content)
	{
		responseCues.add(Cue.create(type, content));
	}

	/**
	 * Adds a response cue to this response object. It can range from
	 * sound cues to changes in scenery, or additional instructions from
	 * the virtual machine to a client. The client is required to obey certain
	 * cues, and can safely ignore the rest.
	 * @param type the cue type name.
	 * @param content the cue content.
	 */
	public void addCue(String type, double content)
	{
		responseCues.add(Cue.create(type, content));
	}

	/**
	 * Adds a response cue to this response object. It can range from
	 * sound cues to changes in scenery, or additional instructions from
	 * the virtual machine to a client. The client is required to obey certain
	 * cues, and can safely ignore the rest.
	 * @param type the cue type name.
	 * @param content the cue content.
	 */
	public void addCue(String type, boolean content)
	{
		responseCues.add(Cue.create(type, content));
	}

	/**
	 * Adds a response cue to this response object. It can range from
	 * sound cues to changes in scenery, or additional instructions from
	 * the virtual machine to a client. The client is required to obey certain
	 * cues, and can safely ignore the rest.
	 * @param type the cue type name.
	 * @param content the cue content.
	 */
	public void addCue(String type, String content)
	{
		responseCues.add(Cue.create(type, content));
	}

	/**
	 * Adds a response cue to this response object with no content. It can range from
	 * sound cues to changes in scenery, or additional instructions from
	 * the virtual machine to a client. The client is required to obey certain
	 * cues, and can safely ignore the rest.
	 * @param type the cue type name.
	 */
	public void addCue(String type)
	{
		responseCues.add(Cue.create(type));
	}

	/**
	 * Increments the function depth by 1.
	 * Also checks against the depth threshold.
	 * After this object is received by the client, this means nothing.
	 * @param maxDepth the function depth maximum.
	 */
	void incrementAndCheckFunctionDepth(long maxDepth)
	{
		functionDepth++;
		if (functionDepth >= maxDepth)
			throw new RunawayRequestException("Runaway request detected! Breached threshold of "+maxDepth+" function calls deep.");
	}
	
	/**
	 * Decrements the function depth by 1.
	 */
	void decrementFunctionDepth()
	{
		functionDepth--;
	}
	
	/**
	 * Increments the operations executed counter by 1.
	 * Also checks against the runaway threshold.
	 * After this object is received by the client, this means nothing.
	 * @param maxOperations the operation maximum.
	 */
	void incrementAndCheckOperationsExecuted(long maxOperations) throws RunawayRequestException
	{
		operationsExecuted++;
		if (operationsExecuted >= maxOperations)
			throw new RunawayRequestException("Runaway request detected! Breached threshold of "+maxOperations+" operations.");
	}
	
	/**
	 * @return the amount of operations executed because of the input.
	 */
	public long getOperationsExecuted()
	{
		return operationsExecuted;
	}
	
	/**
	 * Adds a trace cue, but only if a specific trace type is set on the request.
	 * @param request the request to examine for the trace flag.
	 * @param type the trace type for this trace output.
	 * @param format the formatter string.
	 * @param args the formatter arguments.
	 * @see Formatter
	 */
	public void trace(TAMERequest request, TraceType type, String format, Object ... args)
	{
		if (request.traces(type)) 
			addCue(CUE_TRACE+"-"+type.name().toLowerCase(), String.format(format, args));
	}

	/**
	 * Sets the request nanos for the full processing.
	 * @param requestNanos the time in nanoseconds for processing the request.
	 */
	void setRequestNanos(long requestNanos) 
	{
		this.requestNanos = requestNanos;
	}
	
	/**
	 * Sets the amount of nanos it took to process the input itself.
	 * @param interpretNanos the time in nanoseconds to interpret client input.
	 */
	void setInterpretNanos(long interpretNanos) 
	{
		this.interpretNanos = interpretNanos;
	}
	
	/**
	 * Gets the request nanos for the full processing.
	 * @return the time in nanoseconds for processing the request.
	 */
	public long getRequestNanos() 
	{
		return requestNanos;
	}
	
	/**
	 * Gets the amount of nanos it took to process the input itself.
	 * @return the time in nanoseconds to interpret client input.
	 */
	public long getInterpretNanos() 
	{
		return interpretNanos;
	}
	
}
