import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.*;

public class Client {
	private Socket clientSocket;  
    private BufferedReader inFromServer;
    private BufferedReader inFromUser;
    private DataOutputStream outToServer;
    private boolean flag = true;
    private int timeout;
    private Timer timer;
	
	public static void main(String[] args) throws Exception {

		// use '127.0.0.1' as default server IP address
		String serverIPAddress = "127.0.0.1";
		if (args.length >= 1)
		    serverIPAddress = args[0];
		// get server port
		int serverPort = 6789; 
		//change above port number if required
		if (args.length >= 2)
		    serverPort = Integer.parseInt(args[1]);
		
		new Client().startUp(serverIPAddress, serverPort);
	}
	
	public void startUp(String iPAddress, int port) {
		String serverIPAddress;		
		serverIPAddress = iPAddress;
		int serverPort = port;		
		
		try {
			// create socket which connects to server
			clientSocket = new Socket(serverIPAddress, serverPort);
			// create read stream and receive from server
			inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			// create read stream and receive from client
			inFromUser = new BufferedReader(new InputStreamReader(System.in));
			
			// send username and password to server
			System.out.println("Username:");
			// get input from keyboard
			String userName;
			userName = inFromUser.readLine();
			System.out.println("Password:");
			String password;
			password = inFromUser.readLine();
			// write to server
			outToServer = new DataOutputStream(clientSocket.getOutputStream());
			// send userName and password
			outToServer.writeBytes(userName + '\n');
			outToServer.writeBytes(password + '\n');
		    // get reply from server about if username and password are correct
			String welcomeMess;
			welcomeMess = inFromServer.readLine();
			System.out.println(welcomeMess);
			// try again when password is wrong (two chance)
			while (welcomeMess.equals("Invalid Password. Please try again")) {
				System.out.println("Password:");
				password = inFromUser.readLine();
				outToServer.writeBytes(password + '\n');
				welcomeMess = inFromServer.readLine();
				System.out.println(welcomeMess);
			}
			
			if (welcomeMess.equals("Invalid Password. Your account has been blocked. Please try again later")) {
				clientSocket.close();
				System.exit(0);
			}
			
			if (welcomeMess.equals("Your account is blocked due to multiple login failures. Please try again later")) {
				clientSocket.close();
				System.exit(0);
			}
			
			// set a timer
			timeout = Integer.parseInt(inFromServer.readLine());
			
            
			// create a clientThread to receive message from server
			new Thread(new ClientThread()).start();
			
			// receive message from client
			String clientMessage = null;			

			
			/* try to complete timer function, but it does not work */
/*			while (flag) {
				if (!flag) {
					break;
				}
				while ((clientMessage = inFromUser.readLine()) == null) {
					Timer timer = new Timer();
		            timer.schedule(new RemindTask(), timeout*60*1000);
		            if (inFromUser.readLine() != null ){
		            	timer.cancel();
		            	outToServer.writeBytes(clientMessage+"\n"); 
		            }
				}
			}						
*/			
			while (flag && (clientMessage = inFromUser.readLine())!=null) {  
				if (!flag) break;
	            outToServer.writeBytes(clientMessage+"\n");  
	        } 
		} catch (UnknownHostException e) {  
            e.printStackTrace();  
        } catch (IOException e) {  
            e.printStackTrace();  
        }
	}
	
	// make function to receive message from server and write the message on client's window
	private  void receive() {  
        try {  
            String serverMessage = inFromServer.readLine();           
            if (serverMessage.equalsIgnoreCase("logout")) { 
//            	System.out.println(serverMessage);
            	clientSocket.close();
            	flag = false;
            }  
            else {
            	System.out.println(serverMessage);                        
            }
        } 
        catch (IOException e) {  
            e.printStackTrace();  
        }  
    }  
	
	// create a clientThread to receive message from server
	public class ClientThread implements Runnable {
		public void run() {  
            while (true) {  
                if (!flag) {
                	System.exit(0);
                	break;  
                }
                receive();  
            }  
        }  
	}
	
	// a method to use timer, but it does not work
	public class RemindTask extends TimerTask {
        public void run() {
            try {
				outToServer.writeBytes("logout"+ "\n");
				timer.cancel(); //Terminate the timer thread
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}           
        }
    }
	
	
	
	
	
	
	
	
	
	
}
