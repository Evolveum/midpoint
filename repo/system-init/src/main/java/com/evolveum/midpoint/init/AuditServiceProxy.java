/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.init;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditResultHandler;
import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.audit.spi.AuditServiceRegistry;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectDeltaOperation;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.HttpConnectionInformation;
import com.evolveum.midpoint.security.api.SecurityEnforcer;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CleanupPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * @author lazyman
 */
public class AuditServiceProxy implements AuditService, AuditServiceRegistry {

	private static final Trace LOGGER = TraceManager.getTrace(AuditServiceProxy.class);

	@Autowired
	private LightweightIdentifierGenerator lightweightIdentifierGenerator;

	@Nullable
	@Autowired(required = false) // missing in some tests
	private RepositoryService repositoryService;

	@Nullable
	@Autowired(required = false) // missing in some tests (maybe)
	private TaskManager taskManager;

	@Nullable
	@Autowired(required = false) // missing in some tests (maybe)
	private SecurityEnforcer securityEnforcer;

	@Autowired
	private PrismContext prismContext;

	private List<AuditService> services = new Vector<>();

	@Override
	public void audit(AuditEventRecord record, Task task) {

		if (services.isEmpty()) {
			LOGGER.warn("Audit event will not be recorded. No audit services registered.");
			return;
		}

		assertCorrectness(record, task);
		completeRecord(record, task);

		for (AuditService service : services) {
			service.audit(record, task);
		}
	}

	@Override
	public void cleanupAudit(CleanupPolicyType policy, OperationResult parentResult) {
		Validate.notNull(policy, "Cleanup policy must not be null.");
		Validate.notNull(parentResult, "Operation result must not be null.");

		for (AuditService service : services) {
			service.cleanupAudit(policy, parentResult);
		}
	}

	@Override
	public void registerService(AuditService service) {
		Validate.notNull(service, "Audit service must not be null.");
		if (services.contains(service)) {
			return;
		}

		services.add(service);
	}

	@Override
	public void unregisterService(AuditService service) {
		Validate.notNull(service, "Audit service must not be null.");
		services.remove(service);
	}

	private void assertCorrectness(AuditEventRecord record, Task task) {
		if (task == null) {
			LOGGER.warn("Task is null in a call to audit service");
		}
	}

	/**
	 * Complete the record with data that can be computed or discovered from the
	 * environment
	 */
	private void completeRecord(AuditEventRecord record, Task task) {
		LightweightIdentifier id = null;
		if (record.getEventIdentifier() == null) {
			id = lightweightIdentifierGenerator.generate();
			record.setEventIdentifier(id.toString());
		}
		if (record.getTimestamp() == null) {
			if (id == null) {
				record.setTimestamp(System.currentTimeMillis());
			} else {
				// To be consistent with the ID
				record.setTimestamp(id.getTimestamp());
			}
		}
		if (record.getTaskIdentifier() == null && task != null) {
			record.setTaskIdentifier(task.getTaskIdentifier());
		}
		if (record.getTaskOID() == null && task != null) {
			record.setTaskOID(task.getOid());
		}
		if (record.getChannel() == null && task != null) {
			record.setChannel(task.getChannel());
		}
		if (record.getInitiator() == null && task != null) {
			record.setInitiator(task.getOwner());
		}

		if (record.getNodeIdentifier() == null && taskManager != null) {
			record.setNodeIdentifier(taskManager.getNodeId());
		}

		if (securityEnforcer != null) {
			HttpConnectionInformation connInfo = SecurityUtil.getCurrentConnectionInformation();
			if (connInfo == null) {
				connInfo = securityEnforcer.getStoredConnectionInformation();
			}
			if (connInfo != null) {
				if (record.getSessionIdentifier() == null) {
					record.setSessionIdentifier(connInfo.getSessionId());
				}
				if (record.getRemoteHostAddress() == null) {
					record.setRemoteHostAddress(connInfo.getRemoteHostAddress());
				}
				if (record.getHostIdentifier() == null) {
					record.setHostIdentifier(connInfo.getLocalHostName());
				}
			}
		}

		if (record.getSessionIdentifier() == null && task != null) {
			record.setSessionIdentifier(task.getTaskIdentifier());
		}

		if (record.getDeltas() != null) {
			for (ObjectDeltaOperation<? extends ObjectType> objectDeltaOperation : record.getDeltas()) {
				ObjectDelta<? extends ObjectType> delta = objectDeltaOperation.getObjectDelta();
				final Map<String, PolyString> resolvedOids = new HashMap<>();
				Visitor namesResolver = new Visitor() {
					@Override
					public void visit(Visitable visitable) {
						if (visitable instanceof PrismReferenceValue) {
							PrismReferenceValue refVal = ((PrismReferenceValue) visitable);
							String oid = refVal.getOid();
							if (oid == null) { // sanity check; should not
												// happen
								return;
							}
							if (refVal.getTargetName() != null) {
								resolvedOids.put(oid, refVal.getTargetName());
								return;
							}
							if (resolvedOids.containsKey(oid)) {
								PolyString resolvedName = resolvedOids.get(oid); // may
																					// be
																					// null
								refVal.setTargetName(resolvedName);
								return;
							}
							if (refVal.getObject() != null) {
								PolyString name = refVal.getObject().getName();
								refVal.setTargetName(name);
								resolvedOids.put(oid, name);
								return;
							}
							if (repositoryService == null) {
								LOGGER.warn("No repository, no OID resolution (for {})", oid);
								return;
							}
							PrismObjectDefinition<? extends ObjectType> objectDefinition = null;
							if (refVal.getTargetType() != null) {
								objectDefinition = prismContext.getSchemaRegistry()
										.findObjectDefinitionByType(refVal.getTargetType());
							}
							Class<? extends ObjectType> objectClass = null;
							if (objectDefinition != null) {
								objectClass = objectDefinition.getCompileTimeClass();
							}
							if (objectClass == null) {
								objectClass = ObjectType.class; // the default
																// (shouldn't be
																// needed)
							}
							SelectorOptions<GetOperationOptions> getNameOnly = SelectorOptions.create(
									new ItemPath(ObjectType.F_NAME),
									GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));
							try {
								PrismObject<? extends ObjectType> object = repositoryService.getObject(
										objectClass, oid, Arrays.asList(getNameOnly),
										new OperationResult("dummy"));
								PolyString name = object.getName();
								refVal.setTargetName(name);
								resolvedOids.put(oid, name);
								LOGGER.trace("Resolved {}: {} to {}", objectClass, oid, name);
							} catch (ObjectNotFoundException e) {
								LOGGER.trace("Couldn't determine the name for {}: {} as it does not exist",
										objectClass, oid, e);
								resolvedOids.put(oid, null);
							} catch (SchemaException | RuntimeException e) {
								LOGGER.trace(
										"Couldn't determine the name for {}: {} because of unexpected exception",
										objectClass, oid, e);
								resolvedOids.put(oid, null);
							}
						}
					}
				};
				delta.accept(namesResolver);
			}
		}
	}

	@Override
	public List<AuditEventRecord> listRecords(String query, Map<String, Object> params) {
		List<AuditEventRecord> result = new ArrayList<AuditEventRecord>();
		for (AuditService service : services) {
			if (service.supportsRetrieval()) {
				List<AuditEventRecord> records = service.listRecords(query, params);
				if (records != null && !records.isEmpty()) {
					result.addAll(records);
				}
			}
		}
		return result;
	}

	@Override
	public void listRecordsIterative(String query, Map<String, Object> params, AuditResultHandler handler) {
		for (AuditService service : services) {
			if (service.supportsRetrieval()) {
				service.listRecordsIterative(query, params, handler);
			}
		}
	}

	@Override
	public void reindexEntry(AuditEventRecord record) {
		for (AuditService service : services) {
			if (service.supportsRetrieval()) {
				service.reindexEntry(record);
			}
		}
	}

	@Override
	public long countObjects(String query, Map<String, Object> params) {
		long count = 0;
		for (AuditService service : services) {
			if (service.supportsRetrieval()) {
				long c = service.countObjects(query, params);
				count += c;
			}
		}
		return count;
	}

	@Override
	public boolean supportsRetrieval() {
		for (AuditService service : services) {
			if (service.supportsRetrieval()) {
				return true;
			}
		}
		return false;
	}
}
