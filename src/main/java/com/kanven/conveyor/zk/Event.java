package com.kanven.conveyor.zk;

/**
 * 
 * @author kanven
 *
 */
public class Event {

	private String parent;

	private String seq;

	private EventType type;

	public static enum EventType {
		CREATED, MASTER, EXPIRED,
	}

	public Event(EventType type, String seq, String parent) {
		this.type = type;
		this.seq = seq;
		this.parent = parent;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public String getSeq() {
		return seq;
	}

	public void setSeq(String seq) {
		this.seq = seq;
	}

	public EventType getType() {
		return type;
	}

	public void setType(EventType type) {
		this.type = type;
	}

}
