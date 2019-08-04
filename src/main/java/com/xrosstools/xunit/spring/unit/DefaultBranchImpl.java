package com.xrosstools.xunit.spring.unit;

import com.xrosstools.xunit.Unit;
import com.xrosstools.xunit.impl.BranchImpl;

import java.util.Map;

public class DefaultBranchImpl extends BranchImpl {
    public void setUnitMap(Map<String, Unit> unitMap) {
        if(unitMap == null) {
            return;
        }
        for(String key : unitMap.keySet()) {
            add(key, unitMap.get(key));
        }
    }
}
