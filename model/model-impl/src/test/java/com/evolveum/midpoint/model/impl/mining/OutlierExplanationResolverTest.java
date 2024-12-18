package com.evolveum.midpoint.model.impl.mining;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.LocalizationServiceImpl;
import com.evolveum.midpoint.common.LocalizationTestUtil;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutlierExplanationResolver;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutlierExplanationResolver.OutlierExplanationInput;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutlierExplanationResolver.AnomalyExplanationInput;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutlierExplanationResolver.ExplanationAttribute;
import com.evolveum.midpoint.model.impl.mining.algorithm.cluster.action.util.outlier.OutlierExplanationResolver.RoleStats;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.util.LocalizationUtil;
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizableMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.testng.AssertJUnit.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.OutlierDetectionExplanationCategoryType.*;

public class OutlierExplanationResolverTest extends AbstractUnitTest {

    private static final List<ExplanationAttribute> EMPTY_GROUP_BY = List.of();
    private static Long nextId = 1L;
    private LocalizationService localization;
    private ItemDefinition<?> orgUnitDef, locationDef;

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
        localization = LocalizationTestUtil.getLocalizationService();
        ((LocalizationServiceImpl) localization).init();
        var userDefinition = PrismTestUtil.getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        orgUnitDef = userDefinition.findItemDefinition(ItemPath.fromString("organizationalUnit"));
        locationDef = userDefinition.findItemDefinition(ItemPath.fromString("location"));
    }

    @Test
    void shouldNotFailAndProvideExplanation() {
        given();
        var anomalies = List.of(
                makeAnomaly(
                        makeOverallStats(0.2, 20),
                        makeClusterStats(0.02, 1),
                        makeUnusualAttributes(
                                "attr1", null, "value1",
                                "organizationalUnit", orgUnitDef, "value2"
                        )
                ),
                makeAnomaly(
                        makeOverallStats(0.2, 10),
                        makeClusterStats(0.1, 1),
                        makeUnusualAttributes(
                                "attr1", null, "value1",
                                "attr1", null, "value2"
                        )
                )
        );
        var outlier = makeOutlier(anomalies, EMPTY_GROUP_BY);
        var resolver = new OutlierExplanationResolver(outlier);

        when();
        var result = resolver.explain();

        then();
        assertNotNull("should return non null result", result);
        assertNotNull("should explain outlier", result.explanation());
        assertEquals("should explain every anomaly", anomalies.size(), result.anomalies().size());
        assertThat(List.of(1, 3)).isEqualTo(List.of(1, 3));
        for (var anomaly: result.anomalies()) {
            assertNotNull("should explain anomaly", anomaly.explanations().get(0));
        }
    }

    @Test
    void shouldExplainWithOnlyAnomalies() {
        given();
        var anomalies = List.of(
                makeAnomaly(
                        makeOverallStats(0.0333, 20),
                        makeClusterStats(0.02, 1)
                ),
                makeAnomaly(
                        makeOverallStats(0.004555, 20),
                        makeClusterStats(0.02, 2)
                ),
                makeAnomaly(
                        makeOverallStats(0.3, 10),
                        makeClusterStats(0.05444, 5)
                )
        );
        var outlier = makeOutlier(anomalies, EMPTY_GROUP_BY);
        var resolver = new OutlierExplanationResolver(outlier);

        when();
        var result = resolver.explain();

        then();
        var messages = result.anomalies().stream()
            .map(a -> {
                assertEquals("should provide just 1 explanation per anomaly", 1, a.explanations().size());
                var categories = a.explanations().get(0).getCategory();
                assertThatIterable(categories).isEqualTo(List.of(UNUSUAL_ACCESS));
                return a.explanations().get(0).getMessage();
            })
            .toList();
        assertEquals("3 unusual accesses", translate(result.explanation().getMessage()));
        assertThatIterable(result.explanation().getCategory()).isEqualTo(List.of(UNUSUAL_ACCESS));
        assertEquals("Access is unique within the peer group and granted 3% overall", translate(messages.get(0)));
        assertEquals("Access is granted only to 2 users of the peer group and granted 0.5% overall", translate(messages.get(1)));
        assertEquals("Access is granted only to 5% users of the peer group and granted 30% overall", translate(messages.get(2)));
    }

    @Test
    void shouldExplainUnusualAttributesOutlier() {
        given();
        var anomalies = List.of(
                makeAnomaly(
                        makeOverallStats(0.03, 20),
                        makeClusterStats(0.02, 1),
                        makeUnusualAttributes(
                                // multivalued
                                "attr1", null, "value1", "attr1", null, "value12",
                                "attr2", null, "value2"
                        )
                ),
                makeAnomaly(
                        makeOverallStats(0.03, 20),
                        makeClusterStats(0.02, 1),
                        makeUnusualAttributes(
                                "attr2", null, "value2", // repeating
                                "attr3", null, "value3"
                        )
                )
        );
        var outlier = makeOutlier(anomalies, EMPTY_GROUP_BY);
        var resolver = new OutlierExplanationResolver(outlier);

        when();
        var result = resolver.explain();

        then();
        assertEquals("2 unusual accesses, 3 irregular attributes", translate(result.explanation().getMessage()));
    }

    @Test
    void shouldExplainUnusualAttributesAnomalies() {
        given();
        var anomalies = List.of(
                makeAnomaly(
                        makeOverallStats(0.03, 20),
                        makeClusterStats(0.02, 1),
                        makeUnusualAttributes(
                                "orgUnit", orgUnitDef, "Development"
                        )
                ),
                makeAnomaly(
                        makeOverallStats(0.03, 20),
                        makeClusterStats(0.02, 2),
                        makeUnusualAttributes(
                                "orgUnit", orgUnitDef, "Development",
                                "location", locationDef, "Tokio",
                                "costCenter", null, "research"
                        )
                )
        );
        var outlier = makeOutlier(anomalies, EMPTY_GROUP_BY);
        var resolver = new OutlierExplanationResolver(outlier);

        when();
        var result = resolver.explain();

        then();
        assertEquals("2 unusual accesses, 3 irregular attributes", translate(result.explanation().getMessage()));
        assertThatIterable(result.explanation().getCategory()).isEqualTo(List.of(UNUSUAL_ACCESS, IRREGULAR_ATTRIBUTES));

        var firstAnomaly = result.anomalies().get(0);
        var secondAnomaly = result.anomalies().get(1);

        assertEquals("should provide 2 explanations in anomaly", 2, firstAnomaly.explanations().size());
        assertEquals("Access is unique within the peer group and granted 3% overall", translate(firstAnomaly.explanations().get(0).getMessage()));
        assertEquals("Users with attributes Organizational Unit: Development do not usually get this access", translate(firstAnomaly.explanations().get(1).getMessage()));
        assertThatIterable(firstAnomaly.explanations().get(0).getCategory()).isEqualTo(List.of(UNUSUAL_ACCESS));
        assertThatIterable(firstAnomaly.explanations().get(1).getCategory()).isEqualTo(List.of(IRREGULAR_ATTRIBUTES));

        assertEquals("should provide 2 explanations in anomaly", 2, secondAnomaly.explanations().size());
        assertEquals("Access is granted only to 2 users of the peer group and granted 3% overall", translate(secondAnomaly.explanations().get(0).getMessage()));
        assertEquals("Users with attributes Organizational Unit: Development, location: Tokio, costCenter: research do not usually get this access", translate(secondAnomaly.explanations().get(1).getMessage()));
        assertThatIterable(secondAnomaly.explanations().get(0).getCategory()).isEqualTo(List.of(UNUSUAL_ACCESS));
        assertThatIterable(secondAnomaly.explanations().get(1).getCategory()).isEqualTo(List.of(IRREGULAR_ATTRIBUTES));
    }

    @Test
    void shouldExplainGroupByAttribute() {
        given();
        var anomalies = List.of(makeAnomaly(
                makeOverallStats(0.0333, 20),
                makeClusterStats(0.02, 1),
                makeUnusualAttributes("attr1", null, "value1")
        ));
        var outlier = makeOutlier(anomalies, List.of(new ExplanationAttribute(makeItemPath("location"), null, "Tokio")));
        var resolver = new OutlierExplanationResolver(outlier);

        when();
        var result = resolver.explain();

        then();
        assertEquals("1 unusual accesses, 1 irregular attributes for location: Tokio", translate(result.explanation().getMessage()));
    }

    @Test
    void shouldExplainLocalizedGroupByAttribute() {
        given();
        var anomalies = List.of(makeAnomaly(makeOverallStats(0.0333, 20), makeClusterStats(0.02, 1)));
        var attributeWithDef = new ExplanationAttribute(makeItemPath("organizationalUnit"), orgUnitDef, "Development");
        var outlier = makeOutlier(anomalies, List.of(attributeWithDef));
        var resolver = new OutlierExplanationResolver(outlier);

        when();
        var result = resolver.explain();

        then();
        assertEquals("1 unusual accesses for Organizational Unit: Development", translate(result.explanation().getMessage()));
    }

    @Test
    void shouldExplainMultipleGroupByAttribute() {
        given();
        var anomalies = List.of(makeAnomaly(
                makeOverallStats(0.0333, 20),
                makeClusterStats(0.02, 1),
                makeUnusualAttributes("attr1", null, "value1")
        ));
        var outlier = makeOutlier(anomalies, List.of(
                new ExplanationAttribute(makeItemPath("location"), null, "Tokio"),
                new ExplanationAttribute(makeItemPath("organizationalUnit"), orgUnitDef, "Development")
        ));
        var resolver = new OutlierExplanationResolver(outlier);

        when();
        var result = resolver.explain();

        then();
        assertEquals("1 unusual accesses, 1 irregular attributes for location: Tokio and Organizational Unit: Development", translate(result.explanation().getMessage()));
    }

    @Test
    void shouldMentionPartitionsInExplanationAttribute() {
        given();
        var anomalies = List.of(makeAnomaly(makeOverallStats(0.0333, 20), makeClusterStats(0.02, 1)));
        var attributeWithDef = new ExplanationAttribute(makeItemPath("organizationalUnit"), orgUnitDef, "Development");
        var outlier = makeOutlier(anomalies, List.of(attributeWithDef), 2);
        var resolver = new OutlierExplanationResolver(outlier);

        when();
        var result = resolver.explain();

        then();
        assertEquals("1 unusual accesses for Organizational Unit: Development over 2 partitions", translate(result.explanation().getMessage()));
    }

    private OutlierExplanationInput makeOutlier(List<AnomalyExplanationInput> anomalies, List<ExplanationAttribute> groupByAttributes) {
        return new OutlierExplanationInput(nextId++, anomalies, groupByAttributes, 1);
    }

    private OutlierExplanationInput makeOutlier(List<AnomalyExplanationInput> anomalies, List<ExplanationAttribute> groupByAttributes, int partitionCount) {
        return new OutlierExplanationInput(nextId++, anomalies, groupByAttributes, partitionCount);
    }

    private AnomalyExplanationInput makeAnomaly(RoleStats roleOverallStats, RoleStats roleClusterStats, List<ExplanationAttribute> unusualAttributes) {
        return new AnomalyExplanationInput(nextId++, roleOverallStats, roleClusterStats, unusualAttributes);
    }

    private AnomalyExplanationInput makeAnomaly(RoleStats roleOverallStats, RoleStats roleClusterStats) {
        return makeAnomaly(roleOverallStats, roleClusterStats, List.of());
    }

    private RoleStats makeOverallStats(Double frequency, Integer memberCount) {
        return new RoleStats(frequency, memberCount);
    }

    private RoleStats makeClusterStats(Double frequency, Integer memberCount) {
        return new RoleStats(frequency, memberCount);
    }

    private ItemPathType makeItemPath(String path) {
        return new ItemPathType(ItemPath.fromString(path));
    }

    private List<ExplanationAttribute> makeUnusualAttributes(Object... pathDefinitionValueTripplet) {
        var attributes = new ArrayList<ExplanationAttribute>();
        for (var i = 0; i < pathDefinitionValueTripplet.length; i += 3) {
            var path = (String) pathDefinitionValueTripplet[i];
            var def = (ItemDefinition<?>) pathDefinitionValueTripplet[i + 1];
            var value = (String) pathDefinitionValueTripplet[i + 2];
            attributes.add(new ExplanationAttribute(makeItemPath(path), def, value));
        }
        return attributes;
    }

    private String translate(LocalizableMessageType message) {
        return localization.translate(LocalizationUtil.toLocalizableMessage(message), localization.getDefaultLocale());
    }

}
