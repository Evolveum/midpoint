package com.evolveum.midpoint.web.component.authentication;

import com.evolveum.midpoint.web.component.input.\*;

import com.evolveum.midpoint.web.component.util.SimplePanel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationModuleSaml2Type;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialRefType;

import org.apache.wicket.ajax.AjaxRequestTarget;

import org.apache.wicket.markup.html.form.Form;

import org.apache.wicket.model.IModel;

import org.apache.wicket.model.PropertyModel;

@PageDescriptor

public class Saml2ModuleConfigPanel extends SimplePanel<AuthenticationModuleSaml2Type> {

private static final String ID_FORM = "form";

public Saml2ModuleConfigPanel(String id, IModel<AuthenticationModuleSaml2Type> model) {

super(id, model);

}

@Override

protected void initLayout() {

Form<?> form = new Form<>(ID_FORM);

add(form);

form.add(new TextPanel<>("entityId",

new PropertyModel<>(getModel(), "serviceProvider.entityId")));

form.add(new TextPanel<>("idpMetadataUrl",

new PropertyModel<>(getModel(), "identityProvider.metadataSource")));

form.add(new FileUploadPanel("idpMetadataFile",

new PropertyModel<>(getModel(), "identityProvider.metadataFile")));

form.add(new CredentialDropDownPanel("signingCredential",

new PropertyModel<CredentialRefType>(getModel(), "signingCredentialRef")));

form.add(new Label("acsUrl",

() -> getPageBase().getAcsLocation(getModelObject())));

form.add(new Label("sloUrl",

() -> getPageBase().getSloLocation(getModelObject())));

form.add(new AjaxButton("downloadMetadata",

getPageBase().createStringResource("Saml2.download")) {

@Override protected void onSubmit(AjaxRequestTarget target) {

getPageBase().redirectTo("/midpoint/auth/default/" +

getModelObject().getIdentifier() + "/metadata");

}});

form.add(new AjaxButton("testSso",

getPageBase().createStringResource("Saml2.testSso")) {

@Override protected void onSubmit(AjaxRequestTarget target) {

getPageBase().redirectTo("/saml2/login");

}});

}

}