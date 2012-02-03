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

import com.evolveum.midpoint.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.util.DebugUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.*;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author semancik
 */
public class RefinedAccountDefinition extends ResourceObjectDefinition implements Dumpable, DebugDumpable {

    private static final String DEFAULT_ACCOUNT_NAME = "user";

    private String accountType;
    private String displayName;
    private String description;
    private boolean isDefault;
    private ResourceObjectDefinition objectClassDefinition;
    private ResourceType resourceType;
    private SchemaRegistry schemaRegistry;
    /**
     * Refined object definition. The "any" parts are replaced with appropriate schema (e.g. resource schema)
     */
    ObjectDefinition<AccountShadowType> objectDefinition = null;

    private Collection<RefinedAttributeDefinition> attributeDefinitions;

    private RefinedAccountDefinition(Schema rSchema, SchemaRegistry schemaRegistry, ResourceType resourceType) {
        super(rSchema, SchemaConstants.I_ATTRIBUTES, null);
        attributeDefinitions = new HashSet<RefinedAttributeDefinition>();
        this.resourceType = resourceType;
        this.schemaRegistry = schemaRegistry;
    }

    private RefinedAccountDefinition(Schema rSchema, SchemaRegistry schemaRegistry, ResourceType resourceType,
            ResourceObjectDefinition objectClassDefinition) {
        super(rSchema, SchemaConstants.I_ATTRIBUTES, null);
        attributeDefinitions = new HashSet<RefinedAttributeDefinition>();
        this.resourceType = resourceType;
        this.objectClassDefinition = objectClassDefinition;
        this.schemaRegistry = schemaRegistry;
    }

    @Override
    public Collection<ResourceObjectAttributeDefinition> getIdentifiers() {
        return objectClassDefinition.getIdentifiers();
    }

    @Override
    public Set<ResourceObjectAttributeDefinition> getSecondaryIdentifiers() {
        return objectClassDefinition.getSecondaryIdentifiers();
    }

    @Override
    public ResourceObjectAttributeDefinition getDescriptionAttribute() {
        return objectClassDefinition.getDescriptionAttribute();
    }

    @Override
    public void setDescriptionAttribute(ResourceObjectAttributeDefinition descriptionAttribute) {
        throw new UnsupportedOperationException("Parts of refined account are immutable");
    }

    @Override
    public ResourceObjectAttributeDefinition getNamingAttribute() {
        return objectClassDefinition.getNamingAttribute();
    }

    @Override
    public String getNativeObjectClass() {
        return objectClassDefinition.getNativeObjectClass();
    }

    @Override
    public void setAccountType(boolean accountType) {
        throw new UnsupportedOperationException("Parts of refined account are immutable");
    }

    @Override
    public boolean isDefaultAccountType() {
        return isDefault;
    }

    @Override
    public void setDefaultAccountType(boolean defaultAccountType) {
        this.isDefault = defaultAccountType;
    }

    @Override
    public String getAccountTypeName() {
        return accountType;
    }

    @Override
    public void setAccountTypeName(String accountTypeName) {
        this.accountType = accountTypeName;
    }

    @Override
    public ResourceObjectAttributeDefinition getDisplayNameAttribute() {
        return objectClassDefinition.getDisplayNameAttribute();
    }

    @Override
    public void setDisplayNameAttribute(QName displayName) {
        throw new UnsupportedOperationException("Parts of refined account are immutable");
    }

    @Override
    public ResourceObject instantiate(PropertyPath parentPath) {
        return new ResourceObject(getNameOrDefaultName(), this, null, parentPath);
    }

    @Override
    public ResourceObject instantiate(QName name, PropertyPath parentPath) {
        return new ResourceObject(name, this, null, parentPath);
    }

    @Override
    public ResourceObject instantiate(QName name, Object element, PropertyPath parentPath) {
        return new ResourceObject(name, this, element, parentPath);
    }

    @Override
    public RefinedAccountDefinition clone() {
        RefinedAccountDefinition clone = new RefinedAccountDefinition(schema, schemaRegistry, resourceType, objectClassDefinition);
        copyDefinitionData(clone);
        return clone;
    }

    protected void copyDefinitionData(RefinedAccountDefinition clone) {
        super.copyDefinitionData(clone);
        clone.accountType = this.accountType;
        clone.attributeDefinitions = this.attributeDefinitions;
        clone.description = this.description;
        clone.displayName = this.displayName;
        clone.isDefault = this.isDefault;
        clone.objectClassDefinition = this.objectClassDefinition;
        clone.objectDefinition = this.objectDefinition;
        clone.resourceType = this.resourceType;
        clone.schemaRegistry = this.schemaRegistry;
    }

    @Override
    public Set<ResourceObjectAttribute> parseAttributes(List<Object> elements, PropertyPath parentPath) throws SchemaException {
        return objectClassDefinition.parseAttributes(elements, parentPath);
    }

    @Override
    public Collection<? extends ResourceObjectAttribute> parseIdentifiers(List<Object> elements, PropertyPath parentPath)
            throws SchemaException {
        return objectClassDefinition.parseIdentifiers(elements, parentPath);
    }

    @Override
    public RefinedAttributeDefinition findAttributeDefinition(QName elementQName) {
        return findItemDefinition(elementQName, RefinedAttributeDefinition.class);
    }

    @Override
    public RefinedAttributeDefinition findAttributeDefinition(String elementLocalname) {
        QName elementQName = new QName(schema.getNamespace(), elementLocalname);
        return findAttributeDefinition(elementQName);
    }

    @Override
    public ResourceObjectAttributeDefinition createAttributeDefinition(QName name, QName typeName) {
        throw new UnsupportedOperationException("Parts of refined account are immutable");
    }

    @Override
    public ResourceObjectAttributeDefinition createAttributeDefinition(String localName, QName typeName) {
        throw new UnsupportedOperationException("Parts of refined account are immutable");
    }

    @Override
    public ResourceObjectAttributeDefinition createAttributeDefinition(String localName, String localTypeName) {
        throw new UnsupportedOperationException("Parts of refined account are immutable");
    }


    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public boolean isRuntimeSchema() {
        return true;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public ResourceObjectDefinition getObjectClassDefinition() {
        return objectClassDefinition;
    }

    public void setObjectClassDefinition(ResourceObjectDefinition objectClassDefinition) {
        this.objectClassDefinition = objectClassDefinition;
    }

    @Override
    public boolean isAccountType() {
        return true;
    }

    @Override
    public Collection<RefinedAttributeDefinition> getAttributeDefinitions() {
        return attributeDefinitions;
    }

    @Override
    public Collection<ItemDefinition> getDefinitions() {
        return (Collection) attributeDefinitions;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public ObjectDefinition<AccountShadowType> getObjectDefinition() {
        if (objectDefinition == null) {
            constructObjectDefinition();
        }
        return objectDefinition;
    }

    private void constructObjectDefinition() {
        // Almost-shallow clone of object definition and complex type
        ObjectDefinition<AccountShadowType> originalObjectDefinition = schemaRegistry.getObjectSchema().findObjectDefinition(AccountShadowType.class);
        ObjectDefinition<AccountShadowType> refinedObjectDef = originalObjectDefinition.clone();
        ComplexTypeDefinition originalComplexTypeDefinition = refinedObjectDef.getComplexTypeDefinition();
        ComplexTypeDefinition refinedComplexTypeDefinition = originalComplexTypeDefinition.clone();
        refinedObjectDef.setComplexTypeDefinition(refinedComplexTypeDefinition);

        // Replace definition of "attributes" with objectClass definition
        refinedComplexTypeDefinition.replaceDefinition(SchemaConstants.I_ATTRIBUTES, this);

        // TODO: extension

        this.objectDefinition = refinedObjectDef;
    }

    public RefinedAttributeDefinition getAttributeDefinition(QName attributeName) {
        for (RefinedAttributeDefinition attrDef : attributeDefinitions) {
            if (attrDef.getName().equals(attributeName)) {
                return attrDef;
            }
        }
        return null;
    }


    public void add(RefinedAttributeDefinition refinedAttributeDefinition) {
        attributeDefinitions.add(refinedAttributeDefinition);
    }

    public boolean containsAttributeDefinition(QName attributeName) {
        for (RefinedAttributeDefinition rAttributeDef : attributeDefinitions) {
            if (rAttributeDef.getName().equals(attributeName)) {
                return true;
            }
        }
        return false;
    }

    static RefinedAccountDefinition parse(ResourceAccountTypeDefinitionType accountTypeDefType,
            ResourceType resourceType,
            RefinedResourceSchema rSchema, SchemaRegistry schemaRegistry, String contextDescription) throws
            SchemaException {

        RefinedAccountDefinition rAccountDef = new RefinedAccountDefinition(rSchema, schemaRegistry, resourceType);

        if (accountTypeDefType.getName() != null) {
            rAccountDef.setAccountTypeName(accountTypeDefType.getName());
        } else {
            throw new SchemaException("Account type definition does not have a name, in " + contextDescription);
        }

        ResourceObjectDefinition objectClassDef = null;
        if (accountTypeDefType.getObjectClass() != null) {
            QName objectClass = accountTypeDefType.getObjectClass();
            objectClassDef = rSchema.getOriginalResourceSchema().findResourceObjectDefinitionByType(objectClass);
            if (objectClassDef == null) {
                throw new SchemaException("Object class " + objectClass + " as specified in account type " + accountTypeDefType.getName() + " was not found in the resource schema of " + contextDescription);
            }
            rAccountDef.setObjectClassDefinition(objectClassDef);
        } else {
            throw new SchemaException("Definition of account type " + accountTypeDefType.getName() + " does not have objectclass, in " + contextDescription);
        }

        if (accountTypeDefType.getDisplayName() != null) {
            rAccountDef.setDisplayName(accountTypeDefType.getDisplayName());
        } else {
            if (objectClassDef.getDisplayName() != null) {
                rAccountDef.setDisplayName(objectClassDef.getDisplayName());
            }
        }

        if (accountTypeDefType.getDescription() != null) {
            rAccountDef.setDescription(accountTypeDefType.getDescription());
        }

        if (accountTypeDefType.isDefault() != null) {
            rAccountDef.setDefault(accountTypeDefType.isDefault());
        } else {
            rAccountDef.setDefault(objectClassDef.isDefaultAccountType());
        }

        for (ResourceObjectAttributeDefinition road : objectClassDef.getAttributeDefinitions()) {
//            if (road.isIgnored()) {
//                continue;
//            }
            String attrContextDescription = road.getName() + ", in " + contextDescription;
            ResourceAttributeDefinitionType attrDefType = findAttributeDefinitionType(road.getName(), accountTypeDefType, attrContextDescription);
            if (attrDefType != null && RefinedAttributeDefinition.isIgnored(attrDefType)) {
                continue;
            }

            RefinedAttributeDefinition rAttrDef = RefinedAttributeDefinition.parse(road, attrDefType, objectClassDef, "in account type " + accountTypeDefType.getName() + ", in " + contextDescription);

            if (rAccountDef.containsAttributeDefinition(rAttrDef.getName())) {
                throw new SchemaException("Duplicate definition of attribute " + rAttrDef.getName() + " in account type " + accountTypeDefType.getName() + ", in " + contextDescription);
            }
            rAccountDef.add(rAttrDef);

        }

        // Check for extra attribute definitions in the account type
        for (ResourceAttributeDefinitionType attrDefType : accountTypeDefType.getAttribute()) {
            if (!rAccountDef.containsAttributeDefinition(attrDefType.getRef()) && !RefinedAttributeDefinition.isIgnored(attrDefType)) {
                throw new SchemaException("Definition of attribute " + attrDefType.getRef() + " not found in object class " + objectClassDef.getTypeName() + " as defined in " + contextDescription);
            }
        }

        return rAccountDef;
    }

    static RefinedAccountDefinition parse(ResourceObjectDefinition objectClassDef, ResourceType resourceType,
            RefinedResourceSchema rSchema,
            SchemaRegistry schemaRegistry, String contextDescription) throws SchemaException {

        RefinedAccountDefinition rAccountDef = new RefinedAccountDefinition(rSchema, schemaRegistry, resourceType, objectClassDef);

        String accountTypeName = null;
        if (objectClassDef.getAccountTypeName() != null) {
            accountTypeName = objectClassDef.getAccountTypeName();
            rAccountDef.setAccountTypeName(accountTypeName);
        } else {
            if (objectClassDef.isDefaultAccountType()) {
                rAccountDef.setAccountTypeName(DEFAULT_ACCOUNT_NAME);
            } else {
                throw new SchemaException("Account type definition does not have a name, in " + contextDescription);
            }
        }


        if (objectClassDef.getDisplayName() != null) {
            rAccountDef.setDisplayName(objectClassDef.getDisplayName());
        }

        rAccountDef.setDefault(objectClassDef.isDefaultAccountType());

        for (ResourceObjectAttributeDefinition road : objectClassDef.getAttributeDefinitions()) {
            if (road.isIgnored()) {
                continue;
            }
            String attrContextDescription = accountTypeName + ", in " + contextDescription;

            RefinedAttributeDefinition rAttrDef = RefinedAttributeDefinition.parse(road, null, objectClassDef, attrContextDescription);

            if (rAccountDef.containsAttributeDefinition(rAttrDef.getName())) {
                throw new SchemaException("Duplicate definition of attribute " + rAttrDef.getName() + " in " + attrContextDescription);
            }
            rAccountDef.add(rAttrDef);

        }

        return rAccountDef;


    }


    private static ResourceAttributeDefinitionType findAttributeDefinitionType(QName attrName,
            ResourceAccountTypeDefinitionType accountTypeDefType, String contextDescription) throws SchemaException {
        ResourceAttributeDefinitionType foundAttrDefType = null;
        for (ResourceAttributeDefinitionType attrDefType : accountTypeDefType.getAttribute()) {
            if (attrDefType.getRef() != null) {
                if (attrDefType.getRef().equals(attrName)) {
                    if (foundAttrDefType == null) {
                        foundAttrDefType = attrDefType;
                    } else {
                        throw new SchemaException("Duplicate definition of attribute " + attrDefType.getRef() + " in account type "
                                + accountTypeDefType.getName() + ", in " + contextDescription);
                    }
                }
            } else {
                throw new SchemaException("Missing reference to the attribute schema definition in definition " + DebugUtil.prettyPrint(attrDefType) + " during processing of " + contextDescription);
            }
        }
        return foundAttrDefType;
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
        sb.append("RAccountDef(");
        sb.append(DebugUtil.prettyPrint(getName()));
        if (isDefault()) {
            sb.append(",default");
        }
        sb.append(",");
        sb.append(DebugUtil.prettyPrint(getObjectClassDefinition().getTypeName()));
        sb.append(")\n");
        Iterator<ItemDefinition> i = getDefinitions().iterator();
        while (i.hasNext()) {
            ItemDefinition def = i.next();
            sb.append(def.debugDump(indent + 1));
            if (i.hasNext()) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    @Override
    public String dump() {
        return debugDump(0);
    }

    public AccountShadowType createBlankShadow() {
        AccountShadowType accountShadowType = new AccountShadowType();
        accountShadowType.setAccountType(getAccountTypeName());
        accountShadowType.setObjectClass(objectClassDefinition.getTypeName());
        accountShadowType.setResourceRef(ObjectTypeUtil.createObjectRef(resourceType));
        return accountShadowType;
    }

    public ResourceAccountType getResourceAccountType() {
        return new ResourceAccountType(resourceType.getOid(), getAccountTypeName());
    }

    public Collection<? extends QName> getNamesOfAttributesWithOutboundExpressions() {
        Collection<QName> attrNames = new HashSet<QName>();
        for (RefinedAttributeDefinition attrDef : getAttributeDefinitions()) {
            if (attrDef.getOutboundValueConstructionType() != null) {
                attrNames.add(attrDef.getName());
            }
        }
        return attrNames;
    }

    public Collection<? extends QName> getNamesOfAttributesWithInboundExpressions() {
        Collection<QName> attrNames = new HashSet<QName>();
        for (RefinedAttributeDefinition attrDef : getAttributeDefinitions()) {
            List<ValueAssignmentType> inbounds = attrDef.getInboundAssignmentTypes();
            if (inbounds != null && !inbounds.isEmpty()) {
                attrNames.add(attrDef.getName());
            }
        }

        return attrNames;
    }

    private ResourceAccountTypeDefinitionType getAccountSchemaHandlingDefinition() {
        List<ResourceAccountTypeDefinitionType> types = resourceType.getSchemaHandling().getAccountType();
        ResourceAccountTypeDefinitionType definition = null;
        for (ResourceAccountTypeDefinitionType account : types) {
            if (accountType == null && account.isDefault()) {
                definition = account;
                break;
            }

            if (accountType.equals(account.getName())) {
                definition = account;
                break;
            }
        }

        return definition;
    }

    public ValueAssignmentType getCredentialsInbound() {
        ResourceAccountTypeDefinitionType definition = getAccountSchemaHandlingDefinition();
        if (definition == null) {
            return null;
        }
        ResourceCredentialsDefinitionType credentials = definition.getCredentials();
        if (credentials == null) {
            return null;
        }
        ResourcePasswordDefinitionType password = credentials.getPassword();
        if (password == null || password.getInbound() == null) {
            return null;
        }

        return password.getInbound();
    }

    public ValueAssignmentType getActivationInbound() {
        ResourceAccountTypeDefinitionType definition = getAccountSchemaHandlingDefinition();
        if (definition == null) {
            return null;
        }

        ResourceActivationDefinitionType activation = definition.getActivation();
        if (activation == null) {
            return null;
        }
        ResourceActivationEnableDefinitionType enabled = activation.getEnabled();
        if (enabled == null || enabled.getInbound() == null) {
            return null;
        }

        return (enabled.getInbound());
    }
}
