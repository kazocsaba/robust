package hu.kazocsaba.robust;

import hu.kazocsaba.robust.RobustEstimator.Monitor;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * RECON robust estimator.
 * @see "Rahul Raguram and Jan-Michael Frahm: RECON: Scale-Adaptive Robust Estimation via Residual Consensus
 * (in International Conference on Computer Vision, November 2011)"
 * @param <D> the type of the data elements
 * @param <M> the type of the model
 * @author Kazó Csaba
 */
public final class Recon<D,M> extends RobustEstimator<D, M, Recon.Monitor<D, M>> {
	/** The maximal number the fitter is allowed to fail to generate a model from a minimal data set. */
	private int maxModelFailCount=1000;
	
	/**
	 * The minimal value required for the commonCount/totalCount for the chunks of the residual elements. In the
	 * article, this is alpha squared.
	 */
	double minOverlapFraction=.99*.99;
	
	/** The source of randomness. */
	private Random rnd=new Random();;

	/**
	 * Sets the (only) source of randomness used by the algorithm. This function can be used to ensure deterministic
	 * execution: if the configuration of {@code Recon} is not changed, and the same input is provided, then the
	 * behaviour of the algorithm will be exactly the same as long as the calls to {@code perform} are preceded by
	 * setting {@code Random} instances which produce identical sequences of numbers.
	 * @param random the source of randomness this {@code Recon} instance will use
	 */
	public void setRandom(Random random) {
		if (random == null) throw new NullPointerException();
		rnd = random;
	}
	
	private static class Residual implements Comparable<Residual> {
		public int index;
		public double error;

		public Residual(int index, double error) {
			this.index = index;
			this.error = error;
		}

		@Override
		public int compareTo(Residual o) {
			return Double.compare(error, o.error);
		}
	}
	
	private class ModelData {
		private List<Residual> residuals;
		private M model;
		private BitSet minimalSampleSet;

		public ModelData(M model, BitSet minimalSampleSet, Fitter<D, M> fitter, List<D> data) {
			this.model = model;
			this.minimalSampleSet = minimalSampleSet;
			
			residuals=new ArrayList<Residual>(data.size());
			
			for (int index=0; index<data.size(); index++)
				residuals.add(new Residual(index, fitter.getError(model, data.get(index))));
			Collections.sort(residuals);
		}
		
	}
	
	private class MatchingModelPair {
		private ModelData model1, model2;
		private BitSet commonData;

		public MatchingModelPair(ModelData model1, ModelData model2, BitSet commonData) {
			this.model1 = model1;
			this.model2 = model2;
			this.commonData = commonData;
		}
		
	}
	
	/**
	 * Monitor class for {@link Recon}.
	 * <p>
	 * The algorithm iteratively generates minimal sample sets, then fits a model to them. For each of these steps,
	 * {@link #modelFromMinimalSampleSet(ElementSet, Object)} is called with the model the fitter returned, possibly
	 * {@code null}.
	 * <p>
	 * Then this new model is checked with each of the previously generated ones for alpha-consistency. If an existing
	 * model is found to be consistent with the new one, then {@link #modelPairConsistent(Object, Object, BitSet)} is
	 * called, otherwise {@link #modelPairNotConsistent(Object, Object, BitSet)}. 
	 * 
	 * <p>
	 * The search terminates successfully when three mutually consistent models are found. This is indicated by a call to
	 * {@link Monitor#success(Object, ElementSet)} with the common consistent data as the inlier set and the final model.
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
		 * Called after a new model has been found consistent with a previous one.
		 * @param newModel the new model
		 * @param existingModel the existing model consistent with the new one
		 * @param consistentData bit set representing the data elements indicating consistency
		 */
		public void modelPairConsistent(M newModel, M existingModel, BitSet consistentData) {}
		/**
		 * Called after a new model has been found inconsistent with a previous one.
		 * @param newModel the new model
		 * @param existingModel the existing model (not consistent with the new one)
		 * @param consistentData bit set representing the data elements indicating consistency
		 */
		public void modelPairNotConsistent(M newModel, M existingModel, BitSet consistentData) {}
	}
	
	@Override
	public M perform(Fitter<D, M> fitter, List<D> data, Monitor<D, M> monitor) throws NoModelFoundException {
		List<D> samples=new ArrayList<D>(data.size()/2);
		BitSet sampleMask=new BitSet(data.size());
		int modelFailCount=0; // the number of random sample sets which failed to yield a model
		
		Map<BitSet, ModelData> storedModelData=new HashMap<BitSet, ModelData>();
		Collection<MatchingModelPair> matchingModelPairs=new ArrayList<MatchingModelPair>();
		
		for (int tries=0; tries<200; tries++) {

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
			
			if (storedModelData.containsKey(sampleMask))
				// we've already tried this sample set
				continue;

			// try to compute a model

			M model=fitter.computeModel(samples);
			if (monitor!=null)
				monitor.modelFromMinimalSampleSet(new BitSetElementSet(sampleMask, data.size()), model);

			if (model==null) {
				modelFailCount++;
				if (modelFailCount==maxModelFailCount)
					throw new NoModelFoundException("Fitter failed to compute a model from a minimal data set too many times");
			} else {
				// compute and sort the residuals

				ModelData newModelData=new ModelData(model, (BitSet)sampleMask.clone(), fitter, data);

				// check previous models for overlap

				Map<ModelData, BitSet> matchingPreviousModels=new HashMap<ModelData, BitSet>(); // value is the common inlier set

				for (ModelData existingModelData: storedModelData.values()) {
					if (newModelData.minimalSampleSet.equals(existingModelData.minimalSampleSet)) {
						// these are from the same input sets, don't compare them
						continue;
					}
					checkAlphaConsistency(newModelData, existingModelData, matchingPreviousModels, monitor);
				}

				if (!matchingPreviousModels.isEmpty()) {
					/*
					 * At least one previous model was found which matches with the new one.
					 * We're trying to find a set of 3 mutually matching models. So we check all existing pairs to see if
					 * both match the current one.
					 */
					for (MatchingModelPair existingPair: matchingModelPairs) {
						if (matchingPreviousModels.containsKey(existingPair.model1) && matchingPreviousModels.containsKey(existingPair.model2)) {
							// There you go!
							BitSet commonBetweenNewAnd1=matchingPreviousModels.get(existingPair.model1);
							BitSet commonBetweenNewAnd2=matchingPreviousModels.get(existingPair.model2);

							BitSet commonBetweenAllThree=(BitSet)existingPair.commonData.clone();
							commonBetweenAllThree.and(commonBetweenNewAnd1);
							commonBetweenAllThree.and(commonBetweenNewAnd2);

							int maxCommonCount=Math.max(Math.max(commonBetweenNewAnd1.cardinality(), commonBetweenNewAnd2.cardinality()), existingPair.commonData.cardinality());
							int minCommonCount=Math.min(Math.min(commonBetweenNewAnd1.cardinality(), commonBetweenNewAnd2.cardinality()), existingPair.commonData.cardinality());

							if ((maxCommonCount-minCommonCount)/(double)maxCommonCount<=.05 &&
									commonBetweenAllThree.cardinality()>=.02*data.size()) {
								// Overlap is okey, we're done!

								List<D> commonData=new ArrayList<D>(commonBetweenAllThree.cardinality());
								for (int i=commonBetweenAllThree.nextSetBit(0); i>=0; i=commonBetweenAllThree.nextSetBit(i+1))
									commonData.add(data.get(i));
								M inlierModel=fitter.computeModel(commonData);
								if (inlierModel==null)
									inlierModel=model;
								if (monitor!=null)
									monitor.success(inlierModel, new BitSetElementSet(commonBetweenAllThree, data.size()));
								return inlierModel;
							}
						}
					}
				}

				// store the stuff
				storedModelData.put(newModelData.minimalSampleSet, newModelData);
				for (Map.Entry<ModelData, BitSet> matchingModel: matchingPreviousModels.entrySet()) {
					matchingModelPairs.add(new MatchingModelPair(newModelData, matchingModel.getKey(), matchingModel.getValue()));
				}
			}
		}
		throw new NoModelFoundException("Maximum attempt count reached, no consensus");
	}
	/**
	 * This function checks two models for alpha-consistency, and if they are found to be consistent, stores the existing
	 * model and the corresponding consistent data set in the {@code matchingPreviousModel} map.
	 */
	private void checkAlphaConsistency(
			ModelData newModelData,
			ModelData existingModelData,
			Map<ModelData, BitSet> matchingPreviousModel,
			Monitor<D,M> monitor) {
		// check progressively greater chunks of the residuals, watch the overlap
		int dataSize=newModelData.residuals.size();
		
		BitSet seenInOneModel=new BitSet(dataSize);
		BitSet seenInBothModels=new BitSet(dataSize);
		int commonPointCount=0;
		boolean consistent=false;

		for (int n=0; n<dataSize; n++) {
			// chunk 0..n
			int residualIndex1=newModelData.residuals.get(n).index;
			int residualIndex2=existingModelData.residuals.get(n).index;
			if (residualIndex1==residualIndex2) {
				// the n-th element is the same
				seenInOneModel.set(residualIndex1);
				seenInBothModels.set(residualIndex1);
			} else {
				// different elements
				if (seenInOneModel.get(residualIndex1)) {
					// we've encountered the point from chunk 1 before, in chunk 2
					seenInBothModels.set(residualIndex1);
					commonPointCount++;
				} else {
					// n-th point of chunk 1 is new
					seenInOneModel.set(residualIndex1);
				}
				if (seenInOneModel.get(residualIndex2)) {
					// we've encountered the point from chunk 2 before, in chunk 1
					seenInBothModels.set(residualIndex2);
					commonPointCount++;
				} else {
					// n-th point of chunk 2 is new
					seenInOneModel.set(residualIndex2);
				}
			}

			// okey, chunk 0..n has been processed
			double chunkSize=n+1;
			if (commonPointCount/chunkSize>=minOverlapFraction) {
				if (chunkSize/dataSize<.9 && chunkSize/dataSize>.1) {
					// these two models match with regard to the residuals
					consistent=true;
					break;
				}
			}
		}
		if (consistent) {
			matchingPreviousModel.put(existingModelData, seenInBothModels);
			if (monitor!=null)
				monitor.modelPairConsistent(newModelData.model, existingModelData.model, seenInBothModels);
		} else {
			if (monitor!=null)
				monitor.modelPairNotConsistent(newModelData.model, existingModelData.model, null);
		}
	}

}
