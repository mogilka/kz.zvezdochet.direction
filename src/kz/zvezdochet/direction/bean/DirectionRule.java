package kz.zvezdochet.direction.bean;

import kz.zvezdochet.analytics.bean.PlanetHouseRule;
import kz.zvezdochet.core.service.ModelService;
import kz.zvezdochet.direction.service.DirectionRuleService;

/**
 * Толкование дирекции планеты к астрологическому дому
 * @author Natalie Didenko
 */
public class DirectionRule extends PlanetHouseRule {
	private static final long serialVersionUID = -8428187000344852199L;

	@Override
	public ModelService getService() {
		return new DirectionRuleService();
	}
}
