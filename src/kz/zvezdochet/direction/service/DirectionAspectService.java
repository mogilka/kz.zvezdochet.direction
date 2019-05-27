package kz.zvezdochet.direction.service;

import kz.zvezdochet.analytics.service.PlanetAspectService;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.direction.bean.DirectionAspectText;

/**
 * Сервис дирекций планет
 * @author Natalie Didenko
 */
public class DirectionAspectService extends PlanetAspectService {

	public DirectionAspectService() {
		tableName = "directionaspects";
	}

	@Override
	public Model create() {
		return new DirectionAspectText();
	}
}
