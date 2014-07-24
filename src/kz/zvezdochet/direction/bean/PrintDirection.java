package kz.zvezdochet.direction.bean;

import kz.zvezdochet.bean.SkyPoint;

/**
 * Класс, предствляющий дирекцию для отчета
 * @author Nataly Didenko
 *
 */
public class PrintDirection { 
	/**
	 * Движущаяся небесная точка
	 */
	private SkyPoint skyPoint1;
	/**
	 * Статичная небесная точка
	 */
	private SkyPoint skyPoint2;
	/**
	 * Возраст дирекции
	 */
	private int age;
	
	public PrintDirection(SkyPoint p1, SkyPoint p2, int age) {
		this.skyPoint1 = p1;
		this.skyPoint2 = p2;
		this.age = age;
	}

	public SkyPoint getSkyPoint1() {
		return skyPoint1;
	}
	public void setSkyPoint1(SkyPoint skyPoint1) {
		this.skyPoint1 = skyPoint1;
	}
	public SkyPoint getSkyPoint2() {
		return skyPoint2;
	}
	public void setSkyPoint2(SkyPoint skyPoint2) {
		this.skyPoint2 = skyPoint2;
	}
	public int getAge() {
		return age;
	}
	public void setAge(int age) {
		this.age = age;
	}
}
