import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;

public class Message implements Serializable {
    static final long serialVersionUID = 42L;

    public enum MessageType {
        Unknown,
        LoginRequest,
        LoginOK,
        LoginFailed,
        GlobalTextMessage,
        DirectTextMessage,
        ChatNoti,
        UserJoinedNoti,
        UserLeftNoti,
        GetActiveUsers,
        RespActiveUsers
    }

    String body = "";
    String user = "";
    ArrayList<String> list = new ArrayList<String>();
    MessageType type;

    Message(String body, MessageType type) {
        this.body = body;
        this.type = type;
    }
}
