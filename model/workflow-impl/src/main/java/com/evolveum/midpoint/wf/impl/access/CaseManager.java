/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl.access;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.TreeNode;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.api.request.CancelCaseRequest;
import com.evolveum.midpoint.wf.impl.engine.WorkflowEngine;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;

@Component("wfCaseManager")
public class CaseManager {

    private static final Trace LOGGER = TraceManager.getTrace(CaseManager.class);

    @Autowired private PrismContext prismContext;
    @Autowired private WorkflowEngine workflowEngine;
    @Autowired private SecurityEnforcer securityEnforcer;
    @Autowired private Clock clock;
    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    private static final String DOT_INTERFACE = WorkflowManager.class.getName() + ".";

    private static final String OPERATION_CANCEL_CASE = DOT_INTERFACE + "cancelCase";
    private static final String OPERATION_DELETE_CASE = DOT_INTERFACE + "deleteCase";

    public void cancelCase(String caseOid, Task task, OperationResult parentResult)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ConfigurationException,
            CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(OPERATION_CANCEL_CASE);
        result.addParam("caseOid", caseOid);
        try {
            TreeNode<CaseType> caseTree = getCaseTree(caseOid, result);
            cancelCaseTree(caseTree, task, result);
        } catch (RuntimeException | SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException |
                SecurityViolationException | ExpressionEvaluationException | ConfigurationException | CommunicationException e) {
            result.recordFatalError("Case couldn't be cancelled: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void cancelCaseTree(TreeNode<CaseType> caseTree, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, ConfigurationException, SchemaException,
            SecurityViolationException, ObjectAlreadyExistsException, ExpressionEvaluationException {
        CaseType rootCase = caseTree.getUserObject();
        securityEnforcer.authorize(ModelAuthorizationAction.CANCEL_CASE.getUrl(), null,
                AuthorizationParameters.Builder.buildObject(rootCase.asPrismObject()), null, task, result);
        if (!SchemaConstants.CASE_STATE_CLOSED.equals(rootCase.getState())) {
            if (isApprovalCase(rootCase)) {
                CancelCaseRequest request = new CancelCaseRequest(rootCase.getOid());
                workflowEngine.executeRequest(request, task, result);
            } else {
                cancelNonApprovalCase(rootCase, result);
                for (TreeNode<CaseType> child : caseTree.getChildren()) {
                    cancelCaseTree(child, task, result);
                }
            }
        }
    }

    private void cancelNonApprovalCase(CaseType aCase, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        List<ItemDelta<?, ?>> modifications = new ArrayList<>();
        XMLGregorianCalendar now = clock.currentTimeXMLGregorianCalendar();
        for (CaseWorkItemType workItem : aCase.getWorkItem()) {
            if (workItem.getCloseTimestamp() == null) {
                modifications.add(prismContext.deltaFor(CaseType.class)
                        .item(CaseType.F_WORK_ITEM, workItem.getId(), CaseWorkItemType.F_CLOSE_TIMESTAMP).replace(now).asItemDelta());
            }
        }
        modifications.addAll(prismContext.deltaFor(CaseType.class)
                .item(CaseType.F_STATE).replace(SchemaConstants.CASE_STATE_CLOSED)
                .item(CaseType.F_CLOSE_TIMESTAMP).replace(now)
                .asItemDeltas());
        repositoryService.modifyObject(CaseType.class, aCase.getOid(), modifications, result);
    }

    private boolean isApprovalCase(CaseType aCase) {
        return aCase.getArchetypeRef().stream().anyMatch(ref -> SystemObjectsType.ARCHETYPE_APPROVAL_CASE.value().equals(ref.getOid()));
    }

    private TreeNode<CaseType> getCaseTree(String caseOid, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        PrismObject<CaseType> root = repositoryService.getObject(CaseType.class, caseOid, null, result);
        TreeNode<CaseType> tree = new TreeNode<>(root.asObjectable());
        addChildren(tree, result);
        return tree;
    }

    private void addChildren(TreeNode<CaseType> tree, OperationResult result) throws SchemaException {
        ObjectQuery query = prismContext.queryFor(CaseType.class)
                .item(CaseType.F_PARENT_REF).ref(tree.getUserObject().getOid())
                .build();
        SearchResultList<PrismObject<CaseType>> children = repositoryService
                .searchObjects(CaseType.class, query, null, result);
        for (PrismObject<CaseType> child : children) {
            TreeNode<CaseType> childNode = new TreeNode<>(child.asObjectable());
            tree.add(childNode);
            addChildren(childNode, result);
        }
    }

    public void deleteCase(String caseOid, Task task, OperationResult parentResult)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ConfigurationException,
            CommunicationException, SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(OPERATION_DELETE_CASE);
        result.addParam("caseOid", caseOid);
        try {
            TreeNode<CaseType> caseTree = getCaseTree(caseOid, result);
            cancelCaseTree(caseTree, task, result); // if this fails, deletion will not be tried
            deleteCaseTree(caseTree, true, task, result);
        } catch (RuntimeException | SchemaException | ObjectAlreadyExistsException | ObjectNotFoundException |
                SecurityViolationException | ExpressionEvaluationException | ConfigurationException | CommunicationException e) {
            result.recordFatalError("Case couldn't be cancelled/deleted: " + e.getMessage(), e);
            throw e;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void deleteCaseTree(TreeNode<CaseType> caseTree, boolean isRoot, Task task, OperationResult result)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        CaseType aCase = caseTree.getUserObject();
        if (!isRoot) {
            securityEnforcer.authorize(ModelAuthorizationAction.DELETE.getUrl(), AuthorizationPhaseType.EXECUTION,
                    AuthorizationParameters.Builder.buildObjectDelete(aCase.asPrismObject()), null, task, result);
        }

        for (TreeNode<CaseType> child : caseTree.getChildren()) {
            deleteCaseTree(child, false, task, result);
        }
        String caseOid = aCase.getOid();
        try {
            repositoryService.deleteObject(CaseType.class, caseOid, result);
        } catch (ObjectNotFoundException e) {
            LOGGER.warn("Case {} has been already deleted", caseOid);
            // no auditing needed
        } catch (Throwable t) {
            throw t;
        }
    }
}
