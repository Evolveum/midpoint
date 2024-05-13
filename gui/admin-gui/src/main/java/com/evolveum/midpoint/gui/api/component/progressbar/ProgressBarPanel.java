/*
 * Copyright (c) 2016-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.progressbar;

import java.io.Serial;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

/**
 * @author semancik
 */
public class ProgressBarPanel extends BasePanel<List<ProgressBar>> {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_BARS = "bars";
    private static final String ID_BAR = "bar";
    private static final String ID_FOOTER = "footer";
    private static final String ID_SIMPLE_TEXT_FRAGMENT = "simpleTextFragment";
    private static final String ID_TEXT = "text";
    private static final String ID_LEGEND_FRAGMENT = "legendFragment";
    private static final String ID_TOTAL_LABEL = "totalLabel";
    private static final String ID_TOTAL_VALUE = "totalValue";
    private static final String ID_LEGEND_ITEM = "legendItem";
    private static final String ID_LEGEND_ITEM_LABEL = "legendItemLabel";
    private static final String ID_LEGEND_ITEM_COLOR = "legendItemColor";
    private static final String ID_LEGEND_ITEM_VALUE = "legendItemValue";

    public ProgressBarPanel(String id, IModel<List<ProgressBar>> model) {
        super(id, model);

        initLayout();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        tag.setName("div");
        super.onComponentTag(tag);

        checkComponentTag(tag, "div");
    }

    private void initLayout() {
        ListView<ProgressBar> bars = new ListView<>(ID_BARS, getModelObject()) {
            @Override
            protected void populateItem(ListItem<ProgressBar> item) {
                item.add(createBar(ID_BAR, item.getModel()));
            }
        };
        add(bars);

        Component legend = createLegendPanel(ID_FOOTER);
        legend.setOutputMarkupId(true);
        add(legend);
    }

    private Component createBar(String id, IModel<ProgressBar> model) {
        WebComponent bar = new WebComponent(id);
        bar.add(AttributeAppender.append("class", () -> model.getObject().getState().getCssClass()));
        bar.add(AttributeAppender.append("style", () -> "width: " + getBarWithValue(model.getObject())));

        return bar;
    }

    private String getBarWithValue(ProgressBar progressBar) {
        if (isPercentageBar()) {
            return progressBar.getValue() + "%";
        }
        Double totalCount = countTotalValue();
        if (totalCount == 0) {
            return "0%";
        }
        return progressBar.getValue() / countTotalValue() * 100 + "%";
    }

    protected boolean isPercentageBar() {
        return true;
    }

    private Component createLegendPanel(String id) {
        if (isSimpleView()) {
            return createSimpleLegendPanel(id);
        } else {
            return createFullLegendPanel(id);
        }
    }

    private Component createSimpleLegendPanel(String id) {
        Fragment simpleTextFragment = new Fragment(id, ID_SIMPLE_TEXT_FRAGMENT, this);

        IModel<String> textModel = createSimpleTextModel(getModel());
        Label text = new Label(ID_TEXT, textModel);
        text.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(textModel.getObject())));

        simpleTextFragment.add(text);
        return simpleTextFragment;
    }

    protected IModel<String> createSimpleTextModel(IModel<List<ProgressBar>> model) {
        return new LoadableDetachableModel<>() {

            @Override
            protected String load() {
                Object[] texts = model.getObject().stream()
                        .filter(bar -> bar.getText() != null)
                        .map(bar -> LocalizationUtil.translateMessage(bar.getText()))
                        .filter(StringUtils::isNotEmpty)
                        .toArray();

                return StringUtils.joinWith(" / ", texts);
            }
        };
    }

    private Component createFullLegendPanel(String id) {
        Fragment legendFragment = new Fragment(id, ID_LEGEND_FRAGMENT, this);

        Label totalLabel = new Label(ID_TOTAL_LABEL, createStringResource("PageCertCampaign.statistics.total"));
        totalLabel.setOutputMarkupId(true);
        legendFragment.add(totalLabel);

        Label totalValue = new Label(ID_TOTAL_VALUE, getTotalValueModel());
        totalValue.setOutputMarkupId(true);
        legendFragment.add(totalValue);

        ListView<ProgressBar> legendItems = getLegendItemsPanel();
        legendFragment.add(legendItems);
        return legendFragment;
    }

    private ListView<ProgressBar> getLegendItemsPanel() {
        ListView<ProgressBar> legendItems = new ListView<>(ID_LEGEND_ITEM, getModel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<ProgressBar> item) {
                item.add(new VisibleBehaviour(() -> item.getModelObject().getText() != null));

                Label legendLabel = new Label(ID_LEGEND_ITEM_LABEL,
                        LocalizationUtil.translateMessage(item.getModelObject().getText()));
                item.add(legendLabel);

                WebMarkupContainer icon = new WebMarkupContainer(ID_LEGEND_ITEM_COLOR);

                icon.add(AttributeModifier.append("class", () -> getTextCssClass(item.getModelObject())));
                icon.add(new VisibleBehaviour(() -> item.getModelObject().getState() != null));
                item.add(icon);

                Label legendValue = new Label(ID_LEGEND_ITEM_VALUE, item.getModelObject().getValue());
                item.add(legendValue);
            }

            private String getTextCssClass(ProgressBar progressBar) {
                if (progressBar.getState() == null) {
                    return "";
                }
                String bgCssClass = progressBar.getState().getCssClass();
                return "text" + bgCssClass.substring(2);
            }
        };
        legendItems.setOutputMarkupId(true);
        return legendItems;
    }

    private IModel<String> getTotalValueModel() {
        return () -> countTotalValue()
                .intValue() + "";
    }

    private Double countTotalValue() {
        return getModelObject().stream()
                .map(ProgressBar::getValue)
                .reduce(0d, Double::sum);
    }

    /**
     * simple view means displaying only text under the bar if any defined
     * in case of false, the full legend is displayed
     * @return
     */
    protected boolean isSimpleView() {
        return true;
    }
}
