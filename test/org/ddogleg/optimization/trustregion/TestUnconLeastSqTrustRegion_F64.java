/*
 * Copyright (c) 2012-2018, Peter Abeles. All Rights Reserved.
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

package org.ddogleg.optimization.trustregion;

import org.ddogleg.optimization.MockFunctionNtoM;
import org.ddogleg.optimization.UnconstrainedLeastSquares;
import org.ddogleg.optimization.math.HessianLeastSquares_DDRM;
import org.ddogleg.optimization.math.MatrixMath_DDRM;
import org.ddogleg.optimization.wrap.GenericUnconstrainedLeastSquaresTests_F64;
import org.ejml.UtilEjml;
import org.ejml.data.DMatrixRMaj;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Specific configurations are tested in their respective TrustRegionUpdate implementations
 *
 * @author Peter Abeles
 */
public class TestUnconLeastSqTrustRegion_F64 extends GenericUnconstrainedLeastSquaresTests_F64 {
	/**
	 * Makes sure the specified tolerances are copied into the configuration
	 */
	@Test
	public void initialize() {
		UnconLeastSqTrustRegion_F64<DMatrixRMaj> alg = createAlg();
		alg.functionResiduals = new MockFunctionNtoM(new double[]{1,2,3},3);
		alg.initialize(new double[]{1,2,3},2e-3,6e-12);

		assertEquals(2e-3,alg.config.ftol, UtilEjml.TEST_F64);
		assertEquals(6e-12,alg.config.gtol, UtilEjml.TEST_F64);
	}

	@Test
	public void costFunction() {
		UnconLeastSqTrustRegion_F64<DMatrixRMaj> alg = createAlg();
		alg.functionResiduals = new MockFunctionNtoM(new double[]{-1,2,-3},1);
		alg.residuals.reshape(3,1);

		double expected = 0.5*(1+4+9);
		double found = alg.cost(new DMatrixRMaj(1,1));
		assertEquals(expected,found, UtilEjml.TEST_F64);
	}


	protected UnconLeastSqTrustRegion_F64<DMatrixRMaj> createAlg() {
		TrustRegionUpdateCauchy_F64<DMatrixRMaj> update = new TrustRegionUpdateCauchy_F64<>();
		HessianLeastSquares_DDRM hessian = new HessianLeastSquares_DDRM();
		MatrixMath_DDRM math = new MatrixMath_DDRM();
		return new UnconLeastSqTrustRegion_F64<>(update,hessian,math);
	}

	@Override
	public UnconstrainedLeastSquares<DMatrixRMaj> createAlgorithm() {
		return createAlg();
	}
}