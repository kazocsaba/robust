package hu.kazocsaba.robust;

import java.util.BitSet;

/**
 * An element set backed by a bit set.
 * @author Kazó Csaba
 */
class BitSetElementSet implements ElementSet {
	private final BitSet bitSet;
	private final int size;

	public BitSetElementSet(BitSet bitSet, int size) {
		this.bitSet = bitSet;
		this.size = size;
	}
	
	@Override
	public int size() {
		return bitSet.cardinality();
	}

	@Override
	public boolean contains(int index) {
		if (index<0 || index>=size) throw new IndexOutOfBoundsException("Invalid index: "+index);
		return bitSet.get(index);
	}

	@Override
	public int nextElement(int fromIndex) {
		return bitSet.nextSetBit(fromIndex);
	}
	
}
