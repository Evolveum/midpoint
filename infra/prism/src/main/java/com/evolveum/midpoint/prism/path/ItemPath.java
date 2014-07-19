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
package com.evolveum.midpoint.prism.path;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * @author semancik
 *
 */
public class ItemPath implements Serializable, Cloneable {
	
	public static final QName XSD_TYPE = ItemPathType.COMPLEX_TYPE;
	public static final ItemPath EMPTY_PATH = new ItemPath();
	
	private List<ItemPathSegment> segments;
	private Map<String, String> namespaceMap;
	
	public void setNamespaceMap(Map<String, String> namespaceMap) {
		this.namespaceMap = namespaceMap;
	}
	
	public Map<String, String> getNamespaceMap() {
		return namespaceMap;
	}

	public ItemPath() {
		segments = new ArrayList<ItemPathSegment>(0);
	}
		
	public ItemPath(QName... qnames) {
		this.segments = new ArrayList<ItemPathSegment>(qnames.length);
		for (QName qname : qnames) {
			add(qname);
		}
	}
	
	public ItemPath(ItemPath parentPath, QName subName) {
		this.segments = new ArrayList<ItemPathSegment>(parentPath.segments.size()+1);
		segments.addAll(parentPath.segments);
		add(subName);
	}

	
	public ItemPath(List<ItemPathSegment> segments) {
		this.segments = new ArrayList<ItemPathSegment>(segments.size());
		this.segments.addAll(segments);
	}
			
	public ItemPath(List<ItemPathSegment> segments, ItemPathSegment subSegment) {
		this.segments = new ArrayList<ItemPathSegment>(segments.size()+1);
		this.segments.addAll(segments);
		this.segments.add(subSegment);
	}
	
	public ItemPath(List<ItemPathSegment> segments, QName subName) {
		this.segments = new ArrayList<ItemPathSegment>(segments.size()+1);
		this.segments.addAll(segments);
		add(subName);
	}
	
	public ItemPath(ItemPathSegment... segments) {
		this.segments = new ArrayList<ItemPathSegment>(segments.length);
		for (ItemPathSegment seg : segments) {
			this.segments.add(seg);
		}
	}
	
	public ItemPath(ItemPath parentPath, ItemPathSegment subSegment) {
		this.segments = new ArrayList<ItemPathSegment>(parentPath.segments.size()+1);
		this.segments.addAll(parentPath.segments);
		this.segments.add(subSegment);
	}
	
	public ItemPath subPath(QName subName) {
		return new ItemPath(segments, subName);
	}
	
	public ItemPath subPath(ItemPathSegment subSegment) {
		return new ItemPath(segments, subSegment);
	}

	public ItemPath subPath(ItemPath subPath) {
		ItemPath newPath = new ItemPath(segments);
		newPath.segments.addAll(subPath.getSegments());
		return newPath;
	}
	
	/**
	 * Null-proof static version. 
	 */
	public static ItemPath subPath(ItemPath prefix, ItemPathSegment subSegment) {
		if (prefix == null && subSegment == null) {
			return EMPTY_PATH;
		}
		if (prefix == null) {
			return new ItemPath(subSegment);
		}
		return prefix.subPath(subSegment);
	}

	private void add(QName qname) {
		this.segments.add(new NameItemPathSegment(qname));
	}
		
	public List<ItemPathSegment> getSegments() {
		return segments;
	}
	
	public ItemPathSegment first() {
		if (segments.size() == 0) {
			return null;
		}
		return segments.get(0);
	}

	public ItemPath rest() {
		if (segments.size() == 0) {
			return EMPTY_PATH;
		}
		return new ItemPath(segments.subList(1, segments.size()));
	}

    public NameItemPathSegment lastNamed() {
        for (int i = segments.size()-1; i >= 0; i--) {
            if (segments.get(i) instanceof NameItemPathSegment) {
                return (NameItemPathSegment) segments.get(i);
            }
        }
        return null;
    }
	
	public ItemPathSegment last() {
		if (segments.size() == 0) {
			return null;
		}
		return segments.get(segments.size()-1);
	}

	/**
	 * Returns first segment in a form of path.
	 */
	public ItemPath head() {
		return new ItemPath(first());
	}
	
	/**
	 * Returns path containinig all segments except the first.
	 */
	public ItemPath tail() {
		if (segments.size() == 0) {
			return EMPTY_PATH;
		}
		return new ItemPath(segments.subList(1, segments.size()));
	}

	/**
	 * Returns a path containing all segments except the last one.
	 */
	public ItemPath allExceptLast() {
		if (segments.size() == 0) {
			return EMPTY_PATH;
		}
		return new ItemPath(segments.subList(0, segments.size()-1));
	}

    /**
     * Returns a path containing all segments up to (and not including) the last one.
     */
    public ItemPath allUpToLastNamed() {
        for (int i = segments.size()-1; i >= 0; i--) {
            if (segments.get(i) instanceof NameItemPathSegment) {
                return new ItemPath(segments.subList(0, i));
            }
        }
        return EMPTY_PATH;
    }

    /**
     * Returns a path containing all segments up to (not including) the specified one;
     * counted from backwards.
     * If the segment is not present, returns empty path.
     */
    public ItemPath allUpTo(ItemPathSegment segment) {
        int i = segments.lastIndexOf(segment);
        if (i < 0) {
            return EMPTY_PATH;
        } else {
            return new ItemPath(segments.subList(0, i-1));
        }
    }

    public int size() {
		return segments.size();
	}

	public boolean isEmpty() {
		return segments.isEmpty();
	}
	
	/**
	 * Makes the path "normal" by inserting null Id segments where they were omitted. 
	 */
	public ItemPath normalize() {
		ItemPath normalizedPath = new ItemPath();
		ItemPathSegment lastSegment = null;
		Iterator<ItemPathSegment> iterator = segments.iterator();
		while (iterator.hasNext()) {
			ItemPathSegment origSegment = iterator.next();
			if (lastSegment != null && !(lastSegment instanceof IdItemPathSegment) && 
					!(origSegment instanceof IdItemPathSegment)) {
				normalizedPath.segments.add(new IdItemPathSegment());
			}
			normalizedPath.segments.add(origSegment);
			lastSegment = origSegment;
		}
		if (lastSegment != null && !(lastSegment instanceof IdItemPathSegment) &&
				// Make sure we do not insert the Id segment as a last one. That is not correct and it would spoil comparing paths
				iterator.hasNext()) {
			normalizedPath.segments.add(new IdItemPathSegment());
		}
		return normalizedPath;
	}
	
	public CompareResult compareComplex(ItemPath otherPath) {
		ItemPath thisNormalized = this.normalize();
		ItemPath otherNormalized = otherPath == null ? EMPTY_PATH : otherPath.normalize();
		int i = 0;
		while (i < thisNormalized.segments.size() && i < otherNormalized.segments.size()) {
			ItemPathSegment thisSegment = thisNormalized.segments.get(i);
			ItemPathSegment otherSegment = otherNormalized.segments.get(i);
			if (!thisSegment.equivalent(otherSegment)) {
				return CompareResult.NO_RELATION;
			}
			i++;
		}
		if (i < thisNormalized.size()) {
			return CompareResult.SUPERPATH;
		}
		if (i < otherNormalized.size()) {
			return CompareResult.SUBPATH;
		}
		return CompareResult.EQUIVALENT;
	}

    public static boolean containsEquivalent(Collection<ItemPath> paths, ItemPath pathToBeFound) {
        for (ItemPath path : paths) {
            if (path.equivalent(pathToBeFound)) {
                return true;
            }
        }
        return false;
    }

    public enum CompareResult {
		EQUIVALENT,
		SUPERPATH,
		SUBPATH,
		NO_RELATION;
	}
	
	public boolean isSubPath(ItemPath otherPath) {
		return compareComplex(otherPath) == CompareResult.SUBPATH;
	}
	
	public boolean isSuperPath(ItemPath otherPath) {
		return compareComplex(otherPath) == CompareResult.SUPERPATH;
	}

    public boolean isSubPathOrEquivalent(ItemPath otherPath) {
        CompareResult result = compareComplex(otherPath);
        return result == CompareResult.SUBPATH || result == CompareResult.EQUIVALENT;
    }

    /**
     * Compares two paths semantically.
     *
     * @param otherPath
     * @return
     */
    public boolean equivalent(ItemPath otherPath) {
		return compareComplex(otherPath) == CompareResult.EQUIVALENT;
	}

	public ItemPath substract(ItemPath otherPath) {
//        return remainder(otherPath);                        // the code seems to be equivalent to the one of remainder()
		ItemPath thisNormalized = this.normalize();
		ItemPath otherNormalized = otherPath.normalize();
		if (thisNormalized.size() < otherNormalized.size()) {
			throw new IllegalArgumentException("Cannot substract path '"+otherPath+"' from '"+this+"' because it is not a subset");
		}
		int i = 0;
		while (i < otherNormalized.segments.size()) {
			ItemPathSegment thisSegment = thisNormalized.segments.get(i);
			ItemPathSegment otherSegment = otherNormalized.segments.get(i);
			if (!thisSegment.equivalent(otherSegment)) {
				throw new IllegalArgumentException("Cannot subtract segment '"+otherSegment+"' from path '"+this+
						"' because it does not contain corresponding segment; it has '"+thisSegment+"' instead.");
			}
			i++;
		}
		List<ItemPathSegment> substractSegments = thisNormalized.segments.subList(i, thisNormalized.segments.size());
		return new ItemPath(substractSegments);
	}

    /**
     * Returns the remainder of "this" path after passing all segments from the other path.
     * (I.e. this path must begin with the content of the other path. Throws an exception when
     * it is not the case.)
     *
     * @param otherPath
     * @return
     */
	public ItemPath remainder(ItemPath otherPath) {
		ItemPath thisNormalized = this.normalize();
		ItemPath otherNormalized = otherPath.normalize();
		if (thisNormalized.size() < otherNormalized.size()) {
			throw new IllegalArgumentException("Cannot compute remainder of path '"+this+"' after '"+otherPath+"' because this path is not a superset");
		}
		int i = 0;
		while (i < otherNormalized.segments.size()) {
			ItemPathSegment thisSegment = thisNormalized.segments.get(i);
			ItemPathSegment otherSegment = otherNormalized.segments.get(i);
			if (!thisSegment.equivalent(otherSegment)) {
				throw new IllegalArgumentException("Cannot subtract segment '"+otherSegment+"' from path '"+this+
						"' because it does not contain corresponding segment; it has '"+thisSegment+"' instead.");
			}
			i++;
		}
		List<ItemPathSegment> substractSegments = thisNormalized.segments.subList(i, thisNormalized.segments.size());
		return new ItemPath(substractSegments);
	}
	
	/**
	 * Convenience static method with checks
	 * @throw IllegalArgumentException
	 */
	public static QName getName(ItemPathSegment segment) {
		if (segment == null) {
			return null;
		}
		if (!(segment instanceof NameItemPathSegment)) {
			throw new IllegalArgumentException("Unable to get name from non-name path segment "+segment);
		}
		return ((NameItemPathSegment)segment).getName();
	}
	
	public static IdItemPathSegment getFirstIdSegment(ItemPath itemPath) {
		ItemPathSegment first = itemPath.first();
		if (first instanceof IdItemPathSegment) {
			return (IdItemPathSegment)first;
		}
		return null;
	}
	
	public static ItemPath pathRestStartingWithName(ItemPath path) {
    	ItemPathSegment pathSegment = path.first();
    	if (pathSegment instanceof NameItemPathSegment) {
    		return path;
    	} else if (pathSegment instanceof IdItemPathSegment) {
    		return path.rest();
    	} else {
    		throw new IllegalArgumentException("Unexpected path segment "+pathSegment);
    	}
    }

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		Iterator<ItemPathSegment> iterator = segments.iterator();
		while (iterator.hasNext()) {
			sb.append(iterator.next());
			if (iterator.hasNext()) {
				sb.append("/");
			}
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((segments == null) ? 0 : segments.hashCode());
		return result;
	}

    /**
     * More strict version of ItemPath comparison. Does not use any normalization
     * nor approximate matching QNames via QNameUtil.match.
     *
     * For semantic-level comparison, please use equivalent(..) method.
     *
     * @param obj
     * @return
     */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ItemPath other = (ItemPath) obj;
		if (segments == null) {
			if (other.segments != null)
				return false;
		} else if (!segments.equals(other.segments))
			return false;
		return true;
	}

    public ItemPath clone() {
        ItemPath clone = new ItemPath();
        for (ItemPathSegment segment : segments) {
            clone.segments.add(segment.clone());
        }
        if (namespaceMap != null) {
            clone.namespaceMap = new HashMap<>(namespaceMap);
        }
        return clone;
    }

}
