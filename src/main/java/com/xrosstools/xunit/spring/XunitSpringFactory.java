package com.xrosstools.xunit.spring;

import com.xrosstools.xunit.Converter;
import com.xrosstools.xunit.Processor;
import com.xrosstools.xunit.Unit;
import com.xrosstools.xunit.XunitFactory;

import java.net.URI;
import java.util.Map;

public class XunitSpringFactory extends XunitFactory {
    private URI uri;

    private String packageId;
    private String name;
    private Map<String, String> applicationProperties;
    private Map<String, Unit> unitRepo;

    public XunitSpringFactory(URI uri) {
        this.uri = uri;
    }

    public URI getUri() {
        return uri;
    }

    @Override
    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setApplicationProperties(Map<String, String> applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public void setUnitRepo(Map<String, Unit> unitRepo) {
        this.unitRepo = unitRepo;
    }

    @Override
    public Unit getUnit(String id){
        return unitRepo.get(id);
    }

    @Override
    public Processor getProcessor(String id){
        return (Processor)getUnit(id);
    }

    @Override
    public Converter getConverter(String id){
        return (Converter )getUnit(id);
    }
}
