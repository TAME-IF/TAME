package com.tameif.tame.struct;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Encoding utils.
 * @author Matthew Tropiano
 */
public final class EncodingUtils
{

	/**
	 * Returns a hash of a set of bytes digested by an encryption algorithm.
	 * Can return null if this Java implementation cannot perform this.
	 * Do not use this if you care if the algorithm is provided or not.
	 * @param bytes the bytes to encode.
	 * @param algorithmName the name to the algorithm to use.
	 * @return the resultant byte digest, or null if the algorithm is not supported.
	 */
	public static byte[] digest(byte[] bytes, String algorithmName)
	{
		try {
			return MessageDigest.getInstance(algorithmName).digest(bytes);
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}

	/**
	 * Returns a 20-byte SHA-1 hash of a set of bytes.
	 * Can return null if this Java implementation cannot perform this,
	 * but it shouldn't, since SHA-1 is mandatorily implemented for all implementations.
	 * @param bytes the input bytes.
	 * @return the resultant 20-byte digest.
	 * @see #digest(byte[], String)
	 */
	public static byte[] sha1(byte[] bytes)
	{
		return digest(bytes, "SHA-1");
	}

	/**
	 * Encodes a series of bytes as a Base64 encoded string.
	 * Uses + and / as characters 62 and 63.
	 * @param in the input stream to read to convert to Base64.
	 * @return a String of encoded bytes, or null if the message could not be encoded.
	 * @throws IOException if the input stream cannot be read.
	 */
	public static String asBase64(InputStream in) throws IOException
	{
		return asBase64(in, '+', '/');
	}

	/**
	 * Encodes a series of bytes as a Base64 encoded string.
	 * @param in the input stream to read to convert to Base64.
	 * @param sixtyTwo the character to use for character 62 in the Base64 index.
	 * @param sixtyThree the character to use for character 63 in the Base64 index.
	 * @return a String of encoded bytes, or null if the message could not be encoded.
	 * @throws IOException if the input stream cannot be read.
	 */
	public static String asBase64(InputStream in, char sixtyTwo, char sixtyThree) throws IOException
	{
		final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
		final char BLANK = '=';
		
		String alph = ALPHABET + sixtyTwo + sixtyThree;
		
		StringBuilder out = new StringBuilder();
		int octetBuffer = 0x00000000;
		int bidx = 0;
		
		byte[] buffer = new byte[16384];
		int buf = 0;
		
		while ((buf = in.read(buffer)) > 0) for (int i = 0; i < buf; i++)
		{
			byte b = buffer[i];
			
			octetBuffer |= ((b & 0x0ff) << ((2 - bidx) * 8));
			bidx++;
			if (bidx == 3)
			{
				out.append(alph.charAt((octetBuffer & (0x3f << 18)) >> 18));
				out.append(alph.charAt((octetBuffer & (0x3f << 12)) >> 12));
				out.append(alph.charAt((octetBuffer & (0x3f << 6)) >> 6));
				out.append(alph.charAt(octetBuffer & 0x3f));
				octetBuffer = 0x00000000;
				bidx = 0;
			}
		}
		
		if (bidx == 2)
		{
			out.append(alph.charAt((octetBuffer & (0x3f << 18)) >> 18));
			out.append(alph.charAt((octetBuffer & (0x3f << 12)) >> 12));
			out.append(alph.charAt((octetBuffer & (0x3f << 6)) >> 6));
			out.append(BLANK);
		}
		else if (bidx == 1)
		{
			out.append(alph.charAt((octetBuffer & (0x3f << 18)) >> 18));
			out.append(alph.charAt((octetBuffer & (0x3f << 12)) >> 12));
			out.append(BLANK);
			out.append(BLANK);
		}
		
		return out.toString();
	}

}
