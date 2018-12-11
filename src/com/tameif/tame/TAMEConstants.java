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

/**
 * Contains constants.
 * @author Matthew Tropiano
 */
public interface TAMEConstants
{
	/** Quit cue. */
	public static final String CUE_QUIT = "quit";
	/** Text cue. */
	public static final String CUE_TEXT = "text";
	/** Formatted Text cue. */
	public static final String CUE_TEXTF = "textf";
	/** Wait cue. */
	public static final String CUE_WAIT = "wait";
	/** Pause cue. */
	public static final String CUE_PAUSE = "pause";
	/** Trace cue. */
	public static final String CUE_TRACE = "trace";
	/** Error cue. */
	public static final String CUE_ERROR = "error";
	/** Fatal cue. */
	public static final String CUE_FATAL = "fatal";

	/** Identity of archetype container. */
	public static final String IDENTITY_ARCHETYPE_CONTAINER = "container";
	/** Identity of current room. */
	public static final String IDENTITY_CURRENT_ROOM = "room";
	/** Identity of current player. */
	public static final String IDENTITY_CURRENT_PLAYER = "player";
	/** Identity of current world. */
	public static final String IDENTITY_CURRENT_WORLD = "world";
	
	/** Return variable. */
	public static final String RETURN_VARIABLE = "-. 0Return0 .-";

	/** Default runaway max. */
	public static final int DEFAULT_RUNAWAY_THRESHOLD = 100000;
	/** Default function depth max. */
	public static final int DEFAULT_FUNCTION_DEPTH = 256;

	/* ========= Standard headers that affect things. ========= */
	
	/** Header - TAME Option - Max Runaway Operations. */
	public static final String HEADER_TAME_RUNAWAY_MAX = "tame_runaway_max";
	/** Header - TAME Option - Max Function Depth. */
	public static final String HEADER_TAME_FUNCDEPTH_MAX = "tame_funcdepth_max";

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
