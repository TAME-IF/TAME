package net.mtrop.tame.factory;

import java.io.IOException;
import java.io.Writer;

import net.mtrop.tame.TAMEModule;

/**
 * The JavaScript exporter for TAME.
 * @author Matthew Tropiano
 */
public final class TAMEJSExporter 
{
	/** Root resource for JS */
	private static final String JS_ROOT_RESOURCE = "net/mtrop/tame/js";
	
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
	
	/** Default writer options. */
	private static final TAMEJSExporterOptions DEFAULT_OPTIONS = new DefaultJSExporterOptions();
	
	
	/**
	 * Exports a TAME Module to a stand-alone JavaScript module.
	 * @param writer the output writer.
	 * @param module the module to export.
	 * @param options the exporter options.
	 * @throws IOException if a write error occurs.
	 */
	public static final void export(Writer writer, TAMEModule module) throws IOException
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
	public static final void export(Writer writer, TAMEModule module, TAMEJSExporterOptions options) throws IOException
	{
		// TODO: Finish this.
	}
		
}
