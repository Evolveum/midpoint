/*
 * Copyright (c) 2014-2018 Evolveum
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
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.util.ShortDumpable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;

/**
 * @author semancik
 *
 */
public class SearchResultList<T> implements List<T>, Cloneable, Serializable, ShortDumpable {
	
	private List<T> list = null;
	private SearchResultMetadata metadata = null;

	public SearchResultList() { }

	public SearchResultList(List<T> list) {
		super();
		this.list = list;
	}

	public SearchResultList(List<T> list, SearchResultMetadata metadata) {
		super();
		this.list = list;
		this.metadata = metadata;
	}

	public List<T> getList() {
		return list;
	}

	public void setList(List<T> list) {
		this.list = list;
	}

	public SearchResultMetadata getMetadata() {
		return metadata;
	}

	public void setMetadata(SearchResultMetadata metadata) {
		this.metadata = metadata;
	}

	public int size() {
		if (list == null) {
			return 0;
		}
		return list.size();
	}

	public boolean isEmpty() {
		return list == null || list.isEmpty();
	}

	public boolean contains(Object o) {
		return list != null && list.contains(o);
	}

	@NotNull
	public Iterator<T> iterator() {
		return list != null ? list.iterator() : Collections.emptyIterator();
	}

	@NotNull
	public Object[] toArray() {
		return getInitializedList().toArray();
	}

	@NotNull
	public <TT> TT[] toArray(@NotNull TT[] a) {
		//noinspection SuspiciousToArrayCall
		return getInitializedList().toArray(a);
	}

	public boolean add(T e) {
		return getInitializedList().add(e);
	}

	public boolean remove(Object o) {
		return list != null && list.remove(o);
	}

	public boolean containsAll(@NotNull Collection<?> c) {
		return list != null && list.containsAll(c);
	}

	public boolean addAll(@NotNull Collection<? extends T> c) {
		return getInitializedList().addAll(c);
	}

	public boolean addAll(int index, @NotNull Collection<? extends T> c) {
		return getInitializedList().addAll(index, c);
	}

	public boolean removeAll(@NotNull Collection<?> c) {
		return list != null && list.removeAll(c);
	}

	public boolean retainAll(@NotNull Collection<?> c) {
		return list != null && list.retainAll(c);
	}

	public void clear() {
		if (list != null) {
			list.clear();
		}
	}

	public T get(int index) {
		return list != null ? list.get(index) : null;
	}

	public T set(int index, T element) {
		return getInitializedList().set(index, element);
	}

	public void add(int index, T element) {
		getInitializedList().add(index, element);
	}

	public T remove(int index) {
		return list != null ? list.remove(index) : null;
	}

	public int indexOf(Object o) {
		return list != null ? list.indexOf(o) : -1;
	}

	public int lastIndexOf(Object o) {
		return list != null ? list.lastIndexOf(o) : -1;
	}

	@NotNull
	public ListIterator<T> listIterator() {
		return list != null ? list.listIterator() : Collections.emptyListIterator();
	}

	@NotNull
	public ListIterator<T> listIterator(int index) {
		return list != null ? list.listIterator(index) : Collections.emptyListIterator();
	}

	@NotNull
	public List<T> subList(int fromIndex, int toIndex) {
		return getInitializedList().subList(fromIndex, toIndex);
	}

	// Do NOT auto-generate -- there are manual changes here
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((list == null) ? Collections.emptyList().hashCode() : list.hashCode());
		result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
		return result;
	}

	// Do NOT auto-generate -- there are manual changes here
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SearchResultList other = (SearchResultList) obj;
		if (list == null || list.isEmpty()) {
			if (other.list != null && !other.list.isEmpty())
				return false;
		} else if (!list.equals(other.list))
			return false;
		if (metadata == null) {
			return other.metadata == null;
		} else {
			return metadata.equals(other.metadata);
		}
	}

	@Override
	public String toString() {
		if (metadata == null) {
			if (list == null) {
				return "SearchResultList(null)";
			} else {
				return "SearchResultList("+list+")";
			}
		} else {
			if (list == null) {
				return "SearchResultList("+metadata+")";
			} else {
				return "SearchResultList("+list+", "+metadata+")";
			}
		}
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	public SearchResultList<T> clone() {
		SearchResultList<T> clone = new SearchResultList<>();
		clone.metadata = this.metadata;		// considered read-only object
		if (this.list != null) {
			clone.list = new ArrayList<>(this.list.size());
			for (T item : this.list) {
				clone.list.add(CloneUtil.clone(item));
			}
		}
		return clone;
	}

	private List<T> getInitializedList() {
		if (list == null) {
			list = new ArrayList<>();
		}
		return list;
	}

	@Override
	public void shortDump(StringBuilder sb) {
		if (metadata == null) {
			if (list == null) {
				sb.append("null");
			} else {
				sb.append(list.size()).append(" results");
			}
		} else {
			if (list == null) {
				sb.append("null, metadata=(");
			} else {
				sb.append(list.size()).append(" results, metadata=(");
			}
			metadata.shortDump(sb);
			sb.append(")");
		}
	}
}
