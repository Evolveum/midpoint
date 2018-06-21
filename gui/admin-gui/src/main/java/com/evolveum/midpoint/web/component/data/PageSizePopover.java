package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.SearchFormEnterBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.validation.validator.RangeValidator;

/**
 * @author lazyman
 */
public class PageSizePopover extends BasePanel {

    private static final String ID_POP_BUTTON = "popButton";
    private static final String ID_POPOVER = "popover";
    private static final String ID_FORM = "form";
    private static final String ID_INPUT = "input";
    private static final String ID_BUTTON = "button";

    public PageSizePopover(String id) {
        super(id);
        setRenderBodyOnly(true);
        initLayout();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        String buttonId = get(ID_POP_BUTTON).getMarkupId();

        StringBuilder sb = new StringBuilder();
        sb.append("initPageSizePopover('").append(buttonId);
        sb.append("','").append(get(ID_POPOVER).getMarkupId());
        sb.append("','").append(buttonId);
        sb.append("');");

        response.render(OnDomReadyHeaderItem.forScript(sb.toString()));
    }


    protected void initLayout() {
        Button popButton = new Button(ID_POP_BUTTON);
        popButton.setOutputMarkupId(true);
        add(popButton);

        WebMarkupContainer popover = new WebMarkupContainer(ID_POPOVER);
        popover.setOutputMarkupId(true);
        add(popover);

        Form form = new com.evolveum.midpoint.web.component.form.Form(ID_FORM);
        popover.add(form);

        AjaxSubmitButton button = new AjaxSubmitButton(ID_BUTTON) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                super.onError(target, form);
                target.add(getPageBase().getFeedbackPanel());
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                super.onSubmit(target, form);

                pageSizeChanged(target);
            }
        };
        form.add(button);

        TextField input = new TextField(ID_INPUT, createInputModel());
        input.add(new RangeValidator(5, 100));
        input.setLabel(createStringResource("PageSizePopover.title"));
        input.add(new SearchFormEnterBehavior(button));
        input.setType(Integer.class);
        form.add(input);
    }

    private IModel<Integer> createInputModel() {
        return new IModel<Integer>() {

            @Override
            public Integer getObject() {
                TablePanel tablePanel = findParent(TablePanel.class);
                UserProfileStorage.TableId tableId = tablePanel.getTableId();

                return getPageBase().getSessionStorage().getUserProfile().getPagingSize(tableId);
            }

            @Override
            public void setObject(Integer o) {
                TablePanel tablePanel = findParent(TablePanel.class);
                UserProfileStorage.TableId tableId = tablePanel.getTableId();

                getPageBase().getSessionStorage().getUserProfile().setPagingSize(tableId, o);
            }

            @Override
            public void detach() {
            }
        };
    }

    protected void pageSizeChanged(AjaxRequestTarget target) {
    }
}
