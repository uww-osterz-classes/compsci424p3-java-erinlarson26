/* COMPSCI 424 Program 3
 * Name:
 * 
 * This is a template. Program3.java *must* contain the main class
 * for this program. 
 * 
 * You will need to add other classes to complete the program, but
 * there's more than one way to do this. Create a class structure
 * that works for you. Add any classes, methods, and data structures
 * that you need to solve the problem and display your solution in the
 * correct format.
 */

package compsci424.p3;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Semaphore;


/**
 * Main class for this program. To help you get started, the major
 * steps for the main program are shown as comments in the main
 * method. Feel free to add more comments to help you understand
 * your code, or for any reason. Also feel free to edit this
 * comment to be more helpful.
 */
public class Program3 {
	private int numResources;
    private int numProcesses;
    private int[][] max;
    private int[][] allocation;
    private int[] available;
    private Semaphore mutex;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Not enough command-line arguments provided, exiting.");
            return;
        }

        String mode = args[0];
        String setupFilePath = args[1];

        Program3 bankersAlgorithm = new Program3();
        bankersAlgorithm.run(mode, setupFilePath);
    }

    public void run(String mode, String setupFilePath) {
        try {
            readSetupFile(setupFilePath);
        } catch (IOException e) {
            System.err.println("Error while reading setup file: " + e.getMessage());
            return;
        }

        if (!checkInitialConditions()) {
            System.err.println("System does not start in a safe state.");
            return;
        }

        mutex = new Semaphore(1);

        if (mode.equals("manual")) {
            manualMode();
        } else if (mode.equals("auto")) {
            autoMode();
        } else {
            System.err.println("Invalid mode: " + mode + ". Exiting.");
        }
    }

    private void readSetupFile(String setupFilePath) throws IOException {
        BufferedReader setupFileReader = new BufferedReader(new FileReader(setupFilePath));

        // Read number of resources and processes
        numResources = Integer.parseInt(setupFileReader.readLine().split(" ")[0]);
        numProcesses = Integer.parseInt(setupFileReader.readLine().split(" ")[0]);

        // Initialize arrays
        available = new int[numResources];
        max = new int[numProcesses][numResources];
        allocation = new int[numProcesses][numResources];

        // Read and process "Available" section
        setupFileReader.readLine(); // Skip "Available" line
        String[] availableValues = setupFileReader.readLine().split(" ");
        for (int i = 0; i < numResources; i++) {
            available[i] = Integer.parseInt(availableValues[i]);
        }

        // Read and process "Max" section
        setupFileReader.readLine(); // Skip "Max" line
        for (int i = 0; i < numProcesses; i++) {
            String[] maxValues = setupFileReader.readLine().split(" ");
            for (int j = 0; j < numResources; j++) {
                max[i][j] = Integer.parseInt(maxValues[j]);
            }
        }

        // Read and process "Allocation" section
        setupFileReader.readLine(); // Skip "Allocation" line
        for (int i = 0; i < numProcesses; i++) {
            String[] allocationValues = setupFileReader.readLine().split(" ");
            for (int j = 0; j < numResources; j++) {
                allocation[i][j] = Integer.parseInt(allocationValues[j]);
            }
        }

        setupFileReader.close();
    }

    private boolean checkInitialConditions() {
        // Check if allocation is less than or equal to max
        for (int i = 0; i < numProcesses; i++) {
            for (int j = 0; j < numResources; j++) {
                if (allocation[i][j] > max[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    private void manualMode() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("Enter command(request, release, end): ");
                String command = scanner.nextLine();
                if (command.equals("end")) {
                    break;
                } else if (command.startsWith("request")) {
                    handleManualRequest(command);
                } else if (command.startsWith("release")) {
                    handleManualRelease(command);
                } else {
                    System.out.println("Invalid command.");
                }
            }
        }
    }

    private void handleManualRequest(String command) {
        String[] parts = command.split(" ");
        int processId = Integer.parseInt(parts[1]);
        int resourceType = Integer.parseInt(parts[3]);
        int requestAmount = Integer.parseInt(parts[5]);
        requestResource(processId, resourceType, requestAmount);
    }

    private void handleManualRelease(String command) {
        String[] parts = command.split(" ");
        int processId = Integer.parseInt(parts[1]);
        int resourceType = Integer.parseInt(parts[3]);
        int releaseAmount = Integer.parseInt(parts[5]);
        releaseResource(processId, resourceType, releaseAmount);
    }
    
    private void requestResource(int processId, int resourceType, int requestAmount) {
        try {
            mutex.acquire();
            if (requestAmount <= 0 || requestAmount > available[resourceType]) {
                System.out.println("Process " + processId + " requests " + requestAmount + " units of resource " + resourceType + ": denied");
            } else if (isSafeToAllocate(processId, resourceType, requestAmount)) {
                allocation[processId][resourceType] += requestAmount;
                available[resourceType] -= requestAmount;
                System.out.println("Process " + processId + " requests " + requestAmount + " units of resource " + resourceType + ": granted");
            } else {
                System.out.println("Process " + processId + " requests " + requestAmount + " units of resource " + resourceType + ": denied");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mutex.release();
        }
    }
    
    private void releaseResource(int processId, int resourceType, int releaseAmount) {
        if (releaseAmount < 0 || releaseAmount > allocation[processId][resourceType]) {
            System.out.println("Error: Invalid release request.");
            return;
        }

        allocation[processId][resourceType] -= releaseAmount;
        available[resourceType] += releaseAmount;
        System.out.println("Process " + processId + " releases " + releaseAmount + " units of resource " + resourceType);
    }

    private boolean isSafeToAllocate(int processId, int resourceType, int requestAmount) {
        int[] work = Arrays.copyOf(available, available.length);
        int[][] need = new int[numProcesses][numResources];
        int[][] tempAllocation = new int[numProcesses][numResources];

        for (int i = 0; i < numProcesses; i++) {
            for (int j = 0; j < numResources; j++) {
                need[i][j] = max[i][j] - allocation[i][j];
                tempAllocation[i][j] = allocation[i][j];
            }
        }

        work[resourceType] -= requestAmount;
        tempAllocation[processId][resourceType] += requestAmount;

        boolean[] finish = new boolean[numProcesses];
        Arrays.fill(finish, false);

        int count = 0;
        while (count < numProcesses) {
            boolean found = false;
            for (int i = 0; i < numProcesses; i++) {
                if (!finish[i]) {
                    boolean canAllocate = true;
                    for (int j = 0; j < numResources; j++) {
                        if (need[i][j] > work[j]) {
                            canAllocate = false;
                            break;
                        }
                    }
                    if (canAllocate) {
                        for (int j = 0; j < numResources; j++) {
                            work[j] += tempAllocation[i][j];
                        }
                        finish[i] = true;
                        count++;
                        found = true;
                    }
                }
            }
            if (!found) {
                break;
            }
        }

        return count == numProcesses && finish[processId]; //adding && finish[processId]
    }

    private void autoMode() {
        Thread[] threads = new Thread[numProcesses];
        for (int i = 0; i < numProcesses; i++) {
            threads[i] = new Thread(new Process(i));
            threads[i].start();
        }
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    class Process implements Runnable {
        private final int id;

        public Process(int id) {
            this.id = id;
        }

        public void run() {
            Random random = new Random();
            for (int i = 0; i < 3; i++) {
                int resourceType = random.nextInt(numResources);
                int requestAmount = random.nextInt(max[id][resourceType] - allocation[id][resourceType] + 1);
                requestResource(id, resourceType, requestAmount);
                releaseResource(id, resourceType, random.nextInt(allocation[id][resourceType] + 1));
            }
        }
    }
}