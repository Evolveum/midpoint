package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.BaseSimplePanel;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author Viliam Repan (lazyman)
 */
public class SearchItemPanel extends BaseSimplePanel<SearchItem> {

    private static final Trace LOG = TraceManager.getTrace(SearchItemPanel.class);

    private static final String ID_MAIN_BUTTON = "mainButton";
    private static final String ID_LABEL = "label";
    private static final String ID_DELETE_BUTTON = "deleteButton";
    private static final String ID_POPOVER = "popover";
    private static final String ID_TEXT = "text";
    private static final String ID_UPDATE = "update";
    private static final String ID_CLOSE = "close";


    public SearchItemPanel(String id, IModel<SearchItem> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        AjaxLink mainButton = new AjaxLink(ID_MAIN_BUTTON) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                editPerformed(target);
            }
        };
        add(mainButton);

        Label label = new Label(ID_LABEL, createLabelModel());
        label.setRenderBodyOnly(true);
        mainButton.add(label);

        AjaxLink deleteButton = new AjaxLink(ID_DELETE_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                deletePerformed(target);
            }
        };
        mainButton.add(deleteButton);

        initPopover();
    }

    private void initPopover() {
        WebMarkupContainer popover = new WebMarkupContainer(ID_POPOVER);
        popover.setOutputMarkupId(true);
        add(popover);

        TextField value = new TextField(ID_TEXT, new Model());
        popover.add(value);

        AjaxSubmitButton add = new AjaxSubmitButton(ID_UPDATE, createStringResource("SearchItemPanel.update")) {


            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                //todo implement
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                updateItemPerformed(target);
            }
        };
        popover.add(add);

        AjaxButton close = new AjaxButton(ID_CLOSE, createStringResource("SearchItemPanel.close")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                closeEditPopoverPerformed(target);
            }
        };
        popover.add(close);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(OnDomReadyHeaderItem.forScript(initButtonJavascript()));
    }

    private IModel<String> createLabelModel() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                SearchItem item = getModelObject();

                StringBuilder sb = new StringBuilder();
                sb.append(item.getName());
                sb.append(": ");

                if (StringUtils.isNotEmpty(item.getDisplayValue())) {
                    sb.append('"').append(item.getDisplayValue()).append('"');
                } else {
                    String all = createStringResource("SearchItemPanel.all").getString();
                    sb.append(all);
                }

                return sb.toString();
            }
        };
    }

    private String initButtonJavascript() {
        StringBuilder sb = new StringBuilder();
        String moreId = get(ID_MAIN_BUTTON).getMarkupId();
        String popoverId = get(ID_POPOVER).getMarkupId();

        sb.append("initSearchPopover('").append(moreId);
        sb.append("','").append(popoverId).append("', 0);");

        return sb.toString();
    }

    private void updateItemPerformed(AjaxRequestTarget target) {
        // todo implement
    }

    private void closeEditPopoverPerformed(AjaxRequestTarget target) {
        String popoverId = get(ID_POPOVER).getMarkupId();
        target.appendJavaScript("$('#" + popoverId + "').toggle();");
    }

    private void editPerformed(AjaxRequestTarget target) {
        LOG.debug("Edit performed");
    }

    private void deletePerformed(AjaxRequestTarget target) {
        LOG.debug("Delete performed");

        SearchItem item = getModelObject();
        Search search = item.getSearch();
        search.delete(item);

        SearchPanel panel = findParent(SearchPanel.class);
        panel.refreshForm(target);
    }
}
