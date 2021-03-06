package net.sctp4nat.sample.extended;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sctp4nat.connection.NetworkLink;
import net.sctp4nat.connection.SctpDefaultStreamConfig;
import net.sctp4nat.core.SctpChannelFacade;
import net.sctp4nat.origin.Sctp;
import net.sctp4nat.origin.SctpDataCallback;
import net.sctp4nat.util.SctpInitException;

public class SampleUdpUpgradeableClient {

	private static final Logger LOG = LoggerFactory.getLogger(SampleUdpUpgradeableClient.class);
	private static final String TEST_STR = "Hello World!";

	public static void main(String[] args) throws IOException, SctpInitException {
		
		InetSocketAddress clientSoAddr = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 45678);
		InetSocketAddress serverSoAddr = new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 5689);
		
		Sctp.getInstance().init();
		@SuppressWarnings("resource")
		UpgradeableUdpSocketSample udpSocket = new UpgradeableUdpSocketSample(clientSoAddr.getPort(),
				clientSoAddr.getAddress());
		udpSocket.setCb(new SctpDataCallback() {

			@Override
			public void onSctpPacket(byte[] data, int sid, int ssn, int tsn, long ppid, int context,
					int flags, SctpChannelFacade facade) {
				LOG.debug("CLIENT GOT MESSAGE: "+ new String(data, StandardCharsets.UTF_8));
				LOG.debug("REPLY SUCCESS");
			}
		});

		DatagramPacket testPacket = new DatagramPacket(TEST_STR.getBytes(), TEST_STR.length());
		testPacket.setAddress(serverSoAddr.getAddress());
		testPacket.setPort(serverSoAddr.getPort());
		udpSocket.send(testPacket);

			SctpDefaultStreamConfig config = new SctpDefaultStreamConfig();
			Promise<SctpChannelFacade, Exception, NetworkLink> promise = udpSocket.upgrade(config,
					clientSoAddr, serverSoAddr);

			promise.done(new DoneCallback<SctpChannelFacade>() {

				@Override
				public void onDone(SctpChannelFacade result) {
					result.send(TEST_STR.getBytes(), false, 0, 0);
				}
			});

			promise.fail(new FailCallback<Exception>() {

				@Override
				public void onFail(Exception result) {
					result.printStackTrace();
				}
			});
	}
}
