package net.mtrop.tame.factory;

import java.io.BufferedReader;
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
import net.mtrop.tame.element.TAction;
import net.mtrop.tame.element.TContainer;
import net.mtrop.tame.element.TObject;
import net.mtrop.tame.element.TPlayer;
import net.mtrop.tame.element.TRoom;
import net.mtrop.tame.element.TWorld;
import net.mtrop.tame.lang.Block;
import net.mtrop.tame.lang.BlockEntry;
import net.mtrop.tame.lang.Command;

/**
 * The JavaScript exporter for TAME.
 * @author Matthew Tropiano
 */
public final class TAMEJSExporter 
{
	/** Root resource for JS */
	private static final String JS_ROOT_RESOURCE = "tamejs/";
	
	/** Reader directive prefix. */
	private static final String JS_DIRECTIVE_PREFIX = "//##[[";
	/** Reader directive - START */
	private static final String JS_DIRECTIVE_START = JS_DIRECTIVE_PREFIX + "CONTENT-START";
	/** Reader directive - GENERATE */
	private static final String JS_DIRECTIVE_GENERATE = JS_DIRECTIVE_PREFIX + "CONTENT-GENERATE";
	/** Reader directive - INCLUDE */
	private static final String JS_DIRECTIVE_INCLUDE = JS_DIRECTIVE_PREFIX + "CONTENT-INCLUDE";
	/** Reader directive - END */
	private static final String JS_DIRECTIVE_END = JS_DIRECTIVE_PREFIX + "CONTENT-END";
	
	/** Generate version. */
	private static final String GENERATE_VERSION = "version";
	/** Generate header. */
	private static final String GENERATE_HEADER = "header";
	/** Generate actions. */
	private static final String GENERATE_ACTIONS = "actions";
	/** Generate world. */
	private static final String GENERATE_WORLD = "world";
	/** Generate objects. */
	private static final String GENERATE_OBJECTS = "objects";
	/** Generate players. */
	private static final String GENERATE_PLAYERS = "players";
	/** Generate rooms. */
	private static final String GENERATE_ROOMS = "rooms";
	/** Generate containers. */
	private static final String GENERATE_CONTAINERS = "containers";
	
	/** Default writer options. */
	private static final TAMEJSExporterOptions DEFAULT_OPTIONS = new DefaultJSExporterOptions();
	
	/**
	 * Exports a TAME Module to a stand-alone JavaScript module.
	 * @param file the output file.
	 * @param module the module to export.
	 * @param options the exporter options.
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
	 * @param options the exporter options.
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
		processResource(writer, module, JS_ROOT_RESOURCE + "TAME.js");
	}

	/**
	 * Reads a resource, stopping at a stop directive or end-of-file.
	 * Reads directives to determine reading behavior. 
	 * @param writer the writer to write to.
	 * @param module the module to eventually write to.
	 * @param path the import path.
	 */
	private static void processResource(Writer writer, TAMEModule module, String path) throws IOException
	{
		String parentPath = path.substring(0, path.lastIndexOf('/') + 1);
		
		InputStream in = null;
		try {
			in = Common.openResource(path);
			if (in == null)
				throw new IOException("Resource \""+path+"\" cannot be found! Internal error!");
			
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
					generateResource(writer, module, trimline.substring(JS_DIRECTIVE_GENERATE.length() + 1).trim());
				}
				else if (trimline.startsWith(JS_DIRECTIVE_INCLUDE))
				{
					String nextPath = parentPath + trimline.substring(JS_DIRECTIVE_INCLUDE.length() + 1).trim();
					processResource(writer, module, nextPath);
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
	private static JSONObject convertCommand(Command command) throws IOException
	{
		JSONObject out = JSONObject.createEmptyObject();
		
		out.addMember("opcode", command.getOperation().ordinal());
		if (command.getOperand0() != null)
			out.addMember("operand0", command.getOperand0());
		if (command.getOperand1() != null)
			out.addMember("operand1", command.getOperand1());
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
	private static void generateResource(Writer writer, TAMEModule module, String typeList) throws IOException
	{
		String[] parts = typeList.split("\\,\\s+");
		for (int i = 0; i < parts.length; i++)
		{
			String type = parts[i];
			if (type.equalsIgnoreCase(GENERATE_VERSION))
				generateResourceVersion(writer, module);
			else if (type.equalsIgnoreCase(GENERATE_HEADER))
				generateResourceHeader(writer, module);
			else if (type.equalsIgnoreCase(GENERATE_ACTIONS))
				generateResourceActions(writer, module);
			else if (type.equalsIgnoreCase(GENERATE_WORLD))
				generateResourceWorld(writer, module);
			else if (type.equalsIgnoreCase(GENERATE_OBJECTS))
				generateResourceObjects(writer, module);
			else if (type.equalsIgnoreCase(GENERATE_PLAYERS))
				generateResourcePlayers(writer, module);
			else if (type.equalsIgnoreCase(GENERATE_ROOMS))
				generateResourceRooms(writer, module);
			else if (type.equalsIgnoreCase(GENERATE_CONTAINERS))
				generateResourceContainers(writer, module);
			
			if (i < parts.length - 1)
				writer.write(',');
		}
	}
	
	/**
	 * Generates the Version line in JS.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 */
	private static void generateResourceVersion(Writer writer, TAMEModule module) throws IOException
	{
		writer.append("this.version = "+JSONWriter.writeJSONString(TAMELogic.getVersion())+";\n");
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
	private static void generateResourceWorld(Writer writer, TAMEModule module) throws IOException
	{
		JSONObject out = JSONObject.createEmptyObject();
		TWorld world = module.getWorld();
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
	private static void generateResourceObjects(Writer writer, TAMEModule module) throws IOException
	{
		Iterator<ObjectPair<String, TObject>> it = module.getObjectList().iterator();
		writer.append('[');
		while (it.hasNext())
		{
			TObject object = it.next().getValue();
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
			if ((arr = JSONObject.create(object.getTags())).length() > 0)
				out.addMember("tags", arr);

			out.addMember("blockTable", convertBlockTable(object.getBlockEntries()));

			JSONWriter.writeJSON(out, writer);
			if (it.hasNext())
				writer.append(',');
		}
		writer.append(']');
	}
	
	/**
	 * Generates the player list in JS.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 */
	private static void generateResourcePlayers(Writer writer, TAMEModule module) throws IOException
	{
		Iterator<ObjectPair<String, TPlayer>> it = module.getPlayerList().iterator();
		writer.append('[');
		while (it.hasNext())
		{
			TPlayer player = it.next().getValue();
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
			if (it.hasNext())
				writer.append(',');
		}
		writer.append(']');
	}
	
	/**
	 * Generates the room list in JS.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 */
	private static void generateResourceRooms(Writer writer, TAMEModule module) throws IOException
	{
		Iterator<ObjectPair<String, TRoom>> it = module.getRoomList().iterator();
		writer.append('[');
		while (it.hasNext())
		{
			TRoom room = it.next().getValue();
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
			if (it.hasNext())
				writer.append(',');
		}
		writer.append(']');
	}
	
	/**
	 * Generates the containers list in JS.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 */
	private static void generateResourceContainers(Writer writer, TAMEModule module) throws IOException
	{
		Iterator<ObjectPair<String, TContainer>> it = module.getContainerList().iterator();
		writer.append('[');
		while (it.hasNext())
		{
			TContainer container = it.next().getValue();
			JSONObject out = JSONObject.createEmptyObject();
			
			out.addMember("tameType", TContainer.class.getSimpleName());
			out.addMember("identity", container.getIdentity());
			if (container.getParent() != null)
				out.addMember("parent", container.getParent().getIdentity());

			out.addMember("blockTable", convertBlockTable(container.getBlockEntries()));

			JSONWriter.writeJSON(out, writer);
			if (it.hasNext())
				writer.append(',');
		}
		writer.append(']');
	}
	
}
