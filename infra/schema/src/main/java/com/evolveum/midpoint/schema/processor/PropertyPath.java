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
package com.evolveum.midpoint.schema.processor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.holder.XPathSegment;
import com.evolveum.midpoint.schema.util.DebugUtil;

/**
 * @author semancik
 *
 */
public class PropertyPath {
	
	public static final PropertyPath EMPTY_PATH = new PropertyPath();
	
	private List<QName> qnames;

	public PropertyPath() {
		qnames = new ArrayList<QName>(0);
	}
	
	public PropertyPath(List<QName> qnames) {
		this.qnames = new ArrayList<QName>(qnames.size());
		this.qnames.addAll(qnames);
	}
	
	public PropertyPath(List<QName> qnames, QName subName) {
		this.qnames = new ArrayList<QName>(qnames.size()+1);
		this.qnames.addAll(qnames);
		this.qnames.add(subName);
	}
	
	public PropertyPath(QName... segments) {
		this.qnames = new ArrayList<QName>(segments.length);
		for (QName segment : segments) {
			this.qnames.add(segment);
		}
	}
	
	public PropertyPath(PropertyPath parentPath, QName subName) {
		this.qnames = new ArrayList<QName>(parentPath.qnames.size()+1);
		qnames.addAll(parentPath.qnames);
		qnames.add(subName);
	}

	public PropertyPath(XPathHolder xpath) {
		List<XPathSegment> segments = xpath.toSegments();
		qnames = new ArrayList<QName>(segments.size());
		for (XPathSegment segment : segments) {
			qnames.add(segment.getQName());
		}
	}

	public PropertyPath subPath(QName subName) {
		return new PropertyPath(qnames,subName);
	}
	
	public List<QName> getSegments() {
		return qnames;
	}
	
	public QName first() {
		return qnames.get(0);
	}

	public PropertyPath rest() {
		return new PropertyPath(qnames.subList(1, qnames.size()));
	}
	
	public QName last() {
		return qnames.get(qnames.size()-1);
	}

	public PropertyPath allExceptLast() {
		return new PropertyPath(qnames.subList(0, qnames.size()-1));
	}
	
	public int size() {
		return qnames.size();
	}

	public boolean isEmpty() {
		return qnames.isEmpty();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		Iterator<QName> iterator = qnames.iterator();
		while (iterator.hasNext()) {
			sb.append(DebugUtil.prettyPrint(iterator.next()));
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
		result = prime * result + ((qnames == null) ? 0 : qnames.hashCode());
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
		if (qnames == null) {
			if (other.qnames != null)
				return false;
		} else if (!qnames.equals(other.qnames))
			return false;
		return true;
	}
	
}
