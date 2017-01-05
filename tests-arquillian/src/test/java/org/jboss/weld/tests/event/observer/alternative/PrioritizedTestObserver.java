package org.jboss.weld.tests.event.observer.alternative;

import org.jboss.weld.tests.event.observer.superclass.TestEvent;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Alternative;

@ApplicationScoped
@Alternative
@Priority(2)
public class PrioritizedTestObserver extends AbstractTestObserver{

        // observation disabled because this overrides the observer method without
        // the @Observes
    @Override
    void observe(@Observes TestEvent event) {
        super.observe(event);
    }
}
