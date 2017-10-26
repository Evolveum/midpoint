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

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.container.RCaseWorkItem;
import com.evolveum.midpoint.repo.sql.data.common.embedded.REmbeddedReference;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.query.definition.JaxbName;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.MidPointJoinedPersister;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Persister;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * TODO fill-in relevant data
 *
 * @author mederly
 */
@Entity
@ForeignKey(name = "fk_case")
@Table(uniqueConstraints = @UniqueConstraint(name = "uc_case_name", columnNames = {"name_norm"}))
@Persister(impl = MidPointJoinedPersister.class)
public class RCase extends RObject<CaseType> {

    private RPolyString name;
    private String state;
    private REmbeddedReference objectRef;
    private Set<RCaseWorkItem> workItems = new HashSet<>();

    @Embedded
    public RPolyString getName() {
        return name;
    }

    public void setName(RPolyString name) {
        this.name = name;
    }

    @Column
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    @Embedded
    public REmbeddedReference getObjectRef() {
        return objectRef;
    }

    public void setObjectRef(REmbeddedReference objectRef) {
        this.objectRef = objectRef;
    }

    @JaxbName(localPart = "workItem")
    @OneToMany(mappedBy = "owner", orphanRemoval = true)
    @org.hibernate.annotations.ForeignKey(name = "none")
    @Cascade({org.hibernate.annotations.CascadeType.ALL})
    public Set<RCaseWorkItem> getWorkItems() {
        return workItems;
    }

    public void setWorkItems(Set<RCaseWorkItem> workItems) {
        this.workItems = workItems != null ? workItems : new HashSet<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof RCase))
            return false;
        if (!super.equals(o))
            return false;
        RCase rCase = (RCase) o;
        return Objects.equals(name, rCase.name) &&
                Objects.equals(objectRef, rCase.objectRef) &&
                Objects.equals(workItems, rCase.workItems);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, objectRef, workItems);
    }

    @Override
    public String toString() {
        return "RCase{" +
                "name=" + name +
                ", objectRef=" + objectRef +
                '}';
    }

    public static void copyFromJAXB(CaseType jaxb, RCase repo, RepositoryContext context,
			IdGeneratorResult generatorResult) throws DtoTranslationException {
		RObject.copyFromJAXB(jaxb, repo, context, generatorResult);
        repo.setObjectRef(RUtil.jaxbRefToEmbeddedRepoRef(jaxb.getObjectRef(), context.prismContext));
        repo.setState(jaxb.getState());
        for (CaseWorkItemType workItem : jaxb.getWorkItem()) {
            repo.getWorkItems().add(RCaseWorkItem.toRepo(repo, workItem, context));
        }
    }

    @Override
    public CaseType toJAXB(PrismContext prismContext, Collection<SelectorOptions<GetOperationOptions>> options)
            throws DtoTranslationException {
        throw new UnsupportedOperationException("Shouldn't be needed anymore.");
    }
}
