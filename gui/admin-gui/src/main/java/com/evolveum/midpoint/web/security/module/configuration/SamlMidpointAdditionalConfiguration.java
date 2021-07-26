/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module.configuration;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;

/**
 * @author skublik
 */

public class SamlMidpointAdditionalConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(SamlMidpointAdditionalConfiguration.class);

    private String nameOfUsernameAttribute;
    private String linkText;

    private SamlMidpointAdditionalConfiguration(String nameOfUsernameAttribute, String linkText) {
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

        public SamlMidpointAdditionalConfiguration build(){
            return new SamlMidpointAdditionalConfiguration(this.nameOfUsernameAttribute, this.linkText);
        }
    }
}
