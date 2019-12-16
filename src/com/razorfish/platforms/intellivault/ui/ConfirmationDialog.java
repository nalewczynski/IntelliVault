package com.razorfish.platforms.intellivault.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;
import org.jsoup.helper.Validate;

import javax.swing.*;
import java.awt.event.*;

public class ConfirmationDialog extends DialogWrapper {
    private JPanel contentPane;
    private JLabel labelConfirmationText;
    private Runnable okRunnable;
    private Runnable cancelRunnable;

    public ConfirmationDialog(final Project project, final String labelText, final String title, final Runnable okRunnable, final Runnable cancelRunnable) {
        super(project);
        setModal(true);

        Validate.notNull(okRunnable);
        this.okRunnable = okRunnable;
        this.cancelRunnable = cancelRunnable;

        this.setTitle(title);

        labelConfirmationText.setText(labelText);

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doCancelAction();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        init();
    }

    @Override
    public void doOKAction() {
        okRunnable.run();
        dispose();
    }

    @Override
    public void doCancelAction() {
        if (cancelRunnable != null) {
            cancelRunnable.run();
        }
        dispose();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }
}
