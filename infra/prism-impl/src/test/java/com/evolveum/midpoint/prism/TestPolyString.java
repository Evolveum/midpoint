/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism;

import static com.evolveum.midpoint.prism.PrismInternalTestUtil.NS_FOO;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.USER_POLYNAME_QNAME;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.constructInitializedPrismContext;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.getFooSchema;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertTrue;

import javax.xml.namespace.QName;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.impl.polystring.AlphanumericPolyStringNormalizer;
import com.evolveum.midpoint.prism.impl.polystring.Ascii7PolyStringNormalizer;
import com.evolveum.midpoint.prism.impl.polystring.PassThroughPolyStringNormalizer;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringNormalizerConfigurationType;

import java.util.HashMap;

/**
 * @author semancik
 *
 */
public class TestPolyString extends AbstractPrismTest {

    @Test
    public void testSimpleAlphaNormalization() {
        AlphanumericPolyStringNormalizer normalizer = new AlphanumericPolyStringNormalizer();

        testNormalization(normalizer,
                // Gulôčka v jamôčke leží, Perún ju bleskom usmaží. Hrom do toho!
                " Gul\u00F4\u010Dka  v jam\u00F4\u010Dke le\u017E\u00ED, Per\u00FAn ju  bleskom usma\u017E\u00ED. Hrom do toho!  ",
                "gulocka v jamocke lezi perun ju bleskom usmazi hrom do toho");
        testNormalization(normalizer,
                // Пролетарии всех стран, соединяйтесь!
                "\u041F\u0440\u043E\u043B\u0435\u0442\u0430\u0440\u0438\u0438 \u0432\u0441\u0435\u0445 \u0441\u0442\u0440\u0430\u043D, \u0441\u043E\u0435\u0434\u0438\u043D\u044F\u0439\u0442\u0435\u0441\u044C!",
                "");
        testNormalization(normalizer,
                // in Сою́з Сове́тских Социалисти́ческих Респу́блик the tv watches you!!
                "In \u0421\u043E\u044E\u0301\u0437 \u0421\u043E\u0432\u0435\u0301\u0442\u0441\u043A\u0438\u0445 \u0421\u043E\u0446\u0438\u0430\u043B\u0438\u0441\u0442\u0438\u0301\u0447\u0435\u0441\u043A\u0438\u0445 \u0420\u0435\u0441\u043F\u0443\u0301\u0431\u043B\u0438\u043A the TV watches YOU!!",
                "in the tv watches you");
        testNormalization(normalizer,
                "  Ľala  ho  papľuha!    ",
                "lala ho papluha");
    }

    @Test
    public void testAlphaNormalizationNoNfkd() {
        AlphanumericPolyStringNormalizer normalizer = new AlphanumericPolyStringNormalizer();
        PolyStringNormalizerConfigurationType configuration = new PolyStringNormalizerConfigurationType();
        configuration.setNfkd(false);
        normalizer.configure(configuration);

        testNormalization(normalizer,
                // Gulôčka v jamôčke leží, Perún ju bleskom usmaží. Hrom do toho!
                " Gul\u00F4\u010Dka  v jam\u00F4\u010Dke le\u017E\u00ED, Per\u00FAn ju  bleskom usma\u017E\u00ED. Hrom do toho!  ",
                "gulka v jamke le pern ju bleskom usma hrom do toho");
        testNormalization(normalizer,
                // Пролетарии всех стран, соединяйтесь!
                "\u041F\u0440\u043E\u043B\u0435\u0442\u0430\u0440\u0438\u0438 \u0432\u0441\u0435\u0445 \u0441\u0442\u0440\u0430\u043D, \u0441\u043E\u0435\u0434\u0438\u043D\u044F\u0439\u0442\u0435\u0441\u044C!",
                "");
        testNormalization(normalizer,
                // in Сою́з Сове́тских Социалисти́ческих Респу́блик the tv watches you!!
                "In \u0421\u043E\u044E\u0301\u0437 \u0421\u043E\u0432\u0435\u0301\u0442\u0441\u043A\u0438\u0445 \u0421\u043E\u0446\u0438\u0430\u043B\u0438\u0441\u0442\u0438\u0301\u0447\u0435\u0441\u043A\u0438\u0445 \u0420\u0435\u0441\u043F\u0443\u0301\u0431\u043B\u0438\u043A the TV watches YOU!!",
                "in the tv watches you");
        testNormalization(normalizer,
                "  Ľala  ho  papľuha!    ",
                "ala ho papuha");
    }

    @Test
    public void testSimpleAsciiNormalization() {
        Ascii7PolyStringNormalizer normalizer = new Ascii7PolyStringNormalizer();

        testNormalization(normalizer,
                // Gulôčka v jamôčke leží, Perún ju bleskom usmaží. Hrom do toho!
                " Gul\u00F4\u010Dka  v jam\u00F4\u010Dke le\u017E\u00ED, Per\u00FAn ju  bleskom usma\u017E\u00ED. Hrom do toho!  ",
                "gulocka v jamocke lezi, perun ju bleskom usmazi. hrom do toho!");
        testNormalization(normalizer,
                // Пролетарии всех стран, соединяйтесь!
                "\u041F\u0440\u043E\u043B\u0435\u0442\u0430\u0440\u0438\u0438 \u0432\u0441\u0435\u0445 \u0441\u0442\u0440\u0430\u043D, \u0441\u043E\u0435\u0434\u0438\u043D\u044F\u0439\u0442\u0435\u0441\u044C!",
                " , !");
        testNormalization(normalizer,
                // in Сою́з Сове́тских Социалисти́ческих Респу́блик the tv watches you!!
                "In \u0421\u043E\u044E\u0301\u0437 \u0421\u043E\u0432\u0435\u0301\u0442\u0441\u043A\u0438\u0445 \u0421\u043E\u0446\u0438\u0430\u043B\u0438\u0441\u0442\u0438\u0301\u0447\u0435\u0441\u043A\u0438\u0445 \u0420\u0435\u0441\u043F\u0443\u0301\u0431\u043B\u0438\u043A the TV watches YOU!!",
                "in the tv watches you!!");
        testNormalization(normalizer,
                "  Ľala  ho  papľuha!    ",
                "lala ho papluha!");
    }

    @Test
    public void testAsciiNormalizationNoNfkd() {
        Ascii7PolyStringNormalizer normalizer = new Ascii7PolyStringNormalizer();
        PolyStringNormalizerConfigurationType configuration = new PolyStringNormalizerConfigurationType();
        configuration.setNfkd(false);
        normalizer.configure(configuration);

        testNormalization(normalizer,
                // Gulôčka v jamôčke leží, Perún ju bleskom usmaží. Hrom do toho!
                " Gul\u00F4\u010Dka  v jam\u00F4\u010Dke le\u017E\u00ED, Per\u00FAn ju  bleskom usma\u017E\u00ED. Hrom do toho!  ",
                "gulka v jamke le, pern ju bleskom usma. hrom do toho!");
        testNormalization(normalizer,
                // Пролетарии всех стран, соединяйтесь!
                "\u041F\u0440\u043E\u043B\u0435\u0442\u0430\u0440\u0438\u0438 \u0432\u0441\u0435\u0445 \u0441\u0442\u0440\u0430\u043D, \u0441\u043E\u0435\u0434\u0438\u043D\u044F\u0439\u0442\u0435\u0441\u044C!",
                " , !");
        testNormalization(normalizer,
                // in Сою́з Сове́тских Социалисти́ческих Респу́блик the tv watches you!!
                "In \u0421\u043E\u044E\u0301\u0437 \u0421\u043E\u0432\u0435\u0301\u0442\u0441\u043A\u0438\u0445 \u0421\u043E\u0446\u0438\u0430\u043B\u0438\u0441\u0442\u0438\u0301\u0447\u0435\u0441\u043A\u0438\u0445 \u0420\u0435\u0441\u043F\u0443\u0301\u0431\u043B\u0438\u043A the TV watches YOU!!",
                "in the tv watches you!!");
        testNormalization(normalizer,
                "  Ľala  ho  papľuha!    ",
                "ala ho papuha!");
    }

    @Test
    public void testSimplePassThroughNormalization() {
        PassThroughPolyStringNormalizer normalizer = new PassThroughPolyStringNormalizer();

        testNormalization(normalizer,
                // Gulôčka v jamôčke leží, Perún ju bleskom usmaží. Hrom do toho!
                " Gul\u00F4\u010Dka  v jam\u00F4\u010Dke le\u017E\u00ED, Per\u00FAn ju  bleskom usma\u017E\u00ED. Hrom do toho!  ",
                "gulo\u0302c\u030Cka v jamo\u0302c\u030Cke lez\u030Ci\u0301, peru\u0301n ju bleskom usmaz\u030Ci\u0301. hrom do toho!");
                // Characters are decomposed
        testNormalization(normalizer,
                // Пролетарии всех стран, соединяйтесь!
                "\u041F\u0440\u043E\u043B\u0435\u0442\u0430\u0440\u0438\u0438 \u0432\u0441\u0435\u0445 \u0441\u0442\u0440\u0430\u043D, \u0441\u043E\u0435\u0434\u0438\u043D\u044F\u0439\u0442\u0435\u0441\u044C!",
                "\u043F\u0440\u043E\u043B\u0435\u0442\u0430\u0440\u0438\u0438 \u0432\u0441\u0435\u0445 \u0441\u0442\u0440\u0430\u043D, \u0441\u043E\u0435\u0434\u0438\u043D\u044F\u0438\u0306\u0442\u0435\u0441\u044C!");
                // Lowercase П, and й is decomposed
        testNormalization(normalizer,
                // in Сою́з Сове́тских Социалисти́ческих Респу́блик the tv watches you!!
                "In \u0421\u043E\u044E\u0301\u0437 \u0421\u043E\u0432\u0435\u0301\u0442\u0441\u043A\u0438\u0445 \u0421\u043E\u0446\u0438\u0430\u043B\u0438\u0441\u0442\u0438\u0301\u0447\u0435\u0441\u043A\u0438\u0445 \u0420\u0435\u0441\u043F\u0443\u0301\u0431\u043B\u0438\u043A the TV watches YOU!!",
                "in сою́з сове́тских социалисти́ческих респу́блик the tv watches you!!");
        testNormalization(normalizer,
                "  Ľala  ho  papľuha!    ",
                "l\u030Cala ho papl\u030Cuha!");
                // ľ is decomposed
    }

    @Test
    public void testPassThroughNormalizationNoNfkd() {
        PassThroughPolyStringNormalizer normalizer = new PassThroughPolyStringNormalizer();
        PolyStringNormalizerConfigurationType configuration = new PolyStringNormalizerConfigurationType();
        configuration.setNfkd(false);
        normalizer.configure(configuration);

        testNormalization(normalizer,
                // Gulôčka v jamôčke leží, Perún ju bleskom usmaží. Hrom do toho!
                " Gul\u00F4\u010Dka  v jam\u00F4\u010Dke le\u017E\u00ED, Per\u00FAn ju  bleskom usma\u017E\u00ED. Hrom do toho!  ",
                "gul\u00F4\u010Dka v jam\u00F4\u010Dke le\u017E\u00ED, per\u00FAn ju bleskom usma\u017E\u00ED. hrom do toho!");
        testNormalization(normalizer,
                // Пролетарии всех стран, соединяйтесь!
                "\u041F\u0440\u043E\u043B\u0435\u0442\u0430\u0440\u0438\u0438 \u0432\u0441\u0435\u0445 \u0441\u0442\u0440\u0430\u043D, \u0441\u043E\u0435\u0434\u0438\u043D\u044F\u0439\u0442\u0435\u0441\u044C!",
                "\u043F\u0440\u043E\u043B\u0435\u0442\u0430\u0440\u0438\u0438 \u0432\u0441\u0435\u0445 \u0441\u0442\u0440\u0430\u043D, \u0441\u043E\u0435\u0434\u0438\u043D\u044F\u0439\u0442\u0435\u0441\u044C!");
        testNormalization(normalizer,
                // in Сою́з Сове́тских Социалисти́ческих Респу́блик the tv watches you!!
                "In \u0421\u043E\u044E\u0301\u0437 \u0421\u043E\u0432\u0435\u0301\u0442\u0441\u043A\u0438\u0445 \u0421\u043E\u0446\u0438\u0430\u043B\u0438\u0441\u0442\u0438\u0301\u0447\u0435\u0441\u043A\u0438\u0445 \u0420\u0435\u0441\u043F\u0443\u0301\u0431\u043B\u0438\u043A the TV watches YOU!!",
                "in сою́з сове́тских социалисти́ческих респу́блик the tv watches you!!");
        testNormalization(normalizer,
                "  Ľala  ho  papľuha!    ",
                "ľala ho papľuha!");
    }

    @Test
    public void testPassThroughNormalizationAllOff() {
        PassThroughPolyStringNormalizer normalizer = new PassThroughPolyStringNormalizer();
        PolyStringNormalizerConfigurationType configuration = new PolyStringNormalizerConfigurationType();
        configuration.setTrim(false);
        configuration.setNfkd(false);
        configuration.setTrimWhitespace(false);
        configuration.setLowercase(false);
        normalizer.configure(configuration);

        testNormalization(normalizer,
                // Gulôčka v jamôčke leží, Perún ju bleskom usmaží. Hrom do toho!
                " Gul\u00F4\u010Dka  v jam\u00F4\u010Dke le\u017E\u00ED, Per\u00FAn ju  bleskom usma\u017E\u00ED. Hrom do toho!  ",
                " Gul\u00F4\u010Dka  v jam\u00F4\u010Dke le\u017E\u00ED, Per\u00FAn ju  bleskom usma\u017E\u00ED. Hrom do toho!  ");
        testNormalization(normalizer,
                // Пролетарии всех стран, соединяйтесь!
                "\u041F\u0440\u043E\u043B\u0435\u0442\u0430\u0440\u0438\u0438 \u0432\u0441\u0435\u0445 \u0441\u0442\u0440\u0430\u043D, \u0441\u043E\u0435\u0434\u0438\u043D\u044F\u0439\u0442\u0435\u0441\u044C!",
                "\u041F\u0440\u043E\u043B\u0435\u0442\u0430\u0440\u0438\u0438 \u0432\u0441\u0435\u0445 \u0441\u0442\u0440\u0430\u043D, \u0441\u043E\u0435\u0434\u0438\u043D\u044F\u0439\u0442\u0435\u0441\u044C!");
        testNormalization(normalizer,
                // in Сою́з Сове́тских Социалисти́ческих Респу́блик the tv watches you!!
                "In \u0421\u043E\u044E\u0301\u0437 \u0421\u043E\u0432\u0435\u0301\u0442\u0441\u043A\u0438\u0445 \u0421\u043E\u0446\u0438\u0430\u043B\u0438\u0441\u0442\u0438\u0301\u0447\u0435\u0441\u043A\u0438\u0445 \u0420\u0435\u0441\u043F\u0443\u0301\u0431\u043B\u0438\u043A the TV watches YOU!!",
                "In \u0421\u043E\u044E\u0301\u0437 \u0421\u043E\u0432\u0435\u0301\u0442\u0441\u043A\u0438\u0445 \u0421\u043E\u0446\u0438\u0430\u043B\u0438\u0441\u0442\u0438\u0301\u0447\u0435\u0441\u043A\u0438\u0445 \u0420\u0435\u0441\u043F\u0443\u0301\u0431\u043B\u0438\u043A the TV watches YOU!!");
        testNormalization(normalizer,
                "  Ľala  ho  papľuha!    ",
                "  Ľala  ho  papľuha!    ");
    }

    private void testNormalization(PolyStringNormalizer normalizer, String orig, String expectedNorm) {
        PolyString polyString = new PolyString(orig);
        polyString.recompute(normalizer);
        String norm = polyString.getNorm();
        displayValue("X: "+orig+" -> "+norm, unicodeEscape(orig)+"\n"+unicodeEscape(norm));
        assertEquals("orig have changed", orig, polyString.getOrig());
        assertEquals("wrong norm", expectedNorm, polyString.getNorm());
        assertEquals("wrong toString", orig, polyString.toString());
    }

    @Test
    public void testRecompute() throws Exception {
        // GIVEN
        PrismContext ctx = constructInitializedPrismContext();
        PrismObjectDefinition<UserType> userDefinition = getFooSchema(ctx).findObjectDefinitionByElementName(new QName(NS_FOO,"user"));
        PrismObject<UserType> user = userDefinition.instantiate();

        String orig = "Ľala ho papľuha";
        PolyString polyName = new PolyString(orig);

        PrismProperty<Object> polyNameProperty = user.findOrCreateProperty(USER_POLYNAME_QNAME);

        // WHEN
        when();
        polyNameProperty.setRealValue(polyName);

        // THEN
        then();
        assertEquals("Changed orig", orig, polyName.getOrig());
        assertEquals("Wrong norm", "lala ho papluha", polyName.getNorm());

    }

    @Test
    public void testCompareTo() {
        // GIVEN
        String orig = "Ľala ho papľuha";
        PolyString polyName = new PolyString(orig);

        // WHEN, THEN
        assertEquals(polyName.compareTo("Ľala ho papľuha"), 0);
        assertEquals(polyName.compareTo(new PolyString("Ľala ho papľuha")), 0);
        assertTrue(polyName.compareTo("something different") != 0);
        assertTrue(polyName.compareTo(new PolyString("something different")) != 0);
        assertTrue(polyName.compareTo("") != 0);
        assertTrue(polyName.compareTo(null) != 0);

    }

    @Test
    public void testConversion() {
        PolyString polyString = new PolyString("Ľala ho papľuha");
        executeConversionTest(polyString);
    }

    @Test
    public void testConversionWithEmptyLang() {
        PolyString polyString = new PolyString("Ľala ho papľuha");
        polyString.setLang(new HashMap<>());
        executeConversionTest(polyString);
    }

    @Test
    public void testConversionWithOneLang() {
        PolyString polyString = new PolyString("Ľala ho papľuha");
        polyString.setLang(new HashMap<>());
        polyString.getLang().put("sk", "Lalala");
        executeConversionTest(polyString);
    }

    private void executeConversionTest(PolyString polyString) {
        // WHEN
        when();
        PolyStringType polyStringType = PolyString.toPolyStringType(polyString);
        PolyString polyString2 = PolyString.toPolyString(polyStringType);
        PolyStringType polyStringType2 = PolyString.toPolyStringType(polyString2);

        System.out.println("polyString = " + polyString);
        System.out.println("polyString2 = " + polyString2);
        System.out.println("polyStringType = " + polyStringType);
        System.out.println("polyStringType2 = " + polyStringType2);

        // THEN
        then();
        assertEquals("PolyString differs", polyString, polyString2);
        assertEquals("PolyStringType differs", polyStringType, polyStringType2);
        assertEquals("PolyString.hashCode differs", polyString.hashCode(), polyString2.hashCode());
        assertEquals("PolyStringType.hashCode differs", polyStringType.hashCode(), polyStringType2.hashCode());
    }

    @Test
    public void testSerialization() throws Exception {
        // GIVEN
        PrismContext ctx = constructInitializedPrismContext();
        PrismObjectDefinition<UserType> userDefinition = getFooSchema(ctx).findObjectDefinitionByElementName(new QName(NS_FOO,"user"));
        PrismObject<UserType> user = userDefinition.instantiate();

        PolyString polyString = new PolyString("Ľala ho papľuha");
        executeSerializationTest(ctx, user, polyString);
    }

    @Test
    public void testSerializationWithEmptyLang() throws Exception {
        // GIVEN
        PrismContext ctx = constructInitializedPrismContext();
        PrismObjectDefinition<UserType> userDefinition = getFooSchema(ctx).findObjectDefinitionByElementName(new QName(NS_FOO,"user"));
        PrismObject<UserType> user = userDefinition.instantiate();

        PolyString polyString = new PolyString("Ľala ho papľuha");
        polyString.setLang(new HashMap<>());
        executeSerializationTest(ctx, user, polyString);
    }

    @Test
    public void testSerializationWithOneLang() throws Exception {
        // GIVEN
        PrismContext ctx = constructInitializedPrismContext();
        PrismObjectDefinition<UserType> userDefinition = getFooSchema(ctx).findObjectDefinitionByElementName(new QName(NS_FOO,"user"));
        PrismObject<UserType> user = userDefinition.instantiate();

        PolyString polyString = new PolyString("Ľala ho papľuha");
        polyString.setLang(new HashMap<>());
        polyString.getLang().put("sk", "Lalala");
        executeSerializationTest(ctx, user, polyString);
    }

    private void executeSerializationTest(PrismContext ctx, PrismObject<UserType> user, PolyString polyString)
            throws com.evolveum.midpoint.util.exception.SchemaException {
        PrismProperty<Object> polyNameProperty = user.findOrCreateProperty(USER_POLYNAME_QNAME);
        polyNameProperty.setRealValue(polyString);

        // WHEN
        when();
        String xml = ctx.xmlSerializer().serialize(user);
        System.out.println(xml);
        PrismObject<Objectable> parsed = ctx.parserFor(xml).parse();
        String xml2 = ctx.xmlSerializer().serialize(parsed);
        PrismObject<Objectable> parsed2 = ctx.parserFor(xml2).parse();

        // THEN
        then();
        assertEquals("original and parsed are different", user, parsed);
        assertEquals("parsed and parsed2 are different", parsed, parsed2);
    }

    private String unicodeEscape(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= 128)
                sb.append("\\u").append(String.format("%04X", (int) c));
            else
                sb.append(c);
        }
        return sb.toString();
    }

}
