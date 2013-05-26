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

package fi.hoski.remote.ui.sms;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PhoneNumber;
import fi.hoski.remote.ui.TextUtil;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import org.vesalainen.parsers.sql.dsql.ui.action.PersistenceHandler;
import org.vesalainen.parsers.sql.dsql.ui.plugin.AbstractMessagePlugin;

/**
 * @author Timo Vesalainen
 */
public class SMSPlugin extends AbstractMessagePlugin<PhoneNumber>
{
    public static final String SMSProperty = SMSPlugin.class.getName()+".body";

    public SMSPlugin(String username, String password)
    {
        super(
                TextUtil.getText("SMS"), 
                new SendSMSAction(username, password), 
                PhoneNumber.class);
    }

    @Override
    public void vetoableChange(PropertyChangeEvent evt) throws PropertyVetoException
    {
        Entity entity = (Entity) evt.getNewValue();
        switch (evt.getPropertyName())
        {
            case PersistenceHandler.OPEN:
                if (entity != null)
                {
                    // Open
                    dialog.setBody((String) entity.getProperty(SMSProperty));
                }
                else
                {
                    // New
                    dialog.setBody(null);
                }
                break;
            case PersistenceHandler.SAVE:
                if (entity != null)
                {
                    // Save
                    String body = dialog.getBody();
                    if (body != null && !body.isEmpty())
                    {
                        entity.setUnindexedProperty(SMSProperty, body);
                    }
                    else
                    {
                        entity.removeProperty(SMSProperty);
                    }
                }
                else
                {
                    // Remove
                    dialog.setBody(null);
                }
                break;
        }
    }

    @Override
    public boolean activate(Entity entity)
    {
        return entity.hasProperty(SMSProperty);
    }

    @Override
    public String getString(PhoneNumber phoneNumber)
    {
        return phoneNumber.getNumber();
    }

}
