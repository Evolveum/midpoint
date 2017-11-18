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

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class SelectorOptions<T> implements Serializable, DebugDumpable {

	private ObjectSelector selector;
	private T options;

    //region Construction
	public SelectorOptions(ObjectSelector selector, T options) {
		super();
		this.selector = selector;
		this.options = options;
	}

	public SelectorOptions(T options) {
		super();
		this.selector = null;
		this.options = options;
	}

	public static <T> SelectorOptions<T> create(ItemPath path, T options) {
		return new SelectorOptions<T>(new ObjectSelector(path), options);
	}

	public static <T> SelectorOptions<T> create(QName pathQName, T options) {
		return new SelectorOptions<T>(new ObjectSelector(new ItemPath(pathQName)), options);
	}

	public static <T> SelectorOptions<T> create(T options) {
		return new SelectorOptions<T>(options);
	}

	public static <T> Collection<SelectorOptions<T>> createCollection(ItemPath path, T options) {
		Collection<SelectorOptions<T>> optionsCollection = new ArrayList<SelectorOptions<T>>(1);
		optionsCollection.add(create(path, options));
		return optionsCollection;
	}

	public static <T> Collection<SelectorOptions<T>> createCollection(QName pathQName, T options) {
		Collection<SelectorOptions<T>> optionsCollection = new ArrayList<SelectorOptions<T>>(1);
		optionsCollection.add(create(pathQName, options));
		return optionsCollection;
	}

	public static <T> Collection<SelectorOptions<T>> createCollection(T options) {
		Collection<SelectorOptions<T>> optionsCollection = new ArrayList<>(1);
		optionsCollection.add(new SelectorOptions<>(options));
		return optionsCollection;
	}

	public static <T> Collection<SelectorOptions<T>> createCollection(T options, ItemPath... paths) {
		Collection<SelectorOptions<T>> optionsCollection = new ArrayList<SelectorOptions<T>>(paths.length);
		for (ItemPath path: paths) {
			optionsCollection.add(create(path, options));
		}
		return optionsCollection;
	}

	public static <T> Collection<SelectorOptions<T>> createCollection(T options, QName... pathQNames) {
		Collection<SelectorOptions<T>> optionsCollection = new ArrayList<SelectorOptions<T>>(pathQNames.length);
		for (QName qname: pathQNames) {
			optionsCollection.add(create(qname, options));
		}
		return optionsCollection;
	}

	// modifies existing options collection, or creates a new collection
	// if options for given path exist, reuses them; or creates new ones instead
	public static <T> Collection<SelectorOptions<T>> set(Collection<SelectorOptions<T>> options, ItemPath path,
			Supplier<T> constructor, Consumer<T> setter) {
		if (options == null) {
			options = new ArrayList<>();
		}
		Collection<T> optionsForPath = findOptionsForPath(options, path);
		T option;
		if (optionsForPath.isEmpty()) {
			option = constructor.get();
			options.add(SelectorOptions.create(path, option));
		} else {
			option = optionsForPath.iterator().next();
		}
		setter.accept(option);
		return options;
	}
	//endregion

    //region Simple getters
    public ObjectSelector getSelector() {
        return selector;
    }

    public T getOptions() {
        return options;
    }
    //endregion

    //region Methods for accessing content (findRoot, hasToLoadPath, ...)
	@NotNull
	public ItemPath getItemPath() {
		return selector == null || selector.getPath() == null ? ItemPath.EMPTY_PATH : selector.getPath();
	}

	/**
	 * Returns options that apply to the "root" object. I.e. options that have null selector, null path, empty path, ...
	 * Must return 'live object' that could be modified.
	 */
	public static <T> T findRootOptions(Collection<SelectorOptions<T>> options) {
		if (options == null) {
			return null;
		}
		for (SelectorOptions<T> oooption: options) {
			if (oooption.isRoot()) {
				return oooption.getOptions();
			}
		}
		return null;
	}

	public static <T> Collection<SelectorOptions<T>> updateRootOptions(Collection<SelectorOptions<T>> options, Consumer<T> updater, Supplier<T> newValueSupplier) {
		if (options == null) {
			options = new ArrayList<>();
		}
		T rootOptions = findRootOptions(options);
		if (rootOptions == null) {
			rootOptions = newValueSupplier.get();
			options.add(new SelectorOptions<>(rootOptions));
		}
		updater.accept(rootOptions);
		return options;
	}

	/**
	 * Finds all the options for given path. TODO could there be more than one?
	 * Returns live objects that could be modified by client.
	 */
	@NotNull
	public static <T> Collection<T> findOptionsForPath(Collection<SelectorOptions<T>> options, ItemPath path) {
		if (path == null) {
			path = ItemPath.EMPTY_PATH;
		}
		Collection<T> rv = new ArrayList<>();
		for (SelectorOptions<T> oooption: CollectionUtils.emptyIfNull(options)) {
			if (oooption.getItemPath().equivalent(path)) {
				rv.add(oooption.getOptions());
			}
		}
		return rv;
	}

	public boolean isRoot() {
		return getItemPath().isEmpty();
	}

    public static boolean hasToLoadPath(QName itemName, Collection<SelectorOptions<GetOperationOptions>> options) {
        return hasToLoadPath(new ItemPath(itemName), options);
    }

    // TODO find a better way to specify this
    private static final Set<ItemPath> PATHS_NOT_RETURNED_BY_DEFAULT = new HashSet<>(Arrays.asList(
            new ItemPath(UserType.F_JPEG_PHOTO),
            new ItemPath(TaskType.F_SUBTASK),
            new ItemPath(TaskType.F_NODE_AS_OBSERVED),
            new ItemPath(TaskType.F_NEXT_RUN_START_TIMESTAMP),
            new ItemPath(TaskType.F_NEXT_RETRY_TIMESTAMP),
            new ItemPath(TaskType.F_WORKFLOW_CONTEXT, WfContextType.F_WORK_ITEM),
            new ItemPath(LookupTableType.F_ROW),
            new ItemPath(AccessCertificationCampaignType.F_CASE)));

    public static boolean hasToLoadPath(ItemPath path, Collection<SelectorOptions<GetOperationOptions>> options) {
        List<SelectorOptions<GetOperationOptions>> retrieveOptions = filterRetrieveOptions(options);
        if (retrieveOptions.isEmpty()) {
            return !ItemPath.containsEquivalent(PATHS_NOT_RETURNED_BY_DEFAULT, path);
        }

        for (SelectorOptions<GetOperationOptions> option : retrieveOptions) {
            ObjectSelector selector = option.getSelector();
            if (selector != null) {
	            ItemPath selected = selector.getPath();
	            if (!isPathInSelected(path, selected)) {
	                continue;
	            }
            }

            RetrieveOption retrieveOption = option.getOptions().getRetrieve();
            for (ItemPath notByDefault : PATHS_NOT_RETURNED_BY_DEFAULT) {
                if (notByDefault.equivalent(path)) {
                    //this one is not retrieved by default
                    switch (retrieveOption) {
                        case INCLUDE:
                            return true;
                        case EXCLUDE:
                        case DEFAULT:
                        default:
                            return false;
                    }
                }
            }

            switch (retrieveOption) {
                case EXCLUDE:
                case DEFAULT:
                    return false;
                case INCLUDE:
                default:
                    return true;
            }
        }

        return false;
    }
    
    public static boolean isExplicitlyIncluded(QName attributeName, Collection<SelectorOptions<GetOperationOptions>> options) {
    	return isExplicitlyIncluded(new ItemPath(attributeName), options);
    }
    
    public static boolean isExplicitlyIncluded(ItemPath path, Collection<SelectorOptions<GetOperationOptions>> options) {
        List<SelectorOptions<GetOperationOptions>> retrieveOptions = filterRetrieveOptions(options);
        if (retrieveOptions.isEmpty()) {
            return false;
        }

        for (SelectorOptions<GetOperationOptions> option : retrieveOptions) {
            ObjectSelector selector = option.getSelector();
            if (selector != null) {
	            ItemPath selected = selector.getPath();
	            if (!isPathInSelected(path, selected)) {
	                continue;
	            }
            }

            RetrieveOption retrieveOption = option.getOptions().getRetrieve();
            switch (retrieveOption) {
                case INCLUDE:
                    return true;
                case EXCLUDE:
                case DEFAULT:
                default:
                    return false;
            }
        }

        return false;
    }

    private static boolean isPathInSelected(ItemPath path, ItemPath selected) {
        if (selected == null || path == null) {
            return false;
        }

        if (path.isEmpty()) {
            if (selected.isEmpty()) {
                return true;
            }
        } else {
            List<ItemPathSegment> pSegments = path.getSegments();
            List<ItemPathSegment> sSegments = selected.getSegments();

            for (int i = 0; i < pSegments.size(); i++) {
                if (sSegments.size() <= i) {
                    return true;
                }
                NameItemPathSegment pSegment = (NameItemPathSegment) pSegments.get(i);
                NameItemPathSegment sSegment = (NameItemPathSegment) sSegments.get(i);

                if (!pSegment.equivalent(sSegment)) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    public static List<SelectorOptions<GetOperationOptions>> filterRetrieveOptions(
            Collection<SelectorOptions<GetOperationOptions>> options) {
        List<SelectorOptions<GetOperationOptions>> retrieveOptions = new ArrayList<>();
        if (options == null) {
            return retrieveOptions;
        }

        for (SelectorOptions<GetOperationOptions> option : options) {
            if (option.getOptions() == null || option.getOptions().getRetrieve() == null) {
                continue;
            }

            retrieveOptions.add(option);
        }

        return retrieveOptions;
    }

	public static <T> Map<T, Collection<ItemPath>> extractOptionValues(Collection<SelectorOptions<GetOperationOptions>> options, Function<GetOperationOptions, T> supplier) {
		Map<T, Collection<ItemPath>> rv = new HashMap<>();
		for (SelectorOptions<GetOperationOptions> selectorOption : CollectionUtils.emptyIfNull(options)) {
			T value = supplier.apply(selectorOption.getOptions());
			if (value != null) {
				Collection<ItemPath> itemPaths = rv.computeIfAbsent(value, t -> new HashSet<>());
				itemPaths.add(selectorOption.getItemPath());
			}
		}
		return rv;
	}

	//endregion

    //region hashCode, equals, toString
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((options == null) ? 0 : options.hashCode());
        result = prime * result + ((selector == null) ? 0 : selector.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SelectorOptions other = (SelectorOptions) obj;
        if (options == null) {
            if (other.options != null)
                return false;
        } else if (!options.equals(other.options))
            return false;
        if (selector == null) {
            if (other.selector != null)
                return false;
        } else if (!selector.equals(other.selector))
            return false;
        return true;
    }

    @Override
	public String toString() {
		return "ObjectOperationOptions(" + selector + ": " + options + ")";
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		return toString();
	}

	//endregion
}
