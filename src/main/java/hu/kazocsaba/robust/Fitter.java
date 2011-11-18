package hu.kazocsaba.robust;

import java.util.List;

/**
 * An algorithm that can produce a model which fits a data set. It is also responsible for producing error
 * values between an instance of the model and individual data elements.
 * @param <D> the type of the data elements
 * @param <M> the type of the model
 * @author Kazó Csaba
 */
public abstract class Fitter<D,M> {
	private final int minimalDataSetSize;

	/**
	 * Creates a new instance.
	 * @param minimalDataSetSize the minimal number of data elements this algorithm needs to compute a model
	 * @throws IllegalArgumentException if {@code minimalDataSetSize <= 0}
	 */
	public Fitter(int minimalDataSetSize) {
		this.minimalDataSetSize = minimalDataSetSize;
	}
	
	/**
	 * Returns the error between the model and the datum. Data with lower error values fit the model better.
	 * @param model a model
	 * @param datum a datum
	 * @return the error of the datum
	 */
	public abstract double getError(M model, D datum);
	
	/**
	 * Creates a model from some data. If the data set is unsuitable and a model cannot be created from it, this
	 * method should return {@code null}. This function should not modify the contents of the list.
	 * @param data the data elements from which to calculate a model
	 * @return the model computed from the data set, or {@code null} if no model could be computed
	 */
	public abstract M computeModel(List<D> data);
	
	/**
	 * Returns the minimal number of data elements from which a model can be computed.
	 * @return the minimal data set size
	 */
	public final int getMinimalDataSetSize() {
		return minimalDataSetSize;
	}
}
