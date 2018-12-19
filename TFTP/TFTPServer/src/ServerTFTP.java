import java.net.*;
import java.io.*;

public class ServerTFTP {

	public static void main(String argv[]) {
		try {

			DatagramSocket sock = new DatagramSocket(69);

			System.out.println("Server Online: Port  " + sock.getLocalPort());


			while (true)
			{
				Package receive = Package.receive(sock);

				if (receive instanceof RRQPackage)
				{
					System.out.println("RRQ Processing from " + receive.getAddress());

					ServerRRQ readRequest = new ServerRRQ((RRQPackage) receive);
				}

				else if (receive instanceof WRQPackage)
				{
					System.out.println("WRQ Processing from" + receive.getAddress());

					ServerWRQ writeRequest = new ServerWRQ((WRQPackage) receive);
				}
			}
		} catch (SocketException e)
		{

			System.out.println("Server terminated(SocketException) " + e.getMessage());

		} catch (IOException e)
		{
			System.out.println("Server terminated(IOException)" + e.getMessage());
		}
	}
}
