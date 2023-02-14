package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

public class SimpleContainerPanel extends Border {

    private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";
    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    private static final String ID_CONTENT = "content";

    private IModel<String> titleModel;

    private IModel<Boolean> contentVisibleModel;

    public SimpleContainerPanel(@NotNull String id, @NotNull IModel<String> titleModel) {
        this(id, titleModel, Model.of(true));
    }

    public SimpleContainerPanel(@NotNull String id, @NotNull IModel<String> titleModel, @NotNull IModel<Boolean> contentVisibleModel) {
        super(id);

        this.titleModel = titleModel;
        this.contentVisibleModel = contentVisibleModel;

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "simple-container"));
        setOutputMarkupId(true);

        AjaxLink<Void> link = new AjaxLink<>(ID_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onTitlePerformed(target);
            }
        };
        addToBorder(link);

        WebComponent icon = new WebComponent(ID_ICON);
        icon.add(AttributeAppender.append("class", () -> contentVisibleModel.getObject() ? "fa-solid fa-caret-down" : "fa-solid fa-caret-right"));
        link.add(icon);

        Label title = new Label(ID_TITLE, titleModel);
        link.add(title);

        Component content = createContent(ID_CONTENT);
        content.add(new VisibleBehaviour(() -> contentVisibleModel.getObject()));
        addToBorder(content);
    }

    private void onTitlePerformed(AjaxRequestTarget target) {
        contentVisibleModel.setObject(!contentVisibleModel.getObject());

        target.add(this);
    }

    protected Component createContent(String id) {
        return new WebMarkupContainer(ID_CONTENT);
    }
}
