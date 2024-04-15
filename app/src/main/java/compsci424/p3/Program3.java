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
import java.io.FileNotFoundException;
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
	private static int numResources;
	private static int numProcesses;
	private static int[][] max;
	private static int[][] allocation;
	private static int[] available;
	private static Semaphore mutex;

	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("Not enough command-line arguments provided, exiting.");
			return;
		}

		String mode = args[0];
		String setupFilePath = args[1];

		System.out.println("Selected mode: " + args[0]);
		System.out.println("Setup file location: " + args[1]);

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

		if (mode.equals("manual")) {
			manualMode();
		} else if (mode.equals("auto")) {
			autoMode();
		} else {
			System.err.println("Invalid mode: " + mode + ". Exiting.");
		}
	}

	private static void readSetupFile(String setupFilePath) throws IOException {
		BufferedReader setupFileReader = new BufferedReader(new FileReader(setupFilePath));

		String currentLine;

		currentLine = setupFileReader.readLine();
		numResources = Integer.parseInt(currentLine.split(" ")[0]);

		currentLine = setupFileReader.readLine();
		numProcesses = Integer.parseInt(currentLine.split(" ")[0]);

		available = new int[numResources];
		max = new int[numProcesses][numResources];
		allocation = new int[numProcesses][numResources];
		mutex = new Semaphore(1);

		// Read and process the "Available" section
		setupFileReader.readLine(); // Skip the "Available" line
		currentLine = setupFileReader.readLine();
		String[] availableValues = currentLine.split(" ");
		for (int i = 0; i < numResources; i++) {
			available[i] = Integer.parseInt(availableValues[i]);
		}

		// Read and process the "Max" section
		setupFileReader.readLine(); // Skip the "Max" line
		for (int i = 0; i < numProcesses; i++) {
			currentLine = setupFileReader.readLine();
			String[] maxValues = currentLine.split(" ");
			for (int j = 0; j < numResources; j++) {
				max[i][j] = Integer.parseInt(maxValues[j]);
			}
		}

		// Read and process the "Allocation" section
		setupFileReader.readLine(); // Skip the "Allocation" line
		for (int i = 0; i < numProcesses; i++) {
			currentLine = setupFileReader.readLine();
			String[] allocationValues = currentLine.split(" ");
			for (int j = 0; j < numResources; j++) {
				allocation[i][j] = Integer.parseInt(allocationValues[j]);
			}
		}

		setupFileReader.close();
	}

	private static boolean checkInitialConditions() {
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

	private static void manualMode() {
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

	private static void handleManualRequest(String command) {
		String[] parts = command.split(" ");
		int processId = Integer.parseInt(parts[1]);
		int[] request = new int[numResources];
		for (int i = 0; i < numResources; i++) {
			request[i] = Integer.parseInt(parts[i + 4]);
		}
		if (requestGranted(processId, request)) {
			for (int i = 0; i < numResources; i++) {
				allocation[processId][i] += request[i];
				available[i] -= request[i];
			}
			System.out.println("Request granted");
		} else {
			System.out.println("Request denied");
		}
	}

	private static boolean requestGranted(int processId, int[] request) {
		int[] tempAvailable = Arrays.copyOf(available, available.length);
        int[][] tempAllocation = new int[numProcesses][numResources];

        // Simulate the allocation
        for (int i = 0; i < numProcesses; i++) {
            for (int j = 0; j < numResources; j++) {
                tempAllocation[i][j] = allocation[i][j];
            }
        }

        for (int i = 0; i < numResources; i++) {
            tempAvailable[i] -= request[i];
            tempAllocation[processId][i] += request[i];
        }

        boolean[] finished = new boolean[numProcesses];
        Arrays.fill(finished, false);

        // Simulate resource allocation until all processes finish or deadlock is detected
        boolean deadlock = false;
        int count = 0;
        while (count < numProcesses && !deadlock) {
            boolean allocated = false;
            for (int i = 0; i < numProcesses; i++) {
                if (!finished[i]) {
                    boolean canAllocate = true;
                    for (int j = 0; j < numResources; j++) {
                        if (tempAvailable[j] < max[i][j] - tempAllocation[i][j]) {
                            canAllocate = false;
                            break;
                        }
                    }
                    if (canAllocate) {
                        allocated = true;
                        finished[i] = true;
                        count++;
                        for (int j = 0; j < numResources; j++) {
                            tempAvailable[j] += tempAllocation[i][j];
                        }
                    }
                }
            }
            if (!allocated) {
                deadlock = true;
            }
        }

        return !deadlock;
    }

	private static void handleManualRelease(String command) {
		String[] parts = command.split(" ");
		int processId = Integer.parseInt(parts[1]);
		int[] release = new int[numResources];
		for (int i = 0; i < numResources; i++) {
			release[i] = Integer.parseInt(parts[i + 3]);
		}
		for (int i = 0; i < numResources; i++) {
			if (release[i] < 0 || release[i] > allocation[processId][i]) {
				System.out.println("Error: Invalid release request.");
				return;
			}
		}
		for (int i = 0; i < numResources; i++) {
			allocation[processId][i] -= release[i];
			available[i] += release[i];
		}
		System.out.println("Resources released.");
	}

	private static void autoMode() {
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

	static class Process implements Runnable {
		private final int id;
		private final Random random;

		public Process(int id) {
			this.id = id;
			this.random = new Random();
		}

		@Override
		public void run() {
			for (int i = 0; i < 3; i++) { // Generating 3 requests and 3 releases
				int[] request = generateRequest();
				int[] release = generateRelease();

				try {
					mutex.acquire();
					if (requestGranted(id, request)) {
						for (int j = 0; j < numResources; j++) {
							allocation[id][j] += request[j];
							available[j] -= request[j];
						}
						System.out.println("Process " + id + " requests " + Arrays.toString(request) + ": granted");
					} else {
						System.out.println("Process " + id + " requests " + Arrays.toString(request) + ": denied");
					}
					for (int j = 0; j < numResources; j++) {
						allocation[id][j] -= release[j];
						available[j] += release[j];
					}
					System.out.println("Process " + id + " releases " + Arrays.toString(release));
					mutex.release();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		private int[] generateRequest() {
			int[] request = new int[numResources];
			for (int i = 0; i < numResources; i++) {
				request[i] = random.nextInt(max[id][i] - allocation[id][i] + 1);
			}
			return request;
		}

		private int[] generateRelease() {
			int[] release = new int[numResources];
			for (int i = 0; i < numResources; i++) {
				release[i] = random.nextInt(allocation[id][i] + 1);
			}
			return release;
		}
	}
}