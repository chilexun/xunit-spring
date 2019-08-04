package com.xrosstools.xunit.spring.config;

import com.xrosstools.xunit.spring.annotation.EnableXunit;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.*;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.util.*;

public class XunitComponentRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {
        Set<String> factoryPaths = getFactoryPathsToLoad(annotationMetadata);
        registerConfigBeanPostProcessor(factoryPaths, registry);
    }

    private void registerConfigBeanPostProcessor(Set<String> factoryPaths, BeanDefinitionRegistry registry) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(XunitConfigBeanPostProcessor.class);
        builder.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        builder.addConstructorArgValue(factoryPaths);
        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
        BeanDefinitionReaderUtils.registerWithGeneratedName(beanDefinition, registry);
    }

    private Set<String> getFactoryPathsToLoad(AnnotationMetadata metadata) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(metadata.getAnnotationAttributes(EnableXunit.class.getName()));
        String[] paths = attributes.getStringArray("paths");
        Set<String> pathSet = new HashSet<>(paths.length);
        Collections.addAll(pathSet, paths);
        return pathSet;
    }

}
