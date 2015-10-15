package com.evolveum.midpoint.web.page.self.component;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.input.PasswordPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.admin.certification.PageCertCampaign;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertDecisionDto;
import com.evolveum.midpoint.web.page.admin.home.dto.AssignmentItemDto;
import com.evolveum.midpoint.web.page.admin.home.dto.MyPasswordsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordAccountDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.*;

/**
 * Created by Kate on 09.10.2015.
 */
public class ChangePasswordPanel extends SimplePanel<MyPasswordsDto> {
    private static final String ID_PASSWORD_PANEL = "passwordPanel";
    private static final String ID_PASSWORD_LABEL = "passwordLabel";
    private static final String ID_CONFIRM_PASSWORD_LABEL = "confirmPasswordLabel";
    public static final String ID_ACCOUNTS_TABLE = "accounts";
    public static final String ID_CHANGE_ALL_PASSWORDS = "changeAllPasswords";
    private static final Trace LOGGER = TraceManager.getTrace(ChangePasswordPanel.class);

    private static final String DOT_CLASS = ChangePasswordPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_USER_WITH_ACCOUNTS = DOT_CLASS + "loadUserWithAccounts";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
    private static final String OPERATION_LOAD_ACCOUNT = DOT_CLASS + "loadAccount";

    private PasswordAccountDto midpointAccountDto;
    private LoadableModel<MyPasswordsDto> model;
    MyPasswordsDto myPasswordsDto = new MyPasswordsDto();
    public ChangePasswordPanel(String id) {
        super(id);
    }
    public ChangePasswordPanel(String id, LoadableModel<MyPasswordsDto> model,MyPasswordsDto myPasswordsDto) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        model = (LoadableModel) getModel();
        MyPasswordsDto dto = model.getObject();

        Label passwordLabel = new Label(ID_PASSWORD_LABEL, createStringResource("PageSelfCredentials.passwordLabel1"));
        add(passwordLabel);

        Label confirmPasswordLabel = new Label(ID_CONFIRM_PASSWORD_LABEL, createStringResource("PageSelfCredentials.passwordLabel2"));
        add(confirmPasswordLabel);

        PasswordPanel passwordPanel = new PasswordPanel(ID_PASSWORD_PANEL, new PropertyModel<ProtectedStringType>(model, MyPasswordsDto.F_PASSWORD));
        add(passwordPanel);

        List<IColumn<PasswordAccountDto, String>> columns = initColumns();
        ListDataProvider<PasswordAccountDto> provider = new ListDataProvider<PasswordAccountDto>(this,
                new PropertyModel<List<PasswordAccountDto>>(model, MyPasswordsDto.F_ACCOUNTS));
        TablePanel accounts = new TablePanel(ID_ACCOUNTS_TABLE, provider, columns);
        accounts.setItemsPerPage(30);
        accounts.setShowPaging(false);
        add(accounts);
    }
    private List<IColumn<PasswordAccountDto, String>> initColumns() {
        List<IColumn<PasswordAccountDto, String>> columns = new ArrayList<IColumn<PasswordAccountDto, String>>();

        IColumn column = new CheckBoxHeaderColumn<PasswordAccountDto>();
        column = new IconColumn<PasswordAccountDto>(createStringResource("PageCertDecisions.table.campaignName")) {
            @Override
            protected IModel<String> createIconModel(final IModel<PasswordAccountDto> rowModel) {
                return new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        PasswordAccountDto item = rowModel.getObject();
//                        if (item.getType() == null) {
                            return "silk-error";
//                        }

//                        switch (item.getType()) {
//                            case ACCOUNT_CONSTRUCTION:
//                                return "silk-drive";
//                            case ORG_UNIT:
//                                return "silk-building";
//                            case ROLE:
//                                return "silk-user_suit";
//                            default:
//                                return "silk-error";
//                        }
                    }
                };
            }
        };
        columns.add(column);

        columns.add(new AbstractColumn<PasswordAccountDto, String>(createStringResource("PageMyPasswords.name")) {

                        @Override
                        public void populateItem(Item<ICellPopulator<PasswordAccountDto>> item, String componentId,
                                                 final IModel<PasswordAccountDto> rowModel) {
                            item.add(new Label(componentId, new AbstractReadOnlyModel<Object>() {

                                @Override
                                public Object getObject() {
                                    PasswordAccountDto dto = rowModel.getObject();
                                    return dto.getDisplayName();
                                }
                            }));
                        }
                    });
//            column = new PropertyColumn(createStringResource("PageMyPasswords.name"),
//                PasswordAccountDto.F_DISPLAY_NAME);
//        columns.add(column);

        column = new PropertyColumn(createStringResource("PageMyPasswords.resourceName"),
                PasswordAccountDto.F_RESOURCE_NAME);
        columns.add(column);

        CheckBoxColumn enabled = new CheckBoxColumn(createStringResource("PageMyPasswords.enabled"),
                PasswordAccountDto.F_ENABLED);
        enabled.setEnabled(false);
        columns.add(enabled);

        return columns;
    }
}
