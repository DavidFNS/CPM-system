package uz.devops.intern.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.ToString;

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
//@ToString
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Groups implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "group_owner_name")
    private String groupOwnerName;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "rel_groups__customers",
        joinColumns = @JoinColumn(name = "groups_id"),
        inverseJoinColumns = @JoinColumn(name = "customers_id")
    )
    @JsonIgnoreProperties(value = { "user", "groups" }, allowSetters = true)
    private Set<Customers> customers = new HashSet<>();

    @ManyToOne
    @JsonIgnoreProperties(value = { "groups" }, allowSetters = true)

    private Organization organization;

    @ManyToMany(mappedBy = "groups", fetch = FetchType.EAGER)
    @JsonIgnoreProperties(value = { "groups" }, allowSetters = true)
    private Set<Services> services = new HashSet<>();

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

    public Set<Customers> getCustomers() {
        return this.customers;
    }

    public void setCustomers(Set<Customers> customers) {
        this.customers = customers;
    }

    public Groups customers(Set<Customers> customers) {
        this.setCustomers(customers);
        return this;
    }

    public Groups addCustomers(Customers customers) {
        this.customers.add(customers);
        customers.getGroups().add(this);
        return this;
    }

    public Groups removeCustomers(Customers customers) {
        this.customers.remove(customers);
        customers.getGroups().remove(this);
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

    public Set<Services> getServices() {
        return this.services;
    }

    public void setServices(Set<Services> services) {
        if (this.services != null) {
            this.services.forEach(i -> i.removeGroups(this));
        }
        if (services != null) {
            services.forEach(i -> i.addGroups(this));
        }
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
            ", name='" + getName() + "'" +
            ", groupOwnerName='" + getGroupOwnerName() + "'" +
            "}";
    }
}
