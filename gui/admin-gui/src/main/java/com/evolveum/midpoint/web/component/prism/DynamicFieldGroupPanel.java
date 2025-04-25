/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.prism;

import java.io.Serial;
import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageAdminLTE;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import com.evolveum.midpoint.gui.api.util.WebPrismUtil;

import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismPropertyPanel;

import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismReferencePanel;

import com.evolveum.midpoint.prism.ItemDefinition;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyPanel;
import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PolyStringUtils;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.util.FormTypeUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class DynamicFieldGroupPanel<O extends ObjectType> extends BasePanel<PrismObjectWrapper<O>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(DynamicFieldGroupPanel.class);

    private static final String ID_PROPERTY = "property";
    private static final String ID_HEADER = "header";

    private final List<AbstractFormItemType> formItems;

    public DynamicFieldGroupPanel(String id, String groupName, IModel<PrismObjectWrapper<O>> objectWrapper, List<AbstractFormItemType> formItems, Form<?> mainForm, PageAdminLTE parentPage) {
        super(id, objectWrapper);
        setParent(parentPage);
        this.formItems = formItems;
        initLayout(groupName, formItems, mainForm);
    }

    public DynamicFieldGroupPanel(String id, IModel<PrismObjectWrapper<O>> objectWrapper, @NotNull FormDefinitionType formDefinition, Form<?> mainForm, PageAdminLTE parentPage) {
        super(id, objectWrapper);
        setParent(parentPage);
        this.formItems = FormTypeUtil.getFormItems(formDefinition.getFormItems());
        initLayout(getGroupName(formDefinition), formItems, mainForm);
    }

    private String getGroupName(@NotNull FormDefinitionType formDefinition) {
        if (formDefinition.getDisplay() != null) {
            return formDefinition.getDisplay().getLabel().getOrig();
        } else {
            return "Basic";
        }
    }

    private void initLayout(String groupName, List<AbstractFormItemType> formItems, Form<?> mainForm) {
        PageAdminLTE parentPage = WebComponentUtil.getPage(DynamicFieldGroupPanel.this, PageAdminLTE.class);

        Label header = new Label(ID_HEADER, getString(groupName, (Object[]) null));
        add(header);

        RepeatingView itemView = new RepeatingView(ID_PROPERTY);
        add(itemView);

        for (AbstractFormItemType formItem : formItems) {

            if (formItem instanceof FormFieldGroupType) {
                DynamicFieldGroupPanel<O> dynamicFieldGroupPanel = new DynamicFieldGroupPanel<>(itemView.newChildId(),
                        formItem.getName(), getModel(), FormTypeUtil.getFormItems(((FormFieldGroupType) formItem).getFormItems()), mainForm, parentPage);
                dynamicFieldGroupPanel.setOutputMarkupId(true);
                itemView.add(dynamicFieldGroupPanel);
                continue;
            }

            IModel<ItemWrapper<?, ?>> model = () -> findAndTailorItemWrapper(formItem, getObjectWrapper());
            Panel panel = WebPrismUtil.createVerticalPropertyPanel(itemView.newChildId(), model, null);
            if (panel instanceof VerticalFormPrismPropertyPanel<?>) {
                ((VerticalFormPrismPropertyPanel)panel).setRequiredTagVisibleInHeaderPanel(isMandatory(model));
            } else if (panel instanceof VerticalFormPrismReferencePanel<?>) {
                ((VerticalFormPrismReferencePanel)panel).setRequiredTagVisibleInHeaderPanel(isMandatory(model));
            }
            itemView.add(panel);
        }
    }

    private boolean isMandatory(IModel<ItemWrapper<?, ?>> model) {
        return model != null && model.getObject() != null && model.getObject().getMinOccurs() > 0;
    }

    private RepeatingView getRepeatingPropertyView() {
        return (RepeatingView) get(ID_PROPERTY);
    }

    @NotNull
    private ItemWrapper<?, ?> findAndTailorItemWrapper(AbstractFormItemType formField, PrismObjectWrapper<O> objectWrapper) {
        ItemWrapper<?, ?> itemWrapper = findItemWrapper(formField, objectWrapper);
        applyFormDefinition(itemWrapper, formField);
        return itemWrapper;
    }

    @NotNull
    private ItemWrapper<?, ?> findItemWrapper(AbstractFormItemType formField, PrismObjectWrapper<O> objectWrapper) {
        ItemPath path = GuiImplUtil.getItemPath(formField);
        if (path == null) {
            logErrorAndThrowException("Bad form item definition. It has to contain reference to the real attribute");
        }

        ItemWrapper<?, ?> itemWrapper = null;
        try {
            itemWrapper = objectWrapper.findItem(path, ItemWrapper.class);
        } catch (SchemaException e) {
            logErrorAndThrowException("Bad form item definition. No attribute with path: " + path + " was found");
        }

        if (itemWrapper == null) {
            logErrorAndThrowException("Bad form item definition. No attribute with path: " + path + " was found");
        }
        return itemWrapper;
    }

    private void logErrorAndThrowException(String errorMessage) {
        getSession().error(errorMessage);
        LOGGER.error(errorMessage);
        throw new RestartResponseException(WebComponentUtil.getPage(DynamicFieldGroupPanel.this, PageAdminLTE.class));
    }

    private void applyFormDefinition(ItemWrapper<?, ?> itemWrapper, AbstractFormItemType formField) {

        FormItemDisplayType displayType = formField.getDisplay();

        if (displayType == null) {
            return;
        }

        ItemDefinition.ItemDefinitionMutator itemDefMutator = itemWrapper.mutator();
        if (PolyStringUtils.isNotEmpty(displayType.getLabel())) {
            itemDefMutator.setDisplayName(displayType.getLabel().getOrig());
        }
        if (PolyStringUtils.isNotEmpty(displayType.getHelp())) {
            itemDefMutator.setHelp(displayType.getHelp().getOrig());
        }
        if (StringUtils.isNotEmpty(displayType.getMaxOccurs())) {
            itemDefMutator.setMaxOccurs(XsdTypeMapper.multiplicityToInteger(displayType.getMaxOccurs()));
        }
        if (StringUtils.isNotEmpty(displayType.getMinOccurs())) {
            itemDefMutator.setMinOccurs(XsdTypeMapper.multiplicityToInteger(displayType.getMinOccurs()));
        }
    }

    public PrismObjectWrapper<O> getObjectWrapper() {
        return getModelObject();
    }

    public List<AbstractFormItemType> getFormItems() {
        return formItems;
    }

    /**
     * Checks embedded properties if they are the minOccurs check.
     * Experimental implementation. Please do not rely on it too much.
     */
    public boolean checkRequiredFields() {
        Holder<Boolean> rvHolder = new Holder<>(true);
        getRepeatingPropertyView().visitChildren((component, iVisit) -> {
            if (component instanceof PrismPropertyPanel) {
                IModel<?> model = component.getDefaultModel();
                if (model != null && model.getObject() instanceof ItemWrapper<?, ?> itemWrapper) {
                    if (!itemWrapper.checkRequired()) {
                        rvHolder.setValue(false);
                    }
                }
            }
        });
        return rvHolder.getValue();
    }
}
