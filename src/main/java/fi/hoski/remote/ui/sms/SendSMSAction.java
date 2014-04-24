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

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.PhoneNumber;
import fi.hoski.remote.ui.TextUtil;
import fi.hoski.sms.SMSException;
import fi.hoski.sms.zoner.ZonerSMSService;
import java.io.IOException;
import org.vesalainen.parsers.sql.dsql.ui.plugin.AbstractSendAction;

/**
 * @author Timo Vesalainen
 */
public class SendSMSAction extends AbstractSendAction<PhoneNumber>
{
    private final ZonerSMSService smsService;

    public SendSMSAction(String username, String password)
    {
        super(TextUtil.getText("SEND SMS"));
        smsService = new ZonerSMSService(username, password);
    }

    @Override
    protected void sendTo(String phoneNumber) throws IOException
    {
        try
        {
            String body = dialog.getBody();
            if (body != null)
            {
                body = replaceTags(body);
                smsService.send(phoneNumber, body);
            }
            else
            {
                throw new IOException(TextUtil.getText("EMPTY MESSAGE"));
            }
        }
        catch (SMSException ex)
        {
            throw new IOException(ex);
        }
    }

}
