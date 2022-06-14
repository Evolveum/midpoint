/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel.component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.CheckFormGroup;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

import javax.xml.namespace.QName;

/**
 * @author katka
 */
public abstract class ListContainersPopup<C extends Containerable, CV extends PrismContainerValueWrapper<C>>
        extends BasePanel<CV> implements Popupable {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ListContainersPopup.class);

    private static final String ID_SELECTED = "selected";
    private static final String ID_DEFINITION = "definition";
    private static final String ID_SELECT = "select";
    private static final String ID_CONTAINERS = "containers";
    private static final String ID_BUTTON_CANCEL = "cancelButton";

    public ListContainersPopup(String id, IModel<CV> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private ContainersPopupDto createContainersPopupDto(PrismContainerDefinition<C> def) {
        return new ContainersPopupDto(false, def) {

            @Override
            public String getDisplayName() {
                if (getModelObject().getDefinition() == null) {
                    return super.getDisplayName();
                }
                QName typeName = getModelObject().getDefinition().getTypeName();
                if (typeName == null) {
                    return super.getDisplayName();
                }
                return typeName.getLocalPart() + "." + getItemName();
            }
        };
    }

    private void initLayout() {

        IModel<List<ContainersPopupDto>> popupModel = new LoadableModel<>(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<ContainersPopupDto> load() {
                List<PrismContainerDefinition<C>> defs;
                try {
                    defs = getModelObject().getChildContainers();
                } catch (SchemaException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Cannot get children containers for {}, reason {}", e, getModelObject(), e.getMessage());
                    getSession().error("ListContainersPopup.children.list.failed");
                    defs = new ArrayList<>();
                }
                List<ContainersPopupDto> modelObject = defs.stream().filter(def -> def.isExperimental() ? WebModelServiceUtils.isEnableExperimentalFeature(getPageBase()) : true)
                                .map(def -> createContainersPopupDto(def))
                                        .collect(Collectors.toList());

                return modelObject;
            }
        };

        ListView<ContainersPopupDto> listView = new ListView<ContainersPopupDto>(ID_CONTAINERS, popupModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ContainersPopupDto> item) {

                CheckFormGroup checkFormGroup = new CheckFormGroup(ID_SELECTED, new PropertyModel<>(item.getModel(), "selected"),
                        new StringResourceModel("ListContainersPopup.selected"), "col-md-2", "col-md-10") {

                    protected boolean getLabelVisible() {
                        return false;
                    }

                };
                checkFormGroup.getCheck().add(new EmptyOnChangeAjaxFormUpdatingBehavior());
                checkFormGroup.add(AttributeAppender.append("class", " checkbox-without-margin-bottom "));
                checkFormGroup.setOutputMarkupId(true);
                item.add(checkFormGroup);

                String displayNameKey = item.getModelObject() != null ? item.getModelObject().getDisplayName() : "";
                Label definition = new Label(ID_DEFINITION, new StringResourceModel(displayNameKey));
                definition.setOutputMarkupId(true);
                item.add(definition);
            }

        };
        listView.setOutputMarkupId(true);
        listView.setReuseItems(true);
        add(listView);

        AjaxButton select = new AjaxButton(ID_SELECT, new StringResourceModel("ListContainerPopup.select")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);

                ListView<ContainersPopupDto> listView = (ListView<ContainersPopupDto>) ListContainersPopup.this.get(ID_CONTAINERS);
                List<PrismContainerDefinition<?>> selected = new ArrayList<>();
                listView.getModelObject().forEach(child -> {
                    if (child.isSelected()) {
                        selected.add(child.getDef());
                    }

                });
                processSelectedChildren(target, selected);
            }
        };
        select.setOutputMarkupId(true);
        add(select);

        AjaxButton cancel = new AjaxButton(ID_BUTTON_CANCEL,
                createStringResource("PageBase.button.cancel")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        };
        add(cancel);
    }

    protected abstract void processSelectedChildren(AjaxRequestTarget target, List<PrismContainerDefinition<?>> selected);

    @Override
    public int getWidth() {
        return 20;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("ListContainersPopup.availableContainers");
    }

}

