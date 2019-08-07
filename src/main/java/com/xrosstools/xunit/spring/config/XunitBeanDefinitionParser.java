package com.xrosstools.xunit.spring.config;

import com.xrosstools.xunit.*;
import com.xrosstools.xunit.impl.*;
import com.xrosstools.xunit.spring.XunitSpringFactory;
import com.xrosstools.xunit.spring.unit.DefaultBranchImpl;
import com.xrosstools.xunit.spring.unit.DefaultChainImpl;
import org.springframework.beans.NotWritablePropertyException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.*;
import java.net.URI;
import java.util.*;

public class XunitBeanDefinitionParser implements XunitConstants {

    private XunitConfigBeanPostProcessor processor;
    private BeanDefinitionHolder factoryBeanDefHolder;
    private Resource resource;
    private URI resourceUri;
    private Document doc;

    private Map<String, String> applicationProperties;

    private static Set<String> WRAPPED_UNITS = new HashSet<>();
    static {
        WRAPPED_UNITS.add(DECORATOR_UNIT);
        WRAPPED_UNITS.add(ADAPTER_UNIT);
        WRAPPED_UNITS.add(VALID_UNIT);
        WRAPPED_UNITS.add(INVALID_UNIT);
        WRAPPED_UNITS.add(LOOP_UNIT);
        WRAPPED_UNITS.add(BRANCH_UNIT);
    }

    public XunitBeanDefinitionParser(XunitConfigBeanPostProcessor processor, Resource resource) {
        this.processor = processor;
        this.resource = resource;
        initialize();
    }

    private void initialize() {
        InputStream in = null;
        try {
            resourceUri = resource.getURI();
            in = resource.getInputStream();
            doc= DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);

            this.factoryBeanDefHolder = createFactoryNode(doc);
        } catch (Exception e) {
            throw new BeanDefinitionStoreException("Error to parse Xunit definition : " + resource.getFilename(), e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            }catch (IOException e) {

            }
        }
    }

    public BeanDefinitionHolder getFactoryBeanDefinition() {
        return this.factoryBeanDefHolder;
    }

    public Set<BeanDefinitionHolder> parse() {
        return readUnits(doc);
    }

    private BeanDefinitionHolder createFactoryNode(Document doc){
        Element root = doc.getDocumentElement();
        String packageId = root.getAttribute(PACKAGE_ID);
        String name = root.getAttribute(NAME);
        applicationProperties = createApplicationProperties(doc);

        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(XunitSpringFactory.class);
        builder.setRole(BeanDefinition.ROLE_APPLICATION)
                .addConstructorArgValue(this.resourceUri)
                .addPropertyValue(PACKAGE_ID, packageId)
                .addPropertyValue(NAME, name)
                .addPropertyValue("applicationProperties", applicationProperties);

        return new BeanDefinitionHolder(builder.getBeanDefinition(), generateFactoryBeanName(packageId, name));
    }

    private Set<BeanDefinitionHolder> readUnits(Document doc) {
        List<Node> unitNodes = getValidChildNodes(doc.getElementsByTagName(UNITS).item(0));
        Set<BeanDefinitionHolder> unitBeans = new HashSet<>();
        ManagedMap<String, RuntimeBeanReference> childBeans = new ManagedMap<>();
        for(Node node : unitNodes){
            BeanDefinitionHolder childNodeBean = createUnitNode(node, unitBeans, factoryBeanDefHolder.getBeanName());
            if (childNodeBean != null) {
                childBeans.put(getAttribute(node, NAME), new RuntimeBeanReference(childNodeBean.getBeanName()));
            }
        }
        factoryBeanDefHolder.getBeanDefinition().getPropertyValues().addPropertyValue("unitRepo", childBeans);

        return unitBeans;
    }

    private BeanDefinitionHolder createUnitNode(Node node, Set<BeanDefinitionHolder> unitBeans, String beanNamePrefix){
        if(node == null) {
            return null;
        }
        BeanDefinitionHolder unitBeanDef = createPrimaryUnit(node, unitBeans, beanNamePrefix);
        if(unitBeanDef != null) {
            unitBeans.add(unitBeanDef);
            return unitBeanDef;
        }
        if(WRAPPED_UNITS.contains(node.getNodeName())) {
            return createUnitNode(getFirstValidNode(node), unitBeans, beanNamePrefix);
        }

        return null;
    }

    private BeanDefinitionHolder createPrimaryUnit(Node node, Set<BeanDefinitionHolder> unitBeans, String beanNamePrefix){
        BeanDefinition unitBeanDef = null;
        switch(node.getNodeName()) {
            case PROCESSOR:
                unitBeanDef = createProcessorNode(node, unitBeans, beanNamePrefix);
                break;
            case CONVERTER:
                unitBeanDef = createConverterNode(node, unitBeans, beanNamePrefix);
                break;
            case DECORATOR:
                unitBeanDef = createDecoratorNode(node, unitBeans, beanNamePrefix);
                break;
            case ADAPTER:
                unitBeanDef = createAdapterNode(node, unitBeans, beanNamePrefix);
                break;
            case VALIDATOR:
                unitBeanDef = createValidatorNode(node, unitBeans, beanNamePrefix);
                break;
            case LOCATOR:
                unitBeanDef = createLocatorNode(node, unitBeans, beanNamePrefix);
                break;
            case CHAIN:
                unitBeanDef = createChainNode(node, unitBeans, beanNamePrefix);
                break;
            case BI_BRANCH:
                unitBeanDef = createBiBranchNode(node, unitBeans, beanNamePrefix);
                break;
            case BRANCH:
                unitBeanDef = createBranchNode(node, unitBeans, beanNamePrefix);
                break;
            case WHILE:
                unitBeanDef = createPreValidationLoopNode(node, unitBeans, beanNamePrefix);
                break;
            case DO_WHILE:
                unitBeanDef = createPostValidationLoopNode(node, unitBeans, beanNamePrefix);
                break;
                default:
                    break;
        }
        if(unitBeanDef != null) {
            String beanName = generateUnitBeanName(beanNamePrefix, getAttribute(node, NAME));
            return new BeanDefinitionHolder(unitBeanDef, beanName);
        }
        return null;
    }

    private BeanDefinitionHolder createChildNode(Node node, String name, Set<BeanDefinitionHolder> unitBeans, String beanNamePrefix){
        NodeList children = node.getChildNodes();
        Node found = null;
        for(int i = 0; i < children.getLength(); i++){
            if(!isValidNode(children.item(i), name)) {
                continue;
            }
            found = children.item(i);
            break;
        }
        return createUnitNode(found, unitBeans, beanNamePrefix);
    }

    private BeanDefinitionBuilder createGenericNode(Node node, Class<?> defaultClass) {
        BeanDefinitionBuilder builder;

        String className = getAttribute(node, CLASS);
        String reference = getAttribute(node, REFERENCE);
        Class<?> beanClass = null;
        if(!isEmptyOrDefault(className)) {
            beanClass = ClassUtils.resolveClassName(className, processor.getClassLoader());
            builder = BeanDefinitionBuilder.genericBeanDefinition(beanClass);
        } else if(!isEmptyOrDefault(reference)) {
            builder = BeanDefinitionBuilder.genericBeanDefinition();
            String module = getAttribute(node, MODULE);
            if(isEmptyOrDefault(module)) {
                builder.setParentName(generateUnitBeanName(this.factoryBeanDefHolder.getBeanName(), reference));
            } else {
                BeanDefinitionHolder refFactoryHolder = processor.resolveFactoryBeanDefinition(module);
                builder.setParentName(generateUnitBeanName(refFactoryHolder.getBeanName(), reference));
            }
        } else {
            beanClass = defaultClass;
            builder = BeanDefinitionBuilder.genericBeanDefinition(defaultClass);
            if(defaultClass.equals(DefaultUnitImpl.class)) {
                builder.addConstructorArgValue(null);
            }
        }

        if(beanClass != null) {
            if(ApplicationPropertiesAware.class.isAssignableFrom(beanClass)) {
                builder.addPropertyValue("applicationProperties", new LinkedHashMap<>(applicationProperties));
            }
            if(UnitPropertiesAware.class.isAssignableFrom(beanClass)) {
                builder.addPropertyValue("unitProperties", getProperties(node));
            }
        }
        builder.setRole(BeanDefinition.ROLE_APPLICATION);
        builder.setScope(BeanDefinition.SCOPE_SINGLETON);

        return builder;
    }

    private BeanDefinition createProcessorNode(Node node, Set<BeanDefinitionHolder> unitBeans, String beanNamePrefix){
        return createGenericNode(node, DefaultUnitImpl.class).getBeanDefinition();
    }

    private BeanDefinition createConverterNode(Node node, Set<BeanDefinitionHolder> unitBeans, String beanNamePrefix){
        return createGenericNode(node, DefaultUnitImpl.class).getBeanDefinition();
    }

    private BeanDefinition createDecoratorNode(Node node, Set<BeanDefinitionHolder> unitBeans, String beanNamePrefix){
        BeanDefinitionBuilder decoratorBuilder = createGenericNode(node, DefaultUnitImpl.class);
        String decoratorBeanName = generateUnitBeanName(beanNamePrefix, getAttribute(node, NAME));
        BeanDefinitionHolder childNodeBean = createChildNode(node, DECORATOR_UNIT, unitBeans, decoratorBeanName);
        if(childNodeBean != null) {
            decoratorBuilder.addPropertyReference("unit", childNodeBean.getBeanName());
        }

        return decoratorBuilder.getBeanDefinition();
    }

    private BeanDefinition createAdapterNode(Node node, Set<BeanDefinitionHolder> unitBeans, String beanNamePrefix){
        BeanDefinitionBuilder adapterBuilder = createGenericNode(node, DefaultUnitImpl.class);
        String adapterBeanName = generateUnitBeanName(beanNamePrefix, getAttribute(node, NAME));
        BeanDefinitionHolder childNodeBean = createChildNode( node, ADAPTER_UNIT, unitBeans, adapterBeanName);
        if(childNodeBean != null) {
            adapterBuilder.addPropertyReference("unit", childNodeBean.getBeanName());
        }

        return adapterBuilder.getBeanDefinition();
    }

    private BeanDefinition createValidatorNode(Node node, Set<BeanDefinitionHolder> unitBeans, String beanNamePrefix){
        return createGenericNode(node, DefaultUnitImpl.class).getBeanDefinition();
    }

    private BeanDefinition createLocatorNode(Node node, Set<BeanDefinitionHolder> unitBeans, String beanNamePrefix){
        BeanDefinitionBuilder locatorBuilder = createGenericNode(node, DefaultUnitImpl.class);
        locatorBuilder.addPropertyValue("defaultKey", getAttribute(node, DEFAULT_KEY));
        return locatorBuilder.getBeanDefinition();
    }

    private BeanDefinition createChainNode(Node node, Set<BeanDefinitionHolder> unitBeans, String beanNamePrefix){
        BeanDefinitionBuilder chainDefBuilder = createGenericNode(node, DefaultChainImpl.class);
        String chainBeanName = generateUnitBeanName(beanNamePrefix, getAttribute(node, NAME));
        List<Node> children = getValidChildNodes(node);
        Set<String> childBeanNames = new LinkedHashSet<>(children.size());
        for (Node child : children) {
            BeanDefinitionHolder childNodeBean = createUnitNode(child, unitBeans, chainBeanName);
            if (childNodeBean != null) {
                childBeanNames.add(childNodeBean.getBeanName());
            }
        }

        Class<?> beanClass = chainDefBuilder.getBeanDefinition().getBeanClass();
        Constructor<?>[] constructors = beanClass.getDeclaredConstructors();
        boolean hasCandidateConstructor = false;
        for(Constructor<?> constructor : constructors) {
            Type[] parameterTypes = constructor.getGenericParameterTypes();
            if(!Modifier.isPublic(constructor.getModifiers()) || parameterTypes == null || parameterTypes.length == 0) {
                continue;
            }
            if (parameterTypes[0] instanceof ParameterizedType) {
                Type rawType = ((ParameterizedType) parameterTypes[0]).getRawType();
                Type[] actualTypes = ((ParameterizedType) parameterTypes[0]).getActualTypeArguments();
                if (rawType.equals(List.class) && Unit.class.equals(actualTypes[0])) {
                    chainDefBuilder.addConstructorArgValue(toRuntimeBeanReferences(childBeanNames.toArray(new String[0])));
                    hasCandidateConstructor = true;
                    break;
                }
            }
        }
        if(!hasCandidateConstructor) {
            boolean hasSetMethod = false;
            Method setMethod = ClassUtils.getMethodIfAvailable(beanClass, "setUnits", List.class);
            if(setMethod != null && Modifier.isPublic(setMethod.getModifiers())
                    && setMethod.getGenericParameterTypes()[0] instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType)setMethod.getGenericParameterTypes()[0];
                Type rawType = paramType.getRawType();
                Type[] actualTypes = paramType.getActualTypeArguments();
                if (rawType.equals(List.class) && Unit.class.equals(actualTypes[0])) {
                    chainDefBuilder.addPropertyValue("units", toRuntimeBeanReferences(childBeanNames.toArray(new String[0])));
                    hasSetMethod = true;
                }
            }
            if(!hasSetMethod) {
                throw new NotWritablePropertyException(beanClass, "units", "No matched constructor or setter method.");
            }
        }

        return chainDefBuilder.getBeanDefinition();
    }

    private BeanDefinition createBiBranchNode(Node node, Set<BeanDefinitionHolder> unitBeans, String beanNamePrefix){
        BeanDefinitionBuilder biBranchDefBuilder = createGenericNode(node, BiBranchImpl.class);
        String biBranchBeanName = generateUnitBeanName(beanNamePrefix, getAttribute(node, NAME));
        BeanDefinitionHolder validatorNode = createChildNode(node, VALIDATOR, unitBeans, biBranchBeanName);
        BeanDefinitionHolder validNode = createChildNode(node, VALID_UNIT, unitBeans, biBranchBeanName);
        BeanDefinitionHolder invalidNode = createChildNode(node, INVALID_UNIT, unitBeans, biBranchBeanName);

        if(validatorNode != null) {
            biBranchDefBuilder.addPropertyReference("validator", validatorNode.getBeanName());
        }
        if(validNode != null) {
            biBranchDefBuilder.addPropertyReference("validUnit", validNode.getBeanName());
        }
        if(invalidNode != null) {
            biBranchDefBuilder.addPropertyReference("invalidUnit", invalidNode.getBeanName());
        }

        return biBranchDefBuilder.getBeanDefinition();
    }

    private BeanDefinition createBranchNode(Node node, Set<BeanDefinitionHolder> unitBeans, String beanNamePrefix){
        BeanDefinitionBuilder branchDefBuilder = createGenericNode(node, DefaultBranchImpl.class);
        String branchBeanName = generateUnitBeanName(beanNamePrefix, getAttribute(node, NAME));

        BeanDefinitionHolder locatorNode = createChildNode(node, LOCATOR, unitBeans, branchBeanName);
        List<Node> children = getValidChildNodes(node);
        ManagedMap<String, RuntimeBeanReference> childBeans = new ManagedMap<>();
        for (Node child : children) {
            if (!isValidNode(child, BRANCH_UNIT)) {
                continue;
            }
            BeanDefinitionHolder branchUnit = createUnitNode(child, unitBeans, branchBeanName);
            if (branchUnit != null) {
                childBeans.put(getAttribute(child, KEY), new RuntimeBeanReference(branchUnit.getBeanName()));
            }
        }

        Class<?> branchBeanClass = branchDefBuilder.getBeanDefinition().getBeanClass();
        Constructor<?>[] constructors = branchBeanClass.getConstructors();
        boolean hasCandidateConstructor = false;
        for(Constructor<?> constructor : constructors) {
            Type[] parameterTypes = constructor.getGenericParameterTypes();
            if(!Modifier.isPublic(constructor.getModifiers()) || parameterTypes == null || parameterTypes.length < 2) {
                continue;
            }
            if (parameterTypes[0].equals(Locator.class) && parameterTypes[1] instanceof ParameterizedType) {
                Type rawType = ((ParameterizedType) parameterTypes[1]).getRawType();
                Type[] actualTypes = ((ParameterizedType) parameterTypes[1]).getActualTypeArguments();
                if (rawType.equals(Map.class) && String.class.equals(actualTypes[0]) && Unit.class.equals(actualTypes[1])) {
                    branchDefBuilder.addConstructorArgReference(locatorNode.getBeanName());
                    branchDefBuilder.addConstructorArgValue(childBeans);
                    hasCandidateConstructor = true;
                    break;
                }
            }
        }
        if(!hasCandidateConstructor) {
            branchDefBuilder.addPropertyReference("locator", locatorNode.getBeanName());
            boolean hasSetMethod = false;
            Method setMethod = ClassUtils.getMethodIfAvailable(branchBeanClass, "setUnitMap", Map.class);
            if(setMethod != null && Modifier.isPublic(setMethod.getModifiers())
                    && setMethod.getGenericParameterTypes()[0] instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType)setMethod.getGenericParameterTypes()[0];
                Type rawType = paramType.getRawType();
                Type[] actualTypes = paramType.getActualTypeArguments();
                if (rawType.equals(Map.class) && String.class.equals(actualTypes[0]) && Unit.class.equals(actualTypes[1])) {
                    branchDefBuilder.addPropertyValue("unitMap", childBeans);
                    hasSetMethod = true;
                }
            }
            if(!hasSetMethod) {
                throw new NotWritablePropertyException(branchBeanClass, "unitMap", "No matched constructor or setter method.");
            }
        }

        return branchDefBuilder.getBeanDefinition();
    }

    private BeanDefinition createPreValidationLoopNode(Node node, Set<BeanDefinitionHolder> unitBeans, String beanNamePrefix){
        BeanDefinitionBuilder whileLoopBuilder = createGenericNode(node, PreValidationLoopImpl.class);
        String beanName = generateUnitBeanName(beanNamePrefix, getAttribute(node, NAME));

        BeanDefinitionHolder validatorNode = createChildNode(node, VALIDATOR, unitBeans, beanName);
        BeanDefinitionHolder unitNode = createChildNode(node, LOOP_UNIT, unitBeans, beanName);
        if(validatorNode != null) {
            whileLoopBuilder.addPropertyReference("validator", validatorNode.getBeanName());
        }
        if(unitNode != null) {
            whileLoopBuilder.addPropertyReference("unit", unitNode.getBeanName());
        }

        return whileLoopBuilder.getBeanDefinition();
    }

    private BeanDefinition createPostValidationLoopNode(Node node, Set<BeanDefinitionHolder> unitBeans, String beanNamePrefix){
        BeanDefinitionBuilder doWhileLoopBuilder = createGenericNode(node, PostValidationLoopImpl.class);
        String beanName = generateUnitBeanName(beanNamePrefix, getAttribute(node, NAME));

        BeanDefinitionHolder validatorNode = createChildNode(node, VALIDATOR, unitBeans, beanName);
        BeanDefinitionHolder unitNode = createChildNode(node, LOOP_UNIT, unitBeans, beanName);
        if(validatorNode != null) {
            doWhileLoopBuilder.addPropertyReference("validator", validatorNode.getBeanName());
        }
        if(unitNode != null) {
            doWhileLoopBuilder.addPropertyReference("unit", unitNode.getBeanName());
        }

        return doWhileLoopBuilder.getBeanDefinition();
    }

    private boolean isEmptyOrDefault(String className) {
        if(className == null || className.trim().length() == 0) {
            return true;
        }
        return className.trim().equalsIgnoreCase("default");
    }

    private Map<String, String> createApplicationProperties(Document doc){
        if(doc.getElementsByTagName(PROPERTIES).getLength() == 0) {
            return Collections.emptyMap();
        }
        return getProperties(doc.getElementsByTagName(PROPERTIES).item(0));
    }

    private LinkedHashMap<String, String> getProperties(Node node){
        List<Node> children = getValidChildNodes(node);
        LinkedHashMap<String, String> properties = new LinkedHashMap<>();

        for (Node child : children) {
            if (!isValidNode(child, PROPERTY)) {
                continue;
            }
            properties.put(getAttribute(child, KEY), getAttribute(child, VALUE));
        }

        return properties;
    }

    private String generateFactoryBeanName(String packageId, String name) {
        StringBuilder beanNameBuilder = new StringBuilder();
        if(StringUtils.hasText(name)) {
            if (StringUtils.hasText(packageId)) {
                beanNameBuilder.append(packageId.replaceAll("\\s+", "")).append('.');
            }
            beanNameBuilder.append(name.replaceAll("\\s+", ""));
            return beanNameBuilder.toString();
        }

        String path;
        if(resource instanceof ClassPathResource) {
            path = ((ClassPathResource)resource).getPath();
        } else {
            path = resource.getFilename();
        }
        if(path.endsWith(".xunit")) {
            path = path.substring(0, path.lastIndexOf('.'));
        }

        return path.replace('/', '.');
    }

    private String generateUnitBeanName(String beanNamePrefix, String unitName) {
        if(!StringUtils.hasText(unitName)) {
            throw new BeanDefinitionStoreException("Invalid child node of " + beanNamePrefix + " in "
                    + resource.getFilename() +", property 'name' is null");
        }
        return beanNamePrefix + ":" + unitName.replaceAll("\\s+", "");
    }

    private ManagedList<RuntimeBeanReference> toRuntimeBeanReferences(String... beanNames) {
        ManagedList<RuntimeBeanReference> runtimeBeanReferences = new ManagedList<>();
        for (String beanName : beanNames) {
            runtimeBeanReferences.add(new RuntimeBeanReference(beanName));
        }
        return runtimeBeanReferences;
    }

    private Node getFirstValidNode(Node node) {
        NodeList children = node.getChildNodes();
        for(int i = 0; i < children.getLength(); i++){
            if(isValidNode(children.item(i))) {
                return children.item(i);
            }
        }

        return null;
    }

    private List<Node> getValidChildNodes(Node node) {
        List<Node> nl = new ArrayList<>();
        NodeList nodeList = node.getChildNodes();
        for(int i = 0; i < nodeList.getLength(); i++){
            if(isValidNode(nodeList.item(i))) {
                nl.add(nodeList.item(i));
            }
        }
        return nl;
    }

    private boolean isValidNode(Node node) {
        return !node.getNodeName().equals("#text");
    }

    private boolean isValidNode(Node node, String name) {
        return node.getNodeName().equals(name);
    }

    private String getAttribute(Node node, String attributeName){
        NamedNodeMap map = node.getAttributes();
        for(int i = 0; i < map.getLength(); i++) {
            if (attributeName.equals(map.item(i).getNodeName())) {
                return map.item(i).getNodeValue();
            }
        }

        return null;
    }

}
