package com.xrosstools.xunit.spring.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;

import java.io.IOException;
import java.util.*;

public class XunitConfigBeanPostProcessor implements BeanDefinitionRegistryPostProcessor, EnvironmentAware,
        ResourceLoaderAware, BeanClassLoaderAware {

    private Environment environment;
    private ClassLoader classLoader;
    private ResourcePatternResolver resourcePatternResolver;
    private BeanDefinitionRegistry registry;

    private Set<String> factoryPaths;
    private Map<String, BeanDefinitionHolder> factoryDefinitionMap = new HashMap<>();
    private Map<Resource, BeanDefinitionHolder> resourceMap = new HashMap<>();

    public XunitConfigBeanPostProcessor(Set<String> factoryPaths) {
        this.factoryPaths = factoryPaths;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        this.registry = registry;
        Set<Resource> resources = new HashSet<>();
        for(String path : factoryPaths) {
            resources.addAll(findCandidateResources(environment.resolvePlaceholders(path)));
        }

        for(Resource resource : resources) {
            registerBeanDefinition(resource);
        }
    }

    public BeanDefinitionHolder resolveFactoryBeanDefinition(String path) {
        Set<Resource> resources = findCandidateResources(environment.resolvePlaceholders(path));
        if(!resources.isEmpty()) {
            return registerBeanDefinition(resources.toArray(new Resource[1])[0]);
        } else {
            throw new BeanDefinitionStoreException( path, "Resource not readable : " + path);
        }
    }

    private BeanDefinitionHolder registerBeanDefinition(Resource resource) {
        if(!resourceMap.containsKey(resource)) {
            XunitBeanDefinitionParser parser = new XunitBeanDefinitionParser(this, resource);
            BeanDefinitionHolder factoryBeanDefinition = parser.getFactoryBeanDefinition();
            if (!factoryDefinitionMap.containsKey(factoryBeanDefinition.getBeanName())) {
                cacheFactoryDefinition(resource, factoryBeanDefinition);

                Set<BeanDefinitionHolder> unitBeans = parser.parse();
                for (BeanDefinitionHolder beanDefHolder : unitBeans) {
                    registry.registerBeanDefinition(beanDefHolder.getBeanName(), beanDefHolder.getBeanDefinition());
                }
                registry.registerBeanDefinition(factoryBeanDefinition.getBeanName(), factoryBeanDefinition.getBeanDefinition());
            } else {
                resourceMap.put(resource, factoryDefinitionMap.get(factoryBeanDefinition.getBeanName()));
            }
        }

        return resourceMap.get(resource);
    }

    private void cacheFactoryDefinition(Resource resource, BeanDefinitionHolder factoryBeanDefinition) {
        resourceMap.put(resource, factoryBeanDefinition);
        factoryDefinitionMap.put(factoryBeanDefinition.getBeanName(), factoryBeanDefinition);
    }

    private Set<Resource> findCandidateResources(String path){
        Set<Resource> resources = new HashSet<>();
        try {
            Resource[] allResources = this.resourcePatternResolver.getResources(path);
            for (Resource resource : allResources) {
                if (resource.isReadable()) {
                    resources.add(resource);
                }
            }
        } catch (IOException e) {
            throw new BeanDefinitionStoreException(path, "Error to read resource", e);
        }
        return resources;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
    }
}
