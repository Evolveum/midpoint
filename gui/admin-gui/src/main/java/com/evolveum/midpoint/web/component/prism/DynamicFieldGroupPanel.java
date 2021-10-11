/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.prism;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyPanel;
import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.MutableItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PolyStringUtils;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.schema.util.FormTypeUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractFormItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormFieldGroupType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FormItemDisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class DynamicFieldGroupPanel<O extends ObjectType> extends BasePanel<PrismObjectWrapper<O>> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(DynamicFieldGroupPanel.class);

    private static final String ID_PROPERTY = "property";
    private static final String ID_HEADER = "header";

    private List<AbstractFormItemType> formItems;

    public DynamicFieldGroupPanel(String id, String groupName, IModel<PrismObjectWrapper<O>> objectWrapper, List<AbstractFormItemType> formItems, Form<?> mainForm, PageBase parentPage) {
        super(id, objectWrapper);
        setParent(parentPage);
        this.formItems = formItems;
        initLayout(groupName, formItems, mainForm);
    }

    public DynamicFieldGroupPanel(String id, IModel<PrismObjectWrapper<O>> objectWrapper, @NotNull FormDefinitionType formDefinition, Form<?> mainForm, PageBase parentPage) {
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

        Label header = new Label(ID_HEADER, getPageBase().getString(groupName, (Object []) null));
        add(header);

        RepeatingView itemView = new RepeatingView(ID_PROPERTY);
        add(itemView);

        for (AbstractFormItemType formItem : formItems) {

            if (formItem instanceof FormFieldGroupType) {
                DynamicFieldGroupPanel<O> dynamicFieldGroupPanel = new DynamicFieldGroupPanel<>(itemView.newChildId(), formItem.getName(), getModel(), FormTypeUtil.getFormItems(((FormFieldGroupType) formItem).getFormItems()), mainForm, getPageBase());
                dynamicFieldGroupPanel.setOutputMarkupId(true);
                itemView.add(dynamicFieldGroupPanel);
                continue;
            }

            ItemWrapper<?,?,?,?> itemWrapper = findAndTailorItemWrapper(formItem, getObjectWrapper());

            try {
                Panel panel = getPageBase().initItemPanel(itemView.newChildId(), itemWrapper.getTypeName(), Model.of(itemWrapper), null);
                panel.setOutputMarkupId(true);
                itemView.add(panel);
            } catch (SchemaException e) {
                getSession().error("Cannot create panel " + e.getMessage());
            }

        }
    }

    private RepeatingView getRepeatingPropertyView() {
        return (RepeatingView) get(ID_PROPERTY);
    }

    @NotNull
    private ItemWrapper<?, ?, ?, ?> findAndTailorItemWrapper(AbstractFormItemType formField, PrismObjectWrapper<O> objectWrapper) {
        ItemWrapper<?, ?, ?, ?> itemWrapper = findItemWrapper(formField, objectWrapper);
        applyFormDefinition(itemWrapper, formField);
        return itemWrapper;
    }

    @NotNull
    private ItemWrapper<?, ?, ?, ?> findItemWrapper(AbstractFormItemType formField, PrismObjectWrapper<O> objectWrapper) {
        ItemPath path = GuiImplUtil.getItemPath(formField);
        if (path == null) {
            getSession().error("Bad form item definition. It has to contain reference to the real attribute");
            LOGGER.error("Bad form item definition. It has to contain reference to the real attribute");
            throw new RestartResponseException(getPageBase());
        }

        ItemWrapper<?,?,?,?> itemWrapper;
        ItemDefinition<?> itemDef = objectWrapper.getObject().getDefinition().findItemDefinition(path);
        try {
            itemWrapper = objectWrapper.findItem(path, ItemWrapper.class);
        } catch (SchemaException e) {
            getSession().error("Bad form item definition. No attribute with path: " + path + " was found");
            LOGGER.error("Bad form item definition. No attribute with path: " + path + " was found");
            throw new RestartResponseException(getPageBase());
        }

        if (itemWrapper == null) {
            getSession().error("Bad form item definition. No attribute with path: " + path + " was found");
            LOGGER.error("Bad form item definition. No attribute with path: " + path + " was found");
            throw new RestartResponseException(getPageBase());
        }
        return itemWrapper;
    }

    private void applyFormDefinition(ItemWrapper<?, ?, ?, ?> itemWrapper, AbstractFormItemType formField) {

        FormItemDisplayType displayType = formField.getDisplay();

        if (displayType == null) {
            return;
        }

        MutableItemDefinition itemDef = itemWrapper.toMutable();
        if (PolyStringUtils.isNotEmpty(displayType.getLabel())) {
            itemDef.setDisplayName(displayType.getLabel().getOrig());
        }
        if (PolyStringUtils.isNotEmpty(displayType.getHelp())) {
            itemDef.setHelp(displayType.getHelp().getOrig());
        }
        if (StringUtils.isNotEmpty(displayType.getMaxOccurs())) {
            itemDef.setMaxOccurs(XsdTypeMapper.multiplicityToInteger(displayType.getMaxOccurs()));
        }
        if (StringUtils.isNotEmpty(displayType.getMinOccurs())) {
            itemDef.setMinOccurs(XsdTypeMapper.multiplicityToInteger(displayType.getMinOccurs()));
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
    public boolean checkRequiredFields(PageBase pageBase) {
        Holder<Boolean> rvHolder = new Holder<>(true);
        getRepeatingPropertyView().visitChildren((component, iVisit) -> {
            if (component instanceof PrismPropertyPanel) {
                IModel<?> model = component.getDefaultModel();
                if (model != null && model.getObject() instanceof ItemWrapper) {
                    ItemWrapper<?, ?, ?, ?> itemWrapper = (ItemWrapper<?, ?, ?, ?>) model.getObject();
                    if (!itemWrapper.checkRequired(pageBase)) {
                        rvHolder.setValue(false);
                    }
                }
            }
        });
        return rvHolder.getValue();
    }
}
