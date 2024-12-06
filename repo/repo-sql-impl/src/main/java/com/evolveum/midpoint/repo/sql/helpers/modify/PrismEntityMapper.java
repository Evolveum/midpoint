/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.helpers.modify;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.SqlRepositoryConfiguration;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.*;
import com.evolveum.midpoint.repo.sql.data.common.container.*;
import com.evolveum.midpoint.repo.sql.data.common.dictionary.ExtItemDictionary;
import com.evolveum.midpoint.repo.sql.data.common.embedded.*;
import com.evolveum.midpoint.repo.sql.data.common.enums.SchemaEnum;
import com.evolveum.midpoint.repo.sql.helpers.mapper.*;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class PrismEntityMapper {

    private static final Map<Key, Mapper<?, ?>> MAPPERS = new HashMap<>();

    static {
        MAPPERS.put(new Key(Enum.class, SchemaEnum.class), new EnumMapper());
        MAPPERS.put(new Key(PolyString.class, RPolyString.class), new PolyStringMapper());
        MAPPERS.put(new Key(ActivationType.class, RSimpleActivation.class), new ActivationMapper());
        MAPPERS.put(new Key(ActivationType.class, RFocusActivation.class), new FocusActivationMapper());
        MAPPERS.put(new Key(TaskAutoScalingType.class, RTaskAutoScaling.class), new TaskAutoScalingMapper());
        MAPPERS.put(new Key(Referencable.class, REmbeddedReference.class), new EmbeddedObjectReferenceMapper());
        MAPPERS.put(new Key(OperationalStateType.class, ROperationalState.class), new OperationalStateMapper());
        MAPPERS.put(new Key(AutoassignSpecificationType.class, RAutoassignSpecification.class), new AutoassignSpecificationMapper());
        MAPPERS.put(new Key(QName.class, String.class), new QNameMapper());

        MAPPERS.put(new Key(Referencable.class, RObjectReference.class), new ObjectReferenceMapper());
        MAPPERS.put(new Key(Referencable.class, RAssignmentReference.class), new AssignmentReferenceMapper());
        MAPPERS.put(new Key(Referencable.class, RCaseWorkItemReference.class), new CaseWorkItemReferenceMapper());
        MAPPERS.put(new Key(AssignmentType.class, RAssignment.class), new AssignmentMapper());
        MAPPERS.put(new Key(TriggerType.class, RTrigger.class), new TriggerMapper());
        MAPPERS.put(new Key(OperationExecutionType.class, ROperationExecution.class), new OperationExecutionMapper());
        MAPPERS.put(new Key(CaseWorkItemType.class, RCaseWorkItem.class), new CaseWorkItemMapper());

        MAPPERS.put(new Key(OperationResultType.class, ROperationResult.class), new OperationResultMapper());
        MAPPERS.put(new Key(MetadataType.class, Metadata.class), new MetadataMapper());

        MAPPERS.put(new Key(byte[].class, RFocusPhoto.class), new RFocusPhotoMapper());
        MAPPERS.put(new Key(MetadataType.class, RFocus.class), new PasswordMetadataMapper());
    }

    @Autowired private RepositoryService repositoryService;
    @Autowired private PrismContext prismContext;
    @Autowired private ExtItemDictionary extItemDictionary;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private SqlRepositoryConfiguration sqlRepositoryConfiguration;

    public boolean supports(Class<?> inputType, Class<?> outputType) {
        Key key = buildKey(inputType, outputType);

        return MAPPERS.containsKey(key);
    }

    public <I, O> Mapper<I, O> getMapper(Class<I> inputType, Class<O> outputType) {
        Key key = buildKey(inputType, outputType);
        //noinspection unchecked
        Mapper<I, O> mapper = (Mapper<I, O>) MAPPERS.get(key);
        if (mapper == null) {
            throw new SystemException("Can't map '" + inputType + "' to '" + outputType + "'");
        }

        return mapper;
    }

    public <I, O> O map(I input, Class<O> outputType) {
        return map(input, outputType, null);
    }

    public <I, O> O map(I input, Class<O> outputType, MapperContext context) {
        if (input == null) {
            return null;
        }

        if (!supports(input.getClass(), outputType)) {
            //noinspection unchecked
            return (O) input;
        }

        if (context == null) {
            context = new MapperContext();
        }
        context.setRepositoryContext(new RepositoryContext(repositoryService,
                prismContext, relationRegistry, extItemDictionary, sqlRepositoryConfiguration));

        Key key = buildKey(input.getClass(), outputType);
        //noinspection unchecked
        Mapper<I, O> mapper = (Mapper<I, O>) MAPPERS.get(key);
        if (mapper == null) {
            throw new SystemException("Can't map '" + input.getClass() + "' to '" + outputType + "'");
        }

        return mapper.map(input, context);
    }

    /**
     * todo implement transformation from prism to entity
     * RObjectTextInfo              - handled manually
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
     */
    public <O> O mapPrismValue(PrismValue input, Class<O> outputType, MapperContext context) {
        if (input instanceof PrismPropertyValue) {
            return map(input.getRealValue(), outputType, context);
        } else if (input instanceof PrismReferenceValue) {
            ObjectReferenceType ref = new ObjectReferenceType();
            ref.setupReferenceValue((PrismReferenceValue) input);

            return map(ref, outputType, context);
        } else if (input instanceof PrismContainerValue) {
            //noinspection unchecked
            Class<Containerable> inputType = (Class<Containerable>) input.getRealClass();
            try {
                assert inputType != null;
                Containerable container = inputType.getConstructor().newInstance();
                container.setupContainerValue((PrismContainerValue<?>) input);

                return map(container, outputType, context);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
                throw new SystemException("Couldn't create instance of container '" + inputType + "'");
            }
        }

        //noinspection unchecked
        return (O) input;
    }

    private Key buildKey(Class<?> inputType, Class<?> outputType) {
        if (isSchemaEnum(inputType, outputType)) {
            return new Key(Enum.class, SchemaEnum.class);
        }

        if (Referencable.class.isAssignableFrom(inputType)) {
            return new Key(Referencable.class, outputType);
        }

        return new Key(inputType, outputType);
    }

    private boolean isSchemaEnum(Class<?> inputType, Class<?> outputType) {
        return Enum.class.isAssignableFrom(inputType) && SchemaEnum.class.isAssignableFrom(outputType);
    }

    private static class Key {

        private final Class<?> from;
        private final Class<?> to;

        Key(Class<?> from, Class<?> to) {
            this.from = from;
            this.to = to;
        }

        public Class<?> getFrom() {
            return from;
        }

        public Class<?> getTo() {
            return to;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }

            Key key = (Key) o;

            return Objects.equals(from, key.from)
                    && Objects.equals(to, key.to);
        }

        @Override
        public int hashCode() {
            int result = from != null ? from.hashCode() : 0;
            result = 31 * result + (to != null ? to.hashCode() : 0);
            return result;
        }
    }
}
