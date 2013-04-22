package com.evolveum.midpoint.wf.dao;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.types_2.ObjectDeltaType;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBException;
import java.util.Map;

/**
 * @author mederly
 */

@Component
public class MiscDataUtil {

    private static final transient Trace LOGGER = TraceManager.getTrace(MiscDataUtil.class);

    @Autowired
    private RepositoryService repositoryService;

    // returns oid when user cannot be retrieved
    String getUserNameByOid(String oid, OperationResult result) {
        try {
            PrismObject<UserType> user = repositoryService.getObject(UserType.class, oid, result);
            return user.asObjectable().getName().getOrig();
        } catch (ObjectNotFoundException e) {
            // there should be a note in result by now
            LoggingUtils.logException(LOGGER, "Couldn't get user {} details because it couldn't be found", e, oid);
            return oid;
        } catch (SchemaException e) {
            // there should be a note in result by now
            LoggingUtils.logException(LOGGER, "Couldn't get user {} details due to schema exception", e, oid);
            return oid;
        }
    }

    public PrismObject<UserType> getRequester(Map<String, Object> variables, OperationResult result) throws SchemaException, ObjectNotFoundException {
        String oid = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_REQUESTER_OID);
        return repositoryService.getObject(UserType.class, oid, result);
    }

    public PrismObject<? extends ObjectType> getObjectBefore(Map<String, Object> variables, PrismContext prismContext, OperationResult result) throws SchemaException, ObjectNotFoundException, JAXBException {
        String objectXml = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_TO_BE_ADDED);
        PrismObject<? extends ObjectType> object;
        if (objectXml != null) {
            object = prismContext.getPrismJaxbProcessor().unmarshalObject(objectXml, ObjectType.class).asPrismObject();
        } else {
            String oid = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_OID);
            Validate.notNull(oid, "Object OID in process variables is null");
            object = repositoryService.getObject(ObjectType.class, oid, result);
        }

        if (object.asObjectable() instanceof UserType) {
            resolveAssignmentTargetReferences((PrismObject) object, result);
        }
        return object;
    }

    public PrismObject<? extends ObjectType> getObjectAfter(Map<String, Object> variables, PrismObject<? extends ObjectType> objectBefore, PrismContext prismContext, OperationResult result) throws JAXBException, SchemaException {
        String deltaXml = (String) variables.get(CommonProcessVariableNames.VARIABLE_MIDPOINT_DELTA);
        Validate.notNull(deltaXml, "There's no delta in process variables");
        ObjectDeltaType objectDeltaType = prismContext.getPrismJaxbProcessor().unmarshalObject(deltaXml, ObjectDeltaType.class);
        ObjectDelta delta = DeltaConvertor.createObjectDelta(objectDeltaType, prismContext);

        PrismObject<? extends ObjectType> objectAfter = objectBefore.clone();
        delta.applyTo(objectAfter);

        if (objectAfter.asObjectable() instanceof UserType) {
            resolveAssignmentTargetReferences((PrismObject) objectAfter, result);
        }
        return objectAfter;
    }

    public String serializeObjectToXml(PrismObject<? extends ObjectType> object) throws JAXBException {
        return object.getPrismContext().getPrismJaxbProcessor().marshalToString(object.asObjectable());
    }

    public void resolveAssignmentTargetReferences(PrismObject<? extends UserType> object, OperationResult result) {
        for (AssignmentType assignmentType : object.asObjectable().getAssignment()) {
            if (assignmentType.getTarget() == null) {
                PrismObject<? extends ObjectType> target = null;
                try {
                    target = repositoryService.getObject(ObjectType.class, assignmentType.getTargetRef().getOid(), result);
                    assignmentType.setTarget(target.asObjectable());
                } catch (ObjectNotFoundException e) {
                    LoggingUtils.logException(LOGGER, "Couldn't resolve assignment " + assignmentType, e);
                } catch (SchemaException e) {
                    LoggingUtils.logException(LOGGER, "Couldn't resolve assignment " + assignmentType, e);
                }
            }
        }
    }

}
