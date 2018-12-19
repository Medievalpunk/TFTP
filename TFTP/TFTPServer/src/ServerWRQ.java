import java.net.*;
import java.io.*;


class ServerWRQ extends Thread {

	protected DatagramSocket socket;
	protected InetAddress partner_ip;
	protected int port;
	protected FileOutputStream os;
	protected Package request;
	protected int attemptCounter = 3;
	protected File file;
	protected String filePath;


	public ServerWRQ(WRQPackage request)
	{
		try
		{
			this.request = request;

			socket = new DatagramSocket();

			socket.setSoTimeout(1000);

			partner_ip = request.getAddress();

			port = request.getPort();

			filePath = request.fileName();


			file = new File("../"+ filePath);

			if (!file.exists())
			{
				os = new FileOutputStream(file);

				PackageTFTP a = new PackageTFTP(0);

				a.send(partner_ip, port, socket);

				this.start();
			} else
				throw new ExceptionTFTP("File Already Exists");

		} catch (Exception e)
		{

			ErrorPackage errorPackage = new ErrorPackage(1, e.getMessage());
			try
			{

				errorPackage.send(partner_ip, port, socket);

			} catch (Exception f) {
			}

			System.out.println("Client ERROR:" + e.getMessage());
		}
	}

	public void run() {

		if (request instanceof WRQPackage)
		{
			try
			{
				for (int blockNumber = 1, outputBytes = 512; outputBytes == 512; blockNumber++)
				{
					while (attemptCounter != 0)
					{
						try {
							Package recieve = Package.receive(socket);

							if (recieve instanceof ErrorPackage)
							{
								ErrorPackage p = (ErrorPackage) recieve;

								throw new ExceptionTFTP(p.message());

							} else if (recieve instanceof DataPackage)
							{
								DataPackage p = (DataPackage) recieve;


								if (p.blockNumber() != blockNumber) {

									throw new SocketTimeoutException();
								}

								outputBytes = p.write(os);

								PackageTFTP a = new PackageTFTP(blockNumber);

								a.send(partner_ip, port, socket);

								break;
							}
						} catch (SocketTimeoutException t2) {
							System.out.println("Resend ACK");
							PackageTFTP ACK = new PackageTFTP(blockNumber - 1);
							ACK.send(partner_ip, port, socket);
							attemptCounter--;
						}
					}
					if(attemptCounter ==0)
					{
						throw new Exception("CONNECTION ERROR");
					}
				}
				System.out.println("Transfer to " + partner_ip +" is finished" );
				System.out.println("Filename: "+ filePath + "\nSHA1 checksum: "+ SHA1Sum.getChecksum("../"+ filePath)+"\n");
				
			} catch (Exception e)
			{

				ErrorPackage errorPackage = new ErrorPackage(1, e.getMessage());

				try {
					errorPackage.send(partner_ip, port, socket);
				} catch (Exception f) {
				}

				System.out.println("Client ERROR:  " + e.getMessage());

				file.delete();
			}
		}
	}
}