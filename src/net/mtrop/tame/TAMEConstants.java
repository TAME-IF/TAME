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

	/** Arithmetic function - absolute value. */
	public static final int ARITHMETIC_FUNCTION_ABSOLUTE = 0;
	/** Arithmetic function - negate value. */
	public static final int ARITHMETIC_FUNCTION_NEGATE = 1;
	/** Arithmetic function - logical not. */
	public static final int ARITHMETIC_FUNCTION_LOGICAL_NOT = 2;
	/** Arithmetic function - bitwise not. */
	public static final int ARITHMETIC_FUNCTION_NOT = 3;
	/** Arithmetic function - add. */
	public static final int ARITHMETIC_FUNCTION_ADD = 4;
	/** Arithmetic function - subtract. */
	public static final int ARITHMETIC_FUNCTION_SUBTRACT = 5;
	/** Arithmetic function - multiply. */
	public static final int ARITHMETIC_FUNCTION_MULTIPLY = 6;
	/** Arithmetic function - divide. */
	public static final int ARITHMETIC_FUNCTION_DIVIDE = 7;
	/** Arithmetic function - modulo. */
	public static final int ARITHMETIC_FUNCTION_MODULO = 8;
	/** Arithmetic function - and. */
	public static final int ARITHMETIC_FUNCTION_AND = 9;
	/** Arithmetic function - or. */
	public static final int ARITHMETIC_FUNCTION_OR = 10;
	/** Arithmetic function - xor. */
	public static final int ARITHMETIC_FUNCTION_XOR = 11;
	/** Arithmetic function - left shift. */
	public static final int ARITHMETIC_FUNCTION_LSHIFT = 12;
	/** Arithmetic function - right shift. */
	public static final int ARITHMETIC_FUNCTION_RSHIFT = 13;
	/** Arithmetic function - right shift padded. */
	public static final int ARITHMETIC_FUNCTION_RSHIFTPAD = 14;

	/** Arithmetic function - logical and. */
	public static final int ARITHMETIC_FUNCTION_LOGICAL_AND = 15;
	/** Arithmetic function - logical or. */
	public static final int ARITHMETIC_FUNCTION_LOGICAL_OR = 16;
	/** Arithmetic function - logical xor. */
	public static final int ARITHMETIC_FUNCTION_LOGICAL_XOR = 17;
	/** Arithmetic function - equals. */
	public static final int ARITHMETIC_FUNCTION_EQUALS = 18;
	/** Arithmetic function - not equals. */
	public static final int ARITHMETIC_FUNCTION_NOT_EQUALS = 19;
	/** Arithmetic function - strict equals. */
	public static final int ARITHMETIC_FUNCTION_STRICT_EQUALS = 20;
	/** Arithmetic function - strict not equals. */
	public static final int ARITHMETIC_FUNCTION_STRICT_NOT_EQUALS = 21;
	/** Arithmetic function - less. */
	public static final int ARITHMETIC_FUNCTION_LESS = 22;
	/** Arithmetic function - less or equal. */
	public static final int ARITHMETIC_FUNCTION_LESS_OR_EQUAL = 23;
	/** Arithmetic function - less. */
	public static final int ARITHMETIC_FUNCTION_GREATER = 24;
	/** Arithmetic function - less or equal. */
	public static final int ARITHMETIC_FUNCTION_GREATER_OR_EQUAL = 25;
	
}
