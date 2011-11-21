package hu.kazocsaba.robust;

import java.util.List;

/**
 * Superclass of robust estimator algorithms.
 * @param <D> the type of the data elements
 * @param <M> the type of the model
 * @param <N> the type of the monitor the robust estimator uses
 * @author Kazó Csaba
 */
public abstract class RobustEstimator<D,M,N extends RobustEstimator.Monitor<D,M>> {
	/**
	 * An object that can be used to monitor the execution of a robust estimator. It defines callback functions
	 * that can be used to trace the execution, examine internal state, or perform profiling. Concrete estimators
	 * can supply subclasses of {@code Monitor} that fit their particular algorithm; they are responsible for defining
	 * when and how the estimator calls the monitor methods.
	 * <p>
	 * Unless otherwise stated, monitor subclasses are not allowed to alter any objects passed to their functions,
	 * or to alter the state of the estimator during execution.
	 * @param <D> the type of the data elements
	 * @param <M> the type of the model
	 */
	public static class Monitor<D,M> {
		/**
		 * Called when the estimator terminates successfully and is about to return a model.
		 * @param inliers the data elements that the algorithm eventually decided were inliers to the final model
		 */
		public void success(M model, ElementSet inliers) {}
	}
	
	private static class AllElements implements ElementSet {
		final int size;

		public AllElements(int size) {
			this.size = size;
		}

		@Override
		public int size() {
			return size;
		}

		@Override
		public boolean contains(int index) {
			if (index<0 || index>=size) throw new IndexOutOfBoundsException();
			return true;
		}

		@Override
		public int nextElement(int fromIndex) {
			if (fromIndex<0) throw new IndexOutOfBoundsException();
			if (fromIndex>=size) return -1;
			return fromIndex;
		}
		
	}
	
	/**
	 * Performs some initial checks on the input. This function can be used by subclasses to potentially shortcut
	 * the algorithm.
	 * <p>
	 * This function first checks if there are fewer elements in the data set than required by the fitter, and fails
	 * with an exception if that is the case. If the data set size is equal to the minimal size specified by the fitter,
	 * then the entire set is used to construct a model. This model is reported to the monitor and returned. Exception
	 * is thrown in this case if the fitter failed to compute a model.
	 * <p>
	 * This function returns {@code null} if and only if the size of the data set is greater than
	 * {@code fitter.getMinimalDataSetSize()}.
	 * @param fitter the fitter used to compute a model and qualify data elements
	 * @param data the data set
	 * @param monitor an optional monitor for callbacks, may be {@code null}
	 * @return the valid model produced by the fitter if the data set is of minimal size, {@code null} if it's larger
	 * @throws NoModelFoundException if the data set is smaller than the minimal size, or if it is the minimal size and
	 * the fitter couldn't produce a model from it
	 */
	protected M performCheck(Fitter<D,M> fitter, List<D> data, N monitor) throws NoModelFoundException {
		if (fitter.getMinimalDataSetSize()>data.size())
			throw new NoModelFoundException("Not enough data to compute model");
		else if (fitter.getMinimalDataSetSize()==data.size()) {
			M model=fitter.computeModel(data);
			if (model==null)
				throw new NoModelFoundException("Fitter failed to compute model from data set");
			if (monitor!=null) monitor.success(model, new AllElements(data.size()));
			return model;
		}
		return null;
	}
	
	/**
	 * Executes the estimator algorithm.
	 * @param fitter the fitter used to compute a model and qualify data elements
	 * @param data the data set
	 * @param monitor an optional monitor for callbacks, may be {@code null}
	 * @return the model produced as the result of this algorithm; never {@code null}
	 * @throws NoModelFoundException if the estimator couldn't produce a model
	 */
	public abstract M perform(Fitter<D,M> fitter, List<D> data, N monitor) throws NoModelFoundException;
}
