package cs470;


import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Scanner;


public class HaiyanChat {
	public static ArrayList<Conn> connections;
	public static void main(String[] args) {
		
		//get command line argument for server port number
		System.out.println(args[0]);
		int server_port = Integer.parseInt(args[0]);
		
		connections = new ArrayList<Conn>();
		int client_num=connections.size();
		 InetAddress ip=null;
		//start server
		ChatServer cs = new ChatServer(server_port);
		Thread ts = new Thread(cs);
		ts.start();
		ChatClient cc = null;
		//take in user input from console
		Scanner scanner = new Scanner(System.in);
		while(scanner.hasNext()){
			String input[] = scanner.nextLine().split(" ",3);
            
			// NIVE: Added Help, myip,myport and exit functions here
            //********************************************************
			 if (input[0].equals("help"))
			    {
			    	
	    		    System.out.println("MYIP : Displays the ip address of your computer.");
	    		    System.out.println("MYPORT : Displays the port number of your computer.");
	    		    System.out.println("CONNECT <destination> <port no> : This command establishes a new TCP connection to the specified<destination> at the specified < port no>. The <destination> is the IP address of the computer.");
	    		    System.out.println("LIST : Display a numbered list of all the connections this process is part of. This numbered list will include connections initiated by this process and connections initiated by other processes.");
	    		    System.out.println( "TERMINATE <connection id> : This command will terminate the connection listed under the specified to display all connections.");
	    		    System.out.println("SEND <connection id> <message> : This will send the message to the host on the connection that is designated by the number n when command ìlistî is used. ");
	    		    System.out.println("EXIT:t Close all connections and terminate this process.");
			    }
			 
			 if (input[0].equals("myip"))
			    {
				 try {
						ip = InetAddress.getLocalHost();
					} catch (UnknownHostException e1) {
						e1.printStackTrace();
					}
			    
			    	System.out.println("Your current IP address : " + ip);
			    }
			    
			 if (input[0].equals("myport"))
			    {
			    	
			    
			    	 System.out.println("Your current Hostname : " + server_port);
			    }
			 
			 if(input[0].equals("exit"))
             {

				 connections.removeAll(connections);
    
                  System.exit(0);
                                                                       
             }
			
		if(input[0].equals("connect")){
				//bring up client thread and connect to server
				Conn cn = new Conn(input[1],Integer.parseInt(input[2]));
				System.out.println("Connecting to " + input[1] + " port " + input[2] );

				client_num++;
				cn.setId(client_num);
				
				connections.add(cn);
				cc = new ChatClient(cn);
				
				Thread tc = new Thread(cc);
				tc.start();
			}
			if(input[0].equals("list")){
				for(int i=0;i<connections.size();i++){
					System.out.println(connections.get(i).getId() + " " + connections.get(i).getHost() 
							+ " " + connections.get(i).getPort());
				}
			}
			if(input[0].equals("send")){
				for(int i=0;i<connections.size();i++){
					if(connections.get(i).getId() == Integer.parseInt(input[1])){
						connections.get(i).setMessage(input[2]);
						Socket s = connections.get(i).getSocket();
						if(cc == null){
							cc = connections.get(i).getClient();
							Thread tc = new Thread(cc);
							tc.start();
						}
						cc.sendMessage(s, connections.get(i).getMessage());
					}
				}
			}
		}		
	}
	
}
class ChatServer implements Runnable{
	private int port;
	
	public ChatServer(int p){
		port = p;
	}
	public void run(){
		try{
			ServerSocket servSocket = new ServerSocket(port);
			
			System.out.println("Waiting for connection...");
			
			while(true){
				Socket socket = servSocket.accept();
				System.out.println("Accepted connection from " + socket.getLocalAddress() + " " + socket.getLocalPort());
				
				PrintWriter out = new PrintWriter(socket.getOutputStream());
				out.println("Hello. You got connected successfully!");
				ChatClient chat_s = new ChatClient(socket);
				
				//add to data structure
				String localAddr = socket.getLocalAddress().toString();
				String addr[] = localAddr.split("/"); 
			    Conn c = new Conn(addr[1], socket.getLocalPort());
			    c.setSocket(socket);
			    c.setClient(chat_s);
			    int num = HaiyanChat.connections.size() + 1;
			    //System.out.println("Client Num " + num + " : " + c.getHost() + " " + c.getPort());
			    c.setId(num);
			    HaiyanChat.connections.add(c);
			    
				System.out.println("Created new server client for data transfer");
				Thread ts = new Thread(chat_s);
				ts.start();
			}
		}catch(IOException ex){
			System.err.println("Error: " + ex);
			
		}
	}
}
class ChatClient implements Runnable{
	
	private Socket socket;
	//private String message;
	//private Conn c;
	public ChatClient(Socket s){
		socket = s;
	}
	public ChatClient(Conn c){
		
		try{
			//create new socket to connect to server
			socket = new Socket(c.getHost(), c.getPort());
			c.setSocket(socket);
		}catch(IOException ex){
			System.err.println("Error: " + ex);
			
		}
	}
	public void run(){
		try{
			Scanner in = new Scanner(socket.getInputStream());
			while (true){
				//incoming messages
				if (in.hasNext()){
					String input = in.nextLine();
					System.out.println("Other Said: " + input);
				}
			}
			
		}catch(IOException ex){
			System.err.println("Error: " + ex);
			
		}
	}
	public void sendMessage(Socket s, String msg){
		try{
			PrintWriter out = new PrintWriter(s.getOutputStream());
			out.println(msg);
			out.flush();
			System.out.println("You said: " + msg);
			
		}catch(IOException ex){
			System.err.println("Error: " + ex);
			
		}
	}
}

class Conn{
	private String host;
	private int port;
	private Socket socket;
	private int id;
	private String message="";
	private ChatClient cc;
	
	public Conn(String h, int p){
		host = h;
		port = p;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public void setId(int id) {
		this.id = id;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public void setClient(ChatClient cc) {
		this.cc = cc;
	}
	
	public Socket getSocket() {
		return socket;
	}
	public void setSocket(Socket socket) {
		this.socket = socket;
	}
	public int getPort() {
		return port;
	}
	public int getId() {
		return id;
	}
	public String getMessage() {
		return message;
	}
	public ChatClient getClient() {
		return cc;
	}
	
}