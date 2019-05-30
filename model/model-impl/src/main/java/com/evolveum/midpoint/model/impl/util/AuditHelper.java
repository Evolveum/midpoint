/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.model.impl.util;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectDeltaSchemaLevelUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static com.evolveum.midpoint.schema.util.ObjectDeltaSchemaLevelUtil.resolveNames;

/**
 *  Uses cache repository service to resolve object names.
 */
@Component
public class AuditHelper {

	@Autowired private AuditService auditService;
	@Autowired private PrismContext prismContext;
	@Autowired
	@Qualifier("cacheRepositoryService")
	private RepositoryService repositoryService;

	public void audit(AuditEventRecord record, Task task) {
		if (record.getDeltas() != null) {
			for (ObjectDeltaOperation<? extends ObjectType> objectDeltaOperation : record.getDeltas()) {
				ObjectDelta<? extends ObjectType> delta = objectDeltaOperation.getObjectDelta();

				// we use null options here, in order to utilize the local or global repository cache
				ObjectDeltaSchemaLevelUtil.NameResolver nameResolver = (objectClass, oid) -> {
					PrismObject<? extends ObjectType> object = repositoryService.getObject(objectClass, oid, null,
							new OperationResult(AuditHelper.class.getName() + ".resolveName"));
					return object.getName();
				};
				resolveNames(delta, nameResolver, prismContext);
			}
		}
		auditService.audit(record, task);
	}
}
