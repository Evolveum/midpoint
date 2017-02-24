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

package com.evolveum.midpoint.repo.sql.data.common;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.sql.data.RepositoryContext;
import com.evolveum.midpoint.repo.sql.data.common.id.RObjectTextInfoId;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.repo.sql.data.common.RObjectTextInfo.COLUMN_OWNER_OID;
import static com.evolveum.midpoint.repo.sql.data.common.RObjectTextInfo.TABLE_NAME;

/**
 * @author mederly
 */
@Entity
@IdClass(RObjectTextInfoId.class)
@Table(name = TABLE_NAME)
public class RObjectTextInfo implements Serializable {

	private static final Trace LOGGER = TraceManager.getTrace(RObjectTextInfo.class);

    public static final String TABLE_NAME = "m_object_text_info";
    public static final String COLUMN_OWNER_OID = "owner_oid";
    public static final String F_TEXT = "text";

    public static final int MAX_TEXT_SIZE = 255;

    private RObject owner;
    private String ownerOid;

    private String text;

    public RObjectTextInfo() {
    }

	public RObjectTextInfo(RObject owner, String text) {
		this.owner = owner;
		this.text = text;
	}

    @ForeignKey(name = "fk_object_text_info_owner")
    @MapsId("owner")
    @ManyToOne(fetch = FetchType.LAZY)
    @NotQueryable
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
	@Column(name = "text", length = MAX_TEXT_SIZE)
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof RObjectTextInfo))
			return false;
		RObjectTextInfo that = (RObjectTextInfo) o;
		return Objects.equals(getOwnerOid(), that.getOwnerOid()) &&
				Objects.equals(getText(), that.getText());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getOwnerOid(), getText());
	}

	public static <T extends ObjectType> Set<RObjectTextInfo> createItemsSet(@NotNull ObjectType object, @NotNull RObject repo,
			@NotNull RepositoryContext repositoryContext) {

		FullTextSearchConfigurationType config = repositoryContext.repositoryService.getFullTextSearchConfiguration();
		if (config == null || config.getIndexed().isEmpty() || Boolean.FALSE.equals(config.isEnabled())) {
			return Collections.emptySet();
		}
		List<QName> types =
				ObjectTypes.getObjectType(object.getClass()).thisAndSupertypes().stream()
						.map(ot -> ot.getTypeQName())
						.collect(Collectors.toList());
		Set<ItemPath> paths = new HashSet<>();
		for (FullTextSearchIndexedItemsConfigurationType indexed : config.getIndexed()) {
			if (isApplicable(indexed, types)) {
				for (ItemPathType itemPathType : indexed.getItem()) {
					ItemPath path = itemPathType.getItemPath();
					if (path.isEmpty()) {
						LOGGER.debug("Empty path in full time index configuration; skipping.");
					} else if (!ItemPath.containsEquivalent(paths, path)) {
						paths.add(path);
					}
				}
			}
		}

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

		List<String> allWords = new ArrayList<>();		// not a (hash) set in order to preserve order
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
		LOGGER.trace("Indexing {}:\n  items: {}\n  values: {}\n  words:  {}", object, paths, values, allWords);
		return createItemsSet(repo, allWords);
    }

	private static boolean isApplicable(FullTextSearchIndexedItemsConfigurationType indexed, List<QName> types) {
		if (indexed.getObjectType().isEmpty()) {
			return true;
		}
		for (QName type : types) {
			if (QNameUtil.matchAny(type, indexed.getObjectType())) {
				return true;
			}
		}
		return false;
	}

	private static Set<RObjectTextInfo> createItemsSet(RObject repo, List<String> allWords) {
		Set<RObjectTextInfo> rv = new HashSet<>();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < allWords.size(); i++) {
			String word = allWords.get(i);
			if (sb.length() + word.length() + 2 <= MAX_TEXT_SIZE) {
				sb.append(" ").append(word);
			} else {
				if (sb.length() > 0) {
					sb.append(" ");
					rv.add(new RObjectTextInfo(repo, sb.toString()));
					sb = new StringBuilder();
				} else {
					// a problem - too large string
					LOGGER.warn("Word too long to be correctly indexed: {}", word);
					rv.add(new RObjectTextInfo(repo, " " + word.substring(0, MAX_TEXT_SIZE - 2) + " "));
					allWords.set(i, word.substring(MAX_TEXT_SIZE - 2));
					i--;		// to reiterate
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
				"ownerOid='" + getOwnerOid()+ '\'' +
				", text='" + text + '\'' +
				'}';
	}
}
