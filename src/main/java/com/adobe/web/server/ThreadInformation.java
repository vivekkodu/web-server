package com.adobe.web.server;

import com.adobe.web.utils.WebServerConstants;

/**
 * Created by VIVEK VERMA on 2/5/2017.
 */
public class ThreadInformation {
    public static Integer requestServed = 0;
    public static boolean[] hasRequest = new boolean[WebServerConstants.THREAD_POOL_SIZE];
    public static long[] lastSeenAt = new long[WebServerConstants.THREAD_POOL_SIZE];
}
