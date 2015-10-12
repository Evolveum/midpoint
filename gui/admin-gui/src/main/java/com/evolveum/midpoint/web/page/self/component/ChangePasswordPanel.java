package com.evolveum.midpoint.web.page.self.component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.input.PasswordPanel;
import com.evolveum.midpoint.web.component.util.ListDataProvider;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.home.dto.MyPasswordsDto;
import com.evolveum.midpoint.web.page.admin.home.dto.PasswordAccountDto;
import com.evolveum.midpoint.web.page.admin.home.dto.PersonalInfoDto;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.workflow.dto.ProcessInstanceDto;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

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
        Label passwordLabel = new Label(ID_PASSWORD_LABEL, createStringResource("PageSelfCredentials.passwordLabel1"));
        add(passwordLabel);

        Label confirmPasswordLabel = new Label(ID_CONFIRM_PASSWORD_LABEL, createStringResource("PageSelfCredentials.passwordLabel2"));
        add(confirmPasswordLabel);

        PasswordPanel passwordPanel = new PasswordPanel(ID_PASSWORD_PANEL, new Model<String>());
        add(passwordPanel);

        CheckBox changeAllPasswords = new CheckBox(ID_CHANGE_ALL_PASSWORDS,
                new PropertyModel<Boolean>(model, ExecuteChangeOptionsDto.F_EXECUTE_AFTER_ALL_APPROVALS));
        add(changeAllPasswords);


        List<IColumn<PasswordAccountDto, String>> columns = initColumns();
        model = (LoadableModel) getModel();
        ListDataProvider<PasswordAccountDto> provider = new ListDataProvider<PasswordAccountDto>(this,
                new PropertyModel<List<PasswordAccountDto>>(model, MyPasswordsDto.F_ACCOUNTS));
        TablePanel accounts = new TablePanel(ID_ACCOUNTS_TABLE, provider, columns);
        accounts.setItemsPerPage(30);
        accounts.setShowPaging(false);
        add(accounts);
    }
    private List<IColumn<PasswordAccountDto, String>> initColumns() {
        List<IColumn<PasswordAccountDto, String>> columns = new ArrayList<IColumn<PasswordAccountDto, String>>();

        IColumn column = new CheckBoxHeaderColumn<UserType>();
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

    private PasswordAccountDto createDefaultPasswordAccountDto(PrismObject<UserType> user) {
        return new PasswordAccountDto(user.getOid(), getString("PageMyPasswords.accountMidpoint"),
                getString("PageMyPasswords.resourceMidpoint"), WebMiscUtil.isActivationEnabled(user), true);
    }

    private PasswordAccountDto createPasswordAccountDto(PrismObject<ShadowType> account) {
        PrismReference resourceRef = account.findReference(ShadowType.F_RESOURCE_REF);
        String resourceName;
        if (resourceRef == null || resourceRef.getValue() == null || resourceRef.getValue().getObject() == null) {
            resourceName = getString("PageMyPasswords.couldntResolve");
        } else {
            resourceName = WebMiscUtil.getName(resourceRef.getValue().getObject());
        }

        return new PasswordAccountDto(account.getOid(), WebMiscUtil.getName(account),
                resourceName, WebMiscUtil.isActivationEnabled(account));
    }

}
