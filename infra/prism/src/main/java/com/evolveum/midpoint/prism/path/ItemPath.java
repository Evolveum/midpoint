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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.marshaller.ItemPathHolder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

/**
 * General interface to ItemPath objects.
 * A path is viewed as a sequence of path segments.
 *
 * There are three basic implementations of this interface:
 *
 * 1) ItemPathImpl
 * ===============
 *
 * This is memory-optimized implementation, minimizing the number of objects created during initialization.
 * Its segments contain plain objects (e.g. QNames, Longs), mixed with ItemPathSegments where necessary.
 * Please see ItemPathImpl for details.
 *
 * 2) ItemName
 * ===========
 *
 * A subclass of QName representing a single-item path. It eliminates the need to create artificial ItemPath objects
 * to represent a single name.
 *
 * The problem with ItemPathImpl and ItemName is that equals/hashCode methods do not work on them as one would expect.
 * There are too many ways how to represent a given path, so one cannot rely on these methods. (QName.equals and hashCode
 * are final, so they cannot be adapted.)
 *
 * So, if ItemPath is to be used e.g. as a key in HashMap, the original implementation has to be used.
 *
 * 3) UniformItemPathImpl
 * ======================
 *
 * This is the original implementation. It sees the path as a sequence of ItemPathSegment objects.
 * Its advantage is the reasonable equals/hashCode methods. The disadvantage is the memory intensiveness
 * (creating a high number of tiny objects during path manipulations).
 *
 * Objects of ItemPath type are designed to be immutable. Modification operations in this API always create new objects.
 *
 * Naming convention:
 * - A path consists of SEGMENTS.
 * - However, when creating the path, we provide a sequence of COMPONENTS. We transform components into segments by applying
 *   a normalization procedure.
 */
public interface ItemPath extends ShortDumpable, Serializable {

	ItemPath EMPTY_PATH = ItemPathImpl.EMPTY_PATH;

	//region Creation and basic operations
	/**
	 * Creates the path from given components. The components can contain objects of various kinds:
	 * - QName -> interpreted as either named segment or a special segment (if the name exactly matches special segment name)
	 * - Integer/Long -> interpreted as Id path segment
	 * - null -> interpreted as null Id path segment
	 * - ItemPathSegment -> interpreted as such
	 * - ItemPath, Object[], Collection -> interpreted recursively as a sequence of components
	 *
	 * Creates the default implementation of ItemPathImpl. Components are normalized on creation as necessary; although
	 * the number of object creation is minimized.
	 */
	@NotNull
	static ItemPath create(Object... components) {
		return ItemPathImpl.createFromArray(components);
	}

	/**
	 * Creates the path from given components.
	 * @see ItemPath#create(Object...)
	 */
	@NotNull
	static ItemPath create(@NotNull List<?> components) {
		return ItemPathImpl.createFromList(components);
	}

	/**
	 * Returns true if the path is empty i.e. has no components.
	 */
	boolean isEmpty();

	/**
	 * Returns true if the path is null or empty.
	 */
	static boolean isEmpty(ItemPath path) {
		return path == null || path.isEmpty();
	}

	/**
	 * Returns path size i.e. the number of components.
	 */
	int size();

	/**
	 * Returns a newly created path containing all the segments of this path with added components.
	 */
	@NotNull
	default ItemPath append(Object... components) {
		return ItemPath.create(this, components);       // todo optimize this
	}

	/**
	 * Returns the path segments.
	 *
	 * Avoid using this method and access segments directly. Instead try to find suitable method in ItemPath interface.
	 * NEVER change path content using this method.
	 *
	 * TODO consider returning unmodifiable collection here (beware of performance implications)
	 */
	@NotNull
	List<?> getSegments();

	/**
	 * Returns the given path segment.
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	@Nullable
	Object getSegment(int i);

	//endregion

	//region Path comparison
	enum CompareResult {
		EQUIVALENT, SUPERPATH, SUBPATH, NO_RELATION
	}

	/**
	 * Compares two item paths.
	 */
	default CompareResult compareComplex(@Nullable ItemPath otherPath) {
		return ItemPathComparatorUtil.compareComplex(this, otherPath);
	}

	/**
	 * Checks if the paths are equivalent. Resolves some differences in path segment representation,
	 * e.g. NameItemPathSegment vs QName, null vs missing Id path segments.
	 *
	 * Does NOT detect higher-level semantic equivalency like activation[1]/administrativeStatus vs activation/administrativeStatus.
	 * These are treated as not equivalent.
	 */
	default boolean equivalent(ItemPath path) {
		return ItemPathComparatorUtil.equivalent(this, path);
	}

	static boolean equivalent(ItemPath path1, ItemPath path2) {
		return ItemPathComparatorUtil.equivalent(path1, path2);
	}

	/**
	 * Compares with the other object either literally (exact = true) or via .equivalent (exact = false).
	 */
	default boolean equals(Object other, boolean exact) {
		if (exact) {
			return equals(other);
		} else {
			return other instanceof ItemPath && equivalent((ItemPath) other);
		}
	}

	/**
	 * Checks if current path is a strict subpath (prefix) of the other path.
	 */
	default boolean isSubPath(ItemPath otherPath) {
		return ItemPathComparatorUtil.isSubPath(this, otherPath);
	}

	/**
	 * Check if current path is a subpath (prefix) of the other path or they are equivalent.
	 */
	default boolean isSubPathOrEquivalent(ItemPath otherPath) {
		return ItemPathComparatorUtil.isSubPathOrEquivalent(this, otherPath);
	}

	/**
	 * Check if the other path is a strict subpath (prefix) of this path.
	 * The same as otherPath.isSubPath(this).
	 */
	default boolean isSuperPath(ItemPath otherPath) {
		return ItemPathComparatorUtil.isSuperPath(this, otherPath);
	}

	/**
	 * Check if the other path is a subpath (prefix) of this path or they are equivalent.
	 * The same as otherPath.isSubPathOrEquivalent(this).
	 */
	default boolean isSuperPathOrEquivalent(ItemPath path) {
		return ItemPathComparatorUtil.isSuperPathOrEquivalent(this, path);
	}

	/**
	 * Convenience method with understandable semantics.
	 */
	default boolean startsWith(ItemPath prefix) {
		return isSuperPathOrEquivalent(prefix);
	}
	//endregion

	//region Determining segment types and extracting information from them (isXXX, toXXX)
	/*
	 *  Note that segments to be provided here are to be already normalized!
	 *  I.e. they are to be returned from first(), getSegments() or similar ItemPath methods.
	 */

	/**
	 * Returns true if the segment is a name segment.
	 *
	 * Note that special segments (parent, reference, identifier, variable) are NOT considered to be name segments,
	 * even if they can be represented using QName.
	 */
	static boolean isName(Object segment) {
		return ItemPathSegmentUtil.isName(segment);
	}

	/**
	 * Returns a name corresponding to the name segment, or throw an exception otherwise.
	 */
	@NotNull
	static ItemName toName(Object segment) {
		return ItemPathSegmentUtil.toName(segment, true);
	}

	/**
	 * Returns a name corresponding to the name segment, or throw an exception otherwise.
	 * However, accepts null segments.
	 *
	 * TODO determine whether to keep this method
	 */
	@SuppressWarnings("unused")
	@Nullable
	static QName toNameNullSafe(@Nullable Object segment) {
		return segment != null ? toName(segment) : null;
	}

	/**
	 * Returns a name corresponding to the name segment, or null if it's no name.
	 */
	@Nullable
	static ItemName toNameOrNull(Object segment) {
		return ItemPathSegmentUtil.toName(segment, false);
	}

	/**
	 * Returns true if the segment is the container Id.
	 */
	static boolean isId(Object o) {
		return ItemPathSegmentUtil.isId(o);
	}

	/**
	 * Returns true if the segment is the container Id with value of NULL.
	 */
	static boolean isNullId(Object o) {
		return ItemPathSegmentUtil.isNullId(o);
	}

	/**
	 * Returns a Long value corresponding to the container Id segment, or throw an exception otherwise.
	 */
	static Long toId(Object segment) {
		return ItemPathSegmentUtil.toId(segment, true);
	}

	/**
	 * Returns a Long value corresponding to the container Id segment, or return null otherwise.
	 */
	static Long toIdOrNull(Object segment) {
		return ItemPathSegmentUtil.toId(segment, false);
	}

	/**
	 * Returns true if the segment is a special one: parent, reference, identifier, variable.
	 */
	static boolean isSpecial(Object segment) {
		return ItemPathSegmentUtil.isSpecial(segment);
	}

	/**
	 * Returns true if the segment is the Parent one ("..").
	 */
	static boolean isParent(Object segment) {
		return ItemPathSegmentUtil.isParent(segment);
	}

	/**
	 * Returns true if the segment is the Object Reference one ("@").
	 */
	static boolean isObjectReference(Object segment) {
		return ItemPathSegmentUtil.isObjectReference(segment);
	}

	/**
	 * Returns true if the segment is the Identifier one ("#").
	 */
	static boolean isIdentifier(Object segment) {
		return ItemPathSegmentUtil.isIdentifier(segment);
	}

	/**
	 * Returns true if the segment is the Variable one ("$...").
	 */
	static boolean isVariable(Object segment) {
		return ItemPathSegmentUtil.isVariable(segment);
	}

	/**
	 * Returns a name corresponding to the Variable segment, or throw an exception otherwise.
	 */
	static QName toVariableName(Object segment) {
		return ItemPathSegmentUtil.toVariableName(segment);
	}
	//endregion


	//region Splitting the path

	/**
	 * Returns the first segment or null if the path is empty.
	 */
	@Nullable
	Object first();

	/**
	 * Returns the rest of the path (the tail).
	 */
	@NotNull
	default ItemPath rest() {
		return rest(1);
	}

	/**
	 * Returns the rest of the path (the tail), starting at position "n".
	 */
	@NotNull
	ItemPath rest(int n);

	/**
	 * Returns the first segment as an ItemPath.
	 * TODO consider the necessity of such method
	 */
	ItemPath firstAsPath();

	/**
	 * Returns the remainder of "this" path after passing all segments from the other path.
	 * (I.e. this path must begin with the content of the other path. Throws an exception when
	 * it is not the case.)
	 */
	default ItemPath remainder(ItemPath path) {
		return ItemPathComparatorUtil.remainder(this, path);
	}

	/**
	 * Returns the last segment (or null if the path is empty).
	 */
	@Nullable
	Object last();

	/**
	 * Returns all segments except the last one.
	 */
	@NotNull
	ItemPath allExceptLast();

	/**
	 * Returns all segments up to the specified one (including it).
	 */
	default ItemPath allUpToIncluding(int i) {
		return subPath(0, i+1);
	}

	/**
	 * Returns all segments up to the last named one (excluding).
	 * Returns empty path if there's no named segment.
	 */
	@NotNull
	default ItemPath allUpToLastName() {
		return subPath(0, lastNameIndex());
	}

	/**
	 * Returns a sub-path from (including) to (excluding) given indices.
	 */
	ItemPath subPath(int from, int to);

	//endregion

	//region Checks on path components and extracting information from them
	/**
	 * Returns true if the path starts with the standard segment name (i.e. NOT variable nor special symbol).
	 */
	default boolean startsWithName() {
		return !isEmpty() && ItemPath.isName(first());
	}

	/**
	 * Returns true if the path starts with with value Id.
	 */
	default boolean startsWithId() {
		return !isEmpty() && ItemPath.isId(first());
	}

	/**
	 * Returns true if the path starts with the value Id of null.
	 */
	default boolean startsWithNullId() {
		return !isEmpty() && ItemPath.isNullId(first());
	}

	/**
	 * Returns true if the path starts with an identifier (#).
	 */
	default boolean startsWithIdentifier() {
		return !isEmpty() && ItemPath.isIdentifier(first());
	}

	/**
	 * Returns true if the path starts with variable name ($...).
	 */
	default boolean startsWithVariable() {
		return !isEmpty() && ItemPath.isVariable(first());
	}

	/**
	 * Returns true if the path starts with an object reference (@).
	 */
	default boolean startsWithObjectReference() {
		return !isEmpty() && ItemPath.isObjectReference(first());
	}

	/**
	 * Returns true if the path starts with a parent segment (..).
	 */
	default boolean startsWithParent() {
		return !isEmpty() && ItemPath.isParent(first());
	}

	/**
	 * If the path consists of a single name segment (not variable nor special symbol), returns the corresponding value.
	 * Otherwise returns null.
	 */
	default QName asSingleName() {
		return isSingleName() ? ItemPath.toName(first()) : null;
	}

	/**
	 * If the path consists of a single name segment (not variable nor special symbol), returns the corresponding value.
	 * Otherwise throws an exception.
	 */
	@NotNull
	default ItemName asSingleNameOrFail() {
		if (isSingleName()) {
			return ItemPath.toName(first());
		} else {
			throw new IllegalArgumentException("Expected a single-name path, bug got "+this);
		}
	}

	/**
	 * Returns true if the path consists of a single name segment. (Not variable nor special symbol.)
	 */
	default boolean isSingleName() {
		return size() == 1 && ItemPath.isName(first());
	}

	/**
	 * Returns the value of the first segment if it is a name segment; otherwise null.
	 */
	@NotNull
	default ItemName firstToName() {
		return ItemPath.toName(first());
	}

	/**
	 * Returns the value of the first segment if it is a name segment; otherwise null.
	 */
	@Nullable
	default ItemName firstToNameOrNull() {
		return ItemPath.toNameOrNull(first());
	}

	static ItemName firstToNameOrNull(ItemPath itemPath) {
		return itemPath != null ? itemPath.firstToNameOrNull() : null;
	}

	/**
	 * Returns the value of the first segment if it is a variable name segment; otherwise null.
	 */
	default QName firstToVariableNameOrNull() {
		if (isEmpty()) {
			return null;
		} else {
			Object first = first();
			return isVariable(first) ? toVariableName(first) : null;
		}
	}

	/**
	 * Returns the value of the first segment if it is a Id segment; otherwise throws an exception.
	 */
	default Long firstToId() {
		return ItemPath.toId(first());
	}

	/**
	 * Returns the value of the first segment if it is a Id segment; otherwise null.
	 */
	default Long firstToIdOrNull() {
		return ItemPath.toIdOrNull(first());
	}

	static Long firstToIdOrNull(ItemPath path) {
		return path != null ? path.firstToIdOrNull() : null;
	}

	/**
	 * Returns the value of the first name segment or null if there's no name segment.
	 * NOTE: The difference between firstToName and firstName is that the former always looks
	 * at the first segment and tries to interpret it as a name. The latter, however, tries to
	 * find the first segment of Name type.
	 */
	@Nullable
	default ItemName firstName() {
		int i = firstNameIndex();
		return i >= 0 ? toName(getSegment(i)) : null;
	}

	/**
	 * The same as firstName but throws an exception if there's no name.
	 */
	@NotNull
	default ItemName firstNameOrFail() {
		ItemName name = firstName();
		if (name != null) {
			return name;
		} else {
			throw new IllegalArgumentException("No name segment in path: " + this);
		}
	}

	/**
	 * Returns the first name segment index; or -1 if there's no such segment.
	 */
	default int firstNameIndex() {
		for (int i = 0; i < size(); i++) {
			if (ItemPath.isName(getSegment(i))) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the last name segment value; or null if there's no name segment.
	 */
	ItemName lastName();

	/**
	 * Returns the last name segment index; or -1 if there's no such segment.
	 */
	default int lastNameIndex() {
		for (int i = size()-1; i >= 0; i--) {
			if (ItemPath.isName(getSegment(i))) {
				return i;
			}
		}
		return -1;
	}

	static void checkNoSpecialSymbols(ItemPath path) {
		if (path != null && path.containsSpecialSymbols()) {
			throw new IllegalStateException("Item path shouldn't contain special symbols but it does: " + path);
		}
	}

	static void checkNoSpecialSymbolsExceptParent(ItemPath path) {
		if (path != null && path.containsSpecialSymbolsExceptParent()) {
			throw new IllegalStateException("Item path shouldn't contain special symbols (except for parent) but it does: " + path);
		}
	}

	default boolean containsSpecialSymbols() {
		for (Object segment : getSegments()) {
			if (isSpecial(segment)) {
				return true;
			}
		}
		return false;
	}

	default boolean containsSpecialSymbolsExceptParent() {
		for (Object segment : getSegments()) {
			if (isSpecial(segment) && !isParent(segment)) {
				return true;
			}
		}
		return false;
	}
	//endregion

	//region Path transformation
	/**
	 * Returns the path containing only the regular named segments.
	 */
	@NotNull
	ItemPath namedSegmentsOnly();

	/**
	 * Returns the path with no Id segments.
	 */
	@NotNull
	ItemPath removeIds();

	/**
	 * Removes the leading variable segment, if present.
	 */
	@NotNull
	default ItemPath stripVariableSegment() {
		return startsWithVariable() ? rest() : this;
	}

	/**
	 * Converts an ItemPath to a UniformItemPath.
	 */
	@NotNull
	UniformItemPath toUniform(PrismContext prismContext);

	@Nullable
	static UniformItemPath toUniform(ItemPath path, PrismContext prismContext) {
		return path != null ? path.toUniform(prismContext) : null;
	}
	//endregion

	//region Finding in path

	/**
	 * Returns true if the path contains the specified name (requires exact match).
	 */
	default boolean containsNameExactly(QName name) {
		return getSegments().stream().anyMatch(component -> isName(component) && name.equals(toName(component)));
	}

	/**
	 * Returns true if the path starts with the specified name (approximate match).
	 */
	default boolean startsWithName(QName name) {
		return !isEmpty() && QNameUtil.match(name, firstToNameOrNull());
	}
	//endregion

	//region Misc

	/**
	 * Converts null ItemPath to empty one.
	 */
	static ItemPath emptyIfNull(ItemPath path) {
		return path != null ? path : EMPTY_PATH;
	}

	/**
	 * Returns true if the given segments are equivalent.
	 */
	static boolean segmentsEquivalent(Object segment1, Object segment2) {
		return ItemPathComparatorUtil.segmentsEquivalent(segment1, segment2);
	}

	static UniformItemPath parseFromString(String string) {
		return ItemPathHolder.parseFromString(string);
	}

	static UniformItemPath parseFromElement(Element element) {
		return ItemPathHolder.parseFromElement(element);
	}

	default String serializeWithDeclarations() {
		return ItemPathHolder.serializeWithDeclarations(this);
	}

	default String serializeWithForcedDeclarations() {
		return ItemPathHolder.serializeWithForcedDeclarations(this);
	}
	//endregion

	//region Diagnostics
	@Override
	default void shortDump(StringBuilder sb) {
		Iterator<?> iterator = getSegments().iterator();
		while (iterator.hasNext()) {
			sb.append(iterator.next());
			if (iterator.hasNext()) {
				sb.append("/");
			}
		}
	}
	//endregion
}
