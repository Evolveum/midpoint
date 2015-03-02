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

package com.evolveum.midpoint.wf.impl.processes.modifyResourceAssignment;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import org.apache.commons.lang3.Validate;

import java.io.Serializable;
import java.util.List;

/**
 * TODO extract items that are common with AbstractRoleAssignmentModification
 * @author mederly
 */
public class ResourceAssignmentModification implements Serializable {

    private AssignmentType assignmentTypeOld;
    private ResourceType resourceType;
    private List<ItemDeltaType> modifications;

    public ResourceAssignmentModification(AssignmentType assignmentType, ResourceType resourceType, List<ItemDeltaType> modifications) {
        Validate.notNull(assignmentType, "assignmentType");
        Validate.notNull(resourceType, "resourceType");
        Validate.notNull(modifications, "modifications");
        this.assignmentTypeOld = assignmentType;
        this.resourceType = resourceType;
        this.modifications = modifications;
    }

    public AssignmentType getAssignmentTypeOld() {
        return assignmentTypeOld;
    }

    public void setAssignmentTypeOld(AssignmentType assignmentTypeOld) {
        this.assignmentTypeOld = assignmentTypeOld;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
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

        ResourceAssignmentModification that = (ResourceAssignmentModification) o;

        if (!assignmentTypeOld.equals(that.assignmentTypeOld)) return false;
        if (!modifications.equals(that.modifications)) return false;
        if (!resourceType.equals(that.resourceType)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = assignmentTypeOld.hashCode();
        result = 31 * result + resourceType.hashCode();
        result = 31 * result + modifications.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ResourceAssignmentModification{" +
                "assignmentTypeOld=" + assignmentTypeOld +
                ", resourceType=" + resourceType +
                ", modifications=" + modifications +
                '}';
    }
}
