package com.vercarus.sb322jdk21.backend.integration.core.springboot.extended;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.AutowireCandidateResolver;
import org.springframework.lang.Nullable;

import java.util.function.Function;

public class AutowireCandidateResolverExtended implements AutowireCandidateResolver {
    private AutowireCandidateResolver parentAutowireCandidateResolver;
    private Class targetClass;
    private Function targetFunction;

    public AutowireCandidateResolverExtended(AutowireCandidateResolver parentAutowireCandidateResolver, Class targetClass, Function targetFunction) {
        this.parentAutowireCandidateResolver = parentAutowireCandidateResolver;
        this.targetClass = targetClass;
        this.targetFunction = targetFunction;
    }

    @Override
    public boolean isAutowireCandidate(BeanDefinitionHolder beanDefinitionHolder, DependencyDescriptor descriptor) {
        return parentAutowireCandidateResolver.isAutowireCandidate(beanDefinitionHolder, descriptor);
    }

    @Override
    public Object getSuggestedValue(DependencyDescriptor descriptor) {
        Class dependencyClass = descriptor.getDependencyType();
        if (targetClass.isAssignableFrom(dependencyClass)) {
            return targetFunction.apply(dependencyClass);
        }
        return parentAutowireCandidateResolver.getSuggestedValue(descriptor);
    }

    @Override
    public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, @Nullable String beanName) {
        return parentAutowireCandidateResolver.getLazyResolutionProxyIfNecessary(descriptor, beanName);
    }

    @Override
    public boolean isRequired(DependencyDescriptor descriptor) {
        return parentAutowireCandidateResolver.isRequired(descriptor);
    }

}
