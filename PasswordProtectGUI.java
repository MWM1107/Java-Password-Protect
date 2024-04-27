import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.*;
import java.io.FileReader;
import com.opencsv.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.awt.FlowLayout;


public class PasswordProtectGUI extends JFrame {
    private JTextArea outputTextArea;

    public PasswordProtectGUI() {
        setTitle("Password Strength Checker");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new FlowLayout()); // Set FlowLayout for side-by-side arrangement
    
        outputTextArea = new JTextArea(20, 50);
        outputTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputTextArea);
        mainPanel.add(scrollPane);
    
        JButton uploadButton = new JButton("Upload CSV File");
        uploadButton.addActionListener(e -> chooseCSVFile());
        mainPanel.add(uploadButton);
    
        JButton generateButton = new JButton("Generate Strong Password");
        generateButton.addActionListener(e -> generatePassword());
        mainPanel.add(generateButton);
    
        add(mainPanel);
    }
    
    

    private void chooseCSVFile() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files", "csv");
        fileChooser.setFileFilter(filter);
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            String selectedFile = fileChooser.getSelectedFile().getPath();
            passwordStrengthChecker(selectedFile);
        }
    }

    private void generatePassword() {
        String generatedPassword = randomPasswordGenerator(16); // Change 16 to desired length
        outputTextArea.setText("Generated Strong Password:\n" + generatedPassword);
    }

    private void passwordStrengthChecker(String selectedFile) {
        outputTextArea.setText(""); // Clear previous output
        Map<String, Integer> passwordCounts = new HashMap<>();
    
        try (CSVReader csvReader = new CSVReader(new FileReader(selectedFile))) {
            List<String[]> allData = csvReader.readAll();
    
            int passwordColumnIndex = getPasswordColumnIndex(allData.get(0)); // Get password column index from the header
    
            for (String[] row : allData) {
                if (passwordColumnIndex >= 0 && passwordColumnIndex < row.length) {
                    String password = row[passwordColumnIndex].trim();
                    passwordCounts.put(password, passwordCounts.getOrDefault(password, 0) + 1);
    
                    if (isPasswordPwned(password)) {
                        outputTextArea.append("The password: " + password + " has been exposed in a data breach. \n");
                    }
                }
            }
    
            for (Map.Entry<String, Integer> entry : passwordCounts.entrySet()) {
                String password = entry.getKey();
                int count = entry.getValue();
                int length = password.length();
    
                outputTextArea.append("The password: " + password + " occurs " + count + " time and is " + length + " characters long. Which is considered");
    
                if (length > 12) {
                    outputTextArea.append(" an Excellent password length! \n");
                } else if (length > 10) {
                    outputTextArea.append(" a Good password length! \n");
                } else if (length > 8) {
                    outputTextArea.append(" an Average password length! \n");
                } else if (length < 6) {
                    outputTextArea.append(" a Bad password length! \n");
                } else if (length < 4) {
                    outputTextArea.append(" a Horrible password length! \n");
                }
    
                if (containsUppercase(password) && containsLowercase(password) && containsNumber(password) && containsSpecialCharacter(password)) {
                    outputTextArea.append("Additionally, this password contains a combination of uppercase and lowercase letters plus numbers, and or special characters. \n");
                } else {
                    outputTextArea.append("However, this password does not contain a combination of uppercase and lowercase letters plus numbers, and or special characters. \n");
                }
    
                if (count > 2) {
                    outputTextArea.append("You are reusing this password. Please change it to a unique one.\n");
                } else {
                    outputTextArea.append("Good Job! You are only using this password once.\n");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Method to determine the password column index based on certain criteria
    private int getPasswordColumnIndex(String[] header) {
        for (int i = 0; i < header.length; i++) {
            String columnHeader = header[i].toLowerCase().trim();
            if (columnHeader.contains("password")) {
                return i;
            }
        }
        return -1; // Password column not found
    }    

    private String randomPasswordGenerator(int n) {
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "0123456789" + "!?@#$%&" + "abcdefghijklmnopqrstuvwxyz";

        StringBuilder strongPassword = new StringBuilder();

        for (int i = 0; i < n; i++) {
            int index = (int) (AlphaNumericString.length() * Math.random());
            strongPassword.append(AlphaNumericString.charAt(index));
        }

        return strongPassword.toString();
    }

    private static boolean containsUppercase(String str) {
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch >= 'A' && ch <= 'Z') {
                return true;
            }
        }
        return false;
    }

    private static boolean containsLowercase(String str) {
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch >= 'a' && ch <= 'z') {
                return true;
            }
        }
        return false;
    }

    private static boolean containsNumber(String str) {
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (ch >= '0' && ch <= '9') {
                return true;
            }
        }
        return false;
    }

    private static boolean containsSpecialCharacter(String str) {
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (!((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9'))) {
                return true;
            }
        }
        return false;
    }

    private boolean isPasswordPwned(String password) {
        try {
            String hash = sha1(password);
            String prefix = hash.substring(0, 5);
            String suffix = hash.substring(5);

            URL url = new URL("https://api.pwnedpasswords.com/range/" + prefix);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "Java client");
            int responseCode = con.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                return response.toString().contains(suffix.toUpperCase());
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String sha1(String input) throws Exception {
        java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-1");
        digest.update(input.getBytes("UTF-8"));
        byte[] hash = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString().toUpperCase();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PasswordProtectGUI().setVisible(true));
    }
}