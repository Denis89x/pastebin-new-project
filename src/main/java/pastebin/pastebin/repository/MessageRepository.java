package pastebin.pastebin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pastebin.pastebin.model.Conversation;
import pastebin.pastebin.model.Message;
import pastebin.pastebin.model.Text;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {
    List<Message> findByConversationOrderByDateTimeAsc(Conversation conversation);
    Message findByContent(String linkText);
}
