/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common.refinery;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.DefinitionUtil;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinitionImpl;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
public class RefinedAttributeDefinitionImpl<T> extends ResourceAttributeDefinitionImpl<T> implements RefinedAttributeDefinition<T> {
    private static final long serialVersionUID = 1L;

    private static final LayerType DEFAULT_LAYER = LayerType.MODEL;

    private String displayName;
    private String description;
    private boolean tolerant = true;
    private boolean isExclusiveStrong = false;
    protected Boolean secondaryIdentifierOverride = null;
    private boolean isDisplayNameAttribute = false;
    private List<String> intolerantValuePattern;
    private List<String> tolerantValuePattern;
    private ResourceAttributeDefinition<T> attributeDefinition;
    private AttributeFetchStrategyType fetchStrategy;
    private AttributeStorageStrategyType storageStrategy;
    private MappingType outboundMappingType;
    private List<MappingType> inboundMappingTypes;
    private Map<LayerType, PropertyLimitations> limitationsMap = new HashMap<>();
    private QName matchingRuleQName = null;
    private Integer modificationPriority;
    private Boolean readReplaceMode;
    private PropertyAccessType accessOverride = new PropertyAccessType();
    private boolean isVolatilityTrigger = false;

    private transient @Nullable Optional<ComplexTypeDefinition> structuredType;

    protected RefinedAttributeDefinitionImpl(ResourceAttributeDefinition<T> attrDef, PrismContext prismContext) {
        super(attrDef.getItemName(), attrDef.getTypeName(), prismContext);
        this.attributeDefinition = attrDef;
    }

    @Override
    public void setNativeAttributeName(String nativeAttributeName) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public boolean isTolerant() {
        return tolerant;
    }

    public void setTolerant(boolean tolerant) {
        this.tolerant = tolerant;
    }

    @Override
    public Boolean isSecondaryIdentifierOverride() {
        return secondaryIdentifierOverride;
    }

    public void setSecondaryIdentifierOverride(Boolean secondaryIdentifier) {
        this.secondaryIdentifierOverride = secondaryIdentifier;
    }

    @Override
    public boolean canAdd() {
        return canAdd(DEFAULT_LAYER);
    }

    @Override
    public boolean canAdd(LayerType layer) {
        if (accessOverride.isAdd() != null) {
            return accessOverride.isAdd();
        }
        return limitationsMap.get(layer).getAccess().isAdd();
    }

    @Override
    public boolean canRead() {
        return canRead(DEFAULT_LAYER);
    }

    @Override
    public boolean canRead(LayerType layer) {
        if (accessOverride.isRead() != null) {
            return accessOverride.isRead();
        }
        return limitationsMap.get(layer).getAccess().isRead();
    }

    @Override
    public boolean canModify() {
        return canModify(DEFAULT_LAYER);
    }

    @Override
    public boolean canModify(LayerType layer) {
        if (accessOverride.isModify() != null) {
            return accessOverride.isModify();
        }
        return limitationsMap.get(layer).getAccess().isModify();
    }

    @Override
    public void setReadOnly() {
        setCanRead(false);
    }

    @Override
    public QName getValueType() {
        return attributeDefinition.getValueType();
    }

    @Override
    public void setMinOccurs(int minOccurs) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public void setMaxOccurs(int maxOccurs) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public void setCanRead(boolean read) {
        accessOverride.setRead(read);
    }

    @Override
    public void setCanModify(boolean update) {
        accessOverride.setModify(update);
    }

    @Override
    public void setCanAdd(boolean create) {
        accessOverride.setAdd(create);
    }

    @Override
    public boolean isIgnored() {
        return isIgnored(DEFAULT_LAYER);
    }

    @Override
    public boolean isIgnored(LayerType layer) {
        return limitationsMap.get(layer).getProcessing() == ItemProcessing.IGNORE;
    }

    @Override
    public ItemProcessing getProcessing(LayerType layer) {
        return limitationsMap.get(layer).getProcessing();
    }

    @Override
    public void setProcessing(ItemProcessing processing) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public void setHelp(String help) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public String getDisplayName() {
        if (displayName == null && attributeDefinition != null && StringUtils.isNotEmpty(attributeDefinition.getNativeAttributeName())) {
            return attributeDefinition.getNativeAttributeName();
        }
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        checkMutable();
        this.displayName = displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public ResourceAttributeDefinition<T> getAttributeDefinition() {
        return attributeDefinition;
    }

    public void setAttributeDefinition(ResourceAttributeDefinition<T> attributeDefinition) {
        this.attributeDefinition = attributeDefinition;
    }

    @Override
    public MappingType getOutboundMappingType() {
        return outboundMappingType;
    }

    public void setOutboundMappingType(MappingType outboundMappingType) {
        this.outboundMappingType = outboundMappingType;
    }

    @Override
    public boolean hasOutboundMapping() {
        return outboundMappingType != null;
    }

    @Override
    public List<MappingType> getInboundMappingTypes() {
        return inboundMappingTypes;
    }

    public void setInboundMappingTypes(List<MappingType> inboundAssignmentTypes) {
        this.inboundMappingTypes = inboundAssignmentTypes;
    }

    @Override
    @NotNull
    public ItemName getItemName() {
        return attributeDefinition.getItemName();
    }

    @Override
    @NotNull
    public QName getTypeName() {
        return attributeDefinition.getTypeName();
    }

    @Override
    public String getNativeAttributeName() {
        return attributeDefinition.getNativeAttributeName();
    }

    @Override
    public String getFrameworkAttributeName() {
        return attributeDefinition.getFrameworkAttributeName();
    }

    @Override
    public Collection<? extends DisplayableValue<T>> getAllowedValues() {
        return attributeDefinition.getAllowedValues();
    }

    @Override
    public boolean isReturnedByDefault() {
        return attributeDefinition.isReturnedByDefault();
    }

    @Override
    public void setReturnedByDefault(Boolean returnedByDefault) {
        throw new UnsupportedOperationException("Cannot change returnedByDefault");
    }

    @Override
    public int getMaxOccurs() {
        return getMaxOccurs(DEFAULT_LAYER);
    }

    @Override
    public int getMaxOccurs(LayerType layer) {
        return limitationsMap.get(layer).getMaxOccurs();
    }

    @Override
    public int getMinOccurs() {
        return getMinOccurs(DEFAULT_LAYER);
    }

    @Override
    public int getMinOccurs(LayerType layer) {
        return limitationsMap.get(layer).getMinOccurs();
    }

    @Override
    public boolean isOptional(LayerType layer) {
        return limitationsMap.get(layer).getMinOccurs() == 0;
    }

    @Override
    public boolean isMandatory(LayerType layer) {
        return limitationsMap.get(layer).getMinOccurs() > 0;
    }

    @Override
    public boolean isMultiValue(LayerType layer) {
        int maxOccurs = limitationsMap.get(layer).getMaxOccurs();
        return maxOccurs < 0 || maxOccurs > 1;
    }

    @Override
    public boolean isSingleValue(LayerType layer) {
        return limitationsMap.get(layer).getMaxOccurs() == 1;
    }

    @Override
    public boolean isExclusiveStrong() {
        return isExclusiveStrong;
    }

    public void setExclusiveStrong(boolean isExclusiveStrong) {
        this.isExclusiveStrong = isExclusiveStrong;
    }

    @Override
    public PropertyLimitations getLimitations(LayerType layer) {
        return limitationsMap.get(layer);
    }

    @Override
    public String getHelp() {
        return attributeDefinition.getHelp();
    }

    @Override
    public AttributeFetchStrategyType getFetchStrategy() {
        return fetchStrategy;
    }

    public void setFetchStrategy(AttributeFetchStrategyType fetchStrategy) {
        this.fetchStrategy = fetchStrategy;
    }

    @Override
    public AttributeStorageStrategyType getStorageStrategy() {
        return storageStrategy;
    }

    public void setStorageStrategy(AttributeStorageStrategyType storageStrategy) {
        this.storageStrategy = storageStrategy;
    }

    @Override
    public QName getMatchingRuleQName() {
        return matchingRuleQName;
    }

    @Override
    public void setMatchingRuleQName(QName matchingRuleQName) {
        checkMutable();
        this.matchingRuleQName = matchingRuleQName;
    }

    @Override
    public List<String> getTolerantValuePattern() {
        return tolerantValuePattern;
    }

    @Override
    public List<String> getIntolerantValuePattern() {
        return intolerantValuePattern;
    }

    @Override
    public boolean isVolatilityTrigger() {
        return isVolatilityTrigger;
    }

    public void setVolatilityTrigger(boolean isVolatilityTrigger) {
        this.isVolatilityTrigger = isVolatilityTrigger;
    }

    // schemaHandlingAttrDefType may be null if we are parsing from schema only
    static <T> RefinedAttributeDefinition<T> parse(ResourceAttributeDefinition<T> schemaAttrDef,
            ResourceAttributeDefinitionType schemaHandlingAttrDefType, ObjectClassComplexTypeDefinition objectClassDef,
            PrismContext prismContext, String contextDescription) throws SchemaException {

        RefinedAttributeDefinitionImpl<T> rAttrDef = new RefinedAttributeDefinitionImpl<>(schemaAttrDef, prismContext);

        if (schemaHandlingAttrDefType != null && schemaHandlingAttrDefType.getDisplayName() != null) {
            rAttrDef.setDisplayName(schemaHandlingAttrDefType.getDisplayName());
        } else {
            if (schemaAttrDef.getDisplayName() != null) {
                rAttrDef.setDisplayName(schemaAttrDef.getDisplayName());
            }
        }

        if (schemaHandlingAttrDefType != null && schemaHandlingAttrDefType.getDisplayOrder() != null) {
            rAttrDef.setDisplayOrder(schemaHandlingAttrDefType.getDisplayOrder());
        } else {
            if (schemaAttrDef.getDisplayOrder() != null) {
                rAttrDef.setDisplayOrder(schemaAttrDef.getDisplayOrder());
            }
        }

        rAttrDef.matchingRuleQName = schemaAttrDef.getMatchingRuleQName();
        if (schemaHandlingAttrDefType != null) {
            rAttrDef.fetchStrategy = schemaHandlingAttrDefType.getFetchStrategy();
            rAttrDef.storageStrategy = schemaHandlingAttrDefType.getStorageStrategy();
            rAttrDef.setIndexOnly(rAttrDef.storageStrategy == AttributeStorageStrategyType.INDEX_ONLY);
            if (schemaHandlingAttrDefType.getMatchingRule() != null) {
                rAttrDef.matchingRuleQName = schemaHandlingAttrDefType.getMatchingRule();
            }
        }

        PropertyLimitations schemaLimitations = getOrCreateLimitations(rAttrDef.limitationsMap, LayerType.SCHEMA);
        schemaLimitations.setMinOccurs(schemaAttrDef.getMinOccurs());
        schemaLimitations.setMaxOccurs(schemaAttrDef.getMaxOccurs());
        schemaLimitations.setProcessing(schemaAttrDef.getProcessing());
        schemaLimitations.getAccess().setAdd(schemaAttrDef.canAdd());
        schemaLimitations.getAccess().setModify(schemaAttrDef.canModify());
        schemaLimitations.getAccess().setRead(schemaAttrDef.canRead());

        if (schemaHandlingAttrDefType != null) {

            if (schemaHandlingAttrDefType.getDescription() != null) {
                rAttrDef.setDescription(schemaHandlingAttrDefType.getDescription());
            }

            if (schemaHandlingAttrDefType.isTolerant() == null) {
                rAttrDef.tolerant = true;
            } else {
                rAttrDef.tolerant = schemaHandlingAttrDefType.isTolerant();
            }

            rAttrDef.secondaryIdentifierOverride = schemaHandlingAttrDefType.isSecondaryIdentifier();

            rAttrDef.tolerantValuePattern = schemaHandlingAttrDefType.getTolerantValuePattern();
            rAttrDef.intolerantValuePattern = schemaHandlingAttrDefType.getIntolerantValuePattern();

            rAttrDef.isExclusiveStrong = BooleanUtils.isTrue(schemaHandlingAttrDefType.isExclusiveStrong());
            rAttrDef.isVolatilityTrigger = BooleanUtils.isTrue(schemaHandlingAttrDefType.isVolatilityTrigger());

            if (schemaHandlingAttrDefType.getOutbound() != null) {
                rAttrDef.setOutboundMappingType(schemaHandlingAttrDefType.getOutbound());
            }

            if (schemaHandlingAttrDefType.getInbound() != null) {
                rAttrDef.setInboundMappingTypes(schemaHandlingAttrDefType.getInbound());
            }

            rAttrDef.setModificationPriority(schemaHandlingAttrDefType.getModificationPriority());

            rAttrDef.setReadReplaceMode(schemaHandlingAttrDefType.isReadReplaceMode());            // may be null at this point

            if (schemaHandlingAttrDefType.isDisplayNameAttribute() != null && schemaHandlingAttrDefType.isDisplayNameAttribute()) {
                rAttrDef.isDisplayNameAttribute = true;
            }
        }

        PropertyLimitations previousLimitations = null;
        for (LayerType layer : LayerType.values()) {
            PropertyLimitations limitations = getOrCreateLimitations(rAttrDef.limitationsMap, layer);
            if (previousLimitations != null) {
                limitations.setMinOccurs(previousLimitations.getMinOccurs());
                limitations.setMaxOccurs(previousLimitations.getMaxOccurs());
                limitations.setProcessing(previousLimitations.getProcessing());
                limitations.getAccess().setAdd(previousLimitations.getAccess().isAdd());
                limitations.getAccess().setRead(previousLimitations.getAccess().isRead());
                limitations.getAccess().setModify(previousLimitations.getAccess().isModify());
            }
            previousLimitations = limitations;
            if (schemaHandlingAttrDefType != null) {
                if (layer != LayerType.SCHEMA) {
                    // SCHEMA is a pseudo-layer. It cannot be overridden ... unless specified explicitly
                    PropertyLimitationsType genericLimitationsType = MiscSchemaUtil.getLimitationsType(schemaHandlingAttrDefType.getLimitations(), null);
                    if (genericLimitationsType != null) {
                        applyLimitationsType(limitations, genericLimitationsType);
                    }
                }
                PropertyLimitationsType layerLimitationsType = MiscSchemaUtil.getLimitationsType(schemaHandlingAttrDefType.getLimitations(), layer);
                if (layerLimitationsType != null) {
                    applyLimitationsType(limitations, layerLimitationsType);
                }
            }
        }

        return rAttrDef;

    }

    private static void applyLimitationsType(PropertyLimitations limitations, PropertyLimitationsType layerLimitationsType) {
        if (layerLimitationsType.getMinOccurs() != null) {
            limitations.setMinOccurs(DefinitionUtil.parseMultiplicity(layerLimitationsType.getMinOccurs()));
        }
        if (layerLimitationsType.getMaxOccurs() != null) {
            limitations.setMaxOccurs(DefinitionUtil.parseMultiplicity(layerLimitationsType.getMaxOccurs()));
        }
        if (layerLimitationsType.isIgnore() != null) {
            limitations.setProcessing(ItemProcessing.IGNORE);
        }
        if (layerLimitationsType.getProcessing() != null) {
            limitations.setProcessing(MiscSchemaUtil.toItemProcessing(layerLimitationsType.getProcessing()));
        }
        if (layerLimitationsType.getAccess() != null) {
            PropertyAccessType accessType = layerLimitationsType.getAccess();
            if (accessType.isAdd() != null) {
                limitations.getAccess().setAdd(accessType.isAdd());
            }
            if (accessType.isRead() != null) {
                limitations.getAccess().setRead(accessType.isRead());
            }
            if (accessType.isModify() != null) {
                limitations.getAccess().setModify(accessType.isModify());
            }
        }

    }

    private static PropertyLimitations getOrCreateLimitations(Map<LayerType, PropertyLimitations> limitationsMap,
            LayerType layer) {
        PropertyLimitations limitations = limitationsMap.get(layer);
        if (limitations == null) {
            limitations = new PropertyLimitations();
            limitationsMap.put(layer, limitations);
        }
        return limitations;
    }

    static boolean isIgnored(ResourceAttributeDefinitionType attrDefType) throws SchemaException {
        List<PropertyLimitationsType> limitations = attrDefType.getLimitations();
        if (limitations == null) {
            return false;
        }
        PropertyLimitationsType limitationsType = MiscSchemaUtil.getLimitationsType(limitations, DEFAULT_LAYER);
        if (limitationsType == null) {
            return false;
        }
        if (limitationsType.getProcessing() != null) {
            return limitationsType.getProcessing() == ItemProcessingType.IGNORE;
        }
        if (limitationsType.isIgnore() != null) {
            return limitationsType.isIgnore();
        }
        return false;
    }

    @NotNull
    @Override
    public RefinedAttributeDefinitionImpl<T> clone() {
        ResourceAttributeDefinition<T> attrDefClone = this.attributeDefinition.clone();
        RefinedAttributeDefinitionImpl<T> clone = new RefinedAttributeDefinitionImpl<>(attrDefClone, getPrismContext());
        copyDefinitionData(clone);
        return clone;
    }

    protected void copyDefinitionData(RefinedAttributeDefinitionImpl<T> clone) {
        super.copyDefinitionData(clone);
        clone.accessOverride = this.accessOverride.clone();
        clone.description = this.description;
        clone.displayName = this.displayName;
        clone.fetchStrategy = this.fetchStrategy;
        clone.storageStrategy = this.storageStrategy;
        clone.inboundMappingTypes = this.inboundMappingTypes;
        clone.intolerantValuePattern = this.intolerantValuePattern;
        clone.isExclusiveStrong = this.isExclusiveStrong;
        clone.isVolatilityTrigger = this.isVolatilityTrigger;
        clone.limitationsMap = this.limitationsMap;
        clone.matchingRuleQName = this.matchingRuleQName;
        clone.modificationPriority = this.modificationPriority;
        clone.outboundMappingType = this.outboundMappingType;
        clone.readReplaceMode = this.readReplaceMode;
        clone.secondaryIdentifierOverride = this.secondaryIdentifierOverride;
        clone.tolerant = this.tolerant;
        clone.tolerantValuePattern = this.tolerantValuePattern;
    }

    @Override
    public RefinedAttributeDefinition<T> deepClone(Map<QName, ComplexTypeDefinition> ctdMap, Map<QName, ComplexTypeDefinition> onThisPath, Consumer<ItemDefinition> postCloneAction) {
        return (RefinedAttributeDefinition<T>) super.deepClone(ctdMap, onThisPath, postCloneAction);
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
        if (getOutboundMappingType() != null) {
            sb.append(",OUT");
        }
        if (getInboundMappingTypes() != null) {
            sb.append(",IN");
        }
        if (Boolean.TRUE.equals(getReadReplaceMode())) {
            sb.append(",R+E");
        }
        if (getModificationPriority() != null) {
            sb.append(",P").append(getModificationPriority());
        }
        return sb.toString();
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    public String getDebugDumpClassName() {
        return "rRAD";
    }

    @Override
    public String debugDump(int indent) {
        return debugDump(indent, (LayerType) null);
    }

    @Override
    public String debugDump(int indent, LayerType layer) {
        StringBuilder sb = new StringBuilder();
        sb.append(super.debugDump(indent));
        if (layer == null) {
            sb.append("\n");
            DebugUtil.debugDumpMapSingleLine(sb, limitationsMap, indent + 1);
        } else {
            PropertyLimitations limitations = limitationsMap.get(layer);
            if (limitations != null) {
                sb.append(limitations.toString());
            }
        }
        return sb.toString();
    }

    public void setModificationPriority(Integer modificationPriority) {
        this.modificationPriority = modificationPriority;
    }

    @Override
    public Integer getModificationPriority() {
        return modificationPriority;
    }

    @Override
    public Boolean getReadReplaceMode() {           // "get" instead of "is" because it may be null
        return readReplaceMode;
    }

    public void setReadReplaceMode(Boolean readReplaceMode) {
        this.readReplaceMode = readReplaceMode;
    }

    @Override
    public boolean isDisplayNameAttribute() {
        return isDisplayNameAttribute;
    }

    @Override
    public Optional<ComplexTypeDefinition> structuredType() {
        if(structuredType == null) {
            this.structuredType = Optional.ofNullable(prismContext.getSchemaRegistry().findComplexTypeDefinitionByType(typeName));
        }
        return structuredType;
    }
}
