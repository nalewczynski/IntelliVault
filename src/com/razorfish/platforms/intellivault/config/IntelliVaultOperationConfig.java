package com.razorfish.platforms.intellivault.config;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Value object for storing vault configuration options such as which files are ignored.
 */
public class IntelliVaultOperationConfig {

    private static final String TEMP_DIR_PROP = "java.io.tmpdir";

    private String vaultPath;
    private String tempDirectory;
    private String rootFolderName;
    private boolean verbose;
    private boolean debug;
    private boolean logToConsole;
    private List<String> fileIgnorePatterns;
    private boolean showMessageDialogs;
    private String packagesTempDirectory;
    private int instanceType;
    private int defaultRepository;

    /**
     * Create a new instance using the default value for all fields of the operation config
     */
    public IntelliVaultOperationConfig() {
        this.vaultPath = IntelliVaultConfigDefaults.VAULT_PATH;
        this.tempDirectory = System.getProperty(TEMP_DIR_PROP);
        this.rootFolderName = IntelliVaultConfigDefaults.ROOT_FOLDER;
        this.verbose = IntelliVaultConfigDefaults.VERBOSE;
        this.debug = IntelliVaultConfigDefaults.DEBUG;
        this.logToConsole = IntelliVaultConfigDefaults.CONSOLE_LOG;
        this.fileIgnorePatterns =
                new LinkedList<String>(Arrays.asList(IntelliVaultConfigDefaults.IGNORE_PATTERNS.split(",")));
        this.showMessageDialogs = IntelliVaultConfigDefaults.SHOW_MESSAGE_DIALOG;
        this.packagesTempDirectory = System.getProperty(TEMP_DIR_PROP);
        this.instanceType = IntelliVaultCRXRepository.INSTANCE_TYPE_LOCAL;
    }

    public String getVaultPath() {
        return vaultPath;
    }

    public void setVaultPath(String vaultPath) {
        this.vaultPath = vaultPath;
    }

    public String getTempDirectory() {
        return tempDirectory;
    }

    public void setTempDirectory(String tempDirectory) {
        this.tempDirectory = tempDirectory;
    }

    public String getRootFolderName() {
        return rootFolderName;
    }

    public void setRootFolderName(String rootFolderName) {
        this.rootFolderName = rootFolderName;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isLogToConsole() {
        return logToConsole;
    }

    public void setLogToConsole(boolean logToConsole) {
        this.logToConsole = logToConsole;
    }

    public List<String> getFileIgnorePatterns() {
        return fileIgnorePatterns;
    }

    public void setFileIgnorePatterns(List<String> fileIgnorePatterns) {
        this.fileIgnorePatterns = fileIgnorePatterns;
    }

    public boolean showMessageDialogs() {
        return showMessageDialogs;
    }

    public void setShowMessageDialogs(boolean showMessageDialogs) {
        this.showMessageDialogs = showMessageDialogs;
    }

    public String getPackagesTempDirectory() {
        return packagesTempDirectory;
    }

    public void setPackagesTempDirectory(String packagesTempDirectory) {
        this.packagesTempDirectory = packagesTempDirectory;
    }

    public int getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(int instanceType) {
        this.instanceType = instanceType;
    }

    public int getDefaultRepository() {
        return defaultRepository;
    }

    public void setDefaultRepository(int defaultRepository) {
        this.defaultRepository = defaultRepository;
    }
}
