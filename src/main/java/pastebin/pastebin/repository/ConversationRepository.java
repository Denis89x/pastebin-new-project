package pastebin.pastebin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pastebin.pastebin.model.Conversation;
import pastebin.pastebin.model.ConversationParticipant;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Integer> {

    Optional<Conversation> findByParticipantsIn(List<ConversationParticipant> participants);

/*    @Modifying
    @Query(value = "INSERT INTO conversation_participant (conversation_id, account_id) VALUES (:conversationId, :participantId)", nativeQuery = true)
    void addParticipant(@Param("conversationId") Integer conversationId, @Param("participantId") Integer participantId);*/

    boolean existsByParticipants(ConversationParticipant participant);

    Optional<Conversation> findConversationById(Integer id);

    @Modifying
    @Query(value = "UPDATE conversation_participant " +
            "SET conversation_id = :conversationId " +
            "WHERE id_participant = :participantId",
            nativeQuery = true)
    void updateParticipantConversation(@Param("participantId") Integer participantId, @Param("conversationId") Integer conversationId);

}
