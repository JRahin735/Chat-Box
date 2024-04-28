import java.io.Serializable;
import java.util.List;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;
    private String type; // "JOIN", "MESSAGE", "GROUP"
    private String sender;
    private String content;
    private List<String> recipients; // For group messages or a specific recipient

    // Constructor
    public Message(String type, String sender, String content, List<String> recipients) {
        this.type = type;
        this.sender = sender;
        this.content = content;
        this.recipients = recipients;
    }

    // Getters
    public String getType() { return type; }
    public String getSender() { return sender; }
    public Serializable getContent() { return content; }
    public List<String> getRecipients() { return recipients; }
}
