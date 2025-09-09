/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.form.CreateObjectForReferencePanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.util.ExecutedDeltaPostProcessor;
import com.evolveum.midpoint.gui.impl.util.ReferenceExecutedDeltaProcessor;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.model.LoadableDetachableModel;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author katka
 *
 */
public class PrismReferenceValueWrapperImpl<T extends Referencable> extends PrismValueWrapperImpl<T> {

    private static final Trace LOGGER = TraceManager.getTrace(CreateObjectForReferencePanel.class);

    private static final long serialVersionUID = 1L;

    private ObjectDetailsModels<? extends ObjectType> newObjectModel;
    private PrismObject<? extends ObjectType> newPrismObject = null;

    public PrismReferenceValueWrapperImpl(PrismReferenceWrapper<T> parent, PrismReferenceValue value, ValueStatus status) {
        super(parent, value, status);
    }

    private boolean editEnabled = true;
    private boolean isLink = false;

    @Override
    protected <V extends PrismValue> V getNewValueWithMetadataApplied() throws SchemaException {
        V value = super.getNewValueWithMetadataApplied();
        WebPrismUtil.cleanupValueMetadata(value);
        return value;
    }

    @Override
    public void setRealValue(T realValueReferencable) {
        PrismReferenceValue value = getNewValue();
        if (realValueReferencable == null) {
            value.setOid(null);
            value.setOriginType(null);
            value.setOriginObject(null);
            value.setObject(null);
            value.setTargetName((PolyStringType) null);
            value.setTargetType(null);
            value.setRelation(null);
            value.setFilter(null);

            setStatus(ValueStatus.MODIFIED);

            return;
        }
        PrismReferenceValue realValue = realValueReferencable.asReferenceValue();
        value.setOid(realValue.getOid());
        value.setOriginType(realValue.getOriginType());
        value.setOriginObject(realValue.getOriginObject());
        value.setTargetName(realValue.getTargetName());
        value.setTargetType(realValue.getTargetType());
        value.setRelation(realValue.getRelation());
        value.setFilter(realValue.getFilter());

        setStatus(ValueStatus.MODIFIED);
    }

    public boolean isEditEnabled() {
        return editEnabled;
    }

    public void setEditEnabled(boolean editEnabled) {
        this.editEnabled = editEnabled;
    }

    public boolean isLink() {
        return isLink;
    }

    public void setLink(boolean link) {
        isLink = link;
    }

    @Override
    public PrismReferenceValue getNewValue() {
        return super.getNewValue();
    }

    @Override
    public String toShortString() {
        T referencable = getRealValue();
        if (referencable == null) {
            return "";
        }

        return getRefName(referencable) + " (" + getTargetType(referencable) + ")";
    }

    private String getRefName(T referencable) {
        return referencable.getTargetName() != null ? WebComponentUtil.getOrigStringFromPoly(referencable.getTargetName()) : referencable.getOid();
    }

    private String getTargetType(T referencable) {
        QName type = referencable.getType();
        return type != null ? type.getLocalPart() : "";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createIndentedStringBuilder(indent);
        sb.append(getNewValue().debugDump())
                .append(" (").append(getStatus()).append(", old: ").append(getOldValue().debugDump()).append(")");
        return sb.toString();
    }

    @Override
    public Collection<ExecutedDeltaPostProcessor> getPreconditionDeltas(
            ModelServiceLocator serviceLocator, OperationResult result) throws CommonException {
        if (!isNewObjectModelCreated()) {
            return super.getPreconditionDeltas(serviceLocator, result);
        }
        processBeforeCreatingPreconditionDelta(newObjectModel, serviceLocator);
        return Collections.singletonList(new ReferenceExecutedDeltaProcessor(
                newObjectModel.collectDeltas(result), PrismReferenceValueWrapperImpl.this));
    }

    /**
     * Custom processing of new object for reference.
     */
    protected <O extends ObjectType> void processBeforeCreatingPreconditionDelta(ObjectDetailsModels<O> newObjectModel, ModelServiceLocator serviceLocator) {
    }

    /**
     * Return details model for new object that will be added to reference value.
     */
    public <O extends ObjectType> ObjectDetailsModels<O> getNewObjectModel(
            ContainerPanelConfigurationType config, ModelServiceLocator serviceLocator, OperationResult result) {
        if (!isNewObjectModelCreated()) {

            newObjectModel = createNewObjectModel(config, serviceLocator, result);
        }
        return (ObjectDetailsModels<O>) newObjectModel;
    }

    private  <O extends ObjectType> ObjectDetailsModels<O> createNewObjectModel(
            ContainerPanelConfigurationType config, ModelServiceLocator serviceLocator, OperationResult result) {
        if (newPrismObject == null) {

            try {
                newPrismObject = createNewPrismObject(result);

            } catch (SchemaException e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Cannot create wrapper for new object in reference \nReason: {]", e, e.getMessage());
                result.recordFatalError("Cannot create archetype wrapper for new object in reference, because: " + e.getMessage(), e);
            }
        }
        LoadableDetachableModel<PrismObject<O>> prismObjectModel = new LoadableDetachableModel<>() {
            @Override
            protected PrismObject<O> load() {
                return (PrismObject<O>) newPrismObject;
            }
        };
        return new ObjectDetailsModels<>(prismObjectModel, serviceLocator) {
            @Override
            public List<? extends ContainerPanelConfigurationType> getPanelConfigurations() {
                return Collections.singletonList(config);
            }

            @Override
            protected WrapperContext createWrapperContext(Task task, OperationResult result) {
                return PrismReferenceValueWrapperImpl.this.createWrapperContextForNewObject(super.createWrapperContext(task, result));
            }
        };
    }

    protected WrapperContext createWrapperContextForNewObject(WrapperContext wrapperContext) {
        return wrapperContext;
    }

    /**
     * Create new object that will be added to reference value.
     */
    protected <O extends ObjectType> PrismObject<O> createNewPrismObject(OperationResult result) throws SchemaException {
        PrismReferenceWrapper<T> parent = getParent();
        List<QName> types = parent.getTargetTypes();
        if (types.size() != 1) {
            result.recordFatalError("Cannot create archetype wrapper for new object in reference, because couldn't one type, actual types " + types);
            return null;
        }

        QName type = types.get(0);
        PrismObjectDefinition<O> def = PrismContext.get().getSchemaRegistry()
                .findObjectDefinitionByType(type);
        if (def == null) {
            result.recordFatalError("Cannot create archetype wrapper for new object in reference, because couldn't find def for " + type);
            return null;
        }
        return def.instantiate();
    }

    /**
     * Clean details model for new object that should be added to reference value.
     */
    public void resetNewObjectModel() {
        if (isNewObjectModelCreated()) {
            newObjectModel.detach();
        }
        newObjectModel = null;
        newPrismObject = null;
    }

    public boolean isNewObjectModelCreated(){
        return newObjectModel != null;
    }

    /**
     * Check that exist details model for new object that will be added to reference value.
     */
    public boolean existNewObjectModel() {
        return newPrismObject != null;
    }
}
