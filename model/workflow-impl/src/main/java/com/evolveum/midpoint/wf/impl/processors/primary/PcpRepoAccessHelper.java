/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.processors.primary;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.WfConfiguration;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processes.common.StringHolder;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ObjectDeltaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.bind.JAXBException;

import java.util.Map;

/**
 * @author mederly
 */

@Component
public class PcpRepoAccessHelper {

    private static final transient Trace LOGGER = TraceManager.getTrace(PcpRepoAccessHelper.class);

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired
    private PrismContext prismContext;

    @Autowired
    private TaskManager taskManager;

    @Autowired
    private WfConfiguration wfConfiguration;

    @Autowired
    private MiscDataUtil miscDataUtil;

    public PrismObject<? extends ObjectType> getObjectBefore(Map<String, Object> variables, PrismContext prismContext, OperationResult result) throws SchemaException, ObjectNotFoundException, JAXBException {
        String objectXml = (String) variables.get(PcpProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_TO_BE_ADDED);
        PrismObject<? extends ObjectType> object;
        if (objectXml != null) {
            object = prismContext.parseObject(objectXml, PrismContext.LANG_XML);
        } else {
            String oid = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_OID);
            if (oid == null) {
                return null;
            }
            //Validate.notNull(oid, "Object OID in process variables is null");
            object = repositoryService.getObject(ObjectType.class, oid, null, result);
        }

        if (object.asObjectable() instanceof UserType) {
            miscDataUtil.resolveAssignmentTargetReferences((PrismObject) object, result);
        }
        return object;
    }

    public ObjectDelta getObjectDelta(Map<String, Object> variables) throws JAXBException, SchemaException {
        return getObjectDelta(variables, false);
    }

    public ObjectDelta getObjectDelta(Map<String, Object> variables, boolean mayBeNull) throws JAXBException, SchemaException {
        ObjectDeltaType objectDeltaType = getObjectDeltaType(variables, mayBeNull);
        return objectDeltaType != null ? DeltaConvertor.createObjectDelta(objectDeltaType, prismContext) : null;
    }

    public ObjectDeltaType getObjectDeltaType(Map<String, Object> variables, boolean mayBeNull) throws JAXBException, SchemaException {
        StringHolder deltaXml = (StringHolder) variables.get(PcpProcessVariableNames.VARIABLE_MIDPOINT_DELTA);
        if (deltaXml == null) {
            if (mayBeNull) {
                return null;
            } else {
                throw new IllegalStateException("There's no delta in process variables");
            }
        }
        return prismContext.parseAtomicValue(deltaXml.getValue(), ObjectDeltaType.COMPLEX_TYPE, PrismContext.LANG_XML);
    }

    public PrismObject<? extends ObjectType> getObjectAfter(Map<String, Object> variables, ObjectDeltaType deltaType, PrismObject<? extends ObjectType> objectBefore, PrismContext prismContext, OperationResult result) throws JAXBException, SchemaException {

        ObjectDelta delta;
        if (deltaType != null) {
            delta = DeltaConvertor.createObjectDelta(deltaType, prismContext);
        } else {
            delta = getObjectDelta(variables, true);
        }

        if (delta == null) {
            return null;
        }

        PrismObject<? extends ObjectType> objectAfter;
        if (delta.isAdd()) {
            if (delta.getObjectToAdd() != null) {
                objectAfter = delta.getObjectToAdd().clone();
            } else {
                return null;
            }
        } else if (delta.isModify()) {
            objectAfter = objectBefore.clone();
            delta.applyTo(objectAfter);
        } else if (delta.isDelete()) {
            return null;
        } else {
            return null;        // should not occur
        }

        if (objectAfter.asObjectable() instanceof UserType) {   // quite a hack
            miscDataUtil.resolveAssignmentTargetReferences((PrismObject) objectAfter, result);
        }
        return objectAfter;
    }

}
