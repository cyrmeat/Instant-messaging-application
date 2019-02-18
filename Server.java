import java.net.*;
import java.util.*;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.File;



public class Server {
	// save users'name and password into a HashMap
	private Map<String, String> userInfo = getCredentials();
	// create a block list to store those who failed to login three times
	private Map<String, Long> blockList = new HashMap<String, Long>();
	// create a hash map to save client's name and how many time did they try
	private Map<String, Integer> tryCount =  new HashMap<String, Integer>();	
	// create a list to save online users
	private List<Socket> clients = null; 
	// create a hash map to store user name and their socket
	private Map<String, Socket> clientAndSocket = null;	
	// create a list to store login and logout time stamp	
	private Map<String, List<Long>> loginLogoutMap = new HashMap<String, List<Long>>();
	// save offline message
	private Map<String, List<String>> offlineMessage = new HashMap<String,List<String>>();
	// create a hash map to store those who block other clients
	private Map<String, List<String>> blockFriend = new HashMap<String, List<String>>();

	
	//constructor and while-accept loop all in one
	public Server(int serverPort, int blockDuration, int timeout) throws IOException {
		listen(serverPort, blockDuration, timeout);
	}

	
	// main routine
	public static void main(String args[]) throws Exception {
		int serverPort = 6789; 
		int timeout = 2;
		/* change above port number this if required */			
		if (args.length >= 1) {
			timeout = Integer.parseInt(args[0]);
		}
		// get block duration(minute) and timeout(minute）
		int blockDuration = 5;
		if (args.length >=3 ) {
			blockDuration = Integer.parseInt(args[1]);
			serverPort = Integer.parseInt(args[2]);
			}
		
		// create a Server object, which will automatically begin to accept connection
		new Server(serverPort, blockDuration, timeout);		
	}
	
	
	// way to accept connection
	private void listen(int serverPort, int blockDuration, int timeout) throws IOException {
		// create server socket
		ServerSocket welcomeSocket = new ServerSocket(serverPort);
		System.out.println("waiting for connection...");
		clients = new ArrayList<Socket>();
	    clientAndSocket = new HashMap<String, Socket>();
	    for (String tryCountclient : userInfo.keySet()) {
			tryCount.put(tryCountclient, 0);
		}
		while (true) {
			// accept connection from connection queue
		    Socket connectionSocket = welcomeSocket.accept();
		    System.out.println("connection from " + connectionSocket);	
		    
		    // create a new thread for this connection, and then forget about it
		    ServerThread serverThread = new ServerThread(connectionSocket, blockDuration, timeout);
		    Thread t = new Thread(serverThread);
            t.start();
		}
	}
	
	
	// create thread class to have multiple threads
	public class ServerThread implements Runnable {
		private Socket connectionSocket = null;  
        private BufferedReader inFromClient;  
        private DataOutputStream outToClient;
        private String clientName;
        private int blockDuration;
        private int timeout;
        private boolean flag = true;
        private List<Long> loginLogout = new ArrayList<Long>();
        
        
        // constructor of ServerThread
        public ServerThread(Socket socket, int blockDuration1, int timeout1) throws IOException {  
        	connectionSocket = socket;
        	blockDuration = blockDuration1;
        	timeout = timeout1;
        }
        
        
        /* below are several method that will be used by run() */
        
        // check if the password is correct
        private boolean correctPassword(String clientName, String clientPassword, Map<String, String> userInfo) {
    		if (userInfo.containsKey(clientName)) {
    			String truePassword = userInfo.get(clientName);
    			if (clientPassword.equals(truePassword)) {
    				return true;}
    			else {
    				return false;}
    		}
    	    return false;
    	}
        
        // a method to send message to all the clients online
        private void broadcast(String msg) throws IOException {
        	for (String key: clientAndSocket.keySet()){
        		if (key != clientName) {
        			if (!ifInBlockFriend(key)){
        				DataOutputStream outToClient = new DataOutputStream(clientAndSocket.get(key).getOutputStream());
                		outToClient.writeBytes(msg + "\n");
        			}
        			else {
        				outToClient.writeBytes("Your message could not be delivered to some recipients" + "\n");
        			}
        		}
        	}
        }
        
        // a method to send notification of login logout
        private void broadcast1(String msg) throws IOException {
        	for (String key: clientAndSocket.keySet()){
        		if (key != clientName) {      			
					DataOutputStream outToClient = new DataOutputStream(clientAndSocket.get(key).getOutputStream());
	        		outToClient.writeBytes(msg + "\n");
        		}	
        	}
        }
        
        // a method to reply to client that whoelse are online
        private void whoelse() throws IOException {
        	for (String key : clientAndSocket.keySet()) {
        		if (! key.equals(clientName)) {
        			outToClient.writeBytes(key+"\n");
        		}
        	}
        }
        
        // send to client wholelse was online since a specific time span
        private void whoelseSince(int time) throws IOException {
        	// print all the online user
        	for (String key : clientAndSocket.keySet()) {
        		if (! key.equals(clientName)) {
        			outToClient.writeBytes(key+"\n");
        		}
        	}
        	// get current time
        	long currentTime = System.currentTimeMillis();
        	if (! (loginLogoutMap.size() == 0)) {
        		for (String key : loginLogoutMap.keySet() ){
        			if (loginLogoutMap.get(key).size() == 2 && !key.equals(clientName)){
        				long logoutTime = loginLogoutMap.get(key).get(1);
        				if ((currentTime - logoutTime) < (time*60*1000)) {
        					outToClient.writeBytes(key+"\n");
        				}
        			}
        		}
        	}
        	
        }
        
        // send chat message from one client to another
        private void chatMessage(String name, String content) throws IOException {
        	if (ifInBlockFriend(name)){
        		outToClient.writeBytes("Your message could not be delivered as the recipient has blocked you" + "\n");
        	}
        	else {
	        	int count = 0;
	        	for (String validuser : userInfo.keySet()){       		
	        		if (validuser.equals(name)) {
	        			for (String otherSideName : clientAndSocket.keySet()) {
	                		if (otherSideName.equalsIgnoreCase(name)) {
	                			Socket otherSideSocket = clientAndSocket.get(otherSideName);
	                			DataOutputStream outToClient = new DataOutputStream(otherSideSocket.getOutputStream());
	                			outToClient.writeBytes(clientName + ": " + content + "\n");
	                		}
	                		// save message for offline client
	                		else {
	                			String message = clientName + ": " + content;
	                			System.out.println(message);
	                			if (offlineMessage.isEmpty()) {
	                				System.out.println(name);
	            					List<String> saveMessage = new ArrayList<String>();
	            					saveMessage.add(message);
	            					offlineMessage.put(name, saveMessage);
	                			}
	                			else {
	        	        			for (String key : offlineMessage.keySet()) {
	        	        				if (key.equals(name)){
	        	        					System.out.println(key);
	        	        					List<String> storedMessage = offlineMessage.get(key);
	        	        					storedMessage.add(message);
	        	        					offlineMessage.put(name, storedMessage);
	        	        				} 
	        	        				else {
	        	        					System.out.println(name);
	        	        					List<String> saveMessage = new ArrayList<String>();
	        	        					saveMessage.add(message);
	        	        					offlineMessage.put(name, saveMessage);
	        	        				}        				
	        	        			}
	                			}
	                		}
	                	}
	        		}
	        		// check if the one who client want to send message is a valid user
	        		else {
	        			if (count < userInfo.size()){
	        				count ++;
	        			}
	        			if (count == userInfo.size()){
	        				outToClient.writeBytes("Error. Invalid user" + "\n");
	        			}
	        		}
	        	}
        	}
        }
        
        // check if name is in the userInfo
        private boolean checkValid(String name) {
    		for (String client : userInfo.keySet()){
        		if (client.equals(name)){
        			return true;
        		}
        	}
        	return false;
        }
        
        // block one client(name) from another client(clientName)
        private void block(String name) throws IOException {
        	if (checkValid(name)) {
        		if (name.equals(clientName)) {
        			outToClient.writeBytes("Error. Cannot block self" + "\n");
        		}
        		else {
        			if (blockFriend.isEmpty()) {
                		List<String> blockName = new ArrayList<String>();
                		blockName.add(name);
                		blockFriend.put(clientName, blockName);
                		outToClient.writeBytes(name+" is blocked"+"\n");
                	}
                	else {
                		for (String whoDidBlock : blockFriend.keySet()){
                			if (whoDidBlock.equals(clientName)){
                				List<String> storedList = blockFriend.get(whoDidBlock);
                				storedList.add(name);
                				blockFriend.put(whoDidBlock, storedList);
                				outToClient.writeBytes(name+" is blocked"+"\n");
                			}
                			else {
                				List<String> blockName = new ArrayList<String>();
                        		blockName.add(name);
                        		blockFriend.put(clientName, blockName);
                        		outToClient.writeBytes(name+" is blocked"+"\n");
                			}
                		}
                	}
        		}        		
        	}
        	else {
        		outToClient.writeBytes("Error. Invalid user"+"\n");
        	}
        	
        }
        
        // unblock 
        private void unblock(String name) {       	
        	try {
        		if (checkValid(name)){
        			if (blockFriend.isEmpty()){
            			outToClient.writeBytes("Error. " + name + " was not blocked" + "\n");
            		}
            		else {
            			for (String whoDidBlock : blockFriend.keySet()){
                			if (whoDidBlock.equals(clientName)){
                				List<String> storedList = blockFriend.get(whoDidBlock);
                				List<String> newStoredList = new ArrayList<String>();
                				newStoredList.addAll(storedList);
                				for (String blockedName : storedList) {
                					if (blockedName.equals(name)){
                						outToClient.writeBytes(name+" is unblocked"+"\n");
                						newStoredList.remove(name);
                						blockFriend.put(whoDidBlock, newStoredList);
                					}
                					else {
                						outToClient.writeBytes("Error. " + name + " was not blocked" + "\n");
                					}
                				}
                			}
                			else{
                				outToClient.writeBytes("Error. " + name + " was not blocked" + "\n");
                			}
            			}
            		}
        		}
        		else{
        			outToClient.writeBytes("Error. Invalid user"+"\n");
        		}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}        	
        }
        
        // check if client(clientName) in this thread is blocked by another client(name)
        private boolean ifInBlockFriend(String name) {
        	for (String whoBlock : blockFriend.keySet()) {
        		if (whoBlock.equals(name)){
        			List<String> blocked = blockFriend.get(name);
                	for (String blockedName : blocked) {
                		if (blockedName.equals(clientName)){
                			return true;
                		}
                		else {
                			return false;
                		}
                	}
        		}
        	}
        	return false;
        }
        
        // receive messages from client and classify different command
        private void receive() throws IOException {  
        	String message = null;

		    while ((message = inFromClient.readLine()) != null) {
//		    	System.out.println(message);
		    	String firstWord = message.split(" ")[0];
		    	
		    	if (firstWord.equalsIgnoreCase("whoelse")) {
			    	whoelse();
		    	}
		    	if (firstWord.equalsIgnoreCase("logout")) {
		    		long logoutTime = System.currentTimeMillis();
		    		loginLogout.add(logoutTime);
		    		loginLogoutMap.put(clientName, loginLogout);
                    stop();
                    broadcast1(clientName+ " logged out");
                    break;  
                }
		    	if (firstWord.equalsIgnoreCase("whoelsesince")) {
		    		String msgArray[] = message.split(" ", 2);
		    		String sendMessage = msgArray[1];
		    		int time = Integer.parseInt(sendMessage);
		    		whoelseSince(time);
			    }
		    	if (firstWord.equalsIgnoreCase("message")) {
		    		String msgArray[] = message.split(" ", 3);
			    	chatMessage(msgArray[1], msgArray[2]);
			    }
		    	if (firstWord.equalsIgnoreCase("block")) {
		    		String msgArray[] = message.split(" ", 2);
			    	block(msgArray[1]);
			    }
		    	if (firstWord.equalsIgnoreCase("unblock")) {
		    		String msgArray[] = message.split(" ", 2);
			    	unblock(msgArray[1]);
			    }
		    	if(firstWord.equalsIgnoreCase("broadcast")) {
		    		String msgArray[] = message.split(" ", 2);
		    		String sendMessage = msgArray[1];
		    		System.out.println(clientName);
			    	broadcast(clientName + ": " + sendMessage);
		    	}
		    	// if the client uses an invalid command
		    	if (!firstWord.equalsIgnoreCase("whoelse") && !firstWord.equalsIgnoreCase("logout") &&
		    			!firstWord.equalsIgnoreCase("whoelsesince") && !firstWord.equalsIgnoreCase("message") &&
		    			!firstWord.equalsIgnoreCase("broadcast") && !firstWord.equalsIgnoreCase("block") &&
		    			! firstWord.equalsIgnoreCase("unblock")) {
		    		outToClient.writeBytes("Error. Invalid command"+"\n");		    	
		    	}
		    }
		    
		    
        }
         
        // a method to disconnect from client and logout the client and stop this thread
        private void stop() throws IOException {
        	
            outToClient.writeBytes("logout" + "\n");
            clientAndSocket.remove(clientName, connectionSocket);
        	clients.remove(connectionSocket);
            flag = false;  
            
        }
        
        private void sendOfflineMessage(String name) throws IOException {
        	for (String key: offlineMessage.keySet()){
        		if (key.equalsIgnoreCase(name)){
        			List<String> saveMessage = offlineMessage.get(key);
        			for (String message : saveMessage){
        				outToClient.writeBytes(message + "\n");
        			}
        			offlineMessage.remove(name, saveMessage);
        		}
        	}
        }
        
        // a method to implement login function
        private void login() throws IOException {
        	String clientPassword;
		    int count = tryCount.get(clientName);
		    while (count< 2){
		    	clientPassword = inFromClient.readLine();
		    	boolean correctPassword = correctPassword(clientName,clientPassword,userInfo);
		    	if (correctPassword == true) {
		    		outToClient.writeBytes("Welcome to the greatest messaging application ever!"+"\n");
		    		outToClient.writeBytes(timeout + "\n");
		    		clients.add(connectionSocket);
		    		clientAndSocket.put(clientName, connectionSocket);
		    		long loginTime = System.currentTimeMillis();		    		
		    		loginLogout.add(loginTime);
		    		loginLogoutMap.put(clientName, loginLogout);		    		
		    		broadcast1(clientName  + " logged in");
		    		// send offline message
		    		sendOfflineMessage(clientName);
		    		count = count + 3;
		    		tryCount.put(clientName, 0);
		    	}
		    	else {
		    		outToClient.writeBytes("Invalid Password. Please try again" + "\n");
		    		count ++;
		    		tryCount.put(clientName, count);
		    		}		    	
		    }
		    if (count == 2) {
    			outToClient.writeBytes("Invalid Password. Your account has been blocked. Please try again later"+"\n");
    			flag = false;
    			long blockTime = System.currentTimeMillis();
    			blockList.put(clientName, blockTime);
		    }
        }
        
		public void run(){ 
			try {
				// create read stream to get input from client
	        	inFromClient = new BufferedReader(new InputStreamReader(connectionSocket.getInputStream()));
	        	// create a DataOutputStream for writing data to the client
	        	outToClient = new DataOutputStream(connectionSocket.getOutputStream());
	        	// get clientName
			    clientName = inFromClient.readLine();
			    
			    
			    // if clientName is in the blockList, then refuse the request
			    if (blockList.containsKey(clientName)) {
			    	long currentTime = System.currentTimeMillis();
//			    	System.out.println(currentTime);
			    	long lastBlockTime = blockList.get(clientName);
			    	if (currentTime - lastBlockTime <= blockDuration*60*1000){
			    		outToClient.writeBytes("Your account is blocked due to multiple login failures. Please try again later"+"\n");
			    		flag = false;
			    	}
			    
			    	if (currentTime - lastBlockTime > blockDuration*60*1000){
				    	blockList.remove(clientName, blockList.get(clientName));
				    	login();
				    }			    
			    }
			    else {
			    	login();
			    }
			    
			    // create a loop to receive message from client
			    while(true) {
			    	if (!flag) break;
			    	receive();
			    }			    				    			    
			    
			}
			catch (IOException e) {  
                e.printStackTrace();  
            } finally {  
                try {  
                    if (connectionSocket != null)   
                    	connectionSocket.close();
                    
                } catch (IOException e) {  
                    e.printStackTrace();  
                }  
            }  
		}
		
		
	}
	
	
	// to read file and get data of username and password
	private Map<String, String> getCredentials() {
		Map<String, String> userInfo = new HashMap<String, String>(); 
		try {
	        BufferedReader br = new BufferedReader(new FileReader("credentials.txt"));                                      
	        String lineTxt = null; 
	        while ((lineTxt = br.readLine()) != null) {
	            String[] namePass = lineTxt.split(" ");
	            for (int i = 0 ; i < namePass.length;) {               
	                    userInfo.put(namePass[i], namePass[i+1]);
	                    //System.out.println(namePass[i] + " " + namePass[i+1]);
	                    i = i + 2;
	                }
	            }
	        br.close();
		}
	    catch (Exception e) {
	        System.err.println("read errors :" + e);
	    }
		return userInfo;
	}
	
	
	
	
	
	
	
}
