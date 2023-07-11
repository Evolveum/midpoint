/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import javax.xml.namespace.QName;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author lazyman
 */
public class PageAdminConfiguration extends PageAdmin {

    public PageAdminConfiguration() {
    }

    public PageAdminConfiguration(PageParameters parameters) {
        super(parameters);
    }

    /**
     * Creates and submits a deletion activity task.
     *
     * The task is created with the default mode i.e. raw.
     */
    String deleteObjectsAsync(
            @NotNull QName type, @Nullable ObjectQuery objectQuery, @NotNull String taskName, @NotNull OperationResult result)
            throws CommonException {

        Task task = createSimpleTask(result.getOperation());

        // @formatter:off
        ActivityDefinitionType definition = new ActivityDefinitionType()
                .beginWork()
                    .beginDeletion()
                        .beginObjects()
                            .type(type)
                            .query(createQueryBean(objectQuery))
                        .<DeletionWorkDefinitionType>end()
                    .<WorkDefinitionsType>end()
                .end();
        // @formatter:on

        return getModelInteractionService().submit(
                definition,
                ActivitySubmissionOptions.create()
                        .withTaskTemplate(new TaskType()
                                .name(taskName))
                        .withArchetypes(
                                SystemObjectsType.ARCHETYPE_UTILITY_TASK.value(),
                                SystemObjectsType.ARCHETYPE_OBJECTS_DELETE_TASK.value()),
                task, result);
    }

    private @NotNull QueryType createQueryBean(@Nullable ObjectQuery query) throws SchemaException {
        if (query != null) {
            return getQueryConverter().createQueryType(query);
        } else {
            return new QueryType();
        }
    }
}
