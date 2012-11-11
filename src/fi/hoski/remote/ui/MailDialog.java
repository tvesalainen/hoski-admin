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

import com.google.appengine.api.datastore.Text;
import fi.hoski.remote.DataStoreService;
import fi.hoski.datastore.repository.DataObject;
import fi.hoski.datastore.repository.Messages;
import fi.hoski.remote.DataStore;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.swing.*;

/**
 * @author Timo Vesalainen
 */
public class MailDialog extends JDialog
{
    public static final ResourceBundle uiBundle = ResourceBundle.getBundle("fi/hoski/remote/ui/ui");

    public MailDialog(Frame owner, String subject, Text body, List<? extends DataObject> dataObjectList) throws AddressException
    {
        this(owner, subject, body.getValue(), dataObjectList);
    }
    public MailDialog(Frame owner, String subject, String body, final List<? extends DataObject> dataObjectList) throws AddressException
    {
        super(owner, subject);
        //setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        
        final DataStoreService dss = DataStore.getDss();
        String fromStr = dss.getMessage(Messages.PASSWORDFROMADDRESS);
        final InternetAddress from = new InternetAddress(fromStr);
        
        // Subject
        JPanel subjectPanel = new JPanel();
        subjectPanel.setLayout(new FlowLayout());
        add(subjectPanel, BorderLayout.NORTH);
        JLabel subjectLabel = new JLabel(uiBundle.getString("SUBJECT"));
        subjectPanel.add(subjectLabel);
        final JTextField subjectField = new JTextField(subject, 60);
        subjectPanel.add(subjectField);
        
        final JTextArea bodyArea = new JTextArea(body, 30, 80);
        JScrollPane scrollPane = new JScrollPane(bodyArea);
        add(scrollPane, BorderLayout.CENTER);
        
        // buttons
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        add(panel, BorderLayout.SOUTH);
        JButton ok = new JButton(uiBundle.getString("SEND"));
        ActionListener okAction = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String msg = uiBundle.getString("CONFIRM SEND MAIL");
                msg = String.format(msg, dataObjectList.size());
                if (JOptionPane.showConfirmDialog(rootPane, msg, "", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                {
                    String format = bodyArea.getText();
                    int count = 0;
                    for (DataObject dataObject : dataObjectList)
                    {
                        try
                        {
                            InternetAddress internetAddress = dss.createInternetAddress(dataObject);
                            String message = dataObject.format(format);
                            dss.sendMail(from, subjectField.getText(), message, null, internetAddress);
                            count++;
                        }
                        catch (Exception ex)
                        {
                            System.err.println(ex.getMessage());
                        }
                    }
                    JOptionPane.showMessageDialog(rootPane, uiBundle.getString("SENT MESSAGES")+" "+count);
                    setVisible(false);
                }
            }
        };
        ok.addActionListener(okAction);
        panel.add(ok);
        JButton cancel = new JButton(uiBundle.getString("CANCEL"));
        ActionListener cancelAction = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setVisible(false);
            }
        };
        cancel.addActionListener(cancelAction);
        panel.add(cancel);

        setLocationByPlatform(true);
        setModalityType(Dialog.ModalityType.TOOLKIT_MODAL);
        pack();
    }

    public boolean edit()
    {
        setVisible(true);
        return true;
    }
}
