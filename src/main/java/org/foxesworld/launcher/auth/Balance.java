package org.foxesworld.launcher.auth;

import java.util.HashMap;
import java.util.Map;

public class Balance {
    private int crystals;
    private int units;

    public int getCrystals() {
        return crystals;
    }

    public int getUnits() {
        return units;
    }

    public Map<String, Integer> toMap() {
        Map<String, Integer> balanceMap = new HashMap<>();
        balanceMap.put("crystals", crystals);
        balanceMap.put("units", units);
        return balanceMap;
    }
}
