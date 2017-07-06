/**
 * Copyright (c) 2014-2017 Evolveum
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * @author semancik
 *
 */
public class SearchResultList<T> implements List<T>, Cloneable, Serializable {
	
	private List<T> list = null;
	private SearchResultMetadata metadata = null;
	
	public SearchResultList() { };
	
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
		return list.contains(o);
	}

	public Iterator<T> iterator() {
		return list.iterator();
	}

	public Object[] toArray() {
		return list.toArray();
	}

	public <T> T[] toArray(T[] a) {
		return list.toArray(a);
	}

	public boolean add(T e) {
		return getInitializedList().add(e);
	}

	public boolean remove(Object o) {
		return list.remove(o);
	}

	public boolean containsAll(Collection<?> c) {
		return list.containsAll(c);
	}

	public boolean addAll(Collection<? extends T> c) {
		return list.addAll(c);
	}

	public boolean addAll(int index, Collection<? extends T> c) {
		return list.addAll(index, c);
	}

	public boolean removeAll(Collection<?> c) {
		return list.removeAll(c);
	}

	public boolean retainAll(Collection<?> c) {
		return list.retainAll(c);
	}

	public void clear() {
		list.clear();
	}

	public T get(int index) {
		return list.get(index);
	}

	public T set(int index, T element) {
		return list.set(index, element);
	}

	public void add(int index, T element) {
		list.add(index, element);
	}

	public T remove(int index) {
		return list.remove(index);
	}

	public int indexOf(Object o) {
		return list.indexOf(o);
	}

	public int lastIndexOf(Object o) {
		return list.lastIndexOf(o);
	}

	public ListIterator<T> listIterator() {
		return list.listIterator();
	}

	public ListIterator<T> listIterator(int index) {
		return list.listIterator(index);
	}

	public List<T> subList(int fromIndex, int toIndex) {
		return list.subList(fromIndex, toIndex);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((list == null) ? 0 : list.hashCode());
		result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SearchResultList other = (SearchResultList) obj;
		if (list == null) {
			if (other.list != null)
				return false;
		} else if (!list.equals(other.list))
			return false;
		if (metadata == null) {
			if (other.metadata != null)
				return false;
		} else if (!metadata.equals(other.metadata))
			return false;
		return true;
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

	public SearchResultList<T> clone() {
		SearchResultList<T> clone = new SearchResultList<>();
		clone.metadata = this.metadata;		// considered read-only object
		if (this.list != null) {
			clone.list = new ArrayList(this.list.size());
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

}
