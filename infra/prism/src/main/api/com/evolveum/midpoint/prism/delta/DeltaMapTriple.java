/*
 * Copyright (c) 2010-2018 Evolveum
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
import com.evolveum.midpoint.util.Cloner;
import com.evolveum.midpoint.util.DebugDumpable;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Radovan Semancik
 */
public interface DeltaMapTriple<K,V> extends DebugDumpable, Serializable, SimpleVisitable<Map.Entry<K, V>> {

	Map<K,V> getZeroMap();

	Map<K,V> getPlusMap();

	Map<K,V> getMinusMap();

	Map<K,V> getMap(PlusMinusZero plusMinusZero);

	boolean hasPlusMap();

	boolean hasZeroMap();

	boolean hasMinusMap();

	boolean isZeroOnly();

	void addToPlusMap(K key, V value);

	void addToMinusMap(K key, V value);

	void addToZeroMap(K key, V value);

	void addAllToPlusMap(Map<K, V> map);

	void addAllToMinusMap(Map<K, V> map);

	void addAllToZeroMap(Map<K, V> map);

	void addAllToMap(PlusMinusZero destination, Map<K, V> map);

	void clearPlusMap();

	void clearMinusMap();

	void clearZeroMap();

	int size();

	void merge(DeltaMapTriple<K, V> triple);

	/**
	 * Returns all values, regardless of the internal sets.
	 */
	Collection<K> unionKeySets();

	DeltaMapTriple<K,V> clone(Cloner<Entry<K, V>> cloner);

	boolean isEmpty();
}
