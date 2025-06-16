package com.evolveum.midpoint.gui;

import com.evolveum.midpoint.test.util.MidPointSpringTest;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.context.ApplicationContext;

import org.springframework.test.web.reactive.server.WebTestClient;

/\*\*

\* Just enough to prove the new SAML2 GUI pieces load and metadata endpoint responds.

\*/

@MidPointSpringTest

class Saml2GuiSmokeTest {

@Autowired ApplicationContext ctx;

@Autowired WebTestClient client;

@Test

void contextContainsPanelBean() {

// Bean name is auto-generated from the panel’s @Component annotation

boolean present = ctx.containsBeanDefinition("saml2ModuleConfigPanel");

assert present : "Saml2ModuleConfigPanel bean missing";

}

@Test

void metadataEndpointReturns200() {

client.get()

.uri("/midpoint/auth/default/test-module/metadata")

.exchange()

.expectStatus().isOk();

}

}