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

package com.evolveum.midpoint.repo.sql.data.common.container;

import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.enums.ROperationResultStatus;
import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbType;
import com.evolveum.midpoint.repo.sql.query.definition.OwnerIdGetter;
import com.evolveum.midpoint.repo.sql.query.definition.QueryEntity;
import com.evolveum.midpoint.repo.sql.query2.definition.IdQueryProperty;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointSingleTablePersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationExecutionType;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Persister;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Objects;

/**
 * @author mederly
 */
@JaxbType(type = OperationExecutionType.class)
@Entity
@QueryEntity
@IdClass(RContainerId.class)
@Table(name = "m_operation_execution", indexes = {
        @Index(name = "iOpExecTaskOid", columnList = "taskRef_targetOid"),
        @Index(name = "iOpExecInitiatorOid", columnList = "initiatorRef_targetOid"),
        @Index(name = "iOpExecStatus", columnList = "status")})
@Persister(impl = MidPointSingleTablePersister.class)
public class ROperationExecution implements Container<RObject<?>> {

    public static final String F_OWNER = "owner";

    private static final Trace LOGGER = TraceManager.getTrace(ROperationExecution.class);

    private Boolean trans;

    private RObject<?> owner;
    private String ownerOid;
    private Integer id;

    private REmbeddedReference initiatorRef;
    private REmbeddedReference taskRef;
    private ROperationResultStatus status;
    private XMLGregorianCalendar timestamp;

    public ROperationExecution() {
        this(null);
    }

    public ROperationExecution(RObject<?> owner) {
        this.setOwner(owner);
    }

    @Id
    @org.hibernate.annotations.ForeignKey(name = "fk_op_exec_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
	@Override
    public RObject<?> getOwner() {
        return owner;
    }

	@Override
	public void setOwner(RObject owner) {
		this.owner = owner;
	}

	@Column(name = "owner_oid", length = RUtil.COLUMN_LENGTH_OID, nullable = false)
    @OwnerIdGetter()
	@Override
    public String getOwnerOid() {
        if (owner != null && ownerOid == null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

	public void setOwnerOid(String ownerOid) {
		this.ownerOid = ownerOid;
	}

	@Id
    @GeneratedValue(generator = "ContainerIdGenerator")
    @GenericGenerator(name = "ContainerIdGenerator", strategy = "com.evolveum.midpoint.repo.sql.util.ContainerIdGenerator")
    @Column(name = "id")
    @IdQueryProperty
    public Integer getId() {
        return id;
    }

	public void setId(Integer id) {
		this.id = id;
	}

	@Embedded
    public REmbeddedReference getInitiatorRef() {
        return initiatorRef;
    }

	public void setInitiatorRef(REmbeddedReference initiatorRef) {
		this.initiatorRef = initiatorRef;
	}

	@Embedded
    public REmbeddedReference getTaskRef() {
        return taskRef;
    }

	public void setTaskRef(REmbeddedReference taskRef) {
		this.taskRef = taskRef;
	}

	public ROperationResultStatus getStatus() {
		return status;
	}

	public void setStatus(ROperationResultStatus status) {
		this.status = status;
	}

	@Column(name = "timestampValue")
	public XMLGregorianCalendar getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(XMLGregorianCalendar timestamp) {
		this.timestamp = timestamp;
	}

	@Transient
    @Override
    public Boolean isTransient() {
        return trans;
    }

    @Override
    public void setTransient(Boolean trans) {
        this.trans = trans;
    }

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof ROperationExecution))
			return false;
		ROperationExecution that = (ROperationExecution) o;
		return Objects.equals(trans, that.trans) &&
				Objects.equals(getOwnerOid(), that.getOwnerOid()) &&
				Objects.equals(id, that.id) &&
				Objects.equals(initiatorRef, that.initiatorRef) &&
				Objects.equals(taskRef, that.taskRef) &&
				Objects.equals(timestamp, that.timestamp) &&
				status == that.status;
	}

	@Override
	public int hashCode() {
		return Objects.hash(trans, getOwnerOid(), id, initiatorRef, taskRef, status);
	}

	public static void copyFromJAXB(@NotNull OperationExecutionType jaxb, @NotNull ROperationExecution repo,
			ObjectType parent, RepositoryContext repositoryContext,
			IdGeneratorResult generatorResult) throws DtoTranslationException {

        repo.setTransient(generatorResult.isTransient(jaxb.asPrismContainerValue()));

        repo.setOwnerOid(parent.getOid());
        repo.setId(RUtil.toInteger(jaxb.getId()));
		repo.setTaskRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getTaskRef(), repositoryContext.prismContext));
		repo.setInitiatorRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getInitiatorRef(), repositoryContext.prismContext));
		repo.setStatus(RUtil.getRepoEnumValue(jaxb.getStatus(), ROperationResultStatus.class));
		repo.setTimestamp(jaxb.getTimestamp());
    }

	@Override
	public String toString() {
		return "ROperationExecution{" +
				"ownerOid='" + ownerOid + '\'' +
				", id=" + id +
				", initiatorRef=" + initiatorRef +
				", taskRef=" + taskRef +
				", status=" + status +
				", timestamp=" + timestamp +
				'}';
	}
}
