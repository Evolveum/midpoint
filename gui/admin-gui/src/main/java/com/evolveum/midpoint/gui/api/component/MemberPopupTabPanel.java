/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.AbstractRoleSearchItemWrapper;
import com.evolveum.midpoint.web.component.input.RelationDropDownChoice;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.markup.html.panel.Fragment;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by honchar
 */
public abstract class MemberPopupTabPanel<O extends ObjectType> extends AbstractPopupTabPanel<O> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(MemberPopupTabPanel.class);

    private static final String ID_RELATION = "relation";

    private PageBase pageBase;

    private final Search<O> search;
    private final List<ObjectReferenceType> archetypeReferenceList;

    public MemberPopupTabPanel(String id, Search<O> search) {
        this(id, search, new ArrayList<>());
    }

    public MemberPopupTabPanel(String id, Search<O> search,
            List<ObjectReferenceType> archetypeReferenceList) {
        super(id);
        this.search = search;
        this.archetypeReferenceList = archetypeReferenceList;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        pageBase = getPageBase();
    }

    private List<QName> getSupportedRelations() {
        AbstractRoleSearchItemWrapper memberSearchItem = getMemberSearchItem();
        return memberSearchItem != null ? memberSearchItem.getSupportedRelations() : new ArrayList<>();
//        return search.getSupportedRelations();
    }

    private AbstractRoleSearchItemWrapper getMemberSearchItem() {
        return search.findMemberSearchItem();
    }

    @Override
    protected void initParametersPanel(Fragment parametersPanel) {
        RelationDropDownChoice relation = new RelationDropDownChoice(ID_RELATION, getDefaultRelation(),
                getSupportedRelations(), false);
        relation.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return CollectionUtils.isNotEmpty(getSupportedRelations());
            }

            @Override
            public boolean isEnabled() {
                return CollectionUtils.isNotEmpty(getSupportedRelations())
                        && getSupportedRelations().size() > 1;
            }
        });
        parametersPanel.add(relation);
    }

    private QName getDefaultRelation() {
        QName relation = getRelationValueFromSearch();
        if (QNameUtil.match(relation, PrismConstants.Q_ANY)) {
            QName defRelation = WebComponentUtil.getDefaultRelation();
            if (getSupportedRelations().contains(defRelation)) {
                relation = defRelation;
            } else {
                relation = getSupportedRelations().iterator().next();
            }
        }
        return relation;
    }

    private QName getRelationValueFromSearch() {
        AbstractRoleSearchItemWrapper memberSearchitem = getMemberSearchItem();
        return memberSearchitem != null ? memberSearchitem.getRelationValue() : null;
    }

    protected ObjectDelta prepareDelta() {
        ObjectDelta delta = null;
        try {
            Class classType = WebComponentUtil.qnameToClass(pageBase.getPrismContext(), getObjectType().getTypeQName());
            delta = pageBase.getPrismContext().deltaFactory()
                    .object().createEmptyModifyDelta(classType, "fakeOid");
            AssignmentType newAssignment = new AssignmentType();
            ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(getAbstractRoleTypeObject(), getRelationValue());
            newAssignment.setTargetRef(ref);

            pageBase.getPrismContext().adopt(newAssignment);
            delta.addModificationAddContainer(FocusType.F_ASSIGNMENT, newAssignment);

        } catch (SchemaException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Failed to prepare delta for adding a member operation ", e);
        }

        return delta;
    }

    protected abstract AbstractRoleType getAbstractRoleTypeObject();

    @Override
    protected List<ObjectReferenceType> getArchetypeRefList() {
        return archetypeReferenceList;
    }

    public @NotNull QName getRelationValue() {
        return getRelationDropDown().getRelationValue();
    }

    private RelationDropDownChoice getRelationDropDown() {
        return (RelationDropDownChoice) get(ID_PARAMETERS_PANEL).get(ID_RELATION);
    }
}
