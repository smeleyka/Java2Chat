package ru.geekbrains.chat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class Server {
    private Vector<ClientHandler> clients;
    private AuthService authService;

    final String COMMANDS_HELP_TEXT =
            "Список служебных команд:\n" +
                    "'/end' - отключиться от сервера\n" +
                    "'/w nick' - отослать личное сообщение клиенту с ником nick\n" +
                    "'/changenick newnick' - сменить ник на новый\n" +
                    "'/help' - получить помощь по основным командам";

    public AuthService getAuthService() {
        return authService;
    }

    public Server() {
        clients = new Vector<>();
        authService = new DBAuthService();
        try (ServerSocket server = new ServerSocket(8189);) {
            authService.start();
            System.out.println("Server started. Waiting for clients...");
            while (true) {
                Socket socket = server.accept();
                new ClientHandler(this, socket);
                System.out.println("Client connected");
            }
        } catch (AuthServiceException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            authService.stop();
        }
    }

    public void broadcastMsg(String msg) {
        for (ClientHandler o : clients) {
            o.sendMsg(msg);
        }
    }

    public void broadcastClientsList() {
        StringBuilder sb = new StringBuilder("/clientslist ");
        for (ClientHandler o : clients) {
            sb.append(o.getNick() + " ");
        }
        String out = sb.toString();
        for (ClientHandler o : clients) {
            o.sendMsg(out);
        }
    }

    public void whispMsg(ClientHandler from, String nickTo, String msg) {
        for (ClientHandler o : clients) {
            if (o.getNick().equals(nickTo)) {
                o.sendMsg("from " + from.getNick() + ": " + msg);
                from.sendMsg("to " + nickTo + ": " + msg);
                break;
            }
        }
    }

    public boolean isNickBusy(String nick) {
        for (ClientHandler o : clients) {
            if (o.getNick().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    public void subscribe(ClientHandler clientHandler) {
        broadcastMsg("К чату присоединился " + clientHandler.getNick());
        clients.add(clientHandler);
        broadcastClientsList();
    }

    public void unsubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        broadcastClientsList();
        broadcastMsg("Из чата вышел " + clientHandler.getNick());
    }


}
