/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.resources.content;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.ObjectOperationOption;
import com.evolveum.midpoint.schema.ObjectOperationOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.resources.PageAdminResources;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import java.util.Collection;

/**
 * @author lazyman
 */
public class PageAccount extends PageAdminResources {

    public static final String PARAM_ACCOUNT_ID = "accountOid";

    private static final Trace LOGGER = TraceManager.getTrace(PageAccount.class);
    private static final String DOT_CLASS = PageAccount.class.getName() + ".";
    private static final String OPERATION_LOAD_ACCOUNT = DOT_CLASS + "loadAccount";

    private IModel<ObjectWrapper> accountModel;

    public PageAccount() {
        accountModel = new LoadableModel<ObjectWrapper>(false) {

            @Override
            protected ObjectWrapper load() {
                return loadAccount();
            }
        };
        initLayout();
    }

    private ObjectWrapper loadAccount() {
        OperationResult result = new OperationResult(OPERATION_LOAD_ACCOUNT);
        PrismObject<AccountShadowType> account = null;
        try {
            Task task = createSimpleTask(OPERATION_LOAD_ACCOUNT);
            Collection<ObjectOperationOptions> options = ObjectOperationOptions.createCollection(
                    AccountShadowType.F_RESOURCE, ObjectOperationOption.RESOLVE);

            StringValue userOid = getPageParameters().get(PARAM_ACCOUNT_ID);
            account = getModelService().getObject(AccountShadowType.class, userOid.toString(), options, task, result);
            result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get user.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't load user", ex);
        }

        if (!result.isSuccess()) {
            showResultInSession(result);
        }

        if (account == null) {
            getSession().error(getString("pageAccount.message.cantEditAccount"));
            throw new RestartResponseException(PageUsers.class);
        }

        ObjectWrapper wrapper = new ObjectWrapper(null, null, account, ContainerStatus.MODIFYING);
        wrapper.setShowEmpty(false);
        return wrapper;
    }

    private void initLayout() {
        Label subtitle = new Label("subtitle", createSubtitle());
        subtitle.setRenderBodyOnly(true);
        add(subtitle);

        Form mainForm = new Form("mainForm");
        add(mainForm);

        PrismObjectPanel userForm = new PrismObjectPanel("account", accountModel, new PackageResourceReference(
                ImgResources.class, "Hdd.png"), mainForm) {

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return createStringResource("pageAccount.description");
            }
        };
        mainForm.add(userForm);
    }

    private IModel<String> createSubtitle() {
        return new LoadableModel<String>(false) {

            @Override
            protected String load() {
                PrismObject<AccountShadowType> account = accountModel.getObject().getObject();

                String resourceName = null;
                ResourceType resource = account.asObjectable().getResource();
                if (resource != null && StringUtils.isNotEmpty(resource.getName())) {
                    resourceName = resource.getName();
                }

                return createStringResource("pageAccount.subtitle", resourceName).getString();
            }
        };
    }
}