package kz.zvezdochet.direction.service;

import kz.zvezdochet.analytics.service.PlanetHouseService;

/**
 * Сервис дирекций планет по астрологическим домам
 * @author Nataly Didenko
 */
public class DirectionService extends PlanetHouseService {

	public DirectionService() {
		tableName = "directionhouses";
	}
}
