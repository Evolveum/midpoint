/*
 * Copyright (c) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

import java.util.List;
import java.util.Locale;

import org.apache.wicket.model.Model;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AbstractSummaryPanel;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypePolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SummaryPanelSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringLangType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Regression coverage for assignment relation display labels. The archetype branch must use the localized
 * display label value, not {@code display.label.orig}, when composing labels like {@code <archetype> (<relation>)}.
 */
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestDisplayLabelLocalization extends AbstractGuiIntegrationTest {

    private static final Locale TEST_LOCALE = Locale.forLanguageTag("sk");
    private static final String ARCHETYPE_OID = "00000000-0000-0000-0000-00000000a111";

    private Locale originalSessionLocale;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, RepoAddOptions.createOverwrite(), false, initResult);

        super.initSystem(initTask, initResult);
    }

    @BeforeMethod
    public void setSessionLocale() {
        MidPointAuthWebSession session = MidPointAuthWebSession.get();
        originalSessionLocale = session.getLocale();
        session.setLocale(TEST_LOCALE);
    }

    @AfterMethod(alwaysRun = true)
    public void restoreSessionLocale() {
        MidPointAuthWebSession session = MidPointAuthWebSession.get();
        if (session != null && originalSessionLocale != null) {
            session.setLocale(originalSessionLocale);
        }
        originalSessionLocale = null;
    }

    @Test
    public void testAssignmentObjectRelationDisplayLabelLocalization() throws Exception {
        OperationResult result = new OperationResult("testAssignmentObjectRelationDisplayLabelLocalization");
        repoAddObject(createArchetype().asPrismObject(), "test archetype", RepoAddOptions.createOverwrite(), result);

        AssignmentObjectRelation assignmentTargetRelation = new AssignmentObjectRelation();
        assignmentTargetRelation.setArchetypeRefs(List.of(
                new ObjectReferenceType().oid(ARCHETYPE_OID).type(ArchetypeType.COMPLEX_TYPE)));
        assignmentTargetRelation.setRelations(List.of(SchemaConstants.ORG_DEFAULT));

        PageUsers page = tester.startPage(PageUsers.class);
        DisplayType display = GuiDisplayTypeUtil.getAssignmentObjectRelationDisplayType(
                page, assignmentTargetRelation, "abstractRoleMemberPanel.title");

        String translatedLabel = GuiDisplayTypeUtil.getTranslatedLabel(display);
        assertTrue("Expected assignment relation display label to contain translated archetype label, but was: "
                        + translatedLabel,
                translatedLabel.contains("standardKeyCustomValue"));
        assertFalse("Assignment relation display label must not use raw archetype label: " + translatedLabel,
                translatedLabel.contains("MyApp"));
        assertEquals(translatedLabel, new TestSummaryPanel().callTranslateDisplayLabelOrDefault(display, "fallback"));
    }

    @Test
    public void testOrigDisplayLabelFallback() {
        DisplayType display = new DisplayType().label(new PolyStringType("MyApp"));

        assertEquals("MyApp", GuiDisplayTypeUtil.getTranslatedLabel(display));
        assertEquals("MyApp", new TestSummaryPanel().callTranslateDisplayLabelOrDefault(display, "fallback"));
    }

    @Test
    public void testMissingDisplayLabelFallbackToDefault() {
        DisplayType display = new DisplayType();

        assertEquals("", GuiDisplayTypeUtil.getTranslatedLabel(display));
        assertEquals("fallback", new TestSummaryPanel().callTranslateDisplayLabelOrDefault(display, "fallback"));
    }

    @Test
    public void testNullDisplayFallbackToDefault() {
        assertEquals("fallback", new TestSummaryPanel().callTranslateDisplayLabelOrDefault(null, "fallback"));
    }

    /**
     * Creates a polystring whose raw {@code orig} value intentionally differs from the localized value so the
     * regression is visible.
     */
    private static PolyStringType createPolyString(String orig, String key) {
        PolyStringType value = new PolyStringType(orig);
        PolyStringLangType lang = new PolyStringLangType();
        lang.getLang().put(TEST_LOCALE.getLanguage(), "standardKeyCustomValue");
        value.setLang(lang);
        PolyStringTranslationType translation = new PolyStringTranslationType();
        translation.setKey(key);
        value.setTranslation(translation);
        return value;
    }

    private static ArchetypeType createArchetype() {
        DisplayType displayType = new DisplayType().label(createPolyString("MyApp", "standardKey"));
        ArchetypePolicyType policy = new ArchetypePolicyType().display(displayType);

        ArchetypeType archetype = new ArchetypeType();
        archetype.setOid(ARCHETYPE_OID);
        archetype.setName(new PolyStringType("test-archetype"));
        archetype.setArchetypePolicy(policy);
        return archetype;
    }

    private static class TestSummaryPanel extends AbstractSummaryPanel<UserType> {

        private TestSummaryPanel() {
            super("panel", Model.of(new UserType()), new SummaryPanelSpecificationType());
        }

        private String callTranslateDisplayLabelOrDefault(DisplayType display, String defaultValue) {
            return translateDisplayLabelOrDefault(display, defaultValue);
        }

        @Override
        protected String getDefaultIconCssClass() {
            return "";
        }

        @Override
        protected String getIconBoxAdditionalCssClass() {
            return "";
        }

        @Override
        protected String getBoxAdditionalCssClass() {
            return "";
        }
    }
}
