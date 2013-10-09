package com.evolveum.midpoint.web.component.menu.cog;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.parser.XmlTag;
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

    public InlineMenu(String id, IModel model) {
        super(id, model);
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

                AbstractLink a;
                if (dto.isSubmit()) {
                    a = new AjaxSubmitLink(ID_A) {

                        @Override
                        protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                            InlineMenuItemAction action = dto.getAction();
                            if (action != null) {
                                action.onSubmit(target, form);
                            }
                        }

                        @Override
                        protected void onError(AjaxRequestTarget target, Form<?> form) {
                            InlineMenuItemAction action = dto.getAction();
                            if (action != null) {
                                action.onError(target, form);
                            }
                        }
                    };
                } else {
                    a = new AjaxLink(ID_A) {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            InlineMenuItemAction action = dto.getAction();
                            if (action != null) {
                                action.onClick(target);
                            }
                        }
                    };
                }
                a.setBeforeDisabledLink("");
                a.setAfterDisabledLink("");
                item.add(a);
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
        };
        add(li);
    }

    private boolean getBoolean(IModel<Boolean> model, boolean def) {
        if (model == null) {
            return def;
        }

        Boolean value = model.getObject();
        return value != null ? value : def;
    }

    private static abstract class MenuAjaxLink extends AjaxLink<String> {

        public MenuAjaxLink(String id, IModel<String> model) {
            super(id, model);
        }

        @Override
        public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
            replaceComponentTagBody(markupStream, openTag, getDefaultModelObjectAsString());
        }

        @Override
        protected void onComponentTag(ComponentTag tag) {
            super.onComponentTag(tag);

            if (tag.isOpenClose()) {
                tag.setType(XmlTag.TagType.OPEN);
            }
        }

        @Override
        public String getAfterDisabledLink() {
            return null;
        }

        @Override
        public String getBeforeDisabledLink() {
            return null;
        }
    }
}
