package org.jboss.weld.tests.event.observer.alternative;

import org.jboss.weld.tests.event.observer.superclass.TestEvent;

public abstract class AbstractTestObserver {
    private TestEvent testEvent;

    public TestEvent getTestEvent() {
        return testEvent;
    }

    void observe(TestEvent event) {
        this.testEvent = event;
    }

    public void reset() {
        testEvent = null;
    }
}
