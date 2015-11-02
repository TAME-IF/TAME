package net.mtrop.tame.factory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import com.blackrook.commons.Common;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.commons.hash.HashedQueueMap;
import com.blackrook.commons.linkedlist.Stack;
import com.blackrook.lang.CommonLexer;
import com.blackrook.lang.CommonLexerKernel;
import com.blackrook.lang.Lexer;
import com.blackrook.lang.Parser;

import net.mtrop.tame.TAMECommand;
import net.mtrop.tame.TAMEConstants;
import net.mtrop.tame.TAMEModule;
import net.mtrop.tame.element.TElement;
import net.mtrop.tame.lang.Block;
import net.mtrop.tame.lang.Value;

/**
 * A TAMEScript reading class that produces scripts. 
 * @author Matthew Tropiano
 */
public final class TAMEScriptReader implements TAMEConstants
{
	public static final String STREAMNAME_TEXT = "[Text String]";
	
	/** The singular instance for the kernel. */
	private static final TSKernel KERNEL_INSTANCE = new TSKernel();
	/** The singular instance for the default includer. */
	private static final DefaultIncluder DEFAULT_INCLUDER = new DefaultIncluder();
	
	/** 
	 * Default includer to use when none specified.
	 * This includer can either pull from the classpath, URIs, or files.
	 * <p>
	 * <ul>
	 * <li>Paths that start with {@code classpath:} are parsed as resource paths in the current classpath.</li>
	 * <li>
	 * 		Else, the path is interpreted as a file path, with the following search order:
	 * 		<ul>
	 * 			<li>Relative to parent of source stream.</li>
	 * 			<li>As is.</li>
	 * 		</ul>
	 * </li>
	 * </ul> 
	 */
	public static class DefaultIncluder implements TAMEScriptIncluder
	{
		private static final String CLASSPATH_PREFIX = "classpath:";
		
		// cannot be instantiated outside of this class.
		private DefaultIncluder(){}
		
		@Override
		public InputStream getIncludeResource(String streamName, String path) throws IOException
		{
			if (Common.isWindows() && streamName.contains("\\")) // check for Windows paths.
				streamName = streamName.replace('\\', '/');
			
			String streamParent = null;
			int lidx = -1; 
			if ((lidx = streamName.lastIndexOf('/')) >= 0)
				streamParent = streamName.substring(0, lidx + 1);
			
			if (path.startsWith(CLASSPATH_PREFIX) || (streamParent != null && streamParent.startsWith(CLASSPATH_PREFIX)))
				return Common.openResource(((streamParent != null ? streamParent : "") + path).substring(CLASSPATH_PREFIX.length()));
			else
			{
				File f = null;
				if (streamParent != null)
				{
					f = new File(streamParent + path);
					if (f.exists())
						return new FileInputStream(f);
					else
						return new FileInputStream(new File(path));
				}
				else
				{
					return new FileInputStream(new File(path));
				}
				
			}
			
		}
	}

	/** The Lexer Kernel for the ArcheText Lexers. */
	private static class TSKernel extends CommonLexerKernel
	{
		static final int TYPE_COMMENT = 				0;
		static final int TYPE_FALSE = 					1;
		static final int TYPE_TRUE = 					2;

		static final int TYPE_COMMAND_INTERNAL = 		3;
		static final int TYPE_COMMAND = 				4;
		static final int TYPE_COMMAND_EXPRESSION = 		5;
		static final int TYPE_ELSE =					6;

		static final int TYPE_DELIM_LPAREN =			10;
		static final int TYPE_DELIM_RPAREN =			11;
		static final int TYPE_DELIM_LBRACE = 			12;
		static final int TYPE_DELIM_RBRACE = 			13;
		static final int TYPE_DELIM_LBRACK = 			14;
		static final int TYPE_DELIM_RBRACK = 			15;
		static final int TYPE_DELIM_COLON = 			16;
		static final int TYPE_DELIM_SEMICOLON = 		17;
		static final int TYPE_DELIM_COMMA = 			18;
		static final int TYPE_DELIM_DOT = 				19;
		static final int TYPE_DELIM_PLUS =				20;
		static final int TYPE_DELIM_MINUS =				21;
		static final int TYPE_DELIM_TILDE =				22;
		static final int TYPE_DELIM_EXCLAMATION =		23;
		static final int TYPE_DELIM_STAR = 				24;
		static final int TYPE_DELIM_SLASH = 			25;
		static final int TYPE_DELIM_PERCENT = 			26;
		static final int TYPE_DELIM_AMPERSAND = 		27;
		static final int TYPE_DELIM_AMPERSAND2 = 		28;
		static final int TYPE_DELIM_PIPE = 				29;
		static final int TYPE_DELIM_PIPE2 = 			30;
		static final int TYPE_DELIM_CARAT = 			31;
		static final int TYPE_DELIM_CARAT2 = 			32;
		static final int TYPE_DELIM_LESS = 				33;
		static final int TYPE_DELIM_LESS2 = 			34;
		static final int TYPE_DELIM_LESSEQUAL = 		35;
		static final int TYPE_DELIM_GREATER =			36;
		static final int TYPE_DELIM_GREATER2 = 			37;
		static final int TYPE_DELIM_GREATER3 = 			38;
		static final int TYPE_DELIM_GREATEREQUAL = 		39;
		static final int TYPE_DELIM_EQUAL = 			40;
		static final int TYPE_DELIM_EQUAL2 = 			41;
		static final int TYPE_DELIM_EQUAL3 = 			42;
		static final int TYPE_DELIM_NOTEQUAL = 			43;
		static final int TYPE_DELIM_NOTEQUALEQUAL =		44;

		static final int TYPE_WORLD = 					50;
		static final int TYPE_ROOM = 					51;
		static final int TYPE_PLAYER = 					52;
		static final int TYPE_OBJECT = 					53;
		static final int TYPE_CONTAINER =				54;
		static final int TYPE_ACTION = 					55;
		static final int TYPE_GENERAL = 				56;
		static final int TYPE_MODAL = 					57;
		static final int TYPE_TRANSITIVE = 				58;
		static final int TYPE_DITRANSITIVE = 			59;
		static final int TYPE_OPEN = 					60;
		static final int TYPE_NAMED = 					61;
		static final int TYPE_MODES = 					62;
		static final int TYPE_CONJOINS = 				63;
		static final int TYPE_EXCLUDES = 				64;
		static final int TYPE_RESTRICTS = 				65;

		static final int TYPE_ONINIT = 					70;
		static final int TYPE_ONACTION =				71;
		static final int TYPE_ONACTIONWITH =			72;
		static final int TYPE_ONACTIONWITHOTHER =		73;
		static final int TYPE_ONROOMBROWSE =			74;
		static final int TYPE_ONPLAYERBROWSE =			75;
		static final int TYPE_ONCONTAINERBROWSE =		76;
		static final int TYPE_ONMODALACTION =			77;
		static final int TYPE_ONFOCUS =					78;
		static final int TYPE_ONUNFOCUS =				79;
		static final int TYPE_ONBADACTION =				80;
		static final int TYPE_ONAMBIGUOUSACTION =		81;
		static final int TYPE_ONFORBIDDENACTION =		82;
		static final int TYPE_ONACTIONFAIL =			83;
		
		/** Internal Command map. */
		static CaseInsensitiveHashMap<TAMECommand> INTERNAL_COMMAND_MAP = new CaseInsensitiveHashMap<TAMECommand>();
		/** Command map. */
		static CaseInsensitiveHashMap<TAMECommand> COMMAND_MAP = new CaseInsensitiveHashMap<TAMECommand>();
		/** Expression Command map. */
		static CaseInsensitiveHashMap<TAMECommand> EXPRESSION_COMMAND_MAP = new CaseInsensitiveHashMap<TAMECommand>();
	
		private TSKernel()
		{
			addStringDelimiter('"', '"');
			
			addCommentStartDelimiter("/*", TYPE_COMMENT);
			addCommentLineDelimiter("//", TYPE_COMMENT);
			addCommentEndDelimiter("*/", TYPE_COMMENT);

			addCaseInsensitiveKeyword("true", TYPE_TRUE);
			addCaseInsensitiveKeyword("false", TYPE_FALSE);
			addCaseInsensitiveKeyword("else", TYPE_ELSE);

			addDelimiter("(", TYPE_DELIM_LPAREN);
			addDelimiter(")", TYPE_DELIM_RPAREN);
			addDelimiter("{", TYPE_DELIM_LBRACE);
			addDelimiter("}", TYPE_DELIM_RBRACE);
			addDelimiter("[", TYPE_DELIM_LBRACK);
			addDelimiter("]", TYPE_DELIM_RBRACK);
			addDelimiter(":", TYPE_DELIM_COLON);
			addDelimiter(";", TYPE_DELIM_SEMICOLON);
			addDelimiter(",", TYPE_DELIM_COMMA);
			addDelimiter(".", TYPE_DELIM_DOT);
			addDelimiter("+", TYPE_DELIM_PLUS);
			addDelimiter("-", TYPE_DELIM_MINUS);
			addDelimiter("!", TYPE_DELIM_EXCLAMATION);
			addDelimiter("~", TYPE_DELIM_TILDE);
			addDelimiter("*", TYPE_DELIM_STAR);
			addDelimiter("/", TYPE_DELIM_SLASH);
			addDelimiter("%", TYPE_DELIM_PERCENT);
			addDelimiter("&", TYPE_DELIM_AMPERSAND);
			addDelimiter("&&", TYPE_DELIM_AMPERSAND2);
			addDelimiter("|", TYPE_DELIM_PIPE);
			addDelimiter("||", TYPE_DELIM_PIPE2);
			addDelimiter("^", TYPE_DELIM_CARAT);
			addDelimiter("^^", TYPE_DELIM_CARAT2);
			addDelimiter("<", TYPE_DELIM_LESS);
			addDelimiter("<<", TYPE_DELIM_LESS2);
			addDelimiter("<=", TYPE_DELIM_LESSEQUAL);
			addDelimiter(">", TYPE_DELIM_GREATER);
			addDelimiter(">>", TYPE_DELIM_GREATER2);
			addDelimiter(">>>", TYPE_DELIM_GREATER3);
			addDelimiter(">=", TYPE_DELIM_GREATEREQUAL);
			addDelimiter("=", TYPE_DELIM_EQUAL);
			addDelimiter("==", TYPE_DELIM_EQUAL2);
			addDelimiter("===", TYPE_DELIM_EQUAL3);
			addDelimiter("!=", TYPE_DELIM_NOTEQUAL);
			addDelimiter("!==", TYPE_DELIM_NOTEQUALEQUAL);
			
			addCaseInsensitiveKeyword("world", TYPE_WORLD);
			addCaseInsensitiveKeyword("room", TYPE_ROOM);
			addCaseInsensitiveKeyword("player", TYPE_PLAYER);
			addCaseInsensitiveKeyword("object", TYPE_OBJECT);
			addCaseInsensitiveKeyword("container", TYPE_CONTAINER);
			addCaseInsensitiveKeyword("action", TYPE_ACTION);
			addCaseInsensitiveKeyword("general", TYPE_GENERAL);
			addCaseInsensitiveKeyword("modal", TYPE_MODAL);
			addCaseInsensitiveKeyword("transitive", TYPE_TRANSITIVE);
			addCaseInsensitiveKeyword("ditransitive", TYPE_DITRANSITIVE);
			addCaseInsensitiveKeyword("open", TYPE_OPEN);
			addCaseInsensitiveKeyword("named", TYPE_NAMED);
			addCaseInsensitiveKeyword("modes", TYPE_MODES);
			addCaseInsensitiveKeyword("conjoins", TYPE_CONJOINS);
			addCaseInsensitiveKeyword("excludes", TYPE_EXCLUDES);
			addCaseInsensitiveKeyword("restricts", TYPE_RESTRICTS);
			addCaseInsensitiveKeyword("oninit", TYPE_ONINIT);
			addCaseInsensitiveKeyword("onaction", TYPE_ONACTION);
			addCaseInsensitiveKeyword("onactionwith", TYPE_ONACTIONWITH);
			addCaseInsensitiveKeyword("onactionwithother", TYPE_ONACTIONWITHOTHER);
			addCaseInsensitiveKeyword("onroombrowse", TYPE_ONROOMBROWSE);
			addCaseInsensitiveKeyword("onplayerbrowse", TYPE_ONPLAYERBROWSE);
			addCaseInsensitiveKeyword("oncontainerbrowse", TYPE_ONCONTAINERBROWSE);
			addCaseInsensitiveKeyword("onmodalaction", TYPE_ONMODALACTION);
			addCaseInsensitiveKeyword("onfocus", TYPE_ONFOCUS);
			addCaseInsensitiveKeyword("onunfocus", TYPE_ONUNFOCUS);
			addCaseInsensitiveKeyword("onbadaction", TYPE_ONBADACTION);
			addCaseInsensitiveKeyword("onambiguousaction", TYPE_ONAMBIGUOUSACTION);
			addCaseInsensitiveKeyword("onforbiddenaction", TYPE_ONFORBIDDENACTION);
			addCaseInsensitiveKeyword("onactionfail", TYPE_ONACTIONFAIL);
			
			for (TAMECommand command : TAMECommand.values())
			{
				String name = command.name();
				if (command.isInternal())
				{
					addCaseInsensitiveKeyword(name, TYPE_COMMAND_INTERNAL);
					INTERNAL_COMMAND_MAP.put(name, command);
				}
				else if (command.getReturnType() != null)
				{
					addCaseInsensitiveKeyword(name, TYPE_COMMAND_EXPRESSION);
					EXPRESSION_COMMAND_MAP.put(name, command);
				}
				else
				{
					addCaseInsensitiveKeyword(name, TYPE_COMMAND);
					COMMAND_MAP.put(name, command);
				}
			}
			
		}
		
	}

	/**
	 * The lexer for a reader context.
	 */
	private static class TSLexer extends CommonLexer
	{
		private TAMEScriptIncluder includer;
		
		private TSLexer(Reader in, TAMEScriptIncluder includer)
		{
			super(KERNEL_INSTANCE, in);
			this.includer = includer;
		}
	
		private TSLexer(String in, TAMEScriptIncluder includer)
		{
			super(KERNEL_INSTANCE, in);
			this.includer = includer;
		}
		
		private TSLexer(String name, Reader in, TAMEScriptIncluder includer)
		{
			super(KERNEL_INSTANCE, name, in);
			this.includer = includer;
		}
	
		private TSLexer(String name, String in, TAMEScriptIncluder includer)
		{
			super(KERNEL_INSTANCE, name, in);
			this.includer = includer;
		}
		
		@Override
		public InputStream getResource(String path) throws IOException
		{
			return includer.getIncludeResource(getCurrentStreamName(), path);
		}
	}

	/**
	 * The parser that parses text for the ArcheText structures. 
	 */
	private static class TSParser extends Parser
	{
		/** Set of prototypes. */
		private HashedQueueMap<String, String> prototypes;
		
		/** Current root. */
		private TAMEModule currentModule;
		/** Current object. */
		private TElement currentElement;
		/** Current value from a parseValue() call. */
		private String currentElementIdentifier;
		/** Current element block type. */
		private String currentElementBlockType;
		/** Current element block type. */
		private String currentElementBlockArgument;
		/** Current block. */
		private Stack<Block> currentBlock;
		/** Current command type. */
		private TAMECommand currentCommand;
		/** Current value. */
		private Value currentValue;
		
		private TSParser(TSLexer lexer)
		{
			super(lexer);
			prototypes = new HashedQueueMap<>();
			currentModule = null;
			currentElement = null;
			currentElementIdentifier = null;
			currentElementBlockType = null;
			currentElementBlockArgument = null;
			currentBlock = new Stack<Block>();
			currentCommand = null;
		}
		
		/**
		 * Reads objects into the target root.
		 */
		TAMEModule readModule()
		{
			currentModule = new TAMEModule();
			
			// prime first token.
			nextToken();
			
			// keep parsing entries.
			boolean noError = true;
			while (currentToken() != null && (noError = parseModuleElement())) ;
			
			if (!noError) // awkward, I know.
			{
				String[] errors = getErrorMessages();
				if (errors.length > 0)
				{
					StringBuilder sb = new StringBuilder();
					for (int i = 0; i < errors.length; i++)
					{
						sb.append(errors[i]);
						if (i < errors.length-1)
							sb.append('\n');
					}
					throw new TAMEScriptParseException(sb.toString());
				}
			}
			
			return currentModule;
		}

		@Override
		protected String getTypeErrorText(int tokenType) 
		{
			switch (tokenType)
			{
				case TSKernel.TYPE_ACTION:
					return "'action'";
				case TSKernel.TYPE_ROOM:
					return "'room'";
				case TSKernel.TYPE_PLAYER:
					return "'player'";
				case TSKernel.TYPE_OBJECT:
					return "'object'";
				case TSKernel.TYPE_CONTAINER:
					return "'container'";
				case TSKernel.TYPE_DELIM_LPAREN:
					return "'('";
				case TSKernel.TYPE_DELIM_RPAREN:
					return "')'";
				case TSKernel.TYPE_DELIM_LBRACE:
					return "'{'";
				case TSKernel.TYPE_DELIM_RBRACE:
					return "'}'";
				case TSKernel.TYPE_DELIM_SEMICOLON:
					return "';'";
				case TSKernel.TYPE_DELIM_DOT:
					return "'.'";
				case TSKernel.TYPE_DELIM_COLON:
					return "':'";
				case TSKernel.TYPE_DELIM_COMMA:
					return "','";
				case TSKernel.TYPE_DELIM_EQUAL:
					return "'='";
				case TSKernel.TYPE_DELIM_EQUAL2:
					return "'=='";
				case TSKernel.TYPE_DELIM_EQUAL3:
					return "'==='";
				case TSKernel.TYPE_DELIM_NOTEQUAL:
					return "'!='";
				case TSKernel.TYPE_DELIM_NOTEQUALEQUAL:
					return "'!=='";
				case TSKernel.TYPE_DELIM_GREATER:
					return "'>'";
				case TSKernel.TYPE_DELIM_GREATER2:
					return "'>>'";
				case TSKernel.TYPE_DELIM_GREATER3:
					return "'>>>'";
				case TSKernel.TYPE_DELIM_LESS:
					return "'<'";
				case TSKernel.TYPE_DELIM_LESS2:
					return "'<<'";
				case TSKernel.TYPE_DELIM_GREATEREQUAL:
					return "'>='";
				case TSKernel.TYPE_DELIM_LESSEQUAL:
					return "'<='";
				case TSKernel.TYPE_DELIM_AMPERSAND:
					return "'&'";
				case TSKernel.TYPE_DELIM_AMPERSAND2:
					return "'&&'";
				case TSKernel.TYPE_DELIM_PIPE:
					return "'|'";
				case TSKernel.TYPE_DELIM_PIPE2:
					return "'||'";
				case TSKernel.TYPE_DELIM_CARAT:
					return "'^'";
				case TSKernel.TYPE_DELIM_CARAT2:
					return "'^^'";
				case TSKernel.TYPE_DELIM_PLUS:
					return "'+'";
				case TSKernel.TYPE_DELIM_MINUS:
					return "'-'";
				case TSKernel.TYPE_DELIM_STAR:
					return "'*'";
				case TSKernel.TYPE_DELIM_SLASH:
					return "'/'";
				case TSKernel.TYPE_DELIM_PERCENT:
					return "'%'";
				case TSKernel.TYPE_DELIM_EXCLAMATION:
					return "'!'";
			}
			return null;
		}
	
		/**
		 * Parses a module element.
		 */
		private boolean parseModuleElement()
		{
			return false;
		}
		
		/*
		 * Parses an infix expression.
		 * May throw an ArcheTextOperationException if some values cannot be calculated or combined. 
		 */
		private boolean parseExpression()
		{
			// make stacks.
			Stack<Integer> expressionOperators = new Stack<>();
			Stack<Value> expressionValues = new Stack<>();
			
			// was the last read token a value?
			boolean lastWasValue = false;
			boolean keepGoing = true;
			
			while (keepGoing)
			{
				if (currentType(Lexer.TYPE_IDENTIFIER))
				{
					if (lastWasValue)
					{
						addErrorMessage("Expression error - expected operator.");
						return false;
					}
					
					String identname = currentToken().getLexeme();
					
					expressionValues.push(Value.createVariable(identname));
					
					nextToken();
					lastWasValue = true;
				}
				else if (matchType(TSKernel.TYPE_DELIM_LPAREN))
				{
					if (lastWasValue)
					{
						addErrorMessage("Expression error - expected operator.");
						return false;
					}
					
					if (!parseExpression())
						return false;
					
					// NOTE: Expression ends in a push.
					
					if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
					{
						addErrorMessage("Expected ending parenthesis (')').");
						return false;
					}

					lastWasValue = true;
				}
				else if (isValidLiteralType())
				{
					if (lastWasValue)
					{
						addErrorMessage("Expression error - expected operator.");
						return false;
					}
					
					tokenToValue();
					expressionValues.push(currentValue);
					lastWasValue = true;
				}
				else if (lastWasValue)
				{
					if (isBinaryOperatorType())
					{
						int nextOperator = -1;
						
						switch (currentToken().getType())
						{
							case TSKernel.TYPE_DELIM_DOT:
								nextOperator = ARITHMETIC_FUNCTION_RESOLVE;
								break;
							case TSKernel.TYPE_DELIM_PLUS:
								nextOperator = ARITHMETIC_FUNCTION_ADD;
								break;
							case TSKernel.TYPE_DELIM_MINUS:
								nextOperator = ARITHMETIC_FUNCTION_SUBTRACT;
								break;
							case TSKernel.TYPE_DELIM_STAR:
								nextOperator = ARITHMETIC_FUNCTION_MULTIPLY;
								break;
							case TSKernel.TYPE_DELIM_SLASH:
								nextOperator = ARITHMETIC_FUNCTION_DIVIDE;
								break;
							case TSKernel.TYPE_DELIM_PERCENT:
								nextOperator = ARITHMETIC_FUNCTION_MODULO;
								break;
							case TSKernel.TYPE_DELIM_AMPERSAND:
								nextOperator = ARITHMETIC_FUNCTION_AND;
								break;
							case TSKernel.TYPE_DELIM_AMPERSAND2:
								nextOperator = ARITHMETIC_FUNCTION_LOGICAL_AND;
								break;
							case TSKernel.TYPE_DELIM_PIPE:
								nextOperator = ARITHMETIC_FUNCTION_OR;
								break;
							case TSKernel.TYPE_DELIM_PIPE2:
								nextOperator = ARITHMETIC_FUNCTION_LOGICAL_OR;
								break;
							case TSKernel.TYPE_DELIM_CARAT:
								nextOperator = ARITHMETIC_FUNCTION_XOR;
								break;
							case TSKernel.TYPE_DELIM_CARAT2:
								nextOperator = ARITHMETIC_FUNCTION_LOGICAL_XOR;
								break;
							case TSKernel.TYPE_DELIM_LESS:
								nextOperator = ARITHMETIC_FUNCTION_LESS;
								break;
							case TSKernel.TYPE_DELIM_LESS2:
								nextOperator = ARITHMETIC_FUNCTION_LSHIFT;
								break;
							case TSKernel.TYPE_DELIM_LESSEQUAL:
								nextOperator = ARITHMETIC_FUNCTION_LESS_OR_EQUAL;
								break;
							case TSKernel.TYPE_DELIM_GREATER:
								nextOperator = ARITHMETIC_FUNCTION_GREATER;
								break;
							case TSKernel.TYPE_DELIM_GREATER2:
								nextOperator = ARITHMETIC_FUNCTION_RSHIFT;
								break;
							case TSKernel.TYPE_DELIM_GREATER3:
								nextOperator = ARITHMETIC_FUNCTION_RSHIFTPAD;
								break;
							case TSKernel.TYPE_DELIM_GREATEREQUAL:
								nextOperator = ARITHMETIC_FUNCTION_GREATER_OR_EQUAL;
								break;
							case TSKernel.TYPE_DELIM_EQUAL2:
								nextOperator = ARITHMETIC_FUNCTION_EQUALS;
								break;
							case TSKernel.TYPE_DELIM_EQUAL3:
								nextOperator = ARITHMETIC_FUNCTION_STRICT_EQUALS;
								break;
							case TSKernel.TYPE_DELIM_NOTEQUAL:
								nextOperator = ARITHMETIC_FUNCTION_NOT_EQUALS;
								break;
							case TSKernel.TYPE_DELIM_NOTEQUALEQUAL:
								nextOperator = ARITHMETIC_FUNCTION_STRICT_NOT_EQUALS;
								break;
							default:
								throw new TAMEScriptParseException("Internal error - unexpected binary operator miss.");
						}
						
						nextToken();

						if (!operatorReduce(expressionOperators, expressionValues, nextOperator))
							return false;
						
						expressionOperators.push(nextOperator);
						lastWasValue = false;
					}
					else // end on a value
					{
						keepGoing = false;
					}
				}
				else if (isUnaryOperatorType())
				{
					switch (currentToken().getType())
					{
						case TSKernel.TYPE_DELIM_MINUS:
							expressionOperators.push(ARITHMETIC_FUNCTION_NEGATE);
							break;
						case TSKernel.TYPE_DELIM_PLUS:
							expressionOperators.push(ARITHMETIC_FUNCTION_ABSOLUTE);
							break;
						case TSKernel.TYPE_DELIM_TILDE:
							expressionOperators.push(ARITHMETIC_FUNCTION_NOT);
							break;
						case TSKernel.TYPE_DELIM_EXCLAMATION:
							expressionOperators.push(ARITHMETIC_FUNCTION_LOGICAL_NOT);
							break;
						default:
							throw new TAMEScriptParseException("Internal error - unexpected unary operator miss.");
					}
					
					nextToken();
					lastWasValue = false;
				}
				else
				{
					addErrorMessage("Expression error - expected value after operator.");
					return false;
				}
				
			}
			
			// end of expression - reduce.
			while (!expressionOperators.isEmpty())
			{
				if (!expressionReduce(expressionOperators, expressionValues))
					return false;
			}
			
			if (expressionValues.isEmpty())
			{
				addErrorMessage("Expected valid expression.");
				return false;
			}

			return true;
		}
		
		private boolean operatorReduce(Stack<Integer> expressionOperators, Stack<Value> expressionValues, int nextOperator) 
		{
			// TODO Reduces several operators expressions.
			return false;
		}

		private boolean expressionReduce(Stack<Integer> expressionOperators, Stack<Value> expressionValues)
		{
			// TODO Reduces expressions.
			return false;
		}

		// Token to value.
		private boolean tokenToValue()
		{
			if (isWorld())
				currentValue = Value.createWorld();
			else if (isPlayer())
				currentValue = Value.createPlayer(currentToken().getLexeme());
			else if (isRoom())
				currentValue = Value.createRoom(currentToken().getLexeme());
			else if (isObject())
				currentValue = Value.createObject(currentToken().getLexeme());
			else if (isContainer())
				currentValue = Value.createContainer(currentToken().getLexeme());
			else if (currentType(Lexer.TYPE_STRING))
				currentValue = Value.create(currentToken().getLexeme());
			else if (currentType(Lexer.TYPE_NUMBER))
			{
				String lexeme = currentToken().getLexeme();
				if (lexeme.startsWith("0X") || lexeme.startsWith("0x"))
					currentValue = Value.create(Long.parseLong(lexeme.substring(2), 16));
				else if (lexeme.contains("."))
					currentValue = Value.create(Double.parseDouble(lexeme));
				else
					currentValue = Value.create(Long.parseLong(lexeme));
			}
			else if (currentType(Lexer.TYPE_IDENTIFIER))
				currentValue = Value.createVariable(currentToken().getLexeme());
			else if (currentType(TSKernel.TYPE_TRUE))
				currentValue = Value.create(true);
			else if (currentType(TSKernel.TYPE_FALSE))
				currentValue = Value.create(false);
			else
				throw new TAMEScriptParseException("Internal error - unexpected token type.");

			nextToken();
			return true;
		}
		
		// Checks if an identifier is a world.
		public boolean isWorld()
		{
			return currentToken().getLexeme().equals(IDENTITY_CURRENT_WORLD);
		}
		
		// Checks if an identifier is a player.
		public boolean isPlayer()
		{
			String identifier = currentToken().getLexeme();
			return identifier.equals(IDENTITY_CURRENT_PLAYER)
				|| currentModule.getPlayerByIdentity(identifier) != null;
		}
		
		// Checks if an identifier is a room.
		public boolean isRoom()
		{
			String identifier = currentToken().getLexeme();
			return identifier.equals(IDENTITY_CURRENT_ROOM)
				|| currentModule.getRoomByIdentity(identifier) != null;
		}
		
		// Checks if an identifier is an object.
		public boolean isObject()
		{
			return currentModule.getObjectByIdentity(currentToken().getLexeme()) != null;
		}
		
		// Checks if an identifier is a container.
		public boolean isContainer()
		{
			return currentModule.getContainerByIdentity(currentToken().getLexeme()) != null;
		}
		
		/**
		 * Returns the operator precedence.
		 * Greater values have higher precedence.
		 */
		public int getOperatorPrecedence(int operator)
		{
			switch (operator)
			{
				default:
					return Integer.MIN_VALUE;
				// TODO: Finish this.
			}
		}

		/**
		 * Returns if an operator has right-associativity.
		 */
		public boolean isOperatorRightAssociative(int operator)
		{
			// TODO: Finish this.
			return false;
		}
		
		// Return true if token type can be a unary operator.
		private boolean isValidLiteralType()
		{
			switch (currentToken().getType())
			{
				case Lexer.TYPE_STRING:
				case Lexer.TYPE_NUMBER:
				case TSKernel.TYPE_TRUE:
				case TSKernel.TYPE_FALSE:
					return true;
				default:
					return false;
			}
		}
		
		// Return true if token type can be a unary operator.
		private boolean isUnaryOperatorType()
		{
			switch (currentToken().getType())
			{
				case TSKernel.TYPE_DELIM_MINUS:
				case TSKernel.TYPE_DELIM_PLUS:
				case TSKernel.TYPE_DELIM_TILDE:
				case TSKernel.TYPE_DELIM_EXCLAMATION:
					return true;
				default:
					return false;
			}
		}
		
		// Return true if token type can be a binary operator.
		private boolean isBinaryOperatorType()
		{
			switch (currentToken().getType())
			{
				case TSKernel.TYPE_DELIM_PLUS:
				case TSKernel.TYPE_DELIM_MINUS:
				case TSKernel.TYPE_DELIM_STAR:
				case TSKernel.TYPE_DELIM_SLASH:
				case TSKernel.TYPE_DELIM_PERCENT:
				case TSKernel.TYPE_DELIM_AMPERSAND:
				case TSKernel.TYPE_DELIM_AMPERSAND2:
				case TSKernel.TYPE_DELIM_PIPE:
				case TSKernel.TYPE_DELIM_PIPE2:
				case TSKernel.TYPE_DELIM_CARAT:
				case TSKernel.TYPE_DELIM_CARAT2:
				case TSKernel.TYPE_DELIM_GREATER:
				case TSKernel.TYPE_DELIM_GREATER2:
				case TSKernel.TYPE_DELIM_GREATER3:
				case TSKernel.TYPE_DELIM_GREATEREQUAL:
				case TSKernel.TYPE_DELIM_LESS:
				case TSKernel.TYPE_DELIM_LESS2:
				case TSKernel.TYPE_DELIM_LESSEQUAL:
				case TSKernel.TYPE_DELIM_EQUAL:
				case TSKernel.TYPE_DELIM_EQUAL2:
				case TSKernel.TYPE_DELIM_EQUAL3:
				case TSKernel.TYPE_DELIM_NOTEQUAL:
				case TSKernel.TYPE_DELIM_NOTEQUALEQUAL:
					return true;
				default:
					return false;
			}
		}
		
	}

	/**
	 * Reads TAMEModule objects into a new root from a starting text file.
	 * Note: Calls apply() with a new root.
	 * @param file	the file to read from.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if f is null. 
	 */
	public static TAMEModule read(File file) throws IOException
	{
		FileInputStream fis = new FileInputStream(file);
		try {
			return read(file.getPath(), fis, DEFAULT_INCLUDER);
		} finally {
			Common.close(fis);
		}
	}

	/**
	 * Reads TAMEModule objects from a String of text into a new root.
	 * Note: Calls apply() with a new root.
	 * @param text the String to read from.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if f is null. 
	 */
	public static TAMEModule read(String text) throws IOException
	{
		return read(STREAMNAME_TEXT, new StringReader(text), DEFAULT_INCLUDER);
	}

	/**
	 * Reads TAMEModule objects into a new root.
	 * Note: Calls apply() with a new root.
	 * @param streamName the name of the stream.
	 * @param in the stream to read from.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if in is null. 
	 */
	public static TAMEModule read(String streamName, InputStream in) throws IOException
	{
		return read(streamName, new InputStreamReader(in), DEFAULT_INCLUDER);
	}

	/**
	 * Reads TAMEModule objects into a new root from a reader stream.
	 * Note: Calls apply() with a new root.
	 * @param streamName the name of the stream.
	 * @param reader the reader to read from.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if f is null. 
	 */
	public static TAMEModule read(String streamName, Reader reader) throws IOException
	{
		return read(streamName, reader, DEFAULT_INCLUDER);
	}

	/**
	 * Reads TAMEModule objects into a new root from a starting text file.
	 * Note: Calls apply() with a new root.
	 * @param file	the file to read from.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if f is null. 
	 */
	public static TAMEModule read(File file, TAMEScriptIncluder includer) throws IOException
	{
		FileInputStream fis = new FileInputStream(file);
		try {
			return read(file.getPath(), fis, includer);
		} finally {
			Common.close(fis);
		}
	}

	/**
	 * Reads TAMEModule objects from a String of text into a new root.
	 * Note: Calls apply() with a new root.
	 * @param text the String to read from.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if f is null. 
	 */
	public static TAMEModule read(String text, TAMEScriptIncluder includer) throws IOException
	{
		return read(STREAMNAME_TEXT, new StringReader(text), includer);
	}

	/**
	 * Reads TAMEModule objects into a new root.
	 * Note: Calls apply() with a new root.
	 * @param streamName the name of the stream.
	 * @param in the stream to read from.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if in is null. 
	 */
	public static TAMEModule read(String streamName, InputStream in, TAMEScriptIncluder includer) throws IOException
	{
		return read(streamName, new InputStreamReader(in), includer);
	}

	/**
	 * Reads TAMEModule objects into a new root from a reader stream.
	 * Note: Calls apply() with a new root.
	 * @param streamName the name of the stream.
	 * @param reader the reader to read from.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if f is null. 
	 */
	public static TAMEModule read(String streamName, Reader reader, TAMEScriptIncluder includer) throws IOException
	{
		TSLexer lexer = new TSLexer(streamName, reader, includer);
		TSParser parser = new TSParser(lexer);
		return parser.readModule();
	}

}
