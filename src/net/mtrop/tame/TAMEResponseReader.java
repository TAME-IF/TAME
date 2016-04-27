/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/
package net.mtrop.tame;

import java.util.Iterator;

import net.mtrop.tame.lang.Cue;

/**
 * A response reader class that iterates through 
 * the cues in the response and does stuff to specific responses.
 * This is meant to be a helper class for quick debugging or standard handling. 
 * @author Matthew Tropiano
 */
public abstract class TAMEResponseReader 
{
	/** The response to read from. */
	private TAMEResponse response;
	/** Cue iterator. */
	private Iterator<Cue> iterator;
	
	/**
	 * TAME response reader.
	 * @param response the response that will be read by this.
	 */
	public TAMEResponseReader(TAMEResponse response)
	{
		this.response = response;
		this.iterator = null;
	}
	
	/**
	 * Handles a cue.
	 * @param cue the cue to handle.
	 * @return true if reading should continue, false to halt it.
	 */
	public abstract boolean handleCue(Cue cue);
	
	/**
	 * Called when the reader stops reading cues.
	 */
	protected void handleStop()
	{
		// Do nothing.
	}
	
	/**
	 * Gets if this has more cues to read from the response.
	 * @return true if so, false if not.
	 */
	public final boolean hasMoreCues()
	{
		return iterator != null && iterator.hasNext();
	}
	
	/**
	 * Reads the response cues, caliing the {@link #handleCue(Cue)} method for
	 * each cue in the response. This stops when {@link #handleCue(Cue)} returns false,
	 * or when there are no more cues to read.
	 * After this stops reading for whatever reason, this calls {@link #handleStop()}.
	 * @return true if there's more to read and this should be called again, false if not.
	 */
	public boolean read()
	{
		if (iterator == null)
			iterator = response.getCues().iterator();
		
		while (iterator.hasNext() && handleCue(iterator.next())) ;
		
		handleStop();
		
		return iterator.hasNext();
	}
	
}
