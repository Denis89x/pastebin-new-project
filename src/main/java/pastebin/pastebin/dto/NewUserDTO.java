package pastebin.pastebin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewUserDTO {

    private String newUsername;

    public String getNewUsername() {
        return newUsername;
    }

    public void setNewUsername(String newUsername) {
        this.newUsername = newUsername;
    }
}
