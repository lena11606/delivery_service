package eunbeepa_CSCI201_Assignment03;

import java.io.Serializable;
import java.util.List;


public class Restaurant_List implements Serializable {

	private static final long serialVersionUID = 1L;

	private List<Restaurant> businesses;

	public List<Restaurant> getData() {
		return businesses;
	}

	public void setData(List<Restaurant> data) {
		this.businesses = data;
	}

	public Restaurant_List(List<Restaurant> data) {
		this.businesses = data;
	}

}
