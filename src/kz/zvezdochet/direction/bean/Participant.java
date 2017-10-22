package kz.zvezdochet.direction.bean;

import java.util.ArrayList;
import java.util.List;

import kz.zvezdochet.analytics.bean.PlanetHouseText;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.direction.service.ParticipantService;

/**
 * Участник прогноза
 * @author Nataly Didenko
 *
 */
public class Participant extends Model {
	private static final long serialVersionUID = 5950627294568416946L;

	public Participant() {
		super();
	}

	public Participant(Event event, Collation collation) {
		super();
		this.event = event;
		this.collation = collation;
	}

	/**
	 * Событие
	 */
	private Event event;
	/**
	 * Прогноз
	 */
	private Collation collation;
	/**
	 * Признак победы участника
	 */
	private boolean win = false;

	@Override
	public ModelService getService() {
		return new ParticipantService();
	}

	public Event getEvent() {
		return event;
	}

	public void setEvent(Event event) {
		this.event = event;
	}

	public Collation getCollation() {
		return collation;
	}

	public void setCollation(Collation collation) {
		this.collation = collation;
	}

	public boolean isWin() {
		return win;
	}

	public void setWin(boolean win) {
		this.win = win;
	}

	/**
	 * Список фигурантов события
	 */
	private List<Member> members;

	public List<Member> getMembers() {
		return members;
	}

	public void setMembers(List<Member> members) {
		this.members = members;
	}

	/**
	 * Дирекции планеты участника к планетам события
	 */
	private List<SkyPointAspect> aspects = new ArrayList<>();
	/**
	 * Дирекции планеты участника к куспидам домов события
	 */
	private List<SkyPointAspect> directions = new ArrayList<>();
	/**
	 * Планеты участника в домах события
	 */
	private List<PlanetHouseText> houses = new ArrayList<>();

	public List<SkyPointAspect> getAspects() {
		return aspects;
	}

	public void setAspects(List<SkyPointAspect> aspects) {
		this.aspects = aspects;
	}

	public List<SkyPointAspect> getDirections() {
		return directions;
	}

	public void setDirections(List<SkyPointAspect> directions) {
		this.directions = directions;
	}

	public List<PlanetHouseText> getHouses() {
		return houses;
	}

	public void setHouses(List<PlanetHouseText> houses) {
		this.houses = houses;
	}

	@Override
	public void init(boolean mode) {
//		try {
//			setParticipants(new ParticipantService().finds(id));
//		} catch (DataAccessException e) {
//			e.printStackTrace();
//		}
	}
}
