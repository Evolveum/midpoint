/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.input.expression;

import java.io.Serial;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.dialog.OnePanelPopupPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ExpressionWrapper;
import com.evolveum.midpoint.prism.PrismContext;

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.web.util.ExpressionUtil.ExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

import org.jetbrains.annotations.NotNull;

public class ExpressionPanel extends BasePanel<ExpressionType> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TYPE_CHOICE = "typeChoice";
    private static final String ID_TYPE_BUTTON = "typeButton";
    private static final String ID_EVALUATOR_PANEL = "evaluatorPanel";
    private static final String ID_INFO_CONTAINER = "infoContainer";
    private static final String ID_INFO_LABEL = "infoLabel";
    private static final String ID_INFO_ICON = "infoIcon";
    private static final String ID_RESET_BUTTON = "resetButton";

    private final IModel<PrismPropertyWrapper<ExpressionType>> parent;
    private LoadableModel<RecognizedEvaluator> typeModel;
    private LoadableModel<String> helpModel;
    private boolean isEvaluatorPanelExpanded = false;
    private boolean isInColumn = false;

    Model<String> infoLabelModel = Model.of("");

    public enum RecognizedEvaluator {

        AS_IS(ExpressionEvaluatorType.AS_IS, null, null),
        LITERAL(ExpressionEvaluatorType.LITERAL,
                SimpleValueExpressionPanel.class,
                "ExpressionEvaluatorType.LITERAL.show.button"),
        SCRIPT(ExpressionEvaluatorType.SCRIPT,
                ScriptExpressionPanel.class,
                "ExpressionEvaluatorType.SCRIPT.show.button"),
        GENERATE(ExpressionEvaluatorType.GENERATE,
                GenerateExpressionPanel.class,
                "ExpressionEvaluatorType.GENERATE.show.button"),
        ASSOCIATION_FROM_LINK(ExpressionEvaluatorType.ASSOCIATION_FROM_LINK,
                AssociationFromLinkPanel.class,
                null),
        SHADOW_OWNER_REFERENCE_SEARCH(ExpressionEvaluatorType.SHADOW_OWNER_REFERENCE_SEARCH,
                ShadowOwnerReferenceSearchExpressionPanel.class,
                "ExpressionEvaluatorType.SHADOW_OWNER_REFERENCE_SEARCH.show.button"),
        PATH(ExpressionEvaluatorType.PATH,
                PathExpressionPanel.class,
                "ExpressionEvaluatorType.PATH.show.button");

        private final ExpressionEvaluatorType type;
        private final Class<? extends EvaluatorExpressionPanel> evaluatorPanel;
        private final String buttonLabelKeyPrefix;

        RecognizedEvaluator(
                ExpressionEvaluatorType type, Class<? extends EvaluatorExpressionPanel> evaluatorPanel, String buttonLabelKeyPrefix) {
            this.type = type;
            this.evaluatorPanel = evaluatorPanel;
            this.buttonLabelKeyPrefix = buttonLabelKeyPrefix;
        }
    }

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionPanel.class);

    public ExpressionPanel(String id, IModel<ExpressionType> model) {
        this(id, null, model);
    }

    public ExpressionPanel(String id, IModel<PrismPropertyWrapper<ExpressionType>> parent, IModel<ExpressionType> model) {
        super(id, model);
        this.parent = parent;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initDefaultPanelStyle();
        initTypeModels();
        initLayout();
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        Table table = findParent(Table.class);
        isInColumn = table != null;
    }

    private void initTypeModels() {
        if (getModelObject() == null) {
            getModel().setObject(new ExpressionType());
        }

        if (typeModel == null) {
            typeModel = new LoadableModel<>(false) {
                @Override
                protected RecognizedEvaluator load() {
                    String expression = ExpressionUtil.loadExpression(getModelObject(), PrismContext.get(), LOGGER);

                    ExpressionEvaluatorType type = ExpressionUtil.getExpressionType(expression);
                    return recognizeEvaluator(type);
                }

                @Override
                public void setObject(RecognizedEvaluator object) {
                    RecognizedEvaluator oldType = isLoaded() ? getObject() : null;
                    super.setObject(object);
                    if (oldType != null && oldType != object && ExpressionPanel.this.getModelObject() != null) {
                        ExpressionPanel.this.getModelObject().getExpressionEvaluator().clear();
                    }
                }
            };
        }

        if (helpModel == null) {
            helpModel = new LoadableModel<>(false) {
                @Override
                protected String load() {
                    if (StringUtils.isNotEmpty(getModelObject().getDescription())) {
                        return getPageBase().createStringResource(getModelObject().getDescription()).getString();
                    }

                    Class<? extends EvaluatorExpressionPanel> evaluatorPanel = null;
                    if (typeModel != null && typeModel.getObject() != null) {
                        evaluatorPanel = typeModel.getObject().evaluatorPanel;
                    }

                    if (evaluatorPanel != null) {
                        try {
                            Method m = evaluatorPanel.getMethod("getInfoDescription", ExpressionType.class, PageBase.class);
                            return (String) m.invoke(null, getModelObject(), getPageBase());
                        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                            LOGGER.debug("Couldn't find method getInfoDescription in class " + evaluatorPanel.getSimpleName());
                        }

                    }

                    return StringEscapeUtils.escapeHtml4(
                            ExpressionUtil.loadExpression(getModelObject(), PrismContext.get(), LOGGER));
                }
            };
        }
    }

    private boolean useAsIsForNull() {
        return parent != null && parent.getObject() instanceof ExpressionWrapper &&
                (((ExpressionWrapper) parent.getObject()).isAttributeExpression()
                        || ((ExpressionWrapper) parent.getObject()).isFocusMappingExpression());
    }

    private void initLayout() {
        setOutputMarkupId(true);

        WebMarkupContainer infoContainer = new WebMarkupContainer(ID_INFO_CONTAINER);
        infoContainer.setOutputMarkupId(true);
        infoContainer.add(new VisibleBehaviour(() -> !isEvaluatorPanelExpanded));
        add(infoContainer);

        Label infoLabel = new Label(ID_INFO_LABEL, infoLabelModel);
        infoLabel.setOutputMarkupId(true);
        infoLabel.add(new VisibleBehaviour(() -> !isExpressionEmpty() && isInfoLabelNotEmpty()));
        infoContainer.add(infoLabel);

        Label infoIcon = new Label(ID_INFO_ICON);
        infoIcon.add(AttributeModifier.replace("title", helpModel));
        infoIcon.add(new InfoTooltipBehavior());
        infoIcon.add(new VisibleBehaviour(() -> !isExpressionEmpty() && isInfoLabelNotEmpty() && isHelpDescriptionNotEmpty()));
        infoContainer.add(infoIcon);

        AjaxButton resetButton = new AjaxButton(ID_RESET_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                ExpressionPanel.this.getModelObject().getExpressionEvaluator().clear();
                typeModel.reset();
                helpModel.reset();
                infoLabelModel.setObject("");

                updateLabelForExistingEvaluator();

                ExpressionPanel.this.addOrReplace(createTypeChoice());
                ExpressionPanel.this.addOrReplace(createEvaluatorPanel());
                ExpressionPanel.this.addOrReplace(createTypeButton());

                target.add(getEvaluatorPanel());
                target.add(ExpressionPanel.this.get(ID_TYPE_CHOICE));
                target.add(ExpressionPanel.this.get(ID_TYPE_BUTTON));
                target.add(ExpressionPanel.this.get(ID_INFO_CONTAINER));
                target.add(ExpressionPanel.this);
            }
        };
        resetButton.setOutputMarkupId(true);
        resetButton.add(new VisibleBehaviour(this::isResetButtonVisible));
        infoContainer.add(resetButton);

        if (isExpressionEmpty() || typeModel.getObject() != null) {

            updateLabelForExistingEvaluator();

            add(createTypeChoice());
            add(createEvaluatorPanel());
            add(createTypeButton());

        } else {
            WebMarkupContainer choice = new WebMarkupContainer(ID_TYPE_CHOICE);
            choice.setOutputMarkupId(true);
            choice.add(VisibleBehaviour.ALWAYS_INVISIBLE);
            add(choice);
            add(createEvaluatorPanel());
            add(createTypeButton());
            infoLabelModel.setObject(
                    getPageBase().createStringResource("ExpressionPanel.unrecognizedEvaluator").getString());
        }
    }

    private boolean isHelpDescriptionNotEmpty() {
        return helpModel != null && StringUtils.isNotEmpty(helpModel.getObject());
    }

    private Component getEvaluatorPanel() {
        return get(ID_EVALUATOR_PANEL);
    }

    private boolean isInfoLabelNotEmpty() {
        return StringUtils.isNotEmpty(infoLabelModel.getObject());
    }

    private void updateLabelForExistingEvaluator() {
        infoLabelModel.setObject("");

        if (typeModel.getObject() != null && (!RecognizedEvaluator.AS_IS.equals(typeModel.getObject()) || !useAsIsForNull())) {
            if (StringUtils.isNotEmpty(getModelObject().getName())) {
                infoLabelModel.setObject(getPageBase().createStringResource(getModelObject().getName()).getString());
            } else {
                infoLabelModel.setObject(
                        getPageBase().createStringResource("ExpressionPanel.evaluatorIsSet").getString());
            }
        }
    }

    private boolean isResetButtonVisible() {
        return !isExpressionEmpty() && typeModel.getObject() == null;
    }

    private boolean isExpressionEmpty() {
        return StringUtils.isEmpty(ExpressionUtil.loadExpression(getModelObject(), PrismContext.get(), LOGGER));
    }

    private @NotNull Component createTypeChoice() {

        if (isReadOnly()) {
            Label label = new Label(ID_TYPE_CHOICE, typeModel.getObject() != null
                    ? getPageBase().createStringResource(typeModel.getObject().type).getString()
                    : ExpressionPanel.this.getString(RecognizedEvaluator.AS_IS.type));
            label.setOutputMarkupId(true);
            return label;
        }

        DropDownChoicePanel<RecognizedEvaluator> dropDown = new DropDownChoicePanel<>(ID_TYPE_CHOICE,
                typeModel,
                Model.ofList(getChoices()),
                new EnumChoiceRenderer<>() {
                    @Override
                    public Object getDisplayValue(RecognizedEvaluator object) {
                        if (object == null) {
                            return super.getDisplayValue(object);
                        }
                        return getPageBase().createStringResource(object.type).getString();
                    }
                },
                true) {
            @Override
            protected String getNullValidDisplayValue() {
                if (useAsIsForNull()) {
                    return ExpressionPanel.this.getString(RecognizedEvaluator.AS_IS.type);
                }
                return super.getNullValidDisplayValue();
            }
        };
        dropDown.setOutputMarkupId(true);

        dropDown.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                super.onUpdate(target);

                addOrReplace(createEvaluatorPanel());
                addOrReplace(createTypeButton());

                target.add(getEvaluatorPanel());
                target.add(get(ID_TYPE_BUTTON));
                target.add(get(ID_TYPE_CHOICE));
                target.add(ExpressionPanel.this);

                helpModel.reset();
            }
        });

        dropDown.getBaseFormComponent().add(AttributeAppender.append("style", (IModel<String>) () -> {
            if (isButtonShow()) {
                return "border-bottom-right-radius: 0; border-top-right-radius: 0;";
            }
            return null;
        }));

        return dropDown;
    }

    private boolean isButtonShow() {
        RecognizedEvaluator type = typeModel.getObject();
        return type != null && type.evaluatorPanel != null && type.buttonLabelKeyPrefix != null && !isReadOnly();
    }

    private AjaxButton createTypeButton() {
        RecognizedEvaluator type = typeModel.getObject();
        AjaxButton typeButton = new AjaxButton(ID_TYPE_BUTTON) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (isInColumn) {
                    OnePanelPopupPanel popupPanel = new OnePanelPopupPanel(getPageBase().getMainPopupBodyId()) {
                        @Override
                        protected WebMarkupContainer createPanel(String id) {
                            return createEvaluatorPanel(id, true);
                        }

                        @Override
                        public IModel<String> getTitle() {
                            Component panel = getPanel();
                            if (panel instanceof EvaluatorExpressionPanel) {
                                return ((EvaluatorExpressionPanel) panel).getValueContainerLabelModel();
                            }
                            return null;
                        }

                        @Override
                        protected void processHide(AjaxRequestTarget target) {
                            super.processHide(target);
                            updateLabelForExistingEvaluator();
                            target.add(ExpressionPanel.this.get(ID_INFO_CONTAINER));
                            target.add(ExpressionPanel.this);

                            helpModel.reset();
                        }
                    };
                    getPageBase().showMainPopup(popupPanel, target);

                } else {
                    isEvaluatorPanelExpanded = !isEvaluatorPanelExpanded;

                    if (!ExpressionPanel.this.getModelObject().getExpressionEvaluator().isEmpty()) {
                        updateLabelForExistingEvaluator();
                    }

                    target.add(getEvaluatorPanel());
                    target.add(ExpressionPanel.this.get(ID_INFO_CONTAINER));
                    target.add(ExpressionPanel.this.get(ID_TYPE_BUTTON));
                    target.add(ExpressionPanel.this);

                    helpModel.reset();
                }
            }

            @Override
            public IModel<?> getBody() {
                if (type == null) {
                    return Model.of();
                }
                return getPageBase().createStringResource(type.buttonLabelKeyPrefix + "." + isEvaluatorPanelExpanded());

            }
        };
        typeButton.add(new VisibleBehaviour(this::isButtonShow));
        typeButton.setOutputMarkupId(true);
        return typeButton;
    }

    private WebMarkupContainer createEvaluatorPanel() {
        return createEvaluatorPanel(ExpressionPanel.ID_EVALUATOR_PANEL, false);
    }

    private WebMarkupContainer createEvaluatorPanel(String id, boolean isInPopup) {
        RecognizedEvaluator type = typeModel.getObject();
        if (type != null && type.evaluatorPanel != null) {
            try {
                Constructor<? extends BasePanel> constructor = type.evaluatorPanel.getConstructor(String.class, IModel.class);
                BasePanel evaluatorPanel = constructor.newInstance(id, getModel());
                evaluatorPanel.setOutputMarkupId(true);
                evaluatorPanel.add(new VisibleBehaviour(() -> isInPopup || isEvaluatorPanelExpanded()));
                if (!isInColumn) {
                    evaluatorPanel.add(AttributeAppender.append("class", "pl-3"));
                }
                return evaluatorPanel;
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                LOGGER.error("Couldn't create panel for expression evaluator by constructor for class "
                        + type.evaluatorPanel.getSimpleName() + " with parameters type: String, IModel");
            }
        }
        WebMarkupContainer invisiblePanel = new WebMarkupContainer(id);
        invisiblePanel.add(VisibleBehaviour.ALWAYS_INVISIBLE);
        invisiblePanel.setOutputMarkupId(true);
        return invisiblePanel;
    }

    private boolean isEvaluatorPanelExpanded() {
        return isEvaluatorPanelExpanded;
    }

    protected List<RecognizedEvaluator> getChoices() {
        List<RecognizedEvaluator> choices = new ArrayList<>(Arrays.asList(RecognizedEvaluator.values()));
        choices.removeIf(choice -> RecognizedEvaluator.AS_IS == choice);
        return choices;
    }

    private RecognizedEvaluator recognizeEvaluator(ExpressionEvaluatorType type) {
        Optional<RecognizedEvaluator> recognizedEvaluator = Arrays.stream(RecognizedEvaluator.values())
                .filter(evaluator -> evaluator.type == type).findFirst();
        return recognizedEvaluator.orElse(null);
    }

    protected void initDefaultPanelStyle() {
        if(isReadOnly()){
            add(AttributeModifier.append("class", "d-flex align-items-center gap-2"));
        }
    }

    protected boolean isReadOnly() {
        return false;
    }
}
