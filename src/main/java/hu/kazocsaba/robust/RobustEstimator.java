package hu.kazocsaba.robust;

import java.util.BitSet;
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
	 * <p>
	 * This class and its subclasses provide their functions with empty implementations, so that subclasses only need
	 * to implement the ones they are interested in.
	 * @param <D> the type of the data elements
	 * @param <M> the type of the model
	 */
	public static class Monitor<D,M> {
		/**
		 * Called when the estimator terminates successfully and is about to return a model.
		 * @param model the model that will be the return value of the estimator
		 * @param inliers the data elements that the algorithm eventually decided were inliers to the final model
		 */
		public void success(M model, BitSet inliers) {}
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
			if (monitor!=null) {
				BitSet everything=new BitSet(data.size());
				everything.set(0, data.size());
				monitor.success(model, everything);
			}
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
	
	/**
	 * Executes the estimator algorithm.
	 * @param fitter the fitter used to compute a model and qualify data elements
	 * @param data the data set
	 * @return the model produced as the result of this algorithm; never {@code null}
	 * @throws NoModelFoundException if the estimator couldn't produce a model
	 */
	public M perform(Fitter<D,M> fitter, List<D> data) throws NoModelFoundException {
		return perform(fitter, data, null);
	}
}
