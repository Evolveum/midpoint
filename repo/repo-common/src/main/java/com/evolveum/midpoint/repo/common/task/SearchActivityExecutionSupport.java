/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.task;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ObjectResultHandler;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

interface SearchActivityExecutionSupport<C extends Containerable, PC extends PrismContainer<C>, H extends ObjectResultHandler> {

    int countObjectsInRepository(Class<C> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull IterativeActivityExecution activityExecution, OperationResult result) throws SchemaException;

    OperationResultType getFetchResult(PC object);

    void searchIterativeInRepository(Class<C> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options,
            AtomicInteger sequentialNumberCounter, @NotNull IterativeActivityExecution activityExecution, OperationResult result) throws SchemaException;

    H createSearchResultHandler(AtomicInteger sequentialNumberCounter, ProcessingCoordinator coordinator,
            @NotNull IterativeActivityExecution activityExecution);
}
