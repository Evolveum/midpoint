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

package com.evolveum.midpoint.wf.impl.processes.modifyAssignment;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.wf.impl.util.SerializationSafeContainer;
import com.evolveum.midpoint.wf.impl.util.SingleItemSerializationSafeContainerImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.ItemDeltaType;
import org.apache.commons.lang3.Validate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class AssignmentModification implements Serializable {

    private AssignmentType assignmentOld;
    private ObjectType target;
    private List<ItemDeltaType> modifications;

    public AssignmentModification(AssignmentType assignmentType, ObjectType target, List<ItemDeltaType> modifications) {
        Validate.notNull(assignmentType, "assignment");
        Validate.notNull(target, "target");
        Validate.notNull(modifications, "modifications");
        this.assignmentOld = assignmentType;
        this.target = target;
        this.modifications = modifications;
    }

    public AssignmentType getAssignmentOld() {
        return assignmentOld;
    }

    public void setAssignmentOld(AssignmentType assignmentOld) {
        this.assignmentOld = assignmentOld;
    }

    public ObjectType getTarget() {
        return target;
    }

    public void setTarget(ObjectType targetType) {
        this.target = targetType;
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

        AssignmentModification that = (AssignmentModification) o;

        if (!assignmentOld.equals(that.assignmentOld)) return false;
        if (!target.equals(that.target)) return false;
        return modifications.equals(that.modifications);

    }

    @Override
    public int hashCode() {
        int result = assignmentOld.hashCode();
        result = 31 * result + target.hashCode();
        result = 31 * result + modifications.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "AbstractRoleAssignmentModification{" +
                "assignmentOld=" + assignmentOld +
                ", target=" + target +
                ", modifications=" + modifications +
                '}';
    }

    /**
     * Provides a custom SerializationSafeContainer for this object.
     * This is necessary because standard SingleItemSerializationSafeContainerImpl
     */
    public SerializationSafeContainer<AssignmentModification> wrap(PrismContext prismContext) {
        return new Container(this, prismContext);
    }

    public static class Container implements SerializationSafeContainer<AssignmentModification> {
        private SerializationSafeContainer<AssignmentType> assignmentOldWrapped;
        private SerializationSafeContainer<ObjectType> targetWrapped;
        private List<SerializationSafeContainer<ItemDeltaType>> modificationsWrapped;

        private transient AssignmentModification actualValue;
        private transient PrismContext prismContext;

        public Container(AssignmentModification assignmentModification, PrismContext prismContext) {
            this.prismContext = prismContext;
            setValue(assignmentModification);
        }

        @Override
        public void setValue(AssignmentModification am) {
            actualValue = am;
            assignmentOldWrapped = new SingleItemSerializationSafeContainerImpl<>(am.assignmentOld, prismContext);
            targetWrapped = new SingleItemSerializationSafeContainerImpl<>(am.target, prismContext);
            modificationsWrapped = new ArrayList<>(am.getModifications().size());
            for (ItemDeltaType itemDeltaType : am.getModifications()) {
                modificationsWrapped.add(new SingleItemSerializationSafeContainerImpl<>(itemDeltaType, prismContext));
            }
        }

        @Override
        public AssignmentModification getValue() {
            if (actualValue != null) {
                return actualValue;
            }
            AssignmentType assignmentOld = assignmentOldWrapped.getValue();
            ObjectType target = targetWrapped.getValue();
            List<ItemDeltaType> modifications = new ArrayList<>(modificationsWrapped.size());
            for (SerializationSafeContainer<ItemDeltaType> modificationWrapped : modificationsWrapped) {
                modifications.add(modificationWrapped.getValue());
            }
            actualValue = new AssignmentModification(assignmentOld, target, modifications);
            return actualValue;
        }

        @Override
        public PrismContext getPrismContext() {
            return prismContext;
        }

        @Override
        public void setPrismContext(PrismContext prismContext) {
            this.prismContext = prismContext;
            assignmentOldWrapped.setPrismContext(prismContext);
            targetWrapped.setPrismContext(prismContext);
            for (SerializationSafeContainer<ItemDeltaType> modificationWrapped : modificationsWrapped) {
                modificationWrapped.setPrismContext(prismContext);
            }
        }

        @Override
        public void clearActualValue() {
            assignmentOldWrapped.clearActualValue();
            targetWrapped.clearActualValue();
            modificationsWrapped = new ArrayList<>();           // OK?
        }
    }
}
