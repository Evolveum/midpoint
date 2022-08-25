/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.xnode;

import com.evolveum.midpoint.prism.xnode.SchemaXNode;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author Radovan Semancik
 *
 */
public class SchemaXNodeImpl extends XNodeImpl implements SchemaXNode {

    private Element schemaElement;

    public Element getSchemaElement() {
        return schemaElement;
    }

    public void setSchemaElement(Element schemaElement) {
        this.schemaElement = schemaElement;
        DOMUtil.preserveFormattingIfPresent(schemaElement);
    }

    @Override
    public boolean isEmpty() {
        return schemaElement == null || DOMUtil.isEmpty(schemaElement);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        if (schemaElement == null) {
            sb.append("Schema: null");
        } else {
            sb.append("Schema: present");
        }
        String dumpSuffix = dumpSuffix();
        if (dumpSuffix != null) {
            sb.append(dumpSuffix);
        }
        return sb.toString();
    }

    @Override
    public String getDesc() {
        return "schema";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SchemaXNodeImpl that = (SchemaXNodeImpl) o;

        if (schemaElement == null) {
            return that.schemaElement == null;
        }
        if (that.schemaElement == null) {
            return false;
        }
        return DOMUtil.compareElement(schemaElement, that.schemaElement, false);
    }

    @Override
    public int hashCode() {
        return 1;               // the same as in DomAwareHashCodeStrategy
    }
}
