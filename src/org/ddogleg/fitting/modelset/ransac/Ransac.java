/*
 * Copyright (c) 2012-2020, Peter Abeles. All Rights Reserved.
 *
 * This file is part of DDogleg (http://ddogleg.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ddogleg.fitting.modelset.ransac;

import org.ddogleg.fitting.modelset.DistanceFromModel;
import org.ddogleg.fitting.modelset.ModelGenerator;
import org.ddogleg.fitting.modelset.ModelManager;
import org.ddogleg.fitting.modelset.ModelMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


/**
 * <p>
 * RANSAC is an abbreviation for "RANdom SAmple Consensus" and is an iterative algorithm.  The model with the
 * largest set of inliers is found by randomly sampling the set of points and fitting a model.  The algorithm
 * terminates when the maximum number of iterations has been reached. An inlier is defined as a point which
 * has an error less than a user specified threshold to the estimated model being considered.  The algorithm was
 * first published by Fischler and Bolles in 1981."
 * </p>
 *
 * <p>
 * Sample Points: By default the minimum number of points are sampled.  The user to override this default and set
 * it to any number.
 * </p>
 *
 * @author Peter Abeles
 */
public class Ransac<Model, Point> implements ModelMatcher<Model,Point> {
	// how many points are drawn to generate the model
	protected int sampleSize;

	// how close a point needs to be considered part of the model
	protected double thresholdFit;

	// generates an initial model given a set of points
	protected ModelGenerator<Model,Point> modelGenerator;
	// computes the distance a point is from the model
	protected DistanceFromModel<Model,Point> modelDistance;

	// used to randomly select points/samples
	protected Random rand;
	protected long randSeed;

	// list of points which are a candidate for the best fit set
	protected List<Point> candidatePoints = new ArrayList<Point>();

	// list of samples from the best fit model
	protected List<Point> bestFitPoints = new ArrayList<Point>();

	// the best model found so far
	protected Model bestFitParam;
	// the current model being considered
	protected Model candidateParam;

	// the maximum number of iterations it will perform
	protected int maxIterations;

	// copy of the input data set so that it can be modified
	protected List<Point> dataSet = new ArrayList<>();

	// the set of points which were initially sampled
	protected List<Point> initialSample = new ArrayList<Point>();

	// list of indexes converting it from match set to input list
	protected int []matchToInput = new int[1];
	protected int []bestMatchToInput = new int[1];

	/**
	 * Creates a new instance of the ransac algorithm.  The number of points sampled will default to the
	 * minimum number.  To override this default invoke {@link #setSampleSize(int)}.
	 *
	 * @param randSeed The random seed used by the random number generator.
	 * @param modelGenerator Creates new model(s) given a small number of points.
	 * @param modelDistance Computes the difference between a point an a model.
	 * @param maxIterations The maximum number of iterations the RANSAC algorithm will perform.
	 * @param thresholdFit How close of a fit a points needs to be to the model to be considered a fit.
	 */
	public Ransac(long randSeed,
				  ModelManager<Model> modelManager,
				  ModelGenerator<Model, Point> modelGenerator,
				  DistanceFromModel<Model, Point> modelDistance,
				  int maxIterations,
				  double thresholdFit) {
		this.modelGenerator = modelGenerator;
		this.modelDistance = modelDistance;

		this.randSeed = randSeed;
		this.rand = new Random(randSeed);
		this.maxIterations = maxIterations;

		this.bestFitParam = modelManager.createModelInstance();
		this.candidateParam = modelManager.createModelInstance();

		this.sampleSize = modelGenerator.getMinimumPoints();
		this.thresholdFit = thresholdFit;
	}

	@Override
	public boolean process(List<Point> _dataSet ) {

		// see if it has the minimum number of points
		if (_dataSet.size() < modelGenerator.getMinimumPoints() )
			return false;

		// the data set will be modified so a copy is needed. Otherwise indexes of match set will not
		// be correct
		dataSet.clear();
		dataSet.addAll(_dataSet);

		// configure internal data structures
		initialize(dataSet);

		// iterate until it has exhausted all iterations or stop if the entire data set
		// is in the inlier set
		for (int i = 0; i < maxIterations && bestFitPoints.size() != dataSet.size(); i++) {
			// sample the a small set of points
			randomDraw(dataSet, sampleSize, initialSample, rand);
			
			// get the candidate(s) for this sample set
			if( modelGenerator.generate(initialSample, candidateParam ) ) {

				// see if it can find a model better than the current best one
				selectMatchSet(_dataSet, thresholdFit, candidateParam);

				// save this results
				if (bestFitPoints.size() < candidatePoints.size()) {
					swapCandidateWithBest();
				}
			}
		}

		return bestFitPoints.size() > 0;
	}

	/**
	 * Initialize internal data structures
	 */
	public void initialize( List<Point> dataSet ) {
		bestFitPoints.clear();

		if( dataSet.size() > matchToInput.length ) {
			matchToInput = new int[ dataSet.size() ];
			bestMatchToInput = new int[ dataSet.size() ];
		}
	}

	/**
	 * Performs a random draw in the dataSet.  When an element is selected it is moved to the end of the list
	 * so that it can't be selected again.
	 *
	 * @param dataSet List that points are to be selected from.  Modified.
	 */
	public static <T> void randomDraw(List<T> dataSet, int numSample,
									  List<T> initialSample, Random rand) {
		initialSample.clear();

		for (int i = 0; i < numSample; i++) {
			// index of last element that has not been selected
			int indexLast = dataSet.size()-i-1;
			// randomly select an item from the list which has not been selected
			int indexSelected = rand.nextInt(indexLast+1);

			T a = dataSet.get(indexSelected);
			initialSample.add(a);

			// Swap the selected item with the last unselected item in the list. This way the selected
			// item can't be selected again and the last item can now be selected
			dataSet.set(indexSelected,dataSet.set(indexLast,a));
		}
	}

	/**
	 * Looks for points in the data set which closely match the current best
	 * fit model in the optimizer.
	 *
	 * @param dataSet The points being considered
	 */
	@SuppressWarnings({"ForLoopReplaceableByForEach"})
	protected void selectMatchSet(List<Point> dataSet, double threshold, Model param) {
		candidatePoints.clear();
		modelDistance.setModel(param);

		for (int i = 0; i < dataSet.size(); i++) {
			Point point = dataSet.get(i);

			double distance = modelDistance.computeDistance(point);
			if (distance < threshold) {
				matchToInput[candidatePoints.size()] = i;
				candidatePoints.add(point);
			}
		}
	}

	/**
	 * Turns the current candidates into the best ones.
	 */
	protected void swapCandidateWithBest() {
		List<Point> tempPts = candidatePoints;
		candidatePoints = bestFitPoints;
		bestFitPoints = tempPts;

		int tempIndex[] = matchToInput;
		matchToInput = bestMatchToInput;
		bestMatchToInput = tempIndex;

		Model m = candidateParam;
		candidateParam = bestFitParam;
		bestFitParam = m;
	}

	@Override
	public List<Point> getMatchSet() {
		return bestFitPoints;
	}

	@Override
	public int getInputIndex(int matchIndex) {
		return bestMatchToInput[matchIndex];
	}

	@Override
	public Model getModelParameters() {
		return bestFitParam;
	}

	@Override
	public double getFitQuality() {
		return bestFitPoints.size();
	}

	public int getMaxIterations() {
		return maxIterations;
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	@Override
	public int getMinimumSize() {
		return sampleSize;
	}

	@Override
	public void reset() {
		this.rand = new Random(randSeed);
	}

	/**
	 * Override the number of points that are sampled and used to generate models.  If this value
	 * is not set it defaults to the minimum number.
	 *
	 * @param sampleSize Number of sample points.
	 */
	public void setSampleSize(int sampleSize) {
		this.sampleSize = sampleSize;
	}

	public double getThresholdFit() {
		return thresholdFit;
	}

	public void setThresholdFit(double thresholdFit) {
		this.thresholdFit = thresholdFit;
	}

	@Override
	public Class<Point> getPointType() {
		return modelDistance.getPointType();
	}

	@Override
	public Class<Model> getModelType() {
		return modelDistance.getModelType();
	}
}