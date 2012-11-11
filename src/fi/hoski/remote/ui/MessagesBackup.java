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

import fi.hoski.datastore.repository.Messages;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;
import javax.swing.JFrame;

/**
 * @author Timo Vesalainen
 */
public class MessagesBackup extends BackupBase
{

    public MessagesBackup(JFrame frame)
    {
        super(frame, new PropertyFileFilter());
    }

    @Override
    protected void load(File propertiesFile) throws IOException
    {
        Properties properties = new Properties();
        if (propertiesFile.exists())
        {
            try (FileInputStream fis = new FileInputStream(propertiesFile))
            {
                if (propertiesFile.getName().endsWith(".xml"))
                {
                    properties.loadFromXML(fis);
                }
                else
                {
                    properties.load(fis);
                }
            }
        }
        Messages messages = dss.getMessages();
        for (String prop : properties.stringPropertyNames())
        {
            messages.set(prop, properties.getProperty(prop));
        }
        dss.putMessages(messages);
    }

    @Override
    protected void store(File propertiesFile) throws IOException
    {
        Properties properties = new Properties();
        Messages messages = dss.getMessages();
        for (String property : messages.getModel().getPropertyList())
        {
            String value = messages.getString(property);
            if (value != null)
            {
                properties.setProperty(property, value);
            }
        }
        try (FileOutputStream fos = new FileOutputStream(propertiesFile))
        {
            Date now = new Date();
            String comment = "updated at "+now;
            if (propertiesFile.getName().endsWith(".xml"))
            {
                properties.storeToXML(fos, comment);
            }
            else
            {
                properties.store(fos, comment);
            }
        }
    }
}
