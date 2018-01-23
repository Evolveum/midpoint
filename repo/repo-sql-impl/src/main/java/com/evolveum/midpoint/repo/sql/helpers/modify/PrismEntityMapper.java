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

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.common.embedded.*;
import com.evolveum.midpoint.repo.sql.data.common.enums.SchemaEnum;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AutoassignSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationalStateType;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Viliam Repan (lazyman).
 */
public class PrismEntityMapper {

    private static final Map<Key, Mapper> mappers = new HashMap<>();

    static {
        mappers.put(new Key(Enum.class, SchemaEnum.class), new EnumMapper());
        mappers.put(new Key(PolyString.class, RPolyString.class), new PolyStringMapper());
        mappers.put(new Key(ActivationType.class, RActivation.class), new ActivationMapper());
        mappers.put(new Key(ObjectReferenceType.class, REmbeddedReference.class), new ObjectReferenceMapper());
        mappers.put(new Key(OperationalStateType.class, ROperationalState.class), new OperationalStateMapper());
        mappers.put(new Key(AutoassignSpecificationType.class, RAutoassignSpecification.class), new AutoassignSpecificationMapper());
    }

    public boolean supports(Class inputType, Class outputType) {
        Key key = buildKey(inputType, outputType);

        return mappers.containsKey(key);
    }

    public <I, O> O map(I input, Class<O> outputType) {
        if (input == null) {
            return null;
        }

        if (!supports(input.getClass(), outputType)) {
            return (O) input;
        }

        Key key = buildKey(input.getClass(), outputType);
        Mapper<I, O> mapper = mappers.get(key);
        if (mapper == null) {
            throw new SystemException("Can't map '" + input.getClass() + "' to '" + outputType + "'");
        }

        return mapper.map(input);
    }

    public <O> O mapPrismValue(PrismValue input, Class<O> outputType) {
        if (input instanceof PrismPropertyValue) {
            return map(input.getRealValue(), outputType);
        } else if (input instanceof PrismReferenceValue) {

        } else if (input instanceof PrismContainerValue) {

        }

        Class inputType = input.getRealClass();

//            if (value instanceof PrismContainerValue) {
//                PrismContainerValue containerValue = (PrismContainerValue) value;
//                results.add(containerValue.getId());
//            } else if (value instanceof PrismReferenceValue){
//                Object result = null;//prismEntityMapper.map();
//                results.add(result);
//            }

//            Class clazz = value.getRealClass();
//            ManagedType type = entityModificationRegistry.getJaxbMapping(clazz);
//            Class repoClass = type.getJavaType();
//
//            Object result;
//            if (Container.class.isAssignableFrom(repoClass)) {
//
//            } else {
//                result = prismEntityMapper.map()
//            }

        // todo implement transformation from prism to entity

        return (O) input;
    }

    private Key buildKey(Class inputType, Class outputType) {
        if (isSchemaEnum(inputType, outputType)) {
            return new Key(Enum.class, SchemaEnum.class);
        }

        return new Key(inputType, outputType);
    }

    private boolean isSchemaEnum(Class inputType, Class outputType) {
        return Enum.class.isAssignableFrom(inputType) && SchemaEnum.class.isAssignableFrom(outputType);
    }

    private static class AutoassignSpecificationMapper implements Mapper<AutoassignSpecificationType, RAutoassignSpecification> {

        @Override
        public RAutoassignSpecification map(AutoassignSpecificationType input) {
            RAutoassignSpecification rspec = new RAutoassignSpecification();
            RAutoassignSpecification.copyFromJAXB(input, rspec);
            return rspec;
        }
    }

    private static class OperationalStateMapper implements Mapper<OperationalStateType, ROperationalState> {

        @Override
        public ROperationalState map(OperationalStateType input) {
            try {
                ROperationalState rstate = new ROperationalState();
                ROperationalState.copyFromJAXB(input, rstate);
                return rstate;
            } catch (DtoTranslationException ex) {
                throw new SystemException("Couldn't translate operational state to entity", ex);
            }
        }
    }

    private static class ObjectReferenceMapper implements Mapper<ObjectReferenceType, REmbeddedReference> {

        @Override
        public REmbeddedReference map(ObjectReferenceType input) {
            REmbeddedReference rref = new REmbeddedReference();
            REmbeddedReference.copyFromJAXB(input, rref);
            return rref;
        }
    }

    private static class ActivationMapper implements Mapper<ActivationType, RActivation> {

        @Override
        public RActivation map(ActivationType input) {
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
        public RPolyString map(PolyString input) {
            return new RPolyString(input.getOrig(), input.getNorm());
        }
    }

    private static class EnumMapper implements Mapper<Enum, SchemaEnum> {

        @Override
        public SchemaEnum map(Enum input) {
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

        O map(I input);
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
