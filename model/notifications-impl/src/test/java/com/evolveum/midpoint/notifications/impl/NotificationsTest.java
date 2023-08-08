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

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;
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
import com.evolveum.prism.xml.ns._public.types_3.RawType;

@ContextConfiguration(locations = { "classpath:ctx-notifications-test.xml" })
public class NotificationsTest extends AbstractIntegrationTest {

    private static final String SYS_CONFIG_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();

    @Autowired private TransportService transportService;
    @Autowired private NotificationManager notificationManager;

    private final AtomicInteger idSeq = new AtomicInteger();
    private final LightweightIdentifierGenerator lightweightIdentifierGenerator =
            () -> new LightweightIdentifier(System.currentTimeMillis(), 0, idSeq.incrementAndGet());

    @Override
    protected void initSystem(Task task, OperationResult initResult) throws Exception {
        OperationResult result = new OperationResult("init");

        repositoryService.addObject(new SystemConfigurationType()
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
                .replace(new MessageTransportConfigurationType()
                        .file(new FileTransportConfigurationType()
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
                .replace(new MessageTransportConfigurationType()
                        .mail(new MailTransportConfigurationType()
                                .name("mail1"))
                        .mail(new MailTransportConfigurationType()
                                .name("mail2"))
                        .sms(new SmsTransportConfigurationType()
                                .name("sms1"))
                        .sms(new SmsTransportConfigurationType()
                                .name("sms2"))
                        .file(new FileTransportConfigurationType()
                                .name("file1"))
                        .file(new FileTransportConfigurationType()
                                .name("file2"))
                        .customTransport(new CustomTransportConfigurationType()
                                .name("custom1"))
                        .customTransport(new CustomTransportConfigurationType()
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
                .replace(new NotificationConfigurationType()
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
        CustomEventImpl event = createCustomEvent();
        // This is used as default recipient, no recipient results in no message.
        event.setRequestee(new SimpleObjectRefImpl(
                new UserType().emailAddress("user@example.com")));
        notificationManager.processEvent(event, getTestTask(), result);

        then("transport sends the message");
        Message message = getSingleMessage(testTransport);
        assertThat(message).isNotNull();
        assertThat(message.getTo()).containsExactlyInAnyOrder("user@example.com");
        assertThat(message.getBody()).startsWith(messageBody); // there can be subscription footer
    }

    @Test
    public void test110NotifierWithMessageTemplateReference() throws Exception {
        OperationResult result = getTestOperationResult();

        given("message template");
        String objectName = "messageTemplate" + getTestNumber();
        String templateOid = repositoryService.addObject(
                new MessageTemplateType()
                        .name(objectName)
                        .defaultContent(new MessageTemplateContentType()
                                .subjectExpression(velocityExpression("template-subject"))
                                .bodyExpression(velocityExpression("Hi $requestee.name, channel: $!event.channel"))
                                .attachment(new NotificationMessageAttachmentType()
                                        .contentType("text/plain")
                                        .content("some-text")))
                        .asPrismObject(),
                null, result);

        and("configuration with transport and notifier using the template");
        Collection<? extends ItemDelta<?, ?>> modifications = systemConfigModificationWithTestTransport("test")
                .item(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION)
                .replace(new NotificationConfigurationType()
                        .handler(new EventHandlerType()
                                .generalNotifier(new GeneralNotifierType()
                                        .messageTemplateRef(createObjectReference(
                                                templateOid, MessageTemplateType.COMPLEX_TYPE, null))
                                        .transport("test"))))
                .asItemDeltas();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, result);
        TestMessageTransport testTransport = (TestMessageTransport) transportService.getTransport("test");
        assertThat(testTransport.getMessages()).isEmpty();

        when("event is sent to notification manager");
        CustomEventImpl event = createCustomEvent();
        // This is used as default recipient, no recipient results in no message.
        event.setRequestee(new SimpleObjectRefImpl(
                new UserType().name("John").emailAddress("user@example.com")));
        notificationManager.processEvent(event, getTestTask(), result);

        then("transport sends the message");
        Message message = getSingleMessage(testTransport);
        assertThat(message).isNotNull();
        assertThat(message.getTo()).containsExactlyInAnyOrder("user@example.com");
        assertThat(message.getBody()).startsWith("Hi John, channel: test-channel"); // there can be subscription footer
        assertThat(message.getSubject()).isEqualTo("template-subject");
    }

    @Test
    public void test120NotifierWithMessageTemplateReferenceAndOverridingContentParts() throws Exception {
        OperationResult result = getTestOperationResult();

        given("message template");
        String objectName = "messageTemplate" + getTestNumber();
        String templateOid = repositoryService.addObject(
                new MessageTemplateType()
                        .name(objectName)
                        .defaultContent(new MessageTemplateContentType()
                                .subjectExpression(velocityExpression("template-subject"))
                                .bodyExpression(velocityExpression("template-body"))
                                .contentType("text/plain")
                                .attachment(new NotificationMessageAttachmentType()
                                        .contentType("text/plain")
                                        .content("attachment1")))
                        .asPrismObject(),
                null, result);

        and("configuration with transport and notifier using the template");
        Collection<? extends ItemDelta<?, ?>> modifications = systemConfigModificationWithTestTransport("test")
                .item(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION)
                .replace(new NotificationConfigurationType()
                        .handler(new EventHandlerType()
                                .generalNotifier(new GeneralNotifierType()
                                        .messageTemplateRef(createObjectReference(
                                                templateOid, MessageTemplateType.COMPLEX_TYPE, null))
                                        // overrides content from the template
                                        .subjectExpression(velocityExpression("notifier-subject"))
                                        .bodyExpression(velocityExpression("notifier-body"))
                                        .attachment(new NotificationMessageAttachmentType()
                                                .contentType("text/plain")
                                                .content("attachment2"))
                                        .transport("test"))))
                .asItemDeltas();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, result);
        assertThat(((TestMessageTransport) transportService.getTransport("test")).getMessages()).isEmpty();

        when("event is sent to notification manager");
        CustomEventImpl event = createCustomEvent();
        // This is used as default recipient, no recipient results in no message.
        event.setRequestee(new SimpleObjectRefImpl(
                new UserType().emailAddress("user@example.com")));
        notificationManager.processEvent(event, getTestTask(), result);

        then("transport sends the message with content from notifier overriding the declared template parts");
        Message message = getSingleMessage(((TestMessageTransport) transportService.getTransport("test")));
        assertThat(message).isNotNull();
        assertThat(message.getTo()).containsExactlyInAnyOrder("user@example.com");
        assertThat(message.getBody()).startsWith("notifier-body"); // there can be subscription footer
        assertThat(message.getSubject()).isEqualTo("notifier-subject");
        assertThat(message.getAttachments()).hasSize(2)
                .anyMatch(a -> getRawValue(a.getContent()).equals("attachment1"))
                .anyMatch(a -> getRawValue(a.getContent()).equals("attachment2"));
    }

    @Test
    public void test150NotifierWithLocalizedMessageTemplate() throws Exception {
        OperationResult result = getTestOperationResult();

        given("localized message template");
        String objectName = "messageTemplate" + getTestNumber();
        String templateOid = repositoryService.addObject(
                new MessageTemplateType()
                        .name(objectName)
                        .defaultContent(new MessageTemplateContentType()
                                .bodyExpression(velocityExpression("template-body-default")))
                        .localizedContent(new LocalizedMessageTemplateContentType()
                                .language("sk")
                                .bodyExpression(velocityExpression("template-body-sk")))
                        .asPrismObject(),
                null, result);

        and("configuration with transport and notifier using the template");
        Collection<? extends ItemDelta<?, ?>> modifications = systemConfigModificationWithTestTransport("test")
                .item(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION)
                .replace(new NotificationConfigurationType()
                        .handler(new EventHandlerType()
                                .generalNotifier(new GeneralNotifierType()
                                        .messageTemplateRef(createObjectReference(
                                                templateOid, MessageTemplateType.COMPLEX_TYPE, null))
                                        .transport("test"))))
                .asItemDeltas();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, result);
        TestMessageTransport testTransport = (TestMessageTransport) transportService.getTransport("test");
        assertThat(testTransport.getMessages()).isEmpty();

        when("event is sent to notification manager, recipient has no language set");
        CustomEventImpl event = createCustomEvent();
        // This is used as default recipient, no recipient results in no message.
        event.setRequestee(new SimpleObjectRefImpl(
                new UserType().emailAddress("user@example.com")));
        testTransport.clearMessages();
        notificationManager.processEvent(event, getTestTask(), result);

        then("transport sends the message with default template content");
        Message message = getSingleMessage(testTransport);
        assertThat(message.getTo()).containsExactlyInAnyOrder("user@example.com");
        assertThat(message.getBody()).startsWith("template-body-default"); // there can be subscription footer

        // now when-then for sk language
        when("event is sent to notification manager, recipient has 'sk' language set");
        event = createCustomEvent();
        event.setRequestee(new SimpleObjectRefImpl(
                new UserType().emailAddress("user2@example.com").preferredLanguage("sk")));
        testTransport.clearMessages();
        notificationManager.processEvent(event, getTestTask(), result);

        then("transport sends the message with template content for 'sk' language");
        message = getSingleMessage(testTransport);
        assertThat(message.getTo()).containsExactlyInAnyOrder("user2@example.com");
        assertThat(message.getBody()).startsWith("template-body-sk"); // there can be subscription footer

        // now when-then for other language
        when("event is sent to notification manager, recipient has other language set");
        event = createCustomEvent();
        event.setRequestee(new SimpleObjectRefImpl(
                new UserType().emailAddress("user3@example.com").preferredLanguage("uk")));
        testTransport.clearMessages();
        notificationManager.processEvent(event, getTestTask(), result);

        then("transport sends the message with default template content, because no localized content for specified language is found");
        message = getSingleMessage(testTransport);
        assertThat(message.getTo()).containsExactlyInAnyOrder("user3@example.com");
        assertThat(message.getBody()).startsWith("template-body-default");
    }

    @Test
    public void test160LocalizedMessageTemplateAttachmentInheritance() throws Exception {
        OperationResult result = getTestOperationResult();

        given("localized message template with attachment");
        String objectName = "messageTemplate" + getTestNumber();
        String templateOid = repositoryService.addObject(
                new MessageTemplateType()
                        .name(objectName)
                        .defaultContent(new MessageTemplateContentType()
                                .bodyExpression(velocityExpression("template-body-default"))
                                .attachmentExpression(groovyExpression("def a = new com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationMessageAttachmentType();\n"
                                        + "a.setContentType(\"text/plain\");\n"
                                        + "a.setContent(\"default-content1\");\n"
                                        + "return a;"))
                                .attachment(new NotificationMessageAttachmentType()
                                        .contentType("text/plain")
                                        .content("default-content2")))
                        // this will use its own attachment element and inherit attachmentExpression from default
                        .localizedContent(new LocalizedMessageTemplateContentType()
                                .language("sk")
                                .bodyExpression(velocityExpression("template-body-sk"))
                                .attachment(new NotificationMessageAttachmentType()
                                        .contentType("text/plain")
                                        .content("sk-content2")))
                        // this will use its own attachmentExpression element and inherit attachment from default
                        .localizedContent(new LocalizedMessageTemplateContentType()
                                .language("cz")
                                .bodyExpression(velocityExpression("template-body-cz"))
                                .attachmentExpression(groovyExpression("def a = new com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationMessageAttachmentType();\n"
                                        + "a.setContentType(\"text/plain\");\n"
                                        + "a.setContent(\"cz-content1\");\n"
                                        + "return a;")))
                        .asPrismObject(),
                null, result);

        and("configuration with transport and notifier using the template");
        Collection<? extends ItemDelta<?, ?>> modifications = systemConfigModificationWithTestTransport("test")
                .item(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION)
                .replace(new NotificationConfigurationType()
                        .handler(new EventHandlerType()
                                .generalNotifier(new GeneralNotifierType()
                                        .messageTemplateRef(createObjectReference(
                                                templateOid, MessageTemplateType.COMPLEX_TYPE, null))
                                        .transport("test"))))
                .asItemDeltas();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, result);
        TestMessageTransport testTransport = (TestMessageTransport) transportService.getTransport("test");
        assertThat(testTransport.getMessages()).isEmpty();

        when("event is sent to notification manager, recipient has no language set");
        CustomEventImpl event = createCustomEvent();
        // This is used as default recipient, no recipient results in no message.
        event.setRequestee(new SimpleObjectRefImpl(
                new UserType().emailAddress("user@example.com")));
        notificationManager.processEvent(event, getTestTask(), result);

        then("transport sends the message with default template content");
        Message message = getSingleMessage(testTransport);
        assertThat(message.getAttachments()).hasSize(2)
                .anyMatch(a -> getRawValue(a.getContent()).equals("default-content1")) // from expression
                .anyMatch(a -> getRawValue(a.getContent()).equals("default-content2"));

        // now when-then for sk language (attachment expression inherited)
        when("recipient has 'sk' language set");
        event = createCustomEvent();
        event.setRequestee(new SimpleObjectRefImpl(
                new UserType().emailAddress("user2@example.com").preferredLanguage("sk")));
        testTransport.clearMessages();
        notificationManager.processEvent(event, getTestTask(), result);

        then("message uses attachment expression from default content");
        message = getSingleMessage(testTransport);
        assertThat(message.getAttachments()).hasSize(2)
                .anyMatch(a -> getRawValue(a.getContent()).equals("default-content1")) // from expression
                .anyMatch(a -> getRawValue(a.getContent()).equals("sk-content2"));

        // now when-then for cz language (attachment inherited)
        when("event is sent to notification manager, recipient has 'cz' language set");
        event = createCustomEvent();
        event.setRequestee(new SimpleObjectRefImpl(
                new UserType().emailAddress("user3@example.com").preferredLanguage("cz")));
        testTransport.clearMessages();
        notificationManager.processEvent(event, getTestTask(), result);

        then("transport sends the message with default template content, because no localized content for specified language is found");
        message = getSingleMessage(testTransport);
        assertThat(message.getAttachments()).hasSize(2)
                .anyMatch(a -> getRawValue(a.getContent()).equals("cz-content1")) // from expression
                .anyMatch(a -> getRawValue(a.getContent()).equals("default-content2"));
    }

    @Test
    public void test200RecipientExpressionReturningFocus() throws Exception {
        OperationResult result = getTestOperationResult();

        given("configuration with notifier's recipient expression returning focus object");
        String messageBody = "This is message body"; // velocity template without any placeholders
        Collection<? extends ItemDelta<?, ?>> modifications = systemConfigModificationWithTestTransport("test")
                .item(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION)
                .replace(new NotificationConfigurationType()
                        .handler(new EventHandlerType()
                                .generalNotifier(new GeneralNotifierType()
                                        // requestee provided with the event below
                                        .recipientExpression(groovyExpression("return requestee"))
                                        .bodyExpression(velocityExpression(messageBody))
                                        .transport("test"))))
                .asItemDeltas();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, result);
        TestMessageTransport testTransport = (TestMessageTransport) transportService.getTransport("test");
        assertThat(testTransport.getMessages()).isEmpty();

        when("event is sent to notification manager");
        CustomEventImpl event = createCustomEvent();
        // This is used as default recipient, no recipient results in no message.
        event.setRequestee(new SimpleObjectRefImpl(
                new UserType()
                        // this will be returned by TestMessageTransport.getDefaultRecipientAddress
                        .emailAddress("user@example.com")));
        notificationManager.processEvent(event, getTestTask(), result);

        then("transport sends the message");
        Message message = getSingleMessage(testTransport);
        assertThat(message).isNotNull();
        assertThat(message.getTo()).containsExactlyInAnyOrder("user@example.com");
        assertThat(message.getBody()).startsWith(messageBody); // there can be subscription footer
    }

    @Test
    public void test210RecipientExpressionReturningLiteralValue() throws Exception {
        OperationResult result = getTestOperationResult();

        given("configuration with notifier's recipient expression returning literal value");
        String messageBody = "This is message body"; // velocity template without any placeholders
        Collection<? extends ItemDelta<?, ?>> modifications = systemConfigModificationWithTestTransport("test")
                .item(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION)
                .replace(new NotificationConfigurationType()
                        .handler(new EventHandlerType()
                                .generalNotifier(new GeneralNotifierType()
                                        .recipientExpression(literalExpression("literal@example.com"))
                                        .bodyExpression(velocityExpression(messageBody))
                                        .transport("test"))))
                .asItemDeltas();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, result);
        TestMessageTransport testTransport = (TestMessageTransport) transportService.getTransport("test");
        assertThat(testTransport.getMessages()).isEmpty();

        when("event is sent to notification manager");
        CustomEventImpl event = createCustomEvent();
        notificationManager.processEvent(event, getTestTask(), result);

        then("transport sends the message");
        Message message = getSingleMessage(testTransport);
        assertThat(message).isNotNull();
        assertThat(message.getTo()).containsExactlyInAnyOrder("literal@example.com");
        assertThat(message.getBody()).startsWith(messageBody); // there can be subscription footer
    }

    @Test
    public void test300MessageTransportUsingRecipientAddressExpression() throws Exception {
        OperationResult result = getTestOperationResult();

        given("configuration with transport using recipient address expression");
        String messageBody = "This is message body"; // velocity template without any placeholders
        Collection<? extends ItemDelta<?, ?>> modifications = prismContext.deltaFor(SystemConfigurationType.class)
                .item(SystemConfigurationType.F_MESSAGE_TRANSPORT_CONFIGURATION)
                .replace(new MessageTransportConfigurationType()
                        .customTransport(new CustomTransportConfigurationType()
                                .name("test")
                                .recipientAddressExpression(groovyExpression("this.binding.variables.each {k,v -> println \"$k = $v\"};\n"
                                        + "return 'xxx' + recipient.emailAddress"))
                                .type(TestMessageTransport.class.getName())))
                .item(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION)
                .replace(new NotificationConfigurationType()
                        .handler(new EventHandlerType()
                                .generalNotifier(new GeneralNotifierType()
                                        // requestee provided with the event below
                                        .recipientExpression(groovyExpression("return requestee"))
                                        .bodyExpression(velocityExpression(messageBody))
                                        .transport("test"))))
                .asItemDeltas();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, result);
        TestMessageTransport testTransport = (TestMessageTransport) transportService.getTransport("test");
        assertThat(testTransport.getMessages()).isEmpty();

        when("event with requestee is sent to notification manager");
        CustomEventImpl event = createCustomEvent();
        event.setRequestee(new SimpleObjectRefImpl(
                new UserType().emailAddress("user@example.com")));
        notificationManager.processEvent(event, getTestTask(), result);

        then("transport sends the message");
        Message message = getSingleMessage(testTransport);
        assertThat(message).isNotNull();
        and("address is based on notifier/recipientExpression -> transport/recipientAddressExpression chain");
        assertThat(message.getTo()).containsExactlyInAnyOrder("xxxuser@example.com");
        assertThat(message.getBody()).startsWith(messageBody); // there can be subscription footer
    }

    @Test
    public void test900NotifierWithoutTransportDoesNotSendAnything() throws Exception {
        given("configuration with notifier without transport");
        Collection<? extends ItemDelta<?, ?>> modifications = prismContext.deltaFor(SystemConfigurationType.class)
                .item(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION)
                .replace(new NotificationConfigurationType()
                        .handler(new EventHandlerType()
                                .generalNotifier(new GeneralNotifierType())))
                .asItemDeltas();
        OperationResult result = getTestOperationResult();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, result);

        when("event is sent to notification manager");
        CustomEventImpl event = createCustomEvent();
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
                .replace(new NotificationConfigurationType()
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
        CustomEventImpl event = createCustomEvent();
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
                .replace(new NotificationConfigurationType()
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
        CustomEventImpl event = createCustomEvent();
        // this will be used as default recipient
        event.setRequestee(new SimpleObjectRefImpl(
                new UserType().emailAddress("user@example.com")));
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
                .replace(new NotificationConfigurationType()
                        .handler(new EventHandlerType()
                                .generalNotifier(new GeneralNotifierType()
                                        .transport("nonexistent"))))
                .asItemDeltas();
        OperationResult result = getTestOperationResult();
        repositoryService.modifyObject(
                SystemConfigurationType.class, SYS_CONFIG_OID, modifications, result);

        when("event without default recipient is sent to notification manager");
        CustomEventImpl event = createCustomEvent();
        // this will be used as default recipient
        event.setRequestee(new SimpleObjectRefImpl(
                new UserType().emailAddress("user@example.com")));
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
                .replace(new MessageTransportConfigurationType()
                        .customTransport(new CustomTransportConfigurationType()
                                .name(transportName)
                                .type(TestMessageTransport.class.getName())));
    }

    @NotNull
    private CustomEventImpl createCustomEvent() {
        return new CustomEventImpl(
                lightweightIdentifierGenerator, "test", null,
                EventOperationType.ADD, // TODO why is this not nullable or some OTHER operation is not available?
                EventStatusType.SUCCESS, "test-channel");
    }

    private Message getSingleMessage(TestMessageTransport testTransport) {
        assertThat(testTransport.getMessages()).hasSize(1);
        return testTransport.getMessages().get(0);
    }

    private Object getRawValue(Object value) {
        try {
            return RawType.getValue(value);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }
}
