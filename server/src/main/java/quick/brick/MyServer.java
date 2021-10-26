package quick.brick;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MyServer {

    private static final int PORT = 8189;
    private static final Logger LOGGER = LogManager.getLogger(MyServer.class);

    private List<ClientHandler> clients;
    private AuthService authService;

    public MyServer() {
        try (ServerSocket server = new ServerSocket(PORT)) {
            authService = new BaseAuthService();
//            authService.start();
            clients = new ArrayList<>();
            LOGGER.info("Сервер успешно запущен");

            for (int i = 1; ; i++) {
                LOGGER.info("Сервер ожидает подключения");
                Socket socket = server.accept();
                new ClientHandler(this, socket, i);
            }
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("Ошибка в работе сервера");
        } finally {
            if (authService != null) {
                authService.stop();
            }
        }
    }

    public synchronized void sendMsgToClient(ClientHandler from, String nickTo, String msg) {
        for (ClientHandler o : clients) {
            if (o.getName().equals(nickTo)) {
                o.sendMsg("от " + from.getName() + ": " + msg);
                from.sendMsg("клиенту " + nickTo + ": " + msg);
                return;
            }
        }
        from.sendMsg("Участника с ником " + nickTo + " нет в чат-комнате");
    }

    public synchronized void readClientsList(ClientHandler from) {
        StringBuilder str = new StringBuilder("Connected clients:\n");
        for (ClientHandler o : clients) {
            str.append(o.getName()).append(" ");
        }
        from.sendMsg(str.toString());
    }

    public synchronized boolean isAccountBusy(String nick) {
        for (ClientHandler o : clients) {
            if (o.getName().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void broadcastMsg(String msg) {
        for (ClientHandler o : clients) {
            o.sendMsg(msg);
        }
    }

    public synchronized void unsubscribe(ClientHandler o) {
        clients.remove(o);
    }

    public synchronized void subscribe(ClientHandler o) {
        clients.add(o);
    }

    public AuthService getAuthService() {
        return authService;
    }

}
