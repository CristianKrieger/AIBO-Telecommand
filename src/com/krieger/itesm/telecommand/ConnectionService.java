package com.krieger.itesm.telecommand;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

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
            	//sendInt(msg.getData().getInt("int1"));
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
    
    private void sendBool(String name, boolean boolvaluetosend) {
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
    }
    
    private void sendInt(int intvaluetosend) {
    	for (int i=mClients.size()-1; i>=0; i--) {
            try {
            	Bundle b = new Bundle();
                b.putInt("Finished", intvaluetosend);
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
				sendBool("SendFile", true);
				return true;
			}
			catch(IOException ioe)
			{
				sendBool("SendFile", false);
				Log.e("Error", ioe.toString());
			}
		
		}
		else
		{
			sendBool("Connection", false);
		}
    	return false;
    }
    
    private void connectToAIBO(){    	
    	SharedPreferences netSettings = getSharedPreferences(PREFS_NAME, 0);
        String ip = netSettings.getString("IP", "192.168.1.3");
        int port = netSettings.getInt("Port", 54000);
    	
    	conector=new Telnet();
		if(conector.socket == null){
			sendBool("ConnectionEstablished",conector.establecerConexion(ip, port));
			
		}
		else
			sendBool("Connection", true);
    }
    
    private void disconnectFromAIBO(){
    	if(conector.socket != null && conector.bufferEntrada != null && conector.bufferSalida != null)
			sendBool("Disconnect",conector.cerrarConexion());
		else
			sendBool("Connection", false);
    }
    
    private void sendCommandToAIBO(String s){
    	if(conector.socket != null)
			conector.enviarDatos(s);
		else
			sendBool("Connection", false);
    }
    
    public void run() {
    	connectToAIBO();
        handler.sendEmptyMessage(0);
	}
	
	private Handler handler = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	                sendInt(1);
	        }
	};
}