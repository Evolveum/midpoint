/*
 * Copyright (c) 2010-2018 Evolveum
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

package com.evolveum.midpoint.repo.sql.helpers.modify;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.container.ROperationExecution;
import com.evolveum.midpoint.repo.sql.data.common.container.RTrigger;
import com.evolveum.midpoint.repo.sql.data.common.embedded.*;
import com.evolveum.midpoint.repo.sql.data.common.enums.SchemaEnum;
import com.evolveum.midpoint.repo.sql.data.common.other.RReferenceOwner;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author Viliam Repan (lazyman).
 */
@Component
public class PrismEntityMapper {

    private static final Map<Key, Mapper> mappers = new HashMap<>();

    static {
        mappers.put(new Key(Enum.class, SchemaEnum.class), new EnumMapper());
        mappers.put(new Key(PolyString.class, RPolyString.class), new PolyStringMapper());
        mappers.put(new Key(ActivationType.class, RActivation.class), new ActivationMapper());
        mappers.put(new Key(Referencable.class, REmbeddedReference.class), new EmbeddedObjectReferenceMapper());
        mappers.put(new Key(OperationalStateType.class, ROperationalState.class), new OperationalStateMapper());
        mappers.put(new Key(AutoassignSpecificationType.class, RAutoassignSpecification.class), new AutoassignSpecificationMapper());
        mappers.put(new Key(QName.class, String.class), new QNameMapper());

        mappers.put(new Key(Referencable.class, RObjectReference.class), new ObjectReferenceMapper());
        mappers.put(new Key(AssignmentType.class, RAssignment.class), new AssignmentMapper());
        mappers.put(new Key(TriggerType.class, RTrigger.class), new TriggerMapper());
        mappers.put(new Key(OperationExecutionType.class, ROperationExecution.class), new OperationExecutionMapper());
    }

    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private PrismContext prismContext;

    public boolean supports(Class inputType, Class outputType) {
        Key key = buildKey(inputType, outputType);

        return mappers.containsKey(key);
    }

    public <I, O> O map(I input, Class<O> outputType) {
        return map(input, outputType, null);
    }

    public <I, O> O map(I input, Class<O> outputType, MapperContext context) {
        if (input == null) {
            return null;
        }

        if (!supports(input.getClass(), outputType)) {
            return (O) input;
        }

        if (context == null) {
            context = new MapperContext();
        }
        context.setPrismContext(prismContext);
        context.setRepositoryService(repositoryService);

        Key key = buildKey(input.getClass(), outputType);
        Mapper<I, O> mapper = mappers.get(key);
        if (mapper == null) {
            throw new SystemException("Can't map '" + input.getClass() + "' to '" + outputType + "'");
        }

        return mapper.map(input, context);
    }

    /**
     * todo implement transformation from prism to entity
     * RObjectTextInfo
     * RLookupTableRow
     * RAccessCertificationWorkItem
     * RAssignmentReference
     * RFocusPhoto                  - handled manually
     * RObjectReference             - implemented
     * RObjectDeltaOperation
     * ROperationExecution          - implemented
     * RAccessCertificationCase
     * RAssignment                  - implemented
     * RCertWorkItemReference
     * RTrigger                     - implemented
     *
     * @param input
     * @param outputType
     * @param context
     * @param <O>
     * @return
     */
    public <O> O mapPrismValue(PrismValue input, Class<O> outputType, MapperContext context) {
        if (input instanceof PrismPropertyValue) {
            return map(input.getRealValue(), outputType, context);
        } else if (input instanceof PrismReferenceValue) {
            ObjectReferenceType ref = new ObjectReferenceType();
            ref.setupReferenceValue((PrismReferenceValue) input);

            return map(ref, outputType, context);
        } else if (input instanceof PrismContainerValue) {
            Class<Containerable> inputType = (Class) input.getRealClass();
            try {
                Containerable container = inputType.newInstance();
                container.setupContainerValue((PrismContainerValue) input);

                return map(container, outputType, context);
            } catch (InstantiationException | IllegalAccessException ex) {
                throw new SystemException("Couldn't create instance of container '" + inputType + "'");
            }
        }

        return (O) input;
    }

    private Key buildKey(Class inputType, Class outputType) {
        if (isSchemaEnum(inputType, outputType)) {
            return new Key(Enum.class, SchemaEnum.class);
        }

        if (Referencable.class.isAssignableFrom(inputType)) {
            return new Key(Referencable.class, outputType);
        }

        return new Key(inputType, outputType);
    }

    private boolean isSchemaEnum(Class inputType, Class outputType) {
        return Enum.class.isAssignableFrom(inputType) && SchemaEnum.class.isAssignableFrom(outputType);
    }

    private static class OperationExecutionMapper implements Mapper<OperationExecutionType, ROperationExecution> {

        @Override
        public ROperationExecution map(OperationExecutionType input, MapperContext context) {
            ROperationExecution execution = new ROperationExecution();

            RObject owner = (RObject) context.getOwner();

            RepositoryContext repositoryContext =
                    new RepositoryContext(context.getRepositoryService(), context.getPrismContext());

            try {
                ROperationExecution.copyFromJAXB(input, execution, owner, repositoryContext);
            } catch (DtoTranslationException ex) {
                throw new SystemException("Couldn't translate trigger to entity", ex);
            }


            return execution;
        }
    }

    private static class TriggerMapper implements Mapper<TriggerType, RTrigger> {

        @Override
        public RTrigger map(TriggerType input, MapperContext context) {
            RTrigger trigger = new RTrigger();

            RObject owner = (RObject) context.getOwner();

            RepositoryContext repositoryContext =
                    new RepositoryContext(context.getRepositoryService(), context.getPrismContext());

            try {
                RTrigger.copyFromJAXB(input, trigger, owner, repositoryContext);
            } catch (DtoTranslationException ex) {
                throw new SystemException("Couldn't translate trigger to entity", ex);
            }


            return trigger;
        }
    }

    private static class AssignmentMapper implements Mapper<AssignmentType, RAssignment> {

        @Override
        public RAssignment map(AssignmentType input, MapperContext context) {
            RAssignment ass = new RAssignment();

            RObject owner = (RObject) context.getOwner();

            RepositoryContext repositoryContext =
                    new RepositoryContext(context.getRepositoryService(), context.getPrismContext());

            try {
                RAssignment.copyFromJAXB(input, ass, owner, repositoryContext);
            } catch (DtoTranslationException ex) {
                throw new SystemException("Couldn't translate assignment to entity", ex);
            }

            return ass;
        }
    }

    private static class ObjectReferenceMapper implements Mapper<Referencable, RObjectReference> {

        @Override
        public RObjectReference map(Referencable input, MapperContext context) {
            ObjectReferenceType objectRef;
            if (input instanceof ObjectReferenceType) {
                objectRef = (ObjectReferenceType) input;
            } else {
                objectRef = new ObjectReferenceType();
                objectRef.setupReferenceValue(input.asReferenceValue());
            }

            ObjectTypeUtil.normalizeRelation(objectRef);

            RObject owner = (RObject) context.getOwner();

            ItemPath named = context.getDelta().getPath().namedSegmentsOnly();
            NameItemPathSegment last = named.lastNamed();
            RReferenceOwner refType = RReferenceOwner.getOwnerByQName(last.getName());

            return RUtil.jaxbRefToRepo(objectRef, context.getPrismContext(), owner, refType);
        }
    }

    private static class QNameMapper implements Mapper<QName, String> {

        @Override
        public String map(QName input, MapperContext context) {
            return RUtil.qnameToString(input);
        }
    }

    private static class AutoassignSpecificationMapper implements Mapper<AutoassignSpecificationType, RAutoassignSpecification> {

        @Override
        public RAutoassignSpecification map(AutoassignSpecificationType input, MapperContext context) {
            RAutoassignSpecification rspec = new RAutoassignSpecification();
            RAutoassignSpecification.copyFromJAXB(input, rspec);
            return rspec;
        }
    }

    private static class OperationalStateMapper implements Mapper<OperationalStateType, ROperationalState> {

        @Override
        public ROperationalState map(OperationalStateType input, MapperContext context) {
            try {
                ROperationalState rstate = new ROperationalState();
                ROperationalState.copyFromJAXB(input, rstate);
                return rstate;
            } catch (DtoTranslationException ex) {
                throw new SystemException("Couldn't translate operational state to entity", ex);
            }
        }
    }

    private static class EmbeddedObjectReferenceMapper implements Mapper<Referencable, REmbeddedReference> {

        @Override
        public REmbeddedReference map(Referencable input, MapperContext context) {
            ObjectReferenceType objectRef;
            if (input instanceof ObjectReferenceType) {
                objectRef = (ObjectReferenceType) input;
            } else {
                objectRef = new ObjectReferenceType();
                objectRef.setupReferenceValue(input.asReferenceValue());
            }

            ObjectTypeUtil.normalizeRelation(objectRef);

            REmbeddedReference rref = new REmbeddedReference();
            REmbeddedReference.copyFromJAXB(objectRef, rref);

            return rref;
        }
    }

    private static class ActivationMapper implements Mapper<ActivationType, RActivation> {

        @Override
        public RActivation map(ActivationType input, MapperContext context) {
            try {
                RActivation ractivation = new RActivation();
                RActivation.copyFromJAXB(input, ractivation, null);

                return ractivation;
            } catch (DtoTranslationException ex) {
                throw new SystemException("Couldn't translate activation to entity", ex);
            }
        }
    }

    private static class PolyStringMapper implements Mapper<PolyString, RPolyString> {

        @Override
        public RPolyString map(PolyString input, MapperContext context) {
            return new RPolyString(input.getOrig(), input.getNorm());
        }
    }

    private static class EnumMapper implements Mapper<Enum, SchemaEnum> {

        @Override
        public SchemaEnum map(Enum input, MapperContext context) {
            String repoEnumClass = null;
            try {
                String className = input.getClass().getSimpleName();
                className = StringUtils.left(className, className.length() - 4);

                repoEnumClass = "com.evolveum.midpoint.repo.sql.data.common.enums.R" + className;
                Class clazz = Class.forName(repoEnumClass);

                if (!SchemaEnum.class.isAssignableFrom(clazz)) {
                    throw new SystemException("Can't translate enum value " + input);
                }

                return RUtil.getRepoEnumValue(input, clazz);
            } catch (ClassNotFoundException ex) {
                throw new SystemException("Couldn't find class '" + repoEnumClass + "' for enum '" + input + "'", ex);
            }
        }
    }

    private interface Mapper<I, O> {

        O map(I input, MapperContext context);
    }

    private static class Key {

        private Class from;
        private Class to;

        public Key(Class from, Class to) {
            this.from = from;
            this.to = to;
        }

        public Class getFrom() {
            return from;
        }

        public Class getTo() {
            return to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            if (from != null ? !from.equals(key.from) : key.from != null) return false;
            return to != null ? to.equals(key.to) : key.to == null;
        }

        @Override
        public int hashCode() {
            int result = from != null ? from.hashCode() : 0;
            result = 31 * result + (to != null ? to.hashCode() : 0);
            return result;
        }
    }
}
