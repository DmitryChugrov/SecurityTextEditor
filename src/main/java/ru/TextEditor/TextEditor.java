package ru.TextEditor;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.SortedMap;

public class TextEditor extends JFrame {
    private JTextArea textArea;
    private JFileChooser fileChooser;
    private JComboBox<String> encodingComboBox;
    private String internalBuffer = "";

    public TextEditor() {
        setTitle("Защищенный текстовый редактор");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (confirmClose()) {
                    System.exit(0);
                }
            }
        });

        textArea = new JTextArea();
        textArea.setTransferHandler(null); // Disable default copy-paste
        JScrollPane scrollPane = new JScrollPane(textArea);
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("Файл");
        menuBar.add(fileMenu);

        JMenuItem openItem = new JMenuItem("Открыть");
        openItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                openFile();
            }
        });
        fileMenu.add(openItem);

        JMenuItem saveItem = new JMenuItem("Сохранить");
        saveItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveFile();
            }
        });
        fileMenu.add(saveItem);

        JMenuItem exitItem = new JMenuItem("Выход");
        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (confirmClose()) {
                    System.exit(0);
                }
            }
        });
        fileMenu.add(exitItem);

        JMenu editMenu = new JMenu("Редактировать");
        menuBar.add(editMenu);

        JMenuItem copyItem = new JMenuItem("Копировать");
        copyItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyText();
            }
        });
        editMenu.add(copyItem);

        JMenuItem pasteItem = new JMenuItem("Вставить");
        pasteItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pasteText();
            }
        });
        editMenu.add(pasteItem);

        fileChooser = new JFileChooser();

        encodingComboBox = new JComboBox<>();
        SortedMap<String, Charset> availableCharsets = Charset.availableCharsets();
        for (String charsetName : availableCharsets.keySet()) {
            encodingComboBox.addItem(charsetName);
        }
        encodingComboBox.setSelectedItem(StandardCharsets.UTF_8.name());

        JPanel panel = new JPanel();
        panel.add(new JLabel("Кодировка:"));
        panel.add(encodingComboBox);
        getContentPane().add(panel, BorderLayout.NORTH);
    }

    private boolean confirmClose() {
        if (!textArea.getText().isEmpty()) {
            int option = JOptionPane.showConfirmDialog(this,
                    "Сохранить изменения перед выходом?",
                    "Подтверждение выхода", JOptionPane.YES_NO_CANCEL_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                saveFile();
                return true;
            } else if (option == JOptionPane.NO_OPTION) {
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    private void openFile() {
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String selectedEncoding = (String) encodingComboBox.getSelectedItem();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(selectedFile), selectedEncoding))) {
                StringBuilder text = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    text.append(line).append("\n");
                }
                textArea.setText(text.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveFile() {
        int returnValue = fileChooser.showSaveDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String selectedEncoding = (String) encodingComboBox.getSelectedItem();
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(selectedFile), selectedEncoding))) {
                writer.write(textArea.getText());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void copyText() {
        internalBuffer = textArea.getSelectedText();
    }

    private void pasteText() {
        if (internalBuffer != null && !internalBuffer.isEmpty()) {
            int pos = textArea.getCaretPosition();
            textArea.insert(internalBuffer, pos);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                TextEditor textEditor = new TextEditor();
                textEditor.setVisible(true);
                Monitor monitor = new Monitor();
                monitor.start();
            }
        });
    }
}

