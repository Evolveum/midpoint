/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus;

import static com.evolveum.midpoint.schema.result.OperationResult.HANDLE_OBJECT_FOUND;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.AutoassignRoleMappingEvaluationRequest;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.FocalMappingEvaluationRequest;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.query.SelectorMatcher;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.config.AutoassignSpecificationConfigItem;
import com.evolveum.midpoint.schema.config.ObjectSelectorConfigItem;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Collects auto-assignment mappings from auto-assignable roles.
 */
@Component
public class AutoAssignMappingCollector {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectTemplateProcessor.class);

    private static final String OP_HANDLE_OBJECT_FOUND = AutoAssignMappingCollector.class.getName() + "." + HANDLE_OBJECT_FOUND;

    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService cacheRepositoryService;
    @Autowired private PrismContext prismContext;

    <AH extends AssignmentHolderType>
    void collectAutoassignMappings(
            LensContext<AH> context, List<FocalMappingEvaluationRequest<?, ?>> mappings, OperationResult result)
            throws SchemaException {

        if (!autoassignEnabled(context.getSystemConfiguration())) {
            return;
        }

        ObjectQuery query = prismContext
                .queryFor(AbstractRoleType.class)
                .item(SchemaConstants.PATH_AUTOASSIGN_ENABLED)
                .eq(true)
                .build();

        ResultHandler<AbstractRoleType> handler = (role, objectResult) -> {
            AutoassignSpecificationType bean = role.asObjectable().getAutoassign();
            if (bean == null) {
                return true;
            }
            // [EP:M:AAFM] DONE, obviously in the object
            var autoassignSpec = AutoassignSpecificationConfigItem.embedded(bean);
            if (!autoassignSpec.isEnabled()) {
                return true;
            }
            var focalAutoassignSpec = autoassignSpec.getFocus();
            if (focalAutoassignSpec == null) {
                return true;
            }
            if (!isApplicableFor(focalAutoassignSpec.getSelector(), context.getFocusContext(), objectResult)) {
                return true;
            }
            for (var autoMapping: focalAutoassignSpec.getMappings()) {
                var mappingWithTarget = autoMapping.setTargetIfMissing(FocusType.F_ASSIGNMENT);
                mappings.add(
                        // [EP:M:AAFM] DONE, chained calls from verified CI above
                        new AutoassignRoleMappingEvaluationRequest(mappingWithTarget, role.asObjectable()));
                LOGGER.trace("Collected autoassign mapping {} from {}", mappingWithTarget.getName(), role);
            }
            return true;
        };
        cacheRepositoryService.searchObjectsIterative(
                AbstractRoleType.class, query,
                handler.providingOwnOperationResult(OP_HANDLE_OBJECT_FOUND),
                createReadOnlyCollection(), true, result);
    }

    private <AH extends AssignmentHolderType> boolean isApplicableFor(
            ObjectSelectorConfigItem selector, LensFocusContext<AH> focusContext, OperationResult result) {
        if (selector == null) {
            return true;
        }
        try {
            // Note we do NOT provide filter expression evaluator here. Hence, there's no need to manage expression profiles yet.
            return SelectorMatcher.forSelector(selector.value())
                    .withLogging(LOGGER)
                    .matches(focusContext.getObjectAnyRequired());
        } catch (CommonException e) {
            String message = "Failed to evaluate selector constraints, selector: %s, focusContext: %s, reason: %s".formatted(
                    selector, focusContext, e.getMessage());
            result.recordException(message, e);
            throw new SystemException(message, e);
        }
    }

    private boolean autoassignEnabled(PrismObject<SystemConfigurationType> systemConfiguration) {
        if (systemConfiguration == null) {
            return false;
        }
        RoleManagementConfigurationType roleManagement = systemConfiguration.asObjectable().getRoleManagement();
        return roleManagement != null && isTrue(roleManagement.isAutoassignEnabled());
    }
}
