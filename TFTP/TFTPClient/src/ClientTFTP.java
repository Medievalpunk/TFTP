

import java.net.InetAddress;
import java.net.UnknownHostException;


class MyException extends Exception 
{
	
	public MyException() 
	{
		super();
	}

	public MyException(String s)
	{
		super(s);
	}
}

public class ClientTFTP {
	public static void main(String argv[]) 
	{
		String serverIP = "";
		String filePath = "";
		String mode="netascii"; //default mode
		String RQType="";
		try {
			
			if (argv.length == 0)
				
				throw new MyException("\tUsage \nnetascii mode:  ClientTFTP [serverIP] [R/W] [filename] \nother mode:  ClientTFTP [serverIP] [R/W] [filename] [mode]" );
			
			
			if(argv.length == 3)
			{
				serverIP =argv[0];
				
			    RQType = argv[argv.length - 2];
			    
			    filePath = argv[argv.length - 1];
			}
			    
			
			else if(argv.length == 4)
			{
				serverIP = argv[0];
				
				mode =argv[argv.length-1];
				
				RQType = argv[argv.length - 3];
				
				filePath = argv[argv.length - 2];
				
			}
			
			else throw new MyException("Unnknown Command. \n\tUsage \nnetascii mode:  ClientTFTP [serverIP] [R/W] [filename] " +
						"\nother mode:  ClientTFTP [serverIP] [R/W] [filename] [mode]");

			InetAddress partner_ip = InetAddress.getByName(serverIP);

			if(RQType.matches("R"))
			{

				ClientRRQ readRequest = new ClientRRQ(partner_ip, filePath, mode);

			}

			else if(RQType.matches("W"))
			{

				ClientWRQ writeRequest = new ClientWRQ(partner_ip, filePath, mode);

			}
			else
				{
				throw new MyException("Unnknown Command. \n\tUsage \nnetascii mode:  ClientTFTP [serverIP] [R/W] [filename] " +
						"\nother mode:  ClientTFTP [serverIP] [R/W] [filename] [mode]");
				}
			
		}
		catch (UnknownHostException e)
		{

			System.out.println("WRONG serverIP " + serverIP);

		}
		catch (MyException e)
		{

			System.out.println(e.getMessage());

		}
	}
}