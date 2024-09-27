/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.ObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.tile.*;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ReviewersTileTablePanel extends TileTablePanel<Tile<SelectableBean<UserType>>, SelectableBean<UserType>> implements Popupable {

    private final List<String> reviewerOidList;
    private final Map<ObjectReferenceType, ReviewerStatisticDto> reviewerStatisticDtoMap;

    public ReviewersTileTablePanel(String id,
            Map<ObjectReferenceType, ReviewerStatisticDto> reviewerStatisticDtoMap) {
        super(id, Model.of(ViewToggle.TILE), null);
        this.reviewerStatisticDtoMap = reviewerStatisticDtoMap;
        reviewerOidList = reviewerStatisticDtoMap.keySet().stream().map(ObjectReferenceType::getOid).toList();
    }

    @Override
    protected IModel<Search> createSearchModel() {
        return new LoadableDetachableModel<>() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected Search load() {
                SearchBuilder<UserType> searchBuilder = new SearchBuilder<>(UserType.class)
                        .modelServiceLocator(getPageBase());
                return searchBuilder.build();
            }
        };
    }

    @Override
    protected Component createTile(String id, IModel<Tile<SelectableBean<UserType>>> model) {
        UserType user = model.getObject().getValue().getValue();
        ReviewerTilePanel tilePanel = new ReviewerTilePanel(id, model, getReviewerStatisticDto(user.getOid()));
        tilePanel.add(AttributeAppender.append("class", "d-flex flex-column ml-3 mt-3"));
        tilePanel.add(AttributeAppender.append("style", "height: 270px; width: 220px;"));
        tilePanel.setHorizontal(false);
        return tilePanel;
    }

    @Override
    public ObjectDataProvider createProvider() {
        ObjectDataProvider provider = new ObjectDataProvider(this, getSearchModel()) {

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                if (reviewerOidList == null || reviewerOidList.isEmpty()) {
                    return null;
                }
                return getPageBase().getPrismContext().queryFor(UserType.class)
                        .id(reviewerOidList.toArray(new String[0]))
                        .build();
            }
        };
        Collection<SelectorOptions<GetOperationOptions>> options = getPageBase().getOperationOptionsBuilder()
                .item(FocusType.F_JPEG_PHOTO).retrieve()
                .build();
        provider.setOptions(options);
        return provider;
    }

    private ReviewerStatisticDto getReviewerStatisticDto(String reviewerOid) {
        if (reviewerStatisticDtoMap == null) {
            return null;
        }
        return reviewerStatisticDtoMap
                .keySet()
                .stream()
                .filter(ref -> reviewerOid.equals(ref.getOid()))
                .map(reviewerStatisticDtoMap::get)
                .findFirst()
                .orElse(null);
    }

    @Override
    protected Tile<SelectableBean<UserType>> createTileObject(SelectableBean<UserType> object) {
        UserType user = object.getValue();
        String title = WebComponentUtil.getDisplayNameOrName(user.asPrismObject());

        Tile<SelectableBean<UserType>> tile = new Tile<>(null, title);
        tile.setDescription(user.getDescription());
        tile.setValue(object);

        return tile;
    }

    @Override
    public int getWidth() {
        return 1400;
    }

    @Override
    public int getHeight() {
        return 800;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("CampaignStatisticsPanel.reviewersPanel.title");
    }

    @Override
    public Component getContent() {
        return this;
    }
}
