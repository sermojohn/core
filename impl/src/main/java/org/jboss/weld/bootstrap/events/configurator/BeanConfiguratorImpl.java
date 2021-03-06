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
import static org.jboss.weld.util.reflection.Reflections.cast;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanAttributes;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.PassivationCapable;
import javax.enterprise.inject.spi.configurator.BeanConfigurator;
import javax.enterprise.util.TypeLiteral;

import org.jboss.weld.bean.BeanIdentifiers;
import org.jboss.weld.bootstrap.BeanDeploymentFinder;
import org.jboss.weld.manager.BeanManagerImpl;
import org.jboss.weld.util.bean.ForwardingBeanAttributes;
import org.jboss.weld.util.collections.ImmutableSet;
import org.jboss.weld.util.reflection.Formats;

/**
 *
 * @author Martin Kouba
 *
 * @param <T>
 */
public class BeanConfiguratorImpl<T> implements BeanConfigurator<T>, Configurator<Bean<T>> {

    private BeanManagerImpl beanManager;

    private Class<?> beanClass;

    private final Set<InjectionPoint> injectionPoints;

    private final BeanAttributesConfiguratorImpl<T> attributes;

    private String id;

    private CreateCallback<T> createCallback;

    private DestroyCallback<T> destroyCallback;

    /**
     *
     * @param defaultBeanClass
     * @param beanDeploymentFinder
     */
    public BeanConfiguratorImpl(Class<?> defaultBeanClass, BeanDeploymentFinder beanDeploymentFinder) {
        this.beanClass = defaultBeanClass;
        this.injectionPoints = new HashSet<>();
        this.attributes = new BeanAttributesConfiguratorImpl<>();
        initBeanManager(beanDeploymentFinder);
    }

    @Override
    public BeanConfigurator<T> beanClass(Class<?> beanClass) {
        checkArgumentNotNull(beanClass);
        this.beanClass = beanClass;
        return this;
    }

    @Override
    public BeanConfigurator<T> addInjectionPoint(InjectionPoint injectionPoint) {
        checkArgumentNotNull(injectionPoint);
        this.injectionPoints.add(injectionPoint);
        return this;
    }

    @Override
    public BeanConfigurator<T> addInjectionPoints(InjectionPoint... injectionPoints) {
        checkArgumentNotNull(injectionPoints);
        Collections.addAll(this.injectionPoints, injectionPoints);
        return this;
    }

    @Override
    public BeanConfigurator<T> addInjectionPoints(Set<InjectionPoint> injectionPoints) {
        checkArgumentNotNull(injectionPoints);
        this.injectionPoints.addAll(injectionPoints);
        return this;
    }

    @Override
    public BeanConfigurator<T> injectionPoints(InjectionPoint... injectionPoints) {
        this.injectionPoints.clear();
        return addInjectionPoints(injectionPoints);
    }

    @Override
    public BeanConfigurator<T> injectionPoints(Set<InjectionPoint> injectionPoints) {
        this.injectionPoints.clear();
        return addInjectionPoints(injectionPoints);
    }

    @Override
    public BeanConfigurator<T> id(String id) {
        checkArgumentNotNull(id);
        this.id = id;
        return this;
    }

    @Override
    public <U extends T> BeanConfigurator<U> createWith(Function<CreationalContext<U>, U> callback) {
        checkArgumentNotNull(callback);
        this.createCallback = cast(CreateCallback.fromCreateWith(callback));
        return cast(this);
    }

    @Override
    public <U extends T> BeanConfigurator<U> produceWith(Function<Instance<Object>, U> callback) {
        checkArgumentNotNull(callback);
        this.createCallback = cast(CreateCallback.fromProduceWith(callback));
        return cast(this);
    }

    @Override
    public BeanConfigurator<T> destroyWith(BiConsumer<T, CreationalContext<T>> callback) {
        checkArgumentNotNull(callback);
        this.destroyCallback = DestroyCallback.fromDestroy(callback);
        return this;
    }

    @Override
    public BeanConfigurator<T> disposeWith(BiConsumer<T, Instance<Object>> callback) {
        checkArgumentNotNull(callback);
        this.destroyCallback = DestroyCallback.fromDispose(callback);
        return this;
    }

    @Override
    public <U extends T> BeanConfigurator<U> read(AnnotatedType<U> type) {
        checkArgumentNotNull(type);
        if (beanManager == null) {
            // TODO message
            throw new IllegalStateException();
        }
        // TODO what happens if a new bean class is set after this method?
        final InjectionTarget<T> injectionTarget = cast(beanManager.getInjectionTargetFactory(type).createInjectionTarget(null));
        addInjectionPoints(injectionTarget.getInjectionPoints());
        createWith(c -> {
            T instance = injectionTarget.produce(c);
            injectionTarget.inject(instance, c);
            injectionTarget.postConstruct(instance);
            return instance;
        });
        destroyWith((i, c) -> {
            injectionTarget.preDestroy(i);
            c.release();
        });
        BeanAttributes<U> beanAttributes = beanManager.createBeanAttributes(type);
        read(beanAttributes);
        return cast(this);
    }

    @Override
    public BeanConfigurator<T> read(BeanAttributes<?> beanAttributes) {
        checkArgumentNotNull(beanAttributes);
        this.attributes.read(beanAttributes);
        return this;
    }

    @Override
    public BeanConfigurator<T> addType(Type type) {
        checkArgumentNotNull(type);
        this.attributes.addType(type);
        return this;
    }

    @Override
    public BeanConfigurator<T> addType(TypeLiteral<?> typeLiteral) {
        checkArgumentNotNull(typeLiteral);
        this.attributes.addType(typeLiteral.getType());
        return this;
    }

    @Override
    public BeanConfigurator<T> addTypes(Type... types) {
        checkArgumentNotNull(types);
        this.attributes.addTypes(types);
        return this;
    }

    @Override
    public BeanConfigurator<T> addTypes(Set<Type> types) {
        checkArgumentNotNull(types);
        this.attributes.addTypes(types);
        return this;
    }

    @Override
    public BeanConfigurator<T> addTransitiveTypeClosure(Type type) {
        checkArgumentNotNull(type);
        this.attributes.addTransitiveTypeClosure(type);
        return this;
    }

    @Override
    public BeanConfigurator<T> types(Type... types) {
        checkArgumentNotNull(types);
        this.attributes.types(types);
        return this;
    }

    @Override
    public BeanConfigurator<T> types(Set<Type> types) {
        checkArgumentNotNull(types);
        this.attributes.types(types);
        return this;
    }

    @Override
    public BeanConfigurator<T> scope(Class<? extends Annotation> scope) {
        checkArgumentNotNull(scope);
        this.attributes.scope(scope);
        return this;
    }

    @Override
    public BeanConfigurator<T> addQualifier(Annotation qualifier) {
        checkArgumentNotNull(qualifier);
        this.attributes.addQualifier(qualifier);
        return this;
    }

    @Override
    public BeanConfigurator<T> addQualifiers(Annotation... qualifiers) {
        checkArgumentNotNull(qualifiers);
        this.attributes.addQualifiers(qualifiers);
        return this;
    }

    @Override
    public BeanConfigurator<T> addQualifiers(Set<Annotation> qualifiers) {
        checkArgumentNotNull(qualifiers);
        this.attributes.addQualifiers(qualifiers);
        return this;
    }

    @Override
    public BeanConfigurator<T> qualifiers(Annotation... qualifiers) {
        checkArgumentNotNull(qualifiers);
        this.attributes.qualifiers(qualifiers);
        return this;
    }

    @Override
    public BeanConfigurator<T> qualifiers(Set<Annotation> qualifiers) {
        checkArgumentNotNull(qualifiers);
        this.attributes.qualifiers(qualifiers);
        return this;
    }

    @Override
    public BeanConfigurator<T> addStereotype(Class<? extends Annotation> stereotype) {
        checkArgumentNotNull(stereotype);
        this.attributes.addStereotype(stereotype);
        return this;
    }

    @Override
    public BeanConfigurator<T> addStereotypes(Set<Class<? extends Annotation>> stereotypes) {
        checkArgumentNotNull(stereotypes);
        this.attributes.addStereotypes(stereotypes);
        return this;
    }

    @Override
    public BeanConfigurator<T> stereotypes(Set<Class<? extends Annotation>> stereotypes) {
        checkArgumentNotNull(stereotypes);
        this.attributes.stereotypes(stereotypes);
        return this;
    }

    @Override
    public BeanConfigurator<T> name(String name) {
        this.attributes.name(name);
        return this;
    }

    @Override
    public BeanConfigurator<T> alternative(boolean alternative) {
        this.attributes.alternative(alternative);
        return this;
    }

    public void initBeanManager(BeanDeploymentFinder beanDeploymentFinder) {
        if (this.beanManager == null && beanDeploymentFinder != null) {
            this.beanManager = beanDeploymentFinder.getOrCreateBeanDeployment(beanClass).getBeanManager();
        }
    }

    public Bean<T> complete() {
        return new ImmutableBean<>(this);
    }

    public BeanManagerImpl getBeanManager() {
        return beanManager;
    }

    static final class CreateCallback<T> {

        private final Supplier<T> simple;

        private final Function<CreationalContext<T>, T> create;

        private final Function<Instance<Object>, T> instance;

        static <T> CreateCallback<T> fromProduceWith(Function<Instance<Object>, T> callback) {
            return new CreateCallback<T>(null, null, callback);
        }

        static <T> CreateCallback<T> fromProduceWith(Supplier<T> callback) {
            return new CreateCallback<T>(callback, null, null);
        }

        static <T> CreateCallback<T> fromCreateWith(Function<CreationalContext<T>, T> callback) {
            return new CreateCallback<T>(null, callback, null);
        }

        public CreateCallback(Supplier<T> simple, Function<CreationalContext<T>, T> create, Function<Instance<Object>, T> instance) {
            this.simple = simple;
            this.create = create;
            this.instance = instance;
        }

        T create(CreationalContext<T> ctx, BeanManagerImpl beanManager) {
            if (simple != null) {
                return simple.get();
            } else if (instance != null) {
                return instance.apply(beanManager.getInstance(ctx));
            } else {
                return create.apply(ctx);
            }
        }

    }

    static final class DestroyCallback<T> {

        private final BiConsumer<T, CreationalContext<T>> destroy;

        private final BiConsumer<T, Instance<Object>> dispose;

        static <T> DestroyCallback<T> fromDispose(BiConsumer<T, Instance<Object>> callback) {
            return new DestroyCallback<>(callback, null);
        }

        static <T> DestroyCallback<T> fromDestroy(BiConsumer<T, CreationalContext<T>> callback) {
            return new DestroyCallback<>(null, callback);
        }

        public DestroyCallback(BiConsumer<T, Instance<Object>> dispose, BiConsumer<T, CreationalContext<T>> destroy) {
            this.destroy = destroy;
            this.dispose = dispose;
        }

        void destroy(T instance, CreationalContext<T> ctx, BeanManagerImpl beanManager) {
            if (dispose != null) {
                dispose.accept(instance, beanManager.getInstance(ctx));
            } else {
                destroy.accept(instance, ctx);
            }
        }

    }

    /**
     *
     * @author Martin Kouba
     *
     * @param <T> the class of the bean instance
     */
    static class ImmutableBean<T> extends ForwardingBeanAttributes<T> implements Bean<T>, PassivationCapable {

        private final String id;

        private final BeanManagerImpl beanManager;

        private final Class<?> beanClass;

        private final BeanAttributes<T> attributes;

        private final Set<InjectionPoint> injectionPoints;

        private final CreateCallback<T> createCallback;

        private final DestroyCallback<T> destroyCallback;

        /**
         *
         * @param configurator
         */
        ImmutableBean(BeanConfiguratorImpl<T> configurator) {
            this.beanManager = configurator.getBeanManager();
            this.beanClass = configurator.beanClass;
            this.attributes = new BeanAttributesConfiguratorImpl<T>(configurator.attributes.complete()).complete();
            this.injectionPoints = ImmutableSet.copyOf(configurator.injectionPoints);
            this.createCallback = configurator.createCallback;
            this.destroyCallback = configurator.destroyCallback;
            if (configurator.id != null) {
                this.id = configurator.id;
            } else {
                this.id = BeanIdentifiers.forBuilderBean(attributes, beanClass);
            }
        }

        @Override
        public T create(CreationalContext<T> creationalContext) {
            return createCallback.create(creationalContext, beanManager);
        }

        @Override
        public void destroy(T instance, CreationalContext<T> creationalContext) {
            if (destroyCallback != null) {
                destroyCallback.destroy(instance, creationalContext, beanManager);
            }
        }

        @Override
        public Class<?> getBeanClass() {
            return beanClass;
        }

        @Override
        public Set<InjectionPoint> getInjectionPoints() {
            return injectionPoints;
        }

        @Override
        public boolean isNullable() {
            return false;
        }

        @Override
        protected BeanAttributes<T> attributes() {
            return attributes;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return "Immutable Builder Bean [" + getBeanClass().toString() + ", types: " + Formats.formatTypes(getTypes()) + ", qualifiers: "
                    + Formats.formatAnnotations(getQualifiers()) + "]";
        }

    }

}
