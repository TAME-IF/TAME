package net.mtrop.tame.factory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;

import com.blackrook.commons.Common;
import com.blackrook.commons.linkedlist.Stack;
import com.blackrook.lang.CommonLexer;
import com.blackrook.lang.CommonLexerKernel;
import com.blackrook.lang.Lexer;
import com.blackrook.lang.Parser;

import net.mtrop.tame.TAMECommand;
import net.mtrop.tame.TAMEConstants;
import net.mtrop.tame.TAMEModule;
import net.mtrop.tame.element.ActionAmbiguousHandler;
import net.mtrop.tame.element.ActionFailedHandler;
import net.mtrop.tame.element.ActionForbiddenHandler;
import net.mtrop.tame.element.ActionModalHandler;
import net.mtrop.tame.element.ActionUnknownHandler;
import net.mtrop.tame.element.TAction;
import net.mtrop.tame.element.TAction.Type;
import net.mtrop.tame.element.TActionableElement;
import net.mtrop.tame.element.TContainer;
import net.mtrop.tame.element.TElement;
import net.mtrop.tame.element.TObject;
import net.mtrop.tame.element.TPlayer;
import net.mtrop.tame.element.TRoom;
import net.mtrop.tame.element.TWorld;
import net.mtrop.tame.lang.ArgumentType;
import net.mtrop.tame.lang.ArithmeticOperator;
import net.mtrop.tame.lang.Block;
import net.mtrop.tame.lang.Command;
import net.mtrop.tame.lang.PermissionType;
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
	/** The default options. */
	private static final DefaultReaderOptions DEFAULT_OPTIONS = new DefaultReaderOptions();
	
	/** The Lexer Kernel for the ArcheText Lexers. */
	private static class TSKernel extends CommonLexerKernel
	{
		static final int TYPE_COMMENT = 				0;
		static final int TYPE_FALSE = 					1;
		static final int TYPE_TRUE = 					2;

		static final int TYPE_WHILE =					3;
		static final int TYPE_FOR =						4;
		static final int TYPE_IF =						5;
		static final int TYPE_ELSE =					6;
		static final int TYPE_COMMAND_INTERNAL = 		7;
		static final int TYPE_COMMAND_STATEMENT = 		8;
		static final int TYPE_COMMAND_EXPRESSION = 		9;

		static final int TYPE_DELIM_LPAREN =			20;
		static final int TYPE_DELIM_RPAREN =			21;
		static final int TYPE_DELIM_LBRACE = 			22;
		static final int TYPE_DELIM_RBRACE = 			23;
		static final int TYPE_DELIM_LBRACK = 			24;
		static final int TYPE_DELIM_RBRACK = 			25;
		static final int TYPE_DELIM_COLON = 			26;
		static final int TYPE_DELIM_SEMICOLON = 		27;
		static final int TYPE_DELIM_COMMA = 			28;
		static final int TYPE_DELIM_DOT = 				29;
		static final int TYPE_DELIM_PLUS =				30;
		static final int TYPE_DELIM_MINUS =				41;
		static final int TYPE_DELIM_TILDE =				42;
		static final int TYPE_DELIM_EXCLAMATION =		43;
		static final int TYPE_DELIM_STAR = 				44;
		static final int TYPE_DELIM_SLASH = 			45;
		static final int TYPE_DELIM_PERCENT = 			46;
		static final int TYPE_DELIM_AMPERSAND = 		47;
		static final int TYPE_DELIM_AMPERSAND2 = 		48;
		static final int TYPE_DELIM_PIPE = 				49;
		static final int TYPE_DELIM_PIPE2 = 			50;
		static final int TYPE_DELIM_CARAT = 			51;
		static final int TYPE_DELIM_CARAT2 = 			52;
		static final int TYPE_DELIM_LESS = 				53;
		static final int TYPE_DELIM_LESS2 = 			54;
		static final int TYPE_DELIM_LESSEQUAL = 		55;
		static final int TYPE_DELIM_GREATER =			56;
		static final int TYPE_DELIM_GREATER2 = 			57;
		static final int TYPE_DELIM_GREATER3 = 			58;
		static final int TYPE_DELIM_GREATEREQUAL = 		59;
		static final int TYPE_DELIM_EQUAL = 			60;
		static final int TYPE_DELIM_EQUAL2 = 			61;
		static final int TYPE_DELIM_EQUAL3 = 			62;
		static final int TYPE_DELIM_NOTEQUAL = 			63;
		static final int TYPE_DELIM_NOTEQUALEQUAL =		64;

		static final int TYPE_MODULE = 					70;
		static final int TYPE_WORLD = 					71;
		static final int TYPE_ROOM = 					72;
		static final int TYPE_PLAYER = 					73;
		static final int TYPE_OBJECT = 					74;
		static final int TYPE_CONTAINER =				75;
		static final int TYPE_ACTION = 					76;
		static final int TYPE_GENERAL = 				77;
		static final int TYPE_MODAL = 					78;
		static final int TYPE_TRANSITIVE = 				79;
		static final int TYPE_DITRANSITIVE = 			80;
		static final int TYPE_OPEN = 					81;
		static final int TYPE_NAMED = 					82;
		static final int TYPE_MODES = 					83;
		static final int TYPE_CONJOINS = 				84;
		static final int TYPE_EXCLUDES = 				85;
		static final int TYPE_RESTRICTS = 				86;

		static final int TYPE_INIT = 					90;
		static final int TYPE_ONACTION =				91;
		static final int TYPE_ONACTIONWITH =			92;
		static final int TYPE_ONACTIONWITHOTHER =		93;
		static final int TYPE_ONROOMBROWSE =			94;
		static final int TYPE_ONPLAYERBROWSE =			95;
		static final int TYPE_ONCONTAINERBROWSE =		96;
		static final int TYPE_ONMODALACTION =			97;
		static final int TYPE_ONUNKNOWNACTION =			98;
		static final int TYPE_ONAMBIGUOUSACTION =		99;
		static final int TYPE_ONFORBIDDENACTION =		100;
		static final int TYPE_ONFAILEDACTION =			101;
		static final int TYPE_AFTERREQUEST =			102;
		
		private TSKernel()
		{
			addStringDelimiter('"', '"');
			
			addCommentStartDelimiter("/*", TYPE_COMMENT);
			addCommentLineDelimiter("//", TYPE_COMMENT);
			addCommentEndDelimiter("*/", TYPE_COMMENT);

			addCaseInsensitiveKeyword("true", TYPE_TRUE);
			addCaseInsensitiveKeyword("false", TYPE_FALSE);
			addCaseInsensitiveKeyword("else", TYPE_ELSE);
			addCaseInsensitiveKeyword("if", TYPE_IF);
			addCaseInsensitiveKeyword("while", TYPE_WHILE);
			addCaseInsensitiveKeyword("for", TYPE_FOR);

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
				{
					if(!command.isBlock())
						addCaseInsensitiveKeyword(name, TYPE_COMMAND_INTERNAL);
				}
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
		/** The parser options. */
		private TAMEScriptReaderOptions options;

		/** Current root. */
		private TAMEModule currentModule;
		/** Current block. */
		private Stack<Block> currentBlock;
		
		private TSParser(TSLexer lexer, TAMEScriptReaderOptions options)
		{
			super(lexer);
			for (String def : options.getDefines())
				lexer.addDefineMacro(def);
			this.options = options;
			this.currentModule = null;
			this.currentBlock = new Stack<Block>();
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

		/**
		 * Parses a module element.
		 */
		private boolean parseModuleElement()
		{
			if (matchType(TSKernel.TYPE_MODULE))
			{
				if (!parseModuleAttributes())
					return false;
				
				return true;
			}
			
			if (matchType(TSKernel.TYPE_ACTION))
			{
				if (!parseAction())
					return false;
				
				if (!matchType(TSKernel.TYPE_DELIM_SEMICOLON))
				{
					addErrorMessage("Expected end of action declaration \";\".");
					return false;
				}
				
				return true;
			}

			if (matchType(TSKernel.TYPE_WORLD))
			{
				if (!parseWorld())
					return false;
				
				return true;
			}
			
			if (matchType(TSKernel.TYPE_PLAYER))
			{
				if (!parsePlayer())
					return false;
				
				return true;
			}

			if (matchType(TSKernel.TYPE_ROOM))
			{
				if (!parseRoom())
					return false;
				
				return true;
			}

			if (matchType(TSKernel.TYPE_OBJECT))
			{
				if (!parseObject())
					return false;
				
				return true;
			}

			if (matchType(TSKernel.TYPE_CONTAINER))
			{
				if (!parseContainer())
					return false;
				
				return true;
			}

			addErrorMessage("Expected module element declaration start (module, world, player, room, object, container, action).");
			return false;
		}

		/**
		 * [ModuleAttribute] :=
		 * 		"{" [ModuleAttributeList] "}"
		 */
		private boolean parseModuleAttributes()
		{
			if (!matchType(TSKernel.TYPE_DELIM_LBRACE))
			{
				addErrorMessage("Expected \"{\" to start module attributes.");
				return false;
			}

			if (!parseModuleAttributeList())
				return false;
			
			if (!matchType(TSKernel.TYPE_DELIM_RBRACE))
			{
				addErrorMessage("Expected \"}\" to end module attributes.");
				return false;
			}
			
			return true;
		}
		
		/**
		 * [ModuleAttribute] :=
		 * 		"{" [ModuleAttributeList] "}"
		 */
		private boolean parseModuleAttributeList()
		{
			if (currentType(Lexer.TYPE_IDENTIFIER))
			{
				String attribute = currentToken().getLexeme();
				nextToken();

				if (currentType(Lexer.TYPE_STRING | Lexer.TYPE_NUMBER))
				{
					addErrorMessage("Expected literal value.");
					return false;
				}

				String value = currentToken().getLexeme();
				nextToken();

				currentModule.getHeader().addAttribute(attribute, value);
				
				if (!matchType(TSKernel.TYPE_DELIM_SEMICOLON))
				{
					addErrorMessage("Expected \";\" to end the attribute.");
					return false;
				}
				
				return parseModuleAttributeList();
			}
			
			return true;
		}
		
		/**
		 * Parses a container.
		 * [Container] :=
		 * 		[IDENTIFIER] ";"
		 * 		[IDENTIFIER] "{" [ObjectBody] "}"
		 */
		public boolean parseContainer()
		{
			// container identity.
			if (!currentType(Lexer.TYPE_IDENTIFIER))
			{
				addErrorMessage("Expected container identity.");
				return false;
			}

			String identity = currentToken().getLexeme();
			nextToken();
			
			// prototype?
			if (matchType(TSKernel.TYPE_DELIM_SEMICOLON))
			{
				TContainer element = new TContainer(identity);
				currentModule.addContainer(element);
				return true;
			}

			TContainer container;
			if ((container = currentModule.getContainerByIdentity(identity)) == null)
			{
				container = new TContainer(identity);
				currentModule.addContainer(container);
			}
									
			if (!matchType(TSKernel.TYPE_DELIM_LBRACE))
			{
				addErrorMessage("Expected \"{\" for container body start or \";\" (prototyping).");
				return false;
			}

			if (!parseContainerBody(container))
				return false;
			
			if (!matchType(TSKernel.TYPE_DELIM_RBRACE))
			{
				addErrorMessage("Expected end-of-object \"}\".");
				return false;
			}

			return true;
		}
		
		/**
		 * Parses the container body.
		 * [ContainerBody] :=
		 * 		[ContainerBlock] [ContainerBody]
		 * 		[e]
		 */
		private boolean parseContainerBody(TContainer container)
		{
			while (isContainerBlockType())
			{
				if (currentType(TSKernel.TYPE_INIT))
				{
					nextToken();
					
					if (!parseInitBlock(container))
						return false;
					
					continue;
				}
								
				break;
			}
			
			return true;
		}
				
		/**
		 * Parses an object.
		 * [Object] :=
		 * 		[IDENTIFIER] ";"
		 * 		[IDENTIFIER] "{" [ObjectBody] "}"
		 */
		public boolean parseObject()
		{
			// object identity.
			if (!currentType(Lexer.TYPE_IDENTIFIER))
			{
				addErrorMessage("Expected object identity.");
				return false;
			}

			String identity = currentToken().getLexeme();
			nextToken();
			
			// prototype?
			if (matchType(TSKernel.TYPE_DELIM_SEMICOLON))
			{
				TObject element = new TObject(identity);
				currentModule.addObject(element);
				return true;
			}

			TObject object;
			if ((object = currentModule.getObjectByIdentity(identity)) == null)
			{
				object = new TObject(identity);
				currentModule.addObject(object);
			}
									
			if (!parseObjectNames(object))
				return false;
			
			if (!matchType(TSKernel.TYPE_DELIM_LBRACE))
			{
				addErrorMessage("Expected \"{\" for object body start or \";\" (prototyping).");
				return false;
			}

			if (!parseObjectBody(object))
				return false;
			
			if (!matchType(TSKernel.TYPE_DELIM_RBRACE))
			{
				addErrorMessage("Expected end-of-object \"}\".");
				return false;
			}

			return true;
		}
		
		/**
		 * Parses the object body.
		 * [ObjectBody] :=
		 * 		[ObjectBlock] [ObjectBody]
		 * 		[e]
		 */
		private boolean parseObjectBody(TObject object)
		{
			while (isObjectBlockType())
			{
				if (currentType(TSKernel.TYPE_INIT))
				{
					nextToken();
					
					if (!parseInitBlock(object))
						return false;
					
					continue;
				}
								
				if (currentType(TSKernel.TYPE_ONACTION))
				{
					nextToken();
					
					if (!parseOnActionBlock(object, Type.TRANSITIVE, Type.DITRANSITIVE))
						return false;
					
					continue;
				}
				
				if (currentType(TSKernel.TYPE_ONACTIONWITH))
				{
					nextToken();
					
					if (!parseOnActionWithBlock(object))
						return false;
					
					continue;
				}
				
				if (currentType(TSKernel.TYPE_ONACTIONWITHOTHER))
				{
					nextToken();
					
					if (!parseOnActionWithOtherBlock(object))
						return false;
					
					continue;
				}
				
				if (currentType(TSKernel.TYPE_ONPLAYERBROWSE))
				{
					nextToken();
					
					if (!parsePlayerBrowseBlock(object))
						return false;
					
					continue;
				}
				
				if (currentType(TSKernel.TYPE_ONROOMBROWSE))
				{
					nextToken();
					
					if (!parseRoomBrowseBlock(object))
						return false;
					
					continue;
				}
				
				if (currentType(TSKernel.TYPE_ONCONTAINERBROWSE))
				{
					nextToken();
					
					if (!parseContainerBrowseBlock(object))
						return false;
					
					continue;
				}
				
				break;
			}
			
			return true;
		}
				
		/**
		 * Parses a room.
		 * [Room] :=
		 * 		[IDENTIFIER] ";"
		 * 		[IDENTIFIER] "{" [RoomBody] "}"
		 */
		public boolean parseRoom()
		{
			// room identity.
			if (!currentType(Lexer.TYPE_IDENTIFIER))
			{
				addErrorMessage("Expected room identity.");
				return false;
			}

			String identity = currentToken().getLexeme();
			nextToken();
			
			// prototype?
			if (matchType(TSKernel.TYPE_DELIM_SEMICOLON))
			{
				TRoom element = new TRoom(identity);
				currentModule.addRoom(element);
				return true;
			}

			TRoom room;
			if ((room = currentModule.getRoomByIdentity(identity)) == null)
			{
				room = new TRoom(identity);
				currentModule.addRoom(room);
			}
						
			if (!parseActionPermissionClause(room))
				return false;
			
			if (!matchType(TSKernel.TYPE_DELIM_LBRACE))
			{
				addErrorMessage("Expected \"{\" for room body start or \";\" (prototyping).");
				return false;
			}

			if (!parseRoomBody(room))
				return false;
			
			if (!matchType(TSKernel.TYPE_DELIM_RBRACE))
			{
				addErrorMessage("Expected end-of-room \"}\".");
				return false;
			}

			return true;
		}
		
		/**
		 * Parses the room body.
		 * [RoomBody] :=
		 * 		[RoomBlock] [RoomBody]
		 * 		[e]
		 */
		private boolean parseRoomBody(TRoom room)
		{
			while (isRoomBlockType())
			{
				if (currentType(TSKernel.TYPE_INIT))
				{
					nextToken();
					
					if (!parseInitBlock(room))
						return false;
					
					continue;
				}
								
				if (currentType(TSKernel.TYPE_ONACTION))
				{
					nextToken();
					
					if (!parseOnActionBlock(room, Type.GENERAL, Type.OPEN))
						return false;
					
					continue;
				}
					
				if (currentType(TSKernel.TYPE_ONMODALACTION))
				{
					nextToken();
					
					if (!parseOnModalActionBlock(room))
						return false;
					
					continue;
				}
					
				if (currentType(TSKernel.TYPE_ONFORBIDDENACTION))
				{
					nextToken();
					
					if (!parseOnForbiddenActionBlock(room))
						return false;
					
					continue;
				}
					
				break;
			}
			
			return true;
		}
				
		/**
		 * Parses a player.
		 * [Player] :=
		 * 		[IDENTIFIER] ";"
		 * 		[IDENTIFIER] "{" [PlayerBody] "}"
		 */
		public boolean parsePlayer()
		{
			// player identity.
			if (!currentType(Lexer.TYPE_IDENTIFIER))
			{
				addErrorMessage("Expected player identity.");
				return false;
			}

			String identity = currentToken().getLexeme();
			nextToken();
			
			// prototype?
			if (matchType(TSKernel.TYPE_DELIM_SEMICOLON))
			{
				TPlayer element = new TPlayer(identity);
				currentModule.addPlayer(element);
				return true;
			}

			TPlayer player;
			if ((player = currentModule.getPlayerByIdentity(identity)) == null)
			{
				player = new TPlayer(identity);
				currentModule.addPlayer(player);
			}
			
			if (!parseActionPermissionClause(player))
				return false;
			
			if (!matchType(TSKernel.TYPE_DELIM_LBRACE))
			{
				addErrorMessage("Expected \"{\" for player body start or \";\" (prototyping).");
				return false;
			}

			if (!parsePlayerBody(player))
				return false;
			
			if (!matchType(TSKernel.TYPE_DELIM_RBRACE))
			{
				addErrorMessage("Expected end-of-player \"}\".");
				return false;
			}

			return true;
		}
		
		/**
		 * Parses the player body.
		 * [PlayerBody] :=
		 * 		[PlayerBlock] [PlayerBody]
		 * 		[e]
		 */
		private boolean parsePlayerBody(TPlayer player)
		{
			while (isPlayerBlockType())
			{
				if (currentType(TSKernel.TYPE_INIT))
				{
					nextToken();
					
					if (!parseInitBlock(player))
						return false;
					
					continue;
				}
								
				if (currentType(TSKernel.TYPE_ONACTION))
				{
					nextToken();
					
					if (!parseOnActionBlock(player, Type.GENERAL, Type.OPEN))
						return false;
					
					continue;
				}
					
				if (currentType(TSKernel.TYPE_ONMODALACTION))
				{
					nextToken();
					
					if (!parseOnModalActionBlock(player))
						return false;
					
					continue;
				}
					
				if (currentType(TSKernel.TYPE_ONFAILEDACTION))
				{
					nextToken();
					
					if (!parseOnFailedActionBlock(player))
						return false;
					
					continue;
				}
					
				if (currentType(TSKernel.TYPE_ONAMBIGUOUSACTION))
				{
					nextToken();
					
					if (!parseOnAmbiguousActionBlock(player))
						return false;
					
					continue;
				}
					
				if (currentType(TSKernel.TYPE_ONFORBIDDENACTION))
				{
					nextToken();
					
					if (!parseOnForbiddenActionBlock(player))
						return false;
					
					continue;
				}
					
				if (currentType(TSKernel.TYPE_ONUNKNOWNACTION))
				{
					nextToken();
					
					if (!parseOnUnknownActionBlock(player))
						return false;
					
					continue;
				}
					
				break;
			}
			
			return true;
		}
		
		/**
		 * Parses a world.
		 * [World] :=
		 * 		";"
		 * 		"{" [WorldBody] "}"
		 */
		public boolean parseWorld()
		{
			// prototype?
			if (matchType(TSKernel.TYPE_DELIM_SEMICOLON))
			{
				currentModule.setWorld(new TWorld());
				return true;
			}

			TWorld world;
			if ((world = currentModule.getWorld()) == null)
			{
				world = new TWorld();
				currentModule.setWorld(world);
			}
						
			if (!matchType(TSKernel.TYPE_DELIM_LBRACE))
			{
				addErrorMessage("Expected \"{\" for world body start or \";\" (prototyping).");
				return false;
			}

			if (!parseWorldBody(world))
				return false;
			
			if (!matchType(TSKernel.TYPE_DELIM_RBRACE))
			{
				addErrorMessage("Expected end-of-world \"}\".");
				return false;
			}

			return true;
		}

		/**
		 * Parses the world body.
		 * [WorldBody] :=
		 * 		[WorldBlock] [WorldBody]
		 * 		[e]
		 */
		private boolean parseWorldBody(TWorld world)
		{
			while (isWorldBlockType())
			{
				if (currentType(TSKernel.TYPE_INIT))
				{
					nextToken();
					
					if (!parseInitBlock(world))
						return false;
					
					continue;
				}
								
				if (currentType(TSKernel.TYPE_ONACTION))
				{
					nextToken();
					
					if (!parseOnActionBlock(world, Type.GENERAL, Type.OPEN))
						return false;
					
					continue;
				}
					
				if (currentType(TSKernel.TYPE_ONMODALACTION))
				{
					nextToken();
					
					if (!parseOnModalActionBlock(world))
						return false;
					
					continue;
				}
					
				if (currentType(TSKernel.TYPE_ONFAILEDACTION))
				{
					nextToken();
					
					if (!parseOnFailedActionBlock(world))
						return false;
					
					continue;
				}
					
				if (currentType(TSKernel.TYPE_ONAMBIGUOUSACTION))
				{
					nextToken();
					
					if (!parseOnAmbiguousActionBlock(world))
						return false;
					
					continue;
				}
					
				if (currentType(TSKernel.TYPE_ONUNKNOWNACTION))
				{
					nextToken();
					
					if (!parseOnUnknownActionBlock(world))
						return false;
					
					continue;
				}
					
				if (currentType(TSKernel.TYPE_AFTERREQUEST))
				{
					nextToken();
					
					if (!parseAfterRequestBlock(world))
						return false;
					
					continue;
				}

				break;
			}
			
			return true;
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
				nextToken();
				
				if (!parseActionNames(action))
					return false;
				
				currentModule.addAction(action);
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
				nextToken();

				if (!parseActionNames(action))
					return false;
				
				currentModule.addAction(action);
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
				nextToken();

				if (!parseActionNames(action))
					return false;
				
				if (!parseActionAdditionalNames(action, TSKernel.TYPE_MODES))
					return false;
				
				currentModule.addAction(action);
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
				nextToken();

				if (!parseActionNames(action))
					return false;

				currentModule.addAction(action);
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
				nextToken();

				if (!parseActionNames(action))
					return false;

				if (!parseActionAdditionalNames(action, TSKernel.TYPE_CONJOINS))
					return false;

				currentModule.addAction(action);
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
		 * Parses action permission list.
		 * [ActionPermissionClause] :=
		 * 		[EXCLUDES] [ACTION] [ActionPermissionClauseList]
		 * 		[RESTRICTS] [ACTION] [ActionPermissionClauseList]
		 * 		[e]
		 */
		private boolean parseActionPermissionClause(ActionForbiddenHandler element)
		{
			if (matchType(TSKernel.TYPE_EXCLUDES))
			{
				element.setPermissionType(PermissionType.EXCLUDE);
				
				if (!isAction())
				{
					addErrorMessage("Expected action after \"excludes\".");
					return false;
				}
				
				element.addPermittedAction(currentModule.getActionByIdentity(currentToken().getLexeme()));
				nextToken();
				
				return parseActionPermissionClauseList(element);
			}

			if (matchType(TSKernel.TYPE_RESTRICTS))
			{
				element.setPermissionType(PermissionType.RESTRICT);
				
				if (!isAction())
				{
					addErrorMessage("Expected action after \"restricts\".");
					return false;
				}
				
				element.addPermittedAction(currentModule.getActionByIdentity(currentToken().getLexeme()));
				nextToken();
				
				return parseActionPermissionClauseList(element);
			}
			
			return true;
		}

		/**
		 * Parses action name list.
		 * [ActionPermissionClause] :=
		 * 		"," [ACTION] [ActionPermissionClause]
		 * 		[e]
		 */
		private boolean parseActionPermissionClauseList(ActionForbiddenHandler element)
		{
			if (matchType(TSKernel.TYPE_DELIM_COMMA))
			{
				if (!isAction())
				{
					addErrorMessage("Expected action after \"excludes\".");
					return false;
				}
				
				element.addPermittedAction(currentModule.getActionByIdentity(currentToken().getLexeme()));
				nextToken();
				
				return parseActionPermissionClauseList(element);
			}
			
			return true;
		}
		
		/**
		 * Parses object names.
		 * [ObjectNames] := 
		 * 		[NAMED] [STRING] [ObjectNameList]
		 * 		[e]
		 */
		private boolean parseObjectNames(TObject object)
		{
			if (matchType(TSKernel.TYPE_NAMED))
			{
				if (!currentType(Lexer.TYPE_STRING))
				{
					addErrorMessage("Expected object name (must be string).");
					return false;
				}
				
				object.getNames().put(currentToken().getLexeme());
				nextToken();
				
				return parseObjectNameList(object);
			}
			
			return true;
		}
		
		/**
		 * Parses object name list.
		 * [ObjectNameList] :=
		 * 		"," [STRING] [ObjectNameList]
		 * 		[e]
		 */
		private boolean parseObjectNameList(TObject object)
		{
			if (matchType(TSKernel.TYPE_DELIM_COMMA))
			{
				if (!currentType(Lexer.TYPE_STRING))
				{
					addErrorMessage("Expected object name (must be string).");
					return false;
				}
				
				object.getNames().put(currentToken().getLexeme());
				nextToken();
				
				return parseObjectNameList(object);
			}
			
			return true;
		}
		
		/**
		 * Parses an init block declaration.
		 * [InitBlock] :=
		 * 		"(" ")" [Block]
		 */
		private boolean parseInitBlock(TElement element)
		{
			if (!matchType(TSKernel.TYPE_DELIM_LPAREN))
			{
				addErrorMessage("Expected \"(\" after init.");
				return false;
			}
			
			if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
			{
				addErrorMessage("Expected \")\".");
				return false;
			}
			
			if (!parseBlock())
				return false;
			
			element.setInitBlock(currentBlock.pop());
			
			return true;
		}
		
		/**
		 * Parses an on action block declaration.
		 * [OnActionBlock] :=
		 * 		"(" [GENERALOROPENACTIONIDENTIFIER] ")" [Block]
		 */
		private boolean parseOnActionBlock(TActionableElement element, Type ... requiredActionTypes)
		{
			if (!matchType(TSKernel.TYPE_DELIM_LPAREN))
			{
				addErrorMessage("Expected \"(\" after action block declaration.");
				return false;
			}

			if (!isAction())
			{
				addErrorMessage("Expected valid action identifier.");
				return false;
			}
			
			String actionId = currentToken().getLexeme();
			TAction action = currentModule.getActionByIdentity(actionId);
			boolean matched = false;
			for (Type t : requiredActionTypes)
			{
				if (action.getType() == t)
				{
					matched = true;
					break;
				}
			}
			if (!matched)
			{
				addErrorMessage("Expected valid action type (" + Arrays.toString(requiredActionTypes) + ").");
				return false;
			}
			
			nextToken();
			
			if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
			{
				addErrorMessage("Expected \")\".");
				return false;
			}
			
			if (!parseBlock())
				return false;
			
			if (element.getActionTable().get(actionId) != null)
			{
				addErrorMessage("Action block for action \"" + actionId + "\" already declared.");
				return false;
			}
			element.getActionTable().add(actionId, currentBlock.pop());
			
			return true;
		}

		/**
		 * Parses an on modal action block declaration.
		 * [OnModalActionBlock] :=
		 * 		"(" [MODALACTIONIDENTIFIER] "," [STRING] ")" [Block]
		 */
		private boolean parseOnModalActionBlock(ActionModalHandler element)
		{
			if (!matchType(TSKernel.TYPE_DELIM_LPAREN))
			{
				addErrorMessage("Expected \"(\" after action block declaration.");
				return false;
			}

			if (!isAction())
			{
				addErrorMessage("Expected valid action identifier.");
				return false;
			}
			
			String actionId = currentToken().getLexeme();
			TAction action = currentModule.getActionByIdentity(actionId);
			if (action.getType() != Type.MODAL)
			{
				addErrorMessage("Expected modal action for modal action declaration.");
				return false;
			}
			
			nextToken();

			if (!matchType(TSKernel.TYPE_DELIM_COMMA))
			{
				addErrorMessage("Expected \",\" after action.");
				return false;
			}

			if (!currentType(Lexer.TYPE_STRING))
			{
				addErrorMessage("Expected valid modal type for action \"" + actionId + "\".");
				return false;
			}
			
			String mode = currentToken().getLexeme();
			if (!action.getExtraStrings().contains(mode))
			{
				addErrorMessage("Expected valid mode for action "+actionId+".");
				return false;
			}
			
			nextToken();
			
			if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
			{
				addErrorMessage("Expected \")\".");
				return false;
			}
			
			if (!parseBlock())
				return false;

			if (element.getModalActionTable().get(actionId, mode) != null)
			{
				addErrorMessage("Modal action block for action \"" + actionId + "\" and \"" + mode + "\" already declared.");
				return false;
			}
			element.getModalActionTable().add(actionId, mode, currentBlock.pop());
			
			return true;
		}

		/**
		 * Parses an on failed action block declaration.
		 * [OnFailedActionBlock] :=
		 * 		"(" [ACTIONIDENTIFIER] ")" [Block]
		 * 		"(" ")" [Block]
		 */
		private boolean parseOnFailedActionBlock(ActionFailedHandler element)
		{
			if (!matchType(TSKernel.TYPE_DELIM_LPAREN))
			{
				addErrorMessage("Expected \"(\" after action block declaration.");
				return false;
			}

			String actionId = null;

			if (isAction())
			{
				actionId = currentToken().getLexeme();
				nextToken();
				
				if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
				{
					addErrorMessage("Expected \")\".");
					return false;
				}
				
			}
			else if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
			{
				addErrorMessage("Expected \")\" or action identifier.");
				return false;
			}
			
			if (!parseBlock())
				return false;
			
			if (actionId != null)
			{
				if (element.getActionFailedTable().get(actionId) != null)
				{
					addErrorMessage("Failed action block for action \"" + actionId + "\" already declared.");
					return false;
				}
				element.getActionFailedTable().add(actionId, currentBlock.pop());
			}
			else
			{
				if (element.getActionFailedBlock() != null)
				{
					addErrorMessage("Default failed action block already declared.");
					return false;
				}
				element.setActionFailedBlock(currentBlock.pop());
			}
			
			return true;
		}
		
		/**
		 * Parses an on failed action block declaration.
		 * [OnForbiddenActionBlock] :=
		 * 		"(" [ACTIONIDENTIFIER] ")" [Block]
		 * 		"(" ")" [Block]
		 */
		private boolean parseOnForbiddenActionBlock(ActionForbiddenHandler element)
		{
			if (!matchType(TSKernel.TYPE_DELIM_LPAREN))
			{
				addErrorMessage("Expected \"(\" after action block declaration.");
				return false;
			}

			String actionId = null;

			if (isAction())
			{
				actionId = currentToken().getLexeme();
				nextToken();
				
				if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
				{
					addErrorMessage("Expected \")\".");
					return false;
				}
				
			}
			else if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
			{
				addErrorMessage("Expected \")\" or action identifier.");
				return false;
			}
			
			if (!parseBlock())
				return false;
			
			if (actionId != null)
			{
				if (element.getActionForbiddenTable().get(actionId) != null)
				{
					addErrorMessage("Forbidden action block for action \"" + actionId + "\" already declared.");
					return false;
				}
				element.getActionForbiddenTable().add(actionId, currentBlock.pop());
			}
			else
			{
				if (element.getActionForbiddenBlock() != null)
				{
					addErrorMessage("Default forbidden action block already declared.");
					return false;
				}
				element.setActionForbiddenBlock(currentBlock.pop());
			}
			
			return true;
		}
		
		/**
		 * Parses an on ambiguous action block declaration.
		 * [OnAmbiguousActionBlock] :=
		 * 		"(" [ACTIONIDENTIFIER] ")" [Block]
		 * 		"(" ")" [Block]
		 */
		private boolean parseOnAmbiguousActionBlock(ActionAmbiguousHandler element)
		{
			if (!matchType(TSKernel.TYPE_DELIM_LPAREN))
			{
				addErrorMessage("Expected \"(\" after action block declaration.");
				return false;
			}

			String actionId = null;

			if (isAction())
			{
				actionId = currentToken().getLexeme();
				nextToken();
				
				if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
				{
					addErrorMessage("Expected \")\".");
					return false;
				}
				
			}
			else if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
			{
				addErrorMessage("Expected \")\" or action identifier.");
				return false;
			}
			
			if (!parseBlock())
				return false;
			
			if (actionId != null)
			{
				if (element.getAmbiguousActionTable().get(actionId) != null)
				{
					addErrorMessage("Ambiguous action block for action \"" + actionId + "\" already declared.");
					return false;
				}
				element.getAmbiguousActionTable().add(actionId, currentBlock.pop());
			}
			else
			{
				if (element.getAmbiguousActionBlock() != null)
				{
					addErrorMessage("Default ambiguous action block already declared.");
					return false;
				}
				element.setAmbiguousActionBlock(currentBlock.pop());
			}
			
			return true;
		}
		
		/**
		 * Parses an on unknown action block declaration.
		 * [OnUnknownActionBlock] :=
		 * 		"(" ")" [Block]
		 */
		private boolean parseOnUnknownActionBlock(ActionUnknownHandler element)
		{
			if (!matchType(TSKernel.TYPE_DELIM_LPAREN))
			{
				addErrorMessage("Expected \"(\" after unknown action declaration.");
				return false;
			}
			
			if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
			{
				addErrorMessage("Expected \")\".");
				return false;
			}
			
			if (!parseBlock())
				return false;
			
			if (element.getUnknownActionBlock() != null)
			{
				addErrorMessage("\"Unknown Action\" block already declared.");
				return false;
			}
			element.setUnknownActionBlock(currentBlock.pop());
			
			return true;
		}
		
		/**
		 * Parses an on unknown action block declaration.
		 * [AfterRequestBlock] :=
		 * 		"(" ")" [Block]
		 */
		private boolean parseAfterRequestBlock(TWorld element)
		{
			if (!matchType(TSKernel.TYPE_DELIM_LPAREN))
			{
				addErrorMessage("Expected \"(\" after \"after request\" declaration.");
				return false;
			}
			
			if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
			{
				addErrorMessage("Expected \")\".");
				return false;
			}
			
			if (!parseBlock())
				return false;
			
			if (element.getAfterRequestBlock() != null)
			{
				addErrorMessage("\"After Request\" block already declared.");
				return false;
			}
			element.setAfterRequestBlock(currentBlock.pop());
			
			return true;
		}
		
		/**
		 * Parses an on-action-with block declaration.
		 * [OnActionWithBlock] :=
		 * 		"(" [DITRANSITIVEACTIONIDENTIFIER] "," [OBJECT] ")" [Block]
		 */
		private boolean parseOnActionWithBlock(TObject element)
		{
			if (!matchType(TSKernel.TYPE_DELIM_LPAREN))
			{
				addErrorMessage("Expected \"(\" after action block declaration.");
				return false;
			}

			if (!isAction())
			{
				addErrorMessage("Expected valid action identifier.");
				return false;
			}
			
			String actionId = currentToken().getLexeme();
			TAction action = currentModule.getActionByIdentity(actionId);
			if (action.getType() != Type.DITRANSITIVE)
			{
				addErrorMessage("Expected ditransitive action for on-action-with declaration.");
				return false;
			}
			
			nextToken();

			if (!matchType(TSKernel.TYPE_DELIM_COMMA))
			{
				addErrorMessage("Expected \",\" after action.");
				return false;
			}

			if (!isObject())
			{
				addErrorMessage("Expected valid object for on-action-with declaration.");
				return false;
			}
			
			String objectId = currentToken().getLexeme();
			nextToken();
			
			if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
			{
				addErrorMessage("Expected \")\".");
				return false;
			}
			
			if (!parseBlock())
				return false;

			if (element.getActionWithTable().get(actionId, objectId) != null)
			{
				addErrorMessage("Action block for action \"" + actionId + "\" and object \"" + objectId + "\" already declared.");
				return false;
			}
			element.getActionWithTable().add(actionId, objectId, currentBlock.pop());
			
			return true;
		}

		/**
		 * Parses an on-action-with block declaration.
		 * [OnActionWithOtherBlock] :=
		 * 		"(" [DITRANSITIVEACTIONIDENTIFIER] ")" [Block]
		 */
		private boolean parseOnActionWithOtherBlock(TObject element)
		{
			if (!matchType(TSKernel.TYPE_DELIM_LPAREN))
			{
				addErrorMessage("Expected \"(\" after action block declaration.");
				return false;
			}

			if (!isAction())
			{
				addErrorMessage("Expected valid action identifier.");
				return false;
			}
			
			String actionId = currentToken().getLexeme();
			TAction action = currentModule.getActionByIdentity(actionId);
			if (action.getType() != Type.DITRANSITIVE)
			{
				addErrorMessage("Expected ditransitive action for on-action-with-other declaration.");
				return false;
			}
			nextToken();

			if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
			{
				addErrorMessage("Expected \")\".");
				return false;
			}
			
			if (!parseBlock())
				return false;

			if (element.getActionWithOtherTable().get(actionId) != null)
			{
				addErrorMessage("Action-with-other block for action \"" + actionId + "\" already declared.");
				return false;
			}
			element.getActionWithOtherTable().add(actionId, currentBlock.pop());
			
			return true;
		}

		/**
		 * Parses an on-room-browse block declaration.
		 * [OnRoomBrowse] :=
		 * 		"(" ")" [Block]
		 */
		private boolean parseRoomBrowseBlock(TObject element)
		{
			if (!matchType(TSKernel.TYPE_DELIM_LPAREN))
			{
				addErrorMessage("Expected \"(\" after onRoomBrowse.");
				return false;
			}
			
			if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
			{
				addErrorMessage("Expected \")\".");
				return false;
			}
			
			if (!parseBlock())
				return false;
			
			element.setRoomBrowseBlock(currentBlock.pop());
			
			return true;
		}
		
		/**
		 * Parses an on-player-browse block declaration.
		 * [OnPlayerBrowse] :=
		 * 		"(" ")" [Block]
		 */
		private boolean parsePlayerBrowseBlock(TObject element)
		{
			if (!matchType(TSKernel.TYPE_DELIM_LPAREN))
			{
				addErrorMessage("Expected \"(\" after onPlayerBrowse.");
				return false;
			}
			
			if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
			{
				addErrorMessage("Expected \")\".");
				return false;
			}
			
			if (!parseBlock())
				return false;
			
			element.setPlayerBrowseBlock(currentBlock.pop());
			
			return true;
		}
		
		/**
		 * Parses an on-container-browse block declaration.
		 * [OnContainerBrowse] :=
		 * 		"(" ")" [Block]
		 */
		private boolean parseContainerBrowseBlock(TObject element)
		{
			if (!matchType(TSKernel.TYPE_DELIM_LPAREN))
			{
				addErrorMessage("Expected \"(\" after onContainerBrowse.");
				return false;
			}
			
			if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
			{
				addErrorMessage("Expected \")\".");
				return false;
			}
			
			if (!parseBlock())
				return false;
			
			element.setContainerBrowseBlock(currentBlock.pop());
			
			return true;
		}
		
		/**
		 * Parses a block.
		 * Pushes a block onto the block stack.
		 * [Block] :=
		 * 		"{" [StatementList] "}"
		 * 		[Statement]
		 */
		public boolean parseBlock()
		{
			currentBlock.push(new Block());
			
			if (currentType(TSKernel.TYPE_DELIM_LBRACE))
			{
				nextToken();
				
				if (!parseStatementList())
				{
					currentBlock.pop();
					return false;
				}
				
				if (!matchType(TSKernel.TYPE_DELIM_RBRACE))
				{
					currentBlock.pop();
					addErrorMessage("Expected end of block '}'.");
					return false;
				}
				
				currentBlock.push(optimizeBlock(currentBlock.pop()));
				return true;
			}
			
			if (currentType(TSKernel.TYPE_DELIM_SEMICOLON))
			{
				nextToken();
				return true;
			}
			
			// control block handling.
			if (currentType(TSKernel.TYPE_IF, TSKernel.TYPE_WHILE, TSKernel.TYPE_FOR))
			{
				if (!parseControl())
					return false;
				
				return true;
			}
			
			if (!parseStatement())
				return false;
			
			if (!matchType(TSKernel.TYPE_DELIM_SEMICOLON))
			{
				addErrorMessage("Expected \";\" to terminate statement.");
				return false;
			}
			
			currentBlock.push(optimizeBlock(currentBlock.pop()));
			return true;
		}
		
		/**
		 * Parses a block that consists of only one statement.
		 * Pushes a block onto the block stack.
		 * [BlockStatement] :=
		 * 		[Statement]
		 */
		public boolean parseBlockStatement()
		{
			currentBlock.push(new Block());
			
			if (!parseStatement())
			{
				currentBlock.pop();
				return false;
			}
			
			currentBlock.push(optimizeBlock(currentBlock.pop()));
			return true;
		}

		/**
		 * Parses a block that consists of the commands that evaluate an expression.
		 * Pushes a block onto the block stack.
		 * [BlockExpression] :=
		 * 		[Expression]
		 */
		public boolean parseBlockExpression()
		{
			currentBlock.push(new Block());
			
			if (!parseExpression())
			{
				currentBlock.pop();
				return false;
			}
			
			currentBlock.push(optimizeBlock(currentBlock.pop()));
			return true;
		}
		
		/**
		 * Parses a statement. Emits commands to the current block.
		 * [Statement] := 
		 *		[ELEMENTID] "." [VARIABLE] [ASSIGNMENTOPERATOR] [EXPRESSION]
		 * 		[VARIABLE] [ASSIGNMENTOPERATOR] [EXPRESSION]
		 * 		[COMMANDEXPRESSION]
		 * 		[COMMANDSTATEMENT]
		 * 		[COMMANDBLOCK]
		 *		[e]
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
			else if (currentType(TSKernel.TYPE_COMMAND_STATEMENT))
			{
				TAMECommand commandType = tokenToCommand();
				nextToken();
				
				if (!parseCommandCall(commandType))
					return false;

			}
			else if (currentType(TSKernel.TYPE_COMMAND_EXPRESSION))
			{
				TAMECommand commandType = tokenToCommand();
				nextToken();
				
				if (!parseCommandCall(commandType))
					return false;
				
				emit(Command.create(TAMECommand.POP));
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
			if (!currentType(Lexer.TYPE_IDENTIFIER, TSKernel.TYPE_COMMAND_INTERNAL, TSKernel.TYPE_COMMAND_STATEMENT, TSKernel.TYPE_COMMAND_EXPRESSION, TSKernel.TYPE_DELIM_SEMICOLON, TSKernel.TYPE_IF, TSKernel.TYPE_WHILE, TSKernel.TYPE_FOR))
				return true;
			
			if (currentType(TSKernel.TYPE_DELIM_SEMICOLON))
			{
				nextToken();
				return parseStatementList();
			}
			
			// control block handling.
			if (currentType(TSKernel.TYPE_IF, TSKernel.TYPE_WHILE, TSKernel.TYPE_FOR))
			{
				if (!parseControl())
					return false;
				
				return parseStatementList();
			}
			
			if (!parseStatement())
				return false;
			
			if (!matchType(TSKernel.TYPE_DELIM_SEMICOLON))
			{
				addErrorMessage("Expected \";\" to terminate statement.");
				return false;
			}
			
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

		/**
		 * Parses a control block.
		 * [ControlBlock] :=
		 * 		[IF] "(" [EXPRESSION] ")" [BLOCK] [ELSE] [BLOCK]
		 * 		[WHILE] "(" [EXPRESSION] ")" [BLOCK]
		 * 		[FOR] "(" [STATEMENT] ";" [EXPRESSION] ";" [STATEMENT] ")" [BLOCK]
		 */
		private boolean parseControl() 
		{
			if (currentType(TSKernel.TYPE_IF))
			{
				nextToken();
				
				if (!matchType(TSKernel.TYPE_DELIM_LPAREN))
				{
					addErrorMessage("Expected '(' after \"if\".");
					return false;
				}

				Block conditionalBlock = null;
				Block successBlock = null;
				Block failureBlock = null;
				
				if (!parseBlockExpression())
					return false;
				conditionalBlock = currentBlock.pop();
				
				if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
				{
					addErrorMessage("Expected ')' after conditional expression.");
					return false;
				}

				if (!parseBlock())
					return false;
				successBlock = currentBlock.pop();
				
				if (currentType(TSKernel.TYPE_ELSE))
				{
					nextToken();

					if (!parseBlock())
						return false;
					failureBlock = currentBlock.pop();
				}

				emit(Command.create(TAMECommand.IF, conditionalBlock, successBlock, failureBlock));
				return true;
			}
			else if (currentType(TSKernel.TYPE_WHILE))
			{
				nextToken();

				if (!matchType(TSKernel.TYPE_DELIM_LPAREN))
				{
					addErrorMessage("Expected '(' after \"while\".");
					return false;
				}

				Block conditionalBlock = null;
				Block successBlock = null;
				
				if (!parseBlockExpression())
					return false;
				conditionalBlock = currentBlock.pop();
				
				if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
				{
					addErrorMessage("Expected ')' after conditional expression.");
					return false;
				}

				if (!parseBlock())
					return false;
				successBlock = currentBlock.pop();

				emit(Command.create(TAMECommand.WHILE, conditionalBlock, successBlock));
				return true;
			}
			else if (currentType(TSKernel.TYPE_FOR))
			{
				nextToken();

				if (!matchType(TSKernel.TYPE_DELIM_LPAREN))
				{
					addErrorMessage("Expected '(' after \"for\".");
					return false;
				}

				Block initBlock = null;
				Block conditionalBlock = null;
				Block stepBlock = null;
				Block successBlock = null;
				
				if (!parseBlockStatement())
					return false;
				initBlock = currentBlock.pop();

				if (!matchType(TSKernel.TYPE_DELIM_SEMICOLON))
				{
					addErrorMessage("Expected ';' after inital statement in \"for\".");
					return false;
				}

				if (!parseBlockExpression())
					return false;
				conditionalBlock = currentBlock.pop();
				
				if (!matchType(TSKernel.TYPE_DELIM_SEMICOLON))
				{
					addErrorMessage("Expected ';' after conditional statement in \"for\".");
					return false;
				}

				if (!parseBlockStatement())
					return false;
				stepBlock = currentBlock.pop();

				if (!matchType(TSKernel.TYPE_DELIM_RPAREN))
				{
					addErrorMessage("Expected ')' after stepping statement in \"for\".");
					return false;
				}

				if (!parseBlock())
					return false;
				successBlock = currentBlock.pop();

				emit(Command.create(TAMECommand.FOR, initBlock, conditionalBlock, stepBlock, successBlock));
				return true;
			}
			else
			{
				addErrorMessage("INTERNAL ERROR!! CONTROL BLOCK: You should not see this!");
				return false;
			}	
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
		
		// Attempts to reduce redundant calls and unnecessary ones on blocks.
		// Returns a new block (unless optimization is off).
		private Block optimizeBlock(Block block)
		{
			if (!options.isOptimizing())
				return block;
						
			// TODO: Write this.
			return block;
			
			/*
			List<Command> tempList = new List<>();
			Block outBlock = new Block();
			
			for (Command c : block)
				tempList.add(c);

			final int STATE_OPERATOR = 0;
			final int STATE_OPERAND1 = 1;
			final int STATE_OPERAND2 = 2;
			int state = STATE_OPERATOR;
			int i = tempList.size() - 1;
			Command temp, arithfunc, operand2, operand1;
			
			while (!tempList.isEmpty())
			{
				temp = tempList.getByIndex(i);
				
				switch (state)
				{
					case STATE_OPERATOR:
					{
						
					}
					break;
					
					case STATE_OPERAND1:
					{
						
					}
					break;

					case STATE_OPERAND2:
					{
						
					}
					break;
				}
				
				i--;
			}
			*/
			
		}
		
		/**
		 * Emits a command into the current block.
		 */
		private void emit(Command command)
		{
			currentBlock.peek().add(command);
		}
		
		/**
		 * Prints an object to verbose out.
		 */
		private void verbose(Object output)
		{
			if (options.isVerbose() && options.getVerboseOut() != null)
				options.getVerboseOut().println(String.valueOf(output));
		}
		
		/**
		 * Prints a formatted string to verbose out.
		 */
		private void verbosef(String formatText, Object ... args)
		{
			verbose(String.format(formatText, args));
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
			return read(file.getPath(), fis, DEFAULT_OPTIONS, DEFAULT_INCLUDER);
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
		return read(STREAMNAME_TEXT, new StringReader(text), DEFAULT_OPTIONS, DEFAULT_INCLUDER);
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
		return read(streamName, new InputStreamReader(in), DEFAULT_OPTIONS, DEFAULT_INCLUDER);
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
		return read(streamName, reader, DEFAULT_OPTIONS, DEFAULT_INCLUDER);
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
		return read(STREAMNAME_TEXT, new StringReader(text), DEFAULT_OPTIONS, includer);
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
		return read(streamName, new InputStreamReader(in), DEFAULT_OPTIONS, includer);
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
		return read(streamName, reader, DEFAULT_OPTIONS, includer);
	}

	/**
	 * Reads TAMEModule objects into a new root from a starting text file.
	 * Note: Calls apply() with a new root.
	 * @param file	the file to read from.
	 * @param options the reader options for compiling.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if f is null. 
	 */
	public static TAMEModule read(File file, TAMEScriptReaderOptions options) throws IOException
	{
		FileInputStream fis = new FileInputStream(file);
		try {
			return read(file.getPath(), fis, options, DEFAULT_INCLUDER);
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
	public static TAMEModule read(String text, TAMEScriptReaderOptions options) throws IOException
	{
		return read(STREAMNAME_TEXT, new StringReader(text), options, DEFAULT_INCLUDER);
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
	public static TAMEModule read(String streamName, InputStream in, TAMEScriptReaderOptions options) throws IOException
	{
		return read(streamName, new InputStreamReader(in), options, DEFAULT_INCLUDER);
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
	public static TAMEModule read(String streamName, Reader reader, TAMEScriptReaderOptions options) throws IOException
	{
		return read(streamName, reader, options, DEFAULT_INCLUDER);
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
	public static TAMEModule read(File file, TAMEScriptReaderOptions options, TAMEScriptIncluder includer) throws IOException
	{
		FileInputStream fis = new FileInputStream(file);
		try {
			return read(file.getPath(), fis, options, includer);
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
	public static TAMEModule read(String text, TAMEScriptReaderOptions options, TAMEScriptIncluder includer) throws IOException
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
	public static TAMEModule read(String streamName, InputStream in, TAMEScriptReaderOptions options, TAMEScriptIncluder includer) throws IOException
	{
		return read(streamName, new InputStreamReader(in), options, includer);
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
	public static TAMEModule read(String streamName, Reader reader, TAMEScriptReaderOptions options, TAMEScriptIncluder includer) throws IOException
	{
		return (new TSParser(new TSLexer(streamName, reader, includer), options)).readModule();
	}

}
