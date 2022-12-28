package uz.devops.intern.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uz.devops.intern.domain.User;


/**
 * Spring Data JPA repository for the {@link User} entity.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findOneByActivationKey(String activationKey);
    List<User> findAllByActivatedIsFalseAndActivationKeyIsNotNullAndCreatedDateBefore(Instant dateTime);
    Optional<User> findOneByResetKey(String resetKey);
    Optional<User> findOneByEmailIgnoreCase(String email);
    Optional<User> findOneByLogin(String login);

    Optional<User> findByCreatedBy(String phoneNumber);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findOneWithAuthoritiesByLogin(String login);

    @EntityGraph(attributePaths = "authorities")
    Optional<User> findOneWithAuthoritiesByEmailIgnoreCase(String email);

    Page<User> findAllByIdNotNullAndActivatedIsTrue(Pageable pageable);

    @Query("SELECT u FROM User u WHERE u.createdBy = :phoneNumber")
    Optional<User> getUserByPhoneNumber(@Param("phoneNumber") String phoneNumber);

    Optional<User> findByEmail(String email);

    Optional<User> findByFirstName(String firstName);

    @Query(value = "SELECT * FROM jhi_user WHERE id IN (SELECT user_id FROM customers WHERE id IN (SELECT customers_id FROM rel_groups__customers WHERE groups_id = :groupId))",
    nativeQuery = true)
    List<User> findAllByGroupId(@Param("groupId") Long groupId);
}
