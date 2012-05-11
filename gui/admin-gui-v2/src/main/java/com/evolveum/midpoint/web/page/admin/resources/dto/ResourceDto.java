/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.resources.dto;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.web.component.util.Selectable;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * @author lazyman
 */
public class ResourceDto extends Selectable {

    private String oid;
    private String name;
    private String bundle;
    private String version;
    private String progress;
    private String type;
    private ResourceState state;
    private List<ResourceObjectTypeDto> objectTypes;
    private List<String> capabilities;
    private ResourceSync sync;

    public ResourceDto() {
    }
    
    public ResourceDto(ResourceType resource, ConnectorType connector) {
        Validate.notNull(resource);

        oid = resource.getOid();
        name = resource.getName();
        bundle = connector != null ? connector.getConnectorBundle() : null;
        version = connector != null ? connector.getConnectorVersion() : null;
        type = connector != null ? connector.getConnectorType() : null;
    }

    public ResourceDto(ResourceType resource, ConnectorType connector, List<String> capabilities) {
        Validate.notNull(resource);

        oid = resource.getOid();
        name = resource.getName();
        bundle = connector != null ? connector.getConnectorBundle() : null;
        version = connector != null ? connector.getConnectorVersion() : null;
        type = connector != null ? connector.getConnectorType() : null;
        this.capabilities = capabilities;
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
    
    public ResourceStatus getOverallStatus() {
		if (state == null) {
			return ResourceStatus.NOT_TESTED;
		}
		return state.getOverall();
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
}
