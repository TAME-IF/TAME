/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package net.mtrop.tame;

/**
 * Contains constants.
 * @author Matthew Tropiano
 */
public interface TAMEConstants
{
	/** Quit cue. */
	public static final String CUE_QUIT = "QUIT";
	/** Text cue. */
	public static final String CUE_TEXT = "TEXT";
	/** Formatted Text cue. */
	public static final String CUE_TEXTF = "TEXTF";
	/** Wait cue. */
	public static final String CUE_WAIT = "WAIT";
	/** Pause cue. */
	public static final String CUE_PAUSE = "PAUSE";
	/** Trace cue. */
	public static final String CUE_TRACE = "TRACE";
	/** Tip cue. */
	public static final String CUE_TIP = "TIP";
	/** Info cue. */
	public static final String CUE_INFO = "INFO";
	/** Error cue. */
	public static final String CUE_ERROR = "ERROR";
	/** Fatal cue. */
	public static final String CUE_FATAL = "FATAL";

	/** Identity of current room. */
	public static final String IDENTITY_CURRENT_ROOM = "room";
	/** Identity of current player. */
	public static final String IDENTITY_CURRENT_PLAYER = "player";
	/** Identity of current world. */
	public static final String IDENTITY_CURRENT_WORLD = "world";
	
	/** Return variable. */
	public static final String RETURN_VARIABLE = "-. 0Return0 .-";

	/** Default runaway max. */
	public static final int RUNAWAY_THRESHOLD = 100000;

	/* ========= Known or suggested headers. ========= */
	
	/** Header - Module title. */
	public static final String HEADER_TITLE = "title";
	/** Header - Author Name, Pen Name, or Pseudonym. */
	public static final String HEADER_AUTHOR = "author";
	/** Header - Author E-mail. */
	public static final String HEADER_EMAIL = "email";
	/** Header - Author Twitter. */
	public static final String HEADER_TWITTER = "twitter";
	/** Header - Author Website. */
	public static final String HEADER_WEBSITE = "website";
	/** Header - Origin country. */
	public static final String HEADER_COUNTRY = "country";
	/** Header - Module language (ISO 639 alpha-2 or alpha-3 language code). */
	public static final String HEADER_LANGUAGE = "language";
	/** Header - Module version or revision. */
	public static final String HEADER_VERSION = "version";

}
