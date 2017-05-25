package ru.geekbrains.chat.client;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Paint;
import javafx.util.Callback;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    public TextArea textArea;
    public TextField textField;

    public TextField loginField;
    public TextField passField;
    public Button btnLogin;

    public HBox loginPanel;
    public HBox msgPanel;

    public ListView<String> clientsList;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    private boolean isAuthorized;
    private boolean isClientsListVisible;
    private String myNick;

    class MyListCell extends ListCell<String> {
        @Override
        protected void updateItem(String item, boolean empty) {
            if(!empty) {
                if(item.equals(myNick)) {
                    setTextFill(Paint.valueOf("Green"));

                }
            }
            setText(item);
            super.updateItem(item, empty);
        }
    }

    public void setIsAuthorized(boolean value) {
        isAuthorized = value;
        Platform.runLater(() -> {
            if (value) {
                loginPanel.setVisible(false);
                loginPanel.setManaged(false);
                msgPanel.setVisible(true);
                msgPanel.setManaged(true);
                clientsList.setVisible(true);
                clientsList.setManaged(true);
                textField.requestFocus();
                refreshClientsListVisible();
            } else {
                loginPanel.setVisible(true);
                loginPanel.setManaged(true);
                msgPanel.setVisible(false);
                msgPanel.setManaged(false);
                clientsList.setVisible(false);
                clientsList.setManaged(false);
                refreshClientsListVisible();
                myNick = "";
            }
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        isClientsListVisible = true;
        clientsList.setCellFactory(param -> new MyListCell());

        setIsAuthorized(false);
    }

    public void connect() {
        try {
            socket = new Socket("localhost", 8189);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            Thread inputThread = new Thread(() -> {
                try {
                    String msg = null;
                    while (true) {
                        msg = in.readUTF();
                        if (msg.equals("78s7d6fjh53987t5hkj&^KGujgd")) {
                            setIsAuthorized(true);
                            break;
                        } else {
                            textArea.appendText(msg + "\n");
                        }
                    }
                    while (true) {
                        msg = in.readUTF();
                        if (msg.startsWith("/")) {
                            if (msg.startsWith("/yournickis ")) {
                                myNick = msg.split("\\s")[1];
                                textArea.appendText("Вы вошли под ником " + myNick + "\n");
                            }
                            if (msg.startsWith("/clientslist ")) {
                                String[] clients = msg.split("\\s");
                                Platform.runLater(() -> {
                                    clientsList.getItems().clear();
                                    for (int i = 1; i < clients.length; i++) {
                                        clientsList.getItems().add(clients[i]);
                                    }
                                });
                            }
                            if (msg.equals("/disconnect")) {
                                break;
                            }
                        } else {
                            textArea.appendText(msg + "\n");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert("Проблемы при обращении к серверу");
                } finally {
                    setIsAuthorized(false);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            inputThread.setDaemon(true);
            inputThread.start();
        } catch (IOException e) {
            showAlert("Не удалось подключиться к серверу, проверьте соединение");
            e.printStackTrace();
        }
    }

    public void sendAuth() {
        try {
            if (socket == null || socket.isClosed()) {
                connect();
            }
            out.writeUTF("/auth " + loginField.getText() + " " + passField.getText());
            loginField.clear();
            passField.clear();
            loginField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg() {
        try {
            out.writeUTF(textField.getText());
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            showAlert("Не получается отослать сообщение, проверьте подключение");
            e.printStackTrace();
        }
    }

    public void showAlert(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Возникли проблемы");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
        });
    }

    public void clientsListClick(MouseEvent mouseEvent) {
        if (mouseEvent.getClickCount() == 2) {
            textField.setText("/w " + clientsList.getSelectionModel().getSelectedItem() + " ");
            textField.requestFocus();
            textField.selectEnd();
        }
    }

    public void menuSendHelpRequest() {
        try {
            out.writeUTF("/help");
        } catch (IOException e) {
            showAlert("Не получается отослать сообщение, проверьте подключение");
            e.printStackTrace();
        }
    }

    public void exit() {
        try {
            if (socket != null && !socket.isClosed()) {
                out.writeUTF("/end");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Platform.exit();
        }
    }

    public void clientsListToggle() {
        isClientsListVisible = !isClientsListVisible;
        refreshClientsListVisible();
    }

    public void refreshClientsListVisible() {
        clientsList.setVisible(false);
        clientsList.setManaged(false);
        if (isClientsListVisible && isAuthorized) {
            clientsList.setVisible(true);
            clientsList.setManaged(true);
        }
    }
}