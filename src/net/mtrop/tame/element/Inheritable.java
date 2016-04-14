package net.mtrop.tame.element;

/**
 * Objects that use this interface may have an implied lineage.
 * Some logic processes will trace through an object's lineage to find a valid
 * block or whatever.
 * @author Matthew Tropiano
 * @param <E> the parent type.
 */
public interface Inheritable<E>
{
	/**
	 * Gets this object's parent.
	 * The ordering is in the order they were added.
	 * @return an iterator for this object's lineage.
	 * @see #addParent(Object)
	 */
	public E getParent();
	
	/**
	 * Sets a parent on this object.
	 * @param parent the parent object to set.
	 */
	public void setParent(E parent);

}
