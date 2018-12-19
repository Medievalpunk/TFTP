

import java.net.*;
import java.io.*;

class ExceptionTFTP extends Exception {
	public ExceptionTFTP() {
		super();
	}
	public ExceptionTFTP(String s) {
		super(s);
	}
}
//////////////////////////////////////////////////////////////////////////////
//GENERAL packet: define the packet structure, necessary members and methods// 
//of TFTP packet. To be extended by other specific packet(read, write, etc) //
//////////////////////////////////////////////////////////////////////////////
public class Package {

  // TFTP constants
  public static int tftpPort = 69;
  public static int packetLength =516;
  public static int maxTftpData=512;

  // Tftp opcodes
  protected static final short tftpRRQ=1;
  protected static final short tftpWRQ=2;
  protected static final short tftpDATA=3;
  protected static final short tftpACK=4;
  protected static final short tftpERROR=5;

  // Packet Offsets
  protected static final int opOffset=0;

  protected static final int fileOffset=2;

  protected static final int blkOffset=2;
  protected static final int dataOffset=4;

  protected static final int numOffset=2;
  protected static final int msgOffset=4;

  // The actual packet for UDP transfer
  protected byte [] message;
  protected int length;

  // Address info (required for replies)
  protected InetAddress host;
  protected int port;

  // Constructor 
  public Package() {
    message=new byte[packetLength];
    length= packetLength;
  } 

  // Methods to receive packet and convert it to yhe right type(data/ack/read/...)
  public static Package receive(DatagramSocket sock) throws IOException {
    Package in=new Package(), retPak=new Package();
    //receive data and put them into in.message
    DatagramPacket inPak = new DatagramPacket(in.message,in.length);
    sock.receive(inPak); 
    
    //Check the opcode in message, then cast the message into the corresponding type
    switch (in.get(0)) {
      case tftpRRQ:
    	  retPak=new RRQPackage();
        break;
      case tftpWRQ:
    	  retPak=new WRQPackage();
        break;
      case tftpDATA:
    	  retPak=new DataPackage();
        break;
      case tftpACK:
    	  retPak=new PackageTFTP();
        break;
      case tftpERROR:
    	  retPak=new ErrorPackage();
        break;
    }
    retPak.message=in.message;
    retPak.length=inPak.getLength();
    retPak.host=inPak.getAddress();
    retPak.port=inPak.getPort();

    return retPak;
  }
  
  //Method to send packet
  public void send(InetAddress ip, int port, DatagramSocket s) throws IOException {
    s.send(new DatagramPacket(message,length,ip,port));
  }

  // DatagramPacket like methods
  public InetAddress getAddress() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public int getLength() {
    return length;
  }

  // Methods to put opcode, blkNum, error code into the byte array 'message'. 
  protected void put(int at, short value) {
    message[at++] = (byte)(value >>> 8);  // first byte
    message[at] = (byte)(value % 256);    // last byte
  }

  @SuppressWarnings("deprecation")
  //Put the filename and mode into the 'message' at 'at' follow by byte "del"
  protected void put(int at, String value, byte del) {
    value.getBytes(0, value.length(), message, at);
    message[at + value.length()] = del;
  }

  protected int get(int at) {
    return (message[at] & 0xff) << 8 | message[at+1] & 0xff;
  }

  protected String get (int at, byte del) {
    StringBuffer result = new StringBuffer();
    while (message[at] != del) result.append((char)message[at++]);
    return result.toString();
  }
}

////////////////////////////////////////////////////////
//DATA packet: put the right code in the message; read// 
//file for sending; write file after receiving        //
////////////////////////////////////////////////////////
final class DataPackage extends Package {

	// Constructors
	protected DataPackage() {}
	public DataPackage(int blockNumber, FileInputStream in) throws IOException {
		this.message = new byte[packetLength];
		// manipulate message
		this.put(opOffset, tftpDATA);
		this.put(blkOffset, (short) blockNumber);
		// read the file into packet and calculate the entire length
		length = in.read(message, dataOffset, maxTftpData) + 4;
	}

	// Accessors

	public int blockNumber() {
		return this.get(blkOffset);
	}

	/*
	 * public void data(byte[] buffer) { buffer = new byte[length-4];
	 * 
	 * for (int i=0; i<length-4; i++) buffer[i]=message[i+dataOffset]; }
	 */
	
	// File output
	public int write(FileOutputStream out) throws IOException {
		out.write(message, dataOffset, length - 4);

		return (length - 4);
	}
}

/////////////////////////////////////////////////////////
//ERROR packet: put the right codes and error messages // 
//in the 'message'                                     //
/////////////////////////////////////////////////////////
class ErrorPackage extends Package {

	// Constructors
	protected ErrorPackage() {
	}
	//Generate error packet
	public ErrorPackage(int number, String message) {
		length = 4 + message.length() + 1;
		this.message = new byte[length];
		put(opOffset, tftpERROR);
		put(numOffset, (short) number);
		put(msgOffset, message, (byte) 0);
	}

	// Accessors
	public int number() {
		return this.get(numOffset);
	}
	public String message() {
		return this.get(msgOffset, (byte) 0);
	}
}

/////////////////////////////////////////////////////////
//ACK packet: put the right opcode and block number in // 
//the 'message'                                        //
/////////////////////////////////////////////////////////
final class PackageTFTP extends Package {

	// Constructors
	protected PackageTFTP() {
	}
	//Generate ack packet
	public PackageTFTP(int blockNumber) {
		length = 4;
		this.message = new byte[length];
		put(opOffset, tftpACK);
		put(blkOffset, (short) blockNumber);
	}

	// Accessors
	public int blockNumber() {
		return this.get(blkOffset);
	}
}


/////////////////////////////////////////////////////////
//READ packet: put the right opcode and filename, mode // 
//in the 'message'                                     //
/////////////////////////////////////////////////////////
final class RRQPackage extends Package {


// Constructors
protected RRQPackage() {}

//specify the filename and transfer mode 
public RRQPackage(String filename, String dataMode) {
	length=2+filename.length()+1+dataMode.length()+1;
	  message = new byte[length];

	  put(opOffset,tftpRRQ);
	  put(fileOffset,filename,(byte)0);
	  put(fileOffset+filename.length()+1,dataMode,(byte)0);
}

// Accessors

public String fileName() {
  return this.get(fileOffset,(byte)0);
}

public String requestType() {
  String fname = fileName();
  return this.get(fileOffset+fname.length()+1,(byte)0);
}
}

/////////////////////////////////////////////////////////
//WRITE packet: put the right opcode and filename, mode// 
//in the 'message'                                     //
/////////////////////////////////////////////////////////
final class WRQPackage extends Package {

//Constructors

protected WRQPackage() {}

public WRQPackage(String filename, String dataMode) {
	length=2+filename.length()+1+dataMode.length()+1;
	message = new byte[length];

	put(opOffset,tftpWRQ);
	put(fileOffset,filename,(byte)0);
	put(fileOffset+filename.length()+1,dataMode,(byte)0);
}

//Accessors

public String fileName() {
return this.get(fileOffset,(byte)0);
}

public String requestType() {
String fname = fileName();
return this.get(fileOffset+fname.length()+1,(byte)0);
}
}