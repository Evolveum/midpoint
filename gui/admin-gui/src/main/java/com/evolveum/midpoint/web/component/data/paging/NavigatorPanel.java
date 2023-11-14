/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data.paging;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.markup.html.list.LoopItem;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.AbstractRepeater;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * @author lazyman
 */
public class NavigatorPanel extends Panel {

    private static final int PAGING_SIZE = 5;

    private static final String ID_PAGINATION = "pagination";
    private static final String ID_PREVIOUS = "previous";
    private static final String ID_PREVIOUS_LINK = "previousLink";
    private static final String ID_FIRST = "first";
    private static final String ID_FIRST_LINK = "firstLink";
    private static final String ID_LAST = "last";
    private static final String ID_LAST_LINK = "lastLink";
    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_PAGE_LINK = "pageLink";
    private static final String ID_NEXT = "next";
    private static final String ID_NEXT_LINK = "nextLink";

    private final IPageable pageable;
    private final IModel<Boolean> showPageListingModel;

    public NavigatorPanel(String id, IPageable pageable, final boolean showPageListing) {
        this(id, pageable, () -> showPageListing);
    }

    public NavigatorPanel(String id, IPageable pageable, IModel<Boolean> showPageListingModel) {
        super(id);
        this.pageable = pageable;
        this.showPageListingModel = showPageListingModel;

        setOutputMarkupId(true);
        add(new VisibleBehaviour(() -> NavigatorPanel.this.pageable.getPageCount() > 0));

        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer pagination = new WebMarkupContainer(ID_PAGINATION);
        pagination.add(AttributeAppender.append("class", () -> getPaginationCssClass()));
        add(pagination);

        initFirst(pagination);
        initPrevious(pagination);
        initNavigation(pagination);
        initNext(pagination);
        initLast(pagination);
    }

    protected String getPaginationCssClass() {
        return "pagination-sm";
    }

    private void initPrevious(WebMarkupContainer pagination) {
        WebMarkupContainer previous = new WebMarkupContainer(ID_PREVIOUS);
        previous.add(AttributeAppender.append("class", (IModel<String>) () -> isPreviousEnabled() ? "" : "disabled"));
        pagination.add(previous);
        AjaxLink<Void> previousLink = new AjaxLink<>(ID_PREVIOUS_LINK) {

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                attributes.setChannel(new AjaxChannel("blocking", AjaxChannel.Type.ACTIVE));
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                previousPerformed(target);
            }
        };
        previousLink.add(new EnableBehaviour(() -> isPreviousEnabled()));
        previous.add(previousLink);
    }

    private void initFirst(WebMarkupContainer pagination) {
        WebMarkupContainer first = new WebMarkupContainer(ID_FIRST);
        first.add(AttributeAppender.append("class", (IModel<String>) () -> isFirstEnabled() ? "" : "disabled"));
        pagination.add(first);
        AjaxLink<Void> firstLink = new AjaxLink<>(ID_FIRST_LINK) {

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                attributes.setChannel(new AjaxChannel("blocking", AjaxChannel.Type.ACTIVE));
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                firstPerformed(target);
            }
        };
        firstLink.add(new EnableBehaviour(() -> BooleanUtils.isTrue(showPageListingModel.getObject()) && isFirstEnabled()));
        first.add(firstLink);
    }

    private void initNavigation(WebMarkupContainer pagination) {
        IModel<Integer> model = () -> {
            int count = (int) pageable.getPageCount();
            if (count < PAGING_SIZE) {
                return count;
            }

            return PAGING_SIZE;
        };

        Loop navigation = new Loop(ID_NAVIGATION, model) {

            @Override
            protected void populateItem(final LoopItem item) {
                final NavigatorPageLink pageLink = new NavigatorPageLink(ID_PAGE_LINK,
                        computePageNumber(item.getIndex())) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        pageLinkPerformed(target, getPageNumber());
                    }
                };
                item.add(pageLink);

                item.add(AttributeAppender.append("class", (IModel<String>) () -> pageable.getCurrentPage() == pageLink.getPageNumber() ? "active" : ""));
            }
        };
        navigation.add(new VisibleBehaviour(() -> BooleanUtils.isTrue(showPageListingModel.getObject())));
        pagination.add(navigation);
    }

    private long computePageNumber(int loopIndex) {
        long current = pageable.getCurrentPage();
        long count = pageable.getPageCount();

        final long half = PAGING_SIZE / 2;

        long result;
        if (current - half <= 0) {
            result = loopIndex;
        } else if (current + half + 1 >= count) {
            result = count - PAGING_SIZE + loopIndex;
        } else {
            result = current - half + loopIndex;
        }

        //TODO - this is just quick dirty fix for MID-1808. Fix algorithm later
        if (count == 4 && current == 3) {
            result++;
        }

        return result;
    }

    private void initNext(WebMarkupContainer pagination) {
        WebMarkupContainer next = new WebMarkupContainer(ID_NEXT);
        next.add(AttributeAppender.append("class", (IModel<String>) () -> isNextEnabled() ? "" : "disabled"));
        pagination.add(next);

        AjaxLink<Void> nextLink = new AjaxLink<>(ID_NEXT_LINK) {

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                attributes.setChannel(new AjaxChannel("blocking", AjaxChannel.Type.ACTIVE));
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                nextPerformed(target);
            }
        };
        nextLink.add(new EnableBehaviour(() -> isNextEnabled()));
        next.add(nextLink);
    }

    private void initLast(WebMarkupContainer pagination) {
        WebMarkupContainer last = new WebMarkupContainer(ID_LAST);
        last.add(AttributeAppender.append("class", (IModel<String>) () -> isLastEnabled() ? "" : "disabled"));
        pagination.add(last);

        AjaxLink<Void> lastLink = new AjaxLink<>(ID_LAST_LINK) {

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                attributes.setChannel(new AjaxChannel("blocking", AjaxChannel.Type.ACTIVE));
            }

            @Override
            public void onClick(AjaxRequestTarget target) {
                lastPerformed(target);
            }
        };
        lastLink.add(new EnableBehaviour(() -> !isCountingDisabled() && BooleanUtils.isTrue(showPageListingModel.getObject()) && isLastEnabled()));
        last.add(lastLink);
    }

    private boolean isPreviousEnabled() {
        return pageable.getCurrentPage() > 0;
    }

    private boolean isNextEnabled() {
        return pageable.getCurrentPage() + 1 < pageable.getPageCount();
    }

    private boolean isFirstEnabled() {
        return pageable.getCurrentPage() > 0;
    }

    private boolean isLastEnabled() {
        return pageable.getCurrentPage() + 1 < pageable.getPageCount();
    }

    private void previousPerformed(AjaxRequestTarget target) {
        changeCurrentPage(target, pageable.getCurrentPage() - 1);
    }

    private void firstPerformed(AjaxRequestTarget target) {
        changeCurrentPage(target, 0);
    }

    private void lastPerformed(AjaxRequestTarget target) {
        changeCurrentPage(target, pageable.getPageCount() - 1);
    }

    private void nextPerformed(AjaxRequestTarget target) {
        changeCurrentPage(target, pageable.getCurrentPage() + 1);
    }

    private void changeCurrentPage(AjaxRequestTarget target, long page) {
        pageable.setCurrentPage(page);

        if (isComponent()) {
            Component container = ((Component) pageable);
            while (container instanceof AbstractRepeater) {
                container = container.getParent();
            }
            target.add(container);
        }
        target.add(this);

        onPageChanged(target, page);
    }

    protected boolean isComponent() {
        return true;
    }

    private void pageLinkPerformed(AjaxRequestTarget target, long page) {
        changeCurrentPage(target, page);
    }

    protected void onPageChanged(AjaxRequestTarget target, long page) {
    }

    protected boolean isCountingDisabled() {
        return false;
    }
}
