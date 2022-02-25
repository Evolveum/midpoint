/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.notifications.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static com.evolveum.midpoint.schema.util.SimpleExpressionUtil.*;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.notifications.api.transports.TransportService;
import com.evolveum.midpoint.notifications.impl.events.CustomEventImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.S_ItemEntry;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifier;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.transport.impl.CustomMessageTransport;
import com.evolveum.midpoint.transport.impl.FileMessageTransport;
import com.evolveum.midpoint.transport.impl.MailMessageTransport;
import com.evolveum.midpoint.transport.impl.SmsMessageTransport;
import com.evolveum.midpoint.util.exception.SchemaException;
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
        OperationResult result = new OperationResult("init");

        repositoryService.addObject(new SystemConfigurationType(prismContext)
                .oid(SYS_CONFIG_OID)
                .name("sys-config")
                .asPrismObject(), null, result);
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
        Collection<? extends ItemDelta<?, ?>> modifications =
                systemConfigModificationWithTestTransport("test").asItemDeltas();

        when("sysconfig is modified in repository");
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, getTestOperationResult());

        then("transport of the right type is registered");
        assertThat(transportService.getTransport("test"))
                .isNotNull()
                .isInstanceOf(TestMessageTransport.class);
        assertThat(transportService.getTransport("test").getName()).isEqualTo("test");
    }

    @Test
    public void test020TransportSysconfigChangeRemovesObsoleteTransports() throws Exception {
        given("sysconfig with transport foo");
        Collection<? extends ItemDelta<?, ?>> modifications =
                systemConfigModificationWithTestTransport("foo").asItemDeltas();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, getTestOperationResult());
        assertThat(transportService.getTransport("foo")).isNotNull();

        when("sysconfig is updated to have only transport bar");
        modifications = prismContext.deltaFor(SystemConfigurationType.class)
                .item(SystemConfigurationType.F_MESSAGE_TRANSPORT_CONFIGURATION)
                .replace(new MessageTransportConfigurationType(prismContext)
                        .file(new FileTransportConfigurationType(prismContext)
                                .name("bar")))
                .asItemDeltas();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, getTestOperationResult());

        then("only transport bar is registered, foo is forgotten");
        assertThat(transportService.getTransport("bar"))
                .isNotNull()
                .isInstanceOf(FileMessageTransport.class);
        assertThatThrownBy(() -> transportService.getTransport("foo"))
                .hasMessageStartingWith("Unknown transport");
    }

    @Test
    public void test030TransportTypeInitializationTest() throws Exception {
        given("sysconfig with all known types of transport, each twice");
        Collection<? extends ItemDelta<?, ?>> modifications = prismContext.deltaFor(SystemConfigurationType.class)
                .item(SystemConfigurationType.F_MESSAGE_TRANSPORT_CONFIGURATION)
                .replace(new MessageTransportConfigurationType(prismContext)
                        .mail(new MailTransportConfigurationType(prismContext)
                                .name("mail1"))
                        .mail(new MailTransportConfigurationType(prismContext)
                                .name("mail2"))
                        .sms(new SmsTransportConfigurationType(prismContext)
                                .name("sms1"))
                        .sms(new SmsTransportConfigurationType(prismContext)
                                .name("sms2"))
                        .file(new FileTransportConfigurationType(prismContext)
                                .name("file1"))
                        .file(new FileTransportConfigurationType(prismContext)
                                .name("file2"))
                        .customTransport(new CustomTransportConfigurationType(prismContext)
                                .name("custom1"))
                        .customTransport(new CustomTransportConfigurationType(prismContext)
                                .name("custom2")))
                .asItemDeltas();

        when("sysconfig is modified");
        OperationResult result = getTestOperationResult();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, result);

        then("result is success and all the transports are properly registered");
        assertThatOperationResult(result).isSuccess();
        assertThat(transportService.getTransport("mail1"))
                .isNotNull()
                .isInstanceOf(MailMessageTransport.class);
        assertThat(transportService.getTransport("mail1").getConfiguration())
                .isInstanceOf(MailTransportConfigurationType.class);
        assertThat(transportService.getTransport("mail1").getName()).isEqualTo("mail1");

        assertThat(transportService.getTransport("mail2"))
                .isNotNull()
                .isInstanceOf(MailMessageTransport.class);

        assertThat(transportService.getTransport("sms1"))
                .isNotNull()
                .isInstanceOf(SmsMessageTransport.class);
        assertThat(transportService.getTransport("sms1").getConfiguration())
                .isInstanceOf(SmsTransportConfigurationType.class);
        assertThat(transportService.getTransport("sms1").getName()).isEqualTo("sms1");

        assertThat(transportService.getTransport("sms2"))
                .isNotNull()
                .isInstanceOf(SmsMessageTransport.class);

        assertThat(transportService.getTransport("file1"))
                .isNotNull()
                .isInstanceOf(FileMessageTransport.class);
        assertThat(transportService.getTransport("file1").getConfiguration())
                .isInstanceOf(FileTransportConfigurationType.class);
        assertThat(transportService.getTransport("file1").getName()).isEqualTo("file1");

        assertThat(transportService.getTransport("file2"))
                .isNotNull()
                .isInstanceOf(FileMessageTransport.class);

        assertThat(transportService.getTransport("custom1"))
                .isNotNull()
                .isInstanceOf(CustomMessageTransport.class);
        assertThat(transportService.getTransport("custom1").getConfiguration())
                .isInstanceOf(CustomTransportConfigurationType.class);
        assertThat(transportService.getTransport("custom1").getName()).isEqualTo("custom1");
        assertThat(transportService.getTransport("custom2"))
                .isNotNull()
                .isInstanceOf(CustomMessageTransport.class);
    }

    @Test
    public void test100CustomTransportSendingNotificationMessage() throws Exception {
        OperationResult result = getTestOperationResult();

        given("configuration with custom transport and some notifier");
        String messageBody = "This is message body"; // velocity template without any placeholders
        Collection<? extends ItemDelta<?, ?>> modifications = systemConfigModificationWithTestTransport("test")
                .item(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION)
                .replace(new NotificationConfigurationType(prismContext)
                        .handler(new EventHandlerType()
                                .generalNotifier(new GeneralNotifierType()
                                        .bodyExpression(velocityExpression(messageBody))
                                        .transport("test"))))
                .asItemDeltas();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, result);
        TestMessageTransport testTransport = (TestMessageTransport) transportService.getTransport("test");
        assertThat(testTransport.getMessages()).isEmpty();

        when("event is sent to notification manager");
        CustomEventImpl event = new CustomEventImpl(lightweightIdentifierGenerator, "test", null, null,
                null, // TODO why is this not nullable?
                EventStatusType.SUCCESS, "test-channel");
        // This is used as default recipient, no recipient results in no message.
        event.setRequestee(new SimpleObjectRefImpl(notificationFunctions,
                new UserType(prismContext).emailAddress("user@example.com")));
        notificationManager.processEvent(event, getTestTask(), result);

        then("transport sends the message");
        assertThat(testTransport.getMessages()).hasSize(1);
        Message message = testTransport.getMessages().get(0);
        assertThat(message).isNotNull();
        assertThat(message.getTo()).containsExactlyInAnyOrder("user@example.com");
        assertThat(message.getBody()).isEqualTo(messageBody);
    }

    @Test
    public void test110NotifierWithMessageTemplateReference() throws Exception {
        OperationResult result = getTestOperationResult();

        given("message template");
        String objectName = "messageTemplate" + getTestNumber();
        String templateOid = repositoryService.addObject(
                new MessageTemplateType(prismContext)
                        .name(objectName)
                        .defaultContent(new MessageTemplateContentType(prismContext)
                                .subjectExpression(velocityExpression("template-subject"))
                                .bodyExpression(velocityExpression("Notification about account-related operation\n\n"
                                        + "#if ($event.requesteeObject)Owner: $!event.requesteeDisplayName ($event.requesteeName, oid $event.requesteeOid)#end\n\n"
                                        + "Resource: $!event.resourceName (oid $event.resourceOid)\n\n"
                                        + "An account has been successfully created on the resource with attributes:\n"
                                        + "$event.contentAsFormattedList\n"
                                        + "Channel: $!event.channel"))
                                .attachment(new NotificationMessageAttachmentType()
                                        .contentType("text/plain")
                                        .content("some-text".getBytes(StandardCharsets.UTF_8))))
                        .asPrismObject(),
                null, result);

        and("configuration with transport and notifier using the template");
        Collection<? extends ItemDelta<?, ?>> modifications = systemConfigModificationWithTestTransport("test")
                .item(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION)
                .replace(new NotificationConfigurationType(prismContext)
                        .handler(new EventHandlerType()
                                .generalNotifier(new GeneralNotifierType()
                                        .messageTemplateRef(createObjectReference(
                                                templateOid, MessageTemplateType.COMPLEX_TYPE, null))
                                        // overrides content from the template
//                                        .subjectExpression(velocityExpression("notifier-subject"))
                                        .transport("test"))))
                .asItemDeltas();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, result);
        assertThat(((TestMessageTransport) transportService.getTransport("test")).getMessages()).isEmpty();

        when("event is sent to notification manager");
        CustomEventImpl event = new CustomEventImpl(lightweightIdentifierGenerator, "test", null, null,
                null, // TODO why is this not nullable?
                EventStatusType.SUCCESS, "test-channel");
        // This is used as default recipient, no recipient results in no message.
        event.setRequestee(new SimpleObjectRefImpl(notificationFunctions,
                new UserType(prismContext).emailAddress("user@example.com")));
        notificationManager.processEvent(event, getTestTask(), result);

        then("transport sends the message");
        assertThat(((TestMessageTransport) transportService.getTransport("test")).getMessages()).hasSize(1);
        Message message = ((TestMessageTransport) transportService.getTransport("test")).getMessages().get(0);
        assertThat(message).isNotNull();
        assertThat(message.getTo()).containsExactlyInAnyOrder("user@example.com");
        assertThat(message.getBody()).startsWith("Notification about");
        assertThat(message.getSubject()).isEqualTo("template-subject");
    }

    @Test
    public void test200RecipientExpressionReturningFocus() throws Exception {
        OperationResult result = getTestOperationResult();

        given("configuration with notifier's recipient expression returning focus object");
        String messageBody = "This is message body"; // velocity template without any placeholders
        Collection<? extends ItemDelta<?, ?>> modifications = systemConfigModificationWithTestTransport("test")
                .item(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION)
                .replace(new NotificationConfigurationType(prismContext)
                        .handler(new EventHandlerType()
                                .generalNotifier(new GeneralNotifierType()
                                        // provided with the event below
                                        .recipientExpression(groovyExpression("return requestee"))
                                        .bodyExpression(velocityExpression(messageBody))
                                        .transport("test"))))
                .asItemDeltas();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, result);
        TestMessageTransport testTransport = (TestMessageTransport) transportService.getTransport("test");
        assertThat(testTransport.getMessages()).isEmpty();

        when("event is sent to notification manager");
        CustomEventImpl event = new CustomEventImpl(lightweightIdentifierGenerator, "test", null, null,
                null, // TODO why is this not nullable?
                EventStatusType.SUCCESS, "test-channel");
        // This is used as default recipient, no recipient results in no message.
        event.setRequestee(new SimpleObjectRefImpl(notificationFunctions,
                new UserType(prismContext)
                        .preferredLanguage("sk")
                        // this will be returned by TestMessageTransport.getDefaultRecipientAddress
                        .emailAddress("user@example.com")));
        notificationManager.processEvent(event, getTestTask(), result);

        then("transport sends the message");
        assertThat(testTransport.getMessages()).hasSize(1);
        Message message = testTransport.getMessages().get(0);
        assertThat(message).isNotNull();
        assertThat(message.getTo()).containsExactlyInAnyOrder("user@example.com");
        assertThat(message.getBody()).isEqualTo(messageBody);
    }

    @Test
    public void test210RecipientExpressionReturningLiteralValue() throws Exception {
        OperationResult result = getTestOperationResult();

        given("configuration with notifier's recipient expression returning literal value");
        String messageBody = "This is message body"; // velocity template without any placeholders
        Collection<? extends ItemDelta<?, ?>> modifications = systemConfigModificationWithTestTransport("test")
                .item(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION)
                .replace(new NotificationConfigurationType(prismContext)
                        .handler(new EventHandlerType()
                                .generalNotifier(new GeneralNotifierType()
                                        // provided with the event below
                                        .recipientExpression(literalExpression("literal@example.com"))
                                        .bodyExpression(velocityExpression(messageBody))
                                        .transport("test"))))
                .asItemDeltas();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, result);
        TestMessageTransport testTransport = (TestMessageTransport) transportService.getTransport("test");
        assertThat(testTransport.getMessages()).isEmpty();

        when("event is sent to notification manager");
        CustomEventImpl event = new CustomEventImpl(lightweightIdentifierGenerator, "test", null, null,
                null, // TODO why is this not nullable?
                EventStatusType.SUCCESS, "test-channel");
        notificationManager.processEvent(event, getTestTask(), result);

        then("transport sends the message");
        assertThat(testTransport.getMessages()).hasSize(1);
        Message message = testTransport.getMessages().get(0);
        assertThat(message).isNotNull();
        assertThat(message.getTo()).containsExactlyInAnyOrder("literal@example.com");
        assertThat(message.getBody()).isEqualTo(messageBody);
    }

    @Test // TODO
    public void test120NotifierWithMessageTemplateReferenceOverridingContentParts() throws Exception {
        OperationResult result = getTestOperationResult();

        given("message template");
        String objectName = "messageTemplate" + getTestNumber();
        String templateOid = repositoryService.addObject(
                new MessageTemplateType(prismContext)
                        .name(objectName)
                        .defaultContent(new MessageTemplateContentType(prismContext)
                                .subjectExpression(velocityExpression("template-subject"))
                                .bodyExpression(velocityExpression("Notification about account-related operation\n\n"
                                        + "#if ($event.requesteeObject)Owner: $!event.requesteeDisplayName ($event.requesteeName, oid $event.requesteeOid)#end\n\n"
                                        + "Resource: $!event.resourceName (oid $event.resourceOid)\n\n"
                                        + "An account has been successfully created on the resource with attributes:\n"
                                        + "$event.contentAsFormattedList\n"
                                        + "Channel: $!event.channel"))
                                .contentType("text/plain")
                                .attachment(new NotificationMessageAttachmentType()
                                        .contentType("text/plain")
                                        .content("some-text".getBytes(StandardCharsets.UTF_8))))
                        .asPrismObject(),
                null, result);

        and("configuration with transport and notifier using the template");
        Collection<? extends ItemDelta<?, ?>> modifications = systemConfigModificationWithTestTransport("test")
                .item(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION)
                .replace(new NotificationConfigurationType(prismContext)
                        .handler(new EventHandlerType()
                                .generalNotifier(new GeneralNotifierType()
                                        .messageTemplateRef(createObjectReference(
                                                templateOid, MessageTemplateType.COMPLEX_TYPE, null))
                                        // overrides content from the template
//                                        .subjectExpression(velocityExpression("notifier-subject"))
                                        .transport("test"))))
                .asItemDeltas();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, result);
        assertThat(((TestMessageTransport) transportService.getTransport("test")).getMessages()).isEmpty();

        when("event is sent to notification manager");
        CustomEventImpl event = new CustomEventImpl(lightweightIdentifierGenerator, "test", null, null,
                null, // TODO why is this not nullable?
                EventStatusType.SUCCESS, "test-channel");
        // This is used as default recipient, no recipient results in no message.
        event.setRequestee(new SimpleObjectRefImpl(notificationFunctions,
                new UserType(prismContext).emailAddress("user@example.com")));
        notificationManager.processEvent(event, getTestTask(), result);

        then("transport sends the message");
        assertThat(((TestMessageTransport) transportService.getTransport("test")).getMessages()).hasSize(1);
        Message message = ((TestMessageTransport) transportService.getTransport("test")).getMessages().get(0);
        assertThat(message).isNotNull();
        assertThat(message.getTo()).containsExactlyInAnyOrder("user@example.com");
        assertThat(message.getBody()).startsWith("Notification about");
        assertThat(message.getSubject()).isEqualTo("template-subject");
        assertThat(message.getAttachments()).hasSize(1);
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
        Collection<? extends ItemDelta<?, ?>> modifications = systemConfigModificationWithTestTransport("foo")
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
        Collection<? extends ItemDelta<?, ?>> modifications = systemConfigModificationWithTestTransport("foo")
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

    /**
     * Shortcut for system config modification with typical test transport setup.
     * You can fluently continue with another `.item()` or finish with `.asItemDeltas()`.
     */
    private S_ItemEntry systemConfigModificationWithTestTransport(String transportName) throws SchemaException {
        return prismContext.deltaFor(SystemConfigurationType.class)
                .item(SystemConfigurationType.F_MESSAGE_TRANSPORT_CONFIGURATION)
                .replace(new MessageTransportConfigurationType(prismContext)
                        .customTransport(new CustomTransportConfigurationType(prismContext)
                                .name(transportName)
                                .type(TestMessageTransport.class.getName())));
    }
}
