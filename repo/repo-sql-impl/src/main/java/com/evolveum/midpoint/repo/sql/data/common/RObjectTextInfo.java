/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql.data.common;

import static com.evolveum.midpoint.repo.sql.data.common.RObjectTextInfo.TABLE_NAME;

import java.io.Serializable;
import java.util.*;
import jakarta.persistence.*;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.id.RObjectTextInfoId;
import com.evolveum.midpoint.repo.sql.helpers.modify.Ignore;
import com.evolveum.midpoint.repo.sql.query.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.util.FullTextSearchUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FullTextSearchConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

@Ignore
@Entity
@IdClass(RObjectTextInfoId.class)
@Table(name = TABLE_NAME)
@DynamicUpdate
public class RObjectTextInfo implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(RObjectTextInfo.class);

    public static final String TABLE_NAME = "m_object_text_info";
    public static final String COLUMN_OWNER_OID = "owner_oid";
    public static final String F_TEXT = "text";

    private static final int DEFAULT_MAX_TEXT_SIZE = 255;

    private RObject owner;
    private String ownerOid;

    private String text;

    @SuppressWarnings("unused")
    public RObjectTextInfo() {
    }

    public RObjectTextInfo(RObject owner, String text) {
        this.owner = owner;
        this.text = text;
    }

    @MapsId("ownerOid")
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
    @JoinColumn(name = COLUMN_OWNER_OID, foreignKey = @ForeignKey(name = "fk_object_text_info_owner"))
    public RObject getOwner() {
        return owner;
    }

    @Id
    @Column(name = COLUMN_OWNER_OID, length = RUtil.COLUMN_LENGTH_OID)
    @NotQueryable
    public String getOwnerOid() {
        if (ownerOid == null && owner != null) {
            ownerOid = owner.getOid();
        }
        return ownerOid;
    }

    public void setOwner(RObject owner) {
        this.owner = owner;
    }

    public void setOwnerOid(String ownerOid) {
        this.ownerOid = ownerOid;
    }

    @Id
    @Column(name = "text", length = DEFAULT_MAX_TEXT_SIZE)
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (!(o instanceof RObjectTextInfo)) { return false; }
        RObjectTextInfo that = (RObjectTextInfo) o;
        return Objects.equals(getOwnerOid(), that.getOwnerOid()) &&
                Objects.equals(getText(), that.getText());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOwnerOid(), getText());
    }

    public static Set<RObjectTextInfo> createItemsSet(@NotNull ObjectType object, @NotNull RObject repo,
            @NotNull RepositoryContext repositoryContext) {

        FullTextSearchConfigurationType config = repositoryContext.repositoryService.getFullTextSearchConfiguration();
        if (!FullTextSearchUtil.isEnabled(config)) {
            return Collections.emptySet();
        }
        Set<ItemPath> paths = FullTextSearchUtil.getFullTextSearchItemPaths(config, object.getClass());

        List<PrismValue> values = new ArrayList<>();
        for (ItemPath path : paths) {
            Object o = object.asPrismObject().find(path);
            if (o == null) {
                // shouldn't occur
            } else if (o instanceof PrismValue) {
                values.add((PrismValue) o);
            } else if (o instanceof Item) {
                values.addAll(((Item<?, ?>) o).getValues());
            } else {
                throw new IllegalStateException("Unexpected value " + o + " in " + object + " at " + path);
            }
        }

        List<String> allWords = new ArrayList<>();        // not a (hash) set in order to preserve order
        for (PrismValue value : values) {
            if (value == null) {
                continue;
            }
            if (value instanceof PrismPropertyValue) {
                Object realValue = value.getRealValue();
                if (realValue == null) {
                    // skip
                } else if (realValue instanceof String) {
                    append(allWords, (String) realValue, repositoryContext.prismContext);
                } else if (realValue instanceof PolyString) {
                    append(allWords, (PolyString) realValue, repositoryContext.prismContext);
                } else {
                    append(allWords, realValue.toString(), repositoryContext.prismContext);
                }
            }
        }
        int maxTextSize = repositoryContext.configuration.getTextInfoColumnSize();
        LOGGER.trace("Indexing {}:\n  items: {}\n  values: {}\n  words:  {}\n  max text size:  {}", object, paths, values,
                allWords, maxTextSize);
        return createItemsSet(repo, allWords, maxTextSize);
    }

    private static Set<RObjectTextInfo> createItemsSet(RObject repo, List<String> allWords, int maxTextSize) {
        Set<RObjectTextInfo> rv = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < allWords.size(); i++) {
            String word = allWords.get(i);
            if (sb.length() + word.length() + 2 <= maxTextSize) {
                sb.append(" ").append(word);
            } else {
                if (sb.length() > 0) {
                    sb.append(" ");
                    rv.add(new RObjectTextInfo(repo, sb.toString()));
                    sb = new StringBuilder();
                    i--;        // to reiterate
                } else {
                    // a problem - too large string
                    LOGGER.warn("Word too long to be correctly indexed: {}", word);
                    rv.add(new RObjectTextInfo(repo, " " + word.substring(0, maxTextSize - 2) + " "));
                    allWords.set(i, word.substring(maxTextSize - 2));
                    i--;        // to reiterate (with shortened word)
                }
            }
        }
        if (sb.length() > 0) {
            sb.append(" ");
            rv.add(new RObjectTextInfo(repo, sb.toString()));
        }
        return rv;
    }

    private static void append(List<String> allWords, String text, PrismContext prismContext) {
        if (StringUtils.isBlank(text)) {
            return;
        }
        String normalized = prismContext.getDefaultPolyStringNormalizer().normalize(text);
        String[] words = StringUtils.split(normalized);
        for (String word : words) {
            if (StringUtils.isNotBlank(word)) {
                if (!allWords.contains(word)) {
                    allWords.add(word);
                }
            }
        }
    }

    private static void append(List<String> allWords, PolyString text, PrismContext prismContext) {
        if (text != null) {
            append(allWords, text.getOrig(), prismContext);
        }
    }

    @Override
    public String toString() {
        return "RObjectTextInfo{" +
                "ownerOid='" + getOwnerOid() + '\'' +
                ", text='" + text + '\'' +
                '}';
    }
}
