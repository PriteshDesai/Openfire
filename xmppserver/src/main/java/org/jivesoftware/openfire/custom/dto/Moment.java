package org.jivesoftware.openfire.custom.dto;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "moment")
public class Moment {

	
	private String uuid;
	private String userUUID;
	private String description;
	private String attachmentType;
	private String createdByName;
	private String attachment;
	private String attachmentThumb;
	
	public Moment(String uuid, String userUUID, String description, String attachmentType, String createdByName,
			String attachment, String attachmentThumb) {
		super();
		this.uuid = uuid;
		this.userUUID = userUUID;
		this.description = description;
		this.attachmentType = attachmentType;
		this.createdByName = createdByName;
		this.attachment = attachment;
		this.attachmentThumb = attachmentThumb;
	}
	
	public Moment() {
		super();
	}
	@XmlElement
	public String getUuid() {
		return uuid;
	}
	@XmlElement
	public String getUserUUID() {
		return userUUID;
	}
	@XmlElement
	public String getDescription() {
		return description;
	}
	@XmlElement
	public String getAttachmentType() {
		return attachmentType;
	}
	@XmlElement
	public String getCreatedByName() {
		return createdByName;
	}
	@XmlElement
	public String getAttachment() {
		return attachment;
	}
	
	@XmlElement
	public String getAttachmentThumb() {
		return attachmentThumb;
	}

	public void setAttachmentThumb(String attachmentThumb) {
		this.attachmentThumb = attachmentThumb;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
	public void setUserUUID(String userUUID) {
		this.userUUID = userUUID;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public void setAttachmentType(String attachmentType) {
		this.attachmentType = attachmentType;
	}
	public void setCreatedByName(String createdByName) {
		this.createdByName = createdByName;
	}
	public void setAttachment(String attachment) {
		this.attachment = attachment;
	}
}
