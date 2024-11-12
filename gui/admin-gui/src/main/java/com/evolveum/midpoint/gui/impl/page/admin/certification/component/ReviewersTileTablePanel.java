/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.tile.*;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

/**
 * todo this class contains a lot of duplicate code from ReviewerStatisticsPanel
 * should be cleaned up
 */
public class ReviewersTileTablePanel extends TileTablePanel<Tile<UserType>, UserType> implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private final Map<ObjectReferenceType, ReviewerStatisticDto> reviewerStatisticDtoMap;
    private final boolean isPercentageSorting;
    Search search;

    public ReviewersTileTablePanel(String id, Map<ObjectReferenceType, ReviewerStatisticDto> reviewerStatisticDtoMap,
            boolean isPercentageSorting) {
        super(id, Model.of(ViewToggle.TILE), null);
        this.reviewerStatisticDtoMap = reviewerStatisticDtoMap;
        this.isPercentageSorting = isPercentageSorting;
    }

    @Override
    protected IModel<Search> createSearchModel() {
        return  () -> {
            if (search != null) {
                return search;
            }
            SearchBuilder searchBuilder = new SearchBuilder<>(UserType.class)
                    .modelServiceLocator(getPageBase());
            search = searchBuilder.build();
            return search;
        };
    }

    @Override
    protected Component createTile(String id, IModel<Tile<UserType>> model) {
        UserType user = model.getObject().getValue();
        ReviewerTilePanel tilePanel = new ReviewerTilePanel(id, model, getReviewerStatisticDto(user.getOid()));
        tilePanel.add(AttributeAppender.append("class", "d-flex flex-column ml-3 mt-3"));
        tilePanel.add(AttributeAppender.append("style", "height: 270px; width: 220px;"));
        tilePanel.setHorizontal(false);
        return tilePanel;
    }

    @Override
    public ListDataProvider createProvider() {
        ListDataProvider provider = new ListDataProvider(this, getReviewerListModel());
        return provider;

//        ObjectDataProvider provider = new ObjectDataProvider(this, getSearchModel()) {
//
//            @Override
//            protected ObjectQuery getCustomizeContentQuery() {
//                if (reviewerOidList == null || reviewerOidList.isEmpty()) {
//                    return null;
//                }
//                return getPageBase().getPrismContext().queryFor(UserType.class)
//                        .id(reviewerOidList.toArray(new String[0]))
//                        .build();
//            }
//        };
//        Collection<SelectorOptions<GetOperationOptions>> options = getPageBase().getOperationOptionsBuilder()
//                .item(FocusType.F_JPEG_PHOTO).retrieve()
//                .build();
//        provider.setOptions(options);
//        return provider;
    }

    private ObjectQuery createReviewersQuery() {
        List<String> reviewerOidList = reviewerStatisticDtoMap.keySet().stream()
                .map(ObjectReferenceType::getOid)
                .toList();
        ObjectQuery query = getPageBase().getPrismContext().queryFor(UserType.class)
                .id(reviewerOidList.toArray(new String[0]))
                .build();
        ObjectQuery searchQuery = search != null ? search.createObjectQuery(getPageBase()) : null;
        if (searchQuery != null && searchQuery.getFilter() != null) {
            query.addFilter(searchQuery.getFilter());
        }
        return query;
    }

    private ListModel<UserType> getReviewerListModel() {
        List<UserType> reviewers = getSortedReviewerList();
        return new ListModel<>(reviewers) {

            @Override
            public void setObject(List<UserType> object) {
                super.setObject(object);
            }

            @Override
            public List<UserType> getObject() {
                return searchThroughList(reviewers);
            }
        };
    }

    private List<UserType> searchThroughList(List<UserType> reviewerList) {
        if (reviewerList == null || reviewerList.isEmpty()) {
            return null;
        }

        ObjectQuery query = createReviewersQuery();
        return reviewerList.stream().filter(user -> {
            try {
                return ObjectQuery.match(user, query.getFilter(), getPageBase().getMatchingRuleRegistry());
            } catch (SchemaException e) {
                throw new TunnelException(e.getMessage());
            }
        }).collect(Collectors.toList());
    }

    private List<UserType> getSortedReviewerList() {
        Collection<SelectorOptions<GetOperationOptions>> options = getPageBase().getOperationOptionsBuilder()
                .item(FocusType.F_JPEG_PHOTO).retrieve()
                .build();
        List<ObjectReferenceType> sortedReviewers = sortReviewers();
        return sortedReviewers.stream()
                .map(ref -> {
                    PrismObject<UserType> userPrism = WebModelServiceUtils.loadObject(ref, options, getPageBase());
                    return userPrism != null ? userPrism.asObjectable() : null;
                })
                .toList();
    }

    private List<ObjectReferenceType> sortReviewers() {
        List<ObjectReferenceType> reviewers = new ArrayList<>(reviewerStatisticDtoMap.keySet());
        reviewers = reviewers.stream().sorted((r1, r2) -> {
            ReviewerStatisticDto rs1 = reviewerStatisticDtoMap.get(r1);
            ReviewerStatisticDto rs2 = reviewerStatisticDtoMap.get(r2);

            if (!isPercentageSorting) {
                return Long.compare(rs2.getNotDecidedItemsCount(), rs1.getNotDecidedItemsCount());
            }

            float r1ItemsPercent = rs1.getOpenNotDecidedItemsPercentage();
            float r2ItemsPercent = rs2.getOpenNotDecidedItemsPercentage();
            return Float.compare(r2ItemsPercent, r1ItemsPercent);
        }).toList();
        return reviewers;
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
    protected Tile<UserType> createTileObject(UserType user) {
        if (user == null) {
            return new Tile<>();
        }
        String title = WebComponentUtil.getDisplayNameOrName(user.asPrismObject());

        Tile<UserType> tile = new Tile<>(null, title);
        tile.setDescription(user.getDescription());
        tile.setValue(user);

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

    @Override
    protected String getTilesContainerAdditionalClass() {
        return "card-footer";
    }
}
