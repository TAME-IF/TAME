package net.mtrop.tame.lang;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Describes an object that can be saved/loaded.
 * @author Matthew Tropiano
 */
public interface Saveable
{
	/**
	 * Exports this object to bytes.
	 * @param out the output stream to write to.
	 * @throws IOException if a write problem occurs.
	 */
	public void writeBytes(OutputStream out) throws IOException;
	
	/**
	 * Imports this object from bytes.
	 * @param in the input stream to read from.
	 * @throws IOException if a read problem occurs.
	 */
	public void readBytes(InputStream in) throws IOException;
	
	/**
	 * Gets this object's representation as bytes.
	 * @return the byte array of state bytes.
	 * @throws IOException if a write problem occurs.
	 */
	public byte[] toBytes() throws IOException;
	
	/**
	 * Reads this object's representation from a byte array.
	 * @param data the data to read.
	 * @throws IOException if a read problem occurs.
	 */
	public void fromBytes(byte[] data) throws IOException;
	
}
