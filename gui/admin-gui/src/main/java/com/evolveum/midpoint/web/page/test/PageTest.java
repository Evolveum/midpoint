/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.test;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.atmosphere.NotifyMessage;
import com.evolveum.midpoint.web.component.atmosphere.NotifyMessageFilter;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.atmosphere.Subscribe;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.INullAcceptingValidator;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * todo disable/remove before release
 *
 * @author lazyman
 */
@Deprecated
public class PageTest extends PageBase {

    private static final Trace LOGGER = TraceManager.getTrace(PageTest.class);

    private static final String DOT_CLASS = PageTest.class.getName() + ".";
    private static final String LOAD_USER_EMAIL = DOT_CLASS + "loadUserEmail";

    private static final String ID_FORM = "form";
    private static final String ID_SUBMIT = "submit";
    private static final String ID_TEXT = "text";
    private static final String ID_USER_EMAIL = "userEmail";
    private static final String ID_TEXT_FEEDBACK = "textFeedback";

    // just a simple model for text field, with default value added by default - administrator oid
    // it will be used to load user based on oid. As dto bean we're using just simple String class.
    private IModel<String> textModel = new Model<String>(SystemObjectsType.USER_ADMINISTRATOR.value());
    private LoadableModel<String> userEmailModel = new LoadableModel<String>(false) {

        @Override
        protected String load() {
            return loadUserEmail();
        }
    };

    public PageTest() {
        initLayout();
    }

    private void initLayout() {
        Form form = new Form(ID_FORM);
        add(form);

        TextField text = new TextField(ID_TEXT, textModel);
        text.add(new INullAcceptingValidator<String>() {

            @Override
            public void validate(IValidatable<String> validatable) {
                String value = validatable.getValue();

                if (StringUtils.isEmpty(value)) {
                    validatable.error(new ValidationError(getString("PageTest.message.nullOid")));
                }
            }
        });
        form.add(text);

        FeedbackPanel textFeedback = new FeedbackPanel(ID_TEXT_FEEDBACK, new ComponentFeedbackMessageFilter(text));
        textFeedback.setOutputMarkupId(true);
        form.add(textFeedback);

        AjaxSubmitButton submit = new AjaxSubmitButton(ID_SUBMIT,
                createStringResource("PageTest.printEmail")) {

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                // when validation fail we refresh main feedback panel
                // and feedback panel for our text field
                target.add(getFeedbackPanel());
                target.add(form.get(ID_TEXT_FEEDBACK));
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                submitPerformed(target);
            }
        };
        form.add(submit);

        Label userEmail = new Label(ID_USER_EMAIL, userEmailModel);
        add(userEmail);
    }

    private String loadUserEmail() {
        String email = null;

        String oid = textModel.getObject();
        if (StringUtils.isEmpty(oid)) {
            return null;
        }

        OperationResult result = new OperationResult(LOAD_USER_EMAIL);
        try {
            Collection<SelectorOptions<GetOperationOptions>> options = new ArrayList<SelectorOptions<GetOperationOptions>>();
            options.add(SelectorOptions.create(ItemPath.EMPTY_PATH,
                    GetOperationOptions.createRetrieve(RetrieveOption.DEFAULT)));

            Task task = createSimpleTask(LOAD_USER_EMAIL);

            ModelService model = getModelService();

            PrismObject<UserType> user = model.getObject(UserType.class, oid, options, task, result);

            email = user.getPropertyRealValue(UserType.F_EMAIL_ADDRESS, String.class);
//            or you can find user email (or other property) this way
            PrismProperty property = user.findProperty(UserType.F_EMAIL_ADDRESS);
            if (property != null) {
                 email = (String) property.getRealValue();
            }

            UserType userType = user.asObjectable();
            userType.getEmailAddress();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't load user", ex);
        } finally {
            result.computeStatus();
        }

        if (WebMiscUtil.showResultInPage(result)) {
            showResult(result);
            throw new RestartResponseException(PageTest.class);
        }

        if (StringUtils.isEmpty(email)) {
            getSession().warn("User email is empty or couldn't be loaded.");
            throw new RestartResponseException(PageTest.class);
        }

        return email;
    }

    private void submitPerformed(AjaxRequestTarget target) {
        userEmailModel.reset();

        target.add(getFeedbackPanel());
    }

    /**
     * not used, will be removed before release [lazyman]
     */
    @Deprecated
    @Override
    public List<BottomMenuItem> getBottomMenuItems() {
        return null;
    }
}
