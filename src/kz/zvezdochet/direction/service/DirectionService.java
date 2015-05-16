package kz.zvezdochet.direction.service;

import kz.zvezdochet.analytics.service.PlanetHouseService;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.direction.bean.DirectionText;

/**
 * Сервис дирекций планет по астрологическим домам
 * @author Nataly Didenko
 */
public class DirectionService extends PlanetHouseService {

	public DirectionService() {
		tableName = "directionhouses";
	}

	@Override
	public Model create() {
		return new DirectionText();
	}
}
