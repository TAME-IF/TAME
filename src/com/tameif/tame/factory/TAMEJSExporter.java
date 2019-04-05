/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * 
 * See AUTHORS.TXT for full credits.
 ******************************************************************************/
package com.tameif.tame.factory;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;

import com.blackrook.commons.ObjectPair;
import com.blackrook.commons.util.EncodingUtils;
import com.blackrook.commons.util.IOUtils;
import com.blackrook.lang.json.JSONObject;
import com.blackrook.lang.json.JSONWriter;
import com.tameif.tame.TAMEConstants;
import com.tameif.tame.TAMELogic;
import com.tameif.tame.TAMEModule;
import com.tameif.tame.TAMEModule.Header;
import com.tameif.tame.element.TAction;
import com.tameif.tame.element.TContainer;
import com.tameif.tame.element.TObject;
import com.tameif.tame.element.TPlayer;
import com.tameif.tame.element.TRoom;
import com.tameif.tame.element.TWorld;
import com.tameif.tame.exception.JSExportException;
import com.tameif.tame.lang.Block;
import com.tameif.tame.lang.BlockEntry;
import com.tameif.tame.lang.Operation;
import com.tameif.tame.lang.FunctionEntry;
import com.tameif.tame.lang.Value;

/**
 * The JavaScript exporter for TAME.
 * @author Matthew Tropiano
 */
public final class TAMEJSExporter 
{
	/** JS Module Default Variable Name */
	private static final String DEFAULT_MODULE_VARNAME = "EmbeddedData";
	
	/** Reader directive prefix 2. */
	private static final String JS_DIRECTIVE_JSPREFIX = "//[[EXPORTJS-";
	/** Reader directive prefix. */
	private static final String JS_DIRECTIVE_JSPREFIX2 = "/*[[EXPORTJS-";
	/** Reader directive suffix. */
	private static final String JS_DIRECTIVE_JSSUFFIX2 = "*/";
	/** Reader directive prefix. */
	private static final String JS_DIRECTIVE_HTMLPREFIX = "<!--[[EXPORTJS-";
	/** Reader directive suffix. */
	private static final String JS_DIRECTIVE_HTMLSUFFIX = "-->";

	/** Reader directive - START */
	private static final String JS_DIRECTIVE_START = "START";
	/** Reader directive - GENERATE */
	private static final String JS_DIRECTIVE_GENERATE = "GENERATE";
	/** Reader directive - INCLUDE */
	private static final String JS_DIRECTIVE_INCLUDE = "INCLUDE";
	/** Reader directive - INCLUDEALL */
	private static final String JS_DIRECTIVE_INCLUDEALL = "INCLUDEALL";
	/** Reader directive - END */
	private static final String JS_DIRECTIVE_END = "END";
	
	/** Generate JS header. */
	private static final String GENERATE_JSHEADER = "jsheader";
	/** Generate JS module header. */
	private static final String GENERATE_JSMODULEHEADER = "jsmoduleheader";
	/** Generate JS module variable declaration. */
	private static final String GENERATE_JSMODULEVARNAME = "jsmodulevarname";
	/** Generate version. */
	private static final String GENERATE_VERSION = "version";
	/** Generate module binary as base64. */
	private static final String GENERATE_BASE64 = "modulebase64";
	/** Generate header. */
	private static final String GENERATE_HEADER = "header";
	/** Generate module title. */
	private static final String GENERATE_TITLE = "title";
	/** Generate actions. */
	private static final String GENERATE_ACTIONS = "actions";
	/** Generate world. */
	private static final String GENERATE_ELEMENTS = "elements";
	
	/** Default writer options. */
	private static final TAMEJSExporterOptions DEFAULT_OPTIONS = new DefaultJSExporterOptions();
	
	/** Written to header: year */
	private static final String HEADER_COPYRIGHT_YEAR = "2019";
	
	// No constructor.
	private TAMEJSExporter(){}
	
	/**
	 * Exports a TAME Module to a stand-alone JavaScript module with default options.
	 * @param module the module to export.
	 * @throws IOException if a write error occurs.
	 * @return a String that contains the entirety of the generated code.
	 */
	public static String exportToString(TAMEModule module) throws IOException
	{
		StringWriter sw = new StringWriter();
		export(sw, module, DEFAULT_OPTIONS);
		return sw.toString();
	}
	
	/**
	 * Exports a TAME Module to a stand-alone JavaScript module.
	 * @param module the module to export.
	 * @param options the exporter options.
	 * @return a String that contains the entirety of the generated code.
	 * @throws IOException if a write error occurs.
	 */
	public static String exportToString(TAMEModule module, TAMEJSExporterOptions options) throws IOException
	{
		StringWriter sw = new StringWriter();
		export(sw, module, options);
		return sw.toString();
	}
	
	/**
	 * Exports a TAME Module to a stand-alone JavaScript module with default options.
	 * @param file the output file.
	 * @param module the module to export.
	 * @throws IOException if a write error occurs.
	 */
	public static void export(File file, TAMEModule module) throws IOException
	{
		export(file, module, DEFAULT_OPTIONS);
	}
	
	/**
	 * Exports a TAME Module to a stand-alone JavaScript module.
	 * @param file the output file.
	 * @param module the module to export.
	 * @param options the exporter options.
	 * @throws IOException if a write error occurs.
	 */
	public static void export(File file, TAMEModule module, TAMEJSExporterOptions options) throws IOException
	{
		FileOutputStream fos = null;
		PrintWriter pw = null;
		try {
			fos = new FileOutputStream(file);
			pw = new PrintWriter(fos, true);
			export(pw, module, options);
			pw.flush();
		} finally {
			IOUtils.close(pw);
		}
	}
	
	/**
	 * Exports a TAME Module to a stand-alone JavaScript module.
	 * @param out the output stream.
	 * @param module the module to export.
	 * @throws IOException if a write error occurs.
	 */
	public static void export(OutputStream out, TAMEModule module) throws IOException
	{
		export(out, module, DEFAULT_OPTIONS);
	}
	
	/**
	 * Exports a TAME Module to a stand-alone JavaScript module.
	 * @param out the output stream.
	 * @param module the module to export.
	 * @param options the exporter options.
	 * @throws IOException if a write error occurs.
	 */
	public static void export(OutputStream out, TAMEModule module, TAMEJSExporterOptions options) throws IOException
	{
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(out, true);
			export(pw, module, options);
			pw.flush();
		} finally {
			IOUtils.close(pw);
		}
	}
	
	/**
	 * Exports a TAME Module to a stand-alone JavaScript module.
	 * @param writer the output writer.
	 * @param module the module to export.
	 * @throws IOException if a write error occurs.
	 */
	public static void export(Writer writer, TAMEModule module) throws IOException
	{
		export(writer, module, DEFAULT_OPTIONS);
	}
	
	/**
	 * Exports a TAME Module to a stand-alone JavaScript module.
	 * @param writer the output writer.
	 * @param module the module to export.
	 * @param options the exporter options.
	 * @throws IOException if a write error occurs.
	 */
	public static void export(Writer writer, TAMEModule module, TAMEJSExporterOptions options) throws IOException
	{
		processResource(writer, module, options, options.getStartingPath(), false);
	}

	/**
	 * Attempts to cull a directive tag from an input line. 
	 * @param line the input line.
	 * @return a culled line, or the same line if not transformable.
	 */
	private static String getTrimmedTagContent(String line)
	{
		String trimmed = line.trim();
		if (trimmed.startsWith(JS_DIRECTIVE_JSPREFIX))
			return trimmed.substring(JS_DIRECTIVE_JSPREFIX.length());
		else if (trimmed.startsWith(JS_DIRECTIVE_JSPREFIX2))
		{
			if (trimmed.endsWith(JS_DIRECTIVE_JSSUFFIX2))
				return trimmed.substring(JS_DIRECTIVE_JSPREFIX2.length(), trimmed.length() - JS_DIRECTIVE_JSSUFFIX2.length()).trim();
			else
				return trimmed.substring(JS_DIRECTIVE_JSPREFIX2.length());
		}
		else if (trimmed.startsWith(JS_DIRECTIVE_HTMLPREFIX))
		{
			if (trimmed.endsWith(JS_DIRECTIVE_HTMLSUFFIX))
				return trimmed.substring(JS_DIRECTIVE_HTMLPREFIX.length(), trimmed.length() - JS_DIRECTIVE_HTMLSUFFIX.length()).trim();
			else
				return trimmed.substring(JS_DIRECTIVE_HTMLPREFIX.length());
		}
		return line;
	}
	
	/**
	 * Reads a resource, stopping at a stop directive or end-of-file.
	 * Reads directives to determine reading behavior. 
	 * @param writer the writer to write to.
	 * @param module the module to eventually write to.
	 * @param options the exporter options.
	 * @param path the import resource path.
	 * @param entire if true, do not wait for the start directive to read.
	 */
	private static void processResource(Writer writer, TAMEModule module, TAMEJSExporterOptions options, String path, boolean entire) throws IOException
	{
		String parentPath = path.substring(0, path.lastIndexOf('/') + 1);
		
		BufferedReader br = null;
		InputStream in = null;
		try {
			try {
				in = new FileInputStream(path);
			} catch (FileNotFoundException e) {
				in = IOUtils.openResource(path);
				if (in == null)
					throw new IOException("Resource \""+path+"\" cannot be found!");
			}
			
			br = new BufferedReader(new InputStreamReader(in));
			String line = null;
			boolean startWrite = false;
			
			if (entire)
				startWrite = true;
			
			while ((line = br.readLine()) != null)
			{
				String trimline = getTrimmedTagContent(line);
				if (trimline.startsWith(JS_DIRECTIVE_START))
				{
					startWrite = true;
					continue;
				}
				
				if (!startWrite)
					continue;
				
				if (trimline.startsWith(JS_DIRECTIVE_GENERATE))
				{
					generateResource(writer, module, options, trimline.substring(JS_DIRECTIVE_GENERATE.length() + 1).trim());
				}
				else if (trimline.startsWith(JS_DIRECTIVE_INCLUDEALL))
				{
					String nextPath = parentPath + trimline.substring(JS_DIRECTIVE_INCLUDEALL.length() + 1).trim();
					processResource(writer, module, options, nextPath, true);
				}
				else if (trimline.startsWith(JS_DIRECTIVE_INCLUDE))
				{
					String nextPath = parentPath + trimline.substring(JS_DIRECTIVE_INCLUDE.length() + 1).trim();
					processResource(writer, module, options, nextPath, false);
				}
				else if (trimline.startsWith(JS_DIRECTIVE_END))
				{
					if (!entire)
						break;
				}
				else
				{
					writer.write(line + "\n");
				}
			}

		} finally {
			IOUtils.close(in);
			IOUtils.close(br);
		}
	}

	/**
	 * Generates a function table in JS.
	 * @param writer the writer to write to.
	 * @param functionTable the function table.
	 */
	private static JSONObject convertFunctionTable(Iterable<ObjectPair<String, FunctionEntry>> functionTable) throws IOException
	{
		JSONObject out = JSONObject.createEmptyObject();
		for (ObjectPair<String, FunctionEntry> entry : functionTable)
			out.addMember(entry.getKey().toLowerCase(), convertFunctionEntry(entry.getValue()));
		return out;
	}

	/**
	 * Generates a function entry in JS.
	 * @param writer the writer to write to.
	 * @param entry the entry.
	 */
	private static JSONObject convertFunctionEntry(FunctionEntry entry) throws IOException
	{
		JSONObject out = JSONObject.createEmptyArray();
		out.addMember("arguments", entry.getArguments());
		out.addMember("block", convertBlock(entry.getBlock()));
		return out;
	}

	/**
	 * Generates a block table in JS.
	 * @param writer the writer to write to.
	 * @param block the block.
	 */
	private static JSONObject convertBlockTable(Iterable<ObjectPair<BlockEntry, Block>> blockTable) throws IOException
	{
		JSONObject out = JSONObject.createEmptyObject();
		for (ObjectPair<BlockEntry, Block> entry : blockTable)
			out.addMember(entry.getKey().getEntryString(), convertBlock(entry.getValue()));
		return out;
	}

	/**
	 * Generates a block list in JS.
	 * @param writer the writer to write to.
	 * @param block the block.
	 */
	private static JSONObject convertBlock(Block block) throws IOException
	{
		JSONObject out = JSONObject.createEmptyArray();
		for (Operation operation : block)
			out.append(convertOperation(operation));
		return out;
	}

	/**
	 * Generates a value object in JS.
	 * @param writer the writer to write to.
	 * @param block the block.
	 */
	private static JSONObject convertValue(Value value) throws IOException
	{
		JSONObject out = JSONObject.createEmptyObject();
		out.addMember("type", value.getType());
		switch (value.getType())
		{
			case INTEGER:
			case BOOLEAN:
			case FLOAT:
				out.addMember("value", value.getValue());
			break;
			
			default:
				out.addMember("value", EncodingUtils.asBase64(new ByteArrayInputStream(value.getValue().toString().getBytes("utf-8"))));
			break;
		
		}
		return out;
	}

	/**
	 * Generates an operation object in JS.
	 * @param writer the writer to write to.
	 * @param block the block.
	 */
	private static JSONObject convertOperation(Operation operation) throws IOException
	{
		JSONObject out = JSONObject.createEmptyObject();
		
		out.addMember("opcode", operation.getOperation().ordinal());
		if (operation.getOperand0() != null)
			out.addMember("operand0", convertValue(operation.getOperand0()));
		if (operation.getOperand1() != null)
			out.addMember("operand1", convertValue(operation.getOperand1()));
		if (operation.getInitBlock() != null)
			out.addMember("initBlock", convertBlock(operation.getInitBlock()));
		if (operation.getConditionalBlock() != null)
			out.addMember("conditionalBlock", convertBlock(operation.getConditionalBlock()));
		if (operation.getStepBlock() != null)
			out.addMember("stepBlock", convertBlock(operation.getStepBlock()));
		if (operation.getSuccessBlock() != null)
			out.addMember("successBlock", convertBlock(operation.getSuccessBlock()));
		if (operation.getFailureBlock() != null)
			out.addMember("failureBlock", convertBlock(operation.getFailureBlock()));
		
		return out;
	}

	/**
	 * Generates the TAME Resources in JS.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 * @param typeList the list of types (comma/space separated).
	 */
	private static void generateResource(Writer writer, TAMEModule module, TAMEJSExporterOptions options, String typeList) throws IOException
	{
		String[] parts = typeList.split("\\,\\s+");
		for (int i = 0; i < parts.length; i++)
		{
			String type = parts[i];
			if (type.equalsIgnoreCase(GENERATE_JSHEADER))
				generateResourceJSHeader(writer, module);
			else if (type.equalsIgnoreCase(GENERATE_JSMODULEHEADER))
				generateResourceJSModuleHeader(writer, module);
			else if (type.equalsIgnoreCase(GENERATE_JSMODULEVARNAME))
				generateResourceJSModuleVariableName(writer, module, options);
			else if (type.equalsIgnoreCase(GENERATE_VERSION))
				generateResourceVersion(writer, module);
			else if (type.equalsIgnoreCase(GENERATE_BASE64))
				generateResourceBinaryString(writer, module);
			else if (type.equalsIgnoreCase(GENERATE_HEADER))
				generateResourceHeader(writer, module);
			else if (type.equalsIgnoreCase(GENERATE_TITLE))
				generateResourceTitle(writer, module);
			else if (type.equalsIgnoreCase(GENERATE_ACTIONS))
				generateResourceActions(writer, module);
			else if (type.equalsIgnoreCase(GENERATE_ELEMENTS))
				generateResourceElements(writer, module);
			else
				throw new JSExportException("Unexpected generate type: "+type);
			
			if (i < parts.length - 1)
				writer.write(',');
		}
		writer.write('\n');
	}
	
	/**
	 * Generates the Version line in JS.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 */
	private static void generateResourceVersion(Writer writer, TAMEModule module) throws IOException
	{
		writer.append("this.version = "+JSONWriter.writeJSONString(TAMELogic.getVersion())+";");
	}
	
	/**
	 * Generates the embedded binary (as Base64).
	 * @param writer the writer to write to.
	 * @param module the source module.
	 */
	private static void generateResourceBinaryString(Writer writer, TAMEModule module) throws IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		module.writeBytes(bos);
		writer.append('"');
		writer.append(EncodingUtils.asBase64(new ByteArrayInputStream(bos.toByteArray())));
		writer.append('"');
	}
	
	/**
	 * Generates the JS engine comment header.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 */
	private static void generateResourceJSHeader(Writer writer, TAMEModule module) throws IOException
	{
		writer.append("/*************************************************************************\n");
		if (module == null)
			writer.append(" * TAME Engine v"+TAMELogic.getVersion()+"\n");
		else
			writer.append(" * TAME v"+TAMELogic.getVersion()+" with Embedded Module\n");
		writer.append(" * (C) 2016-").append(HEADER_COPYRIGHT_YEAR).append(" Matthew Tropiano\n");
		writer.append(" * All rights reserved. This program and the accompanying materials\n");
		writer.append(" * are made available under the terms of the GNU Lesser Public License v2.1\n");
		writer.append(" * which accompanies this distribution, and is available at\n");
		writer.append(" * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html\n");
		writer.append(" * \n");
		writer.append(" * https://tame-if.com\n");
		if (module != null)
		{
			Header header = module.getHeader();
			writer.append(" *\n");
			writer.append(" * Module Information:\n");
			for (String entry : header.getAllAttributes())
			{
				writer.append(" * "+Character.toUpperCase(entry.charAt(0))+entry.substring(1)+": "+header.getAttribute(entry)+"\n");
			}
		}
		writer.append(" * @license\n");
		writer.append(" *************************************************************************/\n");
	}
	
	/**
	 * Generates the JS module comment header.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 */
	private static void generateResourceJSModuleHeader(Writer writer, TAMEModule module) throws IOException
	{
		if (module == null)
			return;
		
		writer.append("/*************************************************************************\n");
		Header header = module.getHeader();
		writer.append(" * Module Information:\n");
		for (String entry : header.getAllAttributes())
		{
			writer.append(" * "+Character.toUpperCase(entry.charAt(0))+entry.substring(1)+": "+header.getAttribute(entry)+"\n");
		}
		writer.append(" * @preserve\n");
		writer.append(" *************************************************************************/\n");
	}
	
	/**
	 * Generates the JS module comment header.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 * @param options exporter options.
	 */
	private static void generateResourceJSModuleVariableName(Writer writer, TAMEModule module, TAMEJSExporterOptions options) throws IOException
	{
		if (module == null)
			return;

		String varname = options.getModuleVariableName();
		
		writer.append("var ");
		writer.append(varname != null ? varname : DEFAULT_MODULE_VARNAME);
		writer.append(" = \n");
	}
	
	/**
	 * Generates the header object in JS.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 */
	private static void generateResourceHeader(Writer writer, TAMEModule module) throws IOException
	{
		JSONWriter.writeJSON(module.getHeader().getAttributeMap(), writer);
	}
	
	/**
	 * Generates the title (for HTML), or "TAME Module" if no module header called title.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 */
	private static void generateResourceTitle(Writer writer, TAMEModule module) throws IOException
	{
		String title = module.getHeader().getAttribute(TAMEConstants.HEADER_TITLE);
		writer.append(title != null ? title : "TAME Module");
	}
	
	/**
	 * Generates the action list in JS.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 */
	private static void generateResourceActions(Writer writer, TAMEModule module) throws IOException
	{
		Iterator<ObjectPair<String, TAction>> it = module.getActionList().iterator();
		writer.append('[');
		while (it.hasNext())
		{
			TAction action = it.next().getValue();
			JSONObject out = JSONObject.createEmptyObject();
			JSONObject arr;
			
			out.addMember("tameType", TAction.class.getSimpleName());
			out.addMember("identity", action.getIdentity());
			out.addMember("type", action.getType().ordinal());
			if (action.isStrict())
				out.addMember("strict", true);
			if (action.isReversed())
				out.addMember("reversed", true);
			if ((arr = JSONObject.create(action.getNames())).length() > 0)
				out.addMember("names", arr);
			if ((arr = JSONObject.create(action.getExtraStrings())).length() > 0)
				out.addMember("extraStrings", arr);

			JSONWriter.writeJSON(out, writer);
			if (it.hasNext())
				writer.append(',');
		}
		writer.append(']');
		
	}

	/**
	 * Generates the world object in JS.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 */
	private static void generateResourceElements(Writer writer, TAMEModule module) throws IOException
	{
		writer.append('[');

		// export world
		generateResourceWorld(writer, module, module.getWorld());

		if (module.getObjectCount() > 0)
		{
			writer.append(',');
			// export objects
			Iterator<ObjectPair<String, TObject>> ito = module.getObjectList().iterator();
			while (ito.hasNext())
			{
				generateResourceObject(writer, module, ito.next().getValue());
				if (ito.hasNext())
					writer.append(',');
			}
		}

		if (module.getRoomCount() > 0)
		{
			writer.append(',');
			// export rooms
			Iterator<ObjectPair<String, TRoom>> itr = module.getRoomList().iterator();
			while (itr.hasNext())
			{
				generateResourceRoom(writer, module, itr.next().getValue());
				if (itr.hasNext())
					writer.append(',');
			}
		}
		
		if (module.getPlayerCount() > 0)
		{
			writer.append(',');
			// export players
			Iterator<ObjectPair<String, TPlayer>> itp = module.getPlayerList().iterator();
			while (itp.hasNext())
			{
				generateResourcePlayer(writer, module, itp.next().getValue());
				if (itp.hasNext())
					writer.append(',');
			}
		}
		
		if (module.getContainerCount() > 0)
		{
			writer.append(',');
			// export rooms
			Iterator<ObjectPair<String, TContainer>> itc = module.getContainerList().iterator();
			while (itc.hasNext())
			{
				generateResourceContainer(writer, module, itc.next().getValue());
				if (itc.hasNext())
					writer.append(',');
			}
		}
		
		writer.append(']');
	}
	
	/**
	 * Generates the world object in JS.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 * @param world the world element.
	 */
	private static void generateResourceWorld(Writer writer, TAMEModule module, TWorld world) throws IOException
	{
		JSONObject out = JSONObject.createEmptyObject();
		out.addMember("tameType", TWorld.class.getSimpleName());
		out.addMember("identity", world.getIdentity());
		out.addMember("blockTable", convertBlockTable(world.getBlockEntries()));
		out.addMember("functionTable", convertFunctionTable(world.getFunctionEntries()));
		JSONWriter.writeJSON(out, writer);
	}

	/**
	 * Generates the object list in JS.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 */
	private static void generateResourceObject(Writer writer, TAMEModule module, TObject object) throws IOException
	{
		JSONObject out = JSONObject.createEmptyObject();
		JSONObject arr;
		
		out.addMember("tameType", TObject.class.getSimpleName());
		out.addMember("identity", object.getIdentity());
		if (object.getParent() != null)
			out.addMember("parent", object.getParent().getIdentity());
		if (object.isArchetype())
			out.addMember("archetype", true);

		if ((arr = JSONObject.create(object.getNames())).length() > 0)
			out.addMember("names", arr);
		if ((arr = JSONObject.create(object.getDeterminers())).length() > 0)
			out.addMember("determiners", arr);
		if ((arr = JSONObject.create(object.getTags())).length() > 0)
			out.addMember("tags", arr);

		out.addMember("blockTable", convertBlockTable(object.getBlockEntries()));
		out.addMember("functionTable", convertFunctionTable(object.getFunctionEntries()));
		JSONWriter.writeJSON(out, writer);
	}
	
	/**
	 * Generates the player list in JS.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 * @param player the player to export.
	 */
	private static void generateResourcePlayer(Writer writer, TAMEModule module, TPlayer player) throws IOException
	{
		JSONObject out = JSONObject.createEmptyObject();
		
		out.addMember("tameType", TPlayer.class.getSimpleName());
		out.addMember("identity", player.getIdentity());
		if (player.getParent() != null)
			out.addMember("parent", player.getParent().getIdentity());
		if (player.isArchetype())
			out.addMember("archetype", true);

		out.addMember("blockTable", convertBlockTable(player.getBlockEntries()));
		out.addMember("functionTable", convertFunctionTable(player.getFunctionEntries()));

		JSONWriter.writeJSON(out, writer);
	}
	
	/**
	 * Generates the room list in JS.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 * @param room the room to export.
	 */
	private static void generateResourceRoom(Writer writer, TAMEModule module, TRoom room) throws IOException
	{
		JSONObject out = JSONObject.createEmptyObject();
		
		out.addMember("tameType", TRoom.class.getSimpleName());
		out.addMember("identity", room.getIdentity());
		if (room.getParent() != null)
			out.addMember("parent", room.getParent().getIdentity());
		if (room.isArchetype())
			out.addMember("archetype", true);

		out.addMember("blockTable", convertBlockTable(room.getBlockEntries()));
		out.addMember("functionTable", convertFunctionTable(room.getFunctionEntries()));

		JSONWriter.writeJSON(out, writer);
	}
	
	/**
	 * Generates the containers list in JS.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 * @param container the container to export.
	 */
	private static void generateResourceContainer(Writer writer, TAMEModule module, TContainer container) throws IOException
	{
		JSONObject out = JSONObject.createEmptyObject();
		
		out.addMember("tameType", TContainer.class.getSimpleName());
		out.addMember("identity", container.getIdentity());
		if (container.getParent() != null)
			out.addMember("parent", container.getParent().getIdentity());

		out.addMember("blockTable", convertBlockTable(container.getBlockEntries()));
		out.addMember("functionTable", convertFunctionTable(container.getFunctionEntries()));

		JSONWriter.writeJSON(out, writer);
	}
	
}
