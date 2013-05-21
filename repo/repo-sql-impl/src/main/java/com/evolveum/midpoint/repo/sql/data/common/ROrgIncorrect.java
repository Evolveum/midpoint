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

package com.evolveum.midpoint.repo.sql.data.common;

import java.io.Serializable;

import javax.persistence.*;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

/**
 * @author lazyman
 */
@Entity
@Table(name = "m_org_incorrect")
public class ROrgIncorrect implements Serializable {

	private String ancestorOid;
	private String descendantOid;
	private Long descendantId;

	public ROrgIncorrect() {

	}

	public ROrgIncorrect(String ancestorOid, String descendantOid, Long descendantId) {
		this.ancestorOid = ancestorOid;
		this.descendantOid = descendantOid;
		this.descendantId = descendantId;
	}
	
	@Id
	@Column(name = "ancestor_oid", nullable = false, updatable = false, length = 36)
	public String getAncestorOid() {
		return ancestorOid;
	}

	public void setAncestorOid(String ancestorOid) {
		this.ancestorOid = ancestorOid;
	}
	
	@Id
	@Column(name = "descendant_oid", nullable = false, updatable = false, length = 36)
	public String getDescendantOid() {
		return descendantOid;
	}

	public void setDescendantOid(String descendantOid) {
		this.descendantOid = descendantOid;
	}

	@Id
	@Column(name = "descendant_id")
	public Long getDescendantId() {
		return descendantId;
	}

	public void setDescendantId(Long descendantId) {
		this.descendantId = descendantId;
	}

	@Override
	public int hashCode() {
		int result = ancestorOid != null ? ancestorOid.hashCode() : 0;
		result = 31 * result + (descendantOid != null ? descendantOid.hashCode() : 0);
		result = 31 * result + (descendantId != null ? descendantId.hashCode() : 0);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;

		ROrgIncorrect that = (ROrgIncorrect) obj;	

		if (ancestorOid != null ? !ancestorOid.equals(that.ancestorOid) : that.ancestorOid != null)
			return false;
		if (descendantOid != null ? !descendantOid.equals(that.descendantOid) : that.descendantOid != null)
			return false;
		if (descendantId != null ? !descendantId.equals(that.descendantId) : that.descendantId != null)
			return false;
		
		return true;
	}
}
