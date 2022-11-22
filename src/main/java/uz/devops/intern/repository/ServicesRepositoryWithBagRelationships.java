package uz.devops.intern.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import uz.devops.intern.domain.Services;

public interface ServicesRepositoryWithBagRelationships {
    Optional<Services> fetchBagRelationships(Optional<Services> services);

    List<Services> fetchBagRelationships(List<Services> services);

    Page<Services> fetchBagRelationships(Page<Services> services);
}
