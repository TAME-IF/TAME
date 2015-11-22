package net.mtrop.tame;

/**
 * Contains constants.
 * @author Matthew Tropiano
 */
public interface TAMEConstants
{
	/** Quit cue. */
	public static final String CUE_QUIT = "QUIT";
	/** Save cue. */
	public static final String CUE_SAVE = "SAVE";
	/** Load cue. */
	public static final String CUE_LOAD = "LOAD";
	/** Text cue. */
	public static final String CUE_TEXT = "TEXT";
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

	/** Hardcoded runaway max. */
	public static final int RUNAWAY_THRESHOLD = 100000;

	/** Open target name. */
	public static final String OPEN_TARGET_VARIABLE = "_target";

}
