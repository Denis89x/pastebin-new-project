package pastebin.pastebin.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class RankingEntry {

    private String username;

    private Integer messageCount;

    public RankingEntry(String username, Integer messageCount) {
        this.username = username;
        this.messageCount = messageCount;
    }
}
