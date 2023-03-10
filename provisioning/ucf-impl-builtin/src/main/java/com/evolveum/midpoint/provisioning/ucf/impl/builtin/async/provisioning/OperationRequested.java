/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.builtin.async.provisioning;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorOperationOptions;
import com.evolveum.midpoint.provisioning.ucf.api.Operation;
import com.evolveum.midpoint.provisioning.ucf.api.PropertyModificationOperation;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

/**
 * Describes operation (add, modify, delete) that was requested to be executed asynchronously.
 */
public abstract class OperationRequested {

    final PrismContext prismContext;
    public final ShadowType shadow;

    protected OperationRequested(PrismContext prismContext, ShadowType shadow) {
        this.prismContext = prismContext;
        this.shadow = shadow;
    }

    public static class Add extends OperationRequested {

        public Add(ShadowType shadow, PrismContext prismContext) {
            super(prismContext, shadow);
        }

        @Override
        public AsyncProvisioningOperationRequestedType asBean() {
            return new AsyncProvisioningAddOperationRequestedType(prismContext)
                    .shadowRef(getShadowAsReference());
        }

        @Override
        public AsyncProvisioningOperationRequestedType asBeanWithoutShadow() {
            return asBean();
        }

        @Override
        public Collection<ResourceAttribute<?>> getPrimaryIdentifiers() {
            return ShadowUtil.getPrimaryIdentifiers(shadow);
        }

        @Override
        public Collection<ResourceAttribute<?>> getSecondaryIdentifiers() {
            return ShadowUtil.getSecondaryIdentifiers(shadow);
        }

        @Override
        public QName getObjectClassName() {
            ResourceObjectClassDefinition ocd = ShadowUtil.getObjectClassDefinition(shadow);
            return ocd != null ? ocd.getTypeName() : null;
        }
    }

    public static class Modify extends OperationRequested {

        @NotNull public final ResourceObjectIdentification identification;
        @NotNull public final Collection<Operation> operations;
        public final ConnectorOperationOptions options;

        public Modify(@NotNull ResourceObjectIdentification identification, ShadowType shadow, Collection<Operation> operations,
                ConnectorOperationOptions options, PrismContext prismContext) {
            super(prismContext, shadow);
            this.identification = identification;
            this.operations = Collections.unmodifiableCollection(operations);
            this.options = options;
        }

        @Override
        public AsyncProvisioningOperationRequestedType asBeanWithoutShadow() throws SchemaException {
            AsyncProvisioningModifyOperationRequestedType bean = new AsyncProvisioningModifyOperationRequestedType(prismContext)
                    .identification(identification.asBean());
            for (Operation operation : operations) {
                bean.getOperation().add(operation.asBean(prismContext));
            }
            return bean;
        }

        @Override
        public AsyncProvisioningOperationRequestedType asBean() throws SchemaException {
            return asBeanWithoutShadow()
                    .shadowRef(getShadowAsReference());
        }

        /**
         * Returns the map containing attribute names and corresponding deltas.
         */
        public Map<ItemName, ItemDelta<?,?>> getAttributeChangeMap() {
            Map<ItemName, ItemDelta<?, ?>> map = new HashMap<>();
            for (Operation operation : operations) {
                if (operation instanceof PropertyModificationOperation<?>) {
                    PropertyDelta<?> delta = ((PropertyModificationOperation<?>) operation).getPropertyDelta();
                    map.put(delta.getElementName(), delta);
                }
            }
            return map;
        }

        @Override
        public Collection<? extends ResourceAttribute<?>> getPrimaryIdentifiers() {
            return identification.getPrimaryIdentifiers();
        }

        @Override
        public Collection<? extends ResourceAttribute<?>> getSecondaryIdentifiers() {
            return identification.getSecondaryIdentifiers();
        }

        @Override
        public QName getObjectClassName() {
            return identification.getResourceObjectDefinition() != null ?
                    identification.getResourceObjectDefinition().getTypeName() : null;
        }
    }

    public static class Delete extends OperationRequested {

        public final ResourceObjectIdentification identification;

        public Delete(ResourceObjectDefinition objectClass, ShadowType shadow,
                Collection<? extends ResourceAttribute<?>> identifiers, PrismContext prismContext) throws SchemaException {
            super(prismContext, shadow);
            this.identification = ResourceObjectIdentification.create(objectClass, identifiers);
        }

        @Override
        public AsyncProvisioningOperationRequestedType asBeanWithoutShadow() throws SchemaException {
            return new AsyncProvisioningDeleteOperationRequestedType(prismContext)
                    .identification(identification.asBean());
        }

        @Override
        public AsyncProvisioningOperationRequestedType asBean() throws SchemaException {
            return asBeanWithoutShadow()
                    .shadowRef(getShadowAsReference());
        }

        @Override
        public Collection<? extends ResourceAttribute<?>> getPrimaryIdentifiers() {
            return identification.getPrimaryIdentifiers();
        }

        @Override
        public Collection<? extends ResourceAttribute<?>> getSecondaryIdentifiers() {
            return identification.getSecondaryIdentifiers();
        }

        @Override
        public QName getObjectClassName() {
            return identification.getResourceObjectDefinition() != null ?
                    identification.getResourceObjectDefinition().getTypeName() : null;
        }
    }

    /**
     * Returns the operation requested in the form of a {@link AsyncProvisioningOperationRequestedType} bean.
     */
    public abstract AsyncProvisioningOperationRequestedType asBean() throws SchemaException;

    public abstract AsyncProvisioningOperationRequestedType asBeanWithoutShadow() throws SchemaException;

    /**
     * Returns the map containing attribute names and their real values.
     */
    public Map<ItemName, Collection<?>> getAttributeValueMap() {
        PrismContainer<Containerable> attributesContainer = shadow.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
        if (attributesContainer != null && attributesContainer.hasAnyValue()) {
            return getAttributesValueMap(attributesContainer.getValue().getItems());
        } else {
            return Collections.emptyMap();
        }
    }

    public abstract Collection<? extends ResourceAttribute<?>> getPrimaryIdentifiers();

    public abstract Collection<? extends ResourceAttribute<?>> getSecondaryIdentifiers();

    public Map<ItemName, Collection<?>> getPrimaryIdentifiersValueMap() {
        return getAttributesValueMap(getPrimaryIdentifiers());
    }

    public Map<ItemName, Collection<?>> getSecondaryIdentifiersValueMap() {
        return getAttributesValueMap(getSecondaryIdentifiers());
    }

    private Map<ItemName, Collection<?>> getAttributesValueMap(Collection<? extends Item<?, ?>> attributes) {
        Map<ItemName, Collection<?>> map = new HashMap<>();
        for (Item<?, ?> item : emptyIfNull(attributes)) {
            map.put(item.getElementName(), item.getRealValues());
        }
        return map;
    }

    public Object getPrimaryIdentifierValue() {
        ResourceAttribute<?> identifier = MiscUtil.extractSingleton(getPrimaryIdentifiers());
        return identifier != null ? identifier.getRealValue() : null;
    }

    public Object getSecondaryIdentifierValue() {
        ResourceAttribute<?> identifier = MiscUtil.extractSingleton(getSecondaryIdentifiers());
        return identifier != null ? identifier.getRealValue() : null;
    }

    ObjectReferenceType getShadowAsReference() {
        return ObjectTypeUtil.createObjectRefWithFullObject(shadow);
    }

    public String getObjectClassLocalName() {
        QName objectClassName = getObjectClassName();
        return objectClassName != null ? objectClassName.getLocalPart() : null;
    }

    public abstract QName getObjectClassName();

    public String getSimpleName() {
        return getClass().getSimpleName().toLowerCase();
    }
}
