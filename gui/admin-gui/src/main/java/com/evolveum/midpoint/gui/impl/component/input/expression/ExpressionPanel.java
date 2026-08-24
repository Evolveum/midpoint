/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.CollapsedItem;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.DrawerModel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ExpressionWrapper;
import com.evolveum.midpoint.prism.PrismContext;

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
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

import javax.xml.namespace.QName;

import org.apache.wicket.model.StringResourceModel;
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
    private boolean displayHelp = true;

    /**
     * Copy of the expression the script drawer works with, kept until the user applies it or closes
     * the drawer.
     */
    private IModel<ExpressionType> editedExpression;

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
                "ExpressionEvaluatorType.PATH.show.button"),
        FILTER(ExpressionEvaluatorType.FILTER,
                FilterExpressionPanel.class,
                "ExpressionEvaluatorType.FILTER.show.button"),
        NULL(ExpressionEvaluatorType.NULL,
                null, null);

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
    }

    private boolean isInTable() {
        return findParent(Table.class) != null || findParent(TileTablePanel.class) != null;
    }

    private @NotNull ExpressionType getOrCreateExpression() {
        ExpressionType expression = getModelObject();
        if (expression == null) {
            expression = new ExpressionType();
            getModel().setObject(expression);
        }
        return expression;
    }

    private void initTypeModels() {
        //TODO it cause creating new replace delta if (why it was here?)
//        if (getModelObject() == null) {
//            getModel().setObject(new ExpressionType());
//        }

        if (typeModel == null) {
            typeModel = new LoadableModel<>(false) {
                @Override
                protected RecognizedEvaluator load() {
                    ExpressionEvaluatorType type = ExpressionUtil.getExpressionType(getModelObject());
                    return recognizeEvaluator(type);
                }

                @Override
                public void setObject(RecognizedEvaluator object) {
                    RecognizedEvaluator oldType = isLoaded() ? getObject() : null;
                    super.setObject(object);

                    // No subpanel writes the "null" evaluator into ExpressionType, so it updated on selection.
                    if (object != null && object.equals(RecognizedEvaluator.NULL)) {
                        ExpressionType currentExpression = ExpressionPanel.this.getOrCreateExpression();
                        ExpressionUtil.addNullExpressionValue(currentExpression);
                    } else if (oldType != null && oldType != object && ExpressionPanel.this.getModelObject() != null) {
                        ExpressionPanel.this.getModelObject().getExpressionEvaluator().clear();
                    }
                }
            };
        }

        if (helpModel == null) {
            helpModel = new LoadableModel<>(false) {
                @Override
                protected String load() {
                    if (getModelObject() == null) {
                        return "";
                    }
                    if (StringUtils.isNotEmpty(getModelObject().getDescription())) {
                        StringResourceModel stringResource = getPageBase().createStringResource(getModelObject().getDescription());
                        return StringEscapeUtils.escapeHtml4(stringResource.getString());
                    }

                    Class<? extends EvaluatorExpressionPanel> evaluatorPanel = null;
                    if (typeModel != null && typeModel.getObject() != null) {
                        evaluatorPanel = typeModel.getObject().evaluatorPanel;
                    }

                    if (evaluatorPanel != null) {
                        try {
                            Method m = evaluatorPanel.getMethod("getInfoDescription", ExpressionType.class, PageBase.class);
                            String description = (String) m.invoke(null, getModelObject(), getPageBase());
                            return StringEscapeUtils.escapeHtml4(description);
                        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                            LOGGER.debug("Couldn't find method getInfoDescription in class {}", evaluatorPanel.getSimpleName());
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
        infoContainer.add(new VisibleBehaviour(() -> !isEvaluatorPanelExpanded && (!isInTable() || isReadOnly()) && displayHelp));
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

        AjaxButton resetButton = buildResetButton();
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

    private @NotNull AjaxButton buildResetButton() {
        AjaxButton resetButton = new AjaxButton(ID_RESET_BUTTON) {
            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                ExpressionType expression = getOrCreateExpression();
                expression.getExpressionEvaluator().clear();
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
        return resetButton;
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

        if (getModelObject() != null
                && typeModel.getObject() != null
                && (!RecognizedEvaluator.AS_IS.equals(typeModel.getObject())
                || !useAsIsForNull())) {
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
            label.add(AttributeModifier.replace("class", "form-select form-select-sm text-nowrap "
                    + getAdditionalCssClassForTypeChoice()));
            label.add(AttributeModifier.replace("style", ""));
            label.add(new ExpressionValidationBehavior(typeModel, getModel()));
            return label;
        }

        DropDownChoicePanel<RecognizedEvaluator> dropDown = createDropDownTypeChoice();
        dropDown.getBaseFormComponent()
                .add(new ExpressionValidationBehavior(typeModel, getModel()));

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

    protected String getAdditionalCssClassForTypeChoice() {
        return null;
    }

    private @NotNull DropDownChoicePanel<RecognizedEvaluator> createDropDownTypeChoice() {
        DropDownChoicePanel<RecognizedEvaluator> dropDown = new DropDownChoicePanel<>(ID_TYPE_CHOICE,
                typeModel,
                Model.ofList(getChoices()),
                new EnumChoiceRenderer<>() {
                    @Override
                    public Object getDisplayValue(RecognizedEvaluator object) {
                        if (object == null) {
                            return super.getDisplayValue(null);
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
        dropDown.setRenderBodyOnly(true); // Important for validation behavior.
        return dropDown;
    }

    private boolean isButtonShow() {
        RecognizedEvaluator type = typeModel.getObject();
        return type != null && type.evaluatorPanel != null && type.buttonLabelKeyPrefix != null && !isReadOnly();
    }

    private AjaxIconButton createTypeButton() {
        RecognizedEvaluator type = typeModel.getObject();

        AjaxIconButton typeButton = new AjaxIconButton(ID_TYPE_BUTTON, Model.of("fa fa-gear"),
                () -> type != null
                        ? getPageBase().createStringResource(
                        type.buttonLabelKeyPrefix + "." + isEvaluatorPanelExpanded()).getString() : "") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                if (isInTable()) {
                    if (RecognizedEvaluator.SCRIPT.equals(typeModel.getObject())) {
                        showScriptDrawer(target);
                        return;
                    }
                    DrawerModel drawerModel = new DrawerModel(Model.ofList(getDrawerCollapsedItems()));
                    getPageBase().showDrawer(drawerModel, target);
                } else {
                    isEvaluatorPanelExpanded = !isEvaluatorPanelExpanded;
                    if (ExpressionPanel.this.getModelObject() != null
                            && !ExpressionPanel.this.getModelObject().getExpressionEvaluator().isEmpty()) {
                        updateLabelForExistingEvaluator();
                    }

                    target.add(getEvaluatorPanel());
                    target.add(ExpressionPanel.this.get(ID_INFO_CONTAINER));
                    target.add(ExpressionPanel.this.get(ID_TYPE_BUTTON));
                    target.add(ExpressionPanel.this);

                    helpModel.reset();
                }
            }
        };
        typeButton.add(new VisibleBehaviour(this::isButtonShow));
        typeButton.setOutputMarkupId(true);
        return typeButton;
    }

    private void showScriptDrawer(AjaxRequestTarget target) {
        ExpressionType expression = getModelObject();
        editedExpression = Model.of(expression != null ? expression.clone() : new ExpressionType());

        DrawerModel drawerModel = new ScriptExpressionDrawerModel(
                Model.ofList(getDrawerCollapsedItems()), editedExpression) {

            @Override
            protected void storePerformed(ExpressionType edited, AjaxRequestTarget target) {
                getModel().setObject(ExpressionUtil.hasEvaluatorContent(edited) ? edited : null);
                closeScriptDrawer(target);
            }

            @Override
            protected void closePerformed(AjaxRequestTarget target) {
                closeScriptDrawer(target);
            }
        };
        getPageBase().showDrawer(drawerModel, target);
    }

    private void closeScriptDrawer(AjaxRequestTarget target) {
        editedExpression = null;
        getPageBase().hideDrawer(target);

        typeModel.reset();
        helpModel.reset();
        updateLabelForExistingEvaluator();

        target.add(this);
    }

    private IModel<ExpressionType> getDrawerEditModel() {
        return editedExpression != null ? editedExpression : getModel();
    }

    /**
     * Returns type of the object the expression is evaluated against, taken from the schema context of the item.
     *
     * @return target object type of expression or null while the schema context is not defined for the expression.
     */
    private QName resolveExpressionTargetType() {
        PrismPropertyWrapper<ExpressionType> wrapper = parent != null ? parent.getObject() : null;
        if (wrapper == null) {
            return null;
        }

        var item = wrapper.getItem();
        if (item == null || item.getValue() == null) {
            return null;
        }
        var schemaContext = item.getValue().getSchemaContext();
        if (schemaContext == null || schemaContext.getItemDefinition() == null) {
            return null;
        }
        return schemaContext.getItemDefinition().getTypeName();
    }

    private WebMarkupContainer createEvaluatorPanel() {
        return createEvaluatorPanel(ExpressionPanel.ID_EVALUATOR_PANEL, false, getModel());
    }

    private WebMarkupContainer createEvaluatorPanel(String id, boolean isInPopup, IModel<ExpressionType> model) {
        RecognizedEvaluator type = typeModel.getObject();
        if (type != null && type.evaluatorPanel != null) {
            try {
                Constructor<? extends BasePanel<ExpressionType>> constructor =
                        type.evaluatorPanel.getConstructor(String.class, IModel.class, IModel.class);
                BasePanel<ExpressionType> evaluatorPanel =
                        constructor.newInstance(id, model, (IModel<QName>) this::resolveExpressionTargetType);
                evaluatorPanel.setOutputMarkupId(true);
                evaluatorPanel.add(new VisibleBehaviour(() -> isInPopup || isEvaluatorPanelExpanded()));
                if (!isInTable()) {
                    evaluatorPanel.add(AttributeAppender.append("class", "ps-3"));
                }
                return evaluatorPanel;
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                LOGGER.error("Couldn't create panel for expression evaluator by constructor for class {} with parameters type: "
                        + "String, IModel, IModel", type.evaluatorPanel.getSimpleName());
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

    public static RecognizedEvaluator recognizeEvaluator(ExpressionEvaluatorType type) {
        Optional<RecognizedEvaluator> recognizedEvaluator = Arrays.stream(RecognizedEvaluator.values())
                .filter(evaluator -> evaluator.type == type).findFirst();
        return recognizedEvaluator.orElse(null);
    }

    protected void initDefaultPanelStyle() {
        if (isReadOnly()) {
            add(AttributeModifier.append("class", "d-flex align-items-center gap-2"));
        }
    }

    protected boolean isReadOnly() {
        return false;
    }

    /**
     * Shows the panel of the evaluator right away, without waiting for the user to expand it.
     * @param expanded true to show the evaluator panel from the start.
     */
    public void setEvaluatorPanelExpanded(boolean expanded) {
        this.isEvaluatorPanelExpanded = expanded;
    }

    public void setDisplayHelp(boolean displayHelp) {
        this.displayHelp = displayHelp;
    }

    public List<CollapsedItem<DrawerModel>> getDrawerCollapsedItems() {
        List<CollapsedItem<DrawerModel>> collapsedItems = new ArrayList<>();
        CollapsedItem<DrawerModel> collapsedItem = new CollapsedItem<>() {
            @Override
            public IModel<String> getIcon() {
                return Model.of("fa fa-code");
            }

            @Override
            public Component getPanel(String id, DrawerModel model) {
                WebMarkupContainer panel = createEvaluatorPanel(id, true, getDrawerEditModel());
                if (panel instanceof EvaluatorExpressionPanel evaluatorPanel) {
                    setTitleModel(evaluatorPanel.getValueContainerLabelModel(getPageBase()));
                }
                return panel;
            }
        };
        collapsedItem.setSelected(true);
        collapsedItems.add(collapsedItem);
        return collapsedItems;
    }
}
