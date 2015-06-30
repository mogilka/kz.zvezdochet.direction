package kz.zvezdochet.direction.bean;

import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.direction.service.TransitService;

/**
 * Транзит
 * @author Nataly Didenko
 *
 */
public class Transit extends Model {
	private static final long serialVersionUID = 1549847723120810835L;

	@Override
	public ModelService getService() {
		return new TransitService();
	}
	
	/**
	 * Идентификатор события
	 */
	private long eventid;
	/**
	 * Идентификатор персоны
	 */
	private long personid;
	/**
	 * Описание
	 */
	private String description;

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public long getEventid() {
		return eventid;
	}
	public void setEventid(long eventid) {
		this.eventid = eventid;
	}
	public long getPersonid() {
		return personid;
	}
	public void setPersonid(long personid) {
		this.personid = personid;
	}
}
