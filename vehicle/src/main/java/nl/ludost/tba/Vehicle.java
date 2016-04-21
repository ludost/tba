/*
 * Copyright: Ludo Stellingwerff (2016), Boskoop, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package nl.ludost.tba;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.formats.Params;
import com.almende.util.URIUtil;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class Vehicle, implements a simulated vehicle according to the following
 * assumptions:<br>
 * -Vehicles move in an (empty) 2D space<br>
 * -Vehicles have speed and direction, both which can be manipulated by the
 * user.<br>
 * -Vehicles have singular size and are mass-less. (=Can change speed and
 * direction instantly)<br>
 * -Vehicles don't interact with each other or the environment.<br>
 * -Time is unchangable, message delay leads to slightly different route. Speed
 * and direction changes are applied when message arrives, not backtracked to
 * the exact time of message generation. No latency correction is done,
 * therefore the error is not compensated.
 */
@Access(AccessType.PUBLIC)
public class Vehicle extends Agent {
	private static final Logger	LOG				= Logger.getLogger(Vehicle.class
														.getName());
	Position					lastKnownPos	= null;
	double						heading			= 0;
	double						speed			= 0;

	// Vehicle uses a simple single lock for updating the position
	ReentrantLock				lock			= new ReentrantLock();

	/**
	 * Instantiates a new vehicle.
	 */
	public Vehicle() {
		super();
	}

	/**
	 * Instantiates a new vehicle.
	 *
	 * @param vehicleConfig
	 *            the vehicle config
	 */
	public Vehicle(ObjectNode vehicleConfig) {
		super(vehicleConfig);
	}

	public void onReady() {
		if (getConfig().get("updateInterval").asLong() > 0) {
			LOG.warning("Setting up position reporting"
					+ getConfig().get("updateInterval"));
			scheduleReportPosition();
		}
	}

	/**
	 * Schedule report position.
	 */
	public void scheduleReportPosition() {
		scheduleIntervalSequential("reportPosition", null,
				getConfig().get("updateInterval").asLong());
	}

	/**
	 * Report position.
	 */
	public void reportPosition() {
		calcPosition();
		// TODO: check if no significant move has been made, don't send anything
		// if move was too small.

		final URI manager = URIUtil.create(getConfig().get("manager").asText());
		final Params param = new Params();
		final ObjectNode data = JOM.getInstance().valueToTree(lastKnownPos);
		data.put("id", getId());
		data.put("speed", speed);
		data.put("heading", heading);
		param.add("position", data);
		try {
			call(manager, "updatePosition", param);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Couldn't contact my manager:" + manager, e);
		}
	}

	/**
	 * Calc the current position, updating the lastKnownPos;
	 *
	 * @return the position
	 */
	private Position calcPosition() {
		final Position result = new Position();
		lock.lock();
		{
			long now = getScheduler().now();
			if (lastKnownPos == null) {
				lastKnownPos = result;
			}
			result.timestamp = now;
			result.x = lastKnownPos.x
					+ ((now - lastKnownPos.timestamp) * speed
							* Math.cos(heading) / 1000.0);
			result.y = lastKnownPos.y
					+ ((now - lastKnownPos.timestamp) * speed
							* Math.sin(heading) / 1000.0);
			lastKnownPos = result;
		}
		lock.unlock();
		return result;
	}

	/**
	 * Gets the position in x,y at current time.
	 *
	 * @return the position
	 */
	public ObjectNode getPosition() {
		final ObjectNode result = JOM.getInstance().valueToTree(calcPosition());
		result.put("heading", heading);
		result.put("speed", speed);
		return result;
	}

	/**
	 * Sets the heading of the vehicle in radians
	 *
	 * @param heading
	 *            the new heading (radians)
	 */
	public void setHeading(@Name("heading") double heading) {
		// first update the lastKnownPos
		calcPosition();
		// set new heading
		this.heading = heading;
		reportPosition();
	}

	/**
	 * Sets the speed of the vehicle in distance per second
	 *
	 * @param speed
	 *            the new speed in steps per second
	 */
	public void setSpeed(@Name("speed") double speed) {
		// first update the lastKnownPos
		calcPosition();
		// set new speed
		this.speed = speed;
		reportPosition();
	}

	class Position {
		public Position() {};

		double	x			= 0;
		double	y			= 0;
		long	timestamp	= getScheduler().now();

		public double getX() {
			return x;
		}

		public void setX(double x) {
			this.x = x;
		}

		public double getY() {
			return y;
		}

		public void setY(double y) {
			this.y = y;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(long timestamp) {
			this.timestamp = timestamp;
		}
	}
}
