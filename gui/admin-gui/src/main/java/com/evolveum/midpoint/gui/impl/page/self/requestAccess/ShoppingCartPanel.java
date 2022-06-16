/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType.SKIP;

import java.util.*;

import com.evolveum.midpoint.web.component.util.EnableBehaviour;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.wizard.Badge;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStepPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.self.dto.ConflictDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ShoppingCartPanel extends WizardStepPanel<RequestAccess> {

    private static final long serialVersionUID = 1L;

    private static final List<ValidityPredefinedValueType> DEFAULT_VALIDITY_PERIODS = Arrays.asList(
            new ValidityPredefinedValueType()
                    .duration(XmlTypeConverter.createDuration("P1D"))
                    .display(new DisplayType().label("ShoppingCartPanel.validity1Day")),
            new ValidityPredefinedValueType()
                    .duration(XmlTypeConverter.createDuration("P7D"))
                    .display(new DisplayType().label("ShoppingCartPanel.validity1Week")),
            new ValidityPredefinedValueType()
                    .duration(XmlTypeConverter.createDuration("P1M"))
                    .display(new DisplayType().label("ShoppingCartPanel.validity1Month")),
            new ValidityPredefinedValueType()
                    .duration(XmlTypeConverter.createDuration("P1Y"))
                    .display(new DisplayType().label("ShoppingCartPanel.validity1Year"))
    );

    private static final String VALIDITY_CUSTOM_LENGTH = "validityCustomLength";

    private static final String VALIDITY_CUSTOM_FOR_EACH = "validityCustomForEach";

    public static final String STEP_ID = "shoppingCart";
    private static final String ID_TABLE = "table";
    private static final String ID_TABLE_HEADER_FRAGMENT = "tableHeaderFragment";
    private static final String ID_TABLE_FOOTER_FRAGMENT = "tableFooterFragment";
    private static final String ID_TABLE_BUTTON_COLUMN = "tableButtonColumn";
    private static final String ID_CLEAR_CART = "clearCart";
    private static final String ID_EDIT = "edit";
    private static final String ID_REMOVE = "remove";
    private static final String ID_COMMENT = "comment";
    private static final String ID_VALIDITY = "validity";
    private static final String ID_OPEN_CONFLICT = "openConflict";
    private static final String ID_SUBMIT = "submit";

    public ShoppingCartPanel(IModel<RequestAccess> model) {
        super(model);

        initLayout();
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        DropDownChoice validity = (DropDownChoice) get(ID_VALIDITY);
        validity.setRequired(isValidityRequired());

        TextArea comment = (TextArea) get(ID_COMMENT);
        comment.setRequired(isCommentRequired());

        computeConflicts();
    }

    private void computeConflicts() {
        MidPointApplication mp = MidPointApplication.get();
        PageBase page = getPageBase();

        Task task = page.createSimpleTask("computeConflicts");
        OperationResult result = task.getResult();

        RequestAccess requestAccess = getModelObject();

        int warnings = 0;
        int errors = 0;
        Map<String, ConflictDto> conflictsMap = new HashMap<>();
        try {
            PrismObject<UserType> user = WebModelServiceUtils.loadObject(requestAccess.getPersonOfInterest().get(0), page);
            ObjectDelta<UserType> delta = user.createModifyDelta();

            PrismContainerDefinition def = user.getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);

            handleAssignmentDeltas(delta, requestAccess.getShoppingCartAssignments(), def);

            PartialProcessingOptionsType processing = new PartialProcessingOptionsType();
            processing.setInbound(SKIP);
            processing.setProjection(SKIP);

            ModelExecuteOptions options = ModelExecuteOptions.create().partialProcessing(processing);

            ModelContext<UserType> ctx = mp.getModelInteractionService()
                    .previewChanges(MiscUtil.createCollection(delta), options, task, result);

            DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = ctx.getEvaluatedAssignmentTriple();

            if (evaluatedAssignmentTriple != null) {
                Collection<? extends EvaluatedAssignment> addedAssignments = evaluatedAssignmentTriple.getPlusSet();
                for (EvaluatedAssignment<UserType> evaluatedAssignment : addedAssignments) {
                    for (EvaluatedPolicyRule policyRule : evaluatedAssignment.getAllTargetsPolicyRules()) {
                        if (!policyRule.containsEnabledAction()) {
                            continue;
                        }
                        // everything other than 'enforce' is a warning
                        boolean isWarning = !policyRule.containsEnabledAction(EnforcementPolicyActionType.class);
                        if (isWarning) {
                            warnings++;
                        } else {
                            errors++;
                        }
//                        fillInConflictedObjects(evaluatedAssignment, policyRule.getAllTriggers(), isWarning, conflictsMap);
                    }
                }
            } else if (!result.isSuccess() && StringUtils.isNotEmpty(getSubresultWarningMessages(result))) {
                warn(createStringResource("PageAssignmentsList.conflictsWarning").getString() + " " + getSubresultWarningMessages(result));
//                conflictProblemExists = true;
            }
        } catch (Exception e) {
//            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't get assignments conflicts. Reason: ", e);
//            error("Couldn't get assignments conflicts. Reason: " + e);
            // todo error handling
            e.printStackTrace();
        }
        requestAccess.setWarningCount(warnings);
        requestAccess.setErrorCount(errors);
//        return new ArrayList<>(conflictsMap.values());
    }

//    private void fillInFromEvaluatedExclusionTrigger(EvaluatedAssignment<UserType> evaluatedAssignment, EvaluatedExclusionTrigger exclusionTrigger, boolean isWarning, Map<String, ConflictDto> conflictsMap) {
//        EvaluatedAssignment<F> conflictingAssignment = exclusionTrigger.getConflictingAssignment();
//        PrismObject<F> addedAssignmentTargetObj = (PrismObject<F>) evaluatedAssignment.getTarget();
//        PrismObject<F> exclusionTargetObj = (PrismObject<F>) conflictingAssignment.getTarget();
//
//        AssignmentConflictDto<F> dto1 = new AssignmentConflictDto<>(exclusionTargetObj,
//                conflictingAssignment.getAssignment(true) != null);
//        AssignmentConflictDto<F> dto2 = new AssignmentConflictDto<>(addedAssignmentTargetObj,
//                evaluatedAssignment.getAssignment(true) != null);
//        ConflictDto conflict = new ConflictDto(dto1, dto2, isWarning);
//        String oid1 = exclusionTargetObj.getOid();
//        String oid2 = addedAssignmentTargetObj.getOid();
//        if (!conflictsMap.containsKey(oid1 + oid2) && !conflictsMap.containsKey(oid2 + oid1)) {
//            conflictsMap.put(oid1 + oid2, conflict);
//        } else if (!isWarning) {
//            // error is stronger than warning, so we replace (potential) warnings with this error
//            // TODO Kate please review this
//            if (conflictsMap.containsKey(oid1 + oid2)) {
//                conflictsMap.replace(oid1 + oid2, conflict);
//            }
//            if (conflictsMap.containsKey(oid2 + oid1)) {
//                conflictsMap.replace(oid2 + oid1, conflict);
//            }
//        }
//    }
//
//    private void fillInConflictedObjects(EvaluatedAssignment<UserType> evaluatedAssignment, Collection<EvaluatedPolicyRuleTrigger<?>> triggers, boolean isWarning, Map<String, ConflictDto> conflictsMap) {
//
//        for (EvaluatedPolicyRuleTrigger<?> trigger : triggers) {
//
//            if (trigger instanceof EvaluatedExclusionTrigger) {
//                fillInFromEvaluatedExclusionTrigger(evaluatedAssignment, (EvaluatedExclusionTrigger) trigger, isWarning, conflictsMap);
//            } else if (trigger instanceof EvaluatedCompositeTrigger) {
//                EvaluatedCompositeTrigger compositeTrigger = (EvaluatedCompositeTrigger) trigger;
//                Collection<EvaluatedPolicyRuleTrigger<?>> innerTriggers = compositeTrigger.getInnerTriggers();
//                fillInConflictedObjects(evaluatedAssignment, innerTriggers, isWarning, conflictsMap);
//            }
//        }
//
//    }

    private String getSubresultWarningMessages(OperationResult result) {
        if (result == null || result.getSubresults() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        result.getSubresults().forEach(subresult -> {
            if (subresult.isWarning()) {
                sb.append(subresult.getMessage());
                sb.append("\n");
            }
        });
        return sb.toString();
    }

    private ContainerDelta handleAssignmentDeltas(ObjectDelta<UserType> focusDelta,
            List<AssignmentType> assignments, PrismContainerDefinition def) throws SchemaException {
        ContainerDelta delta = getPrismContext().deltaFactory().container().create(ItemPath.EMPTY_PATH, def.getItemName(), def);

        for (AssignmentType a : assignments) {
            PrismContainerValue newValue = a.asPrismContainerValue();
            newValue.applyDefinition(def, false);
            delta.addValueToAdd(newValue.clone());
        }

        if (!delta.isEmpty()) {
            delta = focusDelta.addModification(delta);
        }

        return delta;
    }

    @Override
    public IModel<List<Badge>> getTitleBadges() {
        return () -> {
            List<Badge> badges = new ArrayList<>();

            int warnings = getModelObject().getWarningCount();
            if (warnings > 0) {
                String key = warnings == 1 ? "ShoppingCartPanel.badge.oneWarning" : "ShoppingCartPanel.badge.multipleWarnings";
                badges.add(new Badge("badge badge-warning", getString(key, warnings)));
            }

            int errors = getModelObject().getErrorCount();
            if (errors > 0) {
                String key = errors == 1 ? "ShoppingCartPanel.badge.oneConflict" : "ShoppingCartPanel.badge.multipleConflicts";
                badges.add(new Badge("badge badge-danger", "fa fa-exclamation-triangle", getString(key, errors)));
            }

            return badges;
        };
    }

    @Override
    public String getStepId() {
        return STEP_ID;
    }

    @Override
    public IModel<String> getTitle() {
        return () -> getString("ShoppingCartPanel.title");
    }

    @Override
    public String appendCssToWizard() {
        return "w-100";
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    private void initLayout() {
        List<IColumn<ShoppingCartItem, String>> columns = createColumns();

        ISortableDataProvider<ShoppingCartItem, String> provider = new ListDataProvider<>(this, () -> getModelObject().getShoppingCartItems());
        BoxedTablePanel table = new BoxedTablePanel(ID_TABLE, provider, columns) {

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                Fragment fragment = new Fragment(id, ID_TABLE_FOOTER_FRAGMENT, ShoppingCartPanel.this);
                fragment.add(new AjaxLink<>(ID_CLEAR_CART) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        clearCartPerformed(target);
                    }
                });

                return fragment;
            }

            @Override
            protected Component createHeader(String headerId) {
                return new Fragment(headerId, ID_TABLE_HEADER_FRAGMENT, ShoppingCartPanel.this);
            }

            @Override
            protected String getPaginationCssClass() {
                return null;
            }
        };
        add(table);

        DropDownChoice validity = new DropDownChoice(ID_VALIDITY, createValidityOptions(), (IChoiceRenderer) object -> {
            if (VALIDITY_CUSTOM_LENGTH.equals(object)) {
                return getString("ShoppingCartPanel.validityCustomLength");
            } else if (VALIDITY_CUSTOM_FOR_EACH.equals(object)) {
                return getString("ShoppingCartPanel.validityCustomForEach");
            }

            if (!(object instanceof ValidityPredefinedValueType)) {
                throw new IllegalArgumentException("Incorrect option type for validity dropdown choice: " + object);
            }

            ValidityPredefinedValueType value = (ValidityPredefinedValueType) object;
            DisplayType display = value.getDisplay();
            if (display != null && display.getLabel() != null) {
                return WebComponentUtil.getTranslatedPolyString(display.getLabel());
            }

            return value.getDuration().toString();
        });
        validity.add(new VisibleBehaviour(() -> isValidityVisible()));
        add(validity);

        TextArea comment = new TextArea(ID_COMMENT, new PropertyModel(getModel(), "comment"));
        comment.add(new VisibleBehaviour(() -> isCommentVisible()));
        add(comment);

        AjaxLink openConflict = new AjaxLink<>(ID_OPEN_CONFLICT) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                openConflictPeformed(target);
            }
        };
        openConflict.add(new VisibleBehaviour(() -> getModelObject().getWarningCount() > 0 || getModelObject().getErrorCount() > 0));
        add(openConflict);

        AjaxLink submit = new AjaxLink<>(ID_SUBMIT) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                submitPerformed(target);
            }
        };
        submit.add(new EnableBehaviour(() -> getModelObject().canSubmit()));
        submit.add(AttributeAppender.append("class", () -> !submit.isEnabledInHierarchy() ? "disabled" : null));
        add(submit);
    }

    private void openConflictPeformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void submitPerformed(AjaxRequestTarget target) {
        //todo implement
    }

    //todo use configuration to populate this
    private IModel<List> createValidityOptions() {
        return new LoadableModel<>(false) {

            @Override
            protected List load() {
                List items = new ArrayList();

                items.addAll(getValidityPeriods());

                if (!isAllowOnlyGlobalSettings()) {
                    items.add(VALIDITY_CUSTOM_LENGTH);
                    items.add(VALIDITY_CUSTOM_FOR_EACH);
                }

                return items;
            }
        };
    }

    private boolean isAllowOnlyGlobalSettings() {
        CheckoutType config = getCheckoutConfiguration();
        if (config == null || config.getValidityConfiguration() == null) {
            return false;
        }

        CheckoutValidityConfigurationType validityConfig = config.getValidityConfiguration();

        return BooleanUtils.toBoolean(validityConfig.isAllowOnlyGlobalSettings());
    }

    private List<ValidityPredefinedValueType> getValidityPeriods() {
        CheckoutType config = getCheckoutConfiguration();
        if (config == null || config.getValidityConfiguration() == null) {
            return DEFAULT_VALIDITY_PERIODS;
        }

        CheckoutValidityConfigurationType validityConfig = config.getValidityConfiguration();
        List<ValidityPredefinedValueType> values = validityConfig.getPredefinedValue();
        return values != null ? values : DEFAULT_VALIDITY_PERIODS;
    }

    private CheckoutType getCheckoutConfiguration() {
        CompiledGuiProfile profile = getPageBase().getCompiledGuiProfile();
        if (profile == null) {
            return null;
        }

        AccessRequestType accessRequest = profile.getAccessRequest();
        if (accessRequest == null) {
            return null;
        }

        return accessRequest.getCheckout();
    }

    private boolean isValidityRequired() {
        CheckoutType config = getCheckoutConfiguration();
        if (config == null || config.getValidityConfiguration() == null) {
            return false;
        }

        CheckoutValidityConfigurationType validity = config.getValidityConfiguration();
        return validity != null && BooleanUtils.toBoolean(validity.isMandatory());
    }

    private boolean isCommentRequired() {
        CheckoutType config = getCheckoutConfiguration();
        if (config == null || config.getComment() == null) {
            return false;
        }

        CheckoutCommentType comment = config.getComment();
        return comment != null && BooleanUtils.toBoolean(comment.isMandatory());
    }

    private boolean isValidityVisible() {
        CheckoutType config = getCheckoutConfiguration();
        if (config == null || config.getValidityConfiguration() == null) {
            return true;
        }

        CheckoutValidityConfigurationType validity = config.getValidityConfiguration();
        return validity.getVisibility() == null || WebComponentUtil.getElementVisibility(validity.getVisibility());
    }

    private boolean isCommentVisible() {
        CheckoutType config = getCheckoutConfiguration();
        if (config == null || config.getComment() == null) {
            return true;
        }

        CheckoutCommentType comment = config.getComment();
        return comment.getVisibility() == null || WebComponentUtil.getElementVisibility(comment.getVisibility());
    }

    private List<IColumn<ShoppingCartItem, String>> createColumns() {
        List<IColumn<ShoppingCartItem, String>> columns = new ArrayList<>();
//        columns.add(new IconColumn() {
//            @Override
//            protected DisplayType getIconDisplayType(IModel rowModel) {
//                return null;
//            }
//        });
        columns.add(new AbstractColumn<>(createStringResource("ShoppingCartPanel.accessName")) {

            @Override
            public void populateItem(Item<ICellPopulator<ShoppingCartItem>> item, String id, IModel<ShoppingCartItem> model) {
                item.add(new Label(id, () -> model.getObject().getName()));
            }
        });
        columns.add(new AbstractColumn<>(createStringResource("ShoppingCartPanel.selectedUsers")) {
            @Override
            public void populateItem(Item<ICellPopulator<ShoppingCartItem>> item, String id, IModel<ShoppingCartItem> model) {
                Label label = new Label(id, () -> {
                    int count = model.getObject().getCount();
                    String key = count == 0 || count > 1 ? "ShoppingCartPanel.countBadgeUsers" : "ShoppingCartPanel.countBadgeUser";

                    return getString(key, count);
                });
                label.add(AttributeAppender.append("class", "badge badge-info"));
                item.add(label);
            }
        });
        columns.add(new AbstractColumn<>(() -> "") {

            @Override
            public void populateItem(Item<ICellPopulator<ShoppingCartItem>> item, String id, IModel<ShoppingCartItem> model) {
                Fragment fragment = new Fragment(id, ID_TABLE_BUTTON_COLUMN, ShoppingCartPanel.this);
                fragment.add(new AjaxLink<>(ID_EDIT) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        editItemPerformed(target, model);
                    }
                });
                fragment.add(new AjaxLink<>(ID_REMOVE) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        removeItemPerformed(target, model);
                    }
                });

                item.add(AttributeAppender.append("style", "width: 100px;"));
                item.add(fragment);
            }
        });

        return columns;
    }

    private void editItemPerformed(AjaxRequestTarget target, IModel<ShoppingCartItem> model) {
        PageBase page = getPageBase();

        ShoppingCartEditPanel panel = new ShoppingCartEditPanel(model) {

            @Override
            protected void savePerformed(AjaxRequestTarget target, IModel<ShoppingCartItem> model) {
                // todo implement
            }

            @Override
            protected void closePerformed(AjaxRequestTarget target, IModel<ShoppingCartItem> model) {
                // todo implement
            }
        };

        page.showMainPopup(panel, target);
    }

    private void removeItemPerformed(AjaxRequestTarget target, IModel<ShoppingCartItem> model) {
        ShoppingCartItem item = model.getObject();

        RequestAccess requestAccess = getModelObject();
        requestAccess.removeAssignments(List.of(item.getAssignment()));

        getPageBase().reloadShoppingCartIcon(target);
        target.add(getWizard().getPanel());
    }

    private void clearCartPerformed(AjaxRequestTarget target) {
        ConfirmationPanel content = new ConfirmationPanel(Popupable.ID_CONTENT, createStringResource("ShoppingCartPanel.clearCartConfirmMessage")) {

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                clearCartConfirmedPerformed(target);
            }

            @Override
            public void noPerformed(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }

            @Override
            protected IModel<String> createYesLabel() {
                return createStringResource("ShoppingCartPanel.confirmClear");
            }

            @Override
            protected IModel<String> createNoLabel() {
                return createStringResource("Button.cancel");
            }
        };
        getPageBase().showMainPopup(content, target);
    }

    private void clearCartConfirmedPerformed(AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);

        getModelObject().clearCart();

        getPageBase().reloadShoppingCartIcon(target);
        target.add(getWizard().getPanel());
    }
}
