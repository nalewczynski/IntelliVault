package com.razorfish.platforms.intellivault.actions.packagemanager;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.razorfish.platforms.intellivault.ui.PackageManagerForm;

public class PackageManagerAction extends AnAction {

    public static final Logger LOG = Logger.getInstance("org.skelet-log");

    @Override
    public void actionPerformed(AnActionEvent evt) {
        LOG.info("Execute ** actionPerformed **.");

        Project project = evt.getData(PlatformDataKeys.PROJECT);
        final PackageManagerForm form = new PackageManagerForm(project);
        form.show();
    }

    @Override
    public void update(final AnActionEvent e) {
        LOG.info("Execute ** update **.");
    }
}