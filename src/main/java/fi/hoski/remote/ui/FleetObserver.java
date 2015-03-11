/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package fi.hoski.remote.ui;

import fi.hoski.datastore.repository.DataObjectObserver;
import fi.hoski.datastore.repository.RaceFleet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Timo Vesalainen
 */
public class FleetObserver implements DataObjectObserver<RaceFleet>
{
    private final Map<RaceFleet,String> oldMap = new HashMap<>();
    private final Map<RaceFleet,String> newMap = new HashMap<>();

    public FleetObserver(Collection<RaceFleet> list)
    {
        for (RaceFleet rf : list)
        {
            rf.addObserver(this);
            oldMap.put(rf, rf.getKeyName());
        }
    }
    
    @Override
    public void changed(RaceFleet raceFleet, String property, Object oldValue, Object newValue)
    {
        if (RaceFleet.EventDate.equals(property))
        {
            newMap.put(raceFleet, raceFleet.getKeyName());
        }
    }
    
    public Map<String,String> getChangeMap()
    {
        Map<String,String> map = new HashMap<>();
        for (RaceFleet nf : newMap.keySet())
        {
            String oldKey = oldMap.get(nf);
            if (oldKey == null)
            {
                throw new IllegalArgumentException(nf+" not found in old keys");
            }
            String newKey = newMap.get(nf);
            map.put(oldKey, newKey);
        }
        return map;
    }
    
}
