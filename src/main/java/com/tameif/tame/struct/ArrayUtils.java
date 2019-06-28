package com.tameif.tame.struct;

/**
 * Array functions.
 * @author Matthew Tropiano
 */
public final class ArrayUtils
{

	/**
	 * Returns a valid index of an element in an array if an object is contained in an array. 
	 * Sequentially searches for first match via {@link #equals(Object)}.
	 * Can search for null. 
	 * @param <T> class that extends Object.
	 * @param object the object to search for. Can be null.
	 * @param searchArray the list of objects to search.
	 * @return the index of the object, or -1 if it cannot be found.
	 */
	public static <T> int indexOf(T object, T[] searchArray)
	{
		for (int i = 0; i < searchArray.length; i++)
		{
			if (object == null && searchArray[i] == null)
				return i;
			else if (object.equals(searchArray[i]))
				return i;
		}
		return -1;
	}
	
}
