package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;

import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class CompositedButtonPanel extends BasePanel<CompositedIconButtonDto> {

    private static final String ID_COMPOSITED_ICON = "compositedIcon";
    private static final String ID_LABEL = "label";
    private static final String ID_DESCRIPTION = "description";

    public CompositedButtonPanel(String id, IModel<CompositedIconButtonDto> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        CompositedIconPanel compositedIconPanel = new CompositedIconPanel(ID_COMPOSITED_ICON, new PropertyModel<>(getModel(), CompositedIconButtonDto.F_COMPOSITED_ICON));
        add(compositedIconPanel);

        Label label = new Label(ID_LABEL, new ReadOnlyModel<>(() -> {
            DisplayType displayType = getModelObject().getAdditionalButtonDisplayType();
            return WebComponentUtil.getTranslatedPolyString(displayType.getLabel());
        }));
        add(label);

        compositedIconPanel.add(AttributeAppender.append("title", new ReadOnlyModel<>(() -> {
            DisplayType displayType = getModelObject().getAdditionalButtonDisplayType();
            return WebComponentUtil.getTranslatedPolyString(displayType.getTooltip());
        })));
        compositedIconPanel.add(new TooltipBehavior());

        compositedIconPanel.add(new AjaxEventBehavior("click"){

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                onButtonClicked(target, getModelObject());
            }
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                attributes.setPreventDefault(true);
                super.updateAjaxAttributes(attributes);
            }

        });


//        Label description = new Label(ID_DESCRIPTION, new ReadOnlyModel<>(() -> {
//            DisplayType displayType = getModelObject().getAdditionalButtonDisplayType();
//            return WebComponentUtil.getTranslatedPolyString(displayType.getTooltip());
//        }));
//        add(description);
    }

    protected void onButtonClicked(AjaxRequestTarget target, CompositedIconButtonDto buttonDescription) {

    }
}
