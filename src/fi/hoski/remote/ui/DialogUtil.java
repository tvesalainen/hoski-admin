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

import com.google.appengine.api.datastore.Link;
import com.google.appengine.api.datastore.Text;
import fi.hoski.datastore.repository.DataObject;
import fi.hoski.datastore.repository.DataObjectModel;
import fi.hoski.datastore.repository.Options;
import fi.hoski.util.Day;
import fi.hoski.util.Time;
import java.awt.Dimension;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import layout.SpringUtilities;

/**
 * @author Timo Vesalainen
 */
public class DialogUtil 
{
    private static int fieldColumns = 40;
    private static int fieldRows = 6;
    
    public static Map<String,JComponent> createEditPane(JDialog dialog, DataObjectModel model, final DataObject dataObject, String layout)
    {
        final Map<String,JComponent> componentMap = new HashMap<String,JComponent>();
        JPanel panel = new JPanel(new SpringLayout());
        JScrollPane scrollPane = new JScrollPane(panel);
        dialog.add(scrollPane, layout);
        URL url = dataObject.getClass().getResource("/fi/hoski/remote/ui/images/calendar.png");
        ImageIcon icon = new ImageIcon(url);
        for (String property : model.getProperties())
        {
            String labelText = dataObject.getPropertyString(property);
            JLabel label = new JLabel(labelText, JLabel.TRAILING);
            panel.add(label);
            Object value = dataObject.get(property);
            Class<?> type = dataObject.getType(property);
            JComponent comp = null;
            if (
                    String.class.equals(type) ||
                    Number.class.isAssignableFrom(type) ||
                    Day.class.isAssignableFrom(type) ||
                    Time.class.isAssignableFrom(type)
                    )
            {
                if (value == null)
                {
                    if (model.isPassword(property))
                    {
                        comp = new JPasswordField(20);
                    }
                    else
                    {
                        comp = new JTextField(20);
                    }
                }
                else
                {
                    if (model.isPassword(property))
                    {
                        comp = new JPasswordField(value.toString());
                    }
                    else
                    {
                        comp = new JTextField(value.toString());
                    }
                }
                panel.add(comp);
                comp.setInputVerifier(new Verifier(dataObject, property));
                componentMap.put(property, comp);
            }
            else
            {
                if (Link.class.equals(type))
                {
                    if (value == null)
                    {
                        comp = new JTextField(60);
                    }
                    else
                    {
                        comp = new JTextField(value.toString());
                    }
                    panel.add(comp);
                    comp.setInputVerifier(new Verifier(dataObject, property));
                    componentMap.put(property, comp);
                }
                else
                {
                    if (Boolean.class.equals(type))
                    {
                        if (value == null)
                        {
                            comp = new JCheckBox();
                        }
                        else
                        {
                            boolean b = (boolean) value;
                            comp = new JCheckBox("", b);
                        }
                        panel.add(comp);
                        comp.setInputVerifier(new Verifier(dataObject, property));
                        componentMap.put(property, comp);
                    }
                    else
                    {
                        if (Text.class.equals(type))
                        {
                            JTextArea ta = null;
                            if (value == null)
                            {
                                ta = new JTextArea(fieldRows, fieldColumns);
                                comp = new JScrollPane(ta);
                            }
                            else
                            {
                                Text text = null;
                                if (value instanceof Text)
                                {
                                    text = (Text) value;
                                }
                                else
                                {
                                    text = new Text(value.toString());
                                }
                                ta = new JTextArea(text.getValue(), fieldRows, fieldColumns);
                                comp = new JScrollPane(ta);

                            }
                            panel.add(comp);
                            ta.setInputVerifier(new Verifier(dataObject, property));
                            componentMap.put(property, ta);
                        }
                        else
                        {
                            if (Options.class.equals(type))
                            {
                                if (value == null)
                                {
                                    comp = new JComboBox();
                                }
                                else
                                {
                                    JComboBox<String> cb = new JComboBox<String>();
                                    comp = cb;
                                    @SuppressWarnings("unchecked")
                                    Options<Object> sel = (Options) value;
                                    for (String s : sel)
                                    {
                                        cb.addItem(s);
                                    }
                                }
                                panel.add(comp);
                                comp.setInputVerifier(new Verifier(dataObject, property));
                                componentMap.put(property, comp);
                            }
                            else
                            {
                                throw new IllegalArgumentException(type+" not suitable");
                            }
                        }
                    }
                }
            }
            // Date chooser
            if (Day.class.equals(type))
            {
                JButton b = new JButton(icon);
                b.setPreferredSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
                b.addActionListener(new DateCalendarHelper(labelText, (JTextField)comp));
                panel.add(b);
            }
            else
            {
                JButton b = new JButton();
                b.setPreferredSize(new Dimension(icon.getIconWidth(), icon.getIconHeight()));
                b.setVisible(false);
                panel.add(b);
            }
        }
        SpringUtilities.makeCompactGrid(panel,
                model.propertyCount(), 3, //rows, cols
                6, 6, //initX, initY
                6, 6);       //xPad, yPad    
        return componentMap;
    }
    public static class Verifier extends InputVerifier
    {
        private String field;
        private DataObject dataObject;

        public Verifier(DataObject dataObject, String field)
        {
            this.dataObject = dataObject;
            this.field = field;
        }
        

        @Override
        public boolean verify(JComponent input)
        {
            Object value = dataObject.get(field);
            String text = ComponentAccessor.get(input).toString();
            if (
                    (text == null || text.isEmpty()) && 
                    (value == null || value.toString().isEmpty())
                    )
            {
                return true;
            }
            try
            {
                if (value instanceof Options)
                {
                    @SuppressWarnings("unchecked")
                    Options<Object> sel = (Options<Object>) value;
                    sel.setSelection(sel.getItem(text));
                }
                else
                {
                    dataObject.set(field, text);
                }
            }
            catch (Exception ex)
            {
                JOptionPane.showMessageDialog(input, ex.getMessage(), "", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            return true;
        }

    }
}
