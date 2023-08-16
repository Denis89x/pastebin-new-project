package pastebin.pastebin.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pastebin.pastebin.model.*;
import pastebin.pastebin.repository.AccountRepository;
import pastebin.pastebin.repository.ConversationPartRepository;
import pastebin.pastebin.repository.ConversationRepository;
import pastebin.pastebin.repository.MessageRepository;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final AccountRepository accountRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationPartRepository conversationPartRepository;
    private final WriteLinkService writeLinkService;
    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;
    private final CommonParticipantsService commonParticipantsService;

    @Autowired
    public MessageService(MessageRepository messageRepository, AccountRepository accountRepository, ConversationRepository conversationRepository, ConversationPartRepository conversationPartRepository, WriteLinkService writeLinkService, ConversationService conversationService, SimpMessagingTemplate messagingTemplate, CommonParticipantsService commonParticipantsService) {
        this.messageRepository = messageRepository;
        this.accountRepository = accountRepository;
        this.conversationRepository = conversationRepository;
        this.conversationPartRepository = conversationPartRepository;
        this.writeLinkService = writeLinkService;
        this.conversationService = conversationService;
        this.messagingTemplate = messagingTemplate;
        this.commonParticipantsService = commonParticipantsService;
    }

/*    List<ConversationParticipant> senderParticipants = conversationPartRepository.findAllByAccountUsername(senderUsername);
    List<ConversationParticipant> receiverParticipants = conversationPartRepository.findAllByAccountUsername(receiverUsername);
        System.err.println("senderParticipants = " + senderParticipants);
        System.err.println("receiverParticipants = " + receiverParticipants);

    List<ConversationParticipant> commonParticipants = commonParticipantsService.findCommonParticipants(senderUsername, receiverUsername);

        System.err.println("#######" + commonParticipants);

    ConversationParticipant sender = commonParticipants.get(0);
    ConversationParticipant receiver = commonParticipants.get(1);*/

    public void sendMessage(String senderUsername, String receiverUsername, String content) {
        List<ConversationParticipant> senderParticipants = conversationPartRepository.findAllByAccountUsername(senderUsername);
        List<ConversationParticipant> receiverParticipants = conversationPartRepository.findAllByAccountUsername(receiverUsername);
        System.err.println("senderParticipants111 = " + senderParticipants);
        System.err.println("receiverParticipants111 = " + receiverParticipants);

        List<ConversationParticipant> commonParticipants = commonParticipantsService.findCommonParticipants(senderUsername, receiverUsername);

        System.err.println("@@@@@@@@@@@@@@@@" + commonParticipants);

        ConversationParticipant sender = null;
        ConversationParticipant receiver = null;

        if (!commonParticipants.isEmpty()) {
            sender = commonParticipants.get(0);
            receiver = commonParticipants.get(1);
        }

        Optional<Account> senderM = accountRepository.findByUsername(senderUsername);
        System.err.println("sender = " + sender);
        Optional<Account> receiverM = accountRepository.findByUsername(receiverUsername);
        System.err.println("receiver = " + receiver);

        if (senderM.isPresent() && receiverM.isPresent() && (sender == null && receiver == null)) {
            System.out.println("я тут");
            List<ConversationParticipant> senders = conversationPartRepository.findAllByAccount(senderM.get());
            List<ConversationParticipant> receivers = conversationPartRepository.findAllByAccount(receiverM.get());
            ConversationParticipant senderParticipant;
            if (senders.isEmpty()) {
                senderParticipant = createParticipant(senderM.get());
            } else {
                senderParticipant = senders.get(0);
            }
            ConversationParticipant receiverParticipant;
            if (receivers.isEmpty()) {
                receiverParticipant = createParticipant(receiverM.get());
            } else {
                receiverParticipant = receivers.get(0);
            }

/*            ConversationParticipant senderParticipant = conversationPartRepository.findByAccount(senderM.get())
                    .orElseGet(() -> createParticipant(senderM.get()));

            ConversationParticipant receiverParticipant = conversationPartRepository.findByAccount(receiverM.get())
                    .orElseGet(() -> createParticipant(receiverM.get()));*/

            commitMessage(senderParticipant, receiverParticipant, content);

            String topic = "/topic/messages/" + receiverUsername;
            messagingTemplate.convertAndSend(topic, content);
            System.err.println("После 111 messagingTemplate.convertAndSend(topic, content);");
        }

        if (sender != null && receiver != null) {
            System.out.println("я тута");
            commitMessage(sender, receiver, content);

            String topic = "/topic/messages/" + receiverUsername;
            messagingTemplate.convertAndSend(topic, content);
            System.err.println("После 222 messagingTemplate.convertAndSend(topic, content);");
        } else {
            System.out.println("не найден 222");
        }
    }

    public Optional<Conversation> findByParticipants(List<ConversationParticipant> participants) {
        return conversationRepository.findByParticipantsIn(participants);
    }

    @Transactional
    public void commitMessage(ConversationParticipant sender, ConversationParticipant receiver, String content) {
        Conversation conversation = findOrCreateConversation(sender, receiver);
        System.err.println("conversation " + conversation);

        // Convert the content to a link and save it to MinIO
        String link = writeLinkService.writeLink(content, "message");

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender.getAccount()); // Assuming ConversationParticipant has a method to get the Account
        message.setContent(link); // Save the link instead of content
        message.setDateTime(LocalDateTime.now());
        message.setIsChecked(false);

        System.err.println("перед message.sage");
        messageRepository.save(message);
        System.err.println("после message.save");
    }

    public List<Message> getConversationMessages(String senderUsername, String receiverUsername) {
        List<ConversationParticipant> senderParticipants = conversationPartRepository.findAllByAccountUsername(senderUsername);
        List<ConversationParticipant> receiverParticipants = conversationPartRepository.findAllByAccountUsername(receiverUsername);
        System.err.println("senderParticipants = " + senderParticipants);
        System.err.println("receiverParticipants = " + receiverParticipants);

        List<ConversationParticipant> commonParticipants = commonParticipantsService.findCommonParticipants(senderUsername, receiverUsername);

        System.err.println("#######" + commonParticipants);

        ConversationParticipant sender = commonParticipants.get(0);
        ConversationParticipant receiver = commonParticipants.get(1);

        if (sender != null && receiver != null) {
            List<Message> messages = new ArrayList<>();

            List<ConversationParticipant> participants = new ArrayList<>();
            participants.add(sender);
            participants.add(receiver);

            Optional<Conversation> conversation = findByParticipants(participants);

            if (conversation.isPresent()) {
                messages = messageRepository.findByConversationOrderByDateTimeAsc(conversation.get());

                for (Message message : messages) {
                    writeLinkService.checkAndUpdateLinkIfNeeded(message);
                }

                // Convert link to content using WriteLinkService
                for (Message message : messages) {

                    String content = writeLinkService.getTextFromLink(message.getContent());
                    message.setContent(content);
                }
            }

            return messages;
        }
        return Collections.emptyList();
    }

    private ConversationParticipant createParticipant(Account account) {
        ConversationParticipant participant = new ConversationParticipant();
        participant.setAccount(account);

        return conversationPartRepository.save(participant);
    }

    @Transactional
    public Conversation findOrCreateConversation(ConversationParticipant sender, ConversationParticipant receiver) {
        if (sender.getConversation() == null && receiver.getConversation() == null) {
            return conversationService.createConversationAndUpdateParticipants(sender, receiver);
        }

        if (Objects.equals(sender.getConversation(), receiver.getConversation())) {
            System.err.println("Вернули беседу так-как нашли");
            return sender.getConversation();
        } else {
            System.err.println("зашли в else т.к не нашли беседу");
            ConversationParticipant newSender = createParticipant(sender.getAccount());
            ConversationParticipant newReceiver = createParticipant(receiver.getAccount());
            return conversationService.createConversationAndUpdateParticipants(newSender, newReceiver);
        }
    }

    public String getCurrentUserUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }

    public List<String> getOtherUsernames() {
        // Получение текущего пользователя
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUserUsername = authentication.getName();

        // Извлечение всех пользователей, кроме текущего
        List<Account> allAccountsExceptCurrent = accountRepository.findAllByUsernameNot(currentUserUsername);

        // Формирование списка имен других пользователей
        List<String> otherUsernames = new ArrayList<>();
        for (Account account : allAccountsExceptCurrent) {
            otherUsernames.add(account.getUsername());
        }

        return otherUsernames;
    }

    /*public List<Account> getDialogUsernames(String currentUser) {
        System.err.println("1");
        List<ConversationParticipant> currentUserParticipants = conversationPartRepository.findByAccountUsername(currentUser)
                .map(Collections::singletonList)
                .orElse(Collections.emptyList());
        System.err.println("current" + currentUserParticipants);
        List<Account> dialogUsernames = new ArrayList<>();
        System.err.println("dialog" + dialogUsernames);

        for (ConversationParticipant participant : currentUserParticipants) {
            List<ConversationParticipant> participants = new ArrayList<>();
            participants.add(participant);

            Optional<Conversation> conversation = findByParticipants(participants);
            conversation.ifPresent(value -> {
                value.getParticipants()
                        .stream()
                        .filter(p -> !p.getAccount().getUsername().equals(currentUser))
                        .forEach(otherParticipant -> {
                            try {
                                if (otherParticipant.getAccount().getProfilePicture() != null) {
                                    if (writeLinkService.isLinkExpired(otherParticipant.getAccount().getProfilePicture())) {
                                        writeLinkService.updateImageLink(otherParticipant.getAccount().getProfilePicture());
                                    }
                                }
                                dialogUsernames.add(otherParticipant.getAccount());
                            } catch (Exception e) {
                                // Обработка ошибок
                            }
                        });
            });
        }

        return dialogUsernames;
    }*/

    public List<Account> getDialogUsernames(String currentUser) {
        List<ConversationParticipant> currentUserParticipants = conversationPartRepository.findAllByAccountUsername(currentUser);

        List<Account> dialogUsernames = new ArrayList<>();

        for (ConversationParticipant participant : currentUserParticipants) {
            List<ConversationParticipant> participants = new ArrayList<>();
            participants.add(participant);

            Optional<Conversation> conversation = findByParticipants(participants);
            conversation.ifPresent(value -> {
                for (ConversationParticipant otherParticipant : value.getParticipants()) {
                    if (!otherParticipant.getAccount().getUsername().equals(currentUser)) {
                        try {
                            if (otherParticipant.getAccount().getProfilePicture() != null) {
                                if (writeLinkService.isLinkExpired(otherParticipant.getAccount().getProfilePicture())) {
                                    writeLinkService.updateImageLink(otherParticipant.getAccount().getProfilePicture());
                                }
                            }
                            dialogUsernames.add(otherParticipant.getAccount());
                        } catch (Exception e) {
                            // Обработка ошибок
                        }
                    }
                }
            });
        }

        return dialogUsernames;
    }




    public Optional<Account> findAccountByUsername(String username) {
        return accountRepository.findByUsername(username);
    }

}



