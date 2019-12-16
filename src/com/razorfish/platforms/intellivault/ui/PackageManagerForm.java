package com.razorfish.platforms.intellivault.ui;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.razorfish.platforms.intellivault.config.IntelliVaultCRXRepository;
import com.razorfish.platforms.intellivault.config.IntelliVaultPreferences;
import com.razorfish.platforms.intellivault.services.impl.IntelliVaultPreferencesService;
import com.razorfish.platforms.intellivault.services.tasks.AEMPackageCreateAndDownloadPackageTask;
import com.razorfish.platforms.intellivault.services.tasks.AEMUploadAndInstallPackageTask;
import com.razorfish.platforms.intellivault.ui.utils.StatusLine;

import java.io.File;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

public class PackageManagerForm extends DialogWrapper {

    private final static int COPY_ACTION = 0;
    private final static int DOWNLOAD_ACTION = 1;
    private final static int UPLOAD_ACTION = 2;

    private JPanel jMainPanel;
    private JComboBox comboSourceInstance;
    private JTextField txtFieldContentPath;
    private JButton btnAddPath;
    private JList listContentPaths;
    private JComboBox comboTargetInstance;
    private JLabel labelStatusLine;
    private JButton btnRemovePath;
    private JPanel panelPathsSelector;
    private JComboBox comboAction;
    private JPanel panelSourceInstance;
    private JPanel panelTargetInstance;
    private JPanel panelDownloadPath;
    private JTextField textDownloadPath;
    private JButton btnBrowseDownloadPath;
    private JTextField textPackageToUpload;
    private JButton btnBrowsePackageToUpload;
    private JPanel panelPackageUpload;
    private JLabel labelContentToMigrate;
    private JTextField txtPackageName;

    private Project project;

    private List<IntelliVaultCRXRepository> repoList;
    private String tmpPath;
    private static final Logger log = Logger.getInstance(PackageManagerForm.class);
    private DefaultListModel<String> contentPathsModel;

    private int selectedAction = 0;

    private StatusLine statusLine = new StatusLine(labelStatusLine);

    public PackageManagerForm(@Nullable Project project) {
        super(project);
        log.debug("Initialising package manager form.");

        setTitle("CRX Package Manager");

        this.project = project;

        //clear status line at the beggining
        statusLine.clear();

        //init configurations
        initConfiguration();

        //init fields
        initComboBoxes();
        initList();
        initButtons();
        initTextFieldAddPath();
        initActionChooser();

        prepareUiCopyAction();
        //init the rest of a dialog
        init();
    }

    @Override
    protected void doOKAction() {
        switch (selectedAction) {
            case COPY_ACTION:
                copyContent();
                break;
            case DOWNLOAD_ACTION:
                downloadPackage();
                break;
            case UPLOAD_ACTION:
                uploadPackage();
                break;
        }
    }

    private void downloadPackage() {
        try {
            validateDownload();
            final IntelliVaultCRXRepository sourceCrx = repoList.get(comboSourceInstance.getSelectedIndex());

            StringBuilder sb = new StringBuilder()
                    .append("<html>")
                    .append("<center>")
                    .append("You are building and downloading package from: ")
                    .append("<b>").append(sourceCrx.getName()).append("</b>").append("<br>")
                    .append("<br>")
                    .append("Please confirm your decision")
                    .append("</center>")
                    .append("</html>");

            final ConfirmationDialog confirmationDialog = new ConfirmationDialog(project, sb.toString(), "Download package", () -> {
                final AEMPackageCreateAndDownloadPackageTask task = new AEMPackageCreateAndDownloadPackageTask(sourceCrx, null, textDownloadPath.getText(), project, getPaths());
                task.setPackageName(txtPackageName.getText());
                ProgressManager.getInstance().run(task);
            }, null);
            confirmationDialog.show();
        } catch (RuntimeException e) {
            statusLine.setMessage(e.getMessage());
        }
    }

    private void validateDownload() throws RuntimeException {
        Validate.isTrue(comboSourceInstance.getSelectedIndex() >= 0, "Please select source instance");
        Validate.notEmpty(getPaths(), "Please provide at least one path");
        Validate.notEmpty(textDownloadPath.getText(), "Please provide download path");
        Validate.isTrue(new File(textDownloadPath.getText()).exists() && new File(textDownloadPath.getText()).isDirectory(), "Please provide valid download path");
        Validate.notEmpty(txtPackageName.getText(), "Please provide package name");
    }

    private void uploadPackage() {
        try {
            validateUpload();
            final IntelliVaultCRXRepository targetCrx = repoList.get(comboTargetInstance.getSelectedIndex());

            if (targetCrx.getInstanceType() == IntelliVaultCRXRepository.INSTANCE_TYPE_PROD) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showErrorDialog("Content write to production environments is not allowed!", "Forbidden operation");
                });
                return;
            }

            if (targetCrx.getInstanceType() == IntelliVaultCRXRepository.INSTANCE_TYPE_QA) {
                StringBuilder sb = new StringBuilder()
                        .append("<html>")
                        .append("<center>")
                        .append("<h3>Warning!</h3>").append("<br>")
                        .append("You are trying to write content on QA environment: ").append("<br>")
                        .append("<b>").append(targetCrx.getName() + " ( " + targetCrx.getRepoUrl() + " ) ").append("</b>").append("<br>")
                        .append("<br>")
                        .append("Please confirm your decision")
                        .append("</center>")
                        .append("</html>");

                final ConfirmationDialog confirmationDialog = new ConfirmationDialog(project, sb.toString(), "Write to QA", () -> {
                    executeUploadPackage(targetCrx);
                }, null);
                confirmationDialog.show();
                return;
            }

            executeUploadPackage(targetCrx);
        } catch (RuntimeException e) {
            statusLine.setMessage(e.getMessage());
        }
    }

    private void executeUploadPackage(final IntelliVaultCRXRepository targetCrx) {
        StringBuilder sb = new StringBuilder()
                .append("<html>")
                .append("<center>")
                .append("You are uploading and installing following package:").append("<br>")
                .append("<b>").append(textPackageToUpload.getText()).append("</b>").append("<br>")
                .append("to the instance: ").append("<br>")
                .append("<b>").append(targetCrx.getName() + " ( " + targetCrx.getRepoUrl() + " ) ").append("</b>").append("<br>")
                .append("<br>")
                .append("Please confirm your decision")
                .append("</center>")
                .append("</html>");

        final ConfirmationDialog confirmationDialog = new ConfirmationDialog(project, sb.toString(), "Install package", () -> {
            final AEMUploadAndInstallPackageTask task = new AEMUploadAndInstallPackageTask(targetCrx, textPackageToUpload.getText(), project);
            ProgressManager.getInstance().run(task);
        }, null);
        confirmationDialog.show();
    }

    private void validateUpload() throws RuntimeException {
        Validate.isTrue(comboTargetInstance.getSelectedIndex() >= 0, "Please select source instance");
        Validate.notEmpty(textPackageToUpload.getText(), "Please provide package to upload");
        Validate.isTrue(new File(textPackageToUpload.getText()).exists() && new File(textPackageToUpload.getText()).isFile(), "Please provide valid path for package to upload");
    }

    private void copyContent() {
        try {
            validateCopy();
            final IntelliVaultCRXRepository sourceCrx = repoList.get(comboSourceInstance.getSelectedIndex());
            final IntelliVaultCRXRepository targetCrx = repoList.get(comboTargetInstance.getSelectedIndex());


            if (targetCrx.getInstanceType() == IntelliVaultCRXRepository.INSTANCE_TYPE_PROD) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showErrorDialog("Content write to production environments is not allowed!", "Forbidden operation");
                });
                return;
            }

            if (targetCrx.getInstanceType() == IntelliVaultCRXRepository.INSTANCE_TYPE_QA) {
                StringBuilder sb = new StringBuilder()
                        .append("<html>")
                        .append("<center>")
                        .append("<h3>Warning!</h3>").append("<br>")
                        .append("You are trying to write content on QA environment: ").append("<br>")
                        .append("<b>").append(targetCrx.getName() + " ( " + targetCrx.getRepoUrl() + " ) ").append("</b>").append("<br>")
                        .append("<br>")
                        .append("Please confirm your decision")
                        .append("</center>")
                        .append("</html>");

                final ConfirmationDialog confirmationDialog = new ConfirmationDialog(project, sb.toString(), "Write to QA", () -> {
                    executeCopyContent(sourceCrx, targetCrx);
                }, null);
                confirmationDialog.show();
                return;
            }

            executeCopyContent(sourceCrx, targetCrx);
        } catch (RuntimeException e) {
            statusLine.setMessage(e.getMessage());
        }
    }

    private void executeCopyContent(final IntelliVaultCRXRepository sourceCrx, final IntelliVaultCRXRepository targetCrx) {
        StringBuilder sb = new StringBuilder()
                .append("<html>")
                .append("<center>").append("<h3>").append("Warning!").append("</h3>").append("<br>")
                .append("This action will override content on: ")
                .append("<b>").append(targetCrx.getName()).append("</b>").append("<br>")
                .append("With the content from: ")
                .append("<b>").append(sourceCrx.getName()).append("</b>").append("</br>")
                .append("<br>")
                .append("Please confirm your decision")
                .append("</center>")
                .append("</html>");

        final ConfirmationDialog confirmationDialog = new ConfirmationDialog(project, sb.toString(), "Content copy", () -> {
            final AEMPackageCreateAndDownloadPackageTask task = new AEMPackageCreateAndDownloadPackageTask(sourceCrx, targetCrx, tmpPath, project, getPaths());
            ProgressManager.getInstance().run(task);
        }, null);
        confirmationDialog.show();
    }

    private void validateCopy() throws RuntimeException {
        Validate.isTrue(comboTargetInstance.getSelectedIndex() >= 0, "Please select target instance");
        Validate.isTrue(comboSourceInstance.getSelectedIndex() >= 0, "Please select source instance");
        Validate.notEmpty(getPaths(), "Please provide at least one path");
        Validate.isTrue(comboSourceInstance.getSelectedIndex() != comboTargetInstance.getSelectedIndex(), "Source and target instances are equal.");
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return jMainPanel;
    }

    private void initConfiguration() {
        IntelliVaultPreferencesService preferenceService =
                ServiceManager.getService(IntelliVaultPreferencesService.class);
        IntelliVaultPreferences preferences = preferenceService.getPreferences();
        repoList = preferences.getRepoConfigList();
        tmpPath = preferences.packagesTempDirectory;
    }

    private void initComboBoxes() {
        for (IntelliVaultCRXRepository repo : repoList) {
            comboSourceInstance.addItem(repo);
            comboTargetInstance.addItem(repo);
        }
    }

    private void initList() {
        contentPathsModel = new DefaultListModel<>();
        listContentPaths.setModel(contentPathsModel);

        listContentPaths.addListSelectionListener(e -> {
            enableDisableButtonRemove();
        });
    }

    private void initButtons() {
        btnAddPath.addActionListener(e -> {
            buttonAddAction(e);
        });

        btnRemovePath.addActionListener(e -> {
            buttonRemoveAction(e);
        });

        btnBrowseDownloadPath.addActionListener(e -> {
            final String path = getStringFromFileChooser("Select download directory", JFileChooser.DIRECTORIES_ONLY);
            if (path != null) {
                textDownloadPath.setText(path);
            }
        });

        btnBrowsePackageToUpload.addActionListener(e -> {
            final String path = getStringFromFileChooser("Select package to install", JFileChooser.FILES_ONLY);
            if (path != null) {
                textPackageToUpload.setText(path);
            }
        });

        enableDisableButtonAdd();
        enableDisableButtonRemove();
    }

    private void buttonAddAction(ActionEvent e) {
        statusLine.clear();

        final String newPath = txtFieldContentPath.getText();

        if (!newPath.matches("^\\/[a-zA-Z0-9_\\/\\-]*(?<!\\/)$")) {
            statusLine.setMessage("This is not a valid path, please validate your expression.");
            return;
        }

        if (contentPathsModel.contains(newPath)) {
            statusLine.setMessage("The path already exists");
            return;
        }

        contentPathsModel.addElement(txtFieldContentPath.getText());
        listContentPaths.clearSelection();
        txtFieldContentPath.setText("");
        enableDisableButtonAdd();
        enableDisableButtonRemove();
    }

    private void buttonRemoveAction(ActionEvent e) {
        statusLine.clear();

        int selectedElement = listContentPaths.getSelectedIndex();

        if (selectedElement < 0) {
            statusLine.setMessage("Please select element to remove first");
            return;
        }

        contentPathsModel.removeElementAt(selectedElement);
        listContentPaths.clearSelection();

        enableDisableButtonAdd();
        enableDisableButtonRemove();
    }

    private void initTextFieldAddPath() {
        txtFieldContentPath.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                enableDisableButtonAdd();
            }

            public void keyTyped(KeyEvent e) {
            }

            public void keyPressed(KeyEvent e) {
            }
        });

        txtFieldContentPath.addActionListener(e -> {
            if (txtFieldContentPath.getText() != null && txtFieldContentPath.getText().length() > 0) {
                buttonAddAction(e);
            }
        });
    }

    private void enableDisableButtonRemove() {
        btnRemovePath.setEnabled(listContentPaths.getSelectedIndex() >= 0);
    }

    private void enableDisableButtonAdd() {
        btnAddPath.setEnabled(txtFieldContentPath.getText() != null && txtFieldContentPath.getText().length() > 0);
    }

    private void initActionChooser() {
        comboAction.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                actionSelectionChanged(comboAction.getSelectedIndex());
            }
        });
    }

    private void actionSelectionChanged(int selectedIndex) {
        this.selectedAction = selectedIndex;

        switch (selectedIndex) {
            case COPY_ACTION:
                prepareUiCopyAction();
                break;
            case DOWNLOAD_ACTION:
                prepareUiDownloadAction();
                break;
            case UPLOAD_ACTION:
                prepareUiUploadAction();
                break;
        }
    }

    private void prepareUiCopyAction() {
        panelSourceInstance.setVisible(true);
        panelTargetInstance.setVisible(true);
        panelDownloadPath.setVisible(false);
        panelPackageUpload.setVisible(false);
        panelPathsSelector.setEnabled(true);
        txtFieldContentPath.setEnabled(true);
        listContentPaths.setEnabled(true);
        labelContentToMigrate.setEnabled(true);
        labelContentToMigrate.setText("Content to migrate");
    }

    private void prepareUiDownloadAction() {
        panelSourceInstance.setVisible(true);
        panelTargetInstance.setVisible(false);
        panelDownloadPath.setVisible(true);
        panelPackageUpload.setVisible(false);
        panelPathsSelector.setEnabled(true);
        txtFieldContentPath.setEnabled(true);
        listContentPaths.setEnabled(true);
        labelContentToMigrate.setEnabled(true);
        labelContentToMigrate.setText("Package content");
        textDownloadPath.setText(tmpPath);
        txtPackageName.setText("intelij-" + RandomStringUtils.randomAlphabetic(8));
    }

    private void prepareUiUploadAction() {
        panelSourceInstance.setVisible(false);
        panelTargetInstance.setVisible(true);
        panelDownloadPath.setVisible(false);
        panelPackageUpload.setVisible(true);
        panelPathsSelector.setEnabled(false);
        txtFieldContentPath.setEnabled(false);
        listContentPaths.setEnabled(false);
        labelContentToMigrate.setEnabled(false);
    }

    private String getStringFromFileChooser(final String title, final int fileSelecttionMode) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new java.io.File(tmpPath));
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(fileSelecttionMode);
        chooser.setAcceptAllFileFilterUsed(false);

        // Demonstrate "Open" dialog:
        int rVal = chooser.showOpenDialog(jMainPanel);
        if (rVal == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile().getAbsolutePath();
        }

        return null;
    }

    private String[] getPaths() {
        final String[] paths = new String[contentPathsModel.getSize()];

        for (int i = 0; i < paths.length; i++) {
            paths[i] = contentPathsModel.getElementAt(i);
        }

        return paths;
    }

}
