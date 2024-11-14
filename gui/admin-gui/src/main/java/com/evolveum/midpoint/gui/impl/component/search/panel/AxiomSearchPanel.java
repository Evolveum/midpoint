package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.AdvancedQueryWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.AxiomQueryWrapper;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class AxiomSearchPanel extends BasePanel<AxiomQueryWrapper> {

    private static final String ID_AXIOM_QUERY_FIELD = "axiomQueryField";
    private static final String ID_ADVANCED_ERROR = "advancedError";

    public AxiomSearchPanel(String id, IModel<AxiomQueryWrapper> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        TextField<String> queryDslField = new TextField<>(ID_AXIOM_QUERY_FIELD,
                new PropertyModel<>(getModel(), AxiomQueryWrapper.F_DSL_QUERY));
        queryDslField.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());

//        queryDslField.add(new AjaxFormComponentUpdatingBehavior("keyup") {
//
//            @Override
//            protected void onUpdate(AjaxRequestTarget target) {
//                updateQueryDSLArea(advancedCheck, advancedGroup, target);
//            }
//
//            @Override
//            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
//                super.updateAjaxAttributes(attributes);
//
//                attributes.setThrottlingSettings(
//                        new ThrottlingSettings(ID_AXIOM_QUERY_FIELD, Duration.ofMillis(500), true));
//                attributes.setChannel(new AjaxChannel("Drop", AjaxChannel.Type.DROP));
//            }
//        });

        queryDslField.add(AttributeAppender.append("class",
                () -> StringUtils.isEmpty(getModelObject().getAdvancedError()) ? "is-valid" : "is-invalid"));
        queryDslField.add(AttributeAppender.append("placeholder", getPageBase().createStringResource("SearchPanel.insertAxiomQuery")));
        queryDslField.add(AttributeAppender.append("title", getPageBase().createStringResource("SearchPanel.insertAxiomQuery")));
        add(queryDslField);

        Label advancedError = new Label(ID_ADVANCED_ERROR,
                new PropertyModel<String>(getModel(), AdvancedQueryWrapper.F_ERROR));
        advancedError.setOutputMarkupId(true);
        advancedError.add(AttributeAppender.append("class",
                () -> StringUtils.isEmpty(getModelObject().getAdvancedError()) ? "valid-feedback" : "invalid-feedback"));
        advancedError.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getModelObject().getAdvancedError())));
        add(advancedError);
    }

//    private void updateQueryDSLArea(Component child, Component parent, AjaxRequestTarget target) {
//
//        target.appendJavaScript("$('#" + child.getMarkupId() + "').updateParentClass('fa-check-circle-o', 'has-success',"
//                + " '" + parent.getMarkupId() + "', 'fa-exclamation-triangle', 'has-error');");
//
//        target.add(
//                get(createComponentPath(ID_FORM, ID_ADVANCED_GROUP, ID_ADVANCED_CHECK)),
//                get(createComponentPath(ID_FORM, ID_ADVANCED_GROUP, ID_ADVANCED_ERROR_GROUP)),
//                get(createComponentPath(ID_FORM, ID_SEARCH_CONTAINER)));
//    }

}
