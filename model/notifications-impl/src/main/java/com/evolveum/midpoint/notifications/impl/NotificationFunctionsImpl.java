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

package com.evolveum.midpoint.notifications.impl;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelElementContext;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.notifications.api.NotificationFunctions;
import com.evolveum.midpoint.notifications.api.OperationStatus;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.notifications.api.events.ResourceObjectEvent;
import com.evolveum.midpoint.notifications.api.events.SimpleObjectRef;
import com.evolveum.midpoint.notifications.impl.formatters.TextFormatter;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * @author mederly
 */
@Component
public class NotificationFunctionsImpl implements NotificationFunctions {

    private static final Trace LOGGER = TraceManager.getTrace(NotificationFunctionsImpl.class);

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService cacheRepositoryService;

	@Autowired
	private MidpointFunctions midpointFunctions;

	@Autowired
	protected TextFormatter textFormatter;

	@Autowired
	private PrismContext prismContext;

	private static final List<ItemPath> SYNCHRONIZATION_PATHS = Collections.unmodifiableList(Arrays.asList(
			new ItemPath(ShadowType.F_SYNCHRONIZATION_SITUATION),
			new ItemPath(ShadowType.F_SYNCHRONIZATION_SITUATION_DESCRIPTION),
			new ItemPath(ShadowType.F_SYNCHRONIZATION_TIMESTAMP),
			new ItemPath(ShadowType.F_FULL_SYNCHRONIZATION_TIMESTAMP)));

	private static final List<ItemPath> AUXILIARY_PATHS = Collections.unmodifiableList(Arrays.asList(
			new ItemPath(ShadowType.F_METADATA),
			new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_VALIDITY_STATUS),                // works for user activation as well
			new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_VALIDITY_CHANGE_TIMESTAMP),
			new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS),
			new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_DISABLE_TIMESTAMP),
			new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_ARCHIVE_TIMESTAMP),
			new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP),
			new ItemPath(ShadowType.F_ITERATION),
			new ItemPath(ShadowType.F_ITERATION_TOKEN),
			new ItemPath(UserType.F_LINK_REF),
			new ItemPath(ShadowType.F_TRIGGER))
	);


	// beware, may return null if there's any problem getting sysconfig (e.g. during initial import)
    public static SystemConfigurationType getSystemConfiguration(RepositoryService repositoryService, OperationResult result) {
    	return getSystemConfiguration(repositoryService, true, result);
	}

    public static SystemConfigurationType getSystemConfiguration(RepositoryService repositoryService, boolean errorIfNotFound, OperationResult result) {
        try {
            return repositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
            		null, result).asObjectable();
        } catch (ObjectNotFoundException|SchemaException e) {
        	if (errorIfNotFound) {
				LoggingUtils.logException(LOGGER,
						"Notification(s) couldn't be processed, because the system configuration couldn't be retrieved", e);
			} else {
        		LoggingUtils.logExceptionOnDebugLevel(LOGGER,
						"Notification(s) couldn't be processed, because the system configuration couldn't be retrieved", e);
			}
            return null;
        }
    }

    public SystemConfigurationType getSystemConfiguration(OperationResult result) {
    	return getSystemConfiguration(cacheRepositoryService, result);
    }

    public static SecurityPolicyType getSecurityPolicyConfiguration(ObjectReferenceType securityPolicyRef, RepositoryService repositoryService, OperationResult result) {
        try {
        	if (securityPolicyRef == null) {
        		return null;
        	}
            return repositoryService.getObject(SecurityPolicyType.class, securityPolicyRef.getOid(),
            		null, result).asObjectable();
        } catch (ObjectNotFoundException|SchemaException e) {
            LoggingUtils.logException(LOGGER, "Notification(s) couldn't be processed, because the security policy configuration couldn't be retrieved", e);
            return null;
        }
    }

    public static String getResourceNameFromRepo(RepositoryService repositoryService, String oid, OperationResult result) {
        try {
            PrismObject<ResourceType> resource = repositoryService.getObject(ResourceType.class, oid, null, result);
            return PolyString.getOrig(resource.asObjectable().getName());
        } catch (ObjectNotFoundException e) {
            LoggingUtils.logException(LOGGER, "Couldn't get resource", e);
            return null;
        } catch (SchemaException e) {
            LoggingUtils.logException(LOGGER, "Couldn't get resource", e);
            return null;
        }
    }

    public String getObjectName(String oid, OperationResult result) {
        try {
            PrismObject<? extends ObjectType> object = cacheRepositoryService.getObject(ObjectType.class, oid, null, result);
            return PolyString.getOrig(object.asObjectable().getName());
        } catch (CommonException|RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get resource", e);
            return null;
        }
    }

    public ObjectType getObjectType(SimpleObjectRef simpleObjectRef, boolean allowNotFound, OperationResult result) {
        if (simpleObjectRef == null) {
            return null;
        }
        if (simpleObjectRef.getObjectType() != null) {
            return simpleObjectRef.getObjectType();
        }
        if (simpleObjectRef.getOid() == null) {
            return null;
        }

		ObjectType objectType = getObjectFromRepo(simpleObjectRef.getOid(), allowNotFound, result);
        simpleObjectRef.setObjectType(objectType);
        return objectType;
    }

    public ObjectType getObjectType(ObjectReferenceType ref, boolean allowNotFound, OperationResult result) {
        if (ref == null) {
            return null;
        }
        if (ref.asReferenceValue().getObject() != null) {
            return (ObjectType) ref.asReferenceValue().getObject().asObjectable();
        }
        if (ref.getOid() == null) {
            return null;
        }

		return getObjectFromRepo(ref.getOid(), allowNotFound, result);
    }

	@Nullable
	private ObjectType getObjectFromRepo(String oid, boolean allowNotFound, OperationResult result) {
		ObjectType objectType;
		try {
			objectType = cacheRepositoryService.getObject(ObjectType.class, oid, null, result).asObjectable();
		} catch (ObjectNotFoundException e) {   // todo correct error handling
			if (allowNotFound) {
				return null;
			} else {
				throw new SystemException(e);
			}
		} catch (SchemaException e) {
			throw new SystemException(e);
		}
		return objectType;
	}

	public static boolean isAmongHiddenPaths(ItemPath path, List<ItemPath> hiddenPaths) {
        if (hiddenPaths == null) {
            return false;
        }
        for (ItemPath hiddenPath : hiddenPaths) {
            if (hiddenPath.isSubPathOrEquivalent(path)) {
                return true;
            }
        }
        return false;
    }

    @Override
	public String getShadowName(PrismObject<? extends ShadowType> shadow) {
        if (shadow == null) {
            return null;
        } else if (shadow.asObjectable().getName() != null) {
            return shadow.asObjectable().getName().getOrig();
        } else {
            Collection<ResourceAttribute<?>> secondaryIdentifiers = ShadowUtil.getSecondaryIdentifiers(shadow);
            LOGGER.trace("secondary identifiers: {}", secondaryIdentifiers);
            // first phase = looking for "name" identifier
            for (ResourceAttribute ra : secondaryIdentifiers) {
                if (ra.getElementName() != null && ra.getElementName().getLocalPart().contains("name")) {
                    LOGGER.trace("Considering {} as a name", ra);
                    return String.valueOf(ra.getAnyRealValue());
                }
            }
            // second phase = returning any value ;)
            if (!secondaryIdentifiers.isEmpty()) {
                return String.valueOf(secondaryIdentifiers.iterator().next().getAnyRealValue());
            } else {
                return null;
            }
        }
    }

    // TODO move to some other class?
    public void addRequesterAndChannelInformation(StringBuilder body, Event event, OperationResult result) {
        if (event.getRequester() != null) {
            body.append("Requester: ");
            try {
                ObjectType requester = event.getRequester().resolveObjectType(result, false);
                if (requester instanceof UserType) {
                    UserType requesterUser = (UserType) requester;
                    body.append(requesterUser.getFullName()).append(" (").append(requester.getName()).append(")");
                } else {
                    body.append(ObjectTypeUtil.toShortString(requester));
                }
            } catch (RuntimeException e) {
                body.append("couldn't be determined: ").append(e.getMessage());
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't determine requester for a notification", e);
            }
            body.append("\n");
        }
        body.append("Channel: ").append(event.getChannel()).append("\n\n");
    }

	@Override
	public String getPlaintextPasswordFromDelta(ObjectDelta delta) {
		try {
			return midpointFunctions.getPlaintextAccountPasswordFromDelta(delta);
		} catch (EncryptionException e) {
			LoggingUtils.logException(LOGGER, "Couldn't decrypt password from shadow delta: {}", e, delta.debugDump());
			return null;
		}
	}

	@Override
	public List<ItemPath> getSynchronizationPaths() {
		return SYNCHRONIZATION_PATHS;
	}

	@Override
	public List<ItemPath> getAuxiliaryPaths() {
		return AUXILIARY_PATHS;
	}

	public String getContentAsFormattedList(Event event, boolean showSynchronizationItems, boolean showAuxiliaryAttributes) {
		List<ItemPath> hiddenPaths = new ArrayList<>();
		if (!showSynchronizationItems) {
			hiddenPaths.addAll(SYNCHRONIZATION_PATHS);
		}
		if (!showAuxiliaryAttributes) {
			hiddenPaths.addAll(AUXILIARY_PATHS);
		}

		if (event instanceof ResourceObjectEvent) {
			final ResourceObjectEvent resourceObjectEvent = (ResourceObjectEvent) event;
			final ObjectDelta<ShadowType> shadowDelta = resourceObjectEvent.getShadowDelta();
			if (shadowDelta == null) {
				return "";
			}
			if (shadowDelta.isAdd()) {
				return getResourceObjectAttributesAsFormattedList(shadowDelta.getObjectToAdd().asObjectable(), hiddenPaths, showAuxiliaryAttributes);
			} else if (shadowDelta.isModify()) {
				return getResourceObjectModifiedAttributesAsFormattedList(resourceObjectEvent, shadowDelta, hiddenPaths, showAuxiliaryAttributes);
			} else {
				return "";
			}
		} else if (event instanceof ModelEvent) {
			final ModelEvent modelEvent = (ModelEvent) event;
			ModelContext<FocusType> modelContext = (ModelContext) modelEvent.getModelContext();
			ModelElementContext<FocusType> focusContext = modelContext.getFocusContext();
			ObjectDelta<? extends FocusType> summarizedDelta;
			try {
				summarizedDelta = modelEvent.getSummarizedFocusDeltas();
			} catch (SchemaException e) {
				LoggingUtils.logUnexpectedException(LOGGER, "Unable to determine the focus change; focus context = {}", e, focusContext.debugDump());
				return("(unable to determine the change because of schema exception: " + e.getMessage() + ")\n");
			}
			if (summarizedDelta.isAdd()) {
				return textFormatter.formatObject(summarizedDelta.getObjectToAdd(), hiddenPaths, showAuxiliaryAttributes);
			} else if (summarizedDelta.isModify()) {
				return textFormatter.formatObjectModificationDelta(summarizedDelta, hiddenPaths, showAuxiliaryAttributes, focusContext.getObjectOld(),
						focusContext.getObjectNew());
			} else {
				return "";
			}
		} else {
			return "";
		}
	}

	private String getResourceObjectAttributesAsFormattedList(ShadowType shadowType, List<ItemPath> hiddenAttributes, boolean showAuxiliaryAttributes) {
		return textFormatter.formatAccountAttributes(shadowType, hiddenAttributes, false);
	}

	private String getResourceObjectModifiedAttributesAsFormattedList(ResourceObjectEvent event, ObjectDelta<ShadowType> shadowDelta,
			List<ItemPath> hiddenPaths, boolean showAuxiliaryAttributes) {

		StringBuilder rv = new StringBuilder();
		if (event.getOperationStatus() != OperationStatus.IN_PROGRESS) {
			// todo we do not have objectOld + objectNew, only the current status
			// it is used to explain modified containers with identifiers -- however, currently I don't know of use of such containers in shadows, which would be visible in notifications
			rv.append(textFormatter.formatObjectModificationDelta(shadowDelta, hiddenPaths, showAuxiliaryAttributes,
					event.getAccountOperationDescription().getCurrentShadow(), null));
		} else {
			// special case - here the attributes are 'result', 'failedOperationType', 'objectChange', 'attemptNumber'
			// we have to unwrap attributes that are to be modified from the objectChange item
			Collection<PrismPropertyValue<ObjectDeltaType>> changes = null;
			if (shadowDelta.getModifications() != null) {
				for (ItemDelta itemDelta : shadowDelta.getModifications()) {
					if (itemDelta.getPath().equivalent(new ItemPath(ShadowType.F_OBJECT_CHANGE))) {
						changes = itemDelta.getValuesToAdd() != null && !itemDelta.getValuesToAdd().isEmpty() ?
								itemDelta.getValuesToAdd() : itemDelta.getValuesToReplace();
					}
				}
			}

			if (changes != null && !changes.isEmpty()) {
				try {
					List<ObjectDelta<ShadowType>> deltas = new ArrayList<>(changes.size());
					for (PrismPropertyValue<ObjectDeltaType> change : changes) {
						deltas.add((ObjectDelta) DeltaConvertor.createObjectDelta(change.getValue(), prismContext));
					}
					ObjectDelta<ShadowType> summarizedDelta = ObjectDelta.summarize(deltas);
					rv.append(textFormatter.formatObjectModificationDelta(summarizedDelta, hiddenPaths, showAuxiliaryAttributes,
							event.getAccountOperationDescription().getCurrentShadow(), null));
				} catch (SchemaException e) {
					LoggingUtils.logUnexpectedException(LOGGER, "Unable to determine the shadow change; operation = {}", e, event.getAccountOperationDescription().debugDump());
					rv.append("(unable to determine the change because of schema exception: ").append(e.getMessage()).append(")\n");
				}
			} else {
				rv.append("(unable to determine the change)\n");
			}
		}
		return rv.toString();
	}

}
