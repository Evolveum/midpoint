/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.accesscert;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType.*;

import java.util.*;
import javax.xml.namespace.QName;

import com.querydsl.core.Tuple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.repo.sqale.SqaleRepoContext;
import com.evolveum.midpoint.repo.sqale.SqaleUtils;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUserMapping;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolderMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;

/**
 * Mapping between {@link QAccessCertificationCampaign}
 * and {@link AccessCertificationCampaignType}.
 */
public class QAccessCertificationCampaignMapping
        extends QAssignmentHolderMapping<AccessCertificationCampaignType,
        QAccessCertificationCampaign, MAccessCertificationCampaign> {

    public static final String DEFAULT_ALIAS_NAME = "acc";
    private static QAccessCertificationCampaignMapping instance;

    // Explanation in class Javadoc for SqaleTableMapping
    public static QAccessCertificationCampaignMapping initAccessCertificationCampaignMapping(
            @NotNull SqaleRepoContext repositoryContext) {
        instance = new QAccessCertificationCampaignMapping(repositoryContext);
        return instance;
    }

    // Explanation in class Javadoc for SqaleTableMapping
    public static QAccessCertificationCampaignMapping getAccessCertificationCampaignMapping() {
        return Objects.requireNonNull(instance);
    }

    private QAccessCertificationCampaignMapping(@NotNull SqaleRepoContext repositoryContext) {
        super(QAccessCertificationCampaign.TABLE_NAME, DEFAULT_ALIAS_NAME,
                AccessCertificationCampaignType.class, QAccessCertificationCampaign.class,
                repositoryContext);

        addRefMapping(F_DEFINITION_REF,
                q -> q.definitionRefTargetOid,
                q -> q.definitionRefTargetType,
                q -> q.definitionRefRelationId,
                QAccessCertificationDefinitionMapping::get);
        addItemMapping(F_END_TIMESTAMP,
                timestampMapper(q -> q.endTimestamp));
        addItemMapping(F_HANDLER_URI, uriMapper(q -> q.handlerUriId));
        // TODO: iteration -> campaignIteration
        addItemMapping(F_ITERATION, integerMapper(q -> q.campaignIteration));
        addRefMapping(F_OWNER_REF,
                q -> q.ownerRefTargetOid,
                q -> q.ownerRefTargetType,
                q -> q.ownerRefRelationId,
                QUserMapping::getUserMapping);
        addItemMapping(F_STAGE_NUMBER, integerMapper(q -> q.stageNumber));
        addItemMapping(F_START_TIMESTAMP,
                timestampMapper(q -> q.startTimestamp));
        addItemMapping(F_STATE, enumMapper(q -> q.state));

        addContainerTableMapping(F_CASE,
                QAccessCertificationCaseMapping.initAccessCertificationCaseMapping(repositoryContext),
                joinOn((o, acase) -> o.oid.eq(acase.ownerOid)));
    }

    @Override
    protected Collection<? extends QName> fullObjectItemsToSkip() {
        return Collections.singletonList(F_CASE);
    }

    @Override
    protected QAccessCertificationCampaign newAliasInstance(String alias) {
        return new QAccessCertificationCampaign(alias);
    }

    @Override
    public MAccessCertificationCampaign newRowObject() {
        return new MAccessCertificationCampaign();
    }

    @Override
    public @NotNull MAccessCertificationCampaign toRowObjectWithoutFullObject(
            AccessCertificationCampaignType schemaObject, JdbcSession jdbcSession) {
        MAccessCertificationCampaign row =
                super.toRowObjectWithoutFullObject(schemaObject, jdbcSession);

        setReference(schemaObject.getDefinitionRef(),
                o -> row.definitionRefTargetOid = o,
                t -> row.definitionRefTargetType = t,
                r -> row.definitionRefRelationId = r);
        row.endTimestamp =
                MiscUtil.asInstant(schemaObject.getEndTimestamp());
        row.handlerUriId = processCacheableUri(schemaObject.getHandlerUri());
        // TODO
        row.campaignIteration = schemaObject.getIteration();
        setReference(schemaObject.getOwnerRef(),
                o -> row.ownerRefTargetOid = o,
                t -> row.ownerRefTargetType = t,
                r -> row.ownerRefRelationId = r);
        row.stageNumber = schemaObject.getStageNumber();
        row.startTimestamp =
                MiscUtil.asInstant(schemaObject.getStartTimestamp());
        row.state = schemaObject.getState();

        return row;
    }

    @Override
    public void storeRelatedEntities(
            @NotNull MAccessCertificationCampaign row, @NotNull AccessCertificationCampaignType schemaObject,
            @NotNull JdbcSession jdbcSession) throws SchemaException {
        super.storeRelatedEntities(row, schemaObject, jdbcSession);

        List<AccessCertificationCaseType> cases = schemaObject.getCase();
        if (!cases.isEmpty()) {
            for (AccessCertificationCaseType c : cases) {
                QAccessCertificationCaseMapping.getAccessCertificationCaseMapping().insert(c, row, jdbcSession);
            }
        }
    }

    // TODO rework to toSchemaObject
    @Override
    public AccessCertificationCampaignType toSchemaObjectWithResolvedNames(Tuple rowTuple, QAccessCertificationCampaign entityPath,
            Collection<SelectorOptions<GetOperationOptions>> options, @NotNull JdbcSession jdbcSession,
            boolean forceFull) throws SchemaException {
        AccessCertificationCampaignType base = super.toSchemaObjectWithResolvedNames(rowTuple, entityPath, options, jdbcSession, forceFull);
        if (forceFull || shouldLoadCases(options)) {
            loadCases(base, options, jdbcSession, forceFull);
        }
        return base;
    }

    private boolean shouldLoadCases(Collection<SelectorOptions<GetOperationOptions>> options) {
        if (options == null) {
            return false;
        }
        for (SelectorOptions<GetOperationOptions> option : options) {
            if (option.getOptions() == null || !RetrieveOption.INCLUDE.equals(option.getOptions().getRetrieve())) {
                continue;
            }
            var path = option.getSelector() != null ? option.getSelector().getPath() : null;
            if (path == null || path.isEmpty() || F_CASE.isSubPathOrEquivalent(path)) {
                return true;
            }
        }
        return false;
    }

    private void loadCases(AccessCertificationCampaignType base, Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull JdbcSession jdbcSession, boolean forceFull) throws SchemaException {
        QAccessCertificationCaseMapping casesMapping = QAccessCertificationCaseMapping.getAccessCertificationCaseMapping();
        PrismContainer<AccessCertificationCaseType> cases = base.asPrismObject().findOrCreateContainer(F_CASE);
        QAccessCertificationCase qcase = casesMapping.defaultAlias();
        var query = jdbcSession.newQuery()
                .from(qcase)
                .select(casesMapping.selectExpressions(qcase, options))
                .where(qcase.ownerOid.eq(SqaleUtils.oidToUUid(base.getOid())));
        // Load all / changed containers

        Collection<Long> idsToFetch = casesToFetch(options);
        if (forceFull || idsToFetch == null) {
            // Noop, no need to add additional condition
            // we are fetching all cases
            cases.setIncomplete(false);

        } else if (idsToFetch.isEmpty()) {
            return;
        } else {
            // We fetch only containers explicitly mentioned in retrieve options
            query = query.where(qcase.cid.in(idsToFetch));
        }
        List<Tuple> rows = query.fetch();
        for (Tuple row : rows) {
            AccessCertificationCaseType c = casesMapping.toSchemaObjectWithResolvedNames(row, qcase, options, jdbcSession, forceFull);
            cases.add(c.asPrismContainerValue());
        }
    }

    private @Nullable Collection<Long> casesToFetch(Collection<SelectorOptions<GetOperationOptions>> options) {
        Set<Long> ret = new HashSet<>();
        for (SelectorOptions<GetOperationOptions> option : options) {
            if (isRetrieveAllCases(option)) {
                return null;
            }
            Long id = caseId(option);
            if (id != null) {
                ret.add(id);
            }
        }
        return ret;
    }

    private Long caseId(SelectorOptions<GetOperationOptions> option) {
        GetOperationOptions getOp = option.getOptions();
        if (getOp == null) {
            return null;
        }
        if (!RetrieveOption.INCLUDE.equals(getOp.getRetrieve())) {
            return null;
        }
        UniformItemPath path = option.getItemPath(null);
        if (path == null || path.size() == 1) {
            return null;
        }
        if (!F_CASE.equals(path.firstName())) {
            return null;
        }
        return ItemPath.toIdOrNull(path.getSegment(1));
    }

    private boolean isRetrieveAllCases(SelectorOptions<GetOperationOptions> option) {
        GetOperationOptions getOp = option.getOptions();
        if (getOp == null) {
            return false;
        }
        UniformItemPath path = option.getSelector() != null ? option.getSelector().getPath() : null;
        return RetrieveOption.INCLUDE.equals(getOp.getRetrieve())
                && (path == null
                || path.isEmpty()
                || F_CASE.equivalent(path)
        );
    }

    @Override
    public Collection<SelectorOptions<GetOperationOptions>> updateGetOptions(
            Collection<SelectorOptions<GetOperationOptions>> options,
            @NotNull Collection<? extends ItemDelta<?, ?>> modifications) {
        Set<Long> alreadyAdded = new HashSet<>();
        Collection<SelectorOptions<GetOperationOptions>> ret = new ArrayList<>(super.updateGetOptions(options, modifications));
        for (ItemDelta<?, ?> modification : modifications) {
            ItemPath modPath = modification.getPath();
            if (modPath.isEmpty()) {
                // Root modification is forbidden
            }
            if (F_CASE.isSubPath(modPath)) {
                Object maybeId = modPath.getSegment(1);
                if (ItemPath.isId(maybeId)) {
                    Long id = ItemPath.toId(maybeId);
                    if (alreadyAdded.contains(id)) {
                        continue;
                    }
                    alreadyAdded.add(id);
                    ret.addAll(SchemaService.get().getOperationOptionsBuilder()
                            .item(modPath.allUpToIncluding(1))
                            .retrieve()
                            .build());
                }
            }
        }
        return ret;

    }
}
