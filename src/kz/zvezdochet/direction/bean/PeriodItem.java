package kz.zvezdochet.direction.bean;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;

/**
 * Вспомогательный класс для отчётов
 * @author Nataly Didenko
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
}