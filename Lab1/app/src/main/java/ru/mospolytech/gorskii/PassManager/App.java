package ru.mospolytech.gorskii.PassManager;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import ru.mospolytech.gorskii.PassManager.ui.AuthFrame;
import ru.mospolytech.gorskii.PassManager.util.Entry;

/**
 * Основной класс приложения
 *
 * @author Горский Владислав, группа 221-3210
 */
public class App {

    public static ArrayList<Entry> ENTRY_LIST = new ArrayList<>();

    public static void main(String[] args) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(AuthFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(() -> {
            new AuthFrame().setVisible(true);
        });
    }

    /**
     * Считывает и расшифровывает данные из файла {@code data.txt},
     * расположенного в {@code user.home}
     *
     * @return массив записей из файла с расшифрованным первым слоем
     */
    public static ArrayList<Entry> getEntriesFromFile(String pinCode) {
        ArrayList<Entry> result = new ArrayList<>();

        File dbFile = Paths.get(System.getProperty("user.home"), "data.txt").toFile();
        try {
            FileInputStream fis = new FileInputStream(dbFile);
            try {
                byte[] data = HexFormat.of().parseHex(new String(fis.readAllBytes()));
                System.out.println("Read:\n" + HexFormat.of().formatHex(data));
                fis.close();
                try {
                    byte[] decodedPinBytes = HexFormat.of().parseHex(pinCode);
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] hashedPin = md.digest(decodedPinBytes);
                    //System.out.println(HexFormat.of().formatHex(hashedPin));

                    SecretKeySpec pinKey = new SecretKeySpec(hashedPin, "AES");
                    byte[] ivBytes = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F};
                    IvParameterSpec iv = new IvParameterSpec(ivBytes);

                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    try {
                        cipher.init(Cipher.DECRYPT_MODE, pinKey, iv);
                        byte[] decryptedData = new byte[cipher.getOutputSize(data.length)];
                        try {
                            int finalLength = cipher.doFinal(data, 0, data.length, decryptedData);

                            String decryptedString = new String(decryptedData).substring(0, finalLength);
                            System.out.println("Decrypted:\n" + decryptedString);

                            Gson jsonToolkit = new Gson();
                            JsonElement credentials = jsonToolkit.fromJson(decryptedString, JsonElement.class);
                            for (JsonElement element : credentials.getAsJsonArray()) {
                                result.add(jsonToolkit.fromJson(element, Entry.class));
                            }
                        } catch (ShortBufferException ex) {
                            Logger.getLogger(App.class.getName()).log(Level.SEVERE, "Buffer size is not enough", ex);
                        } catch (IllegalBlockSizeException ex) {
                            Logger.getLogger(App.class.getName()).log(Level.SEVERE, "Invalid block size", ex);
                        } catch (BadPaddingException ex) {
                            Logger.getLogger(App.class.getName()).log(Level.SEVERE, "Padding algorithm mismatch", ex);
                        }
                    } catch (InvalidKeyException ex) {
                        Logger.getLogger(App.class.getName()).log(Level.SEVERE, "Invalid key", ex);
                    } catch (InvalidAlgorithmParameterException ex) {
                        Logger.getLogger(App.class.getName()).log(Level.SEVERE, "Invalid algorithm parameters", ex);
                    }
                } catch (NoSuchPaddingException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, "This JVM doesn't support PKCS#5 padding", ex);
                } catch (NoSuchAlgorithmException ex) {
                    Logger.getLogger(App.class.getName()).log(Level.SEVERE, "This JVM doesn't support the AES algorithm", ex);
                }
            } catch (IOException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, "I/O error", ex);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, "File not found", ex);
        }
        return result;
    }
}
