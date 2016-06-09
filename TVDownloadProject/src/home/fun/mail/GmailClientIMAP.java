package home.fun.mail;

import com.sun.mail.pop3.POP3Folder;
import com.sun.mail.pop3.POP3SSLStore;
import com.sun.mail.imap.*;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.MimeBodyPart;

import org.apache.log4j.Logger;


public class GmailClientIMAP{
	private Logger logger = Logger.getLogger(GmailClientIMAP.class.getName());

    private Session session;
    private Store store;    
    private Folder folder;
    
    private String username;
    private String password;
           
    
    public static String numberOfFiles = null;
    public static int toCheck = 0;
    public static Writer output = null;
    URLName url;
    public static String receiving_attachments="C:\\download";

    
    
    
    public GmailClientIMAP() {
        session = null;
        store = null;
    }

    public void setUserPass(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public void connect()
    throws Exception {
        String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
        
        Properties props = new Properties();
        
        props.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.setProperty("mail.imap.socketFactory.fallback", "false");
        props.setProperty("mail.store.protocol", "imaps");
        props.setProperty("mail.imap.host", "imap.gmail.com");
        props.setProperty("mail.imap.port", "993");
        //props.setProperty("mail.imap.connectiontimeout", "5000");
        
    
        url = new URLName("imap","imap.gmail.com",993,"",username,password);
        session = Session.getInstance(props, null);        
        store = session.getStore(url);
        
        logger.info("start to connect with gmail");
        store.connect();
        logger.info("establish the connection");
    }
    
    
    

    public void openFolder(String folderName)
    throws Exception {
        folder = (IMAPFolder)store.getFolder(folderName);        
        logger.debug("start to open " + folder.getFullName());
        
        
        if(folder == null)
            throw new Exception("Invalid folder");
        try {
            folder.open(2);
            logger.info("Open folder " + folder.getFullName() + " is successful!");            
        } catch(Exception ex) {
            logger.error("Folder Opening Exception..",ex);
        }
    }

    public void closeFolder()
    throws Exception {
    	logger.info("close folder " + folder.getFullName() );
        folder.close(false);        
    }

    public int getMessageCount()
    throws Exception {
        return folder.getMessageCount();
    }

    public int getNewMessageCount()
    throws Exception {
        return folder.getNewMessageCount();
    }

    public void disconnect()
    throws Exception
    {
        store.close();
    }

    public void dumpAllMessages()
    throws Exception {
        Message msgs[] = folder.getMessages();
        
        FetchProfile fp = new FetchProfile();
        folder.fetch(msgs, fp);
        for(int i = 0; i < msgs.length; i++)
            dumpEnvelope(msgs[i]);

    }

    public int saveFile(File saveFile, Part part) throws Exception {

        BufferedOutputStream bos = new BufferedOutputStream( new FileOutputStream(saveFile) );

        byte[] buff = new byte[2048];
        InputStream is = part.getInputStream();
        int ret = 0, count = 0;
        while( (ret = is.read(buff)) > 0 ){
            bos.write(buff, 0, ret);
            count += ret;
        }
        bos.close();
        is.close();
        return count;
    }
    


    private  void dumpEnvelope(Message m) throws Exception {
        String body="";
        String path="";
        int size=0;
        Object content = m.getContent();
        if(content instanceof String){
            body = (String)content;
        }
        else if(content instanceof Multipart)
        {
            Multipart mp = (Multipart)content;
            for (int j=0; j < mp.getCount(); j++)
            {
                Part part = mp.getBodyPart(j);
                String disposition = part.getDisposition();
                //System.out.println("test disposition---->>"+disposition);
                if (disposition == null) {
                    // Check if plain
                    MimeBodyPart mbp = (MimeBodyPart)part;
                    if (mbp.isMimeType("text/plain")) {
                        body += mbp.getContent().toString();
                    }
                    else if (mbp.isMimeType("TEXT/HTML")) {
                        body += mbp.getContent().toString();
                    }
                    else {
                        //unknown
                    }
                } else if ((disposition != null) &&
                        (disposition.equals(Part.ATTACHMENT) || disposition.equals(Part.INLINE) || disposition.equals("ATTACHMENT") || disposition.equals("INLINE")) )
                {
                    // Check if plain
                    MimeBodyPart mbp = (MimeBodyPart)part;
                    if (mbp.isMimeType("text/plain")) {
                        body += (String)mbp.getContent();
                    }
                    else if (mbp.isMimeType("TEXT/HTML")) {
                        body += mbp.getContent().toString();
                    }
                    else {
                        File savedir = new File(receiving_attachments);
                        savedir.mkdirs();
                        File savefile = new File(savedir+"\\"+part.getFileName());
                        path = savefile.getAbsolutePath();
                        size = saveFile( savefile, part);

                    }
                }
            }
        }
        
        logger.debug("MSG: " + body);
        logger.debug("\n ----------- \n");

    }
    
    
    public static void main(String args[])
    {
        try
        {
        	GmailClientIMAP gmail = new GmailClientIMAP();
            gmail.setUserPass("zha.hongyu@gmail.com", "xxxxxx");
            gmail.connect();
            
            System.out.println("Connected !");
            
            gmail.openFolder("INBOX");
            
            gmail.dumpAllMessages();
            
            System.out.println("Total message cnt = " + gmail.getMessageCount());
            
            System.out.println("new message cnt = " + gmail.getNewMessageCount());
            
            
            gmail.disconnect();
            System.out.println("done!");
            
        }
        catch(Exception e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }



}

