package com.krieger.itesm.telecommand;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class ConnectionService extends Service implements Runnable{
	public static final String PREFS_NAME = "PrefsFile";
	
	/**
	 * Variables para conexion
	 */
	private Telnet conector;
	
	/**
	 * Variables de Notificación
	 */
	private NotificationManager nm;
	Timer tmr;
	
	/**
	 * Variables para Comunicación con UI
	 */
    ArrayList<Messenger> mClients = new ArrayList<Messenger>();
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SET_INT_VALUE = 3;
    static final int MSG_SET_STRING_VALUE = 4;
    static final int MSG_SET_BOOLEAN_VALUE = 5;
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    private static boolean isRunning = false;
    
    /**
     * Métodos y clases para Comunicación UI
     */
	@Override
	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}
	
	class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_REGISTER_CLIENT:
                mClients.add(msg.replyTo);
                break;
            case MSG_UNREGISTER_CLIENT:
                mClients.remove(msg.replyTo);
                break;
            case MSG_SET_INT_VALUE:
            	int p1=msg.getData().getInt("Ping");
            	if(p1==1)
            		pingMethod(true);
                break;
            case MSG_SET_STRING_VALUE:
            	String file=msg.getData().getString("File");
            	String command=msg.getData().getString("Command");
            	if(file!=null)
            		sendFileToAIBO(file);
            	if(command!=null)
            		sendCommandToAIBO(command);
                break;
            case MSG_SET_BOOLEAN_VALUE:
            	if(msg.getData().getBoolean("ToggleConnection")){
            		Thread thread = new Thread(ConnectionService.this);
                    thread.start();
            	}
            	else
            		disconnectFromAIBO();
            default:
                super.handleMessage(msg);
            }
        }
    }
    
    /*private void sendBool(String name, boolean boolvaluetosend) {
    	for (int i=mClients.size()-1; i>=0; i--) {
            try {
            	Bundle b = new Bundle();
                b.putBoolean(name, boolvaluetosend);
                Message msg = Message.obtain(null, MSG_SET_BOOLEAN_VALUE);
                msg.setData(b);
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
            	mClients.remove(i);
            }
        }
    }*/
    
    private void sendInt(String name, int intvaluetosend) {
    	for (int i=mClients.size()-1; i>=0; i--) {
            try {
            	Bundle b = new Bundle();
                b.putInt(name, intvaluetosend);
                Message msg = Message.obtain(null, MSG_SET_INT_VALUE);
                msg.setData(b);
                mClients.get(i).send(msg);
            } catch (RemoteException e) {
            	mClients.remove(i);
            }
        }
    }
    
    public static boolean isRunning()
    {
        return isRunning;
    }
    
    /**
     * Métodos del Life-cycle y Notificaciones
     */
    
    @Override
    public void onCreate() {
        super.onCreate();
        showNotification();
        conector=new Telnet();
    }
    
    private void showNotification() {
        nm = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        String text="Servicio de AIBO Telecommand";
        Notification notification = new Notification(R.drawable.ic_launcher, text, System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainMenu.class), 0);
        notification.setLatestEventInfo(this, getText(R.string.app_name), text, contentIntent);
        nm.notify(1, notification);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        nm.cancel(1);
    }
    
    public int onStartCommand(Intent intent, int flags, int startId) {
    	conector=new Telnet();
        return 1;
    }
    
    /**
     * Métodos para comunicación con AIBO
     */
    private boolean sendFileToAIBO(String s){
    	if(conector.socket != null)
    	{
			try
			{  
				BufferedReader archivoEntrada = null;
				archivoEntrada = new BufferedReader(new FileReader(s));
				String datos1 = "";
				String datos2 = "";
				while(archivoEntrada.ready())
				{
					datos1 = archivoEntrada.readLine();
					conector.enviarDatos(datos1);
					datos2 = datos2 + datos1 + "\n";		
					
				}
				archivoEntrada.close();
				sendInt("SendFile", 1);
				return true;
			}
			catch(IOException ioe)
			{
				sendInt("SendFile", -1);
				Log.e("Error", ioe.toString());
			}
		
		}
		else
		{
			sendInt("Connection", -1);
		}
    	return false;
    }
    
    private void connectToAIBO(){    	
    	SharedPreferences netSettings = getSharedPreferences(PREFS_NAME, 0);
        String ip = netSettings.getString("IP", "192.168.1.2");
        int port = netSettings.getInt("Port", 54000);
    	
    	conector=new Telnet();
		if(conector.socket == null){
			if(conector.establecerConexion(ip, port))
				sendInt("ConnectionEstablished",1);
			else
				sendInt("ConnectionEstablished",-1);			
		}
		else
			sendInt("Connection", 1);
		
		TimerTask t = new TimerTask() {
			
			@Override
			public void run() {
				if(!isAIBOAlive())
					connectionLost();
			}
		};
		
		tmr = new Timer();
		tmr.schedule(t, 10, 2500);
    }
    
    private void disconnectFromAIBO(){
    	if(conector.socket != null && conector.bufferEntrada != null && conector.bufferSalida != null){
    			if(conector.cerrarConexion())
    				sendInt("Disconnect",1);
    			else
    				sendInt("Disconnect",-1);
    	}
		else
			sendInt("Connection", -1);
    }
    
    private void sendCommandToAIBO(String s){
    	if(conector.socket != null)
			conector.enviarDatos(s);
		else
			sendInt("Connection", -1);
    }
    
    private boolean pingMethod(boolean response){
		SharedPreferences netSettings = getSharedPreferences(PREFS_NAME, 0);
        String ip = netSettings.getString("IP", "192.168.1.2");
        InetAddress in=null;
	    try {
	    	in = InetAddress.getByName(ip);
	    } catch (UnknownHostException e) {
	    	Log.e("ERROR","Dirección no es correcta");
	    	return false;
	    }
	    try {
	    	if(in.isReachable(5000)){
	    		if(response)
	    			sendInt("Ping", 1);
	    		return true;
	    	}
	    	else{
	    		if(response)
	    			sendInt("Ping",-1);
	    		else
	    			sendInt("ConnectionLost", 1);
	    		return false;
	    	}
	  	} catch (IOException e){
	  		Log.e("ERROR","Dirección no es correcta");
	  		return false;
	  	}
	}
    
    public boolean isAIBOAlive(){
    	return pingMethod(false);
    }
    
    public void connectionLost(){
    	sendInt("ConnectionLost", 1);
    	tmr.cancel();
    }
    
    public void run() {
    	connectToAIBO();
        handler.sendEmptyMessage(0);
	}
	
	private Handler handler = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	                sendInt("Finished", 1);
	        }
	};
}