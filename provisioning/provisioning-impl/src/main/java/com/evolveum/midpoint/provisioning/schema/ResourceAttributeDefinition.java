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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.provisioning.schema;

import com.evolveum.midpoint.provisioning.conversion.ConverterFactory;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

/**
 * TODO: Description
 *
 * @author Vilo Repan
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public class ResourceAttributeDefinition {

    public static final int MAX_OCCURS_UNBOUNDED = -1;
    /**
     * http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd#nativeAttributeName
     *
     * This is very rare case when the attribute name must be mapped. I have CA
     * HelpDesk adapter where the attribute name is a special query formula
     * so there for we need to map it.
     *
     */
    private final String nativeAttributeName;
    /**
     * http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd#identifier
     */
    private boolean identifier = false;
    /**
     * http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd#secondaryIdentifier
     */
    private boolean secondaryIdentifier = false;
    /**
     * http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd#compositeIdentifier
     */
    private boolean compositeIdentifier = false;
    /**
     * http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd#displayName
     */
    private boolean displayName = false;
    /**
     * http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd#descriptionAttribute
     */
    private boolean descriptionAttribute = false;
    /**
     * http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd#attributeFlag
     */
    private List<AttributeFlag> attributeFlag;
    /**
     * http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd#resourceObjectReference
     */
    private ResourceObjectDefinition resourceObjectReference = null;
    /**
     * http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd#classifiedAttribute
     */
    private ClassifiedAttributeInfo classifiedAttributeInfo = null;
    /**
     * http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd#help
     *
     * id for for localized help if there is one otherwise the help is stored here.
     */
    private String help;
    /**
     * http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd#attributeDisplayName
     */
    private String attributeDisplayName;
    private boolean mandatory = false;
    private boolean filledWithExpression = false;
    private ResourceObjectDefinition parentDefinition;

    public enum Encryption {

        HASH,
        NONE,
        SYMMETRIC,
    }
    //Properties
    private final QName qname;
    private QName type;
    private SimpleTypeRestriction restriction;
    //how many attribute values
    private int minOccurs = 1;
    private int maxOccurs = 1;

    public ResourceAttributeDefinition(QName qname) {
        this.qname = qname;
        this.nativeAttributeName = qname.getLocalPart();
    }

    public ResourceAttributeDefinition(QName qname, String nativeAttributeName) {
        this.qname = qname;
        this.nativeAttributeName = nativeAttributeName;
    }

    public String getAttributeDisplayName() {
        if (attributeDisplayName == null) {
            return qname.getLocalPart();
        }

        return attributeDisplayName;
    }

    public void setAttributeDisplayName(String attributeDisplayName) {
        this.attributeDisplayName = attributeDisplayName;
    }

    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
    }

    public int getMaxOccurs() {
        return maxOccurs;
    }

    public void setMaxOccurs(int maxOccurs) {
        this.maxOccurs = maxOccurs;
    }

    public int getMinOccurs() {
        return minOccurs;
    }

    public void setMinOccurs(int minOccurs) {
        this.minOccurs = minOccurs;
    }

    public QName getQName() {
        return qname;
    }

    public SimpleTypeRestriction getRestriction() {
        return restriction;
    }

    public void setRestriction(SimpleTypeRestriction restriction) {
        this.restriction = restriction;
    }

    public QName getType() {
        return type;
    }

    public ResourceAttributeDefinition setType(QName type) {
        this.type = type;
        return this;
    }

    public List<AttributeFlag> getAttributeFlag() {
        if (attributeFlag == null) {
            attributeFlag = new ArrayList<AttributeFlag>();
        }

        return attributeFlag;
    }

    public void setAttributeFlag(List<AttributeFlag> attributeFlag) {
        this.attributeFlag = attributeFlag;
    }

    public boolean isClassifiedAttribute() {
        return classifiedAttributeInfo != null;
    }

    public ClassifiedAttributeInfo getClassifiedAttributeInfo() {
        return classifiedAttributeInfo;
    }

    public boolean isCompositeIdentifier() {
        return compositeIdentifier;
    }

    public void setCompositeIdentifier(boolean compositeIdentifier) {
        this.compositeIdentifier = compositeIdentifier;
    }

    public boolean isDescriptionAttribute() {
        return descriptionAttribute;
    }

    public void setDescriptionAttribute(boolean descriptionAttribute) {
        this.descriptionAttribute = descriptionAttribute;
    }

    public boolean isDisplayName() {
        return displayName;
    }

    public void setDisplayName(boolean displayName) {
        this.displayName = displayName;
    }

    public boolean isIdentifier() {
        return identifier;
    }

    public void setIdentifier(boolean identifier) {
        this.identifier = identifier;
    }

    public ResourceObjectDefinition getResourceObjectReference() {
        return resourceObjectReference;
    }

    public void setResourceObjectReference(ResourceObjectDefinition resourceObjectReference) {
        this.resourceObjectReference = resourceObjectReference;
    }

    public boolean isSecondaryIdentifier() {
        return secondaryIdentifier;
    }

    public void setSecondaryIdentifier(boolean secondaryIdentifier) {
        this.secondaryIdentifier = secondaryIdentifier;
    }

    public boolean canRead() {
        return !getAttributeFlag().contains(AttributeFlag.NOT_READABLE);
    }

    public boolean canUpdate() {
        return !getAttributeFlag().contains(AttributeFlag.NOT_UPDATEABLE);
    }

    public boolean isPasswordAttribute() {
        return !getAttributeFlag().contains(AttributeFlag.PASSWORD);
    }

    public boolean isStoredInRepository() {
        return isIdentifier() || isSecondaryIdentifier() || isCompositeIdentifier() || (null != attributeFlag && attributeFlag.contains(AttributeFlag.STORE_IN_REPOSITORY));
    }

    public String getNativeAttributeName() {
        return nativeAttributeName;
    }

    public ResourceObjectDefinition getParentDefinition() {
        return parentDefinition;
    }

    public void setParentDefinition(ResourceObjectDefinition parentDefinition) {
        this.parentDefinition = parentDefinition;
    }

    public boolean isRequired() {
        if (minOccurs > 0) {
            return true;
        }

        return false;
    }

    public boolean isMultivalue() {
        if (maxOccurs == MAX_OCCURS_UNBOUNDED || maxOccurs > 1) {
            return true;
        }

        return false;
    }

    public boolean isMandatory() {
        if (minOccurs == 0) {
            return false;
        }

        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }
    
    public boolean isFilledWithExpression() {
		return filledWithExpression;
	}
    
    public void setFilledWithExpression(boolean filledWithExpression) {
		this.filledWithExpression = filledWithExpression;
	}

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        // FIXME
//        builder.append(name);
        if (type != null) {
            builder.append(":{");
            builder.append(type.getNamespaceURI());
            builder.append("}");
            builder.append(type.getLocalPart());
        } else {
            builder.append(": null");
        }

        return builder.toString();
    }

    public void makeClassified(Encryption encryption, String classificationLevel) {
        classifiedAttributeInfo = new ClassifiedAttributeInfo();
        classifiedAttributeInfo.encryption = encryption;
        classifiedAttributeInfo.classificationLevel = classificationLevel;
    }

    public class ClassifiedAttributeInfo {

        /**
         * http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd#ClassifiedAttributeType
         */
        private Encryption encryption = Encryption.SYMMETRIC;
        /**
         * http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd#ClassifiedAttributeType
         */
        private String classificationLevel = null;

        private ClassifiedAttributeInfo() {
        }

        public String getClassificationLevel() {
            return classificationLevel;
        }

        public Encryption getEncryption() {
            return encryption;
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (!(obj instanceof ClassifiedAttributeInfo)) {
                return false;
            }

            ClassifiedAttributeInfo info = (ClassifiedAttributeInfo) obj;
            boolean equal = encryption == null ? info.getEncryption() == null : encryption.equals(info.getEncryption());
            if (!equal) {
                return false;
            }

            return classificationLevel == null ? info.getClassificationLevel() == null
                    : classificationLevel.equals(info.getClassificationLevel());
        }
    }

    public boolean is(ResourceAttributeDefinition other) {
        return other != null && other.getQName().equals(getQName());
    }

    public ConverterFactory getConverterFactory() {
        if (getParentDefinition() == null) {
            throw new IllegalArgumentException("parent definition is null");
        }
        if (getParentDefinition().getParentSchema() == null) {
            throw new IllegalArgumentException("parent schema is null");
        }
        return getParentDefinition().getParentSchema().getConverterFactory();
    }
}
