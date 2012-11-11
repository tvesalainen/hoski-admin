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
import fi.hoski.datastore.repository.ResultSetModel;
import fi.hoski.remote.sync.SqlConnection;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

/**
 * @author Timo Vesalainen
 */
public class SqlDialog extends JDialog
{
    public static final ResourceBundle uiBundle = ResourceBundle.getBundle("fi/hoski/remote/ui/ui");

    private SqlConnection connection;
    
    public SqlDialog(final Frame owner, String sql, Properties properties) throws ClassNotFoundException, SQLException
    {
        super(owner);
        connection = new SqlConnection(properties);
        
        final JTextArea sqlArea = new JTextArea(sql, 30, 80);
        JScrollPane scrollPane = new JScrollPane(sqlArea);
        add(scrollPane, BorderLayout.CENTER);
        
        // buttons
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        add(panel, BorderLayout.SOUTH);
        JButton ok = new JButton(uiBundle.getString("RUN"));
        ActionListener okAction = new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                try
                {
                    ResultSet rs = connection.query(sqlArea.getText());
                    ResultSetModel model = new ResultSetModel(rs);
                    List<DataObject> list = DataObject.convert(model, rs);
                    DataObjectListDialog<DataObject> dia = new DataObjectListDialog<>(owner, "", "OK", model, list);
                    dia.setAutoCreateRowSorter(true);
                    dia.select();
                }
                catch (SQLException ex)
                {
                    ex.printStackTrace();
                    setVisible(false);
                    JOptionPane.showMessageDialog(owner, ex.getMessage());
                    setVisible(true);
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

    public void edit()
    {
        setVisible(true);
    }
}
