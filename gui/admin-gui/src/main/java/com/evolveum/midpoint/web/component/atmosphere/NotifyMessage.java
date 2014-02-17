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

package com.evolveum.midpoint.web.component.atmosphere;

import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusIcon;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class NotifyMessage implements Serializable {

    public static enum MessageType {INFO, SUCCESS, WARNING, DANGER}

    private String title;
    private String message;

    private OperationResultStatus type;
    private MessageType messageType;

    private String icon;

    public NotifyMessage(String title, String message) {
        this(title, message, OperationResultStatus.SUCCESS);
    }

    public NotifyMessage(String title, String message, OperationResultStatus type) {
        this(title, message, type, null);
    }

    public NotifyMessage(String title, String message, OperationResultStatus type, String icon) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.icon = icon;
    }

    public NotifyMessage(String title, String message, MessageType type, String icon) {
        this.title = title;
        this.message = message;
        this.messageType = type;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public MessageType getMessageType() {
        if (messageType != null) {
            return messageType;
        }

        switch (getType()) {
            case FATAL_ERROR:
            case PARTIAL_ERROR:
                return MessageType.DANGER;
            case IN_PROGRESS:
            case NOT_APPLICABLE:
                return MessageType.INFO;
            case SUCCESS:
                return MessageType.SUCCESS;
            case UNKNOWN:
            case WARNING:
            case HANDLED_ERROR:
            default:
                return MessageType.WARNING;
        }
    }

    public OperationResultStatus getType() {
        if (type == null) {
            return OperationResultStatus.UNKNOWN;
        }
        return type;
    }

    public String getIcon() {
        if (icon != null) {
            return icon;
        }

        return OperationResultStatusIcon.parseOperationalResultStatus(type).getIcon();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("NotifyMessage{");
        sb.append("title='").append(title).append('\'');
        sb.append(", message='").append(message).append('\'');
        sb.append(", type=").append(type);
        sb.append(", messageType=").append(messageType);
        sb.append(", icon='").append(icon).append('\'');
        sb.append('}');

        return sb.toString();
    }
}
