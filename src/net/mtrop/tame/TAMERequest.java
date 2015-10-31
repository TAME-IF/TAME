/*******************************************************************************
 * Copyright (c) 2009-2013 Black Rook Software
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *  
 * Contributors:
 *     Matt Tropiano - initial API and implementation
 ******************************************************************************/
package net.mtrop.tame;

import net.mtrop.tame.element.context.TElementContext;
import net.mtrop.tame.exception.ArithmeticStackStateException;
import net.mtrop.tame.lang.Value;

import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.commons.linkedlist.Stack;

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
	TAMERequest()
	{
		inputMessage = null;
		tracing = false;
		
		actionQueue = new Queue<TAMEAction>();
		
		moduleContext = null;
		valueStack = new Stack<Value>();
		contextStack = new Stack<TElementContext<?>>();
	}

	/**
	 * Sets the request's input message.
	 * This gets interpreted by the TAME virtual machine.
	 */
	public void setInputMessage(String inputMessage)
	{
		this.inputMessage = inputMessage;
	}

	/**
	 * Gets the request's input message.
	 * This gets interpreted by the TAME virtual machine.
	 */
	public String getInputMessage()
	{
		return inputMessage;
	}

	/**
	 * Is this a tracing request?
	 */
	public boolean isTracing()
	{
		return tracing;
	}

	/**
	 * Sets if trace is enabled.
	 */
	public void setTracing(boolean trace)
	{
		this.tracing = trace;
	}
	
	/**
	 * Adds an action item to the queue to be processed later.
	 */
	public void addActionItem(TAMEAction item)
	{
		actionQueue.enqueue(item);
	}

	/**
	 * Dequeues an action item from the queue to be processed later.
	 */
	public TAMEAction getActionItem()
	{
		return actionQueue.dequeue();
	}

	/**
	 * Checks if this still has action items to process.
	 */
	public boolean hasActionItems()
	{
		return !actionQueue.isEmpty();
	}

	/**
	 * Gets the module context that this affects. 
	 */
	public TAMEModuleContext getModuleContext()
	{
		return moduleContext;
	}
	
	/**
	 * Sets the module to be affected. 
	 */
	public void setModuleContext(TAMEModuleContext module)
	{
		this.moduleContext = module;
	}
	
	/**
	 * Pushes an element context value onto the context stack.
	 * @param value the value to push.
	 */
	public void pushContext(TElementContext<?> context)
	{
		contextStack.push(context);
	}
	
	/**
	 * Pops an element context value off of the context stack.
	 */
	public TElementContext<?> popContext()
	{
		return contextStack.pop();
	}

	/**
	 * Returns the top of the context stack.
	 */
	public TElementContext<?> peekContext()
	{
		return contextStack.peek();
	}
	
	/**
	 * Pushes a value onto the arithmetic stack.
	 * @param value the value to push.
	 */
	public void pushValue(Value value)
	{
		valueStack.push(value);
	}
	
	/**
	 * Pops a value off the arithmetic stack.
	 * @throws ArithmeticStackStateException if the stack is empty.
	 */
	public Value popValue()
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
	public void checkStackClear()
	{
		if (!valueStack.isEmpty())
			throw new ArithmeticStackStateException("Arithmetic stack is not empty.");
	}

	/**
	 * Resolves a variable from the topmost element context.
	 * @param variableName the variable name.
	 * @return the value resolved.
	 */
	public Value resolveVariableValue(String variableName)
	{
		return peekContext().getValue(variableName);
	}

}
