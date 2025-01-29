package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.AdvancedQueryWrapper;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.AxiomQueryWrapper;
import com.evolveum.midpoint.prism.Containerable;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.query.lang.AxiomQueryContentAssistImpl;
import com.evolveum.midpoint.prism.query.AxiomQueryContentAssist;
import com.evolveum.midpoint.prism.query.ContentAssist;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

import com.evolveum.midpoint.web.page.admin.configuration.component.QueryPlaygroundPanel;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.session.SessionStorage;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.request.cycle.RequestCycle;

import javax.xml.namespace.QName;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class AxiomSearchPanel extends BasePanel<AxiomQueryWrapper> {

    private static final Trace LOGGER = TraceManager.getTrace(QueryPlaygroundPanel.class);
    private static final String ID_AXIOM_QUERY_FIELD = "axiomQueryField";
    private static final String ID_ADVANCED_ERROR = "advancedError";
    private static String idAxiomQueryInputField;

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

        idAxiomQueryInputField = queryDslField.getMarkupId();
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

        ObjectMapper mapper = new ObjectMapper();

        queryDslField.add(new AjaxFormComponentUpdatingBehavior("keyup") {
            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                super.updateAjaxAttributes(attributes);
                attributes.setThrottlingSettings(
                        new ThrottlingSettings(ID_AXIOM_QUERY_FIELD, Duration.ofMillis(300), true)
                );

                attributes.getDynamicExtraParameters().add(
                        "return {'cursorPosition': window.MidPointTheme.cursorPosition || 0};"
                );
            }

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                AxiomQueryWrapper axiomQueryWrapper = getModel().getObject();
                IRequestParameters params = RequestCycle.get().getRequest().getRequestParameters();

                if (axiomQueryWrapper != null) {
                    ItemDefinition<?> rootDef = axiomQueryWrapper.getContainerDefinitionOverride() != null ?
                            axiomQueryWrapper.getContainerDefinitionOverride() :
                            getPrismContext().getSchemaRegistry().findItemDefinitionByType(new QName(axiomQueryWrapper.getTypeClass().getSimpleName()));

                    try {
                        target.appendJavaScript("window.MidPointTheme.syncContentAssist(" +
                                mapper.writeValueAsString(new AxiomQueryContentAssistImpl(getPrismContext()).process(
                                        rootDef,
                                        axiomQueryWrapper.getDslQuery() == null ? "" : axiomQueryWrapper.getDslQuery(),
                                        params.getParameterValue("cursorPosition").toInt()
                                )) + ", '" + idAxiomQueryInputField + "');"
                        );
                    } catch (Exception e) {
                        LOGGER.error(e.getMessage());
                    }
                }
            }
        });

        queryDslField.add(AttributeAppender.append("onkeydown", "window.MidPointTheme.triggerAutocompleteShortcut(event, this);"));
        queryDslField.add(AttributeAppender.append("autocomplete", "off"));
        add(queryDslField);

        Label advancedError = new Label(ID_ADVANCED_ERROR,
                new PropertyModel<String>(getModel(), AdvancedQueryWrapper.F_ERROR));
        advancedError.setOutputMarkupId(true);
        advancedError.add(AttributeAppender.append("class",
                () -> StringUtils.isEmpty(getModelObject().getAdvancedError()) ? "valid-feedback" : "invalid-feedback"));
        advancedError.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(getModelObject().getAdvancedError())));
        add(advancedError);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(OnDomReadyHeaderItem.forScript("window.MidPointTheme.initAxiomSearchPanel('" +  idAxiomQueryInputField + "');"));
    }
}
