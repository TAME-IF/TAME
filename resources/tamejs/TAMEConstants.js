/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 ******************************************************************************/

//##[[CONTENT-START

var TAMEConstants = {};
TAMEConstants.ActionType = {};
TAMEConstants.RestrictionType = {};
TAMEConstants.Cue = {};
TAMEConstants.Identity = {};

TAMEConstants.ActionType.GENERAL = 1;
TAMEConstants.ActionType.TRANSITIVE = 2;
TAMEConstants.ActionType.DITRANSITIVE = 3;
TAMEConstants.ActionType.MODAL = 4;
TAMEConstants.ActionType.OPEN = 5;

TAMEConstants.RestrictionType.FORBID = 1;
TAMEConstants.RestrictionType.ALLOW = 2;

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

//##[[CONTENT-END

//If testing with NODEJS ==================================================
if ((typeof module.exports) !== 'undefined') module.exports = TAMEConstants;
// =========================================================================