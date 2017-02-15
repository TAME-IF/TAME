package net.mtrop.tame;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.blackrook.commons.Common;
import com.blackrook.commons.CommonTokenizer;

import net.mtrop.tame.factory.TAMEJSExporter;
import net.mtrop.tame.factory.TAMEJSExporterOptions;
import net.mtrop.tame.factory.TAMEScriptIncluder;
import net.mtrop.tame.factory.TAMEScriptReader;

/**
 * Generates TAME documentation.
 * @author Matthew Tropiano
 */
public final class TAMEDoxGen 
{
	/** Shorthand STDOUT. */
	static final PrintStream out = System.out;

	/** Current time. */
	static final String NOW_STRING = (new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
	
	/** Input directory for content to directly copy over. */
	static final String STATIC_WEBPATH = "site-assets";

	/** Resource root. */
	static final String RESOURCE_ROOT = "tamedox";
	/** Sidebar content. */
	static final String RESOURCE_SIDEBARHTML = RESOURCE_ROOT + "/sidebar.html";
	/** Index XML file. */
	static final String RESOURCE_INDEXXML = RESOURCE_ROOT + "/index.xml";

	/** Output directory for generated JS. */
	static final String OUTPATH_JS = "js/generated/";
	/** Output directory for generated JS engine. */
	static final String OUTPATH_JS_TAMEENGINE = OUTPATH_JS + "TAME.js";
	/** Output directory for generated JS module. */
	static final String OUTPATH_JS_TAMEMODULE = OUTPATH_JS + "modules/";

	/** Parse command: include (file) */
	static final String COMMAND_INCLUDE = "include";
	/** Parse command: sidebar */
	static final String COMMAND_SIDEBAR = "sidebar";
	/** Parse command: tamescript (name) (modulevarname) (file) */
	static final String COMMAND_TAMESCRIPT = "tamescript";

	
	/** TAMEScript includer. */
	static final TAMEScriptIncluder TAMESCRIPT_INCLUDER = new TAMEScriptIncluder()
	{
		@Override
		public InputStream getIncludeResource(String streamName, String path) throws IOException 
		{
			String streamParent = null;
			int lidx = -1; 
			if ((lidx = streamName.lastIndexOf('/')) >= 0)
				streamParent = streamName.substring(0, lidx + 1);

			return Common.openResource((streamParent != null ? streamParent : "") + path);
		}
	};

	/** TAMEScript JS Exporter Options. */
	static final TAMEJSExporterOptions TAMESCRIPT_JSEXPORTER_OPTIONS_ENGINE = new TAMEJSExporterOptions()
	{
		@Override
		public boolean isPathOutputEnabled() 
		{
			return false;
		}
		
		@Override
		public String getWrapperName() 
		{
			return TAMEJSExporter.WRAPPER_ENGINE;
		}

		@Override
		public String getModuleVariableName()
		{
			return null;
		}
	};
	
	
	private static String sidebarContent;
	

	// Entry point.
	public static void main(String[] args) throws Exception
	{
		if (args.length < 1)
		{
			out.println("Error: Expected output directory path.");
			System.exit(0);
			return;
		}
		
		String outPath = args[0];
		
		// TODO Finish.
		
		Common.noop();
	}

	/**
	 * Parses a file resource (presumably HTML) looking for <code>&lt;? ... ?&gt;</code> tags to parse and interpret.
	 * @param outPath the base output path.
	 * @param inPath input resource path.
	 * @param writer the output writer.
	 */
	public static void parsePageResource(String outPath, String inPath, Writer writer) throws IOException
	{
		final int STATE_PAGE = 0;
		final int STATE_START_TAG_MAYBE = 1;
		final int STATE_TAG = 2;
		final int STATE_END_TAG_MAYBE = 3;
		
		InputStream in = null;
		try {
			in = Common.openResource(inPath);
			if (in == null)
				throw new IOException("Resource \""+inPath+"\" cannot be found! Internal error!");

			StringBuilder tagContent = new StringBuilder();
			Reader r = new InputStreamReader(in, "UTF-8");			
			int state = STATE_PAGE;
			int readChar = 0;
			
			while ((readChar = r.read()) != -1)
			{
				char c = (char)readChar;
				switch (state)
				{
					case STATE_PAGE: 
					{
						if (c == '<')
							state = STATE_START_TAG_MAYBE;
						else
							writer.write(c);
					}
					break;

					case STATE_START_TAG_MAYBE: 
					{
						if (c == '?')
							state = STATE_TAG;
						else
						{
							writer.write('<');
							writer.write(c);
						}
					}
					break;
					
					case STATE_TAG: 
					{
						if (c == '?')
							state = STATE_END_TAG_MAYBE;
						else
							tagContent.append(c);
					}
					break;

					case STATE_END_TAG_MAYBE:
					{
						if (c == '>')
						{
							state = STATE_PAGE;
							String content = tagContent.toString();
							if (!interpretTag(outPath, content.trim(), inPath, writer))
								writer.write("&lt;?"+content+"?&gt;");
							tagContent.delete(0, tagContent.length());
						}
						else
						{
							tagContent.append('?');
							tagContent.append(c);
						}
					}
					break;
				}

			}

		} finally {
			Common.close(in);
		}

	}
	
	/**
	 * Interprets the contents of a parsed tag.
	 * @param outPath the base output path.
	 * @param tagContent the content of the tag.
	 * @param inPath the origin resource path.
	 * @param writer the output writer.
	 */
	private static boolean interpretTag(String outPath, String tagContent, String inPath, Writer writer) throws IOException
	{
		String parentPath = inPath.substring(0, inPath.lastIndexOf('/') + 1);
		CommonTokenizer tokenizer = new CommonTokenizer(tagContent);
		
		if (tokenizer.isEmpty())
			return false;
		
		String command = tokenizer.nextToken();
		
		if (command.equalsIgnoreCase(COMMAND_INCLUDE))
		{
			String relativePath = tokenizer.nextToken();
			parsePageResource(outPath, parentPath + relativePath, writer);
			return true;
		}
		else if (command.equalsIgnoreCase(COMMAND_SIDEBAR))
		{
			
		}
		else if (command.equalsIgnoreCase(COMMAND_TAMESCRIPT))
		{
			String headingName = tokenizer.nextToken();
			String moduleName = tokenizer.nextToken();
			String scriptPath = tokenizer.nextToken();
			
			InputStream scriptIn = Common.openResource(parentPath + scriptPath);
			try {
				String scriptContent = Common.getTextualContents(scriptIn);
				writer.write("<div class=\"tame-codebox\">\n");
				writer.write("\t<div class=\"box-heading\">\n");
				writer.write("\t\t"+headingName+"\n");
				writer.write("\t\t<div class=\"box-launch tame-example-"+moduleName+"\">\n");
				writer.write("\t\t\tPlay Example\n");
				writer.write("\t\t</div>\n");
				writer.write("\t</div>\n");
				writer.write("\t<div class=\"box-body\">\n");
				writer.write("\t\t<pre class=\"tame-code sh_tame\">\n");
				writer.write(scriptContent);
				writer.write("\t\t</pre>\n");
				writer.write("\t</div>\n");
				writer.write("</div>\n");
				
				TAMEModule module = TAMEScriptReader.read(scriptContent, TAMESCRIPT_INCLUDER);
				
				String filePath = OUTPATH_JS_TAMEMODULE + moduleName + ".js";
				
				File jsFile = new File(filePath);
				if (Common.createPathForFile(jsFile))
					TAMEJSExporter.export(jsFile, module, new ModuleExporterOptions(moduleName));
				else
					out.println("!!! CANNOT EXPORT JS !!!");
				
				writer.write("<script type=\"text/javascript\" src=\"" + filePath + "\"></script>\n");
				
			} catch (IOException e) {
				writer.write("<pre>!!! CAN'T FIND SCRIPT \""+parentPath + scriptPath+"\" !!!</pre>");
				return false;
			} finally {
				Common.close(scriptIn);
			}
			
			return true;
		}

		return false;
	}
	
	/** Module Exporter Options. */
	private static class ModuleExporterOptions implements TAMEJSExporterOptions
	{
		private String moduleName;
		private ModuleExporterOptions(String moduleName)
		{
			this.moduleName = moduleName;
		}

		@Override
		public String getModuleVariableName()
		{
			return moduleName;
		}
		
		@Override
		public String getWrapperName()
		{
			return TAMEJSExporter.WRAPPER_MODULE;
		}

		@Override
		public boolean isPathOutputEnabled()
		{
			return false;
		}
		
	}

}
