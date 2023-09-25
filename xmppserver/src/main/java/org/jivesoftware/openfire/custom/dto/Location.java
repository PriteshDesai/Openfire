package org.jivesoftware.openfire.custom.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Location {

	private double lat;
	private double lng;

	public Location() {
		super();
	}

	public Location(double lat, double lng, long messageTime) {
		super();
		this.lat = lat;
		this.lng = lng;
	}

	@XmlElement
	public double getLat() {
		return lat;
	}

	public void setLat(double lat) {
		this.lat = lat;
	}

	@XmlElement
	public double getLng() {
		return lng;
	}

	public void setLng(double lng) {
		this.lng = lng;
	}

	@Override
	public String toString() {
		return "{\"lat\":" + lat + ",\"lng\":" + lng + "}";
	}

}
