package com.qiniu.qbox.testing;

import junit.extensions.ActiveTestSuite;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTest {
	
	public static Test suite(){ 
       TestSuite suite = new ActiveTestSuite();
       suite.addTestSuite(FileopTest.class);
       suite.addTestSuite(RsTest.class);
       return suite;
	} 
}
