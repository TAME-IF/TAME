package com.tameif.tame.struct;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Case-insensitive set of strings.
 * @author Matthew Tropiano
 */
public class CaseInsensitiveStringSet implements Iterable<String>, Sizable
{
	/** Internal set. */
	private Set<String> set;

	public CaseInsensitiveStringSet() 
	{
		this(8);
	}

	public CaseInsensitiveStringSet(int capacity)
	{
		this.set = new HashSet<>(capacity);
	}

	public boolean contains(String str)
	{
		return set.contains(str.toLowerCase());
	}

	public void put(String str)
	{
		set.add(str.toLowerCase());
	}

	public void clear() 
	{
		set.clear();
	}

	public void remove(String str)
	{
		set.remove(str.toLowerCase());
	}

	@Override
	public Iterator<String> iterator()
	{
		return set.iterator();
	}

	@Override
	public int size() 
	{
		return set.size();
	}

	@Override
	public boolean isEmpty() 
	{
		return set.isEmpty();
	}

}
