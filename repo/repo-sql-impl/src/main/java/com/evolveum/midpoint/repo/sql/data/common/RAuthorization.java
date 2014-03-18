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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.Transient;

import org.apache.commons.lang.Validate;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.sql.data.common.enums.RAuthorizationDecision;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;

/**
 * @author lazyman
 */
@JaxbType(type = AuthorizationType.class)
@Entity
@ForeignKey(name = "fk_authorization")
public class RAuthorization extends RContainer implements ROwnable {

	public static final String F_OWNER = "owner";

	// owner
	private RObject owner;
	private String ownerOid;
	private Long ownerId;
	// actual data
	private String description;
	private RAuthorizationDecision decision;
	private Set<String> action;

	private String objectSpecification;

	public RAuthorization() {
		this(null);
	}

	public RAuthorization(RObject owner) {
		this.owner = owner;
	}

	@ForeignKey(name = "fk_authorization_owner")
	@MapsId("owner")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumns({
			@JoinColumn(name = "owner_oid", referencedColumnName = "oid"),
			@JoinColumn(name = "owner_id", referencedColumnName = "id") })
	public RObject getOwner() {
		return owner;
	}

	@Column(name = "owner_id", nullable = false)
	public Long getOwnerId() {
		if (ownerId == null && owner != null) {
			ownerId = owner.getId();
		}
		return ownerId;
	}

	@Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID, nullable = false)
	public String getOwnerOid() {
		if (ownerOid == null && owner != null) {
			ownerOid = owner.getOid();
		}
		return ownerOid;
	}

	@ElementCollection
	@ForeignKey(name = "fk_authorization_action")
	@CollectionTable(name = "m_authorization_action", joinColumns = {
			@JoinColumn(name = "role_oid", referencedColumnName = "oid"),
			@JoinColumn(name = "role_id", referencedColumnName = "id") })
	@Cascade({ org.hibernate.annotations.CascadeType.ALL })
	public Set<String> getAction() {
		return action;
	}

	@Enumerated(EnumType.ORDINAL)
	public RAuthorizationDecision getDecision() {
		return decision;
	}

	@Lob
	@Type(type = RUtil.LOB_STRING_TYPE)
	public String getDescription() {
		return description;
	}

	@Transient
	@Override
	public RContainer getContainerOwner() {
		return getOwner();
	}

	public void setAction(Set<String> action) {
		this.action = action;
	}

	public void setDecision(RAuthorizationDecision decision) {
		this.decision = decision;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setOwner(RObject owner) {
		this.owner = owner;
	}

	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	public void setOwnerOid(String ownerOid) {
		this.ownerOid = ownerOid;
	}

	@Lob
	@Type(type = RUtil.LOB_STRING_TYPE)
	public String getObjectSpecification() {
		return objectSpecification;
	}

	public void setObjectSpecification(String objectSpecification) {
		this.objectSpecification = objectSpecification;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		if (!super.equals(o))
			return false;

		RAuthorization that = (RAuthorization) o;

		if (action != null ? !action.equals(that.action) : that.action != null)
			return false;
		if (decision != that.decision)
			return false;
		if (description != null ? !description.equals(that.description)
				: that.description != null)
			return false;
		if (objectSpecification != null ? !objectSpecification
				.equals(that.objectSpecification)
				: that.objectSpecification != null)
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result
				+ (description != null ? description.hashCode() : 0);
		result = 31 * result + (decision != null ? decision.hashCode() : 0);
		result = 31
				* result
				+ (objectSpecification != null ? objectSpecification.hashCode()
						: 0);
		return result;
	}

	public static void copyToJAXB(RAuthorization repo, AuthorizationType jaxb,
			PrismContext prismContext) throws DtoTranslationException {
		Validate.notNull(repo, "Repo object must not be null.");
		Validate.notNull(jaxb, "JAXB object must not be null.");

		jaxb.setId(repo.getId());
		jaxb.setDescription(repo.getDescription());
		if (repo.getDecision() != null) {
			jaxb.setDecision(repo.getDecision().getSchemaValue());
		}

		List types = RUtil.safeSetToList(repo.getAction());
		if (!types.isEmpty()) {
			jaxb.getAction().addAll(types);
		}
		if (repo.getObjectSpecification() != null) {
			try {
				AuthorizationType objectSpecification = RUtil.toJAXB(RoleType.class, new ItemPath(
						RoleType.F_AUTHORIZATION), repo.getObjectSpecification(), AuthorizationType.class,
						prismContext);
				PrismContainerValue jaxbContainer = jaxb.asPrismContainerValue();

				Collection cloned = RUtil.cloneValuesToJaxb(objectSpecification, SchemaConstants.C_ITEM);
				if (cloned != null) {
					jaxbContainer.findOrCreateProperty(SchemaConstants.C_ITEM).addAll(cloned);
				}
				cloned = RUtil.cloneValuesToJaxb(objectSpecification, AuthorizationType.F_OBJECT);
				if (cloned != null) {
					jaxbContainer.findOrCreateContainer(AuthorizationType.F_OBJECT).addAll(cloned);
				}
				cloned = RUtil.cloneValuesToJaxb(objectSpecification, AuthorizationType.F_TARGET);
				if (cloned != null) {
					jaxbContainer.findOrCreateContainer(AuthorizationType.F_TARGET).addAll(cloned);
				}

			} catch (Exception ex) {
				throw new DtoTranslationException(ex.getMessage(), ex);
			}
		}
	}

	public static void copyFromJAXB(AuthorizationType jaxb, RAuthorization repo, ObjectType parent,
                                    PrismContext prismContext) throws DtoTranslationException {
        Validate.notNull(repo, "Repo object must not be null.");
        Validate.notNull(jaxb, "JAXB object must not be null.");

        repo.setOid(parent.getOid());
        repo.setId(jaxb.getId());
        repo.setDescription(jaxb.getDescription());

        repo.setDecision(RUtil.getRepoEnumValue(jaxb.getDecision(), RAuthorizationDecision.class));
        repo.setAction(RUtil.listToSet(jaxb.getAction()));
//		  object, item, target - set in new authorization type
		AuthorizationType objectSpecification = new AuthorizationType();
		PrismContainerValue<AuthorizationType> objSpecCont = objectSpecification.asPrismContainerValue();
		try {
			objSpecCont.addReplaceExisting(RUtil.cloneValuesFromJaxb(jaxb, AuthorizationType.F_OBJECT));
			objSpecCont.addReplaceExisting(RUtil.cloneValuesFromJaxb(jaxb, AuthorizationType.F_TARGET));
			objSpecCont.addReplaceExisting(RUtil.cloneValuesFromJaxb(jaxb, SchemaConstants.C_ITEM));
			Item i = parent.asPrismObject().findItem(RoleType.F_AUTHORIZATION);
			objSpecCont.setParent(i);

			repo.setObjectSpecification(RUtil.toRepo(objectSpecification, prismContext));
		} catch (Exception ex) {
			throw new DtoTranslationException(ex.getMessage(), ex);
        }
      
    }

	public AuthorizationType toJAXB(PrismContext prismContext)
			throws DtoTranslationException {
		AuthorizationType object = new AuthorizationType();
		RAuthorization.copyToJAXB(this, object, prismContext);
		return object;
	}
}
