package uz.devops.intern.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;
import javax.validation.constraints.*;

/**
 * A Groups.
 */
@Entity
@Table(name = "groups")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Groups implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "group_manager_id", nullable = false)
    private Integer groupManagerId;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @NotNull
    @Column(name = "group_owner_name", nullable = false)
    private String groupOwnerName;

    @ManyToMany
    @JoinTable(
        name = "rel_groups__services",
        joinColumns = @JoinColumn(name = "groups_id"),
        inverseJoinColumns = @JoinColumn(name = "services_id")
    )
    @JsonIgnoreProperties(value = { "users", "groups" }, allowSetters = true)
    private Set<Services> services = new HashSet<>();

    @ManyToOne
    @JsonIgnoreProperties(value = { "groups" }, allowSetters = true)
    private Organization organization;

    @ManyToMany(mappedBy = "groups")
    @JsonIgnoreProperties(value = { "groups", "services" }, allowSetters = true)
    private Set<Customers> users = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Groups id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getGroupManagerId() {
        return this.groupManagerId;
    }

    public Groups groupManagerId(Integer groupManagerId) {
        this.setGroupManagerId(groupManagerId);
        return this;
    }

    public void setGroupManagerId(Integer groupManagerId) {
        this.groupManagerId = groupManagerId;
    }

    public String getName() {
        return this.name;
    }

    public Groups name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupOwnerName() {
        return this.groupOwnerName;
    }

    public Groups groupOwnerName(String groupOwnerName) {
        this.setGroupOwnerName(groupOwnerName);
        return this;
    }

    public void setGroupOwnerName(String groupOwnerName) {
        this.groupOwnerName = groupOwnerName;
    }

    public Set<Services> getServices() {
        return this.services;
    }

    public void setServices(Set<Services> services) {
        this.services = services;
    }

    public Groups services(Set<Services> services) {
        this.setServices(services);
        return this;
    }

    public Groups addServices(Services services) {
        this.services.add(services);
        services.getGroups().add(this);
        return this;
    }

    public Groups removeServices(Services services) {
        this.services.remove(services);
        services.getGroups().remove(this);
        return this;
    }

    public Organization getOrganization() {
        return this.organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Groups organization(Organization organization) {
        this.setOrganization(organization);
        return this;
    }

    public Set<Customers> getUsers() {
        return this.users;
    }

    public void setUsers(Set<Customers> customers) {
        if (this.users != null) {
            this.users.forEach(i -> i.removeGroups(this));
        }
        if (customers != null) {
            customers.forEach(i -> i.addGroups(this));
        }
        this.users = customers;
    }

    public Groups users(Set<Customers> customers) {
        this.setUsers(customers);
        return this;
    }

    public Groups addUsers(Customers customers) {
        this.users.add(customers);
        customers.getGroups().add(this);
        return this;
    }

    public Groups removeUsers(Customers customers) {
        this.users.remove(customers);
        customers.getGroups().remove(this);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Groups)) {
            return false;
        }
        return id != null && id.equals(((Groups) o).id);
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Groups{" +
            "id=" + getId() +
            ", groupManagerId=" + getGroupManagerId() +
            ", name='" + getName() + "'" +
            ", groupOwnerName='" + getGroupOwnerName() + "'" +
            "}";
    }
}
