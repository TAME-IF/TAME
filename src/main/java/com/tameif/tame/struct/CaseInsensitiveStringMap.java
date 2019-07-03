package com.tameif.tame.struct;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Case-insensitive set map of string to value.
 * @author Matthew Tropiano
 * @param <V> the value type.
 */
public class CaseInsensitiveStringMap<V> implements Iterable<Map.Entry<String, V>>, Sizable
{
	/** Internal map. */
	private Map<String, V> map;

	public CaseInsensitiveStringMap(int capacity)
	{
		this.map = new HashMap<String, V>(capacity);
	}

	public void clear() 
	{
		map.clear();
	}

	public V get(String variableName)
	{
		return map.get(variableName.toLowerCase());
	}

	public void put(String variableName, V value) 
	{
		map.put(variableName.toLowerCase(), value);
	}

	public void remove(String variableName)
	{
		map.remove(variableName.toLowerCase());
	}

	public boolean containsKey(String variableName)
	{
		return map.containsKey(variableName.toLowerCase());
	}

	public List<String> keys()
	{
		return new ArrayList<>(map.keySet());
	}

	@Override
	public Iterator<Entry<String, V>> iterator()
	{
		return map.entrySet().iterator();
	}

	@Override
	public int size()
	{
		return map.size();
	}

	@Override
	public boolean isEmpty() 
	{
		return map.isEmpty();
	}
	
}
