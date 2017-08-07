/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GetOperationOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.collections.CollectionUtils;

import javax.xml.namespace.QName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * @author semancik
 *
 */
public class GetOperationOptions extends AbstractOptions implements Serializable, Cloneable {
	private static final long serialVersionUID = 1L;

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
	 */
	private Long staleness;

	/**
	 * Should the results be made distinct.
	 * Not all providers support this option.
	 *
	 * BEWARE:
	 *  - may bring a potentially huge performance penalty
	 *  - may interfere with paging (!)
	 *
	 * So please consider this option an EXPERIMENTAL, for now.
	 */
	private Boolean distinct;

	/**
	 * Whether to attach diagnostics data to the returned object(s).
	 */
	private Boolean attachDiagData;

	private DefinitionProcessingOption definitionProcessing;

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

	// TODO exact placement of this method
	public static Collection<SelectorOptions<GetOperationOptions>> resolveItemsNamed(Object... items) {
		Collection<SelectorOptions<GetOperationOptions>> rv = new ArrayList<>(items.length);
		for (Object item : items) {
			rv.add(SelectorOptions.create(pathForItem(item), createResolve()));
		}
		return rv;
	}

	public static Collection<SelectorOptions<GetOperationOptions>> retrieveItemsNamed(Object... items) {
		Collection<SelectorOptions<GetOperationOptions>> rv = new ArrayList<>(items.length);
		for (Object item : items) {
			rv.add(SelectorOptions.create(pathForItem(item), createRetrieve()));
		}
		return rv;
	}

	protected static ItemPath pathForItem(Object item) {
		final ItemPath path;
		if (item instanceof QName) {
			path = new ItemPath((QName) item);
		} else if (item instanceof ItemPath) {
			path = ((ItemPath) item);
		} else {
			throw new IllegalArgumentException("item has to be QName or ItemPath but is " + item);
		}
		return path;
	}

	public Boolean getNoFetch() {
		return noFetch;
	}

	public void setNoFetch(Boolean noFetch) {
		this.noFetch = noFetch;
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

    // todo maybe at wrong place, but this might be quite useful
    public static Collection<SelectorOptions<GetOperationOptions>> createRetrieveNameOnlyOptions() {
        return SelectorOptions.createCollection(new ItemPath(ObjectType.F_NAME), createRetrieve(RetrieveOption.INCLUDE));
    }

    public static Collection<SelectorOptions<GetOperationOptions>> createRetrieveAttributesOptions(QName... properties) {
        Collection<SelectorOptions<GetOperationOptions>> optionsCollection = new ArrayList<>(properties.length);
        for (QName property : properties) {
            optionsCollection.add(SelectorOptions.create(new ItemPath(property), createRetrieve(RetrieveOption.INCLUDE)));
        }
        return optionsCollection;
    }

    public Boolean getResolveNames() {
		return resolveNames;
	}

	public void setResolveNames(Boolean resolveNames) {
		this.resolveNames = resolveNames;
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
	
	public Boolean getReadOnly() {
		return readOnly;
	}

	public void setReadOnly(Boolean readOnly) {
		this.readOnly = readOnly;
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
	
	/**
	 * Requirement how stale or fresh the retrieved data should be. It specifies maximum age of the value in millisecods. 
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

	public Boolean getDistinct() {
		return distinct;
	}

	public void setDistinct(Boolean distinct) {
		this.distinct = distinct;
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
	 *
	 * BEWARE:
	 *  - may bring a potentially huge performance penalty
	 *  - may interfere with paging (!)
	 *
	 * So please consider this option an EXPERIMENTAL, for now.
	 */
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

	public DefinitionProcessingOption getDefinitionProcessing() {
		return definitionProcessing;
	}

	public void setDefinitionProcessing(DefinitionProcessingOption definitionProcessing) {
		this.definitionProcessing = definitionProcessing;
	}

	public static DefinitionProcessingOption getDefinitionProcessing(GetOperationOptions options) {
		return options != null ? options.definitionProcessing : null;
	}

	/**
	 * Whether to attach diagnostics data to the returned object(s).
	 */
	public static GetOperationOptions createDefinitionProcessing(DefinitionProcessingOption value) {
		GetOperationOptions opts = new GetOperationOptions();
		opts.setDefinitionProcessing(value);
		return opts;
	}

	public RelationalValueSearchQuery getRelationalValueSearchQuery() {
		return relationalValueSearchQuery;
	}

	public void setRelationalValueSearchQuery(RelationalValueSearchQuery relationalValueSearchQuery) {
		this.relationalValueSearchQuery = relationalValueSearchQuery;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof GetOperationOptions))
			return false;
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
				Objects.equals(distinct, that.distinct);
	}

	@Override
	public int hashCode() {
		return Objects
				.hash(retrieve, resolve, resolveNames, noFetch, raw, tolerateRawData, doNotDiscovery, relationalValueSearchQuery,
						allowNotFound, readOnly, staleness, distinct);
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
        if (this.relationalValueSearchQuery != null) {
        	clone.relationalValueSearchQuery = this.relationalValueSearchQuery.clone();
        }
        return clone;
    }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("GetOperationOptions(");
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
		removeLastComma(sb);
		sb.append(")");
		return sb.toString();
	}


	public static Collection<SelectorOptions<GetOperationOptions>> fromRestOptions(List<String> options, List<String> include, List<String> exclude) {
		if (CollectionUtils.isEmpty(options) && CollectionUtils.isEmpty(include) && CollectionUtils.isEmpty(exclude)) {
			return null;
		}
		Collection<SelectorOptions<GetOperationOptions>> rv = new ArrayList<>();
		GetOperationOptions rootOptions = fromRestOptions(options);
		if (rootOptions != null) {
			rv.add(SelectorOptions.create(rootOptions));
		}
		for (ItemPath includePath : ItemPath.fromStringList(include)) {
			rv.add(SelectorOptions.create(includePath, GetOperationOptions.createRetrieve()));
		}
		for (ItemPath excludePath : ItemPath.fromStringList(exclude)) {
			rv.add(SelectorOptions.create(excludePath, GetOperationOptions.createDontRetrieve()));
		}
		return rv;
	}

	public static GetOperationOptions fromRestOptions(List<String> options) {
		if (options == null || options.isEmpty()){
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
		}

		return rv;
	}


}
