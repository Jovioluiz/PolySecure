package com.polysecure.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

// Best-effort transaction across heterogeneous stores (no distributed 2PC).
// Sequential run(): if an operation fails, completed ones are rolled back in reverse order.
// Parallel runParallel(): all operations start concurrently; any failure triggers rollback of all completed.
@Component
public class TransactionCoordinator {

    private static final Logger log = LoggerFactory.getLogger(TransactionCoordinator.class);

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

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

    /**
     * Executes all operations in parallel using virtual threads (Phase 4 — parallel DML).
     * If any operation fails, completed ones are rolled back. Falls back to sequential for a single op.
     */
    public void runParallel(List<Operation> operations) {
        if (operations.size() <= 1) {
            run(operations);
            return;
        }
        List<Future<Operation>> futures = operations.stream()
            .map(op -> executor.submit(() -> { op.execute().run(); return op; }))
            .toList();

        List<Operation> completed = new ArrayList<>();
        List<Throwable> errors = new ArrayList<>();

        for (Future<Operation> f : futures) {
            try {
                completed.add(f.get());
            } catch (ExecutionException ex) {
                errors.add(ex.getCause());
                log.warn("Parallel operation failed: {}", ex.getCause().getMessage());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                errors.add(ex);
            }
        }

        if (!errors.isEmpty()) {
            rollback(completed);
            throw new RuntimeException(
                "Parallel DML failed (" + errors.size() + " error(s)). "
                + "Rollback attempted for " + completed.size() + " completed operation(s). "
                + "First cause: " + errors.get(0).getMessage(), errors.get(0));
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
