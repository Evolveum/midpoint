/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.certification.impl.handlers;

import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.impl.controller.ModelUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationRunType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationScopeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CertificationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

/**
 * @author mederly
 */
@Component
public class UserRoleBasicCertificationHandler extends BaseCertificationHandler {

    public static final String URI = "http://midpoint.evolveum.com/xml/ns/public/certification/certification-3#user-role-basic";        // TODO

    private static final transient Trace LOGGER = TraceManager.getTrace(UserRoleBasicCertificationHandler.class);

    @PostConstruct
    public void init() {
        certificationManager.registerHandler(URI, this);
    }

    @Override
    public void recordRunStarted(final AccessCertificationRunType runType, final Task task, OperationResult result) throws SchemaException, SecurityViolationException, ObjectNotFoundException, CommunicationException, ConfigurationException {
        LOGGER.trace("recordRunStarted executing");
        ObjectQuery query = new ObjectQuery();
        AccessCertificationScopeType scopeType = runType.getScope();
        if (scopeType != null) {
            if (scopeType.getObjectType() != null && !QNameUtil.match(UserType.COMPLEX_TYPE, scopeType.getObjectType())) {
                throw new IllegalStateException("Unsupported object type " + scopeType.getObjectType() + ". Currently only UserType is supported by this certification type.");
            }
            SearchFilterType searchFilterType = scopeType.getSearchFilter();
            if (searchFilterType != null) {
                ObjectFilter filter = QueryConvertor.parseFilter(searchFilterType, UserType.class, prismContext);
                query.setFilter(filter);
            }
        }

        final XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar(new Date());
        final ObjectReferenceType runRef = createRunRef(runType);

        ResultHandler<UserType> handler = new ResultHandler<UserType>() {
            @Override
            public boolean handle(PrismObject<UserType> user, OperationResult parentResult) {
                ObjectDelta<UserType> objectDelta = new ObjectDelta<>(UserType.class, ChangeType.MODIFY, prismContext);
                objectDelta.setOid(user.getOid());
                for (AssignmentType assignmentType : user.asObjectable().getAssignment()) {
                    if (assignmentType.getTargetRef() != null && QNameUtil.match(RoleType.COMPLEX_TYPE, assignmentType.getTargetRef().getType())) {
                        ItemPath path = new ItemPath(new NameItemPathSegment(UserType.F_ASSIGNMENT),
                                                     new IdItemPathSegment(assignmentType.getId()),
                                                     new NameItemPathSegment(AssignmentType.F_METADATA));

                        objectDelta.addModificationReplaceProperty(new ItemPath(path, MetadataType.F_CERTIFICATION_STARTED_TIMESTAMP), now);
                        objectDelta.addModificationReplaceReference(new ItemPath(path, MetadataType.F_CERTIFICATION_RUN_REF), runRef.asReferenceValue());
                        objectDelta.addModificationReplaceProperty(new ItemPath(path, MetadataType.F_CERTIFICATION_STATUS), CertificationStatusType.AWAITING_DECISION);
                        objectDelta.addModificationReplaceReference(new ItemPath(path, MetadataType.F_CERTIFIER_TO_DECIDE_REF),
                                getCertifierToDecideRef(runType, user, assignmentType, task, parentResult));
                    }
                }
                try {
                    modelService.executeChanges((Collection) Arrays.asList(objectDelta), null, task, parentResult);
                } catch (ObjectNotFoundException|ConfigurationException|SecurityViolationException|SchemaException|PolicyViolationException|ExpressionEvaluationException|ObjectAlreadyExistsException|CommunicationException|RuntimeException e) {
                    ModelUtils.recordPartialError(parentResult, "Couldn't record certification start for " + user, e);
                }
                return true;
            }
        };
        modelService.searchObjectsIterative(UserType.class, query, handler, null, task, result);
    }

    // can be overriden
    protected PrismReferenceValue getCertifierToDecideRef(AccessCertificationRunType runType, PrismObject<UserType> user, AssignmentType assignmentType, Task task, OperationResult parentResult) {
        // temporary solution - return certification run owner
        return runType.getOwnerRef().asReferenceValue().clone();
    }

}
