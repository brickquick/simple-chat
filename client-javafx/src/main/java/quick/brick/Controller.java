package quick.brick;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.io.*;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    private static final String HOST = "localhost";
    private static final int PORT = 8189;

    //Bottom panel
    @FXML
    private TextArea chatArea;
    @FXML
    private TextField inputTextField;
    //Top panel
    @FXML
    public HBox topPanel;
    @FXML
    private Button authBtn;
    @FXML
    private TextField loginField, passField;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private FileOutputStream fos;

    private volatile String myNick = "";
    private volatile String myLogin = "";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        openConnection();
    }

    private void openConnection() {
        try {
            socket = new Socket(HOST, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            updateTopPanel();
            Thread t = new Thread(() -> {
                try {
                    while (socket != null && !socket.isClosed()) {
                        String str = in.readUTF();
                        if (str.startsWith("/authok ")) {
                            myNick = str.split("\\s")[1];
                            myLogin = str.split("\\s")[2];
                            initiateLoginFile(myLogin);
                            break;
                        }
                        writeLoginChatFile(str);
                    }
                    updateTopPanel();
                    while (socket != null && !socket.isClosed()) {
                        Platform.runLater(() -> inputTextField.requestFocus());
                        String strFromServer = in.readUTF();
                        if (strFromServer.equalsIgnoreCase("/end")) {
                            closeConnection();
                            break;
                        }
                        if (strFromServer.startsWith("/newnickok ")) {
                            myNick = strFromServer.split("\\s")[1];
                            updateTopPanel();
                            continue;
                        }
                        writeLoginChatFile(strFromServer);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert("Отсутствует подключение к серверу!", e.toString());
                } finally {
                    closeConnection();
                    updateTopPanel();
                    System.out.println("Отключение от сервера!");
                    myNick = "";
                    writeLoginChatFile("Вы были отключены от сервера!");
                    myLogin = "";
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            t.start();
        } catch (IOException e) {
            updateTopPanel();
            System.err.println("Не удалось подключиться к серверу");
            e.printStackTrace();
            showAlert("Не удалось подключиться к серверу!", e.toString());
        }
    }

    public void closeConnection() {
        if (socket != null && !socket.isClosed()) {
            try {
                fos.close();
                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Disconnect");
        }
    }

    public void sendMessage() {
        if (!inputTextField.getText().trim().isEmpty()) {
            try {
                out.writeUTF(inputTextField.getText().trim());
                inputTextField.clear();
                inputTextField.requestFocus();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Ошибка отправки сообщения");
                showAlert("Ошибка отправки сообщения!", e.toString());
            }
        }
    }

    public void focusFieldPass() {
        if (!loginField.getText().trim().isEmpty()) {
            passField.requestFocus();
        }
    }

    public void openConnectionFromPassField() {
        if (!passField.getText().trim().isEmpty() && !loginField.getText().trim().isEmpty()) {
            onAuthClick();
        }
    }

    public void onAuthClick() {
        if (socket == null || socket.isClosed()) {
            openConnection();
        } else {
            if (socket != null && !socket.isClosed()) {
                try {
                    out.writeUTF("/auth " + loginField.getText() + " " + passField.getText());
                    loginField.setText("");
                    passField.setText("");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void showAlert(String msg, String err) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText(msg);
            alert.getDialogPane().setExpandableContent(new ScrollPane(new TextArea(err)));
            alert.showAndWait();
        });
    }

    private void updateTopPanel() {
        Platform.runLater(() -> {
            if ((myNick == null || myNick.equals("")) || (socket == null || socket.isClosed())) {
                authBtn.setText("Connect to server");
                topPanel.getChildren().removeAll(loginField, passField, authBtn);
                topPanel.getChildren().add(authBtn);
                authBtn.setDisable(false);
            }
            if ((myNick == null || myNick.equals("")) && (socket != null && !socket.isClosed())) {
                authBtn.setText("Authentication");
                topPanel.getChildren().remove(authBtn);
                topPanel.getChildren().addAll(loginField, passField, authBtn);
                authBtn.setDisable(false);
            }
            if ((!myNick.equals("") && myNick != null) && (socket != null && !socket.isClosed())) {
                authBtn.setText("Online: " + myNick);
                topPanel.getChildren().removeAll(loginField, passField, authBtn);
                topPanel.getChildren().add(authBtn);
                authBtn.setDisable(true);
            }
        });
    }

    private void initiateLoginFile(String login) {
        String strPath = "client-javafx/src/main/resources/history_" + login + ".txt";
        Platform.runLater(() -> {
//        chatArea.clear();
            chatArea.setText("");
            chatArea.appendText("");
            try {
                File f = new File(strPath);
                fos = new FileOutputStream(strPath, true);
                if (f.createNewFile()) {
                    System.out.println("File created");
                } else {
                    System.out.println("File already exists");
                    try (BufferedReader reader = new BufferedReader(new FileReader(strPath))) {
                        String[] lines = new String[100];
                        int lastNdx = 0;
                        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                            if (lastNdx == lines.length) {
                                lastNdx = 0;
                            }
                            lines[lastNdx++] = line;
                        }
                        for (int ndx = lastNdx; ndx < lines.length && lines[ndx] != null; ndx++) {
                            chatArea.appendText(lines[ndx] + "\n");
                        }
                        for (int ndx = 0; ndx < lastNdx && lines[ndx] != null; ndx++) {
                            chatArea.appendText(lines[ndx] + "\n");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void writeLoginChatFile(String text) {
        String finalText = text + "\n";
        Platform.runLater(() -> chatArea.appendText(finalText));
        try {
            fos.write(finalText.getBytes());
            fos.flush();
        } catch (NullPointerException e) {
            System.out.println(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
