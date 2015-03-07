/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.web.util.validation;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import java.io.Serializable;

/**
 *  This is just a simple representation of custom form validation error. Currently, it holds
 *  only a simple String 'message' attribute as an information about validation error and
 *  an ItemPathType 'attribute' as a path to the source of error. Feel free
 *  to add any information about validation errors that your custom validator requires.
 *
 *  @author shood
 * */
public class SimpleValidationError implements Serializable {

    private String message;
    private ItemPathType attribute;

    public SimpleValidationError() {}

    public SimpleValidationError(String message, ItemPathType attribute) {
        this.message = message;
        this.attribute = attribute;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ItemPathType getAttribute() {
        return attribute;
    }

    public void setAttribute(ItemPathType attribute) {
        this.attribute = attribute;
    }

    /**
     *  Override to create custom implementation of printing the attribute
     *  (for logging and GUI purposes)
     * */
    public String printAttribute(){
        return getAttribute().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SimpleValidationError)) return false;

        SimpleValidationError that = (SimpleValidationError) o;

        if (attribute != null ? !attribute.equals(that.attribute) : that.attribute != null) return false;
        if (message != null ? !message.equals(that.message) : that.message != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = message != null ? message.hashCode() : 0;
        result = 31 * result + (attribute != null ? attribute.hashCode() : 0);
        return result;
    }
}
