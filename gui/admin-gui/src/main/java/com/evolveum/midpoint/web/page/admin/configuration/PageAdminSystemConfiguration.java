package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.web.component.tab.BoxedTabPanel;

import com.evolveum.midpoint.web.component.tab.PanelTab;

import com.evolveum.midpoint.web.page.admin.PageAdmin;

import com.evolveum.midpoint.web.component.authentication.Saml2ModuleConfigPanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationModuleSaml2Type;

import org.apache.wicket.model.IModel;

import org.apache.wicket.model.Model;

import java.util.ArrayList;

import java.util.List;

/\*\*

\* System-configuration page that aggregates all GUI configuration tabs.

\*/

public class PageAdminSystemConfiguration extends PageAdmin {

private static final long serialVersionUID = 1L;

private static final String ID_MAIN_TAB_PANEL = "mainTabPanel";

@Override

protected void initLayout() {

List<PanelTab> tabs = new ArrayList<>();

// ── Existing tabs ───────────────────────────────────────────────────────

tabs.add(new PanelTab(createStringResource("PageAdminSystemConfiguration.logging")) {

@Override public BoxedTabPanel createPanel(String panelId) {

return new LoggingConfigPanel(panelId);

}

});

tabs.add(new PanelTab(createStringResource("PageAdminSystemConfiguration.authentication")) {

@Override public BoxedTabPanel createPanel(String panelId) {

return new AuthenticationConfigPanel(panelId);

}

});

// ── NEW TAB: SAML 2.0 / CAS ────────────────────────────────────────────

tabs.add(new PanelTab(createStringResource("PageAdminSystemConfiguration.saml2")) {

@Override public Saml2ModuleConfigPanel createPanel(String panelId) {

IModel<AuthenticationModuleSaml2Type> emptyModel = Model.of(new AuthenticationModuleSaml2Type());

return new Saml2ModuleConfigPanel(panelId, emptyModel);

}

});

// -----------------------------------------------------------------------

BoxedTabPanel mainTabPanel = new BoxedTabPanel(ID_MAIN_TAB_PANEL, tabs);

mainTabPanel.setOutputMarkupId(true);

add(mainTabPanel);

}

}