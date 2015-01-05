/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.component.data.paging;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.commons.lang.BooleanUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.markup.html.list.LoopItem;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.AbstractRepeater;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public class NavigatorPanel extends Panel {

    private int PAGING_SIZE = 5;

    private static final String ID_PREVIOUS = "previous";
    private static final String ID_PREVIOUS_LINK = "previousLink";
    private static final String ID_FIRST = "first";
    private static final String ID_FIRST_LINK = "firstLink";
    private static final String ID_DOTS = "dots";
    private static final String ID_NAVIGATION = "navigation";
    private static final String ID_PAGE_LINK = "pageLink";
    private static final String ID_NEXT = "next";
    private static final String ID_NEXT_LINK = "nextLink";

    private final IPageable pageable;
    private final IModel<Boolean> showPageListingModel;

    public NavigatorPanel(String id, IPageable pageable, final boolean showPageListing) {
        this(id, pageable, new AbstractReadOnlyModel<Boolean>() {
            @Override
            public Boolean getObject() {
                return showPageListing;
            }
        });
    }

    public NavigatorPanel(String id, IPageable pageable, IModel<Boolean> showPageListingModel) {
        super(id);
        this.pageable = pageable;
        this.showPageListingModel = showPageListingModel;

        setOutputMarkupId(true);
        add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return NavigatorPanel.this.pageable.getPageCount() > 0;
            }
        });

        initLayout();
    }

    private void initLayout() {
        initPrevious();
        initFirst();
        initNavigation();
        initNext();
    }

    private void initPrevious() {
        WebMarkupContainer previous = new WebMarkupContainer(ID_PREVIOUS);
        previous.add(new AttributeModifier("class", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return isPreviousEnabled() ? "" : "disabled";
            }
        }));
        add(previous);
        AjaxLink previousLink = new AjaxLink(ID_PREVIOUS_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                previousPerformed(target);
            }
        };
        previousLink.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isEnabled() {
                return isPreviousEnabled();
            }
        });
        previous.add(previousLink);
    }

    private void initFirst() {
        WebMarkupContainer first = new WebMarkupContainer(ID_FIRST);
        first.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return BooleanUtils.isTrue(showPageListingModel.getObject()) && showFirstAndDots();
            }
        });
        add(first);
        AjaxLink firstLink = new AjaxLink(ID_FIRST_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                firstPerformed(target);
            }
        };
        first.add(firstLink);

        WebMarkupContainer dots = new WebMarkupContainer(ID_DOTS);
        dots.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return BooleanUtils.isTrue(showPageListingModel.getObject()) && showFirstAndDots();
            }
        });
        add(dots);
    }

    private void initNavigation() {
        IModel<Integer> model = new AbstractReadOnlyModel<Integer>() {

            @Override
            public Integer getObject() {
                int count = (int) pageable.getPageCount();
                if (count < PAGING_SIZE) {
                    return count;
                }

                return PAGING_SIZE;
            }
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

                item.add(new AttributeModifier("class", new AbstractReadOnlyModel<String>() {

                    @Override
                    public String getObject() {
                        return pageable.getCurrentPage() == pageLink.getPageNumber() ? "active" : "";
                    }
                }));
            }
        };
        navigation.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return BooleanUtils.isTrue(showPageListingModel.getObject());
            }
        });
        add(navigation);
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
        if(count == 4 && current == 3){
            result++;
        }

        return result;
    }

    private void initNext() {
        WebMarkupContainer next = new WebMarkupContainer(ID_NEXT);
        next.add(new AttributeModifier("class", new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return isNextEnabled() ? "" : "disabled";
            }
        }));
        add(next);

        AjaxLink nextLink = new AjaxLink(ID_NEXT_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                nextPerformed(target);
            }
        };
        nextLink.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isEnabled() {
                return isNextEnabled();
            }
        });
        next.add(nextLink);
    }

    private boolean isPreviousEnabled() {
        return pageable.getCurrentPage() > 0;
    }

    private boolean isNextEnabled() {
        return pageable.getCurrentPage() + 1 < pageable.getPageCount();
    }

    private boolean showFirstAndDots() {
        return pageable.getCurrentPage() >= PAGING_SIZE - 1;
    }

    private void previousPerformed(AjaxRequestTarget target) {
        changeCurrentPage(target, pageable.getCurrentPage() - 1);
    }

    private void firstPerformed(AjaxRequestTarget target) {
        changeCurrentPage(target, 0);
    }

    private void nextPerformed(AjaxRequestTarget target) {
        changeCurrentPage(target, pageable.getCurrentPage() + 1);
    }

    private void changeCurrentPage(AjaxRequestTarget target, long page) {
        pageable.setCurrentPage(page);

        Component container = ((Component) pageable);
        while (container instanceof AbstractRepeater) {
            container = container.getParent();
        }
        target.add(container);
        target.add(this);
    }

    private void pageLinkPerformed(AjaxRequestTarget target, long page) {
        changeCurrentPage(target, page);
    }
}
