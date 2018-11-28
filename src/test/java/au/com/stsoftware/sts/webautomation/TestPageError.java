/*
 *  Copyright (c) 2000-2004 ASP Converters pty ltd
 *
 *  www.stSoftware.com.au
 *
 *  All Rights Reserved.
 *
 *  This software is the proprietary information of
 *  ASP Converters Pty Ltd.
 *  Use is subject to license terms.
 */

package au.com.stsoftware.sts.webautomation;

import org.apache.commons.logging.Log;
import com.aspc.remote.util.misc.CLogger;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;


/**
 * Check that we correctly handle invalid pages.
 *  <br>
 *  <i>THREAD MODE: SINGLE-THREADED test case</i>
 *
 * @author      Liam Itzhaki
 * @since       17 Feb 2016
 */
public class TestPageError extends TestCase
{
    /**
     * Creates a new unit test
     * @param name the name of the unit
     */
    public TestPageError( final String name )
    {
        super( name );
    }

    /**
     * Creates the test suite
     * @return Test the test suite
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite( TestPageError.class );
        return suite;
    }

    /**
     * Entry point to run this test standalone
     * @param args the arguments to the test
     */
    public static void main( String[] args )
    {
        Test test = suite();

        TestRunner.run( test );

        System.exit( 0 );
    }

    /**
     * Check that we do take a screen shot of a valid page.
     *
     * @throws Exception a serious problem.
     */
    public void testValidPage() throws Exception
    {
        if( GraphicsEnvironment.isHeadless())
        {
            LOGGER.warn("no graphics");
            return;
        }

        File good=new File( "/tmp/google.good");
        good.delete();
        execute( "http://www.google.com", good.toString());

        if(good.exists()==false)
        {
            fail( "did not create " + good);
        }
    }

    /**
     * check that we do NOT take a screen shot of an invalid page.
     *
     * @throws Exception a serious problem.
     */
    public void testInvalidPage() throws Exception
    {
        File bad=new File( "/tmp/google.bad");
        bad.delete();
        execute( "http://www.google.com/sjshsahajakjakaka", bad.toString());

        if(bad.exists())
        {
            fail( "Should NOT have created " + bad);
        }
    }

    private int execute( final String url, final String fn)
    {
        try {
            String javaHome = System.getProperty("java.home");
            String javaBin = javaHome +
                    File.separator + "bin" +
                    File.separator + "java";
            String classpath = System.getProperty("java.class.path");
//            String className = "com.aspc.webautomation.selftest.SimpleApp";
            String className = "au.com.stsoftware.sts.webautomation.SimpleApp";

            ProcessBuilder builder = new ProcessBuilder(
                    javaBin, "-cp", classpath, className, url, fn);

            Process process = builder.start();
            process.waitFor();
            return process.exitValue();
        } catch (IOException | InterruptedException ex) {
            LOGGER.warn("could not run", ex);
        }

        return -1;
    }

    private static final Log LOGGER = CLogger.getLog( "au.com.stsoftware.sts.webautomation.TestPageError");//#LOGGER-NOPMD
}

