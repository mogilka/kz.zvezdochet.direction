package kz.zvezdochet.direction.bean;

import kz.zvezdochet.analytics.bean.PlanetHouseText;
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
}
