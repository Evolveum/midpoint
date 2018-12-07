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
package com.evolveum.midpoint.prism.impl.xnode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DebugUtil;

public class ListXNodeImpl extends XNodeImpl implements List<XNodeImpl>, ListXNode {

	private final List<XNodeImpl> subnodes = new ArrayList<>();

	@Override
	public int size() {
		return subnodes.size();
	}

	@Override
	public boolean isEmpty() {
		return subnodes.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return subnodes.contains(o);
	}

	@Override
	public Iterator<XNodeImpl> iterator() {
		return subnodes.iterator();
	}

	@Override
	public Object[] toArray() {
		return subnodes.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return subnodes.toArray(a);
	}

	@Override
	public boolean add(XNodeImpl e) {
		return subnodes.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return subnodes.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return subnodes.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends XNodeImpl> c) {
		return subnodes.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends XNodeImpl> c) {
		return subnodes.addAll(index, c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return subnodes.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return subnodes.retainAll(c);
	}

	@Override
	public void clear() {
		subnodes.clear();
	}

	@Override
	public XNodeImpl get(int index) {
		return subnodes.get(index);
	}

	@Override
	public XNodeImpl set(int index, XNodeImpl element) {
		return subnodes.set(index, element);
	}

	@Override
	public void add(int index, XNodeImpl element) {
		subnodes.add(index, element);
	}

	@Override
	public XNodeImpl remove(int index) {
		return subnodes.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return subnodes.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return subnodes.lastIndexOf(o);
	}

	@Override
	public ListIterator<XNodeImpl> listIterator() {
		return subnodes.listIterator();
	}

	@Override
	public ListIterator<XNodeImpl> listIterator(int index) {
		return subnodes.listIterator(index);
	}

	@Override
	public List<XNodeImpl> subList(int fromIndex, int toIndex) {
		return subnodes.subList(fromIndex, toIndex);
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
		for (XNodeImpl subnode: subnodes) {
			if (subnode != null) {
				subnode.accept(visitor);
			} else {
				// !!!!! TODO
			}
		}
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.debugDump(sb, this, indent, true, dumpSuffix());
		return sb.toString();
	}

	@Override
	public String getDesc() {
		return "list";
	}

	@Override
	public String toString() {
		return "XNode(list:"+subnodes.size()+" elements)";
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ListXNodeImpl xNodes = (ListXNodeImpl) o;

        if (!subnodes.equals(xNodes.subnodes)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return subnodes.hashCode();
    }

	@Override
	public boolean isHeterogeneousList() {
		return subnodes.stream().anyMatch(n -> n != null && n.getElementName() != null);		// TODO - or allMatch?
	}

	@Override
	public List<? extends XNode> asList() {
		return this;
	}
}
