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

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

/**
 * A growable array which provides access to the raw array but does not own the elements inside of the array. When
 * it is inexpensive to do so (O(1) operation) it will discard references to data when they are no longer needed.
 *
 * @author Peter Abeles
 */
public class FastArray<T> extends FastAccess<T> {

	// Wrapper around this class for lists
	private final FastArrayList<T> list = new FastArrayList<>(this);

	public FastArray( Class<T> type , int initialMaxSize ) {
		super(type);
		this.size = 0;
		data = (T[]) Array.newInstance(type, initialMaxSize);
	}

	public FastArray( Class<T> type ) {
		this(type,10);
	}

	public void set( int index, T value ) {
		if( index < 0 || index >= size )
			throw new IllegalArgumentException("Out of bounds. index="+index+" max size "+size);
		data[index] = value;
	}

	public void add( T value ) {
		if( size >= data.length ) {
			growArray((data.length+1)*2);
		}
		data[size++] = value;
	}

	@Override
	public T remove( int index ) {
		T removed = data[index];
		for( int i = index+1; i < size; i++ ) {
			data[i-1] = data[i];
		}
		data[size-1] = null;
		size--;
		return removed;
	}

	@Override
	public T removeSwap( int index ) {
		if( index < 0 || index >= size )
			throw new IllegalArgumentException("Out of bounds. index="+index+" max size "+size);
		T ret = data[index];
		size -= 1;
		data[index] = data[size];
		data[size] = null;
		return ret;
	}

	public T removeTail() {
		if( size <= 0 )
			throw new IllegalArgumentException("The array is empty");
		size -= 1;
		T ret = data[size];
		data[size] = null;
		return ret;
	}

	/**
	 * Sets the size of the list to zero. External references are not modified.
	 */
	public void reset() {
		size = 0;
	}

	/**
	 * Sets the size of the list to zero and removes all internal references inside the current array.
	 */
	public void clear() {
		Arrays.fill(data,0,size,null);
		size = 0;
	}

	/**
	 * Increases the size of the internal array without changing the shape's size. If the array
	 * is already larger than the specified length then nothing is done.  Elements previously
	 * stored in the array are copied over is a new internal array is declared.
	 *
	 * @param length Requested size of internal array.
	 */
	public void growArray( int length) {
		// now need to grow since it is already larger
		if( this.data.length >= length)
			return;

		T []data = (T[])Array.newInstance(type, length);
		System.arraycopy(this.data,0,data,0,size);
		this.data = data;
	}

	/**
	 * Returns the first index which equals() obj. -1 is there is no match
	 *
	 * @param obj The object being searched for
	 * @return index or -1 if not found
	 */
	public int indexOf( T obj ) {
		for (int i = 0; i < size; i++) {
			if( data[i].equals(obj) ) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Changes the size to the specified length. Equivalent to calling {@link #growArray} and this.size = N.
	 * @param length The new size of the queue
	 */
	public void resize(int length) {
		growArray(length);
		this.size = length;
	}

	/**
	 * Returns true if the object is contained inside the array
	 */
	public boolean contains(T o) {
		for( int i = 0; i < size; i++ ) {
			if( data[i].equals(o) )
				return true;
		}

		return false;
	}

	public void addAll( FastAccess<T> list ) {
		for( int i = 0; i < list.size; i++ ) {
			add( list.data[i]);
		}
	}

	public void add( T[] array , int first, int length ) {
		for( int i = 0; i < length; i++ ) {
			add( array[first+i]);
		}
	}

	public void addAll( final List<T> list ) {
		final int originalSize = this.size;
		resize(this.size+list.size());
		for (int i = 0; i < list.size(); i++) {
			data[originalSize+i] = list.get(i);
		}
	}

	/**
	 * Returns a wrapper around FastQueue that allows it to act as a read only list.
	 * There is little overhead in using this interface.
	 *
	 * NOTE: The same instead of a list is returned each time.  Be careful when writing
	 * concurrent code and create a copy.
	 *
	 * @return List wrapper.
	 */
	@Override
	public List<T> toList() {
		return list;
	}
}
