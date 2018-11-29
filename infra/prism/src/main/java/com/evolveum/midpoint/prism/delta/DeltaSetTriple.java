/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.prism.delta;

import com.evolveum.midpoint.prism.SimpleVisitable;
import com.evolveum.midpoint.prism.SimpleVisitor;
import com.evolveum.midpoint.util.*;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * The triple of values (added, unchanged, deleted) that represents difference between two collections of values.
 * <p>
 * The DeltaSetTriple is used as a result of a "diff" operation or it is constructed to determine a ObjectDelta or
 * PropertyDelta. It is a very useful structure in numerous situations when dealing with relative changes.
 * <p>
 * DeltaSetTriple (similarly to other parts of this system) deal only with unordered values.
 *
 * @author Radovan Semancik
 */
public interface DeltaSetTriple<T> extends DebugDumpable, ShortDumpable, Serializable, SimpleVisitable<T>, Foreachable<T> {

	/**
	 * Compares two (unordered) collections and creates a triple describing the differences.
	 */
	static <T> DeltaSetTriple<T> diff(Collection<T> valuesOld, Collection<T> valuesNew) {
		DeltaSetTriple<T> triple = new DeltaSetTripleImpl<>();
		DeltaSetTripleImpl.diff(valuesOld, valuesNew, triple);
		return triple;
	}

	@NotNull
	Collection<T> getZeroSet();

	@NotNull
	Collection<T> getPlusSet();

	@NotNull
	Collection<T> getMinusSet();

	boolean hasPlusSet();

	boolean hasZeroSet();

	boolean hasMinusSet();

	boolean isZeroOnly();

	void addToPlusSet(T item);

	void addToMinusSet(T item);

	void addToZeroSet(T item);

	void addAllToPlusSet(Collection<T> items);

	void addAllToMinusSet(Collection<T> items);

	void addAllToZeroSet(Collection<T> items);

	Collection<T> getSet(PlusMinusZero whichSet);

	void addAllToSet(PlusMinusZero destination, Collection<T> items);

	void addToSet(PlusMinusZero destination, T item);

	boolean presentInPlusSet(T item);

	boolean presentInMinusSet(T item);

	boolean presentInZeroSet(T item);

	void clearPlusSet();

	void clearMinusSet();

	void clearZeroSet();

	int size();

	/**
	 * Returns all values, regardless of the internal sets.
	 */
	Collection<T> union();

	T getAnyValue();

	Collection<T> getAllValues();

	Stream<T> stream();

	@SuppressWarnings("unchecked")
	@NotNull
	Collection<T> getNonNegativeValues();

	@SuppressWarnings("unchecked")
	@NotNull
	Collection<T> getNonPositiveValues();

	void merge(DeltaSetTriple<T> triple);

	DeltaSetTriple<T> clone(Cloner<T> cloner);

	boolean isEmpty();

	/**
	 * Process each element of every set.
	 * This is different from the visitor. Visitor will go
	 * deep inside, foreach will remain on the surface.
	 */
	@Override
	void foreach(Processor<T> processor);

	@Override
	void simpleAccept(SimpleVisitor<T> visitor);

	<X> void transform(DeltaSetTriple<X> transformTarget, Transformer<T, X> transformer);

	void debugDumpSets(StringBuilder sb, Consumer<T> dumper, int indent);

	String toHumanReadableString();
}
