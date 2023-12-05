package eunbeepa_CSCI201_Assignment03;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Server extends Thread implements Serializable {
	private static final long serialVersionUID = 1L;


	private PrintWriter pw;
	private BufferedReader br;
	private Communicator comm;
	private Socket socket;
	private ObjectOutputStream oos;
	private boolean isReady = false;
	private String finishedTimeString = "";

	public Server(Socket s, Communicator comm) {
		try {
			this.comm = comm;
			this.socket = s;
			pw = new PrintWriter(this.socket.getOutputStream());
			br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
			oos = new ObjectOutputStream(this.socket.getOutputStream());
			this.start();

		} catch (IOException ioe) {
			System.out.println("Server constructor: " + ioe.getMessage());
		}
	}

	public boolean isReady() {
		return isReady;
	}

	public void setReady(boolean ready) {
		isReady = ready;
	}

	public void sendMessage(String message) {
		pw.println(message);
		pw.flush();
	}

	public void run() {
		try {
			while (true) {
				String line = br.readLine();
				if (line != null) {
					if (line.equals("Ready") || line.equals("Returned to HQ.")) {
						isReady = true;
						comm.serverIsReady(this);
					} else {
						String regex = "\\[\\d{2}:\\d{2}:\\d{2}\\.\\d{3}\\]";
						Pattern pattern = Pattern.compile(regex);
						Matcher matcher = pattern.matcher(line);
						if (matcher.matches()) {
							finishedTimeString = line;
						}
					}
				}
			}
		} catch (IOException ioe) {
			System.out.println("Server run: " + ioe.getMessage());
		}
	}

	public String getFinishedTime() {
		return finishedTimeString;
	}

	public void sendOrders(List<Order> orderBatch) {
		try {
			oos.writeObject(orderBatch);
			oos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendRestaurants(List<Restaurant> rests) {
		try {
			oos.writeObject(rests);
			oos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void sendOrders(String timeString) {
		try {
			oos.writeObject(timeString);
			oos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
