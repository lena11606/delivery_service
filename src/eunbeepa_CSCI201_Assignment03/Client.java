package eunbeepa_CSCI201_Assignment03;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Client extends Thread implements Serializable {
	private static final long serialVersionUID = 1L;

	private BufferedReader br;
	private PrintWriter pw;
	private ObjectInputStream ois;
	private Socket s;

	private boolean isReady = false;
	boolean first = true;
	private long startTime;
	private long msOffsetTime;
	double lati = 0.00, longi = 0.00, oglat = 0.00, oglong = 0.00;
	private List<Restaurant> restaurants = new ArrayList<>();
	private Set<String> restaurantSet = new HashSet<>();
	String time = "", message = "", item = "", resName = "";
	private int starting = -1;

	public Client(String hostname, int port) {
		try {
			s = new Socket(hostname, port);
			pw = new PrintWriter(s.getOutputStream());
			br = new BufferedReader(new InputStreamReader(s.getInputStream()));
			ois = new ObjectInputStream(s.getInputStream());
			this.start();
		} catch (IOException ioe) {
			System.out.println("Client constructor: " + ioe.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	public void run() {
		try {
			while (true) {
				if (!isReady) {
					String line = br.readLine();
					if (line != null) {
						if (line.equals("Starting Service.")) {
							if (lati == 0.0 && longi == 0.0) {
								lati = Double.parseDouble(br.readLine());
								longi = Double.parseDouble(br.readLine());
								oglat = lati;
								oglong = longi;
							}
							System.out.println("\nAll drivers have arrived!\nStarting service.\n\n");
							sendMessage("Ready");
							System.out.flush();
							isReady = true;
						} else if (line.startsWith("Waiting for")) {
							int numDrivers = 0;
							String[] parts = line.split("\\s");
							for (int i = 0; i < parts.length; i++) {
								// Prompt: How do I extract the # 1 from string "Waiting for 1 more
								// driver(s)..."? (2 lines). ChatGPT, OpenAI, 10.21/2023, chat.openai.com/chat
								if (parts[i].matches("\\d+")) {
									numDrivers = Integer.parseInt(parts[i]);
								}
							}
							System.out.println(
									numDrivers + " more driver is needed before the service can begin.\nWaiting... \n");
						}
					}
				}
				if (isReady) {
					try {
						Object obj = ois.readObject();
						if (obj instanceof List<?>) {
							List<?> receivedList = (List<?>) obj;
							if (!receivedList.isEmpty() && receivedList.get(0) instanceof Order) {
								List<Order> orders = (List<Order>) receivedList;
								try {
									if (restaurants.isEmpty()) { // find all restaurant info
										fillRestaurantInfo(orders);
									} else {
										completeDeliveries(orders);
									}
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}

						} else {
							System.out.println("\n" + obj + " All orders completed!");
							System.exit(0);
						}
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}

	private void fillRestaurantInfo(List<Order> receivedOrders) throws IOException, InterruptedException {
		String apiKey = "zRtkAulsyXLNnI45LtideAyeUht3ibprQZ17cgYDN7Ek-AZSkzEP2h6AsA9mv59ETjcJB9HvSIIUtw2qHWh_UooeCUKnbBl6z4Mjeh1Aq7MtpZQiaJjc8nsEdO4-ZXYx";

		for (Order order : receivedOrders) {
			resName = order.getName();

			if (!restaurantSet.contains(resName)) {
				Restaurant restaurant = new Restaurant(resName);
				restaurants.add(restaurant);

				String currRes = "";
				String term;
				currRes = restaurant.getName();
				term = currRes.replaceAll(" ", "%20");

				// https://docs.developer.yelp.com/reference/v3_business_search
				HttpRequest request = HttpRequest.newBuilder()
						.uri(URI.create("https://api.yelp.com/v3/businesses/search?latitude=" + lati + "&longitude="
								+ longi + "&term=" + term + "&categories=&attributes=restaurants_delivery"
								+ "&sort_by=best_match&limit=1"))
						.header("accept", "application/json").header("Authorization", "Bearer " + apiKey)
						.method("GET", HttpRequest.BodyPublishers.noBody()).build();

				HttpResponse<String> response = HttpClient.newHttpClient().send(request,
						HttpResponse.BodyHandlers.ofString());
				if (response.statusCode() == 200) {
					String body = response.body();

					JsonObject json = JsonParser.parseString(body).getAsJsonObject();
					JsonArray businesses = json.getAsJsonArray("businesses");

					if (businesses.size() > 0) {
						JsonObject business = businesses.get(0).getAsJsonObject();
						JsonObject coordinates = business.getAsJsonObject("coordinates");

						if (restaurant.getLatitude() == 0) {
							restaurant.setLatitude(coordinates.get("latitude").getAsDouble());
							restaurant.setLongitude(coordinates.get("longitude").getAsDouble());
						}
					}

				}
				restaurantSet.add(resName);
			}
		}
		long trueStart = System.currentTimeMillis();
		msOffsetTime = trueStart;
		trueStart = trueStart - msOffsetTime;
	}

	private void completeDeliveries(List<Order> receivedOrders) throws IOException, InterruptedException {

		Order curr = receivedOrders.get(0);
		int timeNeeded = 1000 * curr.getTime();
		long trueTime = System.currentTimeMillis() - msOffsetTime;
		if (starting == -1) {
			starting = timeNeeded;
		}
		long wait = timeNeeded - trueTime - starting - 110;
		if (wait > 0) {
			try {
				Thread.sleep(wait);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		// "Starting" deliveries
		for (Order order : receivedOrders) {
			item = order.getOrder();
			resName = order.getName();
			if (first) {
				startTime = System.currentTimeMillis();
				msOffsetTime = startTime;
				startTime = startTime - msOffsetTime;
				first = false;
			}
			startTime = System.currentTimeMillis() - msOffsetTime;
			time = formatTime(startTime);
			message = " Starting delivery of " + item + " to " + resName + ".";
			System.out.println(time + message);
		}
		System.out.println();
		double dist = 0.0;
		long deliveryTime = 0;
		// completing deliveries
		while (!receivedOrders.isEmpty()) {
			Restaurant r = findClosestRestaurant(receivedOrders);
			List<Order> current = receivedOrders.stream() // orders w/ same restaurant
					.filter(order -> order.getName().equals(r.getName())).collect(Collectors.toList());
			dist = findDist(lati, longi, r.getLatitude(), r.getLongitude());
			deliveryTime = findDeliveryTime(dist);
			try {
				Thread.sleep(deliveryTime);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			startTime = System.currentTimeMillis() - msOffsetTime;
			time = formatTime(startTime);
			for (Order ord : current) {
				message = " Finished delivery of " + ord.getOrder() + " to " + ord.getName() + ".";
				System.out.println(time + message);
				receivedOrders.remove(ord);
			}
			for (Order left : receivedOrders) {
				message = " Continuing delivery to " + left.getName() + ".\n";
				System.out.println(time + message);
			}
			lati = r.getLatitude();
			longi = r.getLongitude();
		}
		// finished deliveries
		startTime = System.currentTimeMillis() - msOffsetTime;
		time = formatTime(startTime);
		message = " Finished all deliveries, returning back to HQ.";
		System.out.println(time + message);

		dist = findDist(lati, longi, oglat, oglong);
		deliveryTime = findDeliveryTime(dist);
		lati = oglat;
		longi = oglong;
		try {
			Thread.sleep(deliveryTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		sendMessage("Returned to HQ.");
		startTime = System.currentTimeMillis() - msOffsetTime;
		time = formatTime(startTime);
		message = " Returned to HQ.\n";
		System.out.println("\n" + time + message);
		startTime = System.currentTimeMillis() - msOffsetTime;
		time = formatTime(startTime);
		sendMessage(time);
	}

	public Restaurant findClosestRestaurant(List<Order> orders) {
		Restaurant closestRestaurant = null;
		double shortestDistance = Double.MAX_VALUE;
		for (Order order : orders) {
			Restaurant r = findRestaurant(order, restaurants);
			double distance = findDist(lati, longi, r.getLatitude(), r.getLongitude());
			if (distance < shortestDistance) {
				closestRestaurant = r;
				shortestDistance = distance;
			}
		}
		return closestRestaurant;
	}

	private static String formatTime(long ms) {
		long hr = ms / 3600000;
		ms %= 3600000;
		long min = ms / 60000;
		ms %= 60000;
		long sec = ms / 1000;
		ms = ms % 1000;
		return String.format("[%02d:%02d:%02d.%03d]", hr, min, sec, ms);
	}

	public void sendMessage(String message) {
		if (pw != null) {
			pw.println(message);
			pw.flush();
		}
	}

	private static Restaurant findRestaurant(Order order, List<Restaurant> restaurants) {
		for (Restaurant restaurant : restaurants) {
			if (restaurant.getName().equals(order.getName())) {
				return restaurant;
			}
		}
		return null;
	}

	public static double findDist(double xa, double xb, double ya, double yb) {
		xa = Math.toRadians(xa);
		xb = Math.toRadians(xb);
		ya = Math.toRadians(ya);
		yb = Math.toRadians(yb);
		if (xa == ya && xb == yb) {
			return 0.0;
		}
		double d = 3963.000
				* Math.acos((Math.sin(xa) * Math.sin(ya)) + (Math.cos(xa) * Math.cos(ya) * Math.cos(yb - xb)));
		//		DecimalFormat df = new DecimalFormat("#.###");
		//		d = Double.parseDouble(df.format(d));
		return d;
	}

	public long findDeliveryTime(double dist) {
		long ms = (long) Math.round(dist * 1000);
		return ms;
	}

	public static void main(String[] args) {
		String hostname = "";
		int port;
		Scanner reader = new Scanner(System.in);
		while (true) {
			System.out.println("Welcome to JoesTable v2.0!\nEnter the server hostname:");
			if (reader.hasNext()) {
				hostname = reader.next();
				break;
			} else {
				System.out.print("That is not a valid hostname. \n\n");
				reader.next();
			}
		}
		while (true) {
			System.out.println("\nEnter the server port:"); // 3456
			if (reader.hasNextInt()) {
				port = reader.nextInt();
				break;
			} else {
				System.out.print("That is not a valid port. \n\n");
				reader.next();
			}
		}
		@SuppressWarnings("unused")
		Client client = new Client(hostname, port);
		reader.close();
	}
}