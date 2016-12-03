/*******************************************************************************
 * Copyright (c) 2016 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
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
import com.blackrook.lang.CommonLexer;
import com.blackrook.lang.CommonLexerKernel;
import com.blackrook.lang.Parser;
import com.blackrook.lang.ParserException;

import net.mtrop.tame.TAMECommand;
import net.mtrop.tame.TAMEConstants;
import net.mtrop.tame.TAMEModule;
import net.mtrop.tame.element.ForbiddenHandler;
import net.mtrop.tame.element.TAction;
import net.mtrop.tame.element.TContainer;
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
		static final int TYPE_BREAK =			16;
		static final int TYPE_CONTINUE =		17;

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
		static final int TYPE_TILDE =			42;
		static final int TYPE_EXCLAMATION =		43;
		static final int TYPE_STAR = 			44;
		static final int TYPE_STARSTAR = 		45;
		static final int TYPE_SLASH = 			46;
		static final int TYPE_PERCENT = 		47;
		static final int TYPE_AMPERSAND = 		48;
		static final int TYPE_AMPERSAND2 = 		49;
		static final int TYPE_PIPE = 			50;
		static final int TYPE_PIPE2 = 			51;
		static final int TYPE_CARAT = 			52;
		static final int TYPE_CARAT2 = 			53;	
		static final int TYPE_LESS = 			54;
		static final int TYPE_LESS2 = 			55;
		static final int TYPE_LESSEQUAL = 		56;
		static final int TYPE_GREATER =			57;
		static final int TYPE_GREATER2 = 		58;
		static final int TYPE_GREATER3 = 		59;
		static final int TYPE_GREATEREQUAL = 	60;
		static final int TYPE_EQUAL = 			61;
		static final int TYPE_EQUAL2 = 			62;
		static final int TYPE_EQUAL3 = 			63;
		static final int TYPE_NOTEQUAL = 		64;
		static final int TYPE_NOTEQUALEQUAL =	65;

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
		static final int TYPE_CONJOINS = 		85;
		static final int TYPE_FORBIDS = 		86;
		static final int TYPE_ALLOWS = 			87;
		static final int TYPE_RESTRICTED = 		88;
		static final int TYPE_LOCAL = 			89;
		static final int TYPE_ARCHETYPE = 		90;

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
			addDelimiter("+", TYPE_PLUS);
			addDelimiter("-", TYPE_MINUS);
			addDelimiter("!", TYPE_EXCLAMATION);
			addDelimiter("~", TYPE_TILDE);
			addDelimiter("*", TYPE_STAR);
			addDelimiter("**", TYPE_STARSTAR);
			addDelimiter("/", TYPE_SLASH);
			addDelimiter("%", TYPE_PERCENT);
			addDelimiter("&", TYPE_AMPERSAND);
			addDelimiter("&&", TYPE_AMPERSAND2);
			addDelimiter("|", TYPE_PIPE);
			addDelimiter("||", TYPE_PIPE2);
			addDelimiter("^", TYPE_CARAT);
			addDelimiter("^^", TYPE_CARAT2);
			addDelimiter("<", TYPE_LESS);
			addDelimiter("<<", TYPE_LESS2);
			addDelimiter("<=", TYPE_LESSEQUAL);
			addDelimiter(">", TYPE_GREATER);
			addDelimiter(">>", TYPE_GREATER2);
			addDelimiter(">>>", TYPE_GREATER3);
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
			addCaseInsensitiveKeyword("conjoins", TYPE_CONJOINS);
			addCaseInsensitiveKeyword("forbids", TYPE_FORBIDS);
			addCaseInsensitiveKeyword("allows", TYPE_ALLOWS);
			addCaseInsensitiveKeyword("local", TYPE_LOCAL);
			addCaseInsensitiveKeyword("archetype", TYPE_ARCHETYPE);

			for (BlockEntryType entryType : BlockEntryType.values())
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

		/** Current module. */
		private TAMEModule currentModule;
		/** Current block. */
		private Stack<Block> currentBlock;
		/** Control block count. */
		private int controlBlock;
		
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
				
				if (!matchType(TSKernel.TYPE_SEMICOLON))
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

				if (currentType(TSKernel.TYPE_STRING | TSKernel.TYPE_NUMBER))
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
		
		// Return type if token type is a valid block type on a container.
		private BlockEntryType parseContainerBlockType()
		{
			BlockEntryType entryType = parseBlockEntryType("container");
			if (entryType == null)
				return null;
				
			if (!TContainer.isValidEntryType(entryType))
			{
				addErrorMessage("Entry name \""+entryType.name()+"\"is not valid for a container.");
				return null;
			}
			
			return entryType;
		}

		// Return type if token type is a valid block type on an object.
		private BlockEntryType parseObjectBlockType()
		{
			BlockEntryType entryType = parseBlockEntryType("object");
			if (entryType == null)
				return null;
				
			if (!TObject.isValidEntryType(entryType))
			{
				addErrorMessage("Entry name \""+entryType.name()+"\" is not valid for an object.");
				return null;
			}
			
			return entryType;
		}

		// Return type if token type is a valid block type on a room.
		private BlockEntryType parseRoomBlockType()
		{
			BlockEntryType entryType = parseBlockEntryType("room");
			if (entryType == null)
				return null;
				
			if (!TRoom.isValidEntryType(entryType))
			{
				addErrorMessage("Entry name \""+entryType.name()+"\" is not valid for a room.");
				return null;
			}
			
			return entryType;
		}

		// Return type if token type is a valid block type on a player.
		private BlockEntryType parsePlayerBlockType()
		{
			BlockEntryType entryType = parseBlockEntryType("player");
			if (entryType == null)
				return null;
				
			if (!TPlayer.isValidEntryType(entryType))
			{
				addErrorMessage("Entry name \""+entryType.name()+"\" is not valid for a player.");
				return null;
			}
			
			return entryType;
		}

		// Return type if token type is a valid block type on a world.
		private BlockEntryType parseWorldBlockType()
		{
			BlockEntryType entryType = parseBlockEntryType("world");
			if (entryType == null)
				return null;
				
			if (!TWorld.isValidEntryType(entryType))
			{
				addErrorMessage("Entry name \""+entryType.name()+"\" is not valid for a world.");
				return null;
			}
			
			return entryType;
		}

		/**
		 * Parses a container.
		 * [Container] :=
		 * 		[IDENTIFIER] ";"
		 * 		[IDENTIFIER] "{" [ObjectBody] "}"
		 */
		private boolean parseContainer()
		{
			// container identity.
			if (!currentType(TSKernel.TYPE_IDENTIFIER))
			{
				addErrorMessage("Expected container identity.");
				return false;
			}

			String identity = currentToken().getLexeme();
			nextToken();

			TContainer container;
			if ((container = currentModule.getContainerByIdentity(identity)) == null)
			{
				container = new TContainer(identity);
				currentModule.addContainer(container);
			}

			verbosef("Read container %s...", identity);
			
			// prototype?
			if (matchType(TSKernel.TYPE_SEMICOLON))
				return true;

			if (!matchType(TSKernel.TYPE_LBRACE))
			{
				addErrorMessage("Expected \"{\" for container body start or \";\" (no body).");
				return false;
			}

			if (!parseContainerBody(container))
				return false;
			
			if (!matchType(TSKernel.TYPE_RBRACE))
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
			BlockEntryType entryType;
			BlockEntry entry;
			while ((entryType = parseContainerBlockType()) != null)
			{
				if ((entry = parseBlockEntry(entryType)) == null)
					return false;
				
				if (container.getBlock(entry) != null)
				{
					addErrorMessage(" Entry " + entry.toFriendlyString() + " was already defined on this container.");
					return false;
				}

				if (!parseBlock())
					return false;
				
				container.addBlock(entry, currentBlock.pop());
			}
			
			return true;
		}
				
		/**
		 * Parses an object.
		 * [Object] :=
		 * 		[IDENTIFIER] ";"
		 * 		[IDENTIFIER] "{" [ObjectBody] "}"
		 */
		private boolean parseObject()
		{
			boolean archetype = false;
			if (matchType(TSKernel.TYPE_ARCHETYPE))
				archetype = true;
			
			// object identity.
			if (!currentType(TSKernel.TYPE_IDENTIFIER))
			{
				addErrorMessage("Expected object identity.");
				return false;
			}

			String identity = currentToken().getLexeme();
			nextToken();
			
			verbosef("Read object %s...", identity);

			TObject object;
			if ((object = currentModule.getObjectByIdentity(identity)) == null)
			{
				object = new TObject(identity);
				// archetype can only be set, never removed.
				if (archetype)
					object.setArchetype(true);
				currentModule.addObject(object);
			}
			else if (object.isArchetype() && !archetype)
			{
				addErrorMessage("Object \""+identity+"\" must be re-declared as \"archetype\" in subsequent declarations!");
				return false;
			}
			else if (!object.isArchetype() && archetype)
			{
				addErrorMessage("Object \""+identity+"\" must be not be re-declared as \"archetype\" if it was never one at first declaration!");
				return false;
			}
									
			if (!parseObjectParent(object))
				return false;

			if (!parseObjectNames(object))
				return false;

			if (!parseObjectTags(object))
				return false;
			
			// no body?
			if (matchType(TSKernel.TYPE_SEMICOLON))
				return true;

			// check for body.
			if (!matchType(TSKernel.TYPE_LBRACE))
			{
				addErrorMessage("Expected \"{\" for object body start or \";\" (no body).");
				return false;
			}

			if (!parseObjectBody(object))
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
				object.setParent(currentModule.getObjectByIdentity(identity));
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
			BlockEntryType entryType;
			BlockEntry entry;
			while ((entryType = parseObjectBlockType()) != null)
			{
				if ((entry = parseBlockEntry(entryType)) == null)
					return false;
				
				if (object.getBlock(entry) != null)
				{
					addErrorMessage(" Entry " + entry.toFriendlyString() + " was already defined on this object.");
					return false;
				}

				if (!parseBlock())
					return false;
				
				object.addBlock(entry, currentBlock.pop());
			}
			
			return true;
		}
				
		/**
		 * Parses a room.
		 * [Room] :=
		 * 		[IDENTIFIER] ";"
		 * 		[IDENTIFIER] "{" [RoomBody] "}"
		 */
		private boolean parseRoom()
		{
			boolean archetype = false;
			if (matchType(TSKernel.TYPE_ARCHETYPE))
				archetype = true;

			// room identity.
			if (!currentType(TSKernel.TYPE_IDENTIFIER))
			{
				addErrorMessage("Expected room identity.");
				return false;
			}

			String identity = currentToken().getLexeme();
			nextToken();
			
			verbosef("Read room %s...", identity);

			TRoom room;
			if ((room = currentModule.getRoomByIdentity(identity)) == null)
			{
				room = new TRoom(identity);
				// archetype can only be set, never removed.
				if (archetype)
					room.setArchetype(true);
				currentModule.addRoom(room);
			}
			else if (room.isArchetype() && !archetype)
			{
				addErrorMessage("Room \""+identity+"\" must be re-declared as \"archetype\" in subsequent declarations!");
				return false;
			}
			else if (!room.isArchetype() && archetype)
			{
				addErrorMessage("Room \""+identity+"\" must be not be re-declared as \"archetype\" if it was never one at first declaration!");
				return false;
			}

															
			if (!parseRoomParent(room))
				return false;

			if (!parseActionPermissionClause(room))
				return false;
			
			// no body?
			if (matchType(TSKernel.TYPE_SEMICOLON))
				return true;

			// check for body.
			if (!matchType(TSKernel.TYPE_LBRACE))
			{
				addErrorMessage("Expected \"{\" for room body start or \";\" (no body).");
				return false;
			}

			if (!parseRoomBody(room))
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
				room.setParent(currentModule.getRoomByIdentity(identity));
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
			BlockEntryType entryType;
			BlockEntry entry;
			while ((entryType = parseRoomBlockType()) != null)
			{
				if ((entry = parseBlockEntry(entryType)) == null)
					return false;

				if (room.getBlock(entry) != null)
				{
					addErrorMessage(" Entry " + entry.toFriendlyString() + " was already defined on this room.");
					return false;
				}

				if (!parseBlock())
					return false;
				
				room.addBlock(entry, currentBlock.pop());
			}
			
			return true;
		}
				
		/**
		 * Parses a player.
		 * [Player] :=
		 * 		[IDENTIFIER] ";"
		 * 		[IDENTIFIER] "{" [PlayerBody] "}"
		 */
		private boolean parsePlayer()
		{
			boolean archetype = false;
			if (matchType(TSKernel.TYPE_ARCHETYPE))
				archetype = true;

			// player identity.
			if (!currentType(TSKernel.TYPE_IDENTIFIER))
			{
				addErrorMessage("Expected player identity.");
				return false;
			}

			String identity = currentToken().getLexeme();
			nextToken();
			
			verbosef("Read player %s...", identity);

			TPlayer player;
			if ((player = currentModule.getPlayerByIdentity(identity)) == null)
			{
				player = new TPlayer(identity);
				// archetype can only be set, never removed.
				if (archetype)
					player.setArchetype(true);
				currentModule.addPlayer(player);
			}
			else if (player.isArchetype() && !archetype)
			{
				addErrorMessage("Player \""+identity+"\" must be re-declared as \"archetype\" in subsequent declarations!");
				return false;
			}
			else if (!player.isArchetype() && archetype)
			{
				addErrorMessage("Player \""+identity+"\" must be not be re-declared as \"archetype\" if it was never one at first declaration!");
				return false;
			}
																		
			if (!parsePlayerParent(player))
				return false;

			if (!parseActionPermissionClause(player))
				return false;
			
			// no body?
			if (matchType(TSKernel.TYPE_SEMICOLON))
				return true;

			// check for body.
			if (!matchType(TSKernel.TYPE_LBRACE))
			{
				addErrorMessage("Expected \"{\" for player body start or \";\" (no body).");
				return false;
			}

			if (!parsePlayerBody(player))
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
				player.setParent(currentModule.getPlayerByIdentity(identity));
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
			BlockEntryType entryType;
			BlockEntry entry;
			while ((entryType = parsePlayerBlockType()) != null)
			{
				if ((entry = parseBlockEntry(entryType)) == null)
					return false;
				
				if (player.getBlock(entry) != null)
				{
					addErrorMessage(" Entry " + entry.toFriendlyString() + " was already defined on this player.");
					return false;
				}
				
				if (!parseBlock())
					return false;
				
				player.addBlock(entry, currentBlock.pop());
			}
			
			return true;
		}
		
		/**
		 * Parses a world.
		 * [World] :=
		 * 		";"
		 * 		"{" [WorldBody] "}"
		 */
		private boolean parseWorld()
		{
			verbose("Read world...");

			TWorld world;
			if ((world = currentModule.getWorld()) == null)
			{
				world = new TWorld();
				currentModule.setWorld(world);
			}
						
			// prototype?
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

			if (!parseWorldBody(world))
				return false;
			
			if (!matchType(TSKernel.TYPE_RBRACE))
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
			BlockEntryType entryType;
			BlockEntry entry;
			while ((entryType = parseWorldBlockType()) != null)
			{
				if ((entry = parseBlockEntry(entryType)) == null)
					return false;
				
				if (world.getBlock(entry) != null)
				{
					addErrorMessage(" Entry " + entry.toFriendlyString() + " was already defined on this world.");
					return false;
				}

				if (!parseBlock())
					return false;
				
				world.addBlock(entry, currentBlock.pop());
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
		private boolean parseAction()
		{
			boolean restricted = false;
			if (matchType(TSKernel.TYPE_RESTRICTED))
				restricted = true;
			
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

				String identity = currentToken().getLexeme();
				verbosef("Read modal action %s...", identity);

				TAction action = new TAction(identity);
				action.setType(actionType);
				action.setRestricted(restricted);
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

				String identity = currentToken().getLexeme();
				verbosef("Read transitive action %s...", identity);

				TAction action = new TAction(identity);
				action.setType(actionType);
				action.setRestricted(restricted);
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
				if (!currentType(TSKernel.TYPE_STRING))
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
			if (matchType(TSKernel.TYPE_COMMA))
			{
				if (!currentType(TSKernel.TYPE_STRING))
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
				if (!currentType(TSKernel.TYPE_STRING))
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
			if (matchType(TSKernel.TYPE_COMMA))
			{
				if (!currentType(TSKernel.TYPE_STRING))
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
		 * Pushes a block onto the block stack.
		 * [Block] :=
		 * 		"{" [StatementList] "}"
		 * 		[Statement]
		 */
		private boolean parseBlock()
		{
			currentBlock.push(new Block());
			
			if (currentType(TSKernel.TYPE_LBRACE))
			{
				nextToken();
				
				if (!parseStatementList())
				{
					currentBlock.pop();
					return false;
				}
				
				if (!matchType(TSKernel.TYPE_RBRACE))
				{
					currentBlock.pop();
					addErrorMessage("Expected end of block '}'.");
					return false;
				}
				
				currentBlock.push(optimizeBlock(currentBlock.pop()));
				return true;
			}
			
			if (currentType(TSKernel.TYPE_SEMICOLON))
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
			
			if (!parseExecutableStatement())
				return false;
			
			if (!matchType(TSKernel.TYPE_SEMICOLON))
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
		private boolean parseBlockStatement()
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
		private boolean parseBlockExpression()
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
		 * 		[IDENTIFIER] [ASSIGNMENTOPERATOR] [EXPRESSION]
		 * 		"local" [IDENTIFIER] [ASSIGNMENTOPERATOR] [EXPRESSION]
		 * 		[COMMANDEXPRESSION]
		 *		[e]
		 */
		private boolean parseStatement()
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
						addErrorMessage("Statement error - expected variable.");
						return false;
					}
					
					Value variable = tokenToValue();
					nextToken();
					
					if (!matchType(TSKernel.TYPE_EQUAL))
					{
						addErrorMessage("Statement error - expected assignment operator.");
						return false;
					}

					if (!parseExpression())
						return false;
					
					emit(Command.create(TAMECommand.POPELEMENTVALUE, identToken, variable));
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
						
						TAMECommand command = getCommand(identName);
						if (command == null || command.isInternal())
						{
							addErrorMessage("Expression error - \""+identName+"\" is not a command.");
							return false;
						}
						else
						{
							if (!parseCommandArguments(command))
								return false;

							if (!matchType(TSKernel.TYPE_RPAREN))
							{
								addErrorMessage("Expression error - expected \")\".");
								return false;
							}
							
							emit(Command.create(command));
							if(command.getReturnType() != null)
								emit(Command.create(TAMECommand.POPVALUE));
							
							return true;
						}
					}
					else if (matchType(TSKernel.TYPE_EQUAL))
					{
						if (!parseExpression())
							return false;
						
						emit(Command.create(TAMECommand.POPVALUE, identToken));
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
					if (!parseExpression())
						return false;
					
					emit(Command.create(TAMECommand.POPLOCALVALUE, identToken));
					return true;
				}
				else
				{
					addErrorMessage("Expression error - expected assignment operator.");
					return false;
				}

			}

			return true;
		}
		
		/**
		 * Parses a statement. Emits commands to the current block.
		 * [StatementList] := 
		 *		[Statement] [StatementList]
		 * 		[e]
		 */
		private boolean parseStatementList()
		{
			if (!currentType(
					TSKernel.TYPE_SEMICOLON, 
					TSKernel.TYPE_IDENTIFIER,
					TSKernel.TYPE_LOCAL,
					TSKernel.TYPE_WORLD,
					TSKernel.TYPE_PLAYER,
					TSKernel.TYPE_ROOM,
					TSKernel.TYPE_IF, 
					TSKernel.TYPE_WHILE, 
					TSKernel.TYPE_FOR,
					TSKernel.TYPE_QUIT,
					TSKernel.TYPE_END,
					TSKernel.TYPE_BREAK,
					TSKernel.TYPE_CONTINUE
				))
				return true;
			
			if (currentType(TSKernel.TYPE_SEMICOLON))
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
			
			if (!parseExecutableStatement())
				return false;
			
			if (!matchType(TSKernel.TYPE_SEMICOLON))
			{
				addErrorMessage("Expected \";\" to terminate statement.");
				return false;
			}
			
			return parseStatementList();
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
				
				if (!matchType(TSKernel.TYPE_LPAREN))
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
				
				if (!matchType(TSKernel.TYPE_RPAREN))
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
		
				if (!matchType(TSKernel.TYPE_LPAREN))
				{
					addErrorMessage("Expected '(' after \"while\".");
					return false;
				}
		
				Block conditionalBlock = null;
				Block successBlock = null;
				
				if (!parseBlockExpression())
					return false;
				conditionalBlock = currentBlock.pop();
				
				if (!matchType(TSKernel.TYPE_RPAREN))
				{
					addErrorMessage("Expected ')' after conditional expression.");
					return false;
				}
		
				controlBlock++;
		
				if (!parseBlock())
					return false;
				successBlock = currentBlock.pop();
		
				emit(Command.create(TAMECommand.WHILE, conditionalBlock, successBlock));
				controlBlock--;
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
		
				Block initBlock = null;
				Block conditionalBlock = null;
				Block stepBlock = null;
				Block successBlock = null;
				
				if (!parseBlockStatement())
					return false;
				initBlock = currentBlock.pop();
		
				if (!matchType(TSKernel.TYPE_SEMICOLON))
				{
					addErrorMessage("Expected ';' after inital statement in \"for\".");
					return false;
				}
		
				if (!parseBlockExpression())
					return false;
				conditionalBlock = currentBlock.pop();
				
				if (!matchType(TSKernel.TYPE_SEMICOLON))
				{
					addErrorMessage("Expected ';' after conditional statement in \"for\".");
					return false;
				}
		
				if (!parseBlockStatement())
					return false;
				stepBlock = currentBlock.pop();
		
				if (!matchType(TSKernel.TYPE_RPAREN))
				{
					addErrorMessage("Expected ')' after stepping statement in \"for\".");
					return false;
				}
		
				controlBlock++;
		
				if (!parseBlock())
					return false;
				successBlock = currentBlock.pop();
		
				emit(Command.create(TAMECommand.FOR, initBlock, conditionalBlock, stepBlock, successBlock));
				controlBlock--;
				return true;
			}
			else
			{
				addErrorMessage("INTERNAL ERROR!! CONTROL BLOCK: You should not see this!");
				return false;
			}	
		}

		/**
		 * Parses a control block.
		 * [ControlCommand] :=
		 * 		[QUIT]
		 * 		[BREAK]
		 * 		[CONTINUE]
		 * 		[END]
		 * 		[Statement]
		 */
		private boolean parseExecutableStatement() 
		{
			if (currentType(TSKernel.TYPE_QUIT))
			{
				nextToken();
				emit(Command.create(TAMECommand.QUIT));
				return true;
			}
			else if (currentType(TSKernel.TYPE_END))
			{
				nextToken();
				emit(Command.create(TAMECommand.END));
				return true;
			}
			else if (currentType(TSKernel.TYPE_CONTINUE))
			{
				if (controlBlock == 0)
				{
					addErrorMessage("Command \"continue\" used without \"for\" or \"while\".");
					return false;
				}
				
				nextToken();
				emit(Command.create(TAMECommand.CONTINUE));
				return true;
			}
			else if (currentType(TSKernel.TYPE_BREAK))
			{
				if (controlBlock == 0)
				{
					addErrorMessage("Command \"break\" used without \"for\" or \"while\".");
					return false;
				}
				
				nextToken();
				emit(Command.create(TAMECommand.BREAK));
				return true;
			}
			else if (!parseStatement())
				return false;
			
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
						if (!isObject(false))
						{
							addErrorMessage("Command requires a non-archetype OBJECT. \""+currentToken().getLexeme()+"\" is not a viable object type.");
							return false;
						}
						
						emit(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
						nextToken();
						break;
					}
					case PLAYER:
					{
						if (!isPlayer(false))
						{
							addErrorMessage("Command requires a non-archetype PLAYER. \""+currentToken().getLexeme()+"\" is not a viable player type.");
							return false;
						}
						
						emit(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
						nextToken();
						break;
					}
					case ROOM:
					{
						if (!isRoom(false))
						{
							addErrorMessage("Command requires a non-archetype ROOM. \""+currentToken().getLexeme()+"\" is not a viable room type.");
							return false;
						}
						
						emit(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
						nextToken();
						break;
					}
					case CONTAINER:
					{
						if (!isContainer(false))
						{
							addErrorMessage("Command requires a non-archetype CONTAINER. \""+currentToken().getLexeme()+"\" is not a viable container type.");
							return false;
						}
						
						emit(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
						nextToken();
						break;
					}
					case OBJECT_CONTAINER:
					{
						if (!isObjectContainer(false))
						{
							addErrorMessage("Command requires a non-archetype OBJECT-CONTAINER. \""+currentToken().getLexeme()+"\" is not an object container type.");
							return false;
						}
						
						emit(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
						nextToken();
						break;
					}
					case ELEMENT:
					{
						if (!isElement(false))
						{
							addErrorMessage("Command requires a non-archetype ELEMENT. \""+currentToken().getLexeme()+"\" is not an element type.");
							return false;
						}
						
						emit(Command.create(TAMECommand.PUSHVALUE, tokenToValue()));
						nextToken();
						break;
					}
					
				} // switch
				
				if (i < argTypes.length - 1)
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
		 * Parses an infix expression.
		 */
		private boolean parseExpression()
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
							addErrorMessage("Expression error - expected variable.");
							return false;
						}
						
						emit(Command.create(TAMECommand.PUSHELEMENTVALUE, identToken, Value.createVariable(currentToken().getLexeme())));
						expressionValueCounter[0] += 1; // the "push"
						lastWasValue = true;
						nextToken();
					}
					else if (identToken.isVariable()) // or command...
					{
						String identName = currentToken().getLexeme();
						nextToken();
						
						// if there's a left parenthesis, check for command.
						if (currentType(TSKernel.TYPE_LPAREN))
						{
							nextToken();
							TAMECommand command = getCommand(identName);
							if (command == null || command.isInternal())
							{
								addErrorMessage("Expression error - \""+identName+"\" is not a command.");
								return false;
							}
							else if (command.getReturnType() == null)
							{
								addErrorMessage("Expression error - command \""+identName+"\" has no return type.");
								return false;
							}
							else
							{
								if (!parseCommandArguments(command))
									return false;

								if (!matchType(TSKernel.TYPE_RPAREN))
								{
									addErrorMessage("Expression error - expected \")\".");
									return false;
								}
								
								emit(Command.create(command));
								expressionValueCounter[0] += 1;
								lastWasValue = true;
							}
						}
						else
						{
							emit(Command.create(TAMECommand.PUSHVALUE, identToken));
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
					
					if (!parseExpression())
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
								nextOperator = ArithmeticOperator.AND;
								break;
							case TSKernel.TYPE_AMPERSAND2:
								nextOperator = ArithmeticOperator.LOGICAL_AND;
								break;
							case TSKernel.TYPE_PIPE:
								nextOperator = ArithmeticOperator.OR;
								break;
							case TSKernel.TYPE_PIPE2:
								nextOperator = ArithmeticOperator.LOGICAL_OR;
								break;
							case TSKernel.TYPE_CARAT:
								nextOperator = ArithmeticOperator.XOR;
								break;
							case TSKernel.TYPE_CARAT2:
								nextOperator = ArithmeticOperator.LOGICAL_XOR;
								break;
							case TSKernel.TYPE_LESS:
								nextOperator = ArithmeticOperator.LESS;
								break;
							case TSKernel.TYPE_LESS2:
								nextOperator = ArithmeticOperator.LSHIFT;
								break;
							case TSKernel.TYPE_LESSEQUAL:
								nextOperator = ArithmeticOperator.LESS_OR_EQUAL;
								break;
							case TSKernel.TYPE_GREATER:
								nextOperator = ArithmeticOperator.GREATER;
								break;
							case TSKernel.TYPE_GREATER2:
								nextOperator = ArithmeticOperator.RSHIFT;
								break;
							case TSKernel.TYPE_GREATER3:
								nextOperator = ArithmeticOperator.RSHIFTPAD;
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
						case TSKernel.TYPE_MINUS:
							expressionOperators.push(ArithmeticOperator.NEGATE);
							break;
						case TSKernel.TYPE_PLUS:
							expressionOperators.push(ArithmeticOperator.ABSOLUTE);
							break;
						case TSKernel.TYPE_TILDE:
							expressionOperators.push(ArithmeticOperator.NOT);
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
				case TSKernel.TYPE_TILDE:
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
				case TSKernel.TYPE_AMPERSAND2:
				case TSKernel.TYPE_PIPE:
				case TSKernel.TYPE_PIPE2:
				case TSKernel.TYPE_CARAT:
				case TSKernel.TYPE_CARAT2:
				case TSKernel.TYPE_GREATER:
				case TSKernel.TYPE_GREATER2:
				case TSKernel.TYPE_GREATER3:
				case TSKernel.TYPE_GREATEREQUAL:
				case TSKernel.TYPE_LESS:
				case TSKernel.TYPE_LESS2:
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
