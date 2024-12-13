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
import com.evolveum.midpoint.tools.testng.AbstractUnitTest;
import com.evolveum.midpoint.util.LocalizableMessage;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.testng.AssertJUnit.*;

public class OutlierExplanationResolverTest extends AbstractUnitTest {

    private static Long nextId = 1L;
    private LocalizationService localization;
    private ItemDefinition<?> orgUnitDef;

    @BeforeSuite
    public void setup() throws SchemaException, SAXException, IOException {
        PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
        localization = LocalizationTestUtil.getLocalizationService();
        ((LocalizationServiceImpl) localization).init();
        var userDefinition = PrismTestUtil.getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        orgUnitDef = userDefinition.findItemDefinition(ItemPath.fromString("organizationalUnit"));
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
        var outlier = makeOutlier(anomalies, null);
        var resolver = new OutlierExplanationResolver(outlier);

        when();
        var result = resolver.explain();

        then();
        assertNotNull("should return non null result", result);
        assertNotNull("should explain outlier", result.explanation());
        assertEquals("should explain every anomaly", anomalies.size(), result.anomalies().size());
        for (var anomaly: result.anomalies()) {
            assertNotNull("should explain anomaly", anomaly.explanation());
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
        var outlier = makeOutlier(anomalies, null);
        var resolver = new OutlierExplanationResolver(outlier);

        when();
        var result = resolver.explain();

        then();
        assertEquals("3 unusual accesses", translate(result.explanation()));
        assertEquals("Access is unique within the peer group and granted 3% overall", translate(result.anomalies().get(0).explanation()));
        assertEquals("Access is granted only to 2 users of the peer group and granted 0.5% overall", translate(result.anomalies().get(1).explanation()));
        assertEquals("Access is granted only to 5% users of the peer group and granted 30% overall", translate(result.anomalies().get(2).explanation()));
    }

    @Test
    void shouldExplainUnusualAttributes() {
        // TODO
    }

    @Test
    void shouldExplainGroupByAttribute() {
        given();
        var anomalies = List.of(makeAnomaly(
                makeOverallStats(0.0333, 20),
                makeClusterStats(0.02, 1),
                makeUnusualAttributes("attr1", null, "value1")
        ));
        var outlier = makeOutlier(anomalies, new ExplanationAttribute(makeItemPath("location"), null, "Tokio"));
        var resolver = new OutlierExplanationResolver(outlier);

        when();
        var result = resolver.explain();

        then();
        assertEquals("1 unusual accesses, 1 irregular attributes for location: Tokio", translate(result.explanation()));
    }

    @Test
    void shouldExplainLocalizedGroupByAttribute() {
        given();
        var anomalies = List.of(makeAnomaly(makeOverallStats(0.0333, 20), makeClusterStats(0.02, 1)));
        var attributeWithDef = new ExplanationAttribute(makeItemPath("organizationalUnit"), orgUnitDef, "Development");
        var outlier = makeOutlier(anomalies, attributeWithDef);
        var resolver = new OutlierExplanationResolver(outlier);

        when();
        var result = resolver.explain();

        then();
        assertEquals("1 unusual accesses for Organizational Unit: Development", translate(result.explanation()));
    }

    private OutlierExplanationInput makeOutlier(List<AnomalyExplanationInput> anomalies, ExplanationAttribute groupByAttribute) {
        return new OutlierExplanationInput(nextId++, anomalies, groupByAttribute);
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

    private String translate(LocalizableMessage message) {
        return localization.translate(message, localization.getDefaultLocale());
    }

}
