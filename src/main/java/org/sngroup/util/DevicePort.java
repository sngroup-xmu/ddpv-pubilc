package org.sngroup.util;

public class DevicePort{

	public String deviceName;
	public String portName;

	public DevicePort(String deviceName, String portName)
	{
		this.deviceName = deviceName;
		this.portName = portName;
	}

	public String getDeviceName()
	{
		return deviceName;
	}

	public String getPortName()
	{
		return portName;
	}

	public String getFullName(){return getDeviceName()+"_"+getPortName();}

	public boolean equals(Object pt)
	{
		return deviceName.equals(((DevicePort)pt).getDeviceName())
				&& portName.equals(((DevicePort)pt).getPortName());
	}

	public int hashCode()
	{
		return deviceName.hashCode() * 13 + portName.hashCode();
	}

	public String toString()
	{
		return getFullName();
	}


}
