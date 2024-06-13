package org.tzr_redstone;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

public class SSBU_Mod_Manager {

    private JFrame frame;
    private JTextField directoryField;
    private DefaultListModel<String> enabledModel = new DefaultListModel<>();
    private DefaultListModel<String> disabledModel = new DefaultListModel<>();

    private static final List<String> REQUIRED_FOLDERS = Arrays.asList("fighter", "camera", "effect", "sound", "stream", "ui");
    private static final String PREFS_KEY_LAST_DIRECTORY = "last_directory";

    private Preferences preferences;

    public static void main(String[] args) {
        // Set the FlatLaf look and feel
        FlatLightLaf.install();

        EventQueue.invokeLater(() -> {
            try {
                SSBU_Mod_Manager window = new SSBU_Mod_Manager();
                window.frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public SSBU_Mod_Manager() {
        preferences = Preferences.userNodeForPackage(SSBU_Mod_Manager.class);
        initialize();
    }

    private void initialize() {
        frame = new JFrame("SSBU Mod Manager");
        frame.setBounds(100, 100, 600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        frame.getContentPane().add(topPanel, BorderLayout.NORTH);

        JButton btnChooseDirectory = new JButton("Choose Directory");
        topPanel.add(btnChooseDirectory, BorderLayout.WEST);

        directoryField = new JTextField();
        topPanel.add(directoryField, BorderLayout.CENTER);
        directoryField.setColumns(20);

        // Set the initial directory from preferences
        String lastDirectory = preferences.get(PREFS_KEY_LAST_DIRECTORY, "");
        if (!lastDirectory.isEmpty()) {
            directoryField.setText(lastDirectory);
        }

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new GridBagLayout());
        frame.getContentPane().add(centerPanel, BorderLayout.CENTER);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 0.5;
        gbc.weighty = 0.0;

        JLabel lblEnabled = new JLabel("Enabled");
        gbc.gridx = 0;
        gbc.gridy = 0;
        centerPanel.add(lblEnabled, gbc);

        JLabel lblDisabled = new JLabel("Disabled");
        gbc.gridx = 2;
        gbc.gridy = 0;
        centerPanel.add(lblDisabled, gbc);

        gbc.weighty = 1.0;

        JList<String> listEnabled = new JList<>(enabledModel);
        JScrollPane scrollEnabled = new JScrollPane(listEnabled);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridheight = 4;
        centerPanel.add(scrollEnabled, gbc);

        JList<String> listDisabled = new JList<>(disabledModel);
        JScrollPane scrollDisabled = new JScrollPane(listDisabled);
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.gridheight = 4;
        centerPanel.add(scrollDisabled, gbc);

        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;

        JPanel moveButtonsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcMoveButtons = new GridBagConstraints();
        gbcMoveButtons.insets = new Insets(5, 0, 5, 0);
        gbcMoveButtons.fill = GridBagConstraints.HORIZONTAL;
        gbcMoveButtons.weightx = 1.0;

        JButton btnMove = new JButton("Move >>>");
        gbcMoveButtons.gridy = 0;
        moveButtonsPanel.add(btnMove, gbcMoveButtons);

        JButton btnMoveAll = new JButton("Move All >>>");
        gbcMoveButtons.gridy = 1;
        moveButtonsPanel.add(btnMoveAll, gbcMoveButtons);

        gbcMoveButtons.gridy = 2;
        gbcMoveButtons.insets = new Insets(20, 0, 5, 0);  // Add space between button groups
        moveButtonsPanel.add(new JLabel(" "), gbcMoveButtons);  // Add a blank space for separation

        gbcMoveButtons.insets = new Insets(5, 0, 5, 0);

        JButton btnMoveBack = new JButton("<<< Move");
        gbcMoveButtons.gridy = 3;
        moveButtonsPanel.add(btnMoveBack, gbcMoveButtons);

        JButton btnMoveAllBack = new JButton("<<< Move All");
        gbcMoveButtons.gridy = 4;
        moveButtonsPanel.add(btnMoveAllBack, gbcMoveButtons);

        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridheight = 4;
        centerPanel.add(moveButtonsPanel, gbc);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

        JButton btnSave = new JButton("Save");
        bottomPanel.add(btnSave);

        btnChooseDirectory.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                String currentDirectory = directoryField.getText();
                if (!currentDirectory.isEmpty()) {
                    fileChooser.setCurrentDirectory(new File(currentDirectory));
                }
                int option = fileChooser.showOpenDialog(frame);
                if (option == JFileChooser.APPROVE_OPTION) {
                    String selectedDirectory = fileChooser.getSelectedFile().getAbsolutePath();
                    directoryField.setText(selectedDirectory);
                    preferences.put(PREFS_KEY_LAST_DIRECTORY, selectedDirectory);
                    scanDirectory();
                }
            }
        });

        btnMove.addActionListener(e -> moveSelectedItems(listEnabled, enabledModel, disabledModel));
        btnMoveAll.addActionListener(e -> moveAllItems(enabledModel, disabledModel));
        btnMoveBack.addActionListener(e -> moveSelectedItems(listDisabled, disabledModel, enabledModel));
        btnMoveAllBack.addActionListener(e -> moveAllItems(disabledModel, enabledModel));
        btnSave.addActionListener(e -> saveChanges());

        // Initial scan of directory on startup
        String initialDirectory = directoryField.getText();
        if (!initialDirectory.isEmpty()) {
            scanDirectory();
        }
    }

    private void scanDirectory() {
        enabledModel.clear();
        disabledModel.clear();
        String directoryPath = directoryField.getText();
        File directory = new File(directoryPath);

        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles(File::isDirectory);
            if (files != null) {
                for (File file : files) {
                    if (containsRequiredFolders(file)) {
                        enabledModel.addElement(file.getName());
                    }
                }
            }

            File disabledDir = new File(directory, "DISABLED");
            if (disabledDir.exists() && disabledDir.isDirectory()) {
                File[] disabledFiles = disabledDir.listFiles(File::isDirectory);
                if (disabledFiles != null) {
                    for (File file : disabledFiles) {
                        if (containsRequiredFolders(file)) {
                            disabledModel.addElement(file.getName());
                        }
                    }
                }
            }
        }
    }

    private boolean containsRequiredFolders(File file) {
        for (String requiredFolder : REQUIRED_FOLDERS) {
            if (new File(file, requiredFolder).exists()) {
                return true;
            }
        }
        return false;
    }

    private void moveSelectedItems(JList<String> list, DefaultListModel<String> fromModel, DefaultListModel<String> toModel) {
        List<String> selectedValues = list.getSelectedValuesList();
        for (String value : selectedValues) {
            fromModel.removeElement(value);
            toModel.addElement(value);
        }
    }

    private void moveAllItems(DefaultListModel<String> fromModel, DefaultListModel<String> toModel) {
        int size = fromModel.size();
        for (int i = 0; i < size; i++) {
            toModel.addElement(fromModel.getElementAt(i));
        }
        fromModel.clear();
    }

    private void saveChanges() {
        String directoryPath = directoryField.getText();
        File directory = new File(directoryPath);
        File disabledDir = new File(directory, "DISABLED");

        if (!disabledDir.exists()) {
            disabledDir.mkdir();
        }

        for (int i = 0; i < disabledModel.size(); i++) {
            String modName = disabledModel.getElementAt(i);
            File modDir = new File(directory, modName);
            File newLocation = new File(disabledDir, modName);
            if (modDir.exists()) {
                modDir.renameTo(newLocation);
            }
        }

        for (int i = 0; i < enabledModel.size(); i++) {
            String modName = enabledModel.getElementAt(i);
            File modDir = new File(disabledDir, modName);
            File newLocation = new File(directory, modName);
            if (modDir.exists()) {
                modDir.renameTo(newLocation);
            }
        }

        scanDirectory();
    }
}
