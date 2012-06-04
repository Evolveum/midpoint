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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PropertyPath;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.util.SchemaDebugUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccessType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ValueAssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ValueConstructionType;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author semancik
 */
public class RefinedAttributeDefinition extends ResourceAttributeDefinition implements Dumpable, DebugDumpable {

    private String displayName;
    private String description;
    private boolean tolerant = true;
    private boolean create = true;
    private boolean read = true;
    private boolean update = true;
    private ResourceAttributeDefinition attributeDefinition;
    private ValueConstructionType outboundValueConstructionType;
    private List<ValueAssignmentType> inboundAssignmentTypes;

    private RefinedAttributeDefinition(ResourceAttributeDefinition attrDef, PrismContext prismContext) {
        super(attrDef.getName(), attrDef.getDefaultName(), attrDef.getTypeName(), prismContext);
        this.attributeDefinition = attrDef;
    }

    @Override
    public void setNativeAttributeName(String nativeAttributeName) {
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
        return ignored;
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

    public ResourceAttributeDefinition getAttributeDefinition() {
        return attributeDefinition;
    }

    public void setAttributeDefinition(ResourceAttributeDefinition attributeDefinition) {
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

    static RefinedAttributeDefinition parse(ResourceAttributeDefinition attrDef, ResourceAttributeDefinitionType attrDefType,
    		ObjectClassComplexTypeDefinition objectClassDef, PrismContext prismContext, 
                                            String contextDescription) throws SchemaException {

        RefinedAttributeDefinition rAttrDef = new RefinedAttributeDefinition(attrDef, prismContext);

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
        
        rAttrDef.ignored = attrDef.isIgnored();
        
        rAttrDef.create = parseAccess(attrDefType, AccessType.CREATE, attrDef.canCreate());
        rAttrDef.update = parseAccess(attrDefType, AccessType.UPDATE, attrDef.canUpdate());
        rAttrDef.read = parseAccess(attrDefType, AccessType.READ, attrDef.canRead());

        return rAttrDef;

    }

	private static boolean parseAccess(ResourceAttributeDefinitionType attrDefType, AccessType access, boolean defaultValue) {
		if (attrDefType == null) {
			return defaultValue;
		}
		List<AccessType> accessList = attrDefType.getAccess();
		if (accessList == null || accessList.isEmpty()) {
			return defaultValue;
		}
		for (AccessType acccessEntry: accessList) {
			if (acccessEntry == access) {
				return true;
			}
		}
		return false;
	}

	public static boolean isIgnored(ResourceAttributeDefinitionType attrDefType) {
        if (attrDefType.isIgnore() == null) {
            return false;
        }
        return attrDefType.isIgnore();
    }
    
    @Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		if (getDisplayName() != null) {
            sb.append(",Disp");
        }
        if (getDescription() != null) {
            sb.append(",Desc");
        }
        if (getOutboundValueConstructionType() != null) {
            sb.append(",OUT");
        }
		return sb.toString();
	}
    
    /**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "RRAD";
    }


}
