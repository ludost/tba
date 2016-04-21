/*
 * Copyright: Ludo Stellingwerff (2016), Boskoop, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package nl.ludost.tba;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentConfig;
import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.annotation.Sender;
import com.almende.util.URIUtil;
import com.almende.util.callback.AsyncCallback;
import com.almende.util.uuid.UUID;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class Manager.
 */
@Access(AccessType.PUBLIC)
public class Manager extends Agent {
	private static final Logger		LOG			= Logger.getLogger(Manager.class
														.getName());
	ConcurrentLinkedQueue<Vehicle>	vehicles	= new ConcurrentLinkedQueue<Vehicle>();
	ConcurrentLinkedQueue<URI>		clients		= new ConcurrentLinkedQueue<URI>();

	/**
	 * Creates a vehicle in this VM
	 */
	public void createVehicle() {
		final String id = new UUID().toString();
		final AgentConfig vehicleConfig = AgentConfig
				.decorate((ObjectNode) getConfig().get("vehicleConfig"));
		vehicleConfig.setId(id);
		vehicles.add(new Vehicle(vehicleConfig));
	}

	/**
	 * Gets the vehicles known in the VM
	 *
	 * @return the vehicles
	 */
	public List<URI> getVehicles() {
		final List<URI> result = new ArrayList<URI>(vehicles.size());
		final Iterator<Vehicle> iter = vehicles.iterator();
		while (iter.hasNext()) {
			final Vehicle vehicle = iter.next();
			result.add(vehicle.getUrlByScheme("http"));
		}
		return result;
	}

	/**
	 * Gets the UIs that have been registered in this VM
	 *
	 * @return the UIs
	 */
	public List<URI> getUIs() {
		return Arrays.asList(clients.toArray(new URI[0]));
	}

	/**
	 * Register ui.
	 *
	 * @param sender
	 *            the sender
	 */
	public void registerUI(@Sender URI sender) {
		LOG.warning("Registering UI:" + sender);
		clients.add(sender);
	}

	/**
	 * Update position of a vehicle, multiplexed to all listening UIs.
	 *
	 * @param position
	 *            the position
	 */
	public void updatePosition(final @Name("position") ObjectNode position) {
		for (URI uri : clients) {
			try {
				call(uri, "updatePosition", position);
			} catch (IOException e) {
				LOG.log(Level.WARNING, "Couldn't update:" + uri, e);
				clients.remove(uri);
			}
		}
	}

	/**
	 * Control a vehicle from the UIs, proxy function.
	 *
	 * @param id
	 *            the id
	 * @param method
	 *            the method
	 * @param params
	 *            the params
	 */
	public void control(final @Name("id") String id,
			final @Name("method") String method,
			final @Name("params") ObjectNode params) {
		try {
			call(URIUtil.create("local:" + id), method, params,
					new AsyncCallback<Void>() {

						@Override
						public void onSuccess(Void result) {}

						@Override
						public void onFailure(Exception exception) {
							LOG.log(Level.WARNING,
									"Vehicle couldn't be controlled:" + id,
									exception);
						}
					});
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Couldn't command:" + id, e);
		}
	}
}
