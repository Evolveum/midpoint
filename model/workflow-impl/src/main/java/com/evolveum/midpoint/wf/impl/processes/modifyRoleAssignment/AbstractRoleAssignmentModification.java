/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.processes.modifyRoleAssignment;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import org.apache.commons.lang3.Validate;

import java.io.Serializable;
import java.util.List;

/**
 * @author mederly
 */
public class AbstractRoleAssignmentModification implements Serializable {

    private AssignmentType assignmentTypeOld;
    private AbstractRoleType abstractRoleType;
    private List<ItemDeltaType> modifications;

    public AbstractRoleAssignmentModification(AssignmentType assignmentType, AbstractRoleType abstractRoleType, List<ItemDeltaType> modifications) {
        Validate.notNull(assignmentType, "assignmentType");
        Validate.notNull(abstractRoleType, "abstractRoleType");
        Validate.notNull(modifications, "modifications");
        this.assignmentTypeOld = assignmentType;
        this.abstractRoleType = abstractRoleType;
        this.modifications = modifications;
    }

    public AssignmentType getAssignmentTypeOld() {
        return assignmentTypeOld;
    }

    public void setAssignmentTypeOld(AssignmentType assignmentTypeOld) {
        this.assignmentTypeOld = assignmentTypeOld;
    }

    public AbstractRoleType getAbstractRoleType() {
        return abstractRoleType;
    }

    public void setAbstractRoleType(AbstractRoleType abstractRoleType) {
        this.abstractRoleType = abstractRoleType;
    }

    public List<ItemDeltaType> getModifications() {
        return modifications;
    }

    public void setModifications(List<ItemDeltaType> modifications) {
        this.modifications = modifications;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractRoleAssignmentModification that = (AbstractRoleAssignmentModification) o;

        if (!abstractRoleType.equals(that.abstractRoleType)) return false;
        if (!assignmentTypeOld.equals(that.assignmentTypeOld)) return false;
        if (!modifications.equals(that.modifications)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = assignmentTypeOld.hashCode();
        result = 31 * result + abstractRoleType.hashCode();
        result = 31 * result + modifications.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "AbstractRoleAssignmentModification{" +
                "assignmentTypeOld=" + assignmentTypeOld +
                ", abstractRoleType=" + abstractRoleType +
                ", modifications=" + modifications +
                '}';
    }
}
