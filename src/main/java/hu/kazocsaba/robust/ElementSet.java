package hu.kazocsaba.robust;

/**
 * A subset of the data elements.
 * @author Kazó Csaba
 */
public interface ElementSet {

	/**
	 * Returns the number of elements in this set.
	 * @return the size of this set
	 */
	public int size();

	/**
	 * Returns whether this set contains the data element at the specified index.
	 * @param index the index of a data element
	 * @return {@code true} if the specified data element belongs to this set, {@code false} otherwise
	 * @throws IndexOutOfBoundsException if {@code index} is invalid
	 */
	public boolean contains(int index);

	/**
	 * Returns the index of the first element in the set that occurs on or after the specified starting index.
	 * If there is no such element, -1 is returned.
	 * <p>
	 * This loop can be used to iterate over the elements:
	 * <pre>{@code
	 * for (int i=es.nextElement(0); i>=0; i=es.nextElement(i+1)) {
	 *  // index i is an element in this set
	 *}}</pre>
	 * This function has the same semantics as {@link java.util.BitSet#nextSetBit(int)}.
	 * @param fromIndex the index to start checking for elements (inclusive)
	 * @return the index of the next element, or -1 if there is no such element
	 * @throws IndexOutOfBoundsException if the specified index is negative
	 */
	public int nextElement(int fromIndex);
	
}
