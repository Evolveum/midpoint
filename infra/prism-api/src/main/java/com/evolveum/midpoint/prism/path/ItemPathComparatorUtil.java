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

import com.evolveum.midpoint.util.QNameUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 *
 */
class ItemPathComparatorUtil {

	/**
	 * Alternative to normalization: reads the same sequence of segments of 'path' as segments of 'path.normalize()'
	 */
	private static class ItemPathNormalizingIterator implements Iterator<Object> {
		private static final IdItemPathSegment NULL_ID_ITEM_PATH_SEGMENT = new IdItemPathSegment();
		final ItemPath path;
		final List<?> components;
		private int i = 0;
		private boolean nextIsArtificialId = false;

		ItemPathNormalizingIterator(ItemPath path) {
			this.path = path != null ? path : ItemPath.EMPTY_PATH;
			this.components = this.path.getSegments();
		}

		@Override
		public boolean hasNext() {
			// note that if i == path.size(), nextIsArtificialId is always false
			return i < components.size();
		}

		@Override
		public Object next() {
			if (i >= components.size()) {
				throw new IndexOutOfBoundsException("Index: " + i + ", path size: " + components.size() + ", path: " + path);
			} else if (nextIsArtificialId) {
				nextIsArtificialId = false;
				return NULL_ID_ITEM_PATH_SEGMENT;
			} else if (i == components.size() - 1) {
				// the last segment: nothing will be added
				return components.get(i++);
			} else {
				Object rv = components.get(i++);
				if (!ItemPath.isId(rv) && !(ItemPath.isId(components.get(i)))) {
					nextIsArtificialId = true;			// next one returned will be artificial id segment
				}
				return rv;
			}
		}
	}

	private static ItemPathNormalizingIterator normalizingIterator(ItemPath path) {
		return new ItemPathNormalizingIterator(path);
	}

	public static ItemPath.CompareResult compareComplex(@Nullable ItemPath path1, @Nullable ItemPath path2) {
		ItemPathNormalizingIterator iterator1 = normalizingIterator(path1);
		ItemPathNormalizingIterator iterator2 = normalizingIterator(path2);
		while (iterator1.hasNext() && iterator2.hasNext()) {
			Object o1 = iterator1.next();
			Object o2 = iterator2.next();
			if (!segmentsEquivalent(o1, o2)) {
				return ItemPath.CompareResult.NO_RELATION;
			}
		}
		if (iterator1.hasNext()) {
			return ItemPath.CompareResult.SUPERPATH;				// "this" is longer than "other"
		}
		if (iterator2.hasNext()) {
			return ItemPath.CompareResult.SUBPATH;				// "this" is shorter than "other"
		}
		return ItemPath.CompareResult.EQUIVALENT;
	}

	static boolean segmentsEquivalent(Object o1, Object o2) {
		if (ItemPath.isName(o1)) {
			return ItemPath.isName(o2) && QNameUtil.match(ItemPath.toName(o1), ItemPath.toName(o2));
		} else if (ItemPath.isVariable(o1)) {
			return ItemPath.isVariable(o2) && QNameUtil.match(ItemPath.toVariableName(o1), ItemPath.toVariableName(o2));
		} else if (ItemPath.isSpecial(o1)) {
			return ItemPath.isSpecial(o2) && QNameUtil.match(ItemPathSegmentUtil.getSpecialSymbol(o1), ItemPathSegmentUtil.getSpecialSymbol(o2));
		} else if (ItemPath.isId(o1)) {
			return ItemPath.isId(o2) && Objects.equals(ItemPath.toId(o1), ItemPath.toId(o2));
		} else {
			return false;
		}
	}

	public static boolean equivalent(ItemPath path1, ItemPath path2) {
		return compareComplex(path1, path2) == ItemPath.CompareResult.EQUIVALENT;
	}

	public static boolean isSubPath(ItemPath path1, ItemPath path2) {
		return compareComplex(path1, path2) == ItemPath.CompareResult.SUBPATH;
	}

	public static boolean isSuperPath(ItemPath path1, ItemPath path2) {
		return compareComplex(path1, path2) == ItemPath.CompareResult.SUPERPATH;
	}

	public static boolean isSuperPathOrEquivalent(ItemPath path1, ItemPath path2) {
		ItemPath.CompareResult result = compareComplex(path1, path2);
		return result == ItemPath.CompareResult.SUPERPATH || result == ItemPath.CompareResult.EQUIVALENT;
	}

	public static boolean isSubPathOrEquivalent(ItemPath path1, ItemPath path2) {
		ItemPath.CompareResult result = compareComplex(path1, path2);
		return result == ItemPath.CompareResult.SUBPATH || result == ItemPath.CompareResult.EQUIVALENT;
	}

	public static ItemPath remainder(ItemPath main, ItemPath prefix) {
		ItemPathNormalizingIterator mainIterator = normalizingIterator(main);
		ItemPathNormalizingIterator prefixIterator = normalizingIterator(prefix);
		while (prefixIterator.hasNext()) {
			if (!mainIterator.hasNext()) {
				throw new IllegalArgumentException("Cannot subtract '"+prefix+"' from path '"+main+
						"' because it is not a prefix (subpath): it is a superpath instead.");
			}
			Object mainSegment = mainIterator.next();
			Object prefixSegment = prefixIterator.next();
			if (!segmentsEquivalent(mainSegment, prefixSegment)) {
				throw new IllegalArgumentException("Cannot subtract segment '"+prefixSegment+"' from path '"+main+
						"' because it does not contain corresponding segment; it has '"+mainSegment+"' instead.");
			}
		}
		return ItemPathImpl.createFromIterator(mainIterator);
	}

}
