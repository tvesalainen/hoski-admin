/*
 * Copyright (C) 2012 Helsingfors Segelklubb ry
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package fi.hoski.remote.ui;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.swing.*;

/**
 * @author Timo Vesalainen
 */
public class TextDialog extends JDialog implements Appendable
{
    public static final ResourceBundle uiBundle = ResourceBundle.getBundle("fi/hoski/remote/ui/ui");
    
    private JTextArea textArea;

    public TextDialog(final Frame owner)
    {
        super(owner);
        textArea = new JTextArea("", 30, 80);
        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);
        
        // buttons
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        add(panel, BorderLayout.SOUTH);
        JButton ok = new JButton(uiBundle.getString("OK"));
        ActionListener cancelAction = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setVisible(false);
            }
        };
        ok.addActionListener(cancelAction);
        panel.add(ok);

        setLocationByPlatform(true);
        setModalityType(Dialog.ModalityType.TOOLKIT_MODAL);
        pack();
    }

    public void edit()
    {
        setVisible(true);
    }

    @Override
    public Appendable append(CharSequence csq) throws IOException
    {
        if (csq != null)
        {
            textArea.append(csq.toString());
        }
        else
        {
            textArea.append("null");
        }
        return this;
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException
    {
        textArea.append(csq.subSequence(start, end).toString());
        return this;
    }

    @Override
    public Appendable append(char c) throws IOException
    {
        textArea.append(Character.toString(c));
        return this;
    }
}
