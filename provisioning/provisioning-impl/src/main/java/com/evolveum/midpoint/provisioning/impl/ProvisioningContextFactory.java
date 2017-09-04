/**
 * Copyright (c) 2015-2017 Evolveum
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
package com.evolveum.midpoint.provisioning.impl;

import java.util.Collection;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
@Component
public class ProvisioningContextFactory {

	@Autowired(required = true)
	private ResourceManager resourceManager;

	public ProvisioningContext create(PrismObject<ShadowType> shadow, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		ProvisioningContext ctx = new ProvisioningContext(resourceManager, parentResult);
		ctx.setTask(task);
		ctx.setOriginalShadow(shadow);
		String resourceOid = ShadowUtil.getResourceOid(shadow.asObjectable());
		ctx.setResourceOid(resourceOid);
		return ctx;
	}

	public ProvisioningContext create(PrismObject<ShadowType> shadow, Collection<QName> additionalAuxiliaryObjectClassQNames, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		ProvisioningContext ctx = new ProvisioningContext(resourceManager, parentResult);
		ctx.setTask(task);
		ctx.setOriginalShadow(shadow);
		ctx.setAdditionalAuxiliaryObjectClassQNames(additionalAuxiliaryObjectClassQNames);
		String resourceOid = ShadowUtil.getResourceOid(shadow.asObjectable());
		ctx.setResourceOid(resourceOid);
		return ctx;
	}

	public ProvisioningContext create(ResourceShadowDiscriminator coords, Task task, OperationResult parentResult) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		ProvisioningContext ctx = new ProvisioningContext(resourceManager, parentResult);
		ctx.setTask(task);
		ctx.setShadowCoordinates(coords);
		String resourceOid = coords.getResourceOid();
		ctx.setResourceOid(resourceOid);
		return ctx;
	}

}
