package com.razorfish.platforms.intellivault.ui.utils;

import javax.swing.*;

public class StatusLine {

    private JLabel statusLineLabel;

    public StatusLine(final JLabel statusLineLabel) {
        this.statusLineLabel = statusLineLabel;
    }

    public void setMessage(final String msg){
        statusLineLabel.setText(msg);
    }

    public void clear(){
        statusLineLabel.setText("");
    }
}
