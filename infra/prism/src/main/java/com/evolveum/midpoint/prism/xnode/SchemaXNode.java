/*
 * Copyright (c) 2014 Evolveum
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
package com.evolveum.midpoint.prism.xnode;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author Radovan Semancik
 *
 */
public class SchemaXNode extends XNode {

	private Element schemaElement;

	public Element getSchemaElement() {
		return schemaElement;
	}

	public void setSchemaElement(Element schemaElement) {
		this.schemaElement = schemaElement;
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

        SchemaXNode that = (SchemaXNode) o;

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
