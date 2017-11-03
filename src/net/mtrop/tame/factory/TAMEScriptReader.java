/*******************************************************************************
 * Copyright (c) 2016-2017 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package net.mtrop.tame.factory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;

import com.blackrook.commons.Common;
import com.blackrook.commons.Reflect;
import com.blackrook.commons.hash.CaseInsensitiveHashMap;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.linkedlist.Queue;
import com.blackrook.commons.linkedlist.Stack;
import com.blackrook.commons.list.List;
import com.blackrook.lang.CommonLexer;
import com.blackrook.lang.CommonLexerKernel;
import com.blackrook.lang.Parser;
import com.blackrook.lang.ParserException;

import net.mtrop.tame.TAMECommand;
import net.mtrop.tame.TAMEConstants;
import net.mtrop.tame.TAMEModule;
import net.mtrop.tame.element.ForbiddenHandler;
import net.mtrop.tame.element.ObjectContainer;
import net.mtrop.tame.element.TAction;
import net.mtrop.tame.element.TContainer;
import net.mtrop.tame.element.TElement;
import net.mtrop.tame.element.TObject;
import net.mtrop.tame.element.TPlayer;
import net.mtrop.tame.element.TRoom;
import net.mtrop.tame.element.TWorld;
import net.mtrop.tame.lang.ArgumentType;
import net.mtrop.tame.lang.ArithmeticOperator;
import net.mtrop.tame.lang.Block;
import net.mtrop.tame.lang.BlockEntry;
import net.mtrop.tame.lang.BlockEntryType;
import net.mtrop.tame.lang.Command;
import net.mtrop.tame.lang.FunctionEntry;
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
		static final int TYPE_COMMENT = 		0;
		static final int TYPE_FALSE = 			1;
		static final int TYPE_TRUE = 			2;
		static final int TYPE_INFINITY = 		3;
		static final int TYPE_NAN = 			4;

		static final int TYPE_WHILE =			10;
		static final int TYPE_FOR =				11;
		static final int TYPE_IF =				12;
		static final int TYPE_ELSE =			13;
		static final int TYPE_QUIT =			14;
		static final int TYPE_END =				15;
		static final int TYPE_FINISH =			16;
		static final int TYPE_BREAK =			17;
		static final int TYPE_CONTINUE =		18;
		static final int TYPE_RETURN =			19;

		static final int TYPE_LPAREN =			20;
		static final int TYPE_RPAREN =			21;
		static final int TYPE_LBRACE = 			22;
		static final int TYPE_RBRACE = 			23;
		static final int TYPE_LBRACK = 			24;
		static final int TYPE_RBRACK = 			25;
		static final int TYPE_COLON = 			26;
		static final int TYPE_SEMICOLON = 		27;
		static final int TYPE_COMMA = 			28;
		static final int TYPE_DOT = 			29;
		static final int TYPE_PLUS =			30;
		static final int TYPE_MINUS =			41;
		static final int TYPE_EXCLAMATION =		42;
		static final int TYPE_STAR = 			43;
		static final int TYPE_STARSTAR = 		44;
		static final int TYPE_SLASH = 			45;
		static final int TYPE_PERCENT = 		46;
		static final int TYPE_AMPERSAND = 		47;
		static final int TYPE_PIPE = 			48;
		static final int TYPE_CARAT = 			49;
		static final int TYPE_LESS = 			50;
		static final int TYPE_LESSEQUAL = 		51;
		static final int TYPE_GREATER =			52;
		static final int TYPE_GREATEREQUAL = 	53;
		static final int TYPE_EQUAL = 			54;
		static final int TYPE_EQUAL2 = 			55;
		static final int TYPE_EQUAL3 = 			56;
		static final int TYPE_NOTEQUAL = 		57;
		static final int TYPE_NOTEQUALEQUAL =	58;
		static final int TYPE_DOUBLEAMPERSAND = 59;
		static final int TYPE_DOUBLEPIPE = 		60;
		static final int TYPE_QUESTIONMARK = 	61;

		static final int TYPE_MODULE = 			70;
		static final int TYPE_WORLD = 			71;
		static final int TYPE_ROOM = 			72;
		static final int TYPE_PLAYER = 			73;
		static final int TYPE_OBJECT = 			74;
		static final int TYPE_CONTAINER =		75;
		static final int TYPE_ACTION = 			76;
		static final int TYPE_GENERAL = 		77;
		static final int TYPE_MODAL = 			78;
		static final int TYPE_TRANSITIVE = 		79;
		static final int TYPE_DITRANSITIVE = 	80;
		static final int TYPE_OPEN = 			81;
		static final int TYPE_NAMED = 			82;
		static final int TYPE_TAGGED = 			83;
		static final int TYPE_MODES = 			84;
		static final int TYPE_USES = 			85;
		static final int TYPE_CONJUNCTIONS = 	86;
		static final int TYPE_DETERMINERS = 	87;
		static final int TYPE_FORBIDS = 		88;
		static final int TYPE_ALLOWS = 			89;
		static final int TYPE_RESTRICTED = 		90;
		static final int TYPE_LOCAL = 			91;
		static final int TYPE_CLEAR = 			92;
		static final int TYPE_ARCHETYPE = 		93;
		static final int TYPE_FUNCTION = 		94;
		static final int TYPE_THIS = 			95;
		static final int TYPE_EXTEND = 			96;
		static final int TYPE_OVERRIDE = 		97;
		static final int TYPE_STRICT = 			98;
		static final int TYPE_REVERSED = 		99;

		static final HashMap<String, BlockEntryType> BLOCKENTRYTYPE_MAP = new CaseInsensitiveHashMap<BlockEntryType>();
		
		private TSKernel()
		{
			addStringDelimiter('"', '"');
			setDecimalSeparator('.');
			
			addCommentStartDelimiter("/*", TYPE_COMMENT);
			addCommentLineDelimiter("//", TYPE_COMMENT);
			addCommentEndDelimiter("*/", TYPE_COMMENT);

			addCaseInsensitiveKeyword("true", TYPE_TRUE);
			addCaseInsensitiveKeyword("false", TYPE_FALSE);
			addCaseInsensitiveKeyword("Infinity", TYPE_INFINITY);
			addCaseInsensitiveKeyword("NaN", TYPE_NAN);
			
			addCaseInsensitiveKeyword("else", TYPE_ELSE);
			addCaseInsensitiveKeyword("if", TYPE_IF);
			addCaseInsensitiveKeyword("while", TYPE_WHILE);
			addCaseInsensitiveKeyword("for", TYPE_FOR);
			addCaseInsensitiveKeyword("quit", TYPE_QUIT);
			addCaseInsensitiveKeyword("end", TYPE_END);
			addCaseInsensitiveKeyword("finish", TYPE_FINISH);
			addCaseInsensitiveKeyword("return", TYPE_RETURN);
			addCaseInsensitiveKeyword("break", TYPE_BREAK);
			addCaseInsensitiveKeyword("continue", TYPE_CONTINUE);

			addDelimiter("(", TYPE_LPAREN);
			addDelimiter(")", TYPE_RPAREN);
			addDelimiter("{", TYPE_LBRACE);
			addDelimiter("}", TYPE_RBRACE);
			addDelimiter("[", TYPE_LBRACK);
			addDelimiter("]", TYPE_RBRACK);
			addDelimiter(":", TYPE_COLON);
			addDelimiter(";", TYPE_SEMICOLON);
			addDelimiter(",", TYPE_COMMA);
			addDelimiter(".", TYPE_DOT);
			addDelimiter("?", TYPE_QUESTIONMARK);
			addDelimiter("+", TYPE_PLUS);
			addDelimiter("-", TYPE_MINUS);
			addDelimiter("!", TYPE_EXCLAMATION);
			addDelimiter("*", TYPE_STAR);
			addDelimiter("**", TYPE_STARSTAR);
			addDelimiter("/", TYPE_SLASH);
			addDelimiter("%", TYPE_PERCENT);
			addDelimiter("&", TYPE_AMPERSAND);
			addDelimiter("&&", TYPE_DOUBLEAMPERSAND);
			addDelimiter("|", TYPE_PIPE);
			addDelimiter("||", TYPE_DOUBLEPIPE);
			addDelimiter("^", TYPE_CARAT);
			addDelimiter("<", TYPE_LESS);
			addDelimiter("<=", TYPE_LESSEQUAL);
			addDelimiter(">", TYPE_GREATER);
			addDelimiter(">=", TYPE_GREATEREQUAL);
			addDelimiter("=", TYPE_EQUAL);
			addDelimiter("==", TYPE_EQUAL2);
			addDelimiter("===", TYPE_EQUAL3);
			addDelimiter("!=", TYPE_NOTEQUAL);
			addDelimiter("!==", TYPE_NOTEQUALEQUAL);
			
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
			addCaseInsensitiveKeyword("restricted", TYPE_RESTRICTED);
			addCaseInsensitiveKeyword("named", TYPE_NAMED);
			addCaseInsensitiveKeyword("tagged", TYPE_TAGGED);
			addCaseInsensitiveKeyword("modes", TYPE_MODES);
			addCaseInsensitiveKeyword("uses", TYPE_USES);
			addCaseInsensitiveKeyword("conjunctions", TYPE_CONJUNCTIONS);
			addCaseInsensitiveKeyword("determiners", TYPE_DETERMINERS);
			addCaseInsensitiveKeyword("forbids", TYPE_FORBIDS);
			addCaseInsensitiveKeyword("allows", TYPE_ALLOWS);
			addCaseInsensitiveKeyword("local", TYPE_LOCAL);
			addCaseInsensitiveKeyword("clear", TYPE_CLEAR);
			addCaseInsensitiveKeyword("archetype", TYPE_ARCHETYPE);
			addCaseInsensitiveKeyword("function", TYPE_FUNCTION);
			addCaseInsensitiveKeyword("this", TYPE_THIS);
			addCaseInsensitiveKeyword("extend", TYPE_EXTEND);
			addCaseInsensitiveKeyword("override", TYPE_OVERRIDE);
			addCaseInsensitiveKeyword("strict", TYPE_STRICT);
			addCaseInsensitiveKeyword("reversed", TYPE_REVERSED);

			for (BlockEntryType entryType : BlockEntryType.VALUES)
				BLOCKENTRYTYPE_MAP.put(entryType.name(), entryType);

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
		protected String getNextResourceName(String currentStreamName, String includePath) throws IOException 
		{
			return includer.getNextIncludeResourceName(currentStreamName, includePath);
		}
		
		@Override
		protected InputStream getResource(String path) throws IOException
		{
			return includer.getIncludeResource(path);
		}
	}

	/**
	 * The parser that parses text for the ArcheText structures. 
	 */
	private static class TSParser extends Parser
	{
		/** The parser options. */
		private TAMEScriptReaderOptions options;

		/** Current module. */
		private TAMEModule currentModule;
		/** Control block count. */
		private int controlDepth;
		/** Function block count. */
		private int functionDepth;
		
		private TSParser(TSLexer lexer, TAMEScriptReaderOptions options)
		{
			super(lexer);
			for (String def : options.getDefines())
				lexer.addDefineMacro(def);
			this.options = options;
			this.currentModule = null;
			this.controlDepth = 0;
			this.functionDepth = 0;
		}
		
		/**
		 * Reads objects into a module.
		 */
		TAMEModule readModule()
		{
			currentModule = new TAMEModule();
			
			// prime first token.
			nextToken();
			
			// keep parsing entries.
			boolean noError = true;
			
			try {
				while (currentToken() != null && (noError = parseModuleElement())) ;
			} catch (ParserException e) {
				addErrorMessage(e.getMessage());
				noError = false;
			}
			
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
			else if (currentModule.getWorld() == null)
			{
				throw new TAMEScriptParseException("No world declared!");
			}
			
			verbosef("Action count: %d", currentModule.getActionCount());
			verbosef("Player count: %d", currentModule.getPlayerCount());
			verbosef("Container count: %d", currentModule.getContainerCount());
			verbosef("Room count: %d", currentModule.getRoomCount());
			verbosef("Object count: %d", currentModule.getObjectCount());
			
			return currentModule;
		}

		/**
		 * Parses a module element.
		 */
		private boolean parseModuleElement()
		{
			boolean extendMode = false;
			
			if (matchType(TSKernel.TYPE_MODULE))
			{
				if (!parseModuleAttributes())
					return false;
				
				return true;
			}
			else if (matchType(TSKernel.TYPE_ACTION))
			{
				if (!parseAction())
					return false;
				
				if (!matchType(TSKernel.TYPE_SEMICOLON))
				{
					addErrorMessage("Expected end of action declaration \";\".");
					return false;
				}
				
				return true;
			}
			else if (matchType(TSKernel.TYPE_EXTEND))
			{
				extendMode = true;
			}

			if (matchType(TSKernel.TYPE_WORLD))
			{
				if (!parseWorld(extendMode))
					return false;
				
				return true;
			}
			
			if (matchType(TSKernel.TYPE_PLAYER))
			{
				if (!parsePlayer(extendMode))
					return false;
				
				return true;
			}

			if (matchType(TSKernel.TYPE_ROOM))
			{
				if (!parseRoom(extendMode))
					return false;
				
				return true;
			}

			if (matchType(TSKernel.TYPE_OBJECT))
			{
				if (!parseObject(extendMode))
					return false;
				
				return true;
			}

			if (matchType(TSKernel.TYPE_CONTAINER))
			{
				if (!parseContainer(extendMode))
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
			if (!matchType(TSKernel.TYPE_LBRACE))
			{
				addErrorMessage("Expected \"{\" to start module attributes.");
				return false;
			}

			if (!parseModuleAttributeList())
				return false;
			
			if (!matchType(TSKernel.TYPE_RBRACE))
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
			if (currentType(TSKernel.TYPE_IDENTIFIER))
			{
				String attribute = currentToken().getLexeme();
				nextToken();

				if (!matchType(TSKernel.TYPE_EQUAL))
				{
					addErrorMessage("Expected \"=\" after attribute name.");
					return false;
				}

				if (!currentType(TSKernel.TYPE_STRING, TSKernel.TYPE_NUMBER))
				{
					addErrorMessage("Expected literal value.");
					return false;
				}

				String value = currentToken().getLexeme();
				nextToken();

				currentModule.getHeader().addAttribute(attribute, value);

				verbosef("Added module attribute %s, value \"%s\".", attribute, value);
				
				if (!matchType(TSKernel.TYPE_SEMICOLON))
				{
					addErrorMessage("Expected \";\" to end the attribute.");
					return false;
				}
				
				return parseModuleAttributeList();
			}
			
			return true;
		}

		/**
		 * Parses a world.
		 * [World] :=
		 * 		";"
		 * 		"{" [WorldBody] "}"
		 */
		private boolean parseWorld(boolean extendMode)
		{
			verbose("Read world...");
		
			// get world instance.
			TWorld world = currentModule.getWorld();

			// if no world
			if (world == null)
			{
				if (extendMode)
				{
					addErrorMessage("Keyword \"extend\" used on first declaration.");
					return false;
				}
				else
				{
					world = new TWorld();
					currentModule.setWorld(world);
				}
			}
			else if (!extendMode)
			{
				addErrorMessage("The world was already declared previously - must have \"extend\" before it.");
				return false;
			}
						
			// empty?
			if (matchType(TSKernel.TYPE_SEMICOLON))
			{
				currentModule.setWorld(new TWorld());
				return true;
			}
		
			// check for body.
			if (!matchType(TSKernel.TYPE_LBRACE))
			{
				addErrorMessage("Expected \"{\" for world body start or \";\" (no body).");
				return false;
			}
		
			if (!parseElementBody(world, "world"))
				return false;
			
			if (!matchType(TSKernel.TYPE_RBRACE))
			{
				addErrorMessage("Expected end-of-world \"}\".");
				return false;
			}
		
			return true;
		}

		/**
		 * Parses a container.
		 * [Container] :=
		 * 		[IDENTIFIER] ";"
		 * 		[IDENTIFIER] "{" [ObjectBody] "}"
		 */
		private boolean parseContainer(boolean extendMode)
		{
			boolean archetype = false;
			if (matchType(TSKernel.TYPE_ARCHETYPE))
			{
				if (extendMode)
				{
					addErrorMessage("Containers must be declared \"archetype\" on first declaration, not \"extend\" ones.");
					return false;
				}
				else
				{
					archetype = true;
				}
			}

			// container identity.
			if (!currentType(TSKernel.TYPE_IDENTIFIER))
			{
				addErrorMessage("Expected container identity.");
				return false;
			}

			String identity = currentToken().getLexeme();
			nextToken();

			verbosef("Read container %s...", identity);

			// Get container.
			TContainer container = currentModule.getContainerByIdentity(identity);
			if (container == null)
			{
				if (extendMode)
				{
					addErrorMessage("Keyword \"extend\" used on first declaration of container \""+identity+"\".");
					return false;
				}
				else
				{
					container = new TContainer(identity);
					// archetype can only be set, never removed.
					if (archetype)
						container.setArchetype(true);
	
					// parse parent clause if any.
					if (!parseContainerParent(container))
						return false;
					
					currentModule.addContainer(container);
				}
			}
			else if (!extendMode)
			{
				addErrorMessage("Container \""+identity+"\" already declared - must have \"extend\" before it.");
				return false;
			}
			
			// prototype?
			if (matchType(TSKernel.TYPE_SEMICOLON))
				return true;

			if (!matchType(TSKernel.TYPE_LBRACE))
			{
				addErrorMessage("Expected \"{\" for container body start or \";\" (no body).");
				return false;
			}

			if (!parseElementBody(container, "container"))
				return false;
			
			if (!matchType(TSKernel.TYPE_RBRACE))
			{
				addErrorMessage("Expected end-of-object \"}\".");
				return false;
			}

			return true;
		}
		
		/**
		 * Parses the container parent clause.
		 * [ContainerParent] :=
		 * 		":" [OBJECT]
		 * 		[e]
		 */
		private boolean parseContainerParent(TContainer container)
		{
			if (matchType(TSKernel.TYPE_COLON))
			{
				if (!isContainer(true))
				{
					addErrorMessage("Expected container type after \":\".");
					return false;
				}
				
				String identity = currentToken().getLexeme();
				nextToken();
				TContainer parent = currentModule.getContainerByIdentity(identity);
				
				if (container.getParent() != null)
				{
					addErrorMessage("Container already has a parent.");
					return false;
				}
				
				container.setParent(parent);
			}
			
			return true;
		}
		
		/**
		 * Parses a room.
		 * [Room] :=
		 * 		[IDENTIFIER] ";"
		 * 		[IDENTIFIER] "{" [RoomBody] "}"
		 */
		private boolean parseRoom(boolean extendMode)
		{
			boolean archetype = false;
			if (matchType(TSKernel.TYPE_ARCHETYPE))
			{
				if (extendMode)
				{
					addErrorMessage("Room must be declared \"archetype\" on first declaration, not \"extend\" ones.");
					return false;
				}
				else
				{
					archetype = true;
				}
			}

			// room identity.
			if (!currentType(TSKernel.TYPE_IDENTIFIER))
			{
				addErrorMessage("Expected room identity.");
				return false;
			}

			String identity = currentToken().getLexeme();
			nextToken();
			
			verbosef("Read room %s...", identity);

			TRoom room = currentModule.getRoomByIdentity(identity);
			if (room == null)
			{
				if (extendMode)
				{
					addErrorMessage("Keyword \"extend\" used on first declaration of room \""+identity+"\".");
					return false;
				}
				else
				{
					room = new TRoom(identity);
					// archetype can only be set, never removed.
					if (archetype)
						room.setArchetype(true);
	
					// parse parent clause if any.
					if (!parseRoomParent(room))
						return false;
					
					if (!parseActionPermissionClause(room))
						return false;

					currentModule.addRoom(room);
				}
			}
			else if (!extendMode)
			{
				addErrorMessage("Room \""+identity+"\" already declared - must have \"extend\" before it.");
				return false;
			}

			// no body?
			if (matchType(TSKernel.TYPE_SEMICOLON))
				return true;

			// check for body.
			if (!matchType(TSKernel.TYPE_LBRACE))
			{
				addErrorMessage("Expected \"{\" for room body start or \";\" (no body).");
				return false;
			}

			if (!parseElementBody(room, "room"))
				return false;
			
			if (!matchType(TSKernel.TYPE_RBRACE))
			{
				addErrorMessage("Expected end-of-room \"}\".");
				return false;
			}

			return true;
		}
		
		/**
		 * Parses the room parent clause.
		 * [RoomParent] :=
		 * 		":" [ROOM]
		 * 		[e]
		 */
		private boolean parseRoomParent(TRoom room)
		{
			if (matchType(TSKernel.TYPE_COLON))
			{
				if (!isRoom(true))
				{
					addErrorMessage("Expected room type after \":\".");
					return false;
				}
				
				String identity = currentToken().getLexeme();
				nextToken();
				TRoom parent = currentModule.getRoomByIdentity(identity);
				
				if (room.getParent() != null)
				{
					addErrorMessage("Room already has a parent.");
					return false;
				}
				
				room.setParent(parent);
			}
			
			return true;
		}
		
		/**
		 * Parses a player.
		 * [Player] :=
		 * 		[IDENTIFIER] ";"
		 * 		[IDENTIFIER] "{" [PlayerBody] "}"
		 */
		private boolean parsePlayer(boolean extendMode)
		{
			boolean archetype = false;
			if (matchType(TSKernel.TYPE_ARCHETYPE))
			{
				if (extendMode)
				{
					addErrorMessage("Player must be declared \"archetype\" on first declaration, not \"extend\" ones.");
					return false;
				}
				else
				{
					archetype = true;
				}
			}

			// player identity.
			if (!currentType(TSKernel.TYPE_IDENTIFIER))
			{
				addErrorMessage("Expected player identity.");
				return false;
			}

			String identity = currentToken().getLexeme();
			nextToken();
			
			verbosef("Read player %s...", identity);

			TPlayer player = currentModule.getPlayerByIdentity(identity);
			if (player == null)
			{
				if (extendMode)
				{
					addErrorMessage("Keyword \"extend\" used on first declaration of player \""+identity+"\".");
					return false;
				}
				else
				{
					player = new TPlayer(identity);
					// archetype can only be set, never removed.
					if (archetype)
						player.setArchetype(true);
					
					// parse parent clause if any.
					if (!parsePlayerParent(player))
						return false;
	
					if (!parseActionPermissionClause(player))
						return false;
	
					currentModule.addPlayer(player);
				}
			}
			else if (!extendMode)
			{
				addErrorMessage("Player \""+identity+"\" already declared - must have \"extend\" before it.");
				return false;
			}
			
			// no body?
			if (matchType(TSKernel.TYPE_SEMICOLON))
				return true;

			// check for body.
			if (!matchType(TSKernel.TYPE_LBRACE))
			{
				addErrorMessage("Expected \"{\" for player body start or \";\" (no body).");
				return false;
			}

			if (!parseElementBody(player, "player"))
				return false;
			
			if (!matchType(TSKernel.TYPE_RBRACE))
			{
				addErrorMessage("Expected end-of-player \"}\".");
				return false;
			}

			return true;
		}
		
		/**
		 * Parses the player parent clause.
		 * [PlayerParent] :=
		 * 		":" [PLAYER]
		 * 		[e]
		 */
		private boolean parsePlayerParent(TPlayer player)
		{
			if (matchType(TSKernel.TYPE_COLON))
			{
				if (!isPlayer(true))
				{
					addErrorMessage("Expected player type after \":\".");
					return false;
				}
				
				String identity = currentToken().getLexeme();
				nextToken();
				TPlayer parent = currentModule.getPlayerByIdentity(identity);
				
				if (player.getParent() != null)
				{
					addErrorMessage("Player already has a parent.");
					return false;
				}
				
				player.setParent(parent);
			}
			
			return true;
		}
		
		/**
		 * Parses an object.
		 * [Object] :=
		 * 		[IDENTIFIER] ";"
		 * 		[IDENTIFIER] "{" [ObjectBody] "}"
		 */
		private boolean parseObject(boolean extendMode)
		{
			boolean archetype = false;
			if (matchType(TSKernel.TYPE_ARCHETYPE))
			{
				if (extendMode)
				{
					addErrorMessage("Object must be declared \"archetype\" on first declaration, not \"extend\" ones.");
					return false;
				}
				else
				{
					archetype = true;
				}
			}
			
			// object identity.
			if (!currentType(TSKernel.TYPE_IDENTIFIER))
			{
				addErrorMessage("Expected object identity.");
				return false;
			}
		
			String identity = currentToken().getLexeme();
			nextToken();
			
			verbosef("Read object %s...", identity);
		
			TObject object = currentModule.getObjectByIdentity(identity);
			if (object == null)
			{
				if (extendMode)
				{
					addErrorMessage("Keyword \"extend\" used on first declaration of object \""+identity+"\".");
					return false;
				}
				else
				{
					object = new TObject(identity);
					// archetype can only be set, never removed.
					if (archetype)
						object.setArchetype(true);
		
					// parse parent clause if any.
					if (!parseObjectParent(object))
						return false;
		
					if (!parseObjectNames(object))
						return false;
		
					if (!parseObjectTags(object))
						return false;
		
					currentModule.addObject(object);
				}
			}
			else if (!extendMode)
			{
				addErrorMessage("Object \""+identity+"\" already declared - must have \"extend\" before it.");
				return false;
			}
									
			// no body?
			if (matchType(TSKernel.TYPE_SEMICOLON))
				return true;
		
			// check for body.
			if (!matchType(TSKernel.TYPE_LBRACE))
			{
				addErrorMessage("Expected \"{\" for object body start or \";\" (no body).");
				return false;
			}
		
			if (!parseElementBody(object, "object"))
				return false;
			
			if (!matchType(TSKernel.TYPE_RBRACE))
			{
				addErrorMessage("Expected end-of-object \"}\".");
				return false;
			}
		
			return true;
		}

		/**
		 * Parses the object parent clause.
		 * [ObjectParent] :=
		 * 		":" [OBJECT]
		 * 		[e]
		 */
		private boolean parseObjectParent(TObject object)
		{
			if (matchType(TSKernel.TYPE_COLON))
			{
				if (!isObject(true))
				{
					addErrorMessage("Expected object type after \":\".");
					return false;
				}
				
				String identity = currentToken().getLexeme();
				nextToken();
				TObject parent = currentModule.getObjectByIdentity(identity);
				
				if (object.getParent() != null)
				{
					addErrorMessage("Object already has a parent.");
					return false;
				}
				
				object.setParent(parent);
			}
			
			return true;
		}

		/**
		 * Parses the container body.
		 * [ElementBody] :=
		 * 		[FUNCTION] [STRING] [FunctionArgumentList] [FunctionBody]
		 * 		[ElementBlock] [ElementBody]
		 * 		[e]
		 * @param element the element to add to. 
		 * @param elementTypeName the type name, for error message and the like.
		 */
		private boolean parseElementBody(TElement element, String elementTypeName)
		{
			BlockEntryType entryType;
			while (true)
			{
				boolean override = false;
				
				// get override.
				if (matchType(TSKernel.TYPE_OVERRIDE))
					override = true;
				
				// test if function.
				if (matchType(TSKernel.TYPE_FUNCTION))
				{
					if (!parseFunction(element, override))
						return false;
				}
				// test if other entry part.
				else if ((entryType = parseElementBlockType(element, elementTypeName)) != null)
				{
					if (!parseElementBlock(element, elementTypeName, entryType, override))
						return false;
				}
				else
					break;
			}
			
			return true;
		}

		/*
		 * Parses element entry block.
		 * [Function] := [IDENTIFIER] [FunctionArgumentList] [Block]
		 */
		private boolean parseElementBlock(TElement element, String elementTypeName, BlockEntryType entryType, boolean override) 
		{
			BlockEntry entry;
			if ((entry = parseBlockEntry(entryType)) == null)
				return false;
			
			if (element.getBlock(entry) != null)
			{
				addErrorMessage("Entry " + entry.toFriendlyString() + " was already defined on this "+elementTypeName+".");
				return false;
			}
			else if (element.resolveBlock(entry) != null && !override)
			{
				addErrorMessage("Entry " + entry.toFriendlyString() + " was already defined on a parent "+elementTypeName+" - must declare with \"override\".");
				return false;
			}
			else if (element.resolveBlock(entry) == null && override)
			{
				addErrorMessage("Entry " + entry.toFriendlyString() + " was never defined on a parent "+elementTypeName+" - do not declare with \"override\".");
				return false;
			}

			Block block;
			if ((block = parseBlock(element)) == null)
				return false;
			
			element.addBlock(entry, block);
			return true;
		}

		/*
		 * Parses function name and body.
		 * [Function] := [IDENTIFIER] [FunctionArgumentList] [Block]
		 */
		private boolean parseFunction(TElement element, boolean override) 
		{
			if (!currentType(TSKernel.TYPE_IDENTIFIER) || !isVariable())
			{
				addErrorMessage("Expected identifier for function name - cannot be an identifier for an existing element.");
				return false;
			}
			
			String functionName = currentToken().getLexeme();

			// cannot be the name of a visible command.
			TAMECommand command = getCommand(functionName);
			if (command != null && !command.isInternal())
			{
				addErrorMessage("Function name cannot be a command.");
				return false;
			}

			// cannot be re-declared on the same element.
			if (element.getFunction(functionName) != null)
			{
				addErrorMessage("Function \""+functionName+"\" was already declared on this element.");
				return false;
			}
			// test if on hierarchy
			if (element.resolveFunction(functionName) != null && !override)
			{
				addErrorMessage("Function \""+functionName+"\" already declared in a parent element - must declare with \"override\".");
				return false;
			}
			// test if not on hierarchy
			if (element.resolveFunction(functionName) == null && override)
			{
				addErrorMessage("Function \""+functionName+"\" not declared in a parent element - do not declare with \"override\".");
				return false;
			}
			
			nextToken();
			
			String[] arguments;
			if ((arguments = parseFunctionArgumentList(functionName, element.resolveFunction(functionName))) == null)
				return false;
			
			// must be added to enable recursion.
			FunctionEntry functionEntry = FunctionEntry.create(arguments);
			element.addFunction(functionName, functionEntry);
			
			functionDepth++;
			
			Block functionBlock;
			if ((functionBlock = parseBlock(element)) == null)
				return false;

			functionDepth--;
			
			functionEntry.setBlock(functionBlock);
			return true;
		}

		// Return type if token type is a valid block type on a container.
		private BlockEntryType parseElementBlockType(TElement element, String elementTypeName)
		{
			BlockEntryType entryType = parseBlockEntryType(elementTypeName);
			if (entryType == null)
				return null;
				
			if (!element.isValidEntryType(entryType))
			{
				addErrorMessage("Entry name \""+entryType.name()+"\"is not valid for a "+elementTypeName+".");
				return null;
			}
			
			return entryType;
		}

		// Returns a parsed block entry type.
		private BlockEntryType parseBlockEntryType(String elementTypeName)
		{
			if (!currentType(TSKernel.TYPE_IDENTIFIER))
				return null;
			
			// if identifier, check if entry block type.
			String lexeme = currentToken().getLexeme();
			BlockEntryType entryType; 
			if ((entryType = TSKernel.BLOCKENTRYTYPE_MAP.get(lexeme)) == null)
			{
				addErrorMessage("Expected valid "+elementTypeName+" block entry name.");
				return null;
			}
			
			nextToken();
			return entryType;
		}

		// Return type if token type is a valid block type on a container.
		private BlockEntry parseBlockEntry(BlockEntryType type)
		{
			if (!matchType(TSKernel.TYPE_LPAREN))
			{
				addErrorMessage("Expected \"(\" after block entry name.");
				return null;
			}
		
			BlockEntry out;
			if ((out = parseBlockEntryArguments(type)) == null)
				return null;
		
			if (out.getValues().length < type.getMinimumArgumentLength())
			{
				addErrorMessage("Expected ',' after entry argument. More arguments remain.");
				return null;
			}
			
			if (!matchType(TSKernel.TYPE_RPAREN))
			{
				addErrorMessage("Expected \")\" after block entry arguments.");
				return null;
			}
			
			return out; 
		}

		// Parses the block entry arguments into a BlockEntry.
		private BlockEntry parseBlockEntryArguments(BlockEntryType type)
		{
			Queue<Value> parsedValues = new Queue<>();
			ArgumentType[] argTypes = type.getArgumentTypes();
			for (int i = 0; i < argTypes.length; i++) 
			{
				if (!currentType(TSKernel.TYPE_IDENTIFIER) && !isValidLiteralType())
					break;
				
				switch (argTypes[i])
				{
					default:
					case VALUE:
					{
						if (!isValidLiteralType())
						{
							addErrorMessage("Entry requires a literal value. \""+currentToken().getLexeme()+"\" is not a valid literal!");
							return null;
						}
						
						parsedValues.enqueue(tokenToValue());
						nextToken();
						break;
					}
					case ACTION:
					{
						if (!isAction())
						{
							addErrorMessage("Entry requires an ACTION. \""+currentToken().getLexeme()+"\" is not an action type.");
							return null;
						}
						
						parsedValues.enqueue(tokenToValue());
						nextToken();
						break;
					}
					case OBJECT:
					{
						if (!isObject(false))
						{
							addErrorMessage("Entry requires a non-archetype OBJECT. \""+currentToken().getLexeme()+"\" is not a viable object type.");
							return null;
						}
						
						parsedValues.enqueue(tokenToValue());
						nextToken();
						break;
					}
					case OBJECT_ANY:
					{
						if (!isObject(true))
						{
							addErrorMessage("Entry requires an OBJECT. \""+currentToken().getLexeme()+"\" is not a viable object type.");
							return null;
						}
						
						parsedValues.enqueue(tokenToValue());
						nextToken();
						break;
					}
					case PLAYER:
					{
						if (!isPlayer(false))
						{
							addErrorMessage("Entry requires a non-archetype PLAYER. \""+currentToken().getLexeme()+"\" is not a viable player type.");
							return null;
						}
						
						parsedValues.enqueue(tokenToValue());
						nextToken();
						break;
					}
					case ROOM:
					{
						if (!isRoom(false))
						{
							addErrorMessage("Entry requires a non-archetype ROOM. \""+currentToken().getLexeme()+"\" is not a viable room type.");
							return null;
						}
						
						parsedValues.enqueue(tokenToValue());
						nextToken();
						break;
					}
					case CONTAINER:
					{
						if (!isContainer(false))
						{
							addErrorMessage("Entry requires a non-archetype CONTAINER. \""+currentToken().getLexeme()+"\" is not a viable container type.");
							return null;
						}
						
						parsedValues.enqueue(tokenToValue());
						nextToken();
						break;
					}
					case OBJECT_CONTAINER:
					{
						if (!isObjectContainer(false))
						{
							addErrorMessage("Entry requires a non-archetype CONTAINER-TYPE. \""+currentToken().getLexeme()+"\" is not a viable object container type.");
							return null;
						}
						
						parsedValues.enqueue(tokenToValue());
						nextToken();
						break;
					}
					case ELEMENT:
					{
						if (!isElement(false))
						{
							addErrorMessage("Entry requires a non-archetype ELEMENT-TYPE. \""+currentToken().getLexeme()+"\" is not a viable element type.");
							return null;
						}
						
						parsedValues.enqueue(tokenToValue());
						nextToken();
						break;
					}
					
				} // switch
				
				if (i < argTypes.length - 1)
				{
					if (!matchType(TSKernel.TYPE_COMMA))
						break;
				}
				
			} // for
			
			Value[] values = new Value[parsedValues.size()];
			parsedValues.toArray(values);
			return BlockEntry.create(type, values);
		}

		// Parses a function's argument list, returns the list of arguments, null if bad.
		private String[] parseFunctionArgumentList(String functionName, FunctionEntry overriddenEntry)
		{
			List<String> argList = new List<>(4);
			
			if (!matchType(TSKernel.TYPE_LPAREN))
			{
				addErrorMessage("Expected \"(\" to start function argument list.");
				return null;
			}
			
			while (currentType(TSKernel.TYPE_IDENTIFIER) && isVariable())
			{
				argList.add(currentToken().getLexeme());
				nextToken();
				
				if (!matchType(TSKernel.TYPE_COMMA))
					break;
			}
		
			if (!matchType(TSKernel.TYPE_RPAREN))
			{
				addErrorMessage("Expected \")\" to end function argument list, or a variable name.");
				return null;
			}
			
			if (overriddenEntry != null && argList.size() != overriddenEntry.getArguments().length)
			{
				addErrorMessage("Overridden function \""+functionName+"\" must have the same amount of arguments as its declaration in parent elements.");
				return null;
			}
			
			String[] out = new String[argList.size()];
			argList.toArray(out);
			return out;
		}

		/**
		 * Parses an action clause (after "action").
		 * 		[GENERAL] [IDENTIFIER] [ActionNames] ";"
		 * 		[OPEN] [IDENTIFIER] [ActionNames] [LOCAL] [IDENTIFIER] ";"
		 * 		[MODAL] [IDENTIFIER] [ActionNames] [ActionAdditionalNames] ";"
		 * 		[TRANSITIVE] [IDENTIFIER] [ActionNames] ";"
		 * 		[DITRANSITIVE] [IDENTIFIER] [ActionNames] [ActionAdditionalNames] ";"
		 */
		private boolean parseAction()
		{
			boolean restricted = false;
			boolean strict = false;
			boolean reversed = false;

			// can happen in any order
			while (currentType(TSKernel.TYPE_RESTRICTED, TSKernel.TYPE_STRICT, TSKernel.TYPE_REVERSED))
			{
				if (matchType(TSKernel.TYPE_RESTRICTED))
					restricted = true;
				if (matchType(TSKernel.TYPE_STRICT))
					strict = true;
				if (matchType(TSKernel.TYPE_REVERSED))
					reversed = true;
			}
			
			if (currentType(TSKernel.TYPE_GENERAL))
			{
				TAction.Type actionType = TAction.Type.GENERAL;
				nextToken();

				if (!isVariable())
				{
					addErrorMessage("Identity "+currentToken().getLexeme()+" is already declared.");
					return false;
				}

				String identity = currentToken().getLexeme();
				verbosef("Read general action %s...", identity);

				TAction action = new TAction(identity);
				action.setType(actionType);
				action.setRestricted(restricted);
				action.setStrict(strict);
				action.setReversed(reversed);
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

				String identity = currentToken().getLexeme();
				verbosef("Read open action %s...", identity);

				TAction action = new TAction(identity);
				action.setType(actionType);
				action.setRestricted(restricted);
				action.setStrict(strict);
				action.setReversed(reversed);
				nextToken();

				if (!parseActionNames(action))
					return false;

				if (!matchType(TSKernel.TYPE_USES))
				{
					addErrorMessage("Expected \"uses local\", to declare the target variable.");
					return false;
				}

				if (!matchType(TSKernel.TYPE_LOCAL))
				{
					addErrorMessage("Expected \"local\" after \"uses\" to declare the target variable.");
					return false;
				}

				if (!isVariable())
				{
					addErrorMessage("Expected non-keyword identifier for variable name.");
					return false;
				}
				
				String varname = currentToken().getLexeme();
				action.addExtraStrings(varname);
				nextToken();
				
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

				String identity = currentToken().getLexeme();
				verbosef("Read modal action %s...", identity);

				TAction action = new TAction(identity);
				action.setType(actionType);
				action.setRestricted(restricted);
				action.setStrict(strict);
				action.setReversed(reversed);
				nextToken();

				if (!parseActionNames(action))
					return false;
				
				if (!matchType(TSKernel.TYPE_USES))
				{
					addErrorMessage("Expected \"uses modes\" to declare modes.");
					return false;
				}

				if (!matchType(TSKernel.TYPE_MODES))
				{
					addErrorMessage("Expected \"modes\" after \"uses\" to declare modes.");
					return false;
				}

				if (!parseActionAdditionalNames(action))
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

				String identity = currentToken().getLexeme();
				verbosef("Read transitive action %s...", identity);

				TAction action = new TAction(identity);
				action.setType(actionType);
				action.setRestricted(restricted);
				action.setStrict(strict);
				action.setReversed(reversed);
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

				String identity = currentToken().getLexeme();
				verbosef("Read ditransitive action %s...", identity);

				TAction action = new TAction(identity);
				action.setType(actionType);
				action.setRestricted(restricted);
				action.setStrict(strict);
				action.setReversed(reversed);
				nextToken();

				if (!parseActionNames(action))
					return false;

				if (matchType(TSKernel.TYPE_USES))
				{
					if (!matchType(TSKernel.TYPE_CONJUNCTIONS))
					{
						addErrorMessage("Expected 'conjunctions' after 'uses'.");
						return false;
					}

					if (!parseActionAdditionalNames(action))
						return false;
				}

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
				if (!currentType(TSKernel.TYPE_STRING))
				{
					addErrorMessage("Expected action name (must be string).");
					return false;
				}
				
				action.addName(currentToken().getLexeme());
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
			if (matchType(TSKernel.TYPE_COMMA))
			{
				if (!currentType(TSKernel.TYPE_STRING))
				{
					addErrorMessage("Expected action name (must be string).");
					return false;
				}
				
				action.addName(currentToken().getLexeme());
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
		private boolean parseActionAdditionalNames(TAction action)
		{
			if (!currentType(TSKernel.TYPE_STRING))
			{
				addErrorMessage("Expected string.");
				return false;
			}
			
			action.addExtraStrings(currentToken().getLexeme());
			nextToken();
			
			return parseActionAdditionalNameList(action);
		}
		
		/**
		 * Parses action name list.
		 * [ActionAdditionalNameList] :=
		 * 		"," [STRING] [ActionAdditionalNameList]
		 * 		[e]
		 */
		private boolean parseActionAdditionalNameList(TAction action)
		{
			if (matchType(TSKernel.TYPE_COMMA))
			{
				if (!currentType(TSKernel.TYPE_STRING))
				{
					addErrorMessage("Expected name (must be string).");
					return false;
				}
				
				action.addExtraStrings(currentToken().getLexeme());
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
		private boolean parseActionPermissionClause(ForbiddenHandler element)
		{
			if (matchType(TSKernel.TYPE_FORBIDS))
			{
				element.setPermissionType(PermissionType.FORBID);
				
				if (!isAction())
				{
					addErrorMessage("Expected action after \"forbids\".");
					return false;
				}
				
				element.addPermissionAction(currentModule.getActionByIdentity(currentToken().getLexeme()));
				nextToken();
				
				return parseActionPermissionClauseList(element);
			}

			if (matchType(TSKernel.TYPE_ALLOWS))
			{
				element.setPermissionType(PermissionType.ALLOW);
				
				if (!isAction())
				{
					addErrorMessage("Expected action after \"allows\".");
					return false;
				}
				
				element.addPermissionAction(currentModule.getActionByIdentity(currentToken().getLexeme()));
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
		private boolean parseActionPermissionClauseList(ForbiddenHandler element)
		{
			if (matchType(TSKernel.TYPE_COMMA))
			{
				if (!isAction())
				{
					addErrorMessage("Expected action after \",\".");
					return false;
				}
				
				element.addPermissionAction(currentModule.getActionByIdentity(currentToken().getLexeme()));
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
				if (object.isArchetype())
				{
					addErrorMessage("Object archetypes cannot have names!");
					return false;
				}
				
				if (!currentType(TSKernel.TYPE_STRING))
				{
					addErrorMessage("Expected object name (must be string).");
					return false;
				}
				
				object.addName(currentToken().getLexeme());
				nextToken();
				
				if (!parseObjectNameList(object))
					return false;
				
				return parseObjectDeterminers(object);
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
			if (matchType(TSKernel.TYPE_COMMA))
			{
				if (!currentType(TSKernel.TYPE_STRING))
				{
					addErrorMessage("Expected object name (must be string).");
					return false;
				}
				
				object.addName(currentToken().getLexeme());
				nextToken();
				
				return parseObjectNameList(object);
			}
			
			return true;
		}

		/**
		 * Parses object determiners.
		 * [ObjectNames] := 
		 * 		[USES] [DETERMINERS] [STRING] [ObjectDeterminerList]
		 * 		[e]
		 */
		private boolean parseObjectDeterminers(TObject object)
		{
			if (matchType(TSKernel.TYPE_USES))
			{
				if (!matchType(TSKernel.TYPE_DETERMINERS))
				{
					addErrorMessage("Expected 'determiners' after 'uses'.");
					return false;
				}

				if (object.isArchetype())
				{
					addErrorMessage("Object archetypes cannot have determiners!");
					return false;
				}
				
				if (!currentType(TSKernel.TYPE_STRING))
				{
					addErrorMessage("Expected object name determiner (must be string).");
					return false;
				}
				
				object.addDeterminer(currentToken().getLexeme());
				nextToken();
				
				return parseObjectDeterminerList(object);
			}

			return true;
		}
		
		/**
		 * Parses object determiner list.
		 * [ObjectDeterminerList] :=
		 * 		"," [STRING] [ObjectDeterminerList]
		 * 		[e]
		 */
		private boolean parseObjectDeterminerList(TObject object)
		{
			if (matchType(TSKernel.TYPE_COMMA))
			{
				if (!currentType(TSKernel.TYPE_STRING))
				{
					addErrorMessage("Expected object name determiner (must be string).");
					return false;
				}
				
				object.addDeterminer(currentToken().getLexeme());
				nextToken();
				
				return parseObjectDeterminerList(object);
			}
			
			return true;
		}

		/**
		 * Parses object tags.
		 * [ObjectTags] := 
		 * 		[TAGGED] [STRING] [ObjectTagList]
		 * 		[e]
		 */
		private boolean parseObjectTags(TObject object)
		{
			if (matchType(TSKernel.TYPE_TAGGED))
			{
				if (object.isArchetype())
				{
					addErrorMessage("Object archetypes cannot have tags!");
					return false;
				}

				if (!currentType(TSKernel.TYPE_STRING))
				{
					addErrorMessage("Expected object tag (must be string).");
					return false;
				}
				
				object.addTag(currentToken().getLexeme());
				nextToken();
				
				return parseObjectTagList(object);
			}
			
			return true;
		}
		
		/**
		 * Parses object tag list.
		 * [ObjectTagList] :=
		 * 		"," [STRING] [ObjectTagList]
		 * 		[e]
		 */
		private boolean parseObjectTagList(TObject object)
		{
			if (matchType(TSKernel.TYPE_COMMA))
			{
				if (!currentType(TSKernel.TYPE_STRING))
				{
					addErrorMessage("Expected object tag (must be string).");
					return false;
				}
				
				object.addTag(currentToken().getLexeme());
				nextToken();
				
				return parseObjectTagList(object);
			}
			
			return true;
		}
		
		/**
		 * Parses a block.
		 * Returns a block or null of something went wrong.
		 * [Block] :=
		 * 		"{" [StatementList] "}"
		 * 		[Statement]
		 */
		private Block parseBlock(TElement currentElement)
		{
			Block out = new Block();
			
			if (currentType(TSKernel.TYPE_LBRACE))
			{
				nextToken();
				
				if (!parseStatementList(currentElement, out))
					return null;
				
				if (!matchType(TSKernel.TYPE_RBRACE))
				{
					addErrorMessage("Expected end of block '}'.");
					return null;
				}
				
				return optimizeBlock(out);
			}
			
			if (currentType(TSKernel.TYPE_SEMICOLON))
			{
				nextToken();
				return out;
			}
			
			// control block handling.
			if (currentType(TSKernel.TYPE_IF, TSKernel.TYPE_WHILE, TSKernel.TYPE_FOR))
			{
				if (!parseControl(currentElement, out))
					return null;
				
				return out;
			}
			
			if (!parseExecutableStatement(currentElement, out))
				return null;
			
			if (!matchType(TSKernel.TYPE_SEMICOLON))
			{
				addErrorMessage("Expected \";\" to terminate statement.");
				return null;
			}
			
			return optimizeBlock(out);
		}

		/**
		 * Parses a block that consists of only one statement.
		 * Pushes a block onto the block stack.
		 * [BlockStatement] :=
		 * 		[Statement]
		 */
		private Block parseBlockStatement(TElement currentElement)
		{
			Block out = new Block();
			
			if (!parseStatement(currentElement, out))
				return null;
			
			return optimizeBlock(out);
		}

		/**
		 * Parses a block that consists of the commands that evaluate an expression.
		 * Pushes a block onto the block stack.
		 * [BlockExpression] :=
		 * 		[Expression]
		 */
		private Block parseBlockExpression(TElement currentElement)
		{
			Block out = new Block();
			
			if (!parseExpression(currentElement, out))
				return null;
			
			return optimizeBlock(out);
		}
		
		/**
		 * Parses a statement. Emits commands to the provided block.
		 * [Statement] := 
		 *		[ELEMENTID] "." [VARIABLE] [ASSIGNMENTOPERATOR] [EXPRESSION]
		 * 		[IDENTIFIER] [ASSIGNMENTOPERATOR] [EXPRESSION]
		 * 		"local" [IDENTIFIER] [ASSIGNMENTOPERATOR] [EXPRESSION]
		 * 		"clear" [IDENTIFIER];
		 * 		[COMMANDEXPRESSION]
		 *		[e]
		 */
		private boolean parseStatement(TElement currentElement, Block block)
		{
			if (currentType(TSKernel.TYPE_IDENTIFIER) || isElement(false))
			{
				Value identToken = tokenToValue();

				if (identToken.isElement())
				{
					nextToken();

					// must have a dot if an element type.
					if (!matchType(TSKernel.TYPE_DOT))
					{
						addErrorMessage("Statement error - expected '.' to dereference an element.");
						return false;
					}
					
					if (!isVariable())
					{
						addErrorMessage("Expression error - expected variable or function name.");
						return false;
					}
					
					Value variable = tokenToValue();
					nextToken();
					String identName = variable.asString();

					// if function call...
					if (currentType(TSKernel.TYPE_LPAREN))
					{
						nextToken();
						TElement derefElement = currentModule.getElementByIdentity(identToken.asString());
						
						FunctionEntry functionEntry;
						if ((functionEntry = derefElement.resolveFunction(identName)) != null)
						{
							if (!parseFunctionCall(currentElement, functionEntry, block))
								return false;
							
							if (!matchType(TSKernel.TYPE_RPAREN))
							{
								addErrorMessage("Expression error - expected \")\".");
								return false;
							}

							block.add(Command.create(TAMECommand.CALLELEMENTFUNCTION, identToken, Value.create(identName)));
							block.add(Command.create(TAMECommand.POP));
							return true;
						}
						else
						{
							addErrorMessage("Expression error - no such function \""+identName+"\" in element \""+identToken.asString()+"\".");
							return false;
						}
					}
					
					// else, not function.
					
					if (!matchType(TSKernel.TYPE_EQUAL))
					{
						addErrorMessage("Statement error - expected assignment operator after variable.");
						return false;
					}

					if (!parseExpression(currentElement, block))
						return false;
					
					block.add(Command.create(TAMECommand.POPELEMENTVALUE, identToken, variable));
					return true;
				}
				else if (identToken.isVariable())
				{
					String identName = currentToken().getLexeme();
					nextToken();
					
					// if there's a left parenthesis, check for command.
					if (currentType(TSKernel.TYPE_LPAREN))
					{
						nextToken();
						
						TAMECommand command;
						FunctionEntry functionEntry;
						if ((command = getCommand(identName)) != null && !command.isInternal())
						{
							if (!parseCommandArguments(currentElement, block, command))
								return false;

							if (!matchType(TSKernel.TYPE_RPAREN))
							{
								addErrorMessage("Expression error - expected \")\".");
								return false;
							}
							
							block.add(Command.create(command));
							if(command.getReturnType() != null)
								block.add(Command.create(TAMECommand.POP));
							
							return true;
						}
						else if ((functionEntry = currentElement.resolveFunction(identName)) != null)
						{
							if (!parseFunctionCall(currentElement, functionEntry, block))
								return false;
							
							if (!matchType(TSKernel.TYPE_RPAREN))
							{
								addErrorMessage("Expression error - expected \")\".");
								return false;
							}

							block.add(Command.create(TAMECommand.CALLFUNCTION, Value.create(identName)));
							block.add(Command.create(TAMECommand.POP));
						}
						else
						{
							addErrorMessage("Expression error - \""+identName+"\" is not a command or the name of as function in this element's lineage.");
							return false;
						}
					}
					else if (matchType(TSKernel.TYPE_EQUAL))
					{
						if (!parseExpression(currentElement, block))
							return false;
						
						block.add(Command.create(TAMECommand.POPVALUE, identToken));
						return true;
					}
					else
					{
						addErrorMessage("Expression error - expected assignment operator.");
						return false;
					}
				}
				else
				{
					addErrorMessage("Statement error - expected variable or element identifier.");
					return false;
				}
				
			}
			else if (matchType(TSKernel.TYPE_LOCAL))
			{
				if (!currentType(TSKernel.TYPE_IDENTIFIER))
				{
					addErrorMessage("Statement error - expected variable after \"local\".");
					return false;
				}
				
				Value identToken = tokenToValue();
				nextToken();

				if (identToken.isElement())
				{
					addErrorMessage("Expression error - expected variable.");
					return false;
				}
				
				if (matchType(TSKernel.TYPE_EQUAL))
				{
					if (!parseExpression(currentElement, block))
						return false;
					
					block.add(Command.create(TAMECommand.POPLOCALVALUE, identToken));
					return true;
				}
				else
				{
					addErrorMessage("Expression error - expected assignment operator.");
					return false;
				}

			}
			else if (matchType(TSKernel.TYPE_CLEAR))
			{
				if (!currentType(TSKernel.TYPE_IDENTIFIER))
				{
					addErrorMessage("Statement error - expected element reference or variable after \"clear\".");
					return false;
				}
				
				Value identToken = tokenToValue();
				nextToken();

				if (identToken.isElement())
				{

					// must have a dot if an element type.
					if (!matchType(TSKernel.TYPE_DOT))
					{
						addErrorMessage("Statement error - expected '.' to dereference an element.");
						return false;
					}

					if (!isVariable())
					{
						addErrorMessage("Statement error - expected variable.");
						return false;
					}
					
					Value variable = tokenToValue();
					nextToken();
					
					block.add(Command.create(TAMECommand.CLEARELEMENTVALUE, identToken, variable));
					return true;
				}
				else
				{
					block.add(Command.create(TAMECommand.CLEARVALUE, identToken));
					return true;
				}
				
			}

			return true;
		}
		
		/**
		 * Parses a statement. block.adds commands to the current block.
		 * [StatementList] := 
		 *		[Statement] [StatementList]
		 * 		[e]
		 */
		private boolean parseStatementList(TElement currentElement, Block block)
		{
			if (!currentType(
					TSKernel.TYPE_SEMICOLON, 
					TSKernel.TYPE_IDENTIFIER,
					TSKernel.TYPE_LOCAL,
					TSKernel.TYPE_CLEAR,
					TSKernel.TYPE_WORLD,
					TSKernel.TYPE_PLAYER,
					TSKernel.TYPE_ROOM,
					TSKernel.TYPE_IF, 
					TSKernel.TYPE_WHILE, 
					TSKernel.TYPE_FOR,
					TSKernel.TYPE_QUIT,
					TSKernel.TYPE_END,
					TSKernel.TYPE_BREAK,
					TSKernel.TYPE_CONTINUE,
					TSKernel.TYPE_RETURN
				))
				return true;
			
			if (currentType(TSKernel.TYPE_SEMICOLON))
			{
				nextToken();
				return parseStatementList(currentElement, block);
			}
			
			// control block handling.
			if (currentType(TSKernel.TYPE_IF, TSKernel.TYPE_WHILE, TSKernel.TYPE_FOR))
			{
				if (!parseControl(currentElement, block))
					return false;
				
				return parseStatementList(currentElement, block);
			}
			
			if (!parseExecutableStatement(currentElement, block))
				return false;
			
			if (!matchType(TSKernel.TYPE_SEMICOLON))
			{
				addErrorMessage("Expected \";\" to terminate statement.");
				return false;
			}
			
			return parseStatementList(currentElement, block);
		}

		/**
		 * Parses a control block.
		 * [ControlBlock] :=
		 * 		[IF] "(" [EXPRESSION] ")" [BLOCK] [ELSE] [BLOCK]
		 * 		[WHILE] "(" [EXPRESSION] ")" [BLOCK]
		 * 		[FOR] "(" [STATEMENT] ";" [EXPRESSION] ";" [STATEMENT] ")" [BLOCK]
		 */
		private boolean parseControl(TElement currentElement, Block block) 
		{
			if (currentType(TSKernel.TYPE_IF))
			{
				nextToken();
				
				if (!matchType(TSKernel.TYPE_LPAREN))
				{
					addErrorMessage("Expected '(' after \"if\".");
					return false;
				}
		
				Block conditionalBlock;
				if ((conditionalBlock = parseBlockExpression(currentElement)) == null)
					return false;
				
				if (!matchType(TSKernel.TYPE_RPAREN))
				{
					addErrorMessage("Expected ')' after conditional expression.");
					return false;
				}
		
				Block successBlock;
				if ((successBlock = parseBlock(currentElement)) == null)
					return false;
				
				Block failureBlock = null;
				if (currentType(TSKernel.TYPE_ELSE))
				{
					nextToken();
		
					if ((failureBlock = parseBlock(currentElement)) == null)
						return false;
				}
		
				block.add(Command.create(TAMECommand.IF, conditionalBlock, successBlock, failureBlock));
				return true;
			}
			else if (currentType(TSKernel.TYPE_WHILE))
			{
				nextToken();
		
				if (!matchType(TSKernel.TYPE_LPAREN))
				{
					addErrorMessage("Expected '(' after \"while\".");
					return false;
				}
		
				Block conditionalBlock;
				if ((conditionalBlock = parseBlockExpression(currentElement)) == null)
					return false;
				
				if (!matchType(TSKernel.TYPE_RPAREN))
				{
					addErrorMessage("Expected ')' after conditional expression.");
					return false;
				}
		
				controlDepth++;
		
				Block successBlock;
				if ((successBlock = parseBlock(currentElement)) == null)
					return false;
		
				block.add(Command.create(TAMECommand.WHILE, conditionalBlock, successBlock));
				controlDepth--;
				return true;
			}
			else if (currentType(TSKernel.TYPE_FOR))
			{
				nextToken();
		
				if (!matchType(TSKernel.TYPE_LPAREN))
				{
					addErrorMessage("Expected '(' after \"for\".");
					return false;
				}
		
				Block initBlock;
				if ((initBlock = parseBlockStatement(currentElement)) == null)
					return false;
		
				if (!matchType(TSKernel.TYPE_SEMICOLON))
				{
					addErrorMessage("Expected ';' after inital statement in \"for\".");
					return false;
				}
		
				Block conditionalBlock;
				if ((conditionalBlock = parseBlockExpression(currentElement)) == null)
					return false;
				
				if (!matchType(TSKernel.TYPE_SEMICOLON))
				{
					addErrorMessage("Expected ';' after conditional statement in \"for\".");
					return false;
				}
		
				Block stepBlock;
				if ((stepBlock = parseBlockStatement(currentElement)) == null)
					return false;
		
				if (!matchType(TSKernel.TYPE_RPAREN))
				{
					addErrorMessage("Expected ')' after stepping statement in \"for\".");
					return false;
				}
		
				controlDepth++;
		
				Block successBlock;
				if ((successBlock = parseBlock(currentElement)) == null)
					return false;
		
				block.add(Command.create(TAMECommand.FOR, initBlock, conditionalBlock, stepBlock, successBlock));
				controlDepth--;
				return true;
			}
			else
			{
				addErrorMessage("INTERNAL ERROR!! CONTROL BLOCK: You should not see this!");
				return false;
			}	
		}

		/**
		 * Parses an executable statement.
		 * [ControlCommand] :=
		 * 		[QUIT]
		 * 		[BREAK]
		 * 		[CONTINUE]
		 * 		[END]
		 * 		[Statement]
		 */
		private boolean parseExecutableStatement(TElement currentElement, Block block) 
		{
			if (currentType(TSKernel.TYPE_QUIT))
			{
				nextToken();
				block.add(Command.create(TAMECommand.QUIT));
				return true;
			}
			else if (currentType(TSKernel.TYPE_FINISH))
			{
				nextToken();
				block.add(Command.create(TAMECommand.FINISH));
				return true;
			}
			else if (currentType(TSKernel.TYPE_END))
			{
				nextToken();
				block.add(Command.create(TAMECommand.END));
				return true;
			}
			else if (currentType(TSKernel.TYPE_RETURN))
			{
				nextToken();

				if (functionDepth == 0)
				{
					addErrorMessage("Command \"return\" used outside of a function.");
					return false;
				}
				
				if (!parseExpression(currentElement, block))
					return false;
				
				block.add(Command.create(TAMECommand.FUNCTIONRETURN));
				
				return true;
			}
			else if (currentType(TSKernel.TYPE_CONTINUE))
			{
				if (controlDepth == 0)
				{
					addErrorMessage("Command \"continue\" used without \"for\" or \"while\".");
					return false;
				}
				
				nextToken();
				block.add(Command.create(TAMECommand.CONTINUE));
				return true;
			}
			else if (currentType(TSKernel.TYPE_BREAK))
			{
				if (controlDepth == 0)
				{
					addErrorMessage("Command \"break\" used without \"for\" or \"while\".");
					return false;
				}
				
				nextToken();
				block.add(Command.create(TAMECommand.BREAK));
				return true;
			}
			else if (!parseStatement(currentElement, block))
				return false;
			
			return true;
		}

		/**
		 * Parses command arguments.
		 */
		private boolean parseCommandArguments(TElement currentElement, Block block, TAMECommand commandType) 
		{
			ArgumentType[] argTypes = commandType.getArgumentTypes();
			for (int n = 0; n < argTypes.length; n++) 
			{
				int i = n + 1;
				switch (argTypes[n])
				{
					default:
					case VALUE:
					{
						// value - read expression.
						if (!parseExpression(currentElement, block))
							return false;
						break;
					}
					case ACTION:
					{
						if (!isAction())
						{
							addErrorMessage("Command "+commandType.name()+" requires an ACTION for parameter "+i+". \""+currentToken().getLexeme()+"\" is not an action type.");
							return false;
						}
						
						block.add(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
						nextToken();
						break;
					}
					case ACTION_GENERAL:
					{
						if (!isAction())
						{
							addErrorMessage("Command "+commandType.name()+" requires an ACTION for parameter "+i+". \""+currentToken().getLexeme()+"\" is not an action type.");
							return false;
						}
						
						TAction action = currentModule.getActionByIdentity(currentToken().getLexeme());
						if (action.getType() != TAction.Type.GENERAL)
						{
							addErrorMessage("Command "+commandType.name()+" requires a GENERAL ACTION for parameter "+i+". \""+currentToken().getLexeme()+"\" is not a general action type.");
							return false;
						}
						
						block.add(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
						nextToken();
						break;
					}
					case ACTION_MODAL_OPEN:
					{
						if (!isAction())
						{
							addErrorMessage("Command "+commandType.name()+" requires an ACTION for parameter "+i+". \""+currentToken().getLexeme()+"\" is not an action type.");
							return false;
						}
						
						TAction action = currentModule.getActionByIdentity(currentToken().getLexeme());
						if (!(action.getType() == TAction.Type.MODAL || action.getType() == TAction.Type.OPEN))
						{
							addErrorMessage("Command "+commandType.name()+" requires a MODAL or OPEN ACTION for parameter "+i+". \""+currentToken().getLexeme()+"\" is not a modal or open action type.");
							return false;
						}
						
						block.add(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
						nextToken();
						break;
					}
					case ACTION_TRANSITIVE_DITRANSITIVE:
					{
						if (!isAction())
						{
							addErrorMessage("Command "+commandType.name()+" requires an ACTION for parameter "+i+". \""+currentToken().getLexeme()+"\" is not an action type.");
							return false;
						}
						
						TAction action = currentModule.getActionByIdentity(currentToken().getLexeme());
						if (!(action.getType() == TAction.Type.TRANSITIVE || action.getType() == TAction.Type.DITRANSITIVE))
						{
							addErrorMessage("Command "+commandType.name()+" requires a TRANSITIVE or DITRANSITIVE ACTION for parameter "+i+". \""+currentToken().getLexeme()+"\" is not a transitive or ditransitive action type.");
							return false;
						}
						
						block.add(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
						nextToken();
						break;
					}
					case ACTION_DITRANSITIVE:
					{
						if (!isAction())
						{
							addErrorMessage("Command "+commandType.name()+" requires an ACTION for parameter "+i+". \""+currentToken().getLexeme()+"\" is not an action type.");
							return false;
						}
						
						TAction action = currentModule.getActionByIdentity(currentToken().getLexeme());
						if (action.getType() != TAction.Type.DITRANSITIVE)
						{
							addErrorMessage("Command "+commandType.name()+" requires a DITRANSITIVE ACTION for parameter "+i+". \""+currentToken().getLexeme()+"\" is not a ditransitive action type.");
							return false;
						}
						
						block.add(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
						nextToken();
						break;
					}
					case OBJECT:
					{
						if (currentType(TSKernel.TYPE_THIS))
						{
							if (TObject.class.isAssignableFrom(currentElement.getClass()))
							{
								block.add(Command.create(TAMECommand.PUSHTHIS));
								nextToken();
							}
							else
							{
								addErrorMessage("Command "+commandType.name()+" requires a non-archetype OBJECT for parameter "+i+". \""+currentToken().getLexeme()+"\" is not a viable object type.");
								return false;
							}
						}
						else if (!isObject(false))
						{
							addErrorMessage("Command "+commandType.name()+" requires a non-archetype OBJECT for parameter "+i+". \""+currentToken().getLexeme()+"\" is not a viable object type.");
							return false;
						}
						else
						{
							block.add(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
							nextToken();
						}
						break;
					}
					case PLAYER:
					{
						if (currentType(TSKernel.TYPE_THIS))
						{
							if (TPlayer.class.isAssignableFrom(currentElement.getClass()))
							{
								block.add(Command.create(TAMECommand.PUSHTHIS));
								nextToken();
							}
							else
							{
								addErrorMessage("Command "+commandType.name()+" requires a non-archetype PLAYER for parameter "+i+". \""+currentToken().getLexeme()+"\" is not a viable player type, or \"player\".");
								return false;
							}
						}
						else if (!isPlayer(false))
						{
							addErrorMessage("Command "+commandType.name()+" requires a non-archetype PLAYER for parameter "+i+". \""+currentToken().getLexeme()+"\" is not a viable player type, or \"player\".");
							return false;
						}
						else
						{
							block.add(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
							nextToken();
						}
						break;
					}
					case ROOM:
					{
						if (currentType(TSKernel.TYPE_THIS))
						{
							if (TRoom.class.isAssignableFrom(currentElement.getClass()))
							{
								block.add(Command.create(TAMECommand.PUSHTHIS));
								nextToken();
							}
							else
							{
								addErrorMessage("Command "+commandType.name()+" requires a non-archetype ROOM for parameter "+i+". \""+currentToken().getLexeme()+"\" is not a viable room type, or \"room\".");
								return false;
							}
						}
						else if (!isRoom(false))
						{
							addErrorMessage("Command "+commandType.name()+" requires a non-archetype ROOM for parameter "+i+". \""+currentToken().getLexeme()+"\" is not a viable room type, or \"room\".");
							return false;
						}
						else
						{
							block.add(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
							nextToken();
						}
						break;
					}
					case CONTAINER:
					{
						if (currentType(TSKernel.TYPE_THIS))
						{
							if (TContainer.class.isAssignableFrom(currentElement.getClass()))
							{
								block.add(Command.create(TAMECommand.PUSHTHIS));
								nextToken();
							}
							else
							{
								addErrorMessage("Command "+commandType.name()+" requires a non-archetype CONTAINER for parameter "+i+". \""+currentToken().getLexeme()+"\" is not a viable container type.");
								return false;
							}
						}
						else if (!isContainer(false))
						{
							addErrorMessage("Command "+commandType.name()+" requires a non-archetype CONTAINER for parameter "+i+". \""+currentToken().getLexeme()+"\" is not a viable container type.");
							return false;
						}
						else
						{
							block.add(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
							nextToken();
						}
						break;
					}
					case OBJECT_CONTAINER:
					{
						if (currentType(TSKernel.TYPE_THIS))
						{
							if (ObjectContainer.class.isAssignableFrom(currentElement.getClass()))
							{
								block.add(Command.create(TAMECommand.PUSHTHIS));
								nextToken();
							}
							else
							{
								addErrorMessage("Command "+commandType.name()+" requires a non-archetype OBJECT-CONTAINER for parameter "+i+". \""+currentToken().getLexeme()+"\" is not an object container type (world, player, room, container).");
								return false;
							}
						}
						else if (!isObjectContainer(false))
						{
							addErrorMessage("Command "+commandType.name()+" requires a non-archetype OBJECT-CONTAINER for parameter "+i+". \""+currentToken().getLexeme()+"\" is not an object container type (world, player, room, container).");
							return false;
						}
						else
						{
							block.add(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
							nextToken();
						}
						break;
					}
					case ELEMENT:
					{
						if (currentType(TSKernel.TYPE_THIS))
						{
							block.add(Command.create(TAMECommand.PUSHTHIS));
							nextToken();
						}
						else if (!isElement(false))
						{
							addErrorMessage("Command "+commandType.name()+" requires a non-archetype ELEMENT for parameter "+i+". \""+currentToken().getLexeme()+"\" is not an element type (world, player, room, container, object).");
							return false;
						}
						else
						{
							block.add(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
							nextToken();
						}
						break;
					}
					case ELEMENT_ANY:
					{
						if (currentType(TSKernel.TYPE_THIS))
						{
							block.add(Command.create(TAMECommand.PUSHTHIS));
							nextToken();
						}
						else if (!isElement(true))
						{
							addErrorMessage("Command "+commandType.name()+" requires an ELEMENT for parameter "+i+". \""+currentToken().getLexeme()+"\" is not an element type (world, player, room, container, object).");
							return false;
						}
						else
						{
							block.add(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
							nextToken();
						}
						break;
					}
					
				} // switch
				
				if (n < argTypes.length - 1)
				{
					if (!matchType(TSKernel.TYPE_COMMA))
					{
						addErrorMessage("Expected ',' after command argument. More arguments remain.");
						return false;
					}
				}
				
			} // for
			
			return true;
		}

		/**
		 * Parses a function call.
		 */
		private boolean parseFunctionCall(TElement currentElement, FunctionEntry entry, Block block)
		{
			int argCount = entry.getArguments().length;
			while (argCount-- > 0)
			{
				if (!parseExpression(currentElement, block))
					return false;
				
				if (argCount > 0)
				{
					if (!matchType(TSKernel.TYPE_COMMA))
					{
						addErrorMessage("Expected ',' after function argument. More arguments remain.");
						return false;
					}
				}
			}
			
			return true;
		}
		
		/**
		 * Parses an infix expression.
		 * @param the block to block.add commands to.
		 */
		private boolean parseExpression(TElement currentElement, Block block)
		{
			// make stacks.
			Stack<ArithmeticOperator> expressionOperators = new Stack<>();
			int[] expressionValueCounter = new int[1];
			
			// was the last read token a value?
			boolean lastWasValue = false;
			boolean keepGoing = true;
			
			while (keepGoing)
			{
				if (currentType(TSKernel.TYPE_IDENTIFIER, TSKernel.TYPE_WORLD, TSKernel.TYPE_PLAYER, TSKernel.TYPE_ROOM))
				{
					if (lastWasValue)
					{
						addErrorMessage("Expression error - expected operator.");
						return false;
					}
					
					Value identToken = tokenToValue();

					if (identToken.isElement())
					{
						nextToken();
						
						// must have a dot if an element type.
						if (!matchType(TSKernel.TYPE_DOT))
						{
							addErrorMessage("Expression error - expected '.' to dereference an element.");
							return false;
						}
						
						if (!isVariable())
						{
							addErrorMessage("Expression error - expected variable or function name.");
							return false;
						}
						
						String identName = currentToken().getLexeme();
						nextToken();

						if (currentType(TSKernel.TYPE_LPAREN))
						{
							String elementName = identToken.asString();
							TElement derefElement = currentModule.getElementByIdentity(elementName);
							
							FunctionEntry functionEntry;
							// Not a real element.
							if (derefElement == null)
							{
								addErrorMessage("Expression error - functions can only be called from discrete elements (not \"player\" or \"room\").");
								return false;
							}
							else if ((functionEntry = derefElement.resolveFunction(identName)) != null)
							{
								// saw LPAREN
								nextToken();

								if (!parseFunctionCall(currentElement, functionEntry, block))
									return false;
								
								if (!matchType(TSKernel.TYPE_RPAREN))
								{
									addErrorMessage("Expression error - expected \")\".");
									return false;
								}
								
								block.add(Command.create(TAMECommand.CALLELEMENTFUNCTION, identToken, Value.create(identName)));
								expressionValueCounter[0] += 1; // push after call.
								lastWasValue = true;
							}
							else
							{
								addErrorMessage("Expression error - no such function \""+identName+"\" in element \""+elementName+"\".");
								return false;
							}
						}
						else
						{
							block.add(Command.create(TAMECommand.PUSHELEMENTVALUE, identToken, Value.createVariable(identName)));
							expressionValueCounter[0] += 1; // the "push"
							lastWasValue = true;
						}
						
					}
					else if (identToken.isVariable()) // or command...
					{
						String identName = currentToken().getLexeme();
						nextToken();
						
						// if there's a left parenthesis, check for command.
						if (currentType(TSKernel.TYPE_LPAREN))
						{
							TAMECommand command;
							FunctionEntry functionEntry;
							
							if ((command = getCommand(identName)) != null && !command.isInternal())
							{
								if (command.getReturnType() == null)
								{
									addErrorMessage("Expression error - command \""+identName+"\" has no return type.");
									return false;
								}
								
								// saw LPAREN
								nextToken();

								if (!parseCommandArguments(currentElement, block, command))
									return false;

								if (!matchType(TSKernel.TYPE_RPAREN))
								{
									addErrorMessage("Expression error - expected \")\".");
									return false;
								}
								
								block.add(Command.create(command));
								expressionValueCounter[0] += 1;
								lastWasValue = true;
							}
							else if ((functionEntry = currentElement.resolveFunction(identName)) != null)
							{
								// saw LPAREN
								nextToken();

								if (!parseFunctionCall(currentElement, functionEntry, block))
									return false;
								
								if (!matchType(TSKernel.TYPE_RPAREN))
								{
									addErrorMessage("Expression error - expected \")\".");
									return false;
								}

								block.add(Command.create(TAMECommand.CALLFUNCTION, Value.create(identName)));
								expressionValueCounter[0] += 1;
								lastWasValue = true;
							}
							else
							{
								addErrorMessage("Expression error - \""+identName+"\" is not a command nor function name.");
								return false;
							}
						}
						else
						{
							block.add(Command.create(TAMECommand.PUSHVALUE, identToken));
							expressionValueCounter[0] += 1;
							lastWasValue = true;
						}
					}
					else
					{
						addErrorMessage("Expression error - expected variable or element identifier.");
						return false;
					}
										
				}
				else if (matchType(TSKernel.TYPE_LPAREN))
				{
					if (lastWasValue)
					{
						addErrorMessage("Expression error - expected operator.");
						return false;
					}
					
					if (!parseExpression(currentElement, block))
						return false;
					
					// NOTE: Expression ends in a push.
					
					if (!matchType(TSKernel.TYPE_RPAREN))
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
					block.add(Command.create(TAMECommand.PUSHVALUE, value));
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
							case TSKernel.TYPE_PLUS:
								nextOperator = ArithmeticOperator.ADD;
								break;
							case TSKernel.TYPE_MINUS:
								nextOperator = ArithmeticOperator.SUBTRACT;
								break;
							case TSKernel.TYPE_STAR:
								nextOperator = ArithmeticOperator.MULTIPLY;
								break;
							case TSKernel.TYPE_SLASH:
								nextOperator = ArithmeticOperator.DIVIDE;
								break;
							case TSKernel.TYPE_PERCENT:
								nextOperator = ArithmeticOperator.MODULO;
								break;
							case TSKernel.TYPE_STARSTAR:
								nextOperator = ArithmeticOperator.POWER;
								break;
							case TSKernel.TYPE_AMPERSAND:
								nextOperator = ArithmeticOperator.LOGICAL_AND;
								break;
							case TSKernel.TYPE_PIPE:
								nextOperator = ArithmeticOperator.LOGICAL_OR;
								break;
							case TSKernel.TYPE_CARAT:
								nextOperator = ArithmeticOperator.LOGICAL_XOR;
								break;
							case TSKernel.TYPE_LESS:
								nextOperator = ArithmeticOperator.LESS;
								break;
							case TSKernel.TYPE_LESSEQUAL:
								nextOperator = ArithmeticOperator.LESS_OR_EQUAL;
								break;
							case TSKernel.TYPE_GREATER:
								nextOperator = ArithmeticOperator.GREATER;
								break;
							case TSKernel.TYPE_GREATEREQUAL:
								nextOperator = ArithmeticOperator.GREATER_OR_EQUAL;
								break;
							case TSKernel.TYPE_EQUAL2:
								nextOperator = ArithmeticOperator.EQUALS;
								break;
							case TSKernel.TYPE_EQUAL3:
								nextOperator = ArithmeticOperator.STRICT_EQUALS;
								break;
							case TSKernel.TYPE_NOTEQUAL:
								nextOperator = ArithmeticOperator.NOT_EQUALS;
								break;
							case TSKernel.TYPE_NOTEQUALEQUAL:
								nextOperator = ArithmeticOperator.STRICT_NOT_EQUALS;
								break;
							default:
								throw new TAMEScriptParseException("Internal error - unexpected binary operator miss.");
						}
						
						nextToken();

						if (!operatorReduce(block, expressionOperators, expressionValueCounter, nextOperator))
							return false;
						
						expressionOperators.push(nextOperator);
						lastWasValue = false;
					}
					// short-circuit and
					else if (matchType(TSKernel.TYPE_DOUBLEAMPERSAND))
					{
						// low priority - emit all commands.
						reduceRest(block, expressionOperators, expressionValueCounter);
						
						// no-op conditional - "if" pops and evaluates.
						Block conditional = new Block();
						conditional.add(Command.create(TAMECommand.NOOP));

						Block successBlock;
						if ((successBlock = parseBlockExpression(currentElement)) == null)
							return false;
						
						Block failureBlock = new Block();
						failureBlock.add(Command.create(TAMECommand.PUSHVALUE, Value.create(false)));

						block.add(Command.create(TAMECommand.IF, conditional, successBlock, failureBlock));

						lastWasValue = true;
					}
					// short-circuit or
					else if (matchType(TSKernel.TYPE_DOUBLEPIPE))
					{
						// low priority - emit all commands.
						reduceRest(block, expressionOperators, expressionValueCounter);

						// negate conditional - "if" pops and evaluates.
						Block conditional = new Block();
						conditional.add(Command.create(TAMECommand.ARITHMETICFUNC, Value.create(ArithmeticOperator.NEGATE.ordinal())));

						Block successBlock;
						if ((successBlock = parseBlockExpression(currentElement)) == null)
							return false;
						
						Block failureBlock = new Block();
						failureBlock.add(Command.create(TAMECommand.PUSHVALUE, Value.create(true)));

						block.add(Command.create(TAMECommand.IF, conditional, successBlock, failureBlock));

						lastWasValue = true;
					}
					// ternary operator
					else if (matchType(TSKernel.TYPE_QUESTIONMARK))
					{
						// low priority - emit all commands.
						reduceRest(block, expressionOperators, expressionValueCounter);

						// no-op conditional - "if" pops and evaluates.
						Block conditional = new Block();
						conditional.add(Command.create(TAMECommand.NOOP));

						Block successBlock;
						if ((successBlock = parseBlockExpression(currentElement)) == null)
							return false;
						
						if (!matchType(TSKernel.TYPE_COLON))
						{
							addErrorMessage("Expected \":\" after ternary branch expression.");
							return false;
						}

						Block failureBlock;
						if ((failureBlock = parseBlockExpression(currentElement)) == null)
							return false;

						block.add(Command.create(TAMECommand.IF, conditional, successBlock, failureBlock));

						lastWasValue = true;
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
						case TSKernel.TYPE_MINUS:
							expressionOperators.push(ArithmeticOperator.NEGATE);
							break;
						case TSKernel.TYPE_PLUS:
							expressionOperators.push(ArithmeticOperator.ABSOLUTE);
							break;
						case TSKernel.TYPE_EXCLAMATION:
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
					addErrorMessage("Expression error - expected value.");
					return false;
				}
				
			}
			
			if (!reduceRest(block, expressionOperators, expressionValueCounter))
				return false;
			
			if (expressionValueCounter[0] != 1)
			{
				addErrorMessage("Expected valid expression.");
				return false;
			}

			return true;
		}

		// reduces until there's nothing left to reduce.
		private boolean reduceRest(Block block, Stack<ArithmeticOperator> expressionOperators, int[] expressionValueCounter) 
		{
			// end of expression - reduce.
			while (!expressionOperators.isEmpty())
			{
				if (!expressionReduce(block, expressionOperators, expressionValueCounter))
					return false;
			}
			return true;
		}

		// Operator reduce.
		private boolean operatorReduce(Block block, Stack<ArithmeticOperator> expressionOperators, int[] expressionValueCounter, ArithmeticOperator operator) 
		{
			ArithmeticOperator top = expressionOperators.peek();
			while (top != null && (top.getPrecedence() > operator.getPrecedence() || (top.getPrecedence() == operator.getPrecedence() && !operator.isRightAssociative())))
			{
				if (!expressionReduce(block, expressionOperators, expressionValueCounter))
					return false;
				top = expressionOperators.peek();
			}
			
			return true;
		}

		// Expression reduce.
		private boolean expressionReduce(Block block, Stack<ArithmeticOperator> expressionOperators, int[] expressionValueCounter)
		{
			if (expressionOperators.isEmpty())
				throw new TAMEScriptParseException("Internal error - operator stack must have one operator in it.");

			ArithmeticOperator operator = expressionOperators.pop();
			
			if (operator.isBinary())
				expressionValueCounter[0] -= 2;
			else
				expressionValueCounter[0] -= 1;
			
			if (expressionValueCounter[0] < 0)
				throw new TAMEScriptParseException("Internal error - value counter did not have enough counter.");
			
			expressionValueCounter[0] += 1; // the "push"
			block.add(Command.create(TAMECommand.ARITHMETICFUNC, Value.create(operator.ordinal())));
			return true;
		}

		// Token to value.
		private Value tokenToValue()
		{
			if (isWorld())
				return Value.createWorld();
			else if (isPlayer(true))
				return Value.createPlayer(currentToken().getLexeme());
			else if (isRoom(true))
				return Value.createRoom(currentToken().getLexeme());
			else if (isObject(true))
				return Value.createObject(currentToken().getLexeme());
			else if (isContainer(true))
				return Value.createContainer(currentToken().getLexeme());
			else if (isAction())
				return Value.createAction(currentToken().getLexeme());
			else if (currentType(TSKernel.TYPE_STRING))
				return Value.create(currentToken().getLexeme());
			else if (currentType(TSKernel.TYPE_NUMBER))
			{
				String lexeme = currentToken().getLexeme();
				if (lexeme.startsWith("0X") || lexeme.startsWith("0x"))
					return Value.create(Long.parseLong(lexeme.substring(2), 16));
				else if (lexeme.contains("."))
					return Value.create(Double.parseDouble(lexeme));
				else
					return Value.create(Long.parseLong(lexeme));
			}
			else if (currentType(TSKernel.TYPE_IDENTIFIER))
				return Value.createVariable(currentToken().getLexeme());
			else if (currentType(TSKernel.TYPE_TRUE))
				return Value.create(true);
			else if (currentType(TSKernel.TYPE_FALSE))
				return Value.create(false);
			else if (currentType(TSKernel.TYPE_INFINITY))
				return Value.create(Double.POSITIVE_INFINITY);
			else if (currentType(TSKernel.TYPE_NAN))
				return Value.create(Double.NaN);
			else
				throw new TAMEScriptParseException("Internal error - unexpected token type.");
		}

		// Checks if an identifier is a variable.
		private boolean isVariable()
		{
			return !isWorld()
				&& !isPlayer(true)
				&& !isRoom(true)
				&& !isObject(true)
				&& !isContainer(true)
				&& !isAction()
			;
		}
		
		// Checks if an identifier is an element.
		private boolean isElement(boolean allowArchetype)
		{
			return isObject(allowArchetype)
				|| isObjectContainer(allowArchetype)
			;
		}

		// Checks if an identifier is an object container.
		private boolean isObjectContainer(boolean allowArchetype)
		{
			return isWorld()
				|| isPlayer(allowArchetype)
				|| isRoom(allowArchetype)
				|| isContainer(allowArchetype)
			;
		}

		// Checks if an identifier is a world.
		private boolean isWorld()
		{
			return currentToken().getType() == TSKernel.TYPE_WORLD;
		}
		
		// Checks if an identifier is a player.
		// If allowArchetype, it accepts an archetype reference.
		private boolean isPlayer(boolean allowArchetype)
		{
			if (currentToken().getType() == TSKernel.TYPE_PLAYER)
				return true;
			
			TPlayer player;
			if (currentToken().getType() == TSKernel.TYPE_IDENTIFIER && (player = currentModule.getPlayerByIdentity(currentToken().getLexeme())) != null)
			{
				if (!allowArchetype && player.isArchetype())
					return false;
				else
					return true;
			}
			else
				return false;
		}
		
		// Checks if an identifier is a room.
		// If allowArchetype, it accepts an archetype reference.
		private boolean isRoom(boolean allowArchetype)
		{
			TRoom room;
			if (currentToken().getType() == TSKernel.TYPE_ROOM)
				return true;
			
			if (currentToken().getType() == TSKernel.TYPE_IDENTIFIER && (room = currentModule.getRoomByIdentity(currentToken().getLexeme())) != null)
			{
				if (!allowArchetype && room.isArchetype())
					return false;
				else
					return true;
			}
			else
				return false;
		}
		
		// Checks if an identifier is an object.
		// If allowArchetype, it accepts an archetype reference.
		private boolean isObject(boolean allowArchetype)
		{
			TObject object;
			if (currentToken().getType() == TSKernel.TYPE_IDENTIFIER && (object = currentModule.getObjectByIdentity(currentToken().getLexeme())) != null)
			{
				if (!allowArchetype && object.isArchetype())
					return false;
				else
					return true;
			}
			else
				return false;
		}
		
		// Checks if an identifier is a container.
		// If allowArchetype, it accepts an archetype reference.
		private boolean isContainer(boolean allowArchetype)
		{
			TContainer container;
			if (currentToken().getType() == TSKernel.TYPE_IDENTIFIER && (container = currentModule.getContainerByIdentity(currentToken().getLexeme())) != null)
			{
				if (!allowArchetype && container.isArchetype())
					return false;
				else
					return true;
			}
			else
				return false;
		}
		
		// Checks if an identifier is an action.
		private boolean isAction()
		{
			return (currentToken().getType() == TSKernel.TYPE_IDENTIFIER && currentModule.getActionByIdentity(currentToken().getLexeme()) != null);
		}
		
		// Returns the command associated with a name, if any.
		private TAMECommand getCommand(String name)
		{
			return Reflect.getEnumInstance(name.toUpperCase(), TAMECommand.class);
		}
		
		// Return true if token type can be a unary operator.
		private boolean isValidLiteralType()
		{
			if (currentToken() == null)
				return false;
			
			switch (currentToken().getType())
			{
				case TSKernel.TYPE_STRING:
				case TSKernel.TYPE_NUMBER:
				case TSKernel.TYPE_TRUE:
				case TSKernel.TYPE_FALSE:
				case TSKernel.TYPE_INFINITY:
				case TSKernel.TYPE_NAN:
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
				case TSKernel.TYPE_MINUS:
				case TSKernel.TYPE_PLUS:
				case TSKernel.TYPE_EXCLAMATION:
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
				case TSKernel.TYPE_PLUS:
				case TSKernel.TYPE_MINUS:
				case TSKernel.TYPE_STAR:
				case TSKernel.TYPE_SLASH:
				case TSKernel.TYPE_PERCENT:
				case TSKernel.TYPE_AMPERSAND:
				case TSKernel.TYPE_PIPE:
				case TSKernel.TYPE_CARAT:
				case TSKernel.TYPE_GREATER:
				case TSKernel.TYPE_GREATEREQUAL:
				case TSKernel.TYPE_LESS:
				case TSKernel.TYPE_LESSEQUAL:
				case TSKernel.TYPE_EQUAL:
				case TSKernel.TYPE_EQUAL2:
				case TSKernel.TYPE_EQUAL3:
				case TSKernel.TYPE_NOTEQUAL:
				case TSKernel.TYPE_NOTEQUALEQUAL:
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
			
			boolean optimizeDone = false;
			Stack<Command> optimizeStack = new Stack<>();
			
			for (Command command : block)
			{
				if (command.getOperation() == TAMECommand.ARITHMETICFUNC)
				{
					ArithmeticOperator operator =  ArithmeticOperator.VALUES[(int)command.getOperand0().asLong()];
					
					// binary operator
					if (operator.isBinary())
					{
						Command c2 = optimizeStack.pop();
						Command c1 = optimizeStack.pop();

						if (c1.getOperation() == TAMECommand.PUSHVALUE && c2.getOperation() == TAMECommand.PUSHVALUE)
						{
							Value v2 = c2.getOperand0();
							Value v1 = c1.getOperand0();

							// if not literals, push back onto reduce stack
							if (!v1.isLiteral() || !v2.isLiteral())
							{
								optimizeStack.push(c1);
								optimizeStack.push(c2);
								optimizeStack.push(command);
							}
							// else reduce and push reduced value.
							else
							{
								Value result = operator.doOperation(v1, v2);
								optimizeStack.push(Command.create(TAMECommand.PUSHVALUE, result));
								optimizeDone = true;
							}
							
						}
						else
						{
							optimizeStack.push(c1);
							optimizeStack.push(c2);
							optimizeStack.push(command);
						}
						
					}
					// unary operator
					else
					{
						Command c1 = optimizeStack.pop();
						
						if (c1.getOperation() == TAMECommand.PUSHVALUE)
						{
							Value v1 = c1.getOperand0();

							// if not literals, push back onto reduce stack
							if (!v1.isLiteral())
							{
								optimizeStack.push(c1);
								optimizeStack.push(command);
							}
							// else reduce and push reduced value.
							else
							{
								Value result = operator.doOperation(v1);
								optimizeStack.push(Command.create(TAMECommand.PUSHVALUE, result));
								optimizeDone = true;
							}
							
						}
						else
						{
							optimizeStack.push(c1);
							optimizeStack.push(command);
						}
						
					}
				}
				else
				{
					optimizeStack.push(command);
				}
				
			}
			
			if (!optimizeDone)
				return block;
			
			Block outBlock = new Block();
			Stack<Command> reverseStack = new Stack<>();
			while (!optimizeStack.isEmpty())
				reverseStack.push(optimizeStack.pop());
			while (!reverseStack.isEmpty())
				outBlock.add(reverseStack.pop());
			
			return outBlock;
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
	 * Reads TAMEModule objects from a String of text into a new module.
	 * @param text the String to read from.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws TAMEScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if file is null. 
	 */
	public static TAMEModule read(String text) throws IOException
	{
		return read(STREAMNAME_TEXT, new StringReader(text), DEFAULT_OPTIONS, DEFAULT_INCLUDER);
	}

	/**
	 * Reads TAMEModule objects from a String of text into a new module.
	 * @param text the String to read from.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws TAMEScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if text is null. 
	 */
	public static TAMEModule read(String text, TAMEScriptIncluder includer) throws IOException
	{
		return read(STREAMNAME_TEXT, new StringReader(text), DEFAULT_OPTIONS, includer);
	}

	/**
	 * Reads TAMEModule objects from a String of text into a new module.
	 * @param text the String to read from.
	 * @param options the reader options for compiling.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if file is null. 
	 */
	public static TAMEModule read(String text, TAMEScriptReaderOptions options) throws IOException
	{
		return read(STREAMNAME_TEXT, new StringReader(text), options, DEFAULT_INCLUDER);
	}

	/**
	 * Reads TAMEModule objects from a String of text into a new module.
	 * @param text the String to read from.
	 * @param options the reader options for compiling.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if file is null. 
	 */
	public static TAMEModule read(String text, TAMEScriptReaderOptions options, TAMEScriptIncluder includer) throws IOException
	{
		return read(STREAMNAME_TEXT, new StringReader(text), includer);
	}

	/**
	 * Reads TAMEModule objects from a String of text into a new module.
	 * @param streamName a name to assign to the stream.
	 * @param text the String to read from.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws TAMEScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if file is null. 
	 */
	public static TAMEModule read(String streamName, String text) throws IOException
	{
		return read(streamName, new StringReader(text), DEFAULT_OPTIONS, DEFAULT_INCLUDER);
	}

	/**
	 * Reads TAMEModule objects from a String of text into a new module.
	 * @param streamName a name to assign to the stream.
	 * @param text the String to read from.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws TAMEScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if text is null. 
	 */
	public static TAMEModule read(String streamName, String text, TAMEScriptIncluder includer) throws IOException
	{
		return read(streamName, new StringReader(text), DEFAULT_OPTIONS, includer);
	}

	/**
	 * Reads TAMEModule objects from a String of text into a new module.
	 * @param streamName a name to assign to the stream.
	 * @param text the String to read from.
	 * @param options the reader options for compiling.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if file is null. 
	 */
	public static TAMEModule read(String streamName, String text, TAMEScriptReaderOptions options) throws IOException
	{
		return read(streamName, new StringReader(text), options, DEFAULT_INCLUDER);
	}

	/**
	 * Reads TAMEModule objects from a String of text into a new module.
	 * @param streamName a name to assign to the stream.
	 * @param text the String to read from.
	 * @param options the reader options for compiling.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws IOException if the stream can't be read.
	 * @throws NullPointerException if file is null. 
	 */
	public static TAMEModule read(String streamName, String text, TAMEScriptReaderOptions options, TAMEScriptIncluder includer) throws IOException
	{
		return read(streamName, new StringReader(text), includer);
	}

	/**
	 * Reads TAMEModule objects into a new module from a starting text file.
	 * @param file the file to read from.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws TAMEScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if file is null. 
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
	 * Reads TAMEModule objects into a new module from a starting text file.
	 * @param file	the file to read from.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws TAMEScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if file is null. 
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
	 * Reads TAMEModule objects into a new module from a starting text file.
	 * @param file	the file to read from.
	 * @param options the reader options for compiling.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws TAMEScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if file is null. 
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
	 * Reads TAMEModule objects into a new module from a starting text file.
	 * @param file	the file to read from.
	 * @param options the reader options for compiling.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws TAMEScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if file is null. 
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
	 * Reads TAMEModule objects into a new module.
	 * @param streamName the name of the stream.
	 * @param in the stream to read from.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws TAMEScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if in is null. 
	 */
	public static TAMEModule read(String streamName, InputStream in) throws IOException
	{
		return read(streamName, new InputStreamReader(in), DEFAULT_OPTIONS, DEFAULT_INCLUDER);
	}

	/**
	 * Reads TAMEModule objects into a new module.
	 * @param streamName the name of the stream.
	 * @param in the stream to read from.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws TAMEScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if in is null. 
	 */
	public static TAMEModule read(String streamName, InputStream in, TAMEScriptIncluder includer) throws IOException
	{
		return read(streamName, new InputStreamReader(in), DEFAULT_OPTIONS, includer);
	}

	/**
	 * Reads TAMEModule objects into a new module.
	 * @param streamName the name of the stream.
	 * @param in the stream to read from.
	 * @param options the reader options for compiling.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws TAMEScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if in is null. 
	 */
	public static TAMEModule read(String streamName, InputStream in, TAMEScriptReaderOptions options) throws IOException
	{
		return read(streamName, new InputStreamReader(in), options, DEFAULT_INCLUDER);
	}

	/**
	 * Reads TAMEModule objects into a new module.
	 * @param streamName the name of the stream.
	 * @param in the stream to read from.
	 * @param options the reader options for compiling.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws TAMEScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if in is null. 
	 */
	public static TAMEModule read(String streamName, InputStream in, TAMEScriptReaderOptions options, TAMEScriptIncluder includer) throws IOException
	{
		return read(streamName, new InputStreamReader(in), options, includer);
	}

	/**
	 * Reads TAMEModule objects into a new module from a reader stream.
	 * @param streamName the name of the stream.
	 * @param reader the reader to read from.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws TAMEScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if f is null. 
	 */
	public static TAMEModule read(String streamName, Reader reader) throws IOException
	{
		return read(streamName, reader, DEFAULT_OPTIONS, DEFAULT_INCLUDER);
	}

	/**
	 * Reads TAMEModule objects into a new module from a reader stream.
	 * @param streamName the name of the stream.
	 * @param reader the reader to read from.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws TAMEScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if file is null. 
	 */
	public static TAMEModule read(String streamName, Reader reader, TAMEScriptIncluder includer) throws IOException
	{
		return read(streamName, reader, DEFAULT_OPTIONS, includer);
	}

	/**
	 * Reads TAMEModule objects into a new module from a reader stream.
	 * @param streamName the name of the stream.
	 * @param reader the reader to read from.
	 * @param options the reader options for compiling.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws TAMEScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if reader is null. 
	 */
	public static TAMEModule read(String streamName, Reader reader, TAMEScriptReaderOptions options) throws IOException
	{
		return read(streamName, reader, options, DEFAULT_INCLUDER);
	}

	/**
	 * Reads TAMEModule objects into a new module from a reader stream.
	 * @param streamName the name of the stream.
	 * @param reader the reader to read from.
	 * @param options the reader options for compiling.
	 * @param includer the includer to use to resolve "included" paths.
	 * @return A new TAMEModule that contains all the read object hierarchy.
	 * @throws TAMEScriptParseException if one or more parse errors happen.
	 * @throws IOException if the stream can't be read.
	 * @throws SecurityException if a read error happens due to OS permissioning.
	 * @throws NullPointerException if reader is null. 
	 */
	public static TAMEModule read(String streamName, Reader reader, TAMEScriptReaderOptions options, TAMEScriptIncluder includer) throws IOException
	{
		return (new TSParser(new TSLexer(streamName, reader, includer), options)).readModule();
	}

}
