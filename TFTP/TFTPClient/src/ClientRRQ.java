
import java.net.*;
import java.io.*;

class ClientRRQ {
	protected InetAddress partner_ip;
	protected String filePath;
	protected String mode;

	public ClientRRQ(InetAddress ip, String name, String mode) {
		partner_ip = ip;
		filePath = name;
		this.mode = mode;

		try {

			DatagramSocket socket = new DatagramSocket();
			socket.setSoTimeout(2000);


			FileOutputStream file = new FileOutputStream("../"+ filePath); //parent folder


			RRQPackage request = new RRQPackage(filePath, this.mode);

			request.send(partner_ip, 69, socket);

			PackageTFTP ACK = null;

			InetAddress new_ip = partner_ip;

			int transPort = 0;

			int attemptCounter = 3;


			System.out.println("Download commencing");

			for (int blockNumber = 1, output = 512; output == 512; blockNumber++)
			{

				while (attemptCounter != 0)
				{
					try
                    {
						Package receive = Package.receive(socket);

						if (receive instanceof ErrorPackage)
						{
							ErrorPackage p = (ErrorPackage) receive;
							throw new ExceptionTFTP(p.message());
						} else if (receive instanceof DataPackage)
						{
							DataPackage data = (DataPackage) receive;

							// visual effect to user
							if (blockNumber % 500 == 0) {
								System.out.print("\b.>");
							}
							if (blockNumber % 15000 == 0) {
								System.out.println("\b.");
							}

							new_ip = data.getAddress();

							if (transPort != 0 && transPort != data.getPort())
							{
								continue;

							}
							transPort = data.getPort();


							if (blockNumber != data.blockNumber()) {

								throw new SocketTimeoutException();

							}

							output = data.write(file);

							ACK = new PackageTFTP(blockNumber);

							ACK.send(new_ip, transPort, socket);

							break;
						} else
							throw new ExceptionTFTP("UNNKNOWN RESPONSE");
					}

					catch (SocketTimeoutException t)
                    {

						if (blockNumber == 1) {
							System.out.println("Cannot Reach Server");
							request.send(partner_ip, 69, socket);
							attemptCounter--;
						}

						else
                        {
							System.out.println("Retry packet. times left=" + attemptCounter);

							ACK = new PackageTFTP(blockNumber - 1);

							ACK.send(new_ip, transPort, socket);

							attemptCounter--;
						}
					}
				}
				if (attemptCounter == 0)
				{

					throw new ExceptionTFTP("Connection ERROR");
				}
			}

			System.out.println("\nComplete.\nFile: " + filePath);
			System.out.println("Checksum: " + SHA1Sum.getChecksum("../"+ filePath));
			
			file.close();
			socket.close();
		} catch (IOException e)
        {
			System.out.println("Input/Output error");

			File wrongFile = new File(filePath);

			wrongFile.delete();

		} catch (ExceptionTFTP e)
        {
			System.out.println(e.getMessage());

			File wrongFile = new File(filePath);

			wrongFile.delete();
		}
	}
}
