/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.weld.tests.event.observer.alternative;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.BeanArchive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.weld.test.util.Utils;
import org.jboss.weld.tests.event.observer.superclass.TestEvent;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.enterprise.event.Event;
import javax.inject.Inject;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(Arquillian.class)
public class AlternativeObservers1Test {
    @Deployment
    public static Archive<?> deploy() {
        return ShrinkWrap.create(BeanArchive.class, Utils.getDeploymentNameAsHash(AlternativeObservers1Test.class)).addPackage(AlternativeObservers1Test.class.getPackage());
    }

    @Inject
    Event<TestEvent> event;

    @Inject
    private DefaultTestObserver defaultTestObserver;

    @Inject
    private PrioritizedTestObserver prioritizedTestObserver;

    @Test
    public void testObserverMethodOnAlternativesWithPriority() {
        defaultTestObserver.reset();
        prioritizedTestObserver.reset();

        assertNull(defaultTestObserver.getTestEvent());
        assertNull(prioritizedTestObserver.getTestEvent());
        event.fire(new TestEvent());
        assertNull(defaultTestObserver.getTestEvent());
        assertNotNull(prioritizedTestObserver.getTestEvent());
    }

}
