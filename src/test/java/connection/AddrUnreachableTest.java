package connection;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.junit.After;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4nat.connection.SctpConnection;
import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.core.SctpPorts;
import net.sctp4nat.origin.Sctp;
import net.sctp4nat.origin.SctpAcceptable;
import net.sctp4nat.origin.SctpDataCallback;
import net.sctp4nat.origin.SctpNotification;
import net.sctp4nat.origin.SctpSocket.NotificationListener;
import net.sctp4nat.util.SctpInitException;
import net.sctp4nat.util.SctpUtils;

public class AddrUnreachableTest {

	private static final Logger LOG = LoggerFactory.getLogger(AddrUnreachableTest.class);
	
	Thread server;
	Thread client;
	CountDownLatch shutdownCb = new CountDownLatch(1);
	CountDownLatch closeCb = new CountDownLatch(2);
	
	@Test
	public void lostConnectionTest() throws InterruptedException {

		CountDownLatch serverSetup = new CountDownLatch(1);
		CountDownLatch clientMessageExchange = new CountDownLatch(1);
		CountDownLatch addrUnreachable = new CountDownLatch(1);
		CountDownLatch serverSocketClosed = new CountDownLatch(1);

		InetAddress localhostCandidate = null;
		try {
			localhostCandidate = Inet6Address.getByName("::1");
		} catch (UnknownHostException e1) {
			fail(e1.getMessage());
		}

		final InetAddress localhost = localhostCandidate;

		server = new Thread(new Runnable() {

			@Override
			public void run() {

				SctpDataCallback cb = new SctpDataCallback() {

					@Override
					public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
							SctpChannelFacade so) {
						SctpChannelFacade facade = (SctpChannelFacade) so;
						LOG.debug("I WAS HERE");
						LOG.debug("got data: " + new String(data, StandardCharsets.UTF_8));
						facade.send(data, false, sid, (int) ppid);
						
						try {
							clientMessageExchange.await(5, TimeUnit.SECONDS);
							SctpUtils.getLink().close(); //fake crash
							Promise<Object, Exception, Object> p = facade.close();
							p.done(new DoneCallback<Object>() {

								@Override
								public void onDone(Object result) {
									serverSocketClosed.countDown();
								}
							});
							
							p.fail(new FailCallback<Exception>() {
								
								@Override
								public void onFail(Exception result) {
									fail(result.getMessage());
								}
							});
						} catch (InterruptedException e) {
							fail(e.getMessage());
						}
					}
				};

				try {
					SctpUtils.init(localhost, SctpPorts.SCTP_TUNNELING_PORT, cb);
				} catch (SocketException | SctpInitException e) {
					fail(e.getMessage());
				}

				LOG.debug("Server ready!");
				serverSetup.countDown();
				
				
				try {
					if (!shutdownCb.await(280, TimeUnit.SECONDS)) {
						fail("Timeout!");
					}
				} catch (InterruptedException e) {
					fail(e.getMessage());
				}
				
				CountDownLatch close = new CountDownLatch(1);
				
				Promise<Void, Exception, Void> closePromise = SctpUtils.shutdownAll();
				closePromise.done(new DoneCallback<Void>() {

					@Override
					public void onDone(Void result) {
						close.countDown();
					}
				});
				
				try {
					if (!close.await(10,  TimeUnit.SECONDS)) {
						fail("Timeout called! teardown could not be executed successfully!");
					} else {
						closeCb.countDown();
					}
				} catch (InterruptedException e) {
					fail(e.getMessage());
				}
			}
		});

		client = new Thread(new Runnable() {

			@Override
			public void run() {
				Sctp.getInstance().init();

				InetSocketAddress local = new InetSocketAddress(localhost, SctpPorts.getInstance().generateDynPort());
				InetSocketAddress remote = new InetSocketAddress(localhost, SctpPorts.SCTP_TUNNELING_PORT);

				SctpDataCallback cb = new SctpDataCallback() {

					@Override
					public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context, int flags,
							SctpChannelFacade so) {
						System.out.println("I WAS HERE");
						System.out.println("got data: " + new String(data, StandardCharsets.UTF_8));
						
						clientMessageExchange.countDown();
						
						so.setNotificationListener(new NotificationListener() {
							
							@Override
							public void onSctpNotification(SctpAcceptable socket, SctpNotification notification) {
								if (notification.toString().indexOf("ADDR_UNREACHABLE") >= 0){
									LOG.error("Heartbeat missing! Now shutting down the SCTP connection...");
									Promise<Object, Exception, Object> p = so.close();
									p.done(new DoneCallback<Object>() {

										@Override
										public void onDone(Object result) {
											addrUnreachable.countDown();
										}
									});
									
									p.fail(new FailCallback<Exception>() {

										@Override
										public void onFail(Exception result) {
											fail(result.getMessage());
										}
									});
								} else {
									LOG.debug(notification.toString());
								}
							}
						});
						
						try {
							serverSocketClosed.await(5, TimeUnit.SECONDS);
						} catch (InterruptedException e) {
							fail(e.getMessage());
						}
						
						if (serverSocketClosed.getCount()>0) {
							fail("serverSocket was not closed!");
						}
						
//						server.interrupt();
					}
				};

				SctpConnection channel = SctpConnection.builder().local(local).remote(remote).build();
				Promise<SctpChannelFacade, Exception, Void> p = null;
				try {
					p = channel.connect(null);
				} catch (Exception e) {
					e.printStackTrace();
					fail(e.getMessage());
				}
				
				p.done(new DoneCallback<SctpChannelFacade>() {

					@Override
					public void onDone(SctpChannelFacade result) {
						result.setSctpDataCallback(cb);
						result.send("Hello World!".getBytes(), false, 0, 0);
					}
				});
				
				try {
					if (!shutdownCb.await(280, TimeUnit.SECONDS)) {
						fail("Timeout!");
					}
				} catch (InterruptedException e) {
					fail(e.getMessage());
				}
				
				CountDownLatch close = new CountDownLatch(1);
				
				Promise<Void, Exception, Void> closePromise = SctpUtils.shutdownAll();
				closePromise.done(new DoneCallback<Void>() {

					@Override
					public void onDone(Void result) {
						close.countDown();
					}
				});
				
				try {
					if (!close.await(10,  TimeUnit.SECONDS)) {
						fail("Timeout called! teardown could not be executed successfully!");
					} else {
						closeCb.countDown();
					}
				} catch (InterruptedException e) {
					fail(e.getMessage());
				}
			}
		});

		server.start();

		try {
			serverSetup.await(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}

		client.start();
		try {
			addrUnreachable.await(270, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
		
		assertTrue(addrUnreachable.getCount()==0);
		
		shutdownCb.countDown();
		
		if (!closeCb.await(300, TimeUnit.SECONDS)) {
			fail("Timeout");
		}
	}
	
	@After
	public void tearDown() throws IOException, InterruptedException {
		server.interrupt();
		client.interrupt();
		
		CountDownLatch close = new CountDownLatch(1);
		
		Promise<Void, Exception, Void> closePromise = SctpUtils.shutdownAll();
		closePromise.done(new DoneCallback<Void>() {

			@Override
			public void onDone(Void result) {
				close.countDown();
			}
		});
		
		try {
			if (!close.await(10,  TimeUnit.SECONDS)) {
				fail("Timeout called! teardown could not be executed successfully!");
			} else {
				closeCb.countDown();
			}
		} catch (InterruptedException e) {
			fail(e.getMessage());
		}
	}
}
