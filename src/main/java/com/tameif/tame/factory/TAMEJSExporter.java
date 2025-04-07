/*******************************************************************************
 * Copyright (c) 2015-2019 Matt Tropiano
 * This program and the accompanying materials are made available under the 
 * terms of the GNU Lesser Public License v2.1 which accompanies this 
 * distribution, and is available at
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
import java.util.Map;

import com.tameif.tame.TAMEConstants;
import com.tameif.tame.TAMELogic;
import com.tameif.tame.TAMEModule;
import com.tameif.tame.TAMEModule.Header;
import com.tameif.tame.exception.JSExportException;
import com.tameif.tame.struct.EncodingUtils;
import com.tameif.tame.struct.IOUtils;
import com.tameif.tame.struct.ValueUtils;

/**
 * The JavaScript exporter for TAME.
 * @author Matthew Tropiano
 */
public final class TAMEJSExporter 
{
	/** Prefix for paths in the classpath. */
	public static final String RESOURCE_PREFIX = "resource:";

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
		InputStream in = getStream(options, options.getStartingPath()); 
		processResource(writer, module, options, options.getStartingPath(), in, false);
		IOUtils.close(in);
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

	private static InputStream getStream(TAMEJSExporterOptions options, String path)
	{
		if (options.getVerboseStream() != null)
			options.getVerboseStream().println("Include \"" + path + "\"...");
		
		if (path.startsWith(RESOURCE_PREFIX))
		{
			String resourcePath = path.substring(RESOURCE_PREFIX.length());
			InputStream in = null;
			in = IOUtils.openResource(resourcePath);
			return in;
		}
		else
		{
			try {
				return new FileInputStream(path);
			} catch (FileNotFoundException e) {
				return null;
			}
		}
	}
	
	/**
	 * Reads a resource, stopping at a stop directive or end-of-file.
	 * Reads directives to determine reading behavior. 
	 * @param writer the writer to write to.
	 * @param module the module to eventually write to.
	 * @param options the exporter options.
	 * @param path the import resource path. If it starts with <code>"resource:"</code>, it will search the classpath.
	 * @param entire if true, do not wait for the start directive to read.
	 */
	private static void processResource(Writer writer, TAMEModule module, TAMEJSExporterOptions options, String path, InputStream in, boolean entire) throws IOException
	{
		if (options.getVerboseStream() != null)
			options.getVerboseStream().println("Processing \"" + path + "\"...");

		String parentPath = path.substring(0, path.lastIndexOf('/') + 1);
		
		try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
			
			int lineNum = 0;
			String line = null;
			boolean startWrite = false;
			
			if (entire)
			{
				if (options.getVerboseStream() != null)
					options.getVerboseStream().println(path + ": Start read at line 1");

				startWrite = true;
			}
			
			while ((line = br.readLine()) != null)
			{
				lineNum++;
				String trimline = getTrimmedTagContent(line);
				if (trimline.startsWith(JS_DIRECTIVE_START))
				{
					if (!startWrite)
					{
						if (options.getVerboseStream() != null)
							options.getVerboseStream().println(path + ": Start read at line " + lineNum);

						startWrite = true;
					}
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
					String includePath = trimline.substring(JS_DIRECTIVE_INCLUDEALL.length() + 1).trim(); 
					String fullPath = parentPath + includePath;
					InputStream nextIn;
					if ((nextIn = getStream(options, fullPath)) != null)
						processResource(writer, module, options, fullPath, nextIn, true);
					else if ((nextIn = getStream(options, includePath)) != null)
						processResource(writer, module, options, includePath, nextIn, true);
					else
						throw new IOException("Could not include path \"" + includePath + "\".");
				}
				else if (trimline.startsWith(JS_DIRECTIVE_INCLUDE))
				{
					String includePath = trimline.substring(JS_DIRECTIVE_INCLUDE.length() + 1).trim(); 
					String fullPath = parentPath + includePath;
					InputStream nextIn;
					if ((nextIn = getStream(options, fullPath)) != null)
						processResource(writer, module, options, fullPath, nextIn, false);
					else if ((nextIn = getStream(options, includePath)) != null)
						processResource(writer, module, options, includePath, nextIn, false);
					else
						throw new IOException("Could not include path \"" + includePath + "\".");
				}
				else if (trimline.startsWith(JS_DIRECTIVE_END))
				{
					if (!entire)
					{
						if (options.getVerboseStream() != null)
							options.getVerboseStream().println(path + ": End read at line " + lineNum);
						break;
					}
				}
				else
				{
					writer.write(line + "\n");
				}
			}
		}
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
		writer.append("this.version = \"" + ValueUtils.escapeString(TAMELogic.getVersion()) + "\";");
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
		writer.append(" * This program and the accompanying materials\n");
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
		writer.append('{');
		int i = 0;
		for (Map.Entry<String, String> entry : module.getHeader().getAttributeMap())
		{
			if (i > 0)
				writer.append(',');
			writer.append('"').append(entry.getKey()).append('"');
			writer.append(':');
			writer.append('"').append(escapeJSONString(entry.getValue())).append('"');
			i++;
		}
		writer.append('}');
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
	 * Escapes a string so it is suitable for JSON.
	 * @param s the input string.
	 * @return the resultant string.
	 */
	private static String escapeJSONString(String s)
	{
    	StringBuilder out = new StringBuilder();
    	for (int i = 0; i < s.length(); i++)
    	{
    		char c = s.charAt(i);
    		switch (c)
    		{
				case '\0':
					out.append("\\0");
					break;
    			case '\b':
    				out.append("\\b");
    				break;
    			case '\t':
    				out.append("\\t");
    				break;
    			case '\n':
    				out.append("\\n");
    				break;
    			case '\f':
    				out.append("\\f");
    				break;
    			case '\r':
    				out.append("\\r");
    				break;
    			case '\\':
    				out.append("\\\\");
    				break;
    			case '"':
    				out.append("\\\"");    					
    				break;
    			default:
    				if (c < 0x0020 || c >= 0x7f)
    					out.append("\\u" + String.format("%04x", c));
    				else
    					out.append(c);
    				break;
    		}
    	}
    	
    	return out.toString();
	}
	
}
