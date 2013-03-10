package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.page.admin.configuration.component.LoggingConfigPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.SystemConfigPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.UserConfigPanel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.SystemConfigurationDto;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.extensions.markup.html.tabs.TabbedPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public class PageSystemConfiguration extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageSystemConfiguration.class);
    private static final String DOT_CLASS = PageSystemConfiguration.class.getName() + ".";
    private static final String OPERATION_TEST_REPOSITORY = DOT_CLASS + "testRepository";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TAB_PANEL = "tabPanel";
    private static final String ID_BACK = "back";
    private static final String ID_SAVE = "save";
    private static final String ID_TEST_REPOSITORY = "testRepository";

    private LoadableModel<SystemConfigurationDto> model;

    public PageSystemConfiguration() {
        model = new LoadableModel<SystemConfigurationDto>(false) {

            @Override
            protected SystemConfigurationDto load() {
                return loadSystemConfiguration();
            }
        };

        initLayout();
    }

    private SystemConfigurationDto loadSystemConfiguration() {
        //todo implement

        return null;
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        List<ITab> tabs = new ArrayList<ITab>();
        tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.system.title")) {

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new SystemConfigPanel(panelId);
            }
        });
        tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.logging.title")) {

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new LoggingConfigPanel(panelId);
            }
        });
        tabs.add(new AbstractTab(createStringResource("pageSystemConfiguration.user.title")) {

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return new UserConfigPanel(panelId);
            }
        });

        mainForm.add(new TabbedPanel(ID_TAB_PANEL, tabs));

        initButtons(mainForm);
    }

    private void initButtons(Form mainForm) {
        AjaxSubmitLinkButton save = new AjaxSubmitLinkButton(ID_SAVE, ButtonType.POSITIVE,
                createStringResource("PageBase.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(save);

        AjaxLinkButton back = new AjaxLinkButton(ID_BACK, createStringResource("PageBase.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                backPerformed(target);
            }
        };
        mainForm.add(back);

        AjaxLinkButton testRepository = new AjaxLinkButton(ID_TEST_REPOSITORY,
                createStringResource("pageSystemConfiguration.button.testRepository")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                testRepositoryPerformed(target);
            }
        };
        mainForm.add(testRepository);
    }

    private void testRepositoryPerformed(AjaxRequestTarget target) {
        Task task = createSimpleTask(OPERATION_TEST_REPOSITORY);

        OperationResult result = getModelDiagnosticService().repositorySelfTest(task);
        showResult(result);

        target.add(getFeedbackPanel());
    }

    private void savePerformed(AjaxRequestTarget target) {
        //todo implement
    }

    private void backPerformed(AjaxRequestTarget target) {
        //todo implement
    }
}
