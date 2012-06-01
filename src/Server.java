
import java.net.ServerSocket;

/**
 * FTP Server, loops endlessly waiting for client to connect with.
 * Starts the  FTP Protocol Interrupt Server as soon as a client connects! 
 * @author yoga1290
 */
public class Server
{
    /**
     * Starts the  FTP Protocol Interrupt Server as soon as a client connects! 
     */
    public Server()
    {
        try{
            ServerSocket server=new ServerSocket(1290);
            while(true)
                new ServerPI(server.accept()).start();
            
        }catch(Exception e){System.err.println("Main FTP server (not PI or DTP):"+e);e.printStackTrace();}
    }
}