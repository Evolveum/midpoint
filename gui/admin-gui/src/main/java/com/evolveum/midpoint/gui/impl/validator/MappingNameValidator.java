/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.validator;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.processor.ResourceSchemaFactory;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.jetbrains.annotations.NotNull;

public class MappingNameValidator implements IValidator<String> {

    private static final Trace LOGGER = TraceManager.getTrace(MappingNameValidator.class);

    private final IModel<PrismPropertyWrapper<String>> itemModel;

    public MappingNameValidator(IModel<PrismPropertyWrapper<String>> itemModel) {
        this.itemModel = itemModel;
    }

    public IModel<PrismPropertyWrapper<String>> getItemModel() {
        return itemModel;
    }

    @Override
    public void validate(IValidatable<String> validatable) {
        String value = validatable.getValue();
        if (StringUtils.isEmpty(value)) {
            return;
        }

        PrismPropertyWrapper<String> item = itemModel.getObject();
        if (item == null) {
            return;
        }

        PrismObjectWrapper<ObjectType> objectWrapper = item.findObjectWrapper();

        int numberOfSameRef = WebPrismUtil.getNumberOfSameMappingNames(objectWrapper.getValue(), value);

        boolean containsSameValue = false;

        try {
            containsSameValue = item.getValue() != null
                    && item.getValue().getRealValue() != null
                    && StringUtils.equals(item.getValue().getRealValue(), value);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get value of " + item, e);
        }

        if ((containsSameValue && numberOfSameRef > 1) || (!containsSameValue && numberOfSameRef > 0)) {
            ValidationError error = new ValidationError();
            error.setMessage(LocalizationUtil.translate("MappingNameValidator.sameValue", new Object[] {value}));
            validatable.error(error);
        }

    }

    protected final boolean alreadyExistMapping(
            PrismContainerValueWrapper prismContainerValue, String errorMessage, String value, IValidatable<String> validatable) {
        int numberOfSameRef = WebPrismUtil.getNumberOfSameMappingNames(prismContainerValue, value);

        boolean containsSameValue = false;

        PrismPropertyWrapper<String> item = getItemModel().getObject();
        try {
            containsSameValue = item.getValue() != null
                    && item.getValue().getRealValue() != null
                    && StringUtils.equals(item.getValue().getRealValue(), value);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get value of " + item, e);
        }

        if ((containsSameValue && numberOfSameRef > 1) || (!containsSameValue && numberOfSameRef > 0)) {
            ValidationError error = new ValidationError();
            error.setMessage(errorMessage);
            validatable.error(error);
            return true;
        }

        return false;
    }

    protected final <C extends Containerable> void alreadyExistMapping(
            C bean, PrismContainerDefinition<C> def, String errorMessage, String value, IValidatable<String> validatable, PageBase pageBase) {

        @NotNull PrismContainer<C> parent;
        try {
            parent = def.instantiate();
            bean.asPrismContainerValue().setParent(parent);
            bean.asPrismContainerValue().applyDefinition(def);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't apply definition to association bean", e);
            return ;
        }

        Task task = pageBase.createSimpleTask("createWrapperForAssociation");
        WrapperContext context = new WrapperContext(task, task.getResult());
        context.setObjectStatus(ItemStatus.NOT_CHANGED);
        context.setShowEmpty(true);
        context.setCreateIfEmpty(false);

        try {
            ItemWrapper parentWrapper = pageBase.createItemWrapper(parent, ItemStatus.NOT_CHANGED, context);

            PrismContainerValueWrapper wrapper = pageBase.createValueWrapper(
                    parentWrapper, bean.asPrismContainerValue(), ValueStatus.NOT_CHANGED, context);
            int numberOfSameRef = WebPrismUtil.getNumberOfSameMappingNames(wrapper, value);

            if (numberOfSameRef > 0) {

                ValidationError error = new ValidationError();
                error.setMessage(errorMessage);
                validatable.error(error);
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't create wrapper for " + bean, e);
        }
    }

    protected final ResourceSchema getResourceSchema(PageBase pageBase) {
        ResourceSchema schema = null;
        PrismPropertyWrapper<String> item = getItemModel().getObject();
        try {
            schema = ResourceSchemaFactory.getCompleteSchema(
                    (ResourceType) item.findObjectWrapper().getObjectOld().asObjectable(), LayerType.PRESENTATION);
        } catch (Exception e) {
            LOGGER.debug("Couldn't get complete resource schema", e);
        }

        if (schema == null) {
            schema = ResourceDetailsModel.getResourceSchema(item.findObjectWrapper(), pageBase);
        }

        return schema;
    }
}
