import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class DirectoryMonitor {
    private static final Path PRODUCTS_PATH = Paths.get("products");
    static final Map<String, Integer> globalTotals = new HashMap<>();
    static final Object globalLock = new Object();
    private static final Map<Character, ProductProcessor> processorMap = new HashMap<>();

    public static void main(String[] args) throws IOException {
        createProductsDirectory();
        initializeProcessors();
        startScanningDirectory();
    }

    private static void createProductsDirectory() throws IOException {
        if (!Files.exists(PRODUCTS_PATH)) {
            Files.createDirectory(PRODUCTS_PATH);
        }
        System.out.println("Please start copying the files to this folder: " + PRODUCTS_PATH.toAbsolutePath());
    }

    private static void initializeProcessors() {
        for (char ch = 'A'; ch <= 'Z'; ch++) {
            ProductProcessor processor = new ProductProcessor(ch);
            processorMap.put(ch, processor);
            new Thread(processor, "Processor-" + ch).start();
        }
    }

    private static void startScanningDirectory() {
        Thread pollerThread = new Thread(DirectoryMonitor::scanDirectory);
        pollerThread.setDaemon(true);
        pollerThread.start();
    }

    private static void processFile(Path filePath) {
        try {
            String line = Files.readString(filePath).trim();
            String[] parts = line.split(":");
            if (parts.length != 2) {
                System.out.println("Incorrect file content for the file: " + filePath.getFileName());
                return;
            }

            String productName = parts[0].trim();
            int productAmount = Integer.parseInt(parts[1].trim());

            char initial = Character.toUpperCase(filePath.getFileName().toString().charAt(0));
            ProductProcessor processor = processorMap.get(initial);
            if (processor == null) {
                System.out.println("File name should start with A-Z for the file: " + filePath.getFileName());
                return;
            }
            processor.submitTask(new Product(productName, productAmount));
            Files.delete(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private static void scanDirectory() {
        while (true) {
            try {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(PRODUCTS_PATH, "*.txt")) {
                    for (Path file : stream) {
                        processFile(file);
                    }
                }
                Thread.sleep(1000);
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        }
    }
}
