package com.xrosstools.xunit.spring.unit;

import com.xrosstools.xunit.Context;
import com.xrosstools.xunit.Unit;
import com.xrosstools.xunit.impl.ChainImpl;
import com.xrosstools.xunit.spring.service.PrintService;

import java.util.List;

public class MyChainImpl extends ChainImpl {

    private PrintService printService;

    public MyChainImpl(List<Unit> units, PrintService printService) {
        for(Unit unit : units) {
            super.add(unit);
        }
        this.printService = printService;
    }

    public void process(Context ctx) {
        printService.print("MyChainImpl.process()");
        super.process(ctx);
    }

    public Context convert(Context inputCtx) {
        printService.print("MyChainImpl.convert()");
        return super.convert(inputCtx);
    }

}
