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
import com.blackrook.lang.json.JSONWriter;

import net.mtrop.tame.TAMELogic;
import net.mtrop.tame.TAMEModule;
import net.mtrop.tame.element.TAction;
import net.mtrop.tame.element.TContainer;
import net.mtrop.tame.element.TObject;
import net.mtrop.tame.element.TPlayer;
import net.mtrop.tame.element.TRoom;

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
		FileOutputStream fos = new FileOutputStream(file);
		try {
			export(new PrintWriter(fos, true), module, options);
		} finally {
			Common.close(fos);
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
		export(new PrintWriter(out, true), module, options);
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
	public static void processResource(Writer writer, TAMEModule module, String path) throws IOException
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
	
	private static <T> void generateResourceList(Writer writer, Iterator<ObjectPair<String, T>> it) throws IOException 
	{
		writer.append('[');
		while (it.hasNext())
		{
			JSONWriter.writeJSON(it.next().getValue(), writer);
			if (it.hasNext())
				writer.append(',');
		}
		writer.append(']');
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
		generateResourceList(writer, module.getActionList().iterator());
	}

	/**
	 * Generates the world object in JS.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 */
	private static void generateResourceWorld(Writer writer, TAMEModule module) throws IOException
	{
		JSONWriter.writeJSON(module.getWorld(), writer);
	}
	
	/**
	 * Generates the object list in JS.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 */
	private static void generateResourceObjects(Writer writer, TAMEModule module) throws IOException
	{
		generateResourceList(writer, module.getObjectList().iterator());
	}
	
	/**
	 * Generates the player list in JS.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 */
	private static void generateResourcePlayers(Writer writer, TAMEModule module) throws IOException
	{
		generateResourceList(writer, module.getPlayerList().iterator());
	}
	
	/**
	 * Generates the room list in JS.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 */
	private static void generateResourceRooms(Writer writer, TAMEModule module) throws IOException
	{
		generateResourceList(writer, module.getRoomList().iterator());
	}
	
	/**
	 * Generates the containers list in JS.
	 * @param writer the writer to write to.
	 * @param module the source module.
	 */
	private static void generateResourceContainers(Writer writer, TAMEModule module) throws IOException
	{
		generateResourceList(writer, module.getContainerList().iterator());
	}
	
}
