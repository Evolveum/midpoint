/*
 * Copyright (c) 2010-2013 Evolveum
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public class SelectorOptions<T> implements Serializable {
	
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
		Collection<SelectorOptions<T>> optionsCollection = new ArrayList<SelectorOptions<T>>(1);
		optionsCollection.add(new SelectorOptions<T>(options));
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
    /**
	 * Returns options that apply to the "root" object. I.e. options that have null selector, null path, empty path, ...
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

	private boolean isRoot() {
		if (selector == null) {
			return true;
		}
		if (selector.getPath() == null || selector.getPath().isEmpty()) {
			return true;
		}
		return false;
	}

    public static boolean hasToLoadPath(QName container, Collection<SelectorOptions<GetOperationOptions>> options) {
        return hasToLoadPath(new ItemPath(container), options);
    }

    // TODO find a better way to specify this
    private static final Set<ItemPath> PATHS_NOT_RETURNED_BY_DEFAULT = new HashSet<>(Arrays.asList(
            new ItemPath(UserType.F_JPEG_PHOTO),
            new ItemPath(TaskType.F_SUBTASK),
            new ItemPath(TaskType.F_NODE_AS_OBSERVED),
            new ItemPath(TaskType.F_NEXT_RUN_START_TIMESTAMP)));

    public static boolean hasToLoadPath(ItemPath path, Collection<SelectorOptions<GetOperationOptions>> options) {
        List<SelectorOptions<GetOperationOptions>> retrieveOptions = filterRetrieveOptions(options);
        if (retrieveOptions.isEmpty()) {
            return !ItemPath.containsEquivalent(PATHS_NOT_RETURNED_BY_DEFAULT, path);
        }

        for (SelectorOptions<GetOperationOptions> option : retrieveOptions) {
            ObjectSelector selector = option.getSelector();
            ItemPath selected = selector.getPath();
            if (!isPathInSelected(path, selected)) {
                continue;
            }

            RetrieveOption retrieveOption = option.getOptions().getRetrieve();
            if (new ItemPath(UserType.F_JPEG_PHOTO).equivalent(path)) {
                //user photo is not retrieved by default
                switch (retrieveOption) {
                    case INCLUDE:
                        return true;
                    case EXCLUDE:
                    case DEFAULT:
                    default:
                        return false;
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
    //endregion
}
