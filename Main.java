package peerToPeer;

import java.net.*;
import java.io.*;
import java.net.Socket;

public class Main{
    
    static String directory;
    /**
     * main diver of the program. Menu for the user to choice between sending or
     * receiving a file. After the user sends or receives a file it will give the
     * user a choice to sync. If else statement to see if the orig file is changed
     * or not.
     */
    public static void main(String[] args) throws NumberFormatException, IOException {

        // BufferReader for user inputs
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Send or Receive File?\n0. Send\n1. Receive");
        int userInput = Integer.parseInt(br.readLine());

        Socket socket = new Socket("47.156.240.252", 5000);
        
        //after user answers goes to either send or recieve file
        if (userInput == 0) {
            sendFile();
        }
        else if(userInput == 1)
        {
            receiveFile();
        } else {
            System.out.println("Value Error.");
        }

        // need to know what file is the original and what file is the new
    }

    /**
     * Sends file to the other computer
     */
    public static void sendFile() throws IOException {
        // BufferReader for user inputs
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Client: Started\n");

        // Takes an input from user to which port number to connect to
        // System.out.print("Enter the port number:
        // ");////////////////////////////////////////////////////////////////////////////6006
        // int port = Integer.parseInt(br.readLine());

        System.out.println("Connecting to server....");

        // This is the IP address of my Server PC. Any user can replace this number with
        // their desired Server's address
        System.out.println("Server: Connected\n");

        // Takes the Directory as input from user of the folder to sync
        System.out.println("Enter the directory: ");
        directory = br.readLine();

        // Lists all the files in desired directory
        File[] files = new File(directory).listFiles();

        // Gets the output stream from Socket
        BufferedOutputStream bos = new BufferedOutputStream(socket.getOutputStream());
        DataOutputStream dos = new DataOutputStream(bos);

        // Sends the number of files in the folder as output stream, it will help
        // receive the files
        dos.writeInt(files.length);

        // Runs a loop to send all the files in the folder
        for (File file : files) {
            // Sends the length of each file before sending the actual file
            // so the receiver knows how much memory to allocate
            long length = file.length();
            dos.writeLong(length);

            // Sends the name of each file before sending the actual file
            String name = file.getName();
            dos.writeUTF(name);

            // In the following part, the file is converted and sent
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);

            int theByte = 0;
            while ((theByte = bis.read()) != -1)
                bos.write(theByte);
            System.out.println("Sent: " + name);
            // Closes buffered input stream
            bis.close();
        }

        // Closes DataOutputStream after all the files are sent
        dos.close();
        System.out.println("Files Sent!");
    }

    /**
     * Receive file from the other computer
     */
    private void receiveFileFromClient() throws IOException {
        try {
            //Listening port
            server = new ServerSocket(5000);
            System.out.println("Server started!! ");
            System.out.println(" ");
            System.out.println("Waiting for the Client to be connected ..");
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

        while(true)
        {
            try{

                //Accepts connection from client
                socket = server.accept();

            }
            catch(IOException e)
            {
                System.out.println("I/O error: " +e);
            }

            //Starts a new thread for each client
            new ClientHandler(socket).start();
        }
        System.out.println("Connected to client \nIP: " + socket.getInetAddress().toString() + "\nPort: "+ socket.getPort());

        //Declaring input streams for reading data sent from client over socket
        BufferedInputStream bis = new BufferedInputStream(socket.getInputStream());
        DataInputStream dis = new DataInputStream(bis);

        //Each peer sends the number of files in their directory
        //So the receiver knows how many times to run the loop for receiving files
        int filesCount = dis.readInt();

        //Declares an Array of files
        File[] files = new File[filesCount];

        //Receiving files from client
        for(int i = 0; i < filesCount; i++) {

            //Reads the files name and length first so it know how much memory to allocate
            long fileLength = dis.readLong();
            String fileName = dis.readUTF();

            System.out.println("Received file: " +fileName);

            //Creates a new empty file
            files[i] = new File(directory  + fileName);

            //Writes the info into files
            FileOutputStream fos = new FileOutputStream(files[i]);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            for(int j = 0; j < fileLength; j++) {
                bos.write(bis.read());
            }
            bos.close();

        }
        //Tells the user that receiving files has been complete
        System.out.println("File receiving complete from client having IP: " + socket.getInetAddress().toString()+ "\nPort: " + socket.getPort());
        //Closes the Input Stream and the socket
        dis.close();
    }

    /**Sync file 
     * if we have time
     */
    public void syncFile(){


    }
    public class ClientHandler extends Thread{
        protected Socket socket;
        //Directory of the folder where the files are stored
        public String directory = "/home/nathaniel/Desktop/Test";
    
    
        //Constructor
        public ClientHandler(Socket clientSocket)
        {
            this.socket=clientSocket;
            //this.globalArray=globalArray;
        }
    
        public void run() {
    
            //Receives and stores the files in the server
            receiveFileFromClient();
    
        }
    }
}


