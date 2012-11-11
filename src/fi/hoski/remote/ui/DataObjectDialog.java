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

import fi.hoski.datastore.repository.DataObject;
import fi.hoski.datastore.repository.DataObjectModel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.ResourceBundle;
import javax.swing.*;

/**
 * @author Timo Vesalainen
 */
public class DataObjectDialog<T extends DataObject> extends JDialog
{
    public static final ResourceBundle uiBundle = ResourceBundle.getBundle("fi/hoski/remote/ui/ui");

    private DataObjectModel model;
    private T dataObject;
    private boolean accepted;
    private Map<String,JComponent> componentMap;

    public DataObjectDialog(JFrame parent, T dataObject)
    {
        this(parent, dataObject.getModel(), dataObject);
    }
    public DataObjectDialog(JFrame parent, DataObjectModel model, T dataObject)
    {
        this(parent, dataObject.getKindString(), model, dataObject);
    }
    public DataObjectDialog(JFrame parent, String title, DataObjectModel model, final T dataObject)
    {
        super(parent, title);
        this.model = model;
        this.dataObject = dataObject;
        
        componentMap = DialogUtil.createEditPane(this, model, dataObject, BorderLayout.CENTER);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        add(buttonPanel, BorderLayout.SOUTH);
        JButton ok = new JButton(uiBundle.getString("OK"));
        ActionListener okAction = new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                String unsetProperty = dataObject.firstMandatoryNullProperty();
                if (unsetProperty == null)
                {
                    accepted = true;
                    setVisible(false);
                }
                else
                {
                    JOptionPane.showMessageDialog(
                            null, 
                            dataObject.getPropertyString(unsetProperty), 
                            uiBundle.getString("UNSET FIELD"), 
                            JOptionPane.ERROR_MESSAGE);
                    JComponent comp = componentMap.get(unsetProperty);
                    if (comp == null)
                    {
                        throw new IllegalArgumentException("mandatory property "+unsetProperty+" not set");
                    }
                    comp.requestFocusInWindow();
                }
            }
        };
        ok.addActionListener(okAction);
        buttonPanel.add(ok);
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
        buttonPanel.add(cancel);
        setModalityType(Dialog.ModalityType.TOOLKIT_MODAL);
        setLocation(100, 100);
    }

    public Map<String, JComponent> getComponentMap()
    {
        return componentMap;
    }

    public boolean edit()
    {
        pack();
        accepted = false;
        setVisible(true);
        return accepted;
    }

}
