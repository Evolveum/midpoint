/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterationMethodType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowClassificationModeType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 *
 */
public interface GetOperationOptionsBuilder {

    static GetOperationOptionsBuilder create() {
        return new GetOperationOptionsBuilderImpl(PrismContext.get());
    }

    GetOperationOptionsBuilder root();
    GetOperationOptionsBuilder items(Object... items);
    GetOperationOptionsBuilder item(ItemPath path);
    GetOperationOptionsBuilder item(Object... components);

    GetOperationOptionsBuilder retrieve();
    GetOperationOptionsBuilder dontRetrieve();
    GetOperationOptionsBuilder retrieve(RetrieveOption value);
    GetOperationOptionsBuilder retrieve(RelationalValueSearchQuery query);
    GetOperationOptionsBuilder.Query retrieveQuery();

    GetOperationOptionsBuilder resolve();
    GetOperationOptionsBuilder resolve(Boolean value);
    GetOperationOptionsBuilder resolveNames();
    GetOperationOptionsBuilder resolveNames(Boolean value);
    GetOperationOptionsBuilder noFetch();
    GetOperationOptionsBuilder noFetch(Boolean value);
    GetOperationOptionsBuilder raw();
    GetOperationOptionsBuilder raw(Boolean value);
    GetOperationOptionsBuilder tolerateRawData();
    GetOperationOptionsBuilder tolerateRawData(Boolean value);
    GetOperationOptionsBuilder doNotDiscovery();
    GetOperationOptionsBuilder doNotDiscovery(Boolean value);
    GetOperationOptionsBuilder allowNotFound();
    GetOperationOptionsBuilder allowNotFound(Boolean value);
    GetOperationOptionsBuilder readOnly();
    GetOperationOptionsBuilder readOnly(Boolean value);
    default GetOperationOptionsBuilder futurePointInTime() {
        return pointInTime(PointInTimeType.FUTURE);
    }
    GetOperationOptionsBuilder pointInTime(PointInTimeType value);
    GetOperationOptionsBuilder staleness(Long value);
    GetOperationOptionsBuilder forceRefresh();
    GetOperationOptionsBuilder forceRefresh(Boolean value);
    GetOperationOptionsBuilder forceRetry();
    GetOperationOptionsBuilder forceRetry(Boolean value);
    GetOperationOptionsBuilder distinct();
    GetOperationOptionsBuilder distinct(Boolean value);
    GetOperationOptionsBuilder attachDiagData();
    GetOperationOptionsBuilder attachDiagData(Boolean value);
    GetOperationOptionsBuilder definitionProcessing(DefinitionProcessingOption value);
    GetOperationOptionsBuilder definitionUpdate(DefinitionUpdateOption value);
    GetOperationOptionsBuilder iterationMethod(IterationMethodType value);
    GetOperationOptionsBuilder iterationPageSize(Integer value);
    GetOperationOptionsBuilder executionPhase();
    GetOperationOptionsBuilder executionPhase(Boolean value);
    GetOperationOptionsBuilder errorHandling(FetchErrorHandlingType errorHandling);
    GetOperationOptionsBuilder errorReportingMethod(FetchErrorReportingMethodType method);
    GetOperationOptionsBuilder shadowClassificationMode(ShadowClassificationModeType mode);

    GetOperationOptionsBuilder setFrom(Collection<SelectorOptions<GetOperationOptions>> options);
    GetOperationOptionsBuilder mergeFrom(Collection<SelectorOptions<GetOperationOptions>> options);

    interface Query {
        Query asc(ItemPath path);
        Query asc(Object... components);
        Query desc(ItemPath path);
        Query desc(Object... components);
        Query offset(Integer n);
        Query maxSize(Integer n);
        Query item(QName column);
        Query eq(String value);
        Query startsWith(String value);
        Query contains(String value);
        GetOperationOptionsBuilder end();
    }

    @NotNull
    Collection<SelectorOptions<GetOperationOptions>> build();
}
