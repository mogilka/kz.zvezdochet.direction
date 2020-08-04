package kz.zvezdochet.direction.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.AspectType;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.Sign;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.direction.part.AgePart;
import kz.zvezdochet.service.AspectService;

/**
 * Обработчик расчёта дирекций на указанный возраст
 * @author Natalie Didenko
 */
public class AgeCalcHandler extends Handler {
	private TreeMap<Integer, List<SkyPointAspect>> aged = null;
	private TreeMap<Integer, TreeMap<Long, Map<Long, SkyPointAspect>>> ageh = null;
	private String aspectype;
	private boolean houseFrom = false;
	List<Model> aspects = null;
	Event event;
	int initage, finage;

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			aged = new TreeMap<>();
			ageh = new TreeMap<>();
			AgePart agePart = (AgePart)activePart.getObject();
			if (!agePart.check(0)) return;
			event = agePart.getEvent();

			Collection<Planet> planets = event.getPlanets().values();
			Collection<House> houses = event.getHouses().values();
			
			updateStatus("Расчёт дирекций на возраст", false);
			List<Planet> selplanets = new ArrayList<Planet>();
			Planet selplanet = agePart.getPlanet();
			for (Planet planet : planets) {
				if (selplanet != null) {
					if (selplanet.getId().equals(planet.getId())) {
						selplanets.add(planet);
						break;
					}
				} else
					selplanets.add(planet);
			}

			List<House> selhouses = new ArrayList<House>();
			House selhouse = agePart.getHouse();
			if (event.isHousable()) {
				if (selhouse != null)
					for (House house : houses) {
						if (selhouse.getId().equals(house.getId()))
							selhouses.add(house);
					}
				else
					selhouses.addAll(houses);
			}
			AspectType selaspect = agePart.getAspect();
			if (null == selaspect)
				aspectype = null;
			else
				aspectype = selaspect.getCode();
			
			houseFrom = agePart.useHouse();

			initage = agePart.getInitialAge();
			finage = agePart.getFinalAge();

			//инициализируем аспекты
			try {
				aspects = new AspectService().getList();
			} catch (DataAccessException e) {
				e.printStackTrace();
			}

			for (int age = initage; age <= finage + 1; age++) {
				//дирекции планеты к другим планетам
				if (null == selhouse && !houseFrom) {
					for (Planet selp : selplanets)
						for (Planet selp2 : planets)
							manageCalc(selp, selp2, age);
				}
				if (event.isHousable()) {
					if (houseFrom) {
						//дирекции домов к планетам
						for (House selp : selhouses)
							for (Planet selp2 : planets)
								manageCalc(selp, selp2, age);
	
						//дирекции домов к домам
						for (House selp : selhouses)
							for (House selp2 : selhouses)
								manageCalc(selp, selp2, age);
					} else {
						//дирекции планеты к куспидам домов
						for (House selp2 : selhouses)
							for (Planet selp : selplanets)
								manageCalc(selp, selp2, age);
					}
				}
			}
			updateStatus("Расчёт дирекций завершён", false);
			List<SkyPointAspect> list = new ArrayList<SkyPointAspect>();
			for (Entry<Integer, List<SkyPointAspect>> entry : aged.entrySet())
				list.addAll(entry.getValue());

			for (Entry<Integer, TreeMap<Long, Map<Long, SkyPointAspect>>> entry : ageh.entrySet()) {
				TreeMap<Long, Map<Long, SkyPointAspect>> hmap = entry.getValue();
				for (Entry<Long, Map<Long, SkyPointAspect>> entryh : hmap.entrySet()) {
					Map<Long, SkyPointAspect> pmap = entryh.getValue();
					for (Entry<Long, SkyPointAspect> entryp : pmap.entrySet())
						list.add(entryp.getValue());
				}
			}
		    agePart.setData(list);
		    agePart.onCalc(false);
			updateStatus("Таблица дирекций сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertError(e);
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}

	/**
	 * Инициализация расчёта дирекций
	 * @param point1 первая небесная точка
	 * @param point2 вторая небесная точка
	 * @param age возраст
	 */
	private void manageCalc(SkyPoint point1, SkyPoint point2, int age) {
		if (point1 instanceof Planet)
			calc(new Planet((Planet)point1), point2, age, false);
		else
			calc(new House((House)point1), point2, age, false);
	}

	/**
	 * Определение аспектной дирекции между небесными точками
	 * @param point1 первая небесная точка
	 * @param point2 вторая небесная точка
	 * @param age возраст
	 * @param retro признак попятного движения
	 */
	private void calc(SkyPoint point1, SkyPoint point2, int age, boolean retro) {
		try {
			//находим угол между точками космограммы с учетом возраста
			double one = CalcUtil.incrementCoord(point1.getLongitude(), age, true);
			double two = point2.getLongitude();
			double res = CalcUtil.getDifference(one, two);

			//искусственно устанавливаем нарастающую оппозицию,
			//чтобы она синхронизировалась с соответствующим ей соединением в этом возрасте
			if (point2 instanceof House) {
				if (res >= 179 && res < 180)
					++res;
			} else if (point1.getCode().equals("Kethu") || point2.getCode().equals("Kethu")) {
				++res;
			}

			//определяем, является ли аспект стандартным
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;
				if (aspectype != null && !aspectype.equals(a.getType().getCode()))
					continue;

				//соединения Солнца не рассматриваем
				if (a.getPlanetid() > 0)
					continue;

				if (a.isExact(res)) {
					String acode = a.getCode();
                    if (acode.equals("OPPOSITION")) {
    	                if (point1.getCode().equals("Rakhu") || point2.getCode().equals("Rakhu"))
	                        continue;
    	                if (point1.getCode().equals("Kethu") || point2.getCode().equals("Kethu"))
	                        continue;
                    }
//					if (21 == point1.getId() && 153 == point2.getId())
//						System.out.println(one + " - " + two + " = " + res);
					if (point2 instanceof House && CalcUtil.compareAngles(one, two)) {
						++res;
						--age;
					}
					if (age < 0)
						continue;
					if (age < initage || age > finage)
						continue;

					SkyPointAspect aspect = new SkyPointAspect();
					point1.setLongitude(one);
					initPlanetHouse(point1);
					initPlanetSign(point1);
					aspect.setSkyPoint1(point1);
					aspect.setSkyPoint2(point2);
					aspect.setScore(res);
					aspect.setAge(age);
					aspect.setAspect(a);
					aspect.setRetro(retro);
					aspect.setExact(true);

					if (point1 instanceof Planet && point2 instanceof Planet) {
						List<SkyPointAspect> list = aged.get(age);
						if (null == list)
							list = new ArrayList<SkyPointAspect>();
						list.add(aspect);
						aged.put(age, list);
					} else {
						TreeMap<Long, Map<Long, SkyPointAspect>> hmap = ageh.get(age);
						if (null == hmap)
							hmap = new TreeMap<Long, Map<Long,SkyPointAspect>>();
						Map<Long, SkyPointAspect> pmap = hmap.get(point2.getId());
						if (null == pmap)
							pmap = new HashMap<Long, SkyPointAspect>();
						pmap.put(point1.getId(), aspect);
						hmap.put(point2.getId(), pmap);
						ageh.put(age, hmap);
					}
				}
			}
		} catch (Exception e) {
			DialogUtil.alertWarning(point1.getNumber() + ", " + point2.getNumber() + ", " + age);
			e.printStackTrace();
		}
	}

	/**
	 * Определяем дом дирекционной планеты
	 * @param skyPoint планета
	 */
	private void initPlanetHouse(SkyPoint skyPoint) {
		if (!(skyPoint instanceof Planet)) return;
		Map<Long, House> houseList = event.getHouses();
		Planet planet = (Planet)skyPoint;
		for (House house : houseList.values()) { 
			long h = (house.getNumber() == houseList.size()) ? 142 : house.getId() + 1;
			House house2 = houseList.get(h);
			if (SkyPoint.getHouse(house.getLongitude(), house2.getLongitude(), planet.getLongitude()))
				planet.setHouse(house);
		}
	}

	/**
	 * Определяем знак дирекционной планеты
	 * @param skyPoint планета
	 * @throws DataAccessException 
	 */
	private void initPlanetSign(SkyPoint skyPoint) throws DataAccessException {
		if (!(skyPoint instanceof Planet)) return;
		Planet planet = (Planet)skyPoint;
		Sign sign = SkyPoint.getSign(planet.getLongitude(), event.getBirthYear());
		planet.setSign(sign);
	}
}
