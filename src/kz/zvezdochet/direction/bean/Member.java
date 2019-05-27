package kz.zvezdochet.direction.bean;

import java.util.ArrayList;
import java.util.List;

import kz.zvezdochet.analytics.bean.PlanetHouseText;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.direction.service.MemberService;
import kz.zvezdochet.direction.service.ParticipantService;

/**
 * Фигурант участника прогноза
 * @author Natalie Didenko
 *
 */
public class Member extends Model {
	private static final long serialVersionUID = 3703103296799225451L;

	/**
	 * Событие
	 */
	private Event event;
	/**
	 * Участник прогноза
	 */
	private Participant participant;
	/**
	 * Признак гола
	 */
	private boolean hit = false;
	/**
	 * Признак гола
	 */
	private boolean pass = false;
	/**
	 * Признак гола
	 */
	private boolean miss = false;
	/**
	 * Признак гола
	 */
	private boolean save = false;
	/**
	 * Признак гола
	 */
	private boolean foul = false;
	/**
	 * Признак гола
	 */
	private boolean substitute = false;

	public boolean isHit() {
		return hit;
	}

	public void setHit(boolean hit) {
		this.hit = hit;
	}

	public boolean isPass() {
		return pass;
	}

	public void setPass(boolean pass) {
		this.pass = pass;
	}

	public boolean isMiss() {
		return miss;
	}

	public void setMiss(boolean miss) {
		this.miss = miss;
	}

	public boolean isSave() {
		return save;
	}

	public void setSave(boolean save) {
		this.save = save;
	}

	public boolean isFoul() {
		return foul;
	}

	public void setFoul(boolean foul) {
		this.foul = foul;
	}

	public boolean isSubstitute() {
		return substitute;
	}

	public void setSubstitute(boolean substitute) {
		this.substitute = substitute;
	}

	@Override
	public ModelService getService() {
		return new MemberService();
	}

	public Event getEvent() {
		return event;
	}

	public void setEvent(Event event) {
		this.event = event;
	}

	public Participant getParticipant() {
		return participant;
	}

	public void setParticipant(Participant participant) {
		this.participant = participant;
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
		try {
			setParticipant((Participant)new ParticipantService().find(participantid));
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Идентификатор участника прогноза
	 */
	private long participantid;

	public void setParticipantid(long participantid) {
		this.participantid = participantid;
	}

	/**
	 * Признак травмы
	 */
	private boolean injury = false;

	public boolean isInjury() {
		return injury;
	}

	public void setInjury(boolean injury) {
		this.injury = injury;
	}

	public Member() {
		super();
	}

	public Member(Event event) {
		super();
		this.event = event;
	}	
}
