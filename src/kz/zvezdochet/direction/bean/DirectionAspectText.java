package kz.zvezdochet.direction.bean;

import kz.zvezdochet.analytics.bean.PlanetAspectText;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.direction.service.DirectionAspectService;

/**
 * Толкование дирекции планеты в аспекте к другой планете
 * @author Natalie Didenko
 *
 */
public class DirectionAspectText extends PlanetAspectText {
	private static final long serialVersionUID = -1376480936435498386L;

	@Override
	public ModelService getService() {
		return new DirectionAspectService();
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
	 * Ретро-толкование
	 */
	private String retro;

	public String getRetro() {
		return retro;
	}

	public void setRetro(String retro) {
		this.retro = retro;
	}
}
