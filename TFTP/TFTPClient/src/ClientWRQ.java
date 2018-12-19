

import java.net.*;
import java.io.*;


class ClientWRQ {
	protected InetAddress partner_ip;
	protected String filePath;
	protected String mode;
	public ClientWRQ(InetAddress ip, String name, String mode)
	{
		partner_ip = ip;

		filePath = name;

		this.mode = mode;

		try {

			DatagramSocket dataSocket = new DatagramSocket();

			dataSocket.setSoTimeout(2000);

			int timeoutLimit = 5;

			FileInputStream source = new FileInputStream("../"+ filePath);

			WRQPackage request = new WRQPackage(filePath, this.mode);

			request.send(partner_ip, 69, dataSocket);

			Package response = Package.receive(dataSocket);

			int port = response.getPort();

			if (response instanceof PackageTFTP)
			{
				PackageTFTP responsePack = (PackageTFTP) response;

				System.out.println("\nUploading");

			} else if (response instanceof ErrorPackage)
			{
				ErrorPackage responsePack = (ErrorPackage) response;

				source.close();

				throw new ExceptionTFTP(responsePack.message());
			}

			int packetLength = Package.packetLength;

			for (int blkNum = 1; packetLength == Package.packetLength; blkNum++)
			{
				DataPackage dataPackage = new DataPackage(blkNum, source);

				packetLength = dataPackage.getLength();

				dataPackage.send(partner_ip, port, dataSocket);

				if(blkNum%500==0)
				{
					System.out.print("\b.>");
				}

				if(blkNum%15000==0)
				{
					System.out.println("\b.");
				}

				while (timeoutLimit != 0)
				{
					try {
						Package ack = Package.receive(dataSocket);
						if (!(ack instanceof PackageTFTP))
						{
							break;
						}

						PackageTFTP a = (PackageTFTP) ack;

						if (port != a.getPort())
						{
							continue;

						}


						if (a.blockNumber() != blkNum) {
							System.out.println("Resend packet");

							throw new SocketTimeoutException("Resend packet");
						}

						break;
					} catch (SocketTimeoutException t0) {

						System.out.println("Retry Block " + blkNum);

						dataPackage.send(partner_ip, port, dataSocket);

						timeoutLimit--;
					}
				}

				if (timeoutLimit == 0)
				{

					throw new ExceptionTFTP("CONNECTION ERROR");
				}

			}

			source.close();

			dataSocket.close();
			
			System.out.println("\nFinished!\nFile- "+ filePath);

			System.out.println("SHA1 Checksum: " + SHA1Sum.getChecksum("../"+ filePath));

		} catch (SocketTimeoutException t) {

			System.out.println("SERVER ERROR");

		} catch (IOException e) {
			System.out.println("Input/Output ERROR");
		} catch (ExceptionTFTP e) {
			System.out.println(e.getMessage());
		}
	}

}
