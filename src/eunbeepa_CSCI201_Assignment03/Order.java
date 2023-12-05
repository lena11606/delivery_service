package eunbeepa_CSCI201_Assignment03;

import java.io.Serializable;

//public class Order {
public class Order implements Serializable {

	private static final long serialVersionUID = 1L;

	private int time;
	private String name;
	private String orderItem;

	public Order(int time, String name, String orderItem) {
		this.time = time;
		this.name = name;
		this.orderItem = orderItem;
	}

	public int getTime() {
		return time;
	}

	public String getName() {
		return name;
	}

	public String getOrder() {
		return orderItem;
	}

	public void setTime(int time) {
		this.time = time;
	}

	public void setOrder(String order) {
		this.orderItem = order;
	}

	@Override
	public String toString() {
		return "\nTime: " + time + "\nRestaurant: " + name + "\nOrder Item: " + orderItem;
	}

}
