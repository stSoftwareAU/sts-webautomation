/*
 *  Copyright (c) 2000-2004stSoftware pty ltd
 *
 *  www.stSoftware.com.au
 *
 *  All Rights Reserved.
 *
 *  This software is the proprietary information of
 * stSoftware pty ltd.
 *  Use is subject to license terms.
 */
package au.com.stsoftware.sts.webautomation;


import com.aspc.remote.jdbc.SoapResultSet;
import com.aspc.remote.soap.Client;
import com.aspc.remote.util.misc.CLogger;
import com.aspc.remote.util.misc.StringUtilities;
import java.io.File;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.stage.Stage;
import java.util.StringTokenizer;
import org.apache.commons.logging.Log;

/**
 *  Screen Snapshot
 *
 * <br>
 * <i>THREAD MODE: SINGLE-THREADED self test unit</i>
 *
 *  @author      Liam Itzhaki
 *  @since       12 Feb 2015
 */
public class WebKitDroid extends Application
{
    private static final Log LOGGER = CLogger.getLog( "au.com.stsoftware.sts.webautomation.WebKitDroid");//#LOGGER-NOPMD
    private Client list[];
    @Override
    public void init() throws Exception {
        super.init();
        Parameters p=getParameters();

        String remoteList=null;
        List<String> args=p.getUnnamed();
        for( String arg: args)
        {
            if( arg.startsWith("-R"))
            {
                remoteList=arg.substring(2);
            }
        }

        if( StringUtilities.isBlank(remoteList))
        {
            throw new IllegalArgumentException( "no remote urls." );
        }

        ArrayList<Client> a=new ArrayList<>();
        assert remoteList!=null;
        for( String url: remoteList.split(",;\\|"))
        {
            Client c=new Client( url);
            c.login();
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            String host = addr.getHostName();
            c.execute("DROID REGISTER 'snapshot via WEB-KIT from " + host + "'");

            a.add(c);
        }

        list=new Client[a.size()];
        a.toArray(list);
    }

    @Override
    public void start(final Stage stage)
    {
        stage.show();

        Runnable scanner=() -> {
            boolean block=false;

            while( true)
            {
                for( Client c: list)
                {
                    processLayer( stage, c, block);
                }

                block=true;
            }
        };

        new Thread( scanner, "droid scanner").start();

    }

    private void processLayer( final Stage stage, final Client client, final boolean block)
    {
        try
        {
            String options="";
            if( block)
            {
                options=" BLOCK 10000";
            }
            SoapResultSet r = client.fetch( "DROID BID WEB_KIT" + options);

            if( r.next())
            {
               String bidId = r.getString( "job_id");
               try
               {
                    String commands = r.getString("commands");
                    assert commands!=null;
                    LOGGER.info("commands:"+commands);
                    if (commands.contains("</echo_message>"))
                    {
                         int start = commands.indexOf("<echo_message>") + "<echo_message>".length();
                         int end = commands.indexOf("</echo_message>");
                         String echo_message = commands.substring(start,end);
                         //LOGGER.info("echo_message:"+echo_message);
                         StringTokenizer st = new StringTokenizer( echo_message, " , ");
                         int count = st.countTokens();

                         if (count==3)
                         {
                             String url = st.nextToken();
                             int w = Integer.parseInt(st.nextToken());
                             int h =  Integer.parseInt(st.nextToken());
                             url = url.replaceAll("&amp;", "&");
                             LOGGER.info( url);
                             PageSnapShot pss=new PageSnapShot(stage, url, w, h);
                             File tmpFile=pss.snap();

                             if( tmpFile.exists() && tmpFile.length() >0)
                             {
                                 try(FileInputStream in=new FileInputStream( tmpFile))
                                 {
                                     byte array[]=new byte[(int)tmpFile.length()];
                                     in.read(array);
                                     String result = new String( StringUtilities.encodeBase64(array));
                                     client.fetch( "DROID COMPLETE " + bidId + ",<DATA>"+result+"</DATA>");
                                 }
                                 tmpFile.delete();
                             }
                             else
                             {
                                 throw new Exception( "snap shot file invalid " + tmpFile);
                             }
                         }
                         else
                         {
                             throw new Exception( "wrong argument count " + count);
                         }
                    }
                    else
                    {
                        throw new Exception( "invalid command " + commands);
                    }
               }
               catch( Exception e)
               {
                   LOGGER.error( "could not process " + bidId, e);
                   String msg=e.getMessage();
                   if( StringUtilities.isBlank(msg))
                   {
                       msg=e.toString();
                   }
                   client.fetch( "DROID ERROR " + bidId + ",'" + msg.replace("\\", "\\\\").replace("'", "\\'") + "'");
               }
            }
        }
        catch(Exception e)
        {
            LOGGER.warn(client, e);
        }
    }

    /**
     * The main for the program
     *
     * @param args The command line arguments
     */
    public static void main(final String[] args)
    {
        launch(args);
    }
}
