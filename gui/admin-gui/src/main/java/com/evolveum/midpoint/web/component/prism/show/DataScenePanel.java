/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class DataScenePanel extends Panel {

    private static final String STRIPED_CLASS = "striped";
    private static final String ID_HEADER = "header";
    private static final String ID_ITEMS = "items";
    private static final String ID_ITEM = "item";
    private static final String ID_PARTIAL_SCENES = "partialScenes";
    private static final String ID_PARTIAL_SCENE = "partialScene";

    private static final Trace LOGGER = TraceManager.getTrace(DataScenePanel.class);

    private boolean showHeader = true;
    private PageBase pageBase;

    public DataScenePanel(String id, IModel<DataSceneDto> model, Form form, PageBase pageBase) {
        super(id);
        setOutputMarkupId(true);

        LOGGER.trace("Creating object panel for {}", model.getObject());

        this.pageBase = pageBase;
        initLayout(model, form);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(
                new PackageResourceReference(DataScenePanel.class, "DataScenePanel.css")));

        StringBuilder sb = new StringBuilder();
        sb.append("fixStripingOnPrismForm('").append(getMarkupId()).append("', '").append(STRIPED_CLASS).append("');");
        response.render(OnDomReadyHeaderItem.forScript(sb.toString()));
    }

    private AjaxEventBehavior createHeaderOnClickBehaviour(final IModel<DataSceneDto> model) {
        return new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                headerOnClickPerformed(target, model);
            }
        };
    }

    private IModel<String> createHeaderClassModel(final IModel<DataSceneDto> model) {
        return new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return getSimpleName(model);
            }
        };
    }

    private String getSimpleName(IModel<DataSceneDto> model) {
        return model.getObject().getScene().getName() != null ? model.getObject().getScene().getName().getSimpleName() : null;
    }

    private IModel<String> createHeaderNameClassModel(final IModel<DataSceneDto> model) {
        return new AbstractReadOnlyModel<String>() {
            @Override
            public String getObject() {
                return "";
            }
        };
    }

    protected Component createHeader(String id, IModel<DataSceneDto> model) {
        return new H3Header2(id, model);
    }

    protected List<InlineMenuItem> createDefaultMenuItems(IModel<DataSceneDto> model) {
        List<InlineMenuItem> items = new ArrayList<>();

        InlineMenuItem item = new InlineMenuItem(createMinMaxLabel(model), createMinMaxAction(model));
        items.add(item);

        return items;
    }

    private InlineMenuItemAction createMinMaxAction(final IModel<DataSceneDto> model) {
        return new InlineMenuItemAction() {

            @Override
            public void onClick(AjaxRequestTarget target) {
                DataSceneDto dto = model.getObject();
                dto.setMinimized(!dto.isMinimized());
                target.add(DataScenePanel.this);
            }
        };
    }

    private IModel<String> createMinMaxLabel(final IModel<DataSceneDto> model) {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                DataSceneDto dto = model.getObject();
                String key = dto.isMinimized() ? "PrismObjectPanel.maximize" : "PrismObjectPanel.minimize";
                return PageBase.createStringResourceStatic(DataScenePanel.this, key).getString();
            }
        };
    }

    private void initLayout(final IModel<DataSceneDto> model, final Form form) {
        Component headerComponent = createHeader(ID_HEADER, model);
//        if (headerComponent instanceof H3Header2) {
//            headerComponent.setVisible(false);
//        }
        add(headerComponent);

        WebMarkupContainer headerPanel = new WebMarkupContainer("headerPanel");
        headerPanel.add(new AttributeAppender("class", createHeaderClassModel(model), " "));
        add(headerPanel);
        headerPanel.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return isShowHeader();

            }
        });

        Label header = new Label("header", createDisplayName(model));
        header.add(new AttributeAppender("class", createHeaderNameClassModel(model), " "));
        header.add(createHeaderOnClickBehaviour(model));
        headerPanel.add(header);
        Label description = new Label("description", createDescription(model));
        description.add(new AttributeModifier("title", createDescription(model)));

        description.add(createHeaderOnClickBehaviour(model));
        headerPanel.add(description);

        initButtons(headerPanel, model);

        WebMarkupContainer body = new WebMarkupContainer("body");
        body.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                DataSceneDto wrapper = model.getObject();
                return !wrapper.isMinimized();
            }
        });
        add(body);

		ISortableDataProvider provider = new ListDataProvider(this, new PropertyModel<>(model.getObject(), DataSceneDto.F_ITEMS));
		BoxedTablePanel table = new BoxedTablePanel(ID_ITEMS, provider, initItemsColumns());
		table.setShowPaging(false);
		table.setOutputMarkupId(true);
		body.add(table);

        ListView<DataSceneDto> containers = new ListView<DataSceneDto>(ID_PARTIAL_SCENES, new PropertyModel<List<DataSceneDto>>(model, DataSceneDto.F_PARTIAL_SCENES)) {
            @Override
            protected void populateItem(ListItem<DataSceneDto> item) {
                DataScenePanel panel = new DataScenePanel(ID_PARTIAL_SCENE, item.getModel(), form, pageBase);
				panel.setOutputMarkupPlaceholderTag(true);
				item.add(panel);
            }
        };
        containers.setReuseItems(true);
        body.add(containers);
    }

	private List<IColumn<ItemDto, String>> initItemsColumns() {
		List<IColumn<ItemDto, String>> columns = new ArrayList<>();

		columns.add(new PropertyColumn(PageBase.createStringResourceStatic(this, "DataContextPanel.item.name"), ItemDto.F_NAME));
		columns.add(new PropertyColumn(PageBase.createStringResourceStatic(this, "DataContextPanel.item.oldValue"), ItemDto.F_OLD_VALUE));
		columns.add(new PropertyColumn(PageBase.createStringResourceStatic(this, "DataContextPanel.item.newValue"), ItemDto.F_NEW_VALUE));
		return columns;
	}

    protected IModel<String> createDisplayName(IModel<DataSceneDto> model) {
        return new PropertyModel<>(model, "scene.name.displayName");
    }

    protected IModel<String> createDescription(IModel<DataSceneDto> model) {
        return new PropertyModel<>(model, "scene.name.description");
    }

    private void initButtons(WebMarkupContainer headerPanel, final IModel<DataSceneDto> model) {
        headerPanel.add(new SceneButtonPanel("optionButtons", (IModel<DataSceneDto>) model) {
        @Override
            public void minimizeOnClick(AjaxRequestTarget target) {
                DataSceneDto dto = model.getObject();
                dto.setMinimized(!dto.isMinimized());
                target.add(DataScenePanel.this);
            }
        });
        headerPanel.add(createOperationPanel("operationButtons"));
    }

    protected Panel createOperationPanel(String id) {
        return new EmptyPanel(id);
    }

    public boolean isShowHeader() {
        return showHeader;
    }

    public void setShowHeader(boolean showHeader) {
        this.showHeader = showHeader;
    }

    public void headerOnClickPerformed(AjaxRequestTarget target, IModel<DataSceneDto> model) {
        DataSceneDto dto = model.getObject();
        dto.setMinimized(!dto.isMinimized());
        target.add(DataScenePanel.this);
    }
}
