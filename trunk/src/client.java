/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 *
 * @author yoga1290
 */
public class client extends JFrame implements  ActionListener,ListSelectionListener{
    private Socket PI,DTP;
    private JPanel jp;
    private JTextField user,pass,host,port;
    private JButton  connect;
    DefaultListModel model = new DefaultListModel();
    private JList list;
    private String response="",DTPresponse="",currentDir="";
    private DataOutputStream out,DTPout;
    private DataInputStream in,DTPin;
    /**
     * 
     */
    public client()
    {
        super("Client");
        setLayout(new GridLayout(2,1));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300,300);
        
        user=new JTextField("username");
        pass=new JPasswordField("password");
        host=new JTextField("Host");
        port=new JTextField("Port");
        JPanel jp1=new JPanel(new GridLayout(5,1));
        jp1.add(user);
        jp1.add(pass);
        jp1.add(host);
        jp1.add(port);
        connect =new JButton("Connect");
        connect.addActionListener(this);
        jp1.add(connect);
        getContentPane().add(jp1);
        
        
                list=new JList(model);
                list.addListSelectionListener(this);
        getContentPane().add(list);        
        
        setVisible(true);
    }
private void sendCMD(String txt)
    {
        try{
            out.writeBytes(txt+"\r\n");
            System.out.println("<<"+txt);
        }catch(Exception e){e.printStackTrace();}
    }
    private void readStringDTP() throws Exception
    {
        try{
            int ch;
            DTPresponse="";
            while(  (ch=DTPin.read())!=-1)
                if( (char)ch == '\n')
                    break;
                else
                    DTPresponse+=""+(char)ch;
            if(DTPresponse.length()>1     &&    DTPresponse.charAt(DTPresponse.length()-1)=='\r')
                    DTPresponse=DTPresponse.substring(0,DTPresponse.length()-1);
            System.out.println("DTP String>>"+DTPresponse);
        }catch(Exception e){e.printStackTrace();}
    }
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
     * 
     * @param ae
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        try {
            //connect button
            PI=new Socket(InetAddress.getByName(host.getText()), Integer.parseInt(port.getText()  )  );
            in=new DataInputStream(PI.getInputStream());
            out=new DataOutputStream(PI.getOutputStream());
            updateResponse();//ok
            sendCMD("USER "+user.getText());
            updateResponse();
            sendCMD("PASS "+pass.getText());
            updateResponse();
            
            sendCMD("PWD");
            updateResponse();
            currentDir=response.split(" ")[1];
            
            sendCMD("PASV");
            updateResponse();
            System.out.println(response.indexOf("(")+","+response.indexOf(")")+"<<<connecting");
            String tmp[]=response.substring(response.indexOf("(")+1,response.indexOf(")")).split(",");
            int port=Integer.parseInt(tmp[4])*256+Integer.parseInt(tmp[5]);
                        InetAddress host=InetAddress.getByAddress(new byte[]{
                                                                     (byte)Integer.parseInt(tmp[0])
                                                                    ,(byte)Integer.parseInt(tmp[1])
                                                                    ,(byte)Integer.parseInt(tmp[2])
                                                                    ,(byte)Integer.parseInt(tmp[3])
                                                                });
              
              sendCMD("LIST");
              DTP=new Socket(host,port);
              DTPin=new DataInputStream(DTP.getInputStream());
              DTPout=new DataOutputStream(DTP.getOutputStream());
              readStringDTP();
              String fulllist="";
              while(DTPresponse.length()>0)
              {
                  fulllist+=DTPresponse+"\n";
                  readStringDTP();
              }
              String tmp2[]=fulllist.split("\n");
//              String filenames="";
              
              model.clear();
              for(int i=0;i<tmp2.length;i++)
              {
                  String tmp3[]=tmp2[i].split(" ");
//                  filenames+=tmp3[tmp3.length-1]+" ";
                  model.add(i,tmp3[tmp3.length-1]);
              }
              updateResponse();
              DTPin.close();
              DTPout.close();
              DTP.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * 
     * @param lse
     */
    @Override
    public void valueChanged(ListSelectionEvent lse) {
        try{
        sendCMD("CWD "+currentDir+File.separator+list.getSelectedValue());
        
        updateResponse();
        updateResponse(); //?
        sendCMD("PASV");
            updateResponse();
            System.out.println(response.indexOf("(")+","+response.indexOf(")")+"<<<connecting");
            String tmp[]=response.substring(response.indexOf("(")+1,response.indexOf(")")).split(",");
            int port=Integer.parseInt(tmp[4])*256+Integer.parseInt(tmp[5]);
                        InetAddress host=InetAddress.getByAddress(new byte[]{
                                                                     (byte)Integer.parseInt(tmp[0])
                                                                    ,(byte)Integer.parseInt(tmp[1])
                                                                    ,(byte)Integer.parseInt(tmp[2])
                                                                    ,(byte)Integer.parseInt(tmp[3])
                                                                });
              
              sendCMD("LIST");
              DTP=new Socket(host,port);
              DTPin=new DataInputStream(DTP.getInputStream());
              DTPout=new DataOutputStream(DTP.getOutputStream());
              
              readStringDTP();
              String fulllist="";
              while(DTPresponse.length()>0)
              {
                  fulllist+=DTPresponse+"\n";
                  readStringDTP();
              }
              String tmp2[]=fulllist.split("\n");
//              String filenames="";
              
              model.clear();
              for(int i=0;i<tmp2.length;i++)
              {
                  String tmp3[]=tmp2[i].split(" ");
//                  filenames+=tmp3[tmp3.length-1]+" ";
                  model.add(i,tmp3[tmp3.length-1]);
              }
              DTPin.close();
              DTPout.close();
              DTP.close();
        }catch(Exception e){e.printStackTrace();}
    }
    /**
     * 
     * @param a
     */
    public static void main(String a[])
    {
        new client();
    }
}
