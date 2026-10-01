/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.WicketTester;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.gui.impl.factory.panel.TextPanelFactory;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.prism.impl.DisplayableValueImpl;

/** Tests autocomplete filtering when numeric suggestions arrive as strings. */
public class TextPanelFactoryTest {

    private WicketTester tester;

    @BeforeMethod
    public void initWicket() {
        tester = new WicketTester();
        tester.getSession().setLocale(Locale.US);
    }

    @AfterMethod(alwaysRun = true)
    public void destroyWicket() {
        if (tester != null) {
            tester.destroy();
        }
    }

    @DataProvider
    public Object[][] suggestions() {
        return new Object[][] {
                { Integer.class, List.of("123", "456"), "2", List.of("123") },
                { Integer.class, List.of(123, 456), "2", List.of(123) },
                { Integer.class, List.of("123", 456), "4", List.of(456) },
                { String.class, List.of("Alpha", "alpha", "beta"), "Al", List.of("Alpha") },
                { Integer.class, List.of("123", "456"), "", List.of("123", "456") },
                { Integer.class, List.of("123", "456"), "9", List.of() }
        };
    }

    @Test(dataProvider = "suggestions")
    public void filtersSuggestions(Class<?> type, List<Object> suggestions, String input, List<Object> expected) {
        var panel = createPanel(type, suggestions);
        List<Object> actual = new ArrayList<>();
        panel.getIterator(input).forEachRemaining(actual::add);
        assertEquals(actual, expected);
    }

    @Test
    public void submitsNumericStringAsNumber() {
        var panel = createPanel(Integer.class, List.of("123", "456"));
        tester.startComponentInPage(panel);
        var input = panel.getBaseFormComponent();
        tester.getRequest().setParameter(input.getInputName(), "123");
        input.processInput();
        assertTrue(input.isValid());
        assertEquals(input.getModelObject(), 123);
    }

    @Test
    public void rejectsInvalidNumericInput() {
        var panel = createPanel(Integer.class, List.of("123", "456"));
        tester.startComponentInPage(panel);
        var input = panel.getBaseFormComponent();
        tester.getRequest().setParameter(input.getInputName(), "not-a-number");
        input.processInput();
        assertFalse(input.isValid());
        assertNull(input.getModelObject());
    }

    private AutoCompleteTextPanel<Object> createPanel(Class<?> type, List<Object> suggestions) {

        var model = new ItemRealValueModel<>(Model.of()) {
            private Object value;

            @Override
            public Object getObject() {
                return value;
            }

            @Override
            public void setObject(Object object) {
                value = object;
            }
        };

        var context = new PrismPropertyPanelContext<>(Model.of()) {
            @Override
            public String getPredefinedValuesOid() {
                return null;
            }

            @Override
            public Collection<? extends DisplayableValue<Object>> getAllowedValues() {
                return List.of();
            }

            @Override
            public Collection<? extends DisplayableValue<Object>> getSuggestedValues() {
                return suggestions.stream().map(value -> new DisplayableValueImpl<>(value, String.valueOf(value))).toList();
            }

            @Override
            @SuppressWarnings("unchecked")
            public Class<Object> getTypeClass() {
                return (Class<Object>) type;
            }

            @Override
            public ItemRealValueModel<Object> getRealValueModel() {
                return model;
            }
        };
        context.setComponentId("input");
        return new TestFactory().create(context);
    }

    private static class TestFactory extends TextPanelFactory<Object> {
        @SuppressWarnings("unchecked")
        private AutoCompleteTextPanel<Object> create(PrismPropertyPanelContext<Object> context) {
            return (AutoCompleteTextPanel<Object>) getPanel(context);
        }
    }
}
