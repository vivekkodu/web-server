package com.adobe.web.server;

import java.net.Socket;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by VIVEK VERMA on 2/5/2017.
 */
public class RequestQueue {
    public static Queue<Socket> httpRequestQueue = new LinkedList<Socket>();
}
