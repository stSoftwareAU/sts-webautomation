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


import com.aspc.remote.database.InvalidDataException;
import com.aspc.remote.rest.errors.ReSTException;
import com.aspc.remote.util.misc.CLogger;
import com.aspc.remote.util.misc.FileUtil;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.application.Platform;
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
public class SimpleApp extends Application
{
    private static final Log LOGGER = CLogger.getLog( "com.sts.webautomation.selftest.SimpleApp");//#LOGGER-NOPMD
    private String url;
    private String fileName;

    @Override
    public void init() throws Exception {
        super.init();
        Parameters p=getParameters();

        List<String> rawArgs = p.getRaw();
        if( rawArgs.size()!=2)
        {
            throw new Exception( "expected 2 args");
        }
        url=rawArgs.get(0);
        fileName=rawArgs.get(1);
    }

    @Override
    public void start(final Stage stage)
    {
        stage.show();

        Runnable scanner=() -> {
          process( stage);
        };

        Thread t=new Thread( scanner, "screen shot");
        t.start();


    }

    private void process( final Stage stage)
    {
        try
        {
            PageSnapShot pss=new PageSnapShot(stage, url, 1000, 600);
            File tmpFile=pss.snap();
            FileUtil.replaceTargetWithTempFile(tmpFile, new File( fileName));
        }
        catch( InvalidDataException | ReSTException | IOException | InterruptedException e)
        {
            LOGGER.info( url, e);
        }

        try{
            Platform.exit();
        }
        catch( Exception e)
        {
            LOGGER.warn( "can not stop", e);
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
