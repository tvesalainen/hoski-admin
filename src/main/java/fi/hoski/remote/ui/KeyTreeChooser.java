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

import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import fi.hoski.datastore.repository.DataObject;
import fi.hoski.remote.DataStoreService;
import fi.hoski.remote.ui.KeyTreeModel.KeyWrapper;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreePath;

/**
 * @author Timo Vesalainen
 */
public class KeyTreeChooser extends JDialog
{
    public static final ResourceBundle uiBundle = ResourceBundle.getBundle("fi/hoski/remote/ui/ui");
    private DataStoreService dss;
    private JTree tree;

    public KeyTreeChooser(Frame owner, String title, DataStoreService dss, Key root, String... kinds)
    {
        this(owner, title, dss, new KeyTreeModel(dss, root, kinds));
    }
    public KeyTreeChooser(Frame owner, String title, DataStoreService dss, KeyTreeModel ktm)
    {
        super(owner, title);
        this.dss = dss;
        tree = new KeyTree(ktm);
        tree.setRootVisible(false);
        //tree.setToggleClickCount(1);
        ktm.setComponent(tree);
        JScrollPane scrollPane = new JScrollPane(tree);
        add(scrollPane, BorderLayout.NORTH);
        // buttons
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        add(panel, BorderLayout.SOUTH);
        JButton ok = new JButton(uiBundle.getString("CHOOSE"));
        ActionListener okAction = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                setVisible(false);
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
                tree.setSelectionPath(null);
                setVisible(false);
            }
        };
        cancel.addActionListener(cancelAction);
        panel.add(cancel);

        setLocation(10, 10);
        //setModalityType(Dialog.ModalityType.TOOLKIT_MODAL);
        pack();
    }
    public DataObject select()
    {
        setModalityType(Dialog.ModalityType.TOOLKIT_MODAL);
        setVisible(true);
        TreePath selectionPath = tree.getSelectionPath();
        if (selectionPath != null)
        {
            KeyWrapper lastPathComponent = (KeyWrapper) selectionPath.getLastPathComponent();
            Key key = lastPathComponent.getKey();
            try
            {
                return dss.newInstance(key);
            }
            catch (EntityNotFoundException ex)
            {
                ex.printStackTrace();
            }
        }
        return null;
    }
    
    
}
