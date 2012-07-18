package com.evolveum.midpoint.repo.sql.data.common;

import org.hibernate.FetchMode;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Index;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import java.io.Serializable;
import java.util.Set;

/**
 * @author lazyman
 */
@Entity
@Table(name = "m_org_closure")
// @org.hibernate.annotations.Table(appliesTo = "m_org_closure", foreignKey=
// @ForeignKey(name = "iDescendant"))
public class ROrgClosure implements Serializable {

	// todo how about FK keys?
	private Long id;
	// private Set<RObject> ancestors;
	// private Set<RObject> descendants;
	private RObject ancestor;
	private RObject descendant;
	private int depth;

	public ROrgClosure() {

	}

	public ROrgClosure(RObject ancestor, RObject descendant, int depth) {
		this.ancestor = ancestor;
		this.descendant = descendant;
		this.depth = depth;
	}

	@Id
	@GeneratedValue
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	// @Id
	// @Index(name = "iAncestor")
	// @ManyToMany(fetch = FetchType.LAZY, mappedBy="closureRefs")
	// @JoinColumns({
	// @JoinColumn(name = "ancestor_oid", referencedColumnName = "oid"),
	// @JoinColumn(name = "ancestor_id", referencedColumnName = "id")
	// })
	// @Cascade({ org.hibernate.annotations.CascadeType.ALL })
	// public Set<RObject> getAncestors() {
	// return ancestors;
	// }

	// @Id
	// @Index(name = "iDescendant")
	// @ManyToMany(fetch = FetchType.LAZY, mappedBy="closureRefs")
	// @JoinColumns({
	// @JoinColumn(name = "descendant_oid", referencedColumnName = "oid"),
	// @JoinColumn(name = "descendant_id", referencedColumnName = "id")
	// })
	// @Cascade({ org.hibernate.annotations.CascadeType.ALL })
	// public Set<RObject> getDescendants() {
	// return descendants;
	// }

	@Index(name = "iAncestor")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumns({ @JoinColumn(name = "ancestor_oid", referencedColumnName = "oid"),
			@JoinColumn(name = "ancestor_id", referencedColumnName = "id") })
	public RObject getAncestor() {
		return ancestor;
	}

	public void setAncestor(RObject ancestor) {
		this.ancestor = ancestor;
	}

	@Index(name = "iDescendant")
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumns({ @JoinColumn(name = "descendant_oid", referencedColumnName = "oid"),
		@JoinColumn(name = "descendant_id", referencedColumnName = "id") })
	public RObject getDescendant() {
		return descendant;
	}

	public void setDescendant(RObject descendant) {
		this.descendant = descendant;
	}

	public int getDepth() {
		return depth;
	}

	// public void setAncestors(Set<RObject> ancestor) {
	// this.ancestors = ancestor;
	// }
	//
	// public void setDescendants(Set<RObject> descendant) {
	// this.descendants = descendant;
	// }

	public void setDepth(int depth) {
		this.depth = depth;
	}

	@Override
	public int hashCode() {
		int result = ancestor != null ? ancestor.hashCode() : 0;
		result = 31 * result + (descendant != null ? descendant.hashCode() : 0);
		result = 31 * result + depth;
		result = 31 * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;

		ROrgClosure that = (ROrgClosure) obj;

		if (depth != that.depth)
			return false;

		if (ancestor != null ? !ancestor.equals(that.ancestor) : that.ancestor != null)
			return false;
		if (descendant != null ? !descendant.equals(that.descendant) : that.descendant != null)
			return false;
		if (id != null ? !id.equals(that.id) : that.id != null)
			return false;

		return true;
	}
}
