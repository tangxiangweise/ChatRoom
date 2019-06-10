package com.chat.room.api.gui;


import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class FooGui extends JFrame {

    private Timer timer;

    private JLabel label;

    public FooGui(String name, Callback callback) {
        super(name);
        JFrame.setDefaultLookAndFeelDecorated(true);

        setLayout(new BorderLayout());
        setMaximumSize(new Dimension(280, 160));
        setMinimumSize(new Dimension(280, 160));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        label = new JLabel(name, SwingConstants.CENTER);
        add(label, BorderLayout.CENTER);

        timer = new Timer(2000, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Object[] objects = callback.takeText();
                if (objects == null || objects.length == 0) {
                    return;
                }
                StringBuilder text = new StringBuilder("<html>");
                for (int i = 0; i < objects.length; i++) {
                    text.append(objects[i].toString());
                    if (i != objects.length - 1) {
                        text.append("<br/>");
                    }
                }
                text.append("</html>");
            }
        });

    }

    public void doShow() {

    }

    private class Callback {
        public Object[] takeText() {
            return null;
        }
    }
}
