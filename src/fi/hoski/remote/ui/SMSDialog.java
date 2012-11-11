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

import fi.hoski.remote.DataStoreProxy;
import fi.hoski.remote.DataStoreService;
import fi.hoski.datastore.Repository;
import fi.hoski.datastore.repository.DataObject;
import fi.hoski.remote.DataStore;
import fi.hoski.sms.IllegalCharacterException;
import fi.hoski.sms.SMSException;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.*;

/**
 * @author Timo Vesalainen
 */
public class SMSDialog extends JDialog
{
    public static final ResourceBundle uiBundle = ResourceBundle.getBundle("fi/hoski/remote/ui/ui");

    public SMSDialog(Frame owner, String title, String message, final List<String> phoneList) throws IOException, SMSException
    {
        super(owner, title);

        final DataStoreService dss = DataStore.getDss();
        final int smsLeft = dss.messagesLeft();
        String smsLeftString = uiBundle.getString("SMS LEFT");
        setTitle(title+" "+smsLeftString+" "+smsLeft);
        final JTextArea bodyArea = new JTextArea(message, 3, 30);
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
                String msg = bodyArea.getText();
                int smsPerMsg = 0;
                try
                {
                    smsPerMsg = dss.messageCount(msg);
                }
                catch (CharacterCodingException ex)
                {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(rootPane, ex.getMessage());
                }
                catch (IllegalCharacterException ex)
                {
                    JOptionPane.showMessageDialog(rootPane, ex.getMessage());
                }
                int smsCount = phoneList.size()*smsPerMsg;
                if (smsCount > smsLeft)
                {
                    JOptionPane.showMessageDialog(rootPane, uiBundle.getString("SMS CREDIT FULL"));
                }
                else
                {
                    String confirmMsg = uiBundle.getString("CONFIRM SEND SMS");
                    confirmMsg = String.format(confirmMsg, phoneList.size());
                    if (JOptionPane.showConfirmDialog(rootPane, confirmMsg, "", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION)
                    {
                        for (String pn : phoneList)
                        {
                            try
                            {
                                dss.send(pn, msg);
                            }
                            catch (Exception ex)
                            {
                                System.err.println(ex.getMessage());
                            }
                        }
                        setVisible(false);
                    }
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
    private List<String> convert(List<? extends DataObject> dataObjectList)
    {
        List<String> phoneList = new ArrayList<String>();
        for (DataObject dataObject : dataObjectList)
        {
            String number = (String) dataObject.get(Repository.JASENET_MOBILE);
            if (number != null)
            {
                phoneList.add(number);
            }
        }
        return phoneList;
    }
}
