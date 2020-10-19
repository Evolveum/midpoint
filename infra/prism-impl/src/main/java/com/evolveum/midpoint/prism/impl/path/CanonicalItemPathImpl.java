/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.path;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.CanonicalItemPath;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * @author katkav
 * @author mederly
 */
public class CanonicalItemPathImpl implements CanonicalItemPath {

    private static final List<Pair<String, String>> EXPLICIT_REPLACEMENTS = Arrays.asList(
            new ImmutablePair<>("http://midpoint.evolveum.com/xml/ns/public/common/common-", "common"),
            new ImmutablePair<>("http://midpoint.evolveum.com/xml/ns/public/connector/icf-", "icf"),
            new ImmutablePair<>("http://midpoint.evolveum.com/xml/ns/public", "public"),
            new ImmutablePair<>("http://midpoint.evolveum.com/xml/ns", "midpoint"),
            new ImmutablePair<>("http://prism.evolveum.com/xml/ns", "prism")
    );

    private static final String SHORTCUT_MARKER_START = "${";
    private static final String SHORTCUT_MARKER_END = "}";

    // currently we support only named segments in canonical paths
    public static final class Segment implements Serializable {
        private final QName name;
        private final Integer index;        // N means this is Nth unique non-empty namespace in the path (starting from 0)
        private final Integer shortcut;        // K means this namespace is the same as the one with index=K (null if 1st occurrence)

        private Segment(QName name, Integer index, Integer shortcut) {
            this.name = name;
            this.index = index;
            this.shortcut = shortcut;
        }

        public QName getName() {
            return name;
        }

        public Integer getIndex() {
            return index;
        }

        public Integer getShortcut() {
            return shortcut;
        }
    }

    private final List<Segment> segments = new ArrayList<>();

    public static CanonicalItemPathImpl create(ItemPath itemPath, QName objectType, PrismContext prismContext) {
        ItemDefinition<?> def = objectType != null && prismContext != null
                ? prismContext.getSchemaRegistry().findContainerDefinitionByType(objectType)
                : null;
        return new CanonicalItemPathImpl(itemPath, def);
    }

    public static CanonicalItemPath create(ItemPath itemPath) {
        return new CanonicalItemPathImpl(itemPath, null);
    }

    private CanonicalItemPathImpl(List<Segment> segments) {
        this.segments.addAll(segments);
    }

    public CanonicalItemPathImpl(ItemPath path, ItemDefinition<?> itemDefinition) {
        while (!ItemPath.isEmpty(path)) {
            Object first = path.first();
            if (ItemPath.isName(first)) {
                ItemName name = ItemPath.toName(first);
                if (itemDefinition instanceof PrismContainerDefinition) {
                    itemDefinition = ((PrismContainerDefinition<?>) itemDefinition)
                            .findItemDefinition(name);
                    if (itemDefinition != null && !QNameUtil.hasNamespace(name)) {
                        name = itemDefinition.getItemName();
                    }
                }
                addToSegments(name);
            } else if (ItemPath.isId(first)) {
                // ignored (for now)
            } else {
                throw new UnsupportedOperationException("Canonicalization of non-name/non-ID segments is not supported: " + first);
            }
            path = path.rest();
        }
    }

    private void addToSegments(QName name) {
        if (!QNameUtil.hasNamespace(name)) {
            segments.add(new Segment(name, null, null));
            return;
        }
        String namespace = name.getNamespaceURI();
        int index = 0;
        Integer shortcut = null;
        for (Segment segment : segments) {
            if (namespace.equals(segment.name.getNamespaceURI())) {
                shortcut = index = segment.index;
                break;
            }
            if (QNameUtil.hasNamespace(segment.name) && segment.shortcut == null) {
                // we found a unique non-empty namespace! (so increase the index)
                index++;
            }
        }
        segments.add(new Segment(name, index, shortcut));
    }

    public List<Segment> getSegments() {
        return segments;
    }

    public int size() {
        return segments.size();
    }

    public CanonicalItemPathImpl allUpToIncluding(int n) {
        if (n + 1 < segments.size()) {
            return new CanonicalItemPathImpl(segments.subList(0, n + 1));
        } else {
            return new CanonicalItemPathImpl(segments);
        }
    }

    public String asString() {
        StringBuilder sb = new StringBuilder();
        for (Segment segment : segments) {
            sb.append("\\");
            if (segment.shortcut == null) {        // always true for unqualified names
                sb.append(extractExplicitReplacements(QNameUtil.qNameToUri(segment.name)));
            } else {
                sb.append(SHORTCUT_MARKER_START).append(segment.shortcut).append(SHORTCUT_MARKER_END)
                        .append(QNameUtil.DEFAULT_QNAME_URI_SEPARATOR_CHAR).append(segment.name.getLocalPart());
            }
        }
        return sb.toString();
    }

    private String extractExplicitReplacements(String uri) {
        for (Pair<String, String> mapping : EXPLICIT_REPLACEMENTS) {
            if (uri.startsWith(mapping.getKey())) {
                return SHORTCUT_MARKER_START + mapping.getValue() + SHORTCUT_MARKER_END + uri.substring(mapping.getKey().length());
            }
        }
        return uri;
    }

    @Override
    public String toString() {
        return asString();
    }

}
