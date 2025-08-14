/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.gui.impl.component.data.column.icon.RoundedImagePanel;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.IResource;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.page.admin.simulation.DetailsTableItem;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

public abstract class ObjectVerticalSummaryPanel<O extends ObjectType> extends BasePanel<O> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_LOGO = "logo";
    private static final String ID_TITLE = "title";
    private static final String ID_DETAILS_CONTAINER = "detailsContainer";
    private static final String ID_DETAILS = "details";
    private static final String ID_DETAILS_COMPONENT_1 = "detailsComponent1";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_DETAILS_COMPONENT_2 = "detailsComponent2";

    private IModel<List<DetailsTableItem>> detailsItemsModel;
    private IModel<DisplayType> displayTypeModel;

    public ObjectVerticalSummaryPanel(String id, IModel<O> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initModels();
        initLayout();
    }

    protected String getIconBoxAdditionalCssClass() {
        return "summary-panel-shadow";
    }

    private void initModels() {
        if (detailsItemsModel == null) {
            detailsItemsModel = createDetailsItems();
        }

        if (displayTypeModel == null) {
            displayTypeModel = createDisplayTypeModel();
        }
    }

    protected IModel<DisplayType> createDisplayTypeModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected DisplayType load() {
                O object = ObjectVerticalSummaryPanel.this.getModelObject();

                if (object == null) {
                    return null;
                }

                OperationResult result =  new OperationResult("getIcon");
                DisplayType type =  GuiDisplayTypeUtil.getDisplayTypeForObject(object, result, getPageBase());
                if (type == null ) {
                    type = new DisplayType();
                } else {
                    // clone to avoid immutable property modification
                    type = type.clone();
                }

                if (type.getIcon() == null) {
                    type.icon(new IconType());
                }

                IconType icon = type.getIcon();
                icon.setCssClass(StringUtils.joinWith(" ", icon.getCssClass(), "fa-inverse fa-2x"));

                String name = WebComponentUtil.getDisplayNameOrName(getModelObject().asPrismObject());
                if (StringUtils.isEmpty(name)) {
                    name = getTitleForNewObject(getModelObject()).getObject();
                }
                type.label(name).help(defineDescription(getModelObject()));

                return type;
            }
        };
    }

    protected String defineDescription(O object) {
        return object.getDescription();
    }

    protected abstract IModel<String> getTitleForNewObject(O modelObject);

    protected @NotNull IModel<List<DetailsTableItem>> createDetailsItems() {
        return Model.ofList(Collections.EMPTY_LIST);
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "p-0"));

        RoundedImagePanel logo = new RoundedImagePanel(ID_LOGO, displayTypeModel, createPreferredImage()) {
            @Override
            protected String getCssClassesIconContainer() {
                return "info-box-icon " + getIconBoxAdditionalCssClass();
            }
        };
        add(logo);

        IModel<String> titleModel = getTitleModel();
        Label title = new Label(ID_TITLE, titleModel);
        title.add(AttributeAppender.replace("title", titleModel));
        title.add(new TooltipBehavior());
        title.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(titleModel.getObject())));
        add(title);

        IModel<String> descriptionModel = getDescriptionModel();
        Label description = new Label(ID_DESCRIPTION, descriptionModel);
        description.add(AttributeAppender.replace("title", descriptionModel));
        description.add(new TooltipBehavior());
        description.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(descriptionModel.getObject())));
        add(description);

        WebMarkupContainer detailsContainer = new WebMarkupContainer(ID_DETAILS_CONTAINER);
        detailsContainer.setOutputMarkupId(true);
        detailsContainer.add(
                new VisibleBehaviour(() -> detailsItemsModel.getObject() != null && !detailsItemsModel.getObject().isEmpty()));
        add(detailsContainer);

        ListView<DetailsTableItem> details = new ListView<>(ID_DETAILS, detailsItemsModel) {

            @Override
            protected void populateItem(@NotNull ListItem<DetailsTableItem> item) {
                DetailsTableItem data = item.getModelObject();
                if (data.isValueComponentBeforeLabel()) {
                    item.add(item.getModelObject().createValueComponent(ID_DETAILS_COMPONENT_1));
                    item.add(new Label(ID_DETAILS_COMPONENT_2, () -> data.getLabel().getObject()));
                } else {
                    item.add(new Label(ID_DETAILS_COMPONENT_1, () -> data.getLabel().getObject()));
                    item.add(data.createValueComponent(ID_DETAILS_COMPONENT_2));
                }

                if (data.isVisible() != null) {
                    item.add(data.isVisible());
                }

                if (!(detailsItemsModel.getObject().size() < 2)) {
                    if (item.getIndex() != 0) {
                        item.add(AttributeAppender.append("class", "border-top"));
                    }
                }
            }
        };
        detailsContainer.add(details);
    }

    private IModel<IResource> createPreferredImage() {
        return new LoadableModel<>(false) {
            @Override
            protected IResource load() {
                O object = getModelObject();
                if (object instanceof FocusType focus) {
                    return WebComponentUtil.createJpegPhotoResource(focus);
                }

                return null;
            }
        };
    }

    protected @NotNull IModel<String> getTitleModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                if (displayTypeModel.getObject() == null) {
                    return "";
                }
                return GuiDisplayTypeUtil.getTranslatedLabel(displayTypeModel.getObject());
            }
        };
    }

    protected @NotNull IModel<String> getDescriptionModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                if (displayTypeModel.getObject() == null) {
                    return "";
                }
                return GuiDisplayTypeUtil.getHelp(displayTypeModel.getObject());
            }
        };

    }

}
