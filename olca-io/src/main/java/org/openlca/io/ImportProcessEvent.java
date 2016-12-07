package org.openlca.io;

public class ImportProcessEvent {
	private String refid;
	public ImportProcessEvent(String refid)
	{
		this.refid=refid;
	}
	public String getRefid()
	{
		return this.refid;
	}
}
