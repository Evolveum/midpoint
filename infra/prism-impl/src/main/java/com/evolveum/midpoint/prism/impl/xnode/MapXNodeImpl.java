/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.xnode;

import java.util.*;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

public class MapXNodeImpl extends XNodeImpl implements MapXNode {

    /**
     * Contains all the nodes in this map - with both qualified and unqualified key names.
     * Equivalent (qualified + unqualified) keys are stored, of course, only once. It is not
     * specified whether it is under qualified or unqualified name.
     *
     * We want to maintain ordering hence the LinkedHashMap.
     *
     * This data structure is very similar to the one of PrismContainerValueImpl.
     *
     * Note that values must NOT be null.
     */
    private LinkedHashMap<QName, XNodeImpl> subnodes = new LinkedHashMap<>();

    /**
     * Unqualified key names. We store them because they deserve special handling if present.
     *
     * So: if a key is qualified AND there's no unqualified version of it present in the map, it is sufficient to
     * work with subnodes map. But if the key is unqualified or it's qualified but its local part is present in the map
     * without namespace, we have to do a full-scan operation.
     */
    private final Set<String> unqualifiedSubnodeNames = new HashSet<>();

    private boolean hasDefaultNamespaceMarkers;

    private MapXNode metadata;

    public int size() {
        return subnodes.size();
    }

    public boolean isEmpty() {
        return subnodes.isEmpty();
    }

    @NotNull
    public Set<QName> keySet() {
        return Collections.unmodifiableSet(subnodes.keySet());
    }

    @NotNull
    public Set<Map.Entry<QName, XNodeImpl>> entrySet() {
        return Collections.unmodifiableSet(subnodes.entrySet());
    }

    public boolean containsKey(QName key) {
        return subnodes.containsKey(key) || unqualifiedSubnodeNames.contains(key.getLocalPart());
    }

    public XNodeImpl get(String key) {
        // Unqualified "get" goes always by full scan route.
        return getByFullScan(key);
    }

    public XNodeImpl get(QName key) {
        // Here we assume that "get" on hash map is quote fast and that we do not mix
        // qualified and unqualified versions very often (so we hit directly if it's there).
        XNodeImpl directHit = subnodes.get(key);
        if (directHit != null || (!QNameUtil.isUnqualified(key) && !unqualifiedSubnodeNames.contains(key.getLocalPart()))) {
            return directHit;
        } else {
            return getByFullScan(key);
        }
    }

    private XNodeImpl getByFullScan(QName key) {
        for (Map.Entry<QName, XNodeImpl> entry: subnodes.entrySet()) {
            if (QNameUtil.match(key, entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private XNodeImpl getByFullScan(String key) {
        for (Map.Entry<QName, XNodeImpl> entry: subnodes.entrySet()) {
            if (entry.getKey().getLocalPart().equals(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    public void put(QName key, @NotNull XNodeImpl value) {
        checkMutable();
        if (QNameUtil.isUnqualified(key)) {
            removeByFullScan(key);
            unqualifiedSubnodeNames.add(key.getLocalPart());
        } else if (unqualifiedSubnodeNames.contains(key.getLocalPart())) {
            removeByFullScan(key);
        }
        subnodes.put(key, value);
    }

    private XNodeImpl putReturningPrevious(QName key, XNodeImpl value) {
        checkMutable();
        boolean unqualified = QNameUtil.isUnqualified(key);
        if (unqualified || unqualifiedSubnodeNames.contains(key.getLocalPart())) {
            XNodeImpl previous = removeByFullScan(key);
            subnodes.put(key, value);
            if (unqualified) {
                unqualifiedSubnodeNames.add(key.getLocalPart());
            }
            return previous;
        } else {
            return subnodes.put(key, value);
        }
    }

    private XNodeImpl removeByFullScan(QName key) {
        checkMutable();
        Iterator<Map.Entry<QName, XNodeImpl>> iterator = subnodes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<QName, XNodeImpl> entry = iterator.next();
            if (QNameUtil.match(key, entry.getKey())) {
                iterator.remove();
                unqualifiedSubnodeNames.remove(key.getLocalPart());
                return entry.getValue();
            }
        }
        return null;
    }

    // TODO immutability of returned Entry
    @Override
    public Map.Entry<QName, XNodeImpl> getSingleSubEntry(String errorContext) throws SchemaException {
        if (isEmpty()) {
            return null;
        }

        if (size() > 1) {
            throw new SchemaException("More than one element in " + errorContext +" : "+dumpKeyNames());
        }

        return subnodes.entrySet().iterator().next();
    }

    @Override
    public RootXNode getSingleSubEntryAsRoot(String errorContext) throws SchemaException {
        Map.Entry<QName, XNodeImpl> entry = getSingleSubEntry(errorContext);
        return entry != null ? new RootXNodeImpl(entry.getKey(), entry.getValue()) : null;
    }

    public Map.Entry<QName, XNodeImpl> getSingleEntryThatDoesNotMatch(QName... excludedKeys) throws SchemaException {
        Map.Entry<QName, XNodeImpl> found = null;
        OUTER: for (Map.Entry<QName, XNodeImpl> subentry: subnodes.entrySet()) {
            for (QName excludedKey: excludedKeys) {
                if (QNameUtil.match(subentry.getKey(), excludedKey)) {
                    continue OUTER;
                }
            }
            if (found != null) {
                throw new SchemaException("More than one extension subnode found under "+this+": "+found.getKey()+" and "+subentry.getKey());
            } else {
                found = subentry;
            }
        }
        return found;
    }


    public <T> T getParsedPrimitiveValue(QName key, QName typeName) throws SchemaException {
        XNodeImpl xnode = get(key);
        if (xnode == null) {
            return null;
        }
        if (!(xnode instanceof PrimitiveXNodeImpl<?>)) {
            throw new SchemaException("Expected that field "+key+" will be primitive, but it is "+xnode.getDesc());
        }
        //noinspection unchecked
        PrimitiveXNodeImpl<T> xprim = (PrimitiveXNodeImpl<T>) xnode;
        return xprim.getParsedValue(typeName, null);            // TODO expected class
    }

    public void merge(@NotNull MapXNodeImpl other) {
        for (java.util.Map.Entry<QName, XNodeImpl> otherEntry: other.subnodes.entrySet()) {
            QName otherKey = otherEntry.getKey();
            XNodeImpl otherValue = otherEntry.getValue();
            if (otherValue != null) {
                merge(otherKey, otherValue);
            }
        }
    }

    public void merge(QName otherKey, @NotNull XNode otherValue) {
        checkMutable();
        XNodeImpl previous = putReturningPrevious(otherKey, (XNodeImpl) otherValue);
        if (previous != null) {
            // It seems that when parsing properly serialized XML, this branch is not used much (elements of the same name
            // are grouped together in DomLexicalProcessor). Therefore we can afford optimistic approach: first try and if
            // there's a problem, correct it.
            ListXNodeImpl valueToStore;
            if (previous instanceof ListXNodeImpl) {
                valueToStore = (ListXNodeImpl) previous;
            } else {
                valueToStore = new ListXNodeImpl();
                valueToStore.add(previous);
            }
            if (otherValue instanceof ListXNodeImpl) {
                valueToStore.addAll((ListXNodeImpl) otherValue);
            } else {
                valueToStore.add((XNodeImpl) otherValue);
            }
            put(otherKey, valueToStore);
        }
    }

    @Override
    public void accept(Visitor<XNode> visitor) {
        visitor.visit(this);
        for (Map.Entry<QName, XNodeImpl> subentry: subnodes.entrySet()) {
            if (subentry.getValue() != null) {
                subentry.getValue().accept(visitor);
            } else {
                //throw new IllegalStateException("null value of key " + subentry.key + " in map: " + debugDump());
            }
        }
    }

    public boolean equals(Object o) {
        if (!(o instanceof MapXNodeImpl)) {
            return false;
        }
        MapXNodeImpl other = (MapXNodeImpl) o;
        return MiscUtil.unorderedCollectionEquals(this.subnodes.entrySet(), other.subnodes.entrySet());
    }

    public int hashCode() {
        int result = 0xCAFEBABE;
        for (XNodeImpl node : subnodes.values()) {
            if (node != null) {
                result = result ^ node.hashCode();          // using XOR instead of multiplying and adding in order to achieve commutativity
            }
        }
        return result;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.debugDumpMapMultiLine(sb, this.toMap(), indent, true, dumpSuffix());
        appendMetadata(sb, indent, metadata);
        return sb.toString();
    }

    @Override
    public String getDesc() {
        return "map";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("XNode(map:"+subnodes.size()+" entries) {");
        sb.append("\n");
        sb.append(subnodes.entrySet().stream()
                .map(Object::toString)
                .collect(Collectors.joining(";\n")));
        sb.append(" }");
        return sb.toString();
    }

    private String dumpKeyNames() {
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<QName, XNodeImpl>> iterator = subnodes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<QName, XNodeImpl> entry = iterator.next();
            sb.append(PrettyPrinter.prettyPrint(entry.getKey()));
            if (iterator.hasNext()) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    public void replace(QName key, XNodeImpl value) {
        checkMutable();
        for (Map.Entry<QName, XNodeImpl> entry : subnodes.entrySet()) {
            if (entry.getKey().equals(key)) {
                entry.setValue(value);
                return;
            }
        }
        put(key, value);
    }

    public RootXNodeImpl getEntryAsRoot(@NotNull QName key) {
        XNodeImpl xnode = get(key);
        return xnode != null ? new RootXNodeImpl(key, xnode) : null;
    }

//    @NotNull
//    public RootXNodeImpl getSingleEntryMapAsRoot() {
//        if (!isSingleEntryMap()) {
//            throw new IllegalStateException("Expected to be called on single-entry map");
//        }
//        QName key = keySet().iterator().next();
//        return new RootXNodeImpl(key, get(key));
//    }

    @NotNull
    @Override
    public MapXNodeImpl clone() {
        return (MapXNodeImpl) super.clone();        // fixme brutal hack
    }

    @Override
    public Map<QName, ? extends XNode> toMap() {
        return new HashMap<>(subnodes);
    }

    @Override
    public void performFreeze() {
        for (Map.Entry<QName, XNodeImpl> subnode : subnodes.entrySet()) {
            if (subnode.getValue() != null) {
                subnode.getValue().freeze();
            }
        }
        super.performFreeze();
    }

    // TODO reconsider performance of this method
    public void replaceDefaultNamespaceMarkers(String marker, String defaultNamespace) {
        if (hasDefaultNamespaceMarkers) {
            // As we have no method to replace existing entry in LinkedHashMap, we need to copy all the entries,
            // qualifying them as needed.
            LinkedHashMap<QName, XNodeImpl> originalSubNodes = subnodes;
            subnodes = new LinkedHashMap<>();
            unqualifiedSubnodeNames.clear();
            for (Map.Entry<QName, XNodeImpl> originalEntry : originalSubNodes.entrySet()) {
                QName originalKey = originalEntry.getKey();
                QName newKey;
                if (marker.equals(originalKey.getNamespaceURI())) {
                    // Note that defaultNamespace can be "", so newKey can be unqualified
                    newKey = new QName(defaultNamespace, originalKey.getLocalPart());
                } else {
                    newKey = originalKey;
                }
                merge(newKey, originalEntry.getValue());
            }
            hasDefaultNamespaceMarkers = false;
        }
    }

    public void setHasDefaultNamespaceMarkers() {
        hasDefaultNamespaceMarkers = true;
    }


    @Override
    public MapXNode getMetadataNode() {
        return metadata;
    }

    @Override
    public void setMetadataNode(MapXNode metadata) {
        this.metadata = metadata;
    }
}
