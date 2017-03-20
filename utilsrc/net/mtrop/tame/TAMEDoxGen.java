package net.mtrop.tame;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.blackrook.commons.Common;
import com.blackrook.commons.CommonTokenizer;
import com.blackrook.commons.hash.HashMap;
import com.blackrook.commons.linkedlist.Queue;

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
	
	/** Resource root. */
	static final String RESOURCE_ROOT = "tamedox";
	/** Sidebar content. */
	static final String RESOURCE_SIDEBARHTML = RESOURCE_ROOT + "/sidebar.html";
	/** Pages list file. */
	static final String RESOURCE_PAGESLIST = RESOURCE_ROOT + "/pages.txt";
	/** Script root. */
	static final String RESOURCE_SCRIPTROOT = RESOURCE_ROOT + "/scripts/";

	/** Pages list file. */
	static final String SOURCE_SIDEASSETS = "./site-assets/docs";

	/** Output directory for generated JS. */
	static final String OUTPATH_JS = "js/generated/";
	/** Output directory for generated JS engine. */
	static final String OUTPATH_JS_TAMEENGINE = OUTPATH_JS + "TAME.js";
	/** Output directory for generated JS engine. */
	static final String OUTPATH_JS_BROWSERHANDLER = OUTPATH_JS + "TAMEBrowserHandler.js";
	/** Output directory for generated JS module. */
	static final String OUTPATH_JS_TAMEMODULE = OUTPATH_JS + "modules/";

	/** Parse command: set variable. */
	static final String COMMAND_SET = "set";
	/** Parse command: clear variable. */
	static final String COMMAND_CLEAR = "clear";
	/** Parse command: print variable. */
	static final String COMMAND_PRINT = "print";
	/** Parse command: include (file) */
	static final String COMMAND_INCLUDE = "include";
	/** Parse command: tamescript (name) (modulevarname) (file) */
	static final String COMMAND_TAMESCRIPT = "tamescript";
	/** Variable prefix. */
	static final String VAR_PREFIX = "$";
	
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
	
	// Entry point.
	public static void main(String[] args) throws Exception
	{
		if (args.length < 1)
		{
			out.println("ERROR: Expected output directory path.");
			System.exit(1);
			return;
		}
		
		// Get outpath root.
		String outPath = Common.removeEndingSequence(args[0], "/");
		File outDir = new File(outPath);
		if (!Common.createPath(outDir.getPath()))
		{
			out.println("ERROR: Could not create path for "+outDir.getPath());
			System.exit(3);
		}

		if (!outDir.isDirectory())
		{
			out.println("ERROR: Provided path is not a directory.");
			System.exit(2);
			return;
		}
		
		// Copy static pages.
		copyStaticPages(outDir);
		
		// Export engine.
		exportEngine(outPath);
		// Export browser JS helper.
		exportBrowserHandler(outPath);
		
		// Process pages.
		processAllPages(outPath);
		
		out.println("Done!");
	}

	private static void processAllPages(String outPath) throws IOException 
	{
		for (String[] page : getPageList())
		{
			boolean error = false;
			File outFile = new File(outPath + "/" + page[1]);
			if (!Common.createPathForFile(outFile))
			{
				out.println("ERROR: Could not create path for "+outFile.getPath());
				continue;
			}
			PrintWriter pw = null;
			try {
				pw = new PrintWriter(outFile, "UTF-8");
				parsePageResource(outPath + "/" + page[1], pw, RESOURCE_ROOT + "/" + page[0]);
			} catch (SecurityException e) {
				out.println("ERROR: Could not write file "+outFile.getPath()+". Access denied.");
				error = true;
			} catch (IOException e) {
				out.println("ERROR: Could not write file "+outFile.getPath()+". "+e.getLocalizedMessage());
				error = true;
			} finally {
				Common.close(pw);
				if (error)
					outFile.delete();
			}
		}
	}

	private static boolean exportBrowserHandler(String outPath) throws IOException
	{
		File outFile = new File(outPath + "/" + OUTPATH_JS_BROWSERHANDLER);
		if (!Common.createPathForFile(outFile))
		{
			out.println("ERROR: Could not create path for "+outFile.getPath());
			return false;
		}
		
		InputStream in = null;
		OutputStream out = null;
		try {
			in = Common.openResource("tamejs/html/TAMEBrowserHandler.js");
			out = new FileOutputStream(outFile);
			Common.relay(in, out);
		} finally {
			Common.close(in);
			Common.close(out);
		}
		
		return true;
	}

	private static boolean exportEngine(String outPath) throws IOException
	{
		File outFile = new File(outPath + "/" + OUTPATH_JS_TAMEENGINE);
		if (!Common.createPathForFile(outFile))
		{
			out.println("ERROR: Could not create path for "+outFile.getPath());
			return false;
		}
		
		TAMEJSExporter.export(outFile, null, TAMESCRIPT_JSEXPORTER_OPTIONS_ENGINE);
		return true;
	}

	private static void copyStaticPages(File outDir)
	{
		for (File inFile : Common.explodeFiles(new File(SOURCE_SIDEASSETS)))
		{
			File outFile = new File(outDir.getPath() + "/" + Common.removeStartingSequence(inFile.getPath().replaceAll("\\\\", "/"), SOURCE_SIDEASSETS));
			if (!Common.createPathForFile(outFile))
			{
				out.println("ERROR: Could not create path for "+outFile.getPath());
				continue;
			}

			FileInputStream fis = null;
			FileOutputStream fos = null;
			try {
				fis = new FileInputStream(inFile);
				fos = new FileOutputStream(outFile);
				Common.relay(fis, fos);
			} catch (SecurityException e) {
				out.printf("ERROR: Could not copy \"%s\" to \"%s\". Access denied.\n", inFile.getPath(), outFile.getPath());
			} catch (IOException e) {
				out.printf("ERROR: Could not copy \"%s\" to \"%s\"\n", inFile.getPath(), outFile.getPath());
			} finally {
				Common.close(fis);
				Common.close(fos);
			}
		}
	}

	private static Iterable<String[]> getPageList() throws IOException
	{
		Queue<String[]> out = new Queue<>();
		InputStream in = null;
		try {
			String line = null;
			BufferedReader pageReader = Common.openTextStream(in = Common.openResource(RESOURCE_PAGESLIST));
			while ((line = pageReader.readLine()) != null)
				out.add(line.split("\\s+"));
		} finally {
			Common.close(in);
		}
		return out;
	}

	/**
	 * Parses a file resource (presumably HTML) looking for <code>&lt;!--[ ... ]--&gt;</code> tags to parse and interpret.
	 * @param outPath the base output path.
	 * @param writer the output writer.
	 * @param inPath input resource path.
	 */
	public static void parsePageResource(String outPath, Writer writer, String inPath) throws IOException
	{
		parsePageResource(outPath, writer, inPath, new HashMap<String, String>());
	}
	
	/**
	 * Parses a file resource (presumably HTML) looking for <code>&lt;? ... ?&gt;</code> tags to parse and interpret.
	 * @param outPath the base output path.
	 * @param writer the output writer.
	 * @param inPath input resource path.
	 */
	public static void parsePageResource(String outPath, Writer writer, String inPath, HashMap<String, String> pageContext) throws IOException
	{
		final String TAG_START = "<!--[";
		final String TAG_END = "]-->";
		final int STATE_PAGE = 0;
		final int STATE_START_TAG_MAYBE = 1;
		final int STATE_TAG = 2;
		final int STATE_END_TAG_MAYBE = 3;
		
		InputStream in = null;
		try {
			in = Common.openResource(inPath);
			if (in == null)
				throw new IOException("Resource \""+inPath+"\" cannot be found! Internal error!");

			StringBuilder tagPart = new StringBuilder();
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
						if (c == TAG_START.charAt(0))
						{
							tagPart.append(c);
							state = STATE_START_TAG_MAYBE;
						}
						else
							writer.write(c);
					}
					break;

					case STATE_START_TAG_MAYBE: 
					{
						if (c != TAG_START.charAt(tagPart.length()))
						{
							writer.write(tagPart.toString());
							tagPart.delete(0, tagPart.length());
							writer.write(c);
							state = STATE_PAGE;
						}
						else
						{
							tagPart.append(c);
							if (tagPart.length() >= TAG_START.length())
							{
								state = STATE_TAG;
								tagPart.delete(0, tagPart.length());
							}
						}
					}
					break;
					
					case STATE_TAG: 
					{
						if (c == TAG_END.charAt(0))
						{
							tagPart.append(c);
							state = STATE_END_TAG_MAYBE;
						}
						else
							tagContent.append(c);
					}
					break;

					case STATE_END_TAG_MAYBE:
					{
						if (c != TAG_END.charAt(tagPart.length()))
						{
							tagContent.append(tagPart.toString());
							tagPart.delete(0, tagPart.length());
							tagContent.append(c);
							state = STATE_TAG;
						}
						else
						{
							tagPart.append(c);
							if (tagPart.length() >= TAG_END.length())
							{
								state = STATE_PAGE;
								String content = tagContent.toString();
								if (!interpretTag(outPath, content.trim(), inPath, writer, pageContext))
									writer.write(TAG_START + content + TAG_END);
								tagContent.delete(0, tagContent.length());
								tagPart.delete(0, tagPart.length());
							}
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
	 * Potentially resolves a token variable.
	 * @param inToken the input token.
	 * @param pageContext the context that holds variables.
	 * @return the associated object.
	 */
	private static String resolveVariable(String inToken, HashMap<String, String> pageContext)
	{
		if (inToken.startsWith(VAR_PREFIX))
		{
			String var = inToken.substring(VAR_PREFIX.length());
			if (pageContext.containsKey(var))
				return pageContext.get(var);
			else
				return inToken;
		}
		else
			return inToken;
	}
	
	/**
	 * Interprets the contents of a parsed tag.
	 * @param outPath the base output path.
	 * @param tagContent the content of the tag.
	 * @param inPath the origin resource path.
	 * @param writer the output writer.
	 */
	private static boolean interpretTag(String outPath, String tagContent, String inPath, Writer writer, HashMap<String, String> pageContext) throws IOException
	{
		String parentPath = inPath.substring(0, inPath.lastIndexOf('/') + 1);
		CommonTokenizer tokenizer = new CommonTokenizer(tagContent);
		
		if (tokenizer.isEmpty())
			return false;
		
		String command = tokenizer.nextToken();
		
		if (command.equalsIgnoreCase(COMMAND_SET))
		{
			String variableName = tokenizer.nextToken();
			String variableValue = resolveVariable(tokenizer.nextToken(), pageContext);
			pageContext.put(variableName.substring(VAR_PREFIX.length()), variableValue);
			return true;
		}
		else if (command.equalsIgnoreCase(COMMAND_CLEAR))
		{
			String variableName = tokenizer.nextToken();
			pageContext.removeUsingKey(variableName);
			return true;
		}
		else if (command.equalsIgnoreCase(COMMAND_PRINT))
		{
			String variableName = tokenizer.nextToken();
			writer.write(resolveVariable(variableName, pageContext));
			return true;
		}
		else if (command.equalsIgnoreCase(COMMAND_INCLUDE))
		{
			String relativePath = resolveVariable(tokenizer.nextToken(), pageContext);
			parsePageResource(outPath, writer, parentPath + relativePath, pageContext);
			return true;
		}
		else if (command.equalsIgnoreCase(COMMAND_TAMESCRIPT))
		{
			String headingName = resolveVariable(tokenizer.nextToken(), pageContext);
			String moduleName = resolveVariable(tokenizer.nextToken(), pageContext);
			String scriptPath = resolveVariable(tokenizer.nextToken(), pageContext);
			
			InputStream scriptIn = Common.openResource(RESOURCE_SCRIPTROOT + scriptPath);
			try {
				String scriptContent = Common.getTextualContents(scriptIn);
				writer.write("<div class=\"tame-codebox\">\n");
				writer.write("\t<div class=\"box-header\">\n");
				writer.write("\t\t"+headingName+"\n");
				writer.write("\t\t<button id=\"tame-"+moduleName+"\" class=\"docs-button button-launch\">Play Example</button>");
				writer.write("\t</div>\n");
				writer.write("\t<div class=\"box-body\">\n");
				writer.write("\t\t<pre><code id=\"tame-source-"+moduleName+"\" class=\"tame-code sh_tame\">\n");
				writer.write(scriptContent);
				writer.write("\t\t</code></pre>\n");
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
