/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.PropertyPath;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.util.DebugUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueAssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ValueConstructionType;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author semancik
 */
public class RefinedAttributeDefinition extends ResourceObjectAttributeDefinition implements Dumpable, DebugDumpable {

    private String displayName;
    private String description;
    private boolean tolerant = true;
    private boolean create = true;
    private boolean read = true;
    private boolean update = true;
    private ResourceObjectAttributeDefinition attributeDefinition;
    private ValueConstructionType outboundValueConstructionType;
    private List<ValueAssignmentType> inboundAssignmentTypes;

    private RefinedAttributeDefinition(ResourceObjectAttributeDefinition attrDef) {
        super(attrDef.getName(), attrDef.getDefaultName(), attrDef.getTypeName());
        this.attributeDefinition = attrDef;
    }

    @Override
    public void setNativeAttributeName(String nativeAttributeName) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public String getAttributeDisplayName() {
        return attributeDefinition.getAttributeDisplayName();
    }

    @Override
    public void setAttributeDisplayName(String attributeDisplayName) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    public boolean isTolerant() {
		return tolerant;
	}

	public void setTolerant(boolean tolerant) {
		this.tolerant = tolerant;
	}

	@Override
    public boolean canRead() {
        return read;
    }

    @Override
    public boolean canUpdate() {
        return update;
    }

    @Override
    public void setReadOnly() {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public QName getValueType() {
        return attributeDefinition.getValueType();
    }

    @Override
    public void setMinOccurs(int minOccurs) {
        attributeDefinition.setMinOccurs(minOccurs);
    }

    @Override
    public void setMaxOccurs(int maxOccurs) {
        attributeDefinition.setMaxOccurs(maxOccurs);
    }

    @Override
    public void setRead(boolean read) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public void setUpdate(boolean update) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public void setCreate(boolean create) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public boolean canCreate() {
        return create;
    }

    @Override
    public QName getDefaultName() {
        return attributeDefinition.getDefaultName();
    }

    @Override
    public boolean isIgnored() {
        return false;
    }

    @Override
    public void setIgnored(boolean ignored) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public void setHelp(String help) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ResourceObjectAttributeDefinition getAttributeDefinition() {
        return attributeDefinition;
    }

    public void setAttributeDefinition(ResourceObjectAttributeDefinition attributeDefinition) {
        this.attributeDefinition = attributeDefinition;
    }

    public ValueConstructionType getOutboundValueConstructionType() {
        return outboundValueConstructionType;
    }

    public void setOutboundValueConstructionType(ValueConstructionType outboundValueConstructionType) {
        this.outboundValueConstructionType = outboundValueConstructionType;
    }

    public List<ValueAssignmentType> getInboundAssignmentTypes() {
        return inboundAssignmentTypes;
    }

    public void setInboundAssignmentTypes(List<ValueAssignmentType> inboundAssignmentTypes) {
        this.inboundAssignmentTypes = inboundAssignmentTypes;
    }

    public QName getName() {
        return attributeDefinition.getName();
    }

    public QName getTypeName() {
        return attributeDefinition.getTypeName();
    }

    public String getNativeAttributeName() {
        return attributeDefinition.getNativeAttributeName();
    }

    public Object[] getAllowedValues() {
        return attributeDefinition.getAllowedValues();
    }

    public int getMaxOccurs() {
        return attributeDefinition.getMaxOccurs();
    }

    public int getMinOccurs() {
        return attributeDefinition.getMinOccurs();
    }

    public boolean isMandatory() {
        return attributeDefinition.isMandatory();
    }

    public boolean isMultiValue() {
        return attributeDefinition.isMultiValue();
    }

    public boolean isOptional() {
        return attributeDefinition.isOptional();
    }

    public boolean isSingleValue() {
        return attributeDefinition.isSingleValue();
    }

    public String getHelp() {
        return attributeDefinition.getHelp();
    }

    static RefinedAttributeDefinition parse(ResourceObjectAttributeDefinition attrDef, ResourceAttributeDefinitionType attrDefType,
                                            ResourceObjectDefinition objectClassDef, String contextDescription) throws SchemaException {

        RefinedAttributeDefinition rAttrDef = new RefinedAttributeDefinition(attrDef);

        if (attrDefType != null && attrDefType.getDisplayName() != null) {
            rAttrDef.setDisplayName(attrDefType.getDisplayName());
        } else {
            if (attrDef.getDisplayName() != null) {
                rAttrDef.setDisplayName(attrDef.getDisplayName());
            }
        }

        if (attrDefType != null && attrDefType.getDescription() != null) {
            rAttrDef.setDescription(attrDefType.getDescription());
        }

        if (attrDefType != null) {

        	if (attrDefType.isTolerant() == null) {
        		rAttrDef.tolerant = true;
        	} else {
        		rAttrDef.tolerant = attrDefType.isTolerant();
        	}
        	
            if (attrDefType.getOutbound() != null) {
                rAttrDef.setOutboundValueConstructionType(attrDefType.getOutbound());
            }

            if (attrDefType.getInbound() != null) {
                rAttrDef.setInboundAssignmentTypes(attrDefType.getInbound());
            }
        }
        

        // TODO: access

        return rAttrDef;

    }

    public static boolean isIgnored(ResourceAttributeDefinitionType attrDefType) {
        if (attrDefType.isIgnore() == null) {
            return false;
        }
        return attrDefType.isIgnore();
    }

    @Override
    public String dump() {
        return debugDump(0);
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        sb.append("RAttributeDef(");
        sb.append(DebugUtil.prettyPrint(getName()));
        sb.append(",");
        sb.append(DebugUtil.prettyPrint(getTypeName()));
        if (getMinOccurs() != 1 || getMaxOccurs() != 1) {
            sb.append("[").append(getMinOccurs()).append("-");
            if (getMaxOccurs() < 0) {
                sb.append("*");
            } else {
                sb.append(getMaxOccurs());
            }
            sb.append("]");
        }
        // TODO
        if (getDisplayName() != null) {
            sb.append(",Disp");
        }
        if (getDescription() != null) {
            sb.append(",Desc");
        }
        if (getOutboundValueConstructionType() != null) {
            sb.append(",OUT");
        }
        sb.append(")");
        return sb.toString();
    }


}
