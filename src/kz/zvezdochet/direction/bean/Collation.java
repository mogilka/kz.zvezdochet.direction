package kz.zvezdochet.direction.bean;

import java.util.Date;
import java.util.List;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.direction.service.CollationService;

/**
 * Групповой прогноз
 * @author Nataly Didenko
 *
 */
public class Collation extends Model {
	private static final long serialVersionUID = 4521653171625744610L;

	public static String getTableName() {
		return "collation";
	}

	/**
	 * Идентификатор события
	 */
	private long eventid;
	/**
	 * Событие
	 */
	private Event event;
	/**
	 * Список участников события
	 */
	private List<Event> participants;
	/**
	 * Описание
	 */
	private String description = "";
	/**
	 * Признак успешного расчёта
	 */
	private boolean calculated = false;
	/**
	 * Дата расчёта
	 */
	private Date created_at;

	public long getEventid() {
		return eventid;
	}

	public void setEventid(long eventid) {
		this.eventid = eventid;
	}

	public List<Event> getParticipants() {
		return participants;
	}

	public void setParticipants(List<Event> participants) {
		this.participants = participants;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isCalculated() {
		return calculated;
	}

	public void setCalculated(boolean calculated) {
		this.calculated = calculated;
	}

	public Date getCreated_at() {
		return created_at;
	}

	public void setCreated_at(Date created_at) {
		this.created_at = created_at;
	}

	@Override
	public ModelService getService() {
		return new CollationService();
	}

	public Event getEvent() {
		return event;
	}

	public void setEvent(Event event) {
		this.event = event;
		this.eventid = event.getId();
	}
}
