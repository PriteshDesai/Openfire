package org.jivesoftware.openfire.custom.dto;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "messageMedia")
public class MessageMedia implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2213372646899510081L;
	private String id;
	private int size;
	private String fileName;
	private String fileType;
	private int fileDuration;
	private String fileUrl;

	public MessageMedia() {
		super();
		// TODO Auto-generated constructor stub
	}

	public MessageMedia(String id, int size, String fileName, String fileType, int fileDuration, String fileUrl) {
		super();
		this.id = id;
		this.size = size;
		this.fileName = fileName;
		this.fileType = fileType;
		this.fileDuration = fileDuration;
		this.fileUrl = fileUrl;
	}

	@XmlElement(name = "id")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@XmlElement(name = "size")
	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	@XmlElement(name = "fileName")
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@XmlElement(name = "fileType")
	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	@XmlElement(name = "fileDuration")
	public int getFileDuration() {
		return fileDuration;
	}

	public void setFileDuration(int fileDuration) {
		this.fileDuration = fileDuration;
	}

	@XmlElement(name = "fileUrl")
	public String getFileUrl() {
		return fileUrl;
	}

	public void setFileUrl(String fileUrl) {
		this.fileUrl = fileUrl;
	}

	@Override
	public String toString() {
		return "{\"id\":\"" + id + "\", \"size\":" + size + ", \"fileName\":\"" + fileName + "\", \"fileType\":\"" + fileType + "\", \"fileDuration\":"
				+ fileDuration + ", \"fileUrl\":\"" + fileUrl + "\"}";
	}	

}
