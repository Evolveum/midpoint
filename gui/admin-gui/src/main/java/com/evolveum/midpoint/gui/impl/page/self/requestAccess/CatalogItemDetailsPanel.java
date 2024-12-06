/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.factory.wrapper.PrismObjectWrapperFactory;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CatalogItemDetailsPanel extends BasePanel<ObjectType> implements Popupable {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(CatalogItemDetailsPanel.class);

    private static final String DOT_CLASS = RelationPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_OBJECT_WRAPPER = DOT_CLASS + "loadObjectWrapper";

    private static final String ID_PANELS = "panels";
    private static final String ID_PANEL = "panel";

    private static final String ID_BUTTONS = "buttons";
    private static final String ID_ADD = "add";
    private static final String ID_CLOSE = "close";

    private Fragment footer;

    private IModel<List<ContainerPanelConfigurationType>> containers;

    public CatalogItemDetailsPanel(IModel<List<ContainerPanelConfigurationType>> containers, IModel<ObjectType> model) {
        super(Popupable.ID_CONTENT, model);
        this.containers = containers;
        initLayout();
        initFooter();
    }

   @Override
    protected void onInitialize() {
        super.onInitialize();
       getPageBase().getMainPopup().getDialogComponent().add(AttributeAppender.replace("class", "modal-dialog modal-lg modal-dialog-centered modal-dialog-scrollable"));
       getPageBase().getMainPopup().getDialogComponent().add(AttributeAppender.replace("style", ""));
    }

    private void initLayout() {
        ListView<ContainerPanelConfigurationType> panels = new ListView<>(ID_PANELS, containers) {
            @Override
            protected void populateItem(ListItem<ContainerPanelConfigurationType> item) {
                IModel<PrismObjectWrapper<ObjectType>> wrapper = new LoadableModel<>(false) {
                    @Override
                    protected PrismObjectWrapper load() {
                        PageBase page = getPageBase();

                        ObjectType object = CatalogItemDetailsPanel.this.getModelObject();
                        if (object == null) {
                            page.error(createStringResource("CatalogItemDetailsPanel.objectDetailsNull"));
                            throw new RestartResponseException(page);
                        }

                        Task task = page.createSimpleTask(OPERATION_LOAD_OBJECT_WRAPPER);
                        OperationResult result = task.getResult();

                        PrismObject prism = object.asPrismObject();
                        try {
                            PrismObjectWrapperFactory factory = getPageBase().findObjectWrapperFactory(prism.getDefinition());
                            WrapperContext context = new WrapperContext(task, result);
                            context.setReadOnly(true);
                            context.setDetailsPageTypeConfiguration(containers.getObject());
                            context.setCreateIfEmpty(true);

                            return factory.createObjectWrapper(prism, ItemStatus.NOT_CHANGED, context);
                        } catch (Exception ex) {
                            LoggingUtils.logUnexpectedException(LOGGER, "Cannot create wrapper for {} \nReason: {]", ex, prism, ex.getMessage());
                            result.recordFatalError("Cannot create wrapper for " + prism + ", because: " + ex.getMessage(), ex);
                            page.showResult(result);

                            throw new RestartResponseException(page);
                        }
                    }
                };

                SingleContainerPanel container = new SingleContainerPanel(ID_PANEL, wrapper, item.getModelObject());
                item.add(container);
            }
        };
        add(panels);
    }

    private void initFooter() {
        footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        footer.add(new AjaxLink<>(ID_ADD) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                addPerformed(target, CatalogItemDetailsPanel.this.getModel());
            }
        });
        footer.add(new AjaxLink<>(ID_CLOSE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                closePerformed(target, CatalogItemDetailsPanel.this.getModel());
            }
        });
    }

    @Override
    public Component getFooter() {
        return footer;
    }

    @Override
    public int getWidth() {
        return 905;
    }

    @Override
    public int getHeight() {
        return 1139;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public IModel<String> getTitle() {
        return () -> WebComponentUtil.getDisplayNameOrName(getModelObject().asPrismObject());
    }

    @Override
    public Component getContent() {
        return this;
    }

    protected void addPerformed(AjaxRequestTarget target, IModel<ObjectType> model) {

    }

    protected void closePerformed(AjaxRequestTarget target, IModel<ObjectType> model) {

    }
}
