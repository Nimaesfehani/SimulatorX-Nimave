package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {
    private static final int PORT = 12345;
    private static final String USERS_FILE = "users.ser";
    private static Map<String, User> users = new HashMap<>();

    public static void main(String[] args) {
        loadUsers();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadUsers() {
        try (FileInputStream fileIn = new FileInputStream(USERS_FILE);
             ObjectInputStream in = new ObjectInputStream(fileIn)) {
            users = (Map<String, User>) in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No existing user data found. Starting fresh.");
            users = new HashMap<>();
        }
    }

    private static void saveUsers() {
        try (FileOutputStream fileOut = new FileOutputStream(USERS_FILE);
             ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                 ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

                String action = (String) in.readObject();
                switch (action) {
                    case "LOGIN":
                        handleLogin(in, out);
                        break;
                    case "REGISTER":
                        handleRegister(in, out);
                        break;
                    case "GET_PROFILE":
                        handleGetProfile(in, out);
                        break;
                    case "UPDATE_PROFILE":
                        handleUpdateProfile(in, out);
                        break;
                    // Add more cases for other actions
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private void handleLogin(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
            String username = (String) in.readObject();
            String password = (String) in.readObject();
            User user = users.get(username);
            if (user != null && user.getPassword().equals(password)) {
                out.writeObject("SUCCESS");
                out.writeObject(user);
            } else {
                out.writeObject("FAIL");
            }
        }

        private void handleRegister(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
            User user = (User) in.readObject();
            if (users.containsKey(user.getUsername())) {
                out.writeObject("FAIL");
            } else {
                users.put(user.getUsername(), user);
                saveUsers();
                out.writeObject("SUCCESS");
            }
        }

        private void handleGetProfile(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
            String username = (String) in.readObject();
            User user = users.get(username);
            out.writeObject(user);
        }

        private void handleUpdateProfile(ObjectInputStream in, ObjectOutputStream out) throws IOException, ClassNotFoundException {
            User user = (User) in.readObject();
            users.put(user.getUsername(), user);
            saveUsers();
            out.writeObject("SUCCESS");
        }
    }
}
