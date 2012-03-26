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
package com.evolveum.midpoint.prism;

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
public class PropertyPath implements Serializable {
	
	public static final PropertyPath EMPTY_PATH = new PropertyPath();
	
	private List<PropertyPathSegment> segments;

	public PropertyPath() {
		segments = new ArrayList<PropertyPathSegment>(0);
	}
	
//	public PropertyPath(List<QName> qnames) {
//		this.segments = new ArrayList<PropertyPathSegment>(qnames.size());
//		addAll(qnames);
//	}
			
//	public PropertyPath(List<QName> qnames, QName subName) {
//		this.segments = new ArrayList<PropertyPathSegment>(qnames.size()+1);
//		addAll(qnames);
//		add(subName);
//	}
	
	public PropertyPath(QName... qnames) {
		this.segments = new ArrayList<PropertyPathSegment>(qnames.length);
		for (QName qname : qnames) {
			add(qname);
		}
	}
	
	public PropertyPath(PropertyPath parentPath, QName subName) {
		this.segments = new ArrayList<PropertyPathSegment>(parentPath.segments.size()+1);
		segments.addAll(parentPath.segments);
		add(subName);
	}

	
	public PropertyPath(List<PropertyPathSegment> segments) {
		this.segments = new ArrayList<PropertyPathSegment>(segments.size());
		this.segments.addAll(segments);
	}
			
	public PropertyPath(List<PropertyPathSegment> segments, PropertyPathSegment subSegment) {
		this.segments = new ArrayList<PropertyPathSegment>(segments.size()+1);
		this.segments.addAll(segments);
		this.segments.add(subSegment);
	}
	
	public PropertyPath(List<PropertyPathSegment> segments, QName subName) {
		this.segments = new ArrayList<PropertyPathSegment>(segments.size()+1);
		this.segments.addAll(segments);
		add(subName);
	}
	
	public PropertyPath(PropertyPathSegment... segments) {
		this.segments = new ArrayList<PropertyPathSegment>(segments.length);
		for (PropertyPathSegment seg : segments) {
			this.segments.add(seg);
		}
	}
	
	public PropertyPath(PropertyPath parentPath, PropertyPathSegment subSegment) {
		this.segments = new ArrayList<PropertyPathSegment>(parentPath.segments.size()+1);
		this.segments.addAll(parentPath.segments);
		this.segments.add(subSegment);
	}

	public PropertyPath subPath(QName subName) {
		return new PropertyPath(segments, subName);
	}
	
	public PropertyPath subPath(PropertyPathSegment subSegment) {
		return new PropertyPath(segments, subSegment);
	}
	
//	private void addAll(List<QName> qnames) {
//		for (QName qname: qnames) {
//			add(qname);
//		}
//	}
	
	private void add(QName qname) {
		this.segments.add(new PropertyPathSegment(qname));
	}
		
	public List<PropertyPathSegment> getSegments() {
		return segments;
	}
	
	public PropertyPathSegment first() {
		if (segments.size() == 0) {
			return null;
		}
		return segments.get(0);
	}

	public PropertyPath rest() {
		if (segments.size() == 0) {
			return EMPTY_PATH;
		}
		return new PropertyPath(segments.subList(1, segments.size()));
	}
	
	public PropertyPathSegment last() {
		if (segments.size() == 0) {
			return null;
		}
		return segments.get(segments.size()-1);
	}

	public PropertyPath allExceptLast() {
		if (segments.size() == 0) {
			return EMPTY_PATH;
		}
		return new PropertyPath(segments.subList(0, segments.size()-1));
	}
	
	public int size() {
		return segments.size();
	}

	public boolean isEmpty() {
		return segments.isEmpty();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		Iterator<PropertyPathSegment> iterator = segments.iterator();
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
		PropertyPath other = (PropertyPath) obj;
		if (segments == null) {
			if (other.segments != null)
				return false;
		} else if (!segments.equals(other.segments))
			return false;
		return true;
	}
	
}
