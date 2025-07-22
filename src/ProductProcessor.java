import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductProcessor implements Runnable {
    private final char threadId;
    private final List<Product> queue = new ArrayList<>();
    private final Map<String, Integer> localTotals = new HashMap<>();

    ProductProcessor(char threadId) {
        this.threadId = threadId;
    }

    public void submitTask(Product task) {
        synchronized (queue) {
            queue.add(task);
            queue.notify();
        }
    }

    @Override
    public void run() {
        while (true) {
            Product task = waitForTask();
            if (task != null) {
                processTask(task);
            }
        }
    }

    private Product waitForTask() {
        synchronized (queue) {
            while (queue.isEmpty()) {
                try {
                    queue.wait();
                } catch (InterruptedException e) {
                    return null;
                }
            }
            return queue.remove(0);
        }
    }

    private void processTask(Product task) {
        synchronized (localTotals) {
            localTotals.merge(task.name, task.amount, Integer::sum);
        }

        int globalTotal;
        synchronized (DirectoryMonitor.globalLock) {
            DirectoryMonitor.globalTotals.merge(task.name, task.amount, Integer::sum);
            globalTotal = DirectoryMonitor.globalTotals.get(task.name);
        }

        printStatus(task.name, globalTotal);
    }

    private void printStatus(String product, int globalTotal) {
        System.out.println("\n[Thread " + threadId + "]");
        System.out.println("Global amount of " + product + ": " + globalTotal);
        synchronized (localTotals) {
            System.out.println("All products processed by thread " + threadId + ": " + localTotals);
        }
    }
}