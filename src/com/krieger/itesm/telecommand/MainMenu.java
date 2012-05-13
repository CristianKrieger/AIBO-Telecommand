package com.krieger.itesm.telecommand;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainMenu extends Activity {
    
	int activeScreen;
	LinearLayout currentLayout;
	ProgressDialog pd;
	public static final String PREFS_NAME = "PrefsFile";
	
	private final static int TERMINAL=0;
	private final static int FUNC=1;
	private final static int ARCHIVO=2;
	private final static int CONFIG=3;
	
	private final static int VISIBLE =0;
	private final static int GONE =8;
	
	/**
	 * Variables para uso del explorador de Archivos
	 */
	private static final String ITEM_KEY = "key";
    private static final String ITEM_IMAGE = "image";
    private static final String ROOT = "/";
    public static final String START_PATH = "START_PATH";
    public static final String FORMAT_FILTER = "FORMAT_FILTER";
    public static final String RESULT_PATH = "RESULT_PATH";
    public static final String SELECTION_MODE = "SELECTION_MODE";

    private List<String> path = null;
    private TextView myPath;
    private ArrayList<HashMap<String, Object>> mList;
    private ListView fileList;
    private Button selectButton;

    private LinearLayout layoutSelect;
    private String parentPath;
    private String currentPath = ROOT;

    private File selectedFile;
    private HashMap<String, Integer> lastPositions = new HashMap<String, Integer>();
	
    /**
     * Variables para conexión de Servicio
     */
    Messenger mService = null;
	boolean mIsBound;
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    boolean connected=false;
    boolean motorState=false;
    
	private class IncomingHandler extends Handler {
	    @Override
	    public void handleMessage(Message msg) {
	        switch (msg.what) {
	        case ConnectionService.MSG_SET_INT_VALUE:
	        	int finish = msg.getData().getInt("Finished");
	        	int c1=msg.getData().getInt("Connection");
	        	int d1=msg.getData().getInt("Disconnect");
	        	int c2=msg.getData().getInt("ConnectionEstablished");
	        	int f1=msg.getData().getInt("SendFile");
	        	int p1=msg.getData().getInt("Ping");
	        	int c3=msg.getData().getInt("ConnectionLost");
	        	if(finish==1){
	        		pd.dismiss();
	        		connect.setEnabled(true);
	        		finish=0;
	        		break;
	        	}
	        	if(c1==1){
	        		connected=true;
	        		Toast.makeText(getApplicationContext(), "¡Ya hay conexión!", Toast.LENGTH_LONG).show();
	        		c1=0;
	        		break;
	        	}
	        	if(c1==-1){
	        		connected=false;
	        		Toast.makeText(getApplicationContext(), "¡No hay conexión!", Toast.LENGTH_LONG).show();
	        		c1=0;
	        		break;
	        	}
	        	if(d1==1){
	        		connected=false;
	        		Toast.makeText(getApplicationContext(), "¡Conexión cerrada!", Toast.LENGTH_LONG).show();
	        		d1=0;
	        		break;
	        	}
	        	if(d1==-1){
	        		connected=false;
	        		Toast.makeText(getApplicationContext(), "¡Error al cerrar la conexión!", Toast.LENGTH_LONG).show();
	        		d1=0;
	        		break;
	        	}
	        	if(c2==1){
	        		connected=true;
	        		Toast.makeText(getApplicationContext(), "¡Conexión Establecida!", Toast.LENGTH_LONG).show();
	        		connect.setEnabled(true);
	        		c2=0;
	        		break;
	        	}
	        	if(c2==-1){
	        		connected=false;
	        		Toast.makeText(getApplicationContext(), "¡Error al establecer la conexión!", Toast.LENGTH_LONG).show();
	        		connect.setEnabled(true);
	        		connect.setChecked(false);
	        		c2=0;
	        		break;
	        	}
	        	if(f1==1){
	        		Toast.makeText(getApplicationContext(), "¡Archivo Enviado correctamente!", Toast.LENGTH_LONG).show();
	        		f1=0;
	        		break;
	        	}
	        	if(f1==-1){
	        		Toast.makeText(getApplicationContext(), "¡Error al enviar el archivo!", Toast.LENGTH_LONG).show();
	        		f1=0;
	        		break;
	        	}
	        	if(p1==1){
	        		Toast.makeText(getApplicationContext(), "El AIBO ha respondido", Toast.LENGTH_LONG).show();
	        		break;
	        	}
	        	if(p1==-1){
	        		Toast.makeText(getApplicationContext(), "El AIBO no respondió", Toast.LENGTH_LONG).show();
	        		break;
	        	}
	        	if(c3==1){
	        		Toast.makeText(getApplicationContext(), "Se ha perdido la conexión", Toast.LENGTH_LONG).show();
	        		connected=false;
	        		connect.setChecked(false);
	        		motors.setChecked(false);
	        		break;
	        	}
	        default:
	            super.handleMessage(msg);
	        }
	    }
	}
	
	private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            try {
                Message msg = Message.obtain(null, ConnectionService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            } catch (RemoteException e) {
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };
    
    /**
     * Variables para uso de la Terminal
     */
    ListView terminal;
	ArrayList<TerminalObject> comandos=new ArrayList<TerminalObject>();
	EditText terminalInput;
	Historial historial;
	
	/**
	 * Variables para uso de las Funciones Basicas
	 */
	Spinner funcBasicas;
	ToggleButton connect;
	ToggleButton motors;
	
	private void initializeUI(){
		/**
         * Codigo de Inicializacion de Terminal
         */
        currentLayout=(LinearLayout)findViewById(R.id.layout_terminal);
        terminal = (ListView) findViewById(R.id.TerminalList);
        terminal.setAdapter(new TerminalAdapter(getApplicationContext()));
        terminalInput=(EditText)findViewById(R.id.commandLine);
        terminalInput.setOnEditorActionListener(new EditText.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(actionId == EditorInfo.IME_ACTION_SEND){
					terminalInput(null);
					return true;
				}
				return false;
			}
        });
        
        /**
         * Codigo de inicializacion de Explorador de Archivos
         */
        
        fileList=(ListView) findViewById(R.id.filelist);
        myPath = (TextView) findViewById(R.id.path);

        selectButton = (Button) findViewById(R.id.fdButtonSelect);
        selectButton.setEnabled(false);
        selectButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                        if (selectedFile != null) {
                        	String s=selectedFile.getPath();
                        	Toast.makeText(getApplicationContext(), "You have chosen: " + s, Toast.LENGTH_LONG).show();
                        	sendFile(s);
                        }
                }
        });

        layoutSelect = (LinearLayout) findViewById(R.id.fdLinearLayoutSelect);
        getDir(currentPath);
        
        /**
         * Codigo de Inicializacion de Funciones Básicas
         */
        funcBasicas=(Spinner)findViewById(R.id.spinnerFuncBas);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getApplicationContext(), R.array.funcbas, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        funcBasicas.setAdapter(adapter);
        connect=(ToggleButton)findViewById(R.id.conexion);
        motors=(ToggleButton)findViewById(R.id.encendidoMotores);
	}
	
	/**
	 * Métodos del Life-Cycle
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main);
        /**
         * Inicializacion de servicio
         */
        doBindService();
        
        if(savedInstanceState!=null){
        	activeScreen=savedInstanceState.getInt("ActiveScreen");
        	historial=(Historial)savedInstanceState.getSerializable("Historial");
        	comandos=historial.datos;
        	currentPath=savedInstanceState.getString("Path");
        	connected=savedInstanceState.getBoolean("Connected");
        	motorState=savedInstanceState.getBoolean("MotorState");
        }else
        	activeScreen=TERMINAL;
        
        initializeUI();
    }
    
    /**
     * Inicialización por cambio de orientación
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    	super.onConfigurationChanged(newConfig);
    	setContentView(R.layout.main);
    	
    	doBindService();
    	initializeUI();
    	
    	if(motorState){
    		motors.setChecked(true);
    	}
    	if(connected){
    		connect.setChecked(true);
    	}
    	ImageView btn=(ImageView)findViewById(R.id.btn_terminal);
    	switch(activeScreen){
    	case(FUNC):
    		currentLayout.setVisibility(GONE);
    		currentLayout=(LinearLayout)findViewById(R.id.layout_funciones);
			currentLayout.setVisibility(VISIBLE);
			btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_term_n));
			btn=(ImageView)findViewById(R.id.btn_func);
			btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_func_p));
    		break;
    	case(ARCHIVO):
    		currentLayout.setVisibility(GONE);
    		currentLayout=(LinearLayout)findViewById(R.id.layout_archivo);
			currentLayout.setVisibility(VISIBLE);
			btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_term_n));
			btn=(ImageView)findViewById(R.id.btn_arch);
			btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_arch_p));
    		break;
    	case(CONFIG):
    		currentLayout.setVisibility(GONE);
    		currentLayout=(LinearLayout)findViewById(R.id.layout_config);
			currentLayout.setVisibility(VISIBLE);
			btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_term_n));
			btn=(ImageView)findViewById(R.id.btn_config);
			btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_config_p));
    		break;
    	}
    }
        
    @Override
	protected void onStop() {
    	doUnbindService();
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		stopService(new Intent(MainMenu.this,ConnectionService.class));
		super.onDestroy();
	}

	/**
     * Metodo para guardado de datos durante cambio de orientacion
     */
	@Override
	protected void onSaveInstanceState(Bundle state){
		super.onSaveInstanceState(state);
		state.putInt("ActiveScreen", activeScreen);
		historial=new Historial(comandos);
		state.putSerializable("Historial", historial);
		state.putString("Path", currentPath);
		state.putBoolean("Connected", connected);
		state.putBoolean("MotorState", motorState);
	}
    
    /**
     * Métodos para el Explorador de Archivos
     */
    
	private void getDir(String dirPath) {
	
	        boolean useAutoSelection = dirPath.length() < currentPath.length();
	
	        Integer position = lastPositions.get(parentPath);
	
	        getDirImpl(dirPath);
	
	        if (position != null && useAutoSelection) {
	                fileList.setSelection(position);
	        }
	
	}
	
	private void getDirImpl(final String dirPath) {
	
	        currentPath = dirPath;
	
	        final List<String> item = new ArrayList<String>();
	        path = new ArrayList<String>();
	        mList = new ArrayList<HashMap<String, Object>>();
	
	        File f = new File(currentPath);
	        File[] files = f.listFiles();
	        if (files == null) {
	                currentPath = ROOT;
	                f = new File(currentPath);
	                files = f.listFiles();
	        }
	        myPath.setText("Ubicación" + ": " + currentPath);
	
	        if (!currentPath.equals(ROOT)) {
	
	                item.add(ROOT);
	                addItem(ROOT, R.drawable.folder);
	                path.add(ROOT);
	
	                item.add("../");
	                addItem("../", R.drawable.folder);
	                path.add(f.getParent());
	                parentPath = f.getParent();
	
	        }
	
	        TreeMap<String, String> dirsMap = new TreeMap<String, String>();
	        TreeMap<String, String> dirsPathMap = new TreeMap<String, String>();
	        TreeMap<String, String> filesMap = new TreeMap<String, String>();
	        TreeMap<String, String> filesPathMap = new TreeMap<String, String>();
	        for (File file : files) {
	                if (file.isDirectory()) {
	                        String dirName = file.getName();
	                        dirsMap.put(dirName, dirName);
	                        dirsPathMap.put(dirName, file.getPath());
	                } else {
	                        final String fileName = file.getName();
	                        filesMap.put(fileName, fileName);
	                        filesPathMap.put(fileName, file.getPath());                               
	                }
	        }
	        item.addAll(dirsMap.tailMap("").values());
	        item.addAll(filesMap.tailMap("").values());
	        path.addAll(dirsPathMap.tailMap("").values());
	        path.addAll(filesPathMap.tailMap("").values());
	
	        SimpleAdapter fileListAdap = new SimpleAdapter(this, mList, R.layout.filerow, new String[] {
	                        ITEM_KEY, ITEM_IMAGE }, new int[] { R.id.fdrowtext, R.id.fdrowimage });
	
	        for (String dir : dirsMap.tailMap("").values()) {
	                addItem(dir, R.drawable.folder);
	        }
	
	        for (String file : filesMap.tailMap("").values()) {
	                addItem(file, R.drawable.file);
	        }
	
	        fileListAdap.notifyDataSetChanged();
	
	        fileList.setAdapter(fileListAdap);
	        fileList.setOnItemClickListener(new OnItemClickListener() {
	
				@Override
				public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
					File file = new File(path.get(position));
	
	                setSelectVisible(v);
	
	                if (file.isDirectory()) {
	                        selectButton.setEnabled(false);
	                        if (file.canRead()) {
	                                lastPositions.put(currentPath, position);
	                                getDir(path.get(position));
	                        } else {
	                                crearDialogo(file.getName());
	                        }
	                } else {
	                        selectedFile = file;
	                        v.setSelected(true);
	                        selectButton.setEnabled(true);
	                }
				}
			});
	}
	
	private void crearDialogo(String fileName){
		new AlertDialog.Builder(this).setIcon(R.drawable.ic_launcher)
        .setTitle("[" + fileName + "]\n" + "¡No se puede leer este folder!")
        .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
        }).show();
	}
	
	private void addItem(String fileName, int imageId) {
	        HashMap<String, Object> item = new HashMap<String, Object>();
	        item.put(ITEM_KEY, fileName);
	        item.put(ITEM_IMAGE, imageId);
	        mList.add(item);
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	        if ((keyCode == KeyEvent.KEYCODE_BACK)&&(activeScreen==ARCHIVO)) {
	                selectButton.setEnabled(false);
	
	                if (!currentPath.equals(ROOT)) {
					        getDir(parentPath);
					} else {
					        return super.onKeyDown(keyCode, event);
					}
	
	                return true;
	        } else {
	                return super.onKeyDown(keyCode, event);
	        }
	}
	
	private void setSelectVisible(View v) {
	        layoutSelect.setVisibility(View.VISIBLE);
	        selectButton.setEnabled(false);
	}
	
	private void sendFile(String s){
		sendStringToService("File", s);
	}
	
	/**
	 * Métodos para Funciones Básicas
	 */
	
	public void toggleConnection(View v){
		ToggleButton t = (ToggleButton) v;
		if(t.isChecked())
			conectarse();
		else
			desconectarse();
	}
	
	public void toggleMotors(View v){
		ToggleButton t = (ToggleButton) v;
		if(!t.isChecked()){
			if(connected)
				turnOnMotors();
			else{
				Toast.makeText(getApplicationContext(), "¡No hay conexión!", Toast.LENGTH_LONG).show();
				t.setChecked(false);
			}
		}
		else{
			if(connected)
				turnOffMotors();
			else{
				Toast.makeText(getApplicationContext(), "¡No hay conexión!", Toast.LENGTH_LONG).show();
				t.setChecked(false);
			}
		}
	}
	
	public void toggleLEDs(View v){
		ToggleButton t = (ToggleButton) v;
		if(!t.isChecked())
			turnAllLEDsON();
		else
			turnAllLEDsOff();
	}
	
	public void conectarse(){
		sendBoolToService("ToggleConnection", true);
		pd = ProgressDialog.show(this, "Conexión al AIBO", "Conectándose...", true, false);
		connect.setEnabled(false);
		connect.setEnabled(false);
		pd.setCancelable(true);
	}
	
	public void desconectarse(){
		sendBoolToService("ToggleConnection", false);
	}
	
	private void turnOnMotors(){
		sendStringToService("Command", "motor on;");
	}
	
	private void turnOffMotors(){
		sendStringToService("Command", "motor off;");
	}
	
	private void turnAllLEDsON(){
		//TODO: Find correct code
	}
	
	private void turnAllLEDsOff(){
		//TODO: Find correct code
	}
	
	public void pingMethod(View v){
		sendIntToService("Ping", 1);
	}
    
    /**
     * Métodos para el uso de la terminal
     * 
     */
    public void terminalInput(View v){
    	String s=terminalInput.getText().toString();
    	if(s.equals(""))
    		return;
    	
    	TerminalObject temp = new TerminalObject();
    	temp.command=s;

		sendStringToService("Command", s);
		if(connected){
	    	Calendar c = Calendar.getInstance(); 
	    	Date time = c.getTime();
	    	temp.time=time.toLocaleString();
	    	comandos.add(temp);
	    	terminalInput.setText("");
	    	terminal.setAdapter(new TerminalAdapter(getApplicationContext()));
		}
    }
    
    /**
     * Métodos de preferencias
     */
    public void saveSettings(View v){
    	EditText ip = (EditText)findViewById(R.id.editIP);
    	EditText port = (EditText)findViewById(R.id.editPort);
    	SharedPreferences netSettings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = netSettings.edit();
        editor.putString("IP", ip.getText().toString());
        editor.putInt("Port", Integer.getInteger(port.getText().toString()));
        editor.commit();
    }
    
    /**
     * Controladores para la barra superior de Acciones
     * 
     */
    
    public void btnPressTerminal(View v){
    	if(activeScreen==TERMINAL)
    		return;
    	else{
    		currentLayout.setVisibility(GONE);
    		currentLayout=(LinearLayout)findViewById(R.id.layout_terminal);
    		currentLayout.setVisibility(VISIBLE);
    		ImageView btn=(ImageView)findViewById(R.id.btn_terminal);
    		btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_term_p));
    		switch(activeScreen){
    		case FUNC:
    			btn=(ImageView)findViewById(R.id.btn_func);
    			btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_func_n));
    			break;
    		case ARCHIVO:
    			btn=(ImageView)findViewById(R.id.btn_arch);
    			btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_arch_n));
    			break;
    		case CONFIG:
    			btn=(ImageView)findViewById(R.id.btn_config);
    			btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_config_n));
    			break;
    		}
    		activeScreen=TERMINAL;
    	}
    }
    
    public void btnPressFunc(View v){
    	if(activeScreen==FUNC)
    		return;
    	else{
    		currentLayout.setVisibility(GONE);
    		currentLayout=(LinearLayout)findViewById(R.id.layout_funciones);
    		currentLayout.setVisibility(VISIBLE);
    		ImageView btn=(ImageView)findViewById(R.id.btn_func);
    		btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_func_p));
    		switch(activeScreen){
    		case TERMINAL:
    			btn=(ImageView)findViewById(R.id.btn_terminal);
    			btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_term_n));
    			break;
    		case ARCHIVO:
    			btn=(ImageView)findViewById(R.id.btn_arch);
    			btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_arch_n));
    			break;
    		case CONFIG:
    			btn=(ImageView)findViewById(R.id.btn_config);
    			btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_config_n));
    			break;
    		}
    		activeScreen=FUNC;
    	}
    }
    
    public void btnPressArch(View v){
    	if(activeScreen==ARCHIVO)
    		return;
    	else{
    		currentLayout.setVisibility(GONE);
    		currentLayout=(LinearLayout)findViewById(R.id.layout_archivo);
    		currentLayout.setVisibility(VISIBLE);
    		ImageView btn=(ImageView)findViewById(R.id.btn_arch);
    		btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_arch_p));
    		switch(activeScreen){
    		case FUNC:
    			btn=(ImageView)findViewById(R.id.btn_func);
    			btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_func_n));
    			break;
    		case TERMINAL:
    			btn=(ImageView)findViewById(R.id.btn_terminal);
    			btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_term_n));
    			break;
    		case CONFIG:
    			btn=(ImageView)findViewById(R.id.btn_config);
    			btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_config_n));
    			break;
    		}
    		activeScreen=ARCHIVO;
    	}
    }
    
    public void btnPressConfig(View v){
    	if(activeScreen==CONFIG)
    		return;
    	else{
    		currentLayout.setVisibility(GONE);
    		currentLayout=(LinearLayout)findViewById(R.id.layout_config);
    		currentLayout.setVisibility(VISIBLE);
    		ImageView btn=(ImageView)findViewById(R.id.btn_config);
    		btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_config_p));
    		switch(activeScreen){
    		case FUNC:
    			btn=(ImageView)findViewById(R.id.btn_func);
    			btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_func_n));
    			break;
    		case ARCHIVO:
    			btn=(ImageView)findViewById(R.id.btn_arch);
    			btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_arch_n));
    			break;
    		case TERMINAL:
    			btn=(ImageView)findViewById(R.id.btn_terminal);
    			btn.setImageDrawable(getResources().getDrawable(R.drawable.btn_term_n));
    			break;
    		}
    		activeScreen=CONFIG;
    	}
    }
    
    /**
     * Métodos de binding y envio de datos con servicio
     */
    void doBindService() {
        bindService(new Intent(this, ConnectionService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }
    void doUnbindService() {
        if (mIsBound) {
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, ConnectionService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                } catch (RemoteException e) {
                }
            }
            unbindService(mConnection);
            mIsBound = false;
        }
    }
    
    private void sendIntToService(String name, int intvaluetosend) {
        if (mService != null) {
            try {
            	Bundle b = new Bundle();
                b.putInt(name, intvaluetosend);
                Message msg = Message.obtain(null, ConnectionService.MSG_SET_INT_VALUE);
                msg.setData(b);
                mService.send(msg);
            } catch (RemoteException e) {
            }
        }
    }
    
    private void sendStringToService(String name, String stringtosend) {
        if (mService != null) {
            try {
            	Bundle b = new Bundle();
                b.putString(name, stringtosend);
                Message msg = Message.obtain(null, ConnectionService.MSG_SET_STRING_VALUE);
                msg.setData(b);
                mService.send(msg);
            } catch (RemoteException e) {
            }
        }
    }
    
    private void sendBoolToService(String name, boolean booltosend) {
        if (mService != null) {
            try {
            	Bundle b = new Bundle();
                b.putBoolean(name, booltosend);
                Message msg = Message.obtain(null, ConnectionService.MSG_SET_BOOLEAN_VALUE);
                msg.setData(b);
                mService.send(msg);
            } catch (RemoteException e) {
            }
        }
    }
    
    /**
     * Adaptador para lista de Terminal
     * @author Krieger
     *
     */
    
	public class TerminalAdapter extends BaseAdapter {
		private Context mContext;
	
		public TerminalAdapter(Context c) {
			mContext = c;
		}
	
		@Override
		public int getCount() {
			return comandos.size();
		}
	
		@Override
		public Object getItem(int position) {
			return position;
		}
	
		@Override
		public long getItemId(int position) {
			return position;
		}
	
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			MyView sv;
			String title=null, time=null;			
	        if (convertView == null) {
	        	title=comandos.get(position).command;
	        	time=comandos.get(position).time;
	        	sv = new MyView(mContext, title, time);
	        } else {
	            sv = (MyView) convertView;
	            title=comandos.get(position).command;
	        	time=comandos.get(position).time;
	            sv.setTop(title);
	            sv.setBottom(time);
	        }
	        return sv;
	      }
	      
	      private class MyView extends LinearLayout {
	  		
	  		private TextView mTop;
	  	    private TextView mBottom;
	  	    
	  	    public MyView(Context context, String top, String bottom){
	  	    	super(context);
	  	    	LayoutInflater.from(context).inflate(R.layout.terminalrow, this, true);
	  	    	mTop=(TextView)findViewById(R.id.top);
	  	    	mTop.setText(top);
	  	    	mBottom=(TextView)findViewById(R.id.bottom);
	  	    	mBottom.setText(bottom);
	  	    }
	  	
	  	    public void setTop(String top) {
	  	        mTop.setText(top);
	  	    }
	  	
	  	    public void setBottom(String bottom) {
	  	        mBottom.setText(bottom);
	  	    }
	  	}
	}
}