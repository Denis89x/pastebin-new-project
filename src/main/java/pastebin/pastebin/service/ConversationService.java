package pastebin.pastebin.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pastebin.pastebin.model.Conversation;
import pastebin.pastebin.model.ConversationParticipant;
import pastebin.pastebin.repository.ConversationPartRepository;
import pastebin.pastebin.repository.ConversationRepository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Optional;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationPartRepository conversationPartRepository;
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public ConversationService(ConversationRepository conversationRepository, ConversationPartRepository conversationPartRepository) {
        this.conversationRepository = conversationRepository;
        this.conversationPartRepository = conversationPartRepository;
    }

    @Transactional
    public Conversation createConversationAndUpdateParticipants(ConversationParticipant sender, ConversationParticipant receiver) {
        System.err.println("Creating new conversation...");
        Conversation newConversation = new Conversation();
        System.err.println("newConversation = " + newConversation);
        conversationRepository.save(newConversation);
        entityManager.refresh(newConversation);
        System.err.println("Saved.");

        conversationRepository.updateParticipantConversation(sender.getId(), newConversation.getId());
        System.err.println("After updating sender: " + sender);
        System.err.println("After updating sender conversation: " + sender.getConversation());

        conversationRepository.updateParticipantConversation(receiver.getId(), newConversation.getId());
        Optional<ConversationParticipant> newReceiver = conversationPartRepository.findById(receiver.getId());
        System.err.println("new receiver = " + newReceiver);
        System.err.println("After updating receiver conversation: " + receiver.getConversation());

        System.err.println("New conversation created: " + newConversation.getId());
        return newConversation;
    }
}

