package com.razorfish.platforms.intellivault.services.tasks;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.razorfish.platforms.intellivault.config.IntelliVaultCRXRepository;
import com.razorfish.platforms.intellivault.services.CurlInvokerService;
import com.razorfish.platforms.intellivault.services.impl.CurlInvokerServiceImpl;
import com.razorfish.platforms.intellivault.services.pojo.AEMResponse;
import org.jetbrains.annotations.NotNull;
import org.jsoup.helper.Validate;

import java.io.File;
import java.text.SimpleDateFormat;

public class AEMUploadAndInstallPackageTask extends Task.Backgroundable {

    private IntelliVaultCRXRepository targetCrxRepository;
    private String filePath;

    private final static SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    public static final Logger log = Logger.getInstance(AEMUploadAndInstallPackageTask.class);

    public AEMUploadAndInstallPackageTask(final IntelliVaultCRXRepository targetCrxRepository,
                                          final String filePath,
                                          final Project project) {
        super(project, "Running IntelliVault Project Manager Action");
        this.targetCrxRepository = targetCrxRepository;
        this.filePath = filePath;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) throws IllegalArgumentException {

        Validate.notNull(filePath);
        File file = new File(filePath);

        if (!file.exists()) {
            throw new IllegalArgumentException("Provided file doesn't exist or cannot be opened.");
        }

        try {
            CurlInvokerService curl = new CurlInvokerServiceImpl();

            indicator.setText("Uploading package to target repository");
            indicator.setIndeterminate(false);
            indicator.setFraction(0.4);

            AEMResponse aemResponse = curl.uploadPackage(targetCrxRepository.getRepoUrl(),
                    targetCrxRepository.getUsername() + ":" + targetCrxRepository.getPassword(),
                    file);

            if (!aemResponse.isSuccess() || aemResponse.getPath() == null) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showErrorDialog("Couldn't upload package to the target instance.", "IntelliVault Package Manager Error!");
                });
                return;
            }

            indicator.setText("Installing package on target repository");
            indicator.setFraction(0.8);

            aemResponse = curl.installPackage(targetCrxRepository.getRepoUrl(),
                    targetCrxRepository.getUsername() + ":" + targetCrxRepository.getPassword(),
                    aemResponse.getPath());

            if (!aemResponse.isSuccess()) {
                ApplicationManager.getApplication().invokeLater(() -> {
                    Messages.showErrorDialog("Couldn't install package on target repository..", "IntelliVault Package Manager Error!");
                });
                return;
            }

            indicator.setText("done!");
            indicator.setFraction(1.0);

            ApplicationManager.getApplication().invokeLater(() -> {
                Messages.showInfoMessage(String.format("Successfully installed content on %s instance.",
                        targetCrxRepository.getName() + " ( " + targetCrxRepository.getRepoUrl() + " )"),
                        "IntelliVault Package Manager Content Copy Successfull!");
            });

        } catch (Exception e) {
            ApplicationManager.getApplication().invokeLater(() -> {
                Messages.showErrorDialog(e.getLocalizedMessage(), "IntelliVault Package Manager Error!");
            });
        }
    }
}
