package pastebin.pastebin.model;


import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Table(name = "text")
@Entity
@Getter
@Setter
@ToString
public class Text {

    @Id
    @Column(name = "id_text")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int textId;

    @Column(name = "link_text")
    private String linkText;

    @Column(name = "date_time")
    private LocalDateTime time;

    @ManyToOne
    @JoinColumn(name = "id_account", referencedColumnName = "id_account")
    private Account owner;
}
