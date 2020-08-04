/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.prism.xml.ns._public.query_3;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.jvnet.jaxb2_commons.lang.Equals;
import org.jvnet.jaxb2_commons.lang.EqualsStrategy;
import org.jvnet.jaxb2_commons.lang.HashCode;
import org.jvnet.jaxb2_commons.lang.HashCodeStrategy;
import org.jvnet.jaxb2_commons.locator.ObjectLocator;
import org.jvnet.jaxb2_commons.locator.util.LocatorUtils;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.xml.DomAwareEqualsStrategy;
import com.evolveum.midpoint.util.xml.DomAwareHashCodeStrategy;

@XmlAccessorType(XmlAccessType.NONE)        // we select getters/fields to expose via JAXB individually
@XmlType(name = "SearchFilterType", propOrder = {       // no prop order, because we serialize this class manually
        // BTW, the order is the following: description, filterClause
})

public class SearchFilterType extends AbstractFreezable implements Serializable, Cloneable, Equals, HashCode, DebugDumpable, Freezable, JaxbVisitable {

    private static final long serialVersionUID = 201303040000L;

    public static final QName COMPLEX_TYPE = new QName(PrismConstants.NS_QUERY, "SearchFilterType");
    public static final QName F_DESCRIPTION = new QName(PrismConstants.NS_QUERY, "description");

    @XmlElement
    protected String description;

    // this one is not exposed via JAXB
    protected MapXNode filterClauseXNode; // single-subnode map node (key = filter element qname, value = contents)

    /**
     * Creates a new {@code QueryType} instance.
     */
    public SearchFilterType() {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super();
    }

    /**
     * Creates a new {@code QueryType} instance by deeply copying a given {@code QueryType} instance.
     *
     * @param o The instance to copy.
     * @throws NullPointerException if {@code o} is {@code null}.
     */
    public SearchFilterType(final SearchFilterType o) {
        // CC-XJC Version 2.0 Build 2011-09-16T18:27:24+0000
        super();
        Objects.requireNonNull(o, "Cannot create a copy of 'SearchFilterType' from 'null'.");
        this.description = o.description;
        this.filterClauseXNode = o.filterClauseXNode.clone();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        checkMutable();
        this.description = description;
    }

    public boolean containsFilterClause() {
        return filterClauseXNode != null && !filterClauseXNode.isEmpty();
    }

    public void setFilterClauseXNode(MapXNode filterClauseXNode) {
        checkMutable();
        this.filterClauseXNode = filterClauseXNode;
    }

    public void setFilterClauseXNode(RootXNode filterClauseNode) {
        checkMutable();
        if (filterClauseNode == null) {
            this.filterClauseXNode = null;
        } else {
            this.filterClauseXNode = filterClauseNode.toMapXNode();
        }
    }

    public MapXNode getFilterClauseXNode() {
        if (this.filterClauseXNode == null) {
            return null;
        } else {
            return this.filterClauseXNode.clone();
        }
    }

    public RootXNode getFilterClauseAsRootXNode() throws SchemaException {
        MapXNode clause = getFilterClauseXNode();
        return clause != null ? clause.getSingleSubEntryAsRoot("getFilterClauseAsRootXNode") : null;
    }

    public static SearchFilterType createFromParsedXNode(XNode xnode, ParsingContext pc, PrismContext prismContext) throws SchemaException {
        SearchFilterType filter = new SearchFilterType();
        filter.parseFromXNode(xnode, pc, prismContext);
        return filter;
    }

    public void parseFromXNode(XNode xnode, ParsingContext pc, PrismContext prismContext) throws SchemaException {
        checkMutable();
        if (xnode == null || xnode.isEmpty()) {
            this.filterClauseXNode = null;
            this.description = null;
        } else {
            if (!(xnode instanceof MapXNode)) {
                throw new SchemaException("Cannot parse filter from " + xnode);
            }
            MapXNode xmap = (MapXNode) xnode;
            XNode xdesc = xmap.get(SearchFilterType.F_DESCRIPTION);
            if (xdesc != null) {
                if (xdesc instanceof PrimitiveXNode<?>) {
                    String desc = ((PrimitiveXNode<String>) xdesc).getParsedValue(DOMUtil.XSD_STRING, String.class);
                    setDescription(desc);
                } else {
                    throw new SchemaException("Description must have a primitive value");
                }
            }
            Map<QName, XNode> filterMap = new HashMap<>();
            for (QName key : xmap.keySet()) {
                if (!QNameUtil.match(key, SearchFilterType.F_DESCRIPTION) && !QNameUtil.match(key, new QName("condition"))) {
                    filterMap.put(key, xmap.get(key));
                }
            }
            if (filterMap.size() > 1) {
                throw new SchemaException("Filter clause has more than one item: " + filterMap);
            }
            this.filterClauseXNode = prismContext.xnodeFactory().map(filterMap);
            prismContext.getQueryConverter().parseFilterPreliminarily(this.filterClauseXNode, pc);
        }
    }

    public MapXNode serializeToXNode(PrismContext prismContext) throws SchemaException {
        MapXNode xmap = getFilterClauseXNode();
        if (description == null) {
            return xmap;
        } else {
            // we have to serialize the map in correct order (see MID-1847): description first, filter clause next
            Map<QName, XNode> newXMap = new HashMap<>();
            newXMap.put(SearchFilterType.F_DESCRIPTION, prismContext.xnodeFactory().primitive(description));
            if (xmap != null && !xmap.isEmpty()) {
                Map.Entry<QName, ? extends XNode> filter = xmap.getSingleSubEntry("search filter");
                newXMap.put(filter.getKey(), filter.getValue());
            }
            return prismContext.xnodeFactory().map(newXMap);
        }
    }

    /**
     * Generates a String representation of the contents of this type.
     * This is an extension method, produced by the 'ts' xjc plugin
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public int hashCode(ObjectLocator locator, HashCodeStrategy strategy) {
        int currentHashCode = 1;
        MapXNode theFilter = this.filterClauseXNode;
        currentHashCode = strategy.hashCode(LocatorUtils.property(locator, "filter", theFilter), currentHashCode, theFilter);
        return currentHashCode;
    }

    public int hashCode() {
        final HashCodeStrategy strategy = DomAwareHashCodeStrategy.INSTANCE;
        return this.hashCode(null, strategy);
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean equals(ObjectLocator thisLocator, ObjectLocator thatLocator, Object object, EqualsStrategy strategy) {
        if (!(object instanceof SearchFilterType)) {
            return false;
        }
        if (this == object) {
            return true;
        }
        final SearchFilterType that = ((SearchFilterType) object);

        if (filterClauseXNode == null) {
            if (that.filterClauseXNode != null) { return false; }
        } else if (!filterClauseXNode.equals(that.filterClauseXNode)) { return false; }

        return true;
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object object) {
        final EqualsStrategy strategy = DomAwareEqualsStrategy.INSTANCE;
        return equals(null, null, object, strategy);
    }

    /**
     * Creates and returns a deep copy of this object.
     *
     * @return A deep copy of this object.
     */
    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public SearchFilterType clone() {
        final SearchFilterType clone;
        try {
            clone = this.getClass().newInstance(); // TODO fix this using super.clone()
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Couldn't instantiate " + this.getClass() + ": " + e.getMessage(), e);
        }
        clone.description = this.description;
        if (this.filterClauseXNode != null) {
            clone.filterClauseXNode = this.filterClauseXNode.clone();
        }
        return clone;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("SearchFilterType");
        if (description != null) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "description", description, indent + 1);
        }
        if (filterClauseXNode != null) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "filterClauseXNode", filterClauseXNode, indent + 1);
        }
        return sb.toString();
    }

    @Override
    protected void performFreeze() {
        if (filterClauseXNode != null) {
            filterClauseXNode.freeze();
        }
    }

    @Override
    public void accept(JaxbVisitor visitor) {
        visitor.visit(this);
    }
}
