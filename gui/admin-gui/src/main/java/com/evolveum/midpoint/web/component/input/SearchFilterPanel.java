/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.input;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AceEditor;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.web.util.WebXmlUtil;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * @author shood
 */
public class SearchFilterPanel<T extends SearchFilterType> extends BasePanel<T> {

    private static final Trace LOGGER = TraceManager.getTrace(SearchFilterPanel.class);

    private static final String ID_DESCRIPTION = "description";
    private static final String ID_FILTER_CLAUSE = "filterClause";
    private static final String ID_BUTTON_UPDATE = "update";
    private static final String ID_T_CLAUSE = "filterClauseTooltip";

    @NotNull private final IModel<String> clauseStringModel;

    public SearchFilterPanel(String id, @NotNull final NonEmptyModel<T> filterModel, @NotNull NonEmptyModel<Boolean> readOnlyModel) {
        super(id, filterModel);
        clauseStringModel = new LoadableModel<String>(false) {
            @Override
            protected String load() {
                return loadFilterClause(getPageBase().getPrismContext());
            }
        };
        initLayout(readOnlyModel);
    }

    private String loadFilterClause(PrismContext prismContext) {
        try {
            T filter = getModelObject();
            if (filter.containsFilterClause()) {
                RootXNode clause = filter.getFilterClauseAsRootXNode();
                String xml = prismContext.xmlSerializer().serialize(clause);
                return WebXmlUtil.stripNamespaceDeclarations(xml);
            } else {
                return null;
            }
        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Could not load filterClause from SearchFilterType object.", e);
            // TODO - find better solution to inform user about fail in filterClause loading
            return e.getMessage();
        }
    }

    protected void initLayout(NonEmptyModel<Boolean> readOnlyModel) {

        TextArea<String> description = new TextArea<>(ID_DESCRIPTION,
                new PropertyModel<>(getModel(), SearchFilterType.F_DESCRIPTION.getLocalPart()));
        description.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
        add(description);

        AceEditor clause = new AceEditor(ID_FILTER_CLAUSE, clauseStringModel);
        clause.add(WebComponentUtil.enabledIfFalse(readOnlyModel));
        add(clause);

        AjaxSubmitLink update = new AjaxSubmitLink(ID_BUTTON_UPDATE) {

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                updateClausePerformed(target);
            }
        };
        update.add(WebComponentUtil.visibleIfFalse(readOnlyModel));
        add(update);

        Label clauseTooltip = new Label(ID_T_CLAUSE);
        clauseTooltip.add(new InfoTooltipBehavior());
        add(clauseTooltip);
    }

    private void updateClausePerformed(AjaxRequestTarget target) {
        try {
            updateFilterClause(getPageBase().getPrismContext());
            success(getString("SearchFilterPanel.message.expressionSuccess"));
        } catch (Exception e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Could not create MapXNode from provided XML filterClause.", e);
            error(getString("SearchFilterPanel.message.cantSerialize", e.getMessage()));
        }

//        performFilterClauseHook(target);
        target.add(getPageBase().getFeedbackPanel());
    }

    private void updateFilterClause(PrismContext context) throws SchemaException {
        final String clauseString = clauseStringModel.getObject();
        if (StringUtils.isNotEmpty(clauseString)) {
            LOGGER.trace("Filter Clause to serialize: {}", clauseString);
            RootXNode filterClauseNode = ExpressionUtil.parseSearchFilter(clauseString, context);
            getModelObject().setFilterClauseXNode(filterClauseNode);
        } else {
            if (getModelObject() != null) {
                getModelObject().setFilterClauseXNode((MapXNode) null);
            }
        }
    }

}
