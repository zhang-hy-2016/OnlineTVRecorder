package home.fun.mail;

import com.sun.mail.pop3.POP3Folder;
import com.sun.mail.pop3.POP3SSLStore;




import java.io.*;
import java.util.*;

import javax.mail.*;
import org.apache.log4j.Logger;




/**
 * General client for pop3 
 * @author zha
 * @version 1.0 
 * @version 1.1 (12.10.2013)
 * - add new method getText() to parse mail text
 * 
 * @version 2.0 (10.01.2015)
 * - copied from GmailClientPop  
 * - delete mail form server  after reading 
 *
 */
public class GeneralClientPop {
	private static Logger logger = Logger.getLogger(GeneralClientPop.class.getName());

    private Session session;
    private POP3SSLStore store;
    private String username;
    private String password;
    private POP3Folder folder;       
    
    
    private String mailServer;
    private int  mailServerPort; 
    
    public static String numberOfFiles = null;
    public static int toCheck = 0;
    public static Writer output = null;
    URLName url;
    public static String receiving_attachments="C:\\download";

    public GeneralClientPop() {
        session = null;
        store = null;
    }

    public void setUserPass(String username, String password) {
        this.username = username;
        this.password = password;
    }
    
    

    /**
     * @return the mailServer
     */
    public String getMailServer() {
        return mailServer;
    }

    /**
     * @param mailServer the mailServer to set
     */
    public void setMailServer(String mailServer) {
        this.mailServer = mailServer;
    }

    /**
     * @return the mailServerPort
     */
    public int getMailServerPort() {
        return mailServerPort;
    }

    /**
     * @param mailServerPort the mailServerPort to set
     */
    public void setMailServerPort(int mailServerPort) {
        this.mailServerPort = mailServerPort;
    }

    public void connect()
    throws Exception {
        String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
        Properties pop3Props = new Properties();
        pop3Props.setProperty("mail.pop3.socketFactory.class", SSL_FACTORY);
        pop3Props.setProperty("mail.pop3.socketFactory.fallback", "false");
        pop3Props.setProperty("mail.pop3.port", Integer.toString(mailServerPort));
        pop3Props.setProperty("mail.pop3.socketFactory.port", Integer.toString(mailServerPort));
        url = new URLName("pop3", mailServer, mailServerPort, "", username, password);
        session = Session.getInstance(pop3Props, null);
        store = new POP3SSLStore(session, url);
        
        logger.info("start to connect with mail server [" + mailServer + "] on port  " + mailServerPort);       
        store.connect();
        
        //logger.debug("default folder = " + store.getDefaultFolder().getFullName() );
        
    }
    
    
    public void listFolders() throws Exception{
        //Folder defFolder = store.getDefaultFolder();  
        
        for(Folder f : store.getPersonalNamespaces()){
            logger.debug("Folder = " + f.getFullName());    
        }
        
    }
    

    public void openFolder(String folderName)
    	throws Exception {
        folder = (POP3Folder)store.getFolder(folderName);
        
        logger.debug("Try to Open the mail folder : " + folder.getFullName());
        
        
        if(folder == null)
            throw new Exception("Invalid folder");
        try {
            folder.open(Folder.READ_WRITE);
            
            logger.debug((new StringBuilder("Open "))
            			.append(folder.getFullName())
            			.append(" is successful").toString());

        } catch(Exception ex) {
            logger.debug((new StringBuilder("Folder Opening Exception..")).toString(),ex);
        }
    }
    
    
    

    public void closeFolder()
    	throws Exception {
        folder.close(true);
    }
    
    

    public int getMessageCount()
    	throws Exception {
        return folder.getMessageCount();
    }

    /**
     * 
     * @return
     * @throws Exception
     */
    public int getNewMessageCount()
    	throws Exception {
        return folder.getNewMessageCount();
    }

    public void disconnect()
    	throws Exception {
        
        closeFolder();        
        store.close();
        logger.info("mail connection is closed!");
    }

    /* this method only for test purpose */
    void readNewMessages()
    	throws Exception {
    	/* this method ONLY return the new message */
        Message msgs[] = folder.getMessages();
        FetchProfile fp = new FetchProfile();
        folder.fetch(msgs, fp);
        
        for(int i = 0; i < msgs.length; i++) {
            dumpEnvelope(msgs[i]);           
        }
        logger.debug("new mail count : " + Integer.toString( msgs.length));
    }
    
    /**
     * return the new message  
     * 
     * @return return null when there is no new message  
     */
    public String[] getNewMessages() throws Exception {
    	/* this method ONLY return the new message */
        Message msgs[] = folder.getMessages();
        FetchProfile fp = new FetchProfile();
        folder.fetch(msgs, fp);
        
        if (msgs.length == 0){
            logger.info("There is no new message in mail folder");
        	return null; 
        }
        
        logger.debug("New mail size = " + msgs.length);
        
        String[] result = new String[msgs.length]; 
        for(int i = 0; i < msgs.length; i++) {            
            result[i] = dumpEnvelope(msgs[i]);            
        }
        return result;          
    } 



    public static int saveFile(File saveFile, Part part) throws Exception {

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

    /**
     * Return message body string 
     * @param m
     * @return
     * @throws Exception
     */
    private String dumpEnvelope(Message m) throws Exception {
        String body="";
        String path="";
        int size=0;
        Object content = m.getContent();
        if(content instanceof String){
            logger.debug("message content is String");
            body = (String)content;
        } else if(content instanceof Multipart) {
            logger.debug("message content is Mulitipart");
            body = getText(m);
     
        } // end of content reading
            
        logger.debug("-------- new message begin---------");
        logger.debug(body);
        logger.debug("-------- new message end---------");
        
        m.setFlag(Flags.Flag.DELETED, true); 
        logger.debug("Delete mail from server ");
        
        return body;
    }

    

    /**
     * Return the primary text content of the message.
     */
        
    private String getText(Part p) throws MessagingException, IOException {

        if (p.isMimeType("text/*")) {
            String s = (String) p.getContent();
            return s;
        }

        if (p.isMimeType("multipart/alternative")) {
            // prefer html text over plain text
            Multipart mp = (Multipart) p.getContent();
            String text = null;
            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (bp.isMimeType("text/plain")) {
                    if (text == null)
                        text = getText(bp);
                    continue;
                } else if (bp.isMimeType("text/html")) {
                    String s = getText(bp);
                    if (s != null)
                        return s;
                } else {
                    return getText(bp);
                }
            }
            return text;
        } else if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                String s = getText(mp.getBodyPart(i));
                if (s != null)
                    return s;
            }
        }

        return null;
    }        
        
    
    /**
     * 
     * Test method  
     *
     */
    
    public static void main(String args[]) {
        try {
            GeneralClientPop client  = new GeneralClientPop();
            
            
            client.setMailServer("pop3.163.com");
            client.setMailServerPort(995);
            
            client.setUserPass("zhang_onlinetv@163.com", "xxxxxx");
            client.connect();
            
            
            client.openFolder("INBOX");
            
            client.readNewMessages();
            //System.out.println("new message cnt = " + gmail.getNewMessageCount());
            
            String[] messageStr = client.getNewMessages();  
            if (messageStr != null){
                for (int i = 0; i<messageStr.length;i++){
                    logger.debug("message[" + i + "] = " + messageStr[i]);
                }    
            }
            
            
            client.disconnect();
            
            System.out.println("done!");
        } catch(Exception e) {
            logger.debug("error : ",e);
            
            e.printStackTrace();
            System.exit(-1);
        }
    }



}

