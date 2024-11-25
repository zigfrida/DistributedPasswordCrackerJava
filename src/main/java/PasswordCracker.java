import org.apache.commons.codec.digest.Crypt;

public class PasswordCracker {

    private static final String CHARSET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int MAX_PASSWORD_LENGTH = 3;
    private static final int THREAD_COUNT = 4;
    private String prefixRange;
    private String salt;
    private String saltPasswordHash;
    private String password;
    private volatile boolean passwordFound = false;

    public PasswordCracker(String prefixRange, String salt, String saltPasswordHash) {
        this.prefixRange = prefixRange;
        this.salt = salt;
        this.saltPasswordHash = saltPasswordHash;
    }

    public String getPassword() {
        return password;
    }

    public boolean bruteForceThreads() {
        System.out.println("Starting to crack password...");
        long start = System.nanoTime();

        // Instead of dividing CHARSET, we'll divide the prefixRange for first character
        int prefixRangeLength = prefixRange.length();
        int partitionSize = Math.max(1, prefixRangeLength / THREAD_COUNT);
        Thread[] threads = new Thread[THREAD_COUNT];

        for (int i = 0; i < THREAD_COUNT; i++) {
            int startIdx = i * partitionSize;
            int endIdx;

            if (i == THREAD_COUNT - 1) {
                endIdx = prefixRangeLength; // Last thread takes remaining characters
            } else {
                endIdx = Math.min(startIdx + partitionSize, prefixRangeLength);
            }

            // Each thread gets a subset of prefixRange characters to start with
            String threadPrefixRange = prefixRange.substring(startIdx, endIdx);

            int finalI = i;
            threads[i] = new Thread(() -> {
                Thread.currentThread().setName("Thread-" + (finalI + 1));
                // Start with empty prefix and length 0
                generateCombinations("", 0, threadPrefixRange);
            });

            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        long elapsedTimeSeconds = (System.nanoTime() - start) / 1000000000;
        long minutes = elapsedTimeSeconds / 60;
        long reminderSeconds = elapsedTimeSeconds % 60;

        System.out.println("Elapsed time: " + minutes + " minutes and " + reminderSeconds + " seconds.");
        return passwordFound;
    }

    public void generateCombinations(String prefix, int length, String threadPrefixRange) {
        if (passwordFound || length == MAX_PASSWORD_LENGTH + 1) {
            return;
        }

        if (length > 0 && checkGuessAndPassword(prefix, this.saltPasswordHash)) {
            passwordFound = true;
            System.out.println(Thread.currentThread().getName() + " found the password: " + prefix);
            password = prefix;

            // Interrupt all other threads
            Thread[] activeThreads = Thread.getAllStackTraces().keySet().toArray(new Thread[0]);
            for (Thread thread : activeThreads) {
                if (!thread.equals(Thread.currentThread()) && thread.getName().startsWith("Thread-")) {
                    thread.interrupt();
                }
            }
            return;
        }

        // If we're at length 0, use the threadPrefixRange for first character
        if (length == 0) {
            for (int i = 0; i < threadPrefixRange.length(); i++) {
                if (passwordFound || Thread.currentThread().isInterrupted()) break;
                generateCombinations(prefix + threadPrefixRange.charAt(i), 1, threadPrefixRange);
            }
        }
        // For subsequent positions, use the full CHARSET
        else {
            for (int i = 0; i < CHARSET.length(); i++) {
                if (passwordFound || Thread.currentThread().isInterrupted()) break;
                generateCombinations(prefix + CHARSET.charAt(i), length + 1, threadPrefixRange);
            }
        }
    }

    public boolean checkGuessAndPassword(String guess, String hashedPassword) {
        String hashedGuess = Crypt.crypt(guess, this.salt);

        if (hashedGuess.equals(hashedPassword)) {
            System.out.println("Password found: " + guess);
            return true;
        }
        return false;
    }
}
