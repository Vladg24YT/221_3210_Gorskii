package ru.mospolytech.gorskii.PassManager.ui;

import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import ru.mospolytech.gorskii.PassManager.App;
import ru.mospolytech.gorskii.PassManager.util.Entry;

/**
 * Основное окно приложения
 *
 * @author Горский Владислав, группа 221-3210
 */
public class AppFrame extends javax.swing.JFrame {

    /**
     * Creates new form AppFrame
     */
    public AppFrame() {
        initComponents();

        this.renderEntries();

        this.searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                this.onUpdate(e);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                this.onUpdate(e);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                this.onUpdate(e);
            }

            private void onUpdate(DocumentEvent docEvent) {
                renderEntries();
            }
        });
    }

    /**
     * Обновляет список записей в графическом интерфейсе
     */
    private void renderEntries() {
        String filterString = this.searchField.getText();

        this.rootPanel.removeAll();

        GroupLayout layoutManager = new GroupLayout(this.rootPanel);
        this.rootPanel.setLayout(layoutManager);
        layoutManager.setAutoCreateContainerGaps(true);
        layoutManager.setAutoCreateGaps(true);
        ParallelGroup horizontalGroup = layoutManager.createParallelGroup(GroupLayout.Alignment.LEADING);
        SequentialGroup verticalGroup = layoutManager.createSequentialGroup();

        for (Entry entry : App.ENTRY_LIST) {
            if (entry.site.startsWith(filterString)) {

                JPanel temp = this.createNewEntryPanel(entry.site, entry.login, entry.pass);
                JSeparator tempSep = new JSeparator();
                tempSep.setOrientation(JSeparator.HORIZONTAL);

                verticalGroup.addComponent(temp, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
                verticalGroup.addComponent(tempSep, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
                horizontalGroup.addComponent(temp);
                horizontalGroup.addComponent(tempSep);
            }
        }

        layoutManager.setHorizontalGroup(horizontalGroup);
        layoutManager.setVerticalGroup(verticalGroup);

        this.repaint();
    }

    /**
     * Создаёт новую панель для отображения информации о записи в базе данных
     *
     * @param site вебсайт
     * @param login логин пользователя
     * @param pass пароль пользователя
     * @return созданную панель
     */
    private JPanel createNewEntryPanel(String site, String login, String pass) {
        JPanel localRoot = new JPanel();

        GroupLayout layoutManager = new GroupLayout(localRoot);
        localRoot.setLayout(layoutManager);
        layoutManager.setAutoCreateContainerGaps(true);
        layoutManager.setAutoCreateGaps(true);

        JLabel tempSiteLabel = new JLabel(site);
        tempSiteLabel.setFont(tempSiteLabel.getFont().deriveFont(Font.ITALIC));

        JLabel tempLoginLabel = new JLabel("Логин:");
        JPasswordField tempLoginField = new JPasswordField(login);
        tempLoginField.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String login = new String(tempLoginField.getPassword());
                String inputPin = JOptionPane.showInputDialog(localRoot.getParent(), "Введите пин-код", "Проверка личности", JOptionPane.QUESTION_MESSAGE);
                try {
                    byte[] pinBytes = inputPin.getBytes();
                    byte[] loginBytes = HexFormat.of().parseHex(login);

                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] hashedPin = md.digest(pinBytes);

                    SecretKeySpec pinKey = new SecretKeySpec(hashedPin, "AES");
                    byte[] ivBytes = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F};
                    IvParameterSpec iv = new IvParameterSpec(ivBytes);

                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    cipher.init(Cipher.DECRYPT_MODE, pinKey, iv);
                    byte[] decryptedData = new byte[cipher.getOutputSize(pinBytes.length)];
                    int finalLength = cipher.doFinal(loginBytes, 0, loginBytes.length, decryptedData);

                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(new String(decryptedData).substring(0, finalLength)), null);
                    JOptionPane.showConfirmDialog(localRoot.getParent(), "Логин скопирован в буфер обмена", "Успешно", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE);
                } catch (NullPointerException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException | NoSuchAlgorithmException | ShortBufferException | IllegalBlockSizeException | BadPaddingException ex) {
                    JOptionPane.showConfirmDialog(localRoot.getParent(), "Неверный пин-код", "Ошибка", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });
        tempLoginField.setEditable(false);

        JLabel tempPassLabel = new JLabel("Пароль:");
        JPasswordField tempPassField = new JPasswordField(pass);
        tempPassField.setEditable(false);
        tempPassField.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String password = new String(tempPassField.getPassword());
                String inputPin = JOptionPane.showInputDialog(localRoot.getParent(), "Введите пин-код", "Проверка личности", JOptionPane.QUESTION_MESSAGE);
                try {
                    byte[] pinBytes = inputPin.getBytes();
                    byte[] passwordBytes = HexFormat.of().parseHex(password);

                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] hashedPin = md.digest(pinBytes);

                    SecretKeySpec pinKey = new SecretKeySpec(hashedPin, "AES");
                    byte[] ivBytes = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F};
                    IvParameterSpec iv = new IvParameterSpec(ivBytes);

                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    cipher.init(Cipher.DECRYPT_MODE, pinKey, iv);
                    byte[] decryptedData = new byte[cipher.getOutputSize(pinBytes.length)];
                    int finalLength = cipher.doFinal(passwordBytes, 0, passwordBytes.length, decryptedData);

                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(new String(decryptedData).substring(0, finalLength)), null);
                    JOptionPane.showConfirmDialog(localRoot.getParent(), "Логин скопирован в буфер обмена", "Успешно", JOptionPane.OK_OPTION, JOptionPane.INFORMATION_MESSAGE);
                } catch (NullPointerException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException | NoSuchAlgorithmException | ShortBufferException | IllegalBlockSizeException | BadPaddingException ex) {
                    JOptionPane.showConfirmDialog(localRoot.getParent(), "Неверный пин-код", "Ошибка", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }
        });

        layoutManager.setHorizontalGroup(layoutManager.createSequentialGroup()
                .addComponent(tempSiteLabel)
                .addComponent(tempLoginLabel)
                .addComponent(tempLoginField)
                .addComponent(tempPassLabel)
                .addComponent(tempPassField)
        );
        layoutManager.setVerticalGroup(layoutManager.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(tempSiteLabel)
                .addComponent(tempLoginLabel)
                .addComponent(tempLoginField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addComponent(tempPassLabel)
                .addComponent(tempPassField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
        );

        return localRoot;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        searchField = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        rootPanel = new javax.swing.JPanel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Password Manager");

        searchField.setToolTipText("");

        javax.swing.GroupLayout rootPanelLayout = new javax.swing.GroupLayout(rootPanel);
        rootPanel.setLayout(rootPanelLayout);
        rootPanelLayout.setHorizontalGroup(
            rootPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 386, Short.MAX_VALUE)
        );
        rootPanelLayout.setVerticalGroup(
            rootPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 216, Short.MAX_VALUE)
        );

        jScrollPane2.setViewportView(rootPanel);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(searchField, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(searchField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPanel rootPanel;
    private javax.swing.JTextField searchField;
    // End of variables declaration//GEN-END:variables
}
