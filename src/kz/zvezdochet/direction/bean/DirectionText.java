package kz.zvezdochet.direction.bean;

import kz.zvezdochet.analytics.bean.PlanetHouseText;
import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.direction.service.DirectionService;

/**
 * Толкование дирекции планеты в астрологическом доме
 * @author Natalie Didenko
 *
 */
public class DirectionText extends PlanetHouseText {
	private static final long serialVersionUID = -1376480936435498386L;

	@Override
	public ModelService getService() {
		return new DirectionService();
	}

	/**
	 * Толкование транзита
	 */
	private String description;
	/**
	 * Краткое толкование транзита
	 */
	private String code;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Признак позитивного толкования
	 */
	private boolean positive;

	public boolean isPositive() {
		return positive;
	}

	public void setPositive(boolean positive) {
		this.positive = positive;
	}

	/**
	 * Ретро-толкование транзита
	 */
	String retro;

	public String getRetro() {
		return retro;
	}

	public void setRetro(String retro) {
		this.retro = retro;
	}

	/**
	 * Аспект
	 */
	private Aspect aspect;

	public Aspect getAspect() {
		return aspect;
	}

	public void setAspect(Aspect aspect) {
		this.aspect = aspect;
	}
}
