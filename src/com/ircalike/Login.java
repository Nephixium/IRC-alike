package com.ircalike;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Ett grafiskt gränssnitt som hanterar inloggningen på en angiven server med port eller skapandet av en ny server.
 * Klassen tillhandahåller möjligheten att välja integreringen av en databas som kopplas tillsammans med skapandet av en instans av Server-klassen.
 * @author Nephixium
 * @version 1.0
 */

public class Login implements Runnable {
    private JFrame frame;
    private GridBagConstraints cc;

    /** Skapar alla element som gränssnittet innehåller och visar sedan gränssnittet för användaren.
     *
     */
    @Override
    public void run() {
        this.frame = new JFrame("Chatserver login");
        this.frame.setPreferredSize(new Dimension(480,480));

        this.frame.addWindowListener(new WindowAdapter() {

            /** Stänger fönstret och avslutar programmet */
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Shutting down..");
                System.exit(0);
            }
        });

        createElements(this.frame.getContentPane());
        this.frame.pack();
        this.frame.setLocationRelativeTo(null);
        this.frame.setVisible(true);
    }

    /** Skapar alla element som gränssnittet innehåller i en GridBagLayout där element placeras enligt metoden setConstraints som anger begränsningar och specifik placering per element.
     *
     * @param container wrapper för gränssnittets element
     */
    private void createElements(Container container) {

        GridBagLayout loginLayout = new GridBagLayout();
        /** Innehåller de begränsningar som appliceras på grafiska element */
        this.cc = new GridBagConstraints();
        container.setLayout(loginLayout);

        JTextField usernameField = new JTextField(10);
        JTextField passwordField = new JTextField(10);
        JTextField hostField = new JTextField(7);
        JTextField hostportField = new JTextField(7);

        JLabel usernameTip = new JLabel();
            usernameTip.setText("Min: 3 - 14 characters");
            usernameTip.setFont((new Font("Arial", Font.PLAIN, 8)));
        JLabel passwordTip = new JLabel();
            passwordTip.setText("Min: 8 - 20 characters");
            passwordTip.setFont((new Font("Arial", Font.PLAIN, 8)));

        JButton joinButton = new JButton("Join server");
            joinButton.setPreferredSize(new Dimension(100,30));
        JButton hostButton = new JButton("Host server");
            hostButton.setPreferredSize(new Dimension(100,30));

        /** Checkbox som används för att välja om en databas skall kopplas eller ej */
        JCheckBox databaseCheck = new JCheckBox();

        setConstraints(0, 1, 2, 21, 0, 0, 0, 0);
        container.add(new JLabel("Username: "), cc);

        setConstraints(1, 1, 2, 21, 0, 5, 0, 0);
        container.add(usernameField, cc);

        setConstraints(1, 0, 2, 21, 0, 5, 3, 0);
        container.add(usernameTip, cc);

        setConstraints(2, 1, 2, 21, 0, 5, 0, 0);
        container.add(new JLabel("Password: "), cc);

        setConstraints(3, 1, 2, 21, 0, 5, 0, 0);
        container.add(passwordField, cc);

        setConstraints(3, 0, 2, 21, 0, 5, 3, 0);
        container.add(passwordTip, cc);

        setConstraints(0, 4, 2, 21, 50, 10, 0, 0);
        container.add(new JLabel("Host: "), cc);

        setConstraints(1, 4, 0, 21, 50, 20, 0, 0);
        container.add(hostField, cc);

        setConstraints(0, 5, 2, 21, 5, 10, 0, 0);
        container.add(new JLabel("Port: "), cc);

        setConstraints(1, 5, 0, 21, 5, 20, 0, 0);
        container.add(hostportField, cc);

        setConstraints(1, 3, 0, 10, 15, 5, 0, 0);
        container.add(joinButton, cc);

        joinButton.addActionListener(new ActionListener() {
            /** Går med i en server om alla fält är ifyllda enligt if-satser som kontrollerar dessa.
             *
             * @param e ett knapptryck
             */
            @Override
            public void actionPerformed(ActionEvent e) {

                // Checks whether the username field is within the expected number of characters and does not contain any whitespace
                /** Kontrollerar att användarnamnet är inom önskad längd och ej innehåller whitespace eller plustecken.
                 * Om användarnamnet är felaktigt formaterat, visas ett felmeddelande.
                 */
                if(!(usernameField.getText().length() >= 3) || !(usernameField.getText().length() <= 14) || (usernameField.getText() == null) || usernameField.getText().matches(".*\\s.*") || usernameField.getText().contains("+")) {
                    new JOptionPane().showMessageDialog(null, "Username must be between 3 and 14 characters long without whitespace or '+' character!");
                }

                /** Kontrollerar att lösenordet är inom önskad längd utan whitespace.
                 * Om lösenordet är felaktigt formaterat, visas ett felmeddelande.
                 */
                else if(!(passwordField.getText().length() >= 8) || !(passwordField.getText().length() <= 20) || (passwordField.getText() == null) || passwordField.getText().matches(".*\\s.*")) {
                    new JOptionPane().showMessageDialog(null, "Password must be between 8 and 20 characters long and contain no whitespace!");
                }

                /** Kontrollerar att hostportField fältet endast innehåller siffror och är inom intervallet 1-65535.
                 * Om allt är korrekt påbörjas en anslutning mot en server med port, användarnamn och lösenord som parametrar.
                 * Fönstrets resurser rensas därefter med dispose.
                 */
                else if(hostportField.getText().matches("\\d+") && Integer.parseInt(hostportField.getText()) <= 65535 && Integer.parseInt(hostportField.getText()) >= 1) {

                    new Thread(new Client(hostField.getText(), Integer.parseInt(hostportField.getText()), usernameField.getText(), passwordField.getText())).start();
                    frame.dispose();

                } else {
                    new JOptionPane().showMessageDialog(null, "Invalid port range");
                }
            }
        });

        setConstraints(1, 6, 0, 10, 15, 5, 0, 0);
        container.add(hostButton, cc);

        hostButton.addActionListener(new ActionListener() {

            /** Startar en server med parametrarna port, som måste vara inom intervallet 1-65535 (true för koppling av databas, false för att köra utan databas) som fås genom ifyllning av checkboxen i gränssnittet.
             *
             * @param e ett knapptryck
             */
            @Override
            public void actionPerformed(ActionEvent e) {

                if(hostportField.getText().matches("\\d+") && Integer.parseInt(hostportField.getText()) <= 65535 && Integer.parseInt(hostportField.getText()) >= 1) {
                    if(databaseCheck.isSelected()) {
                        new Thread(new Server((Integer.parseInt(hostportField.getText())), true)).start();
                        frame.dispose();

                    } else {
                        new Thread(new Server((Integer.parseInt(hostportField.getText())), false)).start();
                        frame.dispose();
                    }
                }
            }
        });

        setConstraints(3, 5, 0, 10, 15, 5, 0, 0);
        container.add(new JLabel("Database setup"), cc);

        setConstraints(3, 6, 0, 10, 15, 5, 0, 0);
        container.add(databaseCheck, cc);

    }

    /** Justerar position på elementen i det grafiska gränssnitten via parametrarna och de ändringar dessa tillför på GridBagConstraints objektet
     *
     * @param xPosition position x-led, det vill säga kolumnen som elementet placeras i
     * @param yPosition position i y-led, det vill säga vilken rad elementet placeras i
     * @param fillDirection fyllnadsriktning på elementet, exempelvis 0 (GridBagConstraints.NONE)
     * @param anchorPoint hur elementet skall placeras inom x och y led, exempelvis centrerat (10) (GridBagConstraints.CENTER)
     * @param topInset hur mycket indrag det skall finnas från toppen av elementet
     * @param leftInset hur mycket indrag det skall finnas från vänster sida av elementet
     * @param bottomInset hur mycket indrag det skall finnas från botten av elementet
     * @param rightInset hur mycket indrag det skall finnas från höger sida av elementet
     */
    public void setConstraints(int xPosition, int yPosition, int fillDirection, int anchorPoint, int topInset, int leftInset, int bottomInset, int rightInset) {
        this.cc.gridx = xPosition;
        this.cc.gridy = yPosition;
        this.cc.fill = fillDirection;
        this.cc.anchor = anchorPoint;
        this.cc.insets.set(topInset, leftInset, bottomInset, rightInset);
    }

    public static void main(String[] args) {
        new Thread(new Login()).start();
    }
}
