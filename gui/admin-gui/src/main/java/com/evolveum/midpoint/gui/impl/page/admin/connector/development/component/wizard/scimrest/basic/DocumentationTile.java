/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.basic;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.schema.util.AiUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevDocumentationSourceType;

import org.apache.commons.lang3.StringUtils;

public class DocumentationTile extends TemplateTile<PrismContainerValueWrapper<ConnDevDocumentationSourceType>> {

    private final String uri;

    public DocumentationTile(PrismContainerValueWrapper<ConnDevDocumentationSourceType> docValue) {
        super(null, docValue.getRealValue().getName(), docValue);
        uri = docValue.getRealValue().getUri();
        setDescription(docValue.getRealValue().getDescription());
    }

    public String getUri() {
        return uri;
    }

    public boolean existUri() {
        return StringUtils.isNotEmpty(uri);
    }

    @Override
    public String getIcon() {
        return existUri() ? "fa fa-globe" : "fa fa-file";
    }

    @Override
    public boolean isSelected() {
        return getValue().isSelected();
    }

    @Override
    public void setSelected(boolean selected) {
        getValue().setSelected(selected);
    }

    public boolean isAIMarked() {
        return AiUtil.isMarkedAsAiProvided(getValue().getOldValue());
    }
}
