package net.mtrop.tame.factory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import com.blackrook.commons.Common;
import com.blackrook.commons.hash.HashedQueueMap;
import com.blackrook.commons.linkedlist.Stack;
import com.blackrook.lang.CommonLexer;
import com.blackrook.lang.CommonLexerKernel;
import com.blackrook.lang.Lexer;
import com.blackrook.lang.Parser;

import net.mtrop.tame.TAMECommand;
import net.mtrop.tame.TAMEConstants;
import net.mtrop.tame.TAMEModule;
import net.mtrop.tame.element.TAction;
import net.mtrop.tame.element.TElement;
import net.mtrop.tame.lang.ArgumentType;
import net.mtrop.tame.lang.ArithmeticOperator;
import net.mtrop.tame.lang.Block;
import net.mtrop.tame.lang.Command;
import net.mtrop.tame.lang.Value;

/**
 * A TAMEScript reading class that produces scripts. 
 * @author Matthew Tropiano
 */
public final class TAMEScriptReader implements TAMEConstants
{
	public static final String STREAMNAME_TEXT = "Text String";
	
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
		static final int TYPE_COMMAND_CONTROL = 		4;
		static final int TYPE_ELSE =					5;
		static final int TYPE_COMMAND_STATEMENT = 		6;
		static final int TYPE_COMMAND_EXPRESSION = 		7;

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

		static final int TYPE_MODULE = 					50;
		static final int TYPE_WORLD = 					51;
		static final int TYPE_ROOM = 					52;
		static final int TYPE_PLAYER = 					53;
		static final int TYPE_OBJECT = 					54;
		static final int TYPE_CONTAINER =				55;
		static final int TYPE_ACTION = 					56;
		static final int TYPE_GENERAL = 				57;
		static final int TYPE_MODAL = 					58;
		static final int TYPE_TRANSITIVE = 				59;
		static final int TYPE_DITRANSITIVE = 			60;
		static final int TYPE_OPEN = 					61;
		static final int TYPE_NAMED = 					62;
		static final int TYPE_MODES = 					63;
		static final int TYPE_CONJOINS = 				64;
		static final int TYPE_EXCLUDES = 				65;
		static final int TYPE_RESTRICTS = 				66;

		static final int TYPE_INIT = 					70;
		static final int TYPE_ONACTION =				71;
		static final int TYPE_ONACTIONWITH =			72;
		static final int TYPE_ONACTIONWITHOTHER =		73;
		static final int TYPE_ONROOMBROWSE =			74;
		static final int TYPE_ONPLAYERBROWSE =			75;
		static final int TYPE_ONCONTAINERBROWSE =		76;
		static final int TYPE_ONMODALACTION =			77;
		static final int TYPE_ONUNKNOWNACTION =			78;
		static final int TYPE_ONAMBIGUOUSACTION =		79;
		static final int TYPE_ONFORBIDDENACTION =		80;
		static final int TYPE_ONFAILEDACTION =			81;
		static final int TYPE_AFTERREQUEST =			82;
		
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
			
			addCaseInsensitiveKeyword("module", TYPE_MODULE);
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
			addCaseInsensitiveKeyword("init", TYPE_INIT);
			addCaseInsensitiveKeyword("onaction", TYPE_ONACTION);
			addCaseInsensitiveKeyword("onactionwith", TYPE_ONACTIONWITH);
			addCaseInsensitiveKeyword("onactionwithother", TYPE_ONACTIONWITHOTHER);
			addCaseInsensitiveKeyword("onroombrowse", TYPE_ONROOMBROWSE);
			addCaseInsensitiveKeyword("onplayerbrowse", TYPE_ONPLAYERBROWSE);
			addCaseInsensitiveKeyword("oncontainerbrowse", TYPE_ONCONTAINERBROWSE);
			addCaseInsensitiveKeyword("onmodalaction", TYPE_ONMODALACTION);
			addCaseInsensitiveKeyword("onunknownaction", TYPE_ONUNKNOWNACTION);
			addCaseInsensitiveKeyword("onambiguousaction", TYPE_ONAMBIGUOUSACTION);
			addCaseInsensitiveKeyword("onforbiddenaction", TYPE_ONFORBIDDENACTION);
			addCaseInsensitiveKeyword("onfailedaction", TYPE_ONFAILEDACTION);
			addCaseInsensitiveKeyword("afterrequest", TYPE_AFTERREQUEST);
			
			for (TAMECommand command : TAMECommand.values())
			{
				String name = command.name();
				if (command.isInternal())
					addCaseInsensitiveKeyword(name, TYPE_COMMAND_INTERNAL);
				else if (command.isEvaluating())
					addCaseInsensitiveKeyword(name, TYPE_COMMAND_CONTROL);
				else if (command.getReturnType() != null)
					addCaseInsensitiveKeyword(name, TYPE_COMMAND_EXPRESSION);
				else
					addCaseInsensitiveKeyword(name, TYPE_COMMAND_STATEMENT);
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
			// DEBUG ==================
			boolean out = parseAction();
			// ========================
			
			return out;
		}

		/**
		 * Parses an action clause (after "action").
		 * 		[GENERAL] [IDENTIFIER] [ActionNames] ";"
		 * 		[OPEN] [IDENTIFIER] [ActionNames] ";"
		 * 		[MODAL] [IDENTIFIER] [ActionNames] [ActionAdditionalNames] ";"
		 * 		[TRANSITIVE] [IDENTIFIER] [ActionNames] ";"
		 * 		[DITRANSITIVE] [IDENTIFIER] [ActionNames] [ActionAdditionalNames] ";"
		 */
		public boolean parseAction()
		{
			if (currentType(TSKernel.TYPE_GENERAL))
			{
				TAction.Type actionType = TAction.Type.GENERAL;
				nextToken();

				if (!isVariable())
				{
					addErrorMessage("Identity "+currentToken().getLexeme()+" is already declared.");
					return false;
				}

				TAction action = new TAction(currentToken().getLexeme());
				action.setType(actionType);

				if (!parseActionNames(action))
					return false;
				
				if (!matchType(TSKernel.TYPE_DELIM_SEMICOLON))
				{
					addErrorMessage("Expected end of action declaration \";\".");
					return false;
				}
				
				return true;
			}
			else if (currentType(TSKernel.TYPE_OPEN))
			{
				TAction.Type actionType = TAction.Type.OPEN;
				nextToken();

				if (!isVariable())
				{
					addErrorMessage("Identity "+currentToken().getLexeme()+" is already declared.");
					return false;
				}

				TAction action = new TAction(currentToken().getLexeme());
				action.setType(actionType);

				if (!parseActionNames(action))
					return false;
				
				if (!matchType(TSKernel.TYPE_DELIM_SEMICOLON))
				{
					addErrorMessage("Expected end of action declaration \";\".");
					return false;
				}
				
				return true;
			}
			else if (currentType(TSKernel.TYPE_MODAL))
			{
				TAction.Type actionType = TAction.Type.MODAL;
				nextToken();
				
				if (!isVariable())
				{
					addErrorMessage("Identity "+currentToken().getLexeme()+" is already declared.");
					return false;
				}

				TAction action = new TAction(currentToken().getLexeme());
				action.setType(actionType);

				if (!parseActionNames(action))
					return false;
				
				if (!parseActionAdditionalNames(action, TSKernel.TYPE_MODES))
					return false;
				
				if (!matchType(TSKernel.TYPE_DELIM_SEMICOLON))
				{
					addErrorMessage("Expected end of action declaration \";\".");
					return false;
				}

				return true;
			}
			else if (currentType(TSKernel.TYPE_TRANSITIVE))
			{
				TAction.Type actionType = TAction.Type.TRANSITIVE;
				nextToken();
				
				if (!isVariable())
				{
					addErrorMessage("Identity "+currentToken().getLexeme()+" is already declared.");
					return false;
				}

				TAction action = new TAction(currentToken().getLexeme());
				action.setType(actionType);

				if (!parseActionNames(action))
					return false;

				if (!matchType(TSKernel.TYPE_DELIM_SEMICOLON))
				{
					addErrorMessage("Expected end of action declaration \";\".");
					return false;
				}

				return true;
			}
			else if (currentType(TSKernel.TYPE_DITRANSITIVE))
			{
				TAction.Type actionType = TAction.Type.DITRANSITIVE;
				nextToken();
				
				if (!isVariable())
				{
					addErrorMessage("Identity "+currentToken().getLexeme()+" is already declared.");
					return false;
				}

				TAction action = new TAction(currentToken().getLexeme());
				action.setType(actionType);

				if (!parseActionNames(action))
					return false;

				if (!parseActionAdditionalNames(action, TSKernel.TYPE_CONJOINS))
					return false;
				
				if (!matchType(TSKernel.TYPE_DELIM_SEMICOLON))
				{
					addErrorMessage("Expected end of action declaration \";\".");
					return false;
				}

				return true;
			}
			else
			{
				addErrorMessage("Expected action type (general, open, modal, transitive, ditransitive).");
				return false;
			}
		}

		/**
		 * Parses action names.
		 * [ActionNames] := 
		 * 		[NAMED] [STRING] [ActionNameList]
		 * 		[e]
		 */
		private boolean parseActionNames(TAction action)
		{
			if (matchType(TSKernel.TYPE_NAMED))
			{
				if (!currentType(Lexer.TYPE_STRING))
				{
					addErrorMessage("Expected action name (must be string).");
					return false;
				}
				
				action.getNames().put(currentToken().getLexeme());
				nextToken();
				
				return parseActionNameList(action);
			}
			
			return true;
		}
		
		/**
		 * Parses action name list.
		 * [ActionNameList] :=
		 * 		"," [STRING] [ActionNameList]
		 * 		[e]
		 */
		private boolean parseActionNameList(TAction action)
		{
			if (matchType(TSKernel.TYPE_DELIM_COMMA))
			{
				if (!currentType(Lexer.TYPE_STRING))
				{
					addErrorMessage("Expected action name (must be string).");
					return false;
				}
				
				action.getNames().put(currentToken().getLexeme());
				nextToken();
				
				return parseActionNameList(action);
			}
			
			return true;
		}
		
		/**
		 * Parses addt'l action names.
		 * [ActionAdditionalNames] := 
		 * 		[CONJOINS] [STRING] [ActionAdditionalNameList]
		 * 		[e]
		 */
		private boolean parseActionAdditionalNames(TAction action, int expectedKeywordType)
		{
			if (matchType(expectedKeywordType))
			{
				if (!currentType(Lexer.TYPE_STRING))
				{
					addErrorMessage("Expected name (must be string).");
					return false;
				}
				
				action.getExtraStrings().put(currentToken().getLexeme());
				nextToken();
				
				return parseActionAdditionalNameList(action);
			}
			
			return true;
		}
		
		/**
		 * Parses action name list.
		 * [ActionAdditionalNameList] :=
		 * 		"," [STRING] [ActionAdditionalNameList]
		 * 		[e]
		 */
		private boolean parseActionAdditionalNameList(TAction action)
		{
			if (matchType(TSKernel.TYPE_DELIM_COMMA))
			{
				if (!currentType(Lexer.TYPE_STRING))
				{
					addErrorMessage("Expected name (must be string).");
					return false;
				}
				
				action.getExtraStrings().put(currentToken().getLexeme());
				nextToken();
				
				return parseActionAdditionalNameList(action);
			}
			
			return true;
		}
		

		/**
		 * Parses a block.
		 * [Block] :=
		 * 		"{" [StatementList] "}"
		 * 		[Statement]
		 */
		public boolean parseBlock()
		{
			if (currentType(TSKernel.TYPE_DELIM_LBRACE))
			{
				nextToken();
				
				if (!parseStatementList())
					return false;
				
				if (!matchType(TSKernel.TYPE_DELIM_RBRACE))
				{
					addErrorMessage("Expected end of block '}'.");
					return false;
				}

				return true;
			}
			
			return parseStatement();
		}
		
		/**
		 * Parses a statement. Emits commands to the current block.
		 * [Statement] := 
		 *		[ELEMENTID] "." [VARIABLE] [ASSIGNMENTOPERATOR] [EXPRESSION] ";"
		 * 		[VARIABLE] [ASSIGNMENTOPERATOR] [EXPRESSION] ";"
		 * 		[COMMANDEXPRESSION] ";"
		 * 		[COMMANDSTATEMENT] ";"
		 * 		[COMMANDBLOCK]
		 *		";"
		 */
		public boolean parseStatement()
		{
			if (currentType(Lexer.TYPE_IDENTIFIER))
			{
				Value identToken = tokenToValue();

				if (identToken.isElement())
				{
					// must have a dot if an element type.
					if (!matchType(TSKernel.TYPE_DELIM_DOT))
					{
						addErrorMessage("Statement error - expected '.' to dereference a variable.");
						return false;
					}
					
					if (!isVariable())
					{
						addErrorMessage("Statement error - expected variable.");
						return false;
					}
					
					Value variable = tokenToValue();
					nextToken();
					
					if (!matchType(TSKernel.TYPE_DELIM_EQUAL))
					{
						addErrorMessage("Statement error - expected assignment operator.");
						return false;
					}

					if (!parseExpression())
						return false;
					
					if (!matchType(TSKernel.TYPE_DELIM_SEMICOLON))
					{
						addErrorMessage("Statement error - expected \";\" at end of statement.");
						return false;
					}
					
					emit(Command.create(TAMECommand.POPELEMENTVALUE, identToken, variable));
				}
				else if (identToken.isVariable())
				{
					Value variable = tokenToValue();
					nextToken();
					
					if (!matchType(TSKernel.TYPE_DELIM_EQUAL))
					{
						addErrorMessage("Expression error - expected assignment operator.");
						return false;
					}

					if (!parseExpression())
						return false;
					
					if (!matchType(TSKernel.TYPE_DELIM_SEMICOLON))
					{
						addErrorMessage("Statement error - expected \";\" at end of statement.");
						return false;
					}
					
					emit(Command.create(TAMECommand.POPVALUE, variable));
				}
				else
				{
					addErrorMessage("Statement error - expected variable or element identifier.");
					return false;
				}
				
			}
			else if (currentType(TSKernel.TYPE_COMMAND_INTERNAL))
			{
				addErrorMessage("Statement error - command \""+currentToken().getLexeme()+"\" is an internal reserved command.");
				return false;
			}
			else if (currentType(TSKernel.TYPE_COMMAND_CONTROL))
			{
				TAMECommand commandType = tokenToCommand();
				nextToken();
				
				if (!parseControlCommand(commandType))
					return false;
				
				if (!matchType(TSKernel.TYPE_DELIM_SEMICOLON))
				{
					addErrorMessage("Statement error - expected \";\" at end of statement.");
					return false;
				}
			}
			else if (currentType(TSKernel.TYPE_COMMAND_STATEMENT))
			{
				TAMECommand commandType = tokenToCommand();
				nextToken();
				
				if (!parseCommandCall(commandType))
					return false;

				if (!matchType(TSKernel.TYPE_DELIM_SEMICOLON))
				{
					addErrorMessage("Statement error - expected \";\" at end of statement.");
					return false;
				}
			}
			else if (currentType(TSKernel.TYPE_COMMAND_EXPRESSION))
			{
				TAMECommand commandType = tokenToCommand();
				nextToken();
				
				if (!parseCommandCall(commandType))
					return false;
				
				emit(Command.create(TAMECommand.POP));

				if (!matchType(TSKernel.TYPE_DELIM_SEMICOLON))
				{
					addErrorMessage("Statement error - expected \";\" at end of statement.");
					return false;
				}
			}
			else if (matchType(TSKernel.TYPE_DELIM_SEMICOLON))
			{
				return true;
			}

			return true;
		}
		
		/**
		 * Parses a statement. Emits commands to the current block.
		 * [StatementList] := 
		 *		[Statement] [StatementList]
		 * 		[e]
		 */
		public boolean parseStatementList()
		{
			if (!currentType(Lexer.TYPE_IDENTIFIER, TSKernel.TYPE_COMMAND_INTERNAL, TSKernel.TYPE_COMMAND_STATEMENT, TSKernel.TYPE_COMMAND_CONTROL, TSKernel.TYPE_COMMAND_EXPRESSION, TSKernel.TYPE_DELIM_SEMICOLON))
				return true;
			
			if (!parseStatement())
				return false;
			
			return parseStatementList();
		}

		/**
		 * Parses an infix expression.
		 */
		public boolean parseExpression()
		{
			// make stacks.
			Stack<ArithmeticOperator> expressionOperators = new Stack<>();
			int[] expressionValueCounter = new int[1];
			
			// was the last read token a value?
			boolean lastWasValue = false;
			boolean keepGoing = true;
			
			while (keepGoing)
			{
				if (currentType(TSKernel.TYPE_COMMAND_EXPRESSION))
				{
					TAMECommand commandType = tokenToCommand();
					nextToken();
					
					if (!parseCommandCall(commandType))
						return false;
					
					expressionValueCounter[0] += 1;
					lastWasValue = true;
				}
				else if (currentType(TSKernel.TYPE_COMMAND_STATEMENT))
				{
					addErrorMessage("Expression error - command \""+currentToken().getLexeme()+"\" has no return.");
					return false;
				}
				else if (currentType(TSKernel.TYPE_COMMAND_INTERNAL))
				{
					addErrorMessage("Expression error - command \""+currentToken().getLexeme()+"\" is an internal reserved command.");
					return false;
				}
				else if (currentType(Lexer.TYPE_IDENTIFIER))
				{
					if (lastWasValue)
					{
						addErrorMessage("Expression error - expected operator.");
						return false;
					}
					
					Value identToken = tokenToValue();

					if (identToken.isElement())
					{
						// must have a dot if an element type.
						if (!matchType(TSKernel.TYPE_DELIM_DOT))
						{
							addErrorMessage("Expression error - expected '.' to dereference a variable.");
							return false;
						}
						
						if (!isVariable())
						{
							addErrorMessage("Expression error - expected variable.");
							return false;
						}
						
						emit(Command.create(TAMECommand.PUSHELEMENTVALUE, identToken, Value.createVariable(currentToken().getLexeme())));
					}
					else if (identToken.isVariable())
					{
						emit(Command.create(TAMECommand.PUSHVALUE, identToken));
					}
					else
					{
						addErrorMessage("Expression error - expected variable or element identifier.");
						return false;
					}
					
					nextToken();
					
					expressionValueCounter[0] += 1;
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

					expressionValueCounter[0] += 1;
					lastWasValue = true;
				}
				else if (isValidLiteralType())
				{
					if (lastWasValue)
					{
						addErrorMessage("Expression error - expected operator.");
						return false;
					}
					
					Value value = tokenToValue();
					emit(Command.create(TAMECommand.PUSHVALUE, value));
					expressionValueCounter[0] += 1;
					nextToken();
					lastWasValue = true;
				}
				else if (lastWasValue)
				{
					if (isBinaryOperatorType())
					{
						ArithmeticOperator nextOperator = null;
						
						switch (currentToken().getType())
						{
							case TSKernel.TYPE_DELIM_PLUS:
								nextOperator = ArithmeticOperator.ADD;
								break;
							case TSKernel.TYPE_DELIM_MINUS:
								nextOperator = ArithmeticOperator.SUBTRACT;
								break;
							case TSKernel.TYPE_DELIM_STAR:
								nextOperator = ArithmeticOperator.MULTIPLY;
								break;
							case TSKernel.TYPE_DELIM_SLASH:
								nextOperator = ArithmeticOperator.DIVIDE;
								break;
							case TSKernel.TYPE_DELIM_PERCENT:
								nextOperator = ArithmeticOperator.MODULO;
								break;
							case TSKernel.TYPE_DELIM_AMPERSAND:
								nextOperator = ArithmeticOperator.AND;
								break;
							case TSKernel.TYPE_DELIM_AMPERSAND2:
								nextOperator = ArithmeticOperator.LOGICAL_AND;
								break;
							case TSKernel.TYPE_DELIM_PIPE:
								nextOperator = ArithmeticOperator.OR;
								break;
							case TSKernel.TYPE_DELIM_PIPE2:
								nextOperator = ArithmeticOperator.LOGICAL_OR;
								break;
							case TSKernel.TYPE_DELIM_CARAT:
								nextOperator = ArithmeticOperator.XOR;
								break;
							case TSKernel.TYPE_DELIM_CARAT2:
								nextOperator = ArithmeticOperator.LOGICAL_XOR;
								break;
							case TSKernel.TYPE_DELIM_LESS:
								nextOperator = ArithmeticOperator.LESS;
								break;
							case TSKernel.TYPE_DELIM_LESS2:
								nextOperator = ArithmeticOperator.LSHIFT;
								break;
							case TSKernel.TYPE_DELIM_LESSEQUAL:
								nextOperator = ArithmeticOperator.LESS_OR_EQUAL;
								break;
							case TSKernel.TYPE_DELIM_GREATER:
								nextOperator = ArithmeticOperator.GREATER;
								break;
							case TSKernel.TYPE_DELIM_GREATER2:
								nextOperator = ArithmeticOperator.RSHIFT;
								break;
							case TSKernel.TYPE_DELIM_GREATER3:
								nextOperator = ArithmeticOperator.RSHIFTPAD;
								break;
							case TSKernel.TYPE_DELIM_GREATEREQUAL:
								nextOperator = ArithmeticOperator.GREATER_OR_EQUAL;
								break;
							case TSKernel.TYPE_DELIM_EQUAL2:
								nextOperator = ArithmeticOperator.EQUALS;
								break;
							case TSKernel.TYPE_DELIM_EQUAL3:
								nextOperator = ArithmeticOperator.STRICT_EQUALS;
								break;
							case TSKernel.TYPE_DELIM_NOTEQUAL:
								nextOperator = ArithmeticOperator.NOT_EQUALS;
								break;
							case TSKernel.TYPE_DELIM_NOTEQUALEQUAL:
								nextOperator = ArithmeticOperator.STRICT_NOT_EQUALS;
								break;
							default:
								throw new TAMEScriptParseException("Internal error - unexpected binary operator miss.");
						}
						
						nextToken();

						if (!operatorReduce(expressionOperators, expressionValueCounter, nextOperator))
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
							expressionOperators.push(ArithmeticOperator.NEGATE);
							break;
						case TSKernel.TYPE_DELIM_PLUS:
							expressionOperators.push(ArithmeticOperator.ABSOLUTE);
							break;
						case TSKernel.TYPE_DELIM_TILDE:
							expressionOperators.push(ArithmeticOperator.NOT);
							break;
						case TSKernel.TYPE_DELIM_EXCLAMATION:
							expressionOperators.push(ArithmeticOperator.LOGICAL_NOT);
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
				if (!expressionReduce(expressionOperators, expressionValueCounter))
					return false;
			}
			
			if (expressionValueCounter[0] != 1)
			{
				addErrorMessage("Expected valid expression.");
				return false;
			}

			return true;
		}

		/**
		 * Parses the argument section of a command.
		 * @param commandType 
		 * @return
		 */
		public boolean parseCommandCall(TAMECommand commandType) 
		{
			if (commandType.isEvaluating())
			{
				if (!parseControlCommand(commandType))
					return false;
				
				return true;
			}
			else
			{
				// clause - no argument list at all.
				if (commandType.getArgumentTypes() == null)
				{
					emit(Command.create(commandType));
					return true;
				}

				if (!matchType(TSKernel.TYPE_DELIM_LPAREN))
				{
					addErrorMessage("Expression error - expected '(' after command \""+commandType.name()+".\"");
					return false;
				}
				
				if (!parseCommandArguments(commandType))
					return false;
				
				if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
				{
					addErrorMessage("Expression error - expected ')' after command arguments.");
					return false;
				}
				
				emit(Command.create(commandType));
				return true;
			}
		}

		/**
		 * Parses command arguments.
		 */
		private boolean parseControlCommand(TAMECommand commandType) 
		{
			Block conditionalBlock = null;
			Block successBlock = null;
			Block failureBlock = null;
			
			if (commandType.isConditionalBlockRequired())
			{
				if (!matchType(TSKernel.TYPE_DELIM_LPAREN))
				{
					addErrorMessage("Statement error - expected '(' after command \""+commandType.name()+".\"");
					return false;
				}

				// conditional block.
				if (commandType.isConditionalBlockRequired())
				{
					currentBlock.push(conditionalBlock = new Block());
					
					if (!parseExpression())
						return false;
					
					currentBlock.pop();
				}
				
				if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
				{
					addErrorMessage("Statement error - expected ')' after expression.");
					return false;
				}
				
				// success block.
				if (commandType.isSuccessBlockRequired())
				{
					currentBlock.push(successBlock = new Block());
					
					if (!parseStatementList())
						return false;
					
					currentBlock.pop();
				}
				
				// failure block.
				if (commandType.isFailureBlockPossible())
				{
					if (matchType(TSKernel.TYPE_ELSE))
					{
						currentBlock.push(failureBlock = new Block());
						
						if (!parseExpression())
							return false;
						
						currentBlock.pop();
					}
					
				}
				
			}
			
			emit(Command.create(commandType, conditionalBlock, successBlock, failureBlock));
			return true;
		}
		
		/**
		 * Parses command arguments.
		 */
		private boolean parseCommandArguments(TAMECommand commandType) 
		{
			ArgumentType[] argTypes = commandType.getArgumentTypes();
			for (int i = 0; i < argTypes.length; i++) 
			{
				switch (argTypes[i])
				{
					default:
					case VALUE:
					{
						// value - read expression.
						if (!parseExpression())
							return false;
						break;
					}
					case ACTION:
					{
						if (!isAction())
						{
							addErrorMessage("Command requires an ACTION. \""+currentToken().getLexeme()+"\" is not an action type.");
							return false;
						}
						
						emit(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
						nextToken();
						break;
					}
					case OBJECT:
					{
						if (!isObject())
						{
							addErrorMessage("Command requires an OBJECT. \""+currentToken().getLexeme()+"\" is not an object type.");
							return false;
						}
						
						emit(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
						nextToken();
						break;
					}
					case PLAYER:
					{
						if (!isPlayer())
						{
							addErrorMessage("Command requires an PLAYER. \""+currentToken().getLexeme()+"\" is not an player type.");
							return false;
						}
						
						emit(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
						nextToken();
						break;
					}
					case ROOM:
					{
						if (!isRoom())
						{
							addErrorMessage("Command requires an ROOM. \""+currentToken().getLexeme()+"\" is not an room type.");
							return false;
						}
						
						emit(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
						nextToken();
						break;
					}
					case CONTAINER:
					{
						if (!isContainer())
						{
							addErrorMessage("Command requires an CONTAINER. \""+currentToken().getLexeme()+"\" is not an container type.");
							return false;
						}
						
						emit(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
						nextToken();
						break;
					}
					case ELEMENT:
					{
						if (!isElement())
						{
							addErrorMessage("Command requires an ELEMENT. \""+currentToken().getLexeme()+"\" is not an element type.");
							return false;
						}
						
						emit(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
						nextToken();
						break;
					}
					
				} // switch
				
				if (i < argTypes.length - 1)
				{
					if (!matchType(TSKernel.TYPE_DELIM_COMMA))
					{
						addErrorMessage("Expected ',' after command argument. More arguments remain.");
						return false;
					}
				}
				
			} // for
			
			return true;
		}
		
		// Operator reduce.
		private boolean operatorReduce(Stack<ArithmeticOperator> expressionOperators, int[] expressionValueCounter, ArithmeticOperator operator) 
		{
			ArithmeticOperator top = expressionOperators.peek();
			while (top != null && (top.getPrecedence() > operator.getPrecedence() || (top.getPrecedence() == operator.getPrecedence() && !operator.isRightAssociative())))
			{
				if (!expressionReduce(expressionOperators, expressionValueCounter))
					return false;
				top = expressionOperators.peek();
			}
			
			return true;
		}

		// Expression reduce.
		private boolean expressionReduce(Stack<ArithmeticOperator> expressionOperators, int[] expressionValueCounter)
		{
			if (expressionOperators.isEmpty())
				throw new TAMEScriptParseException("Internal error - operator stack must have one operator in it.");

			ArithmeticOperator operator = expressionOperators.pop();
			
			// TODO: Operand checking for operators that are type strict, or relax type rules.
			
			if (operator.isBinary())
				expressionValueCounter[0] -= 2;
			else
				expressionValueCounter[0] -= 1;
			
			if (expressionValueCounter[0] < 0)
				throw new TAMEScriptParseException("Internal error - value counter did not have enough counter.");
			
			expressionValueCounter[0] += 1; // the "push"
			emit(Command.create(TAMECommand.ARITHMETICFUNC, Value.create(operator.ordinal())));
			return true;
		}

		// Token to value.
		private Value tokenToValue()
		{
			if (isWorld())
				return Value.createWorld();
			else if (isPlayer())
				return Value.createPlayer(currentToken().getLexeme());
			else if (isRoom())
				return Value.createRoom(currentToken().getLexeme());
			else if (isObject())
				return Value.createObject(currentToken().getLexeme());
			else if (isContainer())
				return Value.createContainer(currentToken().getLexeme());
			else if (isAction())
				return Value.createAction(currentToken().getLexeme());
			else if (currentType(Lexer.TYPE_STRING))
				return Value.create(currentToken().getLexeme());
			else if (currentType(Lexer.TYPE_NUMBER))
			{
				String lexeme = currentToken().getLexeme();
				if (lexeme.startsWith("0X") || lexeme.startsWith("0x"))
					return Value.create(Long.parseLong(lexeme.substring(2), 16));
				else if (lexeme.contains("."))
					return Value.create(Double.parseDouble(lexeme));
				else
					return Value.create(Long.parseLong(lexeme));
			}
			else if (currentType(Lexer.TYPE_IDENTIFIER))
				return Value.createVariable(currentToken().getLexeme());
			else if (currentType(TSKernel.TYPE_TRUE))
				return Value.create(true);
			else if (currentType(TSKernel.TYPE_FALSE))
				return Value.create(false);
			else
				throw new TAMEScriptParseException("Internal error - unexpected token type.");
		}

		// Token to command.
		private TAMECommand tokenToCommand()
		{
			return TAMECommand.valueOf(currentToken().getLexeme().toUpperCase());
		}
		
		// Checks if an identifier is a variable.
		private boolean isVariable()
		{
			return !isWorld()
				&& !isPlayer()
				&& !isRoom()
				&& !isObject()
				&& !isContainer()
				&& !isAction()
			;
		}
		
		// Checks if an identifier is an element.
		private boolean isElement()
		{
			return isWorld()
				|| isPlayer()
				|| isRoom()
				|| isObject()
				|| isContainer()
			;
		}
		
		// Checks if an identifier is an action.
		private boolean isAction()
		{
			return currentModule.getActionByIdentity(currentToken().getLexeme()) != null;
		}
		
		// Checks if an identifier is a world.
		private boolean isWorld()
		{
			return currentToken().getLexeme().equals(IDENTITY_CURRENT_WORLD);
		}
		
		// Checks if an identifier is a player.
		private boolean isPlayer()
		{
			String identifier = currentToken().getLexeme();
			return identifier.equals(IDENTITY_CURRENT_PLAYER)
				|| currentModule.getPlayerByIdentity(identifier) != null;
		}
		
		// Checks if an identifier is a room.
		private boolean isRoom()
		{
			String identifier = currentToken().getLexeme();
			return identifier.equals(IDENTITY_CURRENT_ROOM)
				|| currentModule.getRoomByIdentity(identifier) != null;
		}
		
		// Checks if an identifier is an object.
		private boolean isObject()
		{
			return currentModule.getObjectByIdentity(currentToken().getLexeme()) != null;
		}
		
		// Checks if an identifier is a container.
		private boolean isContainer()
		{
			return currentModule.getContainerByIdentity(currentToken().getLexeme()) != null;
		}
		
		// Return true if token type is a valid block on a world.
		private boolean isWorldBlockType()
		{
			switch (currentToken().getType())
			{
				case TSKernel.TYPE_INIT:
				case TSKernel.TYPE_ONACTION:
				case TSKernel.TYPE_ONMODALACTION:
				case TSKernel.TYPE_ONFAILEDACTION:
				case TSKernel.TYPE_ONUNKNOWNACTION:
				case TSKernel.TYPE_ONAMBIGUOUSACTION:
				case TSKernel.TYPE_AFTERREQUEST:
					return true;
				default:
					return false;
			}
		}
		
		// Return true if token type is a valid block on a player.
		private boolean isPlayerBlockType()
		{
			switch (currentToken().getType())
			{
				case TSKernel.TYPE_INIT:
				case TSKernel.TYPE_ONACTION:
				case TSKernel.TYPE_ONMODALACTION:
				case TSKernel.TYPE_ONFAILEDACTION:
				case TSKernel.TYPE_ONUNKNOWNACTION:
				case TSKernel.TYPE_ONAMBIGUOUSACTION:
				case TSKernel.TYPE_ONFORBIDDENACTION:
					return true;
				default:
					return false;
			}
		}
		
		// Return true if token type is a valid block on a player.
		private boolean isRoomBlockType()
		{
			switch (currentToken().getType())
			{
				case TSKernel.TYPE_INIT:
				case TSKernel.TYPE_ONACTION:
				case TSKernel.TYPE_ONMODALACTION:
				case TSKernel.TYPE_ONFAILEDACTION:
				case TSKernel.TYPE_ONFORBIDDENACTION:
					return true;
				default:
					return false;
			}
		}
		
		// Return true if token type is a valid block on an object.
		private boolean isObjectBlockType()
		{
			switch (currentToken().getType())
			{
				case TSKernel.TYPE_INIT:
				case TSKernel.TYPE_ONACTION:
				case TSKernel.TYPE_ONACTIONWITH:
				case TSKernel.TYPE_ONACTIONWITHOTHER:
				case TSKernel.TYPE_ONPLAYERBROWSE:
				case TSKernel.TYPE_ONROOMBROWSE:
				case TSKernel.TYPE_ONCONTAINERBROWSE:
					return true;
				default:
					return false;
			}
		}
		
		// Return true if token type is a valid block on a container.
		private boolean isContainerBlockType()
		{
			switch (currentToken().getType())
			{
				case TSKernel.TYPE_INIT:
					return true;
				default:
					return false;
			}
		}
		
		// Return true if token type can be a unary operator.
		private boolean isValidLiteralType()
		{
			if (currentToken() == null)
				return false;
			
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
			if (currentToken() == null)
				return false;
			
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
			if (currentToken() == null)
				return false;
			
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
		
		// Attempts to reduce redundant calls and unnecessary ones.
		private void optimizeBlock(Block block)
		{
			// TODO: Finish.
		}
		
		/**
		 * Emits a command into the current block.
		 */
		private void emit(Command command)
		{
			currentBlock.peek().add(command);
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
