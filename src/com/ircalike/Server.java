package com.ircalike;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.*;
import java.util.*;

/** En server som tillhandahåller specifika kommandom som kan utföras i chattprogram som ansluts till den tillsammans med koppling av en databas och metoder för att verifiera säkra lösenord via kryptering.
 * @author Nephixium
 * @version 1.0
 */

public class Server implements Runnable {

    private boolean poweredState;
    private boolean dbConnectState;
    private String dbUserTable;
    private Connection dbConnection;
    private ServerSocket serverSocket;
    private int serverPort;
    private JFrame frame;
    private JTextArea serverOutput;
    private Map<String, Socket> userList;
    private Map<String, PrintWriter> writerList;

    public Server(int port, Boolean connectDatabase) {
        this.dbConnectState = connectDatabase;
        this.poweredState = true;
        this.userList = new HashMap<String, Socket>();
        this.writerList = new HashMap<String, PrintWriter>();
        this.serverPort = port;
        this.serverOutput = new JTextArea();

        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println("Error: Connection failed");
            e.printStackTrace();
        }
    }

    /** Startar en server tillsammans med ett admin konto som har rättigheter att utföra vissa kommandon, exempelvis att stänga servern eller ta bort användare från sessionen.
     * Om dbConnectState är true kopplas en databas till servern som sedan används för att verifiera användarnamn och lösenord på klienter som ansluter sig till servern.
     */
    @Override
    public void run() {

        connectDatabase(this.dbConnectState);

            if(this.poweredState) {

                this.frame = new JFrame("Server window");
                this.frame.setPreferredSize(new Dimension(600, 400));
                this.frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        shutdown();
                        super.windowClosing(e);
                    }
                });
                this.frame.getWindowListeners();
                this.frame.add(new JScrollPane(this.serverOutput));
                this.frame.getContentPane();
                this.frame.pack();
                this.frame.setVisible(true);
                String adminPassword = null;

                while(adminPassword == null || adminPassword.length() < 8) {
                    adminPassword = new JOptionPane().showInputDialog(null, "Input admin password");
                }

                new Thread(new Client(serverSocket.getInetAddress().getHostAddress(), serverSocket.getLocalPort(), "++Administrator", adminPassword)).start();
                while (true) {

                    try {
                        Socket client = null;
                        client = serverSocket.accept();
                        Thread clientHandler = new Thread(new ClientHandler(client, this));
                        clientHandler.start();

                    } catch (IOException e) {
                        if(poweredState) {
                            e.printStackTrace();
                            System.out.println("Could not resolve connection to client");
                        } break;
                    }
                }
            }
        if(poweredState) {
            shutdown();
        }
        this.frame.dispose();
    }

    /** Ansluter till en databas enligt information som samlas in via en rad dialogrutor
     *
     * @param dbConnectState
     */
    private void connectDatabase(boolean dbConnectState) {
        if(dbConnectState) {

            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();

            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                e.printStackTrace();
                new JOptionPane().showMessageDialog(null, "Error initiating com.mysql.jdbc.Driver");
                shutdown();
            }

            /** Uppgifterna som används för att ansluta till databasens url address via tre dialogrutor */
            new JOptionPane().showMessageDialog(null,
                    "MUST contain the following fields in order:\n" +
                                "1. Username (VARCHAR)\n" +
                                "2. Password (VARBINARY - a hashed password)\n" +
                                "3. Salt (VARBINARY - the salt generated with the hashed password");
            String dbUrl = "jdbc:mysql://"
                    + new JOptionPane().showInputDialog(new JTextField(), "Input database host address")
                    + "/"
                    + new JOptionPane().showInputDialog(new JTextField(), "Input database name");
            String dbUsername = new JOptionPane().showInputDialog(new JTextField(), "Input database username");
            String dbPassword = new JOptionPane().showInputDialog(new JTextField(), "Input database password");

            /** Tabellen som data skall hämtas från */
            this.dbUserTable = new JOptionPane().showInputDialog(null, "Input table to collect data from");

            /** Om det går att ansluta till databasen sparas anslutningen i dbConnection
             * Går det inte att ansluta ändras dbConnectState till false och servern fortsätter utan en databas ansluten */
            try {
                Connection connection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
                this.dbConnection = connection;

            } catch (SQLException e) {
                e.printStackTrace();
                new JOptionPane().showMessageDialog(null, "Could not connect to database");
                shutdown();
            }
        }
    }

    /** Söker igenom databasen efter det givna användarnamnet och återger true om en matchning fanns
     *
     * @param username användarnamnet som matchas mot de användarnamn som finns i den kopplade databasen
     * @return true på dbConnectState om anslutningen till databasen lyckades, alternativt false om det misslyckas
     */
    public boolean pollDatabase(String username) {
        String query = "SELECT * FROM " + this.dbUserTable;

        try {
            PreparedStatement preparedStatement = this.dbConnection.prepareStatement(query);
            ResultSet results = preparedStatement.executeQuery(query);

            while(results.next()) {
                if(results.getString("Username").equals(username)) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            this.serverOutput.append("Could not poll database, check database connection");
        }
        return false;
    }

    /** Tar bort en given användare från servern genom att skicka ett privat meddelande till denna som i sin tur stänger socketen lokalt
     *
     * @param username användarnamnet på klienten som tas bort från servern
     */
    public void terminate(String username) {
        ArrayList<String> arrayUsers = new ArrayList<>();
        for(String name : userList.keySet()) {
            arrayUsers.add(name);
        }

        for(int x = 0; x < arrayUsers.size(); x++) {
            if(arrayUsers.get(x).equals(username)) {
                privateSend(username, "/kick " + username);

                userList.remove(username);
                writerList.remove(username);

                this.serverOutput.append(username + " has left the session");
                this.serverOutput.append("\n");

                if(username.equals("++Administrator")) {
                    shutdown();
                }
                updateUserlist();
            }
        }
    }

    /** Använder en boolean för att ange huruvida servern är aktiv (true) eller inaktiv (false)
     *
     * @param serverState serverns nuvarande stadie - true för aktiv, eller false för inaktiv
     */
    public void serverActive(Boolean serverState) {
        this.poweredState = serverState;
    }

    /** Stänger ned servern genom att först skicka en signal till alla anslutna klienter så dessa stänger sina socketar, för att sedan visa ett meddelande och stänga servern
     *
     */
    public void shutdown() {
        ArrayList<String> purgeList = new ArrayList();
        serverActive(false);
        for (String user : userList.keySet()) {
            purgeList.add(user);
        }

        for(int x = 0; x < purgeList.size(); x++) {
            terminate(purgeList.get(x));
        }

        new JOptionPane().showMessageDialog(null, "Server shutting down");
        try {
            this.serverSocket.close();
            serverActive(false);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Could not close server socket, restart program");
            System.exit(1);
        }
    }

    /** Validerar en given användares lösenord med det lösenord som finns lagrat i databasen genom att först hämta salt och det lagrade hashade lösenordet.
     * Det givna lösenordet hashas med det lagrade saltet som hämtats och matchas mot det lösenord som fanns i databasen.
     * Om matchningen var korrekt återges true, i annat fall false.
     *
     * @param username det givna användarnamnet vars lösenord skall kontrolleras
     * @param passwordToValidate det lösenord som skall kontrolleras
     * @return ett korrekt lösenord (true) alternativt ett inkorrekt lösenord (false)
     */
    public boolean validatePassword(String username, String passwordToValidate) {

        /* SQL argument som används för att hämta värden från databasen.
         * Alla fält i tabellen dbUserTable hämtas. */
        String query = "SELECT * FROM " + this.dbUserTable;
        try {
            PreparedStatement preparedStatement = this.dbConnection.prepareStatement(query);
            ResultSet results = preparedStatement.executeQuery(query);
            byte[] dbPassword = null;
            byte[] dbSalt = null;

            // Går igenom databasens tabell så länge pekaren hittar ett nytt värde
            while(results.next()) {
                if(results.getString("Username").equals(username)) {
                    dbPassword = results.getBytes(2);
                    dbSalt = results.getBytes(3);
                    break;
                }
            }

            preparedStatement.close();

            // Hashar det givna lösenordet som skall valideras med salt från databasen
            KeySpec kSpec = new PBEKeySpec(passwordToValidate.toCharArray(), dbSalt, 65536, 256);
            try {
                SecretKeyFactory kFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                byte[] hashedPasswordToValidate = kFactory.generateSecret(kSpec).getEncoded();

                // Kontrollerar byte arrayerna mot varandra och återger true om de matchar
                if(Arrays.equals(hashedPasswordToValidate, dbPassword)) {
                    return true;
                }
            } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            }
        return false;
        }

    /** Skickar ett meddelande till alla anslutna klienter genom att hämta skrivare från en lista
     *
     * @param message meddelandet som skall skickas
     */
    public void globalSend(String message) throws IOException {

        for(String x : writerList.keySet()) {
            writerList.get(x).println(message);
        }
    }

    /** Skickar ett meddelandet till en specifik klient genom att hämta dess skrivare från en lista
     *
     * @param username klientens användarnamn
     * @param message meddelandet som skall skickas
     */
    public void privateSend(String username, String message) {

        for(String x : writerList.keySet()) {
            if(x.equals(username)) {
                writerList.get(x).println(message);
                break;
            }
        }
    }

    /** Om en databas är kopplad till servern, matchas ett givet användarnamn och lösenord mot det hashade lösenordet som finns lagrat i databasen.
     * Användaren registreras på servern om användarnamn och lösenord är korrekt.
     * Om användarnamnet inte existerar, skapas ett nytt konto.
     *
     * I det fallet en databas inte finns ansluten läggs användaren till i en lista och tillåts anslutning till servern.
     *
     * @param username användarnamnet som en klient eller användare försöker ansluta med
     * @param userSocket den socket som klienten eller användaren ansluter genom
     * @param password användaren eller klientens lösenord
     * @throws IOException
     */
    public void registerUsername(String username, Socket userSocket, String password) throws IOException {

        if(this.dbConnectState) {
            if(pollDatabase(username)) {
                if (validatePassword(username, password)) {

                    this.serverOutput.append("<" + username + ">" + " joined with IP: " + userSocket.getInetAddress());
                    this.serverOutput.append("\n");
                    this.userList.put(username, userSocket);
                    this.writerList.put(username, new PrintWriter(userSocket.getOutputStream(), true));

                    updateUserlist();

                } else {
                    this.serverOutput.append("<" + username + ">" + " denied connection with IP: " + userSocket.getInetAddress() + " wrong password");
                    this.serverOutput.append("\n");
                    this.userList.put(username, userSocket);
                    this.writerList.put(username, new PrintWriter(userSocket.getOutputStream(), true));

                    terminate(username);
                }
            } else {
                try {
                    SecureRandom random = new SecureRandom();
                    byte[] salt = new byte[16];
                    random.nextBytes(salt);

                    KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
                    SecretKeyFactory kFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                    byte[] hash = kFactory.generateSecret(spec).getEncoded();

                    String query = "INSERT INTO" + " " + this.dbUserTable + " " + "VALUES(?,?,?)";
                    PreparedStatement preparedStatement = this.dbConnection.prepareStatement(query);
                    preparedStatement.setString(1, username);
                    preparedStatement.setBytes(2, hash);
                    preparedStatement.setBytes(3, salt);
                    preparedStatement.executeUpdate();
                    preparedStatement.close();

                    this.serverOutput.append("<" + username + ">" + " joined with IP: " + userSocket.getInetAddress());
                    this.serverOutput.append("\n");
                    this.userList.put(username, userSocket);
                    this.writerList.put(username, new PrintWriter(userSocket.getOutputStream(),true));

                    updateUserlist();

                } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                    e.printStackTrace();
                } catch (SQLException e) {
                    e.printStackTrace();
                    this.serverOutput.append("Database connection error, could not add user '" + username + "' to database");
                }
            }
        } else {

            this.serverOutput.append("<" + username + ">" + " joined with IP: " + userSocket.getInetAddress());
            this.serverOutput.append("\n");
            this.userList.put(username, userSocket);
            this.writerList.put(username, new PrintWriter(userSocket.getOutputStream(),true));

            updateUserlist();
        }
    }

    /** Uppmanar alla anslutna klienters chattprogram att uppdatera listan av användare i form av ett meddelande som agerar signal som startar höndelsen tillsammans med en Arraylist med användarnamnen.
     *
     */
    public void updateUserlist() {
        ArrayList<String> users = new ArrayList<>();

        for(String x : this.userList.keySet()) {
            users.add(x);
        }

        Collections.sort(users, String.CASE_INSENSITIVE_ORDER);
        try {
            globalSend("/updateuserlist" + users);
        } catch (IOException e) {
            e.printStackTrace();
        }
        updateTitle();
    }

    /** Uppdaterar titelraden i server output fönstret så att den konstant visar korrekt antal anslutna användare
     *
     */
    public void updateTitle() {
        this.frame.setTitle("Server: " + serverSocket.getInetAddress().getHostAddress() +  " Users connected: " + userList.size());
    }

    /** Tar strängkommandon från chatten och kopplar dessa till specifika metoder i serverklassen
     *
     * @param message meddelandet som innehåller kall till ett visst kommando
     */
    public void processCommand(String message) {
        if(message.contains("/kick")) {
            message = message.replace("/kick ", "");
            terminate(message);
        }

        if(message.equals("/shutdown")) {
            shutdown();
        }
    }
}
