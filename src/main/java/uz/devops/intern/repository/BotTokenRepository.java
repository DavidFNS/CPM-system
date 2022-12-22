package uz.devops.intern.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uz.devops.intern.domain.BotToken;

/**
 * Spring Data JPA repository for the BotToken entity.
 */
@SuppressWarnings("unused")
@Repository
public interface BotTokenRepository extends JpaRepository<BotToken, Long> {
    @Query("select botToken from BotToken botToken where botToken.createdBy.login = ?#{principal.username}")
    List<BotToken> findByCreatedByIsCurrentUser();

    Optional<BotToken> findByTelegramId(Long chatId);

    Boolean existsByToken(String token);

    Optional<BotToken> findByToken(String token);

    @Query(value = "SELECT b FROM BotToken b WHERE b.createdBy = (SELECT u FROM User u WHERE u.createdBy = (SELECT ct.phoneNumber FROM CustomerTelegram ct WHERE ct.telegramId = :managerId))")
    Optional<BotToken> findByManagerId(@Param("managerId") Long managerId);
}
