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

import java.io.Serializable;

/**
 * @author lazyman
 */
public class NotifyMessage implements Serializable {

    public static enum Type {INFO, SUCCESS, WARN, ERROR}

    private String title;
    private String message;
    private Type type;
    private String icon;

    public NotifyMessage(String title, String message) {
        this(title, message, Type.INFO);
    }

    public NotifyMessage(String title, String message, Type type) {
        this(title, message, type, null);
    }

    public NotifyMessage(String title, String message, Type type, String icon) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.icon = icon;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public Type getType() {
        if (type == null) {
            return Type.INFO;
        }
        return type;
    }

    public String getIcon() {
        if (icon != null) {
            return icon;
        }

        switch (getType()) {
            case SUCCESS:
                return "silk-accept";
            case WARN:
                return "silk-error";
            case ERROR:
                return "silk-exclamation";
            case INFO:
            default:
                return "silk-information";
        }
    }
}
