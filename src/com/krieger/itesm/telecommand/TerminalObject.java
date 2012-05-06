package com.krieger.itesm.telecommand;

import java.io.Serializable;

public class TerminalObject implements Serializable{
	private static final long serialVersionUID = -1277795941796986557L;
	public String command;
	public String time;
	
	public TerminalObject(){
		command="";
		time="";
	}
}
