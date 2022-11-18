package uz.devops.intern.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import uz.devops.intern.domain.Customers;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.validation.constraints.*;

/**
 * A DTO for the {@link uz.devops.intern.domain.Groups} entity.
 */
@SuppressWarnings("common-java:DuplicatedBlocks")
public class GroupsDTO implements Serializable {

    private Long id;

    @NotNull
    private String name;

    private String groupOwnerName;

    private OrganizationDTO organization;
//    @JsonIgnoreProperties(value = {"services", "customers", "user"})
    private Set<CustomersDTO> users;
    public Set<CustomersDTO> getUsers() {
        return users;
    }

    public void setUsers(Set<CustomersDTO> users) {
        this.users = users;
    }

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
            ", organization=" + getOrganization() +
            "}";
    }
}
