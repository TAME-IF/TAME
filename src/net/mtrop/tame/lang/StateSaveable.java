package net.mtrop.tame.lang;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.mtrop.tame.TAMEModule;

/**
 * Describes an object whose state that can be saved/loaded.
 * @author Matthew Tropiano
 */
public interface StateSaveable
{
	/**
	 * Exports this object's state to bytes.
	 * @param module the source module for reference.
	 * @param out the output stream to write to.
	 * @throws IOException if a write problem occurs.
	 */
	public void writeStateBytes(TAMEModule module, OutputStream out) throws IOException;
	
	/**
	 * Imports this object's state from bytes.
	 * @param module the source module for reference.
	 * @param in the input stream to read from.
	 * @throws IOException if a read problem occurs.
	 */
	public void readStateBytes(TAMEModule module, InputStream in) throws IOException;
	
	/**
	 * Gets this object's state representation as bytes.
	 * @param module the source module for reference.
	 * @return the byte array of state bytes.
	 * @throws IOException if a write problem occurs.
	 */
	public byte[] toStateBytes(TAMEModule module) throws IOException;
	
	/**
	 * Reads this object's state representation from a byte array.
	 * @param module the source module for reference.
	 * @param data the data to read.
	 * @throws IOException if a read problem occurs.
	 */
	public void fromStateBytes(TAMEModule module, byte[] data) throws IOException;
	
}
