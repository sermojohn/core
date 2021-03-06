/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.weld.bootstrap.events.configurator;

import static org.jboss.weld.util.Preconditions.checkArgumentNotNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.configurator.BeanAttributesConfigurator;
import javax.enterprise.util.TypeLiteral;
import javax.inject.Named;

import org.jboss.weld.bean.attributes.ImmutableBeanAttributes;
import org.jboss.weld.exceptions.UnsupportedOperationException;
import org.jboss.weld.util.Beans;
import org.jboss.weld.util.collections.ImmutableSet;
import org.jboss.weld.util.reflection.HierarchyDiscovery;

/**
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
public class BeanAttributesConfiguratorImpl<T> implements BeanAttributesConfigurator<T>, Configurator<BeanAttributes<T>> {

    static final Set<Annotation> DEFAULT_QUALIFIERS = ImmutableSet.of(Any.Literal.INSTANCE, Default.Literal.INSTANCE);

    private String name;

    private final Set<Annotation> qualifiers;

    private Class<? extends Annotation> scope;

    private final Set<Class<? extends Annotation>> stereotypes;

    private Set<Type> types;

    private boolean isAlternative;

    public BeanAttributesConfiguratorImpl() {
        this.qualifiers = new HashSet<Annotation>();
        this.types = new HashSet<Type>();
        this.types.add(Object.class);
        this.stereotypes = new HashSet<Class<? extends Annotation>>();
    }

    /**
     *
     * @param beanAttributes
     */
    public BeanAttributesConfiguratorImpl(BeanAttributes<T> beanAttributes) {
        this();
        read(beanAttributes);
    }

    @Override
    public <U extends T> BeanAttributesConfigurator<U> read(AnnotatedType<U> type) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public <U extends T> BeanAttributesConfigurator<U> read(AnnotatedMember<U> member) {
        // TODO
        throw new UnsupportedOperationException();
    }

    @Override
    public BeanAttributesConfigurator<T> read(BeanAttributes<?> beanAttributes) {
        checkArgumentNotNull(beanAttributes);
        name(beanAttributes.getName());
        qualifiers(beanAttributes.getQualifiers());
        scope(beanAttributes.getScope());
        stereotypes(beanAttributes.getStereotypes());
        types(beanAttributes.getTypes());
        alternative(beanAttributes.isAlternative());
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> addType(Type type) {
        checkArgumentNotNull(type);
        this.types.add(type);
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> addType(TypeLiteral<?> typeLiteral) {
        checkArgumentNotNull(typeLiteral);
        this.types.add(typeLiteral.getType());
        return null;
    }

    @Override
    public BeanAttributesConfigurator<T> addTypes(Type... types) {
        checkArgumentNotNull(types);
        Collections.addAll(this.types, types);
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> addTypes(Set<Type> types) {
        checkArgumentNotNull(types);
        this.types.addAll(types);
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> addTransitiveTypeClosure(Type type) {
        checkArgumentNotNull(type);
        this.types.addAll(Beans.getLegalBeanTypes(new HierarchyDiscovery(type).getTypeClosure(), type));
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> types(Type... types) {
        this.types.clear();
        return addTypes(types);
    }

    @Override
    public BeanAttributesConfigurator<T> types(Set<Type> types) {
        this.types.clear();
        return addTypes(types);
    }

    @Override
    public BeanAttributesConfigurator<T> scope(Class<? extends Annotation> scope) {
        checkArgumentNotNull(scope);
        this.scope = scope;
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> addQualifier(Annotation qualifier) {
        checkArgumentNotNull(qualifier);
        removeDefaultQualifierIfNeeded(qualifier);
        qualifiers.add(qualifier);
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> addQualifiers(Annotation... qualifiers) {
        checkArgumentNotNull(qualifiers);
        for (Annotation annotation : qualifiers) {
            removeDefaultQualifierIfNeeded(annotation);
        }
        Collections.addAll(this.qualifiers, qualifiers);
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> addQualifiers(Set<Annotation> qualifiers) {
        checkArgumentNotNull(qualifiers);
        for (Annotation annotation : qualifiers) {
            removeDefaultQualifierIfNeeded(annotation);
        }
        this.qualifiers.addAll(qualifiers);
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> qualifiers(Annotation... qualifiers) {
        this.qualifiers.clear();
        return addQualifiers(qualifiers);
    }

    @Override
    public BeanAttributesConfigurator<T> qualifiers(Set<Annotation> qualifiers) {
        this.qualifiers.clear();
        return addQualifiers(qualifiers);
    }

    @Override
    public BeanAttributesConfigurator<T> addStereotype(Class<? extends Annotation> stereotype) {
        checkArgumentNotNull(stereotype);
        this.stereotypes.add(stereotype);
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> addStereotypes(Set<Class<? extends Annotation>> stereotypes) {
        checkArgumentNotNull(stereotypes);
        this.stereotypes.addAll(stereotypes);
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> stereotypes(Set<Class<? extends Annotation>> stereotypes) {
        this.stereotypes.clear();
        return addStereotypes(stereotypes);
    }

    @Override
    public BeanAttributesConfigurator<T> name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public BeanAttributesConfigurator<T> alternative(boolean alternative) {
        this.isAlternative = alternative;
        return this;
    }

    @Override
    public BeanAttributes<T> complete() {
        return new ImmutableBeanAttributes<T>(ImmutableSet.copyOf(stereotypes), isAlternative, name, initQualifiers(qualifiers), ImmutableSet.copyOf(types),
                initScope());
    }

    private void removeDefaultQualifierIfNeeded(Annotation qualifier) {
        if (!qualifier.annotationType().equals(Named.class)) {
            qualifiers.remove(Default.Literal.INSTANCE);
        }
    }

    private Class<? extends Annotation> initScope() {
        return scope != null ? scope : Dependent.class;
    }

    private Set<Annotation> initQualifiers(Set<Annotation> qualifiers) {
        if (qualifiers.isEmpty()) {
            return DEFAULT_QUALIFIERS;
        }
        Set<Annotation> normalized = new HashSet<Annotation>(qualifiers);
        normalized.remove(Any.Literal.INSTANCE);
        normalized.remove(Default.Literal.INSTANCE);
        if (normalized.isEmpty()) {
            normalized = DEFAULT_QUALIFIERS;
        } else {
            ImmutableSet.Builder<Annotation> builder = ImmutableSet.builder();
            if (normalized.size() == 1) {
                if (qualifiers.iterator().next().annotationType().equals(Named.class)) {
                    builder.add(Default.Literal.INSTANCE);
                }
            }
            builder.add(Any.Literal.INSTANCE);
            builder.addAll(qualifiers);
            normalized = builder.build();
        }
        return normalized;
    }

}
