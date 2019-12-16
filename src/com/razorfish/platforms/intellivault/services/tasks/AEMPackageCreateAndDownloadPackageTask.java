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

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AEMPackageCreateAndDownloadPackageTask extends Task.Backgroundable {

    private String downloadPath;
    private IntelliVaultCRXRepository sourceCRXRepository;
    private IntelliVaultCRXRepository targetCrxRepository;
    private String[] paths;
    private String packageName;

    private final static SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    public static final Logger log = Logger.getInstance(AEMPackageCreateAndDownloadPackageTask.class);

    public AEMPackageCreateAndDownloadPackageTask(final IntelliVaultCRXRepository crxRepository,
                                                  final IntelliVaultCRXRepository targetCrxRepository,
                                                  final String downloadPath,
                                                  final Project project, final String[] paths) {
        super(project, "Running IntelliVault Project Manager Action");
        this.downloadPath = downloadPath;
        this.sourceCRXRepository = crxRepository;
        this.targetCrxRepository = targetCrxRepository;
        this.paths = paths;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        try {
            if (packageName == null) {
                packageName = "intelij-" + SDF.format(new Date()).replace("-", "");
            } else {
                if (packageName.endsWith(".zip")) {
                    packageName = packageName.replace(".zip", "");
                }

                packageName = packageName + "-" + SDF.format(new Date()).replace("-", "");
            }

            final String packageGroup = "intelij";
            final String packagePath = "/etc/packages/" + packageGroup + "/" + packageName + ".zip";

            indicator.setText("Creating package on source instance");
            indicator.setIndeterminate(false);
            indicator.setFraction(0.2);

            CurlInvokerService curl = new CurlInvokerServiceImpl();
            AEMResponse aemResponse = curl.createPackage(sourceCRXRepository.getRepoUrl(),
                    sourceCRXRepository.getUsername() + ":" + sourceCRXRepository.getPassword(),
                    packageGroup,
                    packageName,
                    paths);

            if (!aemResponse.isSuccess()) {
                ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog("Couldn't create aem package on source instance",
                        "IntelliVault Package Manager Error!"));
                return;
            }

            indicator.setText("Building the package..");
            indicator.setFraction(0.3);

            aemResponse = curl.buildPackage(sourceCRXRepository.getRepoUrl(),
                    sourceCRXRepository.getUsername() + ":" + sourceCRXRepository.getPassword(),
                    packagePath);

            if (!aemResponse.isSuccess()) {
                ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog("Couldn't build package on source instance",
                        "IntelliVault Package Manager Error!"));
                return;
            }

            indicator.setText("Downloading package");
            indicator.setFraction(0.5);

            aemResponse = curl.downloadPackage(sourceCRXRepository.getRepoUrl(),
                    sourceCRXRepository.getUsername() + ":" + sourceCRXRepository.getPassword(),
                    packagePath,
                    new File(downloadPath + File.separator + packageName + ".zip").getPath());

            if (!aemResponse.isSuccess()) {
                ApplicationManager.getApplication().invokeLater(() ->
                        Messages.showErrorDialog("Couldn't download package", "IntelliVault Package Manager Error!"));
                return;
            }

            indicator.setText("Removing package from repository");
            indicator.setFraction(0.6);

            aemResponse = curl.deletePackage(sourceCRXRepository.getRepoUrl(),
                    sourceCRXRepository.getUsername() + ":" + sourceCRXRepository.getPassword(),
                    packagePath);

            if (!aemResponse.isSuccess()) {
                ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog("Couldn't remove package",
                        "IntelliVault Package Manager Error!"));
                return;
            }

            //no target repository - download only
            if (targetCrxRepository == null) {
                indicator.setText("done!");
                indicator.setFraction(1.0);

                ApplicationManager.getApplication().invokeLater(() -> Messages.showInfoMessage(String.format("Successfully downloaded package from %s.",
                        sourceCRXRepository.getName() + " (" + sourceCRXRepository.getRepoUrl() + ")"),
                        "IntelliVault Package Manager Download Successfull!"));
            } else {
                indicator.setText("Uploading package to target repository");
                indicator.setFraction(0.7);

                curl.uploadPackage(targetCrxRepository.getRepoUrl(),
                        targetCrxRepository.getUsername() + ":" + targetCrxRepository.getPassword(),
                        new File(downloadPath + File.separator + packageName + ".zip"));

                indicator.setText("Installing package on target repository");
                indicator.setFraction(0.8);

                aemResponse = curl.installPackage(targetCrxRepository.getRepoUrl(),
                        targetCrxRepository.getUsername() + ":" + targetCrxRepository.getPassword(),
                        packagePath);

                if (!aemResponse.isSuccess()) {
                    ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog("Couldn't install package on target repository..",
                            "IntelliVault Package Manager Error!"));
                    return;
                }

                indicator.setText("done!");
                indicator.setFraction(1.0);

                ApplicationManager.getApplication().invokeLater(() -> Messages.showInfoMessage(String.format("Successfully copied content from %s to %s instance.",
                        sourceCRXRepository.getName() + " ( " + sourceCRXRepository.getRepoUrl() + " )",
                        targetCrxRepository.getName() + " ( " + targetCrxRepository.getRepoUrl() + " )"),
                        "IntelliVault Package Manager Content Copy Successfull!"));
            }
        } catch (Exception e) {
            ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(e.getLocalizedMessage(), "IntelliVault Package Manager Error!"));
        }
    }
}
