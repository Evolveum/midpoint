/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development.component.wizard.scimrest.relation;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.tile.TemplateTile;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnDevRelationInfoType;

public class ConnectorRelationshipTile extends Tile<PrismContainerValueWrapper<ConnDevRelationInfoType>> {

    private String subject;
    private String subjectAttr;

    private String object;
    private String objectAttr;

    public ConnectorRelationshipTile(String title, PrismContainerValueWrapper<ConnDevRelationInfoType> value) {
        super(null, title);
        setValue(value);
    }

    public String getSubject() {
        return subject;
    }

    public ConnectorRelationshipTile setSubject(String subject) {
        this.subject = subject;
        return this;
    }

    public String getSubjectAttribute() {
        return subjectAttr;
    }

    public ConnectorRelationshipTile setSubjectAttribute(String subjectAttr) {
        this.subjectAttr = subjectAttr;
        return this;
    }

    public String getObject() {
        return object;
    }

    public ConnectorRelationshipTile setObject(String object) {
        this.object = object;
        return this;
    }

    public String getObjectAttribute() {
        return objectAttr;
    }

    public ConnectorRelationshipTile setObjectAttribute(String objectAttr) {
        this.objectAttr = objectAttr;
        return this;
    }

    public ConnectorRelationshipTile description(String description) {
        setDescription(description);
        return this;
    }
}
