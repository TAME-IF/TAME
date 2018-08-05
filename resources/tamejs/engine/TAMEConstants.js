/*******************************************************************************
 * Copyright (c) 2016-2018 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/

//##[[EXPORTJS-START

var TAMEConstants = {};
TAMEConstants.ActionType = {};
TAMEConstants.Cue = {};
TAMEConstants.Identity = {};
TAMEConstants.TraceType = {};

TAMEConstants.ActionType.GENERAL = 0;
TAMEConstants.ActionType.TRANSITIVE = 1;
TAMEConstants.ActionType.DITRANSITIVE = 2;
TAMEConstants.ActionType.MODAL = 3;
TAMEConstants.ActionType.OPEN = 4;

TAMEConstants.Cue.QUIT = "QUIT";
TAMEConstants.Cue.TEXT = "TEXT";
TAMEConstants.Cue.TEXTF = "TEXTF";
TAMEConstants.Cue.WAIT = "WAIT";
TAMEConstants.Cue.PAUSE = "PAUSE";
TAMEConstants.Cue.TRACE = "TRACE";
TAMEConstants.Cue.ERROR = "ERROR";
TAMEConstants.Cue.FATAL = "FATAL";

TAMEConstants.Identity.ROOM = "room";
TAMEConstants.Identity.PLAYER = "player";
TAMEConstants.Identity.WORLD = "world";

TAMEConstants.TraceType.INTERPRETER = "INTERPRETER";
TAMEConstants.TraceType.CONTEXT = "CONTEXT";
TAMEConstants.TraceType.ENTRY = "ENTRY";
TAMEConstants.TraceType.CONTROL = "CONTROL";
TAMEConstants.TraceType.FUNCTION = "FUNCTION";
TAMEConstants.TraceType.INTERNAL = "INTERNAL";
TAMEConstants.TraceType.VALUE = "VALUE";

TAMEConstants.DEFAULT_RUNAWAY_THRESHOLD = 100000;
TAMEConstants.DEFAULT_FUNCTION_DEPTH = 256;
TAMEConstants.RETURN_VARIABLE = "-. 0Return0 .-";

//##[[EXPORTJS-END

//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAMEConstants;
// =========================================================================