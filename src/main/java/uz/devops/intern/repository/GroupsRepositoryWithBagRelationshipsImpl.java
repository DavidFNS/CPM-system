package uz.devops.intern.repository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.hibernate.annotations.QueryHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import uz.devops.intern.domain.Groups;

/**
 * Utility repository to load bag relationships based on https://vladmihalcea.com/hibernate-multiplebagfetchexception/
 */
public class GroupsRepositoryWithBagRelationshipsImpl implements GroupsRepositoryWithBagRelationships {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Groups> fetchBagRelationships(Optional<Groups> groups) {
        return groups.map(this::fetchUsers);
    }

    @Override
    public Page<Groups> fetchBagRelationships(Page<Groups> groups) {
        return new PageImpl<>(fetchBagRelationships(groups.getContent()), groups.getPageable(), groups.getTotalElements());
    }

    @Override
    public List<Groups> fetchBagRelationships(List<Groups> groups) {
        return Optional.of(groups).map(this::fetchUsers).orElse(Collections.emptyList());
    }

    Groups fetchUsers(Groups result) {
        return entityManager
            .createQuery("select groups from Groups groups left join fetch groups.users where groups is :groups", Groups.class)
            .setParameter("groups", result)
            .setHint(QueryHints.PASS_DISTINCT_THROUGH, false)
            .getSingleResult();
    }

    List<Groups> fetchUsers(List<Groups> groups) {
        HashMap<Object, Integer> order = new HashMap<>();
        IntStream.range(0, groups.size()).forEach(index -> order.put(groups.get(index).getId(), index));
        List<Groups> result = entityManager
            .createQuery("select distinct groups from Groups groups left join fetch groups.users where groups in :groups", Groups.class)
            .setParameter("groups", groups)
            .setHint(QueryHints.PASS_DISTINCT_THROUGH, false)
            .getResultList();
        Collections.sort(result, (o1, o2) -> Integer.compare(order.get(o1.getId()), order.get(o2.getId())));
        return result;
    }
}
