package com.polysecure.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

// Best-effort transaction across heterogeneous stores (no distributed 2PC).
// On failure, attempts to rollback already-completed operations in reverse order.
@Component
public class TransactionCoordinator {

    private static final Logger log = LoggerFactory.getLogger(TransactionCoordinator.class);

    public record Operation(String description, Runnable execute, Runnable rollback) {}

    public void run(List<Operation> operations) {
        List<Operation> completed = new ArrayList<>();
        for (Operation op : operations) {
            try {
                op.execute().run();
                completed.add(op);
            } catch (Exception ex) {
                log.warn("Operation failed [{}]: {}", op.description(), ex.getMessage());
                rollback(completed);
                throw new RuntimeException(
                    "Failed on [" + op.description() + "]. Rollback attempted for "
                    + completed.size() + " prior operation(s). Cause: " + ex.getMessage(), ex);
            }
        }
    }

    private void rollback(List<Operation> completed) {
        for (int i = completed.size() - 1; i >= 0; i--) {
            Operation op = completed.get(i);
            try {
                op.rollback().run();
                log.info("Rolled back [{}]", op.description());
            } catch (Exception re) {
                log.error("Rollback failed [{}]: {}", op.description(), re.getMessage());
            }
        }
    }
}
