package com.aggregateKafkaStream;

public class IoTEvent {
	
	public String messageId;
	public String deviceId;
	public String timestamp;
	public String category;
	public String measure1;
	public String measure2;
	
	public IoTEvent(String messageId, String deviceId, String timestamp, String category, String measure1, String measure2) {
		super();
		this.messageId = messageId;
		this.deviceId = deviceId;
		this.timestamp = timestamp;
		this.category = category;
		this.measure1 = measure1;
		this.measure2 = measure2;
	}

	public IoTEvent(){
		super();
	}

	public String getMessageId() {
		return messageId;
	}
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	public String getDeviceId() {
		return deviceId;
	}
	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}	
	public String getCategory() {
		return category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	public String getMeasure1() {
		return measure1;
	}
	public void setMeasure1(String measure1) {
		this.measure1 = measure1;
	}
	public String getMeasure2() {
		return measure2;
	}
	public void setMeasure2(String measure2) {
		this.measure2 = measure2;
	}
	
	@Override
	public String toString() {
		return  messageId + "," + deviceId + ", " + timestamp
				+ ", " + category + "," + measure1 + ", " + measure2 ;
	}
	
}
