/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.*;
import com.evolveum.midpoint.prism.impl.EnumerationTypeDefinitionImpl.ValueDefinitionImpl;
import com.evolveum.midpoint.prism.impl.schema.PrismSchemaImpl;
import com.evolveum.midpoint.prism.impl.schema.SchemaDomSerializer;
import com.evolveum.midpoint.prism.impl.schema.SchemaParsingUtil;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayHintType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.*;
import com.evolveum.prism.xml.ns._public.annotation_3.AccessAnnotationType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

/**
 * Util methods for PrismSchemaTypeUtil for converting xsd schema to PrismSchemaType and PrismSchemaType to xsd schema
 */
public class PrismSchemaTypeUtil {

    private static final Trace LOGGER = TraceManager.getTrace(PrismSchemaTypeUtil.class);

    /**
     * Supported lifecycle state for PrismSchemaType and child.
     */
    private static final List<String> SUPPORTED_LIFECYCLE_STATE = Arrays.asList(
            SchemaConstants.LIFECYCLE_PROPOSED,
            SchemaConstants.LIFECYCLE_ACTIVE,
            SchemaConstants.LIFECYCLE_DEPRECATED,
            SchemaConstants.LIFECYCLE_ARCHIVED);

    /**
     * Convert PrismSchemaType to SchemaDefinitionType that contains xsd.
     */
    public static SchemaDefinitionType convertToSchemaDefinitionType(PrismSchemaType prismSchemaBean, @Nullable String lifecycleState) throws SchemaException {
        PrismSchemaImpl parsedSchema = new PrismSchemaImpl(prismSchemaBean.getNamespace());

        prismSchemaBean.getComplexType().forEach(complexTypeBean -> {
            ComplexTypeDefinitionImpl complexTypeDefinition = new ComplexTypeDefinitionImpl(
                    new QName(
                            prismSchemaBean.getNamespace(),
                            complexTypeBean.getName().getLocalPart(),
                            complexTypeBean.getName().getPrefix()));
            processComplexTypeDefinition(complexTypeDefinition.mutator(), complexTypeBean, lifecycleState);
            parsedSchema.add((Definition) complexTypeDefinition);
        });

        prismSchemaBean.getEnumerationType().forEach(enumTypeBean -> {
            List<ValueDefinitionImpl> values = collectValues(enumTypeBean);
            if (enumTypeBean.getBaseType() == null) {
                enumTypeBean.setBaseType(DOMUtil.XSD_STRING);
            }
            EnumerationTypeDefinitionImpl enumTypeDefinition = new EnumerationTypeDefinitionImpl(
                    new QName(
                            prismSchemaBean.getNamespace(),
                            enumTypeBean.getName().getLocalPart(),
                            enumTypeBean.getName().getPrefix()),
                    enumTypeBean.getBaseType(),
                    (List) values);
            processDefinition(enumTypeDefinition.mutator(), enumTypeBean, lifecycleState);
            parsedSchema.add((Definition) enumTypeDefinition);
        });

        SchemaDomSerializer serializer = new SchemaDomSerializer(parsedSchema);
        Document doc = serializer.serializeSchema();
        Element schemaElement = DOMUtil.getFirstChildElement(doc);
        if (StringUtils.isNotBlank(prismSchemaBean.getDefaultPrefix())) {
            DynamicNamespacePrefixMapper namespaceMapper = PrismContext.get().getSchemaRegistry().getNamespacePrefixMapper();
            Element annotationElement = DOMUtil.getOrCreateSubElement(
                    doc,
                    schemaElement,
                    namespaceMapper.setQNamePrefix(PrismConstants.SCHEMA_ANNOTATION));
            Element appinfoElement = DOMUtil.getOrCreateSubElement(
                    doc,
                    annotationElement,
                    namespaceMapper.setQNamePrefix(PrismConstants.SCHEMA_APP_INFO));
            Element defaultPrefix = DOMUtil.createElement(
                    doc,
                    namespaceMapper.setQNamePrefix(PrismConstants.A_DEFAULT_PREFIX));
            appinfoElement.appendChild(defaultPrefix);
            defaultPrefix.setTextContent(prismSchemaBean.getDefaultPrefix());
        }
        SchemaDefinitionType schemaDefinitionType = new SchemaDefinitionType();
        schemaDefinitionType.setSchema(schemaElement);
        return schemaDefinitionType;
    }

    private static List<ValueDefinitionImpl> collectValues(EnumerationTypeDefinitionType enumTypeBean) {
        return enumTypeBean.getValues().stream()
                .map(value -> new ValueDefinitionImpl(value.getValue(), value.getDocumentation(), value.getConstantName()))
                .toList();
    }

    private static void processComplexTypeDefinition(
            ComplexTypeDefinition.ComplexTypeDefinitionMutator complexTypeDefinition, ComplexTypeDefinitionType complexTypeBean, String parentLifecycleState) {
        if (complexTypeBean == null) {
            return;
        }
        processDefinition(complexTypeDefinition, complexTypeBean, parentLifecycleState);

        String lifecycleState = getLifecycleStateForDefinition(parentLifecycleState, complexTypeBean.getLifecycleState());

        ((ComplexTypeDefinition.ComplexTypeDefinitionLikeBuilder) complexTypeDefinition).setExtensionForType(complexTypeBean.getExtension());
        complexTypeBean.getItemDefinitions().forEach(definitionBean -> {
            ItemDefinition itemDefinition = null;
            if (definitionBean instanceof PrismPropertyDefinitionType propertyDefinitionBean) {
                itemDefinition = new PrismPropertyDefinitionImpl(
                        new QName(((ComplexTypeDefinitionImpl) complexTypeDefinition).getTypeName().getNamespaceURI(),
                                propertyDefinitionBean.getName().getLocalPart(),
                                propertyDefinitionBean.getName().getPrefix()),
                        propertyDefinitionBean.getType());
                processItemDefinition(itemDefinition.mutator(), definitionBean, lifecycleState);
            }

            if (definitionBean instanceof PrismContainerDefinitionType containerDefinitionBean) {

                itemDefinition = PrismContext.get().definitionFactory().newContainerDefinitionWithoutTypeDefinition(
                        new QName(((ComplexTypeDefinitionImpl) complexTypeDefinition).getTypeName().getNamespaceURI(),
                                containerDefinitionBean.getName().getLocalPart(),
                                containerDefinitionBean.getName().getPrefix()),
                        containerDefinitionBean.getType());
                processItemDefinition(itemDefinition.mutator(), definitionBean, lifecycleState);
            }

            if (definitionBean instanceof PrismReferenceDefinitionType referenceDefinitionBean) {
                itemDefinition = new PrismReferenceDefinitionImpl(
                        new QName(((ComplexTypeDefinitionImpl) complexTypeDefinition).getTypeName().getNamespaceURI(),
                                referenceDefinitionBean.getName().getLocalPart(),
                                referenceDefinitionBean.getName().getPrefix()),
                        referenceDefinitionBean.getType());
                processReferenceDefinition((PrismReferenceDefinition.PrismReferenceDefinitionMutator) itemDefinition.mutator(), referenceDefinitionBean, lifecycleState);
            }

            if (itemDefinition != null) {
                complexTypeDefinition.add(itemDefinition);
            }
        });
    }

    private static void processReferenceDefinition(
            PrismReferenceDefinition.PrismReferenceDefinitionMutator mutator, PrismReferenceDefinitionType referenceDefinitionBean, String lifecycleState) {
        if (referenceDefinitionBean == null) {
            return;
        }

        processItemDefinition(mutator, referenceDefinitionBean, lifecycleState);
        if (referenceDefinitionBean.getObjectReferenceTargetType() != null) {
            mutator.setTargetTypeName(referenceDefinitionBean.getObjectReferenceTargetType());
        }
    }

    private static void processItemDefinition(
            ItemDefinition.ItemDefinitionMutator mutator, PrismItemDefinitionType definitionBean, String lifecycleState) {
        if (definitionBean == null) {
            return;
        }

        processDefinition(mutator, definitionBean, lifecycleState);

        mutator.setIndexed(Boolean.TRUE.equals(definitionBean.isIndexed()));
        if (definitionBean.getValueEnumerationRef() != null) {
            mutator.setValueEnumerationRef(definitionBean.getValueEnumerationRef().asReferenceValue());
        }
        if (!definitionBean.getAccess().isEmpty()) {
            if (definitionBean.getAccess().contains(AccessAnnotationType.NONE)) {
                mutator.setCanAdd(false);
                mutator.setCanModify(false);
                mutator.setCanRead(false);
            } else {
                mutator.setCanAdd(definitionBean.getAccess().contains(AccessAnnotationType.CREATE));
                mutator.setCanModify(definitionBean.getAccess().contains(AccessAnnotationType.UPDATE));
                mutator.setCanRead(definitionBean.getAccess().contains(AccessAnnotationType.READ));
            }
        }

        if (Boolean.TRUE.equals(definitionBean.isRequired())) {
            mutator.setMinOccurs(1);
        } else {
            mutator.setMinOccurs(0);
        }

        if (Boolean.TRUE.equals(definitionBean.isMultivalue())) {
            mutator.setMaxOccurs(-1);
        } else {
            mutator.setMaxOccurs(1);
        }

    }

    private static void processDefinition(
            Definition.DefinitionMutator definition, DefinitionType definitionTypeBean, String parentLifecycleState) {
        if (definitionTypeBean == null) {
            return;
        }

        definition.setDisplayName(definitionTypeBean.getDisplayName());
        definition.setHelp(definitionTypeBean.getHelp());
        definition.setDocumentation(definitionTypeBean.getDocumentation());
        definition.setDisplayOrder(definitionTypeBean.getDisplayOrder());
        if (definitionTypeBean.getDisplayHint() != null) {
            definition.setDisplayHint(DisplayHint.valueOf(definitionTypeBean.getDisplayHint().name()));
        }

        String lifecycleState = getLifecycleStateForDefinition(parentLifecycleState, definitionTypeBean.getLifecycleState());

        setParameterByLifecycleState(definition, lifecycleState);

    }

    private static void setParameterByLifecycleState(Definition.DefinitionMutator definition, @NotNull String lifecycleState) {
        if (lifecycleState.equals(SchemaConstants.LIFECYCLE_DEPRECATED)) {
            definition.setDeprecated(true);
        }

        if (lifecycleState.equals(SchemaConstants.LIFECYCLE_ARCHIVED)) {
            definition.setRemoved(true);
        }
    }

    private static String getLifecycleStateForDefinition(String initParentLifecycleState, String childLifecycleState) {
        var parentLifecycleState = getSupportedLifecycleState(initParentLifecycleState);
        var lifecycleState = getSupportedLifecycleState(childLifecycleState);
        var parentIndex = SUPPORTED_LIFECYCLE_STATE.indexOf(parentLifecycleState);
        var index = SUPPORTED_LIFECYCLE_STATE.indexOf(lifecycleState);

        if (parentIndex > index) {
            return parentLifecycleState;
        }
        return lifecycleState;
    }

    private static String getSupportedLifecycleState(String lifecycleState) {
        String ret = Objects.requireNonNullElse(lifecycleState, SchemaConstants.LIFECYCLE_ACTIVE);

        if (SUPPORTED_LIFECYCLE_STATE.contains(ret)) {
            return ret;
        }

        LOGGER.warn("Unsupported lifecycleState: " + ret + "; expected " + SUPPORTED_LIFECYCLE_STATE);
        return SchemaConstants.LIFECYCLE_PROPOSED;
    }

    /**
     * Convert SchemaDefinitionType that contains xsd to PrismSchemaType.
     */
    public static PrismSchemaType convertToPrismSchemaType(SchemaDefinitionType schemaDefinition, @Nullable String lifecycleState) throws SchemaException {
        Element schemaElement = schemaDefinition.getSchema();
        PrismSchemaImpl parsedSchema = SchemaParsingUtil.createAndParse(
                schemaElement, true, "schema for PrismSchemaType converter", false);
        PrismSchemaType prismSchema = new PrismSchemaType();

        prismSchema.namespace(parsedSchema.getNamespace());
        Optional<Element> defaultPrefix = DOMUtil.getElement(
                schemaElement,
                PrismConstants.SCHEMA_ANNOTATION, PrismConstants.SCHEMA_APP_INFO, PrismConstants.A_DEFAULT_PREFIX);
        if(defaultPrefix.isPresent()) {
            prismSchema.defaultPrefix(defaultPrefix.get().getTextContent());
        }

        parsedSchema.getComplexTypeDefinitions().forEach(complexTypeDef -> {
            ComplexTypeDefinitionType complexTypeBean = new ComplexTypeDefinitionType();
            processComplexType(complexTypeBean, complexTypeDef, lifecycleState);
            prismSchema.complexType(complexTypeBean);
        });

        parsedSchema.getDefinitions(EnumerationTypeDefinition.class).forEach(enumTypeDef -> {
            EnumerationTypeDefinitionType enumTypeBean = new EnumerationTypeDefinitionType();
            processEnumType(enumTypeBean, enumTypeDef, lifecycleState);
            prismSchema.enumerationType(enumTypeBean);
        });

        return prismSchema;
    }

    private static void processEnumType(
            EnumerationTypeDefinitionType enumTypeBean, EnumerationTypeDefinition enumTypeDef, String lifecycleState) {
        if (enumTypeDef == null) {
            return;
        }

        processDefinitionType(enumTypeBean, enumTypeDef, lifecycleState);

        enumTypeBean.baseType(enumTypeDef.getBaseTypeName());
        enumTypeDef.getValues().forEach(valueDef -> enumTypeBean.values(new EnumerationValueTypeDefinitionType()
                .value(valueDef.getValue())
                .documentation(valueDef.getDocumentation().orElse(null))
                .constantName(valueDef.getConstantName().orElse(null))));
    }

    private static void processComplexType(
            ComplexTypeDefinitionType complexTypeBean, ComplexTypeDefinition complexTypeDef, String lifecycleState) {
        if (complexTypeDef == null) {
            return;
        }

        processDefinitionType(complexTypeBean, complexTypeDef, lifecycleState);
        complexTypeBean.extension(complexTypeDef.getExtensionForType());

        complexTypeDef.getDefinitions().forEach(def -> {
            PrismItemDefinitionType itemDefBean = null;

            if (def instanceof PrismPropertyDefinition propertyDef) {
                itemDefBean = new PrismPropertyDefinitionType();
                processItemDefType(itemDefBean, propertyDef, lifecycleState);
            }

            if (def instanceof PrismContainerDefinition containerDef) {
                itemDefBean = new PrismContainerDefinitionType();
                processItemDefType(itemDefBean, containerDef, lifecycleState);
            }

            if (def instanceof PrismReferenceDefinition referenceDef) {
                itemDefBean = new PrismReferenceDefinitionType();
                processReferenceDefType((PrismReferenceDefinitionType) itemDefBean, referenceDef, lifecycleState);
            }

            if (itemDefBean != null) {
                complexTypeBean.itemDefinitions(itemDefBean);
            }
        });

    }

    private static void processReferenceDefType(
            PrismReferenceDefinitionType refDefBean, PrismReferenceDefinition referenceDef, String lifecycleState) {
        if (referenceDef == null) {
            return;
        }

        processItemDefType(refDefBean, referenceDef, lifecycleState);
        refDefBean.objectReferenceTargetType(referenceDef.getTargetTypeName());
    }

    private static void processItemDefType(
            PrismItemDefinitionType itemBean, ItemDefinition itemDef, String lifecycleState) {
        if (itemDef == null) {
            return;
        }

        processDefinitionType(itemBean, itemDef, lifecycleState);
        itemBean.type(itemDef.getTypeName())
                .name(new QName(itemDef.getItemName().getLocalPart()))
                .indexed(itemDef.isIndexed())
                .required(itemDef.isMandatory())
                .multivalue(itemDef.isMultiValue());

        if (itemDef.getValueEnumerationRef() != null && itemDef.getValueEnumerationRef().getRealValue() != null) {
            @Nullable Referencable reference = itemDef.getValueEnumerationRef().getRealValue();
            itemBean.valueEnumerationRef(reference.getOid(), reference.getType(), reference.getRelation());
        }

        if (!itemDef.canModify() && !itemDef.canAdd() && !itemDef.canRead()) {
            itemBean.access(AccessAnnotationType.NONE);
        } else if (!itemDef.canModify() || !itemDef.canAdd() || !itemDef.canRead()) {
            if (itemDef.canModify()) {
                itemBean.access(AccessAnnotationType.UPDATE);
            }
            if (itemDef.canAdd()) {
                itemBean.access(AccessAnnotationType.CREATE);
            }
            if (itemDef.canRead()) {
                itemBean.access(AccessAnnotationType.READ);
            }
        }
    }

    private static void processDefinitionType(
            DefinitionType defTypeBean, Definition definition, @Nullable String lifecycleState) {
        if (definition == null) {
            return;
        }

        if (definition.getDisplayHint() != null) {
            defTypeBean.displayHint(DisplayHintType.valueOf(definition.getDisplayHint().name()));
        }

        defTypeBean.name(definition.getTypeName())
                .displayName(definition.getDisplayName())
                .help(definition.getHelp())
                .documentation(definition.getDocumentation())
                .displayOrder(definition.getDisplayOrder());

        var explicitStateName = getSupportedLifecycleState(lifecycleState);

        if (explicitStateName.equals(SchemaConstants.LIFECYCLE_ACTIVE)) {
            if (definition.isExperimental()) {
                defTypeBean.lifecycleState(SchemaConstants.LIFECYCLE_PROPOSED);
            } else if (definition.isDeprecated()) {
                defTypeBean.lifecycleState(SchemaConstants.LIFECYCLE_DEPRECATED);
            } else if (definition.isRemoved()) {
                defTypeBean.lifecycleState(SchemaConstants.LIFECYCLE_ARCHIVED);
            } else {
                defTypeBean.lifecycleState(SchemaConstants.LIFECYCLE_ACTIVE);
            }
        } else {
            defTypeBean.lifecycleState(explicitStateName);
        }
    }

}
