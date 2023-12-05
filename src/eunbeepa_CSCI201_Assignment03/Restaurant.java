package eunbeepa_CSCI201_Assignment03;

import java.io.Serializable;

import com.google.gson.annotations.SerializedName;


public class Restaurant implements Serializable {

	private static final long serialVersionUID = 1L;

	// 'How to set latitude and longitude, given they're in JSON file under an array 'coordinates' prompt (2 lines). ChatGPT, OpenAI, 9.30.2023, chat.openai.com/chat
	@SerializedName("coordinates")
	private transient Coordinates coordinates;

	public static class Coordinates {
		private double latitude;
		private double longitude;

		public double getLatitude() {
			return latitude;
		}

		public double getLongitude() {
			return longitude;
		}
	}

	public String name;
	public double latitude;
	public double longitude;
	public int drivers;
	private double dist;

	public Restaurant(String name) {
		this.name = name;
		this.coordinates = new Coordinates();
		this.coordinates.latitude = 0;
		this.coordinates.longitude = 0;
		this.drivers = 0;
		this.dist = 0;
	}

	public Restaurant(String name, double latitude, double longitude, int drivers, double dist) {
		this.name = name;
		this.coordinates = new Coordinates();
		this.coordinates.latitude = latitude;
		this.coordinates.longitude = longitude;
		this.drivers = drivers;
		this.dist = dist;
	}

	// getters 
	public String getName() {
		return name;
	}

	public int getDrivers() {
		return drivers;
	}

	public double getLatitude() {
		return coordinates.getLatitude();
	}

	public double getLongitude() {
		return coordinates.getLongitude();
	}

	public double getDistance() {
		return dist;
	}

	// setters
	public void setName(String name) {
		this.name = name;
	}

	public void setLatitude(double latitude) {
		this.coordinates.latitude = latitude;
	}

	public void setLongitude(double longitude) {
		this.coordinates.longitude = longitude;
	}

	public void setDistance(double dist) {
		this.dist = dist;
	}

	@Override
	public String toString() {
		return "\nRestaurant: " + name + "\nLatitude: " + Double.toString(coordinates.latitude) + "\nLongitude: "
				+ Double.toString(coordinates.longitude);
	}


}
