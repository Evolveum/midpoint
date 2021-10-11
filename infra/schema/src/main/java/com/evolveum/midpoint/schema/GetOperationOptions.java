/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GetOperationOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IterationMethodType;

/**
 * @author semancik
 */
public class GetOperationOptions extends AbstractOptions implements Serializable, Cloneable, ShortDumpable {
    private static final long serialVersionUID = 1L;

    public static final GetOperationOptions EMPTY = new GetOperationOptions();

    /**
     * Specifies whether to return specific items. It is used for optimizations.
     * Some requests only needs a subset of items therefore fetching them all is a waste
     * of resources. Other requests may need expensive data that are not normally returned by default.
     * <p>
     * If no retrieve option is set in the entire options set then it
     * means that the whole object with a default set of properties has to be
     * returned. This is equivalent to specifying DEFAULT retrieve root option.
     * <p>
     * If there is at least one retrieve option in the set then the following rules apply:
     * <ul>
     *   <li>Items marked as INCLUDE will be returned.</li>
     *   <li>Any item marked as EXCLUDE may not be returned. (Note: Excluded items may still be returned if their retrieval is cheap.)</li>
     *   <li>Items marked as DEFAULT will be returned if they would also be returned without any options (by default).</li>
     *   <li>Items that are not marked (have no option or have null retrieve option) but their superitem is marked (have retrieve option)
     *       behave in the same way as superitem. E.g. if a superitem is marked as
     *       INCLUDE they will also be included in the result. This also applies transitively (e.g. superitem of superitem).
     *   <li>If a superitem is marked as EXCLUDE and subitem is marked as INCLUDE then the behavior is undefined. Do not do this. Strange things may happen.</li>
     *   <li>For items that are not marked in any way and for which the superitem is also not marked the "I do not care" behavior is assumed.
     *       This means that they may be returned or they may be not. The implementation will return them if their retrieval is cheap
     *       but they will be most likely omitted from the result.</li>
     *  </ul>
     */
    private RetrieveOption retrieve;

    /**
     * Resolve the object reference. This only makes sense with a (path-based) selector.
     */
    private Boolean resolve;

    /**
     * Resolve the object reference names.
     */
    private Boolean resolveNames;

    /**
     * No not fetch any information from external sources, e.g. do not fetch account data from resource,
     * do not fetch resource schema, etc.
     * Such operation returns only the data stored in midPoint repository.
     */
    private Boolean noFetch;

    /**
     * Avoid any smart processing of the data except for schema application. Do not synchronize the data, do not apply
     * any expressions, etc.
     */
    private Boolean raw;

    /**
     * Tolerate "raw" data in returned object. In some cases, raw data are tolerated by default (e.g. if raw=true
     * and the object is ResourceType or ShadowType). But generally, toleration of raw data can be explicitly requested
     * by setting this flag to TRUE.
     */
    private Boolean tolerateRawData;

    /**
     * Force to get object from the resource even if some of the error occurred.
     * If the any copy of the shadow is fetched, we can't delete this object
     * from the gui, for example
     */
    private Boolean doNotDiscovery;

    private RelationalValueSearchQuery relationalValueSearchQuery;

    /**
     * This flag indicated if the "object not found" error is critical for
     * processing the original request. If it is not, we just ignore it and
     * don't log it. In other cases, error in logs may lead to misleading
     * information..
     */
    private Boolean allowNotFound;

    /**
     * Return read-only object. The returned object will be only read by the client. The client will not modify it.
     * Immutable object is returned if it is possible.
     * This option allows to turn on internal optimization (e.g. avoid cloning the values). It should be used
     * at all times when the client do not plan to modify the returned object.
     */
    private Boolean readOnly;

    /**
     * Specifies the point in time for the returned data. This option controls whether fresh or cached data will
     * be returned or whether future data projection will be returned. MidPoint usually deals with fresh data
     * that describe situation at the current point in time. But the client code may want to get data from the
     * cache that may be possibly stale. Or the client code may want a projection about the future state of the
     * data (e.g. taking running asynchronous operation into consideration).
     * If this option is not specified then the current point in time is the default if no staleness option is
     * specified or if it is zero. If non-zero staleness option is specified then this option defaults to cached
     * data.
     */
    private PointInTimeType pointInTimeType;

    /**
     * Requirement how stale or fresh the retrieved data should be. It specifies maximum age of the value in millisecods.
     * The default value is zero, which means that a fresh value must always be returned. This means that caches that do
     * not guarantee fresh value cannot be used. If non-zero value is specified then such caches may be used. In case that
     * Long.MAX_VALUE is specified then the caches are always used and fresh value is never retrieved.
     * <p>
     * Null value is special in one more aspect: it allows to return partial cached data in case that the original is not
     * accessible. E.g. in that case provisioning can return repository shadow in case that the resource is not reachable.
     * Explicit specification of staleness=0 disables this behavior.
     */
    private Long staleness;

    /**
     * Force refresh of object before the data are retrieved. The operations are retried after the time perios passed.
     * This option is a guarantee that we get the freshest data that is possible. However, strange things may happen here.
     * E.g. object that existed before this operation may get deleted during refresh because it has expired in the
     * meantime. Or get operation may in fact attempt to create, modify and even delete of an account. This may
     * happen in case that there are some unfinished operations in the shadow. Therefore when using this option you
     * have to be really prepared for everything.
     */
    private Boolean forceRefresh;

    /**
     * Force retry of postponed operations of object before the data are retrieved even when the periods hasn't passed yet.
     * This option is a guarantee that we get the freshest data that is possible. However, strange things may happen here.
     * E.g. object that existed before this operation may get deleted during refresh because it has expired in the meantime.
     * Or get operation may in fact attempt to create, modify and even delete of an account. This may happen in case that
     * there are some unfinished operations in the shadow. Therefore when using this option you have to be really prepared for everything.
     */
    private Boolean forceRetry;

    /**
     * Should the results be made distinct.
     * Not all providers support this option.
     * <p>
     * BEWARE:
     * - may bring a potentially huge performance penalty
     * - may interfere with paging (!)
     * <p>
     * So please consider this option an EXPERIMENTAL, for now.
     */
    @Experimental
    private Boolean distinct;

    /**
     * Whether to attach diagnostics data to the returned object(s).
     */
    private Boolean attachDiagData;

    /**
     * TODO
     */
    private DefinitionProcessingOption definitionProcessing;

    /**
     * Whether to override default iteration method (in searchObjectsIterative) configured for particular DBMS.
     */
    private IterationMethodType iterationMethod;

    /**
     * Whether this operation is already part of the execution phase. I.e. the request authorization was already
     * processed. This means that the operation is in fact operation invoked within another operation,
     * e.g. invoked from script or expression evaluator.
     * <p>
     * WARNING: THIS OPTION MUST NOT BE AVAILABLE FROM REMOTE INTERFACES.
     * This is safe to use from a secure area of JVM, where the components can trick model to circumvent
     * authorizations anyway. But it must not be available outside of the secure area.
     */
    private Boolean executionPhase;

    /*
     *  !!! After adding option here don't forget to update equals, clone, merge, etc. !!!
     */

    public RetrieveOption getRetrieve() {
        return retrieve;
    }

    public void setRetrieve(RetrieveOption retrieve) {
        this.retrieve = retrieve;
    }

    public static RetrieveOption getRetrieve(GetOperationOptions options) {
        if (options == null) {
            return null;
        }
        return options.retrieve;
    }

    public static GetOperationOptions createRetrieve(RetrieveOption retrieve) {
        GetOperationOptions options = new GetOperationOptions();
        options.retrieve = retrieve;
        return options;
    }

    /**
     * Specifies whether to return specific items. It is used for optimizations.
     * Some requests only needs a subset of items therefore fetching them all is a waste
     * of resources. Other requests may need expensive data that are not normally returned by default.
     * <p>
     * If no retrieve option is set in the entire options set then it
     * means that the whole object with a default set of properties has to be
     * returned. This is equivalent to specifying DEFAULT retrieve root option.
     * <p>
     * If there is at least one retrieve option in the set then the following rules apply:
     * <ul>
     *   <li>Items marked as INCLUDE will be returned.</li>
     *   <li>Any item marked as EXCLUDE may not be returned. (Note: Excluded items may still be returned if their retrieval is cheap.)</li>
     *   <li>Items marked as DEFAULT will be returned if they would also be returned without any options (by default).</li>
     *   <li>Items that are not marked (have no option or have null retrieve option) but their superitem is marked (have retrieve option)
     *       behave in the same way as superitem. E.g. if a superitem is marked as
     *       INCLUDE they will also be included in the result. This also applies transitively (e.g. superitem of superitem).
     *   <li>If a superitem is marked as EXCLUDE and subitem is marked as INCLUDE then the behavior is undefined. Do not do this. Strange things may happen.</li>
     *   <li>For items that are not marked in any way and for which the superitem is also not marked the "I do not care" behavior is assumed.
     *       This means that they may be returned or they may be not. The implementation will return them if their retrieval is cheap
     *       but they will be most likely omitted from the result.</li>
     *  </ul>
     */
    public static GetOperationOptions createRetrieve() {
        return createRetrieve(RetrieveOption.INCLUDE);
    }

    /**
     * Specifies whether to return specific items. It is used for optimizations.
     * Some requests only needs a subset of items therefore fetching them all is a waste
     * of resources. Other requests may need expensive data that are not normally returned by default.
     * <p>
     * If no retrieve option is set in the entire options set then it
     * means that the whole object with a default set of properties has to be
     * returned. This is equivalent to specifying DEFAULT retrieve root option.
     * <p>
     * If there is at least one retrieve option in the set then the following rules apply:
     * <ul>
     *   <li>Items marked as INCLUDE will be returned.</li>
     *   <li>Any item marked as EXCLUDE may not be returned. (Note: Excluded items may still be returned if their retrieval is cheap.)</li>
     *   <li>Items marked as DEFAULT will be returned if they would also be returned without any options (by default).</li>
     *   <li>Items that are not marked (have no option or have null retrieve option) but their superitem is marked (have retrieve option)
     *       behave in the same way as superitem. E.g. if a superitem is marked as
     *       INCLUDE they will also be included in the result. This also applies transitively (e.g. superitem of superitem).
     *   <li>If a superitem is marked as EXCLUDE and subitem is marked as INCLUDE then the behavior is undefined. Do not do this. Strange things may happen.</li>
     *   <li>For items that are not marked in any way and for which the superitem is also not marked the "I do not care" behavior is assumed.
     *       This means that they may be returned or they may be not. The implementation will return them if their retrieval is cheap
     *       but they will be most likely omitted from the result.</li>
     *  </ul>
     */
    public static GetOperationOptions createDontRetrieve() {
        return createRetrieve(RetrieveOption.EXCLUDE);
    }

    /**
     * Specifies whether to return specific items. It is used for optimizations.
     * Some requests only needs a subset of items therefore fetching them all is a waste
     * of resources. Other requests may need expensive data that are not normally returned by default.
     * <p>
     * If no retrieve option is set in the entire options set then it
     * means that the whole object with a default set of properties has to be
     * returned. This is equivalent to specifying DEFAULT retrieve root option.
     * <p>
     * If there is at least one retrieve option in the set then the following rules apply:
     * <ul>
     *   <li>Items marked as INCLUDE will be returned.</li>
     *   <li>Any item marked as EXCLUDE may not be returned. (Note: Excluded items may still be returned if their retrieval is cheap.)</li>
     *   <li>Items marked as DEFAULT will be returned if they would also be returned without any options (by default).</li>
     *   <li>Items that are not marked (have no option or have null retrieve option) but their superitem is marked (have retrieve option)
     *       behave in the same way as superitem. E.g. if a superitem is marked as
     *       INCLUDE they will also be included in the result. This also applies transitively (e.g. superitem of superitem).
     *   <li>If a superitem is marked as EXCLUDE and subitem is marked as INCLUDE then the behavior is undefined. Do not do this. Strange things may happen.</li>
     *   <li>For items that are not marked in any way and for which the superitem is also not marked the "I do not care" behavior is assumed.
     *       This means that they may be returned or they may be not. The implementation will return them if their retrieval is cheap
     *       but they will be most likely omitted from the result.</li>
     *  </ul>
     */
    public static GetOperationOptions createRetrieve(RelationalValueSearchQuery query) {
        GetOperationOptions options = new GetOperationOptions();
        options.retrieve = RetrieveOption.INCLUDE;
        options.setRelationalValueSearchQuery(query);
        return options;
    }

    public Boolean getResolve() {
        return resolve;
    }

    public void setResolve(Boolean resolve) {
        this.resolve = resolve;
    }

    public GetOperationOptions resolve(Boolean resolve) {
        this.resolve = resolve;
        return this;
    }

    public static boolean isResolve(GetOperationOptions options) {
        if (options == null) {
            return false;
        }
        if (options.resolve == null) {
            return false;
        }
        return options.resolve;
    }

    /**
     * Resolve the object reference. This only makes sense with a (path-based) selector.
     */
    public static GetOperationOptions createResolve() {
        GetOperationOptions opts = new GetOperationOptions();
        opts.setResolve(true);
        return opts;
    }

    public Boolean getNoFetch() {
        return noFetch;
    }

    public void setNoFetch(Boolean noFetch) {
        this.noFetch = noFetch;
    }

    public GetOperationOptions noFetch(Boolean noFetch) {
        this.noFetch = noFetch;
        return this;
    }

    public static boolean isNoFetch(GetOperationOptions options) {
        if (options == null) {
            return false;
        }
        if (options.noFetch == null) {
            return false;
        }
        return options.noFetch;
    }

    /**
     * No not fetch any information from external sources, e.g. do not fetch account data from resource,
     * do not fetch resource schema, etc.
     * Such operation returns only the data stored in midPoint repository.
     */
    public static GetOperationOptions createNoFetch() {
        GetOperationOptions opts = new GetOperationOptions();
        opts.setNoFetch(true);
        return opts;
    }

    public Boolean getResolveNames() {
        return resolveNames;
    }

    public void setResolveNames(Boolean resolveNames) {
        this.resolveNames = resolveNames;
    }

    public GetOperationOptions resolveNames(Boolean resolveNames) {
        this.resolveNames = resolveNames;
        return this;
    }

    public static boolean isResolveNames(GetOperationOptions options) {
        if (options == null) {
            return false;
        }
        if (options.resolveNames == null) {
            return false;
        }
        return options.resolveNames;
    }

    /**
     * Resolve the object reference names.
     */
    public static GetOperationOptions createResolveNames() {
        GetOperationOptions opts = new GetOperationOptions();
        opts.setResolveNames(true);
        return opts;
    }

    public Boolean getTolerateRawData() {
        return tolerateRawData;
    }

    public void setTolerateRawData(Boolean value) {
        this.tolerateRawData = value;
    }

    public GetOperationOptions tolerateRawData(Boolean value) {
        this.tolerateRawData = value;
        return this;
    }

    public static boolean isTolerateRawData(GetOperationOptions options) {
        if (options == null) {
            return false;
        }
        if (options.tolerateRawData == null) {
            return false;
        }
        return options.tolerateRawData;
    }

    /**
     * Tolerate "raw" data in returned object. In some cases, raw data are tolerated by default (e.g. if raw=true
     * and the object is ResourceType or ShadowType). But generally, toleration of raw data can be explicitly requested
     * by setting this flag to TRUE.
     */
    public static GetOperationOptions createTolerateRawData() {
        GetOperationOptions opts = new GetOperationOptions();
        opts.setTolerateRawData(true);
        return opts;
    }

    public Boolean getRaw() {
        return raw;
    }

    public void setRaw(Boolean raw) {
        this.raw = raw;
    }

    public GetOperationOptions raw(Boolean raw) {
        this.raw = raw;
        return this;
    }

    public static boolean isRaw(GetOperationOptions options) {
        if (options == null) {
            return false;
        }
        if (options.raw == null) {
            return false;
        }
        return options.raw;
    }

    /**
     * Avoid any smart processing of the data except for schema application. Do not synchronize the data, do not apply
     * any expressions, etc.
     */
    public static GetOperationOptions createRaw() {
        GetOperationOptions opts = new GetOperationOptions();
        opts.setRaw(true);
        return opts;
    }

    /**
     * Avoid any smart processing of the data except for schema application. Do not synchronize the data, do not apply
     * any expressions, etc.
     */
    public static Collection<SelectorOptions<GetOperationOptions>> createRawCollection() {
        return SelectorOptions.createCollection(createRaw());
    }

    /**
     * No not fetch any information from external sources, e.g. do not fetch account data from resource,
     * do not fetch resource schema, etc.
     * Such operation returns only the data stored in midPoint repository.
     */
    public static Collection<SelectorOptions<GetOperationOptions>> createNoFetchCollection() {
        return SelectorOptions.createCollection(createNoFetch());
    }

    public Boolean getDoNotDiscovery() {
        return doNotDiscovery;
    }

    public void setDoNotDiscovery(Boolean force) {
        this.doNotDiscovery = force;
    }

    public GetOperationOptions doNotDiscovery(Boolean force) {
        this.doNotDiscovery = force;
        return this;
    }

    public static boolean isDoNotDiscovery(GetOperationOptions options) {
        if (options == null) {
            return false;
        }
        if (options.doNotDiscovery == null) {
            return false;
        }
        return options.doNotDiscovery;
    }

    /**
     * Force to get object from the resource even if some of the error occurred.
     * If the any copy of the shadow is fetched, we can't delete this object
     * from the gui, for example
     */
    public static GetOperationOptions createDoNotDiscovery() {
        GetOperationOptions opts = new GetOperationOptions();
        opts.setDoNotDiscovery(true);
        return opts;
    }

    /**
     * This flag indicated if the "object not found" error is critical for
     * processing the original request. If it is not, we just ignore it and
     * don't log it. In other cases, error in logs may lead to misleading
     * information..
     */
    public static GetOperationOptions createAllowNotFound() {
        GetOperationOptions opts = new GetOperationOptions();
        opts.setAllowNotFound(true);
        return opts;
    }

    public Boolean getAllowNotFound() {
        return allowNotFound;
    }

    public void setAllowNotFound(Boolean allowNotFound) {
        this.allowNotFound = allowNotFound;
    }

    public GetOperationOptions allowNotFound(Boolean allowNotFound) {
        this.allowNotFound = allowNotFound;
        return this;
    }

    public static boolean isAllowNotFound(GetOperationOptions options) {
        if (options == null) {
            return false;
        }
        if (options.allowNotFound == null) {
            return false;
        }
        return options.allowNotFound;
    }

    /**
     * Return read-only object. The returned object will be only read by the client. The client will not modify it.
     * Immutable object is returned if it is possible.
     * This option allows to turn on internal optimization (e.g. avoid cloning the values). It should be used
     * at all times when the client do not plan to modify the returned object.
     */
    public static GetOperationOptions createReadOnly() {
        GetOperationOptions opts = new GetOperationOptions();
        opts.setReadOnly(true);
        return opts;
    }

    public static Collection<SelectorOptions<GetOperationOptions>> createReadOnlyCollection() {
        return SelectorOptions.createCollection(createReadOnly());
    }

    public Boolean getReadOnly() {
        return readOnly;
    }

    public void setReadOnly(Boolean readOnly) {
        this.readOnly = readOnly;
    }

    public GetOperationOptions readOnly(Boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    public static boolean isReadOnly(GetOperationOptions options) {
        if (options == null) {
            return false;
        }
        if (options.readOnly == null) {
            return false;
        }
        return options.readOnly;
    }

    public PointInTimeType getPointInTimeType() {
        return pointInTimeType;
    }

    public void setPointInTimeType(PointInTimeType pointInTimeType) {
        this.pointInTimeType = pointInTimeType;
    }

    public GetOperationOptions pointInTimeType(PointInTimeType pointInTimeType) {
        this.pointInTimeType = pointInTimeType;
        return this;
    }

    /**
     * Specifies the point in time for the returned data. This option controls whether fresh or cached data will
     * be returned or whether future data projection will be returned. MidPoint usually deals with fresh data
     * that describe situation at the current point in time. But the client code may want to get data from the
     * cache that may be possibly stale. Or the client code may want a projection about the future state of the
     * data (e.g. taking running asynchronous operation into consideration).
     * If this option is not specified then the current point in time is the default if no staleness option is
     * specified or if it is zero. If non-zero staleness option is specified then this option defaults to cached
     * data.
     */
    public static GetOperationOptions createPointInTimeType(PointInTimeType pit) {
        GetOperationOptions opts = new GetOperationOptions();
        opts.setPointInTimeType(pit);
        return opts;
    }

    public static PointInTimeType getPointInTimeType(GetOperationOptions options) {
        if (options == null) {
            return null;
        }
        if (options.getPointInTimeType() == null) {
            return null;
        }
        return options.getPointInTimeType();
    }

    public Long getStaleness() {
        return staleness;
    }

    public void setStaleness(Long staleness) {
        this.staleness = staleness;
    }

    public GetOperationOptions staleness(Long staleness) {
        this.staleness = staleness;
        return this;
    }

    /**
     * Requirement how stale or fresh the retrieved data should be. It specifies maximum age of the value in milliseconds.
     * The default value is zero, which means that a fresh value must always be returned. This means that caches that do
     * not guarantee fresh value cannot be used. If non-zero value is specified then such caches may be used. In case that
     * Long.MAX_VALUE is specified then the caches are always used and fresh value is never retrieved.
     */
    public static GetOperationOptions createStaleness(Long staleness) {
        GetOperationOptions opts = new GetOperationOptions();
        opts.setStaleness(staleness);
        return opts;
    }

    public static GetOperationOptions createMaxStaleness() {
        GetOperationOptions opts = new GetOperationOptions();
        opts.setStaleness(Long.MAX_VALUE);
        return opts;
    }

    public static long getStaleness(GetOperationOptions options) {
        if (options == null) {
            return 0L;
        }
        if (options.getStaleness() == null) {
            return 0L;
        }
        return options.getStaleness();
    }

    public static boolean isMaxStaleness(GetOperationOptions options) {
        return GetOperationOptions.getStaleness(options) == Long.MAX_VALUE;
    }

    public Boolean getForceRefresh() {
        return forceRefresh;
    }

    public void setForceRefresh(Boolean forceRefresh) {
        this.forceRefresh = forceRefresh;
    }

    public GetOperationOptions forceRefresh(Boolean forceRefresh) {
        this.forceRefresh = forceRefresh;
        return this;
    }

    public static boolean isForceRefresh(GetOperationOptions options) {
        if (options == null) {
            return false;
        }
        if (options.forceRefresh == null) {
            return false;
        }
        return options.forceRefresh;
    }

    public static GetOperationOptions createForceRefresh() {
        GetOperationOptions opts = new GetOperationOptions();
        opts.setForceRefresh(true);
        return opts;
    }

    public Boolean getForceRetry() {
        return forceRetry;
    }

    public void setForceRetry(Boolean forceRetry) {
        this.forceRetry = forceRetry;
    }

    public static GetOperationOptions createForceRetry() {
        GetOperationOptions opts = new GetOperationOptions();
        opts.setForceRetry(Boolean.TRUE);
        return opts;
    }

    public static boolean isForceRetry(GetOperationOptions options) {
        if (options == null) {
            return false;
        }
        if (options.forceRetry == null) {
            return false;
        }
        return options.forceRetry;
    }

    public Boolean getDistinct() {
        return distinct;
    }

    public void setDistinct(Boolean distinct) {
        this.distinct = distinct;
    }

    public GetOperationOptions distinct(Boolean distinct) {
        this.distinct = distinct;
        return this;
    }

    public static boolean isDistinct(GetOperationOptions options) {
        if (options == null) {
            return false;
        }
        if (options.distinct == null) {
            return false;
        }
        return options.distinct;
    }

    /**
     * Should the results be made distinct.
     * Not all providers support this option.
     * <p>
     * BEWARE:
     * - may bring a potentially huge performance penalty
     * - may interfere with paging (!)
     * <p>
     * So please consider this option an EXPERIMENTAL, for now.
     */
    @Experimental
    public static GetOperationOptions createDistinct() {
        GetOperationOptions opts = new GetOperationOptions();
        opts.setDistinct(true);
        return opts;
    }

    public Boolean getAttachDiagData() {
        return attachDiagData;
    }

    public void setAttachDiagData(Boolean value) {
        this.attachDiagData = value;
    }

    public GetOperationOptions attachDiagData(Boolean value) {
        this.attachDiagData = value;
        return this;
    }

    public static boolean isAttachDiagData(GetOperationOptions options) {
        if (options == null) {
            return false;
        }
        if (options.attachDiagData == null) {
            return false;
        }
        return options.attachDiagData;
    }

    /**
     * Whether to attach diagnostics data to the returned object(s).
     */
    public static GetOperationOptions createAttachDiagData() {
        GetOperationOptions opts = new GetOperationOptions();
        opts.setAttachDiagData(true);
        return opts;
    }

    public Boolean getExecutionPhase() {
        return executionPhase;
    }

    public void setExecutionPhase(Boolean executionPhase) {
        this.executionPhase = executionPhase;
    }

    public GetOperationOptions executionPhase(Boolean executionPhase) {
        this.executionPhase = executionPhase;
        return this;
    }

    public static boolean isExecutionPhase(GetOperationOptions options) {
        if (options == null) {
            return false;
        }
        if (options.executionPhase == null) {
            return false;
        }
        return options.executionPhase;
    }

    public static GetOperationOptions createExecutionPhase() {
        GetOperationOptions opts = new GetOperationOptions();
        opts.setExecutionPhase(true);
        return opts;
    }

    public DefinitionProcessingOption getDefinitionProcessing() {
        return definitionProcessing;
    }

    public void setDefinitionProcessing(DefinitionProcessingOption definitionProcessing) {
        this.definitionProcessing = definitionProcessing;
    }

    public GetOperationOptions definitionProcessing(DefinitionProcessingOption definitionProcessing) {
        this.definitionProcessing = definitionProcessing;
        return this;
    }

    public static DefinitionProcessingOption getDefinitionProcessing(GetOperationOptions options) {
        return options != null ? options.definitionProcessing : null;
    }

    /**
     * TODO
     */
    public static GetOperationOptions createDefinitionProcessing(DefinitionProcessingOption value) {
        GetOperationOptions opts = new GetOperationOptions();
        opts.setDefinitionProcessing(value);
        return opts;
    }

    public IterationMethodType getIterationMethod() {
        return iterationMethod;
    }

    public void setIterationMethod(IterationMethodType iterationMethod) {
        this.iterationMethod = iterationMethod;
    }

    public GetOperationOptions iterationMethod(IterationMethodType iterationMethod) {
        this.iterationMethod = iterationMethod;
        return this;
    }

    public static IterationMethodType getIterationMethod(GetOperationOptions options) {
        return options != null ? options.iterationMethod : null;
    }

    /**
     * Whether to override default iteration method (in searchObjectsIterative) configured for particular DBMS.
     */
    public static GetOperationOptions createIterationMethod(IterationMethodType value) {
        GetOperationOptions opts = new GetOperationOptions();
        opts.setIterationMethod(value);
        return opts;
    }

    public RelationalValueSearchQuery getRelationalValueSearchQuery() {
        return relationalValueSearchQuery;
    }

    public void setRelationalValueSearchQuery(RelationalValueSearchQuery relationalValueSearchQuery) {
        this.relationalValueSearchQuery = relationalValueSearchQuery;
    }

    public GetOperationOptions relationalValueSearchQuery(RelationalValueSearchQuery relationalValueSearchQuery) {
        this.relationalValueSearchQuery = relationalValueSearchQuery;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof GetOperationOptions)) { return false; }
        GetOperationOptions that = (GetOperationOptions) o;
        return retrieve == that.retrieve &&
                Objects.equals(resolve, that.resolve) &&
                Objects.equals(resolveNames, that.resolveNames) &&
                Objects.equals(noFetch, that.noFetch) &&
                Objects.equals(raw, that.raw) &&
                Objects.equals(tolerateRawData, that.tolerateRawData) &&
                Objects.equals(doNotDiscovery, that.doNotDiscovery) &&
                Objects.equals(relationalValueSearchQuery, that.relationalValueSearchQuery) &&
                Objects.equals(allowNotFound, that.allowNotFound) &&
                Objects.equals(readOnly, that.readOnly) &&
                Objects.equals(pointInTimeType, that.pointInTimeType) &&
                Objects.equals(staleness, that.staleness) &&
                Objects.equals(attachDiagData, that.attachDiagData) &&
                Objects.equals(executionPhase, that.executionPhase);
    }

    @Override
    public int hashCode() {
        return Objects
                .hash(retrieve, resolve, resolveNames, noFetch, raw, tolerateRawData, doNotDiscovery, relationalValueSearchQuery,
                        allowNotFound, readOnly, staleness, distinct, definitionProcessing, attachDiagData, executionPhase);
    }

    public GetOperationOptions clone() {
        GetOperationOptions clone = new GetOperationOptions();
        clone.noFetch = this.noFetch;
        clone.doNotDiscovery = this.doNotDiscovery;
        clone.raw = this.raw;
        clone.resolve = this.resolve;
        clone.resolveNames = this.resolveNames;
        clone.retrieve = this.retrieve;
        clone.allowNotFound = this.allowNotFound;
        clone.readOnly = this.readOnly;
        clone.pointInTimeType = this.pointInTimeType;
        clone.staleness = this.staleness;
        clone.distinct = this.distinct;
        clone.attachDiagData = this.attachDiagData;
        clone.executionPhase = this.executionPhase;
        if (this.relationalValueSearchQuery != null) {
            clone.relationalValueSearchQuery = this.relationalValueSearchQuery.clone();
        }
        clone.definitionProcessing = this.definitionProcessing;
        return clone;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("GetOperationOptions(");
        shortDump(sb);
        sb.append(")");
        return sb.toString();
    }

    @Override
    public void shortDump(StringBuilder sb) {
        appendFlag(sb, "resolve", resolve);
        appendFlag(sb, "resolveNames", resolveNames);
        appendFlag(sb, "noFetch", noFetch);
        appendFlag(sb, "raw", raw);
        appendFlag(sb, "doNotDiscovery", doNotDiscovery);
        appendVal(sb, "retrieve", retrieve);
        appendFlag(sb, "allowNotFound", allowNotFound);
        appendFlag(sb, "readOnly", readOnly);
        appendVal(sb, "pointInTimeType", pointInTimeType);
        appendVal(sb, "staleness", staleness);
        appendVal(sb, "distinct", distinct);
        appendVal(sb, "relationalValueSearchQuery", relationalValueSearchQuery);
        appendVal(sb, "definitionProcessing", definitionProcessing);
        appendFlag(sb, "attachDiagData", attachDiagData);
        appendFlag(sb, "executionPhase", executionPhase);
        removeLastComma(sb);
    }

    public static Collection<SelectorOptions<GetOperationOptions>> fromRestOptions(List<String> options, List<String> include,
            List<String> exclude, List<String> resolveNames, DefinitionProcessingOption definitionProcessing,
            PrismContext prismContext) {
        if (CollectionUtils.isEmpty(options) && CollectionUtils.isEmpty(include) && CollectionUtils.isEmpty(exclude)
                && CollectionUtils.isEmpty(resolveNames)) {
            if (definitionProcessing != null) {
                return SelectorOptions.createCollection(GetOperationOptions.createDefinitionProcessing(definitionProcessing));
            }
            return null;
        }
        Collection<SelectorOptions<GetOperationOptions>> rv = new ArrayList<>();
        GetOperationOptions rootOptions = fromRestOptions(options, definitionProcessing);
        if (rootOptions != null) {
            rv.add(SelectorOptions.create(rootOptions));
        }
        for (ItemPath includePath : ItemPathCollectionsUtil.pathListFromStrings(include, prismContext)) {
            rv.add(SelectorOptions.create(prismContext.toUniformPath(includePath), GetOperationOptions.createRetrieve()));
        }
        for (ItemPath excludePath : ItemPathCollectionsUtil.pathListFromStrings(exclude, prismContext)) {
            rv.add(SelectorOptions.create(prismContext.toUniformPath(excludePath), GetOperationOptions.createDontRetrieve()));
        }
        for (ItemPath resolveNamesPath : ItemPathCollectionsUtil.pathListFromStrings(resolveNames, prismContext)) {
            rv.add(SelectorOptions.create(prismContext.toUniformPath(resolveNamesPath), GetOperationOptions.createResolveNames()));
        }
        // Do NOT set executionPhase here!
        return rv;
    }

    public static GetOperationOptions fromRestOptions(List<String> options, DefinitionProcessingOption definitionProcessing) {
        if (options == null || options.isEmpty()) {
            if (definitionProcessing != null) {
                return GetOperationOptions.createDefinitionProcessing(definitionProcessing);
            }
            return null;
        }

        GetOperationOptions rv = new GetOperationOptions();
        for (String option : options) {
            if (GetOperationOptionsType.F_RAW.getLocalPart().equals(option)) {
                rv.setRaw(true);
            }
            if (GetOperationOptionsType.F_NO_FETCH.getLocalPart().equals(option)) {
                rv.setNoFetch(true);
            }
            if (GetOperationOptionsType.F_NO_DISCOVERY.getLocalPart().equals(option)) {
                rv.setDoNotDiscovery(true);
            }
            if (GetOperationOptionsType.F_RESOLVE_NAMES.getLocalPart().equals(option)) {
                rv.setResolveNames(true);
            }

            // Do NOT set executionPhase here!
        }

        rv.setDefinitionProcessing(definitionProcessing);

        return rv;
    }

    @NotNull
    @SafeVarargs
    public static Collection<SelectorOptions<GetOperationOptions>> merge(PrismContext prismContext,
            Collection<SelectorOptions<GetOperationOptions>>... parts) {
        final UniformItemPath emptyPath = prismContext.emptyPath();
        Collection<SelectorOptions<GetOperationOptions>> merged = new ArrayList<>();
        for (Collection<SelectorOptions<GetOperationOptions>> part : parts) {
            for (SelectorOptions<GetOperationOptions> increment : CollectionUtils.emptyIfNull(part)) {
                if (increment != null) {        // should always be so
                    Collection<GetOperationOptions> existing = SelectorOptions.findOptionsForPath(
                            merged, increment.getItemPath(emptyPath));
                    if (existing.isEmpty()) {
                        merged.add(increment);
                    } else if (existing.size() == 1) {
                        existing.iterator().next().merge(increment.getOptions());
                    } else {
                        throw new AssertionError("More than one options for path: " + increment.getItemPath(emptyPath));
                    }
                }
            }
        }
        return merged;
    }

    @SuppressWarnings("ConstantConditions")
    public void merge(GetOperationOptions increment) {
        if (increment == null) {
            return;
        }
        if (increment.retrieve != null) {
            this.retrieve = increment.retrieve;
        }
        if (increment.resolve != null) {
            this.resolve = increment.resolve;
        }
        if (increment.resolveNames != null) {
            this.resolveNames = increment.resolveNames;
        }
        if (increment.noFetch != null) {
            this.noFetch = increment.noFetch;
        }
        if (increment.raw != null) {
            this.raw = increment.raw;
        }
        if (increment.tolerateRawData != null) {
            this.tolerateRawData = increment.tolerateRawData;
        }
        if (increment.doNotDiscovery != null) {
            this.doNotDiscovery = increment.doNotDiscovery;
        }
        if (increment.relationalValueSearchQuery != null) {
            this.relationalValueSearchQuery = increment.relationalValueSearchQuery;
        }
        if (increment.allowNotFound != null) {
            this.allowNotFound = increment.allowNotFound;
        }
        if (increment.readOnly != null) {
            this.readOnly = increment.readOnly;
        }
        if (increment.pointInTimeType != null) {
            this.pointInTimeType = increment.pointInTimeType;
        }
        if (increment.staleness != null) {
            this.staleness = increment.staleness;
        }
        if (increment.distinct != null) {
            this.distinct = increment.distinct;
        }
        if (increment.attachDiagData != null) {
            this.attachDiagData = increment.attachDiagData;
        }
        if (increment.definitionProcessing != null) {
            this.definitionProcessing = increment.definitionProcessing;
        }
        if (increment.iterationMethod != null) {
            this.iterationMethod = increment.iterationMethod;
        }
        if (increment.executionPhase != null) {
            this.executionPhase = increment.executionPhase;
        }
    }
}
