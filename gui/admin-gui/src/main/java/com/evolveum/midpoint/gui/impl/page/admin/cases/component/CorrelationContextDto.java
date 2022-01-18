/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.cases.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdMatchCorrelationContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.IdMatchCorrelationPotentialMatchType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

public class CorrelationContextDto implements Serializable {

    private final List<String> matchHeaders = new ArrayList<>();
    private final List<ItemPath> attributes = new ArrayList<>();
    private final List<PotentialMatchDto> potentialMatches = new ArrayList<>();

    private PotentialMatchDto origin;

    public CorrelationContextDto(IdMatchCorrelationContextType idMatchCorrelationContext) {
        List<IdMatchCorrelationPotentialMatchType> idMatches = idMatchCorrelationContext.getPotentialMatch();
        loadPotentialMatchesHeader(idMatches);
    }

    private void loadPotentialMatchesHeader(List<IdMatchCorrelationPotentialMatchType> potentialMatchTypes) {

        int i = 1;
        for (IdMatchCorrelationPotentialMatchType potentialMatch : potentialMatchTypes) {
            if (SchemaConstants.CORRELATION_NONE_URI.equals(potentialMatch.getUri())) {
                origin = new PotentialMatchDto(potentialMatch);
                origin.setOrigin(true);
            } else {
                parsePotentialMatch(potentialMatch, i);
                i++;
            }

        }

        matchHeaders.add(0, "Origin");
        parseAttributeNames(origin);
        potentialMatches.add(0, origin);
    }

    public boolean match(Serializable value, ItemPath path) {
        Serializable originValue = origin.getAttributeValue(path);
        return Objects.equals(value, originValue);
    }

    private void parsePotentialMatch(IdMatchCorrelationPotentialMatchType potentialMatch, int iterator) {
        PotentialMatchDto potentialMatchDto = new PotentialMatchDto(potentialMatch);

        matchHeaders.add("Suggestion " + iterator);
        potentialMatches.add(potentialMatchDto);
    }

    private void parseAttributeNames(PotentialMatchDto origin) {
        if (origin == null) {
            return;
        }
        ShadowAttributesType shadowAttributes = origin.getShadowAttributesType();
        if (shadowAttributes == null) {
            return;
        }
        Collection<Item<?, ?>> items = shadowAttributes.asPrismContainerValue().getItems();
        for (Item item : items) {
            attributes.add(item.getElementName());
        }
    }

    public List<PotentialMatchDto> getPotentialMatches() {
        return potentialMatches;
    }
}
