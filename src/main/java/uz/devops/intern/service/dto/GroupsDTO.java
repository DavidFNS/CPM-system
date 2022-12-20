package uz.devops.intern.service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.validation.constraints.*;

/**
 * A DTO for the {@link uz.devops.intern.domain.Groups} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupsDTO implements Serializable {

    private Long id;

    @NotNull
    private String name;
    private String groupOwnerName;
    private Set<CustomersDTO> customers = new HashSet<>();
    private Set<ServicesDTO> services;

    public Set<ServicesDTO> getServices() {
        return services;
    }

    public void setServices(Set<ServicesDTO> services) {
        this.services = services;
    }

    private OrganizationDTO organization;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupOwnerName() {
        return groupOwnerName;
    }

    public void setGroupOwnerName(String groupOwnerName) {
        this.groupOwnerName = groupOwnerName;
    }

    public Set<CustomersDTO> getCustomers() {
        return customers;
    }

    public void setCustomers(Set<CustomersDTO> customers) {
        this.customers = customers;
    }

    public OrganizationDTO getOrganization() {
        return organization;
    }

    public void setOrganization(OrganizationDTO organization) {
        this.organization = organization;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GroupsDTO)) {
            return false;
        }

        GroupsDTO groupsDTO = (GroupsDTO) o;
        if (this.id == null) {
            return false;
        }
        return Objects.equals(this.id, groupsDTO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "GroupsDTO{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", groupOwnerName='" + getGroupOwnerName() + "'" +
            ", customers=" + getCustomers() +
            ", organization=" + getOrganization() +
            "}";
    }
}
