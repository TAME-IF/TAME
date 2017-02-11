package net.mtrop.tame;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.blackrook.commons.Common;
import com.blackrook.commons.CommonTokenizer;

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
	/** Index XML file. */
	static final String RESOURCE_INDEXXML = RESOURCE_ROOT + "/index.xml";

	/** Output directory for generated JS. */
	static final String OUTPATH_JS = "/js/generated/";

	/** Parse command: include (file) */
	static final String COMMAND_INCLUDE = "include";
	/** Parse command: template (file:header) (file:body) (file:footer) */
	static final String COMMAND_TEMPLATE = "template";
	/** Parse command: tamescript (name) (file) */
	static final String COMMAND_TAMESCRIPT = "tamescript";
	
	
	// Entry point.
	public static void main(String[] args) 
	{
		if (args.length < 1)
		{
			out.println("Error: Expected output directory path.");
			System.exit(0);
			return;
		}
		
		String outPath = args[0];
		
		// TODO Auto-generated method stub
	}

	/**
	 * Parses a file resource (presumably HTML) looking for <code>&lt;? ... ?&gt;</code> tags to parse and interpret.
	 * @param inPath input resource path.
	 * @param writer the output writer.
	 */
	public static void parsePageResource(String inPath, Writer writer) throws IOException
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
							if (!interpretTag(content.trim(), inPath, writer))
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
	 * @param tagContent the content of the tag.
	 * @param inPath the origin resource path.
	 * @param writer the output writer.
	 */
	private static boolean interpretTag(String tagContent, String inPath, Writer writer) throws IOException
	{
		String parentPath = inPath.substring(0, inPath.lastIndexOf('/') + 1);
		CommonTokenizer tokenizer = new CommonTokenizer(tagContent);
		
		if (tokenizer.isEmpty())
			return false;
		
		String command = tokenizer.nextToken();
		
		if (command.equalsIgnoreCase(COMMAND_INCLUDE))
		{
			String relativePath = tokenizer.nextToken();
			parsePageResource(parentPath + relativePath, writer);
			return true;
		}
		else if (command.equalsIgnoreCase(COMMAND_TEMPLATE))
		{
			String headerPath = tokenizer.nextToken();
			String bodyPath = tokenizer.nextToken();
			String footerPath = tokenizer.nextToken();
			writer.append("<!DOCTYPE html>\n");
			writer.append("<html>\n");
			writer.append("\t<head>\n");
			parsePageResource(parentPath + headerPath, writer);
			writer.append("\t</head>\n");
			writer.append("\t<body>\n");
			parsePageResource(parentPath + bodyPath, writer);
			parsePageResource(parentPath + footerPath, writer);
			writer.append("\t</body>\n");
			writer.append("</html>\n");
			return true;
		}
		else if (command.equalsIgnoreCase(COMMAND_TAMESCRIPT))
		{
			String headingName = tokenizer.nextToken();
			String scriptPath = tokenizer.nextToken();
			
			InputStream scriptIn = Common.openResource(parentPath + scriptPath);
			try {
				String scriptContent = Common.getTextualContents(scriptIn);
				writer.write("<pre class=\"sh_tame\">\n");
				writer.write(scriptContent);
				writer.write("</pre>\n");
				// TODO: Compile and add launch button somewhere.
				// TODO: Add script tag for include.
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
	
	/** Each individual page. */
	private static class Page
	{
		// TODO: Finish.
	}

	/** Each individual command (to turn into pages). */
	private static class CommandPage
	{
		// TODO: Finish.
	}

	/**
	 * XML parser for the command list.
	 */
	private static class IndexXMLParser extends DefaultHandler
	{
		@Override
		public void startDocument() throws SAXException 
		{
		}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException 
		{
			super.startElement(uri, localName, qName, attributes);
		}
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException 
		{
			super.characters(ch, start, length);
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException 
		{
			super.endElement(uri, localName, qName);
		}
		
		@Override
		public void endDocument() throws SAXException
		{
		}
		
	}
	
}
