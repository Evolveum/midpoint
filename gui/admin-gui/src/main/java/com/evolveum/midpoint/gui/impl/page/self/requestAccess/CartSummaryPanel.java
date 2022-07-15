/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.form.DateRange;
import com.evolveum.midpoint.gui.api.component.form.DateRangePicker;

import com.evolveum.midpoint.web.component.DateInput;
import com.evolveum.midpoint.web.component.input.DatePanel;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
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
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.AbstractResource;
import org.apache.wicket.request.resource.ByteArrayResource;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.RoundedIconColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class CartSummaryPanel extends BasePanel<RequestAccess> {

    private static final long serialVersionUID = 1L;

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
    private static final String ID_VALIDITY_INFO = "validityInfo";
    private static final String ID_COMMENT_INFO = "commentInfo";
    private static final String ID_CUSTOM_VALIDITY = "customValidity";
    private static final String ID_CUSTOM_VALIDITY_INFO = "customValidityInfo";
    private static final String ID_CUSTOM_VALIDITY_FROM = "customValidityFrom";
    private static final String ID_CUSTOM_VALIDITY_TO = "customValidityTo";

    private WizardModel wizard;

    public CartSummaryPanel(String id, WizardModel wizard, IModel<RequestAccess> model) {
        super(id, model);

        this.wizard = wizard;

        initLayout();
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();

        DropDownChoice validity = (DropDownChoice) get(ID_VALIDITY);
        validity.setRequired(isValidityRequired());

        TextArea comment = (TextArea) get(ID_COMMENT);
        comment.setRequired(isCommentRequired());
    }

    private void initLayout() {
        List<IColumn<ShoppingCartItem, String>> columns = createColumns();

        ISortableDataProvider<ShoppingCartItem, String> provider = new ListDataProvider<>(this, () -> getModelObject().getShoppingCartItems());
        BoxedTablePanel table = new BoxedTablePanel(ID_TABLE, provider, columns) {

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                Fragment fragment = new Fragment(id, ID_TABLE_FOOTER_FRAGMENT, CartSummaryPanel.this);
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
                return new Fragment(headerId, ID_TABLE_HEADER_FRAGMENT, CartSummaryPanel.this);
            }

            @Override
            protected String getPaginationCssClass() {
                return null;
            }
        };
        add(table);

        IModel validityModel = new IModel<>() {
            @Override
            public Object getObject() {
                return getModelObject().getSelectedValidity();
            }

            @Override
            public void setObject(Object object) {
                getModelObject().setSelectedValidity(object);
            }
        };

        WebMarkupContainer customValidity = new WebMarkupContainer(ID_CUSTOM_VALIDITY);
        customValidity.add(new VisibleBehaviour(() -> RequestAccess.VALIDITY_CUSTOM_LENGTH.equals(validityModel.getObject())));
        customValidity.setOutputMarkupId(true);
        customValidity.setOutputMarkupPlaceholderTag(true);
        add(customValidity);

        Label customValidityInfo = new Label(ID_CUSTOM_VALIDITY_INFO);
        customValidityInfo.add(new TooltipBehavior());
        customValidity.add(customValidityInfo);

        DateInput customValidFrom = new DateInput(ID_CUSTOM_VALIDITY_FROM, Model.of(new Date()));
        customValidity.add(customValidFrom);

        DateInput customValidTo = new DateInput(ID_CUSTOM_VALIDITY_TO, Model.of(new Date()));
        customValidity.add(customValidTo);

        Label validityInfo = new Label(ID_VALIDITY_INFO);
        validityInfo.add(new TooltipBehavior());
        add(validityInfo);

        DropDownChoice validity = new DropDownChoice(ID_VALIDITY, validityModel, createValidityOptions(), (IChoiceRenderer) object -> {
            if (RequestAccess.VALIDITY_CUSTOM_LENGTH.equals(object) || RequestAccess.VALIDITY_CUSTOM_FOR_EACH.equals(object)) {
                return getString("RequestAccess." + object);
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
        validity.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(customValidity);
            }
        });
        add(validity);

        Label commentInfo = new Label(ID_COMMENT_INFO);
        commentInfo.add(new TooltipBehavior());
        add(commentInfo);

        TextArea comment = new TextArea(ID_COMMENT, new PropertyModel(getModel(), "comment"));
        comment.add(new VisibleBehaviour(() -> isCommentVisible()));
        add(comment);

        AjaxLink openConflict = new AjaxLink<>(ID_OPEN_CONFLICT) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                openConflictPerformed(target);
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
        WebComponentUtil.addDisabledClassBehavior(submit);
        add(submit);
    }

    protected void openConflictPerformed(AjaxRequestTarget target) {
    }

    protected void submitPerformed(AjaxRequestTarget target) {
    }

    private IModel<List> createValidityOptions() {
        return new LoadableModel<>(false) {

            @Override
            protected List load() {
                List items = new ArrayList();

                items.addAll(getValidityPeriods());

                if (!isAllowOnlyGlobalSettings()) {
                    items.add(RequestAccess.VALIDITY_CUSTOM_LENGTH);
                    // todo custom value for each - UI not implemented yet
                    // items.add(RequestAccess.VALIDITY_CUSTOM_FOR_EACH);
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
            return RequestAccess.DEFAULT_VALIDITY_PERIODS;
        }

        CheckoutValidityConfigurationType validityConfig = config.getValidityConfiguration();
        List<ValidityPredefinedValueType> values = validityConfig.getPredefinedValue();
        return values != null && !values.isEmpty() ? values : RequestAccess.DEFAULT_VALIDITY_PERIODS;
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
        columns.add(new RoundedIconColumn<>(null) {

            @Override
            protected IModel<AbstractResource> createPreferredImage(IModel<ShoppingCartItem> model) {
                return new LoadableModel<>(false) {
                    @Override
                    protected AbstractResource load() {
                        ObjectReferenceType ref = model.getObject().getAssignment().getTargetRef();

                        Collection<SelectorOptions<GetOperationOptions>> options = getPageBase().getOperationOptionsBuilder()
                                .item(FocusType.F_JPEG_PHOTO).retrieve()
                                .build();

                        Task task = getPageBase().createSimpleTask("load photo");
                        OperationResult result = task.getResult();

                        PrismObject obj = WebModelServiceUtils.loadObject(ObjectTypes.getObjectTypeClass(ref.getType()), ref.getOid(), options, getPageBase(), task, result);
                        FocusType focus = (FocusType) obj.asObjectable();
                        byte[] photo = focus.getJpegPhoto();

                        if (photo == null) {
                            return null;
                        }

                        return new ByteArrayResource("image/jpeg", photo);
                    }
                };
            }
        });
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
                Fragment fragment = new Fragment(id, ID_TABLE_BUTTON_COLUMN, CartSummaryPanel.this);
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

                item.add(AttributeAppender.append("style", "width: 120px;"));
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
                getPageBase().hideMainPopup(target);
            }

            @Override
            protected void closePerformed(AjaxRequestTarget target, IModel<ShoppingCartItem> model) {
                // todo implement
                getPageBase().hideMainPopup(target);
            }
        };

        page.showMainPopup(panel, target);
    }

    private void removeItemPerformed(AjaxRequestTarget target, IModel<ShoppingCartItem> model) {
        ShoppingCartItem item = model.getObject();

        RequestAccess requestAccess = getModelObject();
        requestAccess.removeAssignments(List.of(item.getAssignment()));

        getPageBase().reloadShoppingCartIcon(target);
        target.add(wizard.getPanel());
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
        target.add(wizard.getPanel());
    }
}
