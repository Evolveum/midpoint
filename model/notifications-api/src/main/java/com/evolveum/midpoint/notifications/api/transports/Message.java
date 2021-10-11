/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.transports;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationMessageAttachmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationMessageType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class Message implements DebugDumpable {

    private String from;
    @NotNull private List<String> to = new ArrayList<>();
    @NotNull private List<String> cc = new ArrayList<>();
    @NotNull private List<String> bcc = new ArrayList<>();
    private String subject;
    private String body;         // todo
    private String contentType;
    @NotNull private List<NotificationMessageAttachmentType> attachments = new ArrayList<>();

    public Message() {
    }

    public Message(NotificationMessageType message) {
        from = message.getFrom();
        to.addAll(message.getTo());
        cc.addAll(message.getCc());
        bcc.addAll(message.getBcc());
        subject = message.getSubject();
        body = message.getBody();
        contentType = message.getContentType();
        attachments.addAll(message.getAttachment());
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    @NotNull
    public List<String> getTo() {
        return to;
    }

    public void setTo(@NotNull List<String> to) {
        this.to = to;
    }

    @NotNull
    public List<String> getCc() {
        return cc;
    }

    public void setCc(@NotNull List<String> cc) {
        this.cc = cc;
    }

    @NotNull
    public List<String> getBcc() {
        return bcc;
    }

    public void setBcc(@NotNull List<String> bcc) {
        this.bcc = bcc;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    @NotNull
    public List<NotificationMessageAttachmentType> getAttachments() {
        return attachments;
    }

    public void setAttachments(@NotNull List<NotificationMessageAttachmentType> attachments) {
        this.attachments = attachments;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Message{")
          .append("to='").append(to).append("'")
          .append((from != null ? ", from='" + from + "'" : ""))
          .append(", cc='").append(cc).append("'")
          .append(", bcc='").append(bcc).append("'")
          .append(", subject='").append(subject).append("'")
          .append(", contentType='").append(contentType).append("'")
          .append(", body='").append(body).append("'")
          .append(", attachmentsCount: ").append(attachments.size());
        if(attachments.size() > 0) {
            sb.append(", attachments: {");
            boolean isFirst = true;
            for(NotificationMessageAttachmentType attachment : attachments) {
                if(!isFirst) {
                    sb.append(", ");
                }
                isFirst= false;
                sb.append("[")
                  .append("contentType='").append(attachment.getContentType()).append("'")
                  .append(attachment.getContentFromFile() != null ?
                            ", contentFromFile='" + attachment.getContentFromFile() + "'" : "");
                if(attachment.getContent() != null) {
                    if(attachment.getContent() instanceof String) {
                        sb.append(", contentLength='").append(((String)attachment.getContent()).length()).append("'");
                    } else if(attachment.getContent() != null && attachment.getContent() instanceof String) {
                        sb.append(", contentSizeOfByte='").append(((byte[])attachment.getContent()).length).append("'");
                    } else {
                        sb.append(", content='").append(attachment.getContent().toString()).append("'");
                    }
                }
                sb.append(attachment.getFileName() != null ?
                            ", fileName='" + attachment.getFileName() + "'" : "")
                  .append("]");
            }
            sb.append("}");
        }
        return sb.toString();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder rv = new StringBuilder();
        rv.append("\n");
        DebugUtil.debugDumpLabel(rv, "Message", indent);
        rv.append("\n");

        if (from != null){
            DebugUtil.debugDumpWithLabel(rv, "From", from, indent+1);
            rv.append("\n");
        }

        DebugUtil.debugDumpWithLabel(rv, "To", to, indent+1);
        rv.append("\n");

        DebugUtil.debugDumpWithLabel(rv, "Cc", cc, indent+1);
        rv.append("\n");

        DebugUtil.debugDumpWithLabel(rv, "Bcc", bcc, indent+1);
        rv.append("\n");

        DebugUtil.debugDumpWithLabel(rv, "Subject", subject, indent+1);
        rv.append("\n");

        DebugUtil.debugDumpWithLabel(rv, "Content type", contentType, indent+1);
        rv.append("\n");

        DebugUtil.debugDumpWithLabel(rv, "Body", DebugUtil.fixIndentInMultiline(indent+1, DebugDumpable.INDENT_STRING, body), indent+1);
        rv.append("\n");

        DebugUtil.debugDumpLabel(rv, "Attachments", indent);
        rv.append("\n");

        attachments.forEach(attachment -> {

            DebugUtil.debugDumpLabel(rv, "Attachment", indent+2);
            rv.append("\n");

            DebugUtil.debugDumpWithLabel(rv, "Content type", attachment.getContentType(), indent+3);
            rv.append("\n");

            if (from != null){
                DebugUtil.debugDumpWithLabel(rv, "Content from file", attachment.getContentFromFile(), indent+3);
                rv.append("\n");
            }

            if(attachment.getContent() != null) {
                if(attachment.getContent() instanceof String) {
                    DebugUtil.debugDumpWithLabel(rv, "Content length", ((String)attachment.getContent()).length(), indent+3);
                    rv.append("\n");
                } else if(attachment.getContent() != null && attachment.getContent() instanceof String) {
                    DebugUtil.debugDumpWithLabel(rv, "Content size of byte", ((byte[])attachment.getContent()).length, indent+3);
                    rv.append("\n");
                }
            }

            if (from != null){
                DebugUtil.debugDumpWithLabel(rv, "File name", attachment.getFileName(), indent+3);
                rv.append("\n");
            }
        });
        return rv.toString();
    }
}
