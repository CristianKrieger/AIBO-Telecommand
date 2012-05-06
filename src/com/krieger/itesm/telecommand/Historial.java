package com.krieger.itesm.telecommand;

import java.io.Serializable;
import java.util.ArrayList;

public class Historial implements Serializable{
	
	private static final long serialVersionUID = -5689436474537047505L;
	public ArrayList<TerminalObject> datos;
	
	public Historial(ArrayList<TerminalObject> h){
		datos=h;
	}

}
