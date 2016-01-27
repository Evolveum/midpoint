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

import com.evolveum.midpoint.certification.impl.AccCertExpressionHelper;
import com.evolveum.midpoint.certification.impl.AccCertGeneralHelper;
import com.evolveum.midpoint.certification.impl.AccCertResponseComputationHelper;
import com.evolveum.midpoint.certification.impl.AccCertReviewersHelper;
import com.evolveum.midpoint.certification.impl.CertificationManagerImpl;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectResolver;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * @author mederly
 */
@Component
public abstract class BaseCertificationHandler implements CertificationHandler {

    private static final transient Trace LOGGER = TraceManager.getTrace(BaseCertificationHandler.class);

    @Autowired
    protected PrismContext prismContext;

    @Autowired
    protected ModelService modelService;

    @Autowired
    protected ObjectResolver objectResolver;

    @Autowired
    protected CertificationManagerImpl certificationManager;

    @Autowired
    protected AccCertGeneralHelper helper;

    @Autowired
    protected AccCertResponseComputationHelper computationHelper;

    @Autowired
    protected AccCertReviewersHelper reviewersHelper;

    @Autowired
    protected AccCertExpressionHelper expressionHelper;

    // default implementation, depending only on the expressions provided
    public Collection<? extends AccessCertificationCaseType> createCasesForObject(PrismObject<ObjectType> object, AccessCertificationCampaignType campaign, Task task, OperationResult parentResult) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        throw new UnsupportedOperationException("Not implemented yet.");
//        if (CollectionUtils.isEmpty(caseExpressionList)) {
//            throw new IllegalStateException("Unspecified case expression (and no default one provided) for campaign " + ObjectTypeUtil.toShortString(campaign));
//        }
//        return evaluateCaseExpressionList(caseExpressionList, object, task, parentResult);
    }

//    protected Collection<? extends AccessCertificationCaseType> evaluateCaseExpressionList(List<ExpressionType> caseExpressionList, PrismObject<ObjectType> object, Task task, OperationResult parentResult) {
//        List<AccessCertificationCaseType> caseList = new ArrayList<>();
//        for (ExpressionType caseExpression : caseExpressionList) {
//            caseList.addAll(evaluateCaseExpression(caseExpression, object, task, parentResult));
//        }
//        return caseList;
//    }

//    protected Collection<? extends AccessCertificationCaseType> evaluateCaseExpression(ExpressionType caseExpression, PrismObject<ObjectType> object, Task task, OperationResult parentResult) {
//        // todo
//        throw new UnsupportedOperationException("Not implemented yet.");
//    }


    public QName getDefaultObjectType() {
        return null;
    }
}
