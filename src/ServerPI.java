
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.InetAddress;
import java.net.Socket;
import java.util.TreeMap;

/**
 * FTP Server Protocol Interrupt
 * @author yoga1290
 * @see http://www.rhinosoft.com/kbarticle.asp?RefNo=1444?=rs 
 * @see http://www.rhinosoft.com/KnowledgeBase/kbarticle.asp?RefNo=1446&Prod=rs
 **/
public class ServerPI extends Thread
{
    private Socket client;
    private DataOutputStream out;
    private DataInputStream in;
    private String response="";
    
    private File currentDirectory= new File(".");
    
    private boolean access=false,isActiveDTP=false; //read/write access flag 
    private PassiveServerDTP passiveDTP=null;
    private ActiveServerDTP  activeDTP=null;
    
    private InetAddress DTPHost;
    private int         DTPPort;
    
    private TreeMap<String,String> user;
    /**
     * Constructor of the FTP Protocol Interrupt Server
     * @param client client to connect the FTP Protocol Interrupt Server with.
     */
    public ServerPI(Socket client)
    {
        this.client=client;
        
        user=new TreeMap<String, String>();
        user.put("ftp","pass");
        user.put("anonymous","chrome@example.com"); //default Google Chrome user/pass
        user.put("anonymous","cfnetwork@apple.com");//Safari's default
        user.put("user","pass");
    }
    /**
     * Send response to the client
     * @param txt message to be send , +"\r\n" at the end!
     */
    private void sendCMD(String txt)
    {
        try{
            out.writeBytes(txt+"\r\n");
            System.out.println("<<"+txt);
        }catch(Exception e){e.printStackTrace();}
    }
    /**
     * Updates the global variable "response" with the latest received message.
     * @throws Exception 
     */
    private void updateResponse() throws Exception
    {
        try{
            int ch;
            response="";
            while(  (ch=in.read())!=-1)
                if( (char)ch == '\n')
                    break;
                else
                    response+=""+(char)ch;
            if(response.length()>1     &&    response.charAt(response.length()-1)=='\r')
                    response=response.substring(0,response.length()-1);
            System.out.println(">>"+response);
        }catch(Exception e){e.printStackTrace();}
    }
    /**
     * When the thread starts, it sends the 220 Hello response code ,reads the coming message 
     * & enters a while loop , checks for a matching response/action , waits for the coming message & continue the loop
     */
    @Override
    public void run()
    {
        InetAddress inet=client.getInetAddress();
        try
        {
          in=new DataInputStream(client.getInputStream());
          out=new DataOutputStream(client.getOutputStream());
          
          
          sendCMD("220 Hello,I'm the Server & I'm alive!");
          updateResponse();
          
          
          while(!response.toUpperCase().equals("QUIT") && response.length()>0)
          {
                try{

                    if(response.split(" ")[0].toUpperCase().equals("USER"))
                    {
                        String username=response.split(" ")[1];
                        
                        //username is found as key in the TreeMap<User,Password>?
                        if(user.containsKey(username))
                        {
                            sendCMD("331 Restricted, Password?");
                            updateResponse();
                            //if the password matchs the given username
                            if(response.split(" ")[1].equals(
                                        user.get(username)
                                    ))
                            {
                                sendCMD("230 User logged in, proceed.");   
                                access=true;
                            }
                            else
                                sendCMD("500 Wrong password");
                        }
                        else if(response.split(" ")[1].toUpperCase().equals("ANONYMOUS"))
                            sendCMD("500 Account?");
                        else
                            sendCMD("500 User not found kaman");
                    }
                    
                    
//                    //PASS
//                    else if(response.split(" ")[0].toUpperCase().equals("PASS") )
//                    {
//                        sendCMD("250 User logged in, proceed.");
//                        access=true;
////                        sendCMD("332 NOP");
//                    }
                    
                    //TYPE [A|I]: "Most modern Windows FTP clients deal only with type "A" (ASCII) and type "I" (image/binary)." 
                    else if(response.split(" ")[0].toUpperCase().equals("TYPE") )
                    {
                        String type=response.split(" ")[1];
                        //TODO smth here?
                        sendCMD("230 OK");
                    }
                    //PORT
                    else if(response.split(" ")[0].toUpperCase().equals("PORT") && access)
                    {
                        // I didn't try the Active mode, since all of Google Chrome & Safari were using Passive mode only!
                        String tmp[]=response.split(" ")[1].split(",");
                        int port=Integer.parseInt(tmp[4])*256+Integer.parseInt(tmp[5]);
                        InetAddress host=InetAddress.getByAddress(new byte[]{
                                                                     (byte)Integer.parseInt(tmp[0])
                                                                    ,(byte)Integer.parseInt(tmp[1])
                                                                    ,(byte)Integer.parseInt(tmp[2])
                                                                    ,(byte)Integer.parseInt(tmp[3])
                                                                });
                        activeDTP=new ActiveServerDTP(host, port);
                        isActiveDTP=true;
                    }
                    
                    
                    //PWD
                    else if(response.split(" ")[0].toUpperCase().equals("PWD") && access)
                    {
                        sendCMD("257 "+currentDirectory.getCanonicalPath());
                    }
                    
                    //CWD
                    else if(response.split(" ")[0].toUpperCase().equals("CWD") && access)
                    {
                        if(new File(response.split(" ")[1]).isDirectory())
                        {
                            currentDirectory=new File(response.split(" ")[1]);
                            sendCMD("230 OK");
                        }
                        else
                            sendCMD("550 Failed to change directory.");
                    }
                    
                    //RETR
                    else if(response.split(" ")[0].toUpperCase().equals("RETR") && access)
                    {
                        sendCMD("150 about to open data connection.");
                        if(isActiveDTP)
                            activeDTP.sendFile(new File(response.split(" ")[1]));
                        else
                            passiveDTP.sendFile(new File(response.split(" ")[1]));
                        sendCMD("226 Transfer complete");
                    }
                    //STOR
                    else if(response.split(" ")[0].toUpperCase().equals("STOR") && access)
                    {
                        sendCMD("150 about to open data connection.");
                        if(!isActiveDTP)
                                activeDTP.recieveFile(new File(response.split(" ")[1]));
                        else
                                passiveDTP.recieveFile(new File(response.split(" ")[1]));
                                
                        sendCMD("226 Transfer complete");
                    }
                    //LIST
                    else if(response.split(" ")[0].toUpperCase().equals("LIST") && access)
                    {
                        if(passiveDTP!=null)
                        {
                            sendCMD("150 Opening ASCII mode data connection for file list");
                            if(isActiveDTP)
                                activeDTP.list(currentDirectory);
                            else
                                 passiveDTP.list(currentDirectory);
                            sendCMD("226 Transfer complete");
                        }
                        //TODO: send error or smth instead of OK!
                        else
                            sendCMD("226 Done Transfer");
//                        passiveDTP.disconnect();
                    }
                    
                    //PASV
                    else if(response.split(" ")[0].toUpperCase().equals("PASV") && access)
                    {
                        DTPHost=InetAddress.getLocalHost();
                        DTPPort=(int)(Math.random()*1000)+10000;
                            //DTPHost.getHostAddress().split(".") didn't work!...so,I did it manually,msa!!
                            String tmp=DTPHost.getHostAddress();
                            int dot=tmp.indexOf(".");
                            String IP1=tmp.substring(0, dot);
                            String IP2=tmp.substring(dot+1,tmp.indexOf(".",dot+1));
                            dot=tmp.indexOf(".", dot+1);
                            String IP3=tmp.substring(dot+1,tmp.indexOf(".",dot+1));
                            dot=tmp.indexOf(".", dot+1);
                            String IP4=tmp.substring(dot+1,tmp.length());
                           
                        if(passiveDTP!=null)
                        {
                            passiveDTP.disconnect();
//                            passiveDTP.RUN=false;
//                            passiveDTP.join();
                        }
                        
                        passiveDTP=new PassiveServerDTP(DTPHost, DTPPort);
                        passiveDTP.start();
                        
                        sendCMD("227 Entering Passive Mode ("+IP1+","+IP2+","+IP3+","+IP4+","+DTPPort/256+","+DTPPort%256+").");
                    }
                    
                    //SIZE
                    else if(response.split(" ")[0].toUpperCase().equals("SIZE") && access)
                        if(new File(response.split(" ")[1]).isDirectory())
                            sendCMD("550 not a regular file");
                        else
                            sendCMD("213 "+new File(response.split(" ")[1]).length());
                    
                    
                    //SYST ///reply from a real server out there!...yet fake!
                    else if(response.split(" ")[0].toUpperCase().equals("SYST") && access)
                        sendCMD("215 UNIX Type: L8");
                    else
                        sendCMD("502 Command not implemented!..I didn't implement this!");
                    
                    
                    
                    updateResponse();
                }catch(Exception e){System.err.println("Exception from ServerPI 1: "+e);e.printStackTrace();}
          }
          
          sendCMD("221 Goodbye!");
        }catch(Exception e){System.err.println("Exception from ServerPI 2: "+e);}
        
        
    }
    
}