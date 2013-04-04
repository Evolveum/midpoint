/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.prism.path;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugUtil;

/**
 * @author semancik
 *
 */
public class ItemPath implements Serializable {
	
	public static final ItemPath EMPTY_PATH = new ItemPath();
	
	private List<ItemPathSegment> segments;

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
	
	public int size() {
		return segments.size();
	}

	public boolean isEmpty() {
		return segments.isEmpty();
	}
	
	public ItemPath normalize() {
		ItemPath normalizedPath = new ItemPath();
		ItemPathSegment lastSegment = null;
		for (ItemPathSegment origSegment: segments) {
			if (lastSegment != null && !(lastSegment instanceof IdItemPathSegment) && 
					!(origSegment instanceof IdItemPathSegment)) {
				normalizedPath.segments.add(new IdItemPathSegment());
			}
			normalizedPath.segments.add(origSegment);
			lastSegment = origSegment;
		}
		if (lastSegment != null && !(lastSegment instanceof IdItemPathSegment)) {
			normalizedPath.segments.add(new IdItemPathSegment());
		}
		return normalizedPath;
	}
	
	public CompareResult compareComplex(ItemPath otherPath) {
		ItemPath thisNormalized = this.normalize();
		ItemPath otherNormalized = otherPath.normalize();
		int i = 0;
		while (i < thisNormalized.segments.size() && i < otherNormalized.segments.size()) {
			ItemPathSegment thisSegment = thisNormalized.segments.get(i);
			ItemPathSegment otherSegment = otherNormalized.segments.get(i);
			if (!thisSegment.equals(otherSegment)) {
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
	
	public boolean equivalent(ItemPath otherPath) {
		return compareComplex(otherPath) == CompareResult.EQUIVALENT;
	}
	
	public ItemPath substract(ItemPath otherPath) {
		ItemPath thisNormalized = this.normalize();
		ItemPath otherNormalized = otherPath.normalize();
		if (thisNormalized.size() < otherNormalized.size()) {
			throw new IllegalArgumentException("Cannot substract path '"+otherPath+"' from '"+this+"' because it is not a subset");
		}
		int i = 0;
		while (i < otherNormalized.segments.size()) {
			ItemPathSegment thisSegment = thisNormalized.segments.get(i);
			ItemPathSegment otherSegment = otherNormalized.segments.get(i);
			if (!thisSegment.equals(otherSegment)) {
				throw new IllegalArgumentException("Cannot substract segment '"+otherSegment+"' from path '"+this+
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
	
}
