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
import com.aspc.remote.rest.ReST;
import com.aspc.remote.rest.errors.ReSTException;
import com.aspc.remote.rest.internal.ReSTUtil;
import com.aspc.remote.util.misc.CLogger;
import com.aspc.remote.util.misc.FileUtil;
import com.aspc.remote.util.misc.StringUtilities;
import javafx.animation.PauseTransition;
import javafx.beans.value.*;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.*;
import javafx.scene.image.*;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.concurrent.Worker.State;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.web.WebEngine;
import javafx.stage.Stage;
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
public class PageSnapShot
{
    private static final Log LOGGER = CLogger.getLog( "au.com.stsoftware.sts.webautomation.PageSnapShot");//#LOGGER-NOPMD
    private final int width;
    private final int height;
    private final URL url;
    private File resultFile;
    private IOException renderException;
    private final Stage stage;
    private final AtomicBoolean completed=new AtomicBoolean(false);


    public PageSnapShot(final Stage stage,final String url,final int width,final int height) throws MalformedURLException, InvalidDataException
    {
        if( ReSTUtil.validateURL(url) == false)
        {
            throw new InvalidDataException( url + " not a valid web call");
        }

        this.url=new URL( url);
        this.stage=stage;
        this.width = width;
        this.height = height;


    }
    public File snap() throws IOException, InterruptedException, InvalidDataException, FileNotFoundException, ReSTException
    {
        ReST.builder(url).getResponse().checkStatus();
        if( completed.get() == false)
        {
            Platform.runLater(new SnapProcessor());

            long timeout = System.currentTimeMillis() + 15 * 60 * 1000;
            synchronized(completed)
            {
                while( true)
                {
                    if( completed.get()) break;

                    if( System.currentTimeMillis()>timeout )
                    {
                        throw new IOException( "Timed out waiting for " + url);
                    }
                    completed.wait(1000);
                }
            }
        }

        if( renderException != null)
        {
            throw renderException;
        }

        return resultFile;
    }

    private class SnapProcessor implements Runnable
    {
        @Override public void run() {

            WebView wv = new WebView();

            WebEngine we = wv.getEngine();

            Scene scene = new Scene( wv);
            stage.setScene(scene);

            ChangeListener<State> cl=(ObservableValue<? extends State> observable, State oldState, State newState) -> {
                if (newState == State.SUCCEEDED)
                {
                    PauseTransition pt = new PauseTransition(Duration.seconds(5));
                    pt.setOnFinished((ActionEvent event) -> {
                        SnapshotParameters snapshotParams = new SnapshotParameters();
                        snapshotParams.setViewport(new Rectangle2D(0, 0, width, height));

                        WritableImage newSnapshot = wv.snapshot(snapshotParams, null);
                        RenderedImage renderedImage = SwingFXUtils.fromFXImage(newSnapshot, null);
                        try {
                            String title=we.getTitle();
                            String safeTitle=StringUtilities.webSafePath(title);
                            if( safeTitle.length()< 4)safeTitle="snap";
                            if( safeTitle.length()>100)safeTitle=safeTitle.substring(0, 100);

                            File file=File.createTempFile(safeTitle, ".png", FileUtil.makeQuarantineDirectory());

                            ImageIO.write(renderedImage, "png", file);
                            resultFile=file;
                        } catch (IOException ex) {
                            LOGGER.warn( "could not create a screen shot for " + url, ex);
                            renderException=ex;
                        }
                        finally
                        {
                            synchronized(completed)
                            {
                                completed.set(true);
                                completed.notifyAll();
                            }
                            we.load( null);
                        }
                    });
                    pt.play();

                }
                else if (newState == State.FAILED)
                {
                    renderException=new IOException( "failed status when creating a screen shot for " + url);
                    synchronized(completed)
                    {
                        completed.set(true);
                        completed.notifyAll();
                        we.load( null);
                    }
                }
            };

            we.getLoadWorker().stateProperty().addListener( cl);

            we.load(url.toString());
        }
    }
}
