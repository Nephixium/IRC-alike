package com.ircalike;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;

/** Ett grafiskt chatt gränssnitt som tillåter en klient eller användare att kommunicera med andra personer via en server genom att skicka meddelanden med ett PrintWriter objekt som använder användaren eller klientens strömmar via Socket.
 * Chatten innehåller ett meddelandefält för att skicka meddelanden till servern, ett output fält som visar meddelanden från servern och ett användarfält som visar inloggade klienter.
 * Meddelanden som visas i output och användarlistan som visas i användarfältet uppdateras med hjälp av två metoder i klaseen - appendChat och appendMemberList. Båda dessa tar emot meddelanden från ett klient objekt som lyssnar efter meddelanden från servern.
 * @author Nephixium
 * @version 1.0
 */

// Git-testing

public class Chat implements Runnable {

    private JFrame frame;
    private JTextArea chatArea;
    private JTextArea userArea;
    private JTextArea messageArea;
    private GridBagConstraints cc;
    private Socket clientSocket;
    private PrintWriter writer;
    private String loginUsername;
    private String loginPassword;

    /***
     * @param socket klienten eller användarens socket objekt
     * @param loginUsername klienten eller användarens användarnamn
     * @param password klienten eller användarens lösenord
     */
    public Chat(Socket socket, String loginUsername, String password) {
        this.clientSocket = socket;
        this.loginUsername = loginUsername;
        this.loginPassword = password;
    }

    /** Skapar ett grafiskt chatt-gränssnitt och skickar ett första meddelande till servern att registrera användaren eller klienten innan den fysiska chatten visas för den.
     *
     */
    @Override
    public void run() {
        this.frame = new JFrame("Chat window");
        this.frame.setPreferredSize(new Dimension(800,400));
        this.frame.addWindowListener(new WindowAdapter() {

            /** Skickar ett meddelande till servern att utloggning önskas, vilket i sin tur stänger socketen och avslutar sedan programmet.
             *
             * @param e ett stängt fönster
             */
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Shutting down..");
                writer.println("/logout");
                System.exit(0);
            }
        });

        createElements(this.frame.getContentPane());
        this.frame.setLocationRelativeTo(null);
        this.frame.pack();

        try {
            this.writer = new PrintWriter(this.clientSocket.getOutputStream(),true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        writer.println("/updateuserlist" + this.loginUsername + "/" + this.loginPassword);
        this.frame.setVisible(true);
    }

    /** Skapar alla element i det grafiska gränssnittet innehållandes tre textfält och en knapp med individuella metoder.
     * Gränssnittets layout är en GridBagLayout och använder GridBagConstraints för att justera position, storlek och centrering av olika delar.
     * Begränsningarna i GridBagConstraints skapas med metoden setConstraints.
     * @param container wrapper för gränssnittet
     */
    private void createElements(Container container) {
        GridBagLayout chatLayout = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        this.cc = new GridBagConstraints();
        container.setLayout(chatLayout);

        SimpleAttributeSet set = new SimpleAttributeSet();
        StyleConstants.setForeground(set, Color.green);

        this.frame.setResizable(false);
        this.chatArea = new JTextArea();
            chatArea.setBackground(Color.darkGray);
            chatArea.setForeground(Color.green);
            chatArea.setEditable(false);
            chatArea.setLineWrap(true);
            chatArea.setFont(new Font("Verdana", Font.PLAIN, 10));
            chatArea.setMargin(new Insets(5,5,5,5));
        this.userArea = new JTextArea();
            userArea.setBackground(Color.darkGray);
            userArea.setForeground(Color.green);
            userArea.setEditable(false);
            userArea.setLineWrap(true);
            userArea.setFont(new Font("Verdana", Font.BOLD, 9));
            userArea.setMargin(new Insets(5,5,5,5));
        this.messageArea = new JTextArea();
            messageArea.setBackground(Color.darkGray);
            messageArea.setForeground(Color.green);
            messageArea.setLineWrap(true);
            messageArea.setFont(new Font("Verdana", Font.PLAIN, 10));
            messageArea.setMargin(new Insets(5,5,5,5));
            messageArea.addKeyListener(new KeyListener() {
                @Override
                public void keyTyped(KeyEvent e) {

                }

                /** Skickar ett meddelande med metoden sendMessage() innehållandes den text som finns i textfältet messageArea om ENTER tangenten trycks in.
                 * e.consume() tar därefter bort den tidigare införda händelsen (knapptrycket) och den tomrad som det annars skapar.
                 * Meddelandefältet messageArea töms därefter på all text.
                 * @param e ett knapptryck
                 */
                @Override
                public void keyPressed(KeyEvent e) {
                    if(e.getKeyCode() == KeyEvent.VK_ENTER) {
                        sendMessage();
                        e.consume();
                        messageArea.setText("");
                    }
                }

                @Override
                public void keyReleased(KeyEvent e) {

                }
            });

        JScrollPane messageScroll = new JScrollPane(messageArea);
            messageScroll.setPreferredSize(new Dimension(600, 50));
        JScrollPane userScroll = new JScrollPane(userArea);
            userScroll.setPreferredSize(new Dimension(110, 200));
        JScrollPane chatScroll = new JScrollPane(chatArea);
            chatScroll.setPreferredSize(new Dimension(600, 300));

        JButton messageButton = new JButton("Send");
            messageButton.setPreferredSize(new Dimension(70,20));
            messageButton.addActionListener(new ActionListener() {
                /** Skickar ett meddelande med metoden sendMessage() innehållandes texten i messageArea när knappen messageButton trycks in och rensar därefter fältet.
                 *
                 * @param e ett knapptryck
                 */
                @Override
                public void actionPerformed(ActionEvent e) {
                    sendMessage();
                    messageArea.setText("");
                }});

        setConstraints(0,0,1, 18, 0,0,0,0);
        container.add(chatScroll, cc);

        setConstraints(1,0,3, 10, 0,5,0,0);
        container.add(userScroll, cc);

        setConstraints(1,1,0, 10, 0,5,0,0);
        container.add(messageButton, cc);

        setConstraints(0,1,2, 18, 5,0,0,0);
        container.add(messageScroll, cc);
    }

    /** Justerar GridBagConstraints objektet som används för att sätta begränsningar och justeringar på separata element i gränssnittet.
     *
     * @param xPosition elementets position i X led
     * @param yPosition elementets position i Y led
     * @param fillDirection fyllnadsriktning
     * @param anchorPoint ankarläge, exempelvis CENTER = 10
     * @param topInset indrag från toppen av elementet
     * @param leftInset indrag från vänster sida av elementet
     * @param bottomInset indrag från botten av elementet
     * @param rightInset indrag från höger sida av elementet
     */
    public void setConstraints(int xPosition, int yPosition, int fillDirection, int anchorPoint, int topInset, int leftInset, int bottomInset, int rightInset) {
        this.cc.gridx = xPosition;
        this.cc.gridy = yPosition;
        this.cc.fill = fillDirection;
        this.cc.anchor = anchorPoint;
        this.cc.insets.set(topInset, leftInset, bottomInset, rightInset);
    }

    /** Uppdaterar chattfältet chatArea med ett mottaget meddelande från servern.
     *
     * @param message ett meddelande från servern som tagits emot via en användare eller klient
     */
    public void appendChat(String message) {
        this.chatArea.append(message);
        this.chatArea.append("\n\n");
    }

    /** Uppdaterar användar- eller klient listan med en sträng array mottagen från servern via en klient eller användare efter formatering.
     *
     * @param message en sträng array mottagen och vidarebefordrad av en klient eller användare.
     */
    public void appendMemberList(String message) {
        this.userArea.setText("");
        message = message.replace("/updateuserlist", "");
        message = message.replace("[", "");
        message = message.replace("]", "");
        message = message.replace(",", "\n");

        this.userArea.setText(message);
    }

    /** Skickar ett meddelande till servern innehållandes texten i messageArea textfältet.
     *
     */
    public void sendMessage() {
        writer.println(messageArea.getText());
    }

    /** Stänger chatten genom att rensa bort de resurser den tagit upp och startar upp ett Login-objekt som visar inloggningsskärmen.
     *
     */
    public void closeChat() {
        this.frame.dispose();
        new Thread(new Login()).start();
    }
}
