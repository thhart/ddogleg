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

package org.ddogleg.struct;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Peter Abeles
 */
public class TestGrowQueue_B {

	@Test
	void auto_grow() {
		GrowQueue_B alg = new GrowQueue_B(3);

		assertEquals(3,alg.data.length);

		for( int i = 0; i < 10; i++ )
			alg.push((i%2)==0);

		assertEquals(10,alg.size);

		for( int i = 0; i < 10; i++ )
			assertEquals((i%2)==0,alg.get(i));
	}

	@Test
	void reset() {
		GrowQueue_B alg = new GrowQueue_B(10);

		alg.push(true);
		alg.push(false);
		alg.push(false);

		assertTrue(alg.get(0));
		assertEquals(3,alg.size);

		alg.reset();

		assertEquals(0, alg.size);
	}

	@Test
	void push_pop() {
		GrowQueue_B alg = new GrowQueue_B(10);

		alg.push(false);
		alg.push(true);

		assertEquals(2,alg.size);
		assertTrue(alg.pop());
		assertFalse(alg.pop());
		assertEquals(0, alg.size);
	}

	@Test
	void remove_two() {
		GrowQueue_B alg = new GrowQueue_B(10);

		alg.push(true);
		alg.push(true);
		alg.push(false);
		alg.push(true);
		alg.push(false);

		alg.remove(1,1);
		assertEquals(4,alg.size);
		assertTrue(alg.get(0));
		assertFalse(alg.get(1));
		assertTrue(alg.get(2));
		assertFalse(alg.get(3));
		alg.remove(0,1);
		assertEquals(2,alg.size);
		assertTrue(alg.get(0));
		assertFalse(alg.get(1));
	}

	@Test
	void indexOf() {
		GrowQueue_B alg = new GrowQueue_B(10);

		alg.push(true);
		alg.push(false);
		alg.push(false);
		alg.push(true);

		assertEquals(0,alg.indexOf(true));
		assertEquals(1,alg.indexOf(false));
	}

	@Test
	void getTail() {
		GrowQueue_B alg = new GrowQueue_B(20);

		for (int i = 0; i < 20; i++) {
			alg.add(i%2==0);
		}

		for (int i = 0; i < 20; i++) {
			assertEquals((20-i-1)%2==0,alg.getTail(i));
		}
	}
}
