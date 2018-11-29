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

package com.evolveum.midpoint.prism.path;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.*;

import static com.evolveum.midpoint.prism.path.UniformItemPathImpl.EMPTY_PATH;

/**
 * Methods that operate on path collections.
 */
public class ItemPathCollectionsUtil {

	/**
	 * Returns true if the collection contains a path equivalent to pathToBeFound.
	 */
	public static boolean containsEquivalent(Collection<? extends ItemPath> paths, ItemPath pathToBeFound) {
		for (ItemPath path : paths) {
			if (path.equivalent(pathToBeFound)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if the collection contains a superpath of the given path.
	 * I.e. having collection = { A/B, A/C }
	 * then the method for this collection and 'path' returns:
	 *  - path = A/B -&gt; false
	 *  - path = A -&gt; false
	 *  - path = A/B/C -&gt; true
	 *  - path = X -&gt; false
	 */
	public static boolean containsSubpath(Collection<? extends ItemPath> paths, ItemPath pathToBeFound) {
		for (ItemPath path : paths) {
			if (path.isSubPath(pathToBeFound)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if the collection contains a superpath of or equivalent path to the given path.
	 * I.e. having collection = { A/B, A/C }
	 * then the method for this collection and 'path' returns:
	 *  - path = A/B -&gt; true
	 *  - path = A -&gt; true
	 *  - path = A/B/C -&gt; false
	 *  - path = X -&gt; false
	 */
	public static boolean containsSuperpathOrEquivalent(Collection<? extends ItemPath> paths, ItemPath pathToBeFound) {
		for (ItemPath path : paths) {
			if (path.isSuperPathOrEquivalent(pathToBeFound)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if the collection contains a superpath of the given path.
	 * I.e. having collection = { A/B, A/C }
	 * then the method for this collection and 'path' returns:
	 *  - path = A/B -&gt; false
	 *  - path = A -&gt; true
	 *  - path = A/B/C -&gt; false
	 *  - path = X -&gt; false
	 */
	public static boolean containsSuperpath(Collection<? extends ItemPath> paths, ItemPath pathToBeFound) {
		for (ItemPath path : paths) {
			if (path.isSuperPath(pathToBeFound)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if the collection contains a subpath of or equivalent path to the given path.
	 * I.e. having collection = { A/B, A/C }
	 * then the method for this collection and 'path' returns:
	 *  - path = A/B -&gt; true
	 *  - path = A -&gt; false
	 *  - path = A/B/C -&gt; true
	 *  - path = X -&gt; false
	 */
	public static boolean containsSubpathOrEquivalent(Collection<? extends ItemPath> paths, ItemPath pathToBeFound) {
		for (ItemPath path : paths) {
			if (path.isSubPathOrEquivalent(pathToBeFound)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Strips the prefix from a set of paths.
	 *
	 * @param alsoEquivalent If true, 'prefix' in paths is processed as well (resulting in empty path). Otherwise, it is skipped.
	 */
	public static List<UniformItemPath> remainder(Collection<UniformItemPath> paths, ItemPath prefix, boolean alsoEquivalent) {
		List<UniformItemPath> rv = new ArrayList<>();
		for (UniformItemPath path : paths) {
			if (alsoEquivalent && path.isSuperPathOrEquivalent(prefix)
					|| !alsoEquivalent && path.isSuperPath(prefix)) {
				rv.add(path.remainder(prefix));
			}
		}
		return rv;
	}

	@NotNull
	public static List<UniformItemPath> pathListFromStrings(List<String> pathsAsStrings) {
		List<UniformItemPath> rv = new ArrayList<>();
		if (pathsAsStrings != null) {
			for (String pathAsString : pathsAsStrings) {
				rv.add(new ItemPathType(pathAsString).getUniformItemPath());
			}
		}
		return rv;
	}

	public static UniformItemPath[] asPathArray(QName... names) {
		UniformItemPath[] paths = new UniformItemPath[names.length];
		int i = 0;
		for (QName name : names) {
			paths[i++] = new UniformItemPathImpl(name);
		}
		return paths;
	}

	public static <T> T putToMap(Map<UniformItemPath, T> map, UniformItemPath itemPath, T value) {
		for (UniformItemPath key : map.keySet()) {
			if (key.equivalent(itemPath)) {
				return map.put(key, value);
			}
		}
		return map.put(itemPath, value);
	}

	public static <T> void putAllToMap(Map<UniformItemPath, T> target, Map<UniformItemPath, T> source) {
		for (Map.Entry<UniformItemPath, T> entry : source.entrySet()) {
			putToMap(target, entry.getKey(), entry.getValue());
		}
	}

	public static <T> T getFromMap(Map<UniformItemPath, T> map, UniformItemPath itemPath) {
		for (Map.Entry<UniformItemPath, T> entry : map.entrySet()) {
			if (entry.getKey().equivalent(itemPath)) {
				return entry.getValue();
			}
		}
		return null;
	}
}
