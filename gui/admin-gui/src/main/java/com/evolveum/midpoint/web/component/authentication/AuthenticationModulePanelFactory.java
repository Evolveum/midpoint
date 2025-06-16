package com.evolveum.midpoint.web.component.authentication;

import com.evolveum.midpoint.web.component.util.SimplePanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationModuleType;

import org.apache.wicket.model.IModel;

/\*\*

\* Factory that returns the correct Wicket panel for a given authentication-module type.

\*/

public final class AuthenticationModulePanelFactory {

private AuthenticationModulePanelFactory() {

// utility class

}

/\*\*

\* Creates a configuration panel for the supplied {@link AuthenticationModuleType}.

\*

\* @param id Wicket component id

\* @param moduleType enum value of the module (never null)

\* @param moduleModel model wrapping the JAXB bean that backs the panel

\* @return subclass of {@link SimplePanel} wired to the model

\*/

@SuppressWarnings({"rawtypes", "unchecked"})

public static SimplePanel<? extends AuthenticationModuleType> createPanel(

String id,

AuthenticationModuleType moduleType,

IModel<?> moduleModel) {

switch (moduleType) {

case SAML2: // ──► NEW CASE

return new Saml2ModuleConfigPanel(id, (IModel) moduleModel);

case OIDC:

return new OidcModuleConfigPanel(id, (IModel) moduleModel);

case LDAP:

return new LdapModuleConfigPanel(id, (IModel) moduleModel);

// add other existing cases here as they already were …

default:

throw new IllegalStateException(

"Unsupported authentication module type: " + moduleType);

}

}

}