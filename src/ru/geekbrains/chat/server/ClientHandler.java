package ru.geekbrains.chat.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String nick;

    public String getNick() {
        return nick;
    }

    public ClientHandler(Server server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try {
                    String msg = null;
                    while (true) {
                        msg = in.readUTF();
                        if (msg.startsWith("/auth ")) {
                            String[] data = msg.split("\\s");
                            if(data.length != 3) continue;
                            String newNick = server.getAuthService().getNickByLoginAndPass(data[1], data[2]);
                            if (newNick != null) {
                                if (!server.isNickBusy(newNick)) {
                                    sendMsg("78s7d6fjh53987t5hkj&^KGujgd");
                                    sendMsg("/yournickis " + newNick);
                                    sendMsg("Добро пожаловать\nДля помощи наберите команду /help");
                                    nick = newNick;
                                    server.subscribe(this);
                                    break;
                                } else {
                                    sendMsg("Учетная запись уже используется");
                                }
                            } else {
                                sendMsg("Неверный логин/пароль");
                            }
                        } else {
                            sendMsg("Необходимо авторизоваться");
                        }
                    }
                    while (true) {
                        msg = in.readUTF();
                        System.out.println("from client: " + msg);
                        if (msg.startsWith("/")) {
                            if (msg.equals("/end")) {
                                sendMsg("/disconnect");
                                break;
                            }
                            if (msg.startsWith("/w ")) {
                                String[] tokens = msg.split("\\s", 3);
                                server.whispMsg(this, tokens[1], tokens[2]);
                            }
                            if (msg.startsWith("/changenick ")) {
                                String oldNick = nick;
                                String newNick = msg.split("\\s")[1];
                                if (server.getAuthService().changeNick(this, newNick)) {
                                    sendMsg("/yournickis " + newNick);
                                    nick = newNick;
                                    server.broadcastMsg(oldNick + " сменил ник на " + newNick);
                                    server.broadcastClientsList();
                                } else {
                                    sendMsg("Такой ник уже занят");
                                }
                            }
                            if (msg.equals("/help")) {
                                sendMsg(server.COMMANDS_HELP_TEXT);
                            }
                        } else {
                            server.broadcastMsg(nick + ": " + msg);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println("Client disconnected");
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    server.unsubscribe(this);
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
