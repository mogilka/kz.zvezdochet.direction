package kz.zvezdochet.direction.bean;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPointAspect;

/**
 * Вспомогательный класс для отчётов
 * @author Natalie Didenko
 *
 */
public class PeriodItem {
	public Aspect aspect;
	public House house;
	public Planet planet;
	public Planet planet2;

	@Override
	public boolean equals(Object obj) {
		PeriodItem other = (PeriodItem)obj;
		return this.house.getId() == other.house.getId()
				&& this.aspect.getId() == other.aspect.getId();
	}

	@Override
	public int hashCode() {
		Integer i = new Integer(house.getId().toString() + aspect.getId().toString());
		return i.hashCode();
	}

	/**
	 * Возвращает аналогичный аспект планет
	 * @return аспект планет
	 */
	public SkyPointAspect getPlanetAspect() {
		SkyPointAspect spa = new SkyPointAspect();
		spa.setAspect(aspect);
		spa.setScore(aspect.getValue());
		spa.setSkyPoint1(planet);
		spa.setSkyPoint2(planet2);
		return spa;
	}

	/**
	 * Возвращает аналогичный аспект планеты и куспида дома
	 * @return аспект планеты к дому
	 */
	public SkyPointAspect getHouseAspect() {
		SkyPointAspect spa = new SkyPointAspect();
		spa.setAspect(aspect);
		spa.setScore(aspect.getValue());
		spa.setSkyPoint1(planet);
		spa.setSkyPoint2(house);
		return spa;
	}
}
