/*
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sctp4nat.core;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;
import net.sctp4nat.origin.SctpDataCallback;
import net.sctp4nat.origin.SctpSocket;

/**
 * This classs caches all SCTP connections. This is necessary, because usrsctp
 * does not consume information about the remote endpoint. If a user needs to be
 * able to reply via {@link SctpDataCallback}, it needs to know which
 * {@link SctpSocket} is assigned to which remote {@link InetSocketAddress}.
 * 
 * @author root
 *
 */
public class SctpMapper {

	private static final Logger LOG = LoggerFactory.getLogger(SctpMapper.class);
	private static final int CLOSE_TIMEOUT = 10;

	/**
	 * The {@link Map} containing all active {@link SctpChannel}s.s
	 */
	private static final ConcurrentHashMap<InetSocketAddress, SctpChannel> socketMap = new ConcurrentHashMap<>();

	@Getter
	@Setter
	private static boolean isShutdown = false;

	/**
	 * Caches a new {@link SctpChannel} and its corresponding remote information.
	 * 
	 * @param remote
	 *            {@link InetSocketAddress}
	 * @param so
	 *            {@link SctpChannel}
	 * 
	 */
	public synchronized void register(final InetSocketAddress remote, final SctpChannel so) {
		if (isShutdown) {
			LOG.warn("Could not register remote, because SctpMapper is shutting down its connections!");
			return;
		}

		if (remote != null && so != null) {
			socketMap.put(remote, so);
		} else {
			LOG.error("Invalid input, socket could not be registered! Either remote or so was null!");
		}
	}

	/**
	 * This method removes a socket from the {@link Mapper} and shuts down the
	 * usrsctp counterpart. Make sure this {@link SctpChannel} instance is not used
	 * anywhere else!
	 */
	@SuppressWarnings("unlikely-arg-type")
	public synchronized void unregister(SctpChannel so) {
		if (isShutdown) {
			LOG.warn("Could not unregister SctpChannel, because SctpMapper is shutting down its connections!");
			return;
		}

		if (so == null) {
			LOG.error("Invalid input, null can't be removed!");
			return;
		} else if (!socketMap.contains(so)) {
			LOG.error("Invalid input, a socket, which is not registered, cannot be removed!");
			return;
		} else {
			socketMap.remove(so);
		}
	}

	/**
	 * This method removes a socket from the {@link Mapper} and shuts down the
	 * usrsctp counterpart. Make sure this {@link SctpChannel} instance is not used
	 * anywhere else!
	 */
	public synchronized void unregister(InetSocketAddress remote) {
		if (isShutdown) {
			LOG.warn("Could not unregister remote, because SctpMapper is shutting down its connections!");
			return;
		}

		if (remote == null) {
			LOG.error("Invalid input, null can't be removed!");
			return;
		} else if (!socketMap.containsKey(remote)) {
			LOG.error("Invalid input, a socket, which is not registered, cannot be removed!");
			return;
		} else {
			socketMap.remove(remote);
		}
	}

	/**
	 * This method locates a {@link SctpChannel} object given the remote
	 * {@link InetAddress} and port.
	 * 
	 * @param remoteAddress
	 *            {@link String} representation of an IP address (e.g.
	 *            \"192.168.0.1\")
	 *            
	 * @param remotePort
	 * @return {@link SctpChannel}
	 */
	public synchronized static SctpChannel locate(final String remoteAddress, final int remotePort) {
		if (isShutdown) {
			LOG.warn("Could not locate SctpChannel, because SctpMapper is shutting down its connections!");
			return null;
		}

		for (Map.Entry<InetSocketAddress, SctpChannel> element : socketMap.entrySet()) {
			int port = element.getKey().getPort();
			String address = element.getKey().getAddress().getHostAddress();

			if (port == remotePort && address.equals(remoteAddress)) {
				return socketMap.get(element.getKey());
			}
		}

		LOG.info("No socketMap entry found for IP:" + remoteAddress + " and port: " + remotePort);
		return null;
	}

	/**
	 * This method locates a {@link SctpChannel} object given a {@link SctpSocket}.
	 * 
	 * @param sctpSocket
	 *            {@link SctpSocket}
	 * @return
	 */
	public synchronized static SctpChannel locate(final SctpSocket sctpSocket) {
		if (isShutdown) {
			LOG.warn("Could not locate SctpChannel, because SctpMapper is shutting down its connections!");
			return null;
		}

		if (socketMap.isEmpty()) {
			return null;
		}

		SctpChannel facade = null;
		try {
			facade = socketMap.values().stream().filter(so -> so.containsSctpSocket(sctpSocket)).findFirst().get();
		} catch (NoSuchElementException e) {
			LOG.error("Could not retrieve SctpSocket from SctpMapper!");
			return null;
		}

		if (facade == null) {
			LOG.error("Could not retrieve SctpSocket from SctpMapper!");
			return null;
		} else {
			return facade;
		}
	}

	/**
	 * This method shuts down all remaining connections and closes them. Afterwards
	 * the socketMap is cleared.
	 * 
	 * @throws InterruptedException
	 * @throws TimeoutException
	 */
	public void shutdown() throws InterruptedException, TimeoutException {
		isShutdown = true;
		CountDownLatch close = new CountDownLatch(socketMap.size());

		for (Map.Entry<InetSocketAddress, SctpChannel> element : socketMap.entrySet()) {
			SctpChannel so = element.getValue();
			Promise<Object, Exception, Object> p = so.close();
			p.done(new DoneCallback<Object>() {

				@Override
				public void onDone(Object result) {
					close.countDown();
				}
			});
			p.fail(new FailCallback<Exception>() {

				@Override
				public void onFail(Exception result) {
					LOG.error("Could not shutdown connection to " + so.getRemote().getAddress().getHostAddress() + ":"
							+ so.getRemote().getPort());
				}
			});

		}

		if (!close.await(CLOSE_TIMEOUT, TimeUnit.SECONDS)) {
			LOG.error("Timeout called, because not all connections were closed correctly in time");
			throw new TimeoutException();
		} else {
			LOG.debug("all sctp connections closed");

			socketMap.clear();
			LOG.debug("socketMap cleared");
		}
		isShutdown = false;
	}
}
