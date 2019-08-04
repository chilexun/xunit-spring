package com.xrosstools.xunit.spring.unit;

import com.xrosstools.xunit.Context;
import com.xrosstools.xunit.Locator;
import com.xrosstools.xunit.Unit;
import com.xrosstools.xunit.impl.BranchImpl;
import com.xrosstools.xunit.spring.service.PrintService;

import java.util.Map;

public class MyBranchImpl extends BranchImpl {

    private PrintService printService;

    public MyBranchImpl(Locator locator, Map<String, Unit> unitMap, PrintService printService) {
        super();
        super.setLocator(locator);
        for(String key : unitMap.keySet()) {
            super.add(key, unitMap.get(key));
        }
        this.printService = printService;
    }

    public void process(Context ctx) {
        printService.print("MyBranchImpl.process()");
        super.process(ctx);
    }

    public Context convert(Context inputCtx) {
        printService.print("MyBranchImpl.convert()");
        return super.convert(inputCtx);
    }
}
