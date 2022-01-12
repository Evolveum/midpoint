/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.configuration;

import java.io.Serializable;

/**
 * @author skublik
 */

public class SamlAdditionalConfiguration implements Serializable {

    private final String nameOfUsernameAttribute;
    private final String linkText;

    private SamlAdditionalConfiguration(String nameOfUsernameAttribute, String linkText) {
        this.nameOfUsernameAttribute = nameOfUsernameAttribute;
        this.linkText = linkText;
    }

    public String getNameOfUsernameAttribute() {
        return nameOfUsernameAttribute;
    }

    public String getLinkText() {
        return linkText;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String nameOfUsernameAttribute;
        private String linkText;

        private Builder() {
        }

        public Builder nameOfUsernameAttribute(String nameOfUsernameAttribute) {
            this.nameOfUsernameAttribute = nameOfUsernameAttribute;
            return this;
        }

        public Builder linkText(String textOfLink) {
            this.linkText = textOfLink;
            return this;
        }

        public SamlAdditionalConfiguration build(){
            return new SamlAdditionalConfiguration(this.nameOfUsernameAttribute, this.linkText);
        }
    }
}
