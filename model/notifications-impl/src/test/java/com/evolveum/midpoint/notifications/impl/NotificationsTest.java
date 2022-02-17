/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.common.expression.script.velocity.VelocityScriptEvaluator;
import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.Transport;
import com.evolveum.midpoint.notifications.api.transports.TransportService;
import com.evolveum.midpoint.notifications.impl.events.CustomEventImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@ContextConfiguration(locations = { "classpath:ctx-notifications-test.xml" })
public class NotificationsTest extends AbstractIntegrationTest {

    private static final String SYS_CONFIG_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();

    @Autowired private TransportService transportService;
    @Autowired private NotificationManager notificationManager;
    @Autowired private NotificationFunctions notificationFunctions;

    private final AtomicInteger idSeq = new AtomicInteger();
    private final LightweightIdentifierGenerator lightweightIdentifierGenerator =
            () -> new LightweightIdentifier(System.currentTimeMillis(), 0, idSeq.incrementAndGet());

    @Override
    protected void initSystem(Task task, OperationResult initResult) throws Exception {
        SystemConfigurationType config = new SystemConfigurationType(prismContext)
                .oid(SYS_CONFIG_OID)
                .name("sys-config");
        repositoryService.addObject(config.asPrismObject(), null, new OperationResult("dummy"));
    }

    @Test
    public void test000Sanity() throws Exception {
        assertThat(transportService).isNotNull();
        String transportName = "test-transport";
        transportService.registerTransport(new TestMessageTransport(transportName));
        assertThat(transportService.getTransport(transportName)).isNotNull();
        assertThatThrownBy(() -> transportService.getTransport("bad-name"));
        PrismObject<SystemConfigurationType> config = repositoryService.getObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, null, getTestOperationResult());
        assertThat(config).isNotNull();

        // test of legacy transports, should go away after 4.5
        assertThat(transportService.getTransport("mail")).isNotNull();
        assertThat(transportService.getTransport("sms")).isNotNull();
        assertThat(transportService.getTransport("file")).isNotNull();
        assertThat(transportService.getTransport("custom")).isNotNull();
    }

    @Test
    public void test010CustomTransportRegistration() throws Exception {
        given("configuration change with custom transport");
        Collection<? extends ItemDelta<?, ?>> modifications = prismContext.deltaFor(SystemConfigurationType.class)
                .item(SystemConfigurationType.F_MESSAGE_TRANSPORT_CONFIGURATION)
                .replace(new MessageTransportConfigurationType(prismContext)
                        .customTransport(new CustomTransportConfigurationType(prismContext)
                                .name("foo")
                                .type(TestMessageTransport.class.getName())))
                .asItemDeltas();

        when("sysconfig is modified in repository");
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, getTestOperationResult());

        then("transport of the right type is registered");
        Transport<?> fooTransport = transportService.getTransport("foo");
        assertThat(fooTransport)
                .isNotNull()
                .isInstanceOf(TestMessageTransport.class);
        TestMessageTransport testTransport = (TestMessageTransport) fooTransport;
        assertThat(testTransport.getName()).isEqualTo("foo");
    }

    @Test
    public void test100CustomTransportSendingNotificationMessage() throws Exception {
        given("configuration with custom transport and some notifier");
        String messageBody = "This is message body"; // velocity template without any placeholders
        Collection<? extends ItemDelta<?, ?>> modifications = prismContext.deltaFor(SystemConfigurationType.class)
                .item(SystemConfigurationType.F_MESSAGE_TRANSPORT_CONFIGURATION)
                .replace(new MessageTransportConfigurationType(prismContext)
                        .customTransport(new CustomTransportConfigurationType(prismContext)
                                .name("foo")
                                .type(TestMessageTransport.class.getName())))
                .item(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION)
                .replace(new NotificationConfigurationType(prismContext)
                        .handler(new EventHandlerType()
                                .generalNotifier(new GeneralNotifierType()
                                        .bodyExpression(velocityExpression(messageBody))
                                        .transport("foo"))))
                .asItemDeltas();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, getTestOperationResult());
        TestMessageTransport testTransport = (TestMessageTransport) transportService.getTransport("foo");
        assertThat(testTransport.getMessages()).isEmpty();

        when("event is sent to notification manager");
        CustomEventImpl event = new CustomEventImpl(lightweightIdentifierGenerator, "test", null, null,
                null, // TODO why is this not nullable?
                EventStatusType.SUCCESS, "test-channel");
        // This is used as default recipient, no recipient results in no message.
        event.setRequestee(new SimpleObjectRefImpl(notificationFunctions,
                new UserType(prismContext).emailAddress("user@example.com")));
        notificationManager.processEvent(event, getTestTask(), getTestOperationResult());

        then("transport sends the message");
        assertThat(testTransport.getMessages()).hasSize(1);
        Message message = testTransport.getMessages().get(0);
        assertThat(message).isNotNull();
        assertThat(message.getTo()).containsExactlyInAnyOrder("user@example.com");
        assertThat(message.getBody()).isEqualTo(messageBody);
    }

    @Test
    public void test900NotifierWithoutTransportDoesNotSendAnything() throws Exception {
        given("configuration with notifier without transport");
        Collection<? extends ItemDelta<?, ?>> modifications = prismContext.deltaFor(SystemConfigurationType.class)
                .item(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION)
                .replace(new NotificationConfigurationType(prismContext)
                        .handler(new EventHandlerType()
                                .generalNotifier(new GeneralNotifierType())))
                .asItemDeltas();
        OperationResult result = getTestOperationResult();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, result);

        when("event is sent to notification manager");
        CustomEventImpl event = new CustomEventImpl(lightweightIdentifierGenerator, "test", null, null,
                null, // TODO why is this not nullable?
                EventStatusType.SUCCESS, "test-channel");
        notificationManager.processEvent(event, getTestTask(), result);

        then("result is success, but no message was sent because there is no transport");
        assertThatOperationResult(result)
                .isSuccess()
                .anySubResultMatches(s ->
                        s.isNotApplicable() && s.getMessage().equals("No transports"));
    }

    @Test
    public void test910NotifierWithoutRecipientAndEventWithoutDefaultRecipientDoesNotSendAnything() throws Exception {
        given("configuration with transport and notifier without recipient expression");
        Collection<? extends ItemDelta<?, ?>> modifications = prismContext.deltaFor(SystemConfigurationType.class)
                .item(SystemConfigurationType.F_MESSAGE_TRANSPORT_CONFIGURATION)
                .replace(new MessageTransportConfigurationType(prismContext)
                        .customTransport(new CustomTransportConfigurationType(prismContext)
                                .name("foo")
                                .type(TestMessageTransport.class.getName())))
                .item(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION)
                .replace(new NotificationConfigurationType(prismContext)
                        .handler(new EventHandlerType()
                                .generalNotifier(new GeneralNotifierType()
                                        .transport("foo"))))
                .asItemDeltas();
        OperationResult result = getTestOperationResult();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, result);
        TestMessageTransport testTransport = (TestMessageTransport) transportService.getTransport("foo");
        assertThat(testTransport.getMessages()).isEmpty();

        when("event without default recipient is sent to notification manager");
        CustomEventImpl event = new CustomEventImpl(lightweightIdentifierGenerator, "test", null, null,
                null, // TODO why is this not nullable?
                EventStatusType.SUCCESS, "test-channel");
        // No requestee set, that would be taken as default recipient, see test100
        notificationManager.processEvent(event, getTestTask(), result);

        then("result is success, but no message was sent because of no recipients");
        assertThatOperationResult(result)
                .isSuccess()
                .anySubResultMatches(s ->
                        s.isNotApplicable() && s.getMessage().equals("No recipients"));
        assertThat(testTransport.getMessages()).isEmpty();
    }

    @Test
    public void test920NotifierWithoutBodyDoesNotSendAnything() throws Exception {
        given("configuration with transport and notifier without body expression");
        Collection<? extends ItemDelta<?, ?>> modifications = prismContext.deltaFor(SystemConfigurationType.class)
                .item(SystemConfigurationType.F_MESSAGE_TRANSPORT_CONFIGURATION)
                .replace(new MessageTransportConfigurationType(prismContext)
                        .customTransport(new CustomTransportConfigurationType(prismContext)
                                .name("foo")
                                .type(TestMessageTransport.class.getName())))
                .item(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION)
                .replace(new NotificationConfigurationType(prismContext)
                        .handler(new EventHandlerType()
                                .generalNotifier(new GeneralNotifierType()
                                        .transport("foo"))))
                .asItemDeltas();
        OperationResult result = getTestOperationResult();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, result);
        TestMessageTransport testTransport = (TestMessageTransport) transportService.getTransport("foo");
        assertThat(testTransport.getMessages()).isEmpty();

        when("event with default recipient is sent to notification manager");
        CustomEventImpl event = new CustomEventImpl(lightweightIdentifierGenerator, "test", null, null,
                null, // TODO why is this not nullable?
                EventStatusType.SUCCESS, "test-channel");
        // this will be used as default recipient
        event.setRequestee(new SimpleObjectRefImpl(notificationFunctions,
                new UserType(prismContext).emailAddress("user@example.com")));
        notificationManager.processEvent(event, getTestTask(), result);

        then("result is success, but no message was sent because of empty body");
        assertThatOperationResult(result)
                .isSuccess()
                .anySubResultMatches(s ->
                        s.isNotApplicable() && s.getMessage().equals("No message body"));
        assertThat(testTransport.getMessages()).isEmpty();
    }

    @Test
    public void test930NotifierUsingNonexistentTransportDoesNotSendAnything() throws Exception {
        given("configuration with transport and notifier without recipient expression");
        Collection<? extends ItemDelta<?, ?>> modifications = prismContext.deltaFor(SystemConfigurationType.class)
                .item(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION)
                .replace(new NotificationConfigurationType(prismContext)
                        .handler(new EventHandlerType()
                                .generalNotifier(new GeneralNotifierType()
                                        .transport("nonexistent"))))
                .asItemDeltas();
        OperationResult result = getTestOperationResult();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, result);

        when("event without default recipient is sent to notification manager");
        CustomEventImpl event = new CustomEventImpl(lightweightIdentifierGenerator, "test", null, null,
                null, // TODO why is this not nullable?
                EventStatusType.SUCCESS, "test-channel");
        // this will be used as default recipient
        event.setRequestee(new SimpleObjectRefImpl(notificationFunctions,
                new UserType(prismContext).emailAddress("user@example.com")));
        notificationManager.processEvent(event, getTestTask(), result);

        then("result is fatal error because transport can't be found");
        assertThatOperationResult(result)
                .isFatalError() // TODO should this really be fatal error?
                .hasMessage("Unknown transport named 'nonexistent'");
    }

    private ExpressionType velocityExpression(String velocityTemplate) {
        return new ExpressionType()
                .expressionEvaluator(new ObjectFactory().createScript(
                        new ScriptExpressionEvaluatorType()
                                .language(VelocityScriptEvaluator.LANGUAGE_URL)
                                .code(velocityTemplate)));
    }
}
