/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
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

import com.evolveum.midpoint.gui.api.component.wizard.Badge;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStepPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.PropertyModel;

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

    public ShoppingCartPanel(IModel<RequestAccess> model) {
        super(model);

        initLayout();
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        DropDownChoice validity = (DropDownChoice) get(ID_VALIDITY);
        validity.setRequired(isValidityRequired());

        TextArea comment = (TextArea) get(ID_COMMENT);
        comment.setRequired(isCommentRequired());
    }

    @Override
    public IModel<List<Badge>> getTitleBadges() {
        return Model.ofList(List.of(
                new Badge("badge badge-warning", "1 warning"),
                new Badge("badge badge-danger", "fa fa-exclamation-triangle", "2 conflict found")));
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
        List<IColumn> columns = createColumns();

        ISortableDataProvider provider = new ListDataProvider(this, () -> getSession().getSessionStorage().getRequestAccess().getShoppingCartAssignments());
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

    private List<IColumn> createColumns() {
        List<IColumn> columns = new ArrayList<>();
//        columns.add(new IconColumn() {
//            @Override
//            protected DisplayType getIconDisplayType(IModel rowModel) {
//                return null;
//            }
//        });
        columns.add(new AbstractColumn(createStringResource("ShoppingCartPanel.accessName")) {
            @Override
            public void populateItem(Item item, String id, IModel iModel) {
                item.add(new Label(id, "asdf"));
            }
        });
        columns.add(new AbstractColumn(createStringResource("ShoppingCartPanel.selectedUsers")) {
            @Override
            public void populateItem(Item item, String id, IModel model) {
                item.add(new Label(id, "zxcv"));
            }
        });
        columns.add(new AbstractColumn(() -> "") {
            @Override
            public void populateItem(Item item, String id, IModel model) {
                Fragment fragment = new Fragment(id, ID_TABLE_BUTTON_COLUMN, ShoppingCartPanel.this);
                fragment.add(new AjaxLink<>(ID_EDIT) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                    }
                });
                fragment.add(new AjaxLink<>(ID_REMOVE) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {

                    }
                });

                item.add(fragment);
            }
        });

        return columns;
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

        getModelObject().getShoppingCartAssignments().clear();

        getPageBase().reloadShoppingCartIcon(target);
        target.add(get(ID_TABLE));
    }
}
