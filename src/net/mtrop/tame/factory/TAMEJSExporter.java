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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;

import com.blackrook.commons.Common;
import com.blackrook.commons.ObjectPair;
import com.blackrook.lang.json.JSONObject;
import com.blackrook.lang.json.JSONWriter;

import net.mtrop.tame.TAMELogic;
import net.mtrop.tame.TAMEModule;
import net.mtrop.tame.TAMEModule.Header;
import net.mtrop.tame.element.TAction;
import net.mtrop.tame.element.TContainer;
import net.mtrop.tame.element.TObject;
import net.mtrop.tame.element.TPlayer;
import net.mtrop.tame.element.TRoom;
import net.mtrop.tame.element.TWorld;
import net.mtrop.tame.exception.JSExportException;
import net.mtrop.tame.lang.Block;
import net.mtrop.tame.lang.BlockEntry;
import net.mtrop.tame.lang.Command;
import net.mtrop.tame.lang.Value;

/**
 * The JavaScript exporter for TAME.
 * @author Matthew Tropiano
 */
public final class TAMEJSExporter 
{
	/** Wrapper Type: Engine Only for Node. */
	public static final String WRAPPER_NODEENGINE = "nodeengine";
	/** Wrapper Type: Engine Only for Browsers. */
	public static final String WRAPPER_ENGINE = "engine";
	/** Wrapper Type: Module Only. */
	public static final String WRAPPER_MODULE = "module";
	/** Wrapper Type: NodeJS, Embedded Module. */
	public static final String WRAPPER_NODE = "node";
	/** Wrapper Type: Browser JS, Embedded Module (default if no wrapper specified). */
	public static final String WRAPPER_BROWSER = "browser";
	/** Wrapper Type: Browser JS, Embedded Module, HTML Body wrapper. */
	public static final String WRAPPER_HTML = "html";
	/** Wrapper Type: Browser JS, Embedded Module, HTML Body wrapper (debug version). */
	public static final String WRAPPER_HTML_DEBUG = "html-debug";

	/** JS Module Default Variable Name */
	private static final String DEFAULT_MODULE_VARNAME = "ModuleData";
	
	/** Root resource for JS */
	private static final String JS_ROOT_RESOURCE = "tamejs/";
	
	/** Reader directive prefix. */
	private static final String JS_DIRECTIVE_PREFIX = "//##[[EXPORTJS-";
	/** Reader directive - START */
	private static final String JS_DIRECTIVE_START = JS_DIRECTIVE_PREFIX + "START";
	/** Reader directive - GENERATE */
	private static final String JS_DIRECTIVE_GENERATE = JS_DIRECTIVE_PREFIX + "GENERATE";
	/** Reader directive - INCLUDE */
	private static final String JS_DIRECTIVE_INCLUDE = JS_DIRECTIVE_PREFIX + "INCLUDE";
	/** Reader directive - END */
	private static final String JS_DIRECTIVE_END = JS_DIRECTIVE_PREFIX + "END";
	
	/** Generate JS header. */
	private static final String GENERATE_JSHEADER = "jsheader";
	/** Generate JS module header. */
	private static final String GENERATE_JSMODULEHEADER = "jsmoduleheader";
	/** Generate JS module variable declaration. */
	private static final String GENERATE_JSMODULEVARNAME = "jsmodulevarname";
	/** Generate version. */
	private static final String GENERATE_VERSION = "version";
	/** Generate header. */
	private static final String GENERATE_HEADER = "header";
	/** Generate actions. */
	private static final String GENERATE_ACTIONS = "actions";
	/** Generate world. */
	private static final String GENERATE_ELEMENTS = "elements";
	
	/** Default writer options. */
	private static final TAMEJSExporterOptions DEFAULT_OPTIONS = new DefaultJSExporterOptions();
	
	/**
	 * Exports a TAME Module to a stand-alone JavaScript module with default options.
	 * @param file the output file.
	 * @param module the module to export.
	 * @throws IOException if a write error occurs.
	 */
	public static String exportToString(TAMEModule module) throws IOException
	{
		StringWriter sw = new StringWriter();
		export(sw, module, DEFAULT_OPTIONS);
		return sw.toString();
	}
	
	/**
	 * Exports a TAME Module to a stand-alone JavaScript module.
	 * @param file the output file.
	 * @param module the module to export.
	 * @param options the exporter options.
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
			Common.close(pw);
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
			Common.close(pw);
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
		if (WRAPPER_NODE.equalsIgnoreCase(options.getWrapperName()))
			processResource(writer, module, options, JS_ROOT_RESOURCE + "NodeJS.js");
		else if (WRAPPER_MODULE.equalsIgnoreCase(options.getWrapperName()))
			processResource(writer, module, options, JS_ROOT_RESOURCE + "Module.js");
		else if (WRAPPER_ENGINE.equalsIgnoreCase(options.getWrapperName()))
			processResource(writer, module, options, JS_ROOT_RESOURCE + "Engine.js");
		else if (WRAPPER_NODEENGINE.equalsIgnoreCase(options.getWrapperName()))
			processResource(writer, module, options, JS_ROOT_RESOURCE + "NodeEngine.js");
		else if (WRAPPER_BROWSER.equalsIgnoreCase(options.getWrapperName()))
			processResource(writer, module, options, JS_ROOT_RESOURCE + "Browser.js");
		else if (WRAPPER_HTML.equalsIgnoreCase(options.getWrapperName()))
			processResource(writer, module, options, JS_ROOT_RESOURCE + "Browser.html");
		else if (WRAPPER_HTML_DEBUG.equalsIgnoreCase(options.getWrapperName()))
			processResource(writer, module, options, JS_ROOT_RESOURCE + "Browser-Debug.html");
		else
			processResource(writer, module, options, JS_ROOT_RESOURCE + "Browser.js");
	}

	/**
	 * Reads a resource, stopping at a stop directive or end-of-file.
	 * Reads directives to determine reading behavior. 
	 * @param writer the writer to write to.
	 * @param module the module to eventually write to.
	 * @param options the exporter options.
	 * @param path the import resource path.
	 */
	private static void processResource(Writer writer, TAMEModule module, TAMEJSExporterOptions options, String path) throws IOException
	{
		String parentPath = path.substring(0, path.lastIndexOf('/') + 1);
		
		InputStream in = null;
		try {
			in = Common.openResource(path);
			if (in == null)
				throw new IOException("Resource \""+path+"\" cannot be found! Internal error!");
			
			if (options.isPathOutputEnabled())
				writer.append("// ---- "+path);
			BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
			String line = null;
			boolean startWrite = false;
			while ((line = br.readLine()) != null)
			{
				String trimline = line.trim();
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
				else if (trimline.startsWith(JS_DIRECTIVE_INCLUDE))
				{
					String nextPath = parentPath + trimline.substring(JS_DIRECTIVE_INCLUDE.length() + 1).trim();
					processResource(writer, module, options, nextPath);
				}
				else if (trimline.startsWith(JS_DIRECTIVE_END))
				{
					break;
				}
				else
				{
					writer.write(line + "\n");
				}
			}

		} finally {
			Common.close(in);
		}
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
		for (Command command : block)
			out.append(convertCommand(command));
		return out;
	}

	/**
	 * Generates a command object in JS.
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
				out.addMember("value", Common.asBase64(new ByteArrayInputStream(value.getValue().toString().getBytes("utf-8"))));
			break;
		
		}
		return out;
	}

	/**
	 * Generates a command object in JS.
	 * @param writer the writer to write to.
	 * @param block the block.
	 */
	private static JSONObject convertCommand(Command command) throws IOException
	{
		JSONObject out = JSONObject.createEmptyObject();
		
		out.addMember("opcode", command.getOperation().ordinal());
		if (command.getOperand0() != null)
			out.addMember("operand0", convertValue(command.getOperand0()));
		if (command.getOperand1() != null)
			out.addMember("operand1", convertValue(command.getOperand1()));
		if (command.getInitBlock() != null)
			out.addMember("initBlock", convertBlock(command.getInitBlock()));
		if (command.getConditionalBlock() != null)
			out.addMember("conditionalBlock", convertBlock(command.getConditionalBlock()));
		if (command.getStepBlock() != null)
			out.addMember("stepBlock", convertBlock(command.getStepBlock()));
		if (command.getSuccessBlock() != null)
			out.addMember("successBlock", convertBlock(command.getSuccessBlock()));
		if (command.getFailureBlock() != null)
			out.addMember("failureBlock", convertBlock(command.getFailureBlock()));
		
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
			else if (type.equalsIgnoreCase(GENERATE_HEADER))
				generateResourceHeader(writer, module);
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
		writer.append(" * (C) 2016-2017 Matthew Tropiano\n");
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
			if (action.isRestricted())
				out.addMember("restricted", true);
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
		JSONObject arr;
		
		out.addMember("tameType", TPlayer.class.getSimpleName());
		out.addMember("identity", player.getIdentity());
		if (player.getParent() != null)
			out.addMember("parent", player.getParent().getIdentity());
		if (player.isArchetype())
			out.addMember("archetype", true);

		out.addMember("permissionType", player.getPermissionType().ordinal());
		if ((arr = JSONObject.create(player.getPermissionActions())).length() > 0)
			out.addMember("permittedActionList", arr);

		out.addMember("blockTable", convertBlockTable(player.getBlockEntries()));

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
		JSONObject arr;
		
		out.addMember("tameType", TRoom.class.getSimpleName());
		out.addMember("identity", room.getIdentity());
		if (room.getParent() != null)
			out.addMember("parent", room.getParent().getIdentity());
		if (room.isArchetype())
			out.addMember("archetype", true);

		out.addMember("permissionType", room.getPermissionType().ordinal());
		if ((arr = JSONObject.create(room.getPermissionActions())).length() > 0)
			out.addMember("permittedActionList", arr);

		out.addMember("blockTable", convertBlockTable(room.getBlockEntries()));

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

		JSONWriter.writeJSON(out, writer);
	}
	
}
