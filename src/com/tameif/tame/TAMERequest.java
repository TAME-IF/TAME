/*******************************************************************************
 * Copyright (c) 2015-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame;

import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.commons.linkedlist.Stack;
import com.tameif.tame.element.context.TElementContext;
import com.tameif.tame.exception.ArithmeticStackStateException;
import com.tameif.tame.lang.Value;

/**
 * Request object generated by the engine to be interpreted and then
 * have a response returned back. 
 * @author Matthew Tropiano
 */
public class TAMERequest
{
	/** The input message. */
	private String inputMessage;
	/** Is the trace enabled? */
	private boolean tracing;

	/** Belayed action queue. */
	private Queue<TAMEAction> actionQueue;

	/** Module. */
	private TAMEModuleContext moduleContext;
	/** Arithmetic stack. */
	private Stack<Value> valueStack;
	/** Context stack. */
	private Stack<TElementContext<?>> contextStack;
	
	/**
	 * Creates a new request object.
	 */
	private TAMERequest()
	{
		inputMessage = null;
		tracing = false;
		
		actionQueue = new Queue<TAMEAction>();
		
		moduleContext = null;
		valueStack = new Stack<Value>();
		contextStack = new Stack<TElementContext<?>>();
	}

	/**
	 * Creates the request object.
	 * @param moduleContext the module context.
	 * @param tracing if true, this does tracing.
	 * @return a TAMERequest a new request.
	 */
	static TAMERequest create(TAMEModuleContext moduleContext, boolean tracing)
	{
		TAMERequest out = new TAMERequest();
		out.moduleContext = moduleContext;
		out.inputMessage = null;
		out.tracing = tracing;
		return out;
	}
	
	/**
	 * Creates the request object.
	 * @param moduleContext the module context.
	 * @param input the client input query.
	 * @param tracing if true, this does tracing.
	 * @return a TAMERequest a new request.
	 */
	static TAMERequest create(TAMEModuleContext moduleContext, String input, boolean tracing)
	{
		TAMERequest out = new TAMERequest();
		out.moduleContext = moduleContext;
		out.inputMessage = input;
		out.tracing = tracing;
		return out;
	}
	
	/**
	 * Gets the request's input message.
	 * This gets interpreted by the TAME virtual machine.
	 * @return the message used in the request.
	 */
	public String getInputMessage()
	{
		return inputMessage;
	}

	/**
	 * Is this a tracing request?
	 * @return true if so, false if not.
	 */
	public boolean isTracing()
	{
		return tracing;
	}

	/**
	 * Adds an action item to the queue to be processed later.
	 * @param item the action item to add.
	 */
	public void addActionItem(TAMEAction item)
	{
		actionQueue.enqueue(item);
	}

	/**
	 * Checks if this still has action items to process.
	 * @return true if so, false if not.
	 */
	public boolean hasActionItems()
	{
		return !actionQueue.isEmpty();
	}

	/**
	 * Gets the module context that this affects.
	 * @return the module context. 
	 */
	public TAMEModuleContext getModuleContext()
	{
		return moduleContext;
	}
	
	/**
	 * Dequeues an action item from the queue to be processed later.
	 * @return the next action item to process.
	 */
	TAMEAction nextActionItem()
	{
		return actionQueue.dequeue();
	}

	/**
	 * Pushes an element context value onto the context stack.
	 * @param context the context to push.
	 */
	void pushContext(TElementContext<?> context)
	{
		contextStack.push(context);
	}
	
	/**
	 * Removes an element context value off of the context stack and returns it.
	 * @return the element context on the stack or null if none in the stack.
	 */
	TElementContext<?> popContext()
	{
		return contextStack.pop();
	}

	/**
	 * Looks at the top of the element context stack.
	 * @return the top of the context stack, or null if the stack is empty.
	 */
	TElementContext<?> peekContext()
	{
		return contextStack.peek();
	}
	
	/**
	 * Pushes a value onto the arithmetic stack.
	 * @param value the value to push.
	 */
	void pushValue(Value value)
	{
		valueStack.push(value);
	}
	
	/**
	 * Removes the topmost value off the arithmetic stack.
	 * @return the value popped off the stack or null if the stack is empty.
	 * @throws ArithmeticStackStateException if the stack is empty.
	 */
	Value popValue()
	{
		if (valueStack.isEmpty())
			throw new ArithmeticStackStateException("Attempt to pop an empty arithmetic stack.");
		return valueStack.pop();
	}
	
	/**
	 * Checks if the arithmetic stack is empty.
	 * Should be called after a full request is made.
	 * @throws ArithmeticStackStateException if the stack is NOT empty.
	 */
	void checkStackClear()
	{
		if (!valueStack.isEmpty())
			throw new ArithmeticStackStateException("Arithmetic stack is not empty.");
	}

}