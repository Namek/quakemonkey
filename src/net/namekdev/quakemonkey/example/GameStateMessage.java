package net.namekdev.quakemonkey.example;

import java.util.List;

/**
 * Example master gamestate that has to be sent from the server to the client
 * every server tick.
 * 
 * @author Ben Ruijl
 * 
 */
public class GameStateMessage {
	private String name;
	private List<Float> position;
	private List<Float> orientation;
	private byte id;

	public GameStateMessage() {
	}

	public GameStateMessage(String name, List<Float> position, List<Float> orientation, byte id) {
		this.name = name;
		this.position = position;
		this.orientation = orientation;
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public List<Float> getPosition() {
		return position;
	}

	public List<Float> getOrientation() {
		return orientation;
	}

	public byte getId() {
		return id;
	}
}
