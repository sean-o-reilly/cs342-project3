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
        GameStateNoti,
        MovePieceReq,
        MovePieceRejected,
        MovePieceNoti,
        GameChatTextMessage,
        GameChatNoti,
        PlayAgainReq
    }

    String body = "";
    String user = "";
    ArrayList<String> list = new ArrayList<String>();

    int rowStart = -1;
    int rowEnd = -1;
    int colStart = -1;
    int colEnd = -1;

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

    Message(MessageType type, int rowStart, int colStart, int rowEnd, int colEnd){
        this.type = type;
        this.gameState = null;
        this.rowStart = rowStart;
        this.colStart = colStart;
        this.rowEnd = rowEnd;
        this.colEnd = colEnd;
    }
}
