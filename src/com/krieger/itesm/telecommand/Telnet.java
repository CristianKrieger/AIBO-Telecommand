package com.krieger.itesm.telecommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import android.util.Log;

public class Telnet {
	
	public Socket         socket;
	public BufferedReader bufferEntrada;
	public PrintWriter    bufferSalida;
	
	public boolean establecerConexion(String ip, int port)
	{
		try
		{
			socket = new Socket(InetAddress.getByName(ip),port);
			bufferEntrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			bufferSalida  = new PrintWriter(socket.getOutputStream(),true);
			bufferSalida.flush();
			return true;
		}
		catch(IOException ioe)
		{
			Log.e("Error", ioe.toString());
			return false;
		}
	}
	
		
	public void enviarDatos(String dato)
	{
		bufferSalida.println(dato);
	}
	
	
	public String recibirDatos()
	{
		String dato="";
		try
		{
			dato = bufferEntrada.readLine();
		}
		catch(IOException ioe)
		{
			dato = "ERROR";
			Log.e("Error", ioe.toString());
		}
		
		return dato;
	}
	
	public boolean cerrarConexion()
	{
		try
		{
			socket.close();
			bufferEntrada.close();
			bufferSalida.close();
			socket = null;
			bufferEntrada = null;
			bufferSalida = null;
			return true;
		}
		catch(IOException ioe)
		{
			Log.e("Error", ioe.toString());
			return false;
		}
	}
}
