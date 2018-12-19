import java.net.*;
import java.io.*;


class ServerRRQ extends Thread {

	protected DatagramSocket socket;

	protected InetAddress partner_ip;

	protected int port;

	protected FileInputStream is;

	protected Package request;

	protected int attemptCounter =3;

	protected String filePath;

	// initialize read request
	public ServerRRQ(RRQPackage request) {
		try {
			this.request = request;

			//open new socket with random port num for tranfer
			socket = new DatagramSocket();

			socket.setSoTimeout(1000);

			filePath = request.fileName();

			partner_ip = request.getAddress();

			port = request.getPort();

			File readFile = new File("../"+ filePath);

			if (readFile.exists() && readFile.isFile() && readFile.canRead())
			{
				is = new FileInputStream(readFile);

				this.start();

			} else
				throw new ExceptionTFTP("access error");

		} catch (Exception e) {

			ErrorPackage errorPackage = new ErrorPackage(1, e.getMessage());

			try {

				errorPackage.send(partner_ip, port, socket);

			} catch (Exception f) {
			}

			System.out.println("Cannot Start Client:  " + e.getMessage());
		}
	}


	public void run() {

		int bytesRead = Package.packetLength;

		// handle read request

		if (request instanceof RRQPackage) {
			try {
				for (int blockNumber = 1; bytesRead == Package.packetLength; blockNumber++)
				{
					DataPackage outboundData = new DataPackage(blockNumber, is);

					bytesRead = outboundData.getLength();

					outboundData.send(partner_ip, port, socket);

					while (attemptCounter !=0) {
						try {

							Package ack = Package.receive(socket);

							if (!(ack instanceof PackageTFTP)){throw new Exception("ERROR: Client");}

							PackageTFTP a = (PackageTFTP) ack;
							
							if(a.blockNumber()!=blockNumber){

								throw new SocketTimeoutException("last packet lost, resend packet");}

							break;
						} 
						catch (SocketTimeoutException t) {

							System.out.println("Resent blk " + blockNumber);

							attemptCounter--;

							outboundData.send(partner_ip, port, socket);
						}
					}

					if(attemptCounter ==0)
					{
						throw new Exception("Failed Connection");
					}
				}

				System.out.println("Transfer to " + partner_ip +" is complete" );
				System.out.println("File: "+ filePath + "\nSHA1 checksum: "+ SHA1Sum.getChecksum("../"+ filePath)+"\n");

			} catch (Exception e) {

				ErrorPackage errorPackage = new ErrorPackage(1, e.getMessage());

				try {

					errorPackage.send(partner_ip, port, socket);

				} catch (Exception f) {
				}

				System.out.println("Client failed:  " + e.getMessage());
			}
		}
	}
}