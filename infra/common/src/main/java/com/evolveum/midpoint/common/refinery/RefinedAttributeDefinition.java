/*
 * Copyright (c) 2010-2015 Evolveum
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
package com.evolveum.midpoint.common.refinery;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.lang.BooleanUtils;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.SchemaProcessorUtil;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AttributeFetchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyAccessType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PropertyLimitationsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;

/**
 * @author semancik
 */
public class RefinedAttributeDefinition<T> extends ResourceAttributeDefinition<T> implements DebugDumpable {

	private static LayerType DEFAULT_LAYER = LayerType.MODEL;
	
    private String displayName;
    private String description;
    private boolean tolerant = true;
    private boolean isExclusiveStrong = false;
	protected boolean secondaryIdentifier = false;
    private List<String> intolerantValuePattern;
    private List<String> tolerantValuePattern;
    private ResourceAttributeDefinition<T> attributeDefinition;
    private AttributeFetchStrategyType fetchStrategy;
    private MappingType outboundMappingType;
    private List<MappingType> inboundMappingTypes;
    private Map<LayerType,PropertyLimitations> limitationsMap = new HashMap<LayerType, PropertyLimitations>();
    private QName matchingRuleQName = null;
    private Integer modificationPriority;
    private Boolean readReplaceMode;

    protected RefinedAttributeDefinition(ResourceAttributeDefinition<T> attrDef, PrismContext prismContext) {
        super(attrDef.getName(), attrDef.getTypeName(), prismContext);
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
	
	public boolean isSecondaryIdentifier() {
		return secondaryIdentifier;
	}
	
	public void setSecondaryIdentifier(boolean secondaryIdentifier) {
		this.secondaryIdentifier = secondaryIdentifier;
	}
	
	@Override
    public boolean canAdd() {
		return canAdd(DEFAULT_LAYER);
    }
	
	public boolean canAdd(LayerType layer) {
        return limitationsMap.get(layer).getAccess().isAdd();
    }

	@Override
    public boolean canRead() {
		return canRead(DEFAULT_LAYER);
    }

    public boolean canRead(LayerType layer) {
        return limitationsMap.get(layer).getAccess().isRead();
    }

    @Override
    public boolean canModify() {
    	return canModify(DEFAULT_LAYER);
    }
    
    public boolean canModify(LayerType layer) {
        return limitationsMap.get(layer).getAccess().isModify();
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
    	throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public void setMaxOccurs(int maxOccurs) {
    	throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public void setCanRead(boolean read) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public void setCanModify(boolean update) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public void setCanAdd(boolean create) {
        throw new UnsupportedOperationException("Parts of refined attribute are immutable");
    }

    @Override
    public boolean isIgnored() {
        return isIgnored(DEFAULT_LAYER);
    }
    
    public boolean isIgnored(LayerType layer) {
        return limitationsMap.get(layer).isIgnore();
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

    public ResourceAttributeDefinition<T> getAttributeDefinition() {
        return attributeDefinition;
    }

    public void setAttributeDefinition(ResourceAttributeDefinition<T> attributeDefinition) {
        this.attributeDefinition = attributeDefinition;
    }

    public MappingType getOutboundMappingType() {
        return outboundMappingType;
    }

    public void setOutboundMappingType(MappingType outboundMappingType) {
        this.outboundMappingType = outboundMappingType;
    }
    
    public boolean hasOutboundMapping() {
    	return outboundMappingType != null;
    }

    public List<MappingType> getInboundMappingTypes() {
        return inboundMappingTypes;
    }

    public void setInboundMappingTypes(List<MappingType> inboundAssignmentTypes) {
        this.inboundMappingTypes = inboundAssignmentTypes;
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

    public Collection<? extends DisplayableValue<T>> getAllowedValues() {
        return attributeDefinition.getAllowedValues();
    }
    
    public boolean isReturnedByDefault() {
		return attributeDefinition.isReturnedByDefault();
	}

	public void setReturnedByDefault(Boolean returnedByDefault) {
		throw new UnsupportedOperationException("Cannot change returnedByDefault");
	}

	@Override
    public int getMaxOccurs() {
    	return getMaxOccurs(DEFAULT_LAYER);
    }
    
    public int getMaxOccurs(LayerType layer) {
    	return limitationsMap.get(layer).getMaxOccurs();
    }

    @Override
    public int getMinOccurs() {
    	return getMinOccurs(DEFAULT_LAYER);
    }

    public int getMinOccurs(LayerType layer) {
    	return limitationsMap.get(layer).getMinOccurs();
    }
    
    public boolean isOptional(LayerType layer) {
    	return limitationsMap.get(layer).getMinOccurs() == 0;
    }

    public boolean isMandatory(LayerType layer) {
    	return limitationsMap.get(layer).getMinOccurs() > 0;
    }
    
    public boolean isMultiValue(LayerType layer) {
    	int maxOccurs = limitationsMap.get(layer).getMaxOccurs();
    	return maxOccurs < 0 || maxOccurs > 1;
    }
    
    public boolean isSingleValue(LayerType layer) {
    	return limitationsMap.get(layer).getMaxOccurs() == 1;
    }

	public boolean isExlusiveStrong() {
		return isExclusiveStrong;
	}

    public void setExclusiveStrong(boolean isExclusiveStrong) {
		this.isExclusiveStrong = isExclusiveStrong;
	}

	public PropertyLimitations getLimitations(LayerType layer) {
    	return limitationsMap.get(layer);
    }

    public String getHelp() {
        return attributeDefinition.getHelp();
    }

    public AttributeFetchStrategyType getFetchStrategy() {
		return fetchStrategy;
	}

	public void setFetchStrategy(AttributeFetchStrategyType fetchStrategy) {
		this.fetchStrategy = fetchStrategy;
	}

	public QName getMatchingRuleQName() {
		return matchingRuleQName;
	}

	public void setMatchingRuleQName(QName matchingRuleQName) {
		this.matchingRuleQName = matchingRuleQName;
	}
	
	
	public List<String> getTolerantValuePattern(){
		return tolerantValuePattern;
	}
	
	public List<String> getIntolerantValuePattern(){
		return intolerantValuePattern;
		
	}

    // schemaHandlingAttrDefType may be null if we are parsing from schema only
    static <T> RefinedAttributeDefinition<T> parse(ResourceAttributeDefinition<T> schemaAttrDef, ResourceAttributeDefinitionType schemaHandlingAttrDefType,
                                            ObjectClassComplexTypeDefinition objectClassDef, PrismContext prismContext,
                                            String contextDescription) throws SchemaException {

        RefinedAttributeDefinition<T> rAttrDef = new RefinedAttributeDefinition<T>(schemaAttrDef, prismContext);

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

        if (schemaHandlingAttrDefType != null) {
            rAttrDef.fetchStrategy = schemaHandlingAttrDefType.getFetchStrategy();
            rAttrDef.matchingRuleQName = schemaHandlingAttrDefType.getMatchingRule();
        }

        PropertyLimitations schemaLimitations = getOrCreateLimitations(rAttrDef.limitationsMap, LayerType.SCHEMA);
        schemaLimitations.setMinOccurs(schemaAttrDef.getMinOccurs());
        schemaLimitations.setMaxOccurs(schemaAttrDef.getMaxOccurs());
        schemaLimitations.setIgnore(schemaAttrDef.isIgnored());
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

            if (schemaHandlingAttrDefType.isSecondaryIdentifier() == null) {
                rAttrDef.secondaryIdentifier = false;
            } else {
                rAttrDef.secondaryIdentifier = schemaHandlingAttrDefType.isSecondaryIdentifier();
            }

            rAttrDef.tolerantValuePattern = schemaHandlingAttrDefType.getTolerantValuePattern();
            rAttrDef.intolerantValuePattern = schemaHandlingAttrDefType.getIntolerantValuePattern();

            rAttrDef.isExclusiveStrong = BooleanUtils.isTrue(schemaHandlingAttrDefType.isExclusiveStrong());

            if (schemaHandlingAttrDefType.getOutbound() != null) {
                rAttrDef.setOutboundMappingType(schemaHandlingAttrDefType.getOutbound());
            }

            if (schemaHandlingAttrDefType.getInbound() != null) {
                rAttrDef.setInboundMappingTypes(schemaHandlingAttrDefType.getInbound());
            }

            rAttrDef.setModificationPriority(schemaHandlingAttrDefType.getModificationPriority());

            rAttrDef.setReadReplaceMode(schemaHandlingAttrDefType.isReadReplaceMode());            // may be null at this point
        }

        PropertyLimitations previousLimitations = null;
        for (LayerType layer : LayerType.values()) {
            PropertyLimitations limitations = getOrCreateLimitations(rAttrDef.limitationsMap, layer);
            if (previousLimitations != null) {
                limitations.setMinOccurs(previousLimitations.getMinOccurs());
                limitations.setMaxOccurs(previousLimitations.getMaxOccurs());
                limitations.setIgnore(previousLimitations.isIgnore());
                limitations.getAccess().setAdd(previousLimitations.getAccess().isAdd());
                limitations.getAccess().setRead(previousLimitations.getAccess().isRead());
                limitations.getAccess().setModify(previousLimitations.getAccess().isModify());
            }
            previousLimitations = limitations;
            if (schemaHandlingAttrDefType != null) {
                if (layer != LayerType.SCHEMA) {
                    // SCHEMA is a pseudo-layer. It cannot be overriden ... unless specified explicitly
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
			limitations.setMinOccurs(SchemaProcessorUtil.parseMultiplicity(layerLimitationsType.getMinOccurs()));
		}
		if (layerLimitationsType.getMaxOccurs() != null) {
			limitations.setMaxOccurs(SchemaProcessorUtil.parseMultiplicity(layerLimitationsType.getMaxOccurs()));
		}
		if (layerLimitationsType.isIgnore() != null) {
			limitations.setIgnore(layerLimitationsType.isIgnore());
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
		if (limitationsType.isIgnore() == null) {
			return false;
		}
        return limitationsType.isIgnore();
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
    protected String getDebugDumpClassName() {
        return "rRAD";
    }

    @Override
	public String debugDump(int indent) {
    	return debugDump(indent, null);
    }
    
	String debugDump(int indent, LayerType layer) {
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

    public Integer getModificationPriority() {
        return modificationPriority;
    }

    public Boolean getReadReplaceMode() {           // "get" instead of "is" because it may be null
        return readReplaceMode;
    }

    public void setReadReplaceMode(Boolean readReplaceMode) {
        this.readReplaceMode = readReplaceMode;
    }
}
