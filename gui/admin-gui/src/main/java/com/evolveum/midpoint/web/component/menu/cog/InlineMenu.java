package com.evolveum.midpoint.web.component.menu.cog;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

import java.util.List;

/**
 * @author lazyman
 */
public class InlineMenu extends SimplePanel<List<InlineMenuItem>> {

    private static final String ID_LI = "li";
    private static final String ID_A = "a";
    private static final String ID_SPAN = "span";

    private boolean hideByDefault;

    public InlineMenu(String id, IModel model) {
        this(id, model, false);
    }

    public InlineMenu(String id, IModel model, boolean hideByDefault) {
        super(id, model);
        this.hideByDefault = hideByDefault;

        setOutputMarkupId(true);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        StringBuilder sb = new StringBuilder();
        sb.append("initInlineMenu('").append(getMarkupId()).append("', ").append(hideByDefault).append(");");

        response.render(OnDomReadyHeaderItem.forScript(sb.toString()));
    }

    @Override
    protected void initLayout() {
        ListView<InlineMenuItem> li = new ListView<InlineMenuItem>(ID_LI, getModel()) {

            @Override
            protected void populateItem(ListItem<InlineMenuItem> item) {
                final InlineMenuItem dto = item.getModelObject();

                item.add(AttributeModifier.replace("class", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        if (dto.getAction() == null) {
                            return "divider";
                        }

                        return getBoolean(dto.getEnabled(), true) ? "" : "disabled";
                    }
                }));

                if (dto.getEnabled() != null || dto.getVisible() != null) {
                    item.add(new VisibleEnableBehaviour() {

                        @Override
                        public boolean isEnabled() {
                            return getBoolean(dto.getEnabled(), true);
                        }

                        @Override
                        public boolean isVisible() {
                            return getBoolean(dto.getVisible(), true);
                        }
                    });
                }

                initLink(item);
            }
        };
        add(li);
    }

    private void initLink(ListItem<InlineMenuItem> item) {
        final InlineMenuItem dto = item.getModelObject();

        AbstractLink a;
        if (dto.isSubmit()) {
            a = new AjaxSubmitLink(ID_A) {

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                    InlineMenu.this.onSubmit(target, form, dto.getAction());
                }

                @Override
                protected void onError(AjaxRequestTarget target, Form<?> form) {
                    InlineMenu.this.onError(target, form, dto.getAction());
                }
            };
        } else {
            a = new AjaxLink(ID_A) {

                @Override
                public void onClick(AjaxRequestTarget target) {
                    InlineMenu.this.onClick(target, dto.getAction());
                }
            };
        }
        item.add(a);

        a.setBeforeDisabledLink("");
        a.setAfterDisabledLink("");
        a.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                if (dto.getAction() == null) {
                    return false;
                }
                return true;
            }
        });

        Label span = new Label(ID_SPAN, dto.getLabel());
        span.setRenderBodyOnly(true);
        a.add(span);
    }

    protected void onSubmit(AjaxRequestTarget target, Form<?> form, InlineMenuItemAction action) {
        if (action != null) {
            action.onSubmit(target, form);
        }
    }

    protected void onError(AjaxRequestTarget target, Form<?> form, InlineMenuItemAction action) {
        if (action != null) {
            action.onError(target, form);
        }
    }

    protected void onClick(AjaxRequestTarget target, InlineMenuItemAction action) {
        if (action != null) {
            action.onClick(target);
        }
    }

    private boolean getBoolean(IModel<Boolean> model, boolean def) {
        if (model == null) {
            return def;
        }

        Boolean value = model.getObject();
        return value != null ? value : def;
    }
}
