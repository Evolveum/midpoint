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

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.sql.query2.definition.NotQueryable;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

import static com.evolveum.midpoint.repo.sql.data.common.RObjectTextInfo.COLUMN_OWNER_OID;
import static com.evolveum.midpoint.repo.sql.data.common.RObjectTextInfo.TABLE_NAME;

/**
 * @author mederly
 */
@Entity
@Table(name = TABLE_NAME, indexes = {
        @Index(name = "iTextInfoOid", columnList = COLUMN_OWNER_OID)})
public class RObjectTextInfo implements Serializable {

	private static final Trace LOGGER = TraceManager.getTrace(RObjectTextInfo.class);

    public static final String TABLE_NAME = "m_object_text_info";
    public static final String COLUMN_OWNER_OID = "owner_oid";
    public static final String F_TEXT = "text";

    public static final int MAX_TEXT_SIZE = 255;

    private Integer id;

    private RObject owner;
    private String ownerOid;

    private String text;

    public RObjectTextInfo() {
    }

	public RObjectTextInfo(RObject owner, String text) {
		this.owner = owner;
		this.text = text;
	}

	@Id
    @GeneratedValue
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    @ForeignKey(name = "fk_reference_owner")
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RObjectTextInfo that = (RObjectTextInfo) o;
        return Objects.equals(owner, that.owner) &&
                Objects.equals(ownerOid, that.ownerOid) &&
                Objects.equals(id, that.id) &&
                Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, ownerOid, id, text);
    }

    public static <T extends ObjectType> Set<RObjectTextInfo> createItemsSet(@NotNull ObjectType object, @NotNull RObject repo,
			@NotNull PrismContext prismContext) {
        List<String> allWords = new ArrayList<>();
        append(allWords, object.getName(), prismContext);
        append(allWords, object.getDescription(), prismContext);
        if (object instanceof UserType) {
			UserType user = (UserType) object;
			append(allWords, user.getFullName(), prismContext);
			append(allWords, user.getGivenName(), prismContext);
			append(allWords, user.getFamilyName(), prismContext);
			append(allWords, user.getAdditionalName(), prismContext);
			append(allWords, user.getEmailAddress(), prismContext);
		} else if (object instanceof AbstractRoleType) {
			AbstractRoleType role = (AbstractRoleType) object;
			append(allWords, role.getDisplayName(), prismContext);
			append(allWords, role.getIdentifier(), prismContext);
		}
        return createItemsSet(repo, allWords);
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
				allWords.add(word);
			}
		}
	}

	private static void append(List<String> allWords, PolyStringType text, PrismContext prismContext) {
    	if (text != null) {
    		append(allWords, text.getOrig(), prismContext);
		}
	}

	private static RObjectTextInfo create(RObject repo, String text) {
        RObjectTextInfo rv = new RObjectTextInfo();
        rv.setOwner(repo);
        rv.setText(text);
        return rv;
    }
}
