package com.xrosstools.xunit.spring.unit;

import com.xrosstools.xunit.Unit;
import com.xrosstools.xunit.impl.ChainImpl;

import java.util.List;

public class DefaultChainImpl extends ChainImpl {
    public void setUnits(List<Unit> units) {
        if(units == null) {
            return;
        }
        for(Unit unit : units) {
            add(unit);
        }
    }
}
