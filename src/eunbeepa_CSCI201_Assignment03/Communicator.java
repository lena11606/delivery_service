package eunbeepa_CSCI201_Assignment03;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.Vector;

public class Communicator extends Thread {

	@SuppressWarnings("unused")
	private static final long serialVersionUID = 1L;

	private Vector<Server> serverThreads;
	private ServerSocket ss;
	private int availDrivers;
	private List<Order> ordersReadyToSend = null;

	private List<Order> orders = new ArrayList<>();
	@SuppressWarnings("unused")
	private List<Restaurant> restaurants;
	@SuppressWarnings("unused")
	private double latitude, longitude;
	private Server readyThread = null;
	private Queue<Server> serverQueue = new LinkedList<>();

	public Communicator(int port, int numDrivers, double latitude, double longitude, List<Order> orders) {
		serverThreads = new Vector<Server>();
		this.latitude = latitude;
		this.longitude = longitude;
		this.orders = orders;
		this.availDrivers = numDrivers;

		try {
			ss = new ServerSocket(port);
			System.out.println("\nListening on port " + port + ".\nWaiting for drivers...");
			Thread acceptClientsThread = new Thread(() -> {
				while (true) {
					Socket s;
					try {
						s = ss.accept(); // client socket
						Server st = new Server(s, this); // server thread
						serverThreads.add(st);
						System.out.println("\nConnection from " + s.getInetAddress().getHostAddress());
						this.availDrivers -= 1;
						String line = "";
						if (this.availDrivers == 0) {
							line = "Starting Service.";
							System.out.println(line);
							broadcast(line);
							broadcast(Double.toString(latitude)); // lat
							broadcast(Double.toString(longitude)); // long
							assignOrders();
						} else {
							line = "Waiting for " + this.availDrivers + " more driver(s)...";
							System.out.println(line);
							broadcast(line);
						}

					} catch (IOException ioe) {
						ioe.printStackTrace();
					}
				}
			});
			acceptClientsThread.start();
		} catch (IOException ioe) {
			System.out.println("Communicator constructor: " + ioe.getMessage());
		}
	}

	public void broadcast(String message) { // all clients prints
		if (message != null) {
			for (Server thread : serverThreads) {
				thread.sendMessage(message);
			}
		}
	}

	public void assignOrders() {
		Map<Integer, List<Order>> ordersByTime = new HashMap<>();

		List<Order> uniqueOrders = new ArrayList<>();
		Set<String> uniqueRestaurantNames = new HashSet<>();

		for (Order order : orders) {
			String restaurantName = order.getName();
			if (!uniqueRestaurantNames.contains(restaurantName)) {
				uniqueOrders.add(order);
				uniqueRestaurantNames.add(restaurantName);
			}
		}

		for (Server thread : serverThreads) {
			while (!thread.isReady()) {
				try {
					Thread.sleep(2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			thread.sendOrders(uniqueOrders);
		}

		for (Order order : orders) {
			int orderTime = order.getTime();
			ordersByTime.computeIfAbsent(orderTime, k -> new ArrayList<>()).add(order);
		}

		long startTime = -1;
		for (List<Order> ordersAtSameTime : ordersByTime.values()) {

			long orderTime = ordersAtSameTime.get(0).getTime() * 1000L;
			while (serverQueue.isEmpty()) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			if (!serverQueue.isEmpty()) {
				ordersReadyToSend = ordersAtSameTime;
				readyThread = serverQueue.poll();
				if (orderTime > 0) {
					long elapsedTime = System.currentTimeMillis() - startTime;
					long waitTime = orderTime - elapsedTime;
					if (waitTime > 0) {
						try {
							Thread.sleep(waitTime);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				sendToServer(readyThread);
				// true starting time
				if (startTime == -1 || orderTime == 0) {
					startTime = System.currentTimeMillis();
				}
			}
		}
		while (!finished()) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.out.println("\n\nAll orders completed!");
		String timeString = "";
		for (Server thread : serverThreads) {
			if (thread.isReady()) {
				int comparison = timeString.compareTo(thread.getFinishedTime());
				if (comparison < 0) {
					timeString = thread.getFinishedTime();
				}
			}
		}
		for (Server thread : serverThreads) {
			thread.sendOrders(timeString);
		}
		try {
			ss.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	public void serverIsReady(Server server) {
		serverQueue.add(server);
	}

	public void sendToServer(Server server) {
		if (ordersReadyToSend != null && server != null) {
			server.sendOrders(ordersReadyToSend);
			server.setReady(false);
			readyThread = null;
			ordersReadyToSend = null;
		}
	}

	private boolean finished() {
		for (Server thread : serverThreads) {
			if (!thread.isReady()) {
				return false;
			}
		}
		return true;
	}

	public void setRestaurants(List<Restaurant> restaurants) {
		this.restaurants = restaurants;
	}

	public static void main(String[] args) throws IOException, InterruptedException {

		double latitude; // 34.021160
		double longitude; // -118.287132

		Scanner reader = new Scanner(System.in);
		Scanner sc = new Scanner(System.in);
		List<Order> orders = new ArrayList<>();
		String csvFile;

		// reading in schedule.csv
		boolean isValid = false;
		List<Restaurant> restaurants = new ArrayList<>();
		Set<String> restaurantSet = new HashSet<>();

		while (!isValid) {
			System.out.println("What is the name of file containing the schedule information?");
			csvFile = sc.nextLine();

			// source: JavaPoint, https://www.jsonschema2pojo.org/, (5 lines), parsing by
			// split()
			try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
				isValid = true;
				String line;
				while ((line = br.readLine()) != null) {
					String[] param = line.split(",");
					if (param.length == 3) {
						int time = Integer.parseInt(param[0].trim());
						String name = param[1].trim();
						String orderItem = param[2].trim();
						Order order = new Order(time, name, orderItem);
						if (!restaurantSet.contains(name)) {
							Restaurant restaurant = new Restaurant(name);
							restaurants.add(restaurant);
							restaurantSet.add(name);
						}
						orders.add(order);
					}
				}
			} catch (FileNotFoundException fnfe) {
				System.out.print("The file " + csvFile + " could not be found. \n \n");
				csvFile = null;
			} catch (NullPointerException npe) {
				System.out.print("The file " + csvFile + " is missing data. \n \n");
			} catch (IOException ioe) {
				System.out.print("IO Exception in " + csvFile + ". \n \n");
			}
		}
		while (true) {
			System.out.println("What is your latitude?");
			if (reader.hasNextDouble()) {
				latitude = reader.nextDouble();
				break;
			} else {
				System.out.print("That is not a valid latitude. \n \n");
				reader.next();
			}
		}
		while (true) {
			System.out.println("\nWhat is your longitude?");
			if (reader.hasNextDouble()) {
				longitude = reader.nextDouble();
				break;
			} else {
				System.out.print("That is not a valid longitude. \n \n");
				reader.next();
			}
		}
		int numDrivers;
		while (true) {
			System.out.println("\nHow many drivers will be in service today?");
			if (reader.hasNextInt()) {
				numDrivers = reader.nextInt();
				break;
			} else {
				System.out.print("That is not a valid number of drivers. \n \n");
				reader.next();
			}
		}
		int port = 3456;
		Communicator comm = new Communicator(port, numDrivers, latitude, longitude, orders);
		comm.setRestaurants(restaurants);
		sc.close();
		reader.close();
	}
}