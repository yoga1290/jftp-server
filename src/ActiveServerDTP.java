/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * FTP Active Data Transfer Protocol Server
 * @author yoga1290
 */
public class ActiveServerDTP extends Thread
{
    int port=21;
    boolean RUN=true,isConnected=false;
    private File cuurentFile;
    InetAddress host=null;
    
    private Socket client=null;//current client
    private DataOutputStream out;
    private DataInputStream in;
    private ServerSocket server=null;
    
    private String response="";
    
    /**
     * Constructor for initializing the host & port to connect the DTP through
     * @param host address where the DTP Server is hosted
     * @param port port number of the DTP Server
     */
    public ActiveServerDTP(InetAddress host,int port)
    {
        this.host=host;
        this.port=port;
    }
    
    /**
     * closes the DTP connection
     */
    public void disconnect()
    {
        RUN=false;
        try{
         
//        out.close();
//        in.close();
        client.close();
            
        }catch(Exception e){System.err.println("ActiveServerDTP>disconnect:"+e);}
    }
    /**
     * Creates the DTP Server at the given port & waits for the coming action
     */
    @Override
    public void run()
    { 
        boolean pass=false;
        while(!pass)
        {
            try{

                client=new Socket(host, port);
                in=new DataInputStream(client.getInputStream());
                out=new DataOutputStream(client.getOutputStream());
                pass=true;
            }catch(Exception e){System.err.println("ActiveServerDTP:"+e);pass=false;}
        }
        
        while(RUN);
//        try{
//            
//            server.close();
//        }catch(Exception e){}
    }
    /**
     * Send response to the DTP server
     * @param txt message to be send , +"\r\n" at the end!
     */
    private void sendCMD(String txt)
    {
        try{            
            out.writeBytes(txt+"\r\n");
            System.out.println("DTP<<"+txt);
        }catch(Exception e){e.printStackTrace();}
    }
     /**
     * Updates the global variable "response" with the latest received message.
     * @throws Exception 
     */
    private void updateResponse() throws Exception
    {
        int ch;
        response="";
        while(  (ch=in.read())!=-1)
            if((char)ch == '\n')
                break;
            else
                response+=""+(char)ch;
        if(response.length()>1     &&    response.charAt(response.length()-1)=='\r')
                response=response.substring(0,response.length()-1);
        System.out.println("DTP>>"+response);
    }
    /**
     * sends a List of filenames,permissions and modified dates of the given directory then closes the DTP
     * @param dir the given directory to be listed
     */
    public void list(File dir)
    {
        while(client==null);
        //-rw-rw-rw- 1 user group 0 Feb 9 09:38 file.txt
        try{
            cuurentFile=dir;
            File tmp[];
            if(dir.isDirectory())
                tmp=dir.listFiles();
            else
                tmp=new File[]{dir};
            
            String res="",permission="";
            if(tmp.length>0)
            {
                permission=(tmp[0].canRead()? "r":"-")+(tmp[0].canWrite()? "w":"-")+(tmp[0].canExecute()? "x":"-");
                res+=(tmp[0].isDirectory()? "d":"-")+permission+permission+permission+"   "+(tmp[0].isDirectory() ? (""+tmp[0].list().length):"1")+" anonymous anonymous "+tmp[0].length()+" "+
                        new SimpleDateFormat("MMM dd yyyy").format(new Date(tmp[0].lastModified()))
                        +" "+tmp[0].getName();
            }
            for(int i=1;i<tmp.length;i++)
            {
                permission=(tmp[i].canRead()? "r":"-")+(tmp[i].canWrite()? "w":"-")+(tmp[i].canExecute()? "x":"-");
                res+="\r\n"+(tmp[i].isDirectory()? "d":"-")+permission+permission+permission+"   "+(tmp[i].isDirectory() ? (""+tmp[i].list().length):"1")+" anonymous anonymous "+tmp[i].length()+" "
                        +new SimpleDateFormat("MMM dd yyyy").format(new Date(tmp[i].lastModified()))
                        +" "+tmp[i].getName();
            }
            sendCMD(res);
//            out.close();
//            RUN=false;
            disconnect();
        }catch(Exception e){System.err.println("ActiveServerDTP>list: "+e);}
    }
    /**
     * Sends/Retrieves the bytes of the given file
     * @param file File to be send
     */
    public void sendFile(File file) 
    {
        while(client==null);
        try{
            if(file.isDirectory())
            {
                disconnect();
                return;
            }
            FileInputStream fin=new FileInputStream(file);
            byte buff[]=new byte[255];
            int l;
            while((l=fin.read(buff))>0)
                out.write(buff, 0, l);
            fin.close();
            disconnect();
        }catch(Exception e){System.err.println("Exception while writing file: "+e);e.printStackTrace();}
    }
    /**
     * Stores a file
     * @param file file to be stored/saved
     */
    public void recieveFile(File file) 
    {
        while(client==null);
        try{
            if(file.isDirectory())
            {
                disconnect();
                return;
            }
            FileOutputStream fout=new FileOutputStream(file);
            byte buff[]=new byte[255];
            int l;
            while((l=in.read(buff))>0)
                fout.write(buff,0,l);
            fout.close();
            disconnect();
        }catch(Exception e){System.err.println("Exception while writing file: "+e);e.printStackTrace();}
    }
}