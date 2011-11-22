package hu.kazocsaba.robust;

import hu.kazocsaba.robust.RobustEstimator.Monitor;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Random;

/**
 * RANSAC robust fitting algorithm.
 * @see "Martin A. Fischler and Robert C. Bolles: Random Sample Consensus: A Paradigm for
 * Model Fitting with Applications to Image Analysis and Automated Cartography (in Communications
 * of the ACM Magazine, Volume 24 Issue 6, June 1981"
 * @param <D> the type of the data elements
 * @param <M> the type of the model
 * @author Kazó Csaba
 */
public final class Ransac<D,M> extends RobustEstimator<D,M,Ransac.Monitor<D,M>> {
	/** The maximal number the fitter is allowed to fail to generate a model from a minimal data set. */
	private int maxModelFailCount=1000;
	
	/** The desired probability to choose at least one data set which contains no outliers. */
	private double successProbability=.99;
	
	/** The error threshold above which a data element is considered outlier. */
	private double inlierThreshold;
	
	/** The source of randomness. */
	private Random rnd=new Random();;

	/**
	 * Monitor class for {@link Ransac}.
	 * <p>
	 * The algorithm iteratively generates minimal sample sets, then fits a model to them. For each of these steps,
	 * {@link #modelFromMinimalSampleSet(ElementSet, Object)} is called with the model the fitter returned, possibly
	 * {@code null}. If a model could be computed, then Ransac adds to the element set all the inliers, and if their
	 * number is greater than the previous best (and also greater than the minimal set size), it recomputes
	 * the model. After this, {@link #modelFromInliers(Object, ElementSet, Object)} is called with both the original
	 * and the recomputed model. (Note that it is possible for this recomputed model to be {@code null} too; in this
	 * case the algorithm will use the original model for the inlier set.
	 * <p>
	 * This class and provides its functions with empty implementations, so that subclasses only need
	 * to implement the ones they are interested in.
	 * @param <D> the type of the data elements
	 * @param <M> the type of the model
	 */
	public static class Monitor<D,M> extends RobustEstimator.Monitor<D,M> {
		/**
		 * Called for each random minimal data set the algorithm generates, and the model which the fitter computes from
		 * it.
		 * @param samples the minimal sample set
		 * @param model the model computed by the fitter; may be {@code null}
		 */
		public void modelFromMinimalSampleSet(ElementSet samples, M model) {}
		/**
		 * Called after the algorithm found a new best set of inliers. The fitter produced {@code originalModel} from
		 * the minimal data set. This set was extended with the inliers from the entire data set, producing
		 * {@code inliers}. {@code model} corresponds to this extended set. Note that if {@code inliers} is same as the
		 * original minimal set, than {@code model} is the same as {@code originalModel}, and the fitter is not executed
		 * for a second time.
		 * @param originalModel the model computed from the minimal data set; this can not be {@code null}
		 * @param inliers the data consisting of the original minimal set and all the inliers to the original model
		 * @param model the model computed from {@code inliers}; may be {@code null}, in which case {@code originalModel}
		 * will be used by the algorithm instead
		 */
		public void modelFromInliers(M originalModel, ElementSet inliers, M model) {}
	}
	
	/**
	 * Creates a new instance.
	 * @param inlierThreshold the error threshold separating the inliers from the outliers; a data element with error
	 * exceeding this threshold will be considered outlier
	 */
	public Ransac(double inlierThreshold) {
		this.inlierThreshold = inlierThreshold;
	}

	/**
	 * Sets the desired probability that at least one inlier data set will be selected. This variable is used to
	 * set the number of iterations and defaults to 0.99.
	 * @param successProbability the probability
	 * @throws IllegalArgumentException if the probability is not between 0 and 1, exclusive
	 */
	public void setSuccessProbability(double successProbability) {
		if (successProbability<=0 || successProbability>=1)
			throw new IllegalArgumentException("Invalid probability: "+successProbability);
		this.successProbability = successProbability;
	}
	
	/**
	 * Sets the (only) source of randomness used by the algorithm. This function can be used to ensure deterministic
	 * execution: if the configuration of {@code Ransac} is not changed, and the same input is provided, than the
	 * behaviour of the algorithm will be exactly the same as long as the calls to {@code perform} are preceded by
	 * setting {@code Random} instances which produce identical sequences of numbers.
	 * @param random the source of randomness this {@code Ransac} instance will use
	 */
	public void setRandom(Random random) {
		if (random == null) throw new NullPointerException();
		rnd = random;
	}
	
	@Override
	public M perform(Fitter<D, M> fitter, List<D> data, Monitor<D, M> monitor) throws NoModelFoundException {
		performCheck(fitter, data, monitor);
		
		List<D> samples=new ArrayList<D>(data.size()/2);
		BitSet sampleMask=new BitSet(data.size());
		
		int iterationCount=1; // we will adjust this based on our guess of the outlier ratio
		int modelFailCount=0; // the number of random sample sets which failed to yield a model
		
		M bestModel=null;
		int bestModelSupport=0;
		BitSet bestModelInliers=new BitSet(data.size());
		
		for (int iteration=0; iteration<iterationCount; ) {
			
			// generate a random sample set
			
			samples.clear();
			sampleMask.clear();
			while (samples.size()<fitter.getMinimalDataSetSize()) {
				int index=rnd.nextInt(data.size());
				if (!sampleMask.get(index)) {
					samples.add(data.get(index));
					sampleMask.set(index);
				}
			}
			
			// try to compute a model
			
			M model=fitter.computeModel(samples);
			
			if (monitor!=null)
				monitor.modelFromMinimalSampleSet(new BitSetElementSet(sampleMask, samples.size()), model);
			
			if (model==null) {
				modelFailCount++;
				if (modelFailCount==maxModelFailCount)
					throw new NoModelFoundException("Fitter failed to compute a model from a minimal data set too many times");
			} else {
				// check all the rest of the data set for inliers
				{
					int index=-1;
					while (true) {
						index=sampleMask.nextClearBit(index+1);
						if (index>=data.size()) break;
						if (fitter.getError(model, data.get(index))<=inlierThreshold) {
							samples.add(data.get(index));
							sampleMask.set(index);
						}
					}
				}
				if (samples.size()>bestModelSupport) {
					// this sample set has more inliers
					if (samples.size()>fitter.getMinimalDataSetSize()) {
						bestModel=fitter.computeModel(samples);
						
						if (monitor!=null)
							monitor.modelFromInliers(model, new BitSetElementSet(sampleMask, samples.size()), bestModel);
						
						if (bestModel==null) {
							/* 
							 * The fitter successfully computed a model from a subset of this data but failed now.
							 * The extra 'inliers' have somehow brought the data set to a degenerate case. This is
							 * highly unlikely but not impossible.
							 * 
							 * My decision now is that we just publish the model build from the minimal set. That
							 * one still has all the samples as inliers anyway.
							 */
							bestModel=model;
						}
					} else {
						// This is still a minimal set, no extra inliers were added.
						// No need to regenerate model.
						bestModel=model;
						
						if (monitor!=null)
							monitor.modelFromInliers(model, new BitSetElementSet(sampleMask, samples.size()), bestModel);
					}
					bestModelSupport=samples.size();
					bestModelInliers.clear();
					bestModelInliers.or(sampleMask);
					
					// update our estimate of inliars, and thus the number of iterations required
					double inlierRatio=bestModelSupport/(double)data.size();
					iterationCount=(int)Math.round(Math.log1p(-successProbability)/Math.log1p(-Math.pow(inlierRatio, fitter.getMinimalDataSetSize())));
				}
				iteration++;
			}
		}
		if (monitor!=null)
			monitor.success(bestModel, new BitSetElementSet(bestModelInliers, bestModelSupport));
		return bestModel;
	}
	
}
