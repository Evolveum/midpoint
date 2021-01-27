/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.xnode;

import java.util.*;

import com.evolveum.midpoint.prism.PrismNamespaceContext;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.xnode.ListXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DebugUtil;

public class ListXNodeImpl extends XNodeImpl implements List<XNodeImpl>, ListXNode {

    private final List<XNodeImpl> subnodes = new ArrayList<>();

    @Deprecated
    public ListXNodeImpl() {
        super();
    }

    public ListXNodeImpl(PrismNamespaceContext local) {
        super(local);
    }

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
        checkMutable();
        return subnodes.add(e);
    }

    @Override
    public boolean remove(Object o) {
        checkMutable();
        return subnodes.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return subnodes.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends XNodeImpl> c) {
        checkMutable();
        return subnodes.addAll(c);
    }

    @Override
    public boolean addAll(int index, Collection<? extends XNodeImpl> c) {
        checkMutable();
        return subnodes.addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        checkMutable();
        return subnodes.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        checkMutable();
        return subnodes.retainAll(c);
    }

    @Override
    public void clear() {
        checkMutable();
        subnodes.clear();
    }

    @Override
    public XNodeImpl get(int index) {
        return subnodes.get(index);
    }

    @Override
    public XNodeImpl set(int index, XNodeImpl element) {
        checkMutable();
        return subnodes.set(index, element);
    }

    @Override
    public void add(int index, XNodeImpl element) {
        checkMutable();
        subnodes.add(index, element);
    }

    @Override
    public XNodeImpl remove(int index) {
        checkMutable();
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

    // TODO what about immutability?
    @Override
    public ListIterator<XNodeImpl> listIterator() {
        return subnodes.listIterator();
    }

    // TODO what about immutability?
    @Override
    public ListIterator<XNodeImpl> listIterator(int index) {
        return subnodes.listIterator(index);
    }

    // TODO what about immutability?
    @Override
    public List<XNodeImpl> subList(int fromIndex, int toIndex) {
        return subnodes.subList(fromIndex, toIndex);
    }

    @Override
    public void accept(Visitor<XNode> visitor) {
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
        return subnodes.stream().anyMatch(n -> n != null && n.getElementName() != null);        // TODO - or allMatch?
    }

    @Override
    public List<? extends XNode> asList() {
        return immutable ? Collections.unmodifiableList(this) : this;
    }

    @Override
    public void performFreeze() {
        for (XNodeImpl subnode : subnodes) {
            subnode.freeze();
        }
        super.performFreeze();
    }
}
