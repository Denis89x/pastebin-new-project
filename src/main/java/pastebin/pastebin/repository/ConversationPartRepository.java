package pastebin.pastebin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pastebin.pastebin.model.Account;
import pastebin.pastebin.model.ConversationParticipant;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationPartRepository extends JpaRepository<ConversationParticipant, Integer> {
    Optional<ConversationParticipant> findByAccountUsername(String username);
    Optional<ConversationParticipant> findByAccount(Account account);

    List<ConversationParticipant> findAllByAccount(Account account);

    Optional<ConversationParticipant> findById(Integer id);

    List<ConversationParticipant> findAllByAccountUsername(String username);
}
