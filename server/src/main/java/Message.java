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
        RespActiveUsers,
        FindGameReq,
        FindGameResponse,
        JoinGameReq,
        JoinGameOK,
        JoinGameFailed,
        PlayerJoinedGameNoti,
        LeaveGameReq,
        LeaveGameOK,
        PlayerLeftGameNoti,
        GameStateNoti
    }

    String body = "";
    String user = "";
    ArrayList<String> list = new ArrayList<String>();

    final MessageType type;
    final GameStateDTO gameState;

    Message(MessageType type) {
        this.type = type;
        this.gameState = null;
    }

    Message(String body, MessageType type) {
        this.body = body;
        this.type = type;
        this.gameState = null;
    }

    Message(MessageType type, GameStateDTO state) {
        this.type = type;
        this.gameState = state;
    }
}
