/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.preview.expression;

import java.util.Arrays;
import java.util.Optional;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.web.util.ExpressionUtil.ExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

/**
 * Preview panel for {@link ExpressionType}.
 */
public class PreviewExpressionPanel extends BasePanel<ExpressionType> {

    private static final Trace LOGGER = TraceManager.getTrace(PreviewExpressionPanel.class);

    private static final String ID_EXPRESSION_LABEL = "expressionLabel";
    private static final String ID_TYPE_BADGE = "typeBadge";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_DETAILS = "details";

    public enum RecognizedEvaluator {
        AS_IS(ExpressionEvaluatorType.AS_IS,
                "ExpressionEvaluatorType.AS_IS"),
        LITERAL(ExpressionEvaluatorType.LITERAL,
                "ExpressionEvaluatorType.LITERAL"),
        SCRIPT(ExpressionEvaluatorType.SCRIPT,
                "ExpressionEvaluatorType.SCRIPT"),
        GENERATE(ExpressionEvaluatorType.GENERATE,
                "ExpressionEvaluatorType.GENERATE"),
        ASSOCIATION_FROM_LINK(ExpressionEvaluatorType.ASSOCIATION_FROM_LINK,
                "ExpressionEvaluatorType.ASSOCIATION_FROM_LINK"),
        SHADOW_OWNER_REFERENCE_SEARCH(ExpressionEvaluatorType.SHADOW_OWNER_REFERENCE_SEARCH,
                "ExpressionEvaluatorType.SHADOW_OWNER_REFERENCE_SEARCH"),
        PATH(ExpressionEvaluatorType.PATH,
                "ExpressionEvaluatorType.PATH");

        private final ExpressionEvaluatorType type;
        private final String translationKey;

        RecognizedEvaluator(ExpressionEvaluatorType type, String translationKey) {
            this.type = type;
            this.translationKey = translationKey;
        }

        public ExpressionEvaluatorType getType() {
            return type;
        }

        public String getTranslationKey() {
            return translationKey;
        }
    }

    public PreviewExpressionPanel(String id, IModel<ExpressionType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        RecognizedEvaluator evaluator = resolveEvaluator();

        add(new Label(ID_EXPRESSION_LABEL, () -> isCondition()
                ? createStringResource("PreviewExpressionPanel.condition").getString()
                : createStringResource("PreviewExpressionPanel.expression").getString()));

        add(new Label(ID_TYPE_BADGE, Model.of(getBadgeLabel(evaluator)))
                .add(AttributeModifier.append("class", "badge bg-primary px-2 py-1")));

        add(new Label(ID_DESCRIPTION, Model.of(getDescription())).setEscapeModelStrings(false));

        add(createDetailsPanel(evaluator));
    }

    protected boolean isCondition() {
        return false;
    }

    private String getDescription() {
        ExpressionType expression = getModelObject();

        if (expression != null && StringUtils.isNotEmpty(expression.getDescription())) {
            StringResourceModel resource = getPageBase().createStringResource(expression.getDescription());
            return StringEscapeUtils.escapeHtml4(resource.getString());
        }

        RecognizedEvaluator evaluator = resolveEvaluator();

        if (evaluator == null) {
            return "";
        }

        if (isCondition()) {
            return getPageBase()
                    .createStringResource("ExpressionPreviewPanel.conditionDescription")
                    .getString();
        }

        return getPageBase()
                .createStringResource("ExpressionPreviewPanel.description." + evaluator.name())
                .getString();
    }

    private RecognizedEvaluator resolveEvaluator() {
        String expression = ExpressionUtil.loadExpression(getModelObject(), PrismContext.get(), LOGGER);
        ExpressionEvaluatorType type = ExpressionUtil.getExpressionType(expression);
        return recognizeEvaluator(type);
    }

    private @NotNull Component createDetailsPanel(RecognizedEvaluator evaluator) {
        if (evaluator == null) {
            return invisiblePanel();
        }

        return switch (evaluator) {
            case SCRIPT -> new ScriptExpressionPreviewDetailsPanel(PreviewExpressionPanel.ID_DETAILS, getModel());
            case LITERAL -> new LiteralExpressionPreviewDetailsPanel(PreviewExpressionPanel.ID_DETAILS, getModel());
            case PATH -> new PathExpressionPreviewDetailsPanel(PreviewExpressionPanel.ID_DETAILS, getModel());
            case GENERATE -> new GenerateExpressionPreviewDetailsPanel(PreviewExpressionPanel.ID_DETAILS, getModel());
            default -> invisiblePanel();
        };
    }

    private @NotNull WebMarkupContainer invisiblePanel() {
        WebMarkupContainer panel = new WebMarkupContainer(PreviewExpressionPanel.ID_DETAILS);
        panel.add(VisibleBehaviour.ALWAYS_INVISIBLE);
        panel.setOutputMarkupPlaceholderTag(true);
        return panel;
    }

    private String getBadgeLabel(RecognizedEvaluator evaluator) {
        String key = evaluator != null
                ? evaluator.getTranslationKey()
                : "ExpressionEvaluatorType.AS_IS";

        return getString(key);
    }

    public static RecognizedEvaluator recognizeEvaluator(ExpressionEvaluatorType type) {
        Optional<RecognizedEvaluator> recognizedEvaluator = Arrays.stream(RecognizedEvaluator.values())
                .filter(evaluator -> evaluator.type == type)
                .findFirst();
        return recognizedEvaluator.orElse(null);
    }
}
