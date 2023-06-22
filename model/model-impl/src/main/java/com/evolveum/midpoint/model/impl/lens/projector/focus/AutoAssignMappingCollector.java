/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus;

import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensUtil;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.AutoassignRoleMappingEvaluationRequest;
import com.evolveum.midpoint.model.impl.lens.projector.mappings.FocalMappingEvaluationRequest;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.common.query.SelectorMatcher;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.evolveum.midpoint.schema.GetOperationOptions.createReadOnlyCollection;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * Collects auto-assignment mappings from auto-assignable roles.
 */
@Component
public class AutoAssignMappingCollector {

    private static final Trace LOGGER = TraceManager.getTrace(ObjectTemplateProcessor.class);

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
            AutoassignSpecificationType autoassign = role.asObjectable().getAutoassign();
            if (autoassign == null) {
                return true;
            }
            if (!isTrue(autoassign.isEnabled())) {
                return true;
            }
            FocalAutoassignSpecificationType focalAutoassignSpec = autoassign.getFocus();
            if (focalAutoassignSpec == null) {
                return true;
            }

            if (!isApplicableFor(focalAutoassignSpec.getSelector(), context.getFocusContext(), objectResult)) {
                return true;
            }

            for (AutoassignMappingType autoMapping: focalAutoassignSpec.getMapping()) {
                AutoassignMappingType mapping =
                        LensUtil.setMappingTarget(autoMapping, new ItemPathType(SchemaConstants.PATH_ASSIGNMENT));
                mappings.add(new AutoassignRoleMappingEvaluationRequest(mapping, role.asObjectable()));
                LOGGER.trace("Collected autoassign mapping {} from {}", mapping.getName(), role);
            }
            return true;
        };
        cacheRepositoryService.searchObjectsIterative(
                AbstractRoleType.class, query, handler, createReadOnlyCollection(), true, result);
    }

    private <AH extends AssignmentHolderType> boolean isApplicableFor(
            ObjectSelectorType selector, LensFocusContext<AH> focusContext, OperationResult result) {
        if (selector == null) {
            return true;
        }
        try {
            return SelectorMatcher.forSelector(selector)
                    .withLogging(LOGGER)
                    .matches(focusContext.getObjectAnyRequired());
        } catch (CommonException e) {
            String message = "Failed to evaluate selector constrains, selector: %s, focusContext: %s, reason: %s".formatted(
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
