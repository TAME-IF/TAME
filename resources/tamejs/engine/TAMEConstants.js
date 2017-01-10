/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
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
TAMEConstants.RestrictionType = {};
TAMEConstants.Cue = {};
TAMEConstants.Identity = {};

TAMEConstants.ActionType.GENERAL = 0;
TAMEConstants.ActionType.TRANSITIVE = 1;
TAMEConstants.ActionType.DITRANSITIVE = 2;
TAMEConstants.ActionType.MODAL = 3;
TAMEConstants.ActionType.OPEN = 4;

TAMEConstants.RestrictionType.FORBID = 0;
TAMEConstants.RestrictionType.ALLOW = 1;

TAMEConstants.Cue.QUIT = "QUIT";
TAMEConstants.Cue.TEXT = "TEXT";
TAMEConstants.Cue.TEXTF = "TEXTF";
TAMEConstants.Cue.WAIT = "WAIT";
TAMEConstants.Cue.PAUSE = "PAUSE";
TAMEConstants.Cue.TRACE = "TRACE";
TAMEConstants.Cue.TIP = "TIP";
TAMEConstants.Cue.INFO = "INFO";
TAMEConstants.Cue.ERROR = "ERROR";
TAMEConstants.Cue.FATAL = "FATAL";

TAMEConstants.Identity.ROOM = "room";
TAMEConstants.Identity.PLAYER = "player";
TAMEConstants.Identity.WORLD = "world";

TAMEConstants.RUNAWAY_THRESHOLD = 100000;
TAMEConstants.OPEN_TARGET_VARIABLE = "_target";

//##[[EXPORTJS-END

//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAMEConstants;
// =========================================================================