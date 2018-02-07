/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.page.admin.resources.dto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.web.component.data.column.InlineMenuable;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.util.Selectable;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class ResourceDto extends Selectable implements InlineMenuable {

	private static final String DOT_CLASS = ResourceDto.class.getName() + ".";
	private static final String OPERATION_LOAD_RESOURCE_DEFINITION = DOT_CLASS + "ResourceDto - load resource attribute container definition";

    private String oid;
    private String name;
    private String bundle;
    private String version;
    private String progress;
    private String type;
    private ResourceState state;
    private AvailabilityStatusType lastAvailabilityStatus;
    private List<ResourceObjectTypeDto> objectTypes;
    private List<String> capabilities;
    private ResourceSync sync;
    private ResourceImport resImport;
    private QName defaultAccountObjectClass;
    private List<InlineMenuItem> menuItems;

    public ResourceDto() {
    }

    public ResourceDto(PrismObject<ResourceType> resource) {
    	oid = resource.getOid();
        name = WebComponentUtil.getName(resource);

        PrismReference ref = resource.findReference(ResourceType.F_CONNECTOR_REF);
        ConnectorType connector = null;
        if (ref != null && ref.getValue().getObject() != null) {
            connector = (ConnectorType) ref.getValue().getObject().asObjectable();
        }
        bundle = connector != null ? connector.getConnectorBundle() : null;
        version = connector != null ? connector.getConnectorVersion() : null;
        type = connector != null ? connector.getConnectorType() : null;
        lastAvailabilityStatus = resource.asObjectable().getOperationalState() != null ? resource.asObjectable().getOperationalState().getLastAvailabilityStatus() : null;

        if(resource.asObjectable().getFetchResult() != null && resource.asObjectable().getFetchResult().getStatus() != null){
            if(OperationResultStatusType.PARTIAL_ERROR.equals(resource.asObjectable().getFetchResult().getStatus())){
                lastAvailabilityStatus = null;
            }
        }
    }

    public ResourceDto(PrismObject<ResourceType> resource, PrismContext prismContext, ConnectorType connector, List<String> capabilities) {
        Validate.notNull(resource);

        OperationResult result = new OperationResult(OPERATION_LOAD_RESOURCE_DEFINITION);

        oid = resource.getOid();
        name = WebComponentUtil.getName(resource);
        bundle = connector != null ? connector.getConnectorBundle() : null;
        version = connector != null ? connector.getConnectorVersion() : null;
        type = connector != null ? connector.getConnectorType() : null;
        this.capabilities = capabilities;

        try {
	        ResourceSchema resourceSchema = RefinedResourceSchemaImpl.getResourceSchema(resource, prismContext);
	        Collection<ObjectClassComplexTypeDefinition> definitions = resourceSchema.getObjectClassDefinitions();
	        for (ObjectClassComplexTypeDefinition definition : definitions) {
    			if (!(definition instanceof ObjectClassComplexTypeDefinition)) {
    				continue;
    			}
    			if(objectTypes == null){
    				objectTypes = new ArrayList<ResourceObjectTypeDto>();
    			}
    			objectTypes.add(new ResourceObjectTypeDto(definition));
    		}

            //default account object class qname
            ObjectClassComplexTypeDefinition def = resourceSchema.findDefaultObjectClassDefinition(ShadowKindType.ACCOUNT);
            defaultAccountObjectClass = def.getTypeName();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't load resource attribute container definition.", ex);
		}
    }

    public QName getDefaultAccountObjectClass() {
        return defaultAccountObjectClass;
    }

    public String getBundle() {
        return bundle;
    }

    public String getName() {
        return name;
    }

    public String getOid() {
        return oid;
    }

    public String getVersion() {
        return version;
    }

    public String getProgress() {
    	return progress;
    }

    public String getType() {
    	return type;
    }

    public List<String> getCapabilities() {
    	return capabilities;
    }

    public ResourceState getState() {
		if (state == null) {
			state = new ResourceState();
		}
		return state;
	}

    public OperationResultStatus getOverallStatus() {
		if (state == null) {
			return OperationResultStatus.UNKNOWN;
		}
		return state.getOverall();
	}

    public AvailabilityStatusType getLastAvailabilityStatus() {
    	return lastAvailabilityStatus;
	}

    public List<ResourceObjectTypeDto> getObjectTypes() {
		if (objectTypes == null) {
			objectTypes = new ArrayList<ResourceObjectTypeDto>();
		}
		return objectTypes;
	}

    public ResourceSyncStatus getSyncStatus() {
		if (sync == null || !sync.isEnabled()) {
			return ResourceSyncStatus.DISABLE;
		}
		return ResourceSyncStatus.ENABLE;
	}

    public ResourceImportStatus getResImport() {
		if (resImport == null || !resImport.isEnabled()) {
			return ResourceImportStatus.DISABLE;
		}
		return ResourceImportStatus.ENABLE;
	}

    @Override
    public List<InlineMenuItem> getMenuItems() {
        if (menuItems == null) {
            menuItems = new ArrayList<InlineMenuItem>();
        }
        return menuItems;
    }
}
