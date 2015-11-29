/*******************************************************************************
 * Copyright (c) 2015 Matt Tropiano
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v2.1
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 *
 * Contributors:
 *     Matt Tropiano - initial API and implementation
 *******************************************************************************/
package net.mtrop.tame;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.blackrook.commons.Common;

/**
 * TAME Archive file.
 * @author Matthew Tropiano
 */
public class TAMEModuleArchive extends ZipFile
{
	/** Module entry name. */
	public static final String MODULE_ENTRY = "module.o";
	/** Debug key entry. */
	public static final String DEBUG_KEY_ENTRY = "debug-key";
	/** Assets root entry name. */
	public static final String ASSETS_ENTRY = "assets";
	/** Meta root entry name. */
	public static final String META_ENTRY = "meta";
	/** Web root entry name. */
	public static final String WEB_ENTRY = META_ENTRY + "/tamenet";
	
	/**
	 * Opens a TAME container from a file.
	 * @param f the file to read.
	 * @throws IOException if the container can't be read or is not a TAME container.
	 */
	public TAMEModuleArchive(File f) throws IOException
	{
		super(f);
	}

	/**
	 * Gets the debug key.
	 * @return the debug key, if one exists, or null if not.
	 * @throws IOException if the archive cannot be read.
	 */
	public byte[] getDebugKey() throws IOException
	{
		byte[] out = null;
		InputStream in = null;
		if ((in = openAbsolute(DEBUG_KEY_ENTRY)) != null)
			out = Common.getBinaryContents(in);
		Common.close(in);
		return out; 
	}
	
	/**
	 * Checks if the key matches.
	 * @param key the key to check.
	 * @return
	 */
	public boolean checkDebugKey(String key) throws IOException
	{
		byte[] hash = getDebugKey();
		return hash != null ? Arrays.equals(hash, Common.sha1(key.getBytes("UTF-8"))) : false;
	}
	
	/**
	 * Opens a input stream to the compiled module file in the archive.
	 * This is a shortcut for <code>openAbsolute(MODULE_ENTRY)</code>.
	 * @return An open stream for reading the module or null if it doesn't exist.
	 * @throws IOException if the archive cannot be read.
	 */
	public InputStream openModule() throws IOException
	{
		return openAbsolute(MODULE_ENTRY);
	}
	
	/**
	 * Opens a stream to a particular file entry in the archive from the assets
	 * root.
	 * @param path the entry path.
	 * @return An open stream for reading the module or null if no entry
	 * exists by that name.
	 * @throws IOException if the archive cannot be read.
	 */
	public InputStream openAsset(String path) throws IOException
	{
		return openAbsolute(ASSETS_ENTRY+"/"+path);
	}

	/**
	 * Opens a stream to a particular file entry in the archive from the meta
	 * root.
	 * @param path the entry path.
	 * @return An open stream for reading the module or null if no entry
	 * exists by that name.
	 * @throws IOException if the archive cannot be read.
	 */
	public InputStream openMeta(String path) throws IOException
	{
		return openAbsolute(META_ENTRY+"/"+path);
	}

	/**
	 * Opens a stream to a particular file entry in the archive from the meta/tamenet
	 * root.
	 * @param path the entry path.
	 * @return An open stream for reading the module or null if no entry
	 * exists by that name.
	 * @throws IOException if the archive cannot be read.
	 */
	public InputStream openWebAsset(String path) throws IOException
	{
		return openAbsolute(WEB_ENTRY+"/"+path);
	}

	/**
	 * Opens a stream to a particular file entry in the archive from no
	 * relative root.
	 * @param path the entry path.
	 * @return An open stream for reading the module or null if no entry
	 * exists by that name.
	 * @throws IOException if the archive cannot be read.
	 */
	public InputStream openAbsolute(String path) throws IOException
	{
		ZipEntry ze = getEntry(path);
		return ze != null ? new BufferedInputStream(getInputStream(ze)) : null;
	}
	
	@Override
	protected void finalize() throws IOException
	{
		close();
		super.finalize();
	}

}
