package pastebin.pastebin.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pastebin.pastebin.model.ConversationParticipant;
import pastebin.pastebin.repository.ConversationPartRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class CommonParticipantsService {

    private final ConversationPartRepository conversationPartRepository;

    @Autowired
    public CommonParticipantsService(ConversationPartRepository conversationPartRepository) {
        this.conversationPartRepository = conversationPartRepository;
    }

    public List<ConversationParticipant> findCommonParticipants(String senderUsername, String receiverUsername) {
        List<ConversationParticipant> senderParticipants = conversationPartRepository.findAllByAccountUsername(senderUsername);
        List<ConversationParticipant> receiverParticipants = conversationPartRepository.findAllByAccountUsername(receiverUsername);

        List<ConversationParticipant> commonParticipants = new ArrayList<>();
        System.err.println("common first" + commonParticipants);

        for (ConversationParticipant senderParticipant : senderParticipants) {
            for (ConversationParticipant receiverParticipant : receiverParticipants) {
                if (senderParticipant.getConversation() != null && receiverParticipant.getConversation() != null) {
                    if (senderParticipant.getConversation().getId().equals(receiverParticipant.getConversation().getId())) {
                        commonParticipants.add(senderParticipant);
                        commonParticipants.add(receiverParticipant);
                    }
                }
            }
        }

        return commonParticipants;
    }



}
