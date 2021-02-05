package clientServer;

import java.io.BufferedInputStream; // used to read info or data from stream
import java.io.BufferedOutputStream; //used to store  data 
import java.util.concurrent.TimeUnit; //timing and delay
import java.net.ConnectException; //if connection is not allowed
import java.io.DataInputStream; //read java primatives..larger than 1 byte
import java.io.DataOutputStream; //write primative data types in output stream
import java.io.File;//directory and pathnames
import java.io.FileInputStream;//obtains input bytes from file
import java.io.FileOutputStream;//writes data to a file
import java.util.Hashtable; // used for connection and used for maps keys to values
import java.io.IOException; //I/O exception
import java.io.InputStream;//input stream of bytes
import java.net.InetAddress; //used to get IP address of any host name
import java.io.ObjectInputStream;// deserialize data and objects
import java.io.ObjectOutputStream; // objects can be read
import java.net.ServerSocket;// waits for requests to come into the network
import java.net.Socket;//endpoint for communication with 2 computers 
import java.util.Scanner;//for input
import java.util.Set; //no duplicate elements
import java.net.UnknownHostException;// IP address is unknown

public class SynchronizeFiles{
	public static void main(String args[]) throws Exception, UnknownHostException {
		System.out.println(InetAddress.getLocalHost());
		Scanner scan = new Scanner(System.in);
        
        //we will be creating a hash table that will contain files and thir current times
		final Hashtable <String, Integer> hash = new Hashtable<String, Integer>();
		System.out.println("input file directory: ");
		String fileDirectory = scan.next();
		
		int choice = 1;
		
		while (choice >= 1 && choice <= 3) {
			System.out.println("What would you like to do?");
			displayMainMenu();
			
			choice = scan.nextInt();
			
			switch (choice) {//Switch cases for the user to choose
                //getNetworkIPs() will show the IP address of anyone on the current network
				case 1: 
					getNetworkIPs();
					
                    // this will pause so it can look for clients
                    // will continue to search for clients on the same network 
					TimeUnit.SECONDS.sleep(3);
                    break;
                    
				//in this case we will receive the actual files when we create the hash table
				case 2: 
					createHashtable(fileDirectory, hash);
					
					ServerSocket server = new ServerSocket(1717);
					server.setReuseAddress(true);
					
					System.out.println("Request waiting");
					
					// ServerSocket accepts request and creates a socket
					Socket socket = server.accept();

					System.out.println("1");
					socket.setReuseAddress(true); // To avoid any port connection issues
					System.out.println("Successful connection with " + socket.getInetAddress().toString());

                    //the other client will receive other computers HT
					
					ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
					oos.writeObject(hash);
                    
                    //comparion is made with HT and files(2) 
					
					InputStream is = socket.getInputStream();
					ObjectInputStream ois = new ObjectInputStream(is);
					Hashtable <String, Integer> hash2 = (Hashtable <String, Integer>) ois.readObject();
					//checks to see if there are missing files and how many from other client
					
					int fileCount = 0;
					Set<String> keys = hash2.keySet();
					for(String key: keys) {
						if(!hash.containsKey(key)){
                            //other clients file is inputed and the time to the HT
			
							hash.put(key, hash2.get(key));
							fileCount += 1;
						}
						else if(hash.containsKey(key) && hash2.containsKey(key) && 
								hash.get(key) != hash2.get(key)){
                            //delete old file if updated 
			    			if(hash2.get(key) > hash.get(key)){
								fileCount += 1;
								File file = new File(key);
								file.delete();
							}
						}
					}
					System.out.println(fileCount);
					//onbtain contents from the files 
					DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
					
					for (int i = 0; i < fileCount; i++){
						receiveFile(in);
					}
					
					server.close();//closes server 
					socket.close();//closes socket
					break;
                // case 3 will connect to client thats in network and then 
                // send files 
				case 3: 
					createHashtable(fileDirectory, hash);
					
					System.out.print("Enter ther IP address you want to connect to: ");
					String address = scan.next();
					// Creates a socket at the given IP address on port 1717
					Socket sock = new Socket(address, 1717);
					sock.setReuseAddress(true);
                    
                    //input stream that will be uploaded to HT
					
					InputStream iStream = sock.getInputStream();
					ObjectInputStream oIStream = new ObjectInputStream(iStream);
					Hashtable <String, Integer> otherTable = (Hashtable <String, Integer>) oIStream.readObject();
					
					ObjectOutputStream oOut = new ObjectOutputStream(sock.getOutputStream());
					oOut.writeObject(hash);
					
					// Data to read and write data from or to files
					DataOutputStream dOut = new DataOutputStream(new BufferedOutputStream(sock.getOutputStream())); 
					DataInputStream dIn = new DataInputStream(new BufferedInputStream(sock.getInputStream()));

					Set<String> keys1 = hash.keySet();
					for(String key: keys1) {
						if(!otherTable.containsKey(key)){ // If the other client is missing files from this client's table	
							sendFile(dOut, key);
						}
						else if (otherTable.containsKey(key) && otherTable.get(key) != hash.get(key)){// If they have the same file name but different update times
							if (hash.get(key) > otherTable.get(key)){// If the file on this client is more recent, send the file
								sendFile(dOut, key);
							} 
						}
					}
					System.out.println();
					sock.close();
					System.out.println("File Synced");
					break;
				default:
					System.out.println("Thank you. Have a good day");
					break;//break the cases 
			}
		}
		scan.close();//Closes the scanner
	}
	
	/**
	 * Menu for the user to pick an action
	 */
	public static void displayMainMenu() {
		System.out.println("1.) Scan network");//looks for IP address
		System.out.println("2.) Wait for connections");//server
		System.out.println("3.) Connect to client");//client
		System.out.println("4.) Exit");
	}
	
	/**
	 * Looks for IP address of other computer on the same network
	 * @throws ConnectException
	 */
	public static void getNetworkIPs() throws ConnectException{
	    final byte[] ip;
	    
	    try {
	    	// Stores IP address of this computer in the array
	        ip = InetAddress.getLocalHost().getAddress();
		} 
		catch (Exception e) {
	    	e.printStackTrace();
	        return;
	    }
	    
	    // Checks each IP address, replacing fourth set of numbers 
	    // in this computer's IP address with values from 1 - 255
	    for(int i = 1; i < 255; i++) {
	        final int j = i;
	        new Thread(new Runnable() {
	            public void run() {
	                try {
	                	// Checks if this IP address is reachable
	                    ip[3] = (byte) j;
	                    InetAddress address = InetAddress.getByAddress(ip);
	                    String output = address.toString().substring(1);
	                    
	                    // Prints out IP address only if it is reachable
	                    if (address.isReachable(1717)) {
	                        System.out.println(output + " is on the network");
	                    }
					} 
					catch (Exception e) {
						e.printStackTrace();
	                }
	            }
	        }).start(); 
	    }
	}
	
	/**
	 * Creates a Hashtable of key, value pairs given a file directory String.
	 * @param fileDirectory the directory of the files to be made into a hash table
	 * @param hash Hashtable to be created/modified
	 */
	public static void createHashtable(String fileDirectory, Hashtable <String, Integer> hash){
		File directory = new File(fileDirectory);
		File[] files = directory.listFiles();
		
		// Adds file names and their corresponding update times into the hash table
		for (int i = 0; i < files.length; i++){
			String fileName = files[i].getName();//variable for the file name
			
			// Gets files in this project folder, excludes source files and compiled files
			if(!fileName.startsWith(".") && !fileName.equals("src") && !fileName.equals("bin")) {
				hash.put(files[i].getName(), (int) files[i].lastModified());
			}
		}
		System.out.println(hash + "\n");// Displays Hashtable of key and value pairs
	}
	
	/**
	 * Send file to the other computer
	 * @param dOut output stream used to write the file
	 * @param key name of File to be written
	 * @throws IOException 
	 */
	public static void sendFile(DataOutputStream dOut, String key) throws IOException{
		// Writes file name to stream
		System.out.println(key);
		dOut.writeUTF(key);
		dOut.flush();
		
		// Writes file size to stream
		File file = new File(key);
		long length = file.length();
		dOut.writeUTF(Long.toString(length));
		dOut.flush();
		
		FileInputStream fin = new FileInputStream(file);
		
		// Writes data to file
		int count;
		byte[] buffer = new byte[(int) length];
		// read method returns number of bytes read, used to know how many bytes to write
		while ((count = fin.read(buffer)) != -1){
			dOut.write(buffer, 0, count);
			dOut.flush();
		}
		System.out.println("File Sent");
	}
	
	/**
	 * Receive a file from another client
	 * @param in input stream that reads the file
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	public static void receiveFile(DataInputStream in) throws NumberFormatException, IOException{
		// Obtains the file's name and size from the other client
		String fileName = in.readUTF();
		System.out.print("file name: ");
		System.out.println(fileName);
		long size = Long.parseLong(in.readUTF());
		System.out.println(size + " size1\n");
		
		// Byte array to store file's content as bytes
		byte[] bytes = new byte[1024];
		
		// File output stream to write the bytes to the specified file
		FileOutputStream fos = new FileOutputStream(fileName);
		int n;
		
		// While there is still content to write to the file, write data to the file
		while (size > 0 && (n = in.read(bytes, 0, (int)Math.min(bytes.length, size))) != -1)
		{
			fos.write(bytes, 0, n);
			size -= n;
		}
		fos.close();
	}
}